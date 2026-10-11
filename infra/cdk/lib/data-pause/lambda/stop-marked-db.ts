import {
  DeleteParameterCommand,
  GetParameterCommand,
  ParameterNotFound,
  SSMClient,
} from '@aws-sdk/client-ssm';
import {
  InvalidDBInstanceStateFault,
  RDSClient,
  StopDBInstanceCommand,
  StopDBInstanceCommandOutput,
} from '@aws-sdk/client-rds';
import { EventBridgeEvent } from 'aws-lambda';

const AUTO_RESTART_MARKER_VALUE = 'RDS-EVENT-0154';
const STOP_DB_MAX_RETRY_TIME_MS = 12 * 60 * 1000;
const STOP_DB_MIN_TIMEOUT_MS = 10 * 1000;
const STOP_DB_MAX_TIMEOUT_MS = 60 * 1000;
const STOP_DB_RETRY_FACTOR = 1.6;
const STOP_DB_RETRIES = 100;

interface RdsDbInstanceEventDetail {
  EventID?: string;
  SourceIdentifier?: string;
}

type RdsDbInstanceEvent = EventBridgeEvent<'RDS DB Instance Event', RdsDbInstanceEventDetail>;

async function stopDbInstanceWithRetry(
  rdsClient: RDSClient,
  sourceIdentifier: string,
): Promise<StopDBInstanceCommandOutput> {
  const { default: pRetry } = await import('p-retry');

  return pRetry(
    () =>
      rdsClient.send(
        new StopDBInstanceCommand({
          DBInstanceIdentifier: sourceIdentifier,
        }),
      ),
    {
      retries: STOP_DB_RETRIES,
      maxRetryTime: STOP_DB_MAX_RETRY_TIME_MS,
      minTimeout: STOP_DB_MIN_TIMEOUT_MS,
      maxTimeout: STOP_DB_MAX_TIMEOUT_MS,
      factor: STOP_DB_RETRY_FACTOR,
      randomize: true,
      shouldRetry: ({ error }) => error instanceof InvalidDBInstanceStateFault,
      onFailedAttempt: ({ attemptNumber, retriesLeft, retryDelay }) => {
        console.info('StopDBInstance is not accepted yet; retrying', {
          sourceIdentifier,
          attemptNumber,
          retriesLeft,
          retryDelay,
        });
      },
    },
  );
}

export async function handler(event: RdsDbInstanceEvent): Promise<void> {
  const stage = process.env.WORKOPS_STAGE ?? '';
  const sourceIdentifier = event.detail.SourceIdentifier;
  if (!stage) {
    throw new Error('WORKOPS_STAGE is required');
  }
  if (!sourceIdentifier) {
    throw new Error('RDS event detail.SourceIdentifier is required');
  }

  const requestMarkerName = `/workops/${stage}/data-pause/${sourceIdentifier}/auto-restart-marker`;
  const ssmClient = new SSMClient({});
  const rdsClient = new RDSClient({});

  try {
    const response = await ssmClient.send(new GetParameterCommand({ Name: requestMarkerName }));
    if (response.Parameter?.Value !== AUTO_RESTART_MARKER_VALUE) {
      console.info('DataPause marker value did not match auto restart marker', {
        sourceIdentifier,
      });
      return;
    }
  } catch (error) {
    if (error instanceof ParameterNotFound) {
      console.info('DataPause marker not found; skipping stop', {
        sourceIdentifier,
      });
      return;
    }

    throw error;
  }

  const stopResponse = await stopDbInstanceWithRetry(rdsClient, sourceIdentifier);
  const stoppedIdentifier = stopResponse.DBInstance?.DBInstanceIdentifier;
  if (stoppedIdentifier === undefined || stoppedIdentifier.length === 0) {
    throw new Error('StopDBInstance did not return DBInstance.DBInstanceIdentifier');
  }

  await ssmClient.send(
    new DeleteParameterCommand({
      Name: `/workops/${stage}/data-pause/${stoppedIdentifier}/auto-restart-marker`,
    }),
  );
}

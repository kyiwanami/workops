import {
  DeleteParameterCommand,
  GetParameterCommand,
  ParameterNotFound,
  SSMClient,
} from '@aws-sdk/client-ssm';
import { RDSClient, StopDBInstanceCommand } from '@aws-sdk/client-rds';
import { EventBridgeEvent } from 'aws-lambda';

const AUTO_RESTART_MARKER_VALUE = 'RDS-EVENT-0154';

interface RdsDbInstanceEventDetail {
  EventID?: string;
  SourceIdentifier?: string;
}

type RdsDbInstanceEvent = EventBridgeEvent<'RDS DB Instance Event', RdsDbInstanceEventDetail>;

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

  const stopResponse = await rdsClient.send(
    new StopDBInstanceCommand({
      DBInstanceIdentifier: sourceIdentifier,
    }),
  );
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

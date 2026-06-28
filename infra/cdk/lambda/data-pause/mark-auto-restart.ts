import { PutParameterCommand, SSMClient } from '@aws-sdk/client-ssm';
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

  const client = new SSMClient({});
  await client.send(
    new PutParameterCommand({
      Name: `/workops/${stage}/data-pause/${sourceIdentifier}/auto-restart-marker`,
      Overwrite: true,
      Type: 'String',
      Value: AUTO_RESTART_MARKER_VALUE,
    }),
  );
}

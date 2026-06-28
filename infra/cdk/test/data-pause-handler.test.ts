import {
  DeleteParameterCommand,
  GetParameterCommand,
  ParameterNotFound,
  PutParameterCommand,
  SSMClient,
} from '@aws-sdk/client-ssm';
import { RDSClient, StopDBInstanceCommand } from '@aws-sdk/client-rds';
import { EventBridgeEvent } from 'aws-lambda';
import { mockClient } from 'aws-sdk-client-mock';
import { handler as markAutoRestartHandler } from '../lambda/data-pause/mark-auto-restart';
import { handler as stopMarkedDbHandler } from '../lambda/data-pause/stop-marked-db';

const ssmMock = mockClient(SSMClient);
const rdsMock = mockClient(RDSClient);
const autoRestartMarkerValue = 'RDS-EVENT-0154';

interface RdsDbInstanceEventDetail {
  EventID?: string;
  SourceIdentifier?: string;
}

type RdsDbInstanceEvent = EventBridgeEvent<'RDS DB Instance Event', RdsDbInstanceEventDetail>;

const event: RdsDbInstanceEvent = {
  account: '123456789012',
  detail: {
    EventID: 'RDS-EVENT-0154',
    SourceIdentifier: 'workops-dev-db',
  },
  'detail-type': 'RDS DB Instance Event',
  id: 'event-id',
  region: 'ap-northeast-1',
  resources: [],
  source: 'aws.rds',
  time: '2026-06-28T10:00:00Z',
  version: '0',
};

describe('data-pause handlers', () => {
  beforeEach(() => {
    process.env.WORKOPS_STAGE = 'dev';
    ssmMock.reset();
    rdsMock.reset();
  });

  test('mark auto restart creates the SSM marker with overwrite enabled', async () => {
    ssmMock.on(PutParameterCommand).resolves({});

    await markAutoRestartHandler(event);

    const putCalls = ssmMock.commandCalls(PutParameterCommand);
    expect(putCalls).toHaveLength(1);
    expect(putCalls[0].args[0].input).toEqual({
      Name: '/workops/dev/data-pause/workops-dev-db/auto-restart-marker',
      Overwrite: true,
      Type: 'String',
      Value: autoRestartMarkerValue,
    });
  });

  test('stop marked DB no-ops when the SSM marker does not exist', async () => {
    ssmMock.on(GetParameterCommand).rejects(
      new ParameterNotFound({
        $metadata: {},
        message: 'not found',
      }),
    );

    await stopMarkedDbHandler(event);

    expect(ssmMock.commandCalls(GetParameterCommand)).toHaveLength(1);
    expect(rdsMock.commandCalls(StopDBInstanceCommand)).toHaveLength(0);
    expect(ssmMock.commandCalls(DeleteParameterCommand)).toHaveLength(0);
  });

  test('stop marked DB stops the source DB and deletes the response marker', async () => {
    ssmMock.on(GetParameterCommand).resolves({
      Parameter: {
        Value: autoRestartMarkerValue,
      },
    });
    rdsMock.on(StopDBInstanceCommand).resolves({
      DBInstance: {
        DBInstanceIdentifier: 'workops-dev-db-response',
      },
    });
    ssmMock.on(DeleteParameterCommand).resolves({});

    await stopMarkedDbHandler(event);

    expect(rdsMock.commandCalls(StopDBInstanceCommand)).toHaveLength(1);
    expect(rdsMock.commandCalls(StopDBInstanceCommand)[0].args[0].input).toEqual({
      DBInstanceIdentifier: 'workops-dev-db',
    });
    expect(ssmMock.commandCalls(DeleteParameterCommand)).toHaveLength(1);
    expect(ssmMock.commandCalls(DeleteParameterCommand)[0].args[0].input).toEqual({
      Name: '/workops/dev/data-pause/workops-dev-db-response/auto-restart-marker',
    });
  });

  test('stop marked DB does not stop when the marker value is not the auto restart event', async () => {
    ssmMock.on(GetParameterCommand).resolves({
      Parameter: {
        Value: 'unexpected',
      },
    });

    await stopMarkedDbHandler(event);

    expect(rdsMock.commandCalls(StopDBInstanceCommand)).toHaveLength(0);
    expect(ssmMock.commandCalls(DeleteParameterCommand)).toHaveLength(0);
  });

  test('stop marked DB propagates StopDBInstance failures', async () => {
    ssmMock.on(GetParameterCommand).resolves({
      Parameter: {
        Value: autoRestartMarkerValue,
      },
    });
    rdsMock.on(StopDBInstanceCommand).rejects(new Error('stop failed'));

    await expect(stopMarkedDbHandler(event)).rejects.toThrow('stop failed');
    expect(ssmMock.commandCalls(DeleteParameterCommand)).toHaveLength(0);
  });

  test('stop marked DB propagates DeleteParameter failures', async () => {
    ssmMock.on(GetParameterCommand).resolves({
      Parameter: {
        Value: autoRestartMarkerValue,
      },
    });
    rdsMock.on(StopDBInstanceCommand).resolves({
      DBInstance: {
        DBInstanceIdentifier: 'workops-dev-db',
      },
    });
    ssmMock.on(DeleteParameterCommand).rejects(new Error('delete failed'));

    await expect(stopMarkedDbHandler(event)).rejects.toThrow('delete failed');
  });
});

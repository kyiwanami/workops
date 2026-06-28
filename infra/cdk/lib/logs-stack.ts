import { RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';
import { readWorkopsStage, workopsStackName } from './environment';

export class LogsStack extends Stack {
  public readonly webLogGroup: LogGroup;
  public readonly migrationLogGroup: LogGroup;
  public readonly cognitoClientUrlUpdaterLogGroup: LogGroup;
  public readonly cognitoClientUrlUpdaterProviderLogGroup: LogGroup;
  public readonly dataPauseMarkAutoRestartLogGroup: LogGroup;
  public readonly dataPauseStopMarkedDbLogGroup: LogGroup;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readWorkopsStage(scope);
    super(scope, id, {
      ...props,
      stackName: workopsStackName(scope, 'logs'),
    });

    // Runtime stacks can be replaced while these short-retention logs remain available.
    this.webLogGroup = new LogGroup(this, 'WebLogGroup', {
      logGroupName: `/workops/${stage}/web`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    this.migrationLogGroup = new LogGroup(this, 'MigrationLogGroup', {
      logGroupName: `/workops/${stage}/migration`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    // Lambda logs are owned outside runtime stacks to avoid recreation races during WebDeliveryStack replacement.
    this.cognitoClientUrlUpdaterLogGroup = new LogGroup(this, 'CognitoClientUrlUpdaterLogGroup', {
      logGroupName: `/workops/${stage}/lambda/cognito-client-url-updater`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    this.cognitoClientUrlUpdaterProviderLogGroup = new LogGroup(
      this,
      'CognitoClientUrlUpdaterProviderLogGroup',
      {
        logGroupName: `/workops/${stage}/lambda/cognito-client-url-updater-provider`,
        retention: RetentionDays.ONE_WEEK,
        removalPolicy: RemovalPolicy.DESTROY,
      },
    );
    this.dataPauseMarkAutoRestartLogGroup = new LogGroup(this, 'DataPauseMarkAutoRestartLogGroup', {
      logGroupName: `/workops/${stage}/data-pause/mark-auto-restart`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    this.dataPauseStopMarkedDbLogGroup = new LogGroup(this, 'DataPauseStopMarkedDbLogGroup', {
      logGroupName: `/workops/${stage}/data-pause/stop-marked-db`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
  }
}

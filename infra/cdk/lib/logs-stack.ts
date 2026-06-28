import { CfnOutput, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';
import { exportName } from './stack-exports';

export interface LogsStackProps extends StackProps {
  stage: string;
}

export class LogsStack extends Stack {
  public readonly webLogGroup: LogGroup;
  public readonly migrationLogGroup: LogGroup;
  public readonly cognitoClientUrlUpdaterLogGroup: LogGroup;
  public readonly cognitoClientUrlUpdaterProviderLogGroup: LogGroup;
  public readonly dataPauseMarkAutoRestartLogGroup: LogGroup;
  public readonly dataPauseStopMarkedDbLogGroup: LogGroup;

  constructor(scope: Construct, id: string, props: LogsStackProps) {
    super(scope, id, props);

    // Runtime stacks can be replaced while these short-retention logs remain available.
    this.webLogGroup = new LogGroup(this, 'WebLogGroup', {
      logGroupName: `/workops/${props.stage}/web`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    this.migrationLogGroup = new LogGroup(this, 'MigrationLogGroup', {
      logGroupName: `/workops/${props.stage}/migration`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    // Lambda logs are owned outside runtime stacks to avoid recreation races during WebDeliveryStack replacement.
    this.cognitoClientUrlUpdaterLogGroup = new LogGroup(this, 'CognitoClientUrlUpdaterLogGroup', {
      logGroupName: `/workops/${props.stage}/lambda/cognito-client-url-updater`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    this.cognitoClientUrlUpdaterProviderLogGroup = new LogGroup(
      this,
      'CognitoClientUrlUpdaterProviderLogGroup',
      {
        logGroupName: `/workops/${props.stage}/lambda/cognito-client-url-updater-provider`,
        retention: RetentionDays.ONE_WEEK,
        removalPolicy: RemovalPolicy.DESTROY,
      },
    );
    this.dataPauseMarkAutoRestartLogGroup = new LogGroup(this, 'DataPauseMarkAutoRestartLogGroup', {
      logGroupName: `/workops/${props.stage}/data-pause/mark-auto-restart`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    this.dataPauseStopMarkedDbLogGroup = new LogGroup(this, 'DataPauseStopMarkedDbLogGroup', {
      logGroupName: `/workops/${props.stage}/data-pause/stop-marked-db`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });

    new CfnOutput(this, 'webLogGroupName', {
      exportName: exportName(props.stage, 'logs-web-log-group-name'),
      value: this.webLogGroup.logGroupName,
    });
    new CfnOutput(this, 'migrationLogGroupName', {
      value: this.migrationLogGroup.logGroupName,
    });
    new CfnOutput(this, 'cognitoClientUrlUpdaterLogGroupName', {
      value: this.cognitoClientUrlUpdaterLogGroup.logGroupName,
    });
    new CfnOutput(this, 'cognitoClientUrlUpdaterProviderLogGroupName', {
      value: this.cognitoClientUrlUpdaterProviderLogGroup.logGroupName,
    });
    new CfnOutput(this, 'dataPauseMarkAutoRestartLogGroupName', {
      value: this.dataPauseMarkAutoRestartLogGroup.logGroupName,
    });
    new CfnOutput(this, 'dataPauseStopMarkedDbLogGroupName', {
      value: this.dataPauseStopMarkedDbLogGroup.logGroupName,
    });
  }
}

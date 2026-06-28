import { ArnFormat, Duration, Stack, StackProps } from 'aws-cdk-lib';
import { Rule } from 'aws-cdk-lib/aws-events';
import { LambdaFunction } from 'aws-cdk-lib/aws-events-targets';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Runtime } from 'aws-cdk-lib/aws-lambda';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { Alarm, ComparisonOperator, TreatMissingData } from 'aws-cdk-lib/aws-cloudwatch';
import { SnsAction } from 'aws-cdk-lib/aws-cloudwatch-actions';
import { ILogGroup } from 'aws-cdk-lib/aws-logs';
import { ITopic, Topic } from 'aws-cdk-lib/aws-sns';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';
import { join } from 'path';

export interface DataPauseStackProps extends StackProps {
  stage: string;
  markAutoRestartLogGroup: ILogGroup;
  stopMarkedDbLogGroup: ILogGroup;
}

export class DataPauseStack extends Stack {
  constructor(scope: Construct, id: string, props: DataPauseStackProps) {
    super(scope, id, props);

    const markerParameterArn = this.formatArn({
      service: 'ssm',
      resource: 'parameter',
      resourceName: `workops/${props.stage}/data-pause/*`,
    });
    const dbInstanceArn = this.formatArn({
      service: 'rds',
      resource: 'db',
      resourceName: `workops-${props.stage}-db`,
      arnFormat: ArnFormat.COLON_RESOURCE_NAME,
    });
    const opsTopicArn = StringParameter.valueForStringParameter(
      this,
      `/workops/${props.stage}/dependencies/notifications/ops-topic-arn`,
    );
    const opsTopic = Topic.fromTopicArn(this, 'OpsNotificationTopic', opsTopicArn);

    const markAutoRestartFunction = new NodejsFunction(this, 'MarkAutoRestartFunction', {
      functionName: `workops-${props.stage}-data-pause-mark-auto-restart`,
      runtime: Runtime.NODEJS_24_X,
      entry: join(__dirname, '..', 'lambda', 'data-pause', 'mark-auto-restart.ts'),
      handler: 'handler',
      timeout: Duration.minutes(1),
      logGroup: props.markAutoRestartLogGroup,
      environment: {
        WORKOPS_STAGE: props.stage,
      },
      bundling: {
        bundleAwsSDK: true,
      },
    });
    markAutoRestartFunction.addToRolePolicy(
      new PolicyStatement({
        actions: ['ssm:PutParameter'],
        resources: [markerParameterArn],
      }),
    );

    const stopMarkedDbFunction = new NodejsFunction(this, 'StopMarkedDbFunction', {
      functionName: `workops-${props.stage}-data-pause-stop-marked-db`,
      runtime: Runtime.NODEJS_24_X,
      entry: join(__dirname, '..', 'lambda', 'data-pause', 'stop-marked-db.ts'),
      handler: 'handler',
      timeout: Duration.minutes(2),
      logGroup: props.stopMarkedDbLogGroup,
      environment: {
        WORKOPS_STAGE: props.stage,
      },
      bundling: {
        bundleAwsSDK: true,
      },
    });
    stopMarkedDbFunction.addToRolePolicy(
      new PolicyStatement({
        actions: ['ssm:GetParameter', 'ssm:DeleteParameter'],
        resources: [markerParameterArn],
      }),
    );
    stopMarkedDbFunction.addToRolePolicy(
      new PolicyStatement({
        actions: ['rds:StopDBInstance'],
        resources: [dbInstanceArn],
      }),
    );

    new Rule(this, 'MarkAutoRestartRule', {
      ruleName: `workops-${props.stage}-data-pause-mark-auto-restart`,
      eventPattern: {
        source: ['aws.rds'],
        detailType: ['RDS DB Instance Event'],
        detail: {
          EventID: ['RDS-EVENT-0154'],
          SourceIdentifier: [{ exists: true }],
        },
      },
      targets: [new LambdaFunction(markAutoRestartFunction)],
    });
    new Rule(this, 'StopMarkedDbRule', {
      ruleName: `workops-${props.stage}-data-pause-stop-marked-db`,
      eventPattern: {
        source: ['aws.rds'],
        detailType: ['RDS DB Instance Event'],
        detail: {
          EventID: ['RDS-EVENT-0088'],
          SourceIdentifier: [{ exists: true }],
        },
      },
      targets: [new LambdaFunction(stopMarkedDbFunction)],
    });

    this.createLambdaErrorAlarm(
      'MarkAutoRestartErrorsAlarm',
      `workops-${props.stage}-data-pause-mark-auto-restart-errors`,
      markAutoRestartFunction,
      opsTopic,
    );
    this.createLambdaErrorAlarm(
      'StopMarkedDbErrorsAlarm',
      `workops-${props.stage}-data-pause-stop-marked-db-errors`,
      stopMarkedDbFunction,
      opsTopic,
    );
  }

  private createLambdaErrorAlarm(
    id: string,
    alarmName: string,
    lambdaFunction: NodejsFunction,
    opsTopic: ITopic,
  ): void {
    const alarm = new Alarm(this, id, {
      alarmName,
      metric: lambdaFunction.metricErrors({
        period: Duration.minutes(1),
      }),
      threshold: 1,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      comparisonOperator: ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      treatMissingData: TreatMissingData.NOT_BREACHING,
    });
    alarm.addAlarmAction(new SnsAction(opsTopic));
  }
}

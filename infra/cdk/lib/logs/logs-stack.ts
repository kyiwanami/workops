import { RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { FilterPattern, LogGroup, MetricFilter, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';
import { readWorkopsStage, workopsStackName } from '../shared/environment';

interface SecurityMetric {
  id: string;
  pattern: string;
  metricName: string;
}

const SECURITY_METRIC_NAMESPACE = 'WorkOps/Security';
const SECURITY_METRICS: SecurityMetric[] = [
  {
    id: 'AuthorizationDeniedMetricFilter',
    pattern: '"eventType=AUTHORIZATION_DENIED"',
    metricName: 'AuthorizationDenied',
  },
  {
    id: 'UserNotLinkedMetricFilter',
    pattern: '"reasonCode=USER_NOT_LINKED"',
    metricName: 'UserNotLinked',
  },
  {
    id: 'ActorTypeMismatchMetricFilter',
    pattern: '"reasonCode=ACTOR_TYPE_MISMATCH"',
    metricName: 'ActorTypeMismatch',
  },
  {
    id: 'InvalidActorTypeMetricFilter',
    pattern: '"reasonCode=INVALID_ACTOR_TYPE"',
    metricName: 'InvalidActorType',
  },
  {
    id: 'PermissionSetNotAssignedMetricFilter',
    pattern: '"reasonCode=PERMISSION_SET_NOT_ASSIGNED"',
    metricName: 'PermissionSetNotAssigned',
  },
  {
    id: 'InvalidPermissionSetMetricFilter',
    pattern: '"reasonCode=INVALID_PERMISSION_SET"',
    metricName: 'InvalidPermissionSet',
  },
];

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

    this.createSecurityMetricFilters();
  }

  private createSecurityMetricFilters(): void {
    // Security metric filters count existing structured log markers without adding alarms.
    for (const metric of SECURITY_METRICS) {
      new MetricFilter(this, metric.id, {
        logGroup: this.webLogGroup,
        filterPattern: FilterPattern.literal(metric.pattern),
        metricNamespace: SECURITY_METRIC_NAMESPACE,
        metricName: metric.metricName,
        metricValue: '1',
      });
    }
  }
}

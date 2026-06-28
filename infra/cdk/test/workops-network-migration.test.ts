import { Match, Template } from 'aws-cdk-lib/assertions';
import { DataPauseStack } from '../lib/data-pause/data-pause-stack';
import { EgressStack } from '../lib/network/egress-stack';
import { LogsStack } from '../lib/logs/logs-stack';
import { MigrationRunnerStack } from '../lib/migration/migration-runner-stack';
import { createTestApp, testEnv } from './workops-test-fixtures';

describe('WorkOps CDK network and migration support', () => {
  test('creates the MigrationRunnerStack VPC CodeBuild project without an ECS task', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const migrationRunnerStack = new MigrationRunnerStack(app, 'MigrationRunnerStack', {
      env: testEnv,
    });
    const template = Template.fromStack(migrationRunnerStack);
    const templateText = JSON.stringify(template.toJSON());

    template.resourceCountIs('AWS::CodeBuild::Project', 1);
    template.resourceCountIs('AWS::ECS::TaskDefinition', 0);
    template.resourceCountIs('AWS::ECS::Service', 0);
    template.resourceCountIs('AWS::S3::Bucket', 0);
    template.hasResourceProperties('AWS::CodeBuild::Project', {
      Name: 'workops-dev-migration-runner',
      Environment: Match.objectLike({
        ComputeType: 'BUILD_GENERAL1_SMALL',
        Image: 'aws/codebuild/amazonlinux-x86_64-standard:5.0',
        PrivilegedMode: false,
        Type: 'LINUX_CONTAINER',
      }),
      Source: Match.objectLike({
        Type: 'CODEPIPELINE',
      }),
      VpcConfig: Match.objectLike({
        SecurityGroupIds: Match.anyValue(),
        Subnets: Match.anyValue(),
      }),
    });
    template.hasResourceProperties('AWS::CodeBuild::Project', {
      Environment: Match.objectLike({
        EnvironmentVariables: Match.arrayWith([
          Match.objectLike({
            Name: 'WORKOPS_DB_URL',
            Type: 'PARAMETER_STORE',
          }),
        ]),
      }),
    });
    template.hasResourceProperties('AWS::CodeBuild::Project', {
      Environment: Match.objectLike({
        EnvironmentVariables: Match.arrayWith([
          Match.objectLike({
            Name: 'WORKOPS_DB_USERNAME',
            Type: 'SECRETS_MANAGER',
          }),
        ]),
      }),
    });
    template.hasResourceProperties('AWS::CodeBuild::Project', {
      Environment: Match.objectLike({
        EnvironmentVariables: Match.arrayWith([
          Match.objectLike({
            Name: 'WORKOPS_DB_PASSWORD',
            Type: 'SECRETS_MANAGER',
          }),
          Match.objectLike({
            Name: 'WORKOPS_CODEARTIFACT_DOMAIN_NAME',
            Type: 'PARAMETER_STORE',
            Value: '/workops/dev/dependencies/codeartifact/domain-name',
          }),
          Match.objectLike({
            Name: 'WORKOPS_CODEARTIFACT_MAVEN_REPOSITORY_NAME',
            Type: 'PARAMETER_STORE',
            Value: '/workops/dev/dependencies/codeartifact/maven-repository-name',
          }),
        ]),
      }),
    });
    template.resourceCountIs('AWS::EC2::SecurityGroup', 0);
    template.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    template.hasResourceProperties('AWS::IAM::Role', {
      AssumeRolePolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Principal: {
              Service: 'codebuild.amazonaws.com',
            },
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: Match.arrayWith(['ssm:GetParameters', 'ssm:GetParameter']),
          }),
          Match.objectLike({
            Action: Match.arrayWith(['secretsmanager:GetSecretValue']),
          }),
          Match.objectLike({
            Action: 'codeartifact:GetAuthorizationToken',
          }),
          Match.objectLike({
            Action: Match.arrayWith([
              'codeartifact:GetRepositoryEndpoint',
              'codeartifact:ReadFromRepository',
            ]),
          }),
          Match.objectLike({
            Action: 'sts:GetServiceBearerToken',
            Condition: {
              StringEquals: {
                'sts:AWSServiceName': 'codeartifact.amazonaws.com',
              },
            },
          }),
        ]),
      },
    });
    expect(templateText).toContain('corretto25');
    expect(templateText).toContain('.workops-codeartifact');
    expect(templateText).toContain('WORKOPS_MAVEN_SETTINGS_PATH');
    expect(templateText).toContain('WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH');
    expect(templateText).toContain('python3 infra/cdk/scripts/configure-codeartifact-maven.py');
    expect(templateText).toContain('CODEARTIFACT_AUTH_TOKEN');
    expect(templateText).toContain('cd db');
    expect(templateText).toContain('mvn --settings');
    expect(templateText).toContain('-Pdev flyway:migrate');
    expect(templateText).not.toContain('flyway-commandline');
    expect(templateText).not.toContain('FLYWAY_DOWNLOAD_URL');
    expect(templateText).not.toContain('WORKOPS_FLYWAY_LOCATIONS');
    expect(templateText).not.toContain('FLYWAY_LOCATIONS');
    expect(templateText).not.toContain('./flyway-12.9.0/flyway migrate');
    expect(templateText).not.toContain('test -d db/migration');
    expect(templateText).not.toContain('apps/web/src/main/resources/db');
    expect(templateText).not.toContain('mvn -B');
    expect(templateText).not.toContain('migration-runner.jar');
    expect(templateText).not.toContain('infra/migration-runner');
    expect(templateText).not.toContain('amazonlinux-aarch64-standard');
    expect(templateText).toContain('/workops/dev/db/url');
    expect(templateText).toContain('/workops/dev/db/master');
    expect(templateText).toContain('/workops/dev/dependencies/codeartifact/domain-name');
    expect(templateText).toContain('/workops/dev/dependencies/codeartifact/maven-repository-name');
    expect(templateText).not.toContain('AWS::ECS::TaskDefinition');
    expect(templateText).not.toContain('ecs-tasks.amazonaws.com');
    expect(templateText).not.toContain(':test-sha');
    expect(templateText).not.toContain('workops-dev-migration-source');
    expect(template.toJSON()).not.toHaveProperty('Outputs');
  });

  test('creates the LogsStack log groups', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const logsStack = new LogsStack(app, 'LogsStack', {});
    const template = Template.fromStack(logsStack);

    template.resourceCountIs('AWS::Logs::LogGroup', 6);
    template.resourceCountIs('AWS::Logs::MetricFilter', 6);
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/web',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/migration',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/lambda/cognito-client-url-updater',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/lambda/cognito-client-url-updater-provider',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/data-pause/mark-auto-restart',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/data-pause/stop-marked-db',
      RetentionInDays: 7,
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/web',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/migration',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/lambda/cognito-client-url-updater',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/lambda/cognito-client-url-updater-provider',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/data-pause/mark-auto-restart',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/data-pause/stop-marked-db',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResourceProperties('AWS::Logs::MetricFilter', {
      FilterPattern: '"eventType=AUTHORIZATION_DENIED"',
      MetricTransformations: [
        {
          MetricName: 'AuthorizationDenied',
          MetricNamespace: 'WorkOps/Security',
          MetricValue: '1',
        },
      ],
    });
    template.hasResourceProperties('AWS::Logs::MetricFilter', {
      FilterPattern: '"reasonCode=USER_NOT_LINKED"',
      MetricTransformations: [
        {
          MetricName: 'UserNotLinked',
          MetricNamespace: 'WorkOps/Security',
          MetricValue: '1',
        },
      ],
    });
    template.hasResourceProperties('AWS::Logs::MetricFilter', {
      FilterPattern: '"reasonCode=ACTOR_TYPE_MISMATCH"',
      MetricTransformations: [
        {
          MetricName: 'ActorTypeMismatch',
          MetricNamespace: 'WorkOps/Security',
          MetricValue: '1',
        },
      ],
    });
    template.hasResourceProperties('AWS::Logs::MetricFilter', {
      FilterPattern: '"reasonCode=INVALID_ACTOR_TYPE"',
      MetricTransformations: [
        {
          MetricName: 'InvalidActorType',
          MetricNamespace: 'WorkOps/Security',
          MetricValue: '1',
        },
      ],
    });
    template.hasResourceProperties('AWS::Logs::MetricFilter', {
      FilterPattern: '"reasonCode=PERMISSION_SET_NOT_ASSIGNED"',
      MetricTransformations: [
        {
          MetricName: 'PermissionSetNotAssigned',
          MetricNamespace: 'WorkOps/Security',
          MetricValue: '1',
        },
      ],
    });
    template.hasResourceProperties('AWS::Logs::MetricFilter', {
      FilterPattern: '"reasonCode=INVALID_PERMISSION_SET"',
      MetricTransformations: [
        {
          MetricName: 'InvalidPermissionSet',
          MetricNamespace: 'WorkOps/Security',
          MetricValue: '1',
        },
      ],
    });
    template.resourceCountIs('AWS::CloudWatch::Alarm', 0);
    template.resourceCountIs('AWS::CloudWatch::Dashboard', 0);
    expect(template.toJSON()).not.toHaveProperty('Outputs');
  });

  test('creates the DataPauseStack RDS event handlers and alarms', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const logsStack = new LogsStack(app, 'LogsStack', {
      env: testEnv,
    });
    const dataPauseStack = new DataPauseStack(app, 'DataPauseStack', {
      env: testEnv,
      markAutoRestartLogGroup: logsStack.dataPauseMarkAutoRestartLogGroup,
      stopMarkedDbLogGroup: logsStack.dataPauseStopMarkedDbLogGroup,
    });
    const template = Template.fromStack(dataPauseStack);
    const templateText = JSON.stringify(template.toJSON());

    template.resourceCountIs('AWS::Events::Rule', 2);
    template.resourceCountIs('AWS::Lambda::Function', 2);
    template.resourceCountIs('AWS::CloudWatch::Alarm', 2);
    template.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'workops-dev-data-pause-mark-auto-restart',
      Handler: 'index.handler',
      Runtime: 'nodejs24.x',
      Environment: {
        Variables: {
          WORKOPS_STAGE: 'dev',
        },
      },
    });
    template.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'workops-dev-data-pause-stop-marked-db',
      Handler: 'index.handler',
      Runtime: 'nodejs24.x',
      Environment: {
        Variables: {
          WORKOPS_STAGE: 'dev',
        },
      },
    });
    expect(templateText).toContain('DataPauseMarkAutoRestartLogGroup');
    expect(templateText).toContain('DataPauseStopMarkedDbLogGroup');
    template.hasResourceProperties('AWS::Events::Rule', {
      Name: 'workops-dev-data-pause-mark-auto-restart',
      EventPattern: {
        source: ['aws.rds'],
        'detail-type': ['RDS DB Instance Event'],
        detail: {
          EventID: ['RDS-EVENT-0154'],
          SourceIdentifier: [
            {
              exists: true,
            },
          ],
        },
      },
      Targets: Match.arrayWith([
        Match.objectLike({
          Arn: {
            'Fn::GetAtt': [Match.stringLikeRegexp('MarkAutoRestartFunction'), 'Arn'],
          },
        }),
      ]),
    });
    template.hasResourceProperties('AWS::Events::Rule', {
      Name: 'workops-dev-data-pause-stop-marked-db',
      EventPattern: {
        source: ['aws.rds'],
        'detail-type': ['RDS DB Instance Event'],
        detail: {
          EventID: ['RDS-EVENT-0088'],
          SourceIdentifier: [
            {
              exists: true,
            },
          ],
        },
      },
      Targets: Match.arrayWith([
        Match.objectLike({
          Arn: {
            'Fn::GetAtt': [Match.stringLikeRegexp('StopMarkedDbFunction'), 'Arn'],
          },
        }),
      ]),
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'ssm:PutParameter',
            Resource: {
              'Fn::Join': [
                '',
                [
                  'arn:',
                  {
                    Ref: 'AWS::Partition',
                  },
                  ':ssm:ap-northeast-1:123456789012:parameter/workops/dev/data-pause/*',
                ],
              ],
            },
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: Match.arrayWith(['ssm:GetParameter', 'ssm:DeleteParameter']),
            Resource: {
              'Fn::Join': [
                '',
                [
                  'arn:',
                  {
                    Ref: 'AWS::Partition',
                  },
                  ':ssm:ap-northeast-1:123456789012:parameter/workops/dev/data-pause/*',
                ],
              ],
            },
          }),
          Match.objectLike({
            Action: 'rds:StopDBInstance',
            Resource: {
              'Fn::Join': [
                '',
                [
                  'arn:',
                  {
                    Ref: 'AWS::Partition',
                  },
                  ':rds:ap-northeast-1:123456789012:db:workops-dev-db',
                ],
              ],
            },
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::CloudWatch::Alarm', {
      AlarmName: 'workops-dev-data-pause-mark-auto-restart-errors',
      ComparisonOperator: 'GreaterThanOrEqualToThreshold',
      DatapointsToAlarm: 1,
      EvaluationPeriods: 1,
      MetricName: 'Errors',
      Namespace: 'AWS/Lambda',
      Period: 60,
      Statistic: 'Sum',
      Threshold: 1,
      TreatMissingData: 'notBreaching',
      AlarmActions: Match.arrayWith([
        {
          Ref: Match.stringLikeRegexp(
            'SsmParameterValueworkopsdevdependenciesnotificationsopstopicarn',
          ),
        },
      ]),
    });
    template.hasResourceProperties('AWS::CloudWatch::Alarm', {
      AlarmName: 'workops-dev-data-pause-stop-marked-db-errors',
      AlarmActions: Match.arrayWith([
        {
          Ref: Match.stringLikeRegexp(
            'SsmParameterValueworkopsdevdependenciesnotificationsopstopicarn',
          ),
        },
      ]),
    });
    expect(templateText).toContain('/workops/dev/dependencies/notifications/ops-topic-arn');
    expect(templateText).not.toContain('LookupEvents');
    expect(templateText).not.toContain('RDS-EVENT-0087');
    expect(templateText).not.toContain('DescribeDBInstances');
    expect(templateText).not.toContain('ScheduleExpression');
  });

  test('creates the P2-3 EgressStack NAT route for app subnets', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const egressStack = new EgressStack(app, 'EgressStack', {
      env: testEnv,
    });
    const template = Template.fromStack(egressStack);

    template.resourceCountIs('AWS::EC2::EIP', 1);
    template.resourceCountIs('AWS::EC2::NatGateway', 1);
    template.hasResourceProperties('AWS::EC2::NatGateway', {
      AllocationId: {
        'Fn::GetAtt': [Match.stringLikeRegexp('NatGatewayEip'), 'AllocationId'],
      },
    });
    template.resourceCountIs('AWS::EC2::Route', 2);
    template.hasResourceProperties('AWS::EC2::Route', {
      DestinationCidrBlock: '0.0.0.0/0',
      NatGatewayId: {
        Ref: Match.stringLikeRegexp('NatGateway'),
      },
    });
  });
});

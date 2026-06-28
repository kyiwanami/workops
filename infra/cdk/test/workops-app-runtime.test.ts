import { Match, Template } from 'aws-cdk-lib/assertions';
import { AppRuntimeStack } from '../lib/runtime/app-runtime-stack';
import { createTestApp, testEnv, testWebImageTag } from './workops-test-fixtures';

describe('WorkOps CDK app runtime', () => {
  test('creates the P2-3 AppRuntimeStack web service', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
      env: testEnv,
      webImageTag: testWebImageTag,
    });
    const template = Template.fromStack(appRuntimeStack);
    const templateText = JSON.stringify(template.toJSON());

    template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      FromPort: 8080,
      IpProtocol: 'tcp',
      ToPort: 8080,
    });
    template.hasResourceProperties('AWS::ECS::TaskDefinition', {
      Cpu: '512',
      Memory: '1024',
      Family: 'workops-dev-web',
      NetworkMode: 'awsvpc',
      RequiresCompatibilities: ['FARGATE'],
      RuntimePlatform: {
        CpuArchitecture: 'ARM64',
        OperatingSystemFamily: 'LINUX',
      },
      ContainerDefinitions: Match.arrayWith([
        Match.objectLike({
          Name: 'web',
          Environment: Match.arrayWith([
            {
              Name: 'AWS_REGION',
              Value: Match.anyValue(),
            },
            {
              Name: 'WORKOPS_COGNITO_USER_POOL_ID',
              Value: Match.anyValue(),
            },
            {
              Name: 'WORKOPS_COGNITO_PLATFORM_CLIENT_ID',
              Value: Match.anyValue(),
            },
            {
              Name: 'WORKOPS_COGNITO_TENANT_CLIENT_ID',
              Value: Match.anyValue(),
            },
            {
              Name: 'WORKOPS_COGNITO_HOSTED_UI_DOMAIN_BASE_URL',
              Value: Match.anyValue(),
            },
            {
              Name: 'WORKOPS_COGNITO_LOGOUT_URI',
              Value: {
                'Fn::Join': ['', Match.arrayWith(['/login'])],
              },
            },
            {
              Name: 'WORKOPS_COGNITO_PLATFORM_REDIRECT_URI',
              Value: {
                'Fn::Join': ['', Match.arrayWith(['/login/oauth2/code/platform'])],
              },
            },
            {
              Name: 'WORKOPS_COGNITO_TENANT_REDIRECT_URI',
              Value: {
                'Fn::Join': ['', Match.arrayWith(['/login/oauth2/code/tenant'])],
              },
            },
          ]),
          Essential: true,
          LogConfiguration: Match.objectLike({
            LogDriver: 'awslogs',
            Options: Match.objectLike({
              'awslogs-stream-prefix': 'web',
            }),
          }),
          PortMappings: Match.arrayWith([
            Match.objectLike({
              ContainerPort: 8080,
            }),
          ]),
          Secrets: Match.arrayWith([
            Match.objectLike({
              Name: 'SPRING_PROFILES_ACTIVE',
            }),
            Match.objectLike({
              Name: 'WORKOPS_DB_URL',
            }),
            Match.objectLike({
              Name: 'WORKOPS_DB_USERNAME',
            }),
            Match.objectLike({
              Name: 'WORKOPS_DB_PASSWORD',
            }),
          ]),
        }),
      ]),
    });
    expect(templateText).not.toContain('"Name":"SPRING_PROFILES_ACTIVE","Value":"local"');
    expect(templateText).toContain('/workops/dev/dependencies/runtime/spring-profile');
    expect(templateText).not.toContain('/workops/dev/spring/profile');
    expect(templateText).toContain(testWebImageTag);
    expect(templateText).not.toContain('p2-3-manual');
    expect(templateText).toContain('WORKOPS_COGNITO_HOSTED_UI_DOMAIN_BASE_URL');
    expect(templateText).toContain('/workops/dev/identity/hosted-ui-domain-base-url');
    expect(templateText).toContain('/workops/dev/identity/user-pool-id');
    expect(templateText).toContain('/workops/dev/identity/platform-client-id');
    expect(templateText).toContain('/workops/dev/identity/tenant-client-id');
    expect(templateText).toContain('/workops/dev/registry/web-repository-name');
    expect(templateText).toContain('/workops/dev/web-ingress/listener/http-listener-arn');
    expect(templateText).toContain('/workops/dev/web-ingress/alb-full-name');
    expect(templateText).toContain('/workops/dev/web-delivery/cloudfront-https-url');
    expect(templateText).toContain('/login');
    expect(templateText).toContain('/login/oauth2/code/platform');
    expect(templateText).toContain('/login/oauth2/code/tenant');
    expect(templateText).not.toContain('WORKOPS_COGNITO_CLIENT_ID');
    expect(templateText).not.toContain('WORKOPS_COGNITO_REDIRECT_URI');
    expect(templateText).not.toContain('/login/oauth2/code/cognito');
    template.resourceCountIs('AWS::ElasticLoadBalancingV2::TargetGroup', 2);
    template.hasResourceProperties('AWS::ElasticLoadBalancingV2::TargetGroup', {
      HealthCheckIntervalSeconds: 30,
      HealthCheckPath: '/actuator/health',
      HealthCheckTimeoutSeconds: 5,
      HealthyThresholdCount: 2,
      Matcher: {
        HttpCode: '200',
      },
      Name: 'workops-dev-web-blue-tg',
      Port: 8080,
      Protocol: 'HTTP',
      TargetGroupAttributes: Match.arrayWith([
        {
          Key: 'deregistration_delay.timeout_seconds',
          Value: '30',
        },
      ]),
      TargetType: 'ip',
      UnhealthyThresholdCount: 3,
    });
    template.hasResourceProperties('AWS::ElasticLoadBalancingV2::TargetGroup', {
      Name: 'workops-dev-web-green-tg',
      TargetType: 'ip',
    });
    template.hasResourceProperties('AWS::ElasticLoadBalancingV2::ListenerRule', {
      Actions: Match.arrayWith([
        Match.objectLike({
          TargetGroupArn: {
            Ref: Match.stringLikeRegexp('WebBlueTargetGroup'),
          },
          Type: 'forward',
        }),
      ]),
      Conditions: Match.arrayWith([
        Match.objectLike({
          Field: 'path-pattern',
          PathPatternConfig: {
            Values: ['/*'],
          },
        }),
      ]),
      Priority: 10,
    });
    template.resourceCountIs('AWS::CloudWatch::Alarm', 2);
    template.hasResourceProperties('AWS::CloudWatch::Alarm', {
      AlarmName: 'workops-dev-web-healthy-host-count-low',
      ComparisonOperator: 'LessThanThreshold',
      DatapointsToAlarm: 2,
      EvaluationPeriods: 2,
      Metrics: Match.arrayWith([
        Match.objectLike({
          Expression: 'blueHealthy + greenHealthy',
        }),
      ]),
      Threshold: 1,
      TreatMissingData: 'notBreaching',
    });
    template.hasResourceProperties('AWS::CloudWatch::Alarm', {
      AlarmName: 'workops-dev-web-unhealthy-host-count',
      ComparisonOperator: 'GreaterThanThreshold',
      DatapointsToAlarm: 2,
      EvaluationPeriods: 2,
      Metrics: Match.arrayWith([
        Match.objectLike({
          Expression: 'blueUnhealthy + greenUnhealthy',
        }),
      ]),
      Threshold: 0,
      TreatMissingData: 'notBreaching',
    });
    template.hasResourceProperties('AWS::ECS::Service', {
      DesiredCount: 1,
      DeploymentConfiguration: Match.objectLike({
        Alarms: Match.objectLike({
          AlarmNames: Match.arrayWith([
            Match.objectLike({
              Ref: Match.stringLikeRegexp('WebHealthyHostAlarm'),
            }),
            Match.objectLike({
              Ref: Match.stringLikeRegexp('WebUnhealthyHostAlarm'),
            }),
          ]),
          Enable: true,
          Rollback: true,
        }),
        BakeTimeInMinutes: 3,
        DeploymentCircuitBreaker: {
          Enable: true,
          Rollback: true,
        },
        MaximumPercent: 200,
        MinimumHealthyPercent: 100,
        Strategy: 'BLUE_GREEN',
      }),
      HealthCheckGracePeriodSeconds: 90,
      LaunchType: 'FARGATE',
      LoadBalancers: Match.arrayWith([
        Match.objectLike({
          AdvancedConfiguration: Match.objectLike({
            AlternateTargetGroupArn: {
              Ref: Match.stringLikeRegexp('WebGreenTargetGroup'),
            },
            ProductionListenerRule: {
              Ref: Match.stringLikeRegexp('WebListenerRule'),
            },
          }),
          ContainerName: 'web',
          ContainerPort: 8080,
          TargetGroupArn: {
            Ref: Match.stringLikeRegexp('WebBlueTargetGroup'),
          },
        }),
      ]),
      NetworkConfiguration: {
        AwsvpcConfiguration: Match.objectLike({
          AssignPublicIp: 'DISABLED',
        }),
      },
      ServiceName: 'workops-dev-web',
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: Match.arrayWith(['ssm:GetParameters']),
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: Match.arrayWith(['secretsmanager:GetSecretValue']),
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'cognito-idp:AdminCreateUser',
            Effect: 'Allow',
            Resource: Match.anyValue(),
          }),
        ]),
      },
    });
    expect(templateText).toContain('WORKOPS_COGNITO_USER_POOL_ID');
    expect(templateText).toContain('HealthyHostCount');
    expect(templateText).toContain('UnHealthyHostCount');
    expect(templateText).not.toContain('HTTPCode_Target_5XX_Count');
    expect(templateText).not.toContain('TargetResponseTime');
    expect(templateText).not.toContain('cognito-idp:AdminDeleteUser');
    expect(templateText).not.toContain('cognito-idp:AdminGetUser');
    expect(templateText).not.toContain('cognito-idp:AdminUpdateUserAttributes');
    expect(templateText).not.toContain('cognito-idp:AdminDisableUser');
    expect(templateText).toContain('awslogs-stream-prefix');
    expect(templateText).not.toContain('/workops/dev/migration');
    expect(templateText).not.toContain('AWS::CodeDeploy');
  });
});

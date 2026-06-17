import { App, Stack, Tags } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { readFileSync } from 'fs';
import { join } from 'path';
import { Construct } from 'constructs';
import { AppRuntimeStack } from '../lib/app-runtime-stack';
import { ConfigStack } from '../lib/config-stack';
import { DataStack } from '../lib/data-stack';
import { EdgeStack } from '../lib/edge-stack';
import { EgressStack } from '../lib/egress-stack';
import { FoundationStack } from '../lib/foundation-stack';
import { IdentityStack } from '../lib/identity-stack';
import { LogsStack } from '../lib/logs-stack';
import { RegistryStack } from '../lib/registry-stack';
import { SecretStack } from '../lib/secret-stack';

class TaggedResourceStack extends Stack {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    // A concrete test resource makes stack-level tags visible in assertions.
    new Topic(this, 'TaggedTopic');
  }
}

const testCognitoUserPoolId = 'ap-northeast-1_test';
const testCognitoUserPoolClientId = 'testclientid';

describe('WorkOps CDK app', () => {
  test('creates Phase 2 base stack shells using the requested stage', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const secretStack = new SecretStack(app, 'SecretStack', {
      stackName: `workops-${stage}-secret`,
    });
    const dataStack = new DataStack(app, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      stage,
      stackName: `workops-${stage}-data`,
      vpc: foundationStack.vpc,
    });
    const configStack = new ConfigStack(app, 'ConfigStack', {
      stage,
      stackName: `workops-${stage}-config`,
    });
    const identityStack = new IdentityStack(app, 'IdentityStack', {
      stage,
      stackName: `workops-${stage}-identity`,
    });
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      stage,
      stackName: `workops-${stage}-registry`,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      stage,
      stackName: `workops-${stage}-logs`,
    });
    const egressStack = new EgressStack(app, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      publicSubnets: foundationStack.publicSubnets,
      stage,
      stackName: `workops-${stage}-egress`,
      vpc: foundationStack.vpc,
    });
    const edgeStack = new EdgeStack(app, 'EdgeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cognitoUserPoolClientId: testCognitoUserPoolClientId,
      cognitoUserPoolId: testCognitoUserPoolId,
      stage,
      stackName: `workops-${stage}-edge`,
      vpc: foundationStack.vpc,
    });
    const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cluster: foundationStack.ecsCluster,
      repository: registryStack.repository,
      stage,
      stackName: `workops-${stage}-app-runtime`,
      targetGroup: edgeStack.targetGroup,
      webLogGroup: logsStack.webLogGroup,
    });

    expect(foundationStack.stackName).toBe('workops-dev-foundation');
    expect(secretStack.stackName).toBe('workops-dev-secret');
    expect(dataStack.stackName).toBe('workops-dev-data');
    expect(configStack.stackName).toBe('workops-dev-config');
    expect(identityStack.stackName).toBe('workops-dev-identity');
    expect(registryStack.stackName).toBe('workops-dev-registry');
    expect(logsStack.stackName).toBe('workops-dev-logs');
    expect(egressStack.stackName).toBe('workops-dev-egress');
    expect(edgeStack.stackName).toBe('workops-dev-edge');
    expect(appRuntimeStack.stackName).toBe('workops-dev-app-runtime');
  });

  test('creates the FoundationStack network and cluster resources', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const template = Template.fromStack(foundationStack);

    template.resourceCountIs('AWS::EC2::VPC', 1);
    template.hasResourceProperties('AWS::EC2::VPC', {
      CidrBlock: '10.0.0.0/16',
      Tags: Match.arrayWith([
        {
          Key: 'Name',
          Value: 'workops-dev-vpc',
        },
      ]),
    });
    template.resourceCountIs('AWS::EC2::Subnet', 6);
    template.resourceCountIs('AWS::EC2::NatGateway', 0);
    template.resourceCountIs('AWS::EC2::SecurityGroup', 3);
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-alb-sg',
    });
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-app-sg',
    });
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-db-sg',
    });
    template.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    template.hasResourceProperties('AWS::ECS::Cluster', {
      ClusterName: 'workops-dev-cluster',
    });
    template.hasOutput('vpcId', {});
    template.hasOutput('publicSubnetIds', {});
    template.hasOutput('appSubnetIds', {});
    template.hasOutput('dbSubnetIds', {});
    template.hasOutput('ecsClusterName', {});
    template.hasOutput('albSecurityGroupId', {});
    template.hasOutput('appSecurityGroupId', {});
    template.hasOutput('dbSecurityGroupId', {});
  });

  test('applies common WorkOps tags', () => {
    const app = new App();
    const stage = 'dev';

    // CDK app tags mirror the entrypoint's local and CI deploy behavior.
    Tags.of(app).add('Project', 'WorkOps');
    Tags.of(app).add('Environment', stage);
    Tags.of(app).add('ManagedBy', 'CDK');

    const stack = new TaggedResourceStack(app, 'TaggedResourceStack');
    const template = Template.fromStack(stack);

    template.hasResourceProperties('AWS::SNS::Topic', {
      Tags: Match.arrayWith([
        {
          Key: 'Project',
          Value: 'WorkOps',
        },
      ]),
    });
    template.hasResourceProperties('AWS::SNS::Topic', {
      Tags: Match.arrayWith([
        {
          Key: 'Environment',
          Value: 'dev',
        },
      ]),
    });
    template.hasResourceProperties('AWS::SNS::Topic', {
      Tags: Match.arrayWith([
        {
          Key: 'ManagedBy',
          Value: 'CDK',
        },
      ]),
    });
  });

  test('creates the RegistryStack repository and lifecycle policy', () => {
    const app = new App();
    const stage = 'dev';
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      stage,
      stackName: `workops-${stage}-registry`,
    });
    const template = Template.fromStack(registryStack);

    template.resourceCountIs('AWS::ECR::Repository', 1);
    template.hasResourceProperties('AWS::ECR::Repository', {
      RepositoryName: 'workops-dev-web',
      EmptyOnDelete: true,
      LifecyclePolicy: {
        LifecyclePolicyText: Match.serializedJson(
          Match.objectLike({
            rules: Match.arrayWith([
              Match.objectLike({
                selection: Match.objectLike({
                  tagStatus: 'tagged',
                  tagPatternList: ['*'],
                  countType: 'imageCountMoreThan',
                  countNumber: 2,
                }),
              }),
              Match.objectLike({
                selection: Match.objectLike({
                  tagStatus: 'untagged',
                  countType: 'sinceImagePushed',
                  countUnit: 'days',
                  countNumber: 1,
                }),
              }),
            ]),
          }),
        ),
      },
    });
    template.hasResource('AWS::ECR::Repository', {
      Properties: {
        RepositoryName: 'workops-dev-web',
        EmptyOnDelete: true,
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasOutput('repositoryName', {});
    template.hasOutput('repositoryUri', {});
  });

  test('creates the LogsStack log groups', () => {
    const app = new App();
    const stage = 'dev';
    const logsStack = new LogsStack(app, 'LogsStack', {
      stage,
      stackName: `workops-${stage}-logs`,
    });
    const template = Template.fromStack(logsStack);

    template.resourceCountIs('AWS::Logs::LogGroup', 2);
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/web',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/migration',
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
    template.hasOutput('webLogGroupName', {});
    template.hasOutput('migrationLogGroupName', {});
  });

  test('creates the P2-3 EgressStack NAT route for app subnets', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const egressStack = new EgressStack(app, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      publicSubnets: foundationStack.publicSubnets,
      stage,
      stackName: `workops-${stage}-egress`,
      vpc: foundationStack.vpc,
    });
    const template = Template.fromStack(egressStack);

    template.resourceCountIs('AWS::EC2::EIP', 1);
    template.resourceCountIs('AWS::EC2::NatGateway', 1);
    template.hasResourceProperties('AWS::EC2::NatGateway', {
      AllocationId: {
        'Fn::GetAtt': [
          Match.stringLikeRegexp('NatGatewayEip'),
          'AllocationId',
        ],
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

  test('creates the P2-4 EdgeStack CloudFront HTTPS entrypoint', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const edgeStack = new EdgeStack(app, 'EdgeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cognitoUserPoolClientId: testCognitoUserPoolClientId,
      cognitoUserPoolId: testCognitoUserPoolId,
      stage,
      stackName: `workops-${stage}-edge`,
      vpc: foundationStack.vpc,
    });
    const template = Template.fromStack(edgeStack);
    const templateText = JSON.stringify(template.toJSON());

    template.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    template.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
      Name: 'workops-dev-web-alb',
      Scheme: 'internal',
      Type: 'application',
    });
    template.hasResourceProperties('AWS::ElasticLoadBalancingV2::Listener', {
      Port: 80,
      Protocol: 'HTTP',
    });
    template.hasResourceProperties('AWS::ElasticLoadBalancingV2::TargetGroup', {
      HealthCheckIntervalSeconds: 30,
      HealthCheckPath: '/actuator/health',
      HealthCheckTimeoutSeconds: 5,
      HealthyThresholdCount: 2,
      Matcher: {
        HttpCode: '200',
      },
      Port: 8080,
      Protocol: 'HTTP',
      TargetGroupAttributes: Match.arrayWith([
        {
          Key: 'deregistration_delay.timeout_seconds',
          Value: '30',
        },
      ]),
      Name: 'workops-dev-web-tg',
      TargetType: 'ip',
      UnhealthyThresholdCount: 3,
    });
    template.hasResourceProperties('AWS::CloudFront::Distribution', {
      DistributionConfig: Match.objectLike({
        Comment: 'workops-dev-web-edge',
        DefaultCacheBehavior: Match.objectLike({
          AllowedMethods: [
            'GET',
            'HEAD',
            'OPTIONS',
            'PUT',
            'PATCH',
            'POST',
            'DELETE',
          ],
          CachedMethods: [
            'GET',
            'HEAD',
          ],
          CachePolicyId: '4135ea2d-6df8-44a3-9df3-4b5a84be39ad',
          OriginRequestPolicyId: 'b689b0a8-53d0-40ab-baf2-68738e2966ac',
          TargetOriginId: Match.anyValue(),
          ViewerProtocolPolicy: 'redirect-to-https',
        }),
        Origins: Match.arrayWith([
          Match.objectLike({
            VpcOriginConfig: Match.objectLike({
              VpcOriginId: {
                'Fn::GetAtt': [Match.stringLikeRegexp('WebDistributionOrigin1VpcOrigin'), 'Id'],
              },
            }),
          }),
        ]),
        PriceClass: 'PriceClass_200',
      }),
    });
    template.hasResourceProperties('AWS::CloudFront::VpcOrigin', {
      VpcOriginEndpointConfig: Match.objectLike({
        Arn: {
          Ref: Match.stringLikeRegexp('WebAlb'),
        },
        OriginProtocolPolicy: 'http-only',
        OriginSSLProtocols: ['TLSv1.2'],
      }),
    });
    template.hasOutput('albDnsName', {});
    template.hasOutput('cloudFrontDomainName', {});
    template.hasOutput('cloudFrontHttpsUrl', {});
    template.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'workops-dev-cognito-client-url-updater',
      Handler: 'index.handler',
      Runtime: 'nodejs22.x',
      Timeout: 60,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/custom-resources/cognito-client-url-updater',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/custom-resources/cognito-client-url-updater-provider',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: [
              'cognito-idp:DescribeUserPoolClient',
              'cognito-idp:UpdateUserPoolClient',
            ],
            Effect: 'Allow',
          }),
        ]),
      },
    });
    template.resourceCountIs('Custom::WorkOpsCognitoClientUrlUpdater', 1);
    template.hasResourceProperties('Custom::WorkOpsCognitoClientUrlUpdater', {
      UserPoolId: testCognitoUserPoolId,
      UserPoolClientId: testCognitoUserPoolClientId,
      CloudFrontDomainName: Match.anyValue(),
    });
    expect(templateText).not.toContain('"CidrIp":"0.0.0.0/0"');
    expect(templateText).not.toContain('authenticate-cognito');
    expect(templateText).not.toContain('pl-58a04531');
    template.resourceCountIs('AWS::WAFv2::WebACLAssociation', 0);
  });

  test('creates the P2-5 IdentityStack Cognito Hosted UI resources', () => {
    const app = new App();
    const stage = 'dev';
    const identityStack = new IdentityStack(app, 'IdentityStack', {
      stage,
      stackName: `workops-${stage}-identity`,
    });
    const template = Template.fromStack(identityStack);
    const templateText = JSON.stringify(template.toJSON());

    template.resourceCountIs('AWS::Cognito::UserPool', 1);
    template.resourceCountIs('AWS::Cognito::UserPoolDomain', 1);
    template.resourceCountIs('AWS::Cognito::UserPoolClient', 1);
    template.hasResourceProperties('AWS::Cognito::UserPool', {
      UserPoolName: 'workops-dev-user-pool',
      AutoVerifiedAttributes: ['email'],
      MfaConfiguration: 'OFF',
      UsernameConfiguration: {
        CaseSensitive: false,
      },
      AdminCreateUserConfig: {
        AllowAdminCreateUserOnly: true,
      },
      AccountRecoverySetting: {
        RecoveryMechanisms: [
          {
            Name: 'verified_email',
            Priority: 1,
          },
        ],
      },
      Policies: {
        PasswordPolicy: Match.objectLike({
          MinimumLength: 8,
          RequireLowercase: true,
          RequireUppercase: true,
          RequireNumbers: true,
          RequireSymbols: false,
        }),
      },
      Schema: Match.arrayWith([
        Match.objectLike({
          Name: 'email',
          Required: true,
          Mutable: true,
        }),
      ]),
    });
    template.hasResource('AWS::Cognito::UserPool', {
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResourceProperties('AWS::Cognito::UserPoolDomain', {
      Domain: {
        'Fn::Join': [
          '',
          [
            'workops-dev-',
            {
              Ref: 'AWS::AccountId',
            },
          ],
        ],
      },
    });
    template.hasResourceProperties('AWS::Cognito::UserPoolClient', {
      ClientName: 'workops-dev-web-client',
      GenerateSecret: false,
      AllowedOAuthFlowsUserPoolClient: true,
      AllowedOAuthFlows: ['code'],
      AllowedOAuthScopes: [
        'openid',
        'email',
      ],
      SupportedIdentityProviders: ['COGNITO'],
    });
    template.hasResource('AWS::Cognito::UserPoolClient', {
      Properties: Match.objectLike({
        CallbackURLs: Match.absent(),
        LogoutURLs: Match.absent(),
        DefaultRedirectURI: Match.absent(),
      }),
    });
    template.hasOutput('userPoolId', {});
    template.hasOutput('userPoolClientId', {});
    template.hasOutput('hostedUiDomainBaseUrl', {});
    template.resourceCountIs('AWS::CloudFront::Distribution', 0);
    template.resourceCountIs('AWS::ElasticLoadBalancingV2::LoadBalancer', 0);
    template.resourceCountIs('AWS::ECS::Service', 0);
    template.resourceCountIs('AWS::EC2::NatGateway', 0);
    expect(templateText).not.toContain('EdgeStack');
    expect(templateText).not.toContain('WebDistribution');
    expect(templateText).not.toContain('WebAlb');
  });

  test('creates the P2-3 AppRuntimeStack web service', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      stage,
      stackName: `workops-${stage}-registry`,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      stage,
      stackName: `workops-${stage}-logs`,
    });
    const edgeStack = new EdgeStack(app, 'EdgeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cognitoUserPoolClientId: testCognitoUserPoolClientId,
      cognitoUserPoolId: testCognitoUserPoolId,
      stage,
      stackName: `workops-${stage}-edge`,
      vpc: foundationStack.vpc,
    });
    const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cluster: foundationStack.ecsCluster,
      repository: registryStack.repository,
      stage,
      stackName: `workops-${stage}-app-runtime`,
      targetGroup: edgeStack.targetGroup,
      webLogGroup: logsStack.webLogGroup,
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
        CpuArchitecture: 'X86_64',
        OperatingSystemFamily: 'LINUX',
      },
      ContainerDefinitions: Match.arrayWith([
        Match.objectLike({
          Name: 'web',
          Environment: Match.arrayWith([
            {
              Name: 'SPRING_PROFILES_ACTIVE',
              Value: 'local',
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
    expect(templateText).toContain('p2-3-manual');
    template.hasResourceProperties('AWS::ECS::Service', {
      DesiredCount: 1,
      DeploymentConfiguration: Match.objectLike({
        DeploymentCircuitBreaker: {
          Enable: true,
          Rollback: true,
        },
        MaximumPercent: 200,
        MinimumHealthyPercent: 100,
      }),
      HealthCheckGracePeriodSeconds: 90,
      LaunchType: 'FARGATE',
      LoadBalancers: Match.arrayWith([
        {
          ContainerName: 'web',
          ContainerPort: 8080,
          TargetGroupArn: Match.anyValue(),
        },
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
    expect(templateText).toContain('WebLogGroup');
    expect(templateText).not.toContain('/workops/dev/migration');
  });

  test('creates the SecretStack without secret resources in P2-2-01', () => {
    const app = new App();
    const stage = 'dev';
    const secretStack = new SecretStack(app, 'SecretStack', {
      stackName: `workops-${stage}-secret`,
    });
    const template = Template.fromStack(secretStack);

    template.resourceCountIs('AWS::SecretsManager::Secret', 0);
  });

  test('creates the DataStack database resources', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const dataStack = new DataStack(app, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      stage,
      stackName: `workops-${stage}-data`,
      vpc: foundationStack.vpc,
    });
    const dataTemplate = Template.fromStack(dataStack);
    const foundationTemplate = Template.fromStack(foundationStack);
    const dataTemplateText = JSON.stringify(dataTemplate.toJSON());

    dataTemplate.resourceCountIs('AWS::RDS::DBInstance', 1);
    dataTemplate.resourceCountIs('AWS::EC2::Instance', 0);
    dataTemplate.resourceCountIs('AWS::IAM::Role', 0);
    dataTemplate.resourceCountIs('AWS::IAM::InstanceProfile', 0);
    dataTemplate.resourceCountIs('AWS::IAM::Policy', 0);
    dataTemplate.hasResourceProperties('AWS::RDS::DBSubnetGroup', {
      DBSubnetGroupName: 'workops-dev-db-subnet-group',
    });
    dataTemplate.hasResourceProperties('AWS::RDS::DBInstance', {
      AllocatedStorage: '20',
      BackupRetentionPeriod: 1,
      DBInstanceClass: 'db.t4g.micro',
      DBInstanceIdentifier: 'workops-dev-db',
      DBName: 'workops',
      DeletionProtection: false,
      Engine: 'mysql',
      EngineVersion: '8.4.9',
      MultiAZ: false,
      PubliclyAccessible: false,
      StorageEncrypted: true,
      StorageType: 'gp2',
      VPCSecurityGroups: Match.arrayWith([
        {
          'Fn::GetAtt': [
            Match.stringLikeRegexp('RdsConsoleCloudShellSecurityGroup'),
            'GroupId',
          ],
        },
      ]),
    });
    dataTemplate.hasResource('AWS::RDS::DBInstance', {
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    dataTemplate.hasResourceProperties('AWS::SecretsManager::Secret', {
      Name: '/workops/dev/db/master',
      GenerateSecretString: Match.objectLike({
        GenerateStringKey: 'password',
        SecretStringTemplate: '{"username":"workops_admin"}',
      }),
    });
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupDescription: 'WorkOps RDS Console CloudShell VPC security group',
      GroupName: 'workops-dev-rds-console-cloudshell-sg',
      SecurityGroupIngress: Match.absent(),
    });
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroupEgress', {
      Description: 'Allow RDS Console CloudShell VPC environment to reach MySQL',
      DestinationSecurityGroupId: Match.anyValue(),
      FromPort: 3306,
      IpProtocol: 'tcp',
      ToPort: 3306,
    });
    foundationTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      FromPort: 3306,
      IpProtocol: 'tcp',
      ToPort: 3306,
    });
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow RDS Console CloudShell VPC environment to reach MySQL',
      FromPort: 3306,
      IpProtocol: 'tcp',
      SourceSecurityGroupId: Match.anyValue(),
      ToPort: 3306,
    });
    dataTemplate.resourceCountIs('AWS::SSM::Parameter', 3);
    dataTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/name',
      Type: 'String',
      Value: 'workops',
    });
    dataTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/port',
      Type: 'String',
      Value: '3306',
    });
    dataTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/url',
      Type: 'String',
      Value: Match.objectLike({
        'Fn::Join': Match.arrayWith([
          '',
          Match.arrayWith([
            'jdbc:mysql://',
            {
              'Fn::GetAtt': [
                Match.stringLikeRegexp('Database'),
                'Endpoint.Address',
              ],
            },
            ':3306/workops?useSSL=true&serverTimezone=Asia/Tokyo',
          ]),
        ]),
      }),
    });
    expect(dataTemplateText).toContain('workops-dev-rds-console-cloudshell-sg');
    expect(dataTemplateText).not.toContain('al2023-ami');
    expect(dataTemplateText).not.toContain('AmazonSSMManagedInstanceCore');
    expect(dataTemplateText).not.toContain('AssociatePublicIpAddress');
    expect(dataTemplateText).not.toContain('HttpTokens');
    expect(dataTemplateText).not.toContain('DbAccessHost');
    expect(dataTemplateText).not.toContain('secretsmanager:GetSecretValue');
    expect(dataTemplateText).not.toContain('rds-db:connect');
    expect(dataTemplateText).not.toContain('ssm:GetParameter');
    dataTemplate.hasOutput('rdsInstanceIdentifier', {});
    dataTemplate.hasOutput('rdsEndpointAddress', {});
    dataTemplate.hasOutput('rdsPort', {});
    dataTemplate.hasOutput('databaseName', {});
    dataTemplate.hasOutput('dbSubnetGroupName', {});
    dataTemplate.hasOutput('rdsMasterSecretArn', {});
    dataTemplate.hasOutput('rdsConsoleCloudShellSecurityGroupId', {});
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('PublicIp');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('password');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('secretValue');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('dbAccessHostInstanceId');
  });

  test('creates the ConfigStack non-secret parameters in P2-2-01', () => {
    const app = new App();
    const stage = 'dev';
    const configStack = new ConfigStack(app, 'ConfigStack', {
      stage,
      stackName: `workops-${stage}-config`,
    });
    const template = Template.fromStack(configStack);

    template.resourceCountIs('AWS::SSM::Parameter', 1);
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/spring/profile',
      Type: 'String',
      Value: 'dev',
    });
    const templateText = JSON.stringify(template.toJSON());
    expect(templateText).not.toContain('/workops/dev/db/name');
    expect(templateText).not.toContain('/workops/dev/db/port');
    expect(templateText).not.toContain('/workops/dev/db/url');
    expect(templateText).not.toContain('Fn::GetStackOutput');
  });

  test('keeps npm scripts minimal and independent from dotenv', () => {
    const packageJsonPath = join(__dirname, '..', 'package.json');
    const entrypointPath = join(__dirname, '..', 'bin', 'cdk.ts');
    const packageJsonText = readFileSync(packageJsonPath, 'utf8');
    const entrypointText = readFileSync(entrypointPath, 'utf8');

    expect(packageJsonText).toContain('"build": "tsc"');
    expect(packageJsonText).toContain('"watch": "tsc -w"');
    expect(packageJsonText).toContain('"test": "jest"');
    expect(packageJsonText).toContain('"cdk": "cdk"');
    expect(packageJsonText).not.toContain('synth:dev');
    expect(packageJsonText).not.toContain('diff:dev');
    expect(packageJsonText).not.toContain('deploy:dev');
    expect(packageJsonText).not.toContain('dotenv');
    expect(entrypointText).toContain('WORKOPS_STAGE');
    expect(entrypointText).not.toContain('tryGetContext');
    expect(entrypointText).not.toContain('dotenv');
    expect(entrypointText).not.toContain('.env.local');
  });
});

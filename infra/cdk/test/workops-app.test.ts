import { App, Stack, Tags } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { readFileSync } from 'fs';
import { join } from 'path';
import { Construct } from 'constructs';
import { AppRuntimeStack } from '../lib/app-runtime-stack';
import { ConfigStack } from '../lib/config-stack';
import { DataStack } from '../lib/data-stack';
import { DeployStack } from '../lib/deploy-stack';
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
const testCognitoPlatformUserPoolClientId = 'platformclientid';
const testCognitoTenantUserPoolClientId = 'tenantclientid';
const testCognitoHostedUiDomainBaseUrl =
  'https://workops-dev.auth.ap-northeast-1.amazoncognito.com';
const testGitHubRepository = 'owner/repo';
const testWebImageTag = 'test-sha';
const testEnv = {
  account: '123456789012',
  region: 'ap-northeast-1',
};

describe('WorkOps CDK app', () => {
  test('creates Phase 2 base stack shells using the requested stage', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const deployStack = new DeployStack(app, 'DeployStack', {
      env: testEnv,
      githubRepository: testGitHubRepository,
      stage,
      stackName: `workops-${stage}-deploy`,
    });
    const secretStack = new SecretStack(app, 'SecretStack', {
      env: testEnv,
      stackName: `workops-${stage}-secret`,
    });
    const dataStack = new DataStack(app, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      env: testEnv,
      stage,
      stackName: `workops-${stage}-data`,
      vpc: foundationStack.vpc,
    });
    const configStack = new ConfigStack(app, 'ConfigStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-config`,
    });
    const identityStack = new IdentityStack(app, 'IdentityStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-identity`,
    });
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-registry`,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-logs`,
    });
    const egressStack = new EgressStack(app, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      env: testEnv,
      publicSubnets: foundationStack.publicSubnets,
      stage,
      stackName: `workops-${stage}-egress`,
      vpc: foundationStack.vpc,
    });
    const edgeStack = new EdgeStack(app, 'EdgeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cognitoPlatformUserPoolClientId: testCognitoPlatformUserPoolClientId,
      cognitoTenantUserPoolClientId: testCognitoTenantUserPoolClientId,
      cognitoUserPoolId: testCognitoUserPoolId,
      cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
      cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
      env: testEnv,
      stage,
      stackName: `workops-${stage}-edge`,
      vpc: foundationStack.vpc,
    });
    const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cloudFrontHttpsUrl: edgeStack.cloudFrontHttpsUrl,
      cluster: foundationStack.ecsCluster,
      cognitoHostedUiDomainBaseUrl: identityStack.hostedUiDomainBaseUrl,
      cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
      cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
      cognitoUserPoolId: identityStack.userPoolId,
      env: testEnv,
      repository: registryStack.repository,
      stage,
      stackName: `workops-${stage}-app-runtime`,
      targetGroup: edgeStack.targetGroup,
      webImageTag: testWebImageTag,
      webLogGroup: logsStack.webLogGroup,
    });

    expect(foundationStack.stackName).toBe('workops-dev-foundation');
    expect(deployStack.stackName).toBe('workops-dev-deploy');
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

  test('creates the P2-9 DeployStack GitHub Actions OIDC role', () => {
    const app = new App();
    const stage = 'dev';
    const deployStack = new DeployStack(app, 'DeployStack', {
      env: testEnv,
      githubRepository: testGitHubRepository,
      stage,
      stackName: `workops-${stage}-deploy`,
    });
    const template = Template.fromStack(deployStack);
    const templateText = JSON.stringify(template.toJSON());

    template.resourceCountIs('Custom::AWSCDKOpenIdConnectProvider', 1);
    template.hasResourceProperties('Custom::AWSCDKOpenIdConnectProvider', {
      Url: 'https://token.actions.githubusercontent.com',
      ClientIDList: ['sts.amazonaws.com'],
    });
    template.hasResourceProperties('AWS::IAM::Role', {
      RoleName: 'workops-dev-github-actions-deploy',
      AssumeRolePolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'sts:AssumeRoleWithWebIdentity',
            Effect: 'Allow',
            Condition: {
              StringEquals: {
                'token.actions.githubusercontent.com:aud': 'sts.amazonaws.com',
                'token.actions.githubusercontent.com:sub': 'repo:owner/repo:environment:dev',
              },
            },
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: Match.arrayWith(['cloudformation:*', 'iam:*', 'sts:AssumeRole']),
            Effect: 'Allow',
            Resource: '*',
          }),
        ]),
      },
    });
    template.hasOutput('githubActionsDeployRoleArn', {});
    expect(templateText).not.toContain('AdministratorAccess');
  });

  test('creates the FoundationStack network and cluster resources', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
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

    template.resourceCountIs('AWS::Logs::LogGroup', 4);
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/web',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/migration',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/custom-resources/cognito-client-url-updater',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/custom-resources/cognito-client-url-updater-provider',
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
        LogGroupName: '/workops/dev/custom-resources/cognito-client-url-updater',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/custom-resources/cognito-client-url-updater-provider',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasOutput('webLogGroupName', {});
    template.hasOutput('migrationLogGroupName', {});
    template.hasOutput('cognitoClientUrlUpdaterLogGroupName', {});
    template.hasOutput('cognitoClientUrlUpdaterProviderLogGroupName', {});
  });

  test('creates the P2-3 EgressStack NAT route for app subnets', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const egressStack = new EgressStack(app, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      env: testEnv,
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

  test('creates the P2-4 EdgeStack CloudFront HTTPS entrypoint', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-logs`,
    });
    const edgeStack = new EdgeStack(app, 'EdgeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cognitoPlatformUserPoolClientId: testCognitoPlatformUserPoolClientId,
      cognitoTenantUserPoolClientId: testCognitoTenantUserPoolClientId,
      cognitoUserPoolId: testCognitoUserPoolId,
      cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
      cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
      env: testEnv,
      stage,
      stackName: `workops-${stage}-edge`,
      vpc: foundationStack.vpc,
    });
    const template = Template.fromStack(edgeStack);
    const foundationTemplate = Template.fromStack(foundationStack);
    const templateText = JSON.stringify(template.toJSON());
    const edgeStackSource = readFileSync(join(__dirname, '..', 'lib', 'edge-stack.ts'), 'utf8');

    template.resourceCountIs('AWS::EC2::SecurityGroupIngress', 1);
    foundationTemplate.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow CloudFront VPC origin to reach WorkOps ALB',
      FromPort: 80,
      GroupId: Match.anyValue(),
      IpProtocol: 'tcp',
      SourcePrefixListId: Match.stringLikeRegexp('^pl-'),
      ToPort: 80,
    });
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
          AllowedMethods: ['GET', 'HEAD', 'OPTIONS', 'PUT', 'PATCH', 'POST', 'DELETE'],
          CachedMethods: ['GET', 'HEAD'],
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
    template.hasResourceProperties('AWS::Lambda::Function', {
      LoggingConfig: {
        LogGroup: Match.anyValue(),
      },
    });
    template.resourceCountIs('AWS::Logs::LogGroup', 0);
    expect(templateText).toContain('CognitoClientUrlUpdaterLogGroup');
    expect(templateText).toContain('CognitoClientUrlUpdaterProviderLogGroup');
    expect(templateText).not.toContain('/workops/dev/custom-resources/cognito-client-url-updater');
    expect(templateText).not.toContain(
      '/workops/dev/custom-resources/cognito-client-url-updater-provider',
    );
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: ['cognito-idp:DescribeUserPoolClient', 'cognito-idp:UpdateUserPoolClient'],
            Effect: 'Allow',
          }),
        ]),
      },
    });
    template.resourceCountIs('Custom::WorkOpsCognitoClientUrlUpdater', 1);
    template.hasResourceProperties('Custom::WorkOpsCognitoClientUrlUpdater', {
      UserPoolId: testCognitoUserPoolId,
      PlatformClientId: testCognitoPlatformUserPoolClientId,
      TenantClientId: testCognitoTenantUserPoolClientId,
      CloudFrontDomainName: Match.anyValue(),
    });
    expect(templateText).not.toContain('"CidrIp":"0.0.0.0/0"');
    expect(templateText).not.toContain('authenticate-cognito');
    expect(edgeStackSource).toContain('com.amazonaws.global.cloudfront.origin-facing');
    expect(edgeStackSource).not.toContain('pl-58a04531');
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
    template.resourceCountIs('AWS::Cognito::UserPoolClient', 2);
    template.resourceCountIs('AWS::Cognito::ManagedLoginBranding', 2);
    template.hasResourceProperties('AWS::Cognito::UserPool', {
      UserPoolName: 'workops-dev-user-pool',
      UserPoolTier: 'ESSENTIALS',
      AutoVerifiedAttributes: ['email'],
      MfaConfiguration: 'OFF',
      UsernameConfiguration: {
        CaseSensitive: false,
      },
      AdminCreateUserConfig: {
        AllowAdminCreateUserOnly: true,
        InviteMessageTemplate: {
          EmailSubject: 'WorkOps アカウント作成のお知らせ',
          EmailMessage: Match.anyValue(),
        },
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
      ManagedLoginVersion: 2,
    });
    template.hasResourceProperties('AWS::Cognito::UserPoolClient', {
      ClientName: 'workops-dev-platform-client',
      GenerateSecret: false,
      AllowedOAuthFlowsUserPoolClient: true,
      AllowedOAuthFlows: ['code'],
      AllowedOAuthScopes: ['openid', 'email'],
      SupportedIdentityProviders: ['COGNITO'],
    });
    template.hasResourceProperties('AWS::Cognito::UserPoolClient', {
      ClientName: 'workops-dev-tenant-client',
      GenerateSecret: false,
      AllowedOAuthFlowsUserPoolClient: true,
      AllowedOAuthFlows: ['code'],
      AllowedOAuthScopes: ['openid', 'email'],
      SupportedIdentityProviders: ['COGNITO'],
    });
    template.hasResourceProperties('AWS::Cognito::ManagedLoginBranding', {
      UserPoolId: Match.anyValue(),
      ClientId: Match.anyValue(),
      ReturnMergedResources: false,
      UseCognitoProvidedValues: false,
      Settings: Match.objectLike({
        components: Match.objectLike({
          pageBackground: Match.anyValue(),
          primaryButton: Match.anyValue(),
        }),
      }),
    });
    template.hasResource('AWS::Cognito::UserPoolClient', {
      Properties: Match.objectLike({
        CallbackURLs: ['https://workops-dev-placeholder.invalid/login/oauth2/code/platform'],
        LogoutURLs: ['https://workops-dev-placeholder.invalid/login'],
        DefaultRedirectURI: 'https://workops-dev-placeholder.invalid/login/oauth2/code/platform',
      }),
    });
    template.hasResource('AWS::Cognito::UserPoolClient', {
      Properties: Match.objectLike({
        CallbackURLs: ['https://workops-dev-placeholder.invalid/login/oauth2/code/tenant'],
        LogoutURLs: ['https://workops-dev-placeholder.invalid/login'],
        DefaultRedirectURI: 'https://workops-dev-placeholder.invalid/login/oauth2/code/tenant',
      }),
    });
    const brandingTemplateText = JSON.stringify(
      template.findResources('AWS::Cognito::ManagedLoginBranding'),
    );
    expect(templateText).toContain('WorkOps アカウント作成のお知らせ');
    expect(templateText).toContain('WorkOps アカウントを作成しました。');
    expect(templateText).toContain('{username}');
    expect(templateText).toContain('{####}');
    expect(brandingTemplateText).not.toContain('"Assets"');
    expect(brandingTemplateText).toContain('5f1b1bff');
    expect(brandingTemplateText).toContain('0972d3ff');
    template.hasOutput('userPoolId', {});
    template.hasOutput('platformUserPoolClientId', {});
    template.hasOutput('tenantUserPoolClientId', {});
    template.hasOutput('hostedUiDomainBaseUrl', {});
    template.resourceCountIs('AWS::CloudFront::Distribution', 0);
    template.resourceCountIs('AWS::ElasticLoadBalancingV2::LoadBalancer', 0);
    template.resourceCountIs('AWS::ECS::Service', 0);
    template.resourceCountIs('AWS::EC2::NatGateway', 0);
    expect(templateText).not.toContain('EdgeStack');
    expect(templateText).not.toContain('WebDistribution');
    expect(templateText).not.toContain('WebAlb');
    expect(templateText).not.toContain('localhost');
    expect(templateText).not.toContain('workops-dev-web-client');
    expect(templateText).not.toContain('/login/oauth2/code/cognito');
  });

  test('creates the P2-3 AppRuntimeStack web service', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-registry`,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-logs`,
    });
    const edgeStack = new EdgeStack(app, 'EdgeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cognitoPlatformUserPoolClientId: testCognitoPlatformUserPoolClientId,
      cognitoTenantUserPoolClientId: testCognitoTenantUserPoolClientId,
      cognitoUserPoolId: testCognitoUserPoolId,
      cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
      cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
      env: testEnv,
      stage,
      stackName: `workops-${stage}-edge`,
      vpc: foundationStack.vpc,
    });
    const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cloudFrontHttpsUrl: edgeStack.cloudFrontHttpsUrl,
      cluster: foundationStack.ecsCluster,
      cognitoHostedUiDomainBaseUrl: testCognitoHostedUiDomainBaseUrl,
      cognitoPlatformUserPoolClientId: testCognitoPlatformUserPoolClientId,
      cognitoTenantUserPoolClientId: testCognitoTenantUserPoolClientId,
      cognitoUserPoolId: testCognitoUserPoolId,
      env: testEnv,
      repository: registryStack.repository,
      stage,
      stackName: `workops-${stage}-app-runtime`,
      targetGroup: edgeStack.targetGroup,
      webImageTag: testWebImageTag,
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
              Name: 'AWS_REGION',
              Value: Match.anyValue(),
            },
            {
              Name: 'WORKOPS_COGNITO_USER_POOL_ID',
              Value: testCognitoUserPoolId,
            },
            {
              Name: 'WORKOPS_COGNITO_PLATFORM_CLIENT_ID',
              Value: testCognitoPlatformUserPoolClientId,
            },
            {
              Name: 'WORKOPS_COGNITO_TENANT_CLIENT_ID',
              Value: testCognitoTenantUserPoolClientId,
            },
            {
              Name: 'WORKOPS_COGNITO_HOSTED_UI_DOMAIN_BASE_URL',
              Value: testCognitoHostedUiDomainBaseUrl,
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
    expect(templateText).toContain(testWebImageTag);
    expect(templateText).not.toContain('p2-3-manual');
    expect(templateText).toContain(testCognitoHostedUiDomainBaseUrl);
    expect(templateText).toContain('/login');
    expect(templateText).toContain('/login/oauth2/code/platform');
    expect(templateText).toContain('/login/oauth2/code/tenant');
    expect(templateText).not.toContain('WORKOPS_COGNITO_CLIENT_ID');
    expect(templateText).not.toContain('WORKOPS_COGNITO_REDIRECT_URI');
    expect(templateText).not.toContain('/login/oauth2/code/cognito');
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
    expect(templateText).toContain(`userpool/${testCognitoUserPoolId}`);
    expect(templateText).not.toContain('cognito-idp:AdminDeleteUser');
    expect(templateText).not.toContain('cognito-idp:AdminGetUser');
    expect(templateText).not.toContain('cognito-idp:AdminUpdateUserAttributes');
    expect(templateText).not.toContain('cognito-idp:AdminDisableUser');
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
          'Fn::GetAtt': [Match.stringLikeRegexp('RdsConsoleCloudShellSecurityGroup'), 'GroupId'],
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
    foundationTemplate.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow WorkOps app tasks to reach MySQL',
      FromPort: 3306,
      IpProtocol: 'tcp',
      SourceSecurityGroupId: Match.anyValue(),
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
              'Fn::GetAtt': [Match.stringLikeRegexp('Database'), 'Endpoint.Address'],
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

  test('keeps CDK entrypoints scoped and independent from dotenv', () => {
    const packageJsonPath = join(__dirname, '..', 'package.json');
    const cdkJsonPath = join(__dirname, '..', 'cdk.json');
    const deployEntrypointPath = join(__dirname, '..', 'bin', 'cdk-deploy.ts');
    const infraEntrypointPath = join(__dirname, '..', 'bin', 'cdk-infra.ts');
    const runtimeEntrypointPath = join(__dirname, '..', 'bin', 'cdk-runtime.ts');
    const packageJsonText = readFileSync(packageJsonPath, 'utf8');
    const cdkJsonText = readFileSync(cdkJsonPath, 'utf8');
    const deployEntrypointText = readFileSync(deployEntrypointPath, 'utf8');
    const infraEntrypointText = readFileSync(infraEntrypointPath, 'utf8');
    const runtimeEntrypointText = readFileSync(runtimeEntrypointPath, 'utf8');
    const allEntrypointText = `${deployEntrypointText}\n${infraEntrypointText}\n${runtimeEntrypointText}`;

    expect(packageJsonText).toContain('"build": "tsc"');
    expect(packageJsonText).toContain('"watch": "tsc -w"');
    expect(packageJsonText).toContain('"test": "jest"');
    expect(packageJsonText).toContain('"cdk:deploy-app": "cdk --app');
    expect(packageJsonText).toContain('"cdk:infra": "cdk --app');
    expect(packageJsonText).toContain('"cdk:runtime": "cdk --app');
    expect(packageJsonText).not.toContain('"bin"');
    expect(packageJsonText).not.toContain('"cdk": "cdk"');
    expect(cdkJsonText).not.toContain('"app"');
    expect(deployEntrypointText).toContain('DeployStack');
    expect(deployEntrypointText).toContain('GITHUB_REPOSITORY');
    expect(deployEntrypointText).not.toContain('AppRuntimeStack');
    expect(infraEntrypointText).toContain('FoundationStack');
    expect(infraEntrypointText).toContain('LogsStack');
    expect(infraEntrypointText).not.toContain('DataStack');
    expect(infraEntrypointText).not.toContain('EgressStack');
    expect(infraEntrypointText).not.toContain('EdgeStack');
    expect(infraEntrypointText).not.toContain('AppRuntimeStack');
    expect(infraEntrypointText).not.toContain('WORKOPS_WEB_IMAGE_TAG');
    expect(runtimeEntrypointText).toContain('WORKOPS_WEB_IMAGE_TAG');
    expect(runtimeEntrypointText).toContain('AppRuntimeStack is not defined');
    expect(runtimeEntrypointText).toContain('edgeStack.addDependency(logsStack)');
    expect(packageJsonText).not.toContain('synth:dev');
    expect(packageJsonText).not.toContain('diff:dev');
    expect(packageJsonText).not.toContain('deploy:dev');
    expect(packageJsonText).not.toContain('dotenv');
    expect(allEntrypointText).toContain('WORKOPS_STAGE');
    expect(allEntrypointText).not.toContain('tryGetContext');
    expect(allEntrypointText).not.toContain('dotenv');
    expect(allEntrypointText).not.toContain('.env.local');
  });

  test('defines the P2-9 GitHub Actions CI and infra deploy workflows', () => {
    const ciWorkflowPath = join(__dirname, '..', '..', '..', '.github', 'workflows', 'ci.yml');
    const infraWorkflowPath = join(
      __dirname,
      '..',
      '..',
      '..',
      '.github',
      'workflows',
      'infra-dev.yml',
    );
    const appWorkflowPath = join(
      __dirname,
      '..',
      '..',
      '..',
      '.github',
      'workflows',
      'app-deploy-dev.yml',
    );
    const ciWorkflowText = readFileSync(ciWorkflowPath, 'utf8');
    const infraWorkflowText = readFileSync(infraWorkflowPath, 'utf8');
    const appWorkflowText = readFileSync(appWorkflowPath, 'utf8');

    expect(ciWorkflowText).toContain('name: CI');
    expect(ciWorkflowText).toContain('pull_request:');
    expect(ciWorkflowText).toContain('apps/web/**');
    expect(ciWorkflowText).toContain('infra/cdk/**');
    expect(ciWorkflowText).toContain('.github/workflows/app-deploy-dev.yml');
    expect(ciWorkflowText).toContain('p2-9-ci-changes');
    expect(ciWorkflowText).toContain("CDK_DEFAULT_ACCOUNT: '000000000000'");
    expect(ciWorkflowText).toContain('CDK_DEFAULT_REGION: ap-northeast-1');
    expect(ciWorkflowText).toContain('infra_changed=${{ steps.changes.outputs.infra }}');
    expect(ciWorkflowText).toContain('actions/checkout@9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0');
    expect(ciWorkflowText).toContain('actions/setup-java@ad2b38190b15e4d6bdf0c97fb4fca8412226d287');
    expect(ciWorkflowText).toContain('actions/setup-node@48b55a011bda9f5d6aeb4c2d9c7362e8dae4041e');
    expect(ciWorkflowText).toContain(
      'actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a',
    );
    expect(ciWorkflowText).toContain('dorny/paths-filter@fbd0ab8f3e69293af611ebaee6363fc25e6d187d');
    expect(ciWorkflowText).not.toContain('configure-aws-credentials');
    expect(ciWorkflowText).not.toContain('cdk -- deploy');

    expect(infraWorkflowText).toContain('workflow_run:');
    expect(infraWorkflowText).toContain("github.event.workflow_run.conclusion == 'success'");
    expect(infraWorkflowText).toContain("github.event.workflow_run.event == 'push'");
    expect(infraWorkflowText).toContain("github.event.workflow_run.head_branch == 'main'");
    expect(infraWorkflowText).toContain('environment: dev');
    expect(infraWorkflowText).toContain('id-token: write');
    expect(infraWorkflowText).toContain('group: workops-dev-deploy');
    expect(infraWorkflowText).toContain(
      'actions/download-artifact@3e5f45b2cfb9172054b4087a40e8e0b5a5461e7c',
    );
    expect(infraWorkflowText).toContain(
      'actions/checkout@9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0',
    );
    expect(infraWorkflowText).toContain(
      'actions/setup-node@48b55a011bda9f5d6aeb4c2d9c7362e8dae4041e',
    );
    expect(infraWorkflowText).toContain(
      'aws-actions/configure-aws-credentials@e7f100cf4c008499ea8adda475de1042d6975c7b',
    );
    expect(infraWorkflowText).toContain("needs.read-changes.outputs.infra_changed == 'true'");
    expect(ciWorkflowText).toContain('npm run cdk:deploy-app -- synth DeployStack');
    expect(infraWorkflowText).toContain(
      'npm run cdk:infra -- deploy FoundationStack --require-approval never',
    );
    expect(infraWorkflowText).toContain(
      'npm run cdk:infra -- deploy LogsStack --require-approval never',
    );
    expect(infraWorkflowText).not.toContain('deploy DeployStack');
    expect(infraWorkflowText).not.toContain('AppRuntimeStack');

    expect(appWorkflowText).toContain('name: App Deploy Dev');
    expect(appWorkflowText).toContain('workflow_dispatch:');
    expect(appWorkflowText).toContain('confirm_runtime_deploy:');
    expect(appWorkflowText).toContain("github.ref == 'refs/heads/main'");
    expect(appWorkflowText).toContain('inputs.confirm_runtime_deploy == true');
    expect(appWorkflowText).toContain('environment: dev');
    expect(appWorkflowText).toContain('id-token: write');
    expect(appWorkflowText).toContain('group: workops-dev-deploy');
    expect(appWorkflowText).toContain('WORKOPS_WEB_IMAGE_TAG: ${{ github.sha }}');
    expect(appWorkflowText).toContain('actions/checkout@9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0');
    expect(appWorkflowText).toContain(
      'actions/setup-node@48b55a011bda9f5d6aeb4c2d9c7362e8dae4041e',
    );
    expect(appWorkflowText).toContain(
      'aws-actions/configure-aws-credentials@e7f100cf4c008499ea8adda475de1042d6975c7b',
    );
    expect(appWorkflowText).toContain(
      'aws-actions/amazon-ecr-login@d539f0932e70871a027e9d5a9d8fc38589180a64',
    );
    expect(appWorkflowText).toContain('docker build -t "$IMAGE_URI" .');
    expect(appWorkflowText).toContain('docker push "$IMAGE_URI"');
    expect(appWorkflowText).toContain('Preflight runtime stack state');
    expect(appWorkflowText).toContain('workops-dev-foundation');
    expect(appWorkflowText).toContain('workops-dev-logs');
    expect(appWorkflowText).toContain('cleanup is required before runtime deploy');
    expect(appWorkflowText).toContain('npm run cdk:runtime -- diff DataStack --method=template');
    expect(appWorkflowText).toContain('npm run cdk:runtime -- diff EgressStack --method=template');
    expect(appWorkflowText).toContain('npm run cdk:runtime -- diff EdgeStack --method=template');
    expect(appWorkflowText).toContain(
      'npm run cdk:runtime -- diff AppRuntimeStack --method=template',
    );
    expect(appWorkflowText).toContain(
      'npm run cdk:runtime -- deploy DataStack --require-approval never',
    );
    expect(appWorkflowText).toContain(
      'npm run cdk:runtime -- deploy AppRuntimeStack --require-approval never',
    );
    expect(appWorkflowText).not.toContain('deploy LogsStack');
    expect(appWorkflowText).toContain('aws ecs wait services-stable');
    expect(appWorkflowText).toContain('/actuator/health');
    expect(appWorkflowText).not.toContain(':dev');
    expect(appWorkflowText).not.toContain(':latest');
    expect(appWorkflowText).not.toContain('mvnw test');
    expect(appWorkflowText).not.toContain('npm run build');
    expect(appWorkflowText).not.toContain('npm test');
    expect(appWorkflowText).not.toContain('cdk -- synth');
    expect(`${ciWorkflowText}\n${infraWorkflowText}\n${appWorkflowText}`).not.toContain(
      'npm run cdk --',
    );
  });
});

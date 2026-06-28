import { Tags } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { AppRuntimeStack } from '../lib/runtime/app-runtime-stack';
import { DataPauseStack } from '../lib/data-pause/data-pause-stack';
import { DataStack } from '../lib/data/data-stack';
import { DependencyStack } from '../lib/dependencies/dependency-stack';
import { EgressStack } from '../lib/network/egress-stack';
import { FoundationStack } from '../lib/foundation/foundation-stack';
import { IdentityStack } from '../lib/identity/identity-stack';
import { LogsStack } from '../lib/logs/logs-stack';
import { MigrationRunnerStack } from '../lib/migration/migration-runner-stack';
import { RegistryStack } from '../lib/registry/registry-stack';
import { WebAclStack } from '../lib/web/web-acl-stack';
import { WebDeliveryStack } from '../lib/web/web-delivery-stack';
import { WebIngressStack } from '../lib/web/web-ingress-stack';
import {
  TaggedResourceStack,
  createTestApp,
  testCognitoPlatformUserPoolClientId,
  testCognitoTenantUserPoolClientId,
  testCognitoUserPoolId,
  testEnv,
  testOpsNotificationEmail,
  testWebImageTag,
} from './workops-test-fixtures';

describe('WorkOps CDK foundation stacks', () => {
  test('creates Phase 2 base stack shells using the requested stage', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
    });
    const dependencyStack = new DependencyStack(app, 'DependencyStack', {
      env: testEnv,
      notificationEmail: testOpsNotificationEmail,
    });
    const dataStack = new DataStack(app, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      env: testEnv,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      vpc: foundationStack.vpc,
    });
    const identityStack = new IdentityStack(app, 'IdentityStack', {
      env: testEnv,
    });
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      env: testEnv,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      env: testEnv,
    });
    const dataPauseStack = new DataPauseStack(app, 'DataPauseStack', {
      env: testEnv,
      markAutoRestartLogGroup: logsStack.dataPauseMarkAutoRestartLogGroup,
      stopMarkedDbLogGroup: logsStack.dataPauseStopMarkedDbLogGroup,
    });
    const migrationRunnerStack = new MigrationRunnerStack(app, 'MigrationRunnerStack', {
      appSubnets: foundationStack.appSubnets,
      env: testEnv,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      migrationLogGroup: logsStack.migrationLogGroup,
      vpc: foundationStack.vpc,
    });
    const egressStack = new EgressStack(app, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      env: testEnv,
      publicSubnets: foundationStack.publicSubnets,
      vpc: foundationStack.vpc,
    });
    const webAclStack = new WebAclStack(app, 'WebAclStack', {
      crossRegionReferences: true,
      env: {
        account: testEnv.account,
        region: 'us-east-1',
      },
    });
    const webIngressStack = new WebIngressStack(app, 'WebIngressStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      env: testEnv,
      vpc: foundationStack.vpc,
    });
    const webDeliveryStack = new WebDeliveryStack(app, 'WebDeliveryStack', {
      cognitoPlatformUserPoolClientId: testCognitoPlatformUserPoolClientId,
      cognitoTenantUserPoolClientId: testCognitoTenantUserPoolClientId,
      cognitoUserPoolId: testCognitoUserPoolId,
      cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
      cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
      crossRegionReferences: true,
      env: testEnv,
      webAclArn: webAclStack.webAclArn,
    });
    const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
      env: testEnv,
      runtimeResources: {
        albSecurityGroup: foundationStack.albSecurityGroup,
        appSecurityGroup: foundationStack.appSecurityGroup,
        appSubnets: foundationStack.appSubnets,
        cloudFrontHttpsUrl: webDeliveryStack.cloudFrontHttpsUrl,
        cluster: foundationStack.ecsCluster,
        cognitoHostedUiDomainBaseUrl: identityStack.hostedUiDomainBaseUrl,
        cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
        cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
        cognitoUserPoolId: identityStack.userPoolId,
        listener: webIngressStack.listener,
        loadBalancerFullName: webIngressStack.loadBalancer.loadBalancerFullName,
        repository: registryStack.webRepository,
        vpc: foundationStack.vpc,
        webLogGroup: logsStack.webLogGroup,
      },
      webImageTag: testWebImageTag,
    });

    expect(dependencyStack.stackName).toBe('workops-dev-dependency');
    expect(foundationStack.stackName).toBe('workops-dev-foundation');
    expect(dataStack.stackName).toBe('workops-dev-data');
    expect(identityStack.stackName).toBe('workops-dev-identity');
    expect(registryStack.stackName).toBe('workops-dev-registry');
    expect(logsStack.stackName).toBe('workops-dev-logs');
    expect(dataPauseStack.stackName).toBe('workops-dev-data-pause');
    expect(migrationRunnerStack.stackName).toBe('workops-dev-migration-runner');
    expect(egressStack.stackName).toBe('workops-dev-egress');
    expect(webAclStack.stackName).toBe('workops-dev-web-acl');
    expect(webIngressStack.stackName).toBe('workops-dev-web-ingress');
    expect(webDeliveryStack.stackName).toBe('workops-dev-web-delivery');
    expect(appRuntimeStack.stackName).toBe('workops-dev-app-runtime');
  });

  test('creates the FoundationStack network and cluster resources', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
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
    template.resourceCountIs('AWS::EC2::VPCEndpoint', 1);
    template.hasResourceProperties('AWS::EC2::VPCEndpoint', {
      RouteTableIds: [
        {
          Ref: Match.stringLikeRegexp('VpcappSubnet1RouteTable'),
        },
        {
          Ref: Match.stringLikeRegexp('VpcappSubnet2RouteTable'),
        },
      ],
      ServiceName: {
        'Fn::Join': [
          '',
          [
            'com.amazonaws.',
            {
              Ref: 'AWS::Region',
            },
            '.s3',
          ],
        ],
      },
      VpcEndpointType: 'Gateway',
    });
    template.resourceCountIs('AWS::EC2::SecurityGroup', 4);
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-alb-sg',
    });
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-app-sg',
    });
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-db-sg',
    });
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-migration-sg',
    });
    template.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    template.hasResourceProperties('AWS::ECS::Cluster', {
      ClusterName: 'workops-dev-cluster',
    });
    expect(template.toJSON()).not.toHaveProperty('Outputs');
  });

  test('applies common WorkOps tags', () => {
    const stage = 'dev';
    const app = createTestApp(stage);

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

  test('creates the RegistryStack repositories and lifecycle policies', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const registryStack = new RegistryStack(app, 'RegistryStack', {});
    const template = Template.fromStack(registryStack);

    template.resourceCountIs('AWS::ECR::Repository', 2);
    const templateText = JSON.stringify(template.toJSON());
    template.hasResourceProperties('AWS::ECR::Repository', {
      RepositoryName: 'workops-dev-web',
      ImageTagMutability: 'IMMUTABLE',
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
                  countNumber: 10,
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
    template.hasResourceProperties('AWS::ECR::Repository', {
      RepositoryName: 'workops-dev-web-cache',
      ImageTagMutability: 'MUTABLE',
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
                  countNumber: 5,
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
    template.hasResource('AWS::ECR::Repository', {
      Properties: {
        RepositoryName: 'workops-dev-web-cache',
        EmptyOnDelete: true,
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    expect(template.toJSON()).not.toHaveProperty('Outputs');
    expect(templateText).not.toContain('workops-dev-migration-cache');
    expect(templateText).not.toContain('workops-dev-migration');
  });

  test('creates the DependencyStack CodeArtifact, ops topic, and non-secret parameters', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const dependencyStack = new DependencyStack(app, 'DependencyStack', {
      env: testEnv,
      notificationEmail: testOpsNotificationEmail,
    });
    const template = Template.fromStack(dependencyStack);
    const templateText = JSON.stringify(template.toJSON());

    template.hasResourceProperties('AWS::CodeArtifact::Domain', {
      DomainName: 'workops-dev',
    });
    template.hasResource('AWS::CodeArtifact::Domain', {
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResourceProperties('AWS::CodeArtifact::Repository', {
      DomainName: 'workops-dev',
      ExternalConnections: ['public:npmjs'],
      RepositoryName: 'workops-dev-npm',
    });
    template.hasResourceProperties('AWS::CodeArtifact::Repository', {
      DomainName: 'workops-dev',
      ExternalConnections: ['public:maven-central'],
      RepositoryName: 'workops-dev-maven',
    });
    template.resourceCountIs('AWS::CodeArtifact::Repository', 2);
    template.hasResourceProperties('AWS::SNS::Topic', {
      TopicName: 'workops-dev-ops-notifications',
    });
    template.hasResourceProperties('AWS::SNS::Subscription', {
      Endpoint: testOpsNotificationEmail,
      Protocol: 'email',
    });
    template.resourceCountIs('AWS::SSM::Parameter', 5);
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/dependencies/runtime/spring-profile',
      Type: 'String',
      Value: 'dev',
    });
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/dependencies/codeartifact/domain-name',
      Type: 'String',
      Value: 'workops-dev',
    });
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/dependencies/codeartifact/npm-repository-name',
      Type: 'String',
      Value: 'workops-dev-npm',
    });
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/dependencies/codeartifact/maven-repository-name',
      Type: 'String',
      Value: 'workops-dev-maven',
    });
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/dependencies/notifications/ops-topic-arn',
      Type: 'String',
    });
    expect(templateText).not.toContain('/workops/dev/spring/profile');
    expect(templateText).not.toContain('authorization token');
    expect(templateText).not.toContain('repositoryEndpoint');
  });
});

import { App, Stack, Tags } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { existsSync, readdirSync, readFileSync } from 'fs';
import { join } from 'path';
import { Construct } from 'constructs';
import { AppRuntimeStack } from '../lib/app-runtime-stack';
import { DataPauseStack } from '../lib/data-pause-stack';
import { DataStack } from '../lib/data-stack';
import { DependencyStack } from '../lib/dependency-stack';
import { EgressStack } from '../lib/egress-stack';
import { FoundationStack } from '../lib/foundation-stack';
import { IdentityStack } from '../lib/identity-stack';
import { LogsStack } from '../lib/logs-stack';
import { MigrationRunnerStack } from '../lib/migration-runner-stack';
import { PipelineStack } from '../lib/pipeline-stack';
import { RegistryStack } from '../lib/registry-stack';
import { WebAclStack } from '../lib/web-acl-stack';
import { WebDeliveryStack } from '../lib/web-delivery-stack';
import { WebIngressStack } from '../lib/web-ingress-stack';
import { setWorkopsStage } from '../lib/environment';

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
const testGitHubRepository = 'owner/repo';
const testOpsNotificationEmail = 'ops@example.com';
const testWebImageTag = 'test-sha';
const testEnv = {
  account: '123456789012',
  region: 'ap-northeast-1',
};

function createTestApp(stage: string): App {
  const app = new App();
  setWorkopsStage(app, stage);
  return app;
}

describe('WorkOps CDK app', () => {
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
    template.hasOutput('vpcId', {});
    template.hasOutput('publicSubnetIds', {});
    template.hasOutput('appSubnetIds', {});
    template.hasOutput('appSubnetOneId', {
      Export: {
        Name: 'workops-dev-foundation-app-subnet-one-id',
      },
    });
    template.hasOutput('appSubnetTwoId', {
      Export: {
        Name: 'workops-dev-foundation-app-subnet-two-id',
      },
    });
    template.hasOutput('appSubnetOneRouteTableId', {
      Export: {
        Name: 'workops-dev-foundation-app-subnet-one-route-table-id',
      },
    });
    template.hasOutput('appSubnetTwoRouteTableId', {
      Export: {
        Name: 'workops-dev-foundation-app-subnet-two-route-table-id',
      },
    });
    template.hasOutput('dbSubnetIds', {});
    template.hasOutput('ecsClusterName', {
      Export: {
        Name: 'workops-dev-foundation-ecs-cluster-name',
      },
    });
    template.hasOutput('albSecurityGroupId', {
      Export: {
        Name: 'workops-dev-foundation-alb-security-group-id',
      },
    });
    template.hasOutput('appSecurityGroupId', {
      Export: {
        Name: 'workops-dev-foundation-app-security-group-id',
      },
    });
    template.hasOutput('dbSecurityGroupId', {});
    template.hasOutput('migrationSecurityGroupId', {});
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
    const registryStack = new RegistryStack(app, 'RegistryStack', {
    });
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
    template.hasOutput('webRepositoryName', {});
    template.hasOutput('webRepositoryUri', {});
    template.hasOutput('webCacheRepositoryName', {});
    template.hasOutput('webCacheRepositoryUri', {});
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

  test('creates the PipelineStack source, quality gate, image builds, migration run, and app runtime deploy', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const pipelineStack = new PipelineStack(app, 'PipelineStack', {
      env: testEnv,
      githubRepository: testGitHubRepository,
      notificationEmail: testOpsNotificationEmail,
      stage,
      webImageTag: testWebImageTag,
    });
    const template = Template.fromStack(pipelineStack);
    const templateText = JSON.stringify(template.toJSON());
    const appRuntimeStackText = readFileSync(
      join(__dirname, '..', 'lib', 'app-runtime-stack.ts'),
      'utf8',
    );

    template.hasResourceProperties('AWS::CodePipeline::Pipeline', {
      Name: 'workops-dev-pipeline',
      PipelineType: 'V2',
    });
    template.hasResourceProperties('AWS::CodeStarConnections::Connection', {
      ConnectionName: 'workops-dev-github',
      ProviderType: 'GitHub',
    });
    template.hasResourceProperties('AWS::S3::Bucket', {
      BucketEncryption: {
        ServerSideEncryptionConfiguration: [
          {
            ServerSideEncryptionByDefault: {
              SSEAlgorithm: 'AES256',
            },
          },
        ],
      },
      BucketName: 'workops-dev-pipeline-artifacts',
      LifecycleConfiguration: {
        Rules: Match.arrayWith([
          Match.objectLike({
            ExpirationInDays: 30,
            NoncurrentVersionExpiration: {
              NoncurrentDays: 1,
            },
          }),
        ]),
      },
      VersioningConfiguration: {
        Status: 'Enabled',
      },
    });
    template.hasResource('AWS::S3::Bucket', {
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResourceProperties('AWS::CodeStarNotifications::NotificationRule', {
      DetailType: 'BASIC',
      EventTypeIds: [
        'codepipeline-pipeline-manual-approval-needed',
        'codepipeline-pipeline-pipeline-execution-failed',
      ],
    });
    template.hasResourceProperties('AWS::CodeBuild::Project', {
      Environment: Match.objectLike({
        ComputeType: 'BUILD_GENERAL1_MEDIUM',
        Image: 'aws/codebuild/amazonlinux-aarch64-standard:3.0',
        PrivilegedMode: true,
        Type: 'ARM_CONTAINER',
      }),
    });
    template.resourceCountIs('AWS::CodeBuild::Project', 11);
    expect(templateText).toContain('ec2:DescribeAvailabilityZones');
    expect(templateText).not.toContain('ec2:DescribeManagedPrefixLists');
    expect(templateText).not.toContain('ec2:GetManagedPrefixListEntries');
    expect(templateText).toContain('cloudformation:ListResources');
    expect(templateText).toContain('runtime-versions');
    expect(templateText).toContain('corretto25');
    template.hasResourceProperties('AWS::CodePipeline::Pipeline', {
      Stages: Match.arrayWith([
        Match.objectLike({
          Actions: Match.arrayWith([
            Match.objectLike({
              Name: 'BuildAndTest',
              RunOrder: 1,
            }),
            Match.objectLike({
              Name: 'ManualApproval',
              RunOrder: 2,
            }),
          ]),
          Name: 'DeployRegistry',
        }),
        Match.objectLike({
          Actions: Match.arrayWith([
            Match.objectLike({
              Configuration: Match.objectLike({
                EnvironmentVariables: Match.stringLikeRegexp('COMMIT_SHA.*CommitId'),
              }),
              Name: 'BuildWebImage',
              RunOrder: 1,
            }),
          ]),
          Name: 'BuildImages',
        }),
      ]),
    });
    template.hasResourceProperties('AWS::CodePipeline::Pipeline', {
      Stages: Match.arrayWith([
        Match.objectLike({
          Actions: Match.arrayWith([
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-web-acl.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-data-pause.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-data.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-egress.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-migration-runner.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-web-ingress.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-web-delivery.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-app-runtime.Prepare'),
            }),
          ]),
          Name: 'DeployDataNetworkMigration',
        }),
      ]),
    });
    template.hasResourceProperties('AWS::CodePipeline::Pipeline', {
      Stages: Match.arrayWith([
        Match.objectLike({
          Actions: Match.arrayWith([
            Match.objectLike({
              ActionTypeId: Match.objectLike({
                Provider: 'CodeBuild',
              }),
              Configuration: Match.objectLike({
                EnvironmentVariables: Match.stringLikeRegexp('COMMIT_SHA.*CommitId'),
                ProjectName: 'workops-dev-migration-runner',
              }),
              Name: Match.stringLikeRegexp('workops-dev-app-runtime.RunMigration'),
            }),
          ]),
          Name: 'DeployDataNetworkMigration',
        }),
      ]),
    });
    template.hasResourceProperties('AWS::CodePipeline::Pipeline', {
      Stages: Match.arrayWith([
        Match.objectLike({
          Actions: Match.arrayWith([
            Match.objectLike({
              InputArtifacts: Match.arrayWith([
                Match.objectLike({
                  Name: 'SourceArtifact',
                }),
              ]),
              Name: Match.stringLikeRegexp('workops-dev-app-runtime.RunMigration'),
            }),
          ]),
          Name: 'DeployDataNetworkMigration',
        }),
      ]),
    });
    template.hasResourceProperties('AWS::CodeBuild::Project', {
      Environment: Match.objectLike({
        ComputeType: 'BUILD_GENERAL1_MEDIUM',
        EnvironmentVariables: Match.arrayWith([
          Match.objectLike({
            Name: 'BUILD_CONTEXT',
            Value: 'apps/web',
          }),
          Match.objectLike({
            Name: 'DOCKERFILE',
            Value: 'apps/web/Dockerfile',
          }),
        ]),
        Image: 'aws/codebuild/amazonlinux-aarch64-standard:3.0',
        PrivilegedMode: true,
        Type: 'ARM_CONTAINER',
      }),
      Source: Match.objectLike({
        BuildSpec: Match.stringLikeRegexp(
          'docker buildx build --platform linux/arm64.*--cache-to.*--load',
        ),
      }),
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'ecr:GetAuthorizationToken',
            Effect: 'Allow',
            Resource: '*',
          }),
          Match.objectLike({
            Action: Match.arrayWith([
              'ecr:BatchCheckLayerAvailability',
              'ecr:BatchGetImage',
              'ecr:CompleteLayerUpload',
              'ecr:GetDownloadUrlForLayer',
              'ecr:InitiateLayerUpload',
              'ecr:PutImage',
              'ecr:UploadLayerPart',
            ]),
            Effect: 'Allow',
            Resource: Match.arrayWith([
              Match.objectLike({
                'Fn::Join': Match.arrayWith([
                  '',
                  Match.arrayWith([':ecr:ap-northeast-1:123456789012:repository/workops-dev-web']),
                ]),
              }),
              Match.objectLike({
                'Fn::Join': Match.arrayWith([
                  '',
                  Match.arrayWith([
                    ':ecr:ap-northeast-1:123456789012:repository/workops-dev-web-cache',
                  ]),
                ]),
              }),
            ]),
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'ecr:GetAuthorizationToken',
            Effect: 'Allow',
            Resource: '*',
          }),
          Match.objectLike({
            Action: Match.arrayWith([
              'ecr:BatchCheckLayerAvailability',
              'ecr:BatchGetImage',
              'ecr:CompleteLayerUpload',
              'ecr:GetDownloadUrlForLayer',
              'ecr:InitiateLayerUpload',
              'ecr:PutImage',
              'ecr:UploadLayerPart',
            ]),
            Effect: 'Allow',
            Resource: Match.arrayWith([]),
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'codestar-connections:UseConnection',
            Effect: 'Allow',
          }),
        ]),
      },
    });
    expect(templateText).toContain('GitHubSource');
    expect(templateText).not.toContain('GitHubSourceCodePipelineActionRole');
    expect(templateText).not.toContain('codeconnections:FullRepositoryId');
    expect(templateText).not.toContain('codeconnections:BranchName');
    expect(templateText).toContain(testGitHubRepository);
    expect(templateText).toContain('BranchName');
    template.hasResourceProperties('AWS::CodePipeline::Pipeline', {
      Stages: Match.arrayWith([
        Match.objectLike({
          Actions: Match.arrayWith([
            Match.objectLike({
              Configuration: Match.objectLike({
                BranchName: stage,
                FullRepositoryId: testGitHubRepository,
              }),
              Name: 'GitHubSource',
            }),
          ]),
          Name: 'Source',
        }),
      ]),
    });
    expect(templateText).toContain('WORKOPS_SOURCE_BRANCH');
    expect(templateText).not.toContain('WORKOPS_STAGE');
    expect(templateText).toContain('BuildAndTest');
    expect(templateText).toContain('java -version');
    expect(templateText).toContain('python3 scripts/configure-codeartifact-npm.py');
    expect(templateText).toContain('python3 infra/cdk/scripts/configure-codeartifact-maven.py');
    expect(templateText).toContain('export CODEARTIFACT_AUTH_TOKEN=');
    expect(templateText).toContain('$(cat');
    expect(templateText).toContain('./mvnw --settings');
    expect(templateText).toContain('WORKOPS_MAVEN_SETTINGS_PATH');
    expect(templateText).toContain('spotless:check');
    expect(templateText).toContain('compile spotbugs:check');
    expect(templateText).toContain('verify');
    expect(templateText).toContain('npm run lint');
    expect(templateText).toContain('npm run format:check');
    expect(templateText).toContain('ManualApproval');
    expect(templateText).toContain('DeployRegistry');
    expect(templateText).toContain('workops-dev-registry');
    expect(templateText).toContain('BuildImages');
    expect(templateText).toContain('BuildWebImage');
    expect(templateText).toContain('workops-dev-web');
    expect(templateText).toContain('workops-dev-web-cache:buildcache');
    expect(templateText).toContain('docker buildx create --name workops-builder');
    expect(templateText).toContain('--driver docker-container --use');
    expect(templateText).toContain('docker buildx inspect --bootstrap');
    expect(templateText).toContain('type=registry,ref=');
    expect(templateText).toContain('mode=max');
    expect(templateText).toContain('--secret id=maven_settings');
    expect(templateText).toContain('--secret id=codeartifact_token');
    expect(templateText).toContain('WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH');
    expect(templateText).toContain('public.ecr.aws/aquasecurity/trivy:0.71.2');
    expect(templateText).toContain('--exit-code 1');
    expect(templateText).toContain('--severity MEDIUM,HIGH,CRITICAL');
    expect(templateText).toContain('docker push');
    expect(templateText).toContain('$COMMIT_SHA');
    expect(templateText).toContain('WORKOPS_IMAGE_TAG');
    expect(templateText).toContain('DeployDataNetworkMigration');
    expect(templateText).toContain('RunMigration');
    expect(templateText).toContain('workops-dev-app-runtime');
    expect(appRuntimeStackText).toContain('runtimeResources: RuntimeResources');
    expect(appRuntimeStackText).not.toContain('Fn.importValue');
    expect(appRuntimeStackText).not.toContain('createRuntimeResources');
    expect(templateText).toContain('workops-dev-migration-runner');
    expect(templateText).toContain('SourceArtifact');
    expect(templateText).not.toContain('cat > run-migration');
    expect(templateText).not.toContain('python3 infra/cdk/scripts/run-migration-task.py');
    expect(templateText).not.toContain('python3 infra/cdk/scripts/start-migration-build.py');
    expect(templateText).not.toContain('MIGRATION_CLUSTER_NAME');
    expect(templateText).not.toContain('MIGRATION_TASK_DEFINITION_ARN');
    expect(templateText).not.toContain('MIGRATION_SOURCE_BUCKET');
    expect(templateText).toContain('npx cdk synth');
    expect(templateText).toContain('/workops/dev/dependencies/codeartifact/domain-name');
    expect(templateText).toContain('/workops/dev/dependencies/codeartifact/npm-repository-name');
    expect(templateText).toContain('/workops/dev/dependencies/codeartifact/maven-repository-name');
    expect(templateText).toContain('codeartifact:GetAuthorizationToken');
    expect(templateText).toContain('codeartifact:GetRepositoryEndpoint');
    expect(templateText).toContain('codeartifact:ReadFromRepository');
    expect(templateText).toContain('sts:GetServiceBearerToken');
    expect(templateText).toContain('codeartifact.amazonaws.com');
    expect(templateText).toContain('WORKOPS_OPS_NOTIFICATION_EMAIL');
    expect(templateText).not.toContain('AWS::StepFunctions::StateMachine');
    expect(templateText).not.toContain('AWS::Lambda::Function');
    expect(templateText).not.toContain('AWS::CodeDeploy');
    expect(templateText).not.toContain('Custom::RunTask');
    expect(templateText).not.toContain('WORKOPS_WEB_IMAGE_TAG');
    expect(templateText).not.toContain('WORKOPS_PIPELINE_NOTIFICATION_EMAIL');
    expect(templateText).not.toContain('workops-dev-pipeline-notifications');
    expect(templateText).toContain('/workops/dev/dependencies/notifications/ops-topic-arn');
    expect(templateText).not.toContain('CODEARTIFACT_AUTH_TOKEN:');
    expect(templateText).not.toContain('domain-owner');
    expect(templateText).not.toContain(':latest');
    expect(templateText).not.toContain(':dev');
    expect(templateText).not.toContain('workflow_dispatch');
    template.hasOutput('pipelineName', {});
    template.hasOutput('artifactBucketName', {});
    template.hasOutput('githubConnectionName', {});
  });

  test('uses BuildKit secrets for Docker Maven settings', () => {
    const dockerfileText = readFileSync(
      join(__dirname, '..', '..', '..', 'apps', 'web', 'Dockerfile'),
      'utf8',
    );

    expect(dockerfileText).toContain('# syntax=docker/dockerfile:1.7');
    expect(dockerfileText).toContain(
      '--mount=type=secret,id=maven_settings,target=/tmp/maven-settings.xml,required=true',
    );
    expect(dockerfileText).toContain(
      '--mount=type=secret,id=codeartifact_token,target=/tmp/codeartifact-token,required=true',
    );
    expect(dockerfileText).toContain('--settings /tmp/maven-settings.xml');
    expect(dockerfileText).toContain('CODEARTIFACT_AUTH_TOKEN="$(cat /tmp/codeartifact-token)"');
    expect(dockerfileText).not.toContain('ARG CODEARTIFACT');
    expect(dockerfileText).not.toContain('ARG MAVEN_SETTINGS');
  });

  test('creates the MigrationRunnerStack VPC CodeBuild project without an ECS task', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      env: testEnv,
    });
    const migrationRunnerStack = new MigrationRunnerStack(app, 'MigrationRunnerStack', {
      appSubnets: foundationStack.appSubnets,
      env: testEnv,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      migrationLogGroup: logsStack.migrationLogGroup,
      vpc: foundationStack.vpc,
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
    template.hasOutput('migrationProjectName', {});
  });

  test('creates the LogsStack log groups', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const logsStack = new LogsStack(app, 'LogsStack', {
    });
    const template = Template.fromStack(logsStack);

    template.resourceCountIs('AWS::Logs::LogGroup', 6);
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
    template.hasOutput('webLogGroupName', {
      Export: {
        Name: 'workops-dev-logs-web-log-group-name',
      },
    });
    template.hasOutput('migrationLogGroupName', {});
    template.hasOutput('cognitoClientUrlUpdaterLogGroupName', {});
    template.hasOutput('cognitoClientUrlUpdaterProviderLogGroupName', {});
    template.hasOutput('dataPauseMarkAutoRestartLogGroupName', {});
    template.hasOutput('dataPauseStopMarkedDbLogGroupName', {});
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
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
    });
    const egressStack = new EgressStack(app, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      env: testEnv,
      publicSubnets: foundationStack.publicSubnets,
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

  test('creates the P2-beta web ingress, delivery, and ACL stacks', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      env: testEnv,
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
    const ingressTemplate = Template.fromStack(webIngressStack);
    const deliveryTemplate = Template.fromStack(webDeliveryStack);
    const aclTemplate = Template.fromStack(webAclStack);
    const foundationTemplate = Template.fromStack(foundationStack);
    const ingressTemplateText = JSON.stringify(ingressTemplate.toJSON());
    const deliveryTemplateText = JSON.stringify(deliveryTemplate.toJSON());
    const ingressStackSource = readFileSync(
      join(__dirname, '..', 'lib', 'web-ingress-stack.ts'),
      'utf8',
    );

    ingressTemplate.resourceCountIs('AWS::EC2::SecurityGroupIngress', 1);
    foundationTemplate.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    ingressTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow CloudFront VPC origin to reach WorkOps ALB',
      FromPort: 80,
      GroupId: Match.anyValue(),
      IpProtocol: 'tcp',
      SourcePrefixListId: Match.anyValue(),
      ToPort: 80,
    });
    ingressTemplate.hasResourceProperties('Custom::AWS', {
      Create: Match.stringLikeRegexp('describeManagedPrefixLists'),
      InstallLatestAwsSdk: false,
      Update: Match.stringLikeRegexp('describeManagedPrefixLists'),
    });
    ingressTemplate.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/web-ingress/cloudfront-origin-prefix-list',
      RetentionInDays: 7,
    });
    ingressTemplate.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'ec2:DescribeManagedPrefixLists',
            Effect: 'Allow',
            Resource: '*',
          }),
        ]),
      },
    });
    expect(ingressTemplateText).toContain('com.amazonaws.global.cloudfront.origin-facing');
    ingressTemplate.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
      Name: 'workops-dev-web-alb',
      Scheme: 'internal',
      Type: 'application',
    });
    ingressTemplate.hasResourceProperties('AWS::ElasticLoadBalancingV2::Listener', {
      DefaultActions: Match.arrayWith([
        Match.objectLike({
          FixedResponseConfig: Match.objectLike({
            ContentType: 'text/plain',
            MessageBody: 'Not Found',
            StatusCode: '404',
          }),
          Type: 'fixed-response',
        }),
      ]),
      Port: 80,
      Protocol: 'HTTP',
    });
    ingressTemplate.resourceCountIs('AWS::ElasticLoadBalancingV2::TargetGroup', 0);
    ingressTemplate.resourceCountIs('AWS::SSM::Parameter', 3);
    ingressTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/web-ingress/origin/alb-arn',
      Type: 'String',
    });
    ingressTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/web-ingress/origin/alb-dns-name',
      Type: 'String',
    });
    ingressTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/web-ingress/origin/alb-security-group-id',
      Type: 'String',
    });
    ingressTemplate.resourceCountIs('AWS::CloudFront::Distribution', 0);
    ingressTemplate.resourceCountIs('AWS::CloudFront::VpcOrigin', 0);

    deliveryTemplate.hasResourceProperties('AWS::CloudFront::Distribution', {
      DistributionConfig: Match.objectLike({
        Comment: 'workops-dev-web-delivery',
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
        WebACLId: Match.anyValue(),
      }),
    });
    deliveryTemplate.hasResourceProperties('AWS::CloudFront::VpcOrigin', {
      VpcOriginEndpointConfig: Match.objectLike({
        Arn: Match.anyValue(),
        OriginProtocolPolicy: 'http-only',
        OriginSSLProtocols: ['TLSv1.2'],
      }),
    });
    expect(deliveryTemplateText).toContain('/workops/dev/web-ingress/origin/alb-arn');
    expect(deliveryTemplateText).toContain('/workops/dev/web-ingress/origin/alb-dns-name');
    expect(deliveryTemplateText).toContain('/workops/dev/web-ingress/origin/alb-security-group-id');
    deliveryTemplate.hasOutput('cloudFrontDomainName', {});
    deliveryTemplate.hasOutput('cloudFrontHttpsUrl', {});
    deliveryTemplate.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'workops-dev-cognito-client-url-updater',
      Handler: 'index.handler',
      Runtime: 'nodejs24.x',
      Timeout: 60,
    });
    deliveryTemplate.hasResourceProperties('AWS::Lambda::Function', {
      LoggingConfig: {
        LogGroup: Match.anyValue(),
      },
    });
    deliveryTemplate.resourceCountIs('AWS::Logs::LogGroup', 0);
    expect(deliveryTemplateText).toContain('CognitoClientUrlUpdaterLogGroup');
    expect(deliveryTemplateText).toContain('CognitoClientUrlUpdaterProviderLogGroup');
    expect(deliveryTemplateText).not.toContain(
      '/workops/dev/custom-resources/cognito-client-url-updater',
    );
    expect(deliveryTemplateText).not.toContain(
      '/workops/dev/custom-resources/cognito-client-url-updater-provider',
    );
    deliveryTemplate.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: ['cognito-idp:DescribeUserPoolClient', 'cognito-idp:UpdateUserPoolClient'],
            Effect: 'Allow',
          }),
        ]),
      },
    });
    deliveryTemplate.resourceCountIs('Custom::WorkOpsCognitoClientUrlUpdater', 1);
    deliveryTemplate.hasResourceProperties('Custom::WorkOpsCognitoClientUrlUpdater', {
      UserPoolId: testCognitoUserPoolId,
      PlatformClientId: testCognitoPlatformUserPoolClientId,
      TenantClientId: testCognitoTenantUserPoolClientId,
      CloudFrontDomainName: Match.anyValue(),
    });
    expect(deliveryTemplateText).not.toContain('"CidrIp":"0.0.0.0/0"');
    expect(deliveryTemplateText).not.toContain('authenticate-cognito');
    expect(ingressStackSource).toContain('com.amazonaws.global.cloudfront.origin-facing');
    expect(ingressStackSource).not.toContain('pl-58a04531');
    deliveryTemplate.resourceCountIs('AWS::WAFv2::WebACLAssociation', 0);

    aclTemplate.hasResourceProperties('AWS::WAFv2::WebACL', {
      DefaultAction: {
        Allow: {},
      },
      Name: 'workops-dev-cloudfront-web-acl',
      Scope: 'CLOUDFRONT',
      Rules: Match.arrayWith([
        Match.objectLike({
          Name: 'AWSManagedRulesCommonRuleSet',
          OverrideAction: {
            Count: {},
          },
          Statement: {
            ManagedRuleGroupStatement: {
              Name: 'AWSManagedRulesCommonRuleSet',
              VendorName: 'AWS',
            },
          },
        }),
      ]),
    });
    aclTemplate.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: 'aws-waf-logs-workops-dev-cloudfront',
      RetentionInDays: 7,
    });
    aclTemplate.resourceCountIs('AWS::WAFv2::LoggingConfiguration', 1);
  });

  test('creates the P2-5 IdentityStack Cognito Hosted UI resources', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const identityStack = new IdentityStack(app, 'IdentityStack', {
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
    template.hasOutput('userPoolId', {
      Export: {
        Name: 'workops-dev-identity-user-pool-id',
      },
    });
    template.hasOutput('platformUserPoolClientId', {
      Export: {
        Name: 'workops-dev-identity-platform-user-pool-client-id',
      },
    });
    template.hasOutput('tenantUserPoolClientId', {
      Export: {
        Name: 'workops-dev-identity-tenant-user-pool-client-id',
      },
    });
    template.hasOutput('hostedUiDomainBaseUrl', {
      Export: {
        Name: 'workops-dev-identity-hosted-ui-domain-base-url',
      },
    });
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
    const stage = 'dev';
    const app = createTestApp(stage);
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
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
      cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
      cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
      cognitoUserPoolId: identityStack.userPoolId,
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
    expect(templateText).toContain('amazoncognito.com');
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
      AlarmName: 'workops-dev-web-target-5xx',
      ComparisonOperator: 'GreaterThanThreshold',
      DatapointsToAlarm: 2,
      EvaluationPeriods: 2,
      Metrics: Match.arrayWith([
        Match.objectLike({
          Expression: 'blue5xx + green5xx',
        }),
      ]),
      Threshold: 0,
      TreatMissingData: 'notBreaching',
    });
    template.hasResourceProperties('AWS::CloudWatch::Alarm', {
      AlarmName: 'workops-dev-web-unhealthy-host',
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
              Ref: Match.stringLikeRegexp('WebTarget5xxAlarm'),
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
    expect(templateText).not.toContain('cognito-idp:AdminDeleteUser');
    expect(templateText).not.toContain('cognito-idp:AdminGetUser');
    expect(templateText).not.toContain('cognito-idp:AdminUpdateUserAttributes');
    expect(templateText).not.toContain('cognito-idp:AdminDisableUser');
    expect(templateText).toContain('awslogs-stream-prefix');
    expect(templateText).not.toContain('/workops/dev/migration');
    expect(templateText).not.toContain('AWS::CodeDeploy');
  });

  test('creates the DataStack database resources', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
    });
    const dataStack = new DataStack(app, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
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
      Description: 'Allow WorkOps migration CodeBuild to reach MySQL',
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

  test('keeps CDK entrypoints scoped and independent from dotenv', () => {
    const packageJsonPath = join(__dirname, '..', 'package.json');
    const cdkJsonPath = join(__dirname, '..', 'cdk.json');
    const cdkEntrypointPath = join(__dirname, '..', 'bin', 'cdk.ts');
    const packageJsonText = readFileSync(packageJsonPath, 'utf8');
    const cdkJsonText = readFileSync(cdkJsonPath, 'utf8');
    const cdkEntrypointText = readFileSync(cdkEntrypointPath, 'utf8');

    expect(packageJsonText).toContain('"build": "tsc"');
    expect(packageJsonText).toContain('"watch": "tsc -w"');
    expect(packageJsonText).toContain('python3 -m unittest discover scripts -p \\"*_test.py\\"');
    expect(packageJsonText).not.toContain('"cdk:deploy-app"');
    expect(packageJsonText).not.toContain('"cdk' + ':infra"');
    expect(packageJsonText).not.toContain('"cdk' + ':runtime"');
    expect(packageJsonText).not.toContain('"cdk": "cdk"');
    expect(packageJsonText).not.toContain('"bin"');
    expect(packageJsonText).not.toContain('"cdk:pipeline"');
    expect(cdkJsonText).toContain('"app": "npx ts-node --prefer-ts-exts bin/cdk.ts"');
    expect(existsSync(join(__dirname, '..', 'bin', 'cdk-pipeline.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'bin', 'cdk-deploy.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'bin', ['cdk', 'infra.ts'].join('-')))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'bin', ['cdk', 'runtime.ts'].join('-')))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'lib', 'deploy-stack.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'lib', 'config-stack.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'lib', 'secret-stack.ts'))).toBe(false);
    expect(cdkEntrypointText).toContain('DependencyStack');
    expect(cdkEntrypointText).toContain('PipelineStack');
    expect(cdkEntrypointText).toContain('GITHUB_REPOSITORY');
    expect(cdkEntrypointText).toContain('WORKOPS_IMAGE_TAG');
    expect(cdkEntrypointText).toContain('WORKOPS_OPS_NOTIFICATION_EMAIL');
    expect(cdkEntrypointText).not.toContain('WORKOPS_PIPELINE_NOTIFICATION_EMAIL');
    expect(cdkEntrypointText).not.toContain('WORKOPS_WEB_IMAGE_TAG');
    expect(cdkEntrypointText).not.toContain('AppRuntimeStack');
    expect(packageJsonText).not.toContain('synth:dev');
    expect(packageJsonText).not.toContain('diff:dev');
    expect(packageJsonText).not.toContain('deploy:dev');
    expect(packageJsonText).not.toContain('dotenv');
    expect(cdkEntrypointText).toContain('WORKOPS_SOURCE_BRANCH');
    expect(cdkEntrypointText).not.toContain('WORKOPS_STAGE');
    expect(cdkEntrypointText).not.toContain('tryGetContext');
    expect(cdkEntrypointText).not.toContain('dotenv');
    expect(cdkEntrypointText).not.toContain('.env.local');
  });

  test('removes GitHub Actions workflows from Phase 2 alpha CI/CD', () => {
    const workflowsPath = join(__dirname, '..', '..', '..', '.github', 'workflows');
    const workflowFiles = existsSync(workflowsPath) ? readdirSync(workflowsPath) : [];

    expect(workflowFiles).toEqual([]);
  });
});

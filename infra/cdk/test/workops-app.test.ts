import { App, Stack, Tags } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { existsSync, readdirSync, readFileSync } from 'fs';
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
import { MigrationStack } from '../lib/migration-stack';
import { PipelineStack } from '../lib/pipeline-stack';
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
const testGitHubRepository = 'owner/repo';
const testPipelineNotificationEmail = 'pipeline@example.com';
const testWebImageTag = 'test-sha';
const testMigrationImageTag = 'test-sha';
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
    const migrationStack = new MigrationStack(app, 'MigrationStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cluster: foundationStack.ecsCluster,
      env: testEnv,
      migrationImageTag: testMigrationImageTag,
      migrationLogGroup: logsStack.migrationLogGroup,
      migrationRepository: registryStack.migrationRepository,
      stage,
      stackName: `workops-${stage}-migration`,
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
      env: testEnv,
      runtimeResources: {
        albSecurityGroup: foundationStack.albSecurityGroup,
        appSecurityGroup: foundationStack.appSecurityGroup,
        appSubnets: foundationStack.appSubnets,
        cloudFrontHttpsUrl: edgeStack.cloudFrontHttpsUrl,
        cluster: foundationStack.ecsCluster,
        cognitoHostedUiDomainBaseUrl: identityStack.hostedUiDomainBaseUrl,
        cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
        cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
        cognitoUserPoolId: identityStack.userPoolId,
        listener: edgeStack.listener,
        loadBalancerFullName: edgeStack.loadBalancer.loadBalancerFullName,
        repository: registryStack.webRepository,
        vpc: foundationStack.vpc,
        webLogGroup: logsStack.webLogGroup,
      },
      stage,
      stackName: `workops-${stage}-app-runtime`,
      webImageTag: testWebImageTag,
    });

    expect(foundationStack.stackName).toBe('workops-dev-foundation');
    expect(secretStack.stackName).toBe('workops-dev-secret');
    expect(dataStack.stackName).toBe('workops-dev-data');
    expect(configStack.stackName).toBe('workops-dev-config');
    expect(identityStack.stackName).toBe('workops-dev-identity');
    expect(registryStack.stackName).toBe('workops-dev-registry');
    expect(logsStack.stackName).toBe('workops-dev-logs');
    expect(migrationStack.stackName).toBe('workops-dev-migration');
    expect(egressStack.stackName).toBe('workops-dev-egress');
    expect(edgeStack.stackName).toBe('workops-dev-edge');
    expect(appRuntimeStack.stackName).toBe('workops-dev-app-runtime');
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

  test('creates the RegistryStack repositories and lifecycle policies', () => {
    const app = new App();
    const stage = 'dev';
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      stage,
      stackName: `workops-${stage}-registry`,
    });
    const template = Template.fromStack(registryStack);

    template.resourceCountIs('AWS::ECR::Repository', 4);
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
      RepositoryName: 'workops-dev-migration',
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
    template.hasResourceProperties('AWS::ECR::Repository', {
      RepositoryName: 'workops-dev-migration-cache',
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
        RepositoryName: 'workops-dev-migration',
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
    template.hasResource('AWS::ECR::Repository', {
      Properties: {
        RepositoryName: 'workops-dev-migration-cache',
        EmptyOnDelete: true,
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasOutput('webRepositoryName', {});
    template.hasOutput('webRepositoryUri', {});
    template.hasOutput('migrationRepositoryName', {});
    template.hasOutput('migrationRepositoryUri', {});
    template.hasOutput('webCacheRepositoryName', {});
    template.hasOutput('webCacheRepositoryUri', {});
    template.hasOutput('migrationCacheRepositoryName', {});
    template.hasOutput('migrationCacheRepositoryUri', {});
  });

  test('creates the PipelineStack source, quality gate, image builds, migration run, and app runtime deploy', () => {
    const app = new App();
    const stage = 'dev';
    const pipelineStack = new PipelineStack(app, 'PipelineStack', {
      env: testEnv,
      githubRepository: testGitHubRepository,
      migrationImageTag: testMigrationImageTag,
      notificationEmail: testPipelineNotificationEmail,
      stage,
      stackName: `workops-${stage}-pipeline`,
      webImageTag: testWebImageTag,
    });
    const template = Template.fromStack(pipelineStack);
    const templateText = JSON.stringify(template.toJSON());
    const migrationRunTaskScriptText = readFileSync(
      join(__dirname, '..', 'scripts', 'run-migration-task.py'),
      'utf8',
    );
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
    template.hasResourceProperties('AWS::SNS::Topic', {
      TopicName: 'workops-dev-pipeline-notifications',
    });
    template.hasResourceProperties('AWS::SNS::Subscription', {
      Endpoint: testPipelineNotificationEmail,
      Protocol: 'email',
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
    template.resourceCountIs('AWS::CodeBuild::Project', 9);
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
              Name: Match.stringLikeRegexp('workops-dev-config.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-data.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-egress.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-edge.Prepare'),
            }),
            Match.objectLike({
              Name: Match.stringLikeRegexp('workops-dev-migration.Prepare'),
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
              Configuration: Match.objectLike({
                EnvironmentVariables: Match.stringLikeRegexp('MIGRATION_TASK_DEFINITION_ARN'),
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
              Configuration: Match.objectLike({
                EnvironmentVariables: Match.stringLikeRegexp('COMMIT_SHA.*CommitId'),
              }),
              Name: 'BuildMigrationImage',
              RunOrder: 1,
            }),
          ]),
          Name: 'BuildImages',
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
    template.hasResourceProperties('AWS::CodeBuild::Project', {
      Environment: Match.objectLike({
        ComputeType: 'BUILD_GENERAL1_MEDIUM',
        EnvironmentVariables: Match.arrayWith([
          Match.objectLike({
            Name: 'BUILD_CONTEXT',
            Value: '.',
          }),
          Match.objectLike({
            Name: 'DOCKERFILE',
            Value: 'infra/docker/migration/Dockerfile',
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
            Action: 'ecs:RunTask',
            Effect: 'Allow',
            Resource: Match.objectLike({
              'Fn::Join': Match.arrayWith([
                '',
                Match.arrayWith([
                  ':ecs:ap-northeast-1:123456789012:task-definition/workops-dev-migration:*',
                ]),
              ]),
            }),
          }),
          Match.objectLike({
            Action: 'ecs:DescribeTasks',
            Effect: 'Allow',
            Resource: '*',
          }),
          Match.objectLike({
            Action: 'iam:PassRole',
            Condition: {
              StringEquals: {
                'iam:PassedToService': 'ecs-tasks.amazonaws.com',
              },
            },
            Effect: 'Allow',
            Resource: Match.arrayWith([
              Match.objectLike({
                'Fn::Join': Match.arrayWith([
                  '',
                  Match.arrayWith([':iam::123456789012:role/workops-dev-migration-execution']),
                ]),
              }),
              Match.objectLike({
                'Fn::Join': Match.arrayWith([
                  '',
                  Match.arrayWith([':iam::123456789012:role/workops-dev-migration-task']),
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
            Resource: Match.arrayWith([
              Match.objectLike({
                'Fn::Join': Match.arrayWith([
                  '',
                  Match.arrayWith([
                    ':ecr:ap-northeast-1:123456789012:repository/workops-dev-migration',
                  ]),
                ]),
              }),
              Match.objectLike({
                'Fn::Join': Match.arrayWith([
                  '',
                  Match.arrayWith([
                    ':ecr:ap-northeast-1:123456789012:repository/workops-dev-migration-cache',
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
    expect(templateText).toContain('main');
    expect(templateText).toContain('BuildAndTest');
    expect(templateText).toContain('java -version');
    expect(templateText).toContain('./mvnw spotless:check');
    expect(templateText).toContain('./mvnw compile spotbugs:check');
    expect(templateText).toContain('./mvnw verify');
    expect(templateText).toContain('npm run lint');
    expect(templateText).toContain('npm run format:check');
    expect(templateText).toContain('ManualApproval');
    expect(templateText).toContain('DeployRegistry');
    expect(templateText).toContain('workops-dev-registry');
    expect(templateText).toContain('BuildImages');
    expect(templateText).toContain('BuildWebImage');
    expect(templateText).toContain('BuildMigrationImage');
    expect(templateText).toContain('workops-dev-web');
    expect(templateText).toContain('workops-dev-web-cache:buildcache');
    expect(templateText).toContain('workops-dev-migration');
    expect(templateText).toContain('workops-dev-migration-cache:buildcache');
    expect(templateText).toContain('docker buildx create --name workops-builder');
    expect(templateText).toContain('--driver docker-container --use');
    expect(templateText).toContain('docker buildx inspect --bootstrap');
    expect(templateText).toContain('type=registry,ref=');
    expect(templateText).toContain('mode=max');
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
    expect(templateText).toContain('python3 infra/cdk/scripts/run-migration-task.py');
    expect(templateText).not.toContain('cat > run-migration');
    expect(migrationRunTaskScriptText).toContain('run-task');
    expect(migrationRunTaskScriptText).toContain('tasks-stopped');
    expect(migrationRunTaskScriptText).toContain('describe-tasks');
    expect(migrationRunTaskScriptText).toContain('assignPublicIp=DISABLED');
    expect(templateText).toContain('MIGRATION_CLUSTER_NAME');
    expect(templateText).toContain('MIGRATION_TASK_DEFINITION_ARN');
    expect(templateText).toContain('MIGRATION_SUBNET_IDS');
    expect(templateText).toContain('MIGRATION_SECURITY_GROUP_ID');
    expect(templateText).toContain('MIGRATION_CONTAINER_NAME');
    expect(migrationRunTaskScriptText).toContain('EssentialContainerExited');
    expect(migrationRunTaskScriptText).toContain('exitCode');
    expect(migrationRunTaskScriptText).toContain('run-task response did not include taskArn');
    expect(migrationRunTaskScriptText).toContain('did not return exactly one task');
    expect(templateText).toContain('npm run cdk:pipeline -- synth');
    expect(templateText).not.toContain('AWS::StepFunctions::StateMachine');
    expect(templateText).not.toContain('AWS::Lambda::Function');
    expect(templateText).not.toContain('AWS::CodeDeploy');
    expect(templateText).not.toContain('Custom::RunTask');
    expect(templateText).not.toContain('WORKOPS_WEB_IMAGE_TAG');
    expect(templateText).not.toContain(':latest');
    expect(templateText).not.toContain(':dev');
    expect(templateText).not.toContain('workflow_dispatch');
    template.hasOutput('pipelineName', {});
    template.hasOutput('artifactBucketName', {});
    template.hasOutput('githubConnectionName', {});
    template.hasOutput('notificationTopicName', {});
  });

  test('creates the MigrationStack run task definition without an ECS service', () => {
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
    const migrationStack = new MigrationStack(app, 'MigrationStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cluster: foundationStack.ecsCluster,
      env: testEnv,
      migrationImageTag: testMigrationImageTag,
      migrationLogGroup: logsStack.migrationLogGroup,
      migrationRepository: registryStack.migrationRepository,
      stage,
      stackName: `workops-${stage}-migration`,
    });
    const template = Template.fromStack(migrationStack);
    const templateText = JSON.stringify(template.toJSON());

    template.resourceCountIs('AWS::ECS::TaskDefinition', 1);
    template.resourceCountIs('AWS::ECS::Service', 0);
    template.hasResourceProperties('AWS::IAM::Role', {
      RoleName: 'workops-dev-migration-execution',
      AssumeRolePolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'sts:AssumeRole',
            Principal: {
              Service: 'ecs-tasks.amazonaws.com',
            },
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::IAM::Role', {
      RoleName: 'workops-dev-migration-task',
      AssumeRolePolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'sts:AssumeRole',
            Principal: {
              Service: 'ecs-tasks.amazonaws.com',
            },
          }),
        ]),
      },
    });
    template.hasResourceProperties('AWS::ECS::TaskDefinition', {
      Cpu: '512',
      Family: 'workops-dev-migration',
      Memory: '1024',
      NetworkMode: 'awsvpc',
      RequiresCompatibilities: ['FARGATE'],
      RuntimePlatform: {
        CpuArchitecture: 'ARM64',
        OperatingSystemFamily: 'LINUX',
      },
      ContainerDefinitions: Match.arrayWith([
        Match.objectLike({
          Essential: true,
          Name: 'migration',
          Environment: Match.arrayWith([
            {
              Name: 'AWS_REGION',
              Value: 'ap-northeast-1',
            },
            {
              Name: 'WORKOPS_FLYWAY_LOCATIONS',
              Value:
                'filesystem:/flyway/sql/migration,filesystem:/flyway/sql/seed/common,filesystem:/flyway/sql/seed/aws-dev',
            },
          ]),
          LogConfiguration: Match.objectLike({
            LogDriver: 'awslogs',
            Options: Match.objectLike({
              'awslogs-stream-prefix': 'migration',
            }),
          }),
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
    template.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: Match.arrayWith(['ssm:GetParameters', 'ssm:GetParameter']),
          }),
          Match.objectLike({
            Action: Match.arrayWith(['secretsmanager:GetSecretValue']),
          }),
        ]),
      },
    });
    expect(templateText).toContain(':test-sha');
    expect(templateText).toContain('/workops/dev/db/url');
    expect(templateText).toContain('/workops/dev/db/master');
    template.hasOutput('migrationTaskDefinitionArn', {});
    template.hasOutput('migrationContainerName', {});
    template.hasOutput('migrationClusterName', {});
    template.hasOutput('migrationSubnetIds', {});
    template.hasOutput('migrationSecurityGroupId', {});
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
    template.hasOutput('webLogGroupName', {
      Export: {
        Name: 'workops-dev-logs-web-log-group-name',
      },
    });
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
      SourcePrefixListId: Match.anyValue(),
      ToPort: 80,
    });
    template.hasResourceProperties('Custom::AWS', {
      Create: Match.stringLikeRegexp('describeManagedPrefixLists'),
      InstallLatestAwsSdk: false,
      Update: Match.stringLikeRegexp('describeManagedPrefixLists'),
    });
    expect(templateText).toContain('com.amazonaws.global.cloudfront.origin-facing');
    template.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
      Name: 'workops-dev-web-alb',
      Scheme: 'internal',
      Type: 'application',
    });
    template.hasResourceProperties('AWS::ElasticLoadBalancingV2::Listener', {
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
    template.resourceCountIs('AWS::ElasticLoadBalancingV2::TargetGroup', 0);
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
    template.hasOutput('cloudFrontHttpsUrl', {
      Export: {
        Name: 'workops-dev-edge-cloudfront-https-url',
      },
    });
    template.hasOutput('listenerArn', {
      Export: {
        Name: 'workops-dev-edge-listener-arn',
      },
    });
    template.hasOutput('loadBalancerFullName', {
      Export: {
        Name: 'workops-dev-edge-load-balancer-full-name',
      },
    });
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
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      env: testEnv,
      stage,
      stackName: `workops-${stage}-foundation`,
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
    const edgeStack = new EdgeStack(app, 'EdgeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
      cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
      cognitoUserPoolId: identityStack.userPoolId,
      cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
      cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
      env: testEnv,
      stage,
      stackName: `workops-${stage}-edge`,
      vpc: foundationStack.vpc,
    });
    const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
      env: testEnv,
      runtimeResources: {
        albSecurityGroup: foundationStack.albSecurityGroup,
        appSecurityGroup: foundationStack.appSecurityGroup,
        appSubnets: foundationStack.appSubnets,
        cloudFrontHttpsUrl: edgeStack.cloudFrontHttpsUrl,
        cluster: foundationStack.ecsCluster,
        cognitoHostedUiDomainBaseUrl: identityStack.hostedUiDomainBaseUrl,
        cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
        cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
        cognitoUserPoolId: identityStack.userPoolId,
        listener: edgeStack.listener,
        loadBalancerFullName: edgeStack.loadBalancer.loadBalancerFullName,
        repository: registryStack.webRepository,
        vpc: foundationStack.vpc,
        webLogGroup: logsStack.webLogGroup,
      },
      stage,
      stackName: `workops-${stage}-app-runtime`,
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
    const pipelineEntrypointPath = join(__dirname, '..', 'bin', 'cdk-pipeline.ts');
    const packageJsonText = readFileSync(packageJsonPath, 'utf8');
    const cdkJsonText = readFileSync(cdkJsonPath, 'utf8');
    const pipelineEntrypointText = readFileSync(pipelineEntrypointPath, 'utf8');

    expect(packageJsonText).toContain('"build": "tsc"');
    expect(packageJsonText).toContain('"watch": "tsc -w"');
    expect(packageJsonText).toContain('"test": "jest"');
    expect(packageJsonText).not.toContain('"cdk:deploy-app"');
    expect(packageJsonText).not.toContain('"cdk' + ':infra"');
    expect(packageJsonText).not.toContain('"cdk' + ':runtime"');
    expect(packageJsonText).toContain('"cdk:pipeline": "cdk --app');
    expect(packageJsonText).not.toContain('"bin"');
    expect(packageJsonText).not.toContain('"cdk": "cdk"');
    expect(cdkJsonText).not.toContain('"app"');
    expect(existsSync(join(__dirname, '..', 'bin', 'cdk-deploy.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'bin', ['cdk', 'infra.ts'].join('-')))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'bin', ['cdk', 'runtime.ts'].join('-')))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'lib', 'deploy-stack.ts'))).toBe(false);
    expect(pipelineEntrypointText).toContain('PipelineStack');
    expect(pipelineEntrypointText).toContain('GITHUB_REPOSITORY');
    expect(pipelineEntrypointText).toContain('WORKOPS_IMAGE_TAG');
    expect(pipelineEntrypointText).toContain('WORKOPS_PIPELINE_NOTIFICATION_EMAIL');
    expect(pipelineEntrypointText).not.toContain('WORKOPS_WEB_IMAGE_TAG');
    expect(pipelineEntrypointText).not.toContain('AppRuntimeStack');
    expect(packageJsonText).not.toContain('synth:dev');
    expect(packageJsonText).not.toContain('diff:dev');
    expect(packageJsonText).not.toContain('deploy:dev');
    expect(packageJsonText).not.toContain('dotenv');
    expect(pipelineEntrypointText).toContain('WORKOPS_STAGE');
    expect(pipelineEntrypointText).not.toContain('tryGetContext');
    expect(pipelineEntrypointText).not.toContain('dotenv');
    expect(pipelineEntrypointText).not.toContain('.env.local');
  });

  test('removes GitHub Actions workflows from Phase 2 alpha CI/CD', () => {
    const workflowsPath = join(__dirname, '..', '..', '..', '.github', 'workflows');
    const workflowFiles = existsSync(workflowsPath) ? readdirSync(workflowsPath) : [];

    expect(workflowFiles).toEqual([]);
  });
});

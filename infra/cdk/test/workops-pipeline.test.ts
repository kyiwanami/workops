import { Match, Template } from 'aws-cdk-lib/assertions';
import { readFileSync } from 'fs';
import { join } from 'path';
import { PipelineStack } from '../lib/pipeline-stack';
import {
  createTestApp,
  testEnv,
  testGitHubRepository,
  testOpsNotificationEmail,
  testWebImageTag,
} from './workops-test-fixtures';

describe('WorkOps CDK pipeline', () => {
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
    expect(template.toJSON()).not.toHaveProperty('Outputs');
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
});

import {
  Duration,
  RemovalPolicy,
  Stack,
  StackProps,
} from 'aws-cdk-lib';
import { BuildSpec, Project as CodeBuildProject } from 'aws-cdk-lib/aws-codebuild';
import { DetailType } from 'aws-cdk-lib/aws-codestarnotifications';
import { CfnConnection } from 'aws-cdk-lib/aws-codestarconnections';
import {
  Artifact,
  PipelineNotificationEvents,
  PipelineType,
  Pipeline as CodePipelinePipeline,
} from 'aws-cdk-lib/aws-codepipeline';
import { CodeStarConnectionsSourceAction } from 'aws-cdk-lib/aws-codepipeline-actions';
import { CfnServiceLinkedRole, Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Bucket, BucketEncryption } from 'aws-cdk-lib/aws-s3';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import {
  CodeBuildStep,
  CodePipeline,
  CodePipelineFileSet,
  ManualApprovalStep,
} from 'aws-cdk-lib/pipelines';
import { Construct } from 'constructs';
import { workopsStackName } from './environment';
import {
  createBuildEnvironment,
  createBuildImageStep,
  createCodeArtifactParameterNames,
  createCodeArtifactParameterStoreEnvironment,
  createCodeArtifactPolicyStatements,
} from './pipeline-build-steps';
import { DataNetworkMigrationDeployStage, RegistryDeployStage } from './pipeline-deploy-stages';
import { MigrationActionStep } from './pipeline-migration-action-step';

export interface PipelineStackProps extends StackProps {
  stage: string;
  githubRepository: string;
  notificationEmail: string;
  webImageTag: string;
}

interface GitHubRepositoryName {
  owner: string;
  repo: string;
}

export class PipelineStack extends Stack {
  constructor(scope: Construct, id: string, props: PipelineStackProps) {
    super(scope, id, {
      ...props,
      stackName: workopsStackName(scope, 'pipeline'),
    });

    const buildEnvironment = createBuildEnvironment();
    const codeArtifactParameterNames = createCodeArtifactParameterNames(props.stage);
    const codeArtifactPolicyStatements = createCodeArtifactPolicyStatements(this, props.stage);
    const codeStarNotificationsRole = new CfnServiceLinkedRole(
      this,
      'CodeStarNotificationsServiceRole',
      {
        awsServiceName: 'codestar-notifications.amazonaws.com',
      },
    );
    const artifactBucket = new Bucket(this, 'ArtifactBucket', {
      bucketName: `workops-${props.stage}-pipeline-artifacts`,
      encryption: BucketEncryption.S3_MANAGED,
      lifecycleRules: [
        {
          expiration: Duration.days(30),
          noncurrentVersionExpiration: Duration.days(1),
        },
      ],
      removalPolicy: RemovalPolicy.DESTROY,
      versioned: true,
    });
    const notificationTopicArn = StringParameter.valueForStringParameter(
      this,
      `/workops/${props.stage}/dependencies/notifications/ops-topic-arn`,
    );
    const notificationTopic = Topic.fromTopicArn(
      this,
      'OpsNotificationTopic',
      notificationTopicArn,
    );

    const githubConnection = new CfnConnection(this, 'GitHubConnection', {
      connectionName: `workops-${props.stage}-github`,
      providerType: 'GitHub',
    });
    const githubRepositoryName = this.parseGitHubRepository(props.githubRepository);
    const sourceArtifact = new Artifact('SourceArtifact');
    const sourceAction = new CodeStarConnectionsSourceAction({
      actionName: 'GitHubSource',
      branch: props.stage,
      connectionArn: githubConnection.attrConnectionArn,
      output: sourceArtifact,
      owner: githubRepositoryName.owner,
      repo: githubRepositoryName.repo,
      triggerOnPush: true,
    });

    const codePipeline = new CodePipelinePipeline(this, 'Pipeline', {
      artifactBucket,
      crossAccountKeys: false,
      pipelineName: `workops-${props.stage}-pipeline`,
      pipelineType: PipelineType.V2,
      usePipelineRoleForActions: true,
    });
    codePipeline.addStage({
      actions: [sourceAction],
      stageName: 'Source',
    });

    const source = CodePipelineFileSet.fromArtifact(sourceArtifact);
    const commitSha = sourceAction.variables.commitId;

    const pipeline = new CodePipeline(this, 'CdkPipeline', {
      codeBuildDefaults: {
        buildEnvironment,
      },
      codePipeline,
      dockerEnabledForSelfMutation: true,
      dockerEnabledForSynth: true,
      pipelineType: PipelineType.V2,
      selfMutation: true,
      synth: new CodeBuildStep('Synth', {
        buildEnvironment,
        commands: [
          'cd infra/cdk',
          'python3 scripts/configure-codeartifact-npm.py',
          'npm ci',
          'npm run build',
          'npx cdk synth',
        ],
        env: {
          GITHUB_REPOSITORY: props.githubRepository,
          WORKOPS_IMAGE_TAG: commitSha,
          WORKOPS_OPS_NOTIFICATION_EMAIL: props.notificationEmail,
          WORKOPS_SOURCE_BRANCH: props.stage,
        },
        input: source,
        partialBuildSpec: BuildSpec.fromObject({
          env: {
            'parameter-store': createCodeArtifactParameterStoreEnvironment(
              codeArtifactParameterNames,
            ),
          },
          version: '0.2',
        }),
        primaryOutputDirectory: 'infra/cdk/cdk.out',
        rolePolicyStatements: [
          ...codeArtifactPolicyStatements,
          new PolicyStatement({
            actions: ['ec2:DescribeAvailabilityZones'],
            effect: Effect.ALLOW,
            resources: ['*'],
          }),
          new PolicyStatement({
            actions: ['cloudformation:ListResources'],
            effect: Effect.ALLOW,
            resources: ['*'],
          }),
        ],
      }),
      usePipelineRoleForActions: true,
    });

    const buildAndTestStep = new CodeBuildStep('BuildAndTest', {
      buildEnvironment,
      commands: [],
      input: source,
      rolePolicyStatements: codeArtifactPolicyStatements,
      partialBuildSpec: BuildSpec.fromObject({
        env: {
          'parameter-store': createCodeArtifactParameterStoreEnvironment(
            codeArtifactParameterNames,
          ),
        },
        phases: {
          install: {
            'runtime-versions': {
              java: 'corretto25',
            },
          },
          build: {
            commands: [
              'mkdir -p "$CODEBUILD_SRC_DIR/.workops-codeartifact"',
              'export WORKOPS_MAVEN_SETTINGS_PATH="$CODEBUILD_SRC_DIR/.workops-codeartifact/settings.xml"',
              'export WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH="$CODEBUILD_SRC_DIR/.workops-codeartifact/codeartifact-token"',
              'python3 infra/cdk/scripts/configure-codeartifact-maven.py',
              'export CODEARTIFACT_AUTH_TOKEN="$(cat "$WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH")"',
              'cd apps/web',
              'java -version',
              './mvnw --settings "$WORKOPS_MAVEN_SETTINGS_PATH" spotless:check',
              './mvnw --settings "$WORKOPS_MAVEN_SETTINGS_PATH" compile spotbugs:check',
              './mvnw --settings "$WORKOPS_MAVEN_SETTINGS_PATH" verify',
              'cd ../../infra/cdk',
              'python3 scripts/configure-codeartifact-npm.py',
              'npm ci',
              'npm run build',
              'npm run test',
              'npm run lint',
              'npm run format:check',
            ],
          },
        },
        version: '0.2',
      }),
    });
    const manualApprovalStep = new ManualApprovalStep('ManualApproval', {
      comment:
        'Approve WorkOps dev registry deployment before image build and runtime deploy stages.',
      notificationTopic,
    });
    manualApprovalStep.addStepDependency(buildAndTestStep);

    pipeline.addStage(
      new RegistryDeployStage(this, 'DeployRegistry'),
      {
        pre: [buildAndTestStep, manualApprovalStep],
      },
    );
    pipeline.addWave('BuildImages', {
      pre: [
        createBuildImageStep(this, source, {
          buildContext: 'apps/web',
          cacheRepositoryName: `workops-${props.stage}-web-cache`,
          codeArtifactParameterNames,
          codeArtifactPolicyStatements,
          commitSha,
          dockerfile: 'apps/web/Dockerfile',
          id: 'BuildWebImage',
          repositoryName: `workops-${props.stage}-web`,
        }),
      ],
    });
    const dataNetworkMigrationStage = new DataNetworkMigrationDeployStage(
      this,
      'DeployDataNetworkMigration',
      {
        webImageTag: props.webImageTag,
      },
    );
    pipeline.addStage(dataNetworkMigrationStage, {
      stackSteps: [
        {
          stack: dataNetworkMigrationStage.appRuntimeStack,
          pre: [
            new MigrationActionStep('RunMigration', {
              commitSha,
              input: source,
              project: CodeBuildProject.fromProjectName(
                this,
                'MigrationCodeBuildProjectReference',
                `workops-${props.stage}-migration-runner`,
              ),
            }),
          ],
        },
      ],
    });
    pipeline.buildPipeline();

    const pipelineNotifications = codePipeline.notifyOn(
      'PipelineNotifications',
      notificationTopic,
      {
        detailType: DetailType.BASIC,
        events: [
          PipelineNotificationEvents.MANUAL_APPROVAL_NEEDED,
          PipelineNotificationEvents.PIPELINE_EXECUTION_FAILED,
        ],
      },
    );
    pipelineNotifications.node.addDependency(codeStarNotificationsRole);
  }

  private parseGitHubRepository(githubRepository: string): GitHubRepositoryName {
    const parts = githubRepository.split('/');
    const owner = parts[0];
    const repo = parts[1];

    if (parts.length !== 2 || !owner || !repo) {
      throw new Error('githubRepository must use owner/repo format');
    }

    return {
      owner,
      repo,
    };
  }
}

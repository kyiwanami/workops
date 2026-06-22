import {
  Aws,
  CfnOutput,
  Duration,
  Environment,
  RemovalPolicy,
  Stack,
  StackProps,
  Stage,
} from 'aws-cdk-lib';
import { BuildEnvironment, ComputeType, LinuxArmBuildImage } from 'aws-cdk-lib/aws-codebuild';
import { DetailType } from 'aws-cdk-lib/aws-codestarnotifications';
import { CfnConnection } from 'aws-cdk-lib/aws-codestarconnections';
import {
  PipelineNotificationEvents,
  PipelineType,
  Pipeline as CodePipelinePipeline,
} from 'aws-cdk-lib/aws-codepipeline';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Bucket, BucketEncryption } from 'aws-cdk-lib/aws-s3';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { EmailSubscription } from 'aws-cdk-lib/aws-sns-subscriptions';
import {
  CodeBuildStep,
  CodePipeline,
  CodePipelineSource,
  ManualApprovalStep,
} from 'aws-cdk-lib/pipelines';
import { Construct } from 'constructs';
import { RegistryStack } from './registry-stack';

export interface PipelineStackProps extends StackProps {
  stage: string;
  githubRepository: string;
  notificationEmail: string;
}

interface RegistryDeployStageProps {
  env?: Environment;
  stage: string;
}

interface BuildImageConfig {
  cacheRepositoryName: string;
  dockerfile: string;
  id: string;
  repositoryName: string;
  buildContext: string;
}

class RegistryDeployStage extends Stage {
  constructor(scope: Construct, id: string, props: RegistryDeployStageProps) {
    super(scope, id, {
      env: props.env,
    });

    new RegistryStack(this, 'RegistryStack', {
      env: props.env,
      stage: props.stage,
      stackName: `workops-${props.stage}-registry`,
    });
  }
}

export class PipelineStack extends Stack {
  constructor(scope: Construct, id: string, props: PipelineStackProps) {
    super(scope, id, props);

    const buildEnvironment = this.createBuildEnvironment();
    const artifactBucket = new Bucket(this, 'ArtifactBucket', {
      bucketName: `workops-${props.stage}-pipeline-artifacts`,
      encryption: BucketEncryption.S3_MANAGED,
      lifecycleRules: [
        {
          expiration: Duration.days(30),
          noncurrentVersionExpiration: Duration.days(1),
        },
      ],
      removalPolicy: RemovalPolicy.RETAIN,
      versioned: true,
    });
    const notificationTopic = new Topic(this, 'NotificationTopic', {
      topicName: `workops-${props.stage}-pipeline-notifications`,
    });
    notificationTopic.addSubscription(new EmailSubscription(props.notificationEmail));

    const githubConnection = new CfnConnection(this, 'GitHubConnection', {
      connectionName: `workops-${props.stage}-github`,
      providerType: 'GitHub',
    });

    const codePipeline = new CodePipelinePipeline(this, 'Pipeline', {
      artifactBucket,
      crossAccountKeys: false,
      pipelineName: `workops-${props.stage}-pipeline`,
      pipelineType: PipelineType.V2,
      usePipelineRoleForActions: true,
    });

    this.restrictConnectionUseToRepositoryAndBranch(
      codePipeline,
      githubConnection.attrConnectionArn,
      props.githubRepository,
    );

    const source = CodePipelineSource.connection(props.githubRepository, 'main', {
      actionName: 'GitHubSource',
      connectionArn: githubConnection.attrConnectionArn,
      triggerOnPush: true,
    });

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
        commands: ['cd infra/cdk', 'npm ci', 'npm run build', 'npm run cdk:pipeline -- synth'],
        env: {
          GITHUB_REPOSITORY: props.githubRepository,
          WORKOPS_PIPELINE_NOTIFICATION_EMAIL: props.notificationEmail,
          WORKOPS_STAGE: props.stage,
        },
        input: source,
        primaryOutputDirectory: 'infra/cdk/cdk.out',
      }),
      usePipelineRoleForActions: true,
    });

    const buildAndTestStep = new CodeBuildStep('BuildAndTest', {
      buildEnvironment,
      commands: [
        'cd apps/web',
        './mvnw spotless:check',
        './mvnw compile spotbugs:check',
        './mvnw verify',
        'cd ../../infra/cdk',
        'npm ci',
        'npm run build',
        'npm run test',
        'npm run lint',
        'npm run format:check',
      ],
      input: source,
    });
    const manualApprovalStep = new ManualApprovalStep('ManualApproval', {
      comment:
        'Approve WorkOps dev registry deployment before image build and runtime deploy stages.',
      notificationTopic,
    });
    manualApprovalStep.addStepDependency(buildAndTestStep);

    pipeline.addStage(
      new RegistryDeployStage(this, 'DeployRegistry', {
        env: props.env,
        stage: props.stage,
      }),
      {
        pre: [buildAndTestStep, manualApprovalStep],
      },
    );
    pipeline.addWave('BuildImages', {
      pre: [
        this.createBuildImageStep(source, {
          buildContext: 'apps/web',
          cacheRepositoryName: `workops-${props.stage}-web-cache`,
          dockerfile: 'apps/web/Dockerfile',
          id: 'BuildWebImage',
          repositoryName: `workops-${props.stage}-web`,
        }),
        this.createBuildImageStep(source, {
          buildContext: '.',
          cacheRepositoryName: `workops-${props.stage}-migration-cache`,
          dockerfile: 'infra/docker/migration/Dockerfile',
          id: 'BuildMigrationImage',
          repositoryName: `workops-${props.stage}-migration`,
        }),
      ],
    });
    pipeline.buildPipeline();

    codePipeline.notifyOn('PipelineNotifications', notificationTopic, {
      detailType: DetailType.BASIC,
      events: [
        PipelineNotificationEvents.MANUAL_APPROVAL_NEEDED,
        PipelineNotificationEvents.PIPELINE_EXECUTION_FAILED,
      ],
    });

    new CfnOutput(this, 'pipelineName', {
      value: codePipeline.pipelineName,
    });
    new CfnOutput(this, 'artifactBucketName', {
      value: artifactBucket.bucketName,
    });
    new CfnOutput(this, 'githubConnectionName', {
      value: githubConnection.connectionName,
    });
    new CfnOutput(this, 'notificationTopicName', {
      value: notificationTopic.topicName,
    });
  }

  private createBuildEnvironment(): BuildEnvironment {
    return {
      buildImage: LinuxArmBuildImage.AMAZON_LINUX_2023_STANDARD_3_0,
      computeType: ComputeType.MEDIUM,
      privileged: true,
    };
  }

  private createBuildImageStep(
    source: CodePipelineSource,
    config: BuildImageConfig,
  ): CodeBuildStep {
    const registryUri = `${Aws.ACCOUNT_ID}.dkr.ecr.${Aws.REGION}.${Aws.URL_SUFFIX}`;
    const imageUri = `${registryUri}/${config.repositoryName}:$COMMIT_SHA`;
    const cacheUri = `${registryUri}/${config.cacheRepositoryName}:buildcache`;

    // Image build steps publish immutable commit images while keeping buildx cache mutable.
    return new CodeBuildStep(config.id, {
      buildEnvironment: this.createBuildEnvironment(),
      commands: [
        'aws ecr get-login-password --region "$AWS_DEFAULT_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URI"',
        'docker buildx create --name workops-builder --driver docker-container --use',
        'docker buildx inspect --bootstrap',
        'docker buildx build --platform linux/arm64 --file "$DOCKERFILE" --tag "$IMAGE_URI" --cache-from "$CACHE_FROM" --cache-to "$CACHE_TO" --load "$BUILD_CONTEXT"',
        'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock public.ecr.aws/aquasecurity/trivy:0.71.2 image --exit-code 1 --severity MEDIUM,HIGH,CRITICAL --no-progress "$IMAGE_URI"',
        'docker push "$IMAGE_URI"',
      ],
      env: {
        BUILD_CONTEXT: config.buildContext,
        CACHE_FROM: `type=registry,ref=${cacheUri}`,
        CACHE_TO: `type=registry,ref=${cacheUri},mode=max`,
        COMMIT_SHA: source.sourceAttribute('CommitId'),
        DOCKERFILE: config.dockerfile,
        IMAGE_URI: imageUri,
        REGISTRY_URI: registryUri,
      },
      input: source,
      rolePolicyStatements: [
        new PolicyStatement({
          actions: ['ecr:GetAuthorizationToken'],
          effect: Effect.ALLOW,
          resources: ['*'],
        }),
        new PolicyStatement({
          actions: [
            'ecr:BatchCheckLayerAvailability',
            'ecr:BatchGetImage',
            'ecr:CompleteLayerUpload',
            'ecr:GetDownloadUrlForLayer',
            'ecr:InitiateLayerUpload',
            'ecr:PutImage',
            'ecr:UploadLayerPart',
          ],
          effect: Effect.ALLOW,
          resources: [
            this.createEcrRepositoryArn(config.repositoryName),
            this.createEcrRepositoryArn(config.cacheRepositoryName),
          ],
        }),
      ],
    });
  }

  private createEcrRepositoryArn(repositoryName: string): string {
    return this.formatArn({
      service: 'ecr',
      resource: 'repository',
      resourceName: repositoryName,
    });
  }

  private restrictConnectionUseToRepositoryAndBranch(
    pipeline: CodePipelinePipeline,
    connectionArn: string,
    githubRepository: string,
  ): void {
    pipeline.addToRolePolicy(
      new PolicyStatement({
        actions: ['codeconnections:UseConnection', 'codestar-connections:UseConnection'],
        conditions: {
          StringNotEquals: {
            'codeconnections:FullRepositoryId': githubRepository,
          },
        },
        effect: Effect.DENY,
        resources: [connectionArn],
      }),
    );
    pipeline.addToRolePolicy(
      new PolicyStatement({
        actions: ['codeconnections:UseConnection', 'codestar-connections:UseConnection'],
        conditions: {
          StringNotEquals: {
            'codeconnections:BranchName': 'main',
          },
        },
        effect: Effect.DENY,
        resources: [connectionArn],
      }),
    );
  }
}

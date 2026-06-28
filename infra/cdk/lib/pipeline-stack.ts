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
import {
  BuildEnvironment,
  BuildSpec,
  ComputeType,
  IProject,
  LinuxArmBuildImage,
  Project as CodeBuildProject,
} from 'aws-cdk-lib/aws-codebuild';
import { DetailType } from 'aws-cdk-lib/aws-codestarnotifications';
import { CfnConnection } from 'aws-cdk-lib/aws-codestarconnections';
import {
  Artifact,
  PipelineNotificationEvents,
  PipelineType,
  IStage as CodePipelineStage,
  Pipeline as CodePipelinePipeline,
} from 'aws-cdk-lib/aws-codepipeline';
import {
  CodeBuildAction,
  CodeStarConnectionsSourceAction,
} from 'aws-cdk-lib/aws-codepipeline-actions';
import { Repository } from 'aws-cdk-lib/aws-ecr';
import { CfnServiceLinkedRole, Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Bucket, BucketEncryption } from 'aws-cdk-lib/aws-s3';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import {
  CodeBuildStep,
  CodePipeline,
  CodePipelineActionFactoryResult,
  CodePipelineFileSet,
  ICodePipelineActionFactory,
  ManualApprovalStep,
  ProduceActionOptions,
  Step,
} from 'aws-cdk-lib/pipelines';
import { Construct } from 'constructs';
import { AppRuntimeStack } from './app-runtime-stack';
import { RuntimeResources } from './app-runtime-stack';
import { DataStack } from './data-stack';
import { EdgeStack } from './edge-stack';
import { EgressStack } from './egress-stack';
import { FoundationStack } from './foundation-stack';
import { IdentityStack } from './identity-stack';
import { LogsStack } from './logs-stack';
import { MigrationStack } from './migration-stack';
import { RegistryStack } from './registry-stack';

export interface PipelineStackProps extends StackProps {
  env: Environment;
  stage: string;
  githubRepository: string;
  notificationEmail: string;
  webImageTag: string;
}

interface RegistryDeployStageProps {
  env: Environment;
  stage: string;
}

interface BuildImageConfig {
  cacheRepositoryName: string;
  commitSha: string;
  dockerfile: string;
  id: string;
  repositoryName: string;
  buildContext: string;
}

interface DataNetworkMigrationDeployStageProps {
  env: Environment;
  stage: string;
  webImageTag: string;
}

interface GitHubRepositoryName {
  owner: string;
  repo: string;
}

interface MigrationActionStepProps {
  commitSha: string;
  input: CodePipelineFileSet;
  project: IProject;
}

class MigrationActionStep extends Step implements ICodePipelineActionFactory {
  private readonly commitSha: string;
  private readonly input: CodePipelineFileSet;
  private readonly project: IProject;

  constructor(id: string, props: MigrationActionStepProps) {
    super(id);

    this.commitSha = props.commitSha;
    this.input = props.input;
    this.project = props.project;
    this.addDependencyFileSet(props.input);
  }

  public produceAction(
    stage: CodePipelineStage,
    options: ProduceActionOptions,
  ): CodePipelineActionFactoryResult {
    // The migration project is created in the target stage, but this action receives the pipeline source artifact directly.
    stage.addAction(
      new CodeBuildAction({
        actionName: options.actionName,
        environmentVariables: {
          COMMIT_SHA: {
            value: this.commitSha,
          },
        },
        input: options.artifacts.toCodePipeline(this.input),
        project: this.project,
        runOrder: options.runOrder,
        variablesNamespace: options.variablesNamespace,
      }),
    );

    return {
      runOrdersConsumed: 1,
    };
  }
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

class DataNetworkMigrationDeployStage extends Stage {
  public readonly appRuntimeStack: AppRuntimeStack;
  public readonly migrationStack: MigrationStack;

  constructor(scope: Construct, id: string, props: DataNetworkMigrationDeployStageProps) {
    super(scope, id, {
      env: props.env,
    });

    const foundationStack = new FoundationStack(this, 'FoundationStack', {
      env: props.env,
      stage: props.stage,
      stackName: `workops-${props.stage}-foundation`,
    });
    const dataStack = new DataStack(this, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      env: props.env,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      stage: props.stage,
      stackName: `workops-${props.stage}-data`,
      vpc: foundationStack.vpc,
    });
    const identityStack = new IdentityStack(this, 'IdentityStack', {
      env: props.env,
      stage: props.stage,
      stackName: `workops-${props.stage}-identity`,
    });
    const logsStack = new LogsStack(this, 'LogsStack', {
      env: props.env,
      stage: props.stage,
      stackName: `workops-${props.stage}-logs`,
    });
    const egressStack = new EgressStack(this, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      env: props.env,
      publicSubnets: foundationStack.publicSubnets,
      stage: props.stage,
      stackName: `workops-${props.stage}-egress`,
      vpc: foundationStack.vpc,
    });
    const edgeStack = new EdgeStack(this, 'EdgeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
      cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
      cognitoUserPoolId: identityStack.userPoolId,
      cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
      cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
      env: props.env,
      stage: props.stage,
      stackName: `workops-${props.stage}-edge`,
      vpc: foundationStack.vpc,
    });
    const webRepository = Repository.fromRepositoryName(
      foundationStack,
      'WebRepository',
      `workops-${props.stage}-web`,
    );

    // MigrationStack owns the VPC-attached CodeBuild project that runs Flyway against RDS.
    this.migrationStack = new MigrationStack(this, 'MigrationStack', {
      appSubnets: foundationStack.appSubnets,
      env: props.env,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      migrationLogGroup: logsStack.migrationLogGroup,
      stage: props.stage,
      stackName: `workops-${props.stage}-migration`,
      vpc: foundationStack.vpc,
    });

    edgeStack.addDependency(egressStack);
    edgeStack.addDependency(identityStack);
    edgeStack.addDependency(logsStack);
    this.migrationStack.addDependency(dataStack);
    this.migrationStack.addDependency(egressStack);
    this.migrationStack.addDependency(logsStack);

    const runtimeResources: RuntimeResources = {
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
      repository: webRepository,
      vpc: foundationStack.vpc,
      webLogGroup: logsStack.webLogGroup,
    };

    // AppRuntime consumes support resources through construct references within this CDK Stage.
    this.appRuntimeStack = new AppRuntimeStack(this, 'AppRuntimeStack', {
      env: props.env,
      runtimeResources,
      stage: props.stage,
      stackName: `workops-${props.stage}-app-runtime`,
      webImageTag: props.webImageTag,
    });

    this.appRuntimeStack.addDependency(this.migrationStack);
  }
}

export class PipelineStack extends Stack {
  constructor(scope: Construct, id: string, props: PipelineStackProps) {
    super(scope, id, props);

    const buildEnvironment = this.createBuildEnvironment();
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
      branch: 'main',
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
        commands: ['cd infra/cdk', 'npm ci', 'npm run build', 'npx cdk synth'],
        env: {
          GITHUB_REPOSITORY: props.githubRepository,
          WORKOPS_IMAGE_TAG: commitSha,
          WORKOPS_OPS_NOTIFICATION_EMAIL: props.notificationEmail,
          WORKOPS_STAGE: props.stage,
        },
        input: source,
        primaryOutputDirectory: 'infra/cdk/cdk.out',
        rolePolicyStatements: [
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
      partialBuildSpec: BuildSpec.fromObject({
        phases: {
          install: {
            'runtime-versions': {
              java: 'corretto25',
            },
          },
          build: {
            commands: [
              'cd apps/web',
              'java -version',
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
        env: props.env,
        stage: props.stage,
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
                `workops-${props.stage}-migration`,
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

    new CfnOutput(this, 'pipelineName', {
      value: codePipeline.pipelineName,
    });
    new CfnOutput(this, 'artifactBucketName', {
      value: artifactBucket.bucketName,
    });
    new CfnOutput(this, 'githubConnectionName', {
      value: githubConnection.connectionName,
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
    source: CodePipelineFileSet,
    config: BuildImageConfig,
  ): CodeBuildStep {
    const registryUri = `${Aws.ACCOUNT_ID}.dkr.ecr.${Aws.REGION}.${Aws.URL_SUFFIX}`;
    const imageRepositoryUri = `${registryUri}/${config.repositoryName}`;
    const cacheUri = `${registryUri}/${config.cacheRepositoryName}:buildcache`;

    // Image build steps publish immutable commit images while keeping buildx cache mutable.
    return new CodeBuildStep(config.id, {
      buildEnvironment: this.createBuildEnvironment(),
      commands: [
        'aws ecr get-login-password --region "$AWS_DEFAULT_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URI"',
        'docker buildx create --name workops-builder --driver docker-container --use',
        'docker buildx inspect --bootstrap',
        'export IMAGE_URI="$IMAGE_REPOSITORY_URI:$COMMIT_SHA"',
        'docker buildx build --platform linux/arm64 --file "$DOCKERFILE" --tag "$IMAGE_URI" --cache-from "$CACHE_FROM" --cache-to "$CACHE_TO" --load "$BUILD_CONTEXT"',
        'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock public.ecr.aws/aquasecurity/trivy:0.71.2 image --exit-code 1 --severity MEDIUM,HIGH,CRITICAL --no-progress "$IMAGE_URI"',
        'docker push "$IMAGE_URI"',
      ],
      env: {
        BUILD_CONTEXT: config.buildContext,
        CACHE_FROM: `type=registry,ref=${cacheUri}`,
        CACHE_TO: `type=registry,ref=${cacheUri},mode=max`,
        COMMIT_SHA: config.commitSha,
        DOCKERFILE: config.dockerfile,
        IMAGE_REPOSITORY_URI: imageRepositoryUri,
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

import {
  ArnFormat,
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
import { Repository } from 'aws-cdk-lib/aws-ecr';
import { CfnServiceLinkedRole, Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
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
import { AppRuntimeStack } from './app-runtime-stack';
import { ConfigStack } from './config-stack';
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
  migrationImageTag: string;
}

interface RegistryDeployStageProps {
  env: Environment;
  stage: string;
}

interface BuildImageConfig {
  cacheRepositoryName: string;
  dockerfile: string;
  id: string;
  repositoryName: string;
  buildContext: string;
}

interface DataNetworkMigrationDeployStageProps {
  env: Environment;
  stage: string;
  migrationImageTag: string;
}

interface AppRuntimeDeployStageProps {
  env: Environment;
  stage: string;
  webImageTag: string;
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
    const migrationRepository = Repository.fromRepositoryName(
      foundationStack,
      'MigrationRepository',
      `workops-${props.stage}-migration`,
    );

    // MigrationStack consumes the runtime network and log resources but owns only RunTask assets.
    this.migrationStack = new MigrationStack(this, 'MigrationStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cluster: foundationStack.ecsCluster,
      env: props.env,
      migrationImageTag: props.migrationImageTag,
      migrationLogGroup: logsStack.migrationLogGroup,
      migrationRepository,
      stage: props.stage,
      stackName: `workops-${props.stage}-migration`,
    });

    edgeStack.addDependency(egressStack);
    edgeStack.addDependency(identityStack);
    edgeStack.addDependency(logsStack);
    this.migrationStack.addDependency(dataStack);
    this.migrationStack.addDependency(egressStack);
    this.migrationStack.addDependency(logsStack);
  }
}

class AppRuntimeDeployStage extends Stage {
  constructor(scope: Construct, id: string, props: AppRuntimeDeployStageProps) {
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
      stage: props.stage,
      stackName: `workops-${props.stage}-data`,
      vpc: foundationStack.vpc,
    });
    const configStack = new ConfigStack(this, 'ConfigStack', {
      env: props.env,
      stage: props.stage,
      stackName: `workops-${props.stage}-config`,
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

    // AppRuntime deployment reuses support stack constructs so cross-stack references stay local.
    const appRuntimeStack = new AppRuntimeStack(this, 'AppRuntimeStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cloudFrontHttpsUrl: edgeStack.cloudFrontHttpsUrl,
      cluster: foundationStack.ecsCluster,
      cognitoHostedUiDomainBaseUrl: identityStack.hostedUiDomainBaseUrl,
      cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
      cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
      cognitoUserPoolId: identityStack.userPoolId,
      env: props.env,
      listener: edgeStack.listener,
      loadBalancerFullName: edgeStack.loadBalancer.loadBalancerFullName,
      repository: webRepository,
      stage: props.stage,
      stackName: `workops-${props.stage}-app-runtime`,
      vpc: foundationStack.vpc,
      webImageTag: props.webImageTag,
      webLogGroup: logsStack.webLogGroup,
    });

    edgeStack.addDependency(egressStack);
    edgeStack.addDependency(identityStack);
    edgeStack.addDependency(logsStack);
    appRuntimeStack.addDependency(dataStack);
    appRuntimeStack.addDependency(egressStack);
    appRuntimeStack.addDependency(edgeStack);
    appRuntimeStack.addDependency(identityStack);
    appRuntimeStack.addDependency(configStack);
    appRuntimeStack.addDependency(logsStack);
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
          WORKOPS_IMAGE_TAG: source.sourceAttribute('CommitId'),
          WORKOPS_PIPELINE_NOTIFICATION_EMAIL: props.notificationEmail,
          WORKOPS_STAGE: props.stage,
        },
        input: source,
        primaryOutputDirectory: 'infra/cdk/cdk.out',
        rolePolicyStatements: [
          new PolicyStatement({
            actions: [
              'ec2:DescribeAvailabilityZones',
              'ec2:DescribeManagedPrefixLists',
              'ec2:GetManagedPrefixListEntries',
            ],
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
    const dataNetworkMigrationStage = new DataNetworkMigrationDeployStage(
      this,
      'DeployDataNetworkMigration',
      {
        env: props.env,
        migrationImageTag: props.migrationImageTag,
        stage: props.stage,
      },
    );
    pipeline.addStage(dataNetworkMigrationStage);
    pipeline.addWave('MigrationRunTask', {
      pre: [this.createMigrationRunTaskStep(dataNetworkMigrationStage.migrationStack, props.stage)],
    });
    pipeline.addStage(
      new AppRuntimeDeployStage(this, 'DeployAppRuntime', {
        env: props.env,
        stage: props.stage,
        webImageTag: props.webImageTag,
      }),
    );
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

  private createMigrationRunTaskStep(migrationStack: MigrationStack, stage: string): CodeBuildStep {
    return new CodeBuildStep('RunMigration', {
      buildEnvironment: this.createBuildEnvironment(),
      commands: ['set -euo pipefail', 'python3 infra/cdk/scripts/run-migration-task.py'],
      envFromCfnOutputs: {
        MIGRATION_CLUSTER_NAME: migrationStack.migrationClusterNameOutput,
        MIGRATION_CONTAINER_NAME: migrationStack.migrationContainerNameOutput,
        MIGRATION_SECURITY_GROUP_ID: migrationStack.migrationSecurityGroupIdOutput,
        MIGRATION_SUBNET_IDS: migrationStack.migrationSubnetIdsOutput,
        MIGRATION_TASK_DEFINITION_ARN: migrationStack.migrationTaskDefinitionArnOutput,
      },
      rolePolicyStatements: [
        new PolicyStatement({
          actions: ['ecs:RunTask'],
          effect: Effect.ALLOW,
          resources: [this.createEcsTaskDefinitionArn(`workops-${stage}-migration:*`)],
        }),
        new PolicyStatement({
          actions: ['ecs:DescribeTasks'],
          effect: Effect.ALLOW,
          resources: ['*'],
        }),
        new PolicyStatement({
          actions: ['iam:PassRole'],
          conditions: {
            StringEquals: {
              'iam:PassedToService': 'ecs-tasks.amazonaws.com',
            },
          },
          effect: Effect.ALLOW,
          resources: [
            this.createIamRoleArn(`workops-${stage}-migration-execution`),
            this.createIamRoleArn(`workops-${stage}-migration-task`),
          ],
        }),
      ],
    });
  }

  private createEcsTaskDefinitionArn(taskDefinitionName: string): string {
    return this.formatArn({
      service: 'ecs',
      resource: 'task-definition',
      resourceName: taskDefinitionName,
      arnFormat: ArnFormat.SLASH_RESOURCE_NAME,
    });
  }

  private createIamRoleArn(roleName: string): string {
    return this.formatArn({
      service: 'iam',
      region: '',
      resource: 'role',
      resourceName: roleName,
      arnFormat: ArnFormat.SLASH_RESOURCE_NAME,
    });
  }
}

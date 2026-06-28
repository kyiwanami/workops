import {
  Aws,
  CfnOutput,
  Duration,
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
import { DataPauseStack } from './data-pause-stack';
import { DataStack } from './data-stack';
import { EgressStack } from './egress-stack';
import { readWorkopsStage, workopsStackName } from './environment';
import { FoundationStack } from './foundation-stack';
import { IdentityStack } from './identity-stack';
import { LogsStack } from './logs-stack';
import { MigrationRunnerStack } from './migration-runner-stack';
import { RegistryStack } from './registry-stack';
import { WebAclStack } from './web-acl-stack';
import { WebDeliveryStack } from './web-delivery-stack';
import { WebIngressStack } from './web-ingress-stack';

export interface PipelineStackProps extends StackProps {
  stage: string;
  githubRepository: string;
  notificationEmail: string;
  webImageTag: string;
}

interface BuildImageConfig {
  cacheRepositoryName: string;
  commitSha: string;
  dockerfile: string;
  id: string;
  repositoryName: string;
  buildContext: string;
}

interface CodeArtifactParameterNames {
  domainName: string;
  mavenRepositoryName: string;
  npmRepositoryName: string;
}

interface DataNetworkMigrationDeployStageProps {
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
  constructor(scope: Construct, id: string) {
    super(scope, id, {
      env: {
        account: Stack.of(scope).account,
        region: Stack.of(scope).region,
      },
    });

    new RegistryStack(this, 'RegistryStack', {});
  }
}

class DataNetworkMigrationDeployStage extends Stage {
  public readonly appRuntimeStack: AppRuntimeStack;
  public readonly migrationRunnerStack: MigrationRunnerStack;

  constructor(scope: Construct, id: string, props: DataNetworkMigrationDeployStageProps) {
    const stage = readWorkopsStage(scope);
    super(scope, id, {
      env: {
        account: Stack.of(scope).account,
        region: Stack.of(scope).region,
      },
    });

    const foundationStack = new FoundationStack(this, 'FoundationStack', {
    });
    const dataStack = new DataStack(this, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      vpc: foundationStack.vpc,
    });
    const identityStack = new IdentityStack(this, 'IdentityStack', {});
    const logsStack = new LogsStack(this, 'LogsStack', {});
    const dataPauseStack = new DataPauseStack(this, 'DataPauseStack', {
      markAutoRestartLogGroup: logsStack.dataPauseMarkAutoRestartLogGroup,
      stopMarkedDbLogGroup: logsStack.dataPauseStopMarkedDbLogGroup,
    });
    const egressStack = new EgressStack(this, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      publicSubnets: foundationStack.publicSubnets,
      vpc: foundationStack.vpc,
    });
    const webAclStack = new WebAclStack(this, 'WebAclStack', {
      crossRegionReferences: true,
      env: {
        account: Stack.of(scope).account,
        region: 'us-east-1',
      },
    });
    const webIngressStack = new WebIngressStack(this, 'WebIngressStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      vpc: foundationStack.vpc,
    });
    const webDeliveryStack = new WebDeliveryStack(this, 'WebDeliveryStack', {
      cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
      cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
      cognitoUserPoolId: identityStack.userPoolId,
      cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
      cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
      crossRegionReferences: true,
      webAclArn: webAclStack.webAclArn,
    });
    const webRepository = Repository.fromRepositoryName(
      foundationStack,
      'WebRepository',
      `workops-${stage}-web`,
    );

    // MigrationRunnerStack owns the VPC-attached CodeBuild project that runs Flyway against RDS.
    this.migrationRunnerStack = new MigrationRunnerStack(this, 'MigrationRunnerStack', {
      appSubnets: foundationStack.appSubnets,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      migrationLogGroup: logsStack.migrationLogGroup,
      vpc: foundationStack.vpc,
    });

    webIngressStack.addDependency(egressStack);
    webDeliveryStack.addDependency(webAclStack);
    webDeliveryStack.addDependency(webIngressStack);
    webDeliveryStack.addDependency(identityStack);
    webDeliveryStack.addDependency(logsStack);
    dataPauseStack.addDependency(logsStack);
    this.migrationRunnerStack.addDependency(dataStack);
    this.migrationRunnerStack.addDependency(egressStack);
    this.migrationRunnerStack.addDependency(logsStack);

    const runtimeResources: RuntimeResources = {
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
      repository: webRepository,
      vpc: foundationStack.vpc,
      webLogGroup: logsStack.webLogGroup,
    };

    // AppRuntime consumes support resources through construct references within this CDK Stage.
    this.appRuntimeStack = new AppRuntimeStack(this, 'AppRuntimeStack', {
      runtimeResources,
      webImageTag: props.webImageTag,
    });

    this.appRuntimeStack.addDependency(this.migrationRunnerStack);
  }
}

export class PipelineStack extends Stack {
  constructor(scope: Construct, id: string, props: PipelineStackProps) {
    super(scope, id, {
      ...props,
      stackName: workopsStackName(scope, 'pipeline'),
    });

    const buildEnvironment = this.createBuildEnvironment();
    const codeArtifactParameterNames = this.createCodeArtifactParameterNames(props.stage);
    const codeArtifactPolicyStatements = this.createCodeArtifactPolicyStatements(props.stage);
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
            'parameter-store': this.createCodeArtifactParameterStoreEnvironment(
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
          'parameter-store': this.createCodeArtifactParameterStoreEnvironment(
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
        this.createBuildImageStep(source, {
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
    config: BuildImageConfig & {
      codeArtifactParameterNames: CodeArtifactParameterNames;
      codeArtifactPolicyStatements: PolicyStatement[];
    },
  ): CodeBuildStep {
    const registryUri = `${Aws.ACCOUNT_ID}.dkr.ecr.${Aws.REGION}.${Aws.URL_SUFFIX}`;
    const imageRepositoryUri = `${registryUri}/${config.repositoryName}`;
    const cacheUri = `${registryUri}/${config.cacheRepositoryName}:buildcache`;

    // Image build steps publish immutable commit images while keeping buildx cache mutable.
    return new CodeBuildStep(config.id, {
      buildEnvironment: this.createBuildEnvironment(),
      commands: [
        'mkdir -p "$CODEBUILD_SRC_DIR/.workops-codeartifact"',
        'export WORKOPS_MAVEN_SETTINGS_PATH="$CODEBUILD_SRC_DIR/.workops-codeartifact/settings.xml"',
        'export WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH="$CODEBUILD_SRC_DIR/.workops-codeartifact/codeartifact-token"',
        'python3 infra/cdk/scripts/configure-codeartifact-maven.py',
        'aws ecr get-login-password --region "$AWS_DEFAULT_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URI"',
        'docker buildx create --name workops-builder --driver docker-container --use',
        'docker buildx inspect --bootstrap',
        'export IMAGE_URI="$IMAGE_REPOSITORY_URI:$COMMIT_SHA"',
        'docker buildx build --platform linux/arm64 --file "$DOCKERFILE" --tag "$IMAGE_URI" --secret id=maven_settings,src="$WORKOPS_MAVEN_SETTINGS_PATH" --secret id=codeartifact_token,src="$WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH" --cache-from "$CACHE_FROM" --cache-to "$CACHE_TO" --load "$BUILD_CONTEXT"',
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
      partialBuildSpec: BuildSpec.fromObject({
        env: {
          'parameter-store': this.createCodeArtifactParameterStoreEnvironment(
            config.codeArtifactParameterNames,
          ),
        },
        version: '0.2',
      }),
      rolePolicyStatements: [
        ...config.codeArtifactPolicyStatements,
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

  private createCodeArtifactParameterNames(stage: string): CodeArtifactParameterNames {
    return {
      domainName: `/workops/${stage}/dependencies/codeartifact/domain-name`,
      mavenRepositoryName: `/workops/${stage}/dependencies/codeartifact/maven-repository-name`,
      npmRepositoryName: `/workops/${stage}/dependencies/codeartifact/npm-repository-name`,
    };
  }

  private createCodeArtifactParameterStoreEnvironment(
    parameterNames: CodeArtifactParameterNames,
  ): Record<string, string> {
    return {
      WORKOPS_CODEARTIFACT_DOMAIN_NAME: parameterNames.domainName,
      WORKOPS_CODEARTIFACT_MAVEN_REPOSITORY_NAME: parameterNames.mavenRepositoryName,
      WORKOPS_CODEARTIFACT_NPM_REPOSITORY_NAME: parameterNames.npmRepositoryName,
    };
  }

  private createCodeArtifactPolicyStatements(stage: string): PolicyStatement[] {
    const domainName = `workops-${stage}`;
    const npmRepositoryName = `workops-${stage}-npm`;
    const mavenRepositoryName = `workops-${stage}-maven`;
    return [
      new PolicyStatement({
        actions: ['ssm:GetParameters'],
        effect: Effect.ALLOW,
        resources: [
          this.formatArn({
            service: 'ssm',
            resource: 'parameter',
            resourceName: `workops/${stage}/dependencies/codeartifact/*`,
          }),
        ],
      }),
      new PolicyStatement({
        actions: ['codeartifact:GetAuthorizationToken'],
        effect: Effect.ALLOW,
        resources: [
          this.formatArn({
            service: 'codeartifact',
            resource: 'domain',
            resourceName: domainName,
          }),
        ],
      }),
      new PolicyStatement({
        actions: ['codeartifact:GetRepositoryEndpoint', 'codeartifact:ReadFromRepository'],
        effect: Effect.ALLOW,
        resources: [
          this.formatArn({
            service: 'codeartifact',
            resource: 'repository',
            resourceName: `${domainName}/${npmRepositoryName}`,
          }),
          this.formatArn({
            service: 'codeartifact',
            resource: 'repository',
            resourceName: `${domainName}/${mavenRepositoryName}`,
          }),
        ],
      }),
      new PolicyStatement({
        actions: ['sts:GetServiceBearerToken'],
        conditions: {
          StringEquals: {
            'sts:AWSServiceName': 'codeartifact.amazonaws.com',
          },
        },
        effect: Effect.ALLOW,
        resources: ['*'],
      }),
    ];
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

import { Aws, Stack } from 'aws-cdk-lib';
import {
  BuildEnvironment,
  BuildSpec,
  ComputeType,
  LinuxArmBuildImage,
} from 'aws-cdk-lib/aws-codebuild';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { CodeBuildStep, CodePipelineFileSet } from 'aws-cdk-lib/pipelines';

export interface CodeArtifactParameterNames {
  domainName: string;
  mavenRepositoryName: string;
  npmRepositoryName: string;
}

interface BuildImageStepConfig {
  buildContext: string;
  cacheRepositoryName: string;
  codeArtifactParameterNames: CodeArtifactParameterNames;
  codeArtifactPolicyStatements: PolicyStatement[];
  commitSha: string;
  dockerfile: string;
  id: string;
  repositoryName: string;
}

export function createBuildEnvironment(): BuildEnvironment {
  return {
    buildImage: LinuxArmBuildImage.AMAZON_LINUX_2023_STANDARD_3_0,
    computeType: ComputeType.MEDIUM,
    privileged: true,
  };
}

export function createBuildImageStep(
  stack: Stack,
  source: CodePipelineFileSet,
  config: BuildImageStepConfig,
): CodeBuildStep {
  const registryUri = `${Aws.ACCOUNT_ID}.dkr.ecr.${Aws.REGION}.${Aws.URL_SUFFIX}`;
  const imageRepositoryUri = `${registryUri}/${config.repositoryName}`;
  const cacheUri = `${registryUri}/${config.cacheRepositoryName}:buildcache`;

  // Image build steps publish immutable commit images while keeping buildx cache mutable.
  return new CodeBuildStep(config.id, {
    buildEnvironment: createBuildEnvironment(),
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
        'parameter-store': createCodeArtifactParameterStoreEnvironment(
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
          createEcrRepositoryArn(stack, config.repositoryName),
          createEcrRepositoryArn(stack, config.cacheRepositoryName),
        ],
      }),
    ],
  });
}

export function createCodeArtifactParameterNames(stage: string): CodeArtifactParameterNames {
  return {
    domainName: `/workops/${stage}/dependencies/codeartifact/domain-name`,
    mavenRepositoryName: `/workops/${stage}/dependencies/codeartifact/maven-repository-name`,
    npmRepositoryName: `/workops/${stage}/dependencies/codeartifact/npm-repository-name`,
  };
}

export function createCodeArtifactParameterStoreEnvironment(
  parameterNames: CodeArtifactParameterNames,
): Record<string, string> {
  return {
    WORKOPS_CODEARTIFACT_DOMAIN_NAME: parameterNames.domainName,
    WORKOPS_CODEARTIFACT_MAVEN_REPOSITORY_NAME: parameterNames.mavenRepositoryName,
    WORKOPS_CODEARTIFACT_NPM_REPOSITORY_NAME: parameterNames.npmRepositoryName,
  };
}

export function createCodeArtifactPolicyStatements(
  stack: Stack,
  stage: string,
): PolicyStatement[] {
  const domainName = `workops-${stage}`;
  const npmRepositoryName = `workops-${stage}-npm`;
  const mavenRepositoryName = `workops-${stage}-maven`;
  return [
    new PolicyStatement({
      actions: ['ssm:GetParameters'],
      effect: Effect.ALLOW,
      resources: [
        stack.formatArn({
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
        stack.formatArn({
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
        stack.formatArn({
          service: 'codeartifact',
          resource: 'repository',
          resourceName: `${domainName}/${npmRepositoryName}`,
        }),
        stack.formatArn({
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

function createEcrRepositoryArn(stack: Stack, repositoryName: string): string {
  return stack.formatArn({
    service: 'ecr',
    resource: 'repository',
    resourceName: repositoryName,
  });
}

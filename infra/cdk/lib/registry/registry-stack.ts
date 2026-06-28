import { Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { Repository, TagMutability, TagStatus } from 'aws-cdk-lib/aws-ecr';
import { Construct } from 'constructs';
import { readStage, stackName } from '../shared/environment';
import { createParameter } from '../shared/ssm-parameters';

export class RegistryStack extends Stack {
  public readonly webRepository: Repository;
  public readonly webCacheRepository: Repository;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'registry'),
    });

    // Application image repositories keep immutable commit SHA tags for Pipeline traceability.
    this.webRepository = this.createImageRepository('WebRepository', `workops-${stage}-web`);

    // Build cache repositories keep mutable buildcache tags for docker buildx cache reuse.
    this.webCacheRepository = this.createCacheRepository(
      'WebCacheRepository',
      `workops-${stage}-web-cache`,
    );

    createParameter(
      this,
      'WebRepositoryNameParameter',
      'registry/web-repository-name',
      this.webRepository.repositoryName,
    );
    createParameter(
      this,
      'WebRepositoryUriParameter',
      'registry/web-repository-uri',
      this.webRepository.repositoryUri,
    );
    createParameter(
      this,
      'WebCacheRepositoryNameParameter',
      'registry/web-cache-repository-name',
      this.webCacheRepository.repositoryName,
    );
  }

  private createImageRepository(id: string, repositoryName: string): Repository {
    return new Repository(this, id, {
      repositoryName,
      imageTagMutability: TagMutability.IMMUTABLE,
      removalPolicy: RemovalPolicy.DESTROY,
      emptyOnDelete: true,
      lifecycleRules: [
        {
          description: 'Keep the latest ten tagged application images',
          tagStatus: TagStatus.TAGGED,
          tagPatternList: ['*'],
          maxImageCount: 10,
        },
        {
          description: 'Delete untagged application images after one day',
          tagStatus: TagStatus.UNTAGGED,
          maxImageAge: Duration.days(1),
        },
      ],
    });
  }

  private createCacheRepository(id: string, repositoryName: string): Repository {
    return new Repository(this, id, {
      repositoryName,
      imageTagMutability: TagMutability.MUTABLE,
      removalPolicy: RemovalPolicy.DESTROY,
      emptyOnDelete: true,
      lifecycleRules: [
        {
          description: 'Keep the latest five tagged build cache images',
          tagStatus: TagStatus.TAGGED,
          tagPatternList: ['*'],
          maxImageCount: 5,
        },
        {
          description: 'Delete untagged build cache images after one day',
          tagStatus: TagStatus.UNTAGGED,
          maxImageAge: Duration.days(1),
        },
      ],
    });
  }
}

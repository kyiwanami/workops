import { CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { Repository, TagMutability, TagStatus } from 'aws-cdk-lib/aws-ecr';
import { Construct } from 'constructs';

export interface RegistryStackProps extends StackProps {
  stage: string;
}

export class RegistryStack extends Stack {
  public readonly webRepository: Repository;
  public readonly migrationRepository: Repository;
  public readonly webCacheRepository: Repository;
  public readonly migrationCacheRepository: Repository;

  constructor(scope: Construct, id: string, props: RegistryStackProps) {
    super(scope, id, props);

    // Application image repositories keep immutable commit SHA tags for Pipeline traceability.
    this.webRepository = this.createImageRepository('WebRepository', `workops-${props.stage}-web`);
    this.migrationRepository = this.createImageRepository(
      'MigrationRepository',
      `workops-${props.stage}-migration`,
    );

    // Build cache repositories keep mutable buildcache tags for docker buildx cache reuse.
    this.webCacheRepository = this.createCacheRepository(
      'WebCacheRepository',
      `workops-${props.stage}-web-cache`,
    );
    this.migrationCacheRepository = this.createCacheRepository(
      'MigrationCacheRepository',
      `workops-${props.stage}-migration-cache`,
    );

    this.outputRepository('webRepository', this.webRepository);
    this.outputRepository('migrationRepository', this.migrationRepository);
    this.outputRepository('webCacheRepository', this.webCacheRepository);
    this.outputRepository('migrationCacheRepository', this.migrationCacheRepository);
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

  private outputRepository(outputPrefix: string, repository: Repository): void {
    new CfnOutput(this, `${outputPrefix}Name`, {
      value: repository.repositoryName,
    });
    new CfnOutput(this, `${outputPrefix}Uri`, {
      value: repository.repositoryUri,
    });
  }
}

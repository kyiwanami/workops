import { CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { Repository, TagStatus } from 'aws-cdk-lib/aws-ecr';
import { Construct } from 'constructs';

export interface RegistryStackProps extends StackProps {
  stage: string;
}

export class RegistryStack extends Stack {
  public readonly repository: Repository;

  constructor(scope: Construct, id: string, props: RegistryStackProps) {
    super(scope, id, props);

    // The web image repository is disposable because images are rebuildable artifacts.
    this.repository = new Repository(this, 'WebRepository', {
      repositoryName: `workops-${props.stage}-web`,
      removalPolicy: RemovalPolicy.DESTROY,
      emptyOnDelete: true,
      lifecycleRules: [
        {
          description: 'Keep the latest two tagged web images',
          tagStatus: TagStatus.TAGGED,
          tagPatternList: ['*'],
          maxImageCount: 2,
        },
        {
          description: 'Delete untagged web images after one day',
          tagStatus: TagStatus.UNTAGGED,
          maxImageAge: Duration.days(1),
        },
      ],
    });

    new CfnOutput(this, 'repositoryName', {
      value: this.repository.repositoryName,
    });
    new CfnOutput(this, 'repositoryUri', {
      value: this.repository.repositoryUri,
    });
  }
}

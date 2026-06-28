import { RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { CfnDomain, CfnRepository } from 'aws-cdk-lib/aws-codeartifact';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { EmailSubscription } from 'aws-cdk-lib/aws-sns-subscriptions';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface DependencyStackProps extends StackProps {
  stage: string;
  notificationEmail: string;
}

export class DependencyStack extends Stack {
  constructor(scope: Construct, id: string, props: DependencyStackProps) {
    super(scope, id, props);

    const domainName = `workops-${props.stage}`;
    const npmRepositoryName = `workops-${props.stage}-npm`;
    const mavenRepositoryName = `workops-${props.stage}-maven`;

    // DependencyStack owns shared dependency services used before runtime stacks are recreated.
    const domain = new CfnDomain(this, 'CodeArtifactDomain', {
      domainName,
    });
    domain.applyRemovalPolicy(RemovalPolicy.DESTROY);

    const npmRepository = new CfnRepository(this, 'NpmRepository', {
      domainName,
      externalConnections: ['public:npmjs'],
      repositoryName: npmRepositoryName,
    });
    npmRepository.applyRemovalPolicy(RemovalPolicy.DESTROY);
    npmRepository.node.addDependency(domain);

    const mavenRepository = new CfnRepository(this, 'MavenRepository', {
      domainName,
      externalConnections: ['public:maven-central'],
      repositoryName: mavenRepositoryName,
    });
    mavenRepository.applyRemovalPolicy(RemovalPolicy.DESTROY);
    mavenRepository.node.addDependency(domain);

    const opsTopic = new Topic(this, 'OpsNotificationTopic', {
      topicName: `workops-${props.stage}-ops-notifications`,
    });
    opsTopic.addSubscription(new EmailSubscription(props.notificationEmail));

    this.createParameter(
      'SpringProfileParameter',
      `/workops/${props.stage}/dependencies/runtime/spring-profile`,
      'dev',
    );
    this.createParameter(
      'CodeArtifactDomainNameParameter',
      `/workops/${props.stage}/dependencies/codeartifact/domain-name`,
      domainName,
    );
    this.createParameter(
      'CodeArtifactNpmRepositoryNameParameter',
      `/workops/${props.stage}/dependencies/codeartifact/npm-repository-name`,
      npmRepositoryName,
    );
    this.createParameter(
      'CodeArtifactMavenRepositoryNameParameter',
      `/workops/${props.stage}/dependencies/codeartifact/maven-repository-name`,
      mavenRepositoryName,
    );
    this.createParameter(
      'OpsTopicArnParameter',
      `/workops/${props.stage}/dependencies/notifications/ops-topic-arn`,
      opsTopic.topicArn,
    );
  }

  private createParameter(id: string, parameterName: string, stringValue: string): void {
    const parameter = new StringParameter(this, id, {
      parameterName,
      stringValue,
    });
    parameter.applyRemovalPolicy(RemovalPolicy.DESTROY);
  }
}

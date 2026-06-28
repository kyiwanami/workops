import { RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { CfnDomain, CfnRepository } from 'aws-cdk-lib/aws-codeartifact';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { EmailSubscription } from 'aws-cdk-lib/aws-sns-subscriptions';
import { Construct } from 'constructs';
import { readStage, stackName } from '../shared/environment';
import { createParameter } from '../shared/ssm-parameters';

export interface DependencyStackProps extends StackProps {
  notificationEmail: string;
}

export class DependencyStack extends Stack {
  constructor(scope: Construct, id: string, props: DependencyStackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'dependency'),
    });

    const domainName = `workops-${stage}`;
    const npmRepositoryName = `workops-${stage}-npm`;
    const mavenRepositoryName = `workops-${stage}-maven`;

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
      topicName: `workops-${stage}-ops-notifications`,
    });
    opsTopic.addSubscription(new EmailSubscription(props.notificationEmail));

    createParameter(
      this,
      'SpringProfileParameter',
      'dependencies/runtime/spring-profile',
      'dev',
    );
    createParameter(
      this,
      'CodeArtifactDomainNameParameter',
      'dependencies/codeartifact/domain-name',
      domainName,
    );
    createParameter(
      this,
      'CodeArtifactNpmRepositoryNameParameter',
      'dependencies/codeartifact/npm-repository-name',
      npmRepositoryName,
    );
    createParameter(
      this,
      'CodeArtifactMavenRepositoryNameParameter',
      'dependencies/codeartifact/maven-repository-name',
      mavenRepositoryName,
    );
    createParameter(
      this,
      'OpsTopicArnParameter',
      'dependencies/notifications/ops-topic-arn',
      opsTopic.topicArn,
    );
  }
}

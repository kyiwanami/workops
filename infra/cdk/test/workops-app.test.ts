import { App, Stack, Tags } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { readFileSync } from 'fs';
import { join } from 'path';
import { Construct } from 'constructs';
import { ConfigStack } from '../lib/config-stack';
import { FoundationStack } from '../lib/foundation-stack';
import { LogsStack } from '../lib/logs-stack';
import { RegistryStack } from '../lib/registry-stack';

class TaggedResourceStack extends Stack {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    // A concrete test resource makes stack-level tags visible in assertions.
    new Topic(this, 'TaggedTopic');
  }
}

describe('WorkOps CDK app', () => {
  test('creates Phase 2 base stack shells using the requested stage', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const configStack = new ConfigStack(app, 'ConfigStack', {
      stackName: `workops-${stage}-config`,
    });
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      stage,
      stackName: `workops-${stage}-registry`,
    });
    const logsStack = new LogsStack(app, 'LogsStack', {
      stage,
      stackName: `workops-${stage}-logs`,
    });

    expect(foundationStack.stackName).toBe('workops-dev-foundation');
    expect(configStack.stackName).toBe('workops-dev-config');
    expect(registryStack.stackName).toBe('workops-dev-registry');
    expect(logsStack.stackName).toBe('workops-dev-logs');
  });

  test('creates the FoundationStack network and cluster resources', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const template = Template.fromStack(foundationStack);

    template.resourceCountIs('AWS::EC2::VPC', 1);
    template.hasResourceProperties('AWS::EC2::VPC', {
      CidrBlock: '10.0.0.0/16',
      Tags: Match.arrayWith([
        {
          Key: 'Name',
          Value: 'workops-dev-vpc',
        },
      ]),
    });
    template.resourceCountIs('AWS::EC2::Subnet', 6);
    template.resourceCountIs('AWS::EC2::NatGateway', 0);
    template.resourceCountIs('AWS::EC2::SecurityGroup', 3);
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-alb-sg',
    });
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-app-sg',
    });
    template.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupName: 'workops-dev-db-sg',
    });
    template.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    template.hasResourceProperties('AWS::ECS::Cluster', {
      ClusterName: 'workops-dev-cluster',
    });
    template.hasOutput('vpcId', {});
    template.hasOutput('publicSubnetIds', {});
    template.hasOutput('appSubnetIds', {});
    template.hasOutput('dbSubnetIds', {});
    template.hasOutput('ecsClusterName', {});
    template.hasOutput('albSecurityGroupId', {});
    template.hasOutput('appSecurityGroupId', {});
    template.hasOutput('dbSecurityGroupId', {});
  });

  test('applies common WorkOps tags', () => {
    const app = new App();
    const stage = 'dev';

    // CDK app tags mirror the entrypoint's local and CI deploy behavior.
    Tags.of(app).add('Project', 'WorkOps');
    Tags.of(app).add('Environment', stage);
    Tags.of(app).add('ManagedBy', 'CDK');

    const stack = new TaggedResourceStack(app, 'TaggedResourceStack');
    const template = Template.fromStack(stack);

    template.hasResourceProperties('AWS::SNS::Topic', {
      Tags: Match.arrayWith([
        {
          Key: 'Project',
          Value: 'WorkOps',
        },
      ]),
    });
    template.hasResourceProperties('AWS::SNS::Topic', {
      Tags: Match.arrayWith([
        {
          Key: 'Environment',
          Value: 'dev',
        },
      ]),
    });
    template.hasResourceProperties('AWS::SNS::Topic', {
      Tags: Match.arrayWith([
        {
          Key: 'ManagedBy',
          Value: 'CDK',
        },
      ]),
    });
  });

  test('creates the RegistryStack repository and lifecycle policy', () => {
    const app = new App();
    const stage = 'dev';
    const registryStack = new RegistryStack(app, 'RegistryStack', {
      stage,
      stackName: `workops-${stage}-registry`,
    });
    const template = Template.fromStack(registryStack);

    template.resourceCountIs('AWS::ECR::Repository', 1);
    template.hasResourceProperties('AWS::ECR::Repository', {
      RepositoryName: 'workops-dev-web',
      EmptyOnDelete: true,
      LifecyclePolicy: {
        LifecyclePolicyText: Match.serializedJson(
          Match.objectLike({
            rules: Match.arrayWith([
              Match.objectLike({
                selection: Match.objectLike({
                  tagStatus: 'tagged',
                  tagPatternList: ['*'],
                  countType: 'imageCountMoreThan',
                  countNumber: 2,
                }),
              }),
              Match.objectLike({
                selection: Match.objectLike({
                  tagStatus: 'untagged',
                  countType: 'sinceImagePushed',
                  countUnit: 'days',
                  countNumber: 1,
                }),
              }),
            ]),
          }),
        ),
      },
    });
    template.hasResource('AWS::ECR::Repository', {
      Properties: {
        RepositoryName: 'workops-dev-web',
        EmptyOnDelete: true,
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasOutput('repositoryName', {});
    template.hasOutput('repositoryUri', {});
  });

  test('creates the LogsStack log groups', () => {
    const app = new App();
    const stage = 'dev';
    const logsStack = new LogsStack(app, 'LogsStack', {
      stage,
      stackName: `workops-${stage}-logs`,
    });
    const template = Template.fromStack(logsStack);

    template.resourceCountIs('AWS::Logs::LogGroup', 2);
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/web',
      RetentionInDays: 7,
    });
    template.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/migration',
      RetentionInDays: 7,
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/web',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResource('AWS::Logs::LogGroup', {
      Properties: {
        LogGroupName: '/workops/dev/migration',
      },
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasOutput('webLogGroupName', {});
    template.hasOutput('migrationLogGroupName', {});
  });

  test('keeps the ConfigStack empty in P2-1', () => {
    const app = new App();
    const stage = 'dev';
    const configStack = new ConfigStack(app, 'ConfigStack', {
      stackName: `workops-${stage}-config`,
    });
    const template = Template.fromStack(configStack);

    template.resourceCountIs('AWS::SSM::Parameter', 0);
  });

  test('keeps npm scripts minimal and independent from dotenv', () => {
    const packageJsonPath = join(__dirname, '..', 'package.json');
    const entrypointPath = join(__dirname, '..', 'bin', 'cdk.ts');
    const packageJsonText = readFileSync(packageJsonPath, 'utf8');
    const entrypointText = readFileSync(entrypointPath, 'utf8');

    expect(packageJsonText).toContain('"build": "tsc"');
    expect(packageJsonText).toContain('"watch": "tsc -w"');
    expect(packageJsonText).toContain('"test": "jest"');
    expect(packageJsonText).toContain('"cdk": "cdk"');
    expect(packageJsonText).not.toContain('synth:dev');
    expect(packageJsonText).not.toContain('diff:dev');
    expect(packageJsonText).not.toContain('deploy:dev');
    expect(packageJsonText).not.toContain('dotenv');
    expect(entrypointText).toContain('WORKOPS_STAGE');
    expect(entrypointText).not.toContain('tryGetContext');
    expect(entrypointText).not.toContain('dotenv');
    expect(entrypointText).not.toContain('.env.local');
  });
});

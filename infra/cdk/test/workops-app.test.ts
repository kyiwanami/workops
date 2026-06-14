import { App, Stack, Tags } from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { readFileSync } from 'fs';
import { join } from 'path';
import { Construct } from 'constructs';
import { ConfigStack } from '../lib/config-stack';
import { DataStack } from '../lib/data-stack';
import { FoundationStack } from '../lib/foundation-stack';
import { LogsStack } from '../lib/logs-stack';
import { RegistryStack } from '../lib/registry-stack';
import { SecretStack } from '../lib/secret-stack';

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
    const secretStack = new SecretStack(app, 'SecretStack', {
      stackName: `workops-${stage}-secret`,
    });
    const dataStack = new DataStack(app, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      stage,
      stackName: `workops-${stage}-data`,
      vpc: foundationStack.vpc,
    });
    const configStack = new ConfigStack(app, 'ConfigStack', {
      dbEndpointAddress: dataStack.endpointAddress,
      dbName: dataStack.databaseName,
      dbPort: dataStack.databasePort,
      stage,
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
    expect(secretStack.stackName).toBe('workops-dev-secret');
    expect(dataStack.stackName).toBe('workops-dev-data');
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

  test('creates the SecretStack without secret resources in P2-2-01', () => {
    const app = new App();
    const stage = 'dev';
    const secretStack = new SecretStack(app, 'SecretStack', {
      stackName: `workops-${stage}-secret`,
    });
    const template = Template.fromStack(secretStack);

    template.resourceCountIs('AWS::SecretsManager::Secret', 0);
  });

  test('creates the DataStack database resources', () => {
    const app = new App();
    const stage = 'dev';
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
      stage,
      stackName: `workops-${stage}-foundation`,
    });
    const dataStack = new DataStack(app, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      stage,
      stackName: `workops-${stage}-data`,
      vpc: foundationStack.vpc,
    });
    const dataTemplate = Template.fromStack(dataStack);
    const foundationTemplate = Template.fromStack(foundationStack);
    const dataTemplateText = JSON.stringify(dataTemplate.toJSON());

    dataTemplate.resourceCountIs('AWS::RDS::DBInstance', 1);
    dataTemplate.resourceCountIs('AWS::EC2::Instance', 0);
    dataTemplate.resourceCountIs('AWS::IAM::Role', 0);
    dataTemplate.resourceCountIs('AWS::IAM::InstanceProfile', 0);
    dataTemplate.resourceCountIs('AWS::IAM::Policy', 0);
    dataTemplate.hasResourceProperties('AWS::RDS::DBSubnetGroup', {
      DBSubnetGroupName: 'workops-dev-db-subnet-group',
    });
    dataTemplate.hasResourceProperties('AWS::RDS::DBInstance', {
      AllocatedStorage: '20',
      BackupRetentionPeriod: 1,
      DBInstanceClass: 'db.t4g.micro',
      DBInstanceIdentifier: 'workops-dev-db',
      DBName: 'workops',
      DeletionProtection: false,
      Engine: 'mysql',
      EngineVersion: '8.4.9',
      MultiAZ: false,
      PubliclyAccessible: false,
      StorageEncrypted: true,
      StorageType: 'gp2',
      VPCSecurityGroups: Match.arrayWith([
        {
          'Fn::GetAtt': [
            Match.stringLikeRegexp('RdsConsoleCloudShellSecurityGroup'),
            'GroupId',
          ],
        },
      ]),
    });
    dataTemplate.hasResource('AWS::RDS::DBInstance', {
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    dataTemplate.hasResourceProperties('AWS::SecretsManager::Secret', {
      Name: '/workops/dev/db/master',
      GenerateSecretString: Match.objectLike({
        GenerateStringKey: 'password',
        SecretStringTemplate: '{"username":"workops_admin"}',
      }),
    });
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroup', {
      GroupDescription: 'WorkOps RDS Console CloudShell VPC security group',
      GroupName: 'workops-dev-rds-console-cloudshell-sg',
      SecurityGroupIngress: Match.absent(),
    });
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroupEgress', {
      Description: 'Allow RDS Console CloudShell VPC environment to reach MySQL',
      DestinationSecurityGroupId: Match.anyValue(),
      FromPort: 3306,
      IpProtocol: 'tcp',
      ToPort: 3306,
    });
    foundationTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      FromPort: 3306,
      IpProtocol: 'tcp',
      ToPort: 3306,
    });
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow RDS Console CloudShell VPC environment to reach MySQL',
      FromPort: 3306,
      IpProtocol: 'tcp',
      SourceSecurityGroupId: Match.anyValue(),
      ToPort: 3306,
    });
    expect(dataTemplateText).toContain('workops-dev-rds-console-cloudshell-sg');
    expect(dataTemplateText).not.toContain('al2023-ami');
    expect(dataTemplateText).not.toContain('AmazonSSMManagedInstanceCore');
    expect(dataTemplateText).not.toContain('AssociatePublicIpAddress');
    expect(dataTemplateText).not.toContain('HttpTokens');
    expect(dataTemplateText).not.toContain('DbAccessHost');
    expect(dataTemplateText).not.toContain('secretsmanager:GetSecretValue');
    expect(dataTemplateText).not.toContain('rds-db:connect');
    expect(dataTemplateText).not.toContain('ssm:GetParameter');
    dataTemplate.hasOutput('rdsInstanceIdentifier', {});
    dataTemplate.hasOutput('rdsEndpointAddress', {});
    dataTemplate.hasOutput('rdsPort', {});
    dataTemplate.hasOutput('databaseName', {});
    dataTemplate.hasOutput('dbSubnetGroupName', {});
    dataTemplate.hasOutput('rdsMasterSecretArn', {});
    dataTemplate.hasOutput('rdsConsoleCloudShellSecurityGroupId', {});
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('PublicIp');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('password');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('secretValue');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('dbAccessHostInstanceId');
  });

  test('creates the ConfigStack non-secret parameters in P2-2-01', () => {
    const app = new App();
    const stage = 'dev';
    const configStack = new ConfigStack(app, 'ConfigStack', {
      dbEndpointAddress: 'workops-dev-db.example.ap-northeast-1.rds.amazonaws.com',
      dbName: 'workops',
      dbPort: '3306',
      stage,
      stackName: `workops-${stage}-config`,
    });
    const template = Template.fromStack(configStack);

    template.resourceCountIs('AWS::SSM::Parameter', 4);
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/spring/profile',
      Type: 'String',
      Value: 'dev',
    });
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/name',
      Type: 'String',
      Value: 'workops',
    });
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/port',
      Type: 'String',
      Value: '3306',
    });
    template.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/url',
      Type: 'String',
      Value: 'jdbc:mysql://workops-dev-db.example.ap-northeast-1.rds.amazonaws.com:3306/workops?useSSL=true&serverTimezone=Asia/Tokyo',
    });
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

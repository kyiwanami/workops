import { Match, Template } from 'aws-cdk-lib/assertions';
import { DataStack } from '../lib/data-stack';
import { FoundationStack } from '../lib/foundation-stack';
import { createTestApp } from './workops-test-fixtures';

describe('WorkOps CDK data stack', () => {
  test('creates the DataStack database resources', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const foundationStack = new FoundationStack(app, 'FoundationStack', {
    });
    const dataStack = new DataStack(app, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
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
          'Fn::GetAtt': [Match.stringLikeRegexp('RdsConsoleCloudShellSecurityGroup'), 'GroupId'],
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
    foundationTemplate.resourceCountIs('AWS::EC2::SecurityGroupIngress', 0);
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow WorkOps app tasks to reach MySQL',
      FromPort: 3306,
      IpProtocol: 'tcp',
      SourceSecurityGroupId: Match.anyValue(),
      ToPort: 3306,
    });
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow WorkOps migration CodeBuild to reach MySQL',
      FromPort: 3306,
      IpProtocol: 'tcp',
      SourceSecurityGroupId: Match.anyValue(),
      ToPort: 3306,
    });
    dataTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow RDS Console CloudShell VPC environment to reach MySQL',
      FromPort: 3306,
      IpProtocol: 'tcp',
      SourceSecurityGroupId: Match.anyValue(),
      ToPort: 3306,
    });
    dataTemplate.resourceCountIs('AWS::SSM::Parameter', 3);
    dataTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/name',
      Type: 'String',
      Value: 'workops',
    });
    dataTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/port',
      Type: 'String',
      Value: '3306',
    });
    dataTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/db/url',
      Type: 'String',
      Value: Match.objectLike({
        'Fn::Join': Match.arrayWith([
          '',
          Match.arrayWith([
            'jdbc:mysql://',
            {
              'Fn::GetAtt': [Match.stringLikeRegexp('Database'), 'Endpoint.Address'],
            },
            ':3306/workops?useSSL=true&serverTimezone=Asia/Tokyo',
          ]),
        ]),
      }),
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
    dataTemplate.hasOutput('rdsConsoleCloudShellSecurityGroupId', {});
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('PublicIp');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('password');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('secretValue');
    expect(JSON.stringify(dataTemplate.toJSON().Outputs)).not.toContain('dbAccessHostInstanceId');
  });
});

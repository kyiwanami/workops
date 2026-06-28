import { CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import {
  CfnSecurityGroupIngress,
  InstanceClass,
  InstanceSize,
  InstanceType,
  Port,
  SecurityGroup,
} from 'aws-cdk-lib/aws-ec2';
import {
  Credentials,
  DatabaseInstance,
  DatabaseInstanceEngine,
  MysqlEngineVersion,
  StorageType,
  SubnetGroup,
} from 'aws-cdk-lib/aws-rds';
import { Construct } from 'constructs';
import { foundationNetwork, foundationSecurityGroups } from '../shared/contract-imports';
import { readStage, stackName, stagePath } from '../shared/environment';
import { createParameter } from '../shared/ssm-parameters';

export class DataStack extends Stack {
  public readonly databaseName: string;
  public readonly databasePort: string;
  public readonly endpointAddress: string;
  public readonly instance: DatabaseInstance;
  public readonly rdsConsoleCloudShellSecurityGroup: SecurityGroup;
  public readonly subnetGroup: SubnetGroup;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'data'),
    });

    const network = foundationNetwork(this);
    const securityGroups = foundationSecurityGroups(this);
    this.databaseName = 'workops';
    this.databasePort = '3306';

    // The database subnet group keeps the RDS placement fixed to isolated DB subnets.
    this.subnetGroup = new SubnetGroup(this, 'DbSubnetGroup', {
      description: 'WorkOps database isolated subnets',
      subnetGroupName: `workops-${stage}-db-subnet-group`,
      vpc: network.vpc,
      vpcSubnets: {
        subnets: network.dbSubnets,
      },
    });

    // RDS Console integrated CloudShell attaches the DB security groups to its VPC environment.
    this.rdsConsoleCloudShellSecurityGroup = new SecurityGroup(
      this,
      'RdsConsoleCloudShellSecurityGroup',
      {
        allowAllOutbound: false,
        description: 'WorkOps RDS Console CloudShell VPC security group',
        securityGroupName: `workops-${stage}-rds-console-cloudshell-sg`,
        vpc: network.vpc,
      },
    );
    this.rdsConsoleCloudShellSecurityGroup.addEgressRule(
      this.rdsConsoleCloudShellSecurityGroup,
      Port.tcp(3306),
      'Allow RDS Console CloudShell VPC environment to reach MySQL',
    );
    this.rdsConsoleCloudShellSecurityGroup.addIngressRule(
      this.rdsConsoleCloudShellSecurityGroup,
      Port.tcp(3306),
      'Allow RDS Console CloudShell VPC environment to reach MySQL',
    );

    new CfnSecurityGroupIngress(this, 'DbIngressFromApp', {
      groupId: securityGroups.dbSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourceSecurityGroupId: securityGroups.appSecurityGroup.securityGroupId,
      fromPort: 3306,
      toPort: 3306,
      description: 'Allow WorkOps app tasks to reach MySQL',
    });
    new CfnSecurityGroupIngress(this, 'DbIngressFromMigration', {
      groupId: securityGroups.dbSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourceSecurityGroupId: securityGroups.migrationSecurityGroup.securityGroupId,
      fromPort: 3306,
      toPort: 3306,
      description: 'Allow WorkOps migration CodeBuild to reach MySQL',
    });

    // RDS is the Phase 2 source of truth for WorkOps business data.
    this.instance = new DatabaseInstance(this, 'Database', {
      allocatedStorage: 20,
      backupRetention: Duration.days(1),
      credentials: Credentials.fromGeneratedSecret('workops_admin', {
        secretName: stagePath(this, 'db/master'),
      }),
      databaseName: this.databaseName,
      deletionProtection: false,
      engine: DatabaseInstanceEngine.mysql({
        version: MysqlEngineVersion.VER_8_4_9,
      }),
      instanceIdentifier: `workops-${stage}-db`,
      instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO),
      multiAz: false,
      publiclyAccessible: false,
      removalPolicy: RemovalPolicy.DESTROY,
      securityGroups: [
        securityGroups.dbSecurityGroup,
        this.rdsConsoleCloudShellSecurityGroup,
      ],
      storageEncrypted: true,
      storageType: StorageType.GP2,
      subnetGroup: this.subnetGroup,
      vpc: network.vpc,
      vpcSubnets: {
        subnets: network.dbSubnets,
      },
    });

    this.endpointAddress = this.instance.dbInstanceEndpointAddress;
    const masterSecret = this.instance.secret;
    if (!masterSecret) {
      throw new Error('RDS master secret is required');
    }

    // DB connection parameters are owned with the RDS lifecycle because the endpoint changes when RDS is recreated.
    createParameter(this, 'DbNameParameter', 'db/name', this.databaseName);
    createParameter(this, 'DbPortParameter', 'db/port', this.databasePort);
    createParameter(
      this,
      'DbUrlParameter',
      'db/url',
      `jdbc:mysql://${this.endpointAddress}:${this.databasePort}/${this.databaseName}?useSSL=true&serverTimezone=Asia/Tokyo`,
    );

    new CfnOutput(this, 'rdsConsoleCloudShellSecurityGroupId', {
      value: this.rdsConsoleCloudShellSecurityGroup.securityGroupId,
    });
  }
}

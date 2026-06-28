import { CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import {
  CfnSecurityGroupIngress,
  InstanceClass,
  InstanceSize,
  InstanceType,
  ISubnet,
  Port,
  SecurityGroup,
  Vpc,
} from 'aws-cdk-lib/aws-ec2';
import {
  Credentials,
  DatabaseInstance,
  DatabaseInstanceEngine,
  MysqlEngineVersion,
  StorageType,
  SubnetGroup,
} from 'aws-cdk-lib/aws-rds';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';
import { readWorkopsStage, workopsStackName } from './environment';

export interface DataStackProps extends StackProps {
  vpc: Vpc;
  dbSubnets: ISubnet[];
  appSecurityGroup: SecurityGroup;
  dbSecurityGroup: SecurityGroup;
  migrationSecurityGroup: SecurityGroup;
}

export class DataStack extends Stack {
  public readonly databaseName: string;
  public readonly databasePort: string;
  public readonly endpointAddress: string;
  public readonly instance: DatabaseInstance;
  public readonly rdsConsoleCloudShellSecurityGroup: SecurityGroup;
  public readonly subnetGroup: SubnetGroup;

  constructor(scope: Construct, id: string, props: DataStackProps) {
    const stage = readWorkopsStage(scope);
    super(scope, id, {
      ...props,
      stackName: workopsStackName(scope, 'data'),
    });

    this.databaseName = 'workops';
    this.databasePort = '3306';

    // The database subnet group keeps the RDS placement fixed to isolated DB subnets.
    this.subnetGroup = new SubnetGroup(this, 'DbSubnetGroup', {
      description: 'WorkOps database isolated subnets',
      subnetGroupName: `workops-${stage}-db-subnet-group`,
      vpc: props.vpc,
      vpcSubnets: {
        subnets: props.dbSubnets,
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
        vpc: props.vpc,
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
      groupId: props.dbSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourceSecurityGroupId: props.appSecurityGroup.securityGroupId,
      fromPort: 3306,
      toPort: 3306,
      description: 'Allow WorkOps app tasks to reach MySQL',
    });
    new CfnSecurityGroupIngress(this, 'DbIngressFromMigration', {
      groupId: props.dbSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourceSecurityGroupId: props.migrationSecurityGroup.securityGroupId,
      fromPort: 3306,
      toPort: 3306,
      description: 'Allow WorkOps migration CodeBuild to reach MySQL',
    });

    // RDS is the Phase 2 source of truth for WorkOps business data.
    this.instance = new DatabaseInstance(this, 'Database', {
      allocatedStorage: 20,
      backupRetention: Duration.days(1),
      credentials: Credentials.fromGeneratedSecret('workops_admin', {
        secretName: `/workops/${stage}/db/master`,
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
      securityGroups: [props.dbSecurityGroup, this.rdsConsoleCloudShellSecurityGroup],
      storageEncrypted: true,
      storageType: StorageType.GP2,
      subnetGroup: this.subnetGroup,
      vpc: props.vpc,
      vpcSubnets: {
        subnets: props.dbSubnets,
      },
    });

    this.endpointAddress = this.instance.dbInstanceEndpointAddress;
    const masterSecret = this.instance.secret;
    if (!masterSecret) {
      throw new Error('RDS master secret is required');
    }

    // DB connection parameters are owned with the RDS lifecycle because the endpoint changes when RDS is recreated.
    new StringParameter(this, 'DbNameParameter', {
      parameterName: `/workops/${stage}/db/name`,
      stringValue: this.databaseName,
    });
    new StringParameter(this, 'DbPortParameter', {
      parameterName: `/workops/${stage}/db/port`,
      stringValue: this.databasePort,
    });
    new StringParameter(this, 'DbUrlParameter', {
      parameterName: `/workops/${stage}/db/url`,
      stringValue: `jdbc:mysql://${this.endpointAddress}:${this.databasePort}/${this.databaseName}?useSSL=true&serverTimezone=Asia/Tokyo`,
    });

    new CfnOutput(this, 'rdsConsoleCloudShellSecurityGroupId', {
      value: this.rdsConsoleCloudShellSecurityGroup.securityGroupId,
    });
  }
}

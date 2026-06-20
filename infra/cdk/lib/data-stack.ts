import { CfnOutput, Duration, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { CfnSecurityGroupIngress, InstanceClass, InstanceSize, InstanceType, ISubnet, Port, SecurityGroup, Vpc } from 'aws-cdk-lib/aws-ec2';
import { Credentials, DatabaseInstance, DatabaseInstanceEngine, MysqlEngineVersion, StorageType, SubnetGroup } from 'aws-cdk-lib/aws-rds';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface DataStackProps extends StackProps {
  stage: string;
  vpc: Vpc;
  dbSubnets: ISubnet[];
  appSecurityGroup: SecurityGroup;
  dbSecurityGroup: SecurityGroup;
}

export class DataStack extends Stack {
  public readonly databaseName: string;
  public readonly databasePort: string;
  public readonly endpointAddress: string;
  public readonly instance: DatabaseInstance;
  public readonly rdsConsoleCloudShellSecurityGroup: SecurityGroup;
  public readonly subnetGroup: SubnetGroup;

  constructor(scope: Construct, id: string, props: DataStackProps) {
    super(scope, id, props);

    this.databaseName = 'workops';
    this.databasePort = '3306';

    // The database subnet group keeps the RDS placement fixed to isolated DB subnets.
    this.subnetGroup = new SubnetGroup(this, 'DbSubnetGroup', {
      description: 'WorkOps database isolated subnets',
      subnetGroupName: `workops-${props.stage}-db-subnet-group`,
      vpc: props.vpc,
      vpcSubnets: {
        subnets: props.dbSubnets,
      },
    });

    // RDS Console integrated CloudShell attaches the DB security groups to its VPC environment.
    this.rdsConsoleCloudShellSecurityGroup = new SecurityGroup(this, 'RdsConsoleCloudShellSecurityGroup', {
      allowAllOutbound: false,
      description: 'WorkOps RDS Console CloudShell VPC security group',
      securityGroupName: `workops-${props.stage}-rds-console-cloudshell-sg`,
      vpc: props.vpc,
    });
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

    // RDS is the Phase 2 source of truth for WorkOps business data.
    this.instance = new DatabaseInstance(this, 'Database', {
      allocatedStorage: 20,
      backupRetention: Duration.days(1),
      credentials: Credentials.fromGeneratedSecret('workops_admin', {
        secretName: `/workops/${props.stage}/db/master`,
      }),
      databaseName: this.databaseName,
      deletionProtection: false,
      engine: DatabaseInstanceEngine.mysql({
        version: MysqlEngineVersion.VER_8_4_9,
      }),
      instanceIdentifier: `workops-${props.stage}-db`,
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
      parameterName: `/workops/${props.stage}/db/name`,
      stringValue: this.databaseName,
    });
    new StringParameter(this, 'DbPortParameter', {
      parameterName: `/workops/${props.stage}/db/port`,
      stringValue: this.databasePort,
    });
    new StringParameter(this, 'DbUrlParameter', {
      parameterName: `/workops/${props.stage}/db/url`,
      stringValue: `jdbc:mysql://${this.endpointAddress}:${this.databasePort}/${this.databaseName}?useSSL=true&serverTimezone=Asia/Tokyo`,
    });

    new CfnOutput(this, 'rdsInstanceIdentifier', {
      value: this.instance.instanceIdentifier,
    });
    new CfnOutput(this, 'rdsEndpointAddress', {
      value: this.endpointAddress,
    });
    new CfnOutput(this, 'rdsPort', {
      value: this.databasePort,
    });
    new CfnOutput(this, 'databaseName', {
      value: this.databaseName,
    });
    new CfnOutput(this, 'dbSubnetGroupName', {
      value: this.subnetGroup.subnetGroupName,
    });
    new CfnOutput(this, 'rdsMasterSecretArn', {
      value: masterSecret.secretArn,
    });
    new CfnOutput(this, 'rdsConsoleCloudShellSecurityGroupId', {
      value: this.rdsConsoleCloudShellSecurityGroup.securityGroupId,
    });
  }
}

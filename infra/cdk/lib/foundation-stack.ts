import { CfnOutput, Fn, Stack, StackProps } from 'aws-cdk-lib';
import { Cluster } from 'aws-cdk-lib/aws-ecs';
import { IpAddresses, ISubnet, SecurityGroup, SubnetType, Vpc } from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export interface FoundationStackProps extends StackProps {
  stage: string;
}

export class FoundationStack extends Stack {
  public readonly vpc: Vpc;
  public readonly publicSubnets: ISubnet[];
  public readonly appSubnets: ISubnet[];
  public readonly dbSubnets: ISubnet[];
  public readonly albSecurityGroup: SecurityGroup;
  public readonly appSecurityGroup: SecurityGroup;
  public readonly dbSecurityGroup: SecurityGroup;
  public readonly ecsCluster: Cluster;

  constructor(scope: Construct, id: string, props: FoundationStackProps) {
    super(scope, id, props);

    // Foundation networking is shared by later RDS, ECS, and Cognito-facing stacks.
    this.vpc = new Vpc(this, 'Vpc', {
      ipAddresses: IpAddresses.cidr('10.0.0.0/16'),
      maxAzs: 2,
      natGateways: 0,
      vpcName: `workops-${props.stage}-vpc`,
      subnetConfiguration: [
        {
          name: 'public',
          subnetType: SubnetType.PUBLIC,
          cidrMask: 24,
        },
        {
          name: 'app',
          subnetType: SubnetType.PRIVATE_ISOLATED,
          cidrMask: 24,
        },
        {
          name: 'db',
          subnetType: SubnetType.PRIVATE_ISOLATED,
          cidrMask: 24,
        },
      ],
    });

    this.publicSubnets = this.vpc.selectSubnets({ subnetGroupName: 'public' }).subnets;
    this.appSubnets = this.vpc.selectSubnets({ subnetGroupName: 'app' }).subnets;
    this.dbSubnets = this.vpc.selectSubnets({ subnetGroupName: 'db' }).subnets;

    // Security groups are named now; traffic rules are added by resource-owning stacks.
    this.albSecurityGroup = new SecurityGroup(this, 'AlbSecurityGroup', {
      vpc: this.vpc,
      securityGroupName: `workops-${props.stage}-alb-sg`,
      description: 'WorkOps ALB security group',
      allowAllOutbound: true,
    });
    this.appSecurityGroup = new SecurityGroup(this, 'AppSecurityGroup', {
      vpc: this.vpc,
      securityGroupName: `workops-${props.stage}-app-sg`,
      description: 'WorkOps app security group',
      allowAllOutbound: true,
    });
    this.dbSecurityGroup = new SecurityGroup(this, 'DbSecurityGroup', {
      vpc: this.vpc,
      securityGroupName: `workops-${props.stage}-db-sg`,
      description: 'WorkOps database security group',
      allowAllOutbound: true,
    });

    // The cluster is empty in P2-1; services and tasks arrive in later phases.
    this.ecsCluster = new Cluster(this, 'EcsCluster', {
      vpc: this.vpc,
      clusterName: `workops-${props.stage}-cluster`,
    });

    new CfnOutput(this, 'vpcId', {
      value: this.vpc.vpcId,
    });
    new CfnOutput(this, 'publicSubnetIds', {
      value: Fn.join(',', this.publicSubnets.map((subnet) => subnet.subnetId)),
    });
    new CfnOutput(this, 'appSubnetIds', {
      value: Fn.join(',', this.appSubnets.map((subnet) => subnet.subnetId)),
    });
    new CfnOutput(this, 'dbSubnetIds', {
      value: Fn.join(',', this.dbSubnets.map((subnet) => subnet.subnetId)),
    });
    new CfnOutput(this, 'ecsClusterName', {
      value: this.ecsCluster.clusterName,
    });
    new CfnOutput(this, 'albSecurityGroupId', {
      value: this.albSecurityGroup.securityGroupId,
    });
    new CfnOutput(this, 'appSecurityGroupId', {
      value: this.appSecurityGroup.securityGroupId,
    });
    new CfnOutput(this, 'dbSecurityGroupId', {
      value: this.dbSecurityGroup.securityGroupId,
    });
  }
}

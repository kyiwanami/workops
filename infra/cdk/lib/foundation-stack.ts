import { CfnOutput, Fn, Stack, StackProps } from 'aws-cdk-lib';
import { Cluster } from 'aws-cdk-lib/aws-ecs';
import { IpAddresses, ISubnet, SecurityGroup, SubnetType, Vpc } from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';
import { exportName } from './stack-exports';

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
  public readonly migrationSecurityGroup: SecurityGroup;
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
    const appSubnetOne = this.appSubnets[0];
    const appSubnetTwo = this.appSubnets[1];

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
    this.migrationSecurityGroup = new SecurityGroup(this, 'MigrationSecurityGroup', {
      vpc: this.vpc,
      securityGroupName: `workops-${props.stage}-migration-sg`,
      description: 'WorkOps migration CodeBuild security group',
      allowAllOutbound: true,
    });

    // The cluster is empty in P2-1; services and tasks arrive in later phases.
    this.ecsCluster = new Cluster(this, 'EcsCluster', {
      vpc: this.vpc,
      clusterName: `workops-${props.stage}-cluster`,
    });

    new CfnOutput(this, 'vpcId', {
      exportName: exportName(props.stage, 'foundation-vpc-id'),
      value: this.vpc.vpcId,
    });
    new CfnOutput(this, 'publicSubnetIds', {
      value: Fn.join(
        ',',
        this.publicSubnets.map((subnet) => subnet.subnetId),
      ),
    });
    new CfnOutput(this, 'appSubnetIds', {
      value: Fn.join(
        ',',
        this.appSubnets.map((subnet) => subnet.subnetId),
      ),
    });
    new CfnOutput(this, 'appSubnetOneId', {
      exportName: exportName(props.stage, 'foundation-app-subnet-one-id'),
      value: appSubnetOne.subnetId,
    });
    new CfnOutput(this, 'appSubnetTwoId', {
      exportName: exportName(props.stage, 'foundation-app-subnet-two-id'),
      value: appSubnetTwo.subnetId,
    });
    new CfnOutput(this, 'appSubnetOneRouteTableId', {
      exportName: exportName(props.stage, 'foundation-app-subnet-one-route-table-id'),
      value: appSubnetOne.routeTable.routeTableId,
    });
    new CfnOutput(this, 'appSubnetTwoRouteTableId', {
      exportName: exportName(props.stage, 'foundation-app-subnet-two-route-table-id'),
      value: appSubnetTwo.routeTable.routeTableId,
    });
    new CfnOutput(this, 'dbSubnetIds', {
      value: Fn.join(
        ',',
        this.dbSubnets.map((subnet) => subnet.subnetId),
      ),
    });
    new CfnOutput(this, 'ecsClusterName', {
      exportName: exportName(props.stage, 'foundation-ecs-cluster-name'),
      value: this.ecsCluster.clusterName,
    });
    new CfnOutput(this, 'albSecurityGroupId', {
      exportName: exportName(props.stage, 'foundation-alb-security-group-id'),
      value: this.albSecurityGroup.securityGroupId,
    });
    new CfnOutput(this, 'appSecurityGroupId', {
      exportName: exportName(props.stage, 'foundation-app-security-group-id'),
      value: this.appSecurityGroup.securityGroupId,
    });
    new CfnOutput(this, 'dbSecurityGroupId', {
      value: this.dbSecurityGroup.securityGroupId,
    });
    new CfnOutput(this, 'migrationSecurityGroupId', {
      value: this.migrationSecurityGroup.securityGroupId,
    });
  }
}

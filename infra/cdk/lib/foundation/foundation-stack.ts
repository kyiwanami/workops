import { Stack, StackProps } from 'aws-cdk-lib';
import { Cluster } from 'aws-cdk-lib/aws-ecs';
import {
  GatewayVpcEndpointAwsService,
  IpAddresses,
  ISubnet,
  SecurityGroup,
  SubnetType,
  Vpc,
} from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';
import { readStage, stackName } from '../shared/environment';
import { createParameter } from '../shared/ssm-parameters';

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

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'foundation'),
    });

    // Foundation networking is shared by later RDS, ECS, and Cognito-facing stacks.
    this.vpc = new Vpc(this, 'Vpc', {
      ipAddresses: IpAddresses.cidr('10.0.0.0/16'),
      maxAzs: 2,
      natGateways: 0,
      vpcName: `workops-${stage}-vpc`,
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
    // App private subnets reach S3 without requiring NAT or interface endpoints.
    this.vpc.addGatewayEndpoint('S3GatewayEndpoint', {
      service: GatewayVpcEndpointAwsService.S3,
      subnets: [
        {
          subnets: this.appSubnets,
        },
      ],
    });
    // Security groups are named now; traffic rules are added by resource-owning stacks.
    this.albSecurityGroup = new SecurityGroup(this, 'AlbSecurityGroup', {
      vpc: this.vpc,
      securityGroupName: `workops-${stage}-alb-sg`,
      description: 'WorkOps ALB security group',
      allowAllOutbound: true,
    });
    this.appSecurityGroup = new SecurityGroup(this, 'AppSecurityGroup', {
      vpc: this.vpc,
      securityGroupName: `workops-${stage}-app-sg`,
      description: 'WorkOps app security group',
      allowAllOutbound: true,
    });
    this.dbSecurityGroup = new SecurityGroup(this, 'DbSecurityGroup', {
      vpc: this.vpc,
      securityGroupName: `workops-${stage}-db-sg`,
      description: 'WorkOps database security group',
      allowAllOutbound: true,
    });
    this.migrationSecurityGroup = new SecurityGroup(this, 'MigrationSecurityGroup', {
      vpc: this.vpc,
      securityGroupName: `workops-${stage}-migration-sg`,
      description: 'WorkOps migration CodeBuild security group',
      allowAllOutbound: true,
    });

    // The cluster is empty in P2-1; services and tasks arrive in later phases.
    this.ecsCluster = new Cluster(this, 'EcsCluster', {
      vpc: this.vpc,
      clusterName: `workops-${stage}-cluster`,
    });

    this.createFoundationContract(stage);
  }

  private createFoundationContract(stage: string): void {
    const publicSubnetOne = this.requiredSubnet(this.publicSubnets, 0, 'public subnet 1');
    const publicSubnetTwo = this.requiredSubnet(this.publicSubnets, 1, 'public subnet 2');
    const appSubnetOne = this.requiredSubnet(this.appSubnets, 0, 'app subnet 1');
    const appSubnetTwo = this.requiredSubnet(this.appSubnets, 1, 'app subnet 2');
    const dbSubnetOne = this.requiredSubnet(this.dbSubnets, 0, 'db subnet 1');
    const dbSubnetTwo = this.requiredSubnet(this.dbSubnets, 1, 'db subnet 2');

    createParameter(this, 'VpcIdParameter', 'foundation/vpc-id', this.vpc.vpcId);
    createParameter(
      this,
      'VpcCidrBlockParameter',
      'foundation/vpc-cidr-block',
      this.vpc.vpcCidrBlock,
    );
    createParameter(
      this,
      'AvailabilityZoneOneParameter',
      'foundation/availability-zones/az-1',
      publicSubnetOne.availabilityZone,
    );
    createParameter(
      this,
      'AvailabilityZoneTwoParameter',
      'foundation/availability-zones/az-2',
      publicSubnetTwo.availabilityZone,
    );

    this.createSubnetContract(stage, 'PublicSubnetOne', 'public-1', publicSubnetOne);
    this.createSubnetContract(stage, 'PublicSubnetTwo', 'public-2', publicSubnetTwo);
    this.createSubnetContract(stage, 'AppSubnetOne', 'app-1', appSubnetOne);
    this.createSubnetContract(stage, 'AppSubnetTwo', 'app-2', appSubnetTwo);
    this.createSubnetContract(stage, 'DbSubnetOne', 'db-1', dbSubnetOne);
    this.createSubnetContract(stage, 'DbSubnetTwo', 'db-2', dbSubnetTwo);

    createParameter(
      this,
      'AlbSecurityGroupIdParameter',
      'foundation/security-groups/alb-sg-id',
      this.albSecurityGroup.securityGroupId,
    );
    createParameter(
      this,
      'AppSecurityGroupIdParameter',
      'foundation/security-groups/app-sg-id',
      this.appSecurityGroup.securityGroupId,
    );
    createParameter(
      this,
      'DbSecurityGroupIdParameter',
      'foundation/security-groups/db-sg-id',
      this.dbSecurityGroup.securityGroupId,
    );
    createParameter(
      this,
      'MigrationSecurityGroupIdParameter',
      'foundation/security-groups/migration-sg-id',
      this.migrationSecurityGroup.securityGroupId,
    );
    createParameter(
      this,
      'EcsClusterNameParameter',
      'foundation/ecs/cluster-name',
      this.ecsCluster.clusterName,
    );
    createParameter(
      this,
      'EcsClusterArnParameter',
      'foundation/ecs/cluster-arn',
      this.ecsCluster.clusterArn,
    );
  }

  private createSubnetContract(
    stage: string,
    idPrefix: string,
    contractName: string,
    subnet: ISubnet,
  ): void {
    createParameter(
      this,
      `${idPrefix}IdParameter`,
      `foundation/subnets/${contractName}/id`,
      subnet.subnetId,
    );
    createParameter(
      this,
      `${idPrefix}AvailabilityZoneParameter`,
      `foundation/subnets/${contractName}/availability-zone`,
      subnet.availabilityZone,
    );
    createParameter(
      this,
      `${idPrefix}RouteTableIdParameter`,
      `foundation/subnets/${contractName}/route-table-id`,
      subnet.routeTable.routeTableId,
    );
  }

  private requiredSubnet(subnets: ISubnet[], index: number, description: string): ISubnet {
    const subnet = subnets[index];
    if (!subnet) {
      throw new Error(`FoundationStack requires ${description}`);
    }
    return subnet;
  }
}

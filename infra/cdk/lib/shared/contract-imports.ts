import {
  ISecurityGroup,
  ISubnet,
  IVpc,
  SecurityGroup,
  Subnet,
  Vpc,
} from 'aws-cdk-lib/aws-ec2';
import { ILogGroup, LogGroup } from 'aws-cdk-lib/aws-logs';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';
import { stagePath } from './environment';

export interface FoundationNetwork {
  vpc: IVpc;
  publicSubnets: ISubnet[];
  appSubnets: ISubnet[];
  dbSubnets: ISubnet[];
}

export interface FoundationSecurityGroups {
  albSecurityGroup: ISecurityGroup;
  appSecurityGroup: ISecurityGroup;
  dbSecurityGroup: ISecurityGroup;
  migrationSecurityGroup: ISecurityGroup;
}

export interface IdentityContract {
  userPoolId: string;
  platformClientId: string;
  tenantClientId: string;
  hostedUiDomainBaseUrl: string;
}

export function contractValue(scope: Construct, suffix: string): string {
  return StringParameter.valueForStringParameter(scope, stagePath(scope, suffix));
}

export function foundationNetwork(scope: Construct): FoundationNetwork {
  // Stage-crossing network imports read deploy-time SSM parameters inside the consumer Stack.
  const publicSubnetOne = foundationSubnet(scope, 'PublicSubnetOne', 'public-1');
  const publicSubnetTwo = foundationSubnet(scope, 'PublicSubnetTwo', 'public-2');
  const appSubnetOne = foundationSubnet(scope, 'AppSubnetOne', 'app-1');
  const appSubnetTwo = foundationSubnet(scope, 'AppSubnetTwo', 'app-2');
  const dbSubnetOne = foundationSubnet(scope, 'DbSubnetOne', 'db-1');
  const dbSubnetTwo = foundationSubnet(scope, 'DbSubnetTwo', 'db-2');
  const availabilityZones = [
    contractValue(scope, 'foundation/availability-zones/az-1'),
    contractValue(scope, 'foundation/availability-zones/az-2'),
  ];

  return {
    vpc: Vpc.fromVpcAttributes(scope, 'FoundationVpc', {
      availabilityZones,
      vpcCidrBlock: contractValue(scope, 'foundation/vpc-cidr-block'),
      vpcId: contractValue(scope, 'foundation/vpc-id'),
    }),
    publicSubnets: [publicSubnetOne, publicSubnetTwo],
    appSubnets: [appSubnetOne, appSubnetTwo],
    dbSubnets: [dbSubnetOne, dbSubnetTwo],
  };
}

export function foundationSecurityGroups(scope: Construct): FoundationSecurityGroups {
  // Security group IDs are non-secret contracts owned by FoundationStack.
  return {
    albSecurityGroup: SecurityGroup.fromSecurityGroupId(
      scope,
      'FoundationAlbSecurityGroup',
      contractValue(scope, 'foundation/security-groups/alb-sg-id'),
    ),
    appSecurityGroup: SecurityGroup.fromSecurityGroupId(
      scope,
      'FoundationAppSecurityGroup',
      contractValue(scope, 'foundation/security-groups/app-sg-id'),
    ),
    dbSecurityGroup: SecurityGroup.fromSecurityGroupId(
      scope,
      'FoundationDbSecurityGroup',
      contractValue(scope, 'foundation/security-groups/db-sg-id'),
    ),
    migrationSecurityGroup: SecurityGroup.fromSecurityGroupId(
      scope,
      'FoundationMigrationSecurityGroup',
      contractValue(scope, 'foundation/security-groups/migration-sg-id'),
    ),
  };
}

export function identityContract(scope: Construct): IdentityContract {
  // Cognito identifiers are runtime configuration, not secrets.
  return {
    userPoolId: contractValue(scope, 'identity/user-pool-id'),
    platformClientId: contractValue(scope, 'identity/platform-client-id'),
    tenantClientId: contractValue(scope, 'identity/tenant-client-id'),
    hostedUiDomainBaseUrl: contractValue(scope, 'identity/hosted-ui-domain-base-url'),
  };
}

export function logsGroup(scope: Construct, id: string, suffix: string): ILogGroup {
  return LogGroup.fromLogGroupName(scope, id, stagePath(scope, suffix));
}

function foundationSubnet(
  scope: Construct,
  idPrefix: string,
  contractName: string,
): ISubnet {
  return Subnet.fromSubnetAttributes(scope, `Foundation${idPrefix}`, {
    availabilityZone: contractValue(
      scope,
      `foundation/subnets/${contractName}/availability-zone`,
    ),
    routeTableId: contractValue(
      scope,
      `foundation/subnets/${contractName}/route-table-id`,
    ),
    subnetId: contractValue(scope, `foundation/subnets/${contractName}/id`),
  });
}

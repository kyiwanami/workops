import { CfnEIP, CfnNatGateway, CfnRoute } from 'aws-cdk-lib/aws-ec2';
import { Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { foundationNetwork } from '../shared/contract-imports';
import { readStage, stackName } from '../shared/environment';

export class EgressStack extends Stack {
  public readonly natGateway: CfnNatGateway;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'egress'),
    });

    const network = foundationNetwork(this);
    if (network.publicSubnets.length === 0) {
      throw new Error('EgressStack requires at least one public subnet');
    }
    const natSubnet = network.publicSubnets[0];

    // P2-3 runtime tasks need temporary internet egress for ECR image pulls and CloudWatch Logs.
    const natEip = new CfnEIP(this, 'NatGatewayEip', {
      domain: 'vpc',
      tags: [
        {
          key: 'Name',
          value: `workops-${stage}-nat-eip`,
        },
      ],
    });

    this.natGateway = new CfnNatGateway(this, 'NatGateway', {
      allocationId: natEip.attrAllocationId,
      subnetId: natSubnet.subnetId,
      tags: [
        {
          key: 'Name',
          value: `workops-${stage}-nat`,
        },
      ],
    });

    network.appSubnets.forEach((subnet, index) => {
      const routeNumber = String(index + 1);
      new CfnRoute(this, `AppSubnetDefaultRoute${routeNumber}`, {
        routeTableId: subnet.routeTable.routeTableId,
        destinationCidrBlock: '0.0.0.0/0',
        natGatewayId: this.natGateway.ref,
      });
    });
  }
}

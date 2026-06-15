import { CfnEIP, CfnNatGateway, CfnRoute, ISubnet, Vpc } from 'aws-cdk-lib/aws-ec2';
import { Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export interface EgressStackProps extends StackProps {
  stage: string;
  vpc: Vpc;
  publicSubnets: ISubnet[];
  appSubnets: ISubnet[];
}

export class EgressStack extends Stack {
  public readonly natGateway: CfnNatGateway;

  constructor(scope: Construct, id: string, props: EgressStackProps) {
    super(scope, id, props);

    const natSubnet = props.publicSubnets[0];
    if (!natSubnet) {
      throw new Error('EgressStack requires at least one public subnet');
    }

    // P2-3 runtime tasks need temporary internet egress for ECR image pulls and CloudWatch Logs.
    const natEip = new CfnEIP(this, 'NatGatewayEip', {
      domain: 'vpc',
      tags: [
        {
          key: 'Name',
          value: `workops-${props.stage}-nat-eip`,
        },
      ],
    });

    this.natGateway = new CfnNatGateway(this, 'NatGateway', {
      allocationId: natEip.attrAllocationId,
      subnetId: natSubnet.subnetId,
      tags: [
        {
          key: 'Name',
          value: `workops-${props.stage}-nat`,
        },
      ],
    });

    props.appSubnets.forEach((subnet, index) => {
      new CfnRoute(this, `AppSubnetDefaultRoute${index + 1}`, {
        routeTableId: subnet.routeTable.routeTableId,
        destinationCidrBlock: '0.0.0.0/0',
        natGatewayId: this.natGateway.ref,
      });
    });
  }
}

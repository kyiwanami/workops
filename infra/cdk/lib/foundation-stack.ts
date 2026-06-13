import { Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class FoundationStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // P2-1-02 adds the VPC, security groups, and ECS cluster to this stack.
  }
}

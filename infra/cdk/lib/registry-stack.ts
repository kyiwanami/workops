import { Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class RegistryStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // P2-1-03 adds the ECR repository and lifecycle policy to this stack.
  }
}

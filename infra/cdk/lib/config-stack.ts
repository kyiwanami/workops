import { Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class ConfigStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // P2-1 keeps this stack empty until non-secret parameters are defined.
  }
}

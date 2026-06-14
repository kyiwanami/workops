import { Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class SecretStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // P2-2 keeps this stack as the future home for app and migration secrets.
  }
}

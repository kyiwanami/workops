import { Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class LogsStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);

    // P2-1-03 adds CloudWatch log groups that outlive runtime session stacks.
  }
}

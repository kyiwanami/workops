import { Stack, StackProps } from 'aws-cdk-lib';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface ConfigStackProps extends StackProps {
  stage: string;
}

export class ConfigStack extends Stack {
  constructor(scope: Construct, id: string, props: ConfigStackProps) {
    super(scope, id, props);

    // ConfigStack owns runtime parameters that do not depend on recreated infrastructure.
    new StringParameter(this, 'SpringProfileParameter', {
      parameterName: `/workops/${props.stage}/spring/profile`,
      stringValue: 'dev',
    });
  }
}

import { Stack, StackProps } from 'aws-cdk-lib';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface ConfigStackProps extends StackProps {
  stage: string;
  dbName: string;
  dbPort: string;
  dbEndpointAddress: string;
}

export class ConfigStack extends Stack {
  constructor(scope: Construct, id: string, props: ConfigStackProps) {
    super(scope, id, props);

    // ConfigStack owns non-secret runtime parameters for the dev application.
    new StringParameter(this, 'SpringProfileParameter', {
      parameterName: `/workops/${props.stage}/spring/profile`,
      stringValue: 'dev',
    });
    new StringParameter(this, 'DbNameParameter', {
      parameterName: `/workops/${props.stage}/db/name`,
      stringValue: props.dbName,
    });
    new StringParameter(this, 'DbPortParameter', {
      parameterName: `/workops/${props.stage}/db/port`,
      stringValue: props.dbPort,
    });
    new StringParameter(this, 'DbUrlParameter', {
      parameterName: `/workops/${props.stage}/db/url`,
      stringValue: `jdbc:mysql://${props.dbEndpointAddress}:${props.dbPort}/${props.dbName}?useSSL=true&serverTimezone=Asia/Tokyo`,
    });
  }
}

import { RemovalPolicy } from 'aws-cdk-lib';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';
import { stagePath } from './environment';

export function createParameter(
  scope: Construct,
  id: string,
  suffix: string,
  stringValue: string,
): StringParameter {
  // WorkOps-owned SSM parameters follow the owning Stack lifecycle in Phase 2 beta.
  const parameter = new StringParameter(scope, id, {
    parameterName: stagePath(scope, suffix),
    stringValue,
  });
  parameter.applyRemovalPolicy(RemovalPolicy.DESTROY);
  return parameter;
}

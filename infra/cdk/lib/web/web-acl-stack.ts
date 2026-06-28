import { RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { CfnLoggingConfiguration, CfnWebACL } from 'aws-cdk-lib/aws-wafv2';
import { Construct } from 'constructs';
import { readStage, stackName } from '../shared/environment';
import { createParameter } from '../shared/ssm-parameters';

export class WebAclStack extends Stack {
  public readonly webAclArn: string;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'web-acl'),
    });

    // WebAcl starts in Count mode so Phase 2-beta observes managed rule matches without blocking traffic.
    const webAcl = new CfnWebACL(this, 'WebAcl', {
      defaultAction: {
        allow: {},
      },
      name: `workops-${stage}-cloudfront-web-acl`,
      scope: 'CLOUDFRONT',
      visibilityConfig: {
        cloudWatchMetricsEnabled: true,
        metricName: `workops-${stage}-cloudfront-web-acl`,
        sampledRequestsEnabled: true,
      },
      rules: [
        {
          name: 'AWSManagedRulesCommonRuleSet',
          priority: 0,
          overrideAction: {
            count: {},
          },
          statement: {
            managedRuleGroupStatement: {
              name: 'AWSManagedRulesCommonRuleSet',
              vendorName: 'AWS',
            },
          },
          visibilityConfig: {
            cloudWatchMetricsEnabled: true,
            metricName: `workops-${stage}-common-rule-set`,
            sampledRequestsEnabled: true,
          },
        },
      ],
    });
    this.webAclArn = webAcl.attrArn;
    createParameter(
      this,
      'CloudFrontWebAclArnParameter',
      'web-acl/cloudfront-web-acl-arn',
      this.webAclArn,
    );

    // TODO: Split stage-specific WAF log retention and destination handling when stage policy is settled.
    const logGroup = new LogGroup(this, 'WafLogGroup', {
      logGroupName: `aws-waf-logs-workops-${stage}-cloudfront`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    new CfnLoggingConfiguration(this, 'WafLoggingConfiguration', {
      logDestinationConfigs: [logGroup.logGroupArn],
      resourceArn: webAcl.attrArn,
    });
  }
}

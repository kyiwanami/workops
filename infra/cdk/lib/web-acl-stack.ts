import { CfnOutput, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { CfnLoggingConfiguration, CfnWebACL } from 'aws-cdk-lib/aws-wafv2';
import { Construct } from 'constructs';
import { readWorkopsStage, workopsStackName } from './environment';

export class WebAclStack extends Stack {
  public readonly webAclArn: string;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readWorkopsStage(scope);
    super(scope, id, {
      ...props,
      stackName: workopsStackName(scope, 'web-acl'),
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

    new CfnOutput(this, 'webAclArn', {
      value: this.webAclArn,
    });
  }
}

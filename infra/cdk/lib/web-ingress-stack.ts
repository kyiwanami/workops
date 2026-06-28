import { CfnOutput, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import {
  AwsCustomResource,
  AwsCustomResourcePolicy,
  PhysicalResourceId,
} from 'aws-cdk-lib/custom-resources';
import { CfnSecurityGroupIngress, ISubnet, SecurityGroup, Vpc } from 'aws-cdk-lib/aws-ec2';
import {
  ApplicationLoadBalancer,
  ApplicationListener,
  ApplicationProtocol,
  ListenerAction,
} from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

const CLOUD_FRONT_ORIGIN_PREFIX_LIST_NAME = 'com.amazonaws.global.cloudfront.origin-facing';

export interface WebIngressStackProps extends StackProps {
  stage: string;
  vpc: Vpc;
  appSubnets: ISubnet[];
  albSecurityGroup: SecurityGroup;
}

export class WebIngressStack extends Stack {
  public readonly loadBalancer: ApplicationLoadBalancer;
  public readonly listener: ApplicationListener;

  constructor(scope: Construct, id: string, props: WebIngressStackProps) {
    super(scope, id, props);

    // WebIngress owns the replaceable ALB side of the CloudFront VPC origin path.
    this.loadBalancer = new ApplicationLoadBalancer(this, 'WebAlb', {
      vpc: props.vpc,
      internetFacing: false,
      loadBalancerName: `workops-${props.stage}-web-alb`,
      securityGroup: props.albSecurityGroup,
      vpcSubnets: {
        subnets: props.appSubnets,
      },
    });

    this.listener = this.loadBalancer.addListener('HttpListener', {
      port: 80,
      protocol: ApplicationProtocol.HTTP,
      open: false,
      defaultAction: ListenerAction.fixedResponse(404, {
        contentType: 'text/plain',
        messageBody: 'Not Found',
      }),
    });

    const cloudFrontOriginPrefixListId = this.cloudFrontOriginPrefixListId(props.stage);

    // CloudFront VPC Origin still needs ALB inbound permission; use the AWS managed list without static pl-* IDs.
    new CfnSecurityGroupIngress(this, 'AlbHttpIngressFromCloudFront', {
      groupId: props.albSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourcePrefixListId: cloudFrontOriginPrefixListId,
      fromPort: 80,
      toPort: 80,
      description: 'Allow CloudFront VPC origin to reach WorkOps ALB',
    });

    this.createParameter(
      'WebIngressAlbArnParameter',
      `/workops/${props.stage}/web-ingress/origin/alb-arn`,
      this.loadBalancer.loadBalancerArn,
    );
    this.createParameter(
      'WebIngressAlbDnsNameParameter',
      `/workops/${props.stage}/web-ingress/origin/alb-dns-name`,
      this.loadBalancer.loadBalancerDnsName,
    );
    this.createParameter(
      'WebIngressAlbSecurityGroupIdParameter',
      `/workops/${props.stage}/web-ingress/origin/alb-security-group-id`,
      props.albSecurityGroup.securityGroupId,
    );

    new CfnOutput(this, 'albDnsName', {
      value: this.loadBalancer.loadBalancerDnsName,
    });
    new CfnOutput(this, 'listenerArn', {
      value: this.listener.listenerArn,
    });
    new CfnOutput(this, 'loadBalancerFullName', {
      value: this.loadBalancer.loadBalancerFullName,
    });
  }

  private createParameter(id: string, parameterName: string, stringValue: string): void {
    const parameter = new StringParameter(this, id, {
      parameterName,
      stringValue,
    });
    parameter.applyRemovalPolicy(RemovalPolicy.DESTROY);
  }

  private cloudFrontOriginPrefixListId(stage: string): string {
    const logGroup = new LogGroup(this, 'CloudFrontOriginPrefixListLogGroup', {
      logGroupName: `/workops/${stage}/web-ingress/cloudfront-origin-prefix-list`,
      retention: RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });
    const describeManagedPrefixListsCall = {
      service: 'EC2',
      action: 'describeManagedPrefixLists',
      parameters: {
        Filters: [
          {
            Name: 'owner-id',
            Values: ['AWS'],
          },
          {
            Name: 'prefix-list-name',
            Values: [CLOUD_FRONT_ORIGIN_PREFIX_LIST_NAME],
          },
        ],
      },
      physicalResourceId: PhysicalResourceId.of(
        `workops-${stage}-cloudfront-origin-prefix-list`,
      ),
      outputPaths: ['PrefixLists.0.PrefixListId'],
    };

    // EC2 DescribeManagedPrefixLists has no resource-level IAM scope, so the action is constrained instead.
    return new AwsCustomResource(this, 'CloudFrontOriginPrefixListId', {
      installLatestAwsSdk: false,
      logGroup,
      onCreate: describeManagedPrefixListsCall,
      onUpdate: describeManagedPrefixListsCall,
      policy: AwsCustomResourcePolicy.fromStatements([
        new PolicyStatement({
          actions: ['ec2:DescribeManagedPrefixLists'],
          resources: ['*'],
        }),
      ]),
    }).getResponseField('PrefixLists.0.PrefixListId');
  }
}

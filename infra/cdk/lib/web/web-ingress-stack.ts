import { RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import {
  AwsCustomResource,
  AwsCustomResourcePolicy,
  PhysicalResourceId,
} from 'aws-cdk-lib/custom-resources';
import { CfnSecurityGroupIngress } from 'aws-cdk-lib/aws-ec2';
import {
  ApplicationLoadBalancer,
  ApplicationListener,
  ApplicationProtocol,
  ListenerAction,
} from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';
import { foundationNetwork, foundationSecurityGroups } from '../shared/contract-imports';
import { readStage, stackName, stagePath } from '../shared/environment';
import { createParameter } from '../shared/ssm-parameters';

const CLOUD_FRONT_ORIGIN_PREFIX_LIST_NAME = 'com.amazonaws.global.cloudfront.origin-facing';

export class WebIngressStack extends Stack {
  public readonly loadBalancer: ApplicationLoadBalancer;
  public readonly listener: ApplicationListener;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'web-ingress'),
    });

    const network = foundationNetwork(this);
    const securityGroups = foundationSecurityGroups(this);

    // WebIngress owns the replaceable ALB side of the CloudFront VPC origin path.
    this.loadBalancer = new ApplicationLoadBalancer(this, 'WebAlb', {
      vpc: network.vpc,
      internetFacing: false,
      loadBalancerName: `workops-${stage}-web-alb`,
      securityGroup: securityGroups.albSecurityGroup,
      vpcSubnets: {
        subnets: network.appSubnets,
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

    const cloudFrontOriginPrefixListId = this.cloudFrontOriginPrefixListId(stage);

    // CloudFront VPC Origin still needs ALB inbound permission; use the AWS managed list without static pl-* IDs.
    new CfnSecurityGroupIngress(this, 'AlbHttpIngressFromCloudFront', {
      groupId: securityGroups.albSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourcePrefixListId: cloudFrontOriginPrefixListId,
      fromPort: 80,
      toPort: 80,
      description: 'Allow CloudFront VPC origin to reach WorkOps ALB',
    });

    createParameter(
      this,
      'WebIngressAlbArnParameter',
      'web-ingress/origin/alb-arn',
      this.loadBalancer.loadBalancerArn,
    );
    createParameter(
      this,
      'WebIngressAlbDnsNameParameter',
      'web-ingress/origin/alb-dns-name',
      this.loadBalancer.loadBalancerDnsName,
    );
    createParameter(
      this,
      'WebIngressAlbSecurityGroupIdParameter',
      'web-ingress/origin/alb-security-group-id',
      securityGroups.albSecurityGroup.securityGroupId,
    );
    createParameter(
      this,
      'WebIngressHttpListenerArnParameter',
      'web-ingress/listener/http-listener-arn',
      this.listener.listenerArn,
    );
    createParameter(
      this,
      'WebIngressAlbFullNameParameter',
      'web-ingress/alb-full-name',
      this.loadBalancer.loadBalancerFullName,
    );
  }

  private cloudFrontOriginPrefixListId(stage: string): string {
    const logGroup = new LogGroup(this, 'CloudFrontOriginPrefixListLogGroup', {
      logGroupName: stagePath(this, 'web-ingress/cloudfront-origin-prefix-list'),
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
      physicalResourceId: PhysicalResourceId.of(`workops-${stage}-cloudfront-origin-prefix-list`),
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

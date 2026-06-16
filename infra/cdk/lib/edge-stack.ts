import { CfnOutput, Duration, Stack, StackProps } from 'aws-cdk-lib';
import {
  AllowedMethods,
  CachePolicy,
  CachedMethods,
  Distribution,
  OriginProtocolPolicy,
  OriginRequestPolicy,
  PriceClass,
  ViewerProtocolPolicy,
} from 'aws-cdk-lib/aws-cloudfront';
import { LoadBalancerV2Origin } from 'aws-cdk-lib/aws-cloudfront-origins';
import { CfnSecurityGroupIngress, ISubnet, SecurityGroup, Vpc } from 'aws-cdk-lib/aws-ec2';
import {
  ApplicationLoadBalancer,
  ApplicationListener,
  ApplicationProtocol,
  ApplicationTargetGroup,
  TargetType,
} from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { Construct } from 'constructs';

export interface EdgeStackProps extends StackProps {
  stage: string;
  vpc: Vpc;
  publicSubnets: ISubnet[];
  albSecurityGroup: SecurityGroup;
}

export class EdgeStack extends Stack {
  public readonly loadBalancer: ApplicationLoadBalancer;
  public readonly listener: ApplicationListener;
  public readonly targetGroup: ApplicationTargetGroup;
  public readonly distribution: Distribution;

  constructor(scope: Construct, id: string, props: EdgeStackProps) {
    super(scope, id, props);

    // The P2-3 edge is intentionally HTTP-only until HTTPS and domains are introduced later.
    new CfnSecurityGroupIngress(this, 'AlbHttpIngress', {
      groupId: props.albSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      cidrIp: '0.0.0.0/0',
      fromPort: 80,
      toPort: 80,
      description: 'Allow public HTTP traffic to WorkOps ALB',
    });

    this.loadBalancer = new ApplicationLoadBalancer(this, 'WebAlb', {
      vpc: props.vpc,
      internetFacing: true,
      loadBalancerName: `workops-${props.stage}-web-alb`,
      securityGroup: props.albSecurityGroup,
      vpcSubnets: {
        subnets: props.publicSubnets,
      },
    });

    this.targetGroup = new ApplicationTargetGroup(this, 'WebTargetGroup', {
      vpc: props.vpc,
      targetType: TargetType.IP,
      protocol: ApplicationProtocol.HTTP,
      port: 8080,
      targetGroupName: `workops-${props.stage}-web-tg`,
      healthCheck: {
        path: '/actuator/health',
        healthyHttpCodes: '200',
        interval: Duration.seconds(30),
        timeout: Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 3,
      },
    });
    this.targetGroup.setAttribute('deregistration_delay.timeout_seconds', '30');

    this.listener = this.loadBalancer.addListener('HttpListener', {
      port: 80,
      protocol: ApplicationProtocol.HTTP,
      open: false,
      defaultTargetGroups: [this.targetGroup],
    });

    // CloudFront is the P2-4 HTTPS entrypoint; caching stays disabled for the dynamic web app.
    this.distribution = new Distribution(this, 'WebDistribution', {
      comment: `workops-${props.stage}-web-edge`,
      defaultBehavior: {
        allowedMethods: AllowedMethods.ALLOW_ALL,
        cachedMethods: CachedMethods.CACHE_GET_HEAD,
        cachePolicy: CachePolicy.CACHING_DISABLED,
        origin: new LoadBalancerV2Origin(this.loadBalancer, {
          protocolPolicy: OriginProtocolPolicy.HTTP_ONLY,
        }),
        originRequestPolicy: OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER,
        viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
      },
      priceClass: PriceClass.PRICE_CLASS_200,
    });

    new CfnOutput(this, 'albDnsName', {
      value: this.loadBalancer.loadBalancerDnsName,
    });
    new CfnOutput(this, 'cloudFrontDomainName', {
      value: this.distribution.distributionDomainName,
    });
    new CfnOutput(this, 'cloudFrontHttpsUrl', {
      value: `https://${this.distribution.distributionDomainName}`,
    });
  }
}

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
import { VpcOrigin } from 'aws-cdk-lib/aws-cloudfront-origins';
import { ISubnet, SecurityGroup, Vpc } from 'aws-cdk-lib/aws-ec2';
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
  appSubnets: ISubnet[];
  albSecurityGroup: SecurityGroup;
}

export class EdgeStack extends Stack {
  public readonly loadBalancer: ApplicationLoadBalancer;
  public readonly listener: ApplicationListener;
  public readonly targetGroup: ApplicationTargetGroup;
  public readonly distribution: Distribution;
  public readonly cloudFrontDomainName: string;
  public readonly cloudFrontHttpsUrl: string;

  constructor(scope: Construct, id: string, props: EdgeStackProps) {
    super(scope, id, props);

    this.loadBalancer = new ApplicationLoadBalancer(this, 'WebAlb', {
      vpc: props.vpc,
      internetFacing: false,
      loadBalancerName: `workops-${props.stage}-web-alb`,
      securityGroup: props.albSecurityGroup,
      vpcSubnets: {
        subnets: props.appSubnets,
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
        origin: VpcOrigin.withApplicationLoadBalancer(this.loadBalancer, {
          protocolPolicy: OriginProtocolPolicy.HTTP_ONLY,
        }),
        originRequestPolicy: OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER,
        viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
      },
      priceClass: PriceClass.PRICE_CLASS_200,
    });

    // P2-5 consumes these CDK tokens for Cognito URLs without storing environment-specific values.
    this.cloudFrontDomainName = this.distribution.distributionDomainName;
    this.cloudFrontHttpsUrl = `https://${this.cloudFrontDomainName}`;

    new CfnOutput(this, 'albDnsName', {
      value: this.loadBalancer.loadBalancerDnsName,
    });
    new CfnOutput(this, 'cloudFrontDomainName', {
      value: this.cloudFrontDomainName,
    });
    new CfnOutput(this, 'cloudFrontHttpsUrl', {
      value: this.cloudFrontHttpsUrl,
    });
  }
}

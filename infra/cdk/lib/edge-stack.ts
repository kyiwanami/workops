import { ArnFormat, CfnOutput, CustomResource, Duration, Stack, StackProps } from 'aws-cdk-lib';
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
import { Provider } from 'aws-cdk-lib/custom-resources';
import {
  CfnSecurityGroupIngress,
  ISubnet,
  PrefixList,
  SecurityGroup,
  Vpc,
} from 'aws-cdk-lib/aws-ec2';
import {
  ApplicationLoadBalancer,
  ApplicationListener,
  ApplicationProtocol,
  ApplicationTargetGroup,
  TargetType,
} from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Runtime } from 'aws-cdk-lib/aws-lambda';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { ILogGroup } from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';
import { join } from 'path';

export interface EdgeStackProps extends StackProps {
  stage: string;
  vpc: Vpc;
  appSubnets: ISubnet[];
  albSecurityGroup: SecurityGroup;
  cognitoUserPoolId: string;
  cognitoPlatformUserPoolClientId: string;
  cognitoTenantUserPoolClientId: string;
  cognitoClientUrlUpdaterLogGroup: ILogGroup;
  cognitoClientUrlUpdaterProviderLogGroup: ILogGroup;
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

    const cloudFrontOriginPrefixList = PrefixList.fromLookup(
      this,
      'CloudFrontOriginFacingPrefixList',
      {
        ownerId: 'AWS',
        prefixListName: 'com.amazonaws.global.cloudfront.origin-facing',
      },
    );
    // CloudFront VPC Origin still needs ALB inbound permission; use the AWS managed list without static pl-* IDs.
    new CfnSecurityGroupIngress(this, 'AlbHttpIngressFromCloudFront', {
      groupId: props.albSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourcePrefixListId: cloudFrontOriginPrefixList.prefixListId,
      fromPort: 80,
      toPort: 80,
      description: 'Allow CloudFront VPC origin to reach WorkOps ALB',
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

    const updaterFunction = new NodejsFunction(this, 'CognitoClientUrlUpdaterFunction', {
      functionName: `workops-${props.stage}-cognito-client-url-updater`,
      runtime: Runtime.NODEJS_22_X,
      entry: join(__dirname, '..', 'custom-resources', 'cognito-client-url-updater', 'index.ts'),
      handler: 'handler',
      timeout: Duration.minutes(1),
      logGroup: props.cognitoClientUrlUpdaterLogGroup,
      bundling: {
        bundleAwsSDK: true,
      },
    });
    updaterFunction.addToRolePolicy(
      new PolicyStatement({
        actions: ['cognito-idp:DescribeUserPoolClient', 'cognito-idp:UpdateUserPoolClient'],
        resources: [
          this.formatArn({
            service: 'cognito-idp',
            resource: 'userpool',
            resourceName: props.cognitoUserPoolId,
            arnFormat: ArnFormat.SLASH_RESOURCE_NAME,
          }),
        ],
      }),
    );
    const updaterProvider = new Provider(this, 'CognitoClientUrlUpdaterProvider', {
      onEventHandler: updaterFunction,
      logGroup: props.cognitoClientUrlUpdaterProviderLogGroup,
    });

    // The custom resource registers the current CloudFront endpoint without making IdentityStack depend on EdgeStack.
    new CustomResource(this, 'CognitoClientUrlUpdater', {
      resourceType: 'Custom::WorkOpsCognitoClientUrlUpdater',
      serviceToken: updaterProvider.serviceToken,
      properties: {
        UserPoolId: props.cognitoUserPoolId,
        PlatformClientId: props.cognitoPlatformUserPoolClientId,
        TenantClientId: props.cognitoTenantUserPoolClientId,
        CloudFrontDomainName: this.cloudFrontDomainName,
      },
    });

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

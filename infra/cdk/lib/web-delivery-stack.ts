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
import { ApplicationLoadBalancer } from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Runtime } from 'aws-cdk-lib/aws-lambda';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { ILogGroup } from 'aws-cdk-lib/aws-logs';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Provider } from 'aws-cdk-lib/custom-resources';
import { Construct } from 'constructs';
import { join } from 'path';
import { readWorkopsStage, workopsStackName } from './environment';

export interface WebDeliveryStackProps extends StackProps {
  webAclArn: string;
  cognitoUserPoolId: string;
  cognitoPlatformUserPoolClientId: string;
  cognitoTenantUserPoolClientId: string;
  cognitoClientUrlUpdaterLogGroup: ILogGroup;
  cognitoClientUrlUpdaterProviderLogGroup: ILogGroup;
}

export class WebDeliveryStack extends Stack {
  public readonly distribution: Distribution;
  public readonly cloudFrontDomainName: string;
  public readonly cloudFrontHttpsUrl: string;

  constructor(scope: Construct, id: string, props: WebDeliveryStackProps) {
    const stage = readWorkopsStage(scope);
    super(scope, id, {
      ...props,
      stackName: workopsStackName(scope, 'web-delivery'),
    });

    const loadBalancer = ApplicationLoadBalancer.fromApplicationLoadBalancerAttributes(
      this,
      'WebIngressAlb',
      {
        loadBalancerArn: StringParameter.valueForStringParameter(
          this,
          `/workops/${stage}/web-ingress/origin/alb-arn`,
        ),
        loadBalancerDnsName: StringParameter.valueForStringParameter(
          this,
          `/workops/${stage}/web-ingress/origin/alb-dns-name`,
        ),
        securityGroupId: StringParameter.valueForStringParameter(
          this,
          `/workops/${stage}/web-ingress/origin/alb-security-group-id`,
        ),
      },
    );

    // CloudFront stays isolated from ALB replacement except when the ingress origin contract changes.
    this.distribution = new Distribution(this, 'WebDistribution', {
      comment: `workops-${stage}-web-delivery`,
      defaultBehavior: {
        allowedMethods: AllowedMethods.ALLOW_ALL,
        cachedMethods: CachedMethods.CACHE_GET_HEAD,
        cachePolicy: CachePolicy.CACHING_DISABLED,
        origin: VpcOrigin.withApplicationLoadBalancer(loadBalancer, {
          protocolPolicy: OriginProtocolPolicy.HTTP_ONLY,
        }),
        originRequestPolicy: OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER,
        viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
      },
      priceClass: PriceClass.PRICE_CLASS_200,
      webAclId: props.webAclArn,
    });

    this.cloudFrontDomainName = this.distribution.distributionDomainName;
    this.cloudFrontHttpsUrl = `https://${this.cloudFrontDomainName}`;

    const updaterFunction = new NodejsFunction(this, 'CognitoClientUrlUpdaterFunction', {
      functionName: `workops-${stage}-cognito-client-url-updater`,
      runtime: Runtime.NODEJS_24_X,
      entry: join(__dirname, '..', 'lambda', 'cognito-client-url-updater', 'index.ts'),
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

    // The custom resource registers the current CloudFront endpoint without making IdentityStack depend on delivery.
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
    new CfnOutput(this, 'cloudFrontHttpsUrl', {
      value: this.cloudFrontHttpsUrl,
    });
  }
}

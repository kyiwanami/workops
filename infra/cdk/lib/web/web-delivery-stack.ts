import {
  ArnFormat,
  CfnOutput,
  CustomResource,
  Duration,
  RemovalPolicy,
  Stack,
  StackProps,
} from 'aws-cdk-lib';
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
import { Provider } from 'aws-cdk-lib/custom-resources';
import { Construct } from 'constructs';
import { join } from 'path';
import { contractValue, logsGroup } from '../shared/contract-imports';
import { readStage, stackName } from '../shared/environment';
import { createParameter } from '../shared/ssm-parameters';

export class WebDeliveryStack extends Stack {
  public readonly distribution: Distribution;
  public readonly cloudFrontDomainName: string;
  public readonly cloudFrontHttpsUrl: string;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'web-delivery'),
    });

    const userPoolId = contractValue(this, 'identity/user-pool-id');
    const platformClientId = contractValue(this, 'identity/platform-client-id');
    const tenantClientId = contractValue(this, 'identity/tenant-client-id');
    const cognitoClientUrlUpdaterLogGroup = logsGroup(
      this,
      'CognitoClientUrlUpdaterLogGroup',
      'lambda/cognito-client-url-updater',
    );
    const cognitoClientUrlUpdaterProviderLogGroup = logsGroup(
      this,
      'CognitoClientUrlUpdaterProviderLogGroup',
      'lambda/cognito-client-url-updater-provider',
    );
    const loadBalancer = ApplicationLoadBalancer.fromApplicationLoadBalancerAttributes(
      this,
      'WebIngressAlb',
      {
        loadBalancerArn: contractValue(this, 'web-ingress/origin/alb-arn'),
        loadBalancerDnsName: contractValue(
          this,
          'web-ingress/origin/alb-dns-name',
        ),
        securityGroupId: contractValue(
          this,
          'web-ingress/origin/alb-security-group-id',
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
      webAclId: contractValue(this, 'web-acl/cloudfront-web-acl-arn'),
    });

    this.cloudFrontDomainName = this.distribution.distributionDomainName;
    this.cloudFrontHttpsUrl = `https://${this.cloudFrontDomainName}`;
    createParameter(
      this,
      'CloudFrontHttpsUrlParameter',
      'web-delivery/cloudfront-https-url',
      this.cloudFrontHttpsUrl,
    );

    const updaterFunction = new NodejsFunction(this, 'CognitoClientUrlUpdaterFunction', {
      functionName: `workops-${stage}-cognito-client-url-updater`,
      runtime: Runtime.NODEJS_24_X,
      entry: join(__dirname, 'lambda', 'cognito-client-url-updater.ts'),
      handler: 'handler',
      timeout: Duration.minutes(1),
      logGroup: cognitoClientUrlUpdaterLogGroup,
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
            resourceName: userPoolId,
            arnFormat: ArnFormat.SLASH_RESOURCE_NAME,
          }),
        ],
      }),
    );
    const updaterProvider = new Provider(this, 'CognitoClientUrlUpdaterProvider', {
      onEventHandler: updaterFunction,
      logGroup: cognitoClientUrlUpdaterProviderLogGroup,
    });

    // The custom resource registers the current CloudFront endpoint without making IdentityStack depend on delivery.
    new CustomResource(this, 'CognitoClientUrlUpdater', {
      resourceType: 'Custom::WorkOpsCognitoClientUrlUpdater',
      serviceToken: updaterProvider.serviceToken,
      properties: {
        UserPoolId: userPoolId,
        PlatformClientId: platformClientId,
        TenantClientId: tenantClientId,
        CloudFrontDomainName: this.cloudFrontDomainName,
      },
    });
    new CfnOutput(this, 'cloudFrontHttpsUrl', {
      value: this.cloudFrontHttpsUrl,
    });
  }

}

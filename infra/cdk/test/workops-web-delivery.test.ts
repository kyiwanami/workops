import { Match, Template } from 'aws-cdk-lib/assertions';
import { readFileSync } from 'fs';
import { join } from 'path';
import { WebAclStack } from '../lib/web/web-acl-stack';
import { WebDeliveryStack } from '../lib/web/web-delivery-stack';
import { WebIngressStack } from '../lib/web/web-ingress-stack';
import { createTestApp, testEnv } from './workops-test-fixtures';

describe('WorkOps CDK web delivery', () => {
  test('creates the P2-beta web ingress, delivery, and ACL stacks', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const webAclStack = new WebAclStack(app, 'WebAclStack', {
      crossRegionReferences: true,
      env: {
        account: testEnv.account,
        region: 'us-east-1',
      },
    });
    const webIngressStack = new WebIngressStack(app, 'WebIngressStack', {
      env: testEnv,
    });
    const webDeliveryStack = new WebDeliveryStack(app, 'WebDeliveryStack', {
      crossRegionReferences: true,
      env: testEnv,
    });
    const ingressTemplate = Template.fromStack(webIngressStack);
    const deliveryTemplate = Template.fromStack(webDeliveryStack);
    const aclTemplate = Template.fromStack(webAclStack);
    const ingressTemplateText = JSON.stringify(ingressTemplate.toJSON());
    const deliveryTemplateText = JSON.stringify(deliveryTemplate.toJSON());
    const ingressStackSource = readFileSync(
      join(__dirname, '..', 'lib', 'web', 'web-ingress-stack.ts'),
      'utf8',
    );

    ingressTemplate.resourceCountIs('AWS::EC2::SecurityGroupIngress', 1);
    ingressTemplate.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
      Description: 'Allow CloudFront VPC origin to reach WorkOps ALB',
      FromPort: 80,
      GroupId: Match.anyValue(),
      IpProtocol: 'tcp',
      SourcePrefixListId: Match.anyValue(),
      ToPort: 80,
    });
    ingressTemplate.hasResourceProperties('Custom::AWS', {
      Create: Match.stringLikeRegexp('describeManagedPrefixLists'),
      InstallLatestAwsSdk: false,
      Update: Match.stringLikeRegexp('describeManagedPrefixLists'),
    });
    ingressTemplate.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: '/workops/dev/web-ingress/cloudfront-origin-prefix-list',
      RetentionInDays: 7,
    });
    ingressTemplate.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: 'ec2:DescribeManagedPrefixLists',
            Effect: 'Allow',
            Resource: '*',
          }),
        ]),
      },
    });
    expect(ingressTemplateText).toContain('com.amazonaws.global.cloudfront.origin-facing');
    ingressTemplate.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
      Name: 'workops-dev-web-alb',
      Scheme: 'internal',
      Type: 'application',
    });
    ingressTemplate.hasResourceProperties('AWS::ElasticLoadBalancingV2::Listener', {
      DefaultActions: Match.arrayWith([
        Match.objectLike({
          FixedResponseConfig: Match.objectLike({
            ContentType: 'text/plain',
            MessageBody: 'Not Found',
            StatusCode: '404',
          }),
          Type: 'fixed-response',
        }),
      ]),
      Port: 80,
      Protocol: 'HTTP',
    });
    ingressTemplate.resourceCountIs('AWS::ElasticLoadBalancingV2::TargetGroup', 0);
    ingressTemplate.resourceCountIs('AWS::SSM::Parameter', 5);
    ingressTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/web-ingress/origin/alb-arn',
      Type: 'String',
    });
    ingressTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/web-ingress/origin/alb-dns-name',
      Type: 'String',
    });
    ingressTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/web-ingress/origin/alb-security-group-id',
      Type: 'String',
    });
    ingressTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/web-ingress/listener/http-listener-arn',
      Type: 'String',
    });
    ingressTemplate.hasResourceProperties('AWS::SSM::Parameter', {
      Name: '/workops/dev/web-ingress/alb-full-name',
      Type: 'String',
    });
    ingressTemplate.resourceCountIs('AWS::CloudFront::Distribution', 0);
    ingressTemplate.resourceCountIs('AWS::CloudFront::VpcOrigin', 0);

    deliveryTemplate.hasResourceProperties('AWS::CloudFront::Distribution', {
      DistributionConfig: Match.objectLike({
        Comment: 'workops-dev-web-delivery',
        DefaultCacheBehavior: Match.objectLike({
          AllowedMethods: ['GET', 'HEAD', 'OPTIONS', 'PUT', 'PATCH', 'POST', 'DELETE'],
          CachedMethods: ['GET', 'HEAD'],
          CachePolicyId: '4135ea2d-6df8-44a3-9df3-4b5a84be39ad',
          OriginRequestPolicyId: 'b689b0a8-53d0-40ab-baf2-68738e2966ac',
          TargetOriginId: Match.anyValue(),
          ViewerProtocolPolicy: 'redirect-to-https',
        }),
        Origins: Match.arrayWith([
          Match.objectLike({
            VpcOriginConfig: Match.objectLike({
              VpcOriginId: {
                'Fn::GetAtt': [Match.stringLikeRegexp('WebDistributionOrigin1VpcOrigin'), 'Id'],
              },
            }),
          }),
        ]),
        PriceClass: 'PriceClass_200',
        WebACLId: Match.anyValue(),
      }),
    });
    deliveryTemplate.hasResourceProperties('AWS::CloudFront::VpcOrigin', {
      VpcOriginEndpointConfig: Match.objectLike({
        Arn: Match.anyValue(),
        OriginProtocolPolicy: 'http-only',
        OriginSSLProtocols: ['TLSv1.2'],
      }),
    });
    expect(deliveryTemplateText).toContain('/workops/dev/web-ingress/origin/alb-arn');
    expect(deliveryTemplateText).toContain('/workops/dev/web-ingress/origin/alb-dns-name');
    expect(deliveryTemplateText).toContain('/workops/dev/web-ingress/origin/alb-security-group-id');
    expect(deliveryTemplateText).toContain('/workops/dev/web-acl/cloudfront-web-acl-arn');
    expect(deliveryTemplateText).toContain('/workops/dev/identity/user-pool-id');
    expect(deliveryTemplateText).toContain('/workops/dev/identity/platform-client-id');
    expect(deliveryTemplateText).toContain('/workops/dev/identity/tenant-client-id');
    deliveryTemplate.hasOutput('cloudFrontHttpsUrl', {});
    deliveryTemplate.hasResourceProperties('AWS::Lambda::Function', {
      FunctionName: 'workops-dev-cognito-client-url-updater',
      Handler: 'index.handler',
      Runtime: 'nodejs24.x',
      Timeout: 60,
    });
    deliveryTemplate.hasResourceProperties('AWS::Lambda::Function', {
      LoggingConfig: {
        LogGroup: Match.anyValue(),
      },
    });
    deliveryTemplate.resourceCountIs('AWS::Logs::LogGroup', 0);
    expect(deliveryTemplateText).toContain('/workops/dev/lambda/cognito-client-url-updater');
    expect(deliveryTemplateText).toContain(
      '/workops/dev/lambda/cognito-client-url-updater-provider',
    );
    expect(deliveryTemplateText).not.toContain(
      '/workops/dev/custom-resources/cognito-client-url-updater',
    );
    expect(deliveryTemplateText).not.toContain(
      '/workops/dev/custom-resources/cognito-client-url-updater-provider',
    );
    deliveryTemplate.hasResourceProperties('AWS::IAM::Policy', {
      PolicyDocument: {
        Statement: Match.arrayWith([
          Match.objectLike({
            Action: ['cognito-idp:DescribeUserPoolClient', 'cognito-idp:UpdateUserPoolClient'],
            Effect: 'Allow',
          }),
        ]),
      },
    });
    deliveryTemplate.resourceCountIs('Custom::WorkOpsCognitoClientUrlUpdater', 1);
    deliveryTemplate.hasResourceProperties('Custom::WorkOpsCognitoClientUrlUpdater', {
      UserPoolId: Match.anyValue(),
      PlatformClientId: Match.anyValue(),
      TenantClientId: Match.anyValue(),
      CloudFrontDomainName: Match.anyValue(),
    });
    expect(deliveryTemplateText).not.toContain('"CidrIp":"0.0.0.0/0"');
    expect(deliveryTemplateText).not.toContain('authenticate-cognito');
    expect(ingressStackSource).toContain('com.amazonaws.global.cloudfront.origin-facing');
    expect(ingressStackSource).not.toContain('pl-58a04531');
    deliveryTemplate.resourceCountIs('AWS::WAFv2::WebACLAssociation', 0);

    aclTemplate.hasResourceProperties('AWS::WAFv2::WebACL', {
      DefaultAction: {
        Allow: {},
      },
      Name: 'workops-dev-cloudfront-web-acl',
      Scope: 'CLOUDFRONT',
      Rules: Match.arrayWith([
        Match.objectLike({
          Name: 'AWSManagedRulesCommonRuleSet',
          OverrideAction: {
            Count: {},
          },
          Statement: {
            ManagedRuleGroupStatement: {
              Name: 'AWSManagedRulesCommonRuleSet',
              VendorName: 'AWS',
            },
          },
        }),
      ]),
    });
    aclTemplate.hasResourceProperties('AWS::Logs::LogGroup', {
      LogGroupName: 'aws-waf-logs-workops-dev-cloudfront',
      RetentionInDays: 7,
    });
    aclTemplate.resourceCountIs('AWS::WAFv2::LoggingConfiguration', 1);
  });
});

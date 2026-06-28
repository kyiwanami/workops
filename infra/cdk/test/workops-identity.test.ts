import { Match, Template } from 'aws-cdk-lib/assertions';
import { IdentityStack } from '../lib/identity/identity-stack';
import { createTestApp } from './workops-test-fixtures';

describe('WorkOps CDK identity', () => {
  test('creates the P2-5 IdentityStack Cognito Hosted UI resources', () => {
    const stage = 'dev';
    const app = createTestApp(stage);
    const identityStack = new IdentityStack(app, 'IdentityStack', {});
    const template = Template.fromStack(identityStack);
    const templateText = JSON.stringify(template.toJSON());

    template.resourceCountIs('AWS::Cognito::UserPool', 1);
    template.resourceCountIs('AWS::Cognito::UserPoolDomain', 1);
    template.resourceCountIs('AWS::Cognito::UserPoolClient', 2);
    template.resourceCountIs('AWS::Cognito::ManagedLoginBranding', 2);
    template.hasResourceProperties('AWS::Cognito::UserPool', {
      UserPoolName: 'workops-dev-user-pool',
      UserPoolTier: 'ESSENTIALS',
      AutoVerifiedAttributes: ['email'],
      MfaConfiguration: 'OFF',
      UsernameConfiguration: {
        CaseSensitive: false,
      },
      AdminCreateUserConfig: {
        AllowAdminCreateUserOnly: true,
        InviteMessageTemplate: {
          EmailSubject: 'WorkOps アカウント作成のお知らせ',
          EmailMessage: Match.anyValue(),
        },
      },
      AccountRecoverySetting: {
        RecoveryMechanisms: [
          {
            Name: 'verified_email',
            Priority: 1,
          },
        ],
      },
      Policies: {
        PasswordPolicy: Match.objectLike({
          MinimumLength: 8,
          RequireLowercase: true,
          RequireUppercase: true,
          RequireNumbers: true,
          RequireSymbols: false,
        }),
      },
      Schema: Match.arrayWith([
        Match.objectLike({
          Name: 'email',
          Required: true,
          Mutable: true,
        }),
      ]),
    });
    template.hasResource('AWS::Cognito::UserPool', {
      DeletionPolicy: 'Delete',
      UpdateReplacePolicy: 'Delete',
    });
    template.hasResourceProperties('AWS::Cognito::UserPoolDomain', {
      Domain: {
        'Fn::Join': [
          '',
          [
            'workops-dev-',
            {
              Ref: 'AWS::AccountId',
            },
          ],
        ],
      },
      ManagedLoginVersion: 2,
    });
    template.hasResourceProperties('AWS::Cognito::UserPoolClient', {
      ClientName: 'workops-dev-platform-client',
      GenerateSecret: false,
      AllowedOAuthFlowsUserPoolClient: true,
      AllowedOAuthFlows: ['code'],
      AllowedOAuthScopes: ['openid', 'email'],
      SupportedIdentityProviders: ['COGNITO'],
    });
    template.hasResourceProperties('AWS::Cognito::UserPoolClient', {
      ClientName: 'workops-dev-tenant-client',
      GenerateSecret: false,
      AllowedOAuthFlowsUserPoolClient: true,
      AllowedOAuthFlows: ['code'],
      AllowedOAuthScopes: ['openid', 'email'],
      SupportedIdentityProviders: ['COGNITO'],
    });
    template.hasResourceProperties('AWS::Cognito::ManagedLoginBranding', {
      UserPoolId: Match.anyValue(),
      ClientId: Match.anyValue(),
      ReturnMergedResources: false,
      UseCognitoProvidedValues: false,
      Settings: Match.objectLike({
        components: Match.objectLike({
          pageBackground: Match.anyValue(),
          primaryButton: Match.anyValue(),
        }),
      }),
    });
    template.hasResource('AWS::Cognito::UserPoolClient', {
      Properties: Match.objectLike({
        CallbackURLs: ['https://workops-dev-placeholder.invalid/login/oauth2/code/platform'],
        LogoutURLs: ['https://workops-dev-placeholder.invalid/login'],
        DefaultRedirectURI: 'https://workops-dev-placeholder.invalid/login/oauth2/code/platform',
      }),
    });
    template.hasResource('AWS::Cognito::UserPoolClient', {
      Properties: Match.objectLike({
        CallbackURLs: ['https://workops-dev-placeholder.invalid/login/oauth2/code/tenant'],
        LogoutURLs: ['https://workops-dev-placeholder.invalid/login'],
        DefaultRedirectURI: 'https://workops-dev-placeholder.invalid/login/oauth2/code/tenant',
      }),
    });
    const brandingTemplateText = JSON.stringify(
      template.findResources('AWS::Cognito::ManagedLoginBranding'),
    );
    expect(templateText).toContain('WorkOps アカウント作成のお知らせ');
    expect(templateText).toContain('WorkOps アカウントを作成しました。');
    expect(templateText).toContain('{username}');
    expect(templateText).toContain('{####}');
    expect(brandingTemplateText).not.toContain('"Assets"');
    expect(brandingTemplateText).toContain('5f1b1bff');
    expect(brandingTemplateText).toContain('0972d3ff');
    expect(template.toJSON()).not.toHaveProperty('Outputs');
    template.resourceCountIs('AWS::CloudFront::Distribution', 0);
    template.resourceCountIs('AWS::ElasticLoadBalancingV2::LoadBalancer', 0);
    template.resourceCountIs('AWS::ECS::Service', 0);
    template.resourceCountIs('AWS::EC2::NatGateway', 0);
    expect(templateText).not.toContain('EdgeStack');
    expect(templateText).not.toContain('WebDistribution');
    expect(templateText).not.toContain('WebAlb');
    expect(templateText).not.toContain('localhost');
    expect(templateText).not.toContain('workops-dev-web-client');
    expect(templateText).not.toContain('/login/oauth2/code/cognito');
  });
});

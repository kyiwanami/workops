import { Aws, CfnOutput, Fn, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import {
  AccountRecovery,
  CfnManagedLoginBranding,
  FeaturePlan,
  ManagedLoginVersion,
  Mfa,
  OAuthScope,
  UserPool,
  UserPoolClient,
  UserPoolDomain,
} from 'aws-cdk-lib/aws-cognito';
import { Construct } from 'constructs';

export interface IdentityStackProps extends StackProps {
  stage: string;
}

export class IdentityStack extends Stack {
  public readonly userPool: UserPool;
  public readonly platformUserPoolClient: UserPoolClient;
  public readonly tenantUserPoolClient: UserPoolClient;
  public readonly userPoolDomain: UserPoolDomain;
  public readonly userPoolId: string;
  public readonly platformUserPoolClientId: string;
  public readonly tenantUserPoolClientId: string;
  public readonly hostedUiDomainBaseUrl: string;

  constructor(scope: Construct, id: string, props: IdentityStackProps) {
    super(scope, id, props);

    // IdentityStack owns Cognito resources independently from recreated runtime stacks.
    this.userPool = new UserPool(this, 'UserPool', {
      userPoolName: `workops-${props.stage}-user-pool`,
      selfSignUpEnabled: false,
      signInAliases: {
        username: true,
      },
      signInCaseSensitive: false,
      autoVerify: {
        email: true,
      },
      standardAttributes: {
        email: {
          required: true,
          mutable: true,
        },
      },
      mfa: Mfa.OFF,
      accountRecovery: AccountRecovery.EMAIL_ONLY,
      passwordPolicy: {
        minLength: 8,
        requireLowercase: true,
        requireUppercase: true,
        requireDigits: true,
        requireSymbols: false,
      },
      featurePlan: FeaturePlan.ESSENTIALS,
      removalPolicy: RemovalPolicy.DESTROY,
    });

    this.userPoolDomain = this.userPool.addDomain('UserPoolDomain', {
      cognitoDomain: {
        domainPrefix: `workops-${props.stage}-${Aws.ACCOUNT_ID}`,
      },
      managedLoginVersion: ManagedLoginVersion.NEWER_MANAGED_LOGIN,
    });

    const platformPlaceholderCallbackUrl = `https://workops-${props.stage}-placeholder.invalid/login/oauth2/code/platform`;
    const tenantPlaceholderCallbackUrl = `https://workops-${props.stage}-placeholder.invalid/login/oauth2/code/tenant`;
    const placeholderLogoutUrl = `https://workops-${props.stage}-placeholder.invalid/login`;

    this.platformUserPoolClient = this.userPool.addClient('PlatformClient', {
      userPoolClientName: `workops-${props.stage}-platform-client`,
      generateSecret: false,
      oAuth: {
        flows: {
          authorizationCodeGrant: true,
        },
        scopes: [
          OAuthScope.OPENID,
          OAuthScope.EMAIL,
        ],
        // Cognito requires a callback URL at App Client creation; EdgeStack replaces it with CloudFront.
        callbackUrls: [platformPlaceholderCallbackUrl],
        logoutUrls: [placeholderLogoutUrl],
        defaultRedirectUri: platformPlaceholderCallbackUrl,
      },
    });

    this.tenantUserPoolClient = this.userPool.addClient('TenantClient', {
      userPoolClientName: `workops-${props.stage}-tenant-client`,
      generateSecret: false,
      oAuth: {
        flows: {
          authorizationCodeGrant: true,
        },
        scopes: [
          OAuthScope.OPENID,
          OAuthScope.EMAIL,
        ],
        // Cognito requires a callback URL at App Client creation; EdgeStack replaces it with CloudFront.
        callbackUrls: [tenantPlaceholderCallbackUrl],
        logoutUrls: [placeholderLogoutUrl],
        defaultRedirectUri: tenantPlaceholderCallbackUrl,
      },
    });

    // Keep hosted auth pages on Managed Login v2 with Cognito's default branding.
    new CfnManagedLoginBranding(this, 'PlatformManagedLoginBranding', {
      userPoolId: this.userPool.userPoolId,
      clientId: this.platformUserPoolClient.userPoolClientId,
      useCognitoProvidedValues: true,
    });
    new CfnManagedLoginBranding(this, 'TenantManagedLoginBranding', {
      userPoolId: this.userPool.userPoolId,
      clientId: this.tenantUserPoolClient.userPoolClientId,
      useCognitoProvidedValues: true,
    });

    this.userPoolId = this.userPool.userPoolId;
    this.platformUserPoolClientId = this.platformUserPoolClient.userPoolClientId;
    this.tenantUserPoolClientId = this.tenantUserPoolClient.userPoolClientId;
    this.hostedUiDomainBaseUrl = Fn.sub('https://${Domain}.auth.${AWS::Region}.amazoncognito.com', {
      Domain: this.userPoolDomain.domainName,
    });

    new CfnOutput(this, 'userPoolId', {
      value: this.userPoolId,
    });
    new CfnOutput(this, 'platformUserPoolClientId', {
      value: this.platformUserPoolClientId,
    });
    new CfnOutput(this, 'tenantUserPoolClientId', {
      value: this.tenantUserPoolClientId,
    });
    new CfnOutput(this, 'hostedUiDomainBaseUrl', {
      value: this.hostedUiDomainBaseUrl,
    });
  }
}

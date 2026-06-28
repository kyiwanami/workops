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
import { readWorkopsStage, workopsStackName } from './environment';
import { exportName } from './stack-exports';

function managedLoginSettings(
  primaryColor: string,
  primaryHoverColor: string,
  pageColor: string,
  formBorderColor: string,
) {
  return {
    components: {
      form: {
        lightMode: {
          backgroundColor: 'ffffffff',
          borderColor: formBorderColor,
        },
      },
      pageBackground: {
        image: {
          enabled: false,
        },
        lightMode: {
          color: pageColor,
        },
      },
      primaryButton: {
        lightMode: {
          active: {
            backgroundColor: primaryHoverColor,
            textColor: 'ffffffff',
          },
          defaults: {
            backgroundColor: primaryColor,
            textColor: 'ffffffff',
          },
          disabled: {
            backgroundColor: 'ffffffff',
            borderColor: 'ffffffff',
          },
          hover: {
            backgroundColor: primaryHoverColor,
            textColor: 'ffffffff',
          },
        },
      },
    },
  };
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

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readWorkopsStage(scope);
    super(scope, id, {
      ...props,
      stackName: workopsStackName(scope, 'identity'),
    });

    // IdentityStack owns Cognito resources independently from recreated runtime stacks.
    this.userPool = new UserPool(this, 'UserPool', {
      userPoolName: `workops-${stage}-user-pool`,
      selfSignUpEnabled: false,
      // AdminCreateUser sends this WorkOps invitation while Cognito owns the temporary password flow.
      userInvitation: {
        emailSubject: 'WorkOps アカウント作成のお知らせ',
        emailBody: [
          '<p>WorkOps アカウントを作成しました。</p>',
          '<p>ユーザー名: <strong>{username}</strong></p>',
          '<p>一時パスワード: <strong>{####}</strong></p>',
          '<p>初回ログイン時に新しいパスワードを設定してください。</p>',
        ].join(''),
      },
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
        domainPrefix: `workops-${stage}-${Aws.ACCOUNT_ID}`,
      },
      managedLoginVersion: ManagedLoginVersion.NEWER_MANAGED_LOGIN,
    });

    const platformPlaceholderCallbackUrl = `https://workops-${stage}-placeholder.invalid/login/oauth2/code/platform`;
    const tenantPlaceholderCallbackUrl = `https://workops-${stage}-placeholder.invalid/login/oauth2/code/tenant`;
    const placeholderLogoutUrl = `https://workops-${stage}-placeholder.invalid/login`;

    this.platformUserPoolClient = this.userPool.addClient('PlatformClient', {
      userPoolClientName: `workops-${stage}-platform-client`,
      generateSecret: false,
      oAuth: {
        flows: {
          authorizationCodeGrant: true,
        },
        scopes: [OAuthScope.OPENID, OAuthScope.EMAIL],
        // Cognito requires a callback URL at App Client creation; WebDeliveryStack replaces it with CloudFront.
        callbackUrls: [platformPlaceholderCallbackUrl],
        logoutUrls: [placeholderLogoutUrl],
        defaultRedirectUri: platformPlaceholderCallbackUrl,
      },
    });

    this.tenantUserPoolClient = this.userPool.addClient('TenantClient', {
      userPoolClientName: `workops-${stage}-tenant-client`,
      generateSecret: false,
      oAuth: {
        flows: {
          authorizationCodeGrant: true,
        },
        scopes: [OAuthScope.OPENID, OAuthScope.EMAIL],
        // Cognito requires a callback URL at App Client creation; WebDeliveryStack replaces it with CloudFront.
        callbackUrls: [tenantPlaceholderCallbackUrl],
        logoutUrls: [placeholderLogoutUrl],
        defaultRedirectUri: tenantPlaceholderCallbackUrl,
      },
    });

    // Managed Login branding differs by App Client so users can identify the active route inside Cognito.
    new CfnManagedLoginBranding(this, 'PlatformManagedLoginBranding', {
      userPoolId: this.userPool.userPoolId,
      clientId: this.platformUserPoolClient.userPoolClientId,
      returnMergedResources: false,
      settings: managedLoginSettings('5f1b1bff', '7f2525ff', 'fff7f7ff', 'd84a4aff'),
      useCognitoProvidedValues: false,
    });
    new CfnManagedLoginBranding(this, 'TenantManagedLoginBranding', {
      userPoolId: this.userPool.userPoolId,
      clientId: this.tenantUserPoolClient.userPoolClientId,
      returnMergedResources: false,
      settings: managedLoginSettings('0972d3ff', '033160ff', 'f6f9fcff', '7ba7d9ff'),
      useCognitoProvidedValues: false,
    });

    this.userPoolId = this.userPool.userPoolId;
    this.platformUserPoolClientId = this.platformUserPoolClient.userPoolClientId;
    this.tenantUserPoolClientId = this.tenantUserPoolClient.userPoolClientId;
    this.hostedUiDomainBaseUrl = Fn.sub('https://${Domain}.auth.${AWS::Region}.amazoncognito.com', {
      Domain: this.userPoolDomain.domainName,
    });

    new CfnOutput(this, 'userPoolId', {
      exportName: exportName(stage, 'identity-user-pool-id'),
      value: this.userPoolId,
    });
    new CfnOutput(this, 'platformUserPoolClientId', {
      exportName: exportName(stage, 'identity-platform-user-pool-client-id'),
      value: this.platformUserPoolClientId,
    });
    new CfnOutput(this, 'tenantUserPoolClientId', {
      exportName: exportName(stage, 'identity-tenant-user-pool-client-id'),
      value: this.tenantUserPoolClientId,
    });
    new CfnOutput(this, 'hostedUiDomainBaseUrl', {
      exportName: exportName(stage, 'identity-hosted-ui-domain-base-url'),
      value: this.hostedUiDomainBaseUrl,
    });
  }
}

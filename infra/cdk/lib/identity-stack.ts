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
  public readonly userPoolClient: UserPoolClient;
  public readonly userPoolDomain: UserPoolDomain;
  public readonly userPoolId: string;
  public readonly userPoolClientId: string;
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

    const placeholderCallbackUrl = `https://workops-${props.stage}-placeholder.invalid/login/oauth2/code/cognito`;
    const placeholderLogoutUrl = `https://workops-${props.stage}-placeholder.invalid/`;

    this.userPoolClient = this.userPool.addClient('WebClient', {
      userPoolClientName: `workops-${props.stage}-web-client`,
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
        callbackUrls: [placeholderCallbackUrl],
        logoutUrls: [placeholderLogoutUrl],
        defaultRedirectUri: placeholderCallbackUrl,
      },
    });

    // Keep hosted auth pages on Managed Login v2 with Cognito's default branding.
    new CfnManagedLoginBranding(this, 'ManagedLoginBranding', {
      userPoolId: this.userPool.userPoolId,
      clientId: this.userPoolClient.userPoolClientId,
      useCognitoProvidedValues: true,
    });

    this.userPoolId = this.userPool.userPoolId;
    this.userPoolClientId = this.userPoolClient.userPoolClientId;
    this.hostedUiDomainBaseUrl = Fn.sub('https://${Domain}.auth.${AWS::Region}.amazoncognito.com', {
      Domain: this.userPoolDomain.domainName,
    });

    new CfnOutput(this, 'userPoolId', {
      value: this.userPoolId,
    });
    new CfnOutput(this, 'userPoolClientId', {
      value: this.userPoolClientId,
    });
    new CfnOutput(this, 'hostedUiDomainBaseUrl', {
      value: this.hostedUiDomainBaseUrl,
    });
  }
}

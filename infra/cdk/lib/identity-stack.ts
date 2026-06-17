import { Aws, CfnOutput, Fn, RemovalPolicy, Stack, StackProps } from 'aws-cdk-lib';
import {
  AccountRecovery,
  CfnUserPoolClient,
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
      removalPolicy: RemovalPolicy.DESTROY,
    });

    this.userPoolDomain = this.userPool.addDomain('UserPoolDomain', {
      cognitoDomain: {
        domainPrefix: `workops-${props.stage}-${Aws.ACCOUNT_ID}`,
      },
    });

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
      },
    });
    const cfnUserPoolClient = this.userPoolClient.node.defaultChild;
    if (!CfnUserPoolClient.isCfnUserPoolClient(cfnUserPoolClient)) {
      throw new Error('WebClient must synthesize to AWS::Cognito::UserPoolClient');
    }
    // P2-5-02 owns CloudFront redirect URL registration through a custom resource.
    cfnUserPoolClient.callbackUrLs = undefined;
    cfnUserPoolClient.logoutUrLs = undefined;
    cfnUserPoolClient.defaultRedirectUri = undefined;

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

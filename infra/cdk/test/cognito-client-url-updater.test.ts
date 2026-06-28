import {
  DescribeUserPoolClientCommand,
  DescribeUserPoolClientCommandInput,
  DescribeUserPoolClientCommandOutput,
  UpdateUserPoolClientCommand,
  UpdateUserPoolClientCommandInput,
  UpdateUserPoolClientCommandOutput,
  UserPoolClientType,
} from '@aws-sdk/client-cognito-identity-provider';
import {
  buildUpdateInput,
  buildUrls,
  CognitoClient,
  CustomResourceEvent,
  handleEvent,
  ResourceProperties,
} from '../lambda/cognito-client-url-updater/index';

interface DescribeCall {
  kind: 'describe';
  input: DescribeUserPoolClientCommandInput;
}

interface UpdateCall {
  kind: 'update';
  input: UpdateUserPoolClientCommandInput;
}

type CommandCall = DescribeCall | UpdateCall;

class CognitoClientFake implements CognitoClient {
  public readonly commands: CommandCall[] = [];
  private readonly userPoolClient: UserPoolClientType;

  constructor(userPoolClient: UserPoolClientType) {
    this.userPoolClient = userPoolClient;
  }

  send(command: DescribeUserPoolClientCommand): Promise<DescribeUserPoolClientCommandOutput>;
  send(command: UpdateUserPoolClientCommand): Promise<UpdateUserPoolClientCommandOutput>;
  send(
    command: DescribeUserPoolClientCommand | UpdateUserPoolClientCommand,
  ): Promise<DescribeUserPoolClientCommandOutput | UpdateUserPoolClientCommandOutput> {
    if (command instanceof DescribeUserPoolClientCommand) {
      this.commands.push({
        kind: 'describe',
        input: command.input,
      });

      return Promise.resolve({
        UserPoolClient: this.userPoolClient,
        $metadata: {},
      });
    }

    this.commands.push({
      kind: 'update',
      input: command.input,
    });

    return Promise.resolve({
      UserPoolClient: {
        ClientId: command.input.ClientId,
        UserPoolId: command.input.UserPoolId,
      },
      $metadata: {},
    });
  }
}

const resourceProperties: ResourceProperties = {
  UserPoolId: 'ap-northeast-1_test',
  PlatformClientId: 'platformclient123',
  TenantClientId: 'tenantclient123',
  CloudFrontDomainName: 'd111111abcdef8.cloudfront.net',
};

const existingClient: UserPoolClientType = {
  ClientName: 'workops-dev-platform-client',
  RefreshTokenValidity: 30,
  AccessTokenValidity: 60,
  IdTokenValidity: 60,
  TokenValidityUnits: {
    AccessToken: 'minutes',
    IdToken: 'minutes',
    RefreshToken: 'days',
  },
  ReadAttributes: ['email'],
  WriteAttributes: ['email'],
  ExplicitAuthFlows: ['ALLOW_REFRESH_TOKEN_AUTH'],
  SupportedIdentityProviders: ['COGNITO'],
  AllowedOAuthFlows: ['code'],
  AllowedOAuthScopes: ['openid', 'email'],
  AllowedOAuthFlowsUserPoolClient: true,
  AnalyticsConfiguration: {
    ApplicationId: 'pinpoint-app',
    ExternalId: 'external-id',
    RoleArn: 'arn:aws:iam::123456789012:role/pinpoint',
    UserDataShared: false,
  },
  PreventUserExistenceErrors: 'ENABLED',
  EnableTokenRevocation: true,
  AuthSessionValidity: 3,
  RefreshTokenRotation: {
    Feature: 'DISABLED',
    RetryGracePeriodSeconds: 0,
  },
  CallbackURLs: ['https://old.example.com/callback'],
  LogoutURLs: ['https://old.example.com/'],
  DefaultRedirectURI: 'https://old.example.com/callback',
  ClientSecret: 'do-not-send',
  CreationDate: new Date('2026-01-01T00:00:00.000Z'),
  LastModifiedDate: new Date('2026-01-02T00:00:00.000Z'),
};

describe('cognito-client-url-updater custom resource', () => {
  test('builds Cognito callback and logout URLs from the CloudFront domain', () => {
    const urls = buildUrls('d111111abcdef8.cloudfront.net', 'platform');

    expect(urls.callbackUrl).toBe(
      'https://d111111abcdef8.cloudfront.net/login/oauth2/code/platform',
    );
    expect(urls.logoutUrl).toBe('https://d111111abcdef8.cloudfront.net/login');
    expect(urls.defaultRedirectUri).toBe(urls.callbackUrl);
  });

  test('builds tenant Cognito callback URL from the CloudFront domain', () => {
    const urls = buildUrls('d111111abcdef8.cloudfront.net', 'tenant');

    expect(urls.callbackUrl).toBe('https://d111111abcdef8.cloudfront.net/login/oauth2/code/tenant');
    expect(urls.logoutUrl).toBe('https://d111111abcdef8.cloudfront.net/login');
    expect(urls.defaultRedirectUri).toBe(urls.callbackUrl);
  });

  test('keeps existing app client settings and replaces only URL fields', () => {
    const input = buildUpdateInput(
      existingClient,
      resourceProperties,
      resourceProperties.PlatformClientId,
      'platform',
    );

    expect(input.UserPoolId).toBe(resourceProperties.UserPoolId);
    expect(input.ClientId).toBe(resourceProperties.PlatformClientId);
    expect(input.ClientName).toBe(existingClient.ClientName);
    expect(input.RefreshTokenValidity).toBe(existingClient.RefreshTokenValidity);
    expect(input.AccessTokenValidity).toBe(existingClient.AccessTokenValidity);
    expect(input.IdTokenValidity).toBe(existingClient.IdTokenValidity);
    expect(input.TokenValidityUnits).toEqual(existingClient.TokenValidityUnits);
    expect(input.ReadAttributes).toEqual(existingClient.ReadAttributes);
    expect(input.WriteAttributes).toEqual(existingClient.WriteAttributes);
    expect(input.ExplicitAuthFlows).toEqual(existingClient.ExplicitAuthFlows);
    expect(input.SupportedIdentityProviders).toEqual(existingClient.SupportedIdentityProviders);
    expect(input.AllowedOAuthFlows).toEqual(existingClient.AllowedOAuthFlows);
    expect(input.AllowedOAuthScopes).toEqual(existingClient.AllowedOAuthScopes);
    expect(input.AllowedOAuthFlowsUserPoolClient).toBe(true);
    expect(input.AnalyticsConfiguration).toEqual(existingClient.AnalyticsConfiguration);
    expect(input.PreventUserExistenceErrors).toBe(existingClient.PreventUserExistenceErrors);
    expect(input.EnableTokenRevocation).toBe(true);
    expect(input.AuthSessionValidity).toBe(existingClient.AuthSessionValidity);
    expect(input.RefreshTokenRotation).toEqual(existingClient.RefreshTokenRotation);
    expect(input.CallbackURLs).toEqual([
      'https://d111111abcdef8.cloudfront.net/login/oauth2/code/platform',
    ]);
    expect(input.LogoutURLs).toEqual(['https://d111111abcdef8.cloudfront.net/login']);
    expect(input.DefaultRedirectURI).toBe(
      'https://d111111abcdef8.cloudfront.net/login/oauth2/code/platform',
    );
    expect(JSON.stringify(input)).not.toContain('ClientSecret');
    expect(JSON.stringify(input)).not.toContain('GenerateSecret');
    expect(JSON.stringify(input)).not.toContain('CreationDate');
    expect(JSON.stringify(input)).not.toContain('LastModifiedDate');
  });

  test('updates the app client on Create', async () => {
    const client = new CognitoClientFake(existingClient);
    const event: CustomResourceEvent = {
      RequestType: 'Create',
      ResourceProperties: resourceProperties,
    };
    const response = await handleEvent(event, client);

    expect(response.PhysicalResourceId).toBe('cognito-client-url-updater-ap-northeast-1_test');
    expect(client.commands).toHaveLength(4);
    expect(client.commands[0].kind).toBe('describe');
    expect(client.commands[1].kind).toBe('update');
    expect(client.commands[2].kind).toBe('describe');
    expect(client.commands[3].kind).toBe('update');
  });

  test('updates the app client on Update', async () => {
    const client = new CognitoClientFake(existingClient);
    const event: CustomResourceEvent = {
      RequestType: 'Update',
      ResourceProperties: resourceProperties,
      PhysicalResourceId: 'existing-physical-id',
    };
    const response = await handleEvent(event, client);

    expect(response.PhysicalResourceId).toBe('existing-physical-id');
    expect(client.commands).toHaveLength(4);
    expect(client.commands[0].kind).toBe('describe');
    expect(client.commands[1].kind).toBe('update');
    expect(client.commands[2].kind).toBe('describe');
    expect(client.commands[3].kind).toBe('update');
  });

  test('does not call Cognito on Delete', async () => {
    const client = new CognitoClientFake(existingClient);
    const event: CustomResourceEvent = {
      RequestType: 'Delete',
      ResourceProperties: resourceProperties,
      PhysicalResourceId: 'existing-physical-id',
    };
    const response = await handleEvent(event, client);

    expect(response.PhysicalResourceId).toBe('existing-physical-id');
    expect(client.commands).toHaveLength(0);
  });
});

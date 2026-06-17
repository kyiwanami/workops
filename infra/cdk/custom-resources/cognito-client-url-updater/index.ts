import {
  CognitoIdentityProviderClient,
  DescribeUserPoolClientCommand,
  DescribeUserPoolClientCommandOutput,
  UpdateUserPoolClientCommand,
  UpdateUserPoolClientCommandInput,
  UpdateUserPoolClientCommandOutput,
  UserPoolClientType,
} from '@aws-sdk/client-cognito-identity-provider';

export interface ResourceProperties {
  UserPoolId: string;
  UserPoolClientId: string;
  CloudFrontDomainName: string;
}

export interface CustomResourceEvent {
  RequestType: string;
  ResourceProperties: ResourceProperties;
  PhysicalResourceId?: string;
}

export interface HandlerResponse {
  PhysicalResourceId: string;
}

export interface UrlSet {
  callbackUrl: string;
  logoutUrl: string;
  defaultRedirectUri: string;
}

export interface CognitoClient {
  send(command: DescribeUserPoolClientCommand): Promise<DescribeUserPoolClientCommandOutput>;
  send(command: UpdateUserPoolClientCommand): Promise<UpdateUserPoolClientCommandOutput>;
}

export function buildUrls(cloudFrontDomainName: string): UrlSet {
  if (cloudFrontDomainName.length === 0) {
    throw new Error('CloudFrontDomainName must not be empty');
  }

  const callbackUrl = `https://${cloudFrontDomainName}/login/oauth2/code/cognito`;
  const logoutUrl = `https://${cloudFrontDomainName}/`;

  return {
    callbackUrl,
    logoutUrl,
    defaultRedirectUri: callbackUrl,
  };
}

export function buildUpdateInput(
  userPoolClient: UserPoolClientType,
  resourceProperties: ResourceProperties,
): UpdateUserPoolClientCommandInput {
  const urls = buildUrls(resourceProperties.CloudFrontDomainName);
  const updateInput: UpdateUserPoolClientCommandInput = {
    UserPoolId: resourceProperties.UserPoolId,
    ClientId: resourceProperties.UserPoolClientId,
    CallbackURLs: [urls.callbackUrl],
    LogoutURLs: [urls.logoutUrl],
    DefaultRedirectURI: urls.defaultRedirectUri,
  };

  if (userPoolClient.ClientName !== undefined) {
    updateInput.ClientName = userPoolClient.ClientName;
  }
  if (userPoolClient.RefreshTokenValidity !== undefined) {
    updateInput.RefreshTokenValidity = userPoolClient.RefreshTokenValidity;
  }
  if (userPoolClient.AccessTokenValidity !== undefined) {
    updateInput.AccessTokenValidity = userPoolClient.AccessTokenValidity;
  }
  if (userPoolClient.IdTokenValidity !== undefined) {
    updateInput.IdTokenValidity = userPoolClient.IdTokenValidity;
  }
  if (userPoolClient.TokenValidityUnits !== undefined) {
    updateInput.TokenValidityUnits = userPoolClient.TokenValidityUnits;
  }
  if (userPoolClient.ReadAttributes !== undefined) {
    updateInput.ReadAttributes = userPoolClient.ReadAttributes;
  }
  if (userPoolClient.WriteAttributes !== undefined) {
    updateInput.WriteAttributes = userPoolClient.WriteAttributes;
  }
  if (userPoolClient.ExplicitAuthFlows !== undefined) {
    updateInput.ExplicitAuthFlows = userPoolClient.ExplicitAuthFlows;
  }
  if (userPoolClient.SupportedIdentityProviders !== undefined) {
    updateInput.SupportedIdentityProviders = userPoolClient.SupportedIdentityProviders;
  }
  if (userPoolClient.AllowedOAuthFlows !== undefined) {
    updateInput.AllowedOAuthFlows = userPoolClient.AllowedOAuthFlows;
  }
  if (userPoolClient.AllowedOAuthScopes !== undefined) {
    updateInput.AllowedOAuthScopes = userPoolClient.AllowedOAuthScopes;
  }
  if (userPoolClient.AllowedOAuthFlowsUserPoolClient !== undefined) {
    updateInput.AllowedOAuthFlowsUserPoolClient = userPoolClient.AllowedOAuthFlowsUserPoolClient;
  }
  if (userPoolClient.AnalyticsConfiguration !== undefined) {
    updateInput.AnalyticsConfiguration = userPoolClient.AnalyticsConfiguration;
  }
  if (userPoolClient.PreventUserExistenceErrors !== undefined) {
    updateInput.PreventUserExistenceErrors = userPoolClient.PreventUserExistenceErrors;
  }
  if (userPoolClient.EnableTokenRevocation !== undefined) {
    updateInput.EnableTokenRevocation = userPoolClient.EnableTokenRevocation;
  }
  if (userPoolClient.EnablePropagateAdditionalUserContextData !== undefined) {
    updateInput.EnablePropagateAdditionalUserContextData = userPoolClient.EnablePropagateAdditionalUserContextData;
  }
  if (userPoolClient.AuthSessionValidity !== undefined) {
    updateInput.AuthSessionValidity = userPoolClient.AuthSessionValidity;
  }
  if (userPoolClient.RefreshTokenRotation !== undefined) {
    updateInput.RefreshTokenRotation = userPoolClient.RefreshTokenRotation;
  }

  return updateInput;
}

function getPhysicalResourceId(event: CustomResourceEvent): string {
  if (event.PhysicalResourceId !== undefined && event.PhysicalResourceId.length > 0) {
    return event.PhysicalResourceId;
  }

  return `cognito-client-url-updater-${event.ResourceProperties.UserPoolId}-${event.ResourceProperties.UserPoolClientId}`;
}

export async function handleEvent(event: CustomResourceEvent, client: CognitoClient): Promise<HandlerResponse> {
  const physicalResourceId = getPhysicalResourceId(event);

  // Delete keeps the last registered Cognito URLs for the next EdgeStack replacement.
  if (event.RequestType === 'Delete') {
    return {
      PhysicalResourceId: physicalResourceId,
    };
  }

  const properties = event.ResourceProperties;
  const describeResponse = await client.send(new DescribeUserPoolClientCommand({
    UserPoolId: properties.UserPoolId,
    ClientId: properties.UserPoolClientId,
  }));
  const userPoolClient = describeResponse.UserPoolClient;
  if (userPoolClient === undefined) {
    throw new Error('DescribeUserPoolClient did not return UserPoolClient');
  }

  const updateInput = buildUpdateInput(userPoolClient, properties);
  await client.send(new UpdateUserPoolClientCommand(updateInput));

  return {
    PhysicalResourceId: physicalResourceId,
  };
}

export async function handler(event: CustomResourceEvent): Promise<HandlerResponse> {
  const client = new CognitoIdentityProviderClient({});

  return handleEvent(event, client);
}

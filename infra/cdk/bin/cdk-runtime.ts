#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { AppRuntimeStack } from '../lib/app-runtime-stack';
import { ConfigStack } from '../lib/config-stack';
import { DataStack } from '../lib/data-stack';
import { EdgeStack } from '../lib/edge-stack';
import { EgressStack } from '../lib/egress-stack';
import { readRequiredEnv } from '../lib/environment';
import { FoundationStack } from '../lib/foundation-stack';
import { IdentityStack } from '../lib/identity-stack';
import { LogsStack } from '../lib/logs-stack';
import { RegistryStack } from '../lib/registry-stack';

const app = new App();
const stage = readRequiredEnv('WORKOPS_STAGE');
const env: Environment = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION,
};
const imageTag = readRequiredEnv('WORKOPS_IMAGE_TAG');

// Runtime deploy owns the paid and session-oriented stacks used for AWS dev verification.
Tags.of(app).add('Project', 'WorkOps');
Tags.of(app).add('Environment', stage);
Tags.of(app).add('ManagedBy', 'CDK');

const foundationStack = new FoundationStack(app, 'FoundationStack', {
  env,
  stage,
  stackName: `workops-${stage}-foundation`,
});
const dataStack = new DataStack(app, 'DataStack', {
  appSecurityGroup: foundationStack.appSecurityGroup,
  dbSecurityGroup: foundationStack.dbSecurityGroup,
  dbSubnets: foundationStack.dbSubnets,
  env,
  stage,
  stackName: `workops-${stage}-data`,
  vpc: foundationStack.vpc,
});
const configStack = new ConfigStack(app, 'ConfigStack', {
  env,
  stage,
  stackName: `workops-${stage}-config`,
});
const identityStack = new IdentityStack(app, 'IdentityStack', {
  env,
  stage,
  stackName: `workops-${stage}-identity`,
});
const registryStack = new RegistryStack(app, 'RegistryStack', {
  env,
  stage,
  stackName: `workops-${stage}-registry`,
});
const logsStack = new LogsStack(app, 'LogsStack', {
  env,
  stage,
  stackName: `workops-${stage}-logs`,
});
const egressStack = new EgressStack(app, 'EgressStack', {
  appSubnets: foundationStack.appSubnets,
  env,
  publicSubnets: foundationStack.publicSubnets,
  stage,
  stackName: `workops-${stage}-egress`,
  vpc: foundationStack.vpc,
});
const edgeStack = new EdgeStack(app, 'EdgeStack', {
  albSecurityGroup: foundationStack.albSecurityGroup,
  env,
  appSubnets: foundationStack.appSubnets,
  cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
  cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
  cognitoUserPoolId: identityStack.userPoolId,
  cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
  cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
  stage,
  stackName: `workops-${stage}-edge`,
  vpc: foundationStack.vpc,
});

// Runtime deploys use the same immutable commit image tag as the Pipeline image build.
const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
  albSecurityGroup: foundationStack.albSecurityGroup,
  appSecurityGroup: foundationStack.appSecurityGroup,
  appSubnets: foundationStack.appSubnets,
  cloudFrontHttpsUrl: edgeStack.cloudFrontHttpsUrl,
  cluster: foundationStack.ecsCluster,
  cognitoHostedUiDomainBaseUrl: identityStack.hostedUiDomainBaseUrl,
  cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
  cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
  cognitoUserPoolId: identityStack.userPoolId,
  env,
  listener: edgeStack.listener,
  loadBalancerFullName: edgeStack.loadBalancer.loadBalancerFullName,
  repository: registryStack.webRepository,
  stage,
  stackName: `workops-${stage}-app-runtime`,
  vpc: foundationStack.vpc,
  webImageTag: imageTag,
  webLogGroup: logsStack.webLogGroup,
});

appRuntimeStack.addDependency(egressStack);
appRuntimeStack.addDependency(edgeStack);
appRuntimeStack.addDependency(identityStack);
appRuntimeStack.addDependency(configStack);
appRuntimeStack.addDependency(dataStack);
appRuntimeStack.addDependency(logsStack);

edgeStack.addDependency(egressStack);
edgeStack.addDependency(identityStack);
edgeStack.addDependency(logsStack);

#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { AppRuntimeStack } from '../lib/app-runtime-stack';
import { ConfigStack } from '../lib/config-stack';
import { DataStack } from '../lib/data-stack';
import { EdgeStack } from '../lib/edge-stack';
import { EgressStack } from '../lib/egress-stack';
import { FoundationStack } from '../lib/foundation-stack';
import { IdentityStack } from '../lib/identity-stack';
import { LogsStack } from '../lib/logs-stack';
import { RegistryStack } from '../lib/registry-stack';
import { SecretStack } from '../lib/secret-stack';

declare global {
  namespace NodeJS {
    interface ProcessEnv {
      WORKOPS_STAGE: string;
    }
  }
}

const app = new App();
const stage = process.env.WORKOPS_STAGE;
const env: Environment = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION,
};

// WorkOps Phase 2 resources share non-secret tags across local and CI deploys.
Tags.of(app).add('Project', 'WorkOps');
Tags.of(app).add('Environment', stage);
Tags.of(app).add('ManagedBy', 'CDK');

const foundationStack = new FoundationStack(app, 'FoundationStack', {
  env,
  stage,
  stackName: `workops-${stage}-foundation`,
});
new SecretStack(app, 'SecretStack', {
  env,
  stackName: `workops-${stage}-secret`,
});
new DataStack(app, 'DataStack', {
  appSecurityGroup: foundationStack.appSecurityGroup,
  dbSecurityGroup: foundationStack.dbSecurityGroup,
  dbSubnets: foundationStack.dbSubnets,
  env,
  stage,
  stackName: `workops-${stage}-data`,
  vpc: foundationStack.vpc,
});
new ConfigStack(app, 'ConfigStack', {
  env,
  stage,
  stackName: `workops-${stage}-config`,
});
new IdentityStack(app, 'IdentityStack', {
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

// P2-3 runtime stacks are synthesized on every run and deployed only for manual verification sessions.
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
  stage,
  stackName: `workops-${stage}-edge`,
  vpc: foundationStack.vpc,
});
const appRuntimeStack = new AppRuntimeStack(app, 'AppRuntimeStack', {
  albSecurityGroup: foundationStack.albSecurityGroup,
  appSecurityGroup: foundationStack.appSecurityGroup,
  appSubnets: foundationStack.appSubnets,
  cluster: foundationStack.ecsCluster,
  env,
  repository: registryStack.repository,
  stage,
  stackName: `workops-${stage}-app-runtime`,
  targetGroup: edgeStack.targetGroup,
  webLogGroup: logsStack.webLogGroup,
});

edgeStack.addDependency(egressStack);
appRuntimeStack.addDependency(egressStack);
appRuntimeStack.addDependency(edgeStack);

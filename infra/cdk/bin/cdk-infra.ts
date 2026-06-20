#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { ConfigStack } from '../lib/config-stack';
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

// Infra deploys only the lightweight stacks kept during the Phase 2 dev environment.
Tags.of(app).add('Project', 'WorkOps');
Tags.of(app).add('Environment', stage);
Tags.of(app).add('ManagedBy', 'CDK');

new FoundationStack(app, 'FoundationStack', {
  env,
  stage,
  stackName: `workops-${stage}-foundation`,
});
new SecretStack(app, 'SecretStack', {
  env,
  stackName: `workops-${stage}-secret`,
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
new RegistryStack(app, 'RegistryStack', {
  env,
  stage,
  stackName: `workops-${stage}-registry`,
});
new LogsStack(app, 'LogsStack', {
  env,
  stage,
  stackName: `workops-${stage}-logs`,
});

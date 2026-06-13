#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { ConfigStack } from '../lib/config-stack';
import { FoundationStack } from '../lib/foundation-stack';
import { LogsStack } from '../lib/logs-stack';
import { RegistryStack } from '../lib/registry-stack';

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

new FoundationStack(app, 'FoundationStack', {
  env,
  stackName: `workops-${stage}-foundation`,
});
new ConfigStack(app, 'ConfigStack', {
  env,
  stackName: `workops-${stage}-config`,
});
new RegistryStack(app, 'RegistryStack', {
  env,
  stackName: `workops-${stage}-registry`,
});
new LogsStack(app, 'LogsStack', {
  env,
  stackName: `workops-${stage}-logs`,
});

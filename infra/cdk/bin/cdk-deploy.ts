#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { DeployStack } from '../lib/deploy-stack';
import { readRequiredEnv } from '../lib/environment';

const app = new App();
const stage = readRequiredEnv('WORKOPS_STAGE');
const githubRepository = readRequiredEnv('GITHUB_REPOSITORY');
const env: Environment = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION,
};

// DeployStack is a one-time local bootstrap entrypoint for GitHub Actions OIDC.
Tags.of(app).add('Project', 'WorkOps');
Tags.of(app).add('Environment', stage);
Tags.of(app).add('ManagedBy', 'CDK');

new DeployStack(app, 'DeployStack', {
  env,
  githubRepository,
  stage,
  stackName: `workops-${stage}-deploy`,
});

#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { DeployStack } from '../lib/deploy-stack';

declare global {
  namespace NodeJS {
    interface ProcessEnv {
      WORKOPS_STAGE: string;
      GITHUB_REPOSITORY: string;
    }
  }
}

const app = new App();
const stage = process.env.WORKOPS_STAGE;
const githubRepository = process.env.GITHUB_REPOSITORY;
if (!githubRepository) {
  throw new Error('GITHUB_REPOSITORY environment variable is required');
}
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

#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { DependencyStack } from '../lib/dependency-stack';
import { readRequiredEnv } from '../lib/environment';
import { PipelineStack } from '../lib/pipeline-stack';

const app = new App();
const stage = readRequiredEnv('WORKOPS_STAGE');
const githubRepository = readRequiredEnv('GITHUB_REPOSITORY');
const notificationEmail = readRequiredEnv('WORKOPS_OPS_NOTIFICATION_EMAIL');
const imageTag = readRequiredEnv('WORKOPS_IMAGE_TAG');
const env: Environment = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION,
};

// CDK app tags keep WorkOps resources traceable across top-level and Pipeline-managed stacks.
Tags.of(app).add('Project', 'WorkOps');
Tags.of(app).add('Environment', stage);
Tags.of(app).add('ManagedBy', 'CDK');

const dependencyStack = new DependencyStack(app, 'DependencyStack', {
  env,
  notificationEmail,
  stage,
  stackName: `workops-${stage}-dependency`,
});

const pipelineStack = new PipelineStack(app, 'PipelineStack', {
  env,
  githubRepository,
  notificationEmail,
  stage,
  stackName: `workops-${stage}-pipeline`,
  webImageTag: imageTag,
});
pipelineStack.addDependency(dependencyStack);

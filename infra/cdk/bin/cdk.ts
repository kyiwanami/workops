#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { DependencyStack } from '../lib/dependencies/dependency-stack';
import { readRequiredEnv, setWorkopsStage } from '../lib/shared/environment';
import { PipelineStack } from '../lib/pipeline/pipeline-stack';

const app = new App();
const stage = readRequiredEnv('WORKOPS_SOURCE_BRANCH');
setWorkopsStage(app, stage);
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
});

const pipelineStack = new PipelineStack(app, 'PipelineStack', {
  env,
  githubRepository,
  notificationEmail,
  stage,
  webImageTag: imageTag,
});
pipelineStack.addDependency(dependencyStack);

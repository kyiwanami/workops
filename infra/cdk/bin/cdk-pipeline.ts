#!/usr/bin/env node
import { App, Environment, Tags } from 'aws-cdk-lib';
import { readRequiredEnv } from '../lib/environment';
import { PipelineStack } from '../lib/pipeline-stack';

const app = new App();
const stage = readRequiredEnv('WORKOPS_STAGE');
const githubRepository = readRequiredEnv('GITHUB_REPOSITORY');
const notificationEmail = readRequiredEnv('WORKOPS_PIPELINE_NOTIFICATION_EMAIL');
const imageTag = readRequiredEnv('WORKOPS_IMAGE_TAG');
const env: Environment = {
  account: process.env.CDK_DEFAULT_ACCOUNT,
  region: process.env.CDK_DEFAULT_REGION,
};

// Pipeline deploy owns the self-mutating AWS-side delivery path for Phase 2 alpha.
Tags.of(app).add('Project', 'WorkOps');
Tags.of(app).add('Environment', stage);
Tags.of(app).add('ManagedBy', 'CDK');

new PipelineStack(app, 'PipelineStack', {
  env,
  githubRepository,
  notificationEmail,
  stage,
  stackName: `workops-${stage}-pipeline`,
  webImageTag: imageTag,
});

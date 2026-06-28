import { Construct } from 'constructs';

const WORKOPS_STAGE_CONTEXT_KEY = 'workops:stage';

// CDK entrypoints fail fast when required environment variables are missing.
export function readRequiredEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} environment variable is required`);
  }
  return value;
}

// WorkOps stacks share the stage through CDK context so naming-only props stay out of stack APIs.
export function setStage(scope: Construct, stage: string): void {
  scope.node.setContext(WORKOPS_STAGE_CONTEXT_KEY, stage);
}

export function readStage(scope: Construct): string {
  // CDK context values are untyped; this function is the typed boundary for WorkOps stage.
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  const value = scope.node.tryGetContext(WORKOPS_STAGE_CONTEXT_KEY);
  if (typeof value !== 'string' || !value) {
    throw new Error(`${WORKOPS_STAGE_CONTEXT_KEY} context is required`);
  }
  return value;
}

export function stackName(scope: Construct, suffix: string): string {
  return `workops-${readStage(scope)}-${suffix}`;
}

export function stagePath(scope: Construct, suffix: string): string {
  return `/workops/${readStage(scope)}/${suffix}`;
}

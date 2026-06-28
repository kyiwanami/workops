import { App, Stack } from 'aws-cdk-lib';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';
import { setStage } from '../lib/shared/environment';

export class TaggedResourceStack extends Stack {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    // A concrete test resource makes stack-level tags visible in assertions.
    new Topic(this, 'TaggedTopic');
  }
}

export const testCognitoUserPoolId = 'ap-northeast-1_test';
export const testCognitoPlatformUserPoolClientId = 'platformclientid';
export const testCognitoTenantUserPoolClientId = 'tenantclientid';
export const testGitHubRepository = 'owner/repo';
export const testOpsNotificationEmail = 'ops@example.com';
export const testWebImageTag = 'test-sha';
export const testEnv = {
  account: '123456789012',
  region: 'ap-northeast-1',
};

export function createTestApp(stage: string): App {
  const app = new App();
  setStage(app, stage);
  return app;
}

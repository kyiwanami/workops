import { Stack, Stage } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { DataPauseStack } from '../data-pause/data-pause-stack';
import { DataStack } from '../data/data-stack';
import { FoundationStack } from '../foundation/foundation-stack';
import { IdentityStack } from '../identity/identity-stack';
import { LogsStack } from '../logs/logs-stack';
import { MigrationRunnerStack } from '../migration/migration-runner-stack';
import { EgressStack } from '../network/egress-stack';
import { AppRuntimeStack } from '../runtime/app-runtime-stack';
import { RegistryStack } from '../registry/registry-stack';
import { WebAclStack } from '../web/web-acl-stack';
import { WebDeliveryStack } from '../web/web-delivery-stack';
import { WebIngressStack } from '../web/web-ingress-stack';

export class FoundationStage extends Stage {
  constructor(scope: Construct, id: string) {
    super(scope, id, {
      env: {
        account: Stack.of(scope).account,
        region: Stack.of(scope).region,
      },
    });

    const foundationStack = new FoundationStack(this, 'FoundationStack', {});
    const logsStack = new LogsStack(this, 'LogsStack', {});
    new IdentityStack(this, 'IdentityStack', {});
    new RegistryStack(this, 'RegistryStack', {});
    const dataPauseStack = new DataPauseStack(this, 'DataPauseStack', {
      markAutoRestartLogGroup: logsStack.dataPauseMarkAutoRestartLogGroup,
      stopMarkedDbLogGroup: logsStack.dataPauseStopMarkedDbLogGroup,
    });
    dataPauseStack.addDependency(logsStack);
    dataPauseStack.addDependency(foundationStack);
  }
}

export class RuntimeInfrastructureStage extends Stage {
  public readonly migrationRunnerStack: MigrationRunnerStack;

  constructor(scope: Construct, id: string) {
    super(scope, id, {
      env: {
        account: Stack.of(scope).account,
        region: Stack.of(scope).region,
      },
    });

    const dataStack = new DataStack(this, 'DataStack', {});
    const egressStack = new EgressStack(this, 'EgressStack', {});
    const webAclStack = new WebAclStack(this, 'WebAclStack', {
      crossRegionReferences: true,
      env: {
        account: Stack.of(scope).account,
        region: 'us-east-1',
      },
    });
    const webIngressStack = new WebIngressStack(this, 'WebIngressStack', {});
    this.migrationRunnerStack = new MigrationRunnerStack(this, 'MigrationRunnerStack', {});
    const webDeliveryStack = new WebDeliveryStack(this, 'WebDeliveryStack', {
      cloudFrontWebAclArn: webAclStack.webAclArn,
      crossRegionReferences: true,
    });

    webIngressStack.addDependency(egressStack);
    webDeliveryStack.addDependency(webAclStack);
    webDeliveryStack.addDependency(webIngressStack);
    this.migrationRunnerStack.addDependency(dataStack);
    this.migrationRunnerStack.addDependency(egressStack);
  }
}

export class AppRuntimeStage extends Stage {
  public readonly appRuntimeStack: AppRuntimeStack;

  constructor(scope: Construct, id: string, props: { webImageTag: string }) {
    super(scope, id, {
      env: {
        account: Stack.of(scope).account,
        region: Stack.of(scope).region,
      },
    });

    this.appRuntimeStack = new AppRuntimeStack(this, 'AppRuntimeStack', {
      webImageTag: props.webImageTag,
    });
  }
}

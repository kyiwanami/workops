import { Stack, Stage } from 'aws-cdk-lib';
import { Repository } from 'aws-cdk-lib/aws-ecr';
import { Construct } from 'constructs';
import { AppRuntimeStack } from '../runtime/app-runtime-stack';
import { RuntimeResources } from '../runtime/app-runtime-stack';
import { DataPauseStack } from '../data-pause/data-pause-stack';
import { DataStack } from '../data/data-stack';
import { EgressStack } from '../network/egress-stack';
import { readWorkopsStage } from '../shared/environment';
import { FoundationStack } from '../foundation/foundation-stack';
import { IdentityStack } from '../identity/identity-stack';
import { LogsStack } from '../logs/logs-stack';
import { MigrationRunnerStack } from '../migration/migration-runner-stack';
import { RegistryStack } from '../registry/registry-stack';
import { WebAclStack } from '../web/web-acl-stack';
import { WebDeliveryStack } from '../web/web-delivery-stack';
import { WebIngressStack } from '../web/web-ingress-stack';

interface DataNetworkMigrationDeployStageProps {
  webImageTag: string;
}

export class RegistryDeployStage extends Stage {
  constructor(scope: Construct, id: string) {
    super(scope, id, {
      env: {
        account: Stack.of(scope).account,
        region: Stack.of(scope).region,
      },
    });

    new RegistryStack(this, 'RegistryStack', {});
  }
}

export class DataNetworkMigrationDeployStage extends Stage {
  public readonly appRuntimeStack: AppRuntimeStack;
  public readonly migrationRunnerStack: MigrationRunnerStack;

  constructor(scope: Construct, id: string, props: DataNetworkMigrationDeployStageProps) {
    const stage = readWorkopsStage(scope);
    super(scope, id, {
      env: {
        account: Stack.of(scope).account,
        region: Stack.of(scope).region,
      },
    });

    const foundationStack = new FoundationStack(this, 'FoundationStack', {
    });
    const dataStack = new DataStack(this, 'DataStack', {
      appSecurityGroup: foundationStack.appSecurityGroup,
      dbSecurityGroup: foundationStack.dbSecurityGroup,
      dbSubnets: foundationStack.dbSubnets,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      vpc: foundationStack.vpc,
    });
    const identityStack = new IdentityStack(this, 'IdentityStack', {});
    const logsStack = new LogsStack(this, 'LogsStack', {});
    const dataPauseStack = new DataPauseStack(this, 'DataPauseStack', {
      markAutoRestartLogGroup: logsStack.dataPauseMarkAutoRestartLogGroup,
      stopMarkedDbLogGroup: logsStack.dataPauseStopMarkedDbLogGroup,
    });
    const egressStack = new EgressStack(this, 'EgressStack', {
      appSubnets: foundationStack.appSubnets,
      publicSubnets: foundationStack.publicSubnets,
      vpc: foundationStack.vpc,
    });
    const webAclStack = new WebAclStack(this, 'WebAclStack', {
      crossRegionReferences: true,
      env: {
        account: Stack.of(scope).account,
        region: 'us-east-1',
      },
    });
    const webIngressStack = new WebIngressStack(this, 'WebIngressStack', {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      vpc: foundationStack.vpc,
    });
    const webDeliveryStack = new WebDeliveryStack(this, 'WebDeliveryStack', {
      cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
      cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
      cognitoUserPoolId: identityStack.userPoolId,
      cognitoClientUrlUpdaterLogGroup: logsStack.cognitoClientUrlUpdaterLogGroup,
      cognitoClientUrlUpdaterProviderLogGroup: logsStack.cognitoClientUrlUpdaterProviderLogGroup,
      crossRegionReferences: true,
      webAclArn: webAclStack.webAclArn,
    });
    const webRepository = Repository.fromRepositoryName(
      foundationStack,
      'WebRepository',
      `workops-${stage}-web`,
    );

    // MigrationRunnerStack owns the VPC-attached CodeBuild project that runs Flyway against RDS.
    this.migrationRunnerStack = new MigrationRunnerStack(this, 'MigrationRunnerStack', {
      appSubnets: foundationStack.appSubnets,
      migrationSecurityGroup: foundationStack.migrationSecurityGroup,
      migrationLogGroup: logsStack.migrationLogGroup,
      vpc: foundationStack.vpc,
    });

    webIngressStack.addDependency(egressStack);
    webDeliveryStack.addDependency(webAclStack);
    webDeliveryStack.addDependency(webIngressStack);
    webDeliveryStack.addDependency(identityStack);
    webDeliveryStack.addDependency(logsStack);
    dataPauseStack.addDependency(logsStack);
    this.migrationRunnerStack.addDependency(dataStack);
    this.migrationRunnerStack.addDependency(egressStack);
    this.migrationRunnerStack.addDependency(logsStack);

    const runtimeResources: RuntimeResources = {
      albSecurityGroup: foundationStack.albSecurityGroup,
      appSecurityGroup: foundationStack.appSecurityGroup,
      appSubnets: foundationStack.appSubnets,
      cloudFrontHttpsUrl: webDeliveryStack.cloudFrontHttpsUrl,
      cluster: foundationStack.ecsCluster,
      cognitoHostedUiDomainBaseUrl: identityStack.hostedUiDomainBaseUrl,
      cognitoPlatformUserPoolClientId: identityStack.platformUserPoolClientId,
      cognitoTenantUserPoolClientId: identityStack.tenantUserPoolClientId,
      cognitoUserPoolId: identityStack.userPoolId,
      listener: webIngressStack.listener,
      loadBalancerFullName: webIngressStack.loadBalancer.loadBalancerFullName,
      repository: webRepository,
      vpc: foundationStack.vpc,
      webLogGroup: logsStack.webLogGroup,
    };

    // AppRuntime consumes support resources through construct references within this CDK Stage.
    this.appRuntimeStack = new AppRuntimeStack(this, 'AppRuntimeStack', {
      runtimeResources,
      webImageTag: props.webImageTag,
    });

    this.appRuntimeStack.addDependency(this.migrationRunnerStack);
  }
}

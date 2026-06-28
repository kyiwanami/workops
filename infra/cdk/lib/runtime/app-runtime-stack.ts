import { ArnFormat, Duration, Stack, StackProps } from 'aws-cdk-lib';
import { CfnSecurityGroupIngress, ISecurityGroup, ISubnet, IVpc } from 'aws-cdk-lib/aws-ec2';
import { IRepository, Repository } from 'aws-cdk-lib/aws-ecr';
import {
  AlarmBehavior,
  AlternateTarget,
  Cluster,
  ContainerImage,
  CpuArchitecture,
  DeploymentStrategy,
  FargateService,
  FargateTaskDefinition,
  ICluster,
  ListenerRuleConfiguration,
  LogDrivers,
  OperatingSystemFamily,
  Secret as EcsSecret,
} from 'aws-cdk-lib/aws-ecs';
import {
  ApplicationListener,
  IApplicationListener,
  ApplicationListenerRule,
  ApplicationProtocol,
  ApplicationTargetGroup,
  ListenerAction,
  ListenerCondition,
  TargetType,
} from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import {
  Alarm,
  ComparisonOperator,
  MathExpression,
  Metric,
  TreatMissingData,
} from 'aws-cdk-lib/aws-cloudwatch';
import { PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { ILogGroup } from 'aws-cdk-lib/aws-logs';
import { Secret as SecretsManagerSecret } from 'aws-cdk-lib/aws-secretsmanager';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';
import {
  contractValue,
  foundationNetwork,
  foundationSecurityGroups,
  identityContract,
  logsGroup,
} from '../shared/contract-imports';
import { readStage, stackName, stagePath } from '../shared/environment';

interface RuntimeResources {
  cluster: ICluster;
  vpc: IVpc;
  appSubnets: ISubnet[];
  appSecurityGroup: ISecurityGroup;
  albSecurityGroup: ISecurityGroup;
  repository: IRepository;
  webLogGroup: ILogGroup;
  listener: IApplicationListener;
  loadBalancerFullName: string;
  cognitoUserPoolId: string;
  cognitoPlatformUserPoolClientId: string;
  cognitoTenantUserPoolClientId: string;
  cognitoHostedUiDomainBaseUrl: string;
  cloudFrontHttpsUrl: string;
}

export interface AppRuntimeStackProps extends StackProps {
  webImageTag: string;
}

export class AppRuntimeStack extends Stack {
  public readonly service: FargateService;
  public readonly taskDefinition: FargateTaskDefinition;

  constructor(scope: Construct, id: string, props: AppRuntimeStackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'app-runtime'),
    });

    const resources = this.runtimeResources();

    // The ALB reaches only the Spring Boot container port exposed by the P2-3 image.
    new CfnSecurityGroupIngress(this, 'AppHttpIngressFromAlb', {
      groupId: resources.appSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourceSecurityGroupId: resources.albSecurityGroup.securityGroupId,
      fromPort: 8080,
      toPort: 8080,
      description: 'Allow WorkOps ALB to reach web tasks',
    });

    const dbUrlParameter = StringParameter.fromStringParameterName(
      this,
      'DbUrlParameter',
      stagePath(this, 'db/url'),
    );
    const springProfileParameter = StringParameter.fromStringParameterName(
      this,
      'SpringProfileParameter',
      stagePath(this, 'dependencies/runtime/spring-profile'),
    );
    const dbMasterSecret = SecretsManagerSecret.fromSecretNameV2(
      this,
      'DbMasterSecret',
      stagePath(this, 'db/master'),
    );

    this.taskDefinition = new FargateTaskDefinition(this, 'WebTaskDefinition', {
      family: `workops-${stage}-web`,
      cpu: 512,
      memoryLimitMiB: 1024,
      runtimePlatform: {
        operatingSystemFamily: OperatingSystemFamily.LINUX,
        cpuArchitecture: CpuArchitecture.ARM64,
      },
    });

    const executionRole = this.taskDefinition.obtainExecutionRole();
    dbUrlParameter.grantRead(executionRole);
    springProfileParameter.grantRead(executionRole);
    dbMasterSecret.grantRead(executionRole);
    // WorkOps user creation can create Cognito users, but cannot read, update, disable, or delete them.
    this.taskDefinition.addToTaskRolePolicy(
      new PolicyStatement({
        actions: ['cognito-idp:AdminCreateUser'],
        resources: [
          this.formatArn({
            service: 'cognito-idp',
            resource: 'userpool',
            resourceName: resources.cognitoUserPoolId,
            arnFormat: ArnFormat.SLASH_RESOURCE_NAME,
          }),
        ],
      }),
    );

    // The dev ECS task runs the non-local Spring security profile and reads Cognito settings from CDK wiring.
    this.taskDefinition.addContainer('WebContainer', {
      containerName: 'web',
      image: ContainerImage.fromEcrRepository(resources.repository, props.webImageTag),
      environment: {
        AWS_REGION: Stack.of(this).region,
        WORKOPS_COGNITO_USER_POOL_ID: resources.cognitoUserPoolId,
        WORKOPS_COGNITO_PLATFORM_CLIENT_ID: resources.cognitoPlatformUserPoolClientId,
        WORKOPS_COGNITO_TENANT_CLIENT_ID: resources.cognitoTenantUserPoolClientId,
        WORKOPS_COGNITO_HOSTED_UI_DOMAIN_BASE_URL: resources.cognitoHostedUiDomainBaseUrl,
        WORKOPS_COGNITO_LOGOUT_URI: `${resources.cloudFrontHttpsUrl}/login`,
        WORKOPS_COGNITO_PLATFORM_REDIRECT_URI: `${resources.cloudFrontHttpsUrl}/login/oauth2/code/platform`,
        WORKOPS_COGNITO_TENANT_REDIRECT_URI: `${resources.cloudFrontHttpsUrl}/login/oauth2/code/tenant`,
      },
      secrets: {
        SPRING_PROFILES_ACTIVE: EcsSecret.fromSsmParameter(springProfileParameter),
        WORKOPS_DB_URL: EcsSecret.fromSsmParameter(dbUrlParameter),
        WORKOPS_DB_USERNAME: EcsSecret.fromSecretsManager(dbMasterSecret, 'username'),
        WORKOPS_DB_PASSWORD: EcsSecret.fromSecretsManager(dbMasterSecret, 'password'),
      },
      logging: LogDrivers.awsLogs({
        logGroup: resources.webLogGroup,
        streamPrefix: 'web',
      }),
      portMappings: [
        {
          containerPort: 8080,
        },
      ],
    });

    const blueTargetGroup = this.createTargetGroup('WebBlueTargetGroup', stage, resources, 'blue');
    const greenTargetGroup = this.createTargetGroup(
      'WebGreenTargetGroup',
      stage,
      resources,
      'green',
    );
    const listenerRule = new ApplicationListenerRule(this, 'WebListenerRule', {
      listener: resources.listener,
      priority: 10,
      conditions: [ListenerCondition.pathPatterns(['/*'])],
      action: ListenerAction.forward([blueTargetGroup]),
    });
    const healthyHostAlarm = this.createHealthyHostAlarm(
      stage,
      resources.loadBalancerFullName,
      blueTargetGroup,
      greenTargetGroup,
    );
    const unhealthyHostAlarm = this.createUnhealthyHostAlarm(
      stage,
      resources.loadBalancerFullName,
      blueTargetGroup,
      greenTargetGroup,
    );

    this.service = new FargateService(this, 'WebService', {
      cluster: resources.cluster,
      serviceName: `workops-${stage}-web`,
      taskDefinition: this.taskDefinition,
      desiredCount: 1,
      assignPublicIp: false,
      circuitBreaker: {
        rollback: true,
      },
      deploymentStrategy: DeploymentStrategy.BLUE_GREEN,
      bakeTime: Duration.minutes(3),
      deploymentAlarms: {
        alarmNames: [healthyHostAlarm.alarmName, unhealthyHostAlarm.alarmName],
        behavior: AlarmBehavior.ROLLBACK_ON_ALARM,
      },
      healthCheckGracePeriod: Duration.seconds(90),
      minHealthyPercent: 100,
      maxHealthyPercent: 200,
      securityGroups: [resources.appSecurityGroup],
      vpcSubnets: {
        subnets: resources.appSubnets,
      },
    });

    // ECS native blue/green uses the listener rule as production traffic and green as alternate traffic.
    const target = this.service.loadBalancerTarget({
      containerName: 'web',
      containerPort: 8080,
      alternateTarget: new AlternateTarget('WebAlternateTarget', {
        alternateTargetGroup: greenTargetGroup,
        productionListener: ListenerRuleConfiguration.applicationListenerRule(listenerRule),
      }),
    });
    target.attachToApplicationTargetGroup(blueTargetGroup);
    this.service.node.addDependency(listenerRule);
  }

  private runtimeResources(): RuntimeResources {
    // AppRuntime imports only SSM contracts and explicit names produced before this deploy action.
    const network = foundationNetwork(this);
    const securityGroups = foundationSecurityGroups(this);
    const identity = identityContract(this);

    return {
      cluster: Cluster.fromClusterAttributes(this, 'EcsCluster', {
        clusterArn: contractValue(this, 'foundation/ecs/cluster-arn'),
        clusterName: contractValue(this, 'foundation/ecs/cluster-name'),
        hasEc2Capacity: false,
        vpc: network.vpc,
      }),
      vpc: network.vpc,
      appSubnets: network.appSubnets,
      appSecurityGroup: securityGroups.appSecurityGroup,
      albSecurityGroup: securityGroups.albSecurityGroup,
      repository: Repository.fromRepositoryName(
        this,
        'WebRepository',
        contractValue(this, 'registry/web-repository-name'),
      ),
      webLogGroup: logsGroup(this, 'WebLogGroup', 'web'),
      listener: ApplicationListener.fromApplicationListenerAttributes(this, 'WebHttpListener', {
        defaultPort: 80,
        listenerArn: contractValue(this, 'web-ingress/listener/http-listener-arn'),
        securityGroup: securityGroups.albSecurityGroup,
      }),
      loadBalancerFullName: contractValue(this, 'web-ingress/alb-full-name'),
      cognitoUserPoolId: identity.userPoolId,
      cognitoPlatformUserPoolClientId: identity.platformClientId,
      cognitoTenantUserPoolClientId: identity.tenantClientId,
      cognitoHostedUiDomainBaseUrl: identity.hostedUiDomainBaseUrl,
      cloudFrontHttpsUrl: contractValue(this, 'web-delivery/cloudfront-https-url'),
    };
  }

  private createTargetGroup(
    id: string,
    stage: string,
    resources: RuntimeResources,
    color: string,
  ): ApplicationTargetGroup {
    const targetGroup = new ApplicationTargetGroup(this, id, {
      vpc: resources.vpc,
      targetType: TargetType.IP,
      protocol: ApplicationProtocol.HTTP,
      port: 8080,
      targetGroupName: `workops-${stage}-web-${color}-tg`,
      healthCheck: {
        path: '/actuator/health',
        healthyHttpCodes: '200',
        interval: Duration.seconds(30),
        timeout: Duration.seconds(5),
        healthyThresholdCount: 2,
        unhealthyThresholdCount: 3,
      },
    });
    targetGroup.setAttribute('deregistration_delay.timeout_seconds', '30');
    return targetGroup;
  }

  private createHealthyHostAlarm(
    stage: string,
    loadBalancerFullName: string,
    blueTargetGroup: ApplicationTargetGroup,
    greenTargetGroup: ApplicationTargetGroup,
  ): Alarm {
    const totalHealthyHosts = new MathExpression({
      expression: 'blueHealthy + greenHealthy',
      label: 'Total healthy hosts',
      period: Duration.minutes(1),
      usingMetrics: {
        blueHealthy: this.createTargetGroupMetric(
          loadBalancerFullName,
          blueTargetGroup,
          'HealthyHostCount',
          'Minimum',
        ),
        greenHealthy: this.createTargetGroupMetric(
          loadBalancerFullName,
          greenTargetGroup,
          'HealthyHostCount',
          'Minimum',
        ),
      },
    });
    return new Alarm(this, 'WebHealthyHostAlarm', {
      alarmName: `workops-${stage}-web-healthy-host-count-low`,
      metric: totalHealthyHosts,
      threshold: 1,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      comparisonOperator: ComparisonOperator.LESS_THAN_THRESHOLD,
      treatMissingData: TreatMissingData.NOT_BREACHING,
    });
  }

  private createUnhealthyHostAlarm(
    stage: string,
    loadBalancerFullName: string,
    blueTargetGroup: ApplicationTargetGroup,
    greenTargetGroup: ApplicationTargetGroup,
  ): Alarm {
    const totalUnhealthyHosts = new MathExpression({
      expression: 'blueUnhealthy + greenUnhealthy',
      label: 'Total unhealthy hosts',
      period: Duration.minutes(1),
      usingMetrics: {
        blueUnhealthy: this.createTargetGroupMetric(
          loadBalancerFullName,
          blueTargetGroup,
          'UnHealthyHostCount',
          'Maximum',
        ),
        greenUnhealthy: this.createTargetGroupMetric(
          loadBalancerFullName,
          greenTargetGroup,
          'UnHealthyHostCount',
          'Maximum',
        ),
      },
    });
    return new Alarm(this, 'WebUnhealthyHostAlarm', {
      alarmName: `workops-${stage}-web-unhealthy-host-count`,
      metric: totalUnhealthyHosts,
      threshold: 0,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
      treatMissingData: TreatMissingData.NOT_BREACHING,
    });
  }

  private createTargetGroupMetric(
    loadBalancerFullName: string,
    targetGroup: ApplicationTargetGroup,
    metricName: string,
    statistic: string,
  ): Metric {
    return new Metric({
      namespace: 'AWS/ApplicationELB',
      metricName,
      dimensionsMap: {
        LoadBalancer: loadBalancerFullName,
        TargetGroup: targetGroup.targetGroupFullName,
      },
      period: Duration.minutes(1),
      statistic,
    });
  }
}

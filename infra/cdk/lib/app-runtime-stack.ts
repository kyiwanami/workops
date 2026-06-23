import { ArnFormat, Duration, Stack, StackProps } from 'aws-cdk-lib';
import { CfnSecurityGroupIngress, ISecurityGroup, ISubnet, IVpc } from 'aws-cdk-lib/aws-ec2';
import { IRepository } from 'aws-cdk-lib/aws-ecr';
import {
  AlarmBehavior,
  AlternateTarget,
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

export interface RuntimeResources {
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
  stage: string;
  webImageTag: string;
  runtimeResources: RuntimeResources;
}

export class AppRuntimeStack extends Stack {
  public readonly service: FargateService;
  public readonly taskDefinition: FargateTaskDefinition;

  constructor(scope: Construct, id: string, props: AppRuntimeStackProps) {
    super(scope, id, props);

    const resources = props.runtimeResources;

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
      `/workops/${props.stage}/db/url`,
    );
    const springProfileParameter = StringParameter.fromStringParameterName(
      this,
      'SpringProfileParameter',
      `/workops/${props.stage}/spring/profile`,
    );
    const dbMasterSecret = SecretsManagerSecret.fromSecretNameV2(
      this,
      'DbMasterSecret',
      `/workops/${props.stage}/db/master`,
    );

    this.taskDefinition = new FargateTaskDefinition(this, 'WebTaskDefinition', {
      family: `workops-${props.stage}-web`,
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

    const blueTargetGroup = this.createTargetGroup(
      'WebBlueTargetGroup',
      props.stage,
      resources,
      'blue',
    );
    const greenTargetGroup = this.createTargetGroup(
      'WebGreenTargetGroup',
      props.stage,
      resources,
      'green',
    );
    const listenerRule = new ApplicationListenerRule(this, 'WebListenerRule', {
      listener: resources.listener,
      priority: 10,
      conditions: [ListenerCondition.pathPatterns(['/*'])],
      action: ListenerAction.forward([blueTargetGroup]),
    });
    const target5xxAlarm = this.createTarget5xxAlarm(
      props.stage,
      resources.loadBalancerFullName,
      blueTargetGroup,
      greenTargetGroup,
    );
    const unhealthyHostAlarm = this.createUnhealthyHostAlarm(
      props.stage,
      resources.loadBalancerFullName,
      blueTargetGroup,
      greenTargetGroup,
    );

    this.service = new FargateService(this, 'WebService', {
      cluster: resources.cluster,
      serviceName: `workops-${props.stage}-web`,
      taskDefinition: this.taskDefinition,
      desiredCount: 1,
      assignPublicIp: false,
      circuitBreaker: {
        rollback: true,
      },
      deploymentStrategy: DeploymentStrategy.BLUE_GREEN,
      bakeTime: Duration.minutes(3),
      deploymentAlarms: {
        alarmNames: [target5xxAlarm.alarmName, unhealthyHostAlarm.alarmName],
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

  private createTarget5xxAlarm(
    stage: string,
    loadBalancerFullName: string,
    blueTargetGroup: ApplicationTargetGroup,
    greenTargetGroup: ApplicationTargetGroup,
  ): Alarm {
    const totalTarget5xx = new MathExpression({
      expression: 'blue5xx + green5xx',
      label: 'Total target 5xx',
      period: Duration.minutes(1),
      usingMetrics: {
        blue5xx: this.createTargetGroupMetric(
          loadBalancerFullName,
          blueTargetGroup,
          'HTTPCode_Target_5XX_Count',
          'Sum',
        ),
        green5xx: this.createTargetGroupMetric(
          loadBalancerFullName,
          greenTargetGroup,
          'HTTPCode_Target_5XX_Count',
          'Sum',
        ),
      },
    });
    return new Alarm(this, 'WebTarget5xxAlarm', {
      alarmName: `workops-${stage}-web-target-5xx`,
      metric: totalTarget5xx,
      threshold: 0,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
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
      alarmName: `workops-${stage}-web-unhealthy-host`,
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

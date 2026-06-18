import { Duration, Stack, StackProps } from 'aws-cdk-lib';
import { CfnSecurityGroupIngress, ISubnet, SecurityGroup } from 'aws-cdk-lib/aws-ec2';
import { IRepository } from 'aws-cdk-lib/aws-ecr';
import {
  Cluster,
  CfnService,
  ContainerImage,
  CpuArchitecture,
  FargateService,
  FargateTaskDefinition,
  LogDrivers,
  OperatingSystemFamily,
  Secret as EcsSecret,
} from 'aws-cdk-lib/aws-ecs';
import { ApplicationTargetGroup } from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { ILogGroup } from 'aws-cdk-lib/aws-logs';
import { Secret as SecretsManagerSecret } from 'aws-cdk-lib/aws-secretsmanager';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface AppRuntimeStackProps extends StackProps {
  stage: string;
  cluster: Cluster;
  appSubnets: ISubnet[];
  appSecurityGroup: SecurityGroup;
  albSecurityGroup: SecurityGroup;
  repository: IRepository;
  webLogGroup: ILogGroup;
  targetGroup: ApplicationTargetGroup;
  cognitoUserPoolId: string;
  cognitoPlatformUserPoolClientId: string;
  cognitoTenantUserPoolClientId: string;
  cloudFrontHttpsUrl: string;
}

export class AppRuntimeStack extends Stack {
  public readonly service: FargateService;
  public readonly taskDefinition: FargateTaskDefinition;

  constructor(scope: Construct, id: string, props: AppRuntimeStackProps) {
    super(scope, id, props);

    // The ALB reaches only the Spring Boot container port exposed by the P2-3 image.
    new CfnSecurityGroupIngress(this, 'AppHttpIngressFromAlb', {
      groupId: props.appSecurityGroup.securityGroupId,
      ipProtocol: 'tcp',
      sourceSecurityGroupId: props.albSecurityGroup.securityGroupId,
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
        cpuArchitecture: CpuArchitecture.X86_64,
      },
    });

    const executionRole = this.taskDefinition.obtainExecutionRole();
    dbUrlParameter.grantRead(executionRole);
    springProfileParameter.grantRead(executionRole);
    dbMasterSecret.grantRead(executionRole);

    // The dev ECS task runs the non-local Spring security profile and reads Cognito settings from CDK wiring.
    this.taskDefinition.addContainer('WebContainer', {
      containerName: 'web',
      image: ContainerImage.fromEcrRepository(props.repository, 'p2-3-manual'),
      environment: {
        AWS_REGION: Stack.of(this).region,
        WORKOPS_COGNITO_USER_POOL_ID: props.cognitoUserPoolId,
        WORKOPS_COGNITO_PLATFORM_CLIENT_ID: props.cognitoPlatformUserPoolClientId,
        WORKOPS_COGNITO_TENANT_CLIENT_ID: props.cognitoTenantUserPoolClientId,
        WORKOPS_COGNITO_PLATFORM_REDIRECT_URI: `${props.cloudFrontHttpsUrl}/login/oauth2/code/platform`,
        WORKOPS_COGNITO_TENANT_REDIRECT_URI: `${props.cloudFrontHttpsUrl}/login/oauth2/code/tenant`,
      },
      secrets: {
        SPRING_PROFILES_ACTIVE: EcsSecret.fromSsmParameter(springProfileParameter),
        WORKOPS_DB_URL: EcsSecret.fromSsmParameter(dbUrlParameter),
        WORKOPS_DB_USERNAME: EcsSecret.fromSecretsManager(dbMasterSecret, 'username'),
        WORKOPS_DB_PASSWORD: EcsSecret.fromSecretsManager(dbMasterSecret, 'password'),
      },
      logging: LogDrivers.awsLogs({
        logGroup: props.webLogGroup,
        streamPrefix: 'web',
      }),
      portMappings: [
        {
          containerPort: 8080,
        },
      ],
    });

    this.service = new FargateService(this, 'WebService', {
      cluster: props.cluster,
      serviceName: `workops-${props.stage}-web`,
      taskDefinition: this.taskDefinition,
      desiredCount: 1,
      assignPublicIp: false,
      circuitBreaker: {
        rollback: true,
      },
      healthCheckGracePeriod: Duration.seconds(90),
      minHealthyPercent: 100,
      maxHealthyPercent: 200,
      securityGroups: [props.appSecurityGroup],
      vpcSubnets: {
        subnets: props.appSubnets,
      },
    });

    const serviceResource = this.service.node.defaultChild;
    if (!(serviceResource instanceof CfnService)) {
      throw new Error('WebService must synthesize an ECS CfnService');
    }

    // The service target group is wired at the CfnService level to avoid mutating shared security groups.
    serviceResource.loadBalancers = [
      {
        containerName: 'web',
        containerPort: 8080,
        targetGroupArn: props.targetGroup.targetGroupArn,
      },
    ];
    this.service.node.addDependency(props.targetGroup);
  }
}

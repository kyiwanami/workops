import { Duration, Stack, StackProps } from 'aws-cdk-lib';
import { CfnSecurityGroupIngress, ISubnet, SecurityGroup } from 'aws-cdk-lib/aws-ec2';
import { IRepository } from 'aws-cdk-lib/aws-ecr';
import {
  Cluster,
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
    dbMasterSecret.grantRead(executionRole);

    this.taskDefinition.addContainer('WebContainer', {
      containerName: 'web',
      image: ContainerImage.fromEcrRepository(props.repository, 'p2-3-manual'),
      environment: {
        SPRING_PROFILES_ACTIVE: 'dev',
      },
      secrets: {
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

    this.service.attachToApplicationTargetGroup(props.targetGroup);
  }
}

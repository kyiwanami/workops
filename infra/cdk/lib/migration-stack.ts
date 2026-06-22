import { CfnOutput, Fn, Stack, StackProps } from 'aws-cdk-lib';
import { ISubnet, SecurityGroup } from 'aws-cdk-lib/aws-ec2';
import { IRepository } from 'aws-cdk-lib/aws-ecr';
import {
  Cluster,
  ContainerImage,
  CpuArchitecture,
  FargateTaskDefinition,
  LogDrivers,
  OperatingSystemFamily,
  Secret as EcsSecret,
} from 'aws-cdk-lib/aws-ecs';
import { ManagedPolicy, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';
import { ILogGroup } from 'aws-cdk-lib/aws-logs';
import { Secret as SecretsManagerSecret } from 'aws-cdk-lib/aws-secretsmanager';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface MigrationStackProps extends StackProps {
  stage: string;
  cluster: Cluster;
  appSubnets: ISubnet[];
  appSecurityGroup: SecurityGroup;
  migrationRepository: IRepository;
  migrationLogGroup: ILogGroup;
  migrationImageTag: string;
}

export class MigrationStack extends Stack {
  public readonly taskDefinition: FargateTaskDefinition;
  public readonly migrationTaskDefinitionArnOutput: CfnOutput;
  public readonly migrationContainerNameOutput: CfnOutput;
  public readonly migrationClusterNameOutput: CfnOutput;
  public readonly migrationSubnetIdsOutput: CfnOutput;
  public readonly migrationSecurityGroupIdOutput: CfnOutput;

  constructor(scope: Construct, id: string, props: MigrationStackProps) {
    super(scope, id, props);

    const containerName = 'migration';
    const flywayLocations =
      'filesystem:/flyway/sql/migration,filesystem:/flyway/sql/seed/common,filesystem:/flyway/sql/seed/aws-dev';
    const executionRoleName = `workops-${props.stage}-migration-execution`;
    const taskRoleName = `workops-${props.stage}-migration-task`;
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

    // Migration runs as a one-shot task, so the stack owns a task definition but no ECS service.
    const executionRole = new Role(this, 'MigrationExecutionRole', {
      roleName: executionRoleName,
      assumedBy: new ServicePrincipal('ecs-tasks.amazonaws.com'),
    });
    executionRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy'),
    );
    props.migrationRepository.grantPull(executionRole);
    props.migrationLogGroup.grantWrite(executionRole);
    dbUrlParameter.grantRead(executionRole);
    dbMasterSecret.grantRead(executionRole);

    const taskRole = new Role(this, 'MigrationTaskRole', {
      roleName: taskRoleName,
      assumedBy: new ServicePrincipal('ecs-tasks.amazonaws.com'),
    });

    this.taskDefinition = new FargateTaskDefinition(this, 'MigrationTaskDefinition', {
      family: `workops-${props.stage}-migration`,
      cpu: 512,
      memoryLimitMiB: 1024,
      executionRole,
      taskRole,
      runtimePlatform: {
        operatingSystemFamily: OperatingSystemFamily.LINUX,
        cpuArchitecture: CpuArchitecture.ARM64,
      },
    });

    this.taskDefinition.addContainer('MigrationContainer', {
      containerName,
      image: ContainerImage.fromEcrRepository(props.migrationRepository, props.migrationImageTag),
      environment: {
        AWS_REGION: Stack.of(this).region,
        WORKOPS_FLYWAY_LOCATIONS: flywayLocations,
      },
      secrets: {
        WORKOPS_DB_URL: EcsSecret.fromSsmParameter(dbUrlParameter),
        WORKOPS_DB_USERNAME: EcsSecret.fromSecretsManager(dbMasterSecret, 'username'),
        WORKOPS_DB_PASSWORD: EcsSecret.fromSecretsManager(dbMasterSecret, 'password'),
      },
      logging: LogDrivers.awsLogs({
        logGroup: props.migrationLogGroup,
        streamPrefix: 'migration',
      }),
    });

    this.migrationTaskDefinitionArnOutput = new CfnOutput(this, 'migrationTaskDefinitionArn', {
      value: this.taskDefinition.taskDefinitionArn,
    });
    this.migrationContainerNameOutput = new CfnOutput(this, 'migrationContainerName', {
      value: containerName,
    });
    this.migrationClusterNameOutput = new CfnOutput(this, 'migrationClusterName', {
      value: props.cluster.clusterName,
    });
    this.migrationSubnetIdsOutput = new CfnOutput(this, 'migrationSubnetIds', {
      value: Fn.join(
        ',',
        props.appSubnets.map((subnet) => subnet.subnetId),
      ),
    });
    this.migrationSecurityGroupIdOutput = new CfnOutput(this, 'migrationSecurityGroupId', {
      value: props.appSecurityGroup.securityGroupId,
    });
  }
}

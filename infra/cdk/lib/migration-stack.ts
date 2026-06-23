import { Aws, CfnOutput, Duration, Stack, StackProps } from 'aws-cdk-lib';
import {
  BuildEnvironmentVariableType,
  BuildSpec,
  ComputeType,
  LinuxBuildImage,
  PipelineProject,
} from 'aws-cdk-lib/aws-codebuild';
import { ISubnet, SecurityGroup, Vpc } from 'aws-cdk-lib/aws-ec2';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { ILogGroup } from 'aws-cdk-lib/aws-logs';
import { Secret as SecretsManagerSecret } from 'aws-cdk-lib/aws-secretsmanager';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface MigrationStackProps extends StackProps {
  stage: string;
  appSubnets: ISubnet[];
  migrationSecurityGroup: SecurityGroup;
  migrationLogGroup: ILogGroup;
  vpc: Vpc;
}

export class MigrationStack extends Stack {
  public readonly migrationProject: PipelineProject;
  public readonly migrationProjectNameOutput: CfnOutput;

  constructor(scope: Construct, id: string, props: MigrationStackProps) {
    super(scope, id, props);

    const flywayLocations =
      'filesystem:apps/web/src/main/resources/db/migration,filesystem:apps/web/src/main/resources/db/seed/common,filesystem:apps/web/src/main/resources/db/seed/aws-dev';
    const flywayVersion = '12.9.0';
    const flywayDownloadUrl = `https://download.red-gate.com/maven/release/com/redgate/flyway/flyway-commandline/${flywayVersion}/flyway-commandline-${flywayVersion}-linux-x64.tar.gz`;
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
    // Migration runs as a CodePipeline-fed CodeBuild job inside the RDS VPC.
    this.migrationProject = new PipelineProject(this, 'MigrationCodeBuildProject', {
      projectName: `workops-${props.stage}-migration`,
      buildSpec: BuildSpec.fromObject({
        phases: {
          build: {
            commands: [
              'set -euo pipefail',
              'test -d apps/web/src/main/resources/db/migration',
              'curl -fsSL "$FLYWAY_DOWNLOAD_URL" -o flyway-commandline.tar.gz',
              'tar -xzf flyway-commandline.tar.gz',
              'export FLYWAY_URL="$WORKOPS_DB_URL"',
              'export FLYWAY_USER="$WORKOPS_DB_USERNAME"',
              'export FLYWAY_PASSWORD="$WORKOPS_DB_PASSWORD"',
              'export FLYWAY_LOCATIONS="$WORKOPS_FLYWAY_LOCATIONS"',
              './flyway-12.9.0/flyway migrate',
            ],
          },
        },
        version: '0.2',
      }),
      environment: {
        buildImage: LinuxBuildImage.AMAZON_LINUX_2023_5,
        computeType: ComputeType.SMALL,
        privileged: false,
        environmentVariables: {
          FLYWAY_DOWNLOAD_URL: {
            value: flywayDownloadUrl,
          },
          WORKOPS_DB_PASSWORD: {
            type: BuildEnvironmentVariableType.SECRETS_MANAGER,
            value: `${dbMasterSecret.secretArn}:password`,
          },
          WORKOPS_DB_URL: {
            type: BuildEnvironmentVariableType.PARAMETER_STORE,
            value: dbUrlParameter.parameterName,
          },
          WORKOPS_DB_USERNAME: {
            type: BuildEnvironmentVariableType.SECRETS_MANAGER,
            value: `${dbMasterSecret.secretArn}:username`,
          },
          WORKOPS_FLYWAY_LOCATIONS: {
            value: flywayLocations,
          },
        },
      },
      logging: {
        cloudWatch: {
          logGroup: props.migrationLogGroup,
          prefix: 'migration',
        },
      },
      securityGroups: [props.migrationSecurityGroup],
      subnetSelection: {
        subnets: props.appSubnets,
      },
      timeout: Duration.minutes(30),
      vpc: props.vpc,
    });
    props.migrationLogGroup.grantWrite(this.migrationProject);
    dbUrlParameter.grantRead(this.migrationProject);
    dbMasterSecret.grantRead(this.migrationProject);
    this.migrationProject.addToRolePolicy(
      new PolicyStatement({
        actions: ['s3:GetBucket*', 's3:GetObject*', 's3:List*'],
        effect: Effect.ALLOW,
        resources: [
          `arn:${Aws.PARTITION}:s3:::workops-${props.stage}-pipeline-artifacts`,
          `arn:${Aws.PARTITION}:s3:::workops-${props.stage}-pipeline-artifacts/*`,
        ],
      }),
    );

    this.migrationProjectNameOutput = new CfnOutput(this, 'migrationProjectName', {
      value: this.migrationProject.projectName,
    });
  }
}

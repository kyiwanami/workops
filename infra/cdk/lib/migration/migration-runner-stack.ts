import { Aws, Duration, Stack, StackProps } from 'aws-cdk-lib';
import {
  BuildEnvironmentVariableType,
  BuildSpec,
  ComputeType,
  LinuxBuildImage,
  PipelineProject,
} from 'aws-cdk-lib/aws-codebuild';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import { Secret as SecretsManagerSecret } from 'aws-cdk-lib/aws-secretsmanager';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';
import { foundationNetwork, foundationSecurityGroups, logsGroup } from '../shared/contract-imports';
import { readStage, stackName, stagePath } from '../shared/environment';

export class MigrationRunnerStack extends Stack {
  public readonly migrationProject: PipelineProject;

  constructor(scope: Construct, id: string, props: StackProps) {
    const stage = readStage(scope);
    super(scope, id, {
      ...props,
      stackName: stackName(scope, 'migration-runner'),
    });

    const network = foundationNetwork(this);
    const securityGroups = foundationSecurityGroups(this);
    const migrationLogGroup = logsGroup(this, 'MigrationLogGroup', 'migration');
    const dbUrlParameter = StringParameter.fromStringParameterName(
      this,
      'DbUrlParameter',
      stagePath(this, 'db/url'),
    );
    const dbMasterSecret = SecretsManagerSecret.fromSecretNameV2(
      this,
      'DbMasterSecret',
      stagePath(this, 'db/master'),
    );
    // MigrationRunner executes the db Maven project from the CodePipeline source artifact inside the RDS VPC.
    this.migrationProject = new PipelineProject(this, 'MigrationCodeBuildProject', {
      projectName: `workops-${stage}-migration-runner`,
      buildSpec: BuildSpec.fromObject({
        phases: {
          install: {
            'runtime-versions': {
              java: 'corretto25',
            },
          },
          build: {
            commands: [
              'set -euo pipefail',
              'mkdir -p "$CODEBUILD_SRC_DIR/.workops-codeartifact"',
              'export WORKOPS_MAVEN_SETTINGS_PATH="$CODEBUILD_SRC_DIR/.workops-codeartifact/settings.xml"',
              'export WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH="$CODEBUILD_SRC_DIR/.workops-codeartifact/codeartifact-token"',
              'python3 infra/cdk/scripts/configure-codeartifact-maven.py',
              'export CODEARTIFACT_AUTH_TOKEN="$(cat "$WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH")"',
              'cd db',
              'mvn --settings "$WORKOPS_MAVEN_SETTINGS_PATH" -Pdev flyway:migrate',
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
          WORKOPS_CODEARTIFACT_DOMAIN_NAME: {
            type: BuildEnvironmentVariableType.PARAMETER_STORE,
            value: stagePath(this, 'dependencies/codeartifact/domain-name'),
          },
          WORKOPS_CODEARTIFACT_MAVEN_REPOSITORY_NAME: {
            type: BuildEnvironmentVariableType.PARAMETER_STORE,
            value: stagePath(this, 'dependencies/codeartifact/maven-repository-name'),
          },
        },
      },
      logging: {
        cloudWatch: {
          logGroup: migrationLogGroup,
          prefix: 'migration',
        },
      },
      securityGroups: [securityGroups.migrationSecurityGroup],
      subnetSelection: {
        subnets: network.appSubnets,
      },
      timeout: Duration.minutes(30),
      vpc: network.vpc,
    });
    migrationLogGroup.grantWrite(this.migrationProject);
    dbUrlParameter.grantRead(this.migrationProject);
    dbMasterSecret.grantRead(this.migrationProject);
    for (const statement of this.createCodeArtifactPolicyStatements(stage)) {
      this.migrationProject.addToRolePolicy(statement);
    }
    this.migrationProject.addToRolePolicy(
      new PolicyStatement({
        actions: ['s3:GetBucket*', 's3:GetObject*', 's3:List*'],
        effect: Effect.ALLOW,
        resources: [
          `arn:${Aws.PARTITION}:s3:::workops-${stage}-pipeline-artifacts`,
          `arn:${Aws.PARTITION}:s3:::workops-${stage}-pipeline-artifacts/*`,
        ],
      }),
    );
  }

  private createCodeArtifactPolicyStatements(stage: string): PolicyStatement[] {
    const domainName = `workops-${stage}`;
    const mavenRepositoryName = `workops-${stage}-maven`;
    return [
      new PolicyStatement({
        actions: ['ssm:GetParameters'],
        effect: Effect.ALLOW,
        resources: [
          this.formatArn({
            service: 'ssm',
            resource: 'parameter',
            resourceName: `workops/${stage}/dependencies/codeartifact/*`,
          }),
        ],
      }),
      new PolicyStatement({
        actions: ['codeartifact:GetAuthorizationToken'],
        effect: Effect.ALLOW,
        resources: [
          this.formatArn({
            service: 'codeartifact',
            resource: 'domain',
            resourceName: domainName,
          }),
        ],
      }),
      new PolicyStatement({
        actions: ['codeartifact:GetRepositoryEndpoint', 'codeartifact:ReadFromRepository'],
        effect: Effect.ALLOW,
        resources: [
          this.formatArn({
            service: 'codeartifact',
            resource: 'repository',
            resourceName: `${domainName}/${mavenRepositoryName}`,
          }),
        ],
      }),
      new PolicyStatement({
        actions: ['sts:GetServiceBearerToken'],
        conditions: {
          StringEquals: {
            'sts:AWSServiceName': 'codeartifact.amazonaws.com',
          },
        },
        effect: Effect.ALLOW,
        resources: ['*'],
      }),
    ];
  }
}

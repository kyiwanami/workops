import { CfnOutput, Stack, StackProps } from 'aws-cdk-lib';
import {
  Effect,
  FederatedPrincipal,
  OpenIdConnectProvider,
  PolicyStatement,
  Role,
} from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export interface DeployStackProps extends StackProps {
  stage: string;
  githubRepository: string;
}

export class DeployStack extends Stack {
  constructor(scope: Construct, id: string, props: DeployStackProps) {
    super(scope, id, props);

    const provider = new OpenIdConnectProvider(this, 'GitHubActionsOidcProvider', {
      url: 'https://token.actions.githubusercontent.com',
      clientIds: ['sts.amazonaws.com'],
    });

    const deployRole = new Role(this, 'GitHubActionsDeployRole', {
      roleName: `workops-${props.stage}-github-actions-deploy`,
      assumedBy: new FederatedPrincipal(
        provider.openIdConnectProviderArn,
        {
          StringEquals: {
            'token.actions.githubusercontent.com:aud': 'sts.amazonaws.com',
            'token.actions.githubusercontent.com:sub': `repo:${props.githubRepository}:environment:dev`,
          },
        },
        'sts:AssumeRoleWithWebIdentity',
      ),
    });

    // P2-9 uses a dev-only broad deploy role until Phase 2 alpha replaces GitHub Actions with AWS native CI/CD.
    deployRole.addToPolicy(new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        'cloudformation:*',
        'cloudfront:*',
        'cognito-idp:*',
        'ec2:*',
        'ecr:*',
        'ecs:*',
        'elasticloadbalancing:*',
        'iam:*',
        'lambda:*',
        'logs:*',
        'rds:*',
        's3:*',
        'secretsmanager:*',
        'ssm:*',
        'sts:AssumeRole',
      ],
      resources: ['*'],
    }));

    new CfnOutput(this, 'githubActionsDeployRoleArn', {
      value: deployRole.roleArn,
    });
  }
}

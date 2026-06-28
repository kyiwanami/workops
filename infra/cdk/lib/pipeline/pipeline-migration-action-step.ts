import { IProject } from 'aws-cdk-lib/aws-codebuild';
import { IStage as CodePipelineStage } from 'aws-cdk-lib/aws-codepipeline';
import { CodeBuildAction } from 'aws-cdk-lib/aws-codepipeline-actions';
import {
  CodePipelineActionFactoryResult,
  CodePipelineFileSet,
  ICodePipelineActionFactory,
  ProduceActionOptions,
  Step,
} from 'aws-cdk-lib/pipelines';

interface MigrationActionStepProps {
  commitSha: string;
  input: CodePipelineFileSet;
  project: IProject;
}

export class MigrationActionStep extends Step implements ICodePipelineActionFactory {
  private readonly commitSha: string;
  private readonly input: CodePipelineFileSet;
  private readonly project: IProject;

  constructor(id: string, props: MigrationActionStepProps) {
    super(id);

    this.commitSha = props.commitSha;
    this.input = props.input;
    this.project = props.project;
    this.addDependencyFileSet(props.input);
  }

  public produceAction(
    stage: CodePipelineStage,
    options: ProduceActionOptions,
  ): CodePipelineActionFactoryResult {
    // The migration project is created in the target stage, but this action receives the pipeline source artifact directly.
    stage.addAction(
      new CodeBuildAction({
        actionName: options.actionName,
        environmentVariables: {
          COMMIT_SHA: {
            value: this.commitSha,
          },
        },
        input: options.artifacts.toCodePipeline(this.input),
        project: this.project,
        runOrder: options.runOrder,
        variablesNamespace: options.variablesNamespace,
      }),
    );

    return {
      runOrdersConsumed: 1,
    };
  }
}

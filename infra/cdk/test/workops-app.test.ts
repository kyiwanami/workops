import { existsSync, readdirSync, readFileSync } from 'fs';
import { join } from 'path';

describe('WorkOps CDK entrypoints', () => {
  test('keeps CDK entrypoints scoped and independent from dotenv', () => {
    const packageJsonPath = join(__dirname, '..', 'package.json');
    const cdkJsonPath = join(__dirname, '..', 'cdk.json');
    const cdkEntrypointPath = join(__dirname, '..', 'bin', 'cdk.ts');
    const packageJsonText = readFileSync(packageJsonPath, 'utf8');
    const cdkJsonText = readFileSync(cdkJsonPath, 'utf8');
    const cdkEntrypointText = readFileSync(cdkEntrypointPath, 'utf8');

    expect(packageJsonText).toContain('"build": "tsc"');
    expect(packageJsonText).toContain('"watch": "tsc -w"');
    expect(packageJsonText).toContain('node scripts/run-python-unittest.cjs');
    expect(packageJsonText).not.toContain(
      'python3 -m unittest discover scripts -p \\"*_test.py\\"',
    );
    expect(packageJsonText).not.toContain('"cdk:deploy-app"');
    expect(packageJsonText).not.toContain('"cdk' + ':infra"');
    expect(packageJsonText).not.toContain('"cdk' + ':runtime"');
    expect(packageJsonText).not.toContain('"cdk": "cdk"');
    expect(packageJsonText).not.toContain('"bin"');
    expect(packageJsonText).not.toContain('"cdk:pipeline"');
    expect(cdkJsonText).toContain('"app": "npx ts-node --prefer-ts-exts bin/cdk.ts"');
    expect(existsSync(join(__dirname, '..', 'bin', 'cdk-pipeline.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'bin', 'cdk-deploy.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'bin', ['cdk', 'infra.ts'].join('-')))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'bin', ['cdk', 'runtime.ts'].join('-')))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'lib', 'deploy-stack.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'lib', 'config-stack.ts'))).toBe(false);
    expect(existsSync(join(__dirname, '..', 'lib', 'secret-stack.ts'))).toBe(false);
    expect(cdkEntrypointText).toContain('DependencyStack');
    expect(cdkEntrypointText).toContain('PipelineStack');
    expect(cdkEntrypointText).toContain('GITHUB_REPOSITORY');
    expect(cdkEntrypointText).toContain('WORKOPS_IMAGE_TAG');
    expect(cdkEntrypointText).toContain('WORKOPS_OPS_NOTIFICATION_EMAIL');
    expect(cdkEntrypointText).not.toContain('WORKOPS_PIPELINE_NOTIFICATION_EMAIL');
    expect(cdkEntrypointText).not.toContain('WORKOPS_WEB_IMAGE_TAG');
    expect(cdkEntrypointText).not.toContain('AppRuntimeStack');
    expect(packageJsonText).not.toContain('synth:dev');
    expect(packageJsonText).not.toContain('diff:dev');
    expect(packageJsonText).not.toContain('deploy:dev');
    expect(packageJsonText).not.toContain('dotenv');
    expect(cdkEntrypointText).toContain('WORKOPS_SOURCE_BRANCH');
    expect(cdkEntrypointText).not.toContain('WORKOPS_STAGE');
    expect(cdkEntrypointText).not.toContain('tryGetContext');
    expect(cdkEntrypointText).not.toContain('dotenv');
    expect(cdkEntrypointText).not.toContain('.env.local');
  });

  test('removes GitHub Actions workflows from Phase 2 alpha CI/CD', () => {
    const workflowsPath = join(__dirname, '..', '..', '..', '.github', 'workflows');
    const workflowFiles = existsSync(workflowsPath) ? readdirSync(workflowsPath) : [];

    expect(workflowFiles).toEqual([]);
  });
});

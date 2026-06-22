import tseslint from 'typescript-eslint';

export default tseslint.config(
  {
    ignores: [
      'node_modules/',
      'cdk.out/',
      'cdk.out.*/',
      'coverage/',
      'dist/',
      '*.mjs',
      '**/*.js',
      '**/*.d.ts',
    ],
  },
  ...tseslint.configs.strictTypeChecked,
  {
    files: ['**/*.ts'],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
  },
);

// CloudFormation exports bridge pipeline stages without redefining shared stacks.
export function exportName(stage: string, name: string): string {
  return `workops-${stage}-${name}`;
}

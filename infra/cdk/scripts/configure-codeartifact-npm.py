#!/usr/bin/env python3
import os
import subprocess


def env(name):
    value = os.environ.get(name)
    if not value:
        raise RuntimeError(f"{name} environment variable is required")
    return value


domain_name = env("WORKOPS_CODEARTIFACT_DOMAIN_NAME")
repository_name = env("WORKOPS_CODEARTIFACT_NPM_REPOSITORY_NAME")
region = env("AWS_DEFAULT_REGION")
account_id = subprocess.check_output(
    [
        "aws",
        "sts",
        "get-caller-identity",
        "--query",
        "Account",
        "--output",
        "text",
        "--region",
        region,
    ],
    text=True,
).strip()

# npm uses AWS CLI's official CodeArtifact login flow so token material stays outside git.
subprocess.run(
    [
        "aws",
        "codeartifact",
        "login",
        "--tool",
        "npm",
        "--domain",
        domain_name,
        "--domain-owner",
        account_id,
        "--repository",
        repository_name,
        "--region",
        region,
    ],
    check=True,
)

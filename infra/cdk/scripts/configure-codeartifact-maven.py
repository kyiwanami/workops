#!/usr/bin/env python3
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET


def env(name):
    value = os.environ.get(name)
    if not value:
        raise RuntimeError(f"{name} environment variable is required")
    return value


def aws_text(args):
    return subprocess.check_output(args, text=True).strip()


def add_text(parent, tag_name, text):
    child = ET.SubElement(parent, tag_name)
    child.text = text
    return child


domain_name = env("WORKOPS_CODEARTIFACT_DOMAIN_NAME")
repository_name = env("WORKOPS_CODEARTIFACT_MAVEN_REPOSITORY_NAME")
settings_path = Path(env("WORKOPS_MAVEN_SETTINGS_PATH"))
token_path = Path(env("WORKOPS_CODEARTIFACT_AUTH_TOKEN_PATH"))
region = env("AWS_DEFAULT_REGION")
account_id = aws_text(
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
    ]
)
codeartifact_args = [
    "--domain",
    domain_name,
    "--domain-owner",
    account_id,
    "--repository",
    repository_name,
    "--region",
    region,
]
repository_endpoint = aws_text(
    [
        "aws",
        "codeartifact",
        "get-repository-endpoint",
        *codeartifact_args,
        "--format",
        "maven",
        "--query",
        "repositoryEndpoint",
        "--output",
        "text",
    ]
)
authorization_token = aws_text(
    [
        "aws",
        "codeartifact",
        "get-authorization-token",
        "--domain",
        domain_name,
        "--domain-owner",
        account_id,
        "--query",
        "authorizationToken",
        "--output",
        "text",
        "--region",
        region,
    ]
)

settings = ET.Element("settings")
settings.set("xmlns", "http://maven.apache.org/SETTINGS/1.0.0")
settings.set("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
settings.set(
    "xsi:schemaLocation",
    "http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd",
)
server = ET.SubElement(ET.SubElement(settings, "servers"), "server")
add_text(server, "id", "workops-codeartifact")
add_text(server, "username", "aws")
add_text(server, "password", "${env.CODEARTIFACT_AUTH_TOKEN}")
mirror = ET.SubElement(ET.SubElement(settings, "mirrors"), "mirror")
add_text(mirror, "id", "workops-codeartifact")
add_text(mirror, "name", "WorkOps CodeArtifact Maven")
add_text(mirror, "url", repository_endpoint)
add_text(mirror, "mirrorOf", "central")

# Maven settings references the token through an environment variable, keeping token material out of the XML.
settings_path.parent.mkdir(parents=True, exist_ok=True)
token_path.parent.mkdir(parents=True, exist_ok=True)
ET.ElementTree(settings).write(settings_path, encoding="utf-8", xml_declaration=True)
token_path.write_text(authorization_token, encoding="utf-8")

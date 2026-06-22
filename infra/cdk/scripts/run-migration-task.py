import json
import os
import subprocess
import sys


def fail(message):
    print(message, file=sys.stderr)
    sys.exit(1)


def aws_ecs(args):
    command = ["aws", "ecs", *args, "--region", os.environ["AWS_DEFAULT_REGION"]]
    completed = subprocess.run(command, capture_output=True, text=True)
    if completed.returncode != 0:
        if completed.stderr:
            print(completed.stderr, file=sys.stderr)
        fail(f"command failed: {' '.join(command)}")
    return completed


def aws_ecs_json(args):
    completed = aws_ecs([*args, "--output", "json"])
    return json.loads(completed.stdout)


def task_from(response, label):
    failures = response.get("failures", [])
    if failures:
        fail(f"{label} returned failures: {json.dumps(failures)}")
    tasks = response.get("tasks", [])
    if len(tasks) != 1:
        fail(f"{label} did not return exactly one task")
    return tasks[0]


# RunTask success is accepted only after ECS stops the task and the migration container exits 0.
cluster_name = os.environ["MIGRATION_CLUSTER_NAME"]
container_name = os.environ["MIGRATION_CONTAINER_NAME"]
security_group_id = os.environ["MIGRATION_SECURITY_GROUP_ID"]
subnet_ids = os.environ["MIGRATION_SUBNET_IDS"]
task_definition_arn = os.environ["MIGRATION_TASK_DEFINITION_ARN"]
network_configuration = (
    f"awsvpcConfiguration={{subnets=[{subnet_ids}],"
    f"securityGroups=[{security_group_id}],assignPublicIp=DISABLED}}"
)

run_task = aws_ecs_json(
    [
        "run-task",
        "--cluster",
        cluster_name,
        "--task-definition",
        task_definition_arn,
        "--launch-type",
        "FARGATE",
        "--network-configuration",
        network_configuration,
    ]
)
task_arn = task_from(run_task, "run-task").get("taskArn")
if not task_arn:
    fail("run-task response did not include taskArn")

aws_ecs(
    [
        "wait",
        "tasks-stopped",
        "--cluster",
        cluster_name,
        "--tasks",
        task_arn,
    ]
)

describe_tasks = aws_ecs_json(
    [
        "describe-tasks",
        "--cluster",
        cluster_name,
        "--tasks",
        task_arn,
    ]
)
task = task_from(describe_tasks, "describe-tasks")
stop_code = task.get("stopCode")
if stop_code != "EssentialContainerExited":
    stopped_reason = task.get("stoppedReason", "")
    fail(f"Unexpected stopCode: {stop_code} {stopped_reason}")
containers = task.get("containers", [])
container = next((item for item in containers if item.get("name") == container_name), None)
if container is None:
    fail(f"Migration container not found: {container_name}")
exit_code = container.get("exitCode")
if exit_code is None:
    reason = container.get("reason", "")
    fail(f"Migration container exitCode is missing: {reason}")
if exit_code != 0:
    reason = container.get("reason", "")
    print(f"Migration container failed with exitCode {exit_code}: {reason}", file=sys.stderr)
    sys.exit(exit_code)

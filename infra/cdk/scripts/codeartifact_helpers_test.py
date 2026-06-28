import os
import subprocess
import sys
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).parent


class CodeArtifactHelperTests(unittest.TestCase):
    def run_script(self, script_name):
        env = {"PATH": os.environ.get("PATH", "")}
        return subprocess.run(
            [sys.executable, str(SCRIPT_DIR / script_name)],
            capture_output=True,
            env=env,
            text=True,
        )

    def test_npm_helper_fails_when_domain_is_missing(self):
        completed = self.run_script("configure-codeartifact-npm.py")

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("WORKOPS_CODEARTIFACT_DOMAIN_NAME environment variable is required", completed.stderr)

    def test_maven_helper_fails_when_domain_is_missing(self):
        completed = self.run_script("configure-codeartifact-maven.py")

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("WORKOPS_CODEARTIFACT_DOMAIN_NAME environment variable is required", completed.stderr)


if __name__ == "__main__":
    unittest.main()

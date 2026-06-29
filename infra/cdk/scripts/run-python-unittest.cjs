const { existsSync } = require('node:fs');
const { join } = require('node:path');
const { spawnSync } = require('node:child_process');

// Windows must use the Python installed by pymanager instead of WindowsApps aliases.
const candidates =
  process.platform === 'win32'
    ? [
        process.env.WORKOPS_PYTHON,
        process.env.LOCALAPPDATA
          ? join(process.env.LOCALAPPDATA, 'Python', 'bin', 'python.exe')
          : undefined,
        process.env.USERPROFILE
          ? join(process.env.USERPROFILE, 'AppData', 'Local', 'Python', 'bin', 'python.exe')
          : undefined,
      ]
    : [process.env.WORKOPS_PYTHON, 'python3'];

const python = candidates.find((candidate) => {
  if (!candidate) {
    return false;
  }
  if (candidate === 'python3') {
    return true;
  }
  return existsSync(candidate);
});

if (!python) {
  console.error('Python executable was not found.');
  process.exit(1);
}

const result = spawnSync(python, ['-m', 'unittest', 'discover', 'scripts', '-p', '*_test.py'], {
  stdio: 'inherit',
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);

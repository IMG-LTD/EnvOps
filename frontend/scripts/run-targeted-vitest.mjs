import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const cwd = resolve(scriptDir, '..');
const specs = process.argv.slice(2).filter(Boolean);

if (specs.length === 0) {
  console.error('Usage: pnpm test:unit:target <spec...>');
  process.exit(1);
}

const command = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
const result = spawnSync(command, ['exec', 'vitest', 'run', ...specs], {
  cwd,
  stdio: 'inherit'
});

process.exit(result.status ?? 1);

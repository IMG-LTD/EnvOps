import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const deployTaskPageSource = readFileSync(path.resolve(__dirname, 'task/index.vue'), 'utf8');

describe('deploy task action RBAC contract', () => {
  it('gates deploy task actions with fixed RBAC permission codes', () => {
    expect(deployTaskPageSource).toMatch(/useAuth\s*\(/);

    expect(deployTaskPageSource).toContain('deploy:task:create');
    expect(deployTaskPageSource).toContain('deploy:task:approve');
    expect(deployTaskPageSource).toContain('deploy:task:execute');
    expect(deployTaskPageSource).toContain('deploy:task:cancel');
    expect(deployTaskPageSource).toContain('deploy:task:retry');
    expect(deployTaskPageSource).toContain('deploy:task:rollback');
  });
});

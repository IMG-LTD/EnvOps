import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const detectTaskPageSource = readFileSync(path.resolve(__dirname, 'detect-task/index.vue'), 'utf8');

describe('monitor action RBAC contract', () => {
  it('gates detect task create and execute actions with monitor execute permission', () => {
    expect(detectTaskPageSource).toMatch(/useAuth\s*\(/);
    expect(detectTaskPageSource).toContain('monitor:detect-task:execute');
  });
});

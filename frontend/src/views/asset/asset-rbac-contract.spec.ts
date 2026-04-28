import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const hostPageSource = readFileSync(path.resolve(__dirname, 'host/index.vue'), 'utf8');
const credentialPageSource = readFileSync(path.resolve(__dirname, 'credential/index.vue'), 'utf8');
const databasePageSource = readFileSync(path.resolve(__dirname, 'database/index.vue'), 'utf8');

function expectPermissionWiring(source: string, permissionCode: string) {
  expect(source).toMatch(/useAuth\s*\(/);
  expect(source).toContain(permissionCode);
}

describe('asset action RBAC contract', () => {
  it('gates host management actions with asset host manage permission', () => {
    expectPermissionWiring(hostPageSource, 'asset:host:manage');
  });

  it('gates credential management actions with asset credential manage permission', () => {
    expectPermissionWiring(credentialPageSource, 'asset:credential:manage');
  });

  it('gates database management and connectivity actions with asset database permissions', () => {
    expectPermissionWiring(databasePageSource, 'asset:database:manage');
    expect(databasePageSource).toContain('asset:database:connectivity-check');
  });
});

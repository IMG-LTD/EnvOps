import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const appPages = [
  {
    name: 'definition',
    source: readFileSync(path.resolve(__dirname, 'definition/index.vue'), 'utf8'),
    permissionCode: 'app:definition:manage'
  },
  {
    name: 'version',
    source: readFileSync(path.resolve(__dirname, 'version/index.vue'), 'utf8'),
    permissionCode: 'app:version:manage'
  },
  {
    name: 'package',
    source: readFileSync(path.resolve(__dirname, 'package/index.vue'), 'utf8'),
    permissionCode: 'app:package:manage'
  },
  {
    name: 'config template',
    source: readFileSync(path.resolve(__dirname, 'config-template/index.vue'), 'utf8'),
    permissionCode: 'app:config-template:manage'
  },
  {
    name: 'script template',
    source: readFileSync(path.resolve(__dirname, 'script-template/index.vue'), 'utf8'),
    permissionCode: 'app:script-template:manage'
  }
];

describe('app action RBAC contract', () => {
  it.each(appPages)('gates $name mutating actions with its manage permission', page => {
    expect(page.source).toMatch(/useAuth\s*\(/);
    expect(page.source).toContain(page.permissionCode);
  });
});

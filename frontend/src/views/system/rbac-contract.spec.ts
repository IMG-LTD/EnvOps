import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const root = process.cwd();
const apiSource = readFileSync(resolve(root, 'src/service/api/system-rbac.ts'), 'utf8');
const apiIndexSource = readFileSync(resolve(root, 'src/service/api/index.ts'), 'utf8');
const typingSource = readFileSync(resolve(root, 'src/typings/api/system-rbac.d.ts'), 'utf8');
const routesSource = readFileSync(resolve(root, 'src/router/elegant/routes.ts'), 'utf8');
const zhSource = readFileSync(resolve(root, 'src/locales/langs/zh-cn.ts'), 'utf8');
const enSource = readFileSync(resolve(root, 'src/locales/langs/en-us.ts'), 'utf8');

describe('system rbac frontend contract', () => {
  it('declares rbac api methods and endpoints', () => {
    expect(apiSource).toContain('fetchGetSystemRbacRoles');
    expect(apiSource).toContain('/api/system/rbac/roles');
    expect(apiSource).toContain('fetchGetSystemRbacPermissions');
    expect(apiSource).toContain('/api/system/rbac/permissions');
    expect(apiSource).toContain('fetchGetSystemRbacRolePermissions');
    expect(apiSource).toContain('fetchUpdateSystemRbacRolePermissions');
    expect(apiIndexSource).toContain("export * from './system-rbac'");
  });

  it('declares rbac typings', () => {
    expect(typingSource).toContain('namespace SystemRbac');
    expect(typingSource).toContain('interface RoleRecord');
    expect(typingSource).toContain('interface PermissionNode');
    expect(typingSource).toContain('interface UpdateRolePermissionsParams');
  });

  it('registers system rbac route and locales', () => {
    expect(routesSource).toContain("name: 'system_rbac'");
    expect(routesSource).toContain("path: '/system/rbac'");
    expect(routesSource).toContain("component: 'view.system_rbac'");
    expect(zhSource).toContain("system_rbac: '权限管理'");
    expect(enSource).toContain("system_rbac: 'Permission Management'");
    expect(zhSource).toContain('systemRbac');
    expect(enSource).toContain('systemRbac');
  });
});

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
const pageSource = readFileSync(resolve(root, 'src/views/system/rbac/index.vue'), 'utf8');

function extractSourceBlock(source: string, start: string, end: string) {
  const startIndex = source.indexOf(start);
  expect(startIndex).toBeGreaterThanOrEqual(0);

  const endIndex = source.indexOf(end, startIndex + start.length);
  expect(endIndex).toBeGreaterThan(startIndex);

  return source.slice(startIndex, endIndex);
}

function expectInOrder(source: string, patterns: RegExp[]) {
  let searchStart = 0;

  for (const pattern of patterns) {
    const match = pattern.exec(source.slice(searchStart));
    expect(match).not.toBeNull();
    searchStart += (match?.index ?? 0) + (match?.[0].length ?? 0);
  }
}

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

  it('implements role-first permission management behavior', () => {
    expect(pageSource).toContain("name: 'SystemRbacPage'");
    expect(pageSource).toContain('system:role:manage');
    expect(pageSource).toContain('fetchGetSystemRbacRoles');
    expect(pageSource).toContain('fetchGetSystemRbacPermissions');
    expect(pageSource).toContain('fetchUpdateSystemRbacRolePermissions');
    expect(pageSource).toContain('collectActionKeys');
    expect(pageSource).toContain('isActionDisabled');
    expect(pageSource).toContain('handleSavePermissions');
  });

  it('guards stale role permission loads before saving permissions', () => {
    expect(pageSource).toContain('const loadingRolePermissions = ref(false);');
    expect(pageSource).toContain('const permissionsLoadedForRoleId = ref<number | null>(null);');

    const selectRoleSource = extractSourceBlock(pageSource, 'async function selectRole', 'function handleCreateRole');

    expectInOrder(selectRoleSource, [
      /selectedRoleId\.value = role\.id;/,
      /fillRoleForm\(role\);/,
      /assignedPermissionKeys\.value = \[\];/,
      /permissionsLoadedForRoleId\.value = null;/,
      /loadingRolePermissions\.value = true;/
    ]);
    expect(selectRoleSource).toMatch(
      /if \(!response\.error && selectedRoleId\.value === role\.id\) \{[\s\S]*assignedPermissionKeys\.value = response\.data\.permissionKeys;[\s\S]*permissionsLoadedForRoleId\.value = role\.id;/
    );
    expect(selectRoleSource).toMatch(
      /finally \{[\s\S]*if \(selectedRoleId\.value === role\.id\) \{[\s\S]*loadingRolePermissions\.value = false;/
    );

    const savePermissionsSource = extractSourceBlock(
      pageSource,
      'async function handleSavePermissions',
      'async function loadPageData'
    );
    expect(savePermissionsSource).toMatch(
      /if \([\s\S]*selectedRoleId\.value === null[\s\S]*loadingRolePermissions\.value[\s\S]*permissionsLoadedForRoleId\.value !== selectedRoleId\.value[\s\S]*\) \{[\s\S]*return;/
    );

    const saveButtonSource = extractSourceBlock(
      pageSource,
      '<NButton\n                  type="primary"\n                  :loading="savingPermissions"',
      '@click="handleSavePermissions"'
    );
    expect(saveButtonSource).toMatch(
      /:disabled="[\s\S]*loadingRolePermissions[\s\S]*permissionsLoadedForRoleId !== selectedRoleId[\s\S]*"/
    );
  });

  it('guards stale role permission save responses', () => {
    const savePermissionsSource = extractSourceBlock(
      pageSource,
      'async function handleSavePermissions',
      'async function loadPageData'
    );

    expect(savePermissionsSource).toMatch(
      /if \([\s\S]*selectedRoleId\.value === null[\s\S]*loadingRolePermissions\.value[\s\S]*permissionsLoadedForRoleId\.value !== selectedRoleId\.value[\s\S]*savingPermissions\.value[\s\S]*\) \{[\s\S]*return;/
    );
    expectInOrder(savePermissionsSource, [
      /const roleId = selectedRoleId\.value;/,
      /fetchUpdateSystemRbacRolePermissions\(roleId,\s*\{/,
      /if \(!response\.error && selectedRoleId\.value === roleId\) \{/,
      /assignedPermissionKeys\.value = response\.data\.permissionKeys;/
    ]);

    const saveButtonSource = extractSourceBlock(
      pageSource,
      '<NButton\n                  type="primary"\n                  :loading="savingPermissions"',
      '@click="handleSavePermissions"'
    );
    expect(saveButtonSource).toMatch(
      /:disabled="[\s\S]*savingPermissions[\s\S]*loadingRolePermissions[\s\S]*permissionsLoadedForRoleId !== selectedRoleId[\s\S]*"/
    );
  });
});

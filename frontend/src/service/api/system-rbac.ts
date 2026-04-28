import { request } from '../request';

/** get RBAC roles */
export function fetchGetSystemRbacRoles() {
  return request<Api.SystemRbac.RoleListResponse>({ url: '/api/system/rbac/roles' });
}

/** create RBAC role */
export function fetchCreateSystemRbacRole(data: Api.SystemRbac.CreateRoleParams) {
  return request<Api.SystemRbac.RoleRecord>({
    url: '/api/system/rbac/roles',
    method: 'post',
    data
  });
}

/** update RBAC role */
export function fetchUpdateSystemRbacRole(id: number, data: Api.SystemRbac.UpdateRoleParams) {
  return request<Api.SystemRbac.RoleRecord>({
    url: `/api/system/rbac/roles/${id}`,
    method: 'put',
    data
  });
}

/** get fixed permission tree */
export function fetchGetSystemRbacPermissions() {
  return request<Api.SystemRbac.PermissionTreeResponse>({ url: '/api/system/rbac/permissions' });
}

/** get role permissions */
export function fetchGetSystemRbacRolePermissions(id: number) {
  return request<Api.SystemRbac.RolePermissionsResponse>({ url: `/api/system/rbac/roles/${id}/permissions` });
}

/** replace role permissions */
export function fetchUpdateSystemRbacRolePermissions(id: number, data: Api.SystemRbac.UpdateRolePermissionsParams) {
  return request<Api.SystemRbac.RolePermissionsResponse>({
    url: `/api/system/rbac/roles/${id}/permissions`,
    method: 'put',
    data
  });
}

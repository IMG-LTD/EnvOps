import { request } from '../request';

/** get system users */
export function fetchGetSystemUsers() {
  return request<Api.SystemUser.SystemUserListResponse>({ url: '/api/system/users' });
}

/** create system user */
export function fetchCreateSystemUser(data: Api.SystemUser.CreateSystemUserParams) {
  return request<Api.SystemUser.SystemUserRecord>({
    url: '/api/system/users',
    method: 'post',
    data
  });
}

/** update system user */
export function fetchUpdateSystemUser(id: number, data: Api.SystemUser.UpdateSystemUserParams) {
  return request<Api.SystemUser.SystemUserRecord>({
    url: `/api/system/users/${id}`,
    method: 'put',
    data
  });
}

/** get system user roles */
export function fetchGetSystemUserRoles(id: number) {
  return request<Api.SystemUser.UserRoleAssignmentResponse>({ url: `/api/system/users/${id}/roles` });
}

/** replace system user roles */
export function fetchUpdateSystemUserRoles(id: number, data: Api.SystemUser.UpdateSystemUserRolesParams) {
  return request<Api.SystemUser.UserRoleAssignmentResponse>({
    url: `/api/system/users/${id}/roles`,
    method: 'put',
    data
  });
}

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

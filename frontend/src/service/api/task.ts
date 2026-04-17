import { request } from '../request';

/** get deploy tasks */
export function fetchGetDeployTasks() {
  return request<Api.Task.DeployTaskRecord[]>({ url: '/api/deploy/tasks' });
}

/** execute deploy task */
export function fetchPostExecuteDeployTask(id: number) {
  return request<Api.Task.DeployTaskRecord>({
    url: `/api/deploy/tasks/${id}/execute`,
    method: 'post'
  });
}

/** retry deploy task */
export function fetchPostRetryDeployTask(id: number) {
  return request<Api.Task.DeployTaskRecord>({
    url: `/api/deploy/tasks/${id}/retry`,
    method: 'post'
  });
}

/** rollback deploy task */
export function fetchPostRollbackDeployTask(id: number) {
  return request<Api.Task.DeployTaskRecord>({
    url: `/api/deploy/tasks/${id}/rollback`,
    method: 'post'
  });
}

/** cancel deploy task */
export function fetchPostCancelDeployTask(id: number) {
  return request<Api.Task.DeployTaskRecord>({
    url: `/api/deploy/tasks/${id}/cancel`,
    method: 'post'
  });
}

/** get deploy task hosts */
export function fetchGetDeployTaskHosts(id: number) {
  return request<Api.Task.DeployTaskHostRecord[]>({ url: `/api/deploy/tasks/${id}/hosts` });
}

/** get deploy task logs */
export function fetchGetDeployTaskLogs(id: number) {
  return request<Api.Task.DeployTaskLogRecord[]>({ url: `/api/deploy/tasks/${id}/logs` });
}

/** get task center tasks */
export function fetchGetTaskCenterTasks() {
  return request<Api.Task.TaskCenterRecord[]>({ url: '/api/task-center/tasks' });
}

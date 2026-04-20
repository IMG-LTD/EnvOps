import { request } from '../request';

/** get deploy tasks */
export function fetchGetDeployTasks(params: Api.Task.DeployTaskListQuery) {
  return request<Api.Task.DeployTaskPage>({
    url: '/api/deploy/tasks',
    params
  });
}

/** get deploy task detail */
export function fetchGetDeployTask(id: number) {
  return request<Api.Task.DeployTaskDetailRecord>({ url: `/api/deploy/tasks/${id}` });
}

/** create deploy task */
export function fetchPostCreateDeployTask(data: Api.Task.CreateDeployTaskPayload) {
  return request<Api.Task.DeployTaskRecord>({
    url: '/api/deploy/tasks',
    method: 'post',
    data: {
      taskName: data.taskName,
      taskType: data.taskType,
      appId: data.appId,
      versionId: data.versionId,
      hostIds: data.hostIds,
      batchStrategy: data.batchStrategy,
      ...(data.batchSize !== null && data.batchSize !== undefined ? { batchSize: data.batchSize } : {}),
      params: {
        environment: data.environment,
        sshUser: data.sshUser,
        ...(data.sshPort !== null && data.sshPort !== undefined ? { sshPort: data.sshPort } : {}),
        privateKeyPath: data.privateKeyPath,
        remoteBaseDir: data.remoteBaseDir,
        ...(data.rollbackCommand ? { rollbackCommand: data.rollbackCommand } : {})
      }
    }
  });
}

/** approve deploy task */
export function fetchPostApproveDeployTask(id: number, data?: Api.Task.DeployTaskApprovalPayload) {
  return request<Api.Task.DeployTaskRecord>({
    url: `/api/deploy/tasks/${id}/approve`,
    method: 'post',
    data
  });
}

/** reject deploy task */
export function fetchPostRejectDeployTask(id: number, data?: Api.Task.DeployTaskApprovalPayload) {
  return request<Api.Task.DeployTaskRecord>({
    url: `/api/deploy/tasks/${id}/reject`,
    method: 'post',
    data
  });
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
export function fetchGetDeployTaskHosts(id: number, params: Api.Task.DeployTaskHostQuery) {
  return request<Api.Task.DeployTaskHostPage>({
    url: `/api/deploy/tasks/${id}/hosts`,
    params
  });
}

/** get deploy task logs */
export function fetchGetDeployTaskLogs(id: number, params: Api.Task.DeployTaskLogQuery) {
  return request<Api.Task.DeployTaskLogPage>({
    url: `/api/deploy/tasks/${id}/logs`,
    params
  });
}

/** get task center tasks */
export function fetchGetTaskCenterTasks(params: Api.Task.TaskCenterListQuery) {
  return request<Api.Task.TaskCenterPage>({
    url: '/api/task-center/tasks',
    params
  });
}

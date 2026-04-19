import { request } from '../request';

/** get monitor detect tasks */
export function fetchGetMonitorDetectTasks() {
  return request<Api.Monitor.DetectTaskListResponse>({ url: '/api/monitor/detect-tasks' });
}

/** create monitor detect task */
export function fetchPostCreateMonitorDetectTask(data: Api.Monitor.CreateDetectTaskParams) {
  return request<Api.Monitor.DetectTaskRecord>({
    url: '/api/monitor/detect-tasks',
    method: 'post',
    data
  });
}

/** execute monitor detect task */
export function fetchPostExecuteMonitorDetectTask(id: number) {
  return request<Api.Monitor.DetectTaskRecord>({
    url: `/api/monitor/detect-tasks/${id}/execute`,
    method: 'post'
  });
}

/** get latest monitor host facts */
export function fetchGetMonitorHostFactsLatest(hostId: number) {
  return request<Api.Monitor.HostFactRecord>({ url: `/api/monitor/hosts/${hostId}/facts/latest` });
}

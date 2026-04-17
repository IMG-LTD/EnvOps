import { request } from '../request';

/** get monitor detect tasks */
export function fetchGetMonitorDetectTasks() {
  return request<Api.Monitor.DetectTaskListResponse>({ url: '/api/monitor/detect-tasks' });
}

/** get latest monitor host facts */
export function fetchGetMonitorHostFactsLatest(hostId: number) {
  return request<Api.Monitor.HostFactRecord>({ url: `/api/monitor/hosts/${hostId}/facts/latest` });
}

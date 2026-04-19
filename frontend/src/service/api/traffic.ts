import { request } from '../request';

/** get traffic policies */
export function fetchGetTrafficPolicies() {
  return request<Api.Traffic.TrafficPolicyListResponse>({ url: '/api/traffic/policies' });
}

/** get traffic plugins */
export function fetchGetTrafficPlugins() {
  return request<Api.Traffic.TrafficPluginListResponse>({ url: '/api/traffic/plugins' });
}

/** preview traffic policy */
export function fetchPostPreviewTrafficPolicy(id: number) {
  return request<Api.Traffic.TrafficPolicyActionRecord>({
    url: `/api/traffic/policies/${id}/preview`,
    method: 'post'
  });
}

/** apply traffic policy */
export function fetchPostApplyTrafficPolicy(id: number) {
  return request<Api.Traffic.TrafficPolicyActionRecord>({
    url: `/api/traffic/policies/${id}/apply`,
    method: 'post'
  });
}

/** rollback traffic policy */
export function fetchPostRollbackTrafficPolicy(id: number) {
  return request<Api.Traffic.TrafficPolicyActionRecord>({
    url: `/api/traffic/policies/${id}/rollback`,
    method: 'post'
  });
}

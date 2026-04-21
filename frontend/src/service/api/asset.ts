import { request } from '../request';

/** get asset hosts */
export function fetchGetAssetHosts(params: Api.Asset.HostQuery) {
  return request<Api.Asset.HostPage>({
    url: '/api/assets/hosts',
    params
  });
}

/** create asset host */
export function fetchCreateAssetHost(data: Api.Asset.CreateHostParams) {
  return request<Api.Asset.HostRecord>({
    url: '/api/assets/hosts',
    method: 'post',
    data
  });
}

/** get asset databases */
export function fetchGetAssetDatabases(params: Api.Asset.DatabaseQuery) {
  return request<Api.Asset.DatabasePage>({
    url: '/api/assets/databases',
    params
  });
}

/** create asset database */
export function fetchCreateAssetDatabase(data: Api.Asset.CreateDatabaseParams) {
  return request<Api.Asset.DatabaseRecord>({
    url: '/api/assets/databases',
    method: 'post',
    data
  });
}

/** update asset database */
export function fetchUpdateAssetDatabase(id: number, data: Api.Asset.UpdateDatabaseParams) {
  return request<Api.Asset.DatabaseRecord>({
    url: `/api/assets/databases/${id}`,
    method: 'put',
    data
  });
}

/** check one asset database connectivity */
export function fetchCheckAssetDatabase(id: number) {
  return request<Api.Asset.DatabaseConnectivityCheckResponse>({
    url: `/api/assets/databases/${id}/connectivity-check`,
    method: 'post'
  });
}

/** check selected asset databases connectivity */
export function fetchCheckSelectedAssetDatabases(ids: number[]) {
  return request<Api.Asset.DatabaseConnectivityCheckResponse>({
    url: '/api/assets/databases/connectivity-check:selected',
    method: 'post',
    data: { ids }
  });
}

/** check current page asset databases connectivity */
export function fetchCheckCurrentPageAssetDatabases(ids: number[]) {
  return request<Api.Asset.DatabaseConnectivityCheckResponse>({
    url: '/api/assets/databases/connectivity-check:page',
    method: 'post',
    data: { ids }
  });
}

/** check queried asset databases connectivity */
export function fetchCheckQueriedAssetDatabases(query: Api.Asset.DatabaseQuery) {
  return request<Api.Asset.DatabaseConnectivityCheckResponse>({
    url: '/api/assets/databases/connectivity-check:query',
    method: 'post',
    data: {
      keyword: query.keyword || null,
      environment: query.environment || null,
      databaseType: query.databaseType || null,
      lifecycleStatus: query.lifecycleStatus || null,
      connectivityStatus: query.connectivityStatus || null
    }
  });
}

/** delete asset database */
export function fetchDeleteAssetDatabase(id: number) {
  return request<boolean>({
    url: `/api/assets/databases/${id}`,
    method: 'delete'
  });
}

/** get asset credentials */
export function fetchGetAssetCredentials() {
  return request<Api.Asset.CredentialRecord[]>({ url: '/api/assets/credentials' });
}

/** create asset credential */
export function fetchCreateAssetCredential(data: Api.Asset.CreateCredentialParams) {
  return request<Api.Asset.CredentialRecord>({
    url: '/api/assets/credentials',
    method: 'post',
    data
  });
}

/** get asset groups */
export function fetchGetAssetGroups() {
  return request<Api.Asset.GroupRecord[]>({ url: '/api/assets/groups' });
}

/** get asset tags */
export function fetchGetAssetTags() {
  return request<Api.Asset.TagRecord[]>({ url: '/api/assets/tags' });
}

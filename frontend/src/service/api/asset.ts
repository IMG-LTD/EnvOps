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

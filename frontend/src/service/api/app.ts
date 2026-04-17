import { request } from '../request';

export function fetchGetApps() {
  return request<Api.App.AppDefinition[]>({ url: '/api/apps' });
}

export function fetchGetAppDetail(id: Api.App.RecordId) {
  return request<Api.App.AppDefinition>({ url: `/api/apps/${id}` });
}

export function fetchCreateApp(data: Api.App.CreateAppPayload) {
  return request<Api.App.AppDefinition>({
    url: '/api/apps',
    method: 'post',
    data
  });
}

export function fetchUpdateApp(id: Api.App.RecordId, data: Api.App.UpdateAppPayload) {
  return request<Api.App.AppDefinition>({
    url: `/api/apps/${id}`,
    method: 'put',
    data
  });
}

export function fetchDeleteApp(id: Api.App.RecordId) {
  return request<unknown>({
    url: `/api/apps/${id}`,
    method: 'delete'
  });
}

export function fetchGetAppVersions(appId: Api.App.RecordId) {
  return request<Api.App.AppVersion[]>({ url: `/api/apps/${appId}/versions` });
}

export function fetchCreateAppVersion(appId: Api.App.RecordId, data: Api.App.CreateAppVersionPayload) {
  return request<Api.App.AppVersion>({
    url: `/api/apps/${appId}/versions`,
    method: 'post',
    data
  });
}

export function fetchUpdateAppVersion(id: Api.App.RecordId, data: Api.App.UpdateAppVersionPayload) {
  return request<Api.App.AppVersion>({
    url: `/api/app-versions/${id}`,
    method: 'put',
    data
  });
}

export function fetchDeleteAppVersion(id: Api.App.RecordId) {
  return request<unknown>({
    url: `/api/app-versions/${id}`,
    method: 'delete'
  });
}

export function fetchGetPackages() {
  return request<Api.App.AppPackage[]>({ url: '/api/packages' });
}

export function createPackageUploadFormData(payload: Api.App.PackageUploadPayload): Api.App.PackageUploadRequestBody {
  const formData = new FormData();

  formData.append('file', payload.file);

  if (payload.packageName) {
    formData.append('packageName', payload.packageName);
  }

  formData.append('packageType', payload.packageType);

  if (payload.storageType) {
    formData.append('storageType', payload.storageType);
  }

  return formData;
}

export function fetchUploadPackage(data: Api.App.PackageUploadRequestBody) {
  return request<Api.App.AppPackage>({
    url: '/api/packages/upload',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

export function fetchDeletePackage(id: Api.App.RecordId) {
  return request<unknown>({
    url: `/api/packages/${id}`,
    method: 'delete'
  });
}

export function fetchGetConfigTemplates() {
  return request<Api.App.ConfigTemplate[]>({ url: '/api/config-templates' });
}

export function fetchCreateConfigTemplate(data: Api.App.CreateConfigTemplatePayload) {
  return request<Api.App.ConfigTemplate>({
    url: '/api/config-templates',
    method: 'post',
    data
  });
}

export function fetchUpdateConfigTemplate(id: Api.App.RecordId, data: Api.App.UpdateConfigTemplatePayload) {
  return request<Api.App.ConfigTemplate>({
    url: `/api/config-templates/${id}`,
    method: 'put',
    data
  });
}

export function fetchDeleteConfigTemplate(id: Api.App.RecordId) {
  return request<unknown>({
    url: `/api/config-templates/${id}`,
    method: 'delete'
  });
}

export function fetchGetScriptTemplates() {
  return request<Api.App.ScriptTemplate[]>({ url: '/api/script-templates' });
}

export function fetchCreateScriptTemplate(data: Api.App.CreateScriptTemplatePayload) {
  return request<Api.App.ScriptTemplate>({
    url: '/api/script-templates',
    method: 'post',
    data
  });
}

export function fetchUpdateScriptTemplate(id: Api.App.RecordId, data: Api.App.UpdateScriptTemplatePayload) {
  return request<Api.App.ScriptTemplate>({
    url: `/api/script-templates/${id}`,
    method: 'put',
    data
  });
}

export function fetchDeleteScriptTemplate(id: Api.App.RecordId) {
  return request<unknown>({
    url: `/api/script-templates/${id}`,
    method: 'delete'
  });
}

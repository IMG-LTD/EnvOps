import { $t } from '@/locales';

export function getAppTypeOptions() {
  return ['JAVA', 'NGINX', 'SCRIPT', 'DOCKER'].map(value => ({ label: value, value }));
}

export function getDeployModeOptions() {
  return ['SYSTEMD', 'PROCESS', 'DOCKER'].map(value => ({ label: value, value }));
}

export function getPackageTypeOptions() {
  return ['JAR', 'TAR', 'RPM', 'SH'].map(value => ({ label: value, value }));
}

export function getStorageTypeOptions() {
  return ['LOCAL'].map(value => ({ label: value, value }));
}

export function getRenderEngineOptions() {
  return ['PLAINTEXT', 'JINJA2', 'FREEMARKER'].map(value => ({ label: value, value }));
}

export function getScriptTypeOptions() {
  return ['BASH', 'PYTHON'].map(value => ({ label: value, value }));
}

export function getStatusOptions() {
  return [
    {
      label: $t('common.yesOrNo.yes'),
      value: 1
    },
    {
      label: $t('common.yesOrNo.no'),
      value: 0
    }
  ];
}

export function formatText(value: string | number | null | undefined) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  return String(value);
}

export function formatStatus(status: Api.App.ResourceStatus | undefined) {
  if (status === 1 || status === '1') {
    return $t('common.yesOrNo.yes');
  }

  if (status === 0 || status === '0') {
    return $t('common.yesOrNo.no');
  }

  return '-';
}

export function formatFileSize(size: number | null | undefined) {
  if (size === null || size === undefined || Number.isNaN(size)) {
    return '-';
  }

  if (size < 1024) {
    return `${size} B`;
  }

  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = size / 1024;
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  return `${value.toFixed(value >= 10 ? 0 : 1)} ${units[unitIndex]}`;
}

export function getRecordIdKey(value: Api.App.RecordId | null | undefined) {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  return String(value);
}

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  fetchCheckAssetDatabase,
  fetchCheckCurrentPageAssetDatabases,
  fetchCheckQueriedAssetDatabases,
  fetchCheckSelectedAssetDatabases,
  fetchCreateAssetDatabase,
  fetchDeleteAssetDatabase,
  fetchGetAssetCredentials,
  fetchGetAssetDatabases,
  fetchGetAssetHosts,
  fetchUpdateAssetDatabase
} from '@/service/api';

defineOptions({
  name: 'AssetDatabasePage'
});

type DatabaseFilterModel = {
  keyword: string;
  environment: string | null;
  databaseType: string | null;
  lifecycleStatus: string | null;
  connectivityStatus: string | null;
};

type DatabaseFormModel = {
  databaseName: string;
  databaseType: string;
  environment: string;
  hostId: number | null;
  port: number | null;
  instanceName: string;
  credentialId: number | null;
  ownerName: string;
  lifecycleStatus: string;
  connectivityStatus: string;
  connectionUsername: string;
  connectionPassword: string;
  description: string;
  lastCheckedAt: string;
};

const { t } = useI18n();

const loading = ref(false);
const saving = ref(false);
const checking = ref(false);
const drawerVisible = ref(false);
const resultModalVisible = ref(false);
const editingDatabaseId = ref<number | null>(null);
const editingHasStoredConnectionCredential = ref(false);
const selectedDatabaseIds = ref<number[]>([]);
const connectivityReport = ref<Api.Asset.DatabaseConnectivityCheckResponse | null>(null);
const query = reactive<Api.Asset.DatabaseQuery>({
  current: 1,
  size: 10,
  keyword: '',
  environment: null,
  databaseType: null,
  lifecycleStatus: null,
  connectivityStatus: null
});
const filterModel = reactive<DatabaseFilterModel>({
  keyword: '',
  environment: null,
  databaseType: null,
  lifecycleStatus: null,
  connectivityStatus: null
});
const databasePage = ref<Api.Asset.DatabasePage>({
  current: 1,
  size: 10,
  total: 0,
  records: [],
  summary: {
    managedDatabases: 0,
    warningDatabases: 0,
    onlineDatabases: 0
  }
});
const hosts = ref<Api.Asset.HostRecord[]>([]);
const credentials = ref<Api.Asset.CredentialRecord[]>([]);
const formModel = reactive<DatabaseFormModel>(createDefaultFormModel());

const databaseTypeOptions = computed(() => [
  { label: t('page.envops.assetDatabase.types.mysql'), value: 'mysql' },
  { label: t('page.envops.assetDatabase.types.postgresql'), value: 'postgresql' },
  { label: t('page.envops.assetDatabase.types.oracle'), value: 'oracle' },
  { label: t('page.envops.assetDatabase.types.sqlserver'), value: 'sqlserver' },
  { label: t('page.envops.assetDatabase.types.mongodb'), value: 'mongodb' },
  { label: t('page.envops.assetDatabase.types.redis'), value: 'redis' }
]);

const environmentOptions = computed(() => [
  { label: t('page.envops.common.environment.production'), value: 'production' },
  { label: t('page.envops.common.environment.staging'), value: 'staging' },
  { label: t('page.envops.common.environment.sandbox'), value: 'sandbox' }
]);

const lifecycleStatusOptions = computed(() => [
  { label: t('page.envops.common.status.managed'), value: 'managed' },
  { label: t('page.envops.common.status.disabled'), value: 'disabled' }
]);

const connectivityStatusOptions = computed(() => [
  { label: t('page.envops.assetDatabase.connectivity.unknown'), value: 'unknown' },
  { label: t('page.envops.common.status.online'), value: 'online' },
  { label: t('page.envops.common.status.warning'), value: 'warning' },
  { label: t('page.envops.common.status.offline'), value: 'offline' }
]);

const hostOptions = computed(() =>
  hosts.value.map(item => ({
    label: `${item.hostName} (${item.ipAddress})`,
    value: item.id
  }))
);

const credentialOptions = computed(() =>
  credentials.value.map(item => ({
    label: item.name,
    value: item.id
  }))
);

const summary = computed(() => [
  {
    key: 'managedDatabases',
    label: t('page.envops.assetDatabase.summary.managedDatabases.label'),
    value: String(databasePage.value.summary.managedDatabases),
    desc: t('page.envops.assetDatabase.summary.managedDatabases.desc'),
    active: filterModel.lifecycleStatus === 'managed',
    onClick: () => handleApplySummaryFilter('managedDatabases')
  },
  {
    key: 'warningDatabases',
    label: t('page.envops.assetDatabase.summary.warningDatabases.label'),
    value: String(databasePage.value.summary.warningDatabases),
    desc: t('page.envops.assetDatabase.summary.warningDatabases.desc'),
    active: filterModel.connectivityStatus === 'warning',
    onClick: () => handleApplySummaryFilter('warningDatabases')
  },
  {
    key: 'onlineDatabases',
    label: t('page.envops.assetDatabase.summary.onlineDatabases.label'),
    value: String(databasePage.value.summary.onlineDatabases),
    desc: t('page.envops.assetDatabase.summary.onlineDatabases.desc'),
    active: filterModel.connectivityStatus === 'online',
    onClick: () => handleApplySummaryFilter('onlineDatabases')
  }
]);

const tableRows = computed(() =>
  databasePage.value.records.map(item => ({
    ...item,
    databaseTypeLabel: getDatabaseTypeLabel(item.databaseType),
    environmentLabel: getEnvironmentLabel(item.environment),
    lifecycleStatusLabel: getLifecycleStatusLabel(item.lifecycleStatus),
    connectivityStatusLabel: getConnectivityStatusLabel(item.connectivityStatus),
    connectivityStatusType: getConnectivityStatusType(item.connectivityStatus),
    lastCheckedAtLabel: formatDateTime(item.lastCheckedAt)
  }))
);

const drawerTitle = computed(() =>
  t(
    editingDatabaseId.value === null
      ? 'page.envops.assetDatabase.form.titleCreate'
      : 'page.envops.assetDatabase.form.titleEdit'
  )
);

const showPagination = computed(() => databasePage.value.total > 0);
const allCurrentPageSelected = computed(
  () => tableRows.value.length > 0 && tableRows.value.every(item => selectedDatabaseIds.value.includes(item.id))
);
const reportSummary = computed(
  () =>
    connectivityReport.value?.summary || {
      total: 0,
      success: 0,
      failed: 0,
      skipped: 0
    }
);
const reportResults = computed(() => connectivityReport.value?.results || []);

function createDefaultFormModel(): DatabaseFormModel {
  return {
    databaseName: '',
    databaseType: 'mysql',
    environment: 'sandbox',
    hostId: null,
    port: 3306,
    instanceName: '',
    credentialId: null,
    ownerName: '',
    lifecycleStatus: 'managed',
    connectivityStatus: 'unknown',
    connectionUsername: '',
    connectionPassword: '',
    description: '',
    lastCheckedAt: ''
  };
}

function resetForm() {
  Object.assign(formModel, createDefaultFormModel());
}

function fillForm(record: Api.Asset.DatabaseRecord) {
  Object.assign(formModel, {
    databaseName: record.databaseName,
    databaseType: record.databaseType,
    environment: record.environment,
    hostId: record.hostId,
    port: record.port,
    instanceName: record.instanceName || '',
    credentialId: record.credentialId,
    ownerName: record.ownerName,
    lifecycleStatus: record.lifecycleStatus,
    connectivityStatus: record.connectivityStatus,
    connectionUsername: record.connectionUsername || '',
    connectionPassword: '',
    description: record.description || '',
    lastCheckedAt: record.lastCheckedAt || ''
  } satisfies DatabaseFormModel);
}

function buildPayload(): Api.Asset.CreateDatabaseParams {
  const connectionUsername = formModel.connectionUsername.trim() || undefined;
  const connectionPassword = formModel.connectionPassword.trim();

  return {
    databaseName: formModel.databaseName.trim(),
    databaseType: formModel.databaseType.trim(),
    environment: formModel.environment.trim(),
    hostId: formModel.hostId,
    port: formModel.port,
    instanceName: formModel.instanceName.trim() || undefined,
    credentialId: formModel.credentialId,
    ownerName: formModel.ownerName.trim(),
    lifecycleStatus: formModel.lifecycleStatus.trim(),
    connectivityStatus: formModel.connectivityStatus.trim(),
    connectionUsername,
    connectionPassword:
      editingDatabaseId.value !== null && connectionUsername && !connectionPassword
        ? ''
        : connectionPassword || undefined,
    description: formModel.description.trim() || undefined,
    lastCheckedAt: formModel.lastCheckedAt.trim() || undefined
  };
}

function applyFilters() {
  query.keyword = filterModel.keyword.trim();
  query.environment = filterModel.environment;
  query.databaseType = filterModel.databaseType;
  query.lifecycleStatus = filterModel.lifecycleStatus;
  query.connectivityStatus = filterModel.connectivityStatus;
  query.current = 1;
}

async function handleApplySummaryFilter(summaryKey: 'managedDatabases' | 'warningDatabases' | 'onlineDatabases') {
  if (summaryKey === 'managedDatabases') {
    filterModel.lifecycleStatus = filterModel.lifecycleStatus === 'managed' ? null : 'managed';
  }

  if (summaryKey === 'warningDatabases') {
    filterModel.connectivityStatus = filterModel.connectivityStatus === 'warning' ? null : 'warning';
  }

  if (summaryKey === 'onlineDatabases') {
    filterModel.connectivityStatus = filterModel.connectivityStatus === 'online' ? null : 'online';
  }

  applyFilters();
  await loadPageData();
}

async function handleSearch() {
  applyFilters();
  await loadPageData();
}

async function handleResetFilters() {
  filterModel.keyword = '';
  filterModel.environment = null;
  filterModel.databaseType = null;
  filterModel.lifecycleStatus = null;
  filterModel.connectivityStatus = null;
  applyFilters();
  await loadPageData();
}

async function handlePageChange(page: number) {
  query.current = page;
  await loadPageData();
}

async function handlePageSizeChange(pageSize: number) {
  query.size = pageSize;
  query.current = 1;
  await loadPageData();
}

async function loadPageData() {
  loading.value = true;

  try {
    const [databaseResponse, hostResponse, credentialResponse] = await Promise.all([
      fetchGetAssetDatabases({ ...query }),
      fetchGetAssetHosts({ current: 1, size: 100 }),
      fetchGetAssetCredentials()
    ]);

    if (!databaseResponse.error) {
      databasePage.value = databaseResponse.data;
      selectedDatabaseIds.value = selectedDatabaseIds.value.filter(id =>
        databaseResponse.data.records.some(item => item.id === id)
      );
    }

    if (!hostResponse.error) {
      hosts.value = hostResponse.data.records;
    }

    if (!credentialResponse.error) {
      credentials.value = credentialResponse.data;
    }
  } finally {
    loading.value = false;
  }
}

function handleOpenCreateDrawer() {
  editingDatabaseId.value = null;
  editingHasStoredConnectionCredential.value = false;
  resetForm();
  drawerVisible.value = true;
}

function handleOpenEditDrawer(record: Api.Asset.DatabaseRecord) {
  editingDatabaseId.value = record.id;
  editingHasStoredConnectionCredential.value = Boolean(record.connectionUsername);
  fillForm(record);
  drawerVisible.value = true;
}

function handleDrawerVisibleChange(show: boolean) {
  drawerVisible.value = show;

  if (!show) {
    editingDatabaseId.value = null;
    editingHasStoredConnectionCredential.value = false;
    resetForm();
  }
}

function handleToggleAllSelection(checked: boolean) {
  selectedDatabaseIds.value = checked ? tableRows.value.map(item => item.id) : [];
}

function handleToggleRowSelection(id: number, checked: boolean) {
  selectedDatabaseIds.value = checked
    ? Array.from(new Set([...selectedDatabaseIds.value, id]))
    : selectedDatabaseIds.value.filter(itemId => itemId !== id);
}

function validateForm() {
  if (
    !formModel.databaseName.trim() ||
    !formModel.databaseType.trim() ||
    !formModel.environment.trim() ||
    formModel.hostId === null ||
    formModel.port === null ||
    formModel.port < 1 ||
    !formModel.ownerName.trim() ||
    !formModel.lifecycleStatus.trim() ||
    !formModel.connectivityStatus.trim()
  ) {
    window.$message?.warning(t('page.envops.assetDatabase.messages.fillRequired'));
    return false;
  }

  const hasConnectionUsername = Boolean(formModel.connectionUsername.trim());
  const hasConnectionPassword = Boolean(formModel.connectionPassword.trim());

  if (editingDatabaseId.value === null && hasConnectionUsername !== hasConnectionPassword) {
    window.$message?.warning(t('page.envops.assetDatabase.messages.fillConnectionPair'));
    return false;
  }

  if (editingDatabaseId.value !== null && !hasConnectionUsername && hasConnectionPassword) {
    window.$message?.warning(t('page.envops.assetDatabase.messages.fillConnectionUsername'));
    return false;
  }

  if (
    editingDatabaseId.value !== null &&
    hasConnectionUsername &&
    !hasConnectionPassword &&
    !editingHasStoredConnectionCredential.value
  ) {
    window.$message?.warning(t('page.envops.assetDatabase.messages.fillConnectionPair'));
    return false;
  }

  return true;
}

async function handleSubmit() {
  if (!validateForm()) {
    return;
  }

  saving.value = true;

  try {
    const payload = buildPayload();
    const response =
      editingDatabaseId.value === null
        ? await fetchCreateAssetDatabase(payload)
        : await fetchUpdateAssetDatabase(editingDatabaseId.value, payload);

    if (!response.error) {
      window.$message?.success(
        t(
          editingDatabaseId.value === null
            ? 'page.envops.assetDatabase.messages.createSuccess'
            : 'page.envops.assetDatabase.messages.updateSuccess'
        )
      );
      handleDrawerVisibleChange(false);
      query.current = 1;
      await loadPageData();
    }
  } finally {
    saving.value = false;
  }
}

async function handleDelete(record: Api.Asset.DatabaseRecord) {
  window.$dialog?.warning({
    title: t('common.warning'),
    content: `${t('common.confirmDelete')} ${record.databaseName}?`,
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    async onPositiveClick() {
      const { error } = await fetchDeleteAssetDatabase(record.id);

      if (!error) {
        window.$message?.success(t('common.deleteSuccess'));

        if (databasePage.value.records.length === 1 && query.current > 1) {
          query.current -= 1;
        }

        await loadPageData();
      }
    }
  });
}

async function runConnectivityCheck(requestPromise: ReturnType<typeof fetchCheckAssetDatabase>) {
  checking.value = true;

  try {
    const response = await requestPromise;

    if (!response.error) {
      connectivityReport.value = response.data;
      resultModalVisible.value = response.data.results.length > 1;
      window.$message?.success(t('page.envops.assetDatabase.messages.checkFinished'));
      await loadPageData();
    }
  } finally {
    checking.value = false;
  }
}

async function handleCheckDatabase(record: Api.Asset.DatabaseRecord) {
  await runConnectivityCheck(fetchCheckAssetDatabase(record.id));
}

async function handleCheckSelected() {
  await runConnectivityCheck(fetchCheckSelectedAssetDatabases(selectedDatabaseIds.value));
}

async function handleCheckCurrentPage() {
  await runConnectivityCheck(fetchCheckCurrentPageAssetDatabases(tableRows.value.map(item => item.id)));
}

async function handleCheckFiltered() {
  await runConnectivityCheck(fetchCheckQueriedAssetDatabases({ ...query }));
}

function getDatabaseTypeLabel(type: string) {
  const labelMap: Record<string, string> = {
    mysql: t('page.envops.assetDatabase.types.mysql'),
    postgresql: t('page.envops.assetDatabase.types.postgresql'),
    oracle: t('page.envops.assetDatabase.types.oracle'),
    sqlserver: t('page.envops.assetDatabase.types.sqlserver'),
    mongodb: t('page.envops.assetDatabase.types.mongodb'),
    redis: t('page.envops.assetDatabase.types.redis')
  };

  return labelMap[type] || type;
}

function getEnvironmentLabel(environment: string) {
  const labelMap: Record<string, string> = {
    production: t('page.envops.common.environment.production'),
    staging: t('page.envops.common.environment.staging'),
    sandbox: t('page.envops.common.environment.sandbox')
  };

  return labelMap[environment] || environment;
}

function getLifecycleStatusLabel(status: string) {
  const labelMap: Record<string, string> = {
    managed: t('page.envops.common.status.managed'),
    disabled: t('page.envops.common.status.disabled')
  };

  return labelMap[status] || status;
}

function getConnectivityStatusLabel(status: string) {
  const labelMap: Record<string, string> = {
    unknown: t('page.envops.assetDatabase.connectivity.unknown'),
    online: t('page.envops.common.status.online'),
    warning: t('page.envops.common.status.warning'),
    offline: t('page.envops.common.status.offline')
  };

  return labelMap[status] || status;
}

function getConnectivityStatusType(status: string): 'success' | 'warning' | 'error' | 'default' {
  const typeMap: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
    online: 'success',
    warning: 'warning',
    offline: 'error',
    unknown: 'default'
  };

  return typeMap[status] || 'default';
}

function getResultStatusType(status: string): 'success' | 'warning' | 'error' | 'default' {
  const typeMap: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
    success: 'success',
    failed: 'error',
    skipped: 'warning'
  };

  return typeMap[status] || 'default';
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-';
  }

  return value.replace('T', ' ').slice(0, 19);
}

onMounted(() => {
  void loadPageData();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.assetDatabase.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.assetDatabase.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="info">{{ t('page.envops.assetDatabase.tags.registryReady') }}</NTag>
          <NTag type="success">{{ t('page.envops.assetDatabase.tags.connectivityCheckReady') }}</NTag>
          <NTag type="warning">{{ t('page.envops.assetDatabase.tags.warningManual') }}</NTag>
          <NButton secondary :loading="loading" @click="loadPageData">
            {{ t('common.refresh') }}
          </NButton>
          <NButton type="primary" @click="handleOpenCreateDrawer">
            {{ t('page.envops.assetDatabase.actions.create') }}
          </NButton>
        </NSpace>
      </div>
    </NCard>

    <NGrid cols="1 s:3" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in summary" :key="item.key">
        <NCard
          :bordered="false"
          class="card-wrapper cursor-pointer"
          :class="item.active ? 'border border-primary' : ''"
          :data-summary-key="item.key"
          @click="item.onClick"
        >
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NCard :title="t('page.envops.assetDatabase.table.title')" :bordered="false" class="card-wrapper">
      <NSpace vertical :size="16">
        <NGrid cols="1 m:5" responsive="screen" :x-gap="16" :y-gap="12">
          <NGi>
            <NInput
              v-model:value="filterModel.keyword"
              :placeholder="t('page.envops.assetDatabase.filters.keywordPlaceholder')"
            />
          </NGi>
          <NGi>
            <NSelect
              v-model:value="filterModel.environment"
              clearable
              :options="environmentOptions"
              :placeholder="t('page.envops.assetDatabase.filters.environmentPlaceholder')"
            />
          </NGi>
          <NGi>
            <NSelect
              v-model:value="filterModel.databaseType"
              clearable
              :options="databaseTypeOptions"
              :placeholder="t('page.envops.assetDatabase.filters.databaseTypePlaceholder')"
            />
          </NGi>
          <NGi>
            <NSelect
              v-model:value="filterModel.lifecycleStatus"
              clearable
              :options="lifecycleStatusOptions"
              :placeholder="t('page.envops.assetDatabase.filters.lifecycleStatusPlaceholder')"
            />
          </NGi>
          <NGi>
            <NSelect
              v-model:value="filterModel.connectivityStatus"
              clearable
              :options="connectivityStatusOptions"
              :placeholder="t('page.envops.assetDatabase.filters.connectivityStatusPlaceholder')"
            />
          </NGi>
        </NGrid>
        <NSpace>
          <NButton type="primary" @click="handleSearch">{{ t('common.search') }}</NButton>
          <NButton @click="handleResetFilters">{{ t('common.reset') }}</NButton>
          <NButton :disabled="!selectedDatabaseIds.length" :loading="checking" @click="handleCheckSelected">
            {{ t('page.envops.assetDatabase.actions.checkSelected') }}
          </NButton>
          <NButton :disabled="!tableRows.length" :loading="checking" @click="handleCheckCurrentPage">
            {{ t('page.envops.assetDatabase.actions.checkCurrentPage') }}
          </NButton>
          <NButton :loading="checking" @click="handleCheckFiltered">
            {{ t('page.envops.assetDatabase.actions.checkAllFiltered') }}
          </NButton>
        </NSpace>
      </NSpace>
      <NSpin :show="loading">
        <NTable v-if="tableRows.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>
                <NCheckbox
                  :checked="allCurrentPageSelected"
                  @update:checked="checked => handleToggleAllSelection(Boolean(checked))"
                />
              </th>
              <th>{{ t('page.envops.assetDatabase.table.database') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.type') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.environment') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.host') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.port') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.owner') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.lifecycleStatus') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.connectivityStatus') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.lastCheckedAt') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.operation') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tableRows" :key="item.id">
              <td>
                <NCheckbox
                  :checked="selectedDatabaseIds.includes(item.id)"
                  @update:checked="checked => handleToggleRowSelection(item.id, Boolean(checked))"
                />
              </td>
              <td>
                <div>{{ item.databaseName }}</div>
                <div class="text-12px text-#999">{{ item.instanceName || '-' }}</div>
              </td>
              <td>{{ item.databaseTypeLabel }}</td>
              <td>{{ item.environmentLabel }}</td>
              <td>{{ item.hostName || '-' }}</td>
              <td>{{ item.port }}</td>
              <td>{{ item.ownerName }}</td>
              <td>{{ item.lifecycleStatusLabel }}</td>
              <td>
                <NTag :type="item.connectivityStatusType" size="small">{{ item.connectivityStatusLabel }}</NTag>
              </td>
              <td>{{ item.lastCheckedAtLabel }}</td>
              <td>
                <NSpace size="small">
                  <NButton
                    text
                    type="primary"
                    data-action="check-database"
                    :loading="checking"
                    @click="handleCheckDatabase(item)"
                  >
                    {{ t('page.envops.assetDatabase.actions.check') }}
                  </NButton>
                  <NButton text type="primary" data-action="edit-database" @click="handleOpenEditDrawer(item)">
                    {{ t('page.envops.assetDatabase.actions.edit') }}
                  </NButton>
                  <NButton text type="error" @click="handleDelete(item)">
                    {{ t('common.delete') }}
                  </NButton>
                </NSpace>
              </td>
            </tr>
          </tbody>
        </NTable>
        <NEmpty v-else class="py-24px" :description="t('common.noData')" />

        <div v-if="showPagination" class="mt-16px flex justify-end">
          <NPagination
            :page="query.current"
            :page-size="query.size"
            :item-count="databasePage.total"
            :page-sizes="[10, 20, 50]"
            show-size-picker
            @update:page="handlePageChange"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </NSpin>
    </NCard>

    <NDrawer :show="drawerVisible" :width="520" @update:show="handleDrawerVisibleChange">
      <NDrawerContent :title="drawerTitle">
        <NForm label-placement="top">
          <NFormItem :label="t('page.envops.assetDatabase.form.databaseName')">
            <NInput
              v-model:value="formModel.databaseName"
              :placeholder="t('page.envops.assetDatabase.form.placeholders.databaseName')"
            />
          </NFormItem>
          <NGrid cols="1 s:2" responsive="screen" :x-gap="16">
            <NGi>
              <NFormItem :label="t('page.envops.assetDatabase.form.databaseType')">
                <NSelect v-model:value="formModel.databaseType" :options="databaseTypeOptions" />
              </NFormItem>
            </NGi>
            <NGi>
              <NFormItem :label="t('page.envops.assetDatabase.form.environment')">
                <NSelect v-model:value="formModel.environment" :options="environmentOptions" />
              </NFormItem>
            </NGi>
          </NGrid>
          <NFormItem :label="t('page.envops.assetDatabase.form.host')">
            <NSelect
              v-model:value="formModel.hostId"
              :options="hostOptions"
              :placeholder="t('page.envops.assetDatabase.form.placeholders.host')"
            />
          </NFormItem>
          <NGrid cols="1 s:2" responsive="screen" :x-gap="16">
            <NGi>
              <NFormItem :label="t('page.envops.assetDatabase.form.port')">
                <NInputNumber
                  v-model:value="formModel.port"
                  :min="1"
                  :placeholder="t('page.envops.assetDatabase.form.placeholders.port')"
                  class="w-full"
                />
              </NFormItem>
            </NGi>
            <NGi>
              <NFormItem :label="t('page.envops.assetDatabase.form.instanceName')">
                <NInput
                  v-model:value="formModel.instanceName"
                  :placeholder="t('page.envops.assetDatabase.form.placeholders.instanceName')"
                />
              </NFormItem>
            </NGi>
          </NGrid>
          <NFormItem :label="t('page.envops.assetDatabase.form.credential')">
            <NSelect
              v-model:value="formModel.credentialId"
              clearable
              :options="credentialOptions"
              :placeholder="t('page.envops.assetDatabase.form.placeholders.credential')"
            />
          </NFormItem>
          <NFormItem :label="t('page.envops.assetDatabase.form.ownerName')">
            <NInput
              v-model:value="formModel.ownerName"
              :placeholder="t('page.envops.assetDatabase.form.placeholders.ownerName')"
            />
          </NFormItem>
          <NGrid cols="1 s:2" responsive="screen" :x-gap="16">
            <NGi>
              <NFormItem :label="t('page.envops.assetDatabase.form.lifecycleStatus')">
                <NSelect v-model:value="formModel.lifecycleStatus" :options="lifecycleStatusOptions" />
              </NFormItem>
            </NGi>
            <NGi>
              <NFormItem :label="t('page.envops.assetDatabase.form.connectivityStatus')">
                <NSelect v-model:value="formModel.connectivityStatus" :options="connectivityStatusOptions" />
              </NFormItem>
            </NGi>
          </NGrid>
          <NGrid cols="1 s:2" responsive="screen" :x-gap="16">
            <NGi>
              <NFormItem :label="t('page.envops.assetDatabase.form.connectionUsername')">
                <NInput
                  v-model:value="formModel.connectionUsername"
                  data-form-field="connectionUsername"
                  :placeholder="t('page.envops.assetDatabase.form.placeholders.connectionUsername')"
                />
              </NFormItem>
            </NGi>
            <NGi>
              <NFormItem :label="t('page.envops.assetDatabase.form.connectionPassword')">
                <NInput
                  v-model:value="formModel.connectionPassword"
                  type="password"
                  data-form-field="connectionPassword"
                  :placeholder="t('page.envops.assetDatabase.form.placeholders.connectionPassword')"
                />
              </NFormItem>
            </NGi>
          </NGrid>
          <NFormItem :label="t('page.envops.assetDatabase.form.lastCheckedAt')">
            <NInput
              v-model:value="formModel.lastCheckedAt"
              :placeholder="t('page.envops.assetDatabase.form.placeholders.lastCheckedAt')"
            />
          </NFormItem>
          <NFormItem :label="t('page.envops.assetDatabase.form.description')">
            <NInput
              v-model:value="formModel.description"
              type="textarea"
              :placeholder="t('page.envops.assetDatabase.form.placeholders.description')"
            />
          </NFormItem>
          <NSpace justify="end">
            <NButton @click="handleDrawerVisibleChange(false)">{{ t('common.cancel') }}</NButton>
            <NButton type="primary" :loading="saving" @click="handleSubmit">
              {{ t('page.envops.assetDatabase.actions.save') }}
            </NButton>
          </NSpace>
        </NForm>
      </NDrawerContent>
    </NDrawer>

    <NModal
      :show="resultModalVisible"
      preset="card"
      :title="t('page.envops.assetDatabase.result.title')"
      style="width: 820px"
      @update:show="value => (resultModalVisible = value)"
    >
      <NSpace vertical :size="16">
        <NGrid cols="4" :x-gap="12">
          <NGi>
            <NStatistic :label="t('page.envops.assetDatabase.result.total')" :value="reportSummary.total" />
          </NGi>
          <NGi>
            <NStatistic :label="t('page.envops.assetDatabase.result.success')" :value="reportSummary.success" />
          </NGi>
          <NGi>
            <NStatistic :label="t('page.envops.assetDatabase.result.failed')" :value="reportSummary.failed" />
          </NGi>
          <NGi>
            <NStatistic :label="t('page.envops.assetDatabase.result.skipped')" :value="reportSummary.skipped" />
          </NGi>
        </NGrid>
        <NTable :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.assetDatabase.table.database') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.type') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.environment') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.connectivityStatus') }}</th>
              <th>{{ t('page.envops.assetDatabase.result.message') }}</th>
              <th>{{ t('page.envops.assetDatabase.table.lastCheckedAt') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in reportResults" :key="`${item.databaseId}-${item.checkedAt || 'none'}`">
              <td>{{ item.databaseName }}</td>
              <td>{{ getDatabaseTypeLabel(item.databaseType) }}</td>
              <td>{{ getEnvironmentLabel(item.environment) }}</td>
              <td>
                <NTag :type="getResultStatusType(item.status)">{{ item.status }}</NTag>
              </td>
              <td>{{ item.message }}</td>
              <td>{{ formatDateTime(item.checkedAt) }}</td>
            </tr>
          </tbody>
        </NTable>
        <NSpace justify="end">
          <NButton @click="resultModalVisible = false">
            {{ t('page.envops.assetDatabase.actions.closeResult') }}
          </NButton>
        </NSpace>
      </NSpace>
    </NModal>
  </NSpace>
</template>

<style scoped></style>

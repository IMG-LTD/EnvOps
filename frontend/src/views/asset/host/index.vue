<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouterPush } from '@/hooks/common/router';
import { fetchCreateAssetHost, fetchGetAssetHosts } from '@/service/api';

defineOptions({
  name: 'AssetHostPage'
});

const { t } = useI18n();
const { routerPushByKey } = useRouterPush();

const loading = ref(false);
const creating = ref(false);
const query = reactive<Api.Asset.HostQuery>({
  current: 1,
  size: 10
});
const hostPage = ref<Api.Asset.HostPage>({
  current: 1,
  size: 10,
  total: 0,
  records: [],
  summary: {
    managedHosts: 0,
    onlineHosts: 0,
    warningHosts: 0
  }
});
const formModel = reactive<Api.Asset.CreateHostParams>({
  hostName: '',
  ipAddress: '',
  environment: 'sandbox',
  clusterName: '',
  ownerName: '',
  status: 'online',
  lastHeartbeat: ''
});

const environmentOptions = computed(() => [
  {
    label: t('page.envops.common.environment.production'),
    value: 'production'
  },
  {
    label: t('page.envops.common.environment.staging'),
    value: 'staging'
  },
  {
    label: t('page.envops.common.environment.sandbox'),
    value: 'sandbox'
  }
]);

const statusOptions = computed(() => [
  {
    label: t('page.envops.common.status.online'),
    value: 'online'
  },
  {
    label: t('page.envops.common.status.warning'),
    value: 'warning'
  },
  {
    label: t('page.envops.common.status.offline'),
    value: 'offline'
  }
]);

const summary = computed(() => [
  {
    key: 'managedHosts',
    label: t('page.envops.assetHost.summary.managedHosts.label'),
    value: String(hostPage.value.summary.managedHosts),
    desc: t('page.envops.assetHost.summary.managedHosts.desc')
  },
  {
    key: 'online',
    label: t('page.envops.assetHost.summary.online.label'),
    value: String(hostPage.value.summary.onlineHosts),
    desc: t('page.envops.assetHost.summary.online.desc')
  },
  {
    key: 'pendingMaintenance',
    label: t('page.envops.assetHost.summary.pendingMaintenance.label'),
    value: String(hostPage.value.summary.warningHosts),
    desc: t('page.envops.assetHost.summary.pendingMaintenance.desc')
  }
]);

const tableRows = computed(() =>
  hostPage.value.records.map(item => ({
    ...item,
    environmentLabel: getEnvironmentLabel(item.environment),
    statusLabel: getStatusLabel(item.status),
    statusType: getStatusType(item.status),
    lastHeartbeatLabel: formatDateTime(item.lastHeartbeat),
    latestMonitorFactAtLabel: formatDateTime(item.latestMonitorFactAt),
    canViewMetrics: Boolean(item.hasMonitorFacts)
  }))
);

async function loadHosts() {
  loading.value = true;

  try {
    const { data, error } = await fetchGetAssetHosts({ ...query });

    if (!error) {
      hostPage.value = data;
    }
  } finally {
    loading.value = false;
  }
}

async function handleCreateHost() {
  if (
    !formModel.hostName.trim() ||
    !formModel.ipAddress.trim() ||
    !formModel.environment.trim() ||
    !formModel.clusterName.trim() ||
    !formModel.ownerName.trim() ||
    !formModel.status.trim()
  ) {
    window.$message?.warning(t('page.envops.assetHost.messages.fillRequired'));
    return;
  }

  creating.value = true;

  try {
    const payload: Api.Asset.CreateHostParams = {
      hostName: formModel.hostName.trim(),
      ipAddress: formModel.ipAddress.trim(),
      environment: formModel.environment.trim(),
      clusterName: formModel.clusterName.trim(),
      ownerName: formModel.ownerName.trim(),
      status: formModel.status.trim(),
      lastHeartbeat: formModel.lastHeartbeat?.trim() || undefined
    };

    const { error } = await fetchCreateAssetHost(payload);

    if (!error) {
      window.$message?.success(t('page.envops.assetHost.messages.createSuccess'));
      resetForm();
      query.current = 1;
      await loadHosts();
    }
  } finally {
    creating.value = false;
  }
}

function resetForm() {
  formModel.hostName = '';
  formModel.ipAddress = '';
  formModel.environment = 'sandbox';
  formModel.clusterName = '';
  formModel.ownerName = '';
  formModel.status = 'online';
  formModel.lastHeartbeat = '';
}

function getEnvironmentLabel(environment: string) {
  const labelMap: Record<string, string> = {
    production: t('page.envops.common.environment.production'),
    staging: t('page.envops.common.environment.staging'),
    sandbox: t('page.envops.common.environment.sandbox')
  };

  return labelMap[environment] || environment;
}

function getStatusLabel(status: string) {
  const labelMap: Record<string, string> = {
    online: t('page.envops.common.status.online'),
    warning: t('page.envops.common.status.warning'),
    offline: t('page.envops.common.status.offline')
  };

  return labelMap[status] || status;
}

function getStatusType(status: string): 'success' | 'warning' | 'error' | 'default' {
  const typeMap: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
    online: 'success',
    warning: 'warning',
    offline: 'error'
  };

  return typeMap[status] || 'default';
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-';
  }

  return value.replace('T', ' ').slice(0, 19);
}

async function handleViewMetrics(hostId: number) {
  await routerPushByKey('monitor_metric', {
    query: {
      hostId: String(hostId)
    }
  });
}

onMounted(() => {
  void loadHosts();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.assetHost.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.assetHost.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="success">{{ t('page.envops.assetHost.tags.cmdbSynchronized') }}</NTag>
          <NTag type="warning">{{ t('page.envops.assetHost.tags.maintenanceWindow') }}</NTag>
          <NButton secondary :loading="loading" @click="loadHosts">
            {{ t('common.refresh') }}
          </NButton>
        </NSpace>
      </div>
    </NCard>

    <NGrid cols="1 s:3" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in summary" :key="item.key">
        <NCard :bordered="false" class="card-wrapper">
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NGrid cols="1 xl:2" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi>
        <NCard :bordered="false" class="card-wrapper" :title="t('page.envops.assetHost.form.title')">
          <NForm label-placement="top">
            <NFormItem :label="t('page.envops.assetHost.form.hostName')">
              <NInput
                v-model:value="formModel.hostName"
                :placeholder="t('page.envops.assetHost.form.placeholders.hostName')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetHost.form.ipAddress')">
              <NInput
                v-model:value="formModel.ipAddress"
                :placeholder="t('page.envops.assetHost.form.placeholders.ipAddress')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetHost.form.environment')">
              <NSelect v-model:value="formModel.environment" :options="environmentOptions" />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetHost.form.clusterName')">
              <NInput
                v-model:value="formModel.clusterName"
                :placeholder="t('page.envops.assetHost.form.placeholders.clusterName')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetHost.form.ownerName')">
              <NInput
                v-model:value="formModel.ownerName"
                :placeholder="t('page.envops.assetHost.form.placeholders.ownerName')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetHost.form.status')">
              <NSelect v-model:value="formModel.status" :options="statusOptions" />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetHost.form.lastHeartbeat')">
              <NInput
                v-model:value="formModel.lastHeartbeat"
                :placeholder="t('page.envops.assetHost.form.placeholders.lastHeartbeat')"
              />
            </NFormItem>
            <NSpace>
              <NButton type="primary" :loading="creating" @click="handleCreateHost">
                {{ t('page.envops.assetHost.form.actions.create') }}
              </NButton>
              <NButton @click="resetForm">{{ t('common.reset') }}</NButton>
            </NSpace>
          </NForm>
        </NCard>
      </NGi>

      <NGi>
        <NCard :title="t('page.envops.assetHost.table.title')" :bordered="false" class="card-wrapper">
          <NSpin :show="loading">
            <NTable v-if="tableRows.length" :bordered="false" :single-line="false">
              <thead>
                <tr>
                  <th>{{ t('page.envops.assetHost.table.host') }}</th>
                  <th>{{ t('page.envops.assetHost.table.ip') }}</th>
                  <th>{{ t('page.envops.assetHost.table.environment') }}</th>
                  <th>{{ t('page.envops.assetHost.table.cluster') }}</th>
                  <th>{{ t('page.envops.assetHost.table.owner') }}</th>
                  <th>{{ t('page.envops.assetHost.table.status') }}</th>
                  <th>{{ t('page.envops.assetHost.table.lastHeartbeat') }}</th>
                  <th>{{ t('page.envops.assetHost.table.latestMetric') }}</th>
                  <th>{{ t('page.envops.assetHost.table.operation') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in tableRows" :key="item.id">
                  <td>{{ item.hostName }}</td>
                  <td>{{ item.ipAddress }}</td>
                  <td>{{ item.environmentLabel }}</td>
                  <td>{{ item.clusterName }}</td>
                  <td>{{ item.ownerName }}</td>
                  <td>
                    <NTag :type="item.statusType" size="small">{{ item.statusLabel }}</NTag>
                  </td>
                  <td>{{ item.lastHeartbeatLabel }}</td>
                  <td>
                    <NTag :type="item.canViewMetrics ? 'success' : 'default'" size="small">
                      {{ item.canViewMetrics ? item.latestMonitorFactAtLabel : t('common.noData') }}
                    </NTag>
                  </td>
                  <td>
                    <NButton text type="primary" :disabled="!item.canViewMetrics" @click="handleViewMetrics(item.id)">
                      {{ t('page.envops.assetHost.table.viewMetrics') }}
                    </NButton>
                  </td>
                </tr>
              </tbody>
            </NTable>
            <NEmpty v-else class="py-24px" :description="t('common.noData')" />
          </NSpin>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>

<style scoped></style>

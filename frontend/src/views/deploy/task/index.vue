<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  fetchPostCancelDeployTask,
  fetchPostExecuteDeployTask,
  fetchGetDeployTaskHosts,
  fetchGetDeployTaskLogs,
  fetchGetDeployTasks,
  fetchPostRetryDeployTask,
  fetchPostRollbackDeployTask
} from '@/service/api';

defineOptions({
  name: 'DeployTaskPage'
});

type DeployTaskStatusKey = 'success' | 'failed' | 'running' | 'pending' | 'pendingApproval' | 'cancelled' | 'rejected';
type DeployTaskTagType = 'success' | 'error' | 'info' | 'default' | 'warning';
type DeployTaskActionKey = 'execute' | 'retry' | 'rollback' | 'cancel';

const { t } = useI18n();

const loading = ref(false);
const detailLoading = ref(false);
const actionLoadingTaskIds = ref<number[]>([]);
const actionLoadingActions = ref<Partial<Record<number, DeployTaskActionKey>>>({});
const listRequestToken = ref(0);
const listLoadingToken = ref(0);
const detailRequestToken = ref(0);
const detailDrawerVisible = ref(false);
const taskList = ref<Api.Task.DeployTaskRecord[]>([]);
const activeTaskId = ref<number | null>(null);
const activeTask = ref<Api.Task.DeployTaskRecord | null>(null);
const taskHosts = ref<Api.Task.DeployTaskHostRecord[]>([]);
const taskLogs = ref<Api.Task.DeployTaskLogRecord[]>([]);

const deployTasks = computed(() =>
  taskList.value.map(item => {
    const statusKey = getDeployTaskStatusKey(item.status);

    return {
      key: item.id,
      id: item.taskNo || String(item.id),
      app: item.appName || item.taskName || '-',
      env: getDeployTaskEnvironment(item),
      batch: getDeployTaskBatch(item),
      operator: item.operatorName || '-',
      taskType: item.taskType,
      status: getDeployTaskStatusLabel(statusKey),
      statusType: getDeployTaskTagType(statusKey),
      statusKey
    };
  })
);

const metrics = computed(() => {
  const pendingApprovalCount = deployTasks.value.filter(item => item.statusKey === 'pendingApproval').length;
  const inProgressCount = deployTasks.value.filter(item => item.statusKey === 'running').length;
  const failedIn24hCount = taskList.value.filter(item => {
    const statusKey = getDeployTaskStatusKey(item.status);
    const latestTime = item.updatedAt || item.finishedAt || item.createdAt;

    return statusKey === 'failed' && isWithinHours(latestTime, 24);
  }).length;

  return [
    {
      key: 'pendingApproval',
      label: t('page.envops.deployTask.summary.pendingApproval.label'),
      value: String(pendingApprovalCount),
      desc: t('page.envops.deployTask.summary.pendingApproval.desc')
    },
    {
      key: 'inProgress',
      label: t('page.envops.deployTask.summary.inProgress.label'),
      value: String(inProgressCount),
      desc: t('page.envops.deployTask.summary.inProgress.desc')
    },
    {
      key: 'failedIn24h',
      label: t('page.envops.deployTask.summary.failedIn24h.label'),
      value: String(failedIn24hCount),
      desc: t('page.envops.deployTask.summary.failedIn24h.desc')
    }
  ];
});

const activeTaskStatusKey = computed(() => getDeployTaskStatusKey(activeTask.value?.status));
const activeTaskStatusLabel = computed(() => getDeployTaskStatusLabel(activeTaskStatusKey.value));
const activeTaskStatusType = computed(() => getDeployTaskTagType(activeTaskStatusKey.value));

async function loadDeployTasks() {
  const requestToken = ++listRequestToken.value;
  const loadingToken = ++listLoadingToken.value;

  loading.value = true;

  try {
    const { data, error } = await fetchGetDeployTasks();

    if (requestToken !== listRequestToken.value) {
      return;
    }

    if (!error) {
      const nextTaskList = getDeployTaskRecords(data);

      taskList.value = nextTaskList;

      if (activeTaskId.value !== null) {
        activeTask.value = nextTaskList.find(item => item.id === activeTaskId.value) ?? null;
      }
    }
  } finally {
    if (loadingToken === listLoadingToken.value) {
      loading.value = false;
    }
  }
}

async function loadTaskDetail(taskId: number, options: { open?: boolean; refreshList?: boolean } = {}) {
  const { open = false, refreshList = true } = options;
  const requestToken = ++detailRequestToken.value;
  const listToken = refreshList ? ++listRequestToken.value : listRequestToken.value;

  detailLoading.value = true;
  activeTaskId.value = taskId;
  activeTask.value = null;
  taskHosts.value = [];
  taskLogs.value = [];

  try {
    let nextTaskList = taskList.value;

    if (refreshList) {
      const { data, error } = await fetchGetDeployTasks();

      if (!error) {
        nextTaskList = getDeployTaskRecords(data);
      }
    }

    const [hostsResponse, logsResponse] = await Promise.all([
      fetchGetDeployTaskHosts(taskId),
      fetchGetDeployTaskLogs(taskId)
    ]);

    if (requestToken !== detailRequestToken.value) {
      return;
    }

    if (refreshList && listToken === listRequestToken.value) {
      taskList.value = nextTaskList;
    }

    const currentTaskList = refreshList && listToken === listRequestToken.value ? nextTaskList : taskList.value;

    activeTask.value = currentTaskList.find(item => item.id === taskId) ?? null;
    taskHosts.value = hostsResponse.error ? [] : getDeployTaskHostRecords(hostsResponse.data);
    taskLogs.value = logsResponse.error ? [] : getDeployTaskLogRecords(logsResponse.data);

    if (open) {
      detailDrawerVisible.value = true;
    }
  } finally {
    if (requestToken === detailRequestToken.value) {
      detailLoading.value = false;
    }
  }
}

async function handleOpenTaskDetail(taskId: number) {
  await loadTaskDetail(taskId, { open: true });
}

async function handleManualRefresh() {
  if (activeTaskId.value === null) {
    return;
  }

  await loadTaskDetail(activeTaskId.value, { open: true });
}

async function handleExecuteTask(taskId: number) {
  if (isTaskMutating(taskId)) {
    return;
  }

  actionLoadingTaskIds.value = [...actionLoadingTaskIds.value, taskId];
  actionLoadingActions.value[taskId] = 'execute';

  try {
    const { data, error } = await fetchPostExecuteDeployTask(taskId);

    if (!error) {
      window.$message?.success(t('common.updateSuccess'));
      await refreshAfterTaskAction(data?.id ?? taskId);
    }
  } finally {
    actionLoadingTaskIds.value = actionLoadingTaskIds.value.filter(id => id !== taskId);
    delete actionLoadingActions.value[taskId];
  }
}

async function handleRetryTask(taskId: number) {
  if (isTaskMutating(taskId)) {
    return;
  }

  actionLoadingTaskIds.value = [...actionLoadingTaskIds.value, taskId];
  actionLoadingActions.value[taskId] = 'retry';

  try {
    const { data, error } = await fetchPostRetryDeployTask(taskId);

    if (!error) {
      window.$message?.success(t('common.updateSuccess'));
      await refreshAfterTaskAction(data?.id ?? taskId);
    }
  } finally {
    actionLoadingTaskIds.value = actionLoadingTaskIds.value.filter(id => id !== taskId);
    delete actionLoadingActions.value[taskId];
  }
}

async function handleRollbackTask(taskId: number) {
  if (isTaskMutating(taskId)) {
    return;
  }

  actionLoadingTaskIds.value = [...actionLoadingTaskIds.value, taskId];
  actionLoadingActions.value[taskId] = 'rollback';

  try {
    const { data, error } = await fetchPostRollbackDeployTask(taskId);

    if (!error) {
      window.$message?.success(t('common.updateSuccess'));
      await refreshAfterTaskAction(data?.id ?? taskId, true);
    }
  } finally {
    actionLoadingTaskIds.value = actionLoadingTaskIds.value.filter(id => id !== taskId);
    delete actionLoadingActions.value[taskId];
  }
}

async function handleCancelTask(taskId: number) {
  if (isTaskMutating(taskId)) {
    return;
  }

  actionLoadingTaskIds.value = [...actionLoadingTaskIds.value, taskId];
  actionLoadingActions.value[taskId] = 'cancel';

  try {
    const { data, error } = await fetchPostCancelDeployTask(taskId);

    if (!error) {
      window.$message?.success(t('common.updateSuccess'));
      await refreshAfterTaskAction(data?.id ?? taskId);
    }
  } finally {
    actionLoadingTaskIds.value = actionLoadingTaskIds.value.filter(id => id !== taskId);
    delete actionLoadingActions.value[taskId];
  }
}

async function refreshAfterTaskAction(taskId: number, openDetail = false) {
  await loadDeployTasks();

  const shouldRefreshDetail = openDetail || (detailDrawerVisible.value && activeTaskId.value === taskId);

  if (shouldRefreshDetail) {
    await loadTaskDetail(taskId, { open: true, refreshList: false });
  }
}

function getDeployTaskRecords(data: Api.Task.DeployTaskRecord[] | null | undefined) {
  return Array.isArray(data) ? data : [];
}

function getDeployTaskHostRecords(data: Api.Task.DeployTaskHostRecord[] | null | undefined) {
  return Array.isArray(data) ? data : [];
}

function getDeployTaskLogRecords(data: Api.Task.DeployTaskLogRecord[] | null | undefined) {
  return Array.isArray(data) ? data : [];
}

function isTaskMutating(taskId: number) {
  return actionLoadingTaskIds.value.includes(taskId);
}

function isActionLoading(action: DeployTaskActionKey, taskId: number) {
  return actionLoadingActions.value[taskId] === action;
}

function canExecuteTask(statusKey: DeployTaskStatusKey) {
  return statusKey === 'pending';
}

function canRetryTask(statusKey: DeployTaskStatusKey) {
  return statusKey === 'failed' || statusKey === 'cancelled';
}

function canRollbackTask(statusKey: DeployTaskStatusKey, taskType: string) {
  return statusKey === 'success' && itemTaskTypeAllowsRollback(taskType);
}

function canCancelTask(statusKey: DeployTaskStatusKey) {
  return statusKey === 'running' || statusKey === 'pending';
}

function itemTaskTypeAllowsRollback(taskType: string | null | undefined) {
  return taskType !== 'ROLLBACK';
}

function getDeployTaskStatusKey(status: string | null | undefined): DeployTaskStatusKey {
  const normalizedStatus = String(status || '').toLowerCase();

  if (normalizedStatus.includes('cancel_requested')) {
    return 'running';
  }

  if (normalizedStatus.includes('approval')) {
    return 'pendingApproval';
  }

  if (normalizedStatus.includes('reject')) {
    return 'rejected';
  }

  if (normalizedStatus.includes('fail') || normalizedStatus.includes('error') || normalizedStatus.includes('rollback')) {
    return 'failed';
  }

  if (normalizedStatus.includes('cancel')) {
    return 'cancelled';
  }

  if (normalizedStatus.includes('run') || normalizedStatus.includes('progress') || normalizedStatus.includes('execut')) {
    return 'running';
  }

  if (normalizedStatus.includes('success') || normalizedStatus.includes('finish') || normalizedStatus.includes('done')) {
    return 'success';
  }

  return 'pending';
}

function getDeployTaskStatusLabel(statusKey: DeployTaskStatusKey) {
  const labelMap: Record<DeployTaskStatusKey, string> = {
    success: t('page.envops.common.status.success'),
    failed: t('page.envops.common.status.failed'),
    running: t('page.envops.common.status.running'),
    pending: t('page.envops.common.status.pending'),
    pendingApproval: t('page.envops.common.status.pendingApproval'),
    cancelled: t('page.envops.common.status.cancelled'),
    rejected: t('page.envops.common.status.rejected')
  };

  return labelMap[statusKey];
}

function getDeployTaskTagType(statusKey: DeployTaskStatusKey): DeployTaskTagType {
  const typeMap: Record<DeployTaskStatusKey, DeployTaskTagType> = {
    success: 'success',
    failed: 'error',
    running: 'info',
    pending: 'default',
    pendingApproval: 'warning',
    cancelled: 'warning',
    rejected: 'error'
  };

  return typeMap[statusKey];
}

function getDeployTaskEnvironment(item: Api.Task.DeployTaskRecord) {
  const rawValue =
    getFirstMeaningfulValue([
      item.params?.['environment'],
      item.params?.['env'],
      item.params?.['profile'],
      item.params?.['namespace'],
      inferEnvironmentFromText(item.taskName),
      inferEnvironmentFromText(item.taskNo)
    ]) || '-';

  if (rawValue === '-') {
    return rawValue;
  }

  const normalizedValue = rawValue.toLowerCase();

  if (['prod', 'production'].includes(normalizedValue)) {
    return t('page.envops.common.environment.production');
  }

  if (['stage', 'staging', 'pre', 'preprod', 'uat'].includes(normalizedValue)) {
    return t('page.envops.common.environment.staging');
  }

  if (['sandbox', 'test', 'testing', 'dev', 'development'].includes(normalizedValue)) {
    return t('page.envops.common.environment.sandbox');
  }

  return rawValue;
}

function getDeployTaskBatch(item: Api.Task.DeployTaskRecord) {
  const strategy = String(item.batchStrategy || '').trim().toUpperCase();

  if (strategy === 'ALL') {
    return t('page.envops.common.batch.fullRelease');
  }

  if (strategy === 'ROLLING') {
    if (item.batchSize && item.targetCount) {
      const percent = Math.round((item.batchSize / item.targetCount) * 100);

      if (percent === 10) {
        return t('page.envops.common.batch.canary10');
      }

      if (percent === 20) {
        return t('page.envops.common.batch.canary20');
      }

      return `ROLLING ${item.batchSize}/${item.targetCount}`;
    }

    if (item.batchSize) {
      return `ROLLING ${item.batchSize}`;
    }
  }

  return strategy || '-';
}

function getFirstMeaningfulValue(values: Array<string | null | undefined>) {
  return values.find(value => typeof value === 'string' && value.trim())?.trim() || '';
}

function inferEnvironmentFromText(value: string | null | undefined) {
  const normalizedValue = String(value || '').toLowerCase();

  if (normalizedValue.includes('prod')) {
    return 'production';
  }

  if (normalizedValue.includes('stag')) {
    return 'staging';
  }

  if (normalizedValue.includes('sandbox') || normalizedValue.includes('test') || normalizedValue.includes('dev')) {
    return 'sandbox';
  }

  return '';
}

function isWithinHours(value: string | null | undefined, hours: number) {
  if (!value) {
    return false;
  }

  const time = new Date(value).getTime();

  if (Number.isNaN(time)) {
    return false;
  }

  return Date.now() - time <= hours * 60 * 60 * 1000;
}

function formatTextValue(value: string | number | null | undefined) {
  if (value === 0) {
    return '0';
  }

  if (value === null || value === undefined) {
    return '-';
  }

  const text = String(value).trim();

  return text || '-';
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-';
  }

  return value.replace('T', ' ');
}

onMounted(() => {
  void loadDeployTasks();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.deployTask.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.deployTask.hero.description') }}</p>
        </div>
        <NButton secondary :loading="loading" @click="loadDeployTasks">
          {{ t('common.refresh') }}
        </NButton>
      </div>
    </NCard>

    <NGrid cols="1 s:3" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in metrics" :key="item.key">
        <NCard :bordered="false" class="card-wrapper">
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NCard :title="t('page.envops.deployTask.table.title')" :bordered="false" class="card-wrapper">
      <NSpin :show="loading">
        <NTable v-if="deployTasks.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.deployTask.table.taskId') }}</th>
              <th>{{ t('page.envops.deployTask.table.application') }}</th>
              <th>{{ t('page.envops.deployTask.table.environment') }}</th>
              <th>{{ t('page.envops.deployTask.table.batch') }}</th>
              <th>{{ t('page.envops.deployTask.table.operator') }}</th>
              <th>{{ t('page.envops.deployTask.table.status') }}</th>
              <th>{{ t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in deployTasks" :key="item.key">
              <td>{{ item.id }}</td>
              <td>{{ item.app }}</td>
              <td>{{ item.env }}</td>
              <td>{{ item.batch }}</td>
              <td>{{ item.operator }}</td>
              <td>
                <NTag :type="item.statusType" size="small">{{ item.status }}</NTag>
              </td>
              <td>
                <NSpace :size="8" wrap>
                  <NButton text size="small" @click="handleOpenTaskDetail(item.key)">
                    {{ t('page.envops.deployTask.actions.detail') }}
                  </NButton>
                  <NButton
                    text
                    type="primary"
                    size="small"
                    :disabled="isTaskMutating(item.key) || !canExecuteTask(item.statusKey)"
                    :loading="isActionLoading('execute', item.key)"
                    @click="handleExecuteTask(item.key)"
                  >
                    {{ t('page.envops.deployTask.actions.execute') }}
                  </NButton>
                  <NButton
                    text
                    type="warning"
                    size="small"
                    :disabled="isTaskMutating(item.key) || !canRetryTask(item.statusKey)"
                    :loading="isActionLoading('retry', item.key)"
                    @click="handleRetryTask(item.key)"
                  >
                    {{ t('page.envops.deployTask.actions.retry') }}
                  </NButton>
                  <NButton
                    text
                    type="error"
                    size="small"
                    :disabled="isTaskMutating(item.key) || !canRollbackTask(item.statusKey, item.taskType)"
                    :loading="isActionLoading('rollback', item.key)"
                    @click="handleRollbackTask(item.key)"
                  >
                    {{ t('page.envops.deployTask.actions.rollback') }}
                  </NButton>
                  <NButton
                    text
                    type="warning"
                    size="small"
                    :disabled="isTaskMutating(item.key) || !canCancelTask(item.statusKey)"
                    :loading="isActionLoading('cancel', item.key)"
                    @click="handleCancelTask(item.key)"
                  >
                    {{ t('page.envops.deployTask.actions.cancel') }}
                  </NButton>
                </NSpace>
              </td>
            </tr>
          </tbody>
        </NTable>
        <NEmpty v-else class="py-24px" :description="t('common.noData')" />
      </NSpin>
    </NCard>

    <NDrawer v-model:show="detailDrawerVisible" :width="960" placement="right">
      <NDrawerContent :title="t('page.envops.deployTask.detail.title')" closable>
        <NSpin :show="detailLoading">
          <NSpace vertical :size="16">
            <NSpace justify="end">
              <NButton text :loading="detailLoading" @click="handleManualRefresh">
                {{ t('page.envops.deployTask.detail.manualRefresh') }}
              </NButton>
            </NSpace>
            <template v-if="activeTask">
              <NCard :bordered="false" embedded>
                <NDescriptions :column="2" label-placement="left" bordered>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.taskId')">
                    {{ formatTextValue(activeTask.taskNo || activeTask.id) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.taskName')">
                    {{ formatTextValue(activeTask.taskName) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.taskType')">
                    {{ formatTextValue(activeTask.taskType) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.originTaskId')">
                    {{ formatTextValue(activeTask.originTaskId) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.application')">
                    {{ formatTextValue(activeTask.appName) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.version')">
                    {{ formatTextValue(activeTask.versionNo) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.environment')">
                    {{ getDeployTaskEnvironment(activeTask) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.batch')">
                    {{ getDeployTaskBatch(activeTask) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.operator')">
                    {{ formatTextValue(activeTask.operatorName) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.status')">
                    <NTag :type="activeTaskStatusType" size="small">{{ activeTaskStatusLabel }}</NTag>
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.approvalOperator')">
                    {{ formatTextValue(activeTask.approvalOperatorName) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.approvalComment')">
                    {{ formatTextValue(activeTask.approvalComment) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.approvalAt')">
                    {{ formatDateTime(activeTask.approvalAt) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.startedAt')">
                    {{ formatDateTime(activeTask.startedAt) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.finishedAt')">
                    {{ formatDateTime(activeTask.finishedAt) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.createdAt')">
                    {{ formatDateTime(activeTask.createdAt) }}
                  </NDescriptionsItem>
                  <NDescriptionsItem :label="t('page.envops.deployTask.detail.updatedAt')">
                    {{ formatDateTime(activeTask.updatedAt) }}
                  </NDescriptionsItem>
                </NDescriptions>
              </NCard>

              <NCard :bordered="false" embedded :title="t('page.envops.deployTask.hosts.title')">
                <NTable v-if="taskHosts.length" :bordered="false" :single-line="false">
                  <thead>
                    <tr>
                      <th>{{ t('page.envops.deployTask.hosts.hostName') }}</th>
                      <th>{{ t('page.envops.deployTask.hosts.ipAddress') }}</th>
                      <th>{{ t('page.envops.deployTask.hosts.status') }}</th>
                      <th>{{ t('page.envops.deployTask.hosts.currentStep') }}</th>
                      <th>{{ t('page.envops.deployTask.hosts.startedAt') }}</th>
                      <th>{{ t('page.envops.deployTask.hosts.finishedAt') }}</th>
                      <th>{{ t('page.envops.deployTask.hosts.errorMsg') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="host in taskHosts" :key="host.id">
                      <td>{{ formatTextValue(host.hostName) }}</td>
                      <td>{{ formatTextValue(host.ipAddress) }}</td>
                      <td>{{ formatTextValue(host.status) }}</td>
                      <td>{{ formatTextValue(host.currentStep) }}</td>
                      <td>{{ formatDateTime(host.startedAt) }}</td>
                      <td>{{ formatDateTime(host.finishedAt) }}</td>
                      <td>{{ formatTextValue(host.errorMsg) }}</td>
                    </tr>
                  </tbody>
                </NTable>
                <NEmpty v-else class="py-24px" :description="t('common.noData')" />
              </NCard>

              <NCard :bordered="false" embedded :title="t('page.envops.deployTask.logs.title')">
                <NTable v-if="taskLogs.length" :bordered="false" :single-line="false">
                  <thead>
                    <tr>
                      <th>{{ t('page.envops.deployTask.logs.createdAt') }}</th>
                      <th>{{ t('page.envops.deployTask.logs.taskHostId') }}</th>
                      <th>{{ t('page.envops.deployTask.logs.logLevel') }}</th>
                      <th>{{ t('page.envops.deployTask.logs.content') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="log in taskLogs" :key="log.id">
                      <td>{{ formatDateTime(log.createdAt) }}</td>
                      <td>{{ formatTextValue(log.taskHostId) }}</td>
                      <td>{{ formatTextValue(log.logLevel) }}</td>
                      <td class="whitespace-pre-wrap break-all">{{ formatTextValue(log.logContent) }}</td>
                    </tr>
                  </tbody>
                </NTable>
                <NEmpty v-else class="py-24px" :description="t('common.noData')" />
              </NCard>
            </template>
            <NEmpty v-else class="py-24px" :description="t('common.noData')" />
          </NSpace>
        </NSpin>
      </NDrawerContent>
    </NDrawer>
  </NSpace>
</template>

<style scoped></style>

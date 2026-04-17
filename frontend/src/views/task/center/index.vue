<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { fetchGetTaskCenterTasks } from '@/service/api';

defineOptions({
  name: 'TaskCenterPage'
});

type TaskCenterStatusKey = 'success' | 'failed' | 'running' | 'queued' | 'pendingApproval' | 'cancelled' | 'rejected';
type TaskCenterTagType = 'success' | 'error' | 'info' | 'default' | 'warning';

const { t } = useI18n();

const loading = ref(false);
const taskList = ref<Api.Task.TaskCenterRecord[]>([]);

const tasks = computed(() =>
  taskList.value.map(item => {
    const statusKey = getTaskCenterStatusKey(item.status);

    return {
      key: item.id,
      id: item.taskNo || String(item.id),
      type: getTaskTypeLabel(item.taskType),
      source: getTaskSourceLabel(item.sourceType),
      executor: item.operatorName || '-',
      priority: getTaskPriority(item, statusKey),
      status: getTaskCenterStatusLabel(statusKey),
      statusType: getTaskCenterTagType(statusKey),
      statusKey
    };
  })
);

const metrics = computed(() => {
  const queuedCount = tasks.value.filter(item => item.statusKey === 'queued' || item.statusKey === 'pendingApproval').length;
  const runningCount = tasks.value.filter(item => item.statusKey === 'running').length;
  const slaBreachRiskCount = taskList.value.filter(item => hasSlaBreachRisk(item)).length;

  return [
    {
      key: 'queued',
      label: t('page.envops.taskCenter.summary.queued.label'),
      value: String(queuedCount),
      desc: t('page.envops.taskCenter.summary.queued.desc')
    },
    {
      key: 'running',
      label: t('page.envops.taskCenter.summary.running.label'),
      value: String(runningCount),
      desc: t('page.envops.taskCenter.summary.running.desc')
    },
    {
      key: 'slaBreachRisk',
      label: t('page.envops.taskCenter.summary.slaBreachRisk.label'),
      value: String(slaBreachRiskCount),
      desc: t('page.envops.taskCenter.summary.slaBreachRisk.desc')
    }
  ];
});

async function loadTaskCenterTasks() {
  loading.value = true;

  try {
    const { data, error } = await fetchGetTaskCenterTasks();

    if (!error) {
      taskList.value = getTaskCenterRecords(data);
    }
  } finally {
    loading.value = false;
  }
}

function getTaskCenterRecords(data: Api.Task.TaskCenterRecord[] | null | undefined) {
  return Array.isArray(data) ? data : [];
}

function getTaskCenterStatusKey(status: string | null | undefined): TaskCenterStatusKey {
  const normalizedStatus = String(status || '').toLowerCase();

  if (normalizedStatus.includes('approval')) {
    return 'pendingApproval';
  }

  if (normalizedStatus.includes('reject')) {
    return 'rejected';
  }

  if (normalizedStatus.includes('fail') || normalizedStatus.includes('error') || normalizedStatus.includes('timeout')) {
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

  return 'queued';
}

function getTaskCenterStatusLabel(statusKey: TaskCenterStatusKey) {
  const labelMap: Record<TaskCenterStatusKey, string> = {
    success: t('page.envops.common.status.success'),
    failed: t('page.envops.common.status.failed'),
    running: t('page.envops.common.status.running'),
    queued: t('page.envops.common.status.queued'),
    pendingApproval: t('page.envops.common.status.pendingApproval'),
    cancelled: t('page.envops.common.status.cancelled'),
    rejected: t('page.envops.common.status.rejected')
  };

  return labelMap[statusKey];
}

function getTaskCenterTagType(statusKey: TaskCenterStatusKey): TaskCenterTagType {
  const typeMap: Record<TaskCenterStatusKey, TaskCenterTagType> = {
    success: 'success',
    failed: 'error',
    running: 'info',
    queued: 'default',
    pendingApproval: 'warning',
    cancelled: 'warning',
    rejected: 'error'
  };

  return typeMap[statusKey];
}

function getTaskTypeLabel(value: string | null | undefined) {
  const normalizedValue = String(value || '').toLowerCase();

  if (normalizedValue.includes('deploy') || normalizedValue.includes('install') || normalizedValue.includes('upgrade') || normalizedValue.includes('rollback') || normalizedValue.includes('release')) {
    return t('page.envops.common.taskType.deploy');
  }

  if (normalizedValue.includes('inspect') || normalizedValue.includes('detect') || normalizedValue.includes('monitor')) {
    return t('page.envops.common.taskType.inspection');
  }

  if (normalizedValue.includes('traffic') || normalizedValue.includes('route') || normalizedValue.includes('gateway')) {
    return t('page.envops.common.taskType.traffic');
  }

  if (normalizedValue.includes('asset') || normalizedValue.includes('sync')) {
    return t('page.envops.common.taskType.assetSync');
  }

  return String(value || '-');
}

function getTaskSourceLabel(value: string | null | undefined) {
  const normalizedValue = String(value || '').toLowerCase();

  if (normalizedValue.includes('deploy')) {
    return t('page.envops.common.taskType.deploy');
  }

  if (normalizedValue.includes('inspect') || normalizedValue.includes('detect') || normalizedValue.includes('monitor')) {
    return t('page.envops.common.taskType.inspection');
  }

  if (normalizedValue.includes('traffic')) {
    return t('page.envops.common.taskType.traffic');
  }

  if (normalizedValue.includes('asset')) {
    return t('page.envops.common.taskType.assetSync');
  }

  return String(value || '-');
}

function getTaskPriority(item: Api.Task.TaskCenterRecord, statusKey: TaskCenterStatusKey) {
  if (statusKey === 'failed' || (item.failCount || 0) > 0) {
    return 'P1';
  }

  if (statusKey === 'running' || (item.targetCount || 0) >= 10) {
    return 'P2';
  }

  return 'P3';
}

function hasSlaBreachRisk(item: Api.Task.TaskCenterRecord) {
  const statusKey = getTaskCenterStatusKey(item.status);

  if (!['queued', 'running'].includes(statusKey)) {
    return false;
  }

  const time = new Date(item.createdAt || item.updatedAt || '').getTime();

  if (Number.isNaN(time)) {
    return false;
  }

  return Date.now() - time >= 2 * 60 * 60 * 1000;
}

onMounted(() => {
  void loadTaskCenterTasks();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.taskCenter.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.taskCenter.hero.description') }}</p>
        </div>
        <NButton secondary :loading="loading" @click="loadTaskCenterTasks">
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

    <NCard :title="t('page.envops.taskCenter.table.title')" :bordered="false" class="card-wrapper">
      <NSpin :show="loading">
        <NTable v-if="tasks.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.taskCenter.table.taskId') }}</th>
              <th>{{ t('page.envops.taskCenter.table.type') }}</th>
              <th>{{ t('page.envops.taskCenter.table.source') }}</th>
              <th>{{ t('page.envops.taskCenter.table.executor') }}</th>
              <th>{{ t('page.envops.taskCenter.table.priority') }}</th>
              <th>{{ t('page.envops.taskCenter.table.status') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tasks" :key="item.key">
              <td>{{ item.id }}</td>
              <td>{{ item.type }}</td>
              <td>{{ item.source }}</td>
              <td>{{ item.executor }}</td>
              <td>{{ item.priority }}</td>
              <td>
                <NTag :type="item.statusType" size="small">{{ item.status }}</NTag>
              </td>
            </tr>
          </tbody>
        </NTable>
        <NEmpty v-else class="py-24px" :description="t('common.noData')" />
      </NSpin>
    </NCard>
  </NSpace>
</template>

<style scoped></style>

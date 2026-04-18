<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { fetchGetMonitorDetectTasks } from '@/service/api';

defineOptions({
  name: 'MonitorDetectTaskPage'
});

type DetectTaskStatusKey = 'success' | 'warning' | 'timeout' | 'failed' | 'running' | 'pending';
type DetectTaskTagType = 'success' | 'warning' | 'error' | 'info' | 'default';

const { t } = useI18n();

const loading = ref(false);
const detectTaskList = ref<Api.Monitor.DetectTaskRecord[]>([]);

const detectTasks = computed(() =>
  detectTaskList.value.map(item => {
    const statusKey = getDetectTaskStatusKey(item);

    return {
      key: item.id,
      name: item.taskName || String(item.id),
      target: getDetectTaskTarget(item),
      schedule: getDetectTaskSchedule(item),
      lastRun: formatDateTime(item.lastRunAt || item.createdAt),
      result: getDetectTaskStatusLabel(statusKey),
      resultType: getDetectTaskTagType(statusKey),
      statusKey
    };
  })
);

const metrics = computed(() => {
  const total = detectTasks.value.length;
  const successCount = detectTasks.value.filter(item => item.statusKey === 'success').length;
  const needsAttentionCount = detectTasks.value.filter(item =>
    ['warning', 'timeout', 'failed'].includes(item.statusKey)
  ).length;
  const successRate = total ? `${((successCount / total) * 100).toFixed(1)}%` : '0%';

  return [
    {
      key: 'scheduledTasks',
      label: t('page.envops.monitorDetectTask.summary.scheduledTasks.label'),
      value: String(total),
      desc: t('page.envops.monitorDetectTask.summary.scheduledTasks.desc')
    },
    {
      key: 'successRate',
      label: t('page.envops.monitorDetectTask.summary.successRate.label'),
      value: successRate,
      desc: t('page.envops.monitorDetectTask.summary.successRate.desc')
    },
    {
      key: 'needsAttention',
      label: t('page.envops.monitorDetectTask.summary.needsAttention.label'),
      value: String(needsAttentionCount),
      desc: t('page.envops.monitorDetectTask.summary.needsAttention.desc')
    }
  ];
});

const successTaskCount = computed(() => detectTasks.value.filter(item => item.statusKey === 'success').length);
const timedOutTaskCount = computed(() => detectTasks.value.filter(item => item.statusKey === 'timeout').length);

async function loadDetectTasks() {
  loading.value = true;

  const { data, error } = await fetchGetMonitorDetectTasks();

  if (!error) {
    detectTaskList.value = getDetectTaskRecords(data);
  }

  loading.value = false;
}

function getDetectTaskRecords(data: Api.Monitor.DetectTaskListResponse) {
  return Array.isArray(data) ? data : [];
}

function getDetectTaskTarget(item: Api.Monitor.DetectTaskRecord) {
  if (typeof item.target === 'string' && item.target.trim()) {
    return item.target;
  }

  return '-';
}

function getDetectTaskSchedule(item: Api.Monitor.DetectTaskRecord) {
  if (typeof item.schedule === 'string' && item.schedule.trim()) {
    return item.schedule;
  }

  return '-';
}

function getDetectTaskStatusKey(item: Api.Monitor.DetectTaskRecord): DetectTaskStatusKey {
  const status = String(item.lastResult || '').toLowerCase();

  if (status.includes('timeout')) {
    return 'timeout';
  }

  if (status.includes('warn') || status.includes('partial')) {
    return 'warning';
  }

  if (status.includes('fail') || status.includes('error')) {
    return 'failed';
  }

  if (status.includes('running') || status.includes('progress') || status.includes('executing')) {
    return 'running';
  }

  if (status.includes('pending') || status.includes('init') || status.includes('queue')) {
    return 'pending';
  }

  if (status.includes('success') || status.includes('finish') || status.includes('done')) {
    return 'success';
  }

  return 'pending';
}

function getDetectTaskStatusLabel(statusKey: DetectTaskStatusKey) {
  const labelMap: Record<DetectTaskStatusKey, string> = {
    success: t('page.envops.common.status.success'),
    warning: t('page.envops.common.status.warning'),
    timeout: t('page.envops.common.status.timeout'),
    failed: t('page.envops.common.status.failed'),
    running: t('page.envops.common.status.running'),
    pending: t('page.envops.common.status.pending')
  };

  return labelMap[statusKey];
}

function getDetectTaskTagType(statusKey: DetectTaskStatusKey): DetectTaskTagType {
  const typeMap: Record<DetectTaskStatusKey, DetectTaskTagType> = {
    success: 'success',
    warning: 'warning',
    timeout: 'error',
    failed: 'error',
    running: 'info',
    pending: 'default'
  };

  return typeMap[statusKey];
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-';
  }

  return value.replace('T', ' ').slice(0, 19);
}

onMounted(() => {
  void loadDetectTasks();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.monitorDetectTask.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.monitorDetectTask.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="success">
            {{ t('page.envops.monitorDetectTask.tags.healthyCount', { count: successTaskCount }) }}
          </NTag>
          <NTag type="error">
            {{ t('page.envops.monitorDetectTask.tags.timedOutCount', { count: timedOutTaskCount }) }}
          </NTag>
          <NButton secondary :loading="loading" @click="loadDetectTasks">
            {{ t('common.refresh') }}
          </NButton>
        </NSpace>
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

    <NCard :title="t('page.envops.monitorDetectTask.table.title')" :bordered="false" class="card-wrapper">
      <NSpin :show="loading">
        <NTable v-if="detectTasks.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.monitorDetectTask.table.task') }}</th>
              <th>{{ t('page.envops.monitorDetectTask.table.target') }}</th>
              <th>{{ t('page.envops.monitorDetectTask.table.schedule') }}</th>
              <th>{{ t('page.envops.monitorDetectTask.table.lastRun') }}</th>
              <th>{{ t('page.envops.monitorDetectTask.table.result') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in detectTasks" :key="item.key">
              <td>{{ item.name }}</td>
              <td>{{ item.target }}</td>
              <td>{{ item.schedule }}</td>
              <td>{{ item.lastRun }}</td>
              <td>
                <NTag :type="item.resultType" size="small">{{ item.result }}</NTag>
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

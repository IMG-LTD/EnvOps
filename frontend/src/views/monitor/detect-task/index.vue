<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  fetchGetAssetHosts,
  fetchGetMonitorDetectTasks,
  fetchPostCreateMonitorDetectTask,
  fetchPostExecuteMonitorDetectTask
} from '@/service/api';

defineOptions({
  name: 'MonitorDetectTaskPage'
});

type DetectTaskStatusKey = 'success' | 'warning' | 'timeout' | 'failed' | 'running' | 'pending';
type DetectTaskTagType = 'success' | 'warning' | 'error' | 'info' | 'default';

const { t } = useI18n();

const loading = ref(false);
const creating = ref(false);
const executingTaskId = ref<number | null>(null);
const detectTaskList = ref<Api.Monitor.DetectTaskRecord[]>([]);
const hostList = ref<Api.Asset.HostRecord[]>([]);
const formModel = reactive({
  taskName: '',
  hostId: null as number | null,
  schedule: 'manual'
});

const hostOptions = computed(() =>
  hostList.value.map(item => ({
    label: `${item.hostName} (${item.ipAddress})`,
    value: item.id
  }))
);

const scheduleOptions = computed(() => [
  {
    label: t('page.envops.monitorDetectTask.form.manualSchedule'),
    value: 'manual'
  },
  {
    label: t('page.envops.common.schedule.every5Min'),
    value: 'every_5m'
  },
  {
    label: t('page.envops.common.schedule.every15Min'),
    value: 'every_15m'
  },
  {
    label: t('page.envops.common.schedule.everyHour'),
    value: 'every_1h'
  }
]);

const detectTasks = computed(() =>
  detectTaskList.value.map(item => {
    const statusKey = getDetectTaskStatusKey(item);

    return {
      key: item.id,
      id: item.id,
      name: item.taskName || String(item.id),
      target: getDetectTaskTarget(item),
      schedule: getDetectTaskScheduleLabel(item.schedule),
      lastRun: formatDateTime(item.lastRunAt || item.createdAt),
      result: getDetectTaskStatusLabel(statusKey),
      resultType: getDetectTaskTagType(statusKey),
      statusKey,
      executing: executingTaskId.value === item.id
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
const attentionTaskCount = computed(
  () => detectTasks.value.filter(item => ['warning', 'timeout', 'failed'].includes(item.statusKey)).length
);

async function loadPage() {
  loading.value = true;

  const [detectTaskResponse, hostResponse] = await Promise.all([
    fetchGetMonitorDetectTasks(),
    fetchGetAssetHosts({ current: 1, size: 100 })
  ]);

  if (!detectTaskResponse.error) {
    detectTaskList.value = getDetectTaskRecords(detectTaskResponse.data);
  }

  if (!hostResponse.error) {
    hostList.value = getHostRecords(hostResponse.data);

    if (formModel.hostId === null) {
      formModel.hostId = hostList.value[0]?.id ?? null;
    }
  }

  loading.value = false;
}

async function loadDetectTasks() {
  loading.value = true;

  const { data, error } = await fetchGetMonitorDetectTasks();

  if (!error) {
    detectTaskList.value = getDetectTaskRecords(data);
  }

  loading.value = false;
}

async function handleCreateTask() {
  if (!formModel.taskName.trim() || formModel.hostId === null) {
    window.$message?.warning(t('page.envops.monitorDetectTask.messages.fillNameAndHost'));
    return;
  }

  creating.value = true;

  try {
    const payload: Api.Monitor.CreateDetectTaskParams = {
      taskName: formModel.taskName.trim(),
      hostId: formModel.hostId,
      schedule: formModel.schedule
    };

    const { error } = await fetchPostCreateMonitorDetectTask(payload);

    if (!error) {
      window.$message?.success(t('page.envops.monitorDetectTask.messages.createSuccess'));
      resetForm();
      await loadDetectTasks();
    }
  } finally {
    creating.value = false;
  }
}

async function handleExecuteTask(taskId: number) {
  executingTaskId.value = taskId;

  try {
    const { error } = await fetchPostExecuteMonitorDetectTask(taskId);

    if (!error) {
      window.$message?.success(t('page.envops.monitorDetectTask.messages.executeSuccess'));
      await loadDetectTasks();
    }
  } finally {
    executingTaskId.value = null;
  }
}

function resetForm() {
  formModel.taskName = '';
  formModel.hostId = hostList.value[0]?.id ?? null;
  formModel.schedule = 'manual';
}

function getDetectTaskRecords(data: Api.Monitor.DetectTaskListResponse) {
  return Array.isArray(data) ? data : [];
}

function getHostRecords(data?: Api.Asset.HostPage | null) {
  return Array.isArray(data?.records) ? data.records : [];
}

function getDetectTaskTarget(item: Api.Monitor.DetectTaskRecord) {
  if (typeof item.target === 'string' && item.target.trim()) {
    return item.target;
  }

  const host = hostList.value.find(current => current.id === item.hostId);
  if (host) {
    return host.hostName;
  }

  return '-';
}

function getDetectTaskScheduleLabel(schedule?: string | null) {
  const scheduleLabelMap: Record<string, string> = {
    manual: t('page.envops.monitorDetectTask.form.manualSchedule'),
    every_5m: t('page.envops.common.schedule.every5Min'),
    every_10m: t('page.envops.common.schedule.every10Min'),
    every_15m: t('page.envops.common.schedule.every15Min'),
    every_1h: t('page.envops.common.schedule.everyHour')
  };

  return scheduleLabelMap[String(schedule || '').trim()] || schedule || '-';
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
  void loadPage();
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
          <NTag type="warning">
            {{ t('page.envops.monitorDetectTask.tags.attentionCount', { count: attentionTaskCount }) }}
          </NTag>
          <NButton secondary :loading="loading" @click="loadPage">
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

    <NGrid cols="1 xl:2" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi>
        <NCard :bordered="false" class="card-wrapper" :title="t('page.envops.monitorDetectTask.form.title')">
          <NForm label-placement="top">
            <NFormItem :label="t('page.envops.monitorDetectTask.form.taskName')">
              <NInput
                v-model:value="formModel.taskName"
                :placeholder="t('page.envops.monitorDetectTask.form.taskNamePlaceholder')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.monitorDetectTask.form.host')">
              <NSelect
                v-model:value="formModel.hostId"
                :options="hostOptions"
                :placeholder="t('page.envops.monitorDetectTask.form.hostPlaceholder')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.monitorDetectTask.form.schedule')">
              <NSelect v-model:value="formModel.schedule" :options="scheduleOptions" />
            </NFormItem>
            <NSpace>
              <NButton type="primary" :loading="creating" :disabled="!hostOptions.length" @click="handleCreateTask">
                {{ t('page.envops.monitorDetectTask.actions.create') }}
              </NButton>
              <NButton @click="resetForm">{{ t('common.reset') }}</NButton>
            </NSpace>
          </NForm>
        </NCard>
      </NGi>

      <NGi>
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
                  <th>{{ t('page.envops.monitorDetectTask.table.operation') }}</th>
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
                  <td>
                    <NButton text type="primary" :loading="item.executing" @click="handleExecuteTask(item.id)">
                      {{ t('page.envops.monitorDetectTask.actions.execute') }}
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

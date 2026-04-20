<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useRouterPush } from '@/hooks/common/router';
import { fetchGetTaskCenterTasks } from '@/service/api';
import { normalizeTaskCenterRouteQuery, toTaskCenterApiQuery } from '@/views/task/shared/query';

defineOptions({
  name: 'TaskCenterPage'
});

type TaskCenterRouteQuery = ReturnType<typeof normalizeTaskCenterRouteQuery>;
type TaskCenterStatusKey = 'success' | 'failed' | 'running' | 'queued' | 'pendingApproval' | 'cancelled' | 'rejected';
type TaskCenterTagType = 'success' | 'error' | 'info' | 'default' | 'warning';

const DEFAULT_TASK_CENTER_ROUTE_QUERY = normalizeTaskCenterRouteQuery({});

const route = useRoute();
const { routerPushByKey } = useRouterPush();
const { t } = useI18n();

const loading = ref(false);
const total = ref(0);
const listRequestToken = ref(0);
const listLoadingToken = ref(0);
const taskList = ref<Api.Task.TaskCenterRecord[]>([]);

const filterForm = reactive({
  keyword: '',
  status: null as string | null,
  taskType: null as string | null,
  priority: null as string | null
});

const normalizedRouteQuery = computed(() => normalizeTaskCenterRouteQuery(route.query as Record<string, unknown>));
const pendingTaskCenterRouteQuery = ref<TaskCenterRouteQuery>(normalizedRouteQuery.value);
const taskCenterListQueryKey = computed(() => JSON.stringify(toTaskCenterApiQuery(normalizedRouteQuery.value)));

const statusOptions = computed(() => [
  { label: t('page.envops.common.status.queued'), value: 'PENDING' },
  { label: t('page.envops.common.status.pendingApproval'), value: 'PENDING_APPROVAL' },
  { label: t('page.envops.common.status.running'), value: 'RUNNING' },
  { label: t('page.envops.common.status.success'), value: 'SUCCESS' },
  { label: t('page.envops.common.status.failed'), value: 'FAILED' },
  { label: t('page.envops.common.status.cancelled'), value: 'CANCELLED' },
  { label: t('page.envops.common.status.rejected'), value: 'REJECTED' }
]);

const taskTypeOptions = computed(() => [
  { label: t('page.envops.deployTask.filters.taskTypeInstall'), value: 'INSTALL' },
  { label: t('page.envops.deployTask.filters.taskTypeUpgrade'), value: 'UPGRADE' },
  { label: t('page.envops.deployTask.filters.taskTypeRollback'), value: 'ROLLBACK' }
]);

const priorityOptions = computed(() => [
  { label: 'P1', value: 'P1' },
  { label: 'P2', value: 'P2' },
  { label: 'P3', value: 'P3' }
]);

const sortByOptions = computed(() => [
  { label: t('page.envops.taskCenter.sorting.createdAt'), value: 'createdAt' },
  { label: t('page.envops.taskCenter.sorting.updatedAt'), value: 'updatedAt' },
  { label: t('page.envops.taskCenter.sorting.taskNo'), value: 'taskNo' },
  { label: t('page.envops.taskCenter.sorting.status'), value: 'status' }
]);

const sortOrderOptions = computed(() => [
  { label: t('page.envops.taskCenter.sorting.desc'), value: 'desc' },
  { label: t('page.envops.taskCenter.sorting.asc'), value: 'asc' }
]);

watch(
  normalizedRouteQuery,
  query => {
    pendingTaskCenterRouteQuery.value = query;
    filterForm.keyword = query.keyword;
    filterForm.status = query.status || null;
    filterForm.taskType = query.taskType || null;
    filterForm.priority = query.priority || null;
  },
  { immediate: true }
);

watch(
  taskCenterListQueryKey,
  () => {
    void loadTaskCenterTasks(normalizedRouteQuery.value);
  },
  { immediate: true }
);

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
      statusKey,
      canOpenDeployDetail: isDeploySourceType(item.sourceType)
    };
  })
);

const metrics = computed(() => {
  const queuedCount = tasks.value.filter(
    item => item.statusKey === 'queued' || item.statusKey === 'pendingApproval'
  ).length;
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

async function loadTaskCenterTasks(query: TaskCenterRouteQuery) {
  const requestToken = ++listRequestToken.value;
  const loadingToken = ++listLoadingToken.value;

  loading.value = true;

  try {
    const { data, error } = await fetchGetTaskCenterTasks(toTaskCenterApiQuery(query));

    if (requestToken !== listRequestToken.value) {
      return;
    }

    if (!error) {
      const nextTaskPage = getTaskCenterPage(data, query);

      taskList.value = nextTaskPage.records;
      total.value = nextTaskPage.total;
    }
  } finally {
    if (loadingToken === listLoadingToken.value) {
      loading.value = false;
    }
  }
}

async function pushTaskCenterRouteQuery(partialQuery: Partial<TaskCenterRouteQuery>) {
  const currentQuery = stringifyTaskCenterRouteQuery(pendingTaskCenterRouteQuery.value);
  const nextPendingQuery = { ...pendingTaskCenterRouteQuery.value, ...partialQuery };
  const nextQuery = stringifyTaskCenterRouteQuery(nextPendingQuery);

  if (isSameRouteQuery(currentQuery, nextQuery)) {
    return;
  }

  pendingTaskCenterRouteQuery.value = nextPendingQuery;
  await routerPushByKey('task_center', { query: nextQuery });
}

async function handleSearch() {
  await pushTaskCenterRouteQuery({
    keyword: filterForm.keyword.trim(),
    status: filterForm.status ?? '',
    taskType: filterForm.taskType ?? '',
    priority: filterForm.priority ?? '',
    page: DEFAULT_TASK_CENTER_ROUTE_QUERY.page
  });
}

async function handleResetFilters() {
  filterForm.keyword = '';
  filterForm.status = null;
  filterForm.taskType = null;
  filterForm.priority = null;

  await pushTaskCenterRouteQuery({
    keyword: '',
    status: '',
    taskType: '',
    priority: '',
    page: DEFAULT_TASK_CENTER_ROUTE_QUERY.page
  });
}

async function handleSortByChange(sortBy: Api.Task.TaskSortBy) {
  await pushTaskCenterRouteQuery({ sortBy, page: DEFAULT_TASK_CENTER_ROUTE_QUERY.page });
}

async function handleSortOrderChange(sortOrder: Api.Task.TaskSortOrder) {
  await pushTaskCenterRouteQuery({ sortOrder, page: DEFAULT_TASK_CENTER_ROUTE_QUERY.page });
}

async function handlePageChange(page: number) {
  await pushTaskCenterRouteQuery({ page });
}

async function handlePageSizeChange(pageSize: number) {
  await pushTaskCenterRouteQuery({ page: DEFAULT_TASK_CENTER_ROUTE_QUERY.page, pageSize });
}

async function handleRefreshList() {
  await loadTaskCenterTasks(normalizedRouteQuery.value);
}

async function handleOpenDeployDetail(taskId: number) {
  await routerPushByKey('deploy_task', { query: { taskId: String(taskId) } });
}

function stringifyTaskCenterRouteQuery(query: TaskCenterRouteQuery) {
  return {
    ...(query.keyword ? { keyword: query.keyword } : {}),
    ...(query.status ? { status: query.status } : {}),
    ...(query.taskType ? { taskType: query.taskType } : {}),
    ...(query.priority ? { priority: query.priority } : {}),
    page: String(query.page),
    pageSize: String(query.pageSize),
    sortBy: query.sortBy,
    sortOrder: query.sortOrder
  };
}

function isSameRouteQuery(left: Record<string, string>, right: Record<string, string>) {
  const leftKeys = Object.keys(left);
  const rightKeys = Object.keys(right);

  if (leftKeys.length !== rightKeys.length) {
    return false;
  }

  return leftKeys.every(key => left[key] === right[key]);
}

function getTaskCenterPage(
  data: Api.Task.TaskCenterPage | null | undefined,
  query: TaskCenterRouteQuery
): Api.Task.TaskCenterPage {
  return (
    data ?? {
      page: query.page,
      pageSize: query.pageSize,
      total: 0,
      records: []
    }
  );
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

  if (normalizedStatus.includes('cancel_requested')) {
    return 'running';
  }

  if (normalizedStatus.includes('cancel')) {
    return 'cancelled';
  }

  if (
    normalizedStatus.includes('run') ||
    normalizedStatus.includes('progress') ||
    normalizedStatus.includes('execut')
  ) {
    return 'running';
  }

  if (
    normalizedStatus.includes('success') ||
    normalizedStatus.includes('finish') ||
    normalizedStatus.includes('done')
  ) {
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

  if (
    normalizedValue.includes('deploy') ||
    normalizedValue.includes('install') ||
    normalizedValue.includes('upgrade') ||
    normalizedValue.includes('rollback') ||
    normalizedValue.includes('release')
  ) {
    return t('page.envops.common.taskType.deploy');
  }

  if (
    normalizedValue.includes('inspect') ||
    normalizedValue.includes('detect') ||
    normalizedValue.includes('monitor')
  ) {
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

  if (
    normalizedValue.includes('inspect') ||
    normalizedValue.includes('detect') ||
    normalizedValue.includes('monitor')
  ) {
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
  const priority = String(item.priority || '').trim();

  if (priority) {
    return priority;
  }

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

function isDeploySourceType(value: string | null | undefined) {
  const normalizedValue = String(value || '')
    .trim()
    .toLowerCase();

  return normalizedValue === 'deploy';
}
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.taskCenter.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.taskCenter.hero.description') }}</p>
        </div>
        <NButton secondary :loading="loading" @click="handleRefreshList">
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
      <NSpace vertical :size="12" class="mb-16px">
        <NSpace wrap>
          <NInput
            v-model:value="filterForm.keyword"
            clearable
            class="w-240px"
            :placeholder="t('page.envops.taskCenter.filters.keyword')"
            @keyup.enter="handleSearch"
          />
          <NSelect
            v-model:value="filterForm.status"
            clearable
            class="w-180px"
            :options="statusOptions"
            :placeholder="t('page.envops.taskCenter.filters.status')"
          />
          <NSelect
            v-model:value="filterForm.taskType"
            clearable
            class="w-180px"
            :options="taskTypeOptions"
            :placeholder="t('page.envops.taskCenter.filters.taskType')"
          />
          <NSelect
            v-model:value="filterForm.priority"
            clearable
            class="w-160px"
            :options="priorityOptions"
            :placeholder="t('page.envops.taskCenter.filters.priority')"
          />
          <NButton type="primary" @click="handleSearch">
            {{ t('page.envops.taskCenter.filters.search') }}
          </NButton>
          <NButton @click="handleResetFilters">
            {{ t('page.envops.taskCenter.filters.reset') }}
          </NButton>
        </NSpace>

        <NSpace wrap>
          <NSelect
            :value="normalizedRouteQuery.sortBy"
            class="w-180px"
            :options="sortByOptions"
            :placeholder="t('page.envops.taskCenter.sorting.status')"
            @update:value="handleSortByChange"
          />
          <NSelect
            :value="normalizedRouteQuery.sortOrder"
            class="w-180px"
            :options="sortOrderOptions"
            :placeholder="`${t('page.envops.taskCenter.sorting.desc')} / ${t('page.envops.taskCenter.sorting.asc')}`"
            @update:value="handleSortOrderChange"
          />
        </NSpace>
      </NSpace>

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
              <th>{{ t('common.action') }}</th>
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
              <td>
                <NButton
                  v-if="item.canOpenDeployDetail"
                  text
                  type="primary"
                  size="small"
                  @click="handleOpenDeployDetail(item.key)"
                >
                  {{ t('page.envops.taskCenter.actions.openDeployDetail') }}
                </NButton>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </NTable>
        <NEmpty v-else class="py-24px" :description="t('common.noData')" />

        <div v-if="total > 0" class="mt-16px flex justify-end">
          <NPagination
            :page="normalizedRouteQuery.page"
            :page-size="normalizedRouteQuery.pageSize"
            :item-count="total"
            :page-sizes="[10, 20, 50]"
            show-size-picker
            @update:page="handlePageChange"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </NSpin>
    </NCard>
  </NSpace>
</template>

<style scoped></style>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useRouterPush } from '@/hooks/common/router';
import { fetchGetTaskCenterTaskDetail, fetchGetTaskCenterTasks } from '@/service/api';
import {
  formatLocalDateTimeRange,
  normalizeTaskCenterRouteQuery,
  toTaskCenterApiQuery
} from '@/views/task/shared/query';

defineOptions({
  name: 'TaskCenterPage'
});

type TaskCenterRouteQuery = ReturnType<typeof normalizeTaskCenterRouteQuery>;
type TaskCenterStatusKey = Api.Task.UnifiedTaskStatus;
type TaskCenterTagType = 'success' | 'error' | 'info' | 'default';

const DEFAULT_TASK_CENTER_ROUTE_QUERY = normalizeTaskCenterRouteQuery({});

const route = useRoute();
const router = useRouter();
const { routerPushByKey } = useRouterPush();
const { t } = useI18n();

const loading = ref(false);
const total = ref(0);
const listRequestToken = ref(0);
const listLoadingToken = ref(0);
const detailRequestToken = ref(0);
const detailLoadingToken = ref(0);
const taskList = ref<Api.Task.TaskCenterRecord[]>([]);
const showTaskDetailDrawer = ref(false);
const taskDetailLoading = ref(false);
const activeTaskDetail = ref<Api.Task.TaskCenterDetail | null>(null);

const filterForm = reactive({
  keyword: '',
  status: null as Api.Task.UnifiedTaskStatus | null,
  taskType: null as Api.Task.TaskCenterTaskType | null,
  startedRange: null as [number, number] | null
});

const normalizedRouteQuery = computed(() => normalizeTaskCenterRouteQuery(route.query as Record<string, unknown>));
const pendingTaskCenterRouteQuery = ref<TaskCenterRouteQuery>(normalizedRouteQuery.value);
const taskCenterListQueryKey = computed(() => JSON.stringify(toTaskCenterApiQuery(normalizedRouteQuery.value)));

const statusOptions = computed(() => [
  { label: t('page.envops.common.status.pending'), value: 'pending' },
  { label: t('page.envops.common.status.running'), value: 'running' },
  { label: t('page.envops.common.status.success'), value: 'success' },
  { label: t('page.envops.common.status.failed'), value: 'failed' }
]);

const taskTypeOptions = computed(() => [
  { label: t('page.envops.taskCenter.taskTypes.deploy'), value: 'deploy' },
  { label: t('page.envops.taskCenter.taskTypes.databaseConnectivity'), value: 'database_connectivity' },
  { label: t('page.envops.taskCenter.taskTypes.trafficAction'), value: 'traffic_action' }
]);

watch(
  normalizedRouteQuery,
  query => {
    pendingTaskCenterRouteQuery.value = query;
    filterForm.keyword = query.keyword;
    filterForm.status = query.status || null;
    filterForm.taskType = query.taskType || null;
    filterForm.startedRange = getStartedRangeValue(query.startedFrom, query.startedTo);
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
  taskList.value.map(item => ({
    key: item.id,
    taskName: item.taskName,
    taskType: getTaskTypeLabel(item.taskType),
    triggeredBy: item.triggeredBy || '-',
    startedAt: item.startedAt || '-',
    finishedAt: item.finishedAt || '-',
    summary: item.summary || '-',
    status: getTaskCenterStatusLabel(item.status),
    statusType: getTaskCenterTagType(item.status)
  }))
);

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
  const startedRange = getStartedRangeQueryValue(filterForm.startedRange);

  await pushTaskCenterRouteQuery({
    keyword: filterForm.keyword.trim(),
    status: filterForm.status ?? '',
    taskType: filterForm.taskType ?? '',
    startedFrom: startedRange[0],
    startedTo: startedRange[1],
    page: DEFAULT_TASK_CENTER_ROUTE_QUERY.page
  });
}

async function handleResetFilters() {
  filterForm.keyword = '';
  filterForm.status = null;
  filterForm.taskType = null;
  filterForm.startedRange = null;

  await pushTaskCenterRouteQuery({
    keyword: '',
    status: '',
    taskType: '',
    startedFrom: '',
    startedTo: '',
    page: DEFAULT_TASK_CENTER_ROUTE_QUERY.page
  });
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

function closeTaskDetailDrawer() {
  detailRequestToken.value += 1;
  detailLoadingToken.value += 1;
  showTaskDetailDrawer.value = false;
  activeTaskDetail.value = null;
  taskDetailLoading.value = false;
}

function handleTaskDetailDrawerShowChange(show: boolean) {
  if (show) {
    showTaskDetailDrawer.value = true;
    return;
  }

  closeTaskDetailDrawer();
}

async function handleOpenTaskDetail(taskId: number) {
  const requestToken = ++detailRequestToken.value;
  const loadingToken = ++detailLoadingToken.value;

  showTaskDetailDrawer.value = true;
  activeTaskDetail.value = null;
  taskDetailLoading.value = true;

  try {
    const { data, error } = await fetchGetTaskCenterTaskDetail(taskId);

    if (requestToken !== detailRequestToken.value) {
      return;
    }

    if (!error) {
      activeTaskDetail.value = data ?? null;
    }
  } finally {
    if (loadingToken === detailLoadingToken.value) {
      taskDetailLoading.value = false;
    }
  }
}

async function openSourceDetail() {
  if (!activeTaskDetail.value?.sourceRoute) {
    return;
  }

  await router.push(activeTaskDetail.value.sourceRoute);
}

function stringifyTaskCenterRouteQuery(query: TaskCenterRouteQuery) {
  return {
    ...(query.keyword ? { keyword: query.keyword } : {}),
    ...(query.status ? { status: query.status } : {}),
    ...(query.taskType ? { taskType: query.taskType } : {}),
    ...(query.startedFrom ? { startedFrom: query.startedFrom } : {}),
    ...(query.startedTo ? { startedTo: query.startedTo } : {}),
    page: String(query.page),
    pageSize: String(query.pageSize)
  };
}

function getStartedRangeValue(startedFrom: string, startedTo: string): [number, number] | null {
  if (!startedFrom || !startedTo) {
    return null;
  }

  const start = new Date(startedFrom).getTime();
  const end = new Date(startedTo).getTime();

  if (Number.isNaN(start) || Number.isNaN(end)) {
    return null;
  }

  return [start, end];
}

function getStartedRangeQueryValue(startedRange: [number, number] | null): [string, string] {
  return formatLocalDateTimeRange(startedRange);
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

function getTaskCenterStatusLabel(status: string | null | undefined) {
  const labelMap: Record<TaskCenterStatusKey, string> = {
    pending: t('page.envops.common.status.pending'),
    running: t('page.envops.common.status.running'),
    success: t('page.envops.common.status.success'),
    failed: t('page.envops.common.status.failed')
  };

  return labelMap[(status as TaskCenterStatusKey) || 'pending'] ?? String(status || '-');
}

function getTaskCenterTagType(status: string | null | undefined): TaskCenterTagType {
  const typeMap: Record<TaskCenterStatusKey, TaskCenterTagType> = {
    pending: 'default',
    running: 'info',
    success: 'success',
    failed: 'error'
  };

  return typeMap[(status as TaskCenterStatusKey) || 'pending'] ?? 'default';
}

function getTaskTypeLabel(taskType: string | null | undefined) {
  const labelMap: Record<Api.Task.TaskCenterTaskType, string> = {
    deploy: t('page.envops.taskCenter.taskTypes.deploy'),
    database_connectivity: t('page.envops.taskCenter.taskTypes.databaseConnectivity'),
    traffic_action: t('page.envops.taskCenter.taskTypes.trafficAction')
  };

  return labelMap[(taskType as Api.Task.TaskCenterTaskType) || 'deploy'] ?? String(taskType || '-');
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
            v-model:value="filterForm.taskType"
            clearable
            class="w-220px"
            :options="taskTypeOptions"
            :placeholder="t('page.envops.taskCenter.filters.taskType')"
          />
          <NSelect
            v-model:value="filterForm.status"
            clearable
            class="w-180px"
            :options="statusOptions"
            :placeholder="t('page.envops.taskCenter.filters.status')"
          />
          <NDatePicker
            v-model:value="filterForm.startedRange"
            clearable
            type="datetimerange"
            class="w-320px"
            :start-placeholder="t('page.envops.taskCenter.filters.startedFrom')"
            :end-placeholder="t('page.envops.taskCenter.filters.startedTo')"
          />
          <NButton type="primary" @click="handleSearch">
            {{ t('page.envops.taskCenter.filters.search') }}
          </NButton>
          <NButton @click="handleResetFilters">
            {{ t('page.envops.taskCenter.filters.reset') }}
          </NButton>
        </NSpace>
      </NSpace>

      <NSpin :show="loading">
        <NTable v-if="tasks.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.taskCenter.table.taskName') }}</th>
              <th>{{ t('page.envops.taskCenter.table.taskType') }}</th>
              <th>{{ t('page.envops.taskCenter.table.triggeredBy') }}</th>
              <th>{{ t('page.envops.taskCenter.table.startedAt') }}</th>
              <th>{{ t('page.envops.taskCenter.table.finishedAt') }}</th>
              <th>{{ t('page.envops.taskCenter.table.summary') }}</th>
              <th>{{ t('page.envops.taskCenter.table.status') }}</th>
              <th>{{ t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tasks" :key="item.key">
              <td>{{ item.taskName }}</td>
              <td>{{ item.taskType }}</td>
              <td>{{ item.triggeredBy }}</td>
              <td>{{ item.startedAt }}</td>
              <td>{{ item.finishedAt }}</td>
              <td>{{ item.summary }}</td>
              <td>
                <NTag :type="item.statusType" size="small">{{ item.status }}</NTag>
              </td>
              <td>
                <NButton text type="primary" size="small" @click="handleOpenTaskDetail(item.key)">
                  {{ t('page.envops.taskCenter.actions.openTaskDetail') }}
                </NButton>
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

    <NDrawer
      :show="showTaskDetailDrawer"
      :width="520"
      placement="right"
      @update:show="handleTaskDetailDrawerShowChange"
    >
      <NDrawerContent :title="t('page.envops.taskCenter.drawer.title')">
        <NSpin :show="taskDetailLoading">
          <NDescriptions bordered :column="1">
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.taskName')">
              {{ activeTaskDetail?.taskName || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.taskType')">
              {{ activeTaskDetail ? getTaskTypeLabel(activeTaskDetail.taskType) : '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.triggeredBy')">
              {{ activeTaskDetail?.triggeredBy || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.startedAt')">
              {{ activeTaskDetail?.startedAt || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.finishedAt')">
              {{ activeTaskDetail?.finishedAt || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.summary')">
              {{ activeTaskDetail?.summary || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.detailPreview')">
              <pre class="mb-0 whitespace-pre-wrap break-all">{{
                JSON.stringify(activeTaskDetail?.detailPreview ?? {}, null, 2)
              }}</pre>
            </NDescriptionsItem>
            <NDescriptionsItem
              v-if="activeTaskDetail?.errorSummary"
              :label="t('page.envops.taskCenter.drawer.errorSummary')"
            >
              {{ activeTaskDetail.errorSummary }}
            </NDescriptionsItem>
          </NDescriptions>

          <div class="mt-16px flex justify-end">
            <NButton type="primary" :disabled="!activeTaskDetail?.sourceRoute" @click="openSourceDetail">
              {{ t('page.envops.taskCenter.actions.openSourceDetail') }}
            </NButton>
          </div>
        </NSpin>
      </NDrawerContent>
    </NDrawer>
  </NSpace>
</template>

<style scoped></style>

<script setup lang="ts">
import { NAlert, NTabPane } from 'naive-ui';
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useRouterPush } from '@/hooks/common/router';
import {
  fetchGetAppVersions,
  fetchGetApps,
  fetchGetAssetHosts,
  fetchPostApproveDeployTask,
  fetchPostCancelDeployTask,
  fetchPostCreateDeployTask,
  fetchPostExecuteDeployTask,
  fetchGetDeployTask,
  fetchGetDeployTaskHosts,
  fetchGetDeployTaskLogs,
  fetchGetDeployTasks,
  fetchPostRejectDeployTask,
  fetchPostRetryDeployTask,
  fetchPostRollbackDeployTask
} from '@/service/api';
import {
  formatLocalDateTimeRange,
  normalizeDeployTaskRouteQuery,
  toDeployTaskApiQuery
} from '@/views/task/shared/query';

defineOptions({
  name: 'DeployTaskPage'
});

type DeployTaskRouteQuery = ReturnType<typeof normalizeDeployTaskRouteQuery>;
type DeployTaskStatusKey = 'success' | 'failed' | 'running' | 'pending' | 'pendingApproval' | 'cancelled' | 'rejected';
type DeployTaskTagType = 'success' | 'error' | 'info' | 'default' | 'warning';
type DeployTaskActionKey = 'execute' | 'retry' | 'rollback' | 'cancel' | 'approve' | 'reject';
type DeployTaskDetailTab = 'overview' | 'hosts' | 'logs';
type DeployTaskSummaryKey = 'pendingApproval' | 'inProgress' | 'failedIn24h';
type DeployTaskApprovalAction = 'approve' | 'reject';
type DeployTaskCreateFormModel = {
  taskName: string;
  taskType: Api.Task.DeployTaskCreateTaskType;
  appId: number | null;
  versionId: number | null;
  environment: string;
  hostIds: number[];
  batchStrategy: Api.Task.DeployTaskBatchStrategy;
  batchSize: number | null;
  sshUser: string;
  sshPort: number | null;
  privateKeyPath: string;
  remoteBaseDir: string;
  rollbackCommand: string;
};
type DeployTaskApprovalFormModel = {
  taskId: number | null;
  action: DeployTaskApprovalAction;
  comment: string;
};

const DETAIL_PAGE = 1;
const DETAIL_PAGE_SIZE = 10;
const LOG_HOST_OPTIONS_PAGE_SIZE = 100;
const CREATE_HOST_OPTIONS_PAGE_SIZE = 100;
const DETAIL_REFRESH_INTERVAL = 5000;
const SUMMARY_FAILED_RANGE_HOURS = 24;
const DEFAULT_DEPLOY_TASK_ROUTE_QUERY = normalizeDeployTaskRouteQuery({});

const route = useRoute();
const { routerPushByKey } = useRouterPush();
const { t } = useI18n();

const loading = ref(false);
const detailLoading = ref(false);
const hostsLoading = ref(false);
const logsLoading = ref(false);
const total = ref(0);
const actionLoadingTaskIds = ref<number[]>([]);
const actionLoadingActions = ref<Partial<Record<number, DeployTaskActionKey>>>({});
const listRequestToken = ref(0);
const listLoadingToken = ref(0);
const detailRequestToken = ref(0);
const apps = ref<Api.App.AppDefinition[]>([]);
const taskList = ref<Api.Task.DeployTaskRecord[]>([]);
const activeTaskId = ref<number | null>(null);
const activeTask = ref<Api.Task.DeployTaskDetailRecord | null>(null);
const taskHosts = ref<Api.Task.DeployTaskHostRecord[]>([]);
const logHosts = ref<Api.Task.DeployTaskHostRecord[]>([]);
const taskLogs = ref<Api.Task.DeployTaskLogRecord[]>([]);
const hostsPage = ref<Api.Task.DeployTaskHostPage>(createEmptyDeployTaskHostPage());
const logsPage = ref<Api.Task.DeployTaskLogPage>(createEmptyDeployTaskLogPage());
const detailError = ref('');
const hostsError = ref('');
const logsError = ref('');
const activeDetailTab = ref<DeployTaskDetailTab>('overview');
const detailRefreshTimer = ref<number | null>(null);
const detailRequestInFlight = ref(false);
const detailRequestInFlightToken = ref(0);
const hostsRequestInFlight = ref(false);
const hostsRequestToken = ref(0);
const hostsLoadingToken = ref(0);
const logHostsRequestInFlight = ref(false);
const logHostsRequestToken = ref(0);
const logHostsRequestInFlightToken = ref(0);
const logsRequestInFlight = ref(false);
const logsRequestToken = ref(0);
const logsLoadingToken = ref(0);
const createDrawerVisible = ref(false);
const createSubmitting = ref(false);
const createVersionsLoading = ref(false);
const createHostsLoading = ref(false);
const createVersionRequestToken = ref(0);
const createHostRequestToken = ref(0);
const createHosts = ref<Api.Asset.HostRecord[]>([]);
const appVersions = ref<Api.App.AppVersion[]>([]);
const approvalDrawerVisible = ref(false);
const approvalSubmitting = ref(false);

const filterForm = reactive({
  keyword: '',
  status: null as string | null,
  taskType: null as string | null,
  appId: null as number | null,
  environment: null as string | null,
  createdRange: null as [number, number] | null
});

const hostQuery = reactive({
  status: null as string | null,
  keyword: '',
  page: DETAIL_PAGE,
  pageSize: DETAIL_PAGE_SIZE
});

const logQuery = reactive({
  hostId: null as number | null,
  keyword: '',
  page: DETAIL_PAGE,
  pageSize: DETAIL_PAGE_SIZE
});

const createForm = reactive<DeployTaskCreateFormModel>(createDefaultCreateFormModel());
const approvalForm = reactive<DeployTaskApprovalFormModel>(createDefaultApprovalFormModel());

const normalizedRouteQuery = computed(() => normalizeDeployTaskRouteQuery(route.query as Record<string, unknown>));
const detailDrawerVisible = computed(() => normalizedRouteQuery.value.taskId !== null);
const deployTaskListQueryKey = computed(() => JSON.stringify(toDeployTaskApiQuery(normalizedRouteQuery.value)));

const statusOptions = computed(() => [
  { label: t('page.envops.common.status.pending'), value: 'PENDING' },
  { label: t('page.envops.common.status.pendingApproval'), value: 'PENDING_APPROVAL' },
  { label: t('page.envops.common.status.running'), value: 'RUNNING' },
  { label: t('page.envops.common.status.success'), value: 'SUCCESS' },
  { label: t('page.envops.common.status.failed'), value: 'FAILED' },
  { label: t('page.envops.common.status.cancelled'), value: 'CANCELLED' },
  { label: t('page.envops.common.status.rejected'), value: 'REJECTED' }
]);

const hostStatusOptions = computed(() => [
  { label: t('page.envops.common.status.pending'), value: 'PENDING' },
  { label: t('page.envops.common.status.running'), value: 'RUNNING' },
  { label: t('page.envops.common.status.success'), value: 'SUCCESS' },
  { label: t('page.envops.common.status.failed'), value: 'FAILED' },
  { label: t('page.envops.common.status.cancelled'), value: 'CANCELLED' }
]);

const taskTypeOptions = computed(() => [
  { label: t('page.envops.deployTask.filters.taskTypeInstall'), value: 'INSTALL' },
  { label: t('page.envops.deployTask.filters.taskTypeUpgrade'), value: 'UPGRADE' },
  { label: t('page.envops.deployTask.filters.taskTypeRollback'), value: 'ROLLBACK' }
]);

const environmentOptions = computed(() => [
  { label: t('page.envops.common.environment.production'), value: 'production' },
  { label: t('page.envops.common.environment.staging'), value: 'staging' },
  { label: t('page.envops.common.environment.sandbox'), value: 'sandbox' }
]);

const appOptions = computed(() => {
  return apps.value.map(item => ({
    label: `${item.appName} (${item.appCode})`,
    value: Number(item.id)
  }));
});

const createTaskTypeOptions = computed(() => [
  { label: t('page.envops.deployTask.filters.taskTypeInstall'), value: 'INSTALL' },
  { label: t('page.envops.deployTask.filters.taskTypeUpgrade'), value: 'UPGRADE' }
]);

const createBatchStrategyOptions = computed(() => [
  { label: t('page.envops.deployTask.create.batchStrategyAll'), value: 'ALL' },
  { label: t('page.envops.deployTask.create.batchStrategyRolling'), value: 'ROLLING' }
]);

const createVersionOptions = computed(() => {
  return appVersions.value
    .map(item => {
      const value = Number(item.id);

      if (!Number.isInteger(value) || value <= 0) {
        return null;
      }

      return {
        label: item.versionNo,
        value
      };
    })
    .filter((item): item is { label: string; value: number } => item !== null);
});

const createHostOptions = computed(() => {
  return createHosts.value
    .map(item => {
      const value = Number(item.id);

      if (!Number.isInteger(value) || value <= 0) {
        return null;
      }

      return {
        label: `${item.hostName} (${item.ipAddress}) · ${getEnvironmentLabel(item.environment)}`,
        value
      };
    })
    .filter((item): item is { label: string; value: number } => item !== null);
});

const approvalDrawerTitle = computed(() => {
  return t(
    approvalForm.action === 'approve'
      ? 'page.envops.deployTask.approval.approveTitle'
      : 'page.envops.deployTask.approval.rejectTitle'
  );
});

const approvalSubmitText = computed(() => {
  return t(
    approvalForm.action === 'approve'
      ? 'page.envops.deployTask.actions.approve'
      : 'page.envops.deployTask.actions.reject'
  );
});

const logHostOptions = computed(() => {
  const optionMap = new Map<number, string>();

  logHosts.value.forEach(item => {
    if (!Number.isInteger(item.hostId) || item.hostId <= 0) {
      return;
    }

    const label = item.hostName ? `${item.hostName} (#${item.hostId})` : `#${item.hostId}`;

    optionMap.set(item.hostId, label);
  });

  return Array.from(optionMap.entries()).map(([value, label]) => ({ label, value }));
});

const sortByOptions = computed(() => [
  { label: t('page.envops.deployTask.sorting.createdAt'), value: 'createdAt' },
  { label: t('page.envops.deployTask.sorting.updatedAt'), value: 'updatedAt' },
  { label: t('page.envops.deployTask.sorting.taskNo'), value: 'taskNo' },
  { label: t('page.envops.deployTask.sorting.status'), value: 'status' }
]);

const sortOrderOptions = computed(() => [
  { label: 'DESC', value: 'desc' },
  { label: 'ASC', value: 'asc' }
]);

watch(
  normalizedRouteQuery,
  query => {
    filterForm.keyword = query.keyword;
    filterForm.status = query.status || null;
    filterForm.taskType = query.taskType || null;
    filterForm.appId = query.appId;
    filterForm.environment = query.environment || null;
    filterForm.createdRange = getCreatedRangeValue(query.createdFrom, query.createdTo);
  },
  { immediate: true }
);

watch(
  deployTaskListQueryKey,
  () => {
    void loadDeployTasks(normalizedRouteQuery.value);
  },
  { immediate: true }
);

watch(
  () => normalizedRouteQuery.value.taskId,
  taskId => {
    if (taskId === null) {
      detailRequestToken.value++;
      detailLoading.value = false;
      activeTaskId.value = null;
      activeTask.value = null;
      taskHosts.value = [];
      logHosts.value = [];
      taskLogs.value = [];
      resetHostQuery();
      resetLogQuery();
      hostsPage.value = createEmptyDeployTaskHostPage();
      logsPage.value = createEmptyDeployTaskLogPage();
      detailError.value = '';
      hostsError.value = '';
      logsError.value = '';
      activeDetailTab.value = 'overview';
      stopDetailRefreshTimer();
      return;
    }

    prepareTaskDetailState(taskId);
    void openTaskDetail(taskId);
  },
  { immediate: true }
);

watch(
  [detailDrawerVisible, () => activeTask.value?.status],
  () => {
    syncDetailRefreshTimer();
  },
  { immediate: true }
);

watch(
  () => createForm.appId,
  appId => {
    if (!createDrawerVisible.value) {
      return;
    }

    void loadCreateVersions(appId);
  }
);

watch(
  () => createForm.environment,
  environment => {
    if (!createDrawerVisible.value) {
      return;
    }

    void loadCreateHosts(environment);
  }
);

watch(
  () => createForm.batchStrategy,
  batchStrategy => {
    if (batchStrategy === 'ALL') {
      createForm.batchSize = null;
    }
  }
);

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
      rawStatus: item.status,
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

    return statusKey === 'failed' && isWithinHours(latestTime, SUMMARY_FAILED_RANGE_HOURS);
  }).length;

  return [
    {
      key: 'pendingApproval' as const,
      label: t('page.envops.deployTask.summary.pendingApproval.label'),
      value: String(pendingApprovalCount),
      desc: t('page.envops.deployTask.summary.pendingApproval.desc'),
      summaryQuery: {
        status: 'PENDING_APPROVAL',
        createdFrom: '',
        createdTo: ''
      }
    },
    {
      key: 'inProgress' as const,
      label: t('page.envops.deployTask.summary.inProgress.label'),
      value: String(inProgressCount),
      desc: t('page.envops.deployTask.summary.inProgress.desc'),
      summaryQuery: {
        status: 'RUNNING',
        createdFrom: '',
        createdTo: ''
      }
    },
    {
      key: 'failedIn24h' as const,
      label: t('page.envops.deployTask.summary.failedIn24h.label'),
      value: String(failedIn24hCount),
      desc: t('page.envops.deployTask.summary.failedIn24h.desc'),
      summaryQuery: {
        status: 'FAILED',
        createdFrom: '',
        createdTo: ''
      }
    }
  ];
});

const activeTaskStatusKey = computed(() => getDeployTaskStatusKey(activeTask.value?.status));
const activeTaskStatusLabel = computed(() => getDeployTaskStatusLabel(activeTaskStatusKey.value));
const activeTaskStatusType = computed(() => getDeployTaskTagType(activeTaskStatusKey.value));

const progressSummary = computed(() => {
  return [
    {
      key: 'totalHosts',
      label: t('page.envops.deployTask.progress.totalHosts'),
      value: String(activeTask.value?.totalHosts ?? 0)
    },
    {
      key: 'pendingHosts',
      label: t('page.envops.deployTask.progress.pendingHosts'),
      value: String(activeTask.value?.pendingHosts ?? 0)
    },
    {
      key: 'runningHosts',
      label: t('page.envops.deployTask.progress.runningHosts'),
      value: String(activeTask.value?.runningHosts ?? 0)
    },
    {
      key: 'successHosts',
      label: t('page.envops.deployTask.progress.successHosts'),
      value: String(activeTask.value?.successHosts ?? 0)
    },
    {
      key: 'failedHosts',
      label: t('page.envops.deployTask.progress.failedHosts'),
      value: String(activeTask.value?.failedHosts ?? 0)
    },
    {
      key: 'cancelledHosts',
      label: t('page.envops.deployTask.progress.cancelledHosts'),
      value: String(activeTask.value?.cancelledHosts ?? 0)
    }
  ];
});

function createDefaultCreateFormModel(): DeployTaskCreateFormModel {
  return {
    taskName: '',
    taskType: 'INSTALL',
    appId: null,
    versionId: null,
    environment: 'production',
    hostIds: [],
    batchStrategy: 'ALL',
    batchSize: null,
    sshUser: 'deploy',
    sshPort: 22,
    privateKeyPath: '',
    remoteBaseDir: '/opt/envops/releases',
    rollbackCommand: ''
  };
}

function createDefaultApprovalFormModel(): DeployTaskApprovalFormModel {
  return {
    taskId: null,
    action: 'approve',
    comment: ''
  };
}

function resetCreateForm() {
  Object.assign(createForm, createDefaultCreateFormModel());
}

function resetApprovalForm() {
  Object.assign(approvalForm, createDefaultApprovalFormModel());
}

async function loadApps() {
  const { data, error } = await fetchGetApps();

  if (!error && data) {
    apps.value = data.filter(item => Number.isInteger(Number(item.id)) && Number(item.id) > 0);
  }
}

async function loadCreateVersions(appId: number | null) {
  const requestToken = ++createVersionRequestToken.value;

  appVersions.value = [];
  createForm.versionId = null;

  if (appId === null) {
    return;
  }

  createVersionsLoading.value = true;

  try {
    const { data, error } = await fetchGetAppVersions(appId);

    if (requestToken !== createVersionRequestToken.value) {
      return;
    }

    if (!error && data) {
      appVersions.value = data.filter(item => Number.isInteger(Number(item.id)) && Number(item.id) > 0);
    }
  } finally {
    if (requestToken === createVersionRequestToken.value) {
      createVersionsLoading.value = false;
    }
  }
}

async function loadCreateHosts(environment: string) {
  const requestToken = ++createHostRequestToken.value;

  createHosts.value = [];
  createForm.hostIds = [];
  createHostsLoading.value = true;

  try {
    const { data, error } = await fetchGetAssetHosts({ current: DETAIL_PAGE, size: CREATE_HOST_OPTIONS_PAGE_SIZE });

    if (requestToken !== createHostRequestToken.value) {
      return;
    }

    if (!error && data) {
      const normalizedEnvironment = environment.trim().toLowerCase();

      createHosts.value = data.records.filter(item => {
        const value = String(item.environment || '')
          .trim()
          .toLowerCase();

        return !normalizedEnvironment || value === normalizedEnvironment;
      });
    }
  } finally {
    if (requestToken === createHostRequestToken.value) {
      createHostsLoading.value = false;
    }
  }
}

async function loadDeployTasks(query: DeployTaskRouteQuery) {
  const requestToken = ++listRequestToken.value;
  const loadingToken = ++listLoadingToken.value;

  loading.value = true;

  try {
    const { data, error } = await fetchGetDeployTasks(toDeployTaskApiQuery(query));

    if (requestToken !== listRequestToken.value) {
      return;
    }

    if (!error) {
      const nextTaskPage = getDeployTaskPage(data, query);
      const nextTaskList = nextTaskPage.records;

      taskList.value = nextTaskList;
      total.value = nextTaskPage.total;
    }
  } finally {
    if (loadingToken === listLoadingToken.value) {
      loading.value = false;
    }
  }
}

async function openTaskDetail(taskId: number) {
  const detailPromise = loadTaskDetail(taskId, { refreshList: false, preserveOnError: false, force: true });
  const hostsPromise = loadTaskHosts(taskId);
  const logsPromise = loadTaskLogs(taskId);

  await detailPromise;

  if (activeTaskId.value !== taskId) {
    return;
  }

  await Promise.all([hostsPromise, logsPromise, loadLogHosts(taskId)]);
}

async function loadTaskDetail(
  taskId: number,
  options: { refreshList?: boolean; preserveOnError?: boolean; force?: boolean } = {}
) {
  const { refreshList = true, preserveOnError = true, force = false } = options;

  if (detailRequestInFlight.value && !force) {
    return;
  }

  const requestToken = ++detailRequestToken.value;
  const listToken = refreshList ? ++listRequestToken.value : listRequestToken.value;
  const inFlightToken = ++detailRequestInFlightToken.value;

  detailRequestInFlight.value = true;
  detailLoading.value = true;
  activeTaskId.value = taskId;

  try {
    let nextTaskList = taskList.value;
    let nextTotal = total.value;

    if (refreshList) {
      const query = normalizedRouteQuery.value;
      const { data, error } = await fetchGetDeployTasks(toDeployTaskApiQuery(query));

      if (!error) {
        const nextTaskPage = getDeployTaskPage(data, query);

        nextTaskList = nextTaskPage.records;
        nextTotal = nextTaskPage.total;
      }
    }

    const taskDetailResponse = await fetchGetDeployTask(taskId);

    if (requestToken !== detailRequestToken.value) {
      return;
    }

    if (refreshList && listToken === listRequestToken.value) {
      taskList.value = nextTaskList;
      total.value = nextTotal;
    }

    const currentTaskList = refreshList && listToken === listRequestToken.value ? nextTaskList : taskList.value;

    if (!taskDetailResponse.error && taskDetailResponse.data && currentTaskList.some(item => item.id === taskId)) {
      taskList.value = currentTaskList.map(item =>
        item.id === taskId ? { ...item, ...taskDetailResponse.data } : item
      );
    }

    const previousActiveTask = activeTask.value;

    activeTask.value = taskDetailResponse.error ? null : (taskDetailResponse.data ?? null);

    if (taskDetailResponse.error) {
      detailError.value = t('page.envops.deployTask.error.detailLoadFailed');

      if (preserveOnError && previousActiveTask?.id === taskId) {
        activeTask.value = previousActiveTask;
      }
    } else {
      detailError.value = '';
    }
  } finally {
    if (requestToken === detailRequestToken.value) {
      detailLoading.value = false;
    }

    if (inFlightToken === detailRequestInFlightToken.value) {
      detailRequestInFlight.value = false;
    }

    syncDetailRefreshTimer();
  }
}

async function loadTaskHosts(taskId: number) {
  const requestToken = ++hostsRequestToken.value;
  const loadingToken = ++hostsLoadingToken.value;
  const params = getDeployTaskHostQuery();

  hostsRequestInFlight.value = true;
  hostsLoading.value = true;

  try {
    const { data, error } = await fetchGetDeployTaskHosts(taskId, params);

    if (requestToken !== hostsRequestToken.value || activeTaskId.value !== taskId) {
      return;
    }

    if (error) {
      hostsError.value = t('page.envops.deployTask.error.hostsLoadFailed');
      return;
    }

    const nextHostsPage = getDeployTaskHostPage(data, params.page ?? DETAIL_PAGE, params.pageSize ?? DETAIL_PAGE_SIZE);

    hostsError.value = '';
    hostsPage.value = nextHostsPage;
    hostQuery.page = nextHostsPage.page;
    hostQuery.pageSize = nextHostsPage.pageSize;
    taskHosts.value = nextHostsPage.records;
  } finally {
    if (loadingToken === hostsLoadingToken.value) {
      hostsLoading.value = false;
    }

    if (requestToken === hostsRequestToken.value) {
      hostsRequestInFlight.value = false;
    }
  }
}

async function loadTaskLogs(taskId: number) {
  const requestToken = ++logsRequestToken.value;
  const loadingToken = ++logsLoadingToken.value;
  const params = getDeployTaskLogQuery();

  logsRequestInFlight.value = true;
  logsLoading.value = true;

  try {
    const { data, error } = await fetchGetDeployTaskLogs(taskId, params);

    if (requestToken !== logsRequestToken.value || activeTaskId.value !== taskId) {
      return;
    }

    if (error) {
      logsError.value = t('page.envops.deployTask.error.logsLoadFailed');
      return;
    }

    const nextLogsPage = getDeployTaskLogPage(data, params.page ?? DETAIL_PAGE, params.pageSize ?? DETAIL_PAGE_SIZE);

    logsError.value = '';
    logsPage.value = nextLogsPage;
    logQuery.page = nextLogsPage.page;
    logQuery.pageSize = nextLogsPage.pageSize;
    taskLogs.value = nextLogsPage.records;
  } finally {
    if (loadingToken === logsLoadingToken.value) {
      logsLoading.value = false;
    }

    if (requestToken === logsRequestToken.value) {
      logsRequestInFlight.value = false;
    }
  }
}

async function loadLogHosts(taskId: number) {
  const requestToken = ++logHostsRequestToken.value;
  const inFlightToken = ++logHostsRequestInFlightToken.value;
  const nextLogHosts: Api.Task.DeployTaskHostRecord[] = [];
  let page = DETAIL_PAGE;

  logHostsRequestInFlight.value = true;

  try {
    while (true) {
      const { data, error } = await fetchGetDeployTaskHosts(taskId, { page, pageSize: LOG_HOST_OPTIONS_PAGE_SIZE });

      if (requestToken !== logHostsRequestToken.value || activeTaskId.value !== taskId) {
        return;
      }

      if (error) {
        return;
      }

      const nextLogHostsPage = getDeployTaskHostPage(data, page, LOG_HOST_OPTIONS_PAGE_SIZE);
      const records = getDeployTaskHostRecords(nextLogHostsPage);

      if (!records.length) {
        break;
      }

      nextLogHosts.push(...records);

      const loadedAllLogHosts = nextLogHostsPage.total > 0 && nextLogHosts.length >= nextLogHostsPage.total;
      const reachedLastLogHostsPage = records.length < nextLogHostsPage.pageSize;

      if (loadedAllLogHosts || reachedLastLogHostsPage) {
        break;
      }

      page = nextLogHostsPage.page + 1;
    }

    if (requestToken === logHostsRequestToken.value && activeTaskId.value === taskId) {
      logHosts.value = nextLogHosts;
    }
  } catch {
    if (requestToken === logHostsRequestToken.value && activeTaskId.value === taskId) {
      logHosts.value = [];
    }
  } finally {
    if (inFlightToken === logHostsRequestInFlightToken.value) {
      logHostsRequestInFlight.value = false;
    }
  }
}

async function refreshActiveTaskSections(options: { refreshList?: boolean } = {}) {
  if (activeTaskId.value === null) {
    return;
  }

  const { refreshList = true } = options;
  const taskId = activeTaskId.value;

  await Promise.all([
    loadTaskDetail(taskId, { refreshList }),
    loadTaskHosts(taskId),
    loadTaskLogs(taskId),
    loadLogHosts(taskId)
  ]);
}

async function pushDeployTaskRouteQuery(partialQuery: Partial<DeployTaskRouteQuery>) {
  const currentQuery = stringifyDeployTaskRouteQuery(normalizedRouteQuery.value);
  const nextQuery = stringifyDeployTaskRouteQuery({ ...normalizedRouteQuery.value, ...partialQuery });

  if (isSameRouteQuery(currentQuery, nextQuery)) {
    return;
  }

  await routerPushByKey('deploy_task', { query: nextQuery });
}

async function handleSearch() {
  const createdRange = getCreatedRangeQueryValue(filterForm.createdRange);

  await pushDeployTaskRouteQuery({
    keyword: filterForm.keyword.trim(),
    status: filterForm.status ?? '',
    taskType: filterForm.taskType ?? '',
    appId: filterForm.appId,
    environment: filterForm.environment ?? '',
    createdFrom: createdRange[0],
    createdTo: createdRange[1],
    page: DEFAULT_DEPLOY_TASK_ROUTE_QUERY.page
  });
}

async function handleResetFilters() {
  filterForm.keyword = '';
  filterForm.status = null;
  filterForm.taskType = null;
  filterForm.appId = null;
  filterForm.environment = null;
  filterForm.createdRange = null;

  await pushDeployTaskRouteQuery({
    keyword: '',
    status: '',
    taskType: '',
    appId: null,
    environment: '',
    createdFrom: '',
    createdTo: '',
    page: DEFAULT_DEPLOY_TASK_ROUTE_QUERY.page
  });
}

async function handleSortByChange(sortBy: Api.Task.TaskSortBy) {
  await pushDeployTaskRouteQuery({ sortBy, page: DEFAULT_DEPLOY_TASK_ROUTE_QUERY.page });
}

async function handleSortOrderChange(sortOrder: Api.Task.TaskSortOrder) {
  await pushDeployTaskRouteQuery({ sortOrder, page: DEFAULT_DEPLOY_TASK_ROUTE_QUERY.page });
}

async function handlePageChange(page: number) {
  await pushDeployTaskRouteQuery({ page });
}

async function handlePageSizeChange(pageSize: number) {
  await pushDeployTaskRouteQuery({ page: DEFAULT_DEPLOY_TASK_ROUTE_QUERY.page, pageSize });
}

async function handleRefreshList() {
  await loadDeployTasks(normalizedRouteQuery.value);
}

async function handleSummaryCardSelect(summaryKey: DeployTaskSummaryKey) {
  if (summaryKey === 'failedIn24h') {
    const now = Date.now();
    const [createdFrom, createdTo] = formatLocalDateTimeRange([now - SUMMARY_FAILED_RANGE_HOURS * 60 * 60 * 1000, now]);

    await pushDeployTaskRouteQuery({
      status: 'FAILED',
      createdFrom,
      createdTo,
      page: DEFAULT_DEPLOY_TASK_ROUTE_QUERY.page
    });
    return;
  }

  const summaryQueryByKey: Record<Exclude<DeployTaskSummaryKey, 'failedIn24h'>, Partial<DeployTaskRouteQuery>> = {
    pendingApproval: {
      status: 'PENDING_APPROVAL',
      createdFrom: '',
      createdTo: ''
    },
    inProgress: {
      status: 'RUNNING',
      createdFrom: '',
      createdTo: ''
    }
  };

  await pushDeployTaskRouteQuery({
    ...summaryQueryByKey[summaryKey],
    page: DEFAULT_DEPLOY_TASK_ROUTE_QUERY.page
  });
}

function handleSummaryCardKeydown(event: KeyboardEvent, summaryKey: DeployTaskSummaryKey) {
  if (event.key !== 'Enter' && event.key !== ' ') {
    return;
  }

  event.preventDefault();
  void handleSummaryCardSelect(summaryKey);
}

async function handleOpenTaskDetail(taskId: number) {
  await pushDeployTaskRouteQuery({ taskId });
}

async function handleOpenCreateDrawer() {
  resetCreateForm();
  createDrawerVisible.value = true;
  await Promise.all([loadCreateVersions(createForm.appId), loadCreateHosts(createForm.environment)]);
}

function handleCreateDrawerVisibleChange(show: boolean) {
  createDrawerVisible.value = show;

  if (!show) {
    resetCreateForm();
    appVersions.value = [];
    createHosts.value = [];
  }
}

function buildCreateTaskPayload(): Api.Task.CreateDeployTaskPayload {
  const normalizedBatchSize = createForm.batchStrategy === 'ROLLING' ? Number(createForm.batchSize) : null;
  const normalizedSshPort = createForm.sshPort === null ? null : Number(createForm.sshPort);

  const payload: Api.Task.CreateDeployTaskPayload = {
    taskName: createForm.taskName.trim(),
    taskType: createForm.taskType,
    appId: createForm.appId!,
    versionId: createForm.versionId!,
    environment: createForm.environment,
    hostIds: [...createForm.hostIds],
    batchStrategy: createForm.batchStrategy,
    batchSize: normalizedBatchSize,
    sshUser: createForm.sshUser.trim(),
    sshPort: normalizedSshPort,
    privateKeyPath: createForm.privateKeyPath.trim(),
    remoteBaseDir: createForm.remoteBaseDir.trim(),
    rollbackCommand: createForm.rollbackCommand.trim() || null
  };

  return payload;
}

function validateCreateForm() {
  if (!createForm.taskName.trim()) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.taskNameRequired'));
    return false;
  }

  if (createForm.appId === null) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.appRequired'));
    return false;
  }

  if (createForm.versionId === null) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.versionRequired'));
    return false;
  }

  if (!createForm.environment.trim()) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.environmentRequired'));
    return false;
  }

  if (!createForm.hostIds.length) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.hostsRequired'));
    return false;
  }

  if (!createForm.sshUser.trim()) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.sshUserRequired'));
    return false;
  }

  const normalizedSshPort = Number(createForm.sshPort);

  if (createForm.sshPort !== null && (!Number.isFinite(normalizedSshPort) || normalizedSshPort <= 0)) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.sshPortInvalid'));
    return false;
  }

  if (!createForm.privateKeyPath.trim()) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.privateKeyPathRequired'));
    return false;
  }

  if (!createForm.remoteBaseDir.trim()) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.remoteBaseDirRequired'));
    return false;
  }

  const normalizedBatchSize = Number(createForm.batchSize);

  if (createForm.batchStrategy === 'ROLLING' && (!Number.isFinite(normalizedBatchSize) || normalizedBatchSize <= 0)) {
    window.$message?.warning(t('page.envops.deployTask.create.validation.batchSizeRequired'));
    return false;
  }

  return true;
}

async function handleCreateTask() {
  if (createSubmitting.value || !validateCreateForm()) {
    return;
  }

  createSubmitting.value = true;

  try {
    const { data, error } = await fetchPostCreateDeployTask(buildCreateTaskPayload());

    if (!error) {
      window.$message?.success(t('common.addSuccess'));
      createDrawerVisible.value = false;
      resetCreateForm();
      appVersions.value = [];
      createHosts.value = [];
      await loadDeployTasks(normalizedRouteQuery.value);

      if (data?.id) {
        await pushDeployTaskRouteQuery({ taskId: data.id });
      }
    }
  } finally {
    createSubmitting.value = false;
  }
}

function openApprovalDrawer(taskId: number, action: DeployTaskApprovalAction) {
  resetApprovalForm();
  approvalForm.taskId = taskId;
  approvalForm.action = action;
  approvalDrawerVisible.value = true;
}

function handleApprovalDrawerVisibleChange(show: boolean) {
  approvalDrawerVisible.value = show;

  if (!show) {
    resetApprovalForm();
  }
}

async function handleSubmitApproval() {
  if (approvalSubmitting.value || approvalForm.taskId === null || isTaskMutating(approvalForm.taskId)) {
    return;
  }

  const taskId = approvalForm.taskId;
  const action = approvalForm.action;

  actionLoadingTaskIds.value = [...actionLoadingTaskIds.value, taskId];
  actionLoadingActions.value[taskId] = action;
  approvalSubmitting.value = true;

  try {
    const payload = approvalForm.comment.trim() ? { comment: approvalForm.comment.trim() } : undefined;
    const response =
      action === 'approve'
        ? await fetchPostApproveDeployTask(taskId, payload)
        : await fetchPostRejectDeployTask(taskId, payload);

    if (!response.error) {
      window.$message?.success(t('common.updateSuccess'));
      approvalDrawerVisible.value = false;
      resetApprovalForm();
      await refreshAfterTaskAction(response.data?.id ?? taskId);
    }
  } finally {
    approvalSubmitting.value = false;
    actionLoadingTaskIds.value = actionLoadingTaskIds.value.filter(id => id !== taskId);
    actionLoadingActions.value[taskId] = undefined;
  }
}

async function handleDetailDrawerVisibleChange(show: boolean) {
  if (!show) {
    stopDetailRefreshTimer();
    await pushDeployTaskRouteQuery({ taskId: null });
  }
}

async function handleManualRefresh() {
  await refreshActiveTaskSections({ refreshList: true });
}

async function handleHostsSearch() {
  hostQuery.page = DETAIL_PAGE;

  if (activeTaskId.value !== null) {
    await loadTaskHosts(activeTaskId.value);
  }
}

async function handleHostsReset() {
  resetHostQuery();
  hostsPage.value = createEmptyDeployTaskHostPage(hostQuery.page, hostQuery.pageSize);
  hostsError.value = '';

  if (activeTaskId.value !== null) {
    await loadTaskHosts(activeTaskId.value);
  }
}

async function handleHostsPageChange(page: number) {
  hostQuery.page = page;

  if (activeTaskId.value !== null) {
    await loadTaskHosts(activeTaskId.value);
  }
}

async function handleHostsPageSizeChange(pageSize: number) {
  hostQuery.page = DETAIL_PAGE;
  hostQuery.pageSize = pageSize;

  if (activeTaskId.value !== null) {
    await loadTaskHosts(activeTaskId.value);
  }
}

async function handleLogsSearch() {
  logQuery.page = DETAIL_PAGE;

  if (activeTaskId.value !== null) {
    await loadTaskLogs(activeTaskId.value);
  }
}

async function handleLogsReset() {
  resetLogQuery();
  logsPage.value = createEmptyDeployTaskLogPage(logQuery.page, logQuery.pageSize);
  logsError.value = '';

  if (activeTaskId.value !== null) {
    await loadTaskLogs(activeTaskId.value);
  }
}

async function handleLogsPageChange(page: number) {
  logQuery.page = page;

  if (activeTaskId.value !== null) {
    await loadTaskLogs(activeTaskId.value);
  }
}

async function handleLogsPageSizeChange(pageSize: number) {
  logQuery.page = DETAIL_PAGE;
  logQuery.pageSize = pageSize;

  if (activeTaskId.value !== null) {
    await loadTaskLogs(activeTaskId.value);
  }
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
    actionLoadingActions.value[taskId] = undefined;
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
    actionLoadingActions.value[taskId] = undefined;
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
    actionLoadingActions.value[taskId] = undefined;
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
    actionLoadingActions.value[taskId] = undefined;
  }
}

async function refreshAfterTaskAction(taskId: number, openDetail = false) {
  await loadDeployTasks(normalizedRouteQuery.value);

  if (openDetail) {
    await pushDeployTaskRouteQuery({ taskId });
    return;
  }

  const shouldRefreshDetail = detailDrawerVisible.value && activeTaskId.value === taskId;

  if (shouldRefreshDetail) {
    await refreshActiveTaskSections({ refreshList: false });
  }
}

function isActiveTaskRow(taskId: number) {
  return taskId === activeTaskId.value;
}

function stringifyDeployTaskRouteQuery(query: DeployTaskRouteQuery) {
  return {
    ...(query.status ? { status: query.status } : {}),
    ...(query.taskType ? { taskType: query.taskType } : {}),
    ...(query.appId !== null ? { appId: String(query.appId) } : {}),
    ...(query.environment ? { environment: query.environment } : {}),
    ...(query.keyword ? { keyword: query.keyword } : {}),
    ...(query.createdFrom ? { createdFrom: query.createdFrom } : {}),
    ...(query.createdTo ? { createdTo: query.createdTo } : {}),
    page: String(query.page),
    pageSize: String(query.pageSize),
    sortBy: query.sortBy,
    sortOrder: query.sortOrder,
    ...(query.taskId !== null ? { taskId: String(query.taskId) } : {})
  };
}

function prepareTaskDetailState(taskId: number) {
  detailRequestToken.value++;
  activeTaskId.value = taskId;
  activeTask.value = null;
  taskHosts.value = [];
  logHosts.value = [];
  taskLogs.value = [];
  hostsPage.value = createEmptyDeployTaskHostPage();
  logsPage.value = createEmptyDeployTaskLogPage();
  detailError.value = '';
  hostsError.value = '';
  logsError.value = '';
  activeDetailTab.value = 'overview';
  resetHostQuery();
  resetLogQuery();
  stopDetailRefreshTimer();
}

function resetHostQuery() {
  hostQuery.status = null;
  hostQuery.keyword = '';
  hostQuery.page = DETAIL_PAGE;
  hostQuery.pageSize = DETAIL_PAGE_SIZE;
}

function resetLogQuery() {
  logQuery.hostId = null;
  logQuery.keyword = '';
  logQuery.page = DETAIL_PAGE;
  logQuery.pageSize = DETAIL_PAGE_SIZE;
}

function getDeployTaskHostQuery(): Api.Task.DeployTaskHostQuery {
  return {
    ...(hostQuery.status ? { status: hostQuery.status } : {}),
    ...(hostQuery.keyword.trim() ? { keyword: hostQuery.keyword.trim() } : {}),
    page: hostQuery.page,
    pageSize: hostQuery.pageSize
  };
}

function getDeployTaskLogQuery(): Api.Task.DeployTaskLogQuery {
  return {
    ...(logQuery.hostId !== null ? { hostId: logQuery.hostId } : {}),
    ...(logQuery.keyword.trim() ? { keyword: logQuery.keyword.trim() } : {}),
    page: logQuery.page,
    pageSize: logQuery.pageSize
  };
}

function shouldAutoRefreshDetail() {
  if (!detailDrawerVisible.value || activeTaskId.value === null) {
    return false;
  }

  if (document.visibilityState !== 'visible') {
    return false;
  }

  return isPollingDeployTaskStatus(activeTask.value?.status);
}

function startDetailRefreshTimer() {
  if (detailRefreshTimer.value !== null) {
    return;
  }

  detailRefreshTimer.value = window.setInterval(() => {
    void handleDetailRefreshTimerTick();
  }, DETAIL_REFRESH_INTERVAL);
}

function stopDetailRefreshTimer() {
  if (detailRefreshTimer.value !== null) {
    window.clearInterval(detailRefreshTimer.value);
    detailRefreshTimer.value = null;
  }
}

function syncDetailRefreshTimer() {
  if (shouldAutoRefreshDetail()) {
    startDetailRefreshTimer();
    return;
  }

  stopDetailRefreshTimer();
}

async function handleDetailRefreshTimerTick() {
  if (!shouldAutoRefreshDetail()) {
    stopDetailRefreshTimer();
    return;
  }

  if (
    detailRequestInFlight.value ||
    hostsRequestInFlight.value ||
    logHostsRequestInFlight.value ||
    logsRequestInFlight.value
  ) {
    return;
  }

  const hadLoadError = Boolean(detailError.value || hostsError.value || logsError.value);

  await refreshActiveTaskSections({ refreshList: true });

  if (!hadLoadError && (detailError.value || hostsError.value || logsError.value)) {
    window.$message?.warning(t('page.envops.deployTask.error.autoRefreshFailed'));
  }
}

function handleDocumentVisibilityChange() {
  if (document.visibilityState === 'visible' && detailDrawerVisible.value) {
    void handleDetailRefreshTimerTick();
  }

  syncDetailRefreshTimer();
}

function createEmptyDeployTaskHostPage(page = DETAIL_PAGE, pageSize = DETAIL_PAGE_SIZE): Api.Task.DeployTaskHostPage {
  return {
    page,
    pageSize,
    total: 0,
    records: []
  };
}

function createEmptyDeployTaskLogPage(page = DETAIL_PAGE, pageSize = DETAIL_PAGE_SIZE): Api.Task.DeployTaskLogPage {
  return {
    page,
    pageSize,
    total: 0,
    records: []
  };
}

function getCreatedRangeValue(createdFrom: string, createdTo: string): [number, number] | null {
  if (!createdFrom || !createdTo) {
    return null;
  }

  const start = new Date(createdFrom).getTime();
  const end = new Date(createdTo).getTime();

  if (Number.isNaN(start) || Number.isNaN(end)) {
    return null;
  }

  return [start, end];
}

function getCreatedRangeQueryValue(createdRange: [number, number] | null): [string, string] {
  return formatLocalDateTimeRange(createdRange);
}

function isSameRouteQuery(left: Record<string, string>, right: Record<string, string>) {
  const leftKeys = Object.keys(left);
  const rightKeys = Object.keys(right);

  if (leftKeys.length !== rightKeys.length) {
    return false;
  }

  return leftKeys.every(key => left[key] === right[key]);
}

function getDeployTaskPage(
  data: Api.Task.DeployTaskPage | null | undefined,
  query: DeployTaskRouteQuery
): Api.Task.DeployTaskPage {
  return (
    data ?? {
      page: query.page,
      pageSize: query.pageSize,
      total: 0,
      records: []
    }
  );
}

function getDeployTaskHostPage(
  data: Api.Task.DeployTaskHostPage | null | undefined,
  page: number,
  pageSize: number
): Api.Task.DeployTaskHostPage {
  return data ?? createEmptyDeployTaskHostPage(page, pageSize);
}

function getDeployTaskHostRecords(data: Api.Task.DeployTaskHostPage | null | undefined) {
  return Array.isArray(data?.records) ? data.records : [];
}

function getDeployTaskLogPage(
  data: Api.Task.DeployTaskLogPage | null | undefined,
  page: number,
  pageSize: number
): Api.Task.DeployTaskLogPage {
  return data ?? createEmptyDeployTaskLogPage(page, pageSize);
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

function canCancelTask(status: string | null | undefined) {
  const normalizedStatus = String(status || '')
    .trim()
    .toUpperCase();

  return normalizedStatus === 'RUNNING' || normalizedStatus === 'PENDING';
}

function canApproveOrRejectTask(statusKey: DeployTaskStatusKey) {
  return statusKey === 'pendingApproval';
}

function itemTaskTypeAllowsRollback(taskType: string | null | undefined) {
  return taskType !== 'ROLLBACK';
}

function isPollingDeployTaskStatus(status: string | null | undefined) {
  const normalizedStatus = String(status || '')
    .trim()
    .toUpperCase();

  return normalizedStatus === 'RUNNING' || normalizedStatus === 'CANCEL_REQUESTED';
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

  if (
    normalizedStatus.includes('fail') ||
    normalizedStatus.includes('error') ||
    normalizedStatus.includes('rollback')
  ) {
    return 'failed';
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

function getEnvironmentLabel(environment: string | null | undefined) {
  const rawValue = String(environment || '').trim();

  if (!rawValue) {
    return '-';
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

  return getEnvironmentLabel(rawValue);
}

function getDeployTaskBatch(item: Api.Task.DeployTaskRecord) {
  const strategy = String(item.batchStrategy || '')
    .trim()
    .toUpperCase();

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
  void loadApps();
  document.addEventListener('visibilitychange', handleDocumentVisibilityChange);
});

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', handleDocumentVisibilityChange);
  stopDetailRefreshTimer();
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
        <NSpace>
          <NButton type="primary" @click="handleOpenCreateDrawer">
            {{ t('page.envops.deployTask.actions.create') }}
          </NButton>
          <NButton secondary :loading="loading" @click="handleRefreshList">
            {{ t('common.refresh') }}
          </NButton>
        </NSpace>
      </div>
    </NCard>

    <NGrid cols="1 s:3" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in metrics" :key="item.key">
        <NCard
          :bordered="false"
          class="card-wrapper deploy-task-summary-card"
          role="button"
          tabindex="0"
          :data-summary-key="item.key"
          @click="handleSummaryCardSelect(item.key)"
          @keydown="handleSummaryCardKeydown($event, item.key)"
        >
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NCard :title="t('page.envops.deployTask.table.title')" :bordered="false" class="card-wrapper">
      <NSpace vertical :size="12" class="mb-16px">
        <NSpace wrap>
          <NInput
            v-model:value="filterForm.keyword"
            clearable
            class="w-240px"
            :placeholder="t('page.envops.deployTask.filters.keyword')"
            @keyup.enter="handleSearch"
          />
          <NSelect
            v-model:value="filterForm.status"
            clearable
            class="w-180px"
            :options="statusOptions"
            :placeholder="t('page.envops.deployTask.filters.status')"
          />
          <NSelect
            v-model:value="filterForm.taskType"
            clearable
            class="w-180px"
            :options="taskTypeOptions"
            :placeholder="t('page.envops.deployTask.filters.taskType')"
          />
          <NSelect
            v-model:value="filterForm.appId"
            clearable
            class="w-220px"
            :options="appOptions"
            :placeholder="t('page.envops.deployTask.filters.application')"
          />
          <NSelect
            v-model:value="filterForm.environment"
            clearable
            class="w-180px"
            :options="environmentOptions"
            :placeholder="t('page.envops.deployTask.filters.environment')"
          />
          <NDatePicker
            v-model:value="filterForm.createdRange"
            clearable
            class="w-320px"
            type="datetimerange"
            :start-placeholder="t('page.envops.deployTask.filters.createdRange')"
            :end-placeholder="t('page.envops.deployTask.filters.createdRange')"
          />
          <NButton type="primary" @click="handleSearch">
            {{ t('page.envops.deployTask.filters.search') }}
          </NButton>
          <NButton @click="handleResetFilters">
            {{ t('page.envops.deployTask.filters.reset') }}
          </NButton>
        </NSpace>

        <NSpace wrap>
          <NSelect
            :value="normalizedRouteQuery.sortBy"
            class="w-180px"
            :options="sortByOptions"
            :placeholder="t('page.envops.deployTask.sorting.status')"
            @update:value="handleSortByChange"
          />
          <NSelect
            :value="normalizedRouteQuery.sortOrder"
            class="w-160px"
            :options="sortOrderOptions"
            placeholder="DESC / ASC"
            @update:value="handleSortOrderChange"
          />
        </NSpace>
      </NSpace>

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
            <tr
              v-for="item in deployTasks"
              :key="item.key"
              :data-task-id="item.key"
              :data-active="isActiveTaskRow(item.key)"
              :class="{ 'deploy-task-row--active': isActiveTaskRow(item.key) }"
            >
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
                    type="success"
                    size="small"
                    :disabled="isTaskMutating(item.key) || !canApproveOrRejectTask(item.statusKey)"
                    :loading="isActionLoading('approve', item.key)"
                    @click="openApprovalDrawer(item.key, 'approve')"
                  >
                    {{ t('page.envops.deployTask.actions.approve') }}
                  </NButton>
                  <NButton
                    text
                    type="error"
                    size="small"
                    :disabled="isTaskMutating(item.key) || !canApproveOrRejectTask(item.statusKey)"
                    :loading="isActionLoading('reject', item.key)"
                    @click="openApprovalDrawer(item.key, 'reject')"
                  >
                    {{ t('page.envops.deployTask.actions.reject') }}
                  </NButton>
                  <NButton
                    text
                    type="warning"
                    size="small"
                    :disabled="isTaskMutating(item.key) || !canCancelTask(item.rawStatus)"
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

    <NDrawer :show="createDrawerVisible" :width="520" placement="right" @update:show="handleCreateDrawerVisibleChange">
      <NDrawerContent :title="t('page.envops.deployTask.create.title')" closable>
        <NSpace vertical :size="16">
          <NForm label-placement="top">
            <NFormItem :label="t('page.envops.deployTask.create.taskName')">
              <NInput
                v-model:value="createForm.taskName"
                :placeholder="t('page.envops.deployTask.create.taskNamePlaceholder')"
              />
            </NFormItem>
            <NGrid cols="1 s:2" responsive="screen" :x-gap="12" :y-gap="12">
              <NGi>
                <NFormItem :label="t('page.envops.deployTask.create.taskType')">
                  <NSelect v-model:value="createForm.taskType" :options="createTaskTypeOptions" />
                </NFormItem>
              </NGi>
              <NGi>
                <NFormItem :label="t('page.envops.deployTask.create.environment')">
                  <NSelect v-model:value="createForm.environment" :options="environmentOptions" />
                </NFormItem>
              </NGi>
              <NGi>
                <NFormItem :label="t('page.envops.deployTask.create.app')">
                  <NSelect
                    v-model:value="createForm.appId"
                    clearable
                    filterable
                    :options="appOptions"
                    :placeholder="t('page.envops.deployTask.create.appPlaceholder')"
                  />
                </NFormItem>
              </NGi>
              <NGi>
                <NFormItem :label="t('page.envops.deployTask.create.version')">
                  <NSelect
                    v-model:value="createForm.versionId"
                    clearable
                    filterable
                    :loading="createVersionsLoading"
                    :disabled="createForm.appId === null"
                    :options="createVersionOptions"
                    :placeholder="t('page.envops.deployTask.create.versionPlaceholder')"
                  />
                </NFormItem>
              </NGi>
              <NGi>
                <NFormItem :label="t('page.envops.deployTask.create.batchStrategy')">
                  <NSelect v-model:value="createForm.batchStrategy" :options="createBatchStrategyOptions" />
                </NFormItem>
              </NGi>
              <NGi>
                <NFormItem :label="t('page.envops.deployTask.create.batchSize')">
                  <NInputNumber
                    v-model:value="createForm.batchSize"
                    clearable
                    :min="1"
                    :disabled="createForm.batchStrategy !== 'ROLLING'"
                    :placeholder="t('page.envops.deployTask.create.batchSizePlaceholder')"
                    class="w-full"
                  />
                </NFormItem>
              </NGi>
            </NGrid>
            <NGrid cols="1 s:2" responsive="screen" :x-gap="12" :y-gap="12">
              <NGi>
                <NFormItem :label="t('page.envops.deployTask.create.sshUser')">
                  <NInput
                    v-model:value="createForm.sshUser"
                    :placeholder="t('page.envops.deployTask.create.sshUserPlaceholder')"
                  />
                </NFormItem>
              </NGi>
              <NGi>
                <NFormItem :label="t('page.envops.deployTask.create.sshPort')">
                  <NInputNumber
                    v-model:value="createForm.sshPort"
                    clearable
                    :min="1"
                    :placeholder="t('page.envops.deployTask.create.sshPortPlaceholder')"
                    class="w-full"
                  />
                </NFormItem>
              </NGi>
            </NGrid>
            <NFormItem :label="t('page.envops.deployTask.create.privateKeyPath')">
              <NInput
                v-model:value="createForm.privateKeyPath"
                :placeholder="t('page.envops.deployTask.create.privateKeyPathPlaceholder')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.deployTask.create.remoteBaseDir')">
              <NInput
                v-model:value="createForm.remoteBaseDir"
                :placeholder="t('page.envops.deployTask.create.remoteBaseDirPlaceholder')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.deployTask.create.rollbackCommand')">
              <NInput
                v-model:value="createForm.rollbackCommand"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 5 }"
                :placeholder="t('page.envops.deployTask.create.rollbackCommandPlaceholder')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.deployTask.create.hosts')">
              <NSelect
                v-model:value="createForm.hostIds"
                multiple
                clearable
                filterable
                :loading="createHostsLoading"
                :options="createHostOptions"
                :placeholder="t('page.envops.deployTask.create.hostsPlaceholder')"
              />
            </NFormItem>
          </NForm>
          <NSpace justify="end">
            <NButton @click="handleCreateDrawerVisibleChange(false)">
              {{ t('common.cancel') }}
            </NButton>
            <NButton type="primary" :loading="createSubmitting" @click="handleCreateTask">
              {{ t('page.envops.deployTask.actions.create') }}
            </NButton>
          </NSpace>
        </NSpace>
      </NDrawerContent>
    </NDrawer>

    <NDrawer
      :show="approvalDrawerVisible"
      :width="420"
      placement="right"
      @update:show="handleApprovalDrawerVisibleChange"
    >
      <NDrawerContent :title="approvalDrawerTitle" closable>
        <NSpace vertical :size="16">
          <NForm label-placement="top">
            <NFormItem :label="t('page.envops.deployTask.approval.comment')">
              <NInput
                v-model:value="approvalForm.comment"
                type="textarea"
                :autosize="{ minRows: 4, maxRows: 8 }"
                :placeholder="t('page.envops.deployTask.approval.commentPlaceholder')"
              />
            </NFormItem>
          </NForm>
          <NSpace justify="end">
            <NButton @click="handleApprovalDrawerVisibleChange(false)">
              {{ t('common.cancel') }}
            </NButton>
            <NButton type="primary" :loading="approvalSubmitting" @click="handleSubmitApproval">
              {{ approvalSubmitText }}
            </NButton>
          </NSpace>
        </NSpace>
      </NDrawerContent>
    </NDrawer>

    <NDrawer :show="detailDrawerVisible" :width="960" placement="right" @update:show="handleDetailDrawerVisibleChange">
      <NDrawerContent :title="t('page.envops.deployTask.detail.title')" closable>
        <NSpace vertical :size="16">
          <NSpace justify="end">
            <NButton
              text
              :loading="detailRequestInFlight || hostsRequestInFlight || logHostsRequestInFlight || logsRequestInFlight"
              @click="handleManualRefresh"
            >
              {{ t('page.envops.deployTask.detail.manualRefresh') }}
            </NButton>
          </NSpace>

          <NTabs v-model:value="activeDetailTab" type="segment" animated>
            <NTabPane name="overview" :tab="t('page.envops.deployTask.tabs.overview')">
              <NSpin :show="detailLoading && !activeTask">
                <NSpace vertical :size="16">
                  <NAlert v-if="detailError" type="error">
                    {{ detailError }}
                  </NAlert>

                  <template v-if="activeTask">
                    <NCard :bordered="false" embedded>
                      <NGrid cols="2 s:3" responsive="screen" :x-gap="12" :y-gap="12">
                        <NGi v-for="item in progressSummary" :key="item.key">
                          <NStatistic :label="item.label" :value="item.value" />
                        </NGi>
                      </NGrid>
                    </NCard>

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
                  </template>

                  <NEmpty v-else class="py-24px" :description="t('page.envops.deployTask.empty.taskNotFound')" />
                </NSpace>
              </NSpin>
            </NTabPane>

            <NTabPane name="hosts" :tab="t('page.envops.deployTask.tabs.hosts')">
              <NSpace vertical :size="16">
                <NSpace wrap>
                  <NSelect
                    v-model:value="hostQuery.status"
                    clearable
                    class="w-180px"
                    :options="hostStatusOptions"
                    :placeholder="t('page.envops.deployTask.hosts.status')"
                  />
                  <NInput
                    v-model:value="hostQuery.keyword"
                    clearable
                    class="w-240px"
                    :placeholder="t('page.envops.deployTask.filters.keyword')"
                    @keyup.enter="handleHostsSearch"
                  />
                  <NButton type="primary" @click="handleHostsSearch">
                    {{ t('page.envops.deployTask.filters.search') }}
                  </NButton>
                  <NButton @click="handleHostsReset">
                    {{ t('page.envops.deployTask.filters.reset') }}
                  </NButton>
                </NSpace>

                <NAlert v-if="hostsError" type="error">
                  {{ hostsError }}
                </NAlert>

                <NSpin :show="hostsLoading">
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
                        <td>
                          <NTag :type="getDeployTaskTagType(getDeployTaskStatusKey(host.status))" size="small">
                            {{ getDeployTaskStatusLabel(getDeployTaskStatusKey(host.status)) }}
                          </NTag>
                        </td>
                        <td>{{ formatTextValue(host.currentStep) }}</td>
                        <td>{{ formatDateTime(host.startedAt) }}</td>
                        <td>{{ formatDateTime(host.finishedAt) }}</td>
                        <td>{{ formatTextValue(host.errorMsg) }}</td>
                      </tr>
                    </tbody>
                  </NTable>
                  <NEmpty v-else class="py-24px" :description="t('page.envops.deployTask.empty.noHosts')" />
                </NSpin>

                <div v-if="hostsPage.total > 0" class="flex justify-end">
                  <NPagination
                    :page="hostQuery.page"
                    :page-size="hostQuery.pageSize"
                    :item-count="hostsPage.total"
                    :page-sizes="[10, 20, 50]"
                    show-size-picker
                    @update:page="handleHostsPageChange"
                    @update:page-size="handleHostsPageSizeChange"
                  />
                </div>
              </NSpace>
            </NTabPane>

            <NTabPane name="logs" :tab="t('page.envops.deployTask.tabs.logs')">
              <NSpace vertical :size="16">
                <NSpace wrap>
                  <NSelect
                    v-model:value="logQuery.hostId"
                    clearable
                    class="w-220px"
                    :options="logHostOptions"
                    :placeholder="t('page.envops.deployTask.logs.taskHostId')"
                  />
                  <NInput
                    v-model:value="logQuery.keyword"
                    clearable
                    class="w-280px"
                    :placeholder="t('page.envops.deployTask.filters.keyword')"
                    @keyup.enter="handleLogsSearch"
                  />
                  <NButton type="primary" @click="handleLogsSearch">
                    {{ t('page.envops.deployTask.filters.search') }}
                  </NButton>
                  <NButton @click="handleLogsReset">
                    {{ t('page.envops.deployTask.filters.reset') }}
                  </NButton>
                </NSpace>

                <NAlert v-if="logsError" type="error">
                  {{ logsError }}
                </NAlert>

                <NSpin :show="logsLoading">
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
                  <NEmpty v-else class="py-24px" :description="t('page.envops.deployTask.empty.noLogs')" />
                </NSpin>

                <div v-if="logsPage.total > 0" class="flex justify-end">
                  <NPagination
                    :page="logQuery.page"
                    :page-size="logQuery.pageSize"
                    :item-count="logsPage.total"
                    :page-sizes="[10, 20, 50]"
                    show-size-picker
                    @update:page="handleLogsPageChange"
                    @update:page-size="handleLogsPageSizeChange"
                  />
                </div>
              </NSpace>
            </NTabPane>
          </NTabs>
        </NSpace>
      </NDrawerContent>
    </NDrawer>
  </NSpace>
</template>

<style scoped>
.deploy-task-summary-card {
  cursor: pointer;
}

.deploy-task-summary-card:focus-visible {
  outline: 2px solid rgb(24 160 88 / 70%);
  outline-offset: 2px;
}

.deploy-task-row--active {
  background-color: rgb(24 160 88 / 10%);
}

.deploy-task-row--active > td {
  background-color: inherit;
}
</style>

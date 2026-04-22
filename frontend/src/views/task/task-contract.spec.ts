import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createApp, defineComponent, h, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { formatLocalDateTimeRange, normalizeTaskCenterRouteQuery, toTaskCenterApiQuery } from './shared/query';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const deployTaskPage = readFileSync(path.resolve(__dirname, '../deploy/task/index.vue'), 'utf8');
const taskCenterPage = readFileSync(path.resolve(__dirname, 'center/index.vue'), 'utf8');
const taskApiSource = readFileSync(path.resolve(__dirname, '../../service/api/task.ts'), 'utf8');
const taskTypingSource = readFileSync(path.resolve(__dirname, '../../typings/api/task.d.ts'), 'utf8');
const appTypingSource = readFileSync(path.resolve(__dirname, '../../typings/app.d.ts'), 'utf8');
const apiIndexSource = readFileSync(path.resolve(__dirname, '../../service/api/index.ts'), 'utf8');
const zhLocaleSource = readFileSync(path.resolve(__dirname, '../../locales/langs/zh-cn.ts'), 'utf8');
const enLocaleSource = readFileSync(path.resolve(__dirname, '../../locales/langs/en-us.ts'), 'utf8');

function extractSection(source: string, startKey: string, nextKey: string) {
  const match = source.match(new RegExp(`${startKey}:\\s*\\{([\\s\\S]*?)${nextKey}:\\s*\\{`, 's'));
  return match?.[1] ?? '';
}

const mocks = vi.hoisted(() => {
  const route = { query: {} as Record<string, unknown> };
  const routerPushByKey = vi.fn(async (_key: string, payload?: { query?: Record<string, string> }) => {
    mocks.route.query = payload?.query ?? {};
  });
  const routerPush = vi.fn(async (_to: string) => undefined);
  const fetchGetApps = vi.fn();
  const fetchGetDeployTasks = vi.fn();
  const fetchGetDeployTask = vi.fn();
  const fetchGetDeployTaskHosts = vi.fn();
  const fetchGetDeployTaskLogs = vi.fn();
  const fetchGetTaskCenterTasks = vi.fn();
  const fetchGetTaskCenterTaskDetail = vi.fn();
  const fetchGetAppVersions = vi.fn();
  const fetchGetAssetHosts = vi.fn();
  const fetchPostCreateDeployTask = vi.fn();
  const fetchPostApproveDeployTask = vi.fn();
  const fetchPostRejectDeployTask = vi.fn();
  const fetchPostExecuteDeployTask = vi.fn();
  const fetchPostRetryDeployTask = vi.fn();
  const fetchPostRollbackDeployTask = vi.fn();
  const fetchPostCancelDeployTask = vi.fn();
  const warning = vi.fn();
  const success = vi.fn();

  return {
    route,
    routerPushByKey,
    routerPush,
    fetchGetApps,
    fetchGetDeployTasks,
    fetchGetDeployTask,
    fetchGetDeployTaskHosts,
    fetchGetDeployTaskLogs,
    fetchGetTaskCenterTasks,
    fetchGetTaskCenterTaskDetail,
    fetchGetAppVersions,
    fetchGetAssetHosts,
    fetchPostCreateDeployTask,
    fetchPostApproveDeployTask,
    fetchPostRejectDeployTask,
    fetchPostExecuteDeployTask,
    fetchPostRetryDeployTask,
    fetchPostRollbackDeployTask,
    fetchPostCancelDeployTask,
    warning,
    success
  };
});

vi.mock('vue-router', async () => {
  const { reactive } = await import('vue');
  const route = reactive(mocks.route);
  mocks.route = route;

  return {
    useRoute: () => route,
    useRouter: () => ({
      push: mocks.routerPush
    })
  };
});

const passthroughStub = defineComponent({
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () => h('div', attrs, slots.default?.());
  }
});

const drawerStub = defineComponent({
  inheritAttrs: false,
  props: {
    show: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:show'],
  setup(props, { attrs, slots, emit }) {
    return () =>
      props.show
        ? h('div', { ...attrs, 'data-drawer': 'true' }, [
            h(
              'button',
              {
                type: 'button',
                'data-drawer-close': 'true',
                onClick: () => emit('update:show', false)
              },
              'drawer-close'
            ),
            ...(slots.default?.() ?? [])
          ])
        : null;
  }
});

const buttonStub = defineComponent({
  inheritAttrs: false,
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  emits: ['click'],
  setup(props, { attrs, slots, emit }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          disabled: props.disabled,
          'data-loading': String(props.loading),
          onClick: (event: MouseEvent) => emit('click', event)
        },
        slots.default?.()
      );
  }
});

const inputStub = defineComponent({
  inheritAttrs: false,
  props: {
    value: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () =>
      h('input', {
        ...attrs,
        value: props.value ?? '',
        onInput: (event: Event) => emit('update:value', (event.target as HTMLInputElement).value)
      });
  }
});

const inputNumberStub = defineComponent({
  inheritAttrs: false,
  props: {
    value: {
      type: [Number, String, null],
      default: null
    }
  },
  emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () =>
      h('input', {
        ...attrs,
        type: 'number',
        value: props.value ?? '',
        onInput: (event: Event) => {
          const value = (event.target as HTMLInputElement).value;
          emit('update:value', value === '' ? null : Number(value));
        }
      });
  }
});

const selectStub = defineComponent({
  inheritAttrs: false,
  props: {
    value: {
      type: [String, Number, Array, null],
      default: null
    },
    options: {
      type: Array,
      default: () => []
    },
    multiple: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () =>
      h(
        'select',
        {
          ...attrs,
          multiple: props.multiple,
          value: props.multiple ? undefined : (props.value ?? ''),
          onChange: (event: Event) => {
            const target = event.target as HTMLSelectElement;

            if (props.multiple) {
              const values = Array.from(target.selectedOptions).map(option => {
                const matched = props.options.find((item: any) => String(item.value) === option.value) as any;
                return matched ? matched.value : option.value;
              });
              emit('update:value', values);
              return;
            }

            const matched = props.options.find((item: any) => String(item.value) === target.value) as any;
            emit('update:value', matched ? matched.value : target.value);
          }
        },
        props.options.map((option: any) => h('option', { value: String(option.value) }, option.label))
      );
  }
});

const tableStub = defineComponent({
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () => h('table', attrs, slots.default?.());
  }
});

vi.mock('naive-ui', () => ({
  NAlert: passthroughStub,
  NTabPane: passthroughStub
}));

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}));

vi.mock('@/hooks/common/router', () => ({
  useRouterPush: () => ({
    routerPushByKey: mocks.routerPushByKey
  })
}));

vi.mock('@/service/api', () => ({
  fetchGetApps: mocks.fetchGetApps,
  fetchGetDeployTasks: mocks.fetchGetDeployTasks,
  fetchGetDeployTask: mocks.fetchGetDeployTask,
  fetchGetDeployTaskHosts: mocks.fetchGetDeployTaskHosts,
  fetchGetDeployTaskLogs: mocks.fetchGetDeployTaskLogs,
  fetchGetTaskCenterTasks: mocks.fetchGetTaskCenterTasks,
  fetchGetTaskCenterTaskDetail: mocks.fetchGetTaskCenterTaskDetail,
  fetchGetAppVersions: mocks.fetchGetAppVersions,
  fetchGetAssetHosts: mocks.fetchGetAssetHosts,
  fetchPostCreateDeployTask: mocks.fetchPostCreateDeployTask,
  fetchPostApproveDeployTask: mocks.fetchPostApproveDeployTask,
  fetchPostRejectDeployTask: mocks.fetchPostRejectDeployTask,
  fetchPostExecuteDeployTask: mocks.fetchPostExecuteDeployTask,
  fetchPostRetryDeployTask: mocks.fetchPostRetryDeployTask,
  fetchPostRollbackDeployTask: mocks.fetchPostRollbackDeployTask,
  fetchPostCancelDeployTask: mocks.fetchPostCancelDeployTask
}));

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0));
}

function createDeferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(nextResolve => {
    resolve = nextResolve;
  });

  return { promise, resolve };
}

function createDeployTaskRecord(id: number, overrides: Record<string, unknown> = {}) {
  return {
    id,
    taskNo: `TASK-${id}`,
    taskName: `Deploy service-${id}`,
    taskType: 'INSTALL',
    appId: 100 + id,
    appName: `service-${id}`,
    versionId: 200 + id,
    versionNo: `v1.0.${id}`,
    status: 'RUNNING',
    batchStrategy: 'ALL',
    batchSize: 1,
    targetCount: 1,
    successCount: 0,
    failCount: 0,
    operatorName: `operator-${id}`,
    createdAt: `2026-04-18T10:0${id}:00`,
    updatedAt: `2026-04-18T10:0${id}:30`,
    params: { environment: 'production' },
    ...overrides
  };
}

function createDeployTaskDetailRecord(id: number, overrides: Record<string, unknown> = {}) {
  return {
    ...createDeployTaskRecord(id),
    totalHosts: 1,
    pendingHosts: 0,
    runningHosts: 1,
    successHosts: 0,
    failedHosts: 0,
    cancelledHosts: 0,
    ...overrides
  };
}

async function settleRender() {
  await flushPromises();
  await nextTick();
}

function getDeployTaskRow(container: HTMLElement, taskId: number) {
  return container.querySelector(`tr[data-task-id="${taskId}"]`);
}

function getSummaryCard(container: HTMLElement, summaryKey: string) {
  return container.querySelector(`[data-summary-key="${summaryKey}"]`);
}

function getTaskRowActionButton(container: HTMLElement, taskId: number, actionKey: string) {
  const row = getDeployTaskRow(container, taskId);

  if (!row) {
    return null;
  }

  return (
    Array.from(row.querySelectorAll('button')).find(button =>
      button.textContent?.includes(`page.envops.deployTask.actions.${actionKey}`)
    ) ?? null
  );
}

function createTaskCenterRecord(id: number, overrides: Record<string, unknown> = {}) {
  return {
    id,
    taskType: 'deploy',
    taskName: `Task center item ${id}`,
    status: 'pending',
    triggeredBy: `operator-${id}`,
    startedAt: `2026-04-18T11:0${id}:00`,
    finishedAt: null,
    summary: `Summary ${id}`,
    sourceRoute: `/deploy/task?taskId=${1000 + id}`,
    errorSummary: null,
    ...overrides
  };
}

function createTaskCenterTaskDetail(id: number, overrides: Record<string, unknown> = {}) {
  return {
    id,
    taskType: 'deploy',
    taskName: `Task center item ${id}`,
    status: 'pending',
    triggeredBy: `operator-${id}`,
    startedAt: `2026-04-18T11:0${id}:00`,
    finishedAt: null,
    summary: `Summary ${id}`,
    detailPreview: {
      app: `service-${id}`,
      environment: 'production',
      sourceRoute: `/deploy/task?taskId=${1000 + id}`
    },
    sourceRoute: `/deploy/task?taskId=${1000 + id}`,
    errorSummary: null,
    ...overrides
  };
}

async function mountTaskCenterPage() {
  const { default: TaskCenterPage } = await import('./center/index.vue');
  const container = document.createElement('div');
  document.body.appendChild(container);
  const app = createApp(TaskCenterPage);
  [
    'NSpace',
    'NCard',
    'NGrid',
    'NGi',
    'NStatistic',
    'NInput',
    'NSelect',
    'NButton',
    'NSpin',
    'NTable',
    'NEmpty',
    'NPagination',
    'NTag',
    'NDrawer',
    'NDrawerContent',
    'NDescriptions',
    'NDescriptionsItem',
    'NDatePicker'
  ].forEach(name => {
    app.component(
      name,
      name === 'NButton'
        ? buttonStub
        : name === 'NInput'
          ? inputStub
          : name === 'NSelect'
            ? selectStub
            : name === 'NDrawer'
              ? drawerStub
              : name === 'NTable'
                ? tableStub
                : passthroughStub
    );
  });
  app.mount(container);
  await nextTick();
  await flushPromises();
  await nextTick();

  return {
    container,
    unmount() {
      app.unmount();
      container.remove();
    }
  };
}

async function mountDeployTaskPage() {
  const { default: DeployTaskPage } = await import('../deploy/task/index.vue');
  const container = document.createElement('div');
  document.body.appendChild(container);
  const app = createApp(DeployTaskPage);
  [
    'NSpace',
    'NCard',
    'NGrid',
    'NGi',
    'NStatistic',
    'NInput',
    'NInputNumber',
    'NSelect',
    'NButton',
    'NSpin',
    'NTable',
    'NEmpty',
    'NPagination',
    'NForm',
    'NFormItem',
    'NDrawer',
    'NDrawerContent',
    'NTabs',
    'NDescriptions',
    'NDescriptionsItem',
    'NTag',
    'NDatePicker'
  ].forEach(name => {
    app.component(
      name,
      name === 'NButton'
        ? buttonStub
        : name === 'NInput'
          ? inputStub
          : name === 'NInputNumber'
            ? inputNumberStub
            : name === 'NSelect'
              ? selectStub
              : name === 'NDrawer'
                ? drawerStub
                : name === 'NTable'
                  ? tableStub
                  : passthroughStub
    );
  });
  app.mount(container);
  await nextTick();
  await flushPromises();
  await nextTick();

  return {
    container,
    unmount() {
      app.unmount();
      container.remove();
    }
  };
}

describe('task pages contract wiring', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.route.query = {};
    document.body.innerHTML = '';
    Object.defineProperty(document, 'visibilityState', {
      value: 'visible',
      configurable: true
    });
    window.$message = {
      warning: mocks.warning,
      success: mocks.success
    } as unknown as typeof window.$message;

    mocks.fetchGetApps.mockResolvedValue({ data: [], error: null });
    mocks.fetchGetDeployTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 1,
        records: [
          {
            id: 1,
            taskNo: 'TASK-1',
            taskName: 'Deploy order-service',
            taskType: 'INSTALL',
            appId: 101,
            appName: 'order-service',
            versionId: 201,
            versionNo: 'v1.0.0',
            status: 'RUNNING',
            batchStrategy: 'ALL',
            batchSize: 1,
            targetCount: 1,
            successCount: 0,
            failCount: 0,
            operatorName: 'alice',
            createdAt: '2026-04-18T10:00:00',
            updatedAt: '2026-04-18T10:05:00',
            params: { environment: 'production' }
          }
        ]
      }
    });
    mocks.fetchGetDeployTask.mockResolvedValue({
      error: null,
      data: {
        id: 1,
        taskNo: 'TASK-1',
        taskName: 'Deploy order-service',
        taskType: 'INSTALL',
        appId: 101,
        appName: 'order-service',
        versionId: 201,
        versionNo: 'v1.0.0',
        status: 'RUNNING',
        batchStrategy: 'ALL',
        batchSize: 1,
        targetCount: 1,
        successCount: 0,
        failCount: 0,
        operatorName: 'alice',
        createdAt: '2026-04-18T10:00:00',
        updatedAt: '2026-04-18T10:05:00',
        totalHosts: 1,
        pendingHosts: 0,
        runningHosts: 1,
        successHosts: 0,
        failedHosts: 0,
        cancelledHosts: 0,
        params: { environment: 'production' }
      }
    });
    mocks.fetchGetDeployTaskLogs.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 0,
        records: []
      }
    });
    mocks.fetchGetAppVersions.mockResolvedValue({
      data: [],
      error: null
    });
    mocks.fetchGetAssetHosts.mockResolvedValue({
      data: {
        current: 1,
        size: 100,
        total: 0,
        records: [],
        summary: {
          managedHosts: 0,
          onlineHosts: 0,
          warningHosts: 0
        }
      },
      error: null
    });
    mocks.fetchPostCreateDeployTask.mockResolvedValue({ data: null, error: null });
    mocks.fetchPostApproveDeployTask.mockResolvedValue({ data: null, error: null });
    mocks.fetchPostRejectDeployTask.mockResolvedValue({ data: null, error: null });
    mocks.fetchGetTaskCenterTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 1,
        records: [createTaskCenterRecord(1)]
      }
    });
    mocks.fetchGetTaskCenterTaskDetail.mockResolvedValue({
      error: null,
      data: createTaskCenterTaskDetail(1)
    });
    mocks.fetchGetDeployTaskHosts.mockImplementation(
      async (_id: number, params: { page: number; pageSize: number }) => {
        if (params.pageSize === 100) {
          return {
            error: null,
            data: {
              page: params.page,
              pageSize: params.pageSize,
              total: 1,
              records: [
                {
                  id: 1,
                  taskId: 1,
                  hostId: 11,
                  hostName: 'host-a',
                  ipAddress: '10.0.0.1',
                  status: 'RUNNING',
                  currentStep: 'deploy',
                  startedAt: '2026-04-18T10:01:00',
                  finishedAt: null,
                  errorMsg: null
                }
              ]
            }
          };
        }

        return {
          error: null,
          data: {
            page: params.page,
            pageSize: params.pageSize,
            total: 1,
            records: [
              {
                id: 1,
                taskId: 1,
                hostId: 11,
                hostName: 'host-a',
                ipAddress: '10.0.0.1',
                status: 'RUNNING',
                currentStep: 'deploy',
                startedAt: '2026-04-18T10:01:00',
                finishedAt: null,
                errorMsg: null
              }
            ]
          }
        };
      }
    );
  });

  afterEach(() => {
    vi.resetModules();
  });

  it('retries loading log host options on manual refresh after an initial dedicated-host load failure', async () => {
    mocks.route.query = { taskId: '1' };

    let dedicatedHostLoadAttempts = 0;
    mocks.fetchGetDeployTaskHosts.mockImplementation(
      async (_id: number, params: { page: number; pageSize: number }) => {
        if (params.pageSize === 100) {
          dedicatedHostLoadAttempts += 1;

          if (dedicatedHostLoadAttempts === 1) {
            return { error: { message: 'temporary failure' }, data: null };
          }
        }

        return {
          error: null,
          data: {
            page: params.page,
            pageSize: params.pageSize,
            total: 1,
            records: [
              {
                id: 1,
                taskId: 1,
                hostId: 11,
                hostName: 'host-a',
                ipAddress: '10.0.0.1',
                status: 'RUNNING',
                currentStep: 'deploy',
                startedAt: '2026-04-18T10:01:00',
                finishedAt: null,
                errorMsg: null
              }
            ]
          }
        };
      }
    );

    const page = await mountDeployTaskPage();

    expect(dedicatedHostLoadAttempts).toBe(1);

    const manualRefreshButton = Array.from(page.container.querySelectorAll('button')).find(button =>
      button.textContent?.includes('page.envops.deployTask.detail.manualRefresh')
    );

    expect(manualRefreshButton).toBeTruthy();

    manualRefreshButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await flushPromises();
    await nextTick();

    expect(dedicatedHostLoadAttempts).toBe(2);

    page.unmount();
  });

  it('keeps detail loading active while a newer task detail request is still in flight', async () => {
    const firstDetail = createDeferred<{ error: null; data: Record<string, unknown> }>();
    const secondDetail = createDeferred<{ error: null; data: Record<string, unknown> }>();

    mocks.route.query = { taskId: '1' };
    mocks.fetchGetDeployTask.mockImplementation((id: number) => {
      if (id === 1) {
        return firstDetail.promise;
      }

      if (id === 2) {
        return secondDetail.promise;
      }

      return Promise.resolve({ error: null, data: null });
    });

    const page = await mountDeployTaskPage();
    const manualRefreshButton = Array.from(page.container.querySelectorAll('button')).find(button =>
      button.textContent?.includes('page.envops.deployTask.detail.manualRefresh')
    );

    expect(manualRefreshButton?.getAttribute('data-loading')).toBe('true');

    mocks.route.query = { taskId: '2' };
    await nextTick();
    await nextTick();

    expect(manualRefreshButton?.getAttribute('data-loading')).toBe('true');

    firstDetail.resolve({
      error: null,
      data: {
        id: 1,
        taskNo: 'TASK-1',
        taskName: 'Deploy order-service',
        taskType: 'INSTALL',
        appId: 101,
        appName: 'order-service',
        versionId: 201,
        versionNo: 'v1.0.0',
        status: 'RUNNING',
        batchStrategy: 'ALL',
        batchSize: 1,
        targetCount: 1,
        successCount: 0,
        failCount: 0,
        operatorName: 'alice',
        createdAt: '2026-04-18T10:00:00',
        updatedAt: '2026-04-18T10:05:00',
        totalHosts: 1,
        pendingHosts: 0,
        runningHosts: 1,
        successHosts: 0,
        failedHosts: 0,
        cancelledHosts: 0,
        params: { environment: 'production' }
      }
    });
    await flushPromises();
    await nextTick();

    expect(manualRefreshButton?.getAttribute('data-loading')).toBe('true');

    secondDetail.resolve({
      error: null,
      data: {
        id: 2,
        taskNo: 'TASK-2',
        taskName: 'Deploy billing-service',
        taskType: 'INSTALL',
        appId: 102,
        appName: 'billing-service',
        versionId: 202,
        versionNo: 'v1.0.1',
        status: 'RUNNING',
        batchStrategy: 'ALL',
        batchSize: 1,
        targetCount: 1,
        successCount: 0,
        failCount: 0,
        operatorName: 'bob',
        createdAt: '2026-04-18T10:06:00',
        updatedAt: '2026-04-18T10:07:00',
        totalHosts: 1,
        pendingHosts: 0,
        runningHosts: 1,
        successHosts: 0,
        failedHosts: 0,
        cancelledHosts: 0,
        params: { environment: 'production' }
      }
    });
    await flushPromises();
    await nextTick();

    expect(manualRefreshButton?.getAttribute('data-loading')).toBe('false');

    page.unmount();
  });

  it('keeps manual refresh loading active until dedicated log-host refresh completes', async () => {
    const slowLogHosts = createDeferred<{
      error: null;
      data: {
        page: number;
        pageSize: number;
        total: number;
        records: Array<Record<string, unknown>>;
      };
    }>();

    mocks.route.query = { taskId: '1' };

    let dedicatedHostLoadAttempts = 0;
    mocks.fetchGetDeployTaskHosts.mockImplementation(
      async (_id: number, params: { page: number; pageSize: number }) => {
        if (params.pageSize === 100) {
          dedicatedHostLoadAttempts += 1;

          if (dedicatedHostLoadAttempts === 2) {
            return slowLogHosts.promise;
          }
        }

        return {
          error: null,
          data: {
            page: params.page,
            pageSize: params.pageSize,
            total: 1,
            records: [
              {
                id: 1,
                taskId: 1,
                hostId: 11,
                hostName: 'host-a',
                ipAddress: '10.0.0.1',
                status: 'RUNNING',
                currentStep: 'deploy',
                startedAt: '2026-04-18T10:01:00',
                finishedAt: null,
                errorMsg: null
              }
            ]
          }
        };
      }
    );

    const page = await mountDeployTaskPage();
    const manualRefreshButton = Array.from(page.container.querySelectorAll('button')).find(button =>
      button.textContent?.includes('page.envops.deployTask.detail.manualRefresh')
    );

    expect(manualRefreshButton?.getAttribute('data-loading')).toBe('false');

    manualRefreshButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await flushPromises();
    await nextTick();

    expect(dedicatedHostLoadAttempts).toBe(2);
    expect(manualRefreshButton?.getAttribute('data-loading')).toBe('true');

    slowLogHosts.resolve({
      error: null,
      data: {
        page: 1,
        pageSize: 100,
        total: 1,
        records: [
          {
            id: 1,
            taskId: 1,
            hostId: 11,
            hostName: 'host-a',
            ipAddress: '10.0.0.1',
            status: 'RUNNING',
            currentStep: 'deploy',
            startedAt: '2026-04-18T10:01:00',
            finishedAt: null,
            errorMsg: null
          }
        ]
      }
    });
    await flushPromises();
    await nextTick();

    expect(manualRefreshButton?.getAttribute('data-loading')).toBe('false');

    page.unmount();
  });

  it('does not start a stale dedicated log-host load after switching to a newer task', async () => {
    const task1Detail = createDeferred<{ error: null; data: Record<string, unknown> }>();
    const task2Detail = createDeferred<{ error: null; data: Record<string, unknown> }>();
    const dedicatedLogHostTaskIds: number[] = [];

    mocks.route.query = { taskId: '1' };
    mocks.fetchGetDeployTask.mockImplementation((id: number) => {
      if (id === 1) {
        return task1Detail.promise;
      }

      if (id === 2) {
        return task2Detail.promise;
      }

      return Promise.resolve({ error: null, data: null });
    });
    mocks.fetchGetDeployTaskHosts.mockImplementation(async (id: number, params: { page: number; pageSize: number }) => {
      if (params.pageSize === 100) {
        dedicatedLogHostTaskIds.push(id);
      }

      return {
        error: null,
        data: {
          page: params.page,
          pageSize: params.pageSize,
          total: 1,
          records: [
            {
              id,
              taskId: id,
              hostId: id * 10,
              hostName: `host-${id}`,
              ipAddress: `10.0.0.${id}`,
              status: 'RUNNING',
              currentStep: 'deploy',
              startedAt: '2026-04-18T10:01:00',
              finishedAt: null,
              errorMsg: null
            }
          ]
        }
      };
    });

    const page = await mountDeployTaskPage();

    mocks.route.query = { taskId: '2' };
    await nextTick();
    await nextTick();

    task2Detail.resolve({
      error: null,
      data: {
        id: 2,
        taskNo: 'TASK-2',
        taskName: 'Deploy billing-service',
        taskType: 'INSTALL',
        appId: 102,
        appName: 'billing-service',
        versionId: 202,
        versionNo: 'v1.0.1',
        status: 'RUNNING',
        batchStrategy: 'ALL',
        batchSize: 1,
        targetCount: 1,
        successCount: 0,
        failCount: 0,
        operatorName: 'bob',
        createdAt: '2026-04-18T10:06:00',
        updatedAt: '2026-04-18T10:07:00',
        totalHosts: 1,
        pendingHosts: 0,
        runningHosts: 1,
        successHosts: 0,
        failedHosts: 0,
        cancelledHosts: 0,
        params: { environment: 'production' }
      }
    });
    await flushPromises();
    await nextTick();

    expect(dedicatedLogHostTaskIds).toEqual([2]);

    task1Detail.resolve({
      error: null,
      data: {
        id: 1,
        taskNo: 'TASK-1',
        taskName: 'Deploy order-service',
        taskType: 'INSTALL',
        appId: 101,
        appName: 'order-service',
        versionId: 201,
        versionNo: 'v1.0.0',
        status: 'RUNNING',
        batchStrategy: 'ALL',
        batchSize: 1,
        targetCount: 1,
        successCount: 0,
        failCount: 0,
        operatorName: 'alice',
        createdAt: '2026-04-18T10:00:00',
        updatedAt: '2026-04-18T10:05:00',
        totalHosts: 1,
        pendingHosts: 0,
        runningHosts: 1,
        successHosts: 0,
        failedHosts: 0,
        cancelledHosts: 0,
        params: { environment: 'production' }
      }
    });
    await flushPromises();
    await nextTick();

    expect(dedicatedLogHostTaskIds).toEqual([2]);

    page.unmount();
  });

  it('task row highlight marks the deep-linked active task row', async () => {
    mocks.route.query = { taskId: '1' };
    mocks.fetchGetDeployTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 2,
        records: [createDeployTaskRecord(1), createDeployTaskRecord(2)]
      }
    });
    mocks.fetchGetDeployTask.mockImplementation(async (id: number) => ({
      error: null,
      data: createDeployTaskDetailRecord(id)
    }));

    const page = await mountDeployTaskPage();
    const activeRow = getDeployTaskRow(page.container, 1);
    const inactiveRow = getDeployTaskRow(page.container, 2);

    expect(activeRow).toBeTruthy();
    expect(activeRow?.classList.contains('deploy-task-row--active')).toBe(true);
    expect(inactiveRow?.classList.contains('deploy-task-row--active')).toBe(false);

    page.unmount();
  });

  it('task row highlight switches to the newly selected task row when route taskId changes', async () => {
    mocks.route.query = { taskId: '1' };
    mocks.fetchGetDeployTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 2,
        records: [createDeployTaskRecord(1), createDeployTaskRecord(2)]
      }
    });
    mocks.fetchGetDeployTask.mockImplementation(async (id: number) => ({
      error: null,
      data: createDeployTaskDetailRecord(id)
    }));

    const page = await mountDeployTaskPage();

    expect(getDeployTaskRow(page.container, 1)?.classList.contains('deploy-task-row--active')).toBe(true);
    expect(getDeployTaskRow(page.container, 2)?.classList.contains('deploy-task-row--active')).toBe(false);

    mocks.route.query = { taskId: '2' };
    await settleRender();

    expect(getDeployTaskRow(page.container, 1)?.classList.contains('deploy-task-row--active')).toBe(false);
    expect(getDeployTaskRow(page.container, 2)?.classList.contains('deploy-task-row--active')).toBe(true);

    page.unmount();
  });

  it('task row highlight clears after closing the detail drawer via route query', async () => {
    mocks.route.query = { taskId: '1' };
    mocks.fetchGetDeployTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 2,
        records: [createDeployTaskRecord(1), createDeployTaskRecord(2)]
      }
    });
    mocks.fetchGetDeployTask.mockImplementation(async (id: number) => ({
      error: null,
      data: createDeployTaskDetailRecord(id)
    }));

    const page = await mountDeployTaskPage();

    expect(getDeployTaskRow(page.container, 1)?.classList.contains('deploy-task-row--active')).toBe(true);

    mocks.route.query = {};
    await settleRender();

    expect(getDeployTaskRow(page.container, 1)?.classList.contains('deploy-task-row--active')).toBe(false);
    expect(page.container.querySelectorAll('tr.deploy-task-row--active')).toHaveLength(0);

    page.unmount();
  });

  it('submits deploy task create form with execution params wired into payload', async () => {
    mocks.fetchGetApps.mockResolvedValue({
      data: [
        {
          id: 1001,
          appName: 'order-service',
          appCode: 'order-service'
        }
      ],
      error: null
    });
    mocks.fetchGetAppVersions.mockResolvedValue({
      data: [
        {
          id: 1401,
          versionNo: 'v1.0.0'
        }
      ],
      error: null
    });
    mocks.fetchGetAssetHosts.mockResolvedValue({
      data: {
        current: 1,
        size: 100,
        total: 2,
        records: [
          {
            id: 1,
            hostName: 'host-prd-01',
            ipAddress: '10.0.0.11',
            environment: 'production'
          },
          {
            id: 2,
            hostName: 'host-prd-02',
            ipAddress: '10.0.0.12',
            environment: 'production'
          }
        ],
        summary: {
          managedHosts: 2,
          onlineHosts: 2,
          warningHosts: 0
        }
      },
      error: null
    });
    mocks.fetchPostCreateDeployTask.mockResolvedValue({ data: { id: 99 }, error: null });

    const page = await mountDeployTaskPage();

    try {
      const openCreateButton = Array.from(page.container.querySelectorAll('button')).find(button =>
        button.textContent?.includes('page.envops.deployTask.actions.create')
      );
      openCreateButton?.click();
      await settleRender();

      const getInputByPlaceholder = (placeholder: string) =>
        Array.from(page.container.querySelectorAll('input')).find(
          input => input.getAttribute('placeholder') === placeholder
        ) as HTMLInputElement | undefined;
      const getSelectByPlaceholder = (placeholder: string) =>
        Array.from(page.container.querySelectorAll('select')).find(
          select => select.getAttribute('placeholder') === placeholder
        ) as HTMLSelectElement | undefined;

      const taskNameInput = getInputByPlaceholder('page.envops.deployTask.create.taskNamePlaceholder');
      const sshUserInput = getInputByPlaceholder('page.envops.deployTask.create.sshUserPlaceholder');
      const sshPortInput = getInputByPlaceholder('page.envops.deployTask.create.sshPortPlaceholder');
      const privateKeyPathInput = getInputByPlaceholder('page.envops.deployTask.create.privateKeyPathPlaceholder');
      const remoteBaseDirInput = getInputByPlaceholder('page.envops.deployTask.create.remoteBaseDirPlaceholder');
      const rollbackCommandInput = getInputByPlaceholder('page.envops.deployTask.create.rollbackCommandPlaceholder');
      const appSelect = getSelectByPlaceholder('page.envops.deployTask.create.appPlaceholder');
      const versionSelect = getSelectByPlaceholder('page.envops.deployTask.create.versionPlaceholder');
      const hostSelect = getSelectByPlaceholder('page.envops.deployTask.create.hostsPlaceholder');

      expect(taskNameInput).toBeTruthy();
      expect(sshUserInput).toBeTruthy();
      expect(sshPortInput).toBeTruthy();
      expect(privateKeyPathInput).toBeTruthy();
      expect(remoteBaseDirInput).toBeTruthy();
      expect(rollbackCommandInput).toBeTruthy();
      expect(appSelect).toBeTruthy();
      expect(versionSelect).toBeTruthy();
      expect(hostSelect).toBeTruthy();

      taskNameInput!.value = 'deploy-order-service-prod';
      taskNameInput!.dispatchEvent(new Event('input', { bubbles: true }));
      sshUserInput!.value = 'release';
      sshUserInput!.dispatchEvent(new Event('input', { bubbles: true }));
      sshPortInput!.value = '22';
      sshPortInput!.dispatchEvent(new Event('input', { bubbles: true }));
      privateKeyPathInput!.value = '/data/keys/release.pem';
      privateKeyPathInput!.dispatchEvent(new Event('input', { bubbles: true }));
      remoteBaseDirInput!.value = '/opt/envops/releases';
      remoteBaseDirInput!.dispatchEvent(new Event('input', { bubbles: true }));
      rollbackCommandInput!.value = 'bash /opt/envops/bin/rollback.sh';
      rollbackCommandInput!.dispatchEvent(new Event('input', { bubbles: true }));

      appSelect!.value = '1001';
      appSelect!.dispatchEvent(new Event('change', { bubbles: true }));
      await settleRender();

      versionSelect!.value = '1401';
      versionSelect!.dispatchEvent(new Event('change', { bubbles: true }));

      Array.from(hostSelect!.options).forEach(option => {
        option.selected = option.value === '1' || option.value === '2';
      });
      hostSelect!.dispatchEvent(new Event('change', { bubbles: true }));
      await settleRender();

      const createButtons = Array.from(page.container.querySelectorAll('button')).filter(button =>
        button.textContent?.includes('page.envops.deployTask.actions.create')
      );
      createButtons.at(-1)?.click();
      await settleRender();

      expect(mocks.fetchPostCreateDeployTask).toHaveBeenCalledWith({
        taskName: 'deploy-order-service-prod',
        taskType: 'INSTALL',
        appId: 1001,
        versionId: 1401,
        environment: 'production',
        hostIds: [1, 2],
        batchStrategy: 'ALL',
        batchSize: null,
        sshUser: 'release',
        sshPort: 22,
        privateKeyPath: '/data/keys/release.pem',
        remoteBaseDir: '/opt/envops/releases',
        rollbackCommand: 'bash /opt/envops/bin/rollback.sh'
      });
    } finally {
      page.unmount();
    }
  });

  it('exposes task api fetchers through the shared api barrel', () => {
    expect(taskApiSource).toMatch(/export function fetchGetDeployTasks\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*['"]\/api\/deploy\/tasks['"]/);
    expect(taskApiSource).toMatch(/export function fetchPostCreateDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*['"]\/api\/deploy\/tasks['"]/);
    expect(taskApiSource).toMatch(/export function fetchPostApproveDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/approve`/);
    expect(taskApiSource).toMatch(/export function fetchPostRejectDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/reject`/);
    expect(taskApiSource).toMatch(/export function fetchPostExecuteDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/execute`/);
    expect(taskApiSource).toMatch(/export function fetchPostRetryDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/retry`/);
    expect(taskApiSource).toMatch(/export function fetchPostRollbackDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/rollback`/);
    expect(taskApiSource).toMatch(/export function fetchPostCancelDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/cancel`/);
    expect(taskApiSource).toMatch(/export function fetchGetDeployTaskHosts\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/hosts`/);
    expect(taskApiSource).toMatch(/export function fetchGetDeployTaskLogs\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/logs`/);
    expect(taskApiSource).toMatch(/export function fetchGetTaskCenterTasks\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*['"]\/api\/task-center\/tasks['"]/);
    expect(taskApiSource).toMatch(/export function fetchGetTaskCenterTaskDetail\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/task-center\/tasks\/\$\{id\}`/);
    expect(apiIndexSource).toContain("export * from './task'");
  });

  it('defines frontend task typings for deploy and unified task center rows', () => {
    expect(taskTypingSource).toContain('namespace Task');
    expect(taskTypingSource).toContain('type TaskSortBy');
    expect(taskTypingSource).toContain("'createdAt' | 'updatedAt' | 'taskNo' | 'status'");
    expect(taskTypingSource).toContain("type TaskSortOrder = 'asc' | 'desc'");
    expect(taskTypingSource).toContain("type DeployTaskCreateTaskType = 'INSTALL' | 'UPGRADE'");
    expect(taskTypingSource).toContain("type DeployTaskBatchStrategy = 'ALL' | 'ROLLING'");
    expect(taskTypingSource).toContain('interface CreateDeployTaskPayload');
    expect(taskTypingSource).toContain('sshUser: string;');
    expect(taskTypingSource).toContain('sshPort?: number | null;');
    expect(taskTypingSource).toContain('privateKeyPath: string;');
    expect(taskTypingSource).toContain('remoteBaseDir: string;');
    expect(taskTypingSource).toContain('rollbackCommand?: string | null;');
    expect(taskTypingSource).toContain('interface DeployTaskApprovalPayload');
    expect(taskTypingSource).toContain('interface DeployTaskListQuery');
    expect(taskTypingSource).toContain('interface DeployTaskPage');
    expect(taskTypingSource).toContain('interface DeployTaskDetailRecord extends DeployTaskRecord');
    expect(taskTypingSource).toContain('interface DeployTaskRecord');
    expect(taskTypingSource).toContain('originTaskId?: number | null');
    expect(taskTypingSource).toContain('interface DeployTaskHostQuery');
    expect(taskTypingSource).toContain('interface DeployTaskHostPage');
    expect(taskTypingSource).toContain('interface DeployTaskHostRecord');
    expect(taskTypingSource).toContain('interface DeployTaskLogQuery');
    expect(taskTypingSource).toContain('interface DeployTaskLogPage');
    expect(taskTypingSource).toContain('interface DeployTaskLogRecord');
    expect(taskTypingSource).toContain(
      "type TaskCenterTaskType = 'deploy' | 'database_connectivity' | 'traffic_action'"
    );
    expect(taskTypingSource).toContain("type UnifiedTaskStatus = 'pending' | 'running' | 'success' | 'failed'");
    expect(taskTypingSource).toContain('interface TaskCenterListQuery');
    expect(taskTypingSource).toContain('startedFrom?: string;');
    expect(taskTypingSource).toContain('startedTo?: string;');
    expect(taskTypingSource).not.toContain('sourceType?: string;');
    expect(taskTypingSource).not.toContain('priority?: string;');
    expect(taskTypingSource).not.toContain('sortBy: TaskSortBy;');
    expect(taskTypingSource).not.toContain('sortOrder: TaskSortOrder;');
    expect(taskTypingSource).toContain('interface TaskCenterPage');
    expect(taskTypingSource).toContain('interface TaskCenterRecord');
    expect(taskTypingSource).toContain('taskType: TaskCenterTaskType;');
    expect(taskTypingSource).toContain('status: UnifiedTaskStatus;');
    expect(taskTypingSource).toContain('triggeredBy: string;');
    expect(taskTypingSource).toContain('startedAt: string;');
    expect(taskTypingSource).toContain('finishedAt?: string | null;');
    expect(taskTypingSource).toContain('summary: string;');
    expect(taskTypingSource).toContain('sourceRoute: string;');
    expect(taskTypingSource).toContain('interface TaskCenterTaskDetail extends TaskCenterRecord');
    expect(taskTypingSource).toContain('detailPreview: Record<string, unknown>;');
    expect(taskTypingSource).toContain('errorSummary?: string | null;');
    expect(taskTypingSource).not.toContain('sourceType: string;');
    expect(taskTypingSource).not.toContain('priority?: string | null;');
  });

  it('exposes paginated task api fetchers with params', () => {
    expect(taskApiSource).toMatch(/export function fetchGetDeployTasks\s*\(params: Api\.Task\.DeployTaskListQuery\)/);
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.DeployTaskPage>\(\{\s*url:\s*['"]\/api\/deploy\/tasks['"],\s*params\s*\}/s
    );
    expect(taskApiSource).toMatch(/export function fetchGetDeployTask\s*\(id: number\)/);
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.DeployTaskDetailRecord>\(\{\s*url:\s*`\/api\/deploy\/tasks\/\$\{id\}`\s*\}/s
    );
    expect(taskApiSource).toMatch(
      /export function fetchPostCreateDeployTask\s*\(data: Api\.Task\.CreateDeployTaskPayload\)/
    );
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.DeployTaskRecord>\(\{[\s\S]*url:\s*['"]\/api\/deploy\/tasks['"],[\s\S]*method:\s*['"]post['"]/s
    );
    expect(taskApiSource).toContain('sshUser: data.sshUser');
    expect(taskApiSource).toContain('privateKeyPath: data.privateKeyPath');
    expect(taskApiSource).toContain('remoteBaseDir: data.remoteBaseDir');
    expect(taskApiSource).toContain('data.rollbackCommand ? { rollbackCommand: data.rollbackCommand } : {}');
    expect(taskApiSource).toMatch(
      /export function fetchPostApproveDeployTask\s*\(id: number, data\?: Api\.Task\.DeployTaskApprovalPayload\)/
    );
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.DeployTaskRecord>\(\{[\s\S]*url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/approve`,[\s\S]*method:\s*['"]post['"]/s
    );
    expect(taskApiSource).toMatch(
      /export function fetchPostRejectDeployTask\s*\(id: number, data\?: Api\.Task\.DeployTaskApprovalPayload\)/
    );
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.DeployTaskRecord>\(\{[\s\S]*url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/reject`,[\s\S]*method:\s*['"]post['"]/s
    );
    expect(taskApiSource).toMatch(
      /export function fetchGetDeployTaskHosts\s*\(id: number, params: Api\.Task\.DeployTaskHostQuery\)/
    );
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.DeployTaskHostPage>\(\{\s*url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/hosts`,\s*params\s*\}/s
    );
    expect(taskApiSource).toMatch(
      /export function fetchGetDeployTaskLogs\s*\(id: number, params: Api\.Task\.DeployTaskLogQuery\)/
    );
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.DeployTaskLogPage>\(\{\s*url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/logs`,\s*params\s*\}/s
    );
    expect(taskApiSource).toMatch(
      /export function fetchGetTaskCenterTasks\s*\(params: Api\.Task\.TaskCenterListQuery\)/
    );
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.TaskCenterPage>\(\{\s*url:\s*['"]\/api\/task-center\/tasks['"],\s*params\s*\}/s
    );
    expect(taskApiSource).toMatch(/export function fetchGetTaskCenterTaskDetail\s*\(id: number\)/);
    expect(taskApiSource).toMatch(
      /request<Api\.Task\.TaskCenterDetail>\(\{\s*url:\s*`\/api\/task-center\/tasks\/\$\{id\}`\s*\}/s
    );
  });

  it('loads deploy task page from route query and real api data instead of static mock arrays', () => {
    expect(deployTaskPage).toContain('useRoute(');
    expect(deployTaskPage).toContain('routerPushByKey');
    expect(deployTaskPage).toContain('normalizeDeployTaskRouteQuery');
    expect(deployTaskPage).toContain('toDeployTaskApiQuery');
    expect(deployTaskPage).toContain('route.query');
    expect(deployTaskPage).toContain('fetchGetDeployTask');
    expect(deployTaskPage).toContain('fetchGetDeployTasks(toDeployTaskApiQuery(query))');
    expect(deployTaskPage).toContain('const taskList = ref');
    expect(deployTaskPage).toContain('taskList.value =');
    expect(deployTaskPage).toContain('pageSize');
    expect(deployTaskPage).toContain('sortBy');
    expect(deployTaskPage).toContain('taskId');
    expect(deployTaskPage).toContain('handleOpenCreateDrawer');
    expect(deployTaskPage).toContain('handleCreateTask');
    expect(deployTaskPage).toContain('openApprovalDrawer');
    expect(deployTaskPage).toContain('handleSubmitApproval');
    expect(deployTaskPage).toContain('handleExecuteTask');
    expect(deployTaskPage).toContain('handleRetryTask');
    expect(deployTaskPage).toContain('handleRollbackTask');
    expect(deployTaskPage).toContain('handleCancelTask');
    expect(deployTaskPage).toContain('loadTaskDetail');
    expect(deployTaskPage).toContain('const taskHosts = ref<Api.Task.DeployTaskHostRecord[]>');
    expect(deployTaskPage).toContain('const taskLogs = ref<Api.Task.DeployTaskLogRecord[]>');
    expect(deployTaskPage).toContain('NDrawer');
    expect(deployTaskPage).toContain('createDrawerVisible');
    expect(deployTaskPage).toContain('approvalDrawerVisible');
    expect(deployTaskPage).toContain('fetchGetAppVersions');
    expect(deployTaskPage).toContain('fetchGetAssetHosts');
    expect(deployTaskPage).toContain('fetchPostCreateDeployTask');
    expect(deployTaskPage).toContain('sshUser');
    expect(deployTaskPage).toContain('sshPort');
    expect(deployTaskPage).toContain('privateKeyPath');
    expect(deployTaskPage).toContain('remoteBaseDir');
    expect(deployTaskPage).toContain('rollbackCommand');
    expect(deployTaskPage).toContain("t('page.envops.deployTask.create.validation.sshUserRequired')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.create.validation.privateKeyPathRequired')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.create.validation.remoteBaseDirRequired')");
    expect(deployTaskPage).toContain('fetchPostApproveDeployTask');
    expect(deployTaskPage).toContain('fetchPostRejectDeployTask');
    expect(deployTaskPage).toContain('NPagination');
    expect(deployTaskPage).toContain('common.refresh');
    expect(deployTaskPage).toContain('NEmpty');
    expect(deployTaskPage).not.toContain('fetchGetDeployTasks()');
    expect(deployTaskPage).not.toContain('const deployTasks = computed(() => [');
  });

  it('loads task center page from real api data instead of static mock arrays', () => {
    expect(taskCenterPage).toContain('fetchGetTaskCenterTasks');
    expect(taskCenterPage).toContain('fetchGetTaskCenterTaskDetail');
    expect(taskCenterPage).toContain('await fetchGetTaskCenterTasks(');
    expect(taskCenterPage).toContain('const taskList = ref');
    expect(taskCenterPage).toContain('taskList.value =');
    expect(taskCenterPage).toContain('NDrawer');
    expect(taskCenterPage).toContain('NEmpty');
    expect(taskCenterPage).not.toContain('const taskList = computed(() => [');
  });

  it('drives unified task center list from route query pagination and detail drawer loading', () => {
    expect(taskCenterPage).toContain('useRoute(');
    expect(taskCenterPage).toContain('useRouter(');
    expect(taskCenterPage).toContain('routerPushByKey');
    expect(taskCenterPage).toContain('normalizeTaskCenterRouteQuery');
    expect(taskCenterPage).toContain('toTaskCenterApiQuery');
    expect(taskCenterPage).toContain('route.query');
    expect(taskCenterPage).toContain('fetchGetTaskCenterTasks(toTaskCenterApiQuery(query))');
    expect(taskCenterPage).toContain('fetchGetTaskCenterTaskDetail(taskId)');
    expect(taskCenterPage).toContain('filterForm.keyword = query.keyword');
    expect(taskCenterPage).toContain('filterForm.status = query.status || null');
    expect(taskCenterPage).toContain('filterForm.taskType = query.taskType || null');
    expect(taskCenterPage).toContain(
      'filterForm.startedRange = getStartedRangeValue(query.startedFrom, query.startedTo)'
    );
    expect(taskCenterPage).toContain(
      'const pendingTaskCenterRouteQuery = ref<TaskCenterRouteQuery>(normalizedRouteQuery.value)'
    );
    expect(taskCenterPage).toContain('pendingTaskCenterRouteQuery.value = query');
    expect(taskCenterPage).toContain(
      'const currentQuery = stringifyTaskCenterRouteQuery(pendingTaskCenterRouteQuery.value)'
    );
    expect(taskCenterPage).toContain(
      'const nextPendingQuery = { ...pendingTaskCenterRouteQuery.value, ...partialQuery }'
    );
    expect(taskCenterPage).toContain('pendingTaskCenterRouteQuery.value = nextPendingQuery');
    expect(taskCenterPage).toContain('pushTaskCenterRouteQuery');
    expect(taskCenterPage).toContain('handleSearch');
    expect(taskCenterPage).toContain('handleResetFilters');
    expect(taskCenterPage).toContain('handlePageChange');
    expect(taskCenterPage).toContain('handlePageSizeChange');
    expect(taskCenterPage).toContain('handleOpenTaskDetail');
    expect(taskCenterPage).toContain('showTaskDetailDrawer');
    expect(taskCenterPage).toContain('showTaskDetailDrawer.value = true');
    expect(taskCenterPage).toContain('closeTaskDetailDrawer');
    expect(taskCenterPage).toContain('detailRequestToken.value += 1');
    expect(taskCenterPage).toContain('detailLoadingToken.value += 1');
    expect(taskCenterPage).toContain('@update:show="handleTaskDetailDrawerShowChange"');
    expect(taskCenterPage).toContain('activeTaskDetail');
    expect(taskCenterPage).toContain('router.push(activeTaskDetail.value.sourceRoute)');
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.filters.keyword')");
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.filters.status')");
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.filters.taskType')");
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.filters.startedFrom')");
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.filters.startedTo')");
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.filters.search')");
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.filters.reset')");
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.actions.openTaskDetail')");
    expect(taskCenterPage).toContain("t('page.envops.taskCenter.actions.openSourceDetail')");
    expect(taskCenterPage).toContain('NDatePicker');
    expect(taskCenterPage).toContain('NPagination');
    expect(taskCenterPage).not.toContain('filterForm.priority');
    expect(taskCenterPage).not.toContain('sortByOptions');
    expect(taskCenterPage).not.toContain("routerPushByKey('deploy_task'");
    expect(taskCenterPage).not.toContain('metrics = computed');
  });

  it('normalizes unified task center route query and maps it to api params', () => {
    const normalized = normalizeTaskCenterRouteQuery({
      keyword: ['deploy'],
      status: 'running',
      taskType: 'traffic_action',
      startedFrom: 'bad-date',
      startedTo: '2026-04-18T08:30:45.000Z',
      page: '-2',
      pageSize: '0'
    });

    expect(normalized).toEqual({
      keyword: '',
      status: 'running',
      taskType: 'traffic_action',
      startedFrom: '',
      startedTo: formatLocalDateTimeRange([
        new Date('2026-04-18T08:30:45.000Z').getTime(),
        new Date('2026-04-18T08:30:45.000Z').getTime()
      ])[1],
      page: 1,
      pageSize: 10
    });

    expect(toTaskCenterApiQuery(normalized)).toEqual({
      status: 'running',
      taskType: 'traffic_action',
      startedTo: formatLocalDateTimeRange([
        new Date('2026-04-18T08:30:45.000Z').getTime(),
        new Date('2026-04-18T08:30:45.000Z').getTime()
      ])[1],
      page: 1,
      pageSize: 10
    });
  });

  it('drops unsupported task center taskType and status values at runtime', () => {
    const normalized = normalizeTaskCenterRouteQuery({
      taskType: 'rollback',
      status: 'cancelled',
      page: '2',
      pageSize: '20'
    });

    expect(normalized).toEqual({
      keyword: '',
      status: '',
      taskType: '',
      startedFrom: '',
      startedTo: '',
      page: 2,
      pageSize: 20
    });

    expect(toTaskCenterApiQuery(normalized)).toEqual({
      page: 2,
      pageSize: 20
    });
  });

  it('limits task center supported task type filters to deploy database connectivity and traffic actions', () => {
    expect(taskCenterPage).toContain("value: 'deploy'");
    expect(taskCenterPage).toContain("value: 'database_connectivity'");
    expect(taskCenterPage).toContain("value: 'traffic_action'");
    expect(taskCenterPage).not.toContain("value: 'INSTALL'");
    expect(taskCenterPage).not.toContain("value: 'ROLLBACK'");
  });

  it('maps unified task center normalized statuses into localized labels', () => {
    expect(taskCenterPage).toContain("pending: t('page.envops.common.status.pending')");
    expect(taskCenterPage).toContain("running: t('page.envops.common.status.running')");
    expect(taskCenterPage).toContain("success: t('page.envops.common.status.success')");
    expect(taskCenterPage).toContain("failed: t('page.envops.common.status.failed')");
    expect(taskCenterPage).not.toContain('pendingApproval');
    expect(taskCenterPage).not.toContain('cancelled');
    expect(taskCenterPage).not.toContain('rejected');
  });

  it('opens task center detail drawer and deep links to the source module', async () => {
    mocks.fetchGetTaskCenterTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 1,
        records: [
          createTaskCenterRecord(7, {
            taskType: 'database_connectivity',
            status: 'failed',
            sourceRoute: '/asset/database',
            summary: 'Batch database connectivity check'
          })
        ]
      }
    });
    mocks.fetchGetTaskCenterTaskDetail.mockResolvedValue({
      error: null,
      data: createTaskCenterTaskDetail(7, {
        taskType: 'database_connectivity',
        status: 'failed',
        sourceRoute: '/asset/database',
        summary: 'Batch database connectivity check',
        detailPreview: {
          mode: 'batch',
          total: 20,
          failed: 3,
          sourceRoute: '/asset/database'
        },
        errorSummary: '3 databases failed authentication'
      })
    });

    const page = await mountTaskCenterPage();

    const detailButton = Array.from(page.container.querySelectorAll('button')).find(button =>
      button.textContent?.includes('page.envops.taskCenter.actions.openTaskDetail')
    );

    expect(detailButton).toBeTruthy();

    detailButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.fetchGetTaskCenterTaskDetail).toHaveBeenCalledWith(7);

    const sourceButton = Array.from(page.container.querySelectorAll('button')).find(button =>
      button.textContent?.includes('page.envops.taskCenter.actions.openSourceDetail')
    );

    expect(sourceButton).toBeTruthy();

    sourceButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.routerPush).toHaveBeenCalledWith('/asset/database');

    page.unmount();
  });

  it('accepts only the latest task center detail response during rapid row clicks', async () => {
    const firstDetail = createDeferred<{ error: null; data: Record<string, unknown> }>();
    const secondDetail = createDeferred<{ error: null; data: Record<string, unknown> }>();

    mocks.fetchGetTaskCenterTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 2,
        records: [
          createTaskCenterRecord(7, {
            taskType: 'database_connectivity',
            status: 'failed',
            sourceRoute: '/asset/database',
            summary: 'Database detail row'
          }),
          createTaskCenterRecord(8, {
            taskType: 'traffic_action',
            status: 'success',
            sourceRoute: '/traffic/strategy',
            summary: 'Traffic detail row'
          })
        ]
      }
    });
    mocks.fetchGetTaskCenterTaskDetail.mockImplementation((id: number) => {
      if (id === 7) {
        return firstDetail.promise;
      }

      if (id === 8) {
        return secondDetail.promise;
      }

      return Promise.resolve({ error: null, data: null });
    });

    const page = await mountTaskCenterPage();
    const detailButtons = Array.from(page.container.querySelectorAll('button')).filter(button =>
      button.textContent?.includes('page.envops.taskCenter.actions.openTaskDetail')
    );

    expect(detailButtons).toHaveLength(2);

    detailButtons[0]?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();
    detailButtons[1]?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.fetchGetTaskCenterTaskDetail).toHaveBeenNthCalledWith(1, 7);
    expect(mocks.fetchGetTaskCenterTaskDetail).toHaveBeenNthCalledWith(2, 8);
    expect(page.container.querySelector('[data-drawer="true"]')).toBeTruthy();

    firstDetail.resolve({
      error: null,
      data: createTaskCenterTaskDetail(7, {
        taskType: 'database_connectivity',
        status: 'failed',
        sourceRoute: '/asset/database',
        detailPreview: {
          marker: 'first-detail-marker'
        },
        errorSummary: 'first detail should stay stale'
      })
    });
    await settleRender();

    expect(page.container.textContent).not.toContain('first-detail-marker');
    expect(page.container.textContent).not.toContain('first detail should stay stale');

    secondDetail.resolve({
      error: null,
      data: createTaskCenterTaskDetail(8, {
        taskType: 'traffic_action',
        status: 'success',
        sourceRoute: '/traffic/strategy',
        detailPreview: {
          marker: 'second-detail-marker'
        },
        errorSummary: 'second detail is current'
      })
    });
    await settleRender();

    expect(page.container.textContent).toContain('second-detail-marker');
    expect(page.container.textContent).toContain('second detail is current');
    expect(page.container.textContent).not.toContain('first-detail-marker');

    const sourceButton = Array.from(page.container.querySelectorAll('button')).find(button =>
      button.textContent?.includes('page.envops.taskCenter.actions.openSourceDetail')
    );

    expect(sourceButton).toBeTruthy();

    sourceButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.routerPush).toHaveBeenCalledWith('/traffic/strategy');

    page.unmount();
  });

  it('keeps task center detail drawer closed after a manual close during in-flight loading', async () => {
    const pendingDetail = createDeferred<{ error: null; data: Record<string, unknown> }>();

    mocks.fetchGetTaskCenterTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 1,
        records: [
          createTaskCenterRecord(9, {
            taskType: 'database_connectivity',
            status: 'running',
            sourceRoute: '/asset/database',
            summary: 'Pending database detail row'
          })
        ]
      }
    });
    mocks.fetchGetTaskCenterTaskDetail.mockImplementation((id: number) => {
      if (id === 9) {
        return pendingDetail.promise;
      }

      return Promise.resolve({ error: null, data: null });
    });

    const page = await mountTaskCenterPage();

    const detailButton = Array.from(page.container.querySelectorAll('button')).find(button =>
      button.textContent?.includes('page.envops.taskCenter.actions.openTaskDetail')
    );

    expect(detailButton).toBeTruthy();

    detailButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(page.container.querySelector('[data-drawer="true"]')).toBeTruthy();

    const closeButton = page.container.querySelector('[data-drawer-close="true"]');

    expect(closeButton).toBeTruthy();

    closeButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(page.container.querySelector('[data-drawer="true"]')).toBeNull();

    pendingDetail.resolve({
      error: null,
      data: createTaskCenterTaskDetail(9, {
        taskType: 'database_connectivity',
        status: 'success',
        sourceRoute: '/asset/database',
        detailPreview: {
          marker: 'closed-detail-marker'
        },
        errorSummary: 'closed detail should not reopen'
      })
    });
    await settleRender();

    expect(page.container.querySelector('[data-drawer="true"]')).toBeNull();
    expect(page.container.textContent).not.toContain('closed-detail-marker');
    expect(page.container.textContent).not.toContain('closed detail should not reopen');

    page.unmount();
  });

  it('maps cancelled deploy task status into cancelled label instead of pending', () => {
    expect(deployTaskPage).toContain("normalizedStatus.includes('cancel')");
    expect(deployTaskPage).toContain("cancelled: t('page.envops.common.status.cancelled')");
    expect(deployTaskPage).toContain("cancelled: 'warning'");
  });

  it('maps approval and rejected statuses to explicit deploy frontend labels', () => {
    expect(deployTaskPage).toContain("normalizedStatus.includes('approval')");
    expect(deployTaskPage).toContain("normalizedStatus.includes('reject')");
    expect(deployTaskPage).toContain("pendingApproval: t('page.envops.common.status.pendingApproval')");
    expect(deployTaskPage).toContain("pendingApproval: 'warning'");
    expect(deployTaskPage).toContain("rejected: t('page.envops.common.status.rejected')");
  });

  it('allows cancel only for pending or running deploy tasks', () => {
    expect(deployTaskPage).toContain('function canCancelTask(status: string | null | undefined)');
    expect(deployTaskPage).toContain("return normalizedStatus === 'RUNNING' || normalizedStatus === 'PENDING'");
    expect(deployTaskPage).not.toContain(
      "return statusKey === 'running' || statusKey === 'pending' || statusKey === 'pendingApproval'"
    );
  });

  it('prevents duplicate row mutations and rollback-of-rollback actions', () => {
    expect(deployTaskPage).toContain('const actionLoadingTaskIds = ref<number[]>([])');
    expect(deployTaskPage).toContain('function isTaskMutating(taskId: number)');
    expect(deployTaskPage).toContain('actionLoadingTaskIds.value.includes(taskId)');
    expect(deployTaskPage).toContain('actionLoadingTaskIds.value = [...actionLoadingTaskIds.value, taskId]');
    expect(deployTaskPage).toContain(
      'actionLoadingTaskIds.value = actionLoadingTaskIds.value.filter(id => id !== taskId)'
    );
    expect(deployTaskPage).toContain('if (isTaskMutating(taskId)) {');
    expect(deployTaskPage).toContain("return taskType !== 'ROLLBACK'");
    expect(deployTaskPage).toContain(':disabled="isTaskMutating(item.key) || !canExecuteTask(item.statusKey)"');
    expect(deployTaskPage).toContain(':disabled="isTaskMutating(item.key) || !canRetryTask(item.statusKey)"');
    expect(deployTaskPage).toContain(
      ':disabled="isTaskMutating(item.key) || !canRollbackTask(item.statusKey, item.taskType)"'
    );
    expect(deployTaskPage).toContain(':disabled="isTaskMutating(item.key) || !canCancelTask(item.rawStatus)"');
  });

  it('clears stale task detail state before loading a different task', () => {
    expect(deployTaskPage).toContain('const detailRequestToken = ref(0)');
    expect(deployTaskPage).toContain('const requestToken = ++detailRequestToken.value');
    expect(deployTaskPage).toContain('if (requestToken !== detailRequestToken.value) {');
    expect(deployTaskPage).toContain('activeTask.value = null');
    expect(deployTaskPage).toContain('taskHosts.value = []');
    expect(deployTaskPage).toContain('taskLogs.value = []');
    expect(deployTaskPage).not.toContain('nextTaskList.find(item => item.id === taskId) ?? activeTask.value');
  });

  it('guards list refreshes and detail refresh-list writes with latest-only request tokens', () => {
    expect(deployTaskPage).toContain('const listRequestToken = ref(0)');
    expect(deployTaskPage).toContain('const requestToken = ++listRequestToken.value');
    expect(deployTaskPage).toContain('if (requestToken !== listRequestToken.value) {');
    expect(deployTaskPage).toContain(
      'const listToken = refreshList ? ++listRequestToken.value : listRequestToken.value'
    );
    expect(deployTaskPage).toContain(
      'const currentTaskList = refreshList && listToken === listRequestToken.value ? nextTaskList : taskList.value'
    );
    expect(deployTaskPage).toContain('if (refreshList && listToken === listRequestToken.value) {');
    expect(deployTaskPage).toContain('const taskDetailResponse = await fetchGetDeployTask(taskId);');
    expect(deployTaskPage).toContain(
      'activeTask.value = taskDetailResponse.error ? null : (taskDetailResponse.data ?? null);'
    );
    expect(deployTaskPage).not.toContain(
      'activeTask.value = currentTaskList.find(item => item.id === taskId) ?? null;'
    );
    expect(deployTaskPage.indexOf('if (requestToken !== listRequestToken.value) {')).toBeLessThan(
      deployTaskPage.indexOf('taskList.value = nextTaskList;')
    );
    expect(deployTaskPage.indexOf('if (refreshList && listToken === listRequestToken.value) {')).toBeLessThan(
      deployTaskPage.lastIndexOf('taskList.value = nextTaskList;')
    );
  });

  it('uses updateSuccess toast copy for deploy task actions', () => {
    expect(deployTaskPage).toContain("t('common.updateSuccess')");
    expect(deployTaskPage).not.toContain("t('common.success')");
  });

  it('wires deploy task application and created range filters through route query', () => {
    expect(deployTaskPage).toContain('fetchGetApps');
    expect(deployTaskPage).toContain('const apps = ref<Api.App.AppDefinition[]>([])');
    expect(deployTaskPage).toContain('appId: null as number | null');
    expect(deployTaskPage).toContain('createdRange: null as [number, number] | null');
    expect(deployTaskPage).toContain('filterForm.appId = query.appId');
    expect(deployTaskPage).toContain(
      'filterForm.createdRange = getCreatedRangeValue(query.createdFrom, query.createdTo)'
    );
    expect(deployTaskPage).toContain('const createdRange = getCreatedRangeQueryValue(filterForm.createdRange)');
    expect(deployTaskPage).toContain('appId: filterForm.appId');
    expect(deployTaskPage).toContain('createdFrom: createdRange[0]');
    expect(deployTaskPage).toContain('createdTo: createdRange[1]');
    expect(deployTaskPage).toContain("t('page.envops.deployTask.filters.application')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.filters.createdRange')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.filters.search')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.filters.reset')");
    expect(deployTaskPage).toContain('NDatePicker');
  });

  it('maps cancel requested deploy task status explicitly', () => {
    expect(deployTaskPage).toContain("normalizedStatus.includes('cancel_requested')");
    expect(deployTaskPage).toContain("return 'running'");
  });

  it('disables cancel action for cancel requested rows while keeping running rows cancellable', async () => {
    mocks.fetchGetDeployTasks.mockResolvedValue({
      error: null,
      data: {
        page: 1,
        pageSize: 10,
        total: 2,
        records: [
          createDeployTaskRecord(1, { status: 'CANCEL_REQUESTED' }),
          createDeployTaskRecord(2, { status: 'RUNNING' })
        ]
      }
    });

    const page = await mountDeployTaskPage();
    const cancelRequestedCancelButton = getTaskRowActionButton(page.container, 1, 'cancel');
    const runningCancelButton = getTaskRowActionButton(page.container, 2, 'cancel');

    expect(cancelRequestedCancelButton).toBeTruthy();
    expect(cancelRequestedCancelButton?.hasAttribute('disabled')).toBe(true);
    expect(runningCancelButton).toBeTruthy();
    expect(runningCancelButton?.hasAttribute('disabled')).toBe(false);

    page.unmount();
  });

  it('keeps running filter options sending RUNNING for deploy task page and running for unified task center page', () => {
    expect(deployTaskPage).toContain("{ label: t('page.envops.common.status.running'), value: 'RUNNING' }");
    expect(taskCenterPage).toContain("{ label: t('page.envops.common.status.running'), value: 'running' }");
    expect(deployTaskPage).not.toContain("value: 'RUNNING_LIKE'");
    expect(taskCenterPage).not.toContain("value: 'RUNNING_LIKE'");
  });

  it('clicking pending approval summary card pushes pending approval query and reloads deploy tasks', async () => {
    mocks.route.query = {
      keyword: 'order-service',
      appId: '101',
      taskType: 'UPGRADE',
      environment: 'production',
      createdFrom: '2026-04-17T08:00:00',
      createdTo: '2026-04-18T08:00:00',
      page: '3',
      pageSize: '20'
    };
    mocks.fetchGetDeployTasks.mockResolvedValue({
      error: null,
      data: {
        page: 3,
        pageSize: 20,
        total: 2,
        records: [
          createDeployTaskRecord(1, { status: 'PENDING_APPROVAL' }),
          createDeployTaskRecord(2, { status: 'RUNNING' })
        ]
      }
    });

    const page = await mountDeployTaskPage();

    mocks.fetchGetDeployTasks.mockClear();
    mocks.routerPushByKey.mockClear();

    const pendingApprovalCard = getSummaryCard(page.container, 'pendingApproval');

    expect(pendingApprovalCard).toBeTruthy();

    pendingApprovalCard?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.routerPushByKey).toHaveBeenCalledWith('deploy_task', {
      query: {
        keyword: 'order-service',
        appId: '101',
        taskType: 'UPGRADE',
        environment: 'production',
        status: 'PENDING_APPROVAL',
        page: '1',
        pageSize: '20',
        sortBy: 'createdAt',
        sortOrder: 'desc'
      }
    });
    expect(mocks.route.query).toEqual({
      keyword: 'order-service',
      appId: '101',
      taskType: 'UPGRADE',
      environment: 'production',
      status: 'PENDING_APPROVAL',
      page: '1',
      pageSize: '20',
      sortBy: 'createdAt',
      sortOrder: 'desc'
    });
    expect(mocks.fetchGetDeployTasks).toHaveBeenCalledTimes(1);
    expect(mocks.fetchGetDeployTasks).toHaveBeenCalledWith({
      keyword: 'order-service',
      appId: 101,
      taskType: 'UPGRADE',
      environment: 'production',
      status: 'PENDING_APPROVAL',
      page: 1,
      pageSize: 20,
      sortBy: 'createdAt',
      sortOrder: 'desc'
    });

    page.unmount();
  });

  it('clicking running summary card pushes RUNNING query and reloads deploy tasks without inventing new status values', async () => {
    mocks.route.query = {
      keyword: 'billing-service',
      environment: 'staging',
      createdFrom: '2026-04-16T08:00:00',
      createdTo: '2026-04-18T08:00:00',
      page: '4'
    };
    mocks.fetchGetDeployTasks.mockResolvedValue({
      error: null,
      data: {
        page: 4,
        pageSize: 10,
        total: 2,
        records: [createDeployTaskRecord(1, { status: 'RUNNING' }), createDeployTaskRecord(2, { status: 'RUNNING' })]
      }
    });

    const page = await mountDeployTaskPage();

    mocks.fetchGetDeployTasks.mockClear();
    mocks.routerPushByKey.mockClear();

    const runningCard = getSummaryCard(page.container, 'inProgress');

    expect(runningCard).toBeTruthy();

    runningCard?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.routerPushByKey).toHaveBeenCalledWith('deploy_task', {
      query: {
        keyword: 'billing-service',
        environment: 'staging',
        status: 'RUNNING',
        page: '1',
        pageSize: '10',
        sortBy: 'createdAt',
        sortOrder: 'desc'
      }
    });
    expect(mocks.route.query).toEqual({
      keyword: 'billing-service',
      environment: 'staging',
      status: 'RUNNING',
      page: '1',
      pageSize: '10',
      sortBy: 'createdAt',
      sortOrder: 'desc'
    });
    expect(mocks.fetchGetDeployTasks).toHaveBeenCalledTimes(1);
    expect(mocks.fetchGetDeployTasks).toHaveBeenCalledWith({
      keyword: 'billing-service',
      environment: 'staging',
      status: 'RUNNING',
      page: 1,
      pageSize: 10,
      sortBy: 'createdAt',
      sortOrder: 'desc'
    });
    expect(mocks.routerPushByKey.mock.calls[0]?.[1]?.query?.status).toBe('RUNNING');
    expect(mocks.routerPushByKey.mock.calls[0]?.[1]?.query?.status).not.toBe('RUNNING_LIKE');

    page.unmount();
  });

  it('clicking failed in 24h summary card pushes failed query with last 24 hours range and reloads deploy tasks', async () => {
    const fixedNow = new Date('2026-04-18T12:30:45').getTime();
    const nowSpy = vi.spyOn(Date, 'now').mockReturnValue(fixedNow);
    const expectedRange = formatLocalDateTimeRange([fixedNow - 24 * 60 * 60 * 1000, fixedNow]);

    mocks.route.query = {
      keyword: 'gateway',
      appId: '301',
      taskType: 'ROLLBACK',
      environment: 'sandbox',
      page: '2',
      pageSize: '50'
    };
    mocks.fetchGetDeployTasks.mockResolvedValue({
      error: null,
      data: {
        page: 2,
        pageSize: 50,
        total: 2,
        records: [
          createDeployTaskRecord(1, { status: 'FAILED', updatedAt: '2026-04-18T10:30:45' }),
          createDeployTaskRecord(2, { status: 'SUCCESS', updatedAt: '2026-04-18T09:30:45' })
        ]
      }
    });

    const page = await mountDeployTaskPage();

    mocks.fetchGetDeployTasks.mockClear();
    mocks.routerPushByKey.mockClear();

    const failedIn24hCard = getSummaryCard(page.container, 'failedIn24h');

    expect(failedIn24hCard).toBeTruthy();

    failedIn24hCard?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.routerPushByKey).toHaveBeenCalledWith('deploy_task', {
      query: {
        keyword: 'gateway',
        appId: '301',
        taskType: 'ROLLBACK',
        environment: 'sandbox',
        status: 'FAILED',
        createdFrom: expectedRange[0],
        createdTo: expectedRange[1],
        page: '1',
        pageSize: '50',
        sortBy: 'createdAt',
        sortOrder: 'desc'
      }
    });
    expect(mocks.route.query).toEqual({
      keyword: 'gateway',
      appId: '301',
      taskType: 'ROLLBACK',
      environment: 'sandbox',
      status: 'FAILED',
      createdFrom: expectedRange[0],
      createdTo: expectedRange[1],
      page: '1',
      pageSize: '50',
      sortBy: 'createdAt',
      sortOrder: 'desc'
    });
    expect(mocks.fetchGetDeployTasks).toHaveBeenCalledTimes(1);
    expect(mocks.fetchGetDeployTasks).toHaveBeenCalledWith({
      keyword: 'gateway',
      appId: 301,
      taskType: 'ROLLBACK',
      environment: 'sandbox',
      status: 'FAILED',
      createdFrom: expectedRange[0],
      createdTo: expectedRange[1],
      page: 1,
      pageSize: 50,
      sortBy: 'createdAt',
      sortOrder: 'desc'
    });

    nowSpy.mockRestore();
    page.unmount();
  });

  it('uses pending normalized status for the pending task center filter option', () => {
    expect(taskCenterPage).toContain("{ label: t('page.envops.common.status.pending'), value: 'pending' }");
    expect(taskCenterPage).not.toContain("{ label: t('page.envops.common.status.queued'), value: 'PENDING' }");
  });

  it('keeps drawer-first flow and minimal task center scope', () => {
    expect(taskCenterPage).toContain('startedFrom');
    expect(taskCenterPage).toContain('startedTo');
    expect(taskCenterPage).toContain('fetchGetTaskCenterTaskDetail');
    expect(taskCenterPage).toContain('showTaskDetailDrawer.value = true');
    expect(taskCenterPage).toContain('router.push(activeTaskDetail.value.sourceRoute)');
    expect(taskCenterPage).not.toContain('filterForm.priority');
    expect(taskCenterPage).not.toContain("routerPushByKey('deploy_task'");
  });

  it('opens task center source links from backend-provided sourceRoute instead of source-type branching', () => {
    expect(taskCenterPage).toContain('activeTaskDetail.value.sourceRoute');
    expect(taskCenterPage).not.toContain("return normalizedValue === 'deploy'");
    expect(taskCenterPage).not.toContain("routerPushByKey('deploy_task'");
  });

  it('declares deploy task detail i18n keys in locale schema and messages', () => {
    expect(appTypingSource).toContain('actions: {');
    expect(appTypingSource).toContain('detail: {');
    expect(appTypingSource).toContain('hosts: {');
    expect(appTypingSource).toContain('logs: {');
    expect(appTypingSource).toContain('manualRefresh: string');
    expect(appTypingSource).toContain('create: string');
    expect(appTypingSource).toContain('execute: string');
    expect(appTypingSource).toContain('retry: string');
    expect(appTypingSource).toContain('rollback: string');
    expect(appTypingSource).toContain('approve: string');
    expect(appTypingSource).toContain('reject: string');
    expect(appTypingSource).toContain('cancel: string');
    expect(appTypingSource).toContain('originTaskId: string');
    expect(zhLocaleSource).toContain('manualRefresh');
    expect(zhLocaleSource).toContain('originTaskId');
    expect(zhLocaleSource).toContain('create');
    expect(zhLocaleSource).toContain('approve');
    expect(zhLocaleSource).toContain('reject');
    expect(zhLocaleSource).toContain('rollback');
    expect(enLocaleSource).toContain('manualRefresh');
    expect(enLocaleSource).toContain('originTaskId');
    expect(enLocaleSource).toContain('create');
    expect(enLocaleSource).toContain('approve');
    expect(enLocaleSource).toContain('reject');
    expect(enLocaleSource).toContain('rollback');
    expect(appTypingSource).toContain('cancelled: string');
    expect(appTypingSource).toContain('rejected: string');
    expect(appTypingSource).toContain('sshUser: string');
    expect(appTypingSource).toContain('sshUserPlaceholder: string');
    expect(appTypingSource).toContain('sshPort: string');
    expect(appTypingSource).toContain('sshPortPlaceholder: string');
    expect(appTypingSource).toContain('privateKeyPath: string');
    expect(appTypingSource).toContain('privateKeyPathPlaceholder: string');
    expect(appTypingSource).toContain('remoteBaseDir: string');
    expect(appTypingSource).toContain('remoteBaseDirPlaceholder: string');
    expect(appTypingSource).toContain('rollbackCommand: string');
    expect(appTypingSource).toContain('rollbackCommandPlaceholder: string');
    expect(appTypingSource).toContain('sshUserRequired: string');
    expect(appTypingSource).toContain('sshPortInvalid: string');
    expect(appTypingSource).toContain('privateKeyPathRequired: string');
    expect(appTypingSource).toContain('remoteBaseDirRequired: string');
    expect(zhLocaleSource).toContain('SSH 用户');
    expect(zhLocaleSource).toContain('私钥路径');
    expect(zhLocaleSource).toContain('远端发布根目录');
    expect(zhLocaleSource).toContain('回滚命令');
    expect(enLocaleSource).toContain('SSH User');
    expect(enLocaleSource).toContain('Private Key Path');
    expect(enLocaleSource).toContain('Remote Release Root');
    expect(enLocaleSource).toContain('Rollback Command');
  });

  it('declares task center locale schema for unified actions filters table and drawer', () => {
    const taskCenterTypingBlock = extractSection(appTypingSource, 'taskCenter', 'trafficController');
    const taskCenterZhBlock = extractSection(zhLocaleSource, 'taskCenter', 'trafficController');
    const taskCenterEnBlock = extractSection(enLocaleSource, 'taskCenter', 'trafficController');
    const taskCenterTypingFiltersBlock = extractSection(taskCenterTypingBlock, 'filters', 'taskTypes');
    const taskCenterZhFiltersBlock = extractSection(taskCenterZhBlock, 'filters', 'taskTypes');
    const taskCenterEnFiltersBlock = extractSection(taskCenterEnBlock, 'filters', 'taskTypes');

    expect(taskCenterTypingBlock).toContain('actions: {');
    expect(taskCenterTypingBlock).toContain('openTaskDetail: string');
    expect(taskCenterTypingBlock).toContain('openSourceDetail: string');
    expect(taskCenterTypingBlock).toContain('filters: {');
    expect(taskCenterTypingFiltersBlock).toContain('keyword: string');
    expect(taskCenterTypingFiltersBlock).toContain('status: string');
    expect(taskCenterTypingFiltersBlock).toContain('taskType: string');
    expect(taskCenterTypingFiltersBlock).toContain('startedFrom: string');
    expect(taskCenterTypingFiltersBlock).toContain('startedTo: string');
    expect(taskCenterTypingFiltersBlock).toContain('search: string');
    expect(taskCenterTypingFiltersBlock).toContain('reset: string');
    expect(taskCenterTypingFiltersBlock).not.toContain('priority: string');
    expect(taskCenterTypingBlock).toContain('taskTypes: {');
    expect(taskCenterTypingBlock).toContain('deploy: string');
    expect(taskCenterTypingBlock).toContain('databaseConnectivity: string');
    expect(taskCenterTypingBlock).toContain('trafficAction: string');
    expect(taskCenterTypingBlock).toContain('table: {');
    expect(taskCenterTypingBlock).toContain('taskName: string');
    expect(taskCenterTypingBlock).toContain('taskType: string');
    expect(taskCenterTypingBlock).toContain('triggeredBy: string');
    expect(taskCenterTypingBlock).toContain('startedAt: string');
    expect(taskCenterTypingBlock).toContain('finishedAt: string');
    expect(taskCenterTypingBlock).toContain('summary: string');
    expect(taskCenterTypingBlock).toContain('status: string');
    expect(taskCenterTypingBlock).toContain('drawer: {');
    expect(taskCenterTypingBlock).toContain('title: string');
    expect(taskCenterTypingBlock).toContain('taskType: string');
    expect(taskCenterTypingBlock).toContain('taskName: string');
    expect(taskCenterTypingBlock).toContain('triggeredBy: string');
    expect(taskCenterTypingBlock).toContain('startedAt: string');
    expect(taskCenterTypingBlock).toContain('finishedAt: string');
    expect(taskCenterTypingBlock).toContain('summary: string');
    expect(taskCenterTypingBlock).toContain('errorSummary: string');
    expect(taskCenterTypingBlock).toContain('detailPreview: string');

    expect(taskCenterZhBlock).toContain('openTaskDetail');
    expect(taskCenterZhBlock).toContain('openSourceDetail');
    expect(taskCenterZhFiltersBlock).toContain('startedFrom');
    expect(taskCenterZhFiltersBlock).toContain('startedTo');
    expect(taskCenterZhFiltersBlock).not.toContain('priority');
    expect(taskCenterZhBlock).toContain('taskTypes');
    expect(taskCenterZhBlock).toContain('databaseConnectivity');
    expect(taskCenterZhBlock).toContain('trafficAction');
    expect(taskCenterZhBlock).toContain('taskName');
    expect(taskCenterZhBlock).toContain('triggeredBy');
    expect(taskCenterZhBlock).toContain('drawer');
    expect(taskCenterZhBlock).toContain('detailPreview');

    expect(taskCenterEnBlock).toContain('openTaskDetail');
    expect(taskCenterEnBlock).toContain('openSourceDetail');
    expect(taskCenterEnFiltersBlock).toContain('startedFrom');
    expect(taskCenterEnFiltersBlock).toContain('startedTo');
    expect(taskCenterEnFiltersBlock).not.toContain('priority');
    expect(taskCenterEnBlock).toContain('taskTypes');
    expect(taskCenterEnBlock).toContain('databaseConnectivity');
    expect(taskCenterEnBlock).toContain('trafficAction');
    expect(taskCenterEnBlock).toContain('taskName');
    expect(taskCenterEnBlock).toContain('triggeredBy');
    expect(taskCenterEnBlock).toContain('drawer');
    expect(taskCenterEnBlock).toContain('detailPreview');
  });

  it('adds deploy task detail tabs local filters and guarded auto refresh wiring', () => {
    expect(deployTaskPage).toContain('activeDetailTab');
    expect(deployTaskPage).toContain('fetchGetDeployTask(');
    expect(deployTaskPage).toContain('fetchGetDeployTaskHosts(');
    expect(deployTaskPage).toContain('fetchGetDeployTaskLogs(');
    expect(deployTaskPage).toContain('const hostsPage = ref<Api.Task.DeployTaskHostPage>');
    expect(deployTaskPage).toContain('const logsPage = ref<Api.Task.DeployTaskLogPage>');
    expect(deployTaskPage).toContain('const hostQuery = reactive');
    expect(deployTaskPage).toContain('const logQuery = reactive');
    expect(deployTaskPage).toContain('const detailRefreshTimer = ref');
    expect(deployTaskPage).toContain('const detailRequestInFlight = ref(false)');
    expect(deployTaskPage).toContain('const hostsRequestInFlight = ref(false)');
    expect(deployTaskPage).toContain('const logsRequestInFlight = ref(false)');
    expect(deployTaskPage).toContain('document.visibilityState');
    expect(deployTaskPage).toContain('window.setInterval');
    expect(deployTaskPage).toContain('window.clearInterval');
    expect(deployTaskPage).toContain('hostsError');
    expect(deployTaskPage).toContain('logsError');
    expect(deployTaskPage).toContain('NTabPane');
  });

  it('uses latest-only hosts/logs refresh strict polling status boundaries and 10-item detail paging defaults', () => {
    expect(deployTaskPage).toContain('const DETAIL_PAGE_SIZE = 10');
    expect(deployTaskPage).toContain('const hostsRequestToken = ref(0)');
    expect(deployTaskPage).toContain('const hostsLoadingToken = ref(0)');
    expect(deployTaskPage).toContain('const logsRequestToken = ref(0)');
    expect(deployTaskPage).toContain('const logsLoadingToken = ref(0)');
    expect(deployTaskPage).toContain('const requestToken = ++hostsRequestToken.value');
    expect(deployTaskPage).toContain('const loadingToken = ++hostsLoadingToken.value');
    expect(deployTaskPage).toContain('const requestToken = ++logsRequestToken.value');
    expect(deployTaskPage).toContain('const loadingToken = ++logsLoadingToken.value');
    expect(deployTaskPage).toContain(
      'if (requestToken !== hostsRequestToken.value || activeTaskId.value !== taskId) {'
    );
    expect(deployTaskPage).toContain('if (requestToken !== logsRequestToken.value || activeTaskId.value !== taskId) {');
    expect(deployTaskPage).toContain(
      "return normalizedStatus === 'RUNNING' || normalizedStatus === 'CANCEL_REQUESTED'"
    );
    expect(deployTaskPage).toContain("t('page.envops.deployTask.progress.totalHosts')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.progress.pendingHosts')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.progress.runningHosts')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.progress.successHosts')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.progress.failedHosts')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.progress.cancelledHosts')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.error.detailLoadFailed')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.error.hostsLoadFailed')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.error.logsLoadFailed')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.error.autoRefreshFailed')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.empty.taskNotFound')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.empty.noHosts')");
    expect(deployTaskPage).toContain("t('page.envops.deployTask.empty.noLogs')");
    expect(deployTaskPage).toMatch(
      /const hostQuery = reactive\(\{[\s\S]*?page: DETAIL_PAGE,[\s\S]*?pageSize: DETAIL_PAGE_SIZE[\s\S]*?\}\)/s
    );
    expect(deployTaskPage).toMatch(
      /const logQuery = reactive\(\{[\s\S]*?page: DETAIL_PAGE,[\s\S]*?pageSize: DETAIL_PAGE_SIZE[\s\S]*?\}\)/s
    );
    expect(deployTaskPage).not.toContain('if (hostsRequestInFlight.value && !force) {');
    expect(deployTaskPage).not.toContain('if (logsRequestInFlight.value && !force) {');
  });

  it('loads log host filter options from dedicated paged task hosts', () => {
    expect(deployTaskPage).toMatch(/const LOG_HOST_OPTIONS_PAGE_SIZE = \d+/);
    expect(deployTaskPage).toContain('const logHosts = ref<Api.Task.DeployTaskHostRecord[]>([])');
    expect(deployTaskPage).toContain('const logHostsRequestToken = ref(0)');
    expect(deployTaskPage).toContain('logHosts.value.forEach(item => {');
    expect(deployTaskPage).not.toContain('taskHosts.value.forEach(item => {');
    expect(deployTaskPage).toContain('async function loadLogHosts(taskId: number)');
    expect(deployTaskPage).toContain('const nextLogHosts: Api.Task.DeployTaskHostRecord[] = []');
    expect(deployTaskPage).toContain('let page = DETAIL_PAGE');
    expect(deployTaskPage).toContain('while (true) {');
    expect(deployTaskPage).toContain('fetchGetDeployTaskHosts(taskId, { page, pageSize: LOG_HOST_OPTIONS_PAGE_SIZE })');
    expect(deployTaskPage).toContain(
      'if (requestToken !== logHostsRequestToken.value || activeTaskId.value !== taskId) {'
    );
    expect(deployTaskPage).toContain('const records = getDeployTaskHostRecords(nextLogHostsPage)');
    expect(deployTaskPage).toContain('if (!records.length) {');
    expect(deployTaskPage).toContain('nextLogHosts.push(...records)');
    expect(deployTaskPage).toContain(
      'const loadedAllLogHosts = nextLogHostsPage.total > 0 && nextLogHosts.length >= nextLogHostsPage.total'
    );
    expect(deployTaskPage).toContain('const reachedLastLogHostsPage = records.length < nextLogHostsPage.pageSize');
    expect(deployTaskPage).toContain('page = nextLogHostsPage.page + 1');
    expect(deployTaskPage).toContain('logHosts.value = nextLogHosts');
    expect(deployTaskPage).toContain('logHosts.value = []');
    expect(deployTaskPage).toContain('loadLogHosts(taskId)');
    expect(deployTaskPage).not.toContain(
      'const pageSize = Math.max(activeTask.value?.totalHosts ?? 0, DETAIL_PAGE_SIZE)'
    );
    expect(deployTaskPage).not.toContain('fetchGetDeployTaskHosts(taskId, { page: DETAIL_PAGE, pageSize })');
  });

  it('declares deploy task task-5 locale schema for tabs progress errors and empty states', () => {
    expect(appTypingSource).toMatch(
      /deployTask:\s*\{[\s\S]*?tabs:\s*\{[\s\S]*?overview: string[\s\S]*?hosts: string[\s\S]*?logs: string/s
    );
    expect(appTypingSource).toMatch(
      /deployTask:\s*\{[\s\S]*?progress:\s*\{[\s\S]*?totalHosts: string[\s\S]*?pendingHosts: string[\s\S]*?runningHosts: string[\s\S]*?successHosts: string[\s\S]*?failedHosts: string[\s\S]*?cancelledHosts: string/s
    );
    expect(appTypingSource).toMatch(
      /deployTask:\s*\{[\s\S]*?error:\s*\{[\s\S]*?detailLoadFailed: string[\s\S]*?hostsLoadFailed: string[\s\S]*?logsLoadFailed: string[\s\S]*?autoRefreshFailed: string/s
    );
    expect(appTypingSource).toMatch(
      /deployTask:\s*\{[\s\S]*?empty:\s*\{[\s\S]*?taskNotFound: string[\s\S]*?noHosts: string[\s\S]*?noLogs: string/s
    );
    expect(zhLocaleSource).toMatch(/deployTask:\s*\{[\s\S]*?tabs:\s*\{[\s\S]*?overview:[\s\S]*?hosts:[\s\S]*?logs:/s);
    expect(zhLocaleSource).toMatch(
      /deployTask:\s*\{[\s\S]*?progress:\s*\{[\s\S]*?totalHosts:[\s\S]*?pendingHosts:[\s\S]*?runningHosts:[\s\S]*?successHosts:[\s\S]*?failedHosts:[\s\S]*?cancelledHosts:/s
    );
    expect(zhLocaleSource).toMatch(
      /deployTask:\s*\{[\s\S]*?error:\s*\{[\s\S]*?detailLoadFailed:[\s\S]*?hostsLoadFailed:[\s\S]*?logsLoadFailed:[\s\S]*?autoRefreshFailed:/s
    );
    expect(zhLocaleSource).toMatch(
      /deployTask:\s*\{[\s\S]*?empty:\s*\{[\s\S]*?taskNotFound:[\s\S]*?noHosts:[\s\S]*?noLogs:/s
    );
    expect(enLocaleSource).toMatch(/deployTask:\s*\{[\s\S]*?tabs:\s*\{[\s\S]*?overview:[\s\S]*?hosts:[\s\S]*?logs:/s);
    expect(enLocaleSource).toMatch(
      /deployTask:\s*\{[\s\S]*?progress:\s*\{[\s\S]*?totalHosts:[\s\S]*?pendingHosts:[\s\S]*?runningHosts:[\s\S]*?successHosts:[\s\S]*?failedHosts:[\s\S]*?cancelledHosts:/s
    );
    expect(enLocaleSource).toMatch(
      /deployTask:\s*\{[\s\S]*?error:\s*\{[\s\S]*?detailLoadFailed:[\s\S]*?hostsLoadFailed:[\s\S]*?logsLoadFailed:[\s\S]*?autoRefreshFailed:/s
    );
    expect(enLocaleSource).toMatch(
      /deployTask:\s*\{[\s\S]*?empty:\s*\{[\s\S]*?taskNotFound:[\s\S]*?noHosts:[\s\S]*?noLogs:/s
    );
  });

  it('keeps deploy task locale schema aligned with planned filters and sorting structure', () => {
    expect(appTypingSource).toContain('filters: {');
    expect(appTypingSource).toContain('application: string');
    expect(appTypingSource).toContain('createdRange: string');
    expect(appTypingSource).toContain('search: string');
    expect(appTypingSource).toContain('reset: string');
    expect(appTypingSource).toContain('sorting: {');
    expect(appTypingSource).toContain('createdAt: string');
    expect(appTypingSource).toContain('updatedAt: string');
    expect(appTypingSource).toContain('taskNo: string');
    expect(appTypingSource).toContain('keyword: string');
    expect(appTypingSource).not.toContain('sortByCreatedAt: string');
    expect(appTypingSource).not.toContain('sortByUpdatedAt: string');
    expect(appTypingSource).not.toContain('sortByTaskId: string');
    expect(appTypingSource).not.toContain('sortByStatus: string');
    expect(appTypingSource).not.toContain('sortOrderAsc: string');
    expect(appTypingSource).not.toContain('sortOrderDesc: string');
    expect(zhLocaleSource).toContain('application');
    expect(zhLocaleSource).toContain('createdRange');
    expect(zhLocaleSource).toContain('sorting');
    expect(enLocaleSource).toContain('application');
    expect(enLocaleSource).toContain('createdRange');
    expect(enLocaleSource).toContain('sorting');
  });

  it('keeps task center copy aligned with the limited unified scope we actually ship', () => {
    const taskCenterZhBlock = extractSection(zhLocaleSource, 'taskCenter', 'trafficController');
    const taskCenterEnBlock = extractSection(enLocaleSource, 'taskCenter', 'trafficController');

    expect(taskCenterZhBlock).toContain('统一任务中心');
    expect(taskCenterZhBlock).toContain('Deploy');
    expect(taskCenterZhBlock).toContain('数据库连通性检测');
    expect(taskCenterZhBlock).toContain('Traffic 动作');
    expect(taskCenterZhBlock).not.toContain('全域');
    expect(taskCenterZhBlock).not.toContain('跨域任务追踪视图');
    expect(taskCenterEnBlock).toContain('Unified Task Center');
    expect(taskCenterEnBlock).toContain('Deploy');
    expect(taskCenterEnBlock).toContain('Database Connectivity');
    expect(taskCenterEnBlock).toContain('Traffic Action');
    expect(taskCenterEnBlock).not.toContain('global task center');
    expect(taskCenterEnBlock).not.toContain('cross-domain task tracing');
  });
});

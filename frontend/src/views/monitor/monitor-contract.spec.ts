import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createApp, defineComponent, h, nextTick, reactive } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const assetHostFile = path.resolve(__dirname, '../asset/host/index.vue');
const detectTaskFile = path.resolve(__dirname, 'detect-task/index.vue');
const metricFile = path.resolve(__dirname, 'metric/index.vue');
const assetTypingFile = path.resolve(__dirname, '../../typings/api/asset.d.ts');
const appTypingFile = path.resolve(__dirname, '../../typings/app.d.ts');
const assetApiFile = path.resolve(__dirname, '../../service/api/asset.ts');
const monitorTypingFile = path.resolve(__dirname, '../../typings/api/monitor.d.ts');
const monitorApiFile = path.resolve(__dirname, '../../service/api/monitor.ts');
const zhLocaleFile = path.resolve(__dirname, '../../locales/langs/zh-cn.ts');
const enLocaleFile = path.resolve(__dirname, '../../locales/langs/en-us.ts');

const assetHostSource = readFileSync(assetHostFile, 'utf8');
const detectTaskSource = readFileSync(detectTaskFile, 'utf8');
const metricSource = readFileSync(metricFile, 'utf8');
const assetTypingSource = readFileSync(assetTypingFile, 'utf8');
const appTypingSource = readFileSync(appTypingFile, 'utf8');
const assetApiSource = readFileSync(assetApiFile, 'utf8');
const monitorTypingSource = readFileSync(monitorTypingFile, 'utf8');
const monitorApiSource = readFileSync(monitorApiFile, 'utf8');
const zhLocaleSource = readFileSync(zhLocaleFile, 'utf8');
const enLocaleSource = readFileSync(enLocaleFile, 'utf8');

const mocks = vi.hoisted(() => {
  const route = { query: {} as Record<string, unknown> };
  const routerPushByKey = vi.fn(async (_key: string, payload?: { query?: Record<string, string> }) => {
    mocks.route.query = payload?.query ?? {};
  });
  const fetchGetAssetHosts = vi.fn();
  const fetchCreateAssetHost = vi.fn();
  const fetchGetMonitorHostFactsLatest = vi.fn();
  const fetchGetMonitorDetectTasks = vi.fn();
  const fetchPostCreateMonitorDetectTask = vi.fn();
  const fetchPostExecuteMonitorDetectTask = vi.fn();

  return {
    route,
    routerPushByKey,
    fetchGetAssetHosts,
    fetchCreateAssetHost,
    fetchGetMonitorHostFactsLatest,
    fetchGetMonitorDetectTasks,
    fetchPostCreateMonitorDetectTask,
    fetchPostExecuteMonitorDetectTask
  };
});

vi.mock('vue-router', async () => {
  const route = reactive(mocks.route);
  mocks.route = route;

  return {
    useRoute: () => route
  };
});

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, unknown>) => {
      if (key === 'page.envops.monitorMetric.tags.host') {
        return `Host #${params?.id ?? '-'}`;
      }

      if (key === 'page.envops.assetHost.table.viewMetrics') {
        return 'View Metrics';
      }

      if (key === 'page.envops.assetHost.form.actions.create') {
        return 'Onboard Host';
      }

      if (key === 'page.envops.monitorDetectTask.actions.execute') {
        return 'Run Now';
      }

      if (key === 'page.envops.monitorDetectTask.actions.create') {
        return 'Create Task';
      }

      if (key === 'common.noData') {
        return 'No Data';
      }

      return key;
    }
  })
}));

vi.mock('@/hooks/common/router', () => ({
  useRouterPush: () => ({
    routerPushByKey: mocks.routerPushByKey
  })
}));

vi.mock('@/service/api', () => ({
  fetchGetAssetHosts: mocks.fetchGetAssetHosts,
  fetchCreateAssetHost: mocks.fetchCreateAssetHost,
  fetchGetMonitorHostFactsLatest: mocks.fetchGetMonitorHostFactsLatest,
  fetchGetMonitorDetectTasks: mocks.fetchGetMonitorDetectTasks,
  fetchPostCreateMonitorDetectTask: mocks.fetchPostCreateMonitorDetectTask,
  fetchPostExecuteMonitorDetectTask: mocks.fetchPostExecuteMonitorDetectTask
}));

const passthroughStub = defineComponent({
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () => h('div', attrs, slots.default?.());
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
    },
    text: {
      type: Boolean,
      default: false
    },
    type: {
      type: String,
      default: undefined
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
      type: String,
      default: ''
    }
  },
  emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () =>
      h('input', {
        ...attrs,
        value: props.value,
        onInput: (event: Event) => emit('update:value', (event.target as HTMLInputElement).value)
      });
  }
});

const selectStub = defineComponent({
  inheritAttrs: false,
  props: {
    value: {
      type: [String, Number, null],
      default: null
    },
    options: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () =>
      h(
        'select',
        {
          ...attrs,
          value: props.value ?? '',
          onChange: (event: Event) => {
            const target = event.target as HTMLSelectElement;
            const matched = props.options.find((option: any) => String(option.value) === target.value) as any;
            emit('update:value', matched ? matched.value : target.value);
          }
        },
        props.options.map((option: any) => h('option', { value: String(option.value) }, option.label))
      );
  }
});

const statisticStub = defineComponent({
  inheritAttrs: false,
  props: {
    label: {
      type: String,
      default: ''
    },
    value: {
      type: [String, Number],
      default: ''
    }
  },
  setup(props, { attrs }) {
    return () =>
      h('div', attrs, [
        h('div', { 'data-stat-label': props.label }, props.label),
        h('div', { 'data-stat-value': String(props.value) }, String(props.value))
      ]);
  }
});

const tableStub = defineComponent({
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () => h('table', attrs, slots.default?.());
  }
});

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0));
}

async function settleRender() {
  await flushPromises();
  await nextTick();
  await flushPromises();
  await nextTick();
}

async function mountAssetHostPage() {
  const { default: AssetHostPage } = await import('../asset/host/index.vue');
  const container = document.createElement('div');
  document.body.appendChild(container);
  const app = createApp(AssetHostPage);

  ['NSpace', 'NCard', 'NGrid', 'NGi', 'NSpin', 'NEmpty', 'NTag', 'NForm', 'NFormItem'].forEach(name => {
    app.component(name, passthroughStub);
  });
  app.component('NButton', buttonStub);
  app.component('NInput', inputStub);
  app.component('NSelect', selectStub);
  app.component('NStatistic', statisticStub);
  app.component('NTable', tableStub);

  app.mount(container);
  await settleRender();

  return {
    container,
    unmount() {
      app.unmount();
      container.remove();
    }
  };
}

async function mountDetectTaskPage() {
  const { default: MonitorDetectTaskPage } = await import('./detect-task/index.vue');
  const container = document.createElement('div');
  document.body.appendChild(container);
  const app = createApp(MonitorDetectTaskPage);

  ['NSpace', 'NCard', 'NGrid', 'NGi', 'NSpin', 'NEmpty', 'NTag', 'NForm', 'NFormItem'].forEach(name => {
    app.component(name, passthroughStub);
  });
  app.component('NButton', buttonStub);
  app.component('NInput', inputStub);
  app.component('NSelect', selectStub);
  app.component('NStatistic', statisticStub);
  app.component('NTable', tableStub);

  app.mount(container);
  await settleRender();

  return {
    container,
    unmount() {
      app.unmount();
      container.remove();
    }
  };
}

async function mountMetricPage() {
  const { default: MonitorMetricPage } = await import('./metric/index.vue');
  const container = document.createElement('div');
  document.body.appendChild(container);
  const app = createApp(MonitorMetricPage);

  ['NSpace', 'NCard', 'NGrid', 'NGi', 'NSpin', 'NEmpty', 'NTag'].forEach(name => {
    app.component(name, passthroughStub);
  });
  app.component('NButton', buttonStub);
  app.component('NStatistic', statisticStub);
  app.component('NTable', tableStub);

  app.mount(container);
  await settleRender();

  return {
    container,
    unmount() {
      app.unmount();
      container.remove();
    }
  };
}

describe('monitor backend contract wiring', () => {
  it('aligns detect task page, API helpers and typings with backend detect task fields', () => {
    expect(monitorTypingSource).toContain('interface CreateDetectTaskParams');
    expect(monitorTypingSource).toContain('target?: string | null;');
    expect(monitorTypingSource).toContain('lastRunAt?: string | null;');
    expect(monitorTypingSource).toContain('lastResult?: string | null;');
    expect(monitorTypingSource).not.toContain('taskNo: string;');
    expect(monitorTypingSource).not.toContain('taskType: string;');
    expect(monitorTypingSource).not.toContain('targetName?: string | null;');
    expect(monitorTypingSource).not.toContain('targetNames?: string[] | null;');
    expect(monitorTypingSource).not.toContain('status: string;');
    expect(monitorTypingSource).not.toContain('startedAt?: string | null;');
    expect(monitorTypingSource).not.toContain('finishedAt?: string | null;');
    expect(monitorTypingSource).not.toContain('updatedAt?: string | null;');

    expect(monitorApiSource).toContain('fetchPostCreateMonitorDetectTask');
    expect(monitorApiSource).toContain("url: '/api/monitor/detect-tasks'");
    expect(monitorApiSource).toContain('fetchPostExecuteMonitorDetectTask');
    expect(monitorApiSource).toContain('url: `/api/monitor/detect-tasks/${id}/execute`');

    expect(detectTaskSource).toContain('fetchPostCreateMonitorDetectTask');
    expect(detectTaskSource).toContain('fetchPostExecuteMonitorDetectTask');
    expect(detectTaskSource).toContain('fetchGetAssetHosts({ current: 1, size: 100 })');
    expect(detectTaskSource).toContain('formModel.taskName.trim()');
    expect(detectTaskSource).toContain('handleCreateTask');
    expect(detectTaskSource).toContain('handleExecuteTask');
    expect(detectTaskSource).toContain('await loadDetectTasks()');
    expect(detectTaskSource).toContain('item.lastRunAt || item.createdAt');
    expect(detectTaskSource).toContain("String(item.lastResult || '')");
    expect(detectTaskSource).not.toContain('item.taskNo');
    expect(detectTaskSource).not.toContain('item.targetName');
    expect(detectTaskSource).not.toContain('item.targetNames');
    expect(detectTaskSource).not.toContain('item.targetCount');
    expect(detectTaskSource).not.toContain('String(item.status');
    expect(detectTaskSource).not.toContain('item.startedAt');
    expect(detectTaskSource).not.toContain('item.finishedAt');
    expect(detectTaskSource).not.toContain('item.updatedAt');
  });

  it('aligns metric page and API typings with backend host fact fields', () => {
    expect(monitorTypingSource).toContain('hostName?: string | null;');
    expect(monitorTypingSource).toContain('osName?: string | null;');
    expect(monitorTypingSource).toContain('agentVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('hostnameFact?: string | null;');
    expect(monitorTypingSource).not.toContain('cpuModel?: string | null;');
    expect(monitorTypingSource).not.toContain('diskGb?: number | null;');
    expect(monitorTypingSource).not.toContain('jdkVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('dockerVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('nginxVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('redisVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('mysqlVersion?: string | null;');

    expect(metricSource).toContain('hostFacts.value?.hostName');
    expect(metricSource).toContain('hostFacts.value?.osName');
    expect(metricSource).toContain('hostFacts.value?.agentVersion');
    expect(metricSource).not.toContain('hostFacts.value?.hostnameFact');
    expect(metricSource).not.toContain('hostFacts.value?.cpuModel');
    expect(metricSource).not.toContain('hostFacts.value?.diskGb');
    expect(metricSource).not.toContain('hostFacts.value?.jdkVersion');
    expect(metricSource).not.toContain('hostFacts.value?.dockerVersion');
    expect(metricSource).not.toContain('hostFacts.value?.nginxVersion');
    expect(metricSource).not.toContain('hostFacts.value?.redisVersion');
    expect(metricSource).not.toContain('hostFacts.value?.mysqlVersion');
  });
});

describe('monitor asset-to-metric flow contract', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.resetModules();
    document.body.innerHTML = '';
    mocks.route.query = {};
    window.$message = {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
      loading: vi.fn(),
      destroyAll: vi.fn()
    } as unknown as typeof window.$message;

    mocks.fetchGetAssetHosts.mockResolvedValue({
      error: null,
      data: {
        current: 1,
        size: 10,
        total: 3,
        summary: {
          managedHosts: 3,
          onlineHosts: 2,
          warningHosts: 1
        },
        records: [
          {
            id: 1,
            hostName: 'host-prd-01',
            ipAddress: '10.20.1.11',
            environment: 'production',
            clusterName: 'cn-shanghai-a',
            ownerName: 'EnvOps',
            status: 'online',
            lastHeartbeat: '2026-04-15T09:32:00',
            hasMonitorFacts: true,
            latestMonitorFactAt: '2026-04-16T08:45:00'
          },
          {
            id: 3,
            hostName: 'host-stg-01',
            ipAddress: '10.30.2.18',
            environment: 'staging',
            clusterName: 'cn-beijing-b',
            ownerName: 'Release Team',
            status: 'online',
            hasMonitorFacts: false,
            latestMonitorFactAt: null
          },
          {
            id: 2,
            hostName: 'host-prd-02',
            ipAddress: '10.20.1.12',
            environment: 'production',
            clusterName: 'cn-shanghai-a',
            ownerName: 'Traffic Team',
            status: 'warning',
            hasMonitorFacts: true,
            latestMonitorFactAt: '2026-04-16T08:30:00'
          }
        ]
      }
    });

    mocks.fetchCreateAssetHost.mockResolvedValue({
      error: null,
      data: {
        id: 5,
        hostName: 'host-new-01',
        ipAddress: '10.60.1.20',
        environment: 'sandbox',
        clusterName: 'cn-shenzhen-a',
        ownerName: 'Asset Team',
        status: 'online',
        lastHeartbeat: '2026-04-16T11:22:33',
        hasMonitorFacts: false,
        latestMonitorFactAt: null
      }
    });

    mocks.fetchGetMonitorHostFactsLatest.mockResolvedValue({
      error: null,
      data: {
        id: 101,
        hostId: 2,
        hostName: 'host-prd-02',
        osName: 'Alibaba Cloud Linux 3',
        kernelVersion: '5.10.134-18.al8',
        cpuCores: 16,
        memoryMb: 32768,
        agentVersion: '1.0.0',
        collectedAt: '2026-04-16T08:30:00'
      }
    });

    mocks.fetchGetMonitorDetectTasks.mockResolvedValue({
      error: null,
      data: [
        {
          id: 1,
          taskName: 'host-baseline',
          hostId: 1,
          target: 'host-prd-01',
          schedule: 'every_5m',
          lastRunAt: '2026-04-16T08:45:00',
          lastResult: 'success',
          createdAt: '2026-04-16T08:40:00'
        }
      ]
    });

    mocks.fetchPostCreateMonitorDetectTask.mockResolvedValue({
      error: null,
      data: {
        id: 2,
        taskName: 'sandbox-probe',
        hostId: 2,
        target: 'host-prd-02',
        schedule: 'manual',
        lastResult: 'pending',
        createdAt: '2026-04-16T09:00:00'
      }
    });

    mocks.fetchPostExecuteMonitorDetectTask.mockResolvedValue({
      error: null,
      data: {
        id: 1,
        taskName: 'host-baseline',
        hostId: 1,
        target: 'host-prd-01',
        schedule: 'every_5m',
        lastRunAt: '2026-04-16T08:50:00',
        lastResult: 'success',
        createdAt: '2026-04-16T08:40:00'
      }
    });
  });

  afterEach(() => {
    document.body.innerHTML = '';
    window.$message = undefined as unknown as typeof window.$message;
  });

  it('keeps asset host create flow and metric page wired to backend contracts', () => {
    expect(assetTypingSource).toContain('interface CreateHostParams');
    expect(assetTypingSource).toContain('hasMonitorFacts?: boolean;');
    expect(assetTypingSource).toContain('latestMonitorFactAt?: string | null;');
    expect(appTypingSource).toContain('assetHost: {');
    expect(appTypingSource).toContain('form: {');
    expect(appTypingSource).toContain('createSuccess: string;');
    expect(appTypingSource).toContain('viewMetrics: string;');
    expect(assetApiSource).toContain('fetchCreateAssetHost');
    expect(assetApiSource).toContain("url: '/api/assets/hosts'");
    expect(monitorTypingSource).toContain('hostName?: string | null;');

    expect(assetHostSource).toContain('fetchCreateAssetHost');
    expect(assetHostSource).toContain('handleCreateHost');
    expect(assetHostSource).toContain('formModel.hostName.trim()');
    expect(assetHostSource).toContain('window.$message?.success');
    expect(assetHostSource).toContain("routerPushByKey('monitor_metric'");
    expect(assetHostSource).toContain('hostId: String(hostId)');
    expect(assetHostSource).toContain('Boolean(item.hasMonitorFacts)');
    expect(assetHostSource).toContain('formatDateTime(item.latestMonitorFactAt)');
    expect(assetHostSource).toContain("t('page.envops.assetHost.table.viewMetrics')");

    expect(metricSource).toContain('useRoute(');
    expect(metricSource).toContain('normalizeHostId(route.query.hostId)');
    expect(metricSource).toContain('fetchGetAssetHosts({ current: 1, size: 100 })');
    expect(metricSource).toContain('hostRecords.find(item => item.hasMonitorFacts)');
    expect(metricSource).toContain('fetchGetMonitorHostFactsLatest(currentHostId)');
    expect(metricSource).not.toContain('const hostId = 1;');
  });

  it('creates an asset host and keeps metric routing available on existing rows', async () => {
    const page = await mountAssetHostPage();

    expect(mocks.fetchGetAssetHosts).toHaveBeenCalledTimes(1);
    expect(page.container.textContent).toContain('host-prd-01');
    expect(page.container.textContent).toContain('2026-04-16 08:45:00');

    const inputs = Array.from(page.container.querySelectorAll('input'));
    const [hostNameInput, ipInput, clusterInput, ownerInput, lastHeartbeatInput] = inputs;

    if (hostNameInput) {
      hostNameInput.value = 'host-new-01';
      hostNameInput.dispatchEvent(new Event('input', { bubbles: true }));
    }

    if (ipInput) {
      ipInput.value = '10.60.1.20';
      ipInput.dispatchEvent(new Event('input', { bubbles: true }));
    }

    if (clusterInput) {
      clusterInput.value = 'cn-shenzhen-a';
      clusterInput.dispatchEvent(new Event('input', { bubbles: true }));
    }

    if (ownerInput) {
      ownerInput.value = 'Asset Team';
      ownerInput.dispatchEvent(new Event('input', { bubbles: true }));
    }

    if (lastHeartbeatInput) {
      lastHeartbeatInput.value = '2026-04-16T11:22:33';
      lastHeartbeatInput.dispatchEvent(new Event('input', { bubbles: true }));
    }

    await settleRender();

    const buttons = Array.from(page.container.querySelectorAll('button'));
    const createButton = buttons.find(button => button.textContent?.includes('Onboard Host'));
    createButton?.click();
    await settleRender();

    expect(mocks.fetchCreateAssetHost).toHaveBeenCalledWith({
      hostName: 'host-new-01',
      ipAddress: '10.60.1.20',
      environment: 'sandbox',
      clusterName: 'cn-shenzhen-a',
      ownerName: 'Asset Team',
      status: 'online',
      lastHeartbeat: '2026-04-16T11:22:33'
    });
    expect(mocks.fetchGetAssetHosts).toHaveBeenCalledTimes(2);

    const successMock = window.$message?.success as ReturnType<typeof vi.fn> | undefined;
    expect(successMock).toBeDefined();
    expect(successMock).toHaveBeenCalledTimes(1);

    const metricButtons = Array.from(page.container.querySelectorAll('button')).filter(button =>
      button.textContent?.includes('View Metrics')
    );

    expect(metricButtons).toHaveLength(3);
    expect(metricButtons[0]?.hasAttribute('disabled')).toBe(false);
    expect(metricButtons[1]?.hasAttribute('disabled')).toBe(true);

    metricButtons[0]?.click();
    await settleRender();

    expect(mocks.routerPushByKey).toHaveBeenCalledWith('monitor_metric', {
      query: {
        hostId: '1'
      }
    });

    page.unmount();
  });

  it('loads metric page by route hostId and falls back to the first host with monitor facts', async () => {
    mocks.route.query = { hostId: '2' };

    const page = await mountMetricPage();

    expect(mocks.fetchGetMonitorHostFactsLatest).toHaveBeenCalledWith(2);
    expect(page.container.textContent).toContain('host-prd-02');
    expect(page.container.textContent).toContain('Alibaba Cloud Linux 3');
    page.unmount();

    vi.clearAllMocks();
    mocks.route.query = {};
    mocks.fetchGetAssetHosts.mockResolvedValue({
      error: null,
      data: {
        current: 1,
        size: 10,
        total: 2,
        summary: {
          managedHosts: 2,
          onlineHosts: 1,
          warningHosts: 1
        },
        records: [
          {
            id: 3,
            hostName: 'host-stg-01',
            ipAddress: '10.30.2.18',
            environment: 'staging',
            clusterName: 'cn-beijing-b',
            ownerName: 'Release Team',
            status: 'online',
            hasMonitorFacts: false,
            latestMonitorFactAt: null
          },
          {
            id: 2,
            hostName: 'host-prd-02',
            ipAddress: '10.20.1.12',
            environment: 'production',
            clusterName: 'cn-shanghai-a',
            ownerName: 'Traffic Team',
            status: 'warning',
            hasMonitorFacts: true,
            latestMonitorFactAt: '2026-04-16T08:30:00'
          }
        ]
      }
    });
    mocks.fetchGetMonitorHostFactsLatest.mockResolvedValue({
      error: null,
      data: {
        id: 101,
        hostId: 2,
        hostName: 'host-prd-02',
        osName: 'Alibaba Cloud Linux 3',
        kernelVersion: '5.10.134-18.al8',
        cpuCores: 16,
        memoryMb: 32768,
        agentVersion: '1.0.0',
        collectedAt: '2026-04-16T08:30:00'
      }
    });

    const fallbackPage = await mountMetricPage();

    expect(mocks.fetchGetAssetHosts).toHaveBeenCalledWith({ current: 1, size: 100 });
    expect(mocks.fetchGetMonitorHostFactsLatest).toHaveBeenCalledWith(2);
    expect(fallbackPage.container.textContent).toContain('host-prd-02');

    fallbackPage.unmount();
  });

  it('wires detect task page to create and execute monitor tasks with list refresh', async () => {
    const page = await mountDetectTaskPage();

    expect(mocks.fetchGetMonitorDetectTasks).toHaveBeenCalledTimes(1);
    expect(mocks.fetchGetAssetHosts).toHaveBeenCalledWith({ current: 1, size: 100 });
    expect(page.container.textContent).toContain('host-baseline');
    expect(page.container.textContent).toContain('host-prd-01');

    const input = page.container.querySelector('input');
    input?.dispatchEvent(new Event('input', { bubbles: true }));
    if (input) {
      input.value = 'sandbox-probe';
      input.dispatchEvent(new Event('input', { bubbles: true }));
    }

    const selects = Array.from(page.container.querySelectorAll('select'));
    const hostSelect = selects[0];
    const scheduleSelect = selects[1];

    if (hostSelect) {
      hostSelect.value = '2';
      hostSelect.dispatchEvent(new Event('change', { bubbles: true }));
    }

    if (scheduleSelect) {
      scheduleSelect.value = 'manual';
      scheduleSelect.dispatchEvent(new Event('change', { bubbles: true }));
    }

    await settleRender();

    const buttons = Array.from(page.container.querySelectorAll('button'));
    const createButton = buttons.find(button => button.textContent?.includes('Create Task'));
    createButton?.click();
    await settleRender();

    expect(mocks.fetchPostCreateMonitorDetectTask).toHaveBeenCalledWith({
      taskName: 'sandbox-probe',
      hostId: 2,
      schedule: 'manual'
    });
    expect(mocks.fetchGetMonitorDetectTasks).toHaveBeenCalledTimes(2);

    const executeButton = Array.from(page.container.querySelectorAll('button')).find(button =>
      button.textContent?.includes('Run Now')
    );
    executeButton?.click();
    await settleRender();

    expect(mocks.fetchPostExecuteMonitorDetectTask).toHaveBeenCalledWith(1);
    expect(mocks.fetchGetMonitorDetectTasks).toHaveBeenCalledTimes(3);

    page.unmount();
  });

  it('updates localized copy to describe the real asset and monitor flows', () => {
    expect(zhLocaleSource).toContain('展示资产主机清单，支持新增纳管主机，并可直接进入已有监控快照的主机指标详情。');
    expect(zhLocaleSource).toContain("create: '纳管主机'");
    expect(zhLocaleSource).toContain("createSuccess: '主机纳管成功'");
    expect(zhLocaleSource).toContain('支持创建任务、手动执行，并回看最新执行结果。');
    expect(zhLocaleSource).toContain("attentionCount: '{count} 个待关注'");
    expect(zhLocaleSource).toContain("create: '创建任务'");
    expect(zhLocaleSource).toContain("execute: '立即执行'");
    expect(zhLocaleSource).toContain("operation: '操作'");

    expect(enLocaleSource).toContain(
      'Show managed hosts, add newly onboarded hosts and jump straight into metric details for hosts with monitor snapshots.'
    );
    expect(enLocaleSource).toContain("create: 'Onboard Host'");
    expect(enLocaleSource).toContain("createSuccess: 'Host onboarded successfully'");
    expect(enLocaleSource).toContain(
      'Use the live monitor task API to create tasks, run them manually and review the latest results.'
    );
    expect(enLocaleSource).toContain("attentionCount: '{count} need attention'");
    expect(enLocaleSource).toContain("create: 'Create Task'");
    expect(enLocaleSource).toContain("execute: 'Run Now'");
    expect(enLocaleSource).toContain("operation: 'Action'");
  });
});

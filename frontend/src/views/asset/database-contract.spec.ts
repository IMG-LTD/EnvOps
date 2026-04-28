import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createApp, defineComponent, h, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const databasePageSource = readFileSync(path.resolve(__dirname, 'database/index.vue'), 'utf8');
const assetApiSource = readFileSync(path.resolve(__dirname, '../../service/api/asset.ts'), 'utf8');
const assetTypingSource = readFileSync(path.resolve(__dirname, '../../typings/api/asset.d.ts'), 'utf8');
const apiIndexSource = readFileSync(path.resolve(__dirname, '../../service/api/index.ts'), 'utf8');
const routeSource = readFileSync(path.resolve(__dirname, '../../router/elegant/routes.ts'), 'utf8');
const importSource = readFileSync(path.resolve(__dirname, '../../router/elegant/imports.ts'), 'utf8');
const transformSource = readFileSync(path.resolve(__dirname, '../../router/elegant/transform.ts'), 'utf8');
const elegantTypingSource = readFileSync(path.resolve(__dirname, '../../typings/elegant-router.d.ts'), 'utf8');
const zhLocaleSource = readFileSync(path.resolve(__dirname, '../../locales/langs/zh-cn.ts'), 'utf8');
const enLocaleSource = readFileSync(path.resolve(__dirname, '../../locales/langs/en-us.ts'), 'utf8');

const mocks = vi.hoisted(() => {
  const fetchGetAssetDatabases = vi.fn();
  const fetchCreateAssetDatabase = vi.fn();
  const fetchUpdateAssetDatabase = vi.fn();
  const fetchDeleteAssetDatabase = vi.fn();
  const fetchGetAssetHosts = vi.fn();
  const fetchGetAssetCredentials = vi.fn();
  const fetchCheckAssetDatabase = vi.fn();
  const fetchCheckSelectedAssetDatabases = vi.fn();
  const fetchCheckCurrentPageAssetDatabases = vi.fn();
  const fetchCheckQueriedAssetDatabases = vi.fn();

  return {
    fetchGetAssetDatabases,
    fetchCreateAssetDatabase,
    fetchUpdateAssetDatabase,
    fetchDeleteAssetDatabase,
    fetchGetAssetHosts,
    fetchGetAssetCredentials,
    fetchCheckAssetDatabase,
    fetchCheckSelectedAssetDatabases,
    fetchCheckCurrentPageAssetDatabases,
    fetchCheckQueriedAssetDatabases
  };
});

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}));

vi.mock('@/service/api', () => ({
  fetchGetAssetDatabases: mocks.fetchGetAssetDatabases,
  fetchCreateAssetDatabase: mocks.fetchCreateAssetDatabase,
  fetchUpdateAssetDatabase: mocks.fetchUpdateAssetDatabase,
  fetchDeleteAssetDatabase: mocks.fetchDeleteAssetDatabase,
  fetchGetAssetHosts: mocks.fetchGetAssetHosts,
  fetchGetAssetCredentials: mocks.fetchGetAssetCredentials,
  fetchCheckAssetDatabase: mocks.fetchCheckAssetDatabase,
  fetchCheckSelectedAssetDatabases: mocks.fetchCheckSelectedAssetDatabases,
  fetchCheckCurrentPageAssetDatabases: mocks.fetchCheckCurrentPageAssetDatabases,
  fetchCheckQueriedAssetDatabases: mocks.fetchCheckQueriedAssetDatabases
}));

vi.mock('@/hooks/business/auth', () => ({
  useAuth: () => ({
    hasAuth: () => true,
    hasEveryAuth: () => true
  })
}));

const passthroughStub = defineComponent({
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () =>
      h(
        'div',
        attrs,
        Object.values(slots).flatMap(slot => slot?.() ?? [])
      );
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
      type: [String, Number, null],
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

const inputNumberStub = defineComponent({
  inheritAttrs: false,
  props: {
    value: {
      type: [Number, null],
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
          const nextValue = (event.target as HTMLInputElement).value;
          emit('update:value', nextValue === '' ? null : Number(nextValue));
        }
      });
  }
});

const paginationStub = defineComponent({
  inheritAttrs: false,
  props: {
    page: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    itemCount: {
      type: Number,
      default: 0
    }
  },
  setup(props, { attrs }) {
    return () =>
      h('div', attrs, [
        h('span', { 'data-pagination-page': String(props.page) }, String(props.page)),
        h('span', { 'data-pagination-size': String(props.pageSize) }, String(props.pageSize)),
        h('span', { 'data-pagination-total': String(props.itemCount) }, String(props.itemCount))
      ]);
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

async function mountDatabasePage() {
  const { default: AssetDatabasePage } = await import('./database/index.vue');
  const container = document.createElement('div');
  document.body.appendChild(container);
  const app = createApp(AssetDatabasePage);

  [
    'NSpace',
    'NCard',
    'NGrid',
    'NGi',
    'NSpin',
    'NTag',
    'NEmpty',
    'NForm',
    'NFormItem',
    'NDrawer',
    'NDrawerContent',
    'NModal',
    'NCheckbox'
  ].forEach(name => {
    app.component(name, passthroughStub);
  });
  app.component('NButton', buttonStub);
  app.component('NInput', inputStub);
  app.component('NInputNumber', inputNumberStub);
  app.component('NSelect', selectStub);
  app.component('NStatistic', statisticStub);
  app.component('NTable', tableStub);
  app.component('NPagination', paginationStub);

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

describe('asset database contract wiring', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.innerHTML = '';

    mocks.fetchGetAssetDatabases.mockResolvedValue({
      error: null,
      data: {
        current: 1,
        size: 10,
        total: 3,
        summary: {
          managedDatabases: 2,
          warningDatabases: 1,
          onlineDatabases: 1
        },
        records: [
          {
            id: 6,
            databaseName: 'session_hub',
            databaseType: 'redis',
            environment: 'sandbox',
            hostId: 4,
            hostName: 'host-sbx-01',
            port: 6379,
            instanceName: 'redis-sbx-a',
            credentialId: null,
            credentialName: null,
            ownerName: 'QA Team',
            lifecycleStatus: 'disabled',
            connectivityStatus: 'unknown',
            description: '沙箱会话缓存实例',
            lastCheckedAt: null,
            createdAt: '2026-04-14T12:10:00',
            updatedAt: '2026-04-14T12:10:00'
          },
          {
            id: 3,
            databaseName: 'billing_archive',
            databaseType: 'oracle',
            environment: 'staging',
            hostId: 3,
            hostName: 'host-stg-01',
            port: 1521,
            instanceName: 'oracle-stg-a',
            credentialId: 2,
            credentialName: 'demo-fake-staging-deploy-key',
            ownerName: 'Finance DBA',
            lifecycleStatus: 'managed',
            connectivityStatus: 'warning',
            description: '归档计费库',
            lastCheckedAt: '2026-04-18T07:40:00',
            createdAt: '2026-04-14T11:40:00',
            updatedAt: '2026-04-18T07:40:00'
          },
          {
            id: 1,
            databaseName: 'order_prod',
            databaseType: 'mysql',
            environment: 'production',
            hostId: 1,
            hostName: 'host-prd-01',
            port: 3306,
            instanceName: 'mysql-prd-a',
            credentialId: 1,
            credentialName: 'demo-fake-prod-root-password',
            ownerName: 'Platform DBA',
            lifecycleStatus: 'managed',
            connectivityStatus: 'online',
            description: '订单主库生产实例',
            lastCheckedAt: '2026-04-18T09:10:00',
            createdAt: '2026-04-14T10:00:00',
            updatedAt: '2026-04-18T09:10:00'
          }
        ]
      }
    });

    mocks.fetchGetAssetHosts.mockResolvedValue({
      error: null,
      data: {
        current: 1,
        size: 100,
        total: 4,
        summary: {
          managedHosts: 4,
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
            lastHeartbeat: '2026-04-15T09:32:00'
          },
          {
            id: 2,
            hostName: 'host-prd-02',
            ipAddress: '10.20.1.12',
            environment: 'production',
            clusterName: 'cn-shanghai-a',
            ownerName: 'Traffic Team',
            status: 'warning',
            lastHeartbeat: '2026-04-15T09:27:00'
          },
          {
            id: 3,
            hostName: 'host-stg-01',
            ipAddress: '10.30.2.18',
            environment: 'staging',
            clusterName: 'cn-beijing-b',
            ownerName: 'Release Team',
            status: 'online',
            lastHeartbeat: '2026-04-15T09:31:00'
          },
          {
            id: 4,
            hostName: 'host-sbx-01',
            ipAddress: '10.40.8.6',
            environment: 'sandbox',
            clusterName: 'cn-hangzhou-c',
            ownerName: 'QA Team',
            status: 'offline',
            lastHeartbeat: '2026-04-15T08:54:00'
          }
        ]
      }
    });

    mocks.fetchGetAssetCredentials.mockResolvedValue({
      error: null,
      data: [
        {
          id: 1,
          name: 'demo-fake-prod-root-password',
          credentialType: 'ssh_password',
          username: 'root',
          description: 'prod',
          createdAt: '2026-04-10T10:00:00'
        },
        {
          id: 2,
          name: 'demo-fake-staging-deploy-key',
          credentialType: 'ssh_key',
          username: 'deploy',
          description: 'staging',
          createdAt: '2026-04-11T14:30:00'
        }
      ]
    });

    mocks.fetchCreateAssetDatabase.mockResolvedValue({ error: null, data: null });
    mocks.fetchUpdateAssetDatabase.mockResolvedValue({ error: null, data: null });
    mocks.fetchDeleteAssetDatabase.mockResolvedValue({ error: null, data: true });
  });

  afterEach(() => {
    vi.resetModules();
  });

  it('keeps asset database api and typings aligned with backend endpoints', () => {
    expect(assetApiSource).toMatch(/export function fetchGetAssetDatabases\s*\(/);
    expect(assetApiSource).toMatch(/export function fetchCreateAssetDatabase\s*\(/);
    expect(assetApiSource).toMatch(/export function fetchUpdateAssetDatabase\s*\(/);
    expect(assetApiSource).toMatch(/export function fetchDeleteAssetDatabase\s*\(/);
    expect(assetApiSource).toMatch(/url:\s*['"]\/api\/assets\/databases['"]/);
    expect(assetApiSource).toMatch(/url:\s*`\/api\/assets\/databases\/\$\{id\}`/);
    expect(apiIndexSource).toContain("export * from './asset'");

    expect(assetTypingSource).toContain('interface DatabaseRecord');
    expect(assetTypingSource).toContain('interface DatabasePage');
    expect(assetTypingSource).toContain('warningDatabases: number;');
    expect(assetTypingSource).toContain(
      "databaseType: 'mysql' | 'postgresql' | 'oracle' | 'sqlserver' | 'mongodb' | 'redis' | string;"
    );
    expect(assetTypingSource).toContain('keyword?: string;');
    expect(assetTypingSource).toContain('environment?: string | null;');
    expect(assetTypingSource).toContain('databaseType?: string | null;');
    expect(assetTypingSource).toContain('lifecycleStatus?: string | null;');
    expect(assetTypingSource).toContain('connectivityStatus?: string | null;');
    expect(assetTypingSource).toContain('interface CreateDatabaseParams');
    expect(assetTypingSource).toContain('interface UpdateDatabaseParams extends CreateDatabaseParams');
    expect(assetTypingSource).toContain('databaseType: string;');
    expect(assetTypingSource).toContain('hostId: number | null;');
    expect(assetTypingSource).toContain('credentialId?: number | null;');
    expect(assetTypingSource).toContain("connectivityStatus: 'unknown' | 'online' | 'warning' | 'offline' | string;");
  });

  it('includes connectivity-check APIs and response typings', () => {
    expect(assetApiSource).toContain('fetchCheckAssetDatabase');
    expect(assetApiSource).toContain('fetchCheckSelectedAssetDatabases');
    expect(assetApiSource).toContain('fetchCheckCurrentPageAssetDatabases');
    expect(assetApiSource).toContain('fetchCheckQueriedAssetDatabases');
    expect(assetTypingSource).toContain('interface DatabaseConnectivityCheckSummary');
    expect(assetTypingSource).toContain('interface DatabaseConnectivityCheckItem');
    expect(assetTypingSource).toContain('interface DatabaseConnectivityCheckResponse');
    expect(assetTypingSource).toContain('connectionUsername: string | null;');
    expect(assetTypingSource).toContain('connectionUsername?: string;');
    expect(assetTypingSource).toContain('connectionPassword?: string;');
    expect(zhLocaleSource).toContain("checkCurrentPage: '检测当前页'");
    expect(enLocaleSource).toContain("checkCurrentPage: 'Check current page'");
  });

  it('registers database page in route files and generated route typings', () => {
    expect(routeSource).toContain("name: 'asset_database'");
    expect(routeSource).toContain("path: '/asset/database'");
    expect(routeSource).toContain("component: 'view.asset_database'");
    expect(importSource).toContain('asset_database: () => import("@/views/asset/database/index.vue")');
    expect(transformSource).toContain('"asset_database": "/asset/database"');
    expect(elegantTypingSource).toContain('"asset_database": "/asset/database";');
    expect(elegantTypingSource).toContain('| "asset_database"');
  });

  it('gates database management and connectivity actions by RBAC permissions', () => {
    expect(databasePageSource).toMatch(/useAuth\s*\(/);
    expect(databasePageSource).toContain('asset:database:manage');
    expect(databasePageSource).toContain('asset:database:connectivity-check');
  });

  it('loads database summary and table data from asset apis', async () => {
    const page = await mountDatabasePage();

    expect(mocks.fetchGetAssetDatabases).toHaveBeenCalledTimes(1);
    expect(mocks.fetchGetAssetHosts).toHaveBeenCalledTimes(1);
    expect(mocks.fetchGetAssetCredentials).toHaveBeenCalledTimes(1);

    expect(page.container.querySelector('[data-summary-key="managedDatabases"]')?.textContent).toContain('2');
    expect(page.container.querySelector('[data-summary-key="warningDatabases"]')?.textContent).toContain('1');
    expect(page.container.querySelector('[data-summary-key="onlineDatabases"]')?.textContent).toContain('1');

    const rows = page.container.querySelectorAll('tbody tr');
    expect(rows).toHaveLength(3);
    expect(page.container.querySelector('[data-pagination-total="3"]')).not.toBeNull();
    expect(page.container.textContent).toContain('session_hub');
    expect(page.container.textContent).toContain('billing_archive');
    expect(page.container.textContent).toContain('order_prod');
    expect(page.container.textContent).toContain('page.envops.assetDatabase.actions.create');
    expect(page.container.textContent).toContain('page.envops.assetDatabase.actions.edit');

    page.unmount();
  });

  it('keeps database page wired to drawer editing, asset dependencies and localized copy', () => {
    expect(databasePageSource).toContain('fetchGetAssetDatabases');
    expect(databasePageSource).toContain('fetchCreateAssetDatabase');
    expect(databasePageSource).toContain('fetchUpdateAssetDatabase');
    expect(databasePageSource).toContain('fetchDeleteAssetDatabase');
    expect(databasePageSource).toContain('fetchGetAssetHosts');
    expect(databasePageSource).toContain('fetchGetAssetCredentials');
    expect(databasePageSource).toContain('fetchCheckAssetDatabase');
    expect(databasePageSource).toContain('fetchCheckSelectedAssetDatabases');
    expect(databasePageSource).toContain('fetchCheckCurrentPageAssetDatabases');
    expect(databasePageSource).toContain('fetchCheckQueriedAssetDatabases');
    expect(databasePageSource).toContain('const filterModel = reactive<DatabaseFilterModel>');
    expect(databasePageSource).toContain('lifecycleStatus: null');
    expect(databasePageSource).toContain('connectivityStatus: null');
    expect(databasePageSource).toContain(
      "function handleApplySummaryFilter(summaryKey: 'managedDatabases' | 'warningDatabases' | 'onlineDatabases')"
    );
    expect(databasePageSource).toContain('function handleSearch()');
    expect(databasePageSource).toContain('function handleResetFilters()');
    expect(databasePageSource).toContain('function handlePageChange(page: number)');
    expect(databasePageSource).toContain('function handlePageSizeChange(pageSize: number)');
    expect(databasePageSource).toContain('function handleDelete(record: Api.Asset.DatabaseRecord)');
    expect(databasePageSource).toContain('fetchDeleteAssetDatabase(record.id)');
    expect(databasePageSource).toContain("t('common.delete')");
    expect(databasePageSource).toContain('NPagination');
    expect(databasePageSource).toContain('page.envops.assetDatabase.filters.keywordPlaceholder');
    expect(databasePageSource).toContain('page.envops.assetDatabase.filters.lifecycleStatusPlaceholder');
    expect(databasePageSource).toContain('page.envops.assetDatabase.filters.connectivityStatusPlaceholder');
    expect(databasePageSource).toContain(
      "filterModel.lifecycleStatus = filterModel.lifecycleStatus === 'managed' ? null : 'managed';"
    );
    expect(databasePageSource).toContain(
      "filterModel.connectivityStatus = filterModel.connectivityStatus === 'warning' ? null : 'warning';"
    );
    expect(databasePageSource).toContain(
      "filterModel.connectivityStatus = filterModel.connectivityStatus === 'online' ? null : 'online';"
    );
    expect(databasePageSource).toContain('const drawerVisible = ref(false);');
    expect(databasePageSource).toContain('const editingDatabaseId = ref<number | null>(null);');
    expect(databasePageSource).toContain('const selectedDatabaseIds = ref<number[]>([]);');
    expect(databasePageSource).toContain('const checking = ref(false);');
    expect(databasePageSource).toContain('const resultModalVisible = ref(false);');
    expect(databasePageSource).toContain(
      'const connectivityReport = ref<Api.Asset.DatabaseConnectivityCheckResponse | null>(null);'
    );
    expect(databasePageSource).toContain('function handleOpenCreateDrawer()');
    expect(databasePageSource).toContain('function handleOpenEditDrawer(record: Api.Asset.DatabaseRecord)');
    expect(databasePageSource).toContain('function handleCheckDatabase(record: Api.Asset.DatabaseRecord)');
    expect(databasePageSource).toContain('function handleCheckSelected()');
    expect(databasePageSource).toContain('function handleCheckCurrentPage()');
    expect(databasePageSource).toContain('function handleCheckFiltered()');
    expect(databasePageSource).toContain('await fetchCreateAssetDatabase(payload)');
    expect(databasePageSource).toContain('await fetchUpdateAssetDatabase(editingDatabaseId.value, payload)');
    expect(databasePageSource).toContain('NDrawer');
    expect(databasePageSource).toContain('page.envops.assetDatabase.form.titleCreate');
    expect(databasePageSource).toContain('page.envops.assetDatabase.messages.createSuccess');
    expect(databasePageSource).toContain('page.envops.assetDatabase.messages.updateSuccess');
    expect(databasePageSource).toContain('page.envops.assetDatabase.types.oracle');
    expect(databasePageSource).toContain('page.envops.assetDatabase.types.sqlserver');
    expect(databasePageSource).toContain('page.envops.assetDatabase.types.mongodb');
    expect(databasePageSource).toContain('page.envops.assetDatabase.types.redis');
    expect(databasePageSource).toContain('page.envops.assetDatabase.actions.check');
    expect(databasePageSource).toContain('page.envops.assetDatabase.actions.checkSelected');
    expect(databasePageSource).toContain('page.envops.assetDatabase.actions.checkCurrentPage');
    expect(databasePageSource).toContain('page.envops.assetDatabase.actions.checkAllFiltered');
    expect(databasePageSource).toContain('page.envops.assetDatabase.form.connectionUsername');
    expect(databasePageSource).toContain('page.envops.assetDatabase.form.connectionPassword');
    expect(databasePageSource).toContain('data-form-field="connectionPassword"');

    expect(zhLocaleSource).toContain('asset_database');
    expect(zhLocaleSource).toContain('assetDatabase');
    expect(enLocaleSource).toContain('asset_database');
    expect(enLocaleSource).toContain('assetDatabase');
  });
});

import { createApp, defineComponent, h, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  fetchGetAssetDatabases: vi.fn(),
  fetchCreateAssetDatabase: vi.fn(),
  fetchUpdateAssetDatabase: vi.fn(),
  fetchDeleteAssetDatabase: vi.fn(),
  fetchGetAssetHosts: vi.fn(),
  fetchGetAssetCredentials: vi.fn(),
  fetchCheckAssetDatabase: vi.fn(),
  fetchCheckSelectedAssetDatabases: vi.fn(),
  fetchCheckCurrentPageAssetDatabases: vi.fn(),
  fetchCheckQueriedAssetDatabases: vi.fn()
}));

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
    },
    type: {
      type: String,
      default: 'text'
    }
  },
  emits: ['update:value'],
  setup(props, { attrs, emit }) {
    return () =>
      h('input', {
        ...attrs,
        type: props.type,
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

const checkboxStub = defineComponent({
  inheritAttrs: false,
  props: {
    checked: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:checked'],
  setup(props, { attrs, slots, emit }) {
    return () =>
      h('label', attrs, [
        h('input', {
          type: 'checkbox',
          checked: props.checked,
          onChange: (event: Event) => emit('update:checked', (event.target as HTMLInputElement).checked)
        }),
        ...(slots.default?.() ?? [])
      ]);
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

function buildDatabasePage(): Api.Asset.DatabasePage {
  return {
    current: 1,
    size: 10,
    total: 1,
    summary: {
      managedDatabases: 1,
      warningDatabases: 0,
      onlineDatabases: 0
    },
    records: [
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
        connectivityStatus: 'unknown',
        connectionUsername: 'orders_app',
        description: '订单主库生产实例',
        lastCheckedAt: null,
        createdAt: '2026-04-14T10:00:00',
        updatedAt: '2026-04-18T09:10:00'
      }
    ]
  };
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
    'NTable',
    'NPagination',
    'NStatistic',
    'NModal'
  ].forEach(name => {
    app.component(name, passthroughStub);
  });
  app.component('NButton', buttonStub);
  app.component('NInput', inputStub);
  app.component('NInputNumber', inputNumberStub);
  app.component('NSelect', selectStub);
  app.component('NCheckbox', checkboxStub);

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

describe('asset database connectivity page behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.innerHTML = '';

    mocks.fetchGetAssetDatabases.mockResolvedValue({ error: null, data: buildDatabasePage() });
    mocks.fetchGetAssetHosts.mockResolvedValue({
      error: null,
      data: {
        current: 1,
        size: 100,
        total: 0,
        summary: { managedHosts: 0, onlineHosts: 0, warningHosts: 0 },
        records: []
      }
    });
    mocks.fetchGetAssetCredentials.mockResolvedValue({ error: null, data: [] });
    mocks.fetchCreateAssetDatabase.mockResolvedValue({ error: null, data: null });
    mocks.fetchUpdateAssetDatabase.mockResolvedValue({ error: null, data: null });
    mocks.fetchDeleteAssetDatabase.mockResolvedValue({ error: null, data: true });
    mocks.fetchCheckAssetDatabase.mockResolvedValue({
      error: null,
      data: {
        summary: { total: 1, success: 1, failed: 0, skipped: 0 },
        results: [
          {
            databaseId: 1,
            databaseName: 'order_prod',
            databaseType: 'mysql',
            environment: 'production',
            status: 'success',
            message: 'connected',
            connectivityStatus: 'online',
            checkedAt: '2026-04-21T10:15:00'
          }
        ]
      }
    });
  });

  afterEach(() => {
    vi.resetModules();
  });

  it('submits a single connectivity check and refreshes the list', async () => {
    const page = await mountDatabasePage();
    const checkButton = page.container.querySelector('button[data-action="check-database"]');

    checkButton?.dispatchEvent(new MouseEvent('click'));
    await settleRender();

    expect(mocks.fetchCheckAssetDatabase).toHaveBeenCalledWith(1);
    expect(mocks.fetchGetAssetDatabases).toHaveBeenCalledTimes(2);

    page.unmount();
  });

  it('keeps the password field blank when editing an existing database row', async () => {
    const page = await mountDatabasePage();
    const editButton = page.container.querySelector('button[data-action="edit-database"]');

    editButton?.dispatchEvent(new MouseEvent('click'));
    await settleRender();

    const passwordInput = page.container.querySelector(
      'input[data-form-field="connectionPassword"]'
    ) as HTMLInputElement;
    expect(passwordInput.value).toBe('');

    page.unmount();
  });
});

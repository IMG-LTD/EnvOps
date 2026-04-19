import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createApp, defineComponent, h, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const trafficPage = readFileSync(path.resolve(__dirname, 'controller/index.vue'), 'utf8');
const trafficApiSource = readFileSync(path.resolve(__dirname, '../../service/api/traffic.ts'), 'utf8');
const trafficTypingSource = readFileSync(path.resolve(__dirname, '../../typings/api/traffic.d.ts'), 'utf8');
const apiIndexSource = readFileSync(path.resolve(__dirname, '../../service/api/index.ts'), 'utf8');

const mocks = vi.hoisted(() => {
  const fetchGetTrafficPolicies = vi.fn();
  const fetchGetTrafficPlugins = vi.fn();
  const fetchPostPreviewTrafficPolicy = vi.fn();
  const fetchPostApplyTrafficPolicy = vi.fn();
  const fetchPostRollbackTrafficPolicy = vi.fn();

  return {
    fetchGetTrafficPolicies,
    fetchGetTrafficPlugins,
    fetchPostPreviewTrafficPolicy,
    fetchPostApplyTrafficPolicy,
    fetchPostRollbackTrafficPolicy
  };
});

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}));

vi.mock('@/service/api', () => ({
  fetchGetTrafficPolicies: mocks.fetchGetTrafficPolicies,
  fetchGetTrafficPlugins: mocks.fetchGetTrafficPlugins,
  fetchPostPreviewTrafficPolicy: mocks.fetchPostPreviewTrafficPolicy,
  fetchPostApplyTrafficPolicy: mocks.fetchPostApplyTrafficPolicy,
  fetchPostRollbackTrafficPolicy: mocks.fetchPostRollbackTrafficPolicy
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
          onClick: (event: MouseEvent) => {
            if (!props.disabled) {
              emit('click', event);
            }
          }
        },
        slots.default?.()
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

const alertStub = defineComponent({
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () => h('div', attrs, slots.default?.());
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

async function mountTrafficPage() {
  const { default: TrafficControllerPage } = await import('./controller/index.vue');
  const container = document.createElement('div');
  document.body.appendChild(container);
  const app = createApp(TrafficControllerPage);

  ['NSpace', 'NCard', 'NGrid', 'NGi', 'NSpin', 'NTag', 'NEmpty'].forEach(name => {
    app.component(name, passthroughStub);
  });
  app.component('NButton', buttonStub);
  app.component('NStatistic', statisticStub);
  app.component('NTable', tableStub);
  app.component('NAlert', alertStub);

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

describe('traffic controller contract wiring', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.innerHTML = '';
    window.$message = {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
      loading: vi.fn(),
      destroyAll: vi.fn()
    } as any;

    mocks.fetchGetTrafficPolicies.mockResolvedValue({
      error: null,
      data: [
        {
          id: 3001,
          app: 'checkout-gateway',
          strategy: 'header_canary',
          scope: 'prod / cn-shanghai-a',
          trafficRatio: '20%',
          owner: 'traffic-team',
          status: 'ENABLED',
          pluginType: 'NGINX',
          rollbackToken: 'traffic-rb-3001'
        },
        {
          id: 3002,
          app: 'billing-admin',
          strategy: 'blue_green',
          scope: 'staging / all',
          trafficRatio: '100%',
          owner: 'release-team',
          status: 'PREVIEW',
          pluginType: 'REST',
          rollbackToken: 'traffic-rb-3002'
        },
        {
          id: 3003,
          app: 'ops-worker',
          strategy: 'weighted_routing',
          scope: 'prod / cn-beijing-b',
          trafficRatio: '10%',
          owner: 'platform-team',
          status: 'REVIEW',
          pluginType: 'NGINX',
          rollbackToken: null
        }
      ]
    });

    mocks.fetchGetTrafficPlugins.mockResolvedValue({
      error: null,
      data: [
        {
          type: 'NGINX',
          name: 'Nginx traffic plugin',
          status: 'NOT_IMPLEMENTED',
          supportsPreview: true,
          supportsApply: true,
          supportsRollback: true
        },
        {
          type: 'REST',
          name: 'Rest traffic plugin',
          status: 'NOT_IMPLEMENTED',
          supportsPreview: true,
          supportsApply: true,
          supportsRollback: true
        }
      ]
    });

    mocks.fetchPostPreviewTrafficPolicy.mockResolvedValue({
      error: null,
      data: {
        action: 'preview',
        policy: {
          id: 3001,
          app: 'checkout-gateway',
          strategy: 'header_canary',
          scope: 'prod / cn-shanghai-a',
          trafficRatio: '20%',
          owner: 'traffic-team',
          status: 'PREVIEW',
          pluginType: 'NGINX',
          rollbackToken: 'traffic-rb-3001'
        },
        pluginResult: {
          pluginType: 'NGINX',
          status: 'NOT_IMPLEMENTED',
          action: 'preview',
          message: 'preview skeleton',
          app: 'checkout-gateway'
        }
      }
    });

    mocks.fetchPostApplyTrafficPolicy.mockResolvedValue({
      error: null,
      data: {
        action: 'apply',
        policy: {
          id: 3002,
          app: 'billing-admin',
          strategy: 'blue_green',
          scope: 'staging / all',
          trafficRatio: '100%',
          owner: 'release-team',
          status: 'ENABLED',
          pluginType: 'REST',
          rollbackToken: 'traffic-rb-3002'
        },
        pluginResult: {
          pluginType: 'REST',
          status: 'NOT_IMPLEMENTED',
          action: 'apply',
          message: 'apply skeleton',
          app: 'billing-admin'
        }
      }
    });

    mocks.fetchPostRollbackTrafficPolicy.mockResolvedValue({
      error: null,
      data: {
        action: 'rollback',
        policy: {
          id: 3001,
          app: 'checkout-gateway',
          strategy: 'header_canary',
          scope: 'prod / cn-shanghai-a',
          trafficRatio: '20%',
          owner: 'traffic-team',
          status: 'ENABLED',
          pluginType: 'NGINX',
          rollbackToken: 'traffic-rb-3001'
        },
        pluginResult: {
          pluginType: 'NGINX',
          status: 'NOT_IMPLEMENTED',
          action: 'rollback',
          message: 'rollback skeleton',
          app: 'checkout-gateway',
          rollbackToken: 'traffic-rb-3001'
        }
      }
    });
  });

  afterEach(() => {
    vi.resetModules();
  });

  it('keeps traffic api fetchers aligned with backend endpoints and typings', () => {
    expect(trafficApiSource).toMatch(/export function fetchGetTrafficPolicies\s*\(/);
    expect(trafficApiSource).toMatch(/url:\s*['"]\/api\/traffic\/policies['"]/);
    expect(trafficApiSource).toMatch(/export function fetchGetTrafficPlugins\s*\(/);
    expect(trafficApiSource).toMatch(/url:\s*['"]\/api\/traffic\/plugins['"]/);
    expect(trafficApiSource).toMatch(/export function fetchPostPreviewTrafficPolicy\s*\(/);
    expect(trafficApiSource).toMatch(/\/api\/traffic\/policies\/\$\{id\}\/preview/);
    expect(trafficApiSource).toMatch(/export function fetchPostApplyTrafficPolicy\s*\(/);
    expect(trafficApiSource).toMatch(/\/api\/traffic\/policies\/\$\{id\}\/apply/);
    expect(trafficApiSource).toMatch(/export function fetchPostRollbackTrafficPolicy\s*\(/);
    expect(trafficApiSource).toMatch(/\/api\/traffic\/policies\/\$\{id\}\/rollback/);
    expect(apiIndexSource).toContain("export * from './traffic'");

    expect(trafficTypingSource).toContain('namespace Traffic');
    expect(trafficTypingSource).toContain('interface TrafficPolicyRecord');
    expect(trafficTypingSource).toContain('id: number;');
    expect(trafficTypingSource).toContain('rollbackToken?: string | null;');
    expect(trafficTypingSource).toContain('interface TrafficPluginRecord');
    expect(trafficTypingSource).toContain('interface TrafficPolicyActionRecord');
    expect(trafficTypingSource).toContain('pluginResult: TrafficPluginResult;');
  });

  it('loads traffic controller summary and table from real api data instead of static mock arrays', async () => {
    const page = await mountTrafficPage();

    try {
      expect(mocks.fetchGetTrafficPolicies).toHaveBeenCalledTimes(1);
      expect(mocks.fetchGetTrafficPlugins).toHaveBeenCalledTimes(1);

      const policiesEnabledCard = page.container.querySelector('[data-summary-key="policiesEnabled"]');
      const canaryReleasesCard = page.container.querySelector('[data-summary-key="canaryReleases"]');
      const rollbackReadyCard = page.container.querySelector('[data-summary-key="rollbackReady"]');

      expect(policiesEnabledCard?.textContent).toContain('1');
      expect(canaryReleasesCard?.textContent).toContain('2');
      expect(rollbackReadyCard?.textContent).toContain('67%');

      const rows = page.container.querySelectorAll('tbody tr');

      expect(rows).toHaveLength(3);
      expect(page.container.textContent).toContain('checkout-gateway');
      expect(page.container.textContent).toContain('billing-admin');
      expect(page.container.textContent).toContain('ops-worker');
    } finally {
      page.unmount();
    }
  });

  it('submits traffic policy actions and refreshes the table state', async () => {
    const page = await mountTrafficPage();

    try {
      const buttons = Array.from(page.container.querySelectorAll('button'));
      const previewButton = buttons.find(button =>
        button.textContent?.includes('page.envops.trafficController.actions.preview')
      );
      const applyButton = buttons.find(button =>
        button.textContent?.includes('page.envops.trafficController.actions.apply')
      );
      const rollbackButtons = buttons.filter(button =>
        button.textContent?.includes('page.envops.trafficController.actions.rollback')
      );

      expect(previewButton).toBeTruthy();
      expect(applyButton).toBeTruthy();
      expect(rollbackButtons).toHaveLength(3);
      expect(rollbackButtons[2]?.hasAttribute('disabled')).toBe(true);

      previewButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      await settleRender();
      expect(mocks.fetchPostPreviewTrafficPolicy).toHaveBeenCalledWith(3001);
      expect(mocks.fetchGetTrafficPolicies).toHaveBeenCalledTimes(2);

      applyButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      await settleRender();
      expect(mocks.fetchPostApplyTrafficPolicy).toHaveBeenCalledWith(3001);
      expect(mocks.fetchGetTrafficPolicies).toHaveBeenCalledTimes(3);

      rollbackButtons[0]?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      await settleRender();
      expect(mocks.fetchPostRollbackTrafficPolicy).toHaveBeenCalledWith(3001);
      expect(mocks.fetchGetTrafficPolicies).toHaveBeenCalledTimes(4);

      expect(page.container.textContent).toContain('page.envops.trafficController.messages.latestAction');
      const successMock = window.$message?.success as ReturnType<typeof vi.fn> | undefined;
      expect(successMock).toBeDefined();
      expect(successMock?.mock.calls).toHaveLength(3);
    } finally {
      page.unmount();
    }
  });

  it('keeps traffic page wired to async api state and action handlers, and removes old hard-coded data seeds', () => {
    expect(trafficPage).toContain('fetchGetTrafficPolicies');
    expect(trafficPage).toContain('fetchGetTrafficPlugins');
    expect(trafficPage).toContain('fetchPostPreviewTrafficPolicy');
    expect(trafficPage).toContain('fetchPostApplyTrafficPolicy');
    expect(trafficPage).toContain('fetchPostRollbackTrafficPolicy');
    expect(trafficPage).toContain('Promise.all([');
    expect(trafficPage).toContain('handlePolicyAction');
    expect(trafficPage).toContain("handlePolicyAction(item.id, 'preview')");
    expect(trafficPage).toContain("handlePolicyAction(item.id, 'apply')");
    expect(trafficPage).toContain("handlePolicyAction(item.id, 'rollback')");
    expect(trafficPage).toContain('latestActionResult');
    expect(trafficPage).toContain('NAlert');
    expect(trafficPage).toContain('NSpin');
    expect(trafficPage).toContain('NEmpty');
    expect(trafficPage).toContain("t('common.refresh')");
    expect(trafficPage).toContain("t('page.envops.trafficController.table.operation')");

    expect(trafficPage).not.toContain('const trafficPolicies = computed(() => [');
    expect(trafficPage).not.toContain("value: '14'");
    expect(trafficPage).not.toContain("value: '3'");
    expect(trafficPage).not.toContain("app: 'payment-gateway'");
    expect(trafficPage).not.toContain("app: 'asset-sync'");
    expect(trafficPage).not.toContain("app: 'traffic-admin'");
    expect(trafficPage).not.toContain("app: 'ops-worker'");
  });
});

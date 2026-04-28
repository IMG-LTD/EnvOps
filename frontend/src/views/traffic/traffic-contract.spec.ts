import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createApp, defineComponent, h, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const trafficControllerSource = readFileSync(path.resolve(__dirname, 'controller/index.vue'), 'utf8');

const translations = vi.hoisted(() => ({
  'common.noData': 'No Data',
  'common.refresh': 'Refresh',
  'page.envops.common.status.disabled': 'Disabled',
  'page.envops.common.status.enabled': 'Enabled',
  'page.envops.common.status.preview': 'Preview',
  'page.envops.common.status.review': 'Review',
  'page.envops.common.status.standby': 'Standby',
  'page.envops.common.strategy.blueGreen': 'Blue-green',
  'page.envops.common.strategy.headerCanary': 'Header canary',
  'page.envops.common.strategy.weightedRouting': 'Weighted routing',
  'page.envops.common.strategy.emergencyRollback': 'Emergency rollback',
  'page.envops.common.team.envops': 'EnvOps',
  'page.envops.common.team.platform': 'Platform',
  'page.envops.common.team.release': 'Release Team',
  'page.envops.common.team.sre': 'SRE',
  'page.envops.common.team.traffic': 'Traffic',
  'page.envops.trafficController.actions.apply': 'Apply',
  'page.envops.trafficController.actions.preview': 'Preview',
  'page.envops.trafficController.actions.rollback': 'Rollback',
  'page.envops.trafficController.hero.description':
    'Execute a limited Traffic MVP with real REST-based preview, apply, and rollback for weighted routing policies.',
  'page.envops.trafficController.hero.title': 'Traffic Controller',
  'page.envops.trafficController.messages.actionFailed': 'Traffic action failed',
  'page.envops.trafficController.messages.applySuccess': 'Traffic policy applied successfully',
  'page.envops.trafficController.messages.latestAction': 'Latest Traffic Action',
  'page.envops.trafficController.messages.notReadyWarning':
    'Traffic MVP currently supports REST plugin and weighted routing only. NGINX, blue-green, and header canary remain outside this release.',
  'page.envops.trafficController.messages.pluginNotReady': 'Plugin not ready',
  'page.envops.trafficController.messages.pluginNotSupported': 'Plugin not supported in v0.0.5',
  'page.envops.trafficController.messages.previewSuccess': 'Traffic policy previewed successfully',
  'page.envops.trafficController.messages.rollbackSuccess': 'Traffic policy rolled back successfully',
  'page.envops.trafficController.messages.rollbackTokenMissing': 'Rollback token required',
  'page.envops.trafficController.messages.strategyNotSupported': 'Strategy not supported in v0.0.5',
  'page.envops.trafficController.summary.canaryReleases.desc':
    'Policies currently in preview or ready for preview within the MVP boundary',
  'page.envops.trafficController.summary.canaryReleases.label': 'Preview-ready Policies',
  'page.envops.trafficController.summary.policiesEnabled.desc':
    'Traffic policy records loaded into the controller, including supported and unsupported rows',
  'page.envops.trafficController.summary.policiesEnabled.label': 'Policy Records',
  'page.envops.trafficController.summary.rollbackReady.desc':
    'Share of records that currently hold a usable rollback token from the external traffic service',
  'page.envops.trafficController.summary.rollbackReady.label': 'Rollback-ready Coverage',
  'page.envops.trafficController.table.application': 'Application',
  'page.envops.trafficController.table.operation': 'Action',
  'page.envops.trafficController.table.owner': 'Owner',
  'page.envops.trafficController.table.scope': 'Scope',
  'page.envops.trafficController.table.status': 'Status',
  'page.envops.trafficController.table.strategy': 'Strategy',
  'page.envops.trafficController.table.title': 'Policy Snapshot',
  'page.envops.trafficController.table.trafficRatio': 'Traffic Ratio'
}));

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

vi.mock('vue-i18n', async () => {
  const actual = await vi.importActual<typeof import('vue-i18n')>('vue-i18n');

  return {
    ...actual,
    useI18n: () => ({
      t: (key: string) => translations[key as keyof typeof translations] ?? key
    })
  };
});

vi.mock('@/service/api', () => ({
  fetchGetTrafficPolicies: mocks.fetchGetTrafficPolicies,
  fetchGetTrafficPlugins: mocks.fetchGetTrafficPlugins,
  fetchPostPreviewTrafficPolicy: mocks.fetchPostPreviewTrafficPolicy,
  fetchPostApplyTrafficPolicy: mocks.fetchPostApplyTrafficPolicy,
  fetchPostRollbackTrafficPolicy: mocks.fetchPostRollbackTrafficPolicy
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
  it('gates traffic preview, apply and rollback actions by RBAC permissions', () => {
    expect(trafficControllerSource).toMatch(/useAuth\s*\(/);
    expect(trafficControllerSource).toContain('traffic:policy:preview');
    expect(trafficControllerSource).toContain('traffic:policy:apply');
    expect(trafficControllerSource).toContain('traffic:policy:rollback');
  });

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
          strategy: 'weighted_routing',
          scope: 'prod / cn-beijing-b',
          trafficRatio: '10%',
          owner: 'platform-team',
          status: 'REVIEW',
          pluginType: 'REST',
          rollbackToken: null
        },
        {
          id: 3002,
          app: 'billing-admin',
          strategy: 'weighted_routing',
          scope: 'staging / all',
          trafficRatio: '20%',
          owner: 'release-team',
          status: 'PREVIEW',
          pluginType: 'REST',
          rollbackToken: 'rb-apply-3002'
        },
        {
          id: 3003,
          app: 'ops-worker',
          strategy: 'header_canary',
          scope: 'prod / cn-shanghai-a',
          trafficRatio: '5%',
          owner: 'traffic-team',
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
          type: 'REST',
          name: 'REST Traffic Plugin',
          status: 'READY',
          supportsPreview: true,
          supportsApply: true,
          supportsRollback: true
        },
        {
          type: 'NGINX',
          name: 'Nginx Traffic Plugin',
          status: 'NOT_READY',
          supportsPreview: false,
          supportsApply: false,
          supportsRollback: false
        }
      ]
    });

    mocks.fetchPostPreviewTrafficPolicy.mockResolvedValue({ error: null, data: null });
    mocks.fetchPostApplyTrafficPolicy.mockResolvedValue({ error: null, data: null });
    mocks.fetchPostRollbackTrafficPolicy.mockResolvedValue({ error: null, data: null });
  });

  afterEach(() => {
    vi.resetModules();
  });

  it('enables actions only for REST weighted-routing policies and keeps unsupported rows disabled', async () => {
    const page = await mountTrafficPage();

    try {
      expect(page.container.textContent).toContain(
        'Traffic MVP currently supports REST plugin and weighted routing only'
      );
      const rows = Array.from(page.container.querySelectorAll('tbody tr'));
      expect(rows).toHaveLength(3);

      const checkoutButtons = Array.from(rows[0].querySelectorAll('button'));
      const billingButtons = Array.from(rows[1].querySelectorAll('button'));
      const opsButtons = Array.from(rows[2].querySelectorAll('button'));

      expect(checkoutButtons[0]?.hasAttribute('disabled')).toBe(false);
      expect(checkoutButtons[1]?.hasAttribute('disabled')).toBe(false);
      expect(checkoutButtons[2]?.hasAttribute('disabled')).toBe(true);

      expect(billingButtons[0]?.hasAttribute('disabled')).toBe(false);
      expect(billingButtons[1]?.hasAttribute('disabled')).toBe(false);
      expect(billingButtons[2]?.hasAttribute('disabled')).toBe(false);

      expect(opsButtons[0]?.hasAttribute('disabled')).toBe(true);
      expect(opsButtons[1]?.hasAttribute('disabled')).toBe(true);
      expect(opsButtons[2]?.hasAttribute('disabled')).toBe(true);

      expect(rows[0]?.textContent).toContain('REST');
      expect(rows[0]?.textContent).toContain('Rollback token required');
      expect(rows[2]?.textContent).toContain('NGINX');
      expect(rows[2]?.textContent).toContain('Plugin not ready');
    } finally {
      page.unmount();
    }
  });

  it('refreshes the page and shows latest success summary after apply succeeds', async () => {
    mocks.fetchPostApplyTrafficPolicy.mockResolvedValueOnce({
      error: null,
      data: {
        action: 'apply',
        policy: {
          id: 3001,
          app: 'checkout-gateway',
          strategy: 'weighted_routing',
          scope: 'prod / cn-beijing-b',
          trafficRatio: '10%',
          owner: 'platform-team',
          status: 'ENABLED',
          pluginType: 'REST',
          rollbackToken: 'rb-apply-3001'
        },
        pluginResult: {
          pluginType: 'REST',
          status: 'READY',
          action: 'apply',
          message: 'traffic rule applied',
          app: 'checkout-gateway',
          strategy: 'weighted_routing',
          scope: 'prod / cn-beijing-b',
          trafficRatio: '10%',
          owner: 'platform-team',
          rollbackToken: 'rb-apply-3001',
          reason: null
        }
      }
    });

    const page = await mountTrafficPage();

    try {
      const rows = Array.from(page.container.querySelectorAll('tbody tr'));
      const checkoutApplyButton = Array.from(rows[0].querySelectorAll('button')).find(button =>
        button.textContent?.includes('Apply')
      );

      checkoutApplyButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      await settleRender();

      expect(mocks.fetchPostApplyTrafficPolicy).toHaveBeenCalledWith(3001);
      expect(mocks.fetchGetTrafficPolicies).toHaveBeenCalledTimes(2);
      expect(mocks.fetchGetTrafficPlugins).toHaveBeenCalledTimes(2);
      expect(page.container.textContent).toContain('Latest Traffic Action');
      expect(page.container.textContent).toContain('Apply · checkout-gateway · traffic rule applied');

      const successMock = window.$message?.success as ReturnType<typeof vi.fn> | undefined;
      expect(successMock?.mock.calls[0]?.[0]).toContain('Traffic policy applied successfully');
    } finally {
      page.unmount();
    }
  });

  it('shows error feedback and latest failure summary when a traffic action fails', async () => {
    mocks.fetchPostApplyTrafficPolicy.mockResolvedValueOnce({
      error: { message: 'traffic rest service is unavailable' },
      data: null
    });

    const page = await mountTrafficPage();

    try {
      const rows = Array.from(page.container.querySelectorAll('tbody tr'));
      const checkoutApplyButton = Array.from(rows[0].querySelectorAll('button')).find(button =>
        button.textContent?.includes('Apply')
      );

      checkoutApplyButton?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      await settleRender();

      expect(mocks.fetchPostApplyTrafficPolicy).toHaveBeenCalledWith(3001);
      const errorMock = window.$message?.error as ReturnType<typeof vi.fn> | undefined;
      expect(errorMock?.mock.calls[0]?.[0]).toContain('traffic rest service is unavailable');
      expect(page.container.textContent).toContain('Latest Traffic Action');
      expect(page.container.textContent).toContain('Apply · checkout-gateway · traffic rest service is unavailable');
    } finally {
      page.unmount();
    }
  });
});

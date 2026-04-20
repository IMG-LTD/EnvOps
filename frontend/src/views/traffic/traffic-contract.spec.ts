import { createApp, defineComponent, h, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

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
    'Show policy records and plugin readiness. EnvOps 0.0.4 does not provide real traffic switching actions.',
  'page.envops.trafficController.hero.title': 'Traffic Controller',
  'page.envops.trafficController.messages.applySuccess': 'Traffic policy applied successfully',
  'page.envops.trafficController.messages.latestAction': 'Latest Traffic Action',
  'page.envops.trafficController.messages.notReadyWarning':
    'The current NGINX / REST plugins are still skeletons. This page only shows directory status and does not allow real traffic actions.',
  'page.envops.trafficController.messages.previewSuccess': 'Traffic policy previewed successfully',
  'page.envops.trafficController.messages.rollbackSuccess': 'Traffic policy rolled back successfully',
  'page.envops.trafficController.summary.canaryReleases.desc': 'Policy records currently in preview or demo semantics',
  'page.envops.trafficController.summary.canaryReleases.label': 'Preview Records',
  'page.envops.trafficController.summary.policiesEnabled.desc':
    'Traffic policy records in the current store; they do not mean real gateway rules are active',
  'page.envops.trafficController.summary.policiesEnabled.label': 'Policy Records',
  'page.envops.trafficController.summary.rollbackReady.desc':
    'Only indicates rollback tokens exist at the record level; it does not mean external systems are connected',
  'page.envops.trafficController.summary.rollbackReady.label': 'Rollback Token Coverage',
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

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => translations[key as keyof typeof translations] ?? key
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

function getButtonsByText(container: HTMLElement, label: string) {
  return Array.from(container.querySelectorAll('button')).filter(button => button.textContent?.includes(label));
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
          supportsPreview: false,
          supportsApply: false,
          supportsRollback: false
        },
        {
          type: 'REST',
          name: 'Rest traffic plugin',
          status: 'NOT_IMPLEMENTED',
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

  it('renders degraded summaries, tags, warning and disabled actions from api data', async () => {
    const page = await mountTrafficPage();

    try {
      expect(mocks.fetchGetTrafficPolicies).toHaveBeenCalledTimes(1);
      expect(mocks.fetchGetTrafficPlugins).toHaveBeenCalledTimes(1);

      const policyRecordsCard = page.container.querySelector('[data-summary-key="policiesEnabled"]');
      const previewRecordsCard = page.container.querySelector('[data-summary-key="canaryReleases"]');
      const rollbackCoverageCard = page.container.querySelector('[data-summary-key="rollbackReady"]');

      expect(policyRecordsCard?.textContent).toContain('Policy Records');
      expect(policyRecordsCard?.textContent).toContain('3');
      expect(previewRecordsCard?.textContent).toContain('Preview Records');
      expect(previewRecordsCard?.textContent).toContain('2');
      expect(rollbackCoverageCard?.textContent).toContain('Rollback Token Coverage');
      expect(rollbackCoverageCard?.textContent).toContain('67%');

      expect(page.container.textContent).toContain('3 / Policy Records');
      expect(page.container.textContent).toContain('2 / Preview Records');
      expect(page.container.textContent).toContain(
        'The current NGINX / REST plugins are still skeletons. This page only shows directory status and does not allow real traffic actions.'
      );

      const rows = page.container.querySelectorAll('tbody tr');
      expect(rows).toHaveLength(3);
      expect(page.container.textContent).toContain('checkout-gateway');
      expect(page.container.textContent).toContain('billing-admin');
      expect(page.container.textContent).toContain('ops-worker');

      const previewButtons = getButtonsByText(page.container, 'Preview');
      const applyButtons = getButtonsByText(page.container, 'Apply');
      const rollbackButtons = getButtonsByText(page.container, 'Rollback');

      expect(previewButtons).toHaveLength(3);
      expect(applyButtons).toHaveLength(3);
      expect(rollbackButtons).toHaveLength(3);
      expect(previewButtons.every(button => button.hasAttribute('disabled'))).toBe(true);
      expect(applyButtons.every(button => button.hasAttribute('disabled'))).toBe(true);
      expect(rollbackButtons.every(button => button.hasAttribute('disabled'))).toBe(true);
    } finally {
      page.unmount();
    }
  });

  it('does not submit traffic actions or show success feedback when plugins are not ready', async () => {
    const page = await mountTrafficPage();

    try {
      const previewButtons = getButtonsByText(page.container, 'Preview');
      const applyButtons = getButtonsByText(page.container, 'Apply');
      const rollbackButtons = getButtonsByText(page.container, 'Rollback');

      previewButtons[0]?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      applyButtons[0]?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      rollbackButtons[0]?.dispatchEvent(new MouseEvent('click', { bubbles: true }));
      await settleRender();

      expect(mocks.fetchPostPreviewTrafficPolicy).not.toHaveBeenCalled();
      expect(mocks.fetchPostApplyTrafficPolicy).not.toHaveBeenCalled();
      expect(mocks.fetchPostRollbackTrafficPolicy).not.toHaveBeenCalled();
      expect(mocks.fetchGetTrafficPolicies).toHaveBeenCalledTimes(1);
      expect(mocks.fetchGetTrafficPlugins).toHaveBeenCalledTimes(1);
      expect(page.container.textContent).not.toContain('Latest Traffic Action');

      const successMock = window.$message?.success as ReturnType<typeof vi.fn> | undefined;
      expect(successMock).toBeDefined();
      expect(successMock?.mock.calls).toHaveLength(0);
    } finally {
      page.unmount();
    }
  });

  it('maps standard ENABLED status to enabled display instead of the conservative fallback', async () => {
    const page = await mountTrafficPage();

    try {
      const rows = Array.from(page.container.querySelectorAll('tbody tr'));
      const enabledRow = rows.find(row => row.textContent?.includes('checkout-gateway'));

      expect(enabledRow).toBeTruthy();
      expect(enabledRow?.textContent).toContain('Enabled');
      expect(enabledRow?.textContent).not.toContain('Standby');
    } finally {
      page.unmount();
    }
  });

  it('maps unknown policy status to standby instead of an optimistic state', async () => {
    mocks.fetchGetTrafficPolicies.mockResolvedValueOnce({
      error: null,
      data: [
        {
          id: 4001,
          app: 'shadow-router',
          strategy: 'weighted_routing',
          scope: 'lab / all',
          trafficRatio: '5%',
          owner: 'platform-team',
          status: 'CUSTOM_STATE',
          pluginType: 'NGINX',
          rollbackToken: null
        }
      ]
    });

    const page = await mountTrafficPage();

    try {
      const rows = page.container.querySelectorAll('tbody tr');
      expect(rows).toHaveLength(1);
      expect(rows[0]?.textContent).toContain('shadow-router');
      expect(rows[0]?.textContent).toContain('Standby');
      expect(rows[0]?.textContent).not.toContain('Enabled');
    } finally {
      page.unmount();
    }
  });
});

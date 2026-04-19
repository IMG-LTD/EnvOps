import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createApp, defineComponent, h, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const systemUserPage = readFileSync(path.resolve(__dirname, 'user/index.vue'), 'utf8');
const systemUserApiSource = readFileSync(path.resolve(__dirname, '../../service/api/system-user.ts'), 'utf8');
const systemUserTypingSource = readFileSync(path.resolve(__dirname, '../../typings/api/system-user.d.ts'), 'utf8');
const apiIndexSource = readFileSync(path.resolve(__dirname, '../../service/api/index.ts'), 'utf8');

const mocks = vi.hoisted(() => {
  const fetchGetSystemUsers = vi.fn();
  const fetchCreateSystemUser = vi.fn();
  const fetchUpdateSystemUser = vi.fn();

  return {
    fetchGetSystemUsers,
    fetchCreateSystemUser,
    fetchUpdateSystemUser
  };
});

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}));

vi.mock('@/service/api', () => ({
  fetchGetSystemUsers: mocks.fetchGetSystemUsers,
  fetchCreateSystemUser: mocks.fetchCreateSystemUser,
  fetchUpdateSystemUser: mocks.fetchUpdateSystemUser
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

async function mountSystemUserPage() {
  const { default: SystemUserPage } = await import('./user/index.vue');
  const container = document.createElement('div');
  document.body.appendChild(container);
  const app = createApp(SystemUserPage);

  [
    'NSpace',
    'NCard',
    'NGrid',
    'NGi',
    'NSpin',
    'NTag',
    'NEmpty',
    'NButton',
    'NDrawer',
    'NDrawerContent',
    'NForm',
    'NFormItem',
    'NInput',
    'NSelect'
  ].forEach(name => {
    app.component(name, passthroughStub);
  });
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

describe('system user contract wiring', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.innerHTML = '';
    const today = new Date().toISOString().slice(0, 10);

    mocks.fetchGetSystemUsers.mockResolvedValue({
      error: null,
      data: [
        {
          id: 1,
          userName: 'envops-admin',
          phone: '13800138000',
          teamKey: 'platform',
          loginType: 'PASSWORD',
          status: 'ACTIVE',
          lastLoginAt: `${today}T09:15:00`,
          roles: ['SUPER_ADMIN', 'PLATFORM_ADMIN']
        },
        {
          id: 20,
          userName: 'release-admin',
          phone: '13900139000',
          teamKey: 'release',
          loginType: 'PASSWORD_OTP',
          status: 'ACTIVE',
          lastLoginAt: `${today}T08:45:00`,
          roles: ['SUPER_ADMIN', 'RELEASE_MANAGER']
        },
        {
          id: 21,
          userName: 'traffic-owner',
          phone: '13700137000',
          teamKey: 'traffic',
          loginType: 'SSO',
          status: 'REVIEW',
          lastLoginAt: '2026-04-17T19:20:00',
          roles: ['TRAFFIC_OWNER']
        },
        {
          id: 22,
          userName: 'qa-observer',
          phone: '13600136000',
          teamKey: 'qa',
          loginType: 'SSO',
          status: 'DISABLED',
          lastLoginAt: '2026-04-16T18:10:00',
          roles: ['OBSERVER']
        }
      ]
    });
    mocks.fetchCreateSystemUser.mockResolvedValue({ error: null, data: null });
    mocks.fetchUpdateSystemUser.mockResolvedValue({ error: null, data: null });
  });

  afterEach(() => {
    vi.resetModules();
  });

  it('keeps system user api fetchers aligned with backend endpoints and typings', () => {
    expect(systemUserApiSource).toMatch(/export function fetchGetSystemUsers\s*\(/);
    expect(systemUserApiSource).toMatch(/export function fetchCreateSystemUser\s*\(/);
    expect(systemUserApiSource).toMatch(/export function fetchUpdateSystemUser\s*\(/);
    expect(systemUserApiSource).toMatch(/url:\s*['"]\/api\/system\/users['"]/);
    expect(systemUserApiSource).toMatch(/method:\s*['"]post['"]/);
    expect(systemUserApiSource).toMatch(/url:\s*`\/api\/system\/users\/\$\{id\}`/);
    expect(systemUserApiSource).toMatch(/method:\s*['"]put['"]/);
    expect(apiIndexSource).toContain("export * from './system-user'");

    expect(systemUserTypingSource).toContain('namespace SystemUser');
    expect(systemUserTypingSource).toContain('interface SystemUserRecord');
    expect(systemUserTypingSource).toContain('interface CreateSystemUserParams');
    expect(systemUserTypingSource).toContain('interface UpdateSystemUserParams');
    expect(systemUserTypingSource).toContain('teamKey: string;');
    expect(systemUserTypingSource).toContain('loginType: string;');
    expect(systemUserTypingSource).toContain('status: string;');
    expect(systemUserTypingSource).toContain('lastLoginAt: string | null;');
    expect(systemUserTypingSource).toContain('roles: string[];');
  });

  it('loads system user summary and table from real api data instead of static mock arrays', async () => {
    const page = await mountSystemUserPage();

    expect(mocks.fetchGetSystemUsers).toHaveBeenCalledTimes(1);

    const usersCard = page.container.querySelector('[data-summary-key="users"]');
    const adminsCard = page.container.querySelector('[data-summary-key="admins"]');
    const activeTodayCard = page.container.querySelector('[data-summary-key="activeToday"]');

    expect(usersCard?.textContent).toContain('4');
    expect(adminsCard?.textContent).toContain('2');
    expect(activeTodayCard?.textContent).toContain('2');

    const rows = page.container.querySelectorAll('tbody tr');

    expect(rows).toHaveLength(4);
    expect(page.container.textContent).toContain('envops-admin');
    expect(page.container.textContent).toContain('release-admin');
    expect(page.container.textContent).toContain('traffic-owner');
    expect(page.container.textContent).toContain('qa-observer');
    expect(page.container.textContent).toContain('page.envops.systemUser.actions.create');
    expect(page.container.textContent).toContain('page.envops.systemUser.actions.edit');

    page.unmount();
  });

  it('keeps system user page wired to async api state and management actions', () => {
    expect(systemUserPage).toContain('fetchGetSystemUsers');
    expect(systemUserPage).toContain('fetchCreateSystemUser');
    expect(systemUserPage).toContain('fetchUpdateSystemUser');
    expect(systemUserPage).toContain('const drawerVisible = ref(false);');
    expect(systemUserPage).toContain('const editingUserId = ref<number | null>(null);');
    expect(systemUserPage).toContain('function handleOpenCreateDrawer()');
    expect(systemUserPage).toContain('function handleOpenEditDrawer(user: Api.SystemUser.SystemUserRecord)');
    expect(systemUserPage).toContain('await fetchCreateSystemUser(buildCreatePayload())');
    expect(systemUserPage).toContain('await fetchUpdateSystemUser(editingUserId.value, buildUpdatePayload())');
    expect(systemUserPage).toContain('NDrawer');
    expect(systemUserPage).toContain('page.envops.systemUser.form.titleCreate');
    expect(systemUserPage).toContain('page.envops.systemUser.messages.createSuccess');
    expect(systemUserPage).toContain('page.envops.systemUser.messages.updateSuccess');

    expect(systemUserPage).not.toContain("value: '28'");
    expect(systemUserPage).not.toContain("value: '6'");
    expect(systemUserPage).not.toContain("value: '17'");
    expect(systemUserPage).not.toContain("userName: 'lin.sre'");
    expect(systemUserPage).not.toContain("userName: 'amy.ops'");
    expect(systemUserPage).not.toContain("userName: 'jack.release'");
    expect(systemUserPage).not.toContain("userName: 'luna.qa'");
  });
});

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
const zhLocaleSource = readFileSync(path.resolve(__dirname, '../../locales/langs/zh-cn.ts'), 'utf8');
const enLocaleSource = readFileSync(path.resolve(__dirname, '../../locales/langs/en-us.ts'), 'utf8');

const mocks = vi.hoisted(() => {
  const fetchGetSystemUsers = vi.fn();
  const fetchCreateSystemUser = vi.fn();
  const fetchUpdateSystemUser = vi.fn();
  const fetchGetSystemRbacRoles = vi.fn();
  const fetchGetSystemUserRoles = vi.fn();
  const fetchUpdateSystemUserRoles = vi.fn();

  return {
    fetchGetSystemUsers,
    fetchCreateSystemUser,
    fetchUpdateSystemUser,
    fetchGetSystemRbacRoles,
    fetchGetSystemUserRoles,
    fetchUpdateSystemUserRoles
  };
});

vi.mock('vue-i18n', async () => {
  const actual = await vi.importActual<typeof import('vue-i18n')>('vue-i18n');

  return {
    ...actual,
    useI18n: () => ({
      t: (key: string) => key
    })
  };
});

vi.mock('@/service/api', () => ({
  fetchGetSystemUsers: mocks.fetchGetSystemUsers,
  fetchCreateSystemUser: mocks.fetchCreateSystemUser,
  fetchUpdateSystemUser: mocks.fetchUpdateSystemUser,
  fetchGetSystemRbacRoles: mocks.fetchGetSystemRbacRoles,
  fetchGetSystemUserRoles: mocks.fetchGetSystemUserRoles,
  fetchUpdateSystemUserRoles: mocks.fetchUpdateSystemUserRoles
}));

vi.mock('@/hooks/business/auth', () => ({
  useAuth: () => ({
    hasAuth: (code: string | string[]) => {
      const permissions = ['system:user:manage'];

      return typeof code === 'string' ? permissions.includes(code) : code.some(item => permissions.includes(item));
    }
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

function getExactTextElement(container: HTMLElement, text: string, index = 0) {
  const matches = Array.from(container.querySelectorAll<HTMLElement>('div, button')).filter(
    element => element.textContent?.replace(/\s+/g, ' ').trim() === text
  );

  expect(matches.length).toBeGreaterThan(index);

  return matches[index]!;
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
    Object.assign(window, { $message: { error: vi.fn(), success: vi.fn(), warning: vi.fn() } });
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
    mocks.fetchGetSystemRbacRoles.mockResolvedValue({
      error: null,
      data: [
        {
          id: 1,
          roleKey: 'SUPER_ADMIN',
          roleName: 'Super Admin',
          description: null,
          enabled: true,
          builtIn: true,
          createdAt: null,
          updatedAt: null
        },
        {
          id: 2,
          roleKey: 'DISABLED_ROLE',
          roleName: 'Disabled Role',
          description: null,
          enabled: false,
          builtIn: false,
          createdAt: null,
          updatedAt: null
        }
      ]
    });
    mocks.fetchGetSystemUserRoles.mockResolvedValue({
      error: null,
      data: {
        userId: 1,
        roles: [],
        roleIds: [1],
        roleKeys: ['SUPER_ADMIN']
      }
    });
    mocks.fetchUpdateSystemUserRoles.mockResolvedValue({ error: null, data: null });
  });

  afterEach(() => {
    vi.resetModules();
  });

  it('keeps system user api fetchers aligned with backend endpoints and typings', () => {
    expect(systemUserApiSource).toMatch(/export function fetchGetSystemUsers\s*\(/);
    expect(systemUserApiSource).toMatch(/export function fetchCreateSystemUser\s*\(/);
    expect(systemUserApiSource).toMatch(/export function fetchUpdateSystemUser\s*\(/);
    expect(systemUserApiSource).toContain('fetchGetSystemUserRoles');
    expect(systemUserApiSource).toContain('fetchUpdateSystemUserRoles');
    expect(systemUserApiSource).toMatch(/url:\s*['"]\/api\/system\/users['"]/);
    expect(systemUserApiSource).toMatch(/method:\s*['"]post['"]/);
    expect(systemUserApiSource).toMatch(/url:\s*`\/api\/system\/users\/\$\{id\}`/);
    expect(systemUserApiSource).toMatch(/method:\s*['"]put['"]/);
    expect(apiIndexSource).toContain("export * from './system-user'");

    expect(systemUserTypingSource).toContain('namespace SystemUser');
    expect(systemUserTypingSource).toContain('interface SystemUserRecord');
    expect(systemUserTypingSource).toContain('interface CreateSystemUserParams');
    expect(systemUserTypingSource).toContain('interface UpdateSystemUserParams');
    expect(systemUserTypingSource).toContain('UpdateSystemUserRolesParams');
    expect(systemUserTypingSource).toContain('roleIds: number[]');
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
    expect(systemUserPage).toContain('fetchGetSystemRbacRoles');
    expect(systemUserPage).toContain('fetchGetSystemUserRoles');
    expect(systemUserPage).toContain('fetchUpdateSystemUserRoles');
    expect(systemUserPage).toContain('system:user:manage');
    expect(systemUserPage).toContain('handleOpenRoleAssignment');
    expect(systemUserPage).toContain('handleSaveUserRoles');
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

  it('defines role assignment locale keys in Chinese and English', () => {
    const localeSources = [zhLocaleSource, enLocaleSource];
    const requiredKeys = ['title', 'roles', 'placeholder', 'save', 'saveSuccess', 'loadFailed'];

    localeSources.forEach(source => {
      const roleAssignmentMatch = source.match(/roleAssignment:\s*{([\s\S]*?)\n\s*},\n\s*form:/);

      expect(roleAssignmentMatch).not.toBeNull();

      const roleAssignmentBlock = roleAssignmentMatch?.[1] ?? '';

      requiredKeys.forEach(key => {
        expect(roleAssignmentBlock).toMatch(new RegExp(`${key}:\\s*'[^']+'`));
      });
    });
  });

  it('does not save an empty role set when assignment load fails', async () => {
    const message = { error: vi.fn(), success: vi.fn(), warning: vi.fn() };

    Object.assign(window, { $message: message });
    mocks.fetchGetSystemUserRoles.mockResolvedValueOnce({ error: new Error('load failed'), data: null });

    const page = await mountSystemUserPage();
    const assignRolesButton = getExactTextElement(page.container, 'page.envops.systemUser.roleAssignment.title');

    assignRolesButton.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.fetchGetSystemUserRoles).toHaveBeenCalledWith(1);
    expect(message.error).toHaveBeenCalledWith('page.envops.systemUser.roleAssignment.loadFailed');

    const saveRolesButton = getExactTextElement(page.container, 'page.envops.systemUser.roleAssignment.save');

    saveRolesButton.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    await settleRender();

    expect(mocks.fetchUpdateSystemUserRoles).not.toHaveBeenCalled();

    page.unmount();
  });

  it('keeps role assignment save and close guarded by loaded and saving states', () => {
    expect(systemUserPage).toContain('const roleAssignmentLoaded = ref(false);');
    expect(systemUserPage).toContain('roleAssignmentLoaded.value = false;');
    expect(systemUserPage).toContain('roleAssignmentLoaded.value = true;');
    expect(systemUserPage).toContain('!roleAssignmentLoaded.value');
    expect(systemUserPage).toContain("window.$message?.error(t('page.envops.systemUser.roleAssignment.loadFailed'))");
    expect(systemUserPage).toContain('page.envops.systemUser.roleAssignment.roles');
    expect(systemUserPage).toContain('page.envops.systemUser.roleAssignment.placeholder');
    expect(systemUserPage).toContain('page.envops.systemUser.roleAssignment.save');
    expect(systemUserPage).toContain('function handleRoleDrawerVisibleChange(show: boolean)');
    expect(systemUserPage).toMatch(/if \(!show && savingUserRoles\.value\) {\n\s+return;\n\s+}/);
    expect(systemUserPage).toContain(':show="roleDrawerVisible"');
    expect(systemUserPage).toContain('@update:show="handleRoleDrawerVisibleChange"');
    expect(systemUserPage).toContain(':mask-closable="!savingUserRoles"');
    expect(systemUserPage).toContain(':close-on-esc="!savingUserRoles"');
    expect(systemUserPage).toContain(':closable="!savingUserRoles"');
    expect(systemUserPage).toContain('@click="handleRoleDrawerVisibleChange(false)"');
    expect(systemUserPage).not.toContain('v-model:show="roleDrawerVisible"');

    const resetRoleAssignmentMatch = systemUserPage.match(/function resetRoleAssignmentState\(\) {([\s\S]*?)\n}/);

    expect(resetRoleAssignmentMatch?.[1]).not.toContain('savingUserRoles.value = false;');
  });
});

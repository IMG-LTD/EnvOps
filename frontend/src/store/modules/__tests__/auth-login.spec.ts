import { createApp } from 'vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { resetSetupStore } from '@/store/plugins';

const mocks = vi.hoisted(() => {
  const fetchLogin = vi.fn();
  const fetchCodeLogin = vi.fn();
  const fetchGetUserInfo = vi.fn();
  const startLoading = vi.fn();
  const endLoading = vi.fn();
  const toLogin = vi.fn();
  const redirectFromLogin = vi.fn();
  const route = {
    meta: {
      constant: true
    },
    query: {},
    fullPath: '/login/pwd-login'
  };
  const routeStore = {
    resetStore: vi.fn()
  };
  const tabStore = {
    cacheTabs: vi.fn(),
    clearTabs: vi.fn()
  };
  const localGet = vi.fn(() => null);
  const localSet = vi.fn();
  const localRemove = vi.fn();

  return {
    fetchLogin,
    fetchCodeLogin,
    fetchGetUserInfo,
    startLoading,
    endLoading,
    toLogin,
    redirectFromLogin,
    route,
    routeStore,
    tabStore,
    localGet,
    localSet,
    localRemove
  };
});

vi.mock('@sa/hooks', () => ({
  useLoading: () => ({
    loading: false,
    startLoading: mocks.startLoading,
    endLoading: mocks.endLoading
  })
}));

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route
}));

vi.mock('@/service/api', () => ({
  fetchLogin: mocks.fetchLogin,
  fetchCodeLogin: mocks.fetchCodeLogin,
  fetchGetUserInfo: mocks.fetchGetUserInfo
}));

vi.mock('@/hooks/common/router', () => ({
  useRouterPush: () => ({
    toLogin: mocks.toLogin,
    redirectFromLogin: mocks.redirectFromLogin
  })
}));

vi.mock('@/utils/storage', () => ({
  localStg: {
    get: mocks.localGet,
    set: mocks.localSet,
    remove: mocks.localRemove
  }
}));

vi.mock('@/locales', () => ({
  $t: (key: string) => key
}));

vi.mock('@/store/modules/route', () => ({
  useRouteStore: () => mocks.routeStore
}));

vi.mock('@/store/modules/tab', () => ({
  useTabStore: () => mocks.tabStore
}));

describe('auth store login handling', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    const pinia = createPinia();
    pinia.use(resetSetupStore);
    createApp({}).use(pinia);
    setActivePinia(pinia);

    mocks.route.meta.constant = true;
    mocks.route.query = {};
    mocks.route.fullPath = '/login/pwd-login';
    mocks.localGet.mockReturnValue(null);
    mocks.fetchLogin.mockResolvedValue({
      data: null,
      error: {
        response: {
          status: 401,
          data: {
            code: '401',
            msg: 'Unauthorized'
          }
        }
      }
    });
    mocks.fetchCodeLogin.mockResolvedValue({
      data: null,
      error: {
        response: {
          status: 401,
          data: {
            code: '401',
            msg: 'Unauthorized'
          }
        }
      }
    });
  });

  it('does not reset global auth state when password login returns 401', async () => {
    const { useAuthStore } = await import('@/store/modules/auth');

    const authStore = useAuthStore();

    await authStore.login('envops-admin', 'wrong-password');

    expect(mocks.routeStore.resetStore).not.toHaveBeenCalled();
    expect(mocks.tabStore.cacheTabs).not.toHaveBeenCalled();
    expect(mocks.toLogin).not.toHaveBeenCalled();
  });

  it('does not reset global auth state when code login returns 401', async () => {
    const { useAuthStore } = await import('@/store/modules/auth');

    const authStore = useAuthStore();

    await authStore.loginByCode('13900139000', '000000');

    expect(mocks.routeStore.resetStore).not.toHaveBeenCalled();
    expect(mocks.tabStore.cacheTabs).not.toHaveBeenCalled();
    expect(mocks.toLogin).not.toHaveBeenCalled();
  });

  it('reuses token login flow after code login succeeds', async () => {
    mocks.fetchCodeLogin.mockResolvedValue({
      data: {
        token: 'code-login-token'
      },
      error: null
    });
    mocks.fetchGetUserInfo.mockResolvedValue({
      data: {
        userId: '20',
        userName: 'release-admin',
        roles: ['SUPER_ADMIN', 'RELEASE_MANAGER'],
        buttons: ['envops:dashboard:view']
      },
      error: null
    });

    const { useAuthStore } = await import('@/store/modules/auth');

    const authStore = useAuthStore();

    await authStore.loginByCode('13900139000', '139000');

    expect(mocks.fetchCodeLogin).toHaveBeenCalledWith('13900139000', '139000');
    expect(mocks.fetchLogin).not.toHaveBeenCalled();
    expect(mocks.localSet).toHaveBeenCalledWith('token', 'code-login-token');
    expect(mocks.redirectFromLogin).toHaveBeenCalled();
    expect(authStore.userInfo.userName).toBe('release-admin');
  });
});

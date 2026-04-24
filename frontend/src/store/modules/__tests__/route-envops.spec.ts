import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => {
  const clone = <T>(value: T): T => structuredClone(value);

  const loginRoutePath = '/login/:module(pwd-login|code-login|register|reset-pwd|bind-wechat)?';
  const menuRouteKeys = ['asset', 'monitor', 'app', 'deploy', 'task', 'traffic', 'system'];
  const searchMenuRouteKeys = [
    'asset_host',
    'asset_group',
    'asset_tag',
    'asset_credential',
    'asset_database',
    'monitor_detect-task',
    'monitor_metric',
    'app_definition',
    'app_version',
    'app_package',
    'app_config-template',
    'app_script-template',
    'deploy_task',
    'task_center',
    'traffic_controller',
    'system_user'
  ];

  const dynamicRoutes = [
    {
      id: 'asset',
      name: 'asset',
      path: '/asset',
      component: 'layout.base',
      meta: { title: 'asset', i18nKey: 'route.asset', order: 1 },
      children: [
        {
          id: 'asset_host',
          name: 'asset_host',
          path: '/asset/host',
          component: 'view.asset_host',
          meta: { title: 'asset_host', i18nKey: 'route.asset_host', order: 1 }
        },
        {
          id: 'asset_group',
          name: 'asset_group',
          path: '/asset/group',
          component: 'view.asset_group',
          meta: { title: 'asset_group', i18nKey: 'route.asset_group', order: 2 }
        },
        {
          id: 'asset_tag',
          name: 'asset_tag',
          path: '/asset/tag',
          component: 'view.asset_tag',
          meta: { title: 'asset_tag', i18nKey: 'route.asset_tag', order: 3 }
        },
        {
          id: 'asset_credential',
          name: 'asset_credential',
          path: '/asset/credential',
          component: 'view.asset_credential',
          meta: { title: 'asset_credential', i18nKey: 'route.asset_credential', order: 4 }
        },
        {
          id: 'asset_database',
          name: 'asset_database',
          path: '/asset/database',
          component: 'view.asset_database',
          meta: { title: 'asset_database', i18nKey: 'route.asset_database', order: 5 }
        }
      ]
    },
    {
      id: 'monitor',
      name: 'monitor',
      path: '/monitor',
      component: 'layout.base',
      meta: { title: 'monitor', i18nKey: 'route.monitor', order: 2 },
      children: [
        {
          id: 'monitor_detect-task',
          name: 'monitor_detect-task',
          path: '/monitor/detect-task',
          component: 'view.monitor_detect-task',
          meta: { title: 'monitor_detect-task', i18nKey: 'route.monitor_detect-task', order: 1 }
        },
        {
          id: 'monitor_metric',
          name: 'monitor_metric',
          path: '/monitor/metric',
          component: 'view.monitor_metric',
          meta: { title: 'monitor_metric', i18nKey: 'route.monitor_metric', order: 2 }
        }
      ]
    },
    {
      id: 'app',
      name: 'app',
      path: '/app',
      component: 'layout.base',
      meta: { title: 'app', i18nKey: 'route.app', order: 3 },
      children: [
        {
          id: 'app_definition',
          name: 'app_definition',
          path: '/app/definition',
          component: 'view.app_definition',
          meta: { title: 'app_definition', i18nKey: 'route.app_definition', order: 1 }
        },
        {
          id: 'app_version',
          name: 'app_version',
          path: '/app/version',
          component: 'view.app_version',
          meta: { title: 'app_version', i18nKey: 'route.app_version', order: 2 }
        },
        {
          id: 'app_package',
          name: 'app_package',
          path: '/app/package',
          component: 'view.app_package',
          meta: { title: 'app_package', i18nKey: 'route.app_package', order: 3 }
        },
        {
          id: 'app_config-template',
          name: 'app_config-template',
          path: '/app/config-template',
          component: 'view.app_config-template',
          meta: { title: 'app_config-template', i18nKey: 'route.app_config-template', order: 4 }
        },
        {
          id: 'app_script-template',
          name: 'app_script-template',
          path: '/app/script-template',
          component: 'view.app_script-template',
          meta: { title: 'app_script-template', i18nKey: 'route.app_script-template', order: 5 }
        }
      ]
    },
    {
      id: 'deploy',
      name: 'deploy',
      path: '/deploy',
      component: 'layout.base',
      meta: { title: 'deploy', i18nKey: 'route.deploy', order: 4 },
      children: [
        {
          id: 'deploy_task',
          name: 'deploy_task',
          path: '/deploy/task',
          component: 'view.deploy_task',
          meta: { title: 'deploy_task', i18nKey: 'route.deploy_task', order: 1 }
        }
      ]
    },
    {
      id: 'task',
      name: 'task',
      path: '/task',
      component: 'layout.base',
      meta: { title: 'task', i18nKey: 'route.task', order: 5 },
      children: [
        {
          id: 'task_center',
          name: 'task_center',
          path: '/task/center',
          component: 'view.task_center',
          meta: { title: 'task_center', i18nKey: 'route.task_center', order: 1, hideInMenu: false }
        },
        {
          id: 'task_tracking_[id]',
          name: 'task_tracking_[id]',
          path: '/task/tracking/:id',
          component: 'view.task_tracking_[id]',
          meta: {
            title: '任务追踪',
            i18nKey: 'route.task_tracking_[id]',
            order: 2,
            hideInMenu: true,
            activeMenu: 'task_center'
          }
        }
      ]
    },
    {
      id: 'traffic',
      name: 'traffic',
      path: '/traffic',
      component: 'layout.base',
      meta: { title: 'traffic', i18nKey: 'route.traffic', order: 6 },
      children: [
        {
          id: 'traffic_controller',
          name: 'traffic_controller',
          path: '/traffic/controller',
          component: 'view.traffic_controller',
          meta: { title: 'traffic_controller', i18nKey: 'route.traffic_controller', order: 1 }
        }
      ]
    },
    {
      id: 'system',
      name: 'system',
      path: '/system',
      component: 'layout.base',
      meta: { title: 'system', i18nKey: 'route.system', order: 7 },
      children: [
        {
          id: 'system_user',
          name: 'system_user',
          path: '/system/user',
          component: 'view.system_user',
          meta: { title: 'system_user', i18nKey: 'route.system_user', order: 1 }
        }
      ]
    }
  ];

  const staticConstantRoutes = [
    {
      name: '403',
      path: '/403',
      component: 'layout.blank$view.403',
      meta: {
        title: '403',
        i18nKey: 'route.403',
        constant: true,
        hideInMenu: true
      }
    },
    {
      name: '404',
      path: '/404',
      component: 'layout.blank$view.404',
      meta: {
        title: '404',
        i18nKey: 'route.404',
        constant: true,
        hideInMenu: true
      }
    },
    {
      name: '500',
      path: '/500',
      component: 'layout.blank$view.500',
      meta: {
        title: '500',
        i18nKey: 'route.500',
        constant: true,
        hideInMenu: true
      }
    },
    {
      name: 'login',
      path: loginRoutePath,
      component: 'layout.blank$view.login',
      props: true,
      meta: {
        title: 'login',
        i18nKey: 'route.login',
        constant: true,
        hideInMenu: true
      }
    }
  ];

  const authStore = {
    userInfo: {
      userId: 'envops-user',
      userName: 'envops',
      roles: ['admin'],
      buttons: []
    },
    isStaticSuper: false,
    initUserInfo: vi.fn(),
    resetStore: vi.fn()
  };

  const tabStore = {
    initHomeTab: vi.fn()
  };

  const router = {
    addRoute: vi.fn(() => vi.fn()),
    removeRoute: vi.fn(),
    currentRoute: {
      value: {
        name: 'home',
        meta: {},
        matched: []
      }
    }
  };

  const fetchGetUserRoutes = vi.fn();
  const fetchGetConstantRoutes = vi.fn();

  const toVueRoutes = (routes: any[]) =>
    routes.map(route => ({
      name: route.name,
      path: route.path,
      meta: route.meta ?? {},
      redirect: route.redirect,
      component: {},
      children: route.children?.map((child: any) => ({
        name: child.name,
        path: child.path,
        meta: child.meta ?? {},
        component: {}
      }))
    }));

  const getAuthVueRoutes = vi.fn(toVueRoutes);
  const createStaticRoutes = vi.fn(() => ({
    constantRoutes: clone(staticConstantRoutes),
    authRoutes: clone(dynamicRoutes)
  }));
  const getRoutePath = vi.fn((name: string) => `/${name.replaceAll('_', '/')}`);
  const getRouteName = vi.fn();

  return {
    clone,
    loginRoutePath,
    menuRouteKeys,
    searchMenuRouteKeys,
    dynamicRoutes,
    staticConstantRoutes,
    authStore,
    tabStore,
    router,
    fetchGetUserRoutes,
    fetchGetConstantRoutes,
    toVueRoutes,
    getAuthVueRoutes,
    createStaticRoutes,
    getRoutePath,
    getRouteName
  };
});

vi.mock('@/store/modules/auth', () => ({
  useAuthStore: () => mocks.authStore
}));

vi.mock('@/store/modules/tab', () => ({
  useTabStore: () => mocks.tabStore
}));

vi.mock('@/router', () => ({
  router: mocks.router
}));

vi.mock('@/router/routes', () => ({
  createStaticRoutes: mocks.createStaticRoutes,
  getAuthVueRoutes: mocks.getAuthVueRoutes
}));

vi.mock('@/router/routes/builtin', () => ({
  ROOT_ROUTE: {
    name: 'root',
    path: '/',
    redirect: '/home',
    meta: {
      title: 'root',
      constant: true
    }
  }
}));

vi.mock('@/router/elegant/transform', () => ({
  getRoutePath: mocks.getRoutePath,
  getRouteName: mocks.getRouteName
}));

vi.mock('@/service/api', () => ({
  fetchGetUserRoutes: mocks.fetchGetUserRoutes,
  fetchGetConstantRoutes: mocks.fetchGetConstantRoutes
}));

vi.mock('@/hooks/common/icon', () => ({
  useSvgIcon: () => ({
    SvgIconVNode: () => null
  })
}));

describe('route envops integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.resetModules();
    vi.stubEnv('VITE_AUTH_ROUTE_MODE', 'dynamic');
    vi.stubEnv('VITE_ROUTE_HOME', 'home');
    vi.stubEnv('VITE_MENU_ICON', 'mdi:menu');

    setActivePinia(createPinia());

    mocks.authStore.userInfo.userId = 'envops-user';
    mocks.authStore.userInfo.userName = 'envops';
    mocks.authStore.userInfo.roles = ['admin'];
    mocks.authStore.userInfo.buttons = [];

    mocks.authStore.initUserInfo.mockReset();
    mocks.authStore.initUserInfo.mockImplementation(async () => {
      mocks.authStore.userInfo.userId = 'envops-user';
    });

    mocks.fetchGetUserRoutes.mockReset();
    mocks.fetchGetUserRoutes.mockResolvedValue({
      data: {
        routes: mocks.clone(mocks.dynamicRoutes),
        home: 'task_center'
      },
      error: null
    });

    mocks.fetchGetConstantRoutes.mockReset();
    mocks.fetchGetConstantRoutes.mockResolvedValue({
      data: [],
      error: null
    });

    mocks.getAuthVueRoutes.mockReset();
    mocks.getAuthVueRoutes.mockImplementation(mocks.toVueRoutes);

    mocks.createStaticRoutes.mockReset();
    mocks.createStaticRoutes.mockImplementation(() => ({
      constantRoutes: mocks.clone(mocks.staticConstantRoutes),
      authRoutes: mocks.clone(mocks.dynamicRoutes)
    }));

    mocks.getRoutePath.mockReset();
    mocks.getRoutePath.mockImplementation((name: string) => `/${name.replaceAll('_', '/')}`);

    mocks.getRouteName.mockReset();
  });

  it('builds envops menus from backend-shaped dynamic routes', async () => {
    const backendRoutes = mocks.clone(mocks.dynamicRoutes).map((route: any) => ({
      ...route,
      meta: {
        title: route.meta.title,
        order: route.meta.order
      },
      children: route.children?.map((child: any) => ({
        ...child,
        meta: {
          title: child.meta.title,
          order: child.meta.order,
          hideInMenu: child.meta.hideInMenu,
          activeMenu: child.meta.activeMenu
        }
      }))
    }));

    mocks.fetchGetUserRoutes.mockResolvedValue({
      data: {
        routes: backendRoutes,
        home: 'task_center'
      },
      error: null
    });

    const { useRouteStore } = await import('@/store/modules/route');

    const routeStore = useRouteStore();

    await routeStore.initAuthRoute();

    expect(mocks.fetchGetUserRoutes).toHaveBeenCalledOnce();
    expect(routeStore.isInitAuthRoute).toBe(true);
    expect(routeStore.routeHome).toBe('task_center');
    expect(routeStore.menus.map(item => item.routeKey)).toEqual(mocks.menuRouteKeys);
    expect(routeStore.searchMenus.map(item => item.routeKey)).toEqual(mocks.searchMenuRouteKeys);
    expect(mocks.router.removeRoute).toHaveBeenCalledWith('root');
    expect(mocks.router.addRoute).toHaveBeenCalledTimes(8);
    expect(mocks.tabStore.initHomeTab).toHaveBeenCalledOnce();
  });

  it('keeps the hidden task tracking route routable while task center stays visible in menus', async () => {
    const { useRouteStore } = await import('@/store/modules/route');

    const routeStore = useRouteStore();

    await routeStore.initAuthRoute();

    const [injectedRoutes] = mocks.getAuthVueRoutes.mock.calls[0] as [any[]];
    const injectedTaskRoute = injectedRoutes.find(route => route.name === 'task');

    expect(routeStore.menus.map(menu => String(menu.routeKey)).includes('task_tracking_[id]')).toBe(false);
    expect(routeStore.menus.find(menu => menu.routeKey === 'task')?.children?.map(item => item.routeKey)).toEqual([
      'task_center'
    ]);
    expect(injectedRoutes.some(route => route.name === 'task')).toBe(true);
    expect(injectedTaskRoute?.children?.some((child: any) => child.name === 'task_tracking_[id]')).toBe(true);
  });

  it('uses the full known route set to distinguish 403 from 404', async () => {
    const limitedDynamicRoutes = mocks.clone(mocks.dynamicRoutes).filter((route: any) => route.name !== 'system');
    mocks.fetchGetUserRoutes.mockResolvedValue({
      data: {
        routes: limitedDynamicRoutes,
        home: 'task_center'
      },
      error: null
    });
    mocks.getRouteName.mockReturnValue('system_user');

    const { useRouteStore } = await import('@/store/modules/route');

    const routeStore = useRouteStore();

    await routeStore.initAuthRoute();

    await expect(routeStore.getIsAuthRouteExist('/system/user' as never)).resolves.toBe(true);
    expect(routeStore.menus.map(item => item.routeKey)).not.toContain('system');
  });

  it('keeps local constant route definitions authoritative in dynamic mode', async () => {
    mocks.fetchGetConstantRoutes.mockResolvedValue({
      data: [
        {
          name: 'login',
          path: '/login',
          component: 'layout.blank$view.login',
          meta: {
            title: 'login',
            i18nKey: 'route.login',
            constant: true,
            hideInMenu: true
          }
        },
        {
          name: 'callback',
          path: '/callback',
          component: 'layout.blank$view.callback',
          meta: {
            title: 'callback',
            constant: true,
            hideInMenu: true
          }
        }
      ],
      error: null
    });

    const { useRouteStore } = await import('@/store/modules/route');

    const routeStore = useRouteStore();

    await routeStore.initConstantRoute();

    const [constantRoutes] = mocks.getAuthVueRoutes.mock.calls[0] as [any[]];

    expect(constantRoutes.find(route => route.name === 'login')?.path).toBe(mocks.loginRoutePath);
    expect(constantRoutes.find(route => route.name === 'callback')?.path).toBe('/callback');
  });

  it('initializes user info before fetching dynamic routes and injecting menus', async () => {
    const callOrder: string[] = [];

    mocks.authStore.userInfo.userId = '';
    mocks.authStore.initUserInfo.mockImplementation(async () => {
      callOrder.push('initUserInfo');
      mocks.authStore.userInfo.userId = 'envops-user';
    });
    mocks.fetchGetUserRoutes.mockImplementation(async () => {
      callOrder.push('fetchGetUserRoutes');

      return {
        data: {
          routes: mocks.clone(mocks.dynamicRoutes),
          home: 'task_center'
        },
        error: null
      };
    });

    const { useRouteStore } = await import('@/store/modules/route');

    const routeStore = useRouteStore();

    await routeStore.initAuthRoute();

    expect(callOrder).toEqual(['initUserInfo', 'fetchGetUserRoutes']);
    expect(mocks.authStore.initUserInfo).toHaveBeenCalledOnce();
    expect(mocks.fetchGetUserRoutes).toHaveBeenCalledOnce();
    expect(routeStore.isInitAuthRoute).toBe(true);
    expect(routeStore.routeHome).toBe('task_center');
    expect(routeStore.menus.map(item => item.routeKey)).toEqual(mocks.menuRouteKeys);
  });
});

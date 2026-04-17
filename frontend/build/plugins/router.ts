import type { RouteMeta } from 'vue-router';
import ElegantVueRouter from '@elegant-router/vue/vite';
import type { RouteKey } from '@elegant-router/types';

export function setupElegantRouter() {
  return ElegantVueRouter({
    layouts: {
      base: 'src/layouts/base-layout/index.vue',
      blank: 'src/layouts/blank-layout/index.vue'
    },
    routePathTransformer(routeName, routePath) {
      const key = routeName as RouteKey;

      if (key === 'login') {
        const modules: UnionKey.LoginModule[] = ['pwd-login', 'code-login', 'register', 'reset-pwd', 'bind-wechat'];

        const moduleReg = modules.join('|');

        return `/login/:module(${moduleReg})?`;
      }

      return routePath;
    },
    onRouteMetaGen(routeName) {
      const key = routeName as RouteKey;

      const constantRoutes: RouteKey[] = ['login', '403', '404', '500'];

      const envopsRouteMeta: Partial<Record<string, Partial<RouteMeta>>> = {
        home: {
          icon: 'mdi:monitor-dashboard',
          order: 1
        },
        asset: {
          icon: 'mdi:server-network',
          order: 2
        },
        monitor: {
          icon: 'mdi:radar',
          order: 3
        },
        app: {
          icon: 'mdi:application-braces-outline',
          order: 4
        },
        deploy: {
          icon: 'mdi:rocket-launch-outline',
          order: 5
        },
        task: {
          icon: 'mdi:clipboard-text-clock-outline',
          order: 6
        },
        traffic: {
          icon: 'mdi:source-branch',
          order: 7
        },
        system: {
          icon: 'mdi:account-cog-outline',
          order: 8
        }
      };

      const meta: Partial<RouteMeta> = {
        title: key,
        i18nKey: `route.${key}` as App.I18n.I18nKey,
        ...envopsRouteMeta[key]
      };

      if (constantRoutes.includes(key)) {
        meta.constant = true;
      }

      return meta;
    }
  });
}

import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { PluginOption, ResolvedConfig } from 'vite';
import type { RouteMeta } from 'vue-router';
import ElegantVueRouter from '@elegant-router/vue/vite';
import type { RouteKey } from '@elegant-router/types';

let frontendRoot = process.cwd();

export function setupElegantRouter() {
  return [
    ElegantVueRouter({
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
    }),
    {
      name: 'envops-task-tracking-route-patch',
      enforce: 'pre' as const,
      configResolved(config: ResolvedConfig) {
        frontendRoot = config.root;
      },
      buildStart() {
        patchTaskTrackingRouteArtifacts();
      },
      configureServer() {
        patchTaskTrackingRouteArtifacts();
      },
      closeBundle() {
        patchTaskTrackingRouteArtifacts();
      },
      transform(_code: string, id: string) {
        if (!isTaskTrackingRouteArtifact(id)) {
          return null;
        }

        patchTaskTrackingRouteArtifacts();
        return readFileSync(stripQuery(id), 'utf8');
      }
    }
  ] satisfies PluginOption[];
}

type RouteArtifactPatch = {
  path: string;
  patch: (source: string) => string;
  required: string[];
  forbidden: string[];
};

function isTaskTrackingRouteArtifact(id: string) {
  const path = stripQuery(id);

  return (
    path.endsWith('/src/router/elegant/imports.ts') ||
    path.endsWith('/src/router/elegant/routes.ts') ||
    path.endsWith('/src/router/elegant/transform.ts') ||
    path.endsWith('/src/typings/elegant-router.d.ts')
  );
}

function stripQuery(id: string) {
  return id.split('?')[0];
}

function patchTaskTrackingRouteArtifacts() {
  const patches: RouteArtifactPatch[] = [
    {
      path: 'src/router/elegant/imports.ts',
      patch: patchImportsArtifact,
      required: [
        'GeneratedLastLevelRouteKey',
        'Record<GeneratedLastLevelRouteKey',
        '  "task_tracking_[id]": () => import("@/views/task/tracking/[id].vue"),'
      ],
      forbidden: ['Record<LastLevelRouteKey', '  task_tracking: () => import("@/views/task/tracking/[id].vue"),']
    },
    {
      path: 'src/router/elegant/routes.ts',
      patch: patchRoutesArtifact,
      required: ["name: 'task_tracking_[id]'", "path: '/task/tracking/:id'", "component: 'view.task_tracking_[id]'"],
      forbidden: ["name: 'task_tracking'", "component: 'view.task_tracking'", "i18nKey: 'route.task_tracking'"]
    },
    {
      path: 'src/router/elegant/transform.ts',
      patch: patchTransformArtifact,
      required: ['  "task_tracking_[id]": "/task/tracking/:id",'],
      forbidden: ['  "task_tracking": "/task/tracking/:id",']
    },
    {
      path: 'src/typings/elegant-router.d.ts',
      patch: patchTypesArtifact,
      required: [
        'export type GeneratedRouteMapKey = keyof RouteMap;',
        'type HiddenRouteKey = "task_tracking_[id]";',
        'export type RouteKey = Exclude<GeneratedRouteMapKey, HiddenRouteKey>;',
        'export type GeneratedRouteKey = Exclude<GeneratedRouteMapKey, CustomRouteKey>;',
        'export type GeneratedLastLevelRouteKey = Extract<',
        'export type LastLevelRouteKey = Exclude<GeneratedLastLevelRouteKey, HiddenRouteKey>;',
        'type GetChildRouteKey<K extends GeneratedRouteMapKey, T extends GeneratedRouteMapKey = GeneratedRouteMapKey>',
        'type LastLevelRoute<K extends GeneratedRouteKey> = K extends GeneratedLastLevelRouteKey'
      ],
      forbidden: [
        '    "task_tracking": "/task/tracking/:id";',
        'export type RouteKey = keyof RouteMap;',
        'export type GeneratedRouteKey = Exclude<RouteKey, CustomRouteKey>;',
        'export type CenterLevelRouteKey = Exclude<GeneratedRouteKey, FirstLevelRouteKey | LastLevelRouteKey>;',
        'type GetChildRouteKey<K extends RouteKey, T extends RouteKey = RouteKey>',
        'type LastLevelRoute<K extends GeneratedRouteKey> = K extends LastLevelRouteKey',
        '    | "task_tracking"\n'
      ]
    }
  ];

  patches.forEach(patch => patchFile(patch));
  patches.forEach(assertPatchedArtifact);
}

function patchImportsArtifact(source: string) {
  return source
    .replace(
      'import type { LastLevelRouteKey, RouteLayout } from "@elegant-router/types";',
      'import type { GeneratedLastLevelRouteKey, RouteLayout } from "@elegant-router/types";'
    )
    .replace(
      'Record<LastLevelRouteKey, RouteComponent | (() => Promise<RouteComponent>)>',
      'Record<GeneratedLastLevelRouteKey, RouteComponent | (() => Promise<RouteComponent>)>'
    )
    .replace(
      '  task_tracking: () => import("@/views/task/tracking/[id].vue"),',
      '  "task_tracking_[id]": () => import("@/views/task/tracking/[id].vue"),'
    );
}

function patchRoutesArtifact(source: string) {
  return source.replace(
    `      {
        name: 'task_tracking',
        path: '/task/tracking/:id',
        component: 'view.task_tracking',
        meta: {
          title: 'task_tracking',
          i18nKey: 'route.task_tracking'
        }
      }`,
    `      {
        name: 'task_tracking_[id]',
        path: '/task/tracking/:id',
        component: 'view.task_tracking_[id]',
        meta: {
          title: 'task_tracking_[id]'
        }
      }`
  );
}

function patchTransformArtifact(source: string) {
  return source.replace('  "task_tracking": "/task/tracking/:id",', '  "task_tracking_[id]": "/task/tracking/:id",');
}

function patchTypesArtifact(source: string) {
  return patchLastLevelRouteTypes(source)
    .replace('    "task_tracking": "/task/tracking/:id";', '    "task_tracking_[id]": "/task/tracking/:id";')
    .replace(
      `  /**
   * route key
   */
  export type RouteKey = keyof RouteMap;`,
      `  /**
   * internal route key including generated hidden route entries
   */
  export type GeneratedRouteMapKey = keyof RouteMap;

  /**
   * hidden route keys that do not need top-level route locale records
   */
  type HiddenRouteKey = "task_tracking_[id]";

  /**
   * route key
   */
  export type RouteKey = Exclude<GeneratedRouteMapKey, HiddenRouteKey>;`
    )
    .replace(
      '  export type GeneratedRouteKey = Exclude<RouteKey, CustomRouteKey>;',
      '  export type GeneratedRouteKey = Exclude<GeneratedRouteMapKey, CustomRouteKey>;'
    )
    .replace(
      '  export type CenterLevelRouteKey = Exclude<GeneratedRouteKey, FirstLevelRouteKey | LastLevelRouteKey>;',
      '  export type CenterLevelRouteKey = Exclude<GeneratedRouteKey, FirstLevelRouteKey | GeneratedLastLevelRouteKey>;'
    )
    .replace(
      `  type GetChildRouteKey<K extends RouteKey, T extends RouteKey = RouteKey> = T extends \`${'${K}'}_\${infer R}\`
    ? R extends \`${'${string}'}_\${string}\`
      ? never
      : T
    : never;`,
      `  type GetChildRouteKey<K extends GeneratedRouteMapKey, T extends GeneratedRouteMapKey = GeneratedRouteMapKey> = T extends \`${'${K}'}_\${infer R}\`
    ? R extends \`[\${string}]\`
      ? T
      : R extends \`tracking_[\${string}]\`
        ? T
        : R extends \`${'${string}'}_\${string}\`
          ? never
          : T
    : never;`
    )
    .replace(
      '  type LastLevelRoute<K extends GeneratedRouteKey> = K extends LastLevelRouteKey',
      '  type LastLevelRoute<K extends GeneratedRouteKey> = K extends GeneratedLastLevelRouteKey'
    );
}

function patchLastLevelRouteTypes(source: string) {
  if (source.includes('export type GeneratedLastLevelRouteKey = Extract<')) {
    return source;
  }

  const routeBlockPattern =
    /  \/\*\*\n   \* the last level route key, which has the page file\n   \*\/\n  export type LastLevelRouteKey = Extract<\n    RouteKey,\n([\s\S]*?)  >;/;
  const match = source.match(routeBlockPattern);

  if (!match) {
    return source;
  }

  const lastLevelRouteUnion = match[1].replace('    | "task_tracking"', '    | "task_tracking_[id]"');

  return source.replace(
    routeBlockPattern,
    `  /**
   * generated last level route key including hidden route entries
   */
  export type GeneratedLastLevelRouteKey = Extract<
    GeneratedRouteMapKey,
${lastLevelRouteUnion}  >;

  /**
   * the last level route key, which has the page file and participates in app tabs and route home
   */
  export type LastLevelRouteKey = Exclude<GeneratedLastLevelRouteKey, HiddenRouteKey>;`
  );
}

function patchFile(patch: RouteArtifactPatch) {
  const filePath = resolve(frontendRoot, patch.path);
  const source = readFileSync(filePath, 'utf8');
  const next = patch.patch(source);

  if (next !== source) {
    writeFileSync(filePath, next);
  }
}

function assertPatchedArtifact(patch: RouteArtifactPatch) {
  const filePath = resolve(frontendRoot, patch.path);
  const source = readFileSync(filePath, 'utf8');
  const missing = patch.required.filter(marker => !source.includes(marker));
  const present = patch.forbidden.filter(marker => source.includes(marker));

  if (missing.length || present.length) {
    throw new Error(
      `Failed to patch ${patch.path}. Missing markers: ${missing.map(marker => JSON.stringify(marker)).join(', ') || 'none'}. Unexpected markers: ${present.map(marker => JSON.stringify(marker)).join(', ') || 'none'}.`
    );
  }
}

import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const routesFile = path.resolve(__dirname, 'router/elegant/routes.ts');
const routesSource = readFileSync(routesFile, 'utf8');

describe('app center route registration', () => {
  it('registers all task 6 app pages with real route keys', () => {
    const requiredRoutes = [
      "name: 'app'",
      "name: 'app_definition'",
      "name: 'app_version'",
      "name: 'app_package'",
      "name: 'app_config-template'",
      "name: 'app_script-template'"
    ];

    for (const routeSignature of requiredRoutes) {
      expect(routesSource).toMatch(new RegExp(routeSignature.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
    }
  });
});

import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const definitionPage = readFileSync(path.resolve(__dirname, 'definition/index.vue'), 'utf8');
const versionPage = readFileSync(path.resolve(__dirname, 'version/index.vue'), 'utf8');
const packagePage = readFileSync(path.resolve(__dirname, 'package/index.vue'), 'utf8');
const configTemplatePage = readFileSync(path.resolve(__dirname, 'config-template/index.vue'), 'utf8');
const scriptTemplatePage = readFileSync(path.resolve(__dirname, 'script-template/index.vue'), 'utf8');
const apiSource = readFileSync(path.resolve(__dirname, '../../service/api/app.ts'), 'utf8');
const appTypingSource = readFileSync(path.resolve(__dirname, '../../typings/api/app.d.ts'), 'utf8');

describe('app center contract wiring', () => {
  it('does not keep mock-only fields in app views', () => {
    const forbiddenMockFields = ['owner', 'repo', 'currentVersion'];

    for (const field of forbiddenMockFields) {
      const pattern = new RegExp(`\\b${field}\\b`);

      expect(definitionPage).not.toMatch(pattern);
      expect(versionPage).not.toMatch(pattern);
      expect(packagePage).not.toMatch(pattern);
      expect(configTemplatePage).not.toMatch(pattern);
      expect(scriptTemplatePage).not.toMatch(pattern);
    }
  });

  it('keeps app api endpoints aligned with backend contracts', () => {
    const requiredEndpoints = [
      '/api/apps',
      '/api/apps/${id}',
      '/api/apps/${appId}/versions',
      '/api/app-versions/${id}',
      '/api/packages',
      '/api/packages/upload',
      '/api/config-templates',
      '/api/script-templates'
    ];

    for (const endpoint of requiredEndpoints) {
      expect(apiSource).toMatch(new RegExp(endpoint.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
    }
  });

  it('keeps package upload on multipart real-file form data', () => {
    expect(apiSource).toMatch(/headers:\s*\{\s*'Content-Type': 'multipart\/form-data'\s*\}/);
    expect(apiSource).toMatch(/formData\.append\('file', payload\.file\)/);
  });

  it('keeps frontend package storage choices aligned with current backend capability', () => {
    expect(packagePage).not.toContain("'MINIO'");
  });

  it('keeps frontend app typings aligned with current backend storage capability', () => {
    expect(appTypingSource).toMatch(/type\s+StorageType\s*=\s*'LOCAL'\s*\|\s*null/);
    expect(appTypingSource).not.toMatch(/type\s+StorageType\s*=.*\|\s*string\s*\|/);
  });
});

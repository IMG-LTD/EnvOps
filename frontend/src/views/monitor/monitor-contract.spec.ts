import { readFileSync } from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

const detectTaskFile = path.resolve(process.cwd(), 'src/views/monitor/detect-task/index.vue');
const metricFile = path.resolve(process.cwd(), 'src/views/monitor/metric/index.vue');
const monitorTypingFile = path.resolve(process.cwd(), 'src/typings/api/monitor.d.ts');

describe('monitor backend contract wiring', () => {
  it('aligns detect task page and API typings with backend detect task fields', () => {
    const detectTaskSource = readFileSync(detectTaskFile, 'utf8');
    const monitorTypingSource = readFileSync(monitorTypingFile, 'utf8');

    expect(monitorTypingSource).toContain('target?: string | null;');
    expect(monitorTypingSource).toContain('lastRunAt?: string | null;');
    expect(monitorTypingSource).toContain('lastResult?: string | null;');
    expect(monitorTypingSource).not.toContain('taskNo: string;');
    expect(monitorTypingSource).not.toContain('taskType: string;');
    expect(monitorTypingSource).not.toContain('targetName?: string | null;');
    expect(monitorTypingSource).not.toContain('targetNames?: string[] | null;');
    expect(monitorTypingSource).not.toContain('status: string;');
    expect(monitorTypingSource).not.toContain('startedAt?: string | null;');
    expect(monitorTypingSource).not.toContain('finishedAt?: string | null;');
    expect(monitorTypingSource).not.toContain('updatedAt?: string | null;');

    expect(detectTaskSource).toContain('item.target');
    expect(detectTaskSource).toContain('item.lastRunAt || item.createdAt');
    expect(detectTaskSource).toContain("String(item.lastResult || '')");
    expect(detectTaskSource).not.toContain('item.taskNo');
    expect(detectTaskSource).not.toContain('item.targetName');
    expect(detectTaskSource).not.toContain('item.targetNames');
    expect(detectTaskSource).not.toContain('item.targetCount');
    expect(detectTaskSource).not.toContain('String(item.status');
    expect(detectTaskSource).not.toContain('item.startedAt');
    expect(detectTaskSource).not.toContain('item.finishedAt');
    expect(detectTaskSource).not.toContain('item.updatedAt');
  });

  it('aligns metric page and API typings with backend host fact fields', () => {
    const metricSource = readFileSync(metricFile, 'utf8');
    const monitorTypingSource = readFileSync(monitorTypingFile, 'utf8');

    expect(monitorTypingSource).toContain('hostName?: string | null;');
    expect(monitorTypingSource).toContain('osName?: string | null;');
    expect(monitorTypingSource).toContain('agentVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('hostnameFact?: string | null;');
    expect(monitorTypingSource).not.toContain('cpuModel?: string | null;');
    expect(monitorTypingSource).not.toContain('diskGb?: number | null;');
    expect(monitorTypingSource).not.toContain('jdkVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('dockerVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('nginxVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('redisVersion?: string | null;');
    expect(monitorTypingSource).not.toContain('mysqlVersion?: string | null;');

    expect(metricSource).toContain('hostFacts.value?.hostName');
    expect(metricSource).toContain('hostFacts.value?.osName');
    expect(metricSource).toContain('hostFacts.value?.agentVersion');
    expect(metricSource).not.toContain('hostFacts.value?.hostnameFact');
    expect(metricSource).not.toContain('hostFacts.value?.cpuModel');
    expect(metricSource).not.toContain('hostFacts.value?.diskGb');
    expect(metricSource).not.toContain('hostFacts.value?.jdkVersion');
    expect(metricSource).not.toContain('hostFacts.value?.dockerVersion');
    expect(metricSource).not.toContain('hostFacts.value?.nginxVersion');
    expect(metricSource).not.toContain('hostFacts.value?.redisVersion');
    expect(metricSource).not.toContain('hostFacts.value?.mysqlVersion');
  });
});

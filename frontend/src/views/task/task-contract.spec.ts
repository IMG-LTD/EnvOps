import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const deployTaskPage = readFileSync(path.resolve(__dirname, '../deploy/task/index.vue'), 'utf8');
const taskCenterPage = readFileSync(path.resolve(__dirname, 'center/index.vue'), 'utf8');
const taskApiSource = readFileSync(path.resolve(__dirname, '../../service/api/task.ts'), 'utf8');
const taskTypingSource = readFileSync(path.resolve(__dirname, '../../typings/api/task.d.ts'), 'utf8');
const appTypingSource = readFileSync(path.resolve(__dirname, '../../typings/app.d.ts'), 'utf8');
const apiIndexSource = readFileSync(path.resolve(__dirname, '../../service/api/index.ts'), 'utf8');
const zhLocaleSource = readFileSync(path.resolve(__dirname, '../../locales/langs/zh-cn.ts'), 'utf8');
const enLocaleSource = readFileSync(path.resolve(__dirname, '../../locales/langs/en-us.ts'), 'utf8');

describe('task pages contract wiring', () => {
  it('exposes task api fetchers through the shared api barrel', () => {
    expect(taskApiSource).toMatch(/export function fetchGetDeployTasks\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*['"]\/api\/deploy\/tasks['"]/);
    expect(taskApiSource).toMatch(/export function fetchPostExecuteDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/execute`/);
    expect(taskApiSource).toMatch(/export function fetchPostRetryDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/retry`/);
    expect(taskApiSource).toMatch(/export function fetchPostRollbackDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/rollback`/);
    expect(taskApiSource).toMatch(/export function fetchPostCancelDeployTask\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/cancel`/);
    expect(taskApiSource).toMatch(/export function fetchGetDeployTaskHosts\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/hosts`/);
    expect(taskApiSource).toMatch(/export function fetchGetDeployTaskLogs\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*`\/api\/deploy\/tasks\/\$\{id\}\/logs`/);
    expect(taskApiSource).toMatch(/export function fetchGetTaskCenterTasks\s*\(/);
    expect(taskApiSource).toMatch(/url:\s*['"]\/api\/task-center\/tasks['"]/);
    expect(apiIndexSource).toContain("export * from './task'");
  });

  it('defines frontend task typings for deploy and task center rows', () => {
    expect(taskTypingSource).toContain('namespace Task');
    expect(taskTypingSource).toContain('interface DeployTaskRecord');
    expect(taskTypingSource).toContain('originTaskId?: number | null');
    expect(taskTypingSource).toContain('interface DeployTaskHostRecord');
    expect(taskTypingSource).toContain('interface DeployTaskLogRecord');
    expect(taskTypingSource).toContain('interface TaskCenterRecord');
  });

  it('loads deploy task page from real api data instead of static mock arrays', () => {
    expect(deployTaskPage).toContain('fetchGetDeployTasks');
    expect(deployTaskPage).toContain('await fetchGetDeployTasks(');
    expect(deployTaskPage).toContain('const taskList = ref');
    expect(deployTaskPage).toContain('taskList.value =');
    expect(deployTaskPage).toContain('handleExecuteTask');
    expect(deployTaskPage).toContain('handleRetryTask');
    expect(deployTaskPage).toContain('handleRollbackTask');
    expect(deployTaskPage).toContain('handleCancelTask');
    expect(deployTaskPage).toContain('loadTaskDetail');
    expect(deployTaskPage).toContain('const taskHosts = ref<Api.Task.DeployTaskHostRecord[]>');
    expect(deployTaskPage).toContain('const taskLogs = ref<Api.Task.DeployTaskLogRecord[]>');
    expect(deployTaskPage).toContain('NDrawer');
    expect(deployTaskPage).toContain('common.refresh');
    expect(deployTaskPage).toContain('NEmpty');
    expect(deployTaskPage).not.toContain('const deployTasks = computed(() => [');
  });

  it('loads task center page from real api data instead of static mock arrays', () => {
    expect(taskCenterPage).toContain('fetchGetTaskCenterTasks');
    expect(taskCenterPage).toContain('await fetchGetTaskCenterTasks(');
    expect(taskCenterPage).toContain('const taskList = ref');
    expect(taskCenterPage).toContain('taskList.value =');
    expect(taskCenterPage).toContain('NEmpty');
    expect(taskCenterPage).not.toContain('const taskList = computed(() => [');
  });

  it('maps upgrade and rollback task types into deploy task labels', () => {
    expect(taskCenterPage).toContain("normalizedValue.includes('upgrade')");
    expect(taskCenterPage).toContain("normalizedValue.includes('rollback')");
  });

  it('maps cancelled task status into cancelled label instead of queued', () => {
    expect(taskCenterPage).toContain("normalizedStatus.includes('cancel')");
    expect(taskCenterPage).toContain("cancelled: t('page.envops.common.status.cancelled')");
    expect(taskCenterPage).toContain("cancelled: 'warning'");
  });

  it('maps cancelled deploy task status into cancelled label instead of pending', () => {
    expect(deployTaskPage).toContain("normalizedStatus.includes('cancel')");
    expect(deployTaskPage).toContain("cancelled: t('page.envops.common.status.cancelled')");
    expect(deployTaskPage).toContain("cancelled: 'warning'");
  });

  it('maps approval and rejected statuses to explicit frontend labels', () => {
    expect(taskCenterPage).toContain("normalizedStatus.includes('approval')");
    expect(taskCenterPage).toContain("normalizedStatus.includes('reject')");
    expect(taskCenterPage).toContain("pendingApproval: t('page.envops.common.status.pendingApproval')");
    expect(taskCenterPage).toContain("pendingApproval: 'warning'");
    expect(taskCenterPage).toContain("rejected: t('page.envops.common.status.rejected')");
    expect(deployTaskPage).toContain("normalizedStatus.includes('approval')");
    expect(deployTaskPage).toContain("normalizedStatus.includes('reject')");
    expect(deployTaskPage).toContain("pendingApproval: t('page.envops.common.status.pendingApproval')");
    expect(deployTaskPage).toContain("pendingApproval: 'warning'");
    expect(deployTaskPage).toContain("rejected: t('page.envops.common.status.rejected')");
  });

  it('allows cancel only for pending or running deploy tasks', () => {
    expect(deployTaskPage).toContain("function canCancelTask(statusKey: DeployTaskStatusKey)");
    expect(deployTaskPage).toContain("return statusKey === 'running' || statusKey === 'pending'");
    expect(deployTaskPage).not.toContain("return statusKey === 'running' || statusKey === 'pending' || statusKey === 'pendingApproval'");
  });

  it('prevents duplicate row mutations and rollback-of-rollback actions', () => {
    expect(deployTaskPage).toContain('const actionLoadingTaskIds = ref<number[]>([])');
    expect(deployTaskPage).toContain('function isTaskMutating(taskId: number)');
    expect(deployTaskPage).toContain('actionLoadingTaskIds.value.includes(taskId)');
    expect(deployTaskPage).toContain('actionLoadingTaskIds.value = [...actionLoadingTaskIds.value, taskId]');
    expect(deployTaskPage).toContain('actionLoadingTaskIds.value = actionLoadingTaskIds.value.filter(id => id !== taskId)');
    expect(deployTaskPage).toContain('if (isTaskMutating(taskId)) {');
    expect(deployTaskPage).toContain("return taskType !== 'ROLLBACK'");
    expect(deployTaskPage).toContain(":disabled=\"isTaskMutating(item.key) || !canExecuteTask(item.statusKey)\"");
    expect(deployTaskPage).toContain(":disabled=\"isTaskMutating(item.key) || !canRetryTask(item.statusKey)\"");
    expect(deployTaskPage).toContain(":disabled=\"isTaskMutating(item.key) || !canRollbackTask(item.statusKey, item.taskType)\"");
    expect(deployTaskPage).toContain(":disabled=\"isTaskMutating(item.key) || !canCancelTask(item.statusKey)\"");
  });

  it('clears stale task detail state before loading a different task', () => {
    expect(deployTaskPage).toContain('const detailRequestToken = ref(0)');
    expect(deployTaskPage).toContain('const requestToken = ++detailRequestToken.value');
    expect(deployTaskPage).toContain('if (requestToken !== detailRequestToken.value) {');
    expect(deployTaskPage).toContain('activeTask.value = null');
    expect(deployTaskPage).toContain('taskHosts.value = []');
    expect(deployTaskPage).toContain('taskLogs.value = []');
    expect(deployTaskPage).not.toContain('nextTaskList.find(item => item.id === taskId) ?? activeTask.value');
  });

  it('guards list refreshes and detail refresh-list writes with latest-only request tokens', () => {
    expect(deployTaskPage).toContain('const listRequestToken = ref(0)');
    expect(deployTaskPage).toContain('const requestToken = ++listRequestToken.value');
    expect(deployTaskPage).toContain('if (requestToken !== listRequestToken.value) {');
    expect(deployTaskPage).toContain('const listToken = refreshList ? ++listRequestToken.value : listRequestToken.value');
    expect(deployTaskPage).toContain('const currentTaskList = refreshList && listToken === listRequestToken.value ? nextTaskList : taskList.value');
    expect(deployTaskPage).toContain('if (refreshList && listToken === listRequestToken.value) {');
    expect(deployTaskPage).toContain('activeTask.value = currentTaskList.find(item => item.id === taskId) ?? null;');
    expect(deployTaskPage.indexOf('if (requestToken !== listRequestToken.value) {')).toBeLessThan(
      deployTaskPage.indexOf('taskList.value = nextTaskList;')
    );
    expect(deployTaskPage.indexOf('if (refreshList && listToken === listRequestToken.value) {')).toBeLessThan(
      deployTaskPage.lastIndexOf('taskList.value = nextTaskList;')
    );
  });

  it('uses updateSuccess toast copy for deploy task actions', () => {
    expect(deployTaskPage).toContain("t('common.updateSuccess')");
    expect(deployTaskPage).not.toContain("t('common.success')");
  });

  it('maps cancel requested deploy task status explicitly', () => {
    expect(deployTaskPage).toContain("normalizedStatus.includes('cancel_requested')");
    expect(deployTaskPage).toContain("return 'running'");
  });

  it('counts pending approval tasks in task center queued summary', () => {
    expect(taskCenterPage).toContain("item.statusKey === 'queued' || item.statusKey === 'pendingApproval'");
  });

  it('declares deploy task detail i18n keys in locale schema and messages', () => {
    expect(appTypingSource).toContain('actions: {');
    expect(appTypingSource).toContain('detail: {');
    expect(appTypingSource).toContain('hosts: {');
    expect(appTypingSource).toContain('logs: {');
    expect(appTypingSource).toContain('manualRefresh: string');
    expect(appTypingSource).toContain('execute: string');
    expect(appTypingSource).toContain('retry: string');
    expect(appTypingSource).toContain('rollback: string');
    expect(appTypingSource).toContain('cancel: string');
    expect(appTypingSource).toContain('originTaskId: string');
    expect(zhLocaleSource).toContain('manualRefresh');
    expect(zhLocaleSource).toContain('originTaskId');
    expect(zhLocaleSource).toContain('rollback');
    expect(enLocaleSource).toContain('manualRefresh');
    expect(enLocaleSource).toContain('originTaskId');
    expect(enLocaleSource).toContain('rollback');
    expect(appTypingSource).toContain('cancelled: string');
    expect(appTypingSource).toContain('rejected: string');
  });
});

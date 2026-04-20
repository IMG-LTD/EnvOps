import { describe, expect, it } from 'vitest';
import * as queryHelpers from './query';
import {
  normalizeDeployTaskRouteQuery,
  normalizeTaskCenterRouteQuery,
  toDeployTaskApiQuery,
  toTaskCenterApiQuery
} from './query';

function formatExpectedLocalDateTime(value: string) {
  const date = new Date(value);
  const pad = (unit: number) => String(unit).padStart(2, '0');

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}:${pad(date.getSeconds())}`;
}

describe('task route query helpers', () => {
  it('normalizes invalid deploy task route query to safe defaults', () => {
    expect(
      normalizeDeployTaskRouteQuery({
        status: undefined,
        taskType: null,
        appId: 'abc',
        environment: [],
        keyword: 123,
        createdFrom: {},
        createdTo: false,
        page: '0',
        pageSize: 'NaN',
        sortBy: 'oops',
        sortOrder: 'sideways',
        taskId: 'abc'
      })
    ).toEqual({
      status: '',
      taskType: '',
      appId: null,
      environment: '',
      keyword: '',
      createdFrom: '',
      createdTo: '',
      page: 1,
      pageSize: 10,
      sortBy: 'createdAt',
      sortOrder: 'desc',
      taskId: null
    });
  });

  it('strictly parses deploy numeric route fields', () => {
    expect(
      normalizeDeployTaskRouteQuery({
        appId: '12abc',
        taskId: '2.5',
        page: '1e3',
        pageSize: '12abc'
      })
    ).toMatchObject({
      appId: null,
      taskId: null,
      page: 1,
      pageSize: 10
    });

    expect(
      normalizeDeployTaskRouteQuery({
        appId: '2.5',
        taskId: '1e3',
        page: '2.5',
        pageSize: '1e3'
      })
    ).toMatchObject({
      appId: null,
      taskId: null,
      page: 1,
      pageSize: 10
    });

    expect(
      normalizeDeployTaskRouteQuery({
        appId: ' 02 ',
        taskId: ' 02 ',
        page: ' 02 ',
        pageSize: ' 02 '
      })
    ).toMatchObject({
      appId: 2,
      taskId: 2,
      page: 2,
      pageSize: 2
    });
  });

  it('normalizes deploy environment aliases into canonical values and clears invalid values', () => {
    expect(normalizeDeployTaskRouteQuery({ environment: 'prod' }).environment).toBe('production');
    expect(normalizeDeployTaskRouteQuery({ environment: 'production' }).environment).toBe('production');
    expect(normalizeDeployTaskRouteQuery({ environment: 'UAT' }).environment).toBe('staging');
    expect(normalizeDeployTaskRouteQuery({ environment: ' development ' }).environment).toBe('sandbox');
    expect(normalizeDeployTaskRouteQuery({ environment: 'prod-like' }).environment).toBe('');
    expect(normalizeDeployTaskRouteQuery({ environment: '   ' }).environment).toBe('');
  });

  it('maps normalized deploy route query to backend api params without taskId', () => {
    expect(
      toDeployTaskApiQuery({
        status: 'RUNNING',
        taskType: 'INSTALL',
        appId: 1001,
        environment: 'production',
        keyword: 'order',
        createdFrom: '2026-04-01T00:00:00',
        createdTo: '2026-04-17T23:59:59',
        page: 2,
        pageSize: 20,
        sortBy: 'updatedAt',
        sortOrder: 'asc',
        taskId: 2001
      })
    ).toEqual({
      status: 'RUNNING',
      taskType: 'INSTALL',
      appId: 1001,
      environment: 'production',
      keyword: 'order',
      createdFrom: '2026-04-01T00:00:00',
      createdTo: '2026-04-17T23:59:59',
      page: 2,
      pageSize: 20,
      sortBy: 'updatedAt',
      sortOrder: 'asc'
    });
  });

  it('serializes created range timestamps as timezone-free local datetime strings', () => {
    const start = new Date(2026, 3, 1, 0, 0, 0).getTime();
    const end = new Date(2026, 3, 17, 23, 59, 59).getTime();

    expect(queryHelpers.formatLocalDateTimeRange([start, end])).toEqual(['2026-04-01T00:00:00', '2026-04-17T23:59:59']);
  });

  it('normalizes created datetime route fields to canonical local strings', () => {
    expect(
      normalizeDeployTaskRouteQuery({
        createdFrom: '2026-04-01T00:00:00',
        createdTo: '2026-04-17T23:59:59'
      })
    ).toMatchObject({
      createdFrom: '2026-04-01T00:00:00',
      createdTo: '2026-04-17T23:59:59'
    });

    expect(
      normalizeDeployTaskRouteQuery({
        createdFrom: '2026-04-01T00:00:00.000Z',
        createdTo: '2026-04-17T23:59:59.250+08:00'
      })
    ).toMatchObject({
      createdFrom: formatExpectedLocalDateTime('2026-04-01T00:00:00.000Z'),
      createdTo: formatExpectedLocalDateTime('2026-04-17T23:59:59.250+08:00')
    });

    expect(
      normalizeDeployTaskRouteQuery({
        createdFrom: 'not-a-date',
        createdTo: '2026-13-99T99:99:99'
      })
    ).toMatchObject({
      createdFrom: '',
      createdTo: ''
    });
  });

  it('rejects rollover-invalid created datetime route fields', () => {
    expect(
      normalizeDeployTaskRouteQuery({
        createdFrom: '2026-02-30T00:00:00',
        createdTo: '2026-02-30T00:00:00Z'
      })
    ).toMatchObject({
      createdFrom: '',
      createdTo: ''
    });
  });

  it('normalizes task center route query and maps it to api params', () => {
    const normalized = normalizeTaskCenterRouteQuery({
      keyword: ['deploy'],
      status: undefined,
      taskType: 'INSTALL',
      priority: 'P1',
      page: '-2',
      pageSize: '0',
      sortBy: 'taskNo',
      sortOrder: 'bad-order'
    });

    expect(normalized).toEqual({
      keyword: '',
      status: '',
      sourceType: '',
      taskType: 'INSTALL',
      priority: 'P1',
      page: 1,
      pageSize: 10,
      sortBy: 'taskNo',
      sortOrder: 'desc'
    });

    expect(toTaskCenterApiQuery(normalized)).toEqual({
      sourceType: 'DEPLOY',
      taskType: 'INSTALL',
      priority: 'P1',
      page: 1,
      pageSize: 10,
      sortBy: 'taskNo',
      sortOrder: 'desc'
    });
  });

  it('forces task-center api queries to DEPLOY so the page cannot pretend to be cross-domain', () => {
    const query = toTaskCenterApiQuery(normalizeTaskCenterRouteQuery({ keyword: 'order-service' }));

    expect(query.sourceType).toBe('DEPLOY');
  });

  it('canonicalizes task center sourceType and priority and clears invalid whitelist values', () => {
    expect(
      normalizeTaskCenterRouteQuery({
        sourceType: ' deploy ',
        priority: ' p1 ',
        page: '2',
        pageSize: '20',
        sortBy: 'status',
        sortOrder: 'asc'
      })
    ).toMatchObject({
      sourceType: 'DEPLOY',
      priority: 'P1',
      page: 2,
      pageSize: 20,
      sortBy: 'status',
      sortOrder: 'asc'
    });

    const invalidNormalized = normalizeTaskCenterRouteQuery({
      sourceType: 'manual',
      priority: 'p0',
      page: '2',
      pageSize: '20',
      sortBy: 'status',
      sortOrder: 'asc'
    });

    expect(invalidNormalized).toMatchObject({
      sourceType: '',
      priority: '',
      page: 2,
      pageSize: 20,
      sortBy: 'status',
      sortOrder: 'asc'
    });

    expect(toTaskCenterApiQuery(invalidNormalized)).toEqual({
      sourceType: 'DEPLOY',
      page: 2,
      pageSize: 20,
      sortBy: 'status',
      sortOrder: 'asc'
    });
  });
});

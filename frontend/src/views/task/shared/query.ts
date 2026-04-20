const DEFAULT_PAGE = 1;
const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_SORT_BY: Api.Task.TaskSortBy = 'createdAt';
const DEFAULT_SORT_ORDER: Api.Task.TaskSortOrder = 'desc';
const TASK_SORT_FIELDS: Api.Task.TaskSortBy[] = ['createdAt', 'updatedAt', 'taskNo', 'status'];
const CANONICAL_LOCAL_DATE_TIME_RE = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/;
const ISO_DATE_TIME_RE = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})?$/;
const DEPLOY_ENVIRONMENT_ALIAS_TO_CANONICAL: Record<string, string> = {
  prod: 'production',
  production: 'production',
  stage: 'staging',
  staging: 'staging',
  pre: 'staging',
  preprod: 'staging',
  uat: 'staging',
  sandbox: 'sandbox',
  test: 'sandbox',
  testing: 'sandbox',
  dev: 'sandbox',
  development: 'sandbox'
};
const TASK_CENTER_SOURCE_TYPES = ['DEPLOY'];
const TASK_CENTER_PRIORITIES = ['P1', 'P2', 'P3'];

type DeployTaskRouteQuery = {
  status: string;
  taskType: string;
  appId: number | null;
  environment: string;
  keyword: string;
  createdFrom: string;
  createdTo: string;
  page: number;
  pageSize: number;
  sortBy: Api.Task.TaskSortBy;
  sortOrder: Api.Task.TaskSortOrder;
  taskId: number | null;
};

type TaskCenterRouteQuery = {
  keyword: string;
  status: string;
  sourceType: string;
  taskType: string;
  priority: string;
  page: number;
  pageSize: number;
  sortBy: Api.Task.TaskSortBy;
  sortOrder: Api.Task.TaskSortOrder;
};

function normalizeString(value: unknown) {
  return typeof value === 'string' ? value : '';
}

function normalizeDeployEnvironment(value: unknown) {
  if (typeof value !== 'string') {
    return '';
  }

  return DEPLOY_ENVIRONMENT_ALIAS_TO_CANONICAL[value.trim().toLowerCase()] ?? '';
}

function normalizeNullableNumber(value: unknown) {
  if (typeof value === 'number' && Number.isInteger(value) && value > 0) {
    return value;
  }

  if (typeof value !== 'string') {
    return null;
  }

  const trimmed = value.trim();

  if (!/^\d+$/.test(trimmed)) {
    return null;
  }

  const parsed = Number(trimmed);

  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

function normalizeTaskCenterSourceType(value: unknown) {
  if (typeof value !== 'string') {
    return '';
  }

  const normalized = value.trim().toUpperCase();

  return TASK_CENTER_SOURCE_TYPES.includes(normalized) ? normalized : '';
}

function normalizeTaskCenterPriority(value: unknown) {
  if (typeof value !== 'string') {
    return '';
  }

  const normalized = value.trim().toUpperCase();

  return TASK_CENTER_PRIORITIES.includes(normalized) ? normalized : '';
}

function normalizePositiveInt(value: unknown, fallback: number) {
  const normalized = normalizeNullableNumber(value);

  return normalized ?? fallback;
}

function normalizeSortBy(value: unknown): Api.Task.TaskSortBy {
  return typeof value === 'string' && TASK_SORT_FIELDS.includes(value as Api.Task.TaskSortBy)
    ? (value as Api.Task.TaskSortBy)
    : DEFAULT_SORT_BY;
}

function normalizeSortOrder(value: unknown): Api.Task.TaskSortOrder {
  return value === 'asc' || value === 'desc' ? value : DEFAULT_SORT_ORDER;
}

function isValidDateTimePart(value: number, min: number, max: number) {
  return Number.isInteger(value) && value >= min && value <= max;
}

function hasValidLocalDateTimeParts(
  year: number,
  month: number,
  day: number,
  hour: number,
  minute: number,
  second: number
) {
  if (
    !isValidDateTimePart(month, 1, 12) ||
    !isValidDateTimePart(day, 1, 31) ||
    !isValidDateTimePart(hour, 0, 23) ||
    !isValidDateTimePart(minute, 0, 59) ||
    !isValidDateTimePart(second, 0, 59)
  ) {
    return false;
  }

  const date = new Date(year, month - 1, day, hour, minute, second);

  return !(
    Number.isNaN(date.getTime()) ||
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day ||
    date.getHours() !== hour ||
    date.getMinutes() !== minute ||
    date.getSeconds() !== second
  );
}

function normalizeCanonicalLocalDateTime(value: string) {
  const trimmed = value.trim();
  const match = CANONICAL_LOCAL_DATE_TIME_RE.exec(trimmed);

  if (!match) {
    return '';
  }

  const [, yearText, monthText, dayText, hourText, minuteText, secondText] = match;
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const hour = Number(hourText);
  const minute = Number(minuteText);
  const second = Number(secondText);

  return hasValidLocalDateTimeParts(year, month, day, hour, minute, second) ? trimmed : '';
}

function padDateTimeUnit(value: number) {
  return String(value).padStart(2, '0');
}

function formatLocalDateTime(timestamp: number) {
  const date = new Date(timestamp);

  return [date.getFullYear(), padDateTimeUnit(date.getMonth() + 1), padDateTimeUnit(date.getDate())]
    .join('-')
    .concat(
      'T',
      [padDateTimeUnit(date.getHours()), padDateTimeUnit(date.getMinutes()), padDateTimeUnit(date.getSeconds())].join(
        ':'
      )
    );
}

export function formatLocalDateTimeRange(createdRange: [number, number] | null): [string, string] {
  if (!createdRange) {
    return ['', ''];
  }

  const [start, end] = createdRange;

  return [formatLocalDateTime(start), formatLocalDateTime(end)];
}

function normalizeLocalDateTimeQueryValue(value: unknown) {
  if (typeof value !== 'string') {
    return '';
  }

  const canonicalValue = normalizeCanonicalLocalDateTime(value);

  if (canonicalValue) {
    return canonicalValue;
  }

  const trimmed = value.trim();
  const match = ISO_DATE_TIME_RE.exec(trimmed);

  if (!match) {
    return '';
  }

  const [, yearText, monthText, dayText, hourText, minuteText, secondText] = match;
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const hour = Number(hourText);
  const minute = Number(minuteText);
  const second = Number(secondText);

  if (!hasValidLocalDateTimeParts(year, month, day, hour, minute, second)) {
    return '';
  }

  const timestamp = new Date(trimmed).getTime();

  return Number.isNaN(timestamp) ? '' : formatLocalDateTime(timestamp);
}

export function normalizeDeployTaskRouteQuery(query: Record<string, unknown>): DeployTaskRouteQuery {
  return {
    status: normalizeString(query.status),
    taskType: normalizeString(query.taskType),
    appId: normalizeNullableNumber(query.appId),
    environment: normalizeDeployEnvironment(query.environment),
    keyword: normalizeString(query.keyword),
    createdFrom: normalizeLocalDateTimeQueryValue(query.createdFrom),
    createdTo: normalizeLocalDateTimeQueryValue(query.createdTo),
    page: normalizePositiveInt(query.page, DEFAULT_PAGE),
    pageSize: normalizePositiveInt(query.pageSize, DEFAULT_PAGE_SIZE),
    sortBy: normalizeSortBy(query.sortBy),
    sortOrder: normalizeSortOrder(query.sortOrder),
    taskId: normalizeNullableNumber(query.taskId)
  };
}

export function normalizeTaskCenterRouteQuery(query: Record<string, unknown>): TaskCenterRouteQuery {
  return {
    keyword: normalizeString(query.keyword),
    status: normalizeString(query.status),
    sourceType: normalizeTaskCenterSourceType(query.sourceType),
    taskType: normalizeString(query.taskType),
    priority: normalizeTaskCenterPriority(query.priority),
    page: normalizePositiveInt(query.page, DEFAULT_PAGE),
    pageSize: normalizePositiveInt(query.pageSize, DEFAULT_PAGE_SIZE),
    sortBy: normalizeSortBy(query.sortBy),
    sortOrder: normalizeSortOrder(query.sortOrder)
  };
}

export function toDeployTaskApiQuery(query: DeployTaskRouteQuery): Api.Task.DeployTaskListQuery {
  return {
    ...(query.keyword ? { keyword: query.keyword } : {}),
    ...(query.status ? { status: query.status } : {}),
    ...(query.taskType ? { taskType: query.taskType } : {}),
    ...(query.appId !== null ? { appId: query.appId } : {}),
    ...(query.environment ? { environment: query.environment } : {}),
    ...(query.createdFrom ? { createdFrom: query.createdFrom } : {}),
    ...(query.createdTo ? { createdTo: query.createdTo } : {}),
    page: query.page,
    pageSize: query.pageSize,
    sortBy: query.sortBy,
    sortOrder: query.sortOrder
  };
}

export function toTaskCenterApiQuery(query: TaskCenterRouteQuery): Api.Task.TaskCenterListQuery {
  return {
    ...(query.keyword ? { keyword: query.keyword } : {}),
    ...(query.status ? { status: query.status } : {}),
    sourceType: 'DEPLOY',
    ...(query.taskType ? { taskType: query.taskType } : {}),
    ...(query.priority ? { priority: query.priority } : {}),
    page: query.page,
    pageSize: query.pageSize,
    sortBy: query.sortBy,
    sortOrder: query.sortOrder
  };
}

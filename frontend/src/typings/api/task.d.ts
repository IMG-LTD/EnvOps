declare namespace Api {
  /**
   * namespace Task
   *
   * backend api module: "task"
   */
  namespace Task {
    type TaskSortBy = 'createdAt' | 'updatedAt' | 'taskNo' | 'status';

    type TaskSortOrder = 'asc' | 'desc';

    type DeployTaskCreateTaskType = 'INSTALL' | 'UPGRADE';

    type DeployTaskBatchStrategy = 'ALL' | 'ROLLING';

    interface CreateDeployTaskPayload {
      taskName: string;
      taskType: DeployTaskCreateTaskType;
      appId: number;
      versionId: number;
      environment: string;
      hostIds: number[];
      batchStrategy: DeployTaskBatchStrategy;
      batchSize?: number | null;
      sshUser: string;
      sshPort?: number | null;
      privateKeyPath: string;
      remoteBaseDir: string;
      rollbackCommand?: string | null;
    }

    interface DeployTaskApprovalPayload {
      comment?: string | null;
    }

    interface DeployTaskListQuery {
      keyword?: string;
      status?: string;
      taskType?: string;
      appId?: number;
      environment?: string;
      createdFrom?: string;
      createdTo?: string;
      page: number;
      pageSize: number;
      sortBy: Api.Task.TaskSortBy;
      sortOrder: Api.Task.TaskSortOrder;
    }

    interface DeployTaskPage {
      page: number;
      pageSize: number;
      total: number;
      records: DeployTaskRecord[];
    }

    interface DeployTaskRecord {
      id: number;
      taskNo: string;
      taskName: string;
      taskType: string;
      appId: number;
      appName?: string | null;
      versionId: number;
      versionNo?: string | null;
      originTaskId?: number | null;
      status: string;
      batchStrategy?: string | null;
      batchSize?: number | null;
      targetCount?: number | null;
      successCount?: number | null;
      failCount?: number | null;
      operatorName?: string | null;
      approvalOperatorName?: string | null;
      approvalComment?: string | null;
      approvalAt?: string | null;
      startedAt?: string | null;
      finishedAt?: string | null;
      createdAt?: string | null;
      updatedAt?: string | null;
      params?: Record<string, string> | null;
    }

    interface DeployTaskDetailRecord extends DeployTaskRecord {
      totalHosts: number;
      pendingHosts: number;
      runningHosts: number;
      successHosts: number;
      failedHosts: number;
      cancelledHosts: number;
    }

    interface DeployTaskHostQuery {
      status?: string;
      keyword?: string;
      page?: number;
      pageSize?: number;
    }

    interface DeployTaskHostPage {
      page: number;
      pageSize: number;
      total: number;
      records: DeployTaskHostRecord[];
    }

    interface DeployTaskHostRecord {
      id: number;
      taskId: number;
      hostId: number;
      hostName?: string | null;
      ipAddress?: string | null;
      status?: string | null;
      currentStep?: string | null;
      startedAt?: string | null;
      finishedAt?: string | null;
      errorMsg?: string | null;
    }

    interface DeployTaskLogQuery {
      hostId?: number;
      keyword?: string;
      page?: number;
      pageSize?: number;
    }

    interface DeployTaskLogPage {
      page: number;
      pageSize: number;
      total: number;
      records: DeployTaskLogRecord[];
    }

    interface DeployTaskLogRecord {
      id: number;
      taskId: number;
      taskHostId?: number | null;
      logLevel?: string | null;
      logContent?: string | null;
      createdAt?: string | null;
    }

    type TaskCenterTaskType = 'deploy' | 'database_connectivity' | 'traffic_action';

    type UnifiedTaskStatus = 'pending' | 'running' | 'success' | 'failed';

    interface TaskCenterListQuery {
      keyword?: string;
      taskType?: TaskCenterTaskType;
      status?: UnifiedTaskStatus;
      startedFrom?: string;
      startedTo?: string;
      page: number;
      pageSize: number;
    }

    interface TaskCenterPage {
      page: number;
      pageSize: number;
      total: number;
      records: TaskCenterRecord[];
    }

    interface TaskCenterRecord {
      id: number;
      taskType: TaskCenterTaskType;
      taskName: string;
      status: UnifiedTaskStatus;
      triggeredBy: string;
      startedAt: string;
      finishedAt?: string | null;
      summary: string;
      sourceRoute: string;
      errorSummary?: string | null;
    }

    interface TaskCenterTaskDetail extends TaskCenterRecord {
      detailPreview: Record<string, unknown>;
    }

    interface TaskCenterTrackingBasicInfo {
      taskType: TaskCenterTaskType;
      taskName: string;
      status: UnifiedTaskStatus;
      triggeredBy: string;
      startedAt: string;
      finishedAt?: string | null;
    }

    interface TaskCenterTimelineItem {
      label: string;
      status: string;
      occurredAt?: string | null;
      description?: string | null;
    }

    interface TaskCenterSourceLink {
      type: 'log' | 'detail' | 'module' | string;
      label: string;
      route: string;
    }

    interface TaskCenterTrackingDetail {
      id: number;
      basicInfo: TaskCenterTrackingBasicInfo;
      timeline: TaskCenterTimelineItem[];
      logSummary: string;
      logRoute?: string | null;
      detailPreview: Record<string, unknown>;
      sourceLinks: TaskCenterSourceLink[];
      degraded: boolean;
    }

    type TaskCenterDetail = TaskCenterTaskDetail;
  }
}

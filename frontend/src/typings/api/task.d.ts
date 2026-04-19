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
      deployDir: string;
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
      sortBy: TaskSortBy;
      sortOrder: TaskSortOrder;
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

    interface TaskCenterListQuery {
      keyword?: string;
      status?: string;
      sourceType?: string;
      taskType?: string;
      priority?: string;
      page: number;
      pageSize: number;
      sortBy: TaskSortBy;
      sortOrder: TaskSortOrder;
    }

    interface TaskCenterPage {
      page: number;
      pageSize: number;
      total: number;
      records: TaskCenterRecord[];
    }

    interface TaskCenterRecord {
      id: number;
      sourceType: string;
      taskNo: string;
      taskName: string;
      taskType: string;
      status: string;
      appId?: number | null;
      appName?: string | null;
      versionId?: number | null;
      versionNo?: string | null;
      priority?: string | null;
      targetCount?: number | null;
      successCount?: number | null;
      failCount?: number | null;
      operatorName?: string | null;
      approvalOperatorName?: string | null;
      approvalComment?: string | null;
      approvalAt?: string | null;
      createdAt?: string | null;
      updatedAt?: string | null;
    }
  }
}

declare namespace Api {
  /**
   * namespace Task
   *
   * backend api module: "task"
   */
  namespace Task {
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

    interface DeployTaskLogRecord {
      id: number;
      taskId: number;
      taskHostId?: number | null;
      logLevel?: string | null;
      logContent?: string | null;
      createdAt?: string | null;
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

declare namespace Api {
  /**
   * namespace Monitor
   *
   * backend api module: "monitor"
   */
  namespace Monitor {
    interface DetectTaskRecord {
      id: number;
      taskName: string;
      hostId: number;
      target?: string | null;
      schedule?: string | null;
      lastRunAt?: string | null;
      lastResult?: string | null;
      createdAt?: string | null;
      [key: string]: unknown;
    }

    interface CreateDetectTaskParams {
      taskName: string;
      hostId: number;
      schedule?: string;
    }

    type DetectTaskListResponse = DetectTaskRecord[];

    interface HostFactRecord {
      id: number;
      hostId: number;
      hostName?: string | null;
      osName?: string | null;
      kernelVersion?: string | null;
      cpuCores?: number | null;
      memoryMb?: number | null;
      agentVersion?: string | null;
      collectedAt?: string | null;
      [key: string]: unknown;
    }
  }
}

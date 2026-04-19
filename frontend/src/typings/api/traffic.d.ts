declare namespace Api {
  /**
   * namespace Traffic
   *
   * backend api module: "traffic"
   */
  namespace Traffic {
    type TrafficPolicyActionType = 'preview' | 'apply' | 'rollback';

    interface TrafficPolicyRecord {
      id: number;
      app: string;
      strategy: string;
      scope: string;
      trafficRatio: string;
      owner: string;
      status: string;
      pluginType: string;
      rollbackToken?: string | null;
      [key: string]: unknown;
    }

    type TrafficPolicyListResponse = TrafficPolicyRecord[];

    interface TrafficPluginRecord {
      type: string;
      name: string;
      status: string;
      supportsPreview: boolean;
      supportsApply: boolean;
      supportsRollback: boolean;
      [key: string]: unknown;
    }

    type TrafficPluginListResponse = TrafficPluginRecord[];

    interface TrafficPluginResult {
      pluginType: string;
      status: string;
      action: string;
      message: string;
      app: string;
      strategy?: string | null;
      scope?: string | null;
      trafficRatio?: string | null;
      owner?: string | null;
      rollbackToken?: string | null;
      reason?: string | null;
      [key: string]: unknown;
    }

    interface TrafficPolicyActionRecord {
      action: TrafficPolicyActionType;
      policy: TrafficPolicyRecord;
      pluginResult: TrafficPluginResult;
      [key: string]: unknown;
    }
  }
}

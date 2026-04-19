declare namespace Api {
  /**
   * namespace Asset
   *
   * backend api module: "asset"
   */
  namespace Asset {
    interface HostQuery extends Common.CommonSearchParams {}

    interface HostRecord {
      id: number;
      hostName: string;
      ipAddress: string;
      environment: 'production' | 'staging' | 'sandbox' | string;
      clusterName: string;
      ownerName: string;
      status: 'online' | 'warning' | 'offline' | string;
      lastHeartbeat: string;
      hasMonitorFacts?: boolean;
      latestMonitorFactAt?: string | null;
    }

    interface HostSummary {
      managedHosts: number;
      onlineHosts: number;
      warningHosts: number;
    }

    interface HostPage extends Common.PaginatingQueryRecord<HostRecord> {
      summary: HostSummary;
    }

    interface CreateHostParams {
      hostName: string;
      ipAddress: string;
      environment: string;
      clusterName: string;
      ownerName: string;
      status: string;
      lastHeartbeat?: string;
    }

    interface CredentialRecord {
      id: number;
      name: string;
      credentialType: 'ssh_password' | 'ssh_key' | 'api_token' | string;
      username: string | null;
      description: string | null;
      createdAt: string;
    }

    interface CreateCredentialParams {
      name: string;
      credentialType: string;
      username?: string;
      secret?: string;
      description?: string;
    }

    interface GroupRecord {
      id: number;
      name: string;
      description: string | null;
      hostCount: number;
    }

    interface TagRecord {
      id: number;
      name: string;
      color: string | null;
      description: string | null;
    }
  }
}

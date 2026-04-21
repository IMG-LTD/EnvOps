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

    interface DatabaseQuery extends Common.CommonSearchParams {
      keyword?: string;
      environment?: string | null;
      databaseType?: string | null;
      lifecycleStatus?: string | null;
      connectivityStatus?: string | null;
    }

    interface DatabaseRecord {
      id: number;
      databaseName: string;
      databaseType: 'mysql' | 'postgresql' | 'oracle' | 'sqlserver' | 'mongodb' | 'redis' | string;
      environment: 'production' | 'staging' | 'sandbox' | string;
      hostId: number;
      hostName: string | null;
      port: number;
      instanceName: string | null;
      credentialId: number | null;
      credentialName: string | null;
      ownerName: string;
      lifecycleStatus: 'managed' | 'disabled' | string;
      connectivityStatus: 'unknown' | 'online' | 'warning' | 'offline' | string;
      connectionUsername: string | null;
      description: string | null;
      lastCheckedAt: string | null;
      createdAt: string;
      updatedAt: string;
    }

    interface DatabaseSummary {
      managedDatabases: number;
      warningDatabases: number;
      onlineDatabases: number;
    }

    interface DatabasePage extends Common.PaginatingQueryRecord<DatabaseRecord> {
      summary: DatabaseSummary;
    }

    interface CreateDatabaseParams {
      databaseName: string;
      databaseType: string;
      environment: string;
      hostId: number | null;
      port: number | null;
      instanceName?: string;
      credentialId?: number | null;
      ownerName: string;
      lifecycleStatus: string;
      connectivityStatus: string;
      connectionUsername?: string;
      connectionPassword?: string;
      description?: string;
      lastCheckedAt?: string;
    }

    interface UpdateDatabaseParams extends CreateDatabaseParams {}

    interface DatabaseConnectivityCheckSummary {
      total: number;
      success: number;
      failed: number;
      skipped: number;
    }

    interface DatabaseConnectivityCheckItem {
      databaseId: number;
      databaseName: string;
      databaseType: string;
      environment: string;
      status: 'success' | 'failed' | 'skipped';
      message: string;
      connectivityStatus: 'unknown' | 'online' | 'warning' | 'offline' | string;
      checkedAt: string | null;
    }

    interface DatabaseConnectivityCheckResponse {
      summary: DatabaseConnectivityCheckSummary;
      results: DatabaseConnectivityCheckItem[];
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

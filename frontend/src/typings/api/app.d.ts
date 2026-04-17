declare namespace Api {
  /**
   * namespace App
   *
   * backend api module: "app"
   */
  namespace App {
    type RecordId = string | number;

    type ResourceStatus = 0 | 1 | '0' | '1' | null;

    type AppType = 'JAVA' | 'NGINX' | 'SCRIPT' | 'DOCKER' | string;

    type DeployMode = 'SYSTEMD' | 'PROCESS' | 'DOCKER' | string | null;

    type RuntimeType = string | null;

    type PackageType = 'JAR' | 'TAR' | 'RPM' | 'SH' | string;

    type StorageType = 'LOCAL' | null;

    type RenderEngine = 'PLAINTEXT' | 'JINJA2' | 'FREEMARKER' | string | null;

    type ScriptType = 'BASH' | 'PYTHON' | string;

    interface AuditFields {
      createBy?: string | null;
      createTime?: string | null;
      updateBy?: string | null;
      updateTime?: string | null;
      status?: ResourceStatus;
    }

    interface AppDefinition extends AuditFields {
      id: RecordId;
      appCode: string;
      appName: string;
      appType: AppType;
      runtimeType?: RuntimeType;
      deployMode?: DeployMode;
      defaultPort?: number | null;
      healthCheckPath?: string | null;
      description?: string | null;
    }

    interface AppVersion extends AuditFields {
      id: RecordId;
      appId: RecordId;
      versionNo: string;
      packageId?: RecordId | null;
      configTemplateId?: RecordId | null;
      scriptTemplateId?: RecordId | null;
      changelog?: string | null;
    }

    interface AppPackage extends AuditFields {
      id: RecordId;
      packageName: string;
      packageType: PackageType;
      filePath: string;
      fileSize?: number | null;
      fileHash?: string | null;
      storageType?: StorageType;
    }

    interface ConfigTemplate extends AuditFields {
      id: RecordId;
      templateCode: string;
      templateName: string;
      templateContent: string;
      renderEngine?: RenderEngine;
    }

    interface ScriptTemplate extends AuditFields {
      id: RecordId;
      templateCode: string;
      templateName: string;
      scriptType: ScriptType;
      scriptContent: string;
    }

    interface CreateAppPayload {
      appCode: string;
      appName: string;
      appType: AppType;
      runtimeType?: RuntimeType;
      deployMode?: DeployMode;
      defaultPort?: number | null;
      healthCheckPath?: string | null;
      description?: string | null;
      status?: ResourceStatus;
    }

    type UpdateAppPayload = CreateAppPayload;

    interface CreateAppVersionPayload {
      versionNo: string;
      packageId?: RecordId | null;
      configTemplateId?: RecordId | null;
      scriptTemplateId?: RecordId | null;
      changelog?: string | null;
      status?: ResourceStatus;
    }

    type UpdateAppVersionPayload = CreateAppVersionPayload;

    interface PackageUploadPayload {
      file: File;
      packageName?: string | null;
      packageType: PackageType;
      storageType?: StorageType;
    }

    type PackageUploadRequestBody = FormData;

    interface CreateConfigTemplatePayload {
      templateCode: string;
      templateName: string;
      templateContent: string;
      renderEngine?: RenderEngine;
      status?: ResourceStatus;
    }

    type UpdateConfigTemplatePayload = CreateConfigTemplatePayload;

    interface CreateScriptTemplatePayload {
      templateCode: string;
      templateName: string;
      scriptType: ScriptType;
      scriptContent: string;
      status?: ResourceStatus;
    }

    type UpdateScriptTemplatePayload = CreateScriptTemplatePayload;
  }
}

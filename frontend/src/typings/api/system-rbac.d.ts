declare namespace Api {
  namespace SystemRbac {
    type PermissionType = 'menu' | 'action';

    interface RoleRecord {
      id: number;
      roleKey: string;
      roleName: string;
      description: string | null;
      enabled: boolean;
      builtIn: boolean;
      createdAt: string | null;
      updatedAt: string | null;
      [key: string]: unknown;
    }

    interface CreateRoleParams {
      roleKey: string;
      roleName: string;
      description?: string | null;
      enabled: boolean;
    }

    interface UpdateRoleParams {
      roleName: string;
      description?: string | null;
      enabled: boolean;
    }

    interface PermissionNode {
      id: number;
      permissionKey: string;
      permissionName: string;
      permissionType: PermissionType;
      moduleKey: string;
      parentKey: string | null;
      routeName: string | null;
      actionKey: string | null;
      sortOrder: number;
      enabled: boolean;
      children?: PermissionNode[];
      [key: string]: unknown;
    }

    interface PermissionModule {
      moduleKey: string;
      moduleName: string;
      permissions: PermissionNode[];
      [key: string]: unknown;
    }

    interface RolePermissionsResponse {
      roleId: number;
      roleKey: string;
      permissionKeys: string[];
      [key: string]: unknown;
    }

    interface UpdateRolePermissionsParams {
      permissionKeys: string[];
    }

    type PermissionTreeResponse = PermissionModule[];
    type RoleListResponse = RoleRecord[];
  }
}

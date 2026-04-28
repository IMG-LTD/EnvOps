declare namespace Api {
  namespace SystemUser {
    interface SystemUserRecord {
      id: number;
      userName: string;
      phone: string;
      teamKey: string;
      loginType: string;
      status: string;
      lastLoginAt: string | null;
      roles: string[];
      [key: string]: unknown;
    }

    interface CreateSystemUserParams {
      userName: string;
      password: string;
      phone: string;
      teamKey: string;
      loginType: string;
      status: string;
      roles: string[];
    }

    interface UpdateSystemUserParams {
      userName: string;
      password?: string | null;
      phone: string;
      teamKey: string;
      loginType: string;
      status: string;
      roles: string[];
    }

    interface UserRoleRecord {
      id: number;
      roleKey: string;
      roleName: string;
      enabled: boolean;
      builtIn: boolean;
      [key: string]: unknown;
    }

    interface UserRoleAssignmentResponse {
      userId: number;
      roles: UserRoleRecord[];
      roleIds: number[];
      roleKeys: string[];
      [key: string]: unknown;
    }

    interface UpdateSystemUserRolesParams {
      roleIds: number[];
    }

    type SystemUserListResponse = SystemUserRecord[];
  }
}

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

    type SystemUserListResponse = SystemUserRecord[];
  }
}

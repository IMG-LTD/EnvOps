declare namespace Api {
  /**
   * namespace Auth
   *
   * backend api module: "auth"
   */
  namespace Auth {
    interface LoginToken {
      token: string;
    }

    interface SendCodeResult {
      maskedPhone: string;
      expireSeconds: number;
    }

    interface UserInfo {
      userId: string;
      userName: string;
      roles: string[];
      buttons: string[];
    }
  }
}

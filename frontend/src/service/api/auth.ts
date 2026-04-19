import { request } from '../request';

/**
 * Login
 *
 * @param userName User name
 * @param password Password
 */
export function fetchLogin(userName: string, password: string) {
  return request<Api.Auth.LoginToken>({
    url: '/api/auth/login',
    method: 'post',
    data: {
      userName,
      password
    }
  });
}

/** send verification code */
export function fetchSendLoginCode(phone: string) {
  return request<Api.Auth.SendCodeResult>({
    url: '/api/auth/sendCode',
    method: 'post',
    data: {
      phone
    }
  });
}

/** login by verification code */
export function fetchCodeLogin(phone: string, code: string) {
  return request<Api.Auth.LoginToken>({
    url: '/api/auth/codeLogin',
    method: 'post',
    data: {
      phone,
      code
    }
  });
}

/** Get user info */
export function fetchGetUserInfo() {
  return request<Api.Auth.UserInfo>({ url: '/api/auth/getUserInfo' });
}

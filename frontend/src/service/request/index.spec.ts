import { beforeEach, describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => {
  const flatRequest = { state: {} as Record<string, unknown> };
  const createFlatRequest = vi.fn((_config?: unknown, options?: Record<string, any>) => {
    flatRequest.state = {
      ...(options?.defaultState ?? {})
    };

    return flatRequest as any;
  });
  const createRequest = vi.fn(() => ({ state: {} } as any));
  const handleExpiredRequest = vi.fn();
  const getAuthorization = vi.fn(() => 'Bearer refreshed-token');
  const showErrorMsg = vi.fn();
  const authStore = {
    resetStore: vi.fn()
  };

  return {
    flatRequest,
    createFlatRequest,
    createRequest,
    handleExpiredRequest,
    getAuthorization,
    showErrorMsg,
    authStore
  };
});

vi.mock('@sa/axios', () => ({
  BACKEND_ERROR_CODE: 'BACKEND_ERROR_CODE',
  createFlatRequest: mocks.createFlatRequest,
  createRequest: mocks.createRequest
}));

vi.mock('@/service/request/shared', () => ({
  getAuthorization: mocks.getAuthorization,
  handleExpiredRequest: mocks.handleExpiredRequest,
  showErrorMsg: mocks.showErrorMsg
}));

vi.mock('@/utils/service', () => ({
  getServiceBaseURL: () => ({
    baseURL: '',
    otherBaseURL: {
      demo: ''
    }
  })
}));

vi.mock('@/utils/storage', () => ({
  localStg: {
    get: vi.fn(() => null)
  }
}));

vi.mock('@/locales', () => ({
  $t: (key: string) => key
}));

vi.mock('@/store/modules/auth', () => ({
  useAuthStore: () => mocks.authStore
}));

describe('service request expired-token flow', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    vi.stubEnv('VITE_SERVICE_SUCCESS_CODE', '0000');
    vi.stubEnv('VITE_SERVICE_LOGOUT_CODES', '8888,8889');
    vi.stubEnv('VITE_SERVICE_MODAL_LOGOUT_CODES', '7777,7778');
    vi.stubEnv('VITE_SERVICE_EXPIRED_TOKEN_CODES', '9999,9998,3333');
    mocks.handleExpiredRequest.mockResolvedValue(true);
    mocks.flatRequest.state = {};
  });

  it('does not replay expired-token requests after logout cleanup', async () => {
    await import('@/service/request/index');

    const [, options] = mocks.createFlatRequest.mock.calls[0] as [unknown, Record<string, any>];
    const instance = {
      request: vi.fn()
    };
    const response = {
      data: {
        code: '9999',
        msg: 'token expired'
      },
      config: {
        headers: {}
      }
    };

    const result = await options.onBackendFail(response as any, instance as any);

    expect(mocks.handleExpiredRequest).toHaveBeenCalledOnce();
    expect(instance.request).not.toHaveBeenCalled();
    expect(result).toBeNull();
  });

  it('logs out on http 401 responses from the backend auth contract', async () => {
    await import('@/service/request/index');

    const [, options] = mocks.createFlatRequest.mock.calls[0] as [unknown, Record<string, any>];
    const error = {
      message: 'Request failed with status code 401',
      config: {
        url: '/api/auth/getUserInfo'
      },
      response: {
        status: 401,
        data: {
          code: '401',
          msg: 'Unauthorized'
        }
      }
    };

    await options.onError(error as any);

    expect(mocks.handleExpiredRequest).toHaveBeenCalledOnce();
    expect(mocks.showErrorMsg).not.toHaveBeenCalled();
  });

  it('does not treat login 401 responses as expired-session logout flow', async () => {
    await import('@/service/request/index');

    const [, options] = mocks.createFlatRequest.mock.calls[0] as [unknown, Record<string, any>];
    const error = {
      message: 'Request failed with status code 401',
      config: {
        url: '/api/auth/login'
      },
      response: {
        status: 401,
        data: {
          code: '401',
          msg: 'Invalid username or password'
        }
      }
    };

    await options.onError(error as any);

    expect(mocks.handleExpiredRequest).not.toHaveBeenCalled();
    expect(mocks.showErrorMsg).toHaveBeenCalledOnce();
    expect(mocks.showErrorMsg).toHaveBeenCalledWith(mocks.flatRequest.state, 'Invalid username or password');
  });
});

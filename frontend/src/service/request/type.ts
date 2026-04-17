export interface RequestInstanceState {
  /** the promise of handling expired-token logout flow */
  expiredTokenActionPromise: Promise<boolean> | null;
  /** the request error message stack */
  errMsgStack: string[];
  [key: string]: unknown;
}

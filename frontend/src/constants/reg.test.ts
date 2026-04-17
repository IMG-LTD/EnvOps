import { describe, expect, it } from 'vitest';
import { REG_PWD } from './reg';

describe('REG_PWD', () => {
  it('accepts passwords containing @ for EnvOps login', () => {
    expect(REG_PWD.test('EnvOps@123')).toBe(true);
  });
});

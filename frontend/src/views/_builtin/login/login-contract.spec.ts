import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const codeLoginSource = readFileSync(path.resolve(__dirname, 'modules/code-login.vue'), 'utf8');
const captchaSource = readFileSync(path.resolve(__dirname, '../../../hooks/business/captcha.ts'), 'utf8');
const authApiSource = readFileSync(path.resolve(__dirname, '../../../service/api/auth.ts'), 'utf8');
const authTypingSource = readFileSync(path.resolve(__dirname, '../../../typings/api/auth.d.ts'), 'utf8');
const authStoreSource = readFileSync(path.resolve(__dirname, '../../../store/modules/auth/index.ts'), 'utf8');

describe('code login contract wiring', () => {
  it('keeps auth api endpoints aligned with backend code login contracts', () => {
    expect(authApiSource).toMatch(/export function fetchSendLoginCode\s*\(/);
    expect(authApiSource).toMatch(/url:\s*['"]\/api\/auth\/sendCode['"]/);
    expect(authApiSource).toMatch(/export function fetchCodeLogin\s*\(/);
    expect(authApiSource).toMatch(/url:\s*['"]\/api\/auth\/codeLogin['"]/);

    expect(authTypingSource).toContain('interface SendCodeResult');
    expect(authTypingSource).toContain('maskedPhone: string;');
    expect(authTypingSource).toContain('expireSeconds: number;');
  });

  it('keeps code login page and captcha hook wired to real async requests', () => {
    expect(codeLoginSource).toContain('authStore.loginByCode');
    expect(codeLoginSource).toContain('getCaptcha(model.phone)');
    expect(codeLoginSource).not.toContain('// request');

    expect(captchaSource).toContain('fetchSendLoginCode');
    expect(captchaSource).toContain('page.login.codeLogin.demoCodeHint');
    expect(captchaSource).toContain('start();');
    expect(captchaSource).not.toContain('setTimeout(');

    expect(authStoreSource).toContain('fetchCodeLogin');
    expect(authStoreSource).toContain('async function loginByCode');
    expect(authStoreSource).toContain('await finalizeLogin(loginToken, redirect);');
  });
});

import { createI18n } from 'vue-i18n';
import { describe, expect, it } from 'vitest';
import messages from './locale';

function translateInvalidPassword(locale: 'zh-CN' | 'en-US') {
  const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'en-US',
    messages
  });

  return i18n.global.t('form.pwd.invalid');
}

describe('form.pwd.invalid locale messages', () => {
  it.each([
    ['zh-CN', '密码格式不正确'],
    ['en-US', '6-18 characters']
  ] as const)('translates %s invalid password copy with a literal @', (locale, expectedFragment) => {
    let translated = '';

    expect(() => {
      translated = translateInvalidPassword(locale);
    }).not.toThrow();

    expect(translated).toContain(expectedFragment);
    expect(translated).toContain('@');
  });
});

import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

const pwdLoginFile = path.resolve(process.cwd(), 'src/views/_builtin/login/modules/pwd-login.vue');
const headerBannerFile = path.resolve(process.cwd(), 'src/views/home/modules/header-banner.vue');
const projectNewsFile = path.resolve(process.cwd(), 'src/views/home/modules/project-news.vue');
const soybeanAvatarFile = path.resolve(process.cwd(), 'src/components/custom/soybean-avatar.vue');
const soybeanImageFile = path.resolve(process.cwd(), 'src/assets/imgs/soybean.jpg');
const componentsTypingFile = path.resolve(process.cwd(), 'src/typings/components.d.ts');
const systemLogoFile = path.resolve(process.cwd(), 'src/components/common/system-logo.vue');
const faviconFile = path.resolve(process.cwd(), 'public/favicon.svg');
const loadingPluginFile = path.resolve(process.cwd(), 'src/plugins/loading.ts');
const globalFooterFile = path.resolve(process.cwd(), 'src/layouts/modules/global-footer/index.vue');
const themeSettingsFile = path.resolve(process.cwd(), 'src/theme/settings.ts');
const watermarkSettingsFile = path.resolve(
  process.cwd(),
  'src/layouts/modules/theme-drawer/modules/general/modules/watermark-settings.vue'
);
const defaultThemePresetFile = path.resolve(process.cwd(), 'src/theme/preset/default.json');
const darkThemePresetFile = path.resolve(process.cwd(), 'src/theme/preset/dark.json');

describe('envops brand cleanup', () => {
  it('does not ship seeded admin credentials in the login screen', () => {
    const source = readFileSync(pwdLoginFile, 'utf8');

    expect(source).not.toContain('envops-admin');
    expect(source).not.toContain('EnvOps@123');
    expect(source).not.toContain('handleAccountLogin');
    expect(source).not.toContain('otherAccountLogin');
  });

  it('removes leftover soybean-only home shell visuals and avatar component', () => {
    const headerBannerSource = readFileSync(headerBannerFile, 'utf8');
    const projectNewsSource = readFileSync(projectNewsFile, 'utf8');

    expect(headerBannerSource).not.toContain('soybean.jpg');
    expect(projectNewsSource).not.toContain('SoybeanAvatar');
    expect(existsSync(soybeanAvatarFile)).toBe(false);
    expect(existsSync(soybeanImageFile)).toBe(false);
  });

  it('drops SoybeanAvatar from generated component typings', () => {
    const source = readFileSync(componentsTypingFile, 'utf8');

    expect(source).not.toContain("typeof import('./../components/custom/soybean-avatar.vue')['default']");
    expect(source).not.toContain('const SoybeanAvatar:');
  });

  it('keeps shared branding surfaces on EnvOps instead of template defaults', () => {
    const systemLogoSource = readFileSync(systemLogoFile, 'utf8');
    const faviconSource = readFileSync(faviconFile, 'utf8');
    const loadingPluginSource = readFileSync(loadingPluginFile, 'utf8');
    const footerSource = readFileSync(globalFooterFile, 'utf8');
    const themeSettingsSource = readFileSync(themeSettingsFile, 'utf8');
    const watermarkSettingsSource = readFileSync(watermarkSettingsFile, 'utf8');
    const defaultThemePresetSource = readFileSync(defaultThemePresetFile, 'utf8');
    const darkThemePresetSource = readFileSync(darkThemePresetFile, 'utf8');

    expect(systemLogoSource).toContain('local-icon="logo"');
    expect(systemLogoSource).not.toContain('viewBox="0 0 1000 1000"');
    expect(faviconSource).toContain('viewBox="0 0 160 160"');
    expect(faviconSource).not.toContain('LinearGradient');
    expect(loadingPluginSource).toContain('viewBox="0 0 160 160"');
    expect(loadingPluginSource).not.toContain('LinearGradient');
    expect(footerSource).toContain('EnvOps');
    expect(footerSource).not.toContain('Soybean');
    expect(themeSettingsSource).toContain("text: 'EnvOps'");
    expect(themeSettingsSource).not.toContain('SoybeanAdmin');
    expect(watermarkSettingsSource).toContain('placeholder="EnvOps"');
    expect(watermarkSettingsSource).not.toContain('SoybeanAdmin');
    expect(defaultThemePresetSource).toContain('"text": "EnvOps"');
    expect(defaultThemePresetSource).not.toContain('SoybeanAdmin');
    expect(darkThemePresetSource).toContain('"text": "EnvOps"');
    expect(darkThemePresetSource).not.toContain('SoybeanAdmin');
  });
});

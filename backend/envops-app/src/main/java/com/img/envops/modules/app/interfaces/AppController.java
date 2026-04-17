package com.img.envops.modules.app.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.app.application.AppApplicationService;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class AppController {
  private final AppApplicationService appApplicationService;

  public AppController(AppApplicationService appApplicationService) {
    this.appApplicationService = appApplicationService;
  }

  @GetMapping("/api/apps")
  public R<List<AppApplicationService.AppRecord>> getApps() {
    return R.ok(appApplicationService.getApps());
  }

  @GetMapping("/api/apps/{id}")
  public R<AppApplicationService.AppRecord> getApp(@PathVariable Long id) {
    return R.ok(appApplicationService.getApp(id));
  }

  @PostMapping("/api/apps")
  public R<AppApplicationService.AppRecord> createApp(@RequestBody AppRequest request) {
    return R.ok(appApplicationService.createApp(new AppApplicationService.CreateAppCommand(
        request.appCode(),
        request.appName(),
        request.appType(),
        request.runtimeType(),
        request.deployMode(),
        request.defaultPort(),
        request.healthCheckPath(),
        request.description(),
        request.status())));
  }

  @PutMapping("/api/apps/{id}")
  public R<AppApplicationService.AppRecord> updateApp(@PathVariable Long id, @RequestBody AppRequest request) {
    return R.ok(appApplicationService.updateApp(id, new AppApplicationService.UpdateAppCommand(
        request.appCode(),
        request.appName(),
        request.appType(),
        request.runtimeType(),
        request.deployMode(),
        request.defaultPort(),
        request.healthCheckPath(),
        request.description(),
        request.status())));
  }

  @DeleteMapping("/api/apps/{id}")
  public R<Boolean> deleteApp(@PathVariable Long id) {
    return R.ok(appApplicationService.deleteApp(id));
  }

  @GetMapping("/api/apps/{id}/versions")
  public R<List<AppApplicationService.AppVersionRecord>> getVersions(@PathVariable Long id) {
    return R.ok(appApplicationService.getVersions(id));
  }

  @PostMapping("/api/apps/{id}/versions")
  public R<AppApplicationService.AppVersionRecord> createVersion(@PathVariable Long id,
                                                                 @RequestBody AppVersionRequest request) {
    return R.ok(appApplicationService.createVersion(id, new AppApplicationService.CreateAppVersionCommand(
        request.versionNo(),
        request.packageId(),
        request.configTemplateId(),
        request.scriptTemplateId(),
        request.changelog(),
        request.status())));
  }

  @PutMapping("/api/app-versions/{id}")
  public R<AppApplicationService.AppVersionRecord> updateVersion(@PathVariable Long id,
                                                                 @RequestBody AppVersionRequest request) {
    return R.ok(appApplicationService.updateVersion(id, new AppApplicationService.UpdateAppVersionCommand(
        request.versionNo(),
        request.packageId(),
        request.configTemplateId(),
        request.scriptTemplateId(),
        request.changelog(),
        request.status())));
  }

  @DeleteMapping("/api/app-versions/{id}")
  public R<Boolean> deleteVersion(@PathVariable Long id) {
    return R.ok(appApplicationService.deleteVersion(id));
  }

  @GetMapping("/api/packages")
  public R<List<AppApplicationService.AppPackageRecord>> getPackages() {
    return R.ok(appApplicationService.getPackages());
  }

  @PostMapping(path = "/api/packages/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public R<AppApplicationService.AppPackageRecord> uploadPackage(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam(required = false) String packageName,
                                                                 @RequestParam(required = false) String packageType,
                                                                 @RequestParam(required = false) String storageType,
                                                                 @RequestParam(required = false) String fileHash) {
    String resolvedPackageName = resolvePackageName(file, packageName);
    String filePath = "packages/" + extractFileName(StringUtils.hasText(file.getOriginalFilename())
        ? file.getOriginalFilename()
        : resolvedPackageName);

    return R.ok(appApplicationService.uploadPackage(file, new AppApplicationService.UploadPackageCommand(
        resolvedPackageName,
        packageType,
        filePath,
        file.getSize(),
        fileHash,
        storageType)));
  }

  @DeleteMapping("/api/packages/{id}")
  public R<Boolean> deletePackage(@PathVariable Long id) {
    return R.ok(appApplicationService.deletePackage(id));
  }

  @GetMapping("/api/config-templates")
  public R<List<AppApplicationService.AppConfigTemplateRecord>> getConfigTemplates() {
    return R.ok(appApplicationService.getConfigTemplates());
  }

  @PostMapping("/api/config-templates")
  public R<AppApplicationService.AppConfigTemplateRecord> createConfigTemplate(@RequestBody ConfigTemplateRequest request) {
    return R.ok(appApplicationService.createConfigTemplate(new AppApplicationService.CreateConfigTemplateCommand(
        request.templateCode(),
        request.templateName(),
        request.templateContent(),
        request.renderEngine())));
  }

  @PutMapping("/api/config-templates/{id}")
  public R<AppApplicationService.AppConfigTemplateRecord> updateConfigTemplate(@PathVariable Long id,
                                                                               @RequestBody ConfigTemplateRequest request) {
    return R.ok(appApplicationService.updateConfigTemplate(id, new AppApplicationService.UpdateConfigTemplateCommand(
        request.templateCode(),
        request.templateName(),
        request.templateContent(),
        request.renderEngine())));
  }

  @DeleteMapping("/api/config-templates/{id}")
  public R<Boolean> deleteConfigTemplate(@PathVariable Long id) {
    return R.ok(appApplicationService.deleteConfigTemplate(id));
  }

  @GetMapping("/api/script-templates")
  public R<List<AppApplicationService.AppScriptTemplateRecord>> getScriptTemplates() {
    return R.ok(appApplicationService.getScriptTemplates());
  }

  @PostMapping("/api/script-templates")
  public R<AppApplicationService.AppScriptTemplateRecord> createScriptTemplate(@RequestBody ScriptTemplateRequest request) {
    return R.ok(appApplicationService.createScriptTemplate(new AppApplicationService.CreateScriptTemplateCommand(
        request.templateCode(),
        request.templateName(),
        request.scriptType(),
        request.scriptContent())));
  }

  @PutMapping("/api/script-templates/{id}")
  public R<AppApplicationService.AppScriptTemplateRecord> updateScriptTemplate(@PathVariable Long id,
                                                                               @RequestBody ScriptTemplateRequest request) {
    return R.ok(appApplicationService.updateScriptTemplate(id, new AppApplicationService.UpdateScriptTemplateCommand(
        request.templateCode(),
        request.templateName(),
        request.scriptType(),
        request.scriptContent())));
  }

  @DeleteMapping("/api/script-templates/{id}")
  public R<Boolean> deleteScriptTemplate(@PathVariable Long id) {
    return R.ok(appApplicationService.deleteScriptTemplate(id));
  }

  private String resolvePackageName(MultipartFile file, String packageName) {
    if (StringUtils.hasText(packageName)) {
      return packageName.trim();
    }

    return extractFileName(file.getOriginalFilename());
  }

  private String extractFileName(String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }

    String normalized = StringUtils.cleanPath(value.trim()).replace('\\', '/');
    int index = normalized.lastIndexOf('/');
    return index >= 0 ? normalized.substring(index + 1) : normalized;
  }

  public record AppRequest(String appCode,
                           String appName,
                           String appType,
                           String runtimeType,
                           String deployMode,
                           Integer defaultPort,
                           String healthCheckPath,
                           String description,
                           Integer status) {
  }

  public record AppVersionRequest(String versionNo,
                                  Long packageId,
                                  Long configTemplateId,
                                  Long scriptTemplateId,
                                  String changelog,
                                  Integer status) {
  }

  public record ConfigTemplateRequest(String templateCode,
                                      String templateName,
                                      String templateContent,
                                      String renderEngine) {
  }

  public record ScriptTemplateRequest(String templateCode,
                                      String templateName,
                                      String scriptType,
                                      String scriptContent) {
  }
}

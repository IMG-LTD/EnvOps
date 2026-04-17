package com.img.envops.modules.app.application;

import com.img.envops.common.exception.ConflictException;
import com.img.envops.modules.app.infrastructure.LocalPackageStorage;
import com.img.envops.modules.app.infrastructure.mapper.AppConfigTemplateMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppDefinitionMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppPackageMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppScriptTemplateMapper;
import com.img.envops.modules.app.infrastructure.mapper.AppVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class AppApplicationService {
  private static final String DEFAULT_OPERATOR = "envops-admin";
  private static final List<String> APP_TYPES = List.of("JAVA", "NGINX", "SCRIPT", "DOCKER");
  private static final List<String> DEPLOY_MODES = List.of("SYSTEMD", "PROCESS", "DOCKER");
  private static final List<String> PACKAGE_TYPES = List.of("JAR", "TAR", "RPM", "SH");
  private static final List<String> STORAGE_TYPES = List.of("LOCAL");
  private static final List<String> RENDER_ENGINES = List.of("PLAINTEXT", "JINJA2", "FREEMARKER");
  private static final List<String> SCRIPT_TYPES = List.of("BASH", "PYTHON");
  private static final List<Integer> VALID_STATUS = List.of(0, 1);

  private final AppDefinitionMapper appDefinitionMapper;
  private final AppVersionMapper appVersionMapper;
  private final AppPackageMapper appPackageMapper;
  private final AppConfigTemplateMapper appConfigTemplateMapper;
  private final AppScriptTemplateMapper appScriptTemplateMapper;
  private final LocalPackageStorage localPackageStorage;

  public AppApplicationService(AppDefinitionMapper appDefinitionMapper,
                               AppVersionMapper appVersionMapper,
                               AppPackageMapper appPackageMapper,
                               AppConfigTemplateMapper appConfigTemplateMapper,
                               AppScriptTemplateMapper appScriptTemplateMapper,
                               LocalPackageStorage localPackageStorage) {
    this.appDefinitionMapper = appDefinitionMapper;
    this.appVersionMapper = appVersionMapper;
    this.appPackageMapper = appPackageMapper;
    this.appConfigTemplateMapper = appConfigTemplateMapper;
    this.appScriptTemplateMapper = appScriptTemplateMapper;
    this.localPackageStorage = localPackageStorage;
  }

  public List<AppRecord> getApps() {
    return appDefinitionMapper.findAllActive().stream()
        .map(this::toAppRecord)
        .toList();
  }

  public AppRecord getApp(Long id) {
    return toAppRecord(requireApp(id));
  }

  public AppRecord createApp(CreateAppCommand command) {
    validateCreateOrUpdateAppCommand(command);

    LocalDateTime now = LocalDateTime.now();
    AppDefinitionMapper.AppDefinitionEntity entity = new AppDefinitionMapper.AppDefinitionEntity();
    entity.setAppCode(command.appCode().trim());
    entity.setAppName(command.appName().trim());
    entity.setAppType(normalizeRequiredValue(command.appType()));
    entity.setRuntimeType(trimToNull(command.runtimeType()));
    entity.setDeployMode(normalizeOptionalValue(command.deployMode()));
    entity.setDefaultPort(command.defaultPort());
    entity.setHealthCheckPath(trimToNull(command.healthCheckPath()));
    entity.setDescription(trimToNull(command.description()));
    entity.setStatus(normalizeStatus(command.status()));
    entity.setDeleted(0);
    entity.setCreatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    appDefinitionMapper.insertApp(entity);
    return getApp(entity.getId());
  }

  public AppRecord updateApp(Long id, UpdateAppCommand command) {
    requireApp(id);
    validateCreateOrUpdateAppCommand(command);

    AppDefinitionMapper.AppDefinitionEntity entity = new AppDefinitionMapper.AppDefinitionEntity();
    entity.setId(id);
    entity.setAppCode(command.appCode().trim());
    entity.setAppName(command.appName().trim());
    entity.setAppType(normalizeRequiredValue(command.appType()));
    entity.setRuntimeType(trimToNull(command.runtimeType()));
    entity.setDeployMode(normalizeOptionalValue(command.deployMode()));
    entity.setDefaultPort(command.defaultPort());
    entity.setHealthCheckPath(trimToNull(command.healthCheckPath()));
    entity.setDescription(trimToNull(command.description()));
    entity.setStatus(normalizeStatus(command.status()));
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedAt(LocalDateTime.now());

    appDefinitionMapper.updateApp(entity);
    return getApp(id);
  }

  public boolean deleteApp(Long id) {
    requireApp(id);
    if (appVersionMapper.countActiveByAppId(id) > 0) {
      throw new ConflictException("app is referenced by active versions");
    }
    appDefinitionMapper.markDeleted(id, DEFAULT_OPERATOR, LocalDateTime.now());
    return true;
  }

  public List<AppVersionRecord> getVersions(Long appId) {
    requireApp(appId);
    return appVersionMapper.findActiveByAppId(appId).stream()
        .map(this::toAppVersionRecord)
        .toList();
  }

  public AppVersionRecord createVersion(Long appId, CreateAppVersionCommand command) {
    requireApp(appId);
    validateCreateOrUpdateVersionCommand(command);
    requirePackageIfPresent(command.packageId());
    requireConfigTemplateIfPresent(command.configTemplateId());
    requireScriptTemplateIfPresent(command.scriptTemplateId());

    LocalDateTime now = LocalDateTime.now();
    AppVersionMapper.AppVersionEntity entity = new AppVersionMapper.AppVersionEntity();
    entity.setAppId(appId);
    entity.setVersionNo(command.versionNo().trim());
    entity.setPackageId(command.packageId());
    entity.setConfigTemplateId(command.configTemplateId());
    entity.setScriptTemplateId(command.scriptTemplateId());
    entity.setChangelog(trimToNull(command.changelog()));
    entity.setStatus(normalizeStatus(command.status()));
    entity.setDeleted(0);
    entity.setCreatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    appVersionMapper.insertVersion(entity);
    return getVersion(entity.getId());
  }

  public AppVersionRecord updateVersion(Long id, UpdateAppVersionCommand command) {
    AppVersionMapper.AppVersionRow existing = requireVersion(id);
    validateCreateOrUpdateVersionCommand(command);
    requirePackageIfPresent(command.packageId());
    requireConfigTemplateIfPresent(command.configTemplateId());
    requireScriptTemplateIfPresent(command.scriptTemplateId());

    AppVersionMapper.AppVersionEntity entity = new AppVersionMapper.AppVersionEntity();
    entity.setId(id);
    entity.setAppId(existing.getAppId());
    entity.setVersionNo(command.versionNo().trim());
    entity.setPackageId(command.packageId());
    entity.setConfigTemplateId(command.configTemplateId());
    entity.setScriptTemplateId(command.scriptTemplateId());
    entity.setChangelog(trimToNull(command.changelog()));
    entity.setStatus(normalizeStatus(command.status()));
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedAt(LocalDateTime.now());

    appVersionMapper.updateVersion(entity);
    return getVersion(id);
  }

  public boolean deleteVersion(Long id) {
    requireVersion(id);
    appVersionMapper.markDeleted(id, DEFAULT_OPERATOR, LocalDateTime.now());
    return true;
  }

  public List<AppPackageRecord> getPackages() {
    return appPackageMapper.findAllActive().stream()
        .map(this::toAppPackageRecord)
        .toList();
  }

  public AppPackageRecord uploadPackage(MultipartFile file, UploadPackageCommand command) {
    validateUploadPackageCommand(file, command);

    byte[] content = readFileContent(file);
    String serverHash = sha256(content);
    if (StringUtils.hasText(command.fileHash()) && !serverHash.equals(command.fileHash().trim())) {
      throw new IllegalArgumentException("fileHash does not match uploaded file content");
    }

    String relativePath = localPackageStorage.store(command.filePath().trim(), file);

    LocalDateTime now = LocalDateTime.now();
    AppPackageMapper.AppPackageEntity entity = new AppPackageMapper.AppPackageEntity();
    entity.setPackageName(command.packageName().trim());
    entity.setPackageType(normalizeRequiredValue(command.packageType()));
    entity.setFilePath(relativePath);
    entity.setFileSize(command.fileSize());
    entity.setFileHash(serverHash);
    entity.setStorageType(normalizeOptionalValue(command.storageType(), STORAGE_TYPES, "storageType", "LOCAL"));
    entity.setDeleted(0);
    entity.setCreatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    appPackageMapper.insertPackage(entity);
    return getPackage(entity.getId());
  }

  public boolean deletePackage(Long id) {
    requirePackage(id);
    if (appVersionMapper.countActiveByPackageId(id) > 0) {
      throw new ConflictException("package is referenced by active app versions");
    }
    appPackageMapper.markDeleted(id, DEFAULT_OPERATOR, LocalDateTime.now());
    return true;
  }

  public List<AppConfigTemplateRecord> getConfigTemplates() {
    return appConfigTemplateMapper.findAllActive().stream()
        .map(this::toAppConfigTemplateRecord)
        .toList();
  }

  public AppConfigTemplateRecord createConfigTemplate(CreateConfigTemplateCommand command) {
    validateCreateOrUpdateConfigTemplateCommand(command);

    LocalDateTime now = LocalDateTime.now();
    AppConfigTemplateMapper.AppConfigTemplateEntity entity = new AppConfigTemplateMapper.AppConfigTemplateEntity();
    entity.setTemplateCode(command.templateCode().trim());
    entity.setTemplateName(command.templateName().trim());
    entity.setTemplateContent(command.templateContent());
    entity.setRenderEngine(normalizeOptionalValue(command.renderEngine(), RENDER_ENGINES, "renderEngine", "PLAINTEXT"));
    entity.setDeleted(0);
    entity.setCreatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    appConfigTemplateMapper.insertTemplate(entity);
    return getConfigTemplate(entity.getId());
  }

  public AppConfigTemplateRecord updateConfigTemplate(Long id, UpdateConfigTemplateCommand command) {
    requireConfigTemplate(id);
    validateCreateOrUpdateConfigTemplateCommand(command);

    AppConfigTemplateMapper.AppConfigTemplateEntity entity = new AppConfigTemplateMapper.AppConfigTemplateEntity();
    entity.setId(id);
    entity.setTemplateCode(command.templateCode().trim());
    entity.setTemplateName(command.templateName().trim());
    entity.setTemplateContent(command.templateContent());
    entity.setRenderEngine(normalizeOptionalValue(command.renderEngine(), RENDER_ENGINES, "renderEngine", "PLAINTEXT"));
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedAt(LocalDateTime.now());

    appConfigTemplateMapper.updateTemplate(entity);
    return getConfigTemplate(id);
  }

  public boolean deleteConfigTemplate(Long id) {
    requireConfigTemplate(id);
    if (appVersionMapper.countActiveByConfigTemplateId(id) > 0) {
      throw new ConflictException("config template is referenced by active app versions");
    }
    appConfigTemplateMapper.markDeleted(id, DEFAULT_OPERATOR, LocalDateTime.now());
    return true;
  }

  public List<AppScriptTemplateRecord> getScriptTemplates() {
    return appScriptTemplateMapper.findAllActive().stream()
        .map(this::toAppScriptTemplateRecord)
        .toList();
  }

  public AppScriptTemplateRecord createScriptTemplate(CreateScriptTemplateCommand command) {
    validateCreateOrUpdateScriptTemplateCommand(command);

    LocalDateTime now = LocalDateTime.now();
    AppScriptTemplateMapper.AppScriptTemplateEntity entity = new AppScriptTemplateMapper.AppScriptTemplateEntity();
    entity.setTemplateCode(command.templateCode().trim());
    entity.setTemplateName(command.templateName().trim());
    entity.setScriptType(normalizeRequiredValue(command.scriptType()));
    entity.setScriptContent(command.scriptContent());
    entity.setDeleted(0);
    entity.setCreatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    appScriptTemplateMapper.insertTemplate(entity);
    return getScriptTemplate(entity.getId());
  }

  public AppScriptTemplateRecord updateScriptTemplate(Long id, UpdateScriptTemplateCommand command) {
    requireScriptTemplate(id);
    validateCreateOrUpdateScriptTemplateCommand(command);

    AppScriptTemplateMapper.AppScriptTemplateEntity entity = new AppScriptTemplateMapper.AppScriptTemplateEntity();
    entity.setId(id);
    entity.setTemplateCode(command.templateCode().trim());
    entity.setTemplateName(command.templateName().trim());
    entity.setScriptType(normalizeRequiredValue(command.scriptType()));
    entity.setScriptContent(command.scriptContent());
    entity.setUpdatedBy(DEFAULT_OPERATOR);
    entity.setUpdatedAt(LocalDateTime.now());

    appScriptTemplateMapper.updateTemplate(entity);
    return getScriptTemplate(id);
  }

  public boolean deleteScriptTemplate(Long id) {
    requireScriptTemplate(id);
    if (appVersionMapper.countActiveByScriptTemplateId(id) > 0) {
      throw new ConflictException("script template is referenced by active app versions");
    }
    appScriptTemplateMapper.markDeleted(id, DEFAULT_OPERATOR, LocalDateTime.now());
    return true;
  }

  private AppDefinitionMapper.AppDefinitionRow requireApp(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("appId is required");
    }

    AppDefinitionMapper.AppDefinitionRow row = appDefinitionMapper.findActiveById(id);
    if (row == null) {
      throw new IllegalArgumentException("app not found: " + id);
    }
    return row;
  }

  private AppVersionMapper.AppVersionRow requireVersion(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("appVersionId is required");
    }

    AppVersionMapper.AppVersionRow row = appVersionMapper.findActiveById(id);
    if (row == null) {
      throw new IllegalArgumentException("app version not found: " + id);
    }
    return row;
  }

  private AppVersionRecord getVersion(Long id) {
    return toAppVersionRecord(requireVersion(id));
  }

  private AppPackageMapper.AppPackageRow requirePackage(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("packageId is required");
    }

    AppPackageMapper.AppPackageRow row = appPackageMapper.findActiveById(id);
    if (row == null) {
      throw new IllegalArgumentException("package not found: " + id);
    }
    return row;
  }

  private AppPackageRecord getPackage(Long id) {
    return toAppPackageRecord(requirePackage(id));
  }

  private AppConfigTemplateMapper.AppConfigTemplateRow requireConfigTemplate(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("configTemplateId is required");
    }

    AppConfigTemplateMapper.AppConfigTemplateRow row = appConfigTemplateMapper.findActiveById(id);
    if (row == null) {
      throw new IllegalArgumentException("config template not found: " + id);
    }
    return row;
  }

  private AppConfigTemplateRecord getConfigTemplate(Long id) {
    return toAppConfigTemplateRecord(requireConfigTemplate(id));
  }

  private AppScriptTemplateMapper.AppScriptTemplateRow requireScriptTemplate(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("scriptTemplateId is required");
    }

    AppScriptTemplateMapper.AppScriptTemplateRow row = appScriptTemplateMapper.findActiveById(id);
    if (row == null) {
      throw new IllegalArgumentException("script template not found: " + id);
    }
    return row;
  }

  private AppScriptTemplateRecord getScriptTemplate(Long id) {
    return toAppScriptTemplateRecord(requireScriptTemplate(id));
  }

  private void requirePackageIfPresent(Long packageId) {
    if (packageId != null) {
      requirePackage(packageId);
    }
  }

  private void requireConfigTemplateIfPresent(Long configTemplateId) {
    if (configTemplateId != null) {
      requireConfigTemplate(configTemplateId);
    }
  }

  private void requireScriptTemplateIfPresent(Long scriptTemplateId) {
    if (scriptTemplateId != null) {
      requireScriptTemplate(scriptTemplateId);
    }
  }

  private void validateCreateOrUpdateAppCommand(AppMutation command) {
    if (command == null
        || !StringUtils.hasText(command.appCode())
        || !StringUtils.hasText(command.appName())
        || !StringUtils.hasText(command.appType())) {
      throw new IllegalArgumentException("appCode, appName and appType are required");
    }

    validateAllowedValue(command.appType(), APP_TYPES, "appType");
    validateOptionalAllowedValue(command.deployMode(), DEPLOY_MODES, "deployMode");
    validatePort(command.defaultPort());
    validateStatus(command.status());
  }

  private void validateCreateOrUpdateVersionCommand(AppVersionMutation command) {
    if (command == null || !StringUtils.hasText(command.versionNo())) {
      throw new IllegalArgumentException("versionNo is required");
    }

    validateStatus(command.status());
  }

  private void validateUploadPackageCommand(MultipartFile file, UploadPackageCommand command) {
    if (file == null) {
      throw new IllegalArgumentException("file is required");
    }

    if (file.isEmpty()) {
      throw new IllegalArgumentException("file must not be empty");
    }

    if (command == null
        || !StringUtils.hasText(command.packageName())
        || !StringUtils.hasText(command.packageType())
        || !StringUtils.hasText(command.filePath())) {
      throw new IllegalArgumentException("packageName, packageType and filePath are required");
    }

    validateAllowedValue(command.packageType(), PACKAGE_TYPES, "packageType");
    validateOptionalAllowedValue(command.storageType(), STORAGE_TYPES, "storageType");

    if (command.fileSize() != null && command.fileSize() < 0) {
      throw new IllegalArgumentException("fileSize must be greater than or equal to 0");
    }
  }

  private void validateCreateOrUpdateConfigTemplateCommand(ConfigTemplateMutation command) {
    if (command == null
        || !StringUtils.hasText(command.templateCode())
        || !StringUtils.hasText(command.templateName())
        || !StringUtils.hasText(command.templateContent())) {
      throw new IllegalArgumentException("templateCode, templateName and templateContent are required");
    }

    validateOptionalAllowedValue(command.renderEngine(), RENDER_ENGINES, "renderEngine");
  }

  private void validateCreateOrUpdateScriptTemplateCommand(ScriptTemplateMutation command) {
    if (command == null
        || !StringUtils.hasText(command.templateCode())
        || !StringUtils.hasText(command.templateName())
        || !StringUtils.hasText(command.scriptType())
        || !StringUtils.hasText(command.scriptContent())) {
      throw new IllegalArgumentException("templateCode, templateName, scriptType and scriptContent are required");
    }

    validateAllowedValue(command.scriptType(), SCRIPT_TYPES, "scriptType");
  }

  private Integer normalizeStatus(Integer status) {
    validateStatus(status);
    return status == null ? 1 : status;
  }

  private void validateStatus(Integer status) {
    if (status != null && !VALID_STATUS.contains(status)) {
      throw new IllegalArgumentException("status must be one of [0, 1]");
    }
  }

  private void validatePort(Integer defaultPort) {
    if (defaultPort != null && (defaultPort < 1 || defaultPort > 65535)) {
      throw new IllegalArgumentException("defaultPort must be between 1 and 65535");
    }
  }

  private void validateAllowedValue(String value, List<String> allowedValues, String fieldName) {
    String normalized = normalizeRequiredValue(value);
    if (!allowedValues.contains(normalized)) {
      throw new IllegalArgumentException(fieldName + " must be one of " + allowedValues);
    }
  }

  private void validateOptionalAllowedValue(String value, List<String> allowedValues, String fieldName) {
    if (!StringUtils.hasText(value)) {
      return;
    }

    String normalized = normalizeRequiredValue(value);
    if (!allowedValues.contains(normalized)) {
      throw new IllegalArgumentException(fieldName + " must be one of " + allowedValues);
    }
  }

  private String normalizeRequiredValue(String value) {
    return value == null ? null : value.trim().toUpperCase();
  }

  private String normalizeOptionalValue(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
  }

  private String normalizeOptionalValue(String value, List<String> allowedValues, String fieldName, String defaultValue) {
    if (!StringUtils.hasText(value)) {
      return defaultValue;
    }
    validateOptionalAllowedValue(value, allowedValues, fieldName);
    return value.trim().toUpperCase();
  }

  private byte[] readFileContent(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to read uploaded package", exception);
    }
  }

  private String sha256(byte[] content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return "sha256:" + HexFormat.of().formatHex(digest.digest(content));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to compute file hash", exception);
    }
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private AppRecord toAppRecord(AppDefinitionMapper.AppDefinitionRow row) {
    return new AppRecord(
        row.getId(),
        row.getAppCode(),
        row.getAppName(),
        row.getAppType(),
        row.getRuntimeType(),
        row.getDeployMode(),
        row.getDefaultPort(),
        row.getHealthCheckPath(),
        row.getDescription(),
        row.getStatus(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }

  private AppVersionRecord toAppVersionRecord(AppVersionMapper.AppVersionRow row) {
    return new AppVersionRecord(
        row.getId(),
        row.getAppId(),
        row.getVersionNo(),
        row.getPackageId(),
        row.getConfigTemplateId(),
        row.getScriptTemplateId(),
        row.getChangelog(),
        row.getStatus(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }

  private AppPackageRecord toAppPackageRecord(AppPackageMapper.AppPackageRow row) {
    return new AppPackageRecord(
        row.getId(),
        row.getPackageName(),
        row.getPackageType(),
        row.getFilePath(),
        row.getFileSize(),
        row.getFileHash(),
        row.getStorageType(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }

  private AppConfigTemplateRecord toAppConfigTemplateRecord(AppConfigTemplateMapper.AppConfigTemplateRow row) {
    return new AppConfigTemplateRecord(
        row.getId(),
        row.getTemplateCode(),
        row.getTemplateName(),
        row.getTemplateContent(),
        row.getRenderEngine(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }

  private AppScriptTemplateRecord toAppScriptTemplateRecord(AppScriptTemplateMapper.AppScriptTemplateRow row) {
    return new AppScriptTemplateRecord(
        row.getId(),
        row.getTemplateCode(),
        row.getTemplateName(),
        row.getScriptType(),
        row.getScriptContent(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }

  public record AppRecord(Long id,
                          String appCode,
                          String appName,
                          String appType,
                          String runtimeType,
                          String deployMode,
                          Integer defaultPort,
                          String healthCheckPath,
                          String description,
                          Integer status,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
  }

  public record AppVersionRecord(Long id,
                                 Long appId,
                                 String versionNo,
                                 Long packageId,
                                 Long configTemplateId,
                                 Long scriptTemplateId,
                                 String changelog,
                                 Integer status,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
  }

  public record AppPackageRecord(Long id,
                                 String packageName,
                                 String packageType,
                                 String filePath,
                                 Long fileSize,
                                 String fileHash,
                                 String storageType,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
  }

  public record AppConfigTemplateRecord(Long id,
                                        String templateCode,
                                        String templateName,
                                        String templateContent,
                                        String renderEngine,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
  }

  public record AppScriptTemplateRecord(Long id,
                                        String templateCode,
                                        String templateName,
                                        String scriptType,
                                        String scriptContent,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
  }

  public record CreateAppCommand(String appCode,
                                 String appName,
                                 String appType,
                                 String runtimeType,
                                 String deployMode,
                                 Integer defaultPort,
                                 String healthCheckPath,
                                 String description,
                                 Integer status) implements AppMutation {
  }

  public record UpdateAppCommand(String appCode,
                                 String appName,
                                 String appType,
                                 String runtimeType,
                                 String deployMode,
                                 Integer defaultPort,
                                 String healthCheckPath,
                                 String description,
                                 Integer status) implements AppMutation {
  }

  public record CreateAppVersionCommand(String versionNo,
                                        Long packageId,
                                        Long configTemplateId,
                                        Long scriptTemplateId,
                                        String changelog,
                                        Integer status) implements AppVersionMutation {
  }

  public record UpdateAppVersionCommand(String versionNo,
                                        Long packageId,
                                        Long configTemplateId,
                                        Long scriptTemplateId,
                                        String changelog,
                                        Integer status) implements AppVersionMutation {
  }

  public record UploadPackageCommand(String packageName,
                                     String packageType,
                                     String filePath,
                                     Long fileSize,
                                     String fileHash,
                                     String storageType) {
  }

  public record CreateConfigTemplateCommand(String templateCode,
                                            String templateName,
                                            String templateContent,
                                            String renderEngine) implements ConfigTemplateMutation {
  }

  public record UpdateConfigTemplateCommand(String templateCode,
                                            String templateName,
                                            String templateContent,
                                            String renderEngine) implements ConfigTemplateMutation {
  }

  public record CreateScriptTemplateCommand(String templateCode,
                                            String templateName,
                                            String scriptType,
                                            String scriptContent) implements ScriptTemplateMutation {
  }

  public record UpdateScriptTemplateCommand(String templateCode,
                                            String templateName,
                                            String scriptType,
                                            String scriptContent) implements ScriptTemplateMutation {
  }

  private interface AppMutation {
    String appCode();

    String appName();

    String appType();

    String runtimeType();

    String deployMode();

    Integer defaultPort();

    String healthCheckPath();

    String description();

    Integer status();
  }

  private interface AppVersionMutation {
    String versionNo();

    Long packageId();

    Long configTemplateId();

    Long scriptTemplateId();

    String changelog();

    Integer status();
  }

  private interface ConfigTemplateMutation {
    String templateCode();

    String templateName();

    String templateContent();

    String renderEngine();
  }

  private interface ScriptTemplateMutation {
    String templateCode();

    String templateName();

    String scriptType();

    String scriptContent();
  }
}

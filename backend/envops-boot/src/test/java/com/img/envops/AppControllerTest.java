package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345",
    "envops.storage.local-base-dir=${java.io.tmpdir}/envops-task6-app-packages"
})
class AppControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Value("${envops.storage.local-base-dir}")
  private String storageBaseDir;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Test
  void getAppsReturnsSeededApps() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/apps")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)))
        .andExpect(jsonPath("$.data[*].appCode", hasItems("gateway-nginx", "order-service")));
  }

  @Test
  void appCrudAndVersionCrudWorkTogether() throws Exception {
    String accessToken = login();

    MvcResult createAppResult = mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "inventory-service",
                  "appName": "库存服务",
                  "appType": "JAVA",
                  "runtimeType": "SPRING_BOOT",
                  "deployMode": "SYSTEMD",
                  "defaultPort": 8090,
                  "healthCheckPath": "/actuator/health",
                  "description": "库存管理后端",
                  "status": 1
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.appCode").value("inventory-service"))
        .andReturn();

    long appId = extractId(createAppResult);

    mockMvc.perform(get("/api/apps/{id}", appId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.appName").value("库存服务"))
        .andExpect(jsonPath("$.data.defaultPort").value(8090));

    mockMvc.perform(put("/api/apps/{id}", appId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "inventory-service",
                  "appName": "库存服务升级版",
                  "appType": "JAVA",
                  "runtimeType": "SPRING_BOOT",
                  "deployMode": "SYSTEMD",
                  "defaultPort": 8091,
                  "healthCheckPath": "/healthz",
                  "description": "库存管理后端升级版",
                  "status": 1
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.appName").value("库存服务升级版"))
        .andExpect(jsonPath("$.data.defaultPort").value(8091))
        .andExpect(jsonPath("$.data.healthCheckPath").value("/healthz"));

    MvcResult createVersionResult = mockMvc.perform(post("/api/apps/{id}/versions", appId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "versionNo": "2.0.0",
                  "packageId": 1101,
                  "configTemplateId": 1201,
                  "scriptTemplateId": 1301,
                  "changelog": "库存服务首个正式版本",
                  "status": 1
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.versionNo").value("2.0.0"))
        .andExpect(jsonPath("$.data.appId").value((int) appId))
        .andReturn();

    long versionId = extractId(createVersionResult);

    mockMvc.perform(get("/api/apps/{id}/versions", appId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data[0].id").value((int) versionId))
        .andExpect(jsonPath("$.data[0].versionNo").value("2.0.0"));

    mockMvc.perform(put("/api/app-versions/{id}", versionId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "versionNo": "2.0.1",
                  "packageId": 1101,
                  "configTemplateId": 1201,
                  "scriptTemplateId": 1301,
                  "changelog": "修复库存预热逻辑",
                  "status": 1
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.versionNo").value("2.0.1"))
        .andExpect(jsonPath("$.data.changelog").value("修复库存预热逻辑"));

    mockMvc.perform(delete("/api/app-versions/{id}", versionId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data").value(true));

    Integer deletedVersionFlag = jdbcTemplate.queryForObject(
        "SELECT deleted FROM app_version WHERE id = ?",
        Integer.class,
        versionId);
    org.assertj.core.api.Assertions.assertThat(deletedVersionFlag).isEqualTo(1);

    mockMvc.perform(delete("/api/apps/{id}", appId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data").value(true));

    Integer deletedAppFlag = jdbcTemplate.queryForObject(
        "SELECT deleted FROM app_definition WHERE id = ?",
        Integer.class,
        appId);
    org.assertj.core.api.Assertions.assertThat(deletedAppFlag).isEqualTo(1);
  }

  @Test
  void packageUploadStoresFileAndComputesServerHash() throws Exception {
    String accessToken = login();
    String fileName = "inventory-service-" + System.nanoTime() + ".jar";
    byte[] fileContent = "fake-jar-content".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile(
        "file",
        fileName,
        "application/java-archive",
        fileContent);

    MvcResult uploadPackageResult = mockMvc.perform(multipart("/api/packages/upload")
            .file(file)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageName", fileName)
            .param("packageType", "JAR")
            .param("storageType", "LOCAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.packageName").value(fileName))
        .andExpect(jsonPath("$.data.packageType").value("JAR"))
        .andExpect(jsonPath("$.data.filePath").value("packages/" + fileName))
        .andExpect(jsonPath("$.data.fileHash").value(sha256(fileContent)))
        .andReturn();

    long packageId = extractId(uploadPackageResult);
    Path storedFile = Paths.get(storageBaseDir).resolve("packages").resolve(fileName);

    org.assertj.core.api.Assertions.assertThat(Files.exists(storedFile)).isTrue();
    org.assertj.core.api.Assertions.assertThat(Files.readAllBytes(storedFile)).isEqualTo(fileContent);

    String storedHash = jdbcTemplate.queryForObject(
        "SELECT file_hash FROM app_package WHERE id = ?",
        String.class,
        packageId);
    org.assertj.core.api.Assertions.assertThat(storedHash).isEqualTo(sha256(fileContent));
  }

  @Test
  void uploadingSamePackageNameDoesNotOverwriteExistingArtifact() throws Exception {
    String accessToken = login();
    String fileName = "duplicate-package-" + System.nanoTime() + ".jar";
    byte[] firstContent = "first-package-content".getBytes(StandardCharsets.UTF_8);
    byte[] secondContent = "second-package-content".getBytes(StandardCharsets.UTF_8);

    MockMultipartFile firstFile = new MockMultipartFile(
        "file",
        fileName,
        "application/java-archive",
        firstContent);
    MockMultipartFile secondFile = new MockMultipartFile(
        "file",
        fileName,
        "application/java-archive",
        secondContent);

    MvcResult firstUploadResult = mockMvc.perform(multipart("/api/packages/upload")
            .file(firstFile)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageName", fileName)
            .param("packageType", "JAR")
            .param("storageType", "LOCAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    MvcResult secondUploadResult = mockMvc.perform(multipart("/api/packages/upload")
            .file(secondFile)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageName", fileName)
            .param("packageType", "JAR")
            .param("storageType", "LOCAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andReturn();

    String firstFilePath = extractStringField(firstUploadResult, "filePath");
    String secondFilePath = extractStringField(secondUploadResult, "filePath");

    org.assertj.core.api.Assertions.assertThat(firstFilePath).startsWith("packages/");
    org.assertj.core.api.Assertions.assertThat(secondFilePath).startsWith("packages/");
    org.assertj.core.api.Assertions.assertThat(secondFilePath).isNotEqualTo(firstFilePath);

    Path firstStoredFile = Paths.get(storageBaseDir).resolve(firstFilePath);
    Path secondStoredFile = Paths.get(storageBaseDir).resolve(secondFilePath);

    org.assertj.core.api.Assertions.assertThat(Files.readAllBytes(firstStoredFile)).isEqualTo(firstContent);
    org.assertj.core.api.Assertions.assertThat(Files.readAllBytes(secondStoredFile)).isEqualTo(secondContent);
  }

  @Test
  void uploadPackageRejectsEmptyFile() throws Exception {
    String accessToken = login();
    MockMultipartFile emptyFile = new MockMultipartFile(
        "file",
        "empty.jar",
        "application/java-archive",
        new byte[0]);

    mockMvc.perform(multipart("/api/packages/upload")
            .file(emptyFile)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageType", "JAR"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("file must not be empty"));
  }

  @Test
  void uploadPackageRejectsInvalidPackageType() throws Exception {
    String accessToken = login();
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "invalid-package-type.jar",
        "application/java-archive",
        "fake-jar-content".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/packages/upload")
            .file(file)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageType", "ZIP"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("packageType must be one of [JAR, TAR, RPM, SH]"));

    mockMvc.perform(multipart("/api/packages/upload")
            .file(file)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageType", "JAR")
            .param("storageType", "OSS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("storageType must be one of [LOCAL]"));
  }

  @Test
  void uploadPackageRejectsMinioStorageTypeUntilRemoteStoreExists() throws Exception {
    String accessToken = login();
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "minio-package.jar",
        "application/java-archive",
        "fake-jar-content".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/packages/upload")
            .file(file)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageType", "JAR")
            .param("storageType", "MINIO"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("storageType must be one of [LOCAL]"));
  }

  @Test
  void uploadPackageRejectsMismatchedClientHash() throws Exception {
    String accessToken = login();
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "hash-mismatch.jar",
        "application/java-archive",
        "fake-jar-content".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(multipart("/api/packages/upload")
            .file(file)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageType", "JAR")
            .param("fileHash", "sha256:deadbeef"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("fileHash does not match uploaded file content"));
  }

  @Test
  void createAppRejectsMissingRequiredFields() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appName": "bad-app"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("appCode, appName and appType are required"));
  }

  @Test
  void createAppRejectsInvalidAppType() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "bad-app-type",
                  "appName": "bad-app-type",
                  "appType": "NODEJS"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("appType must be one of [JAVA, NGINX, SCRIPT, DOCKER]"));
  }

  @Test
  void createAppRejectsInvalidDefaultPort() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "bad-port-app",
                  "appName": "bad-port-app",
                  "appType": "JAVA",
                  "defaultPort": 70000
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("defaultPort must be between 1 and 65535"));
  }

  @Test
  void createAppRejectsInvalidDeployModeAndStatus() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "bad-deploy-mode",
                  "appName": "bad-deploy-mode",
                  "appType": "JAVA",
                  "deployMode": "K8S"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("deployMode must be one of [SYSTEMD, PROCESS, DOCKER]"));

    mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "bad-status",
                  "appName": "bad-status",
                  "appType": "JAVA",
                  "status": 2
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("status must be one of [0, 1]"));
  }

  @Test
  void templateEndpointsRejectInvalidEnumFields() throws Exception {
    String accessToken = login();

    mockMvc.perform(post("/api/config-templates")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "templateCode": "invalid-render-engine",
                  "templateName": "invalid-render-engine",
                  "templateContent": "server.port={{port}}",
                  "renderEngine": "HANDLEBARS"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("renderEngine must be one of [PLAINTEXT, JINJA2, FREEMARKER]"));

    mockMvc.perform(post("/api/script-templates")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "templateCode": "invalid-script-type",
                  "templateName": "invalid-script-type",
                  "scriptType": "POWERSHELL",
                  "scriptContent": "Write-Host test"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("scriptType must be one of [BASH, PYTHON]"));
  }

  @Test
  void createVersionRejectsInvalidStatus() throws Exception {
    String accessToken = login();
    long appId = createApp(accessToken, "invalid-version-status-" + System.nanoTime());

    mockMvc.perform(post("/api/apps/{id}/versions", appId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "versionNo": "1.0.0",
                  "status": 2
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("400"))
        .andExpect(jsonPath("$.msg").value("status must be one of [0, 1]"));
  }

  @Test
  void duplicateBusinessKeysReturnConflict() throws Exception {
    String accessToken = login();
    String appCode = "duplicate-app-" + System.nanoTime();
    long appId = createApp(accessToken, appCode);

    mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "%s",
                  "appName": "duplicate-app",
                  "appType": "JAVA"
                }
                """.formatted(appCode)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"));

    createVersion(accessToken, appId, "1.0.0");
    mockMvc.perform(post("/api/apps/{id}/versions", appId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "versionNo": "1.0.0",
                  "status": 1
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"));
  }

  @Test
  void referencedResourcesCannotBeDeleted() throws Exception {
    String accessToken = login();
    long packageId = createPackage(accessToken, "referenced-package-" + System.nanoTime() + ".jar");
    long configTemplateId = createConfigTemplate(accessToken, "referenced-config-" + System.nanoTime());
    long scriptTemplateId = createScriptTemplate(accessToken, "referenced-script-" + System.nanoTime());
    long appId = createApp(accessToken, "referenced-app-" + System.nanoTime());

    mockMvc.perform(post("/api/apps/{id}/versions", appId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "versionNo": "1.0.0",
                  "packageId": %d,
                  "configTemplateId": %d,
                  "scriptTemplateId": %d,
                  "status": 1
                }
                """.formatted(packageId, configTemplateId, scriptTemplateId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"));

    mockMvc.perform(delete("/api/packages/{id}", packageId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"))
        .andExpect(jsonPath("$.msg").value("package is referenced by active app versions"));

    mockMvc.perform(delete("/api/config-templates/{id}", configTemplateId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"))
        .andExpect(jsonPath("$.msg").value("config template is referenced by active app versions"));

    mockMvc.perform(delete("/api/script-templates/{id}", scriptTemplateId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"))
        .andExpect(jsonPath("$.msg").value("script template is referenced by active app versions"));

    mockMvc.perform(delete("/api/apps/{id}", appId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("409"))
        .andExpect(jsonPath("$.msg").value("app is referenced by active versions"));
  }

  @Test
  void softDeletedResourcesCanReuseOriginalBusinessKeys() throws Exception {
    String accessToken = login();
    String appCode = "reusable-app-" + System.nanoTime();
    String configTemplateCode = "reusable-config-" + System.nanoTime();
    String scriptTemplateCode = "reusable-script-" + System.nanoTime();

    long appId = createApp(accessToken, appCode);
    mockMvc.perform(delete("/api/apps/{id}", appId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
    mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "%s",
                  "appName": "recreated-app",
                  "appType": "JAVA"
                }
                """.formatted(appCode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.appCode").value(appCode));

    long configTemplateId = createConfigTemplate(accessToken, configTemplateCode);
    mockMvc.perform(delete("/api/config-templates/{id}", configTemplateId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
    mockMvc.perform(post("/api/config-templates")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "templateCode": "%s",
                  "templateName": "recreated-config",
                  "templateContent": "server.port={{port}}",
                  "renderEngine": "PLAINTEXT"
                }
                """.formatted(configTemplateCode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.templateCode").value(configTemplateCode));

    long scriptTemplateId = createScriptTemplate(accessToken, scriptTemplateCode);
    mockMvc.perform(delete("/api/script-templates/{id}", scriptTemplateId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
    mockMvc.perform(post("/api/script-templates")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "templateCode": "%s",
                  "templateName": "recreated-script",
                  "scriptType": "BASH",
                  "scriptContent": "#!/usr/bin/env bash\\necho ok"
                }
                """.formatted(scriptTemplateCode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.templateCode").value(scriptTemplateCode));

    long versionAppId = createApp(accessToken, "version-app-" + System.nanoTime());
    long versionId = createVersion(accessToken, versionAppId, "1.0.0");
    mockMvc.perform(delete("/api/app-versions/{id}", versionId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
    mockMvc.perform(post("/api/apps/{id}/versions", versionAppId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "versionNo": "1.0.0",
                  "status": 1
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.versionNo").value("1.0.0"));
  }

  @Test
  void reusedBusinessKeysCanBeDeletedMoreThanOnce() throws Exception {
    String accessToken = login();
    String appCode = "multi-delete-app-" + System.nanoTime();
    String configTemplateCode = "multi-delete-config-" + System.nanoTime();
    String scriptTemplateCode = "multi-delete-script-" + System.nanoTime();

    long firstAppId = createApp(accessToken, appCode);
    mockMvc.perform(delete("/api/apps/{id}", firstAppId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
    long secondAppId = createApp(accessToken, appCode);
    mockMvc.perform(delete("/api/apps/{id}", secondAppId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));

    long firstConfigTemplateId = createConfigTemplate(accessToken, configTemplateCode);
    mockMvc.perform(delete("/api/config-templates/{id}", firstConfigTemplateId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
    long secondConfigTemplateId = createConfigTemplate(accessToken, configTemplateCode);
    mockMvc.perform(delete("/api/config-templates/{id}", secondConfigTemplateId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));

    long firstScriptTemplateId = createScriptTemplate(accessToken, scriptTemplateCode);
    mockMvc.perform(delete("/api/script-templates/{id}", firstScriptTemplateId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
    long secondScriptTemplateId = createScriptTemplate(accessToken, scriptTemplateCode);
    mockMvc.perform(delete("/api/script-templates/{id}", secondScriptTemplateId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));

    long versionAppId = createApp(accessToken, "multi-delete-version-app-" + System.nanoTime());
    long firstVersionId = createVersion(accessToken, versionAppId, "1.0.0");
    mockMvc.perform(delete("/api/app-versions/{id}", firstVersionId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
    long secondVersionId = createVersion(accessToken, versionAppId, "1.0.0");
    mockMvc.perform(delete("/api/app-versions/{id}", secondVersionId)
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(true));
  }

  @Test
  void nonSuperAdminCannotAccessAppApis() throws Exception {
    seedUserWithRole(2L, "release-engineer", "Release@123", 6L, "RELEASE_ENGINEER", "Release Engineer");
    String accessToken = login("release-engineer", "Release@123");

    mockMvc.perform(get("/api/apps")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("403"))
        .andExpect(jsonPath("$.msg").value("Forbidden"));
  }

  private long createApp(String accessToken, String appCode) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/apps")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "appCode": "%s",
                  "appName": "%s",
                  "appType": "JAVA"
                }
                """.formatted(appCode, appCode)))
        .andExpect(status().isOk())
        .andReturn();

    return extractId(result);
  }

  private long createPackage(String accessToken, String packageName) throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        packageName,
        "application/java-archive",
        ("package-" + packageName).getBytes(StandardCharsets.UTF_8));

    MvcResult result = mockMvc.perform(multipart("/api/packages/upload")
            .file(file)
            .header("Authorization", "Bearer " + accessToken)
            .param("packageName", packageName)
            .param("packageType", "JAR")
            .param("storageType", "LOCAL"))
        .andExpect(status().isOk())
        .andReturn();

    return extractId(result);
  }

  private long createConfigTemplate(String accessToken, String templateCode) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/config-templates")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "templateCode": "%s",
                  "templateName": "%s",
                  "templateContent": "server.port={{port}}",
                  "renderEngine": "PLAINTEXT"
                }
                """.formatted(templateCode, templateCode)))
        .andExpect(status().isOk())
        .andReturn();

    return extractId(result);
  }

  private long createScriptTemplate(String accessToken, String templateCode) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/script-templates")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "templateCode": "%s",
                  "templateName": "%s",
                  "scriptType": "BASH",
                  "scriptContent": "#!/usr/bin/env bash\\necho ok"
                }
                """.formatted(templateCode, templateCode)))
        .andExpect(status().isOk())
        .andReturn();

    return extractId(result);
  }

  private long createVersion(String accessToken, long appId, String versionNo) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/apps/{id}/versions", appId)
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "versionNo": "%s",
                  "status": 1
                }
                """.formatted(versionNo)))
        .andExpect(status().isOk())
        .andReturn();

    return extractId(result);
  }

  private long extractId(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
  }

  private String extractStringField(MvcResult result, String fieldName) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path(fieldName).asText();
  }

  private void seedUserWithRole(Long userId,
                                String userName,
                                String password,
                                Long roleId,
                                String roleKey,
                                String roleName) {
    jdbcTemplate.update(
        "INSERT INTO sys_user (id, user_name, password, phone, team_key, login_type, status, last_login_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
        userId,
        userName,
        passwordEncoder.encode(password),
        "139" + String.format("%08d", userId % 100000000L),
        "platform",
        "PASSWORD",
        "ACTIVE");
    jdbcTemplate.update("INSERT INTO sys_role (id, role_key, role_name) VALUES (?, ?, ?)", roleId, roleKey, roleName);
    jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
  }

  private String login() throws Exception {
    return login("envops-admin", "EnvOps@123");
  }

  private String login(String userName, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "userName": "%s",
                  "password": "%s"
                }
                """.formatted(userName, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }

  private String sha256(byte[] content) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return "sha256:" + HexFormat.of().formatHex(digest.digest(content));
  }
}

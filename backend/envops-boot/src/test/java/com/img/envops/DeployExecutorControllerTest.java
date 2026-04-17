package com.img.envops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.app.infrastructure.LocalPackageStorage;
import com.img.envops.modules.deploy.application.DeployExecutorApplicationService;
import com.img.envops.modules.deploy.executor.OpenSshProcessRunner;
import com.img.envops.modules.deploy.executor.SshConnectionOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "envops.security.token-secret=test-only-envops-token-secret-12345",
    "envops.security.credential-protection-secret=test-only-envops-credential-protection-secret-12345",
    "envops.storage.local-base-dir=${java.io.tmpdir}/envops-task1-deploy-storage"
})
class DeployExecutorControllerTest {
  private static final String SSH_REMOTE_EXECUTOR_CLASS = "com.img.envops.modules.deploy.executor.SshRemoteExecutor";
  private static final String SSH_PROCESS_RUNNER_CLASS = "com.img.envops.modules.deploy.executor.SshProcessRunner";
  private static final String SSH_CONNECTION_OPTIONS_CLASS = "com.img.envops.modules.deploy.executor.SshConnectionOptions";
  private static final String EXEC_REQUEST_CLASS = "com.img.envops.modules.deploy.executor.ExecRequest";
  private static final String FILE_UPLOAD_REQUEST_CLASS = "com.img.envops.modules.deploy.executor.FileUploadRequest";
  private static final String DETECT_REQUEST_CLASS = "com.img.envops.modules.deploy.executor.DetectRequest";
  private static final String EXEC_OUTPUT = "runner-exec-output";
  private static final String UPLOAD_OUTPUT = "runner-upload-output";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private DeployExecutorApplicationService deployExecutorApplicationService;

  @Autowired
  private LocalPackageStorage localPackageStorage;

  @Value("${envops.storage.local-base-dir}")
  private String storageBaseDir;

  @Test
  void deployExecutorApplicationServiceDependsOnRemoteExecutorAbstraction() {
    Type[] genericInterfaces = deployExecutorApplicationService.getClass().getGenericInterfaces();
    assertThat(genericInterfaces).isEmpty();
    assertThat(Arrays.stream(deployExecutorApplicationService.getClass().getDeclaredFields())
        .map(field -> field.getType().getName()))
        .contains("com.img.envops.modules.deploy.executor.RemoteExecutor")
        .doesNotContain("com.img.envops.modules.deploy.executor.SshRemoteExecutor");
  }

  @Test
  void getDeployExecutorsRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/deploy/executors"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("401"))
        .andExpect(jsonPath("$.msg").value("Unauthorized"));
  }

  @Test
  void getDeployExecutorsReturnsSshAgentlessDirectoryAfterLogin() throws Exception {
    String accessToken = login();

    mockMvc.perform(get("/api/deploy/executors")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data[0].type").value("SSH_AGENTLESS"))
        .andExpect(jsonPath("$.data[0].name").value("SSH Agentless Executor"))
        .andExpect(jsonPath("$.data[0].status").value("READY"))
        .andExpect(jsonPath("$.data[0].supportsExec").value(true))
        .andExpect(jsonPath("$.data[0].supportsUpload").value(true))
        .andExpect(jsonPath("$.data[0].supportsDetect").value(true));
  }

  @Test
  void sshRemoteExecutorUsesConnectionOptionsAndReturnsRunnerOutput() throws Exception {
    RecordingSshRunner recordingRunner = new RecordingSshRunner(EXEC_OUTPUT, UPLOAD_OUTPUT);
    Object sshRemoteExecutor = instantiateSshRemoteExecutor(recordingRunner.createProxy());

    JsonNode execResult = invokeRecordMethod(
        sshRemoteExecutor,
        "exec",
        EXEC_REQUEST_CLASS,
        101L,
        "host-prd-01",
        "10.0.0.11",
        "envops",
        2222,
        "/tmp/keys/envops.pem",
        "systemctl status envops-agent");
    assertSuccessfulResult(execResult, EXEC_OUTPUT);
    assertThat(execResult.path("hostId").asLong()).isEqualTo(101L);
    assertThat(execResult.path("hostName").asText()).isEqualTo("host-prd-01");
    assertThat(execResult.path("ipAddress").asText()).isEqualTo("10.0.0.11");
    assertThat(execResult.path("command").asText()).isEqualTo("systemctl status envops-agent");
    assertThat(fieldNames(execResult)).doesNotContain("privateKeyPath", "secret");
    recordingRunner.assertExecInvocation(
        "10.0.0.11",
        "envops",
        2222,
        "/tmp/keys/envops.pem",
        "systemctl status envops-agent");

    JsonNode uploadResult = invokeRecordMethod(
        sshRemoteExecutor,
        "upload",
        FILE_UPLOAD_REQUEST_CLASS,
        101L,
        "host-prd-01",
        "10.0.0.11",
        "envops",
        2222,
        "/tmp/keys/envops.pem",
        "/tmp/envops-release.tar.gz",
        "/opt/envops/releases/envops-release.tar.gz");
    assertSuccessfulResult(uploadResult, UPLOAD_OUTPUT);
    assertThat(uploadResult.path("sourcePath").asText()).isEqualTo("/tmp/envops-release.tar.gz");
    assertThat(uploadResult.path("targetPath").asText()).isEqualTo("/opt/envops/releases/envops-release.tar.gz");
    assertThat(fieldNames(uploadResult)).doesNotContain("privateKeyPath", "secret");
    recordingRunner.assertUploadInvocation(
        "10.0.0.11",
        "envops",
        2222,
        "/tmp/keys/envops.pem",
        "/tmp/envops-release.tar.gz",
        "/opt/envops/releases/envops-release.tar.gz");
  }

  @Test
  void sshConnectionOptionsDefaultsPortAndRejectsInvalidInputs() throws Exception {
    Object options = invokeConnectionOptionsFrom("envops", null, "/tmp/keys/default.pem");

    assertThat(readProperty(options, "sshUser")).isEqualTo("envops");
    assertThat(readProperty(options, "sshPort")).isEqualTo(22);
    assertThat(readProperty(options, "privateKeyPath")).isEqualTo("/tmp/keys/default.pem");

    assertThatThrownBy(() -> invokeConnectionOptionsFrom(" ", 22, "/tmp/keys/default.pem"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sshUser");
    assertThatThrownBy(() -> invokeConnectionOptionsFrom("envops", 0, "/tmp/keys/default.pem"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sshPort");
    assertThatThrownBy(() -> invokeConnectionOptionsFrom("envops", 22, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("privateKeyPath");
  }

  @Test
  void sshRemoteExecutorDetectStaysReadyWithoutLeakingSecrets() throws Exception {
    RecordingSshRunner recordingRunner = new RecordingSshRunner(EXEC_OUTPUT, UPLOAD_OUTPUT);
    Object sshRemoteExecutor = instantiateSshRemoteExecutor(recordingRunner.createProxy());

    JsonNode detectResult = invokeRecordMethod(
        sshRemoteExecutor,
        "detect",
        DETECT_REQUEST_CLASS,
        101L,
        "host-prd-01",
        "10.0.0.11",
        "systemctl is-active nginx");

    assertThat(detectResult.path("executorType").asText()).isEqualTo("SSH_AGENTLESS");
    assertThat(detectResult.path("status").asText()).isEqualTo("READY");
    assertThat(detectResult.path("message").asText()).isNotBlank();
    assertThat(detectResult.path("probe").asText()).isEqualTo("systemctl is-active nginx");
    assertThat(fieldNames(detectResult)).doesNotContain("privateKeyPath", "secret");
  }

  @Test
  void localPackageStorageResolveReturnsExistingNormalizedPath() throws Exception {
    Path basePath = Paths.get(storageBaseDir).toAbsolutePath().normalize();
    Path existingFile = basePath.resolve("packages").resolve("deploy-" + System.nanoTime() + ".tar.gz");
    Files.createDirectories(existingFile.getParent());
    Files.writeString(existingFile, "fake-package-content");

    Path resolvedPath = invokeResolve("packages/../packages/" + existingFile.getFileName());

    assertThat(resolvedPath).isEqualTo(existingFile);
  }

  @Test
  void localPackageStorageResolveRejectsEscapingOrMissingPath() {
    assertThatThrownBy(() -> invokeResolve("../ssh/id_rsa"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("escapes storage base directory");

    assertThatThrownBy(() -> invokeResolve("packages/missing-" + System.nanoTime() + ".tar.gz"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void openSshProcessRunnerUsesSaferNonInteractiveCommandOptions() throws Exception {
    OpenSshProcessRunner runner = new OpenSshProcessRunner();
    SshConnectionOptions options = SshConnectionOptions.from("envops", 2222, "/tmp/keys/envops.pem");

    assertThat(invokeBuildExecCommand(runner, "10.0.0.11", options, "hostname"))
        .contains("ssh", "-i", "/tmp/keys/envops.pem", "-p", "2222")
        .contains("-o", "BatchMode=yes")
        .contains("-o", "ConnectTimeout=10")
        .contains("-o", "StrictHostKeyChecking=accept-new")
        .doesNotContain("StrictHostKeyChecking=no");
    assertThat(invokeBuildUploadCommand(runner, "10.0.0.11", options, "/tmp/demo.jar", "/opt/demo.jar"))
        .contains("scp", "-i", "/tmp/keys/envops.pem", "-P", "2222")
        .contains("-o", "BatchMode=yes")
        .contains("-o", "ConnectTimeout=10")
        .contains("-o", "StrictHostKeyChecking=accept-new")
        .doesNotContain("StrictHostKeyChecking=no");
  }

  private void assertSuccessfulResult(JsonNode result, String expectedMessage) {
    assertThat(result.path("executorType").asText()).isEqualTo("SSH_AGENTLESS");
    assertThat(result.path("status").asText()).isEqualTo("SUCCESS");
    assertThat(result.path("message").asText()).isEqualTo(expectedMessage);
  }

  private Iterable<String> fieldNames(JsonNode result) {
    Iterator<String> iterator = result.fieldNames();
    ArrayList<String> fields = new ArrayList<>();
    iterator.forEachRemaining(fields::add);
    return fields;
  }

  private Object instantiateSshRemoteExecutor(Object runnerProxy) throws Exception {
    Class<?> executorClass = loadClass(SSH_REMOTE_EXECUTOR_CLASS);
    Class<?> runnerType = loadClass(SSH_PROCESS_RUNNER_CLASS);
    try {
      Constructor<?> constructor = executorClass.getDeclaredConstructor(runnerType);
      return constructor.newInstance(runnerProxy);
    } catch (NoSuchMethodException exception) {
      throw new AssertionError("Expected SshRemoteExecutor to accept SshProcessRunner", exception);
    }
  }

  private JsonNode invokeRecordMethod(Object target,
                                      String methodName,
                                      String requestClassName,
                                      Object... args) throws Exception {
    Class<?> requestClass = loadClass(requestClassName);
    Object request = instantiateRecord(requestClass, args);
    Object result = target.getClass().getMethod(methodName, requestClass).invoke(target, request);
    return objectMapper.valueToTree(result);
  }

  private Object instantiateRecord(Class<?> recordType, Object... args) throws Exception {
    Class<?>[] parameterTypes = Arrays.stream(recordType.getRecordComponents())
        .map(RecordComponent::getType)
        .toArray(Class<?>[]::new);
    Constructor<?> constructor = recordType.getDeclaredConstructor(parameterTypes);
    try {
      return constructor.newInstance(args);
    } catch (IllegalArgumentException exception) {
      throw new AssertionError("Expected " + recordType.getSimpleName() + " to expose task-scoped SSH fields", exception);
    }
  }

  private Object invokeConnectionOptionsFrom(String sshUser, Integer sshPort, String privateKeyPath) throws Exception {
    Class<?> optionsClass = loadClass(SSH_CONNECTION_OPTIONS_CLASS);
    Method method;
    try {
      method = optionsClass.getMethod("from", String.class, Integer.class, String.class);
    } catch (NoSuchMethodException exception) {
      throw new AssertionError("Expected SshConnectionOptions.from(String, Integer, String) to exist", exception);
    }

    try {
      return method.invoke(null, sshUser, sshPort, privateKeyPath);
    } catch (InvocationTargetException exception) {
      throw rethrowInvocationTarget(exception);
    }
  }

  private Path invokeResolve(String relativePath) {
    Method method;
    try {
      method = LocalPackageStorage.class.getMethod("resolve", String.class);
    } catch (NoSuchMethodException exception) {
      throw new AssertionError("Expected LocalPackageStorage.resolve(String) to exist", exception);
    }

    try {
      return (Path) method.invoke(localPackageStorage, relativePath);
    } catch (IllegalAccessException exception) {
      throw new IllegalStateException("Failed to access LocalPackageStorage.resolve", exception);
    } catch (InvocationTargetException exception) {
      throw rethrowInvocationTarget(exception);
    }
  }

  private static Object readProperty(Object target, String methodName) {
    try {
      Method method = target.getClass().getMethod(methodName);
      return method.invoke(target);
    } catch (NoSuchMethodException exception) {
      throw new AssertionError("Expected method " + methodName + " on " + target.getClass().getSimpleName(), exception);
    } catch (IllegalAccessException exception) {
      throw new IllegalStateException("Failed to access " + methodName, exception);
    } catch (InvocationTargetException exception) {
      throw rethrowInvocationTarget(exception);
    }
  }

  private java.util.List<String> invokeBuildExecCommand(OpenSshProcessRunner runner,
                                                       String ipAddress,
                                                       SshConnectionOptions options,
                                                       String command) throws Exception {
    Method method = OpenSshProcessRunner.class.getDeclaredMethod("buildExecCommand", String.class, SshConnectionOptions.class, String.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.List<String> result = (java.util.List<String>) method.invoke(runner, ipAddress, options, command);
    return result;
  }

  private java.util.List<String> invokeBuildUploadCommand(OpenSshProcessRunner runner,
                                                         String ipAddress,
                                                         SshConnectionOptions options,
                                                         String sourcePath,
                                                         String targetPath) throws Exception {
    Method method = OpenSshProcessRunner.class.getDeclaredMethod("buildUploadCommand", String.class, SshConnectionOptions.class, String.class, String.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.List<String> result = (java.util.List<String>) method.invoke(runner, ipAddress, options, sourcePath, targetPath);
    return result;
  }

  private static RuntimeException rethrowInvocationTarget(InvocationTargetException exception) {
    Throwable targetException = exception.getTargetException();
    if (targetException instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    if (targetException instanceof Error error) {
      throw error;
    }
    return new IllegalStateException(targetException);
  }

  private static Class<?> loadClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException exception) {
      throw new AssertionError("Expected class " + className + " to exist", exception);
    }
  }

  private String login() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  \"userName\": \"envops-admin\",
                  \"password\": \"EnvOps@123\"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("0000"))
        .andExpect(jsonPath("$.data.token", not(blankOrNullString())))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    return data.path("token").asText();
  }

  private static final class RecordingSshRunner implements java.lang.reflect.InvocationHandler {
    private final String execOutput;
    private final String uploadOutput;
    private RunnerCall execCall;
    private UploadCall uploadCall;

    private RecordingSshRunner(String execOutput, String uploadOutput) {
      this.execOutput = execOutput;
      this.uploadOutput = uploadOutput;
    }

    private Object createProxy() {
      Class<?> runnerType = loadClass(SSH_PROCESS_RUNNER_CLASS);
      return Proxy.newProxyInstance(runnerType.getClassLoader(), new Class<?>[]{runnerType}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      if (method.getDeclaringClass() == Object.class) {
        return switch (method.getName()) {
          case "toString" -> "RecordingSshRunner";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> null;
        };
      }

      return switch (method.getName()) {
        case "exec" -> {
          execCall = new RunnerCall((String) args[0], args[1], (String) args[2]);
          yield execOutput;
        }
        case "upload" -> {
          uploadCall = new UploadCall((String) args[0], args[1], (String) args[2], (String) args[3]);
          yield uploadOutput;
        }
        default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
      };
    }

    private void assertExecInvocation(String ipAddress,
                                      String sshUser,
                                      Integer sshPort,
                                      String privateKeyPath,
                                      String command) {
      assertThat(execCall).isNotNull();
      assertThat(execCall.ipAddress()).isEqualTo(ipAddress);
      assertThat(execCall.command()).isEqualTo(command);
      assertConnectionOptions(execCall.options(), sshUser, sshPort, privateKeyPath);
    }

    private void assertUploadInvocation(String ipAddress,
                                        String sshUser,
                                        Integer sshPort,
                                        String privateKeyPath,
                                        String sourcePath,
                                        String targetPath) {
      assertThat(uploadCall).isNotNull();
      assertThat(uploadCall.ipAddress()).isEqualTo(ipAddress);
      assertThat(uploadCall.sourcePath()).isEqualTo(sourcePath);
      assertThat(uploadCall.targetPath()).isEqualTo(targetPath);
      assertConnectionOptions(uploadCall.options(), sshUser, sshPort, privateKeyPath);
    }

    private void assertConnectionOptions(Object options,
                                         String sshUser,
                                         Integer sshPort,
                                         String privateKeyPath) {
      assertThat(readProperty(options, "sshUser")).isEqualTo(sshUser);
      assertThat(readProperty(options, "sshPort")).isEqualTo(sshPort);
      assertThat(readProperty(options, "privateKeyPath")).isEqualTo(privateKeyPath);
    }
  }

  private record RunnerCall(String ipAddress, Object options, String command) {
  }

  private record UploadCall(String ipAddress, Object options, String sourcePath, String targetPath) {
  }

}

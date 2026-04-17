package com.img.envops.modules.deploy.executor;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OpenSshProcessRunner implements SshProcessRunner {
  private static final int CONNECT_TIMEOUT_SECONDS = 10;
  private static final int PROCESS_TIMEOUT_SECONDS = 60;
  private static final String HOST_KEY_CHECKING = "StrictHostKeyChecking=accept-new";
  private static final String BATCH_MODE = "BatchMode=yes";
  private static final String CONNECT_TIMEOUT = "ConnectTimeout=" + CONNECT_TIMEOUT_SECONDS;

  @Override
  public String exec(String ipAddress, SshConnectionOptions options, String command) {
    return run(buildExecCommand(ipAddress, options, command), "SSH command failed");
  }

  @Override
  public String upload(String ipAddress, SshConnectionOptions options, String sourcePath, String targetPath) {
    return run(buildUploadCommand(ipAddress, options, sourcePath, targetPath), "SCP upload failed");
  }

  private List<String> buildExecCommand(String ipAddress, SshConnectionOptions options, String command) {
    return List.of(
        "ssh",
        "-i", options.privateKeyPath(),
        "-p", String.valueOf(options.sshPort()),
        "-o", BATCH_MODE,
        "-o", CONNECT_TIMEOUT,
        "-o", HOST_KEY_CHECKING,
        options.sshUser() + "@" + ipAddress,
        command);
  }

  private List<String> buildUploadCommand(String ipAddress, SshConnectionOptions options, String sourcePath, String targetPath) {
    return List.of(
        "scp",
        "-i", options.privateKeyPath(),
        "-P", String.valueOf(options.sshPort()),
        "-o", BATCH_MODE,
        "-o", CONNECT_TIMEOUT,
        "-o", HOST_KEY_CHECKING,
        sourcePath,
        options.sshUser() + "@" + ipAddress + ":" + targetPath);
  }

  private String run(List<String> command, String failureMessage) {
    try {
      Process process = new ProcessBuilder(command)
          .redirectErrorStream(true)
          .start();

      if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new IllegalStateException(failureMessage + " timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
      }

      String output;
      try (InputStream inputStream = process.getInputStream()) {
        output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
      }

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new IllegalStateException(failureMessage + " (exit=" + exitCode + "): " + output);
      }

      return output;
    } catch (IOException exception) {
      throw new IllegalStateException(failureMessage + ": " + exception.getMessage(), exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(failureMessage + " was interrupted", exception);
    }
  }
}

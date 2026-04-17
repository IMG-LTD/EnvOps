package com.img.envops.modules.app.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LocalPackageStorage {
  private final Path baseDir;

  public LocalPackageStorage(@Value("${envops.storage.local-base-dir:${java.io.tmpdir}/envops-storage}") String baseDir) {
    this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
  }

  public String store(String relativePath, MultipartFile file) {
    if (!StringUtils.hasText(relativePath)) {
      throw new IllegalArgumentException("relativePath is required");
    }

    try {
      Path requestedPath = baseDir.resolve(relativePath).normalize();
      if (!requestedPath.startsWith(baseDir)) {
        throw new IllegalArgumentException("relativePath escapes storage base directory");
      }

      Path targetPath = resolveAvailablePath(requestedPath);
      Path parent = targetPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, targetPath);
      }

      return baseDir.relativize(targetPath).toString().replace('\\', '/');
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to store uploaded package", exception);
    }
  }

  public Path resolve(String relativePath) {
    if (!StringUtils.hasText(relativePath)) {
      throw new IllegalArgumentException("relativePath is required");
    }

    Path resolvedPath = baseDir.resolve(relativePath).normalize();
    if (!resolvedPath.startsWith(baseDir)) {
      throw new IllegalArgumentException("relativePath escapes storage base directory");
    }
    if (!Files.exists(resolvedPath)) {
      throw new IllegalArgumentException("stored package does not exist: " + relativePath);
    }

    return resolvedPath;
  }

  private Path resolveAvailablePath(Path requestedPath) throws IOException {
    if (!Files.exists(requestedPath)) {
      return requestedPath;
    }

    Path parent = requestedPath.getParent();
    String fileName = requestedPath.getFileName().toString();
    int extensionIndex = fileName.lastIndexOf('.');
    String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    String extension = extensionIndex > 0 ? fileName.substring(extensionIndex) : "";

    Path candidate;
    do {
      String candidateName = baseName + "-" + System.nanoTime() + extension;
      candidate = parent == null ? Paths.get(candidateName) : parent.resolve(candidateName);
      candidate = candidate.normalize();
    } while (Files.exists(candidate));

    if (!candidate.startsWith(baseDir)) {
      throw new IllegalArgumentException("relativePath escapes storage base directory");
    }

    return candidate;
  }
}

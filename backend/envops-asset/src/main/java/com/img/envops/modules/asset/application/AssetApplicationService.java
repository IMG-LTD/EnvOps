package com.img.envops.modules.asset.application;

import com.img.envops.modules.asset.infrastructure.mapper.AssetCatalogMapper;
import com.img.envops.modules.asset.infrastructure.mapper.AssetCredentialMapper;
import com.img.envops.modules.asset.infrastructure.mapper.AssetHostMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AssetApplicationService {
  private static final Set<String> SUPPORTED_CREDENTIAL_TYPES = Set.of("ssh_password", "ssh_key", "api_token");

  private final AssetHostMapper assetHostMapper;
  private final AssetCredentialMapper assetCredentialMapper;
  private final AssetCatalogMapper assetCatalogMapper;
  private final CredentialSecretProtector credentialSecretProtector;

  public AssetApplicationService(AssetHostMapper assetHostMapper,
                                 AssetCredentialMapper assetCredentialMapper,
                                 AssetCatalogMapper assetCatalogMapper,
                                 CredentialSecretProtector credentialSecretProtector) {
    this.assetHostMapper = assetHostMapper;
    this.assetCredentialMapper = assetCredentialMapper;
    this.assetCatalogMapper = assetCatalogMapper;
    this.credentialSecretProtector = credentialSecretProtector;
  }

  public HostPage getHosts(Integer current, Integer size) {
    int normalizedCurrent = current == null || current < 1 ? 1 : current;
    int normalizedSize = size == null || size < 1 ? 10 : size;
    int offset = (normalizedCurrent - 1) * normalizedSize;

    List<HostRecord> records = assetHostMapper.findHosts(normalizedSize, offset).stream()
        .map(this::toHostRecord)
        .toList();
    AssetHostMapper.HostSummaryRow summary = assetHostMapper.summarizeHosts();
    long managedHosts = summary == null ? assetHostMapper.countHosts() : summary.getManagedHosts();
    long onlineHosts = summary == null ? 0 : summary.getOnlineHosts();
    long warningHosts = summary == null ? 0 : summary.getWarningHosts();

    return new HostPage(
        normalizedCurrent,
        normalizedSize,
        managedHosts,
        records,
        new HostSummary(managedHosts, onlineHosts, warningHosts));
  }

  public List<CredentialRecord> getCredentials() {
    return assetCredentialMapper.findAll().stream()
        .map(this::toCredentialRecord)
        .toList();
  }

  public CredentialRecord createCredential(CreateCredentialCommand command) {
    validateCreateCredentialCommand(command);

    AssetCredentialMapper.CredentialEntity entity = new AssetCredentialMapper.CredentialEntity();
    entity.setName(command.name().trim());
    entity.setCredentialType(command.credentialType().trim());
    entity.setUsername(trimToNull(command.username()));
    entity.setSecret(credentialSecretProtector.protect(command.secret()));
    entity.setDescription(trimToNull(command.description()));
    entity.setCreatedAt(LocalDateTime.now());

    assetCredentialMapper.insertCredential(entity);

    AssetCredentialMapper.CredentialRow created = assetCredentialMapper.findById(entity.getId());
    if (created == null) {
      throw new IllegalStateException("Failed to create credential");
    }

    return toCredentialRecord(created);
  }

  public HostRecord createHost(CreateHostCommand command) {
    if (command == null
        || !StringUtils.hasText(command.hostName())
        || !StringUtils.hasText(command.ipAddress())
        || !StringUtils.hasText(command.environment())
        || !StringUtils.hasText(command.clusterName())
        || !StringUtils.hasText(command.ownerName())
        || !StringUtils.hasText(command.status())) {
      throw new IllegalArgumentException(
          "hostName, ipAddress, environment, clusterName, ownerName and status are required");
    }

    AssetHostMapper.HostEntity entity = new AssetHostMapper.HostEntity();
    entity.setHostName(command.hostName().trim());
    entity.setIpAddress(command.ipAddress().trim());
    entity.setEnvironment(command.environment().trim());
    entity.setClusterName(command.clusterName().trim());
    entity.setOwnerName(command.ownerName().trim());
    entity.setStatus(command.status().trim());
    entity.setLastHeartbeat(command.lastHeartbeat() == null ? LocalDateTime.now() : command.lastHeartbeat());

    assetHostMapper.insertHost(entity);

    AssetHostMapper.HostRow created = assetHostMapper.findById(entity.getId());
    if (created == null) {
      throw new IllegalStateException("Failed to create host");
    }

    return toHostRecord(created);
  }

  public List<GroupRecord> getGroups() {
    return assetCatalogMapper.findGroups().stream()
        .map(row -> new GroupRecord(row.getId(), row.getName(), row.getDescription(), row.getHostCount()))
        .toList();
  }

  public List<TagRecord> getTags() {
    return assetCatalogMapper.findTags().stream()
        .map(row -> new TagRecord(row.getId(), row.getName(), row.getColor(), row.getDescription()))
        .toList();
  }

  private HostRecord toHostRecord(AssetHostMapper.HostRow row) {
    return new HostRecord(
        row.getId(),
        row.getHostName(),
        row.getIpAddress(),
        row.getEnvironment(),
        row.getClusterName(),
        row.getOwnerName(),
        row.getStatus(),
        row.getLastHeartbeat());
  }

  private CredentialRecord toCredentialRecord(AssetCredentialMapper.CredentialRow row) {
    return new CredentialRecord(
        row.getId(),
        row.getName(),
        row.getCredentialType(),
        row.getUsername(),
        row.getDescription(),
        row.getCreatedAt());
  }

  private void validateCreateCredentialCommand(CreateCredentialCommand command) {
    if (command == null || !StringUtils.hasText(command.name()) || !StringUtils.hasText(command.credentialType())) {
      throw new IllegalArgumentException("name and credentialType are required");
    }

    String credentialType = command.credentialType().trim();
    if (!SUPPORTED_CREDENTIAL_TYPES.contains(credentialType)) {
      throw new IllegalArgumentException("credentialType must be one of ssh_password, ssh_key, api_token");
    }

    if (!StringUtils.hasText(command.secret())) {
      throw new IllegalArgumentException("secret is required");
    }
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim();
  }

  public record HostPage(int current, int size, long total, List<HostRecord> records, HostSummary summary) {
  }

  public record HostSummary(long managedHosts, long onlineHosts, long warningHosts) {
  }

  public record HostRecord(Long id,
                           String hostName,
                           String ipAddress,
                           String environment,
                           String clusterName,
                           String ownerName,
                           String status,
                           LocalDateTime lastHeartbeat) {
  }

  public record CredentialRecord(Long id,
                                 String name,
                                 String credentialType,
                                 String username,
                                 String description,
                                 LocalDateTime createdAt) {
  }

  public record GroupRecord(Long id, String name, String description, Integer hostCount) {
  }

  public record TagRecord(Long id, String name, String color, String description) {
  }

  public record CreateCredentialCommand(String name,
                                        String credentialType,
                                        String username,
                                        String secret,
                                        String description) {
  }

  public record CreateHostCommand(String hostName,
                                  String ipAddress,
                                  String environment,
                                  String clusterName,
                                  String ownerName,
                                  String status,
                                  LocalDateTime lastHeartbeat) {
  }
}

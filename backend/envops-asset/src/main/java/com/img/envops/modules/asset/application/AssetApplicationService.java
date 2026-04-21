package com.img.envops.modules.asset.application;

import com.img.envops.modules.asset.infrastructure.mapper.AssetCatalogMapper;
import com.img.envops.modules.asset.infrastructure.mapper.AssetCredentialMapper;
import com.img.envops.modules.asset.infrastructure.mapper.AssetDatabaseMapper;
import com.img.envops.modules.asset.infrastructure.mapper.AssetHostMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AssetApplicationService {
  private static final Set<String> SUPPORTED_CREDENTIAL_TYPES = Set.of("ssh_password", "ssh_key", "api_token");
  private static final List<String> SUPPORTED_DATABASE_TYPES = List.of("mysql", "postgresql", "oracle", "sqlserver", "mongodb", "redis");
  private static final List<String> SUPPORTED_DATABASE_LIFECYCLE_STATUSES = List.of("managed", "disabled");
  private static final List<String> SUPPORTED_DATABASE_CONNECTIVITY_STATUSES = List.of("unknown", "online", "warning", "offline");

  private final AssetHostMapper assetHostMapper;
  private final AssetCredentialMapper assetCredentialMapper;
  private final AssetCatalogMapper assetCatalogMapper;
  private final AssetDatabaseMapper assetDatabaseMapper;
  private final CredentialSecretProtector credentialSecretProtector;
  private final DatabaseConnectionSecretProtector databaseConnectionSecretProtector;

  public AssetApplicationService(AssetHostMapper assetHostMapper,
                                 AssetCredentialMapper assetCredentialMapper,
                                 AssetCatalogMapper assetCatalogMapper,
                                 AssetDatabaseMapper assetDatabaseMapper,
                                 CredentialSecretProtector credentialSecretProtector,
                                 DatabaseConnectionSecretProtector databaseConnectionSecretProtector) {
    this.assetHostMapper = assetHostMapper;
    this.assetCredentialMapper = assetCredentialMapper;
    this.assetCatalogMapper = assetCatalogMapper;
    this.assetDatabaseMapper = assetDatabaseMapper;
    this.credentialSecretProtector = credentialSecretProtector;
    this.databaseConnectionSecretProtector = databaseConnectionSecretProtector;
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

  public DatabasePage getDatabases(String keyword,
                                   String environment,
                                   String databaseType,
                                   String lifecycleStatus,
                                   String connectivityStatus,
                                   Integer current,
                                   Integer size) {
    int normalizedCurrent = current == null || current < 1 ? 1 : current;
    int normalizedSize = size == null || size < 1 ? 10 : size;
    int offset = (normalizedCurrent - 1) * normalizedSize;
    String normalizedKeyword = trimToNull(keyword);
    String normalizedEnvironment = normalizeLowercase(environment);
    String normalizedDatabaseType = normalizeLowercase(databaseType);
    String normalizedLifecycleStatus = normalizeLowercase(lifecycleStatus);
    String normalizedConnectivityStatus = normalizeLowercase(connectivityStatus);

    long total = assetDatabaseMapper.countDatabasesByQuery(
        normalizedKeyword,
        normalizedEnvironment,
        normalizedDatabaseType,
        normalizedLifecycleStatus,
        normalizedConnectivityStatus);
    List<DatabaseRecord> records = assetDatabaseMapper.findDatabasesByQuery(
            normalizedKeyword,
            normalizedEnvironment,
            normalizedDatabaseType,
            normalizedLifecycleStatus,
            normalizedConnectivityStatus,
            normalizedSize,
            offset)
        .stream()
        .map(this::toDatabaseRecord)
        .toList();
    AssetDatabaseMapper.DatabaseSummaryRow summary =
        assetDatabaseMapper.summarizeDatabasesByQuery(
            normalizedKeyword,
            normalizedEnvironment,
            normalizedDatabaseType,
            normalizedLifecycleStatus,
            normalizedConnectivityStatus);
    long managedDatabases = summary == null ? 0 : summary.getManagedDatabases();
    long warningDatabases = summary == null ? 0 : summary.getWarningDatabases();
    long onlineDatabases = summary == null ? 0 : summary.getOnlineDatabases();

    return new DatabasePage(
        normalizedCurrent,
        normalizedSize,
        total,
        records,
        new DatabaseSummary(managedDatabases, warningDatabases, onlineDatabases));
  }

  public DatabaseRecord createDatabase(CreateDatabaseCommand command) {
    ValidatedDatabaseCommand validated = validateDatabaseCommand(command, null, null);
    LocalDateTime now = LocalDateTime.now();

    AssetDatabaseMapper.DatabaseEntity entity = new AssetDatabaseMapper.DatabaseEntity();
    entity.setDatabaseName(validated.databaseName());
    entity.setDatabaseType(validated.databaseType());
    entity.setEnvironment(validated.environment());
    entity.setHostId(validated.hostId());
    entity.setPort(validated.port());
    entity.setInstanceName(validated.instanceName());
    entity.setCredentialId(validated.credentialId());
    entity.setOwnerName(validated.ownerName());
    entity.setLifecycleStatus(validated.lifecycleStatus());
    entity.setConnectivityStatus(validated.connectivityStatus());
    entity.setConnectionUsername(validated.connectionUsername());
    entity.setConnectionPassword(validated.connectionPassword());
    entity.setDescription(validated.description());
    entity.setLastCheckedAt(validated.lastCheckedAt());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    assetDatabaseMapper.insertDatabase(entity);

    AssetDatabaseMapper.DatabaseRow created = assetDatabaseMapper.findById(entity.getId());
    if (created == null) {
      throw new IllegalStateException("Failed to create database");
    }

    return toDatabaseRecord(created);
  }

  public DatabaseRecord updateDatabase(Long id, UpdateDatabaseCommand command) {
    if (id == null || id < 1) {
      throw new IllegalArgumentException("id is required");
    }

    AssetDatabaseMapper.DatabaseRow existing = requireDatabase(id);
    ValidatedDatabaseCommand validated = validateDatabaseCommand(command, id, existing);

    AssetDatabaseMapper.DatabaseEntity entity = new AssetDatabaseMapper.DatabaseEntity();
    entity.setId(existing.getId());
    entity.setDatabaseName(validated.databaseName());
    entity.setDatabaseType(validated.databaseType());
    entity.setEnvironment(validated.environment());
    entity.setHostId(validated.hostId());
    entity.setPort(validated.port());
    entity.setInstanceName(validated.instanceName());
    entity.setCredentialId(validated.credentialId());
    entity.setOwnerName(validated.ownerName());
    entity.setLifecycleStatus(validated.lifecycleStatus());
    entity.setConnectivityStatus(validated.connectivityStatus());
    entity.setConnectionUsername(validated.connectionUsername());
    entity.setConnectionPassword(validated.connectionPassword());
    entity.setDescription(validated.description());
    entity.setLastCheckedAt(validated.lastCheckedAt());
    entity.setUpdatedAt(LocalDateTime.now());

    assetDatabaseMapper.updateDatabase(entity);

    return toDatabaseRecord(requireDatabase(id));
  }

  public boolean deleteDatabase(Long id) {
    if (id == null || id < 1) {
      throw new IllegalArgumentException("id is required");
    }

    requireDatabase(id);
    return assetDatabaseMapper.deleteById(id) > 0;
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
        row.getLastHeartbeat(),
        Boolean.TRUE.equals(row.getHasMonitorFacts()),
        row.getLatestMonitorFactAt());
  }

  private DatabaseRecord toDatabaseRecord(AssetDatabaseMapper.DatabaseRow row) {
    return new DatabaseRecord(
        row.getId(),
        row.getDatabaseName(),
        row.getDatabaseType(),
        row.getEnvironment(),
        row.getHostId(),
        row.getHostName(),
        row.getPort(),
        row.getInstanceName(),
        row.getCredentialId(),
        row.getCredentialName(),
        row.getOwnerName(),
        row.getLifecycleStatus(),
        row.getConnectivityStatus(),
        row.getConnectionUsername(),
        row.getDescription(),
        row.getLastCheckedAt(),
        row.getCreatedAt(),
        row.getUpdatedAt());
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

  private ValidatedDatabaseCommand validateDatabaseCommand(DatabaseCommand command,
                                                           Long currentId,
                                                           AssetDatabaseMapper.DatabaseRow existing) {
    if (command == null
        || !StringUtils.hasText(command.databaseName())
        || !StringUtils.hasText(command.databaseType())
        || !StringUtils.hasText(command.environment())
        || command.hostId() == null
        || command.hostId() < 1
        || command.port() == null
        || command.port() < 1
        || !StringUtils.hasText(command.ownerName())
        || !StringUtils.hasText(command.lifecycleStatus())
        || !StringUtils.hasText(command.connectivityStatus())) {
      throw new IllegalArgumentException(
          "databaseName, databaseType, environment, hostId, port, ownerName, lifecycleStatus and connectivityStatus are required");
    }

    String databaseType = normalizeLowercase(command.databaseType());
    if (!SUPPORTED_DATABASE_TYPES.contains(databaseType)) {
      throw new IllegalArgumentException("databaseType must be one of " + String.join(", ", SUPPORTED_DATABASE_TYPES));
    }

    String lifecycleStatus = normalizeLowercase(command.lifecycleStatus());
    if (!SUPPORTED_DATABASE_LIFECYCLE_STATUSES.contains(lifecycleStatus)) {
      throw new IllegalArgumentException("lifecycleStatus must be one of " + String.join(", ", SUPPORTED_DATABASE_LIFECYCLE_STATUSES));
    }

    String connectivityStatus = normalizeLowercase(command.connectivityStatus());
    if (!SUPPORTED_DATABASE_CONNECTIVITY_STATUSES.contains(connectivityStatus)) {
      throw new IllegalArgumentException("connectivityStatus must be one of " + String.join(", ", SUPPORTED_DATABASE_CONNECTIVITY_STATUSES));
    }

    AssetHostMapper.HostRow host = assetHostMapper.findById(command.hostId());
    if (host == null) {
      throw new IllegalArgumentException("hostId is invalid");
    }

    Long credentialId = command.credentialId();
    if (credentialId != null) {
      AssetCredentialMapper.CredentialRow credential = assetCredentialMapper.findById(credentialId);
      if (credential == null) {
        throw new IllegalArgumentException("credentialId is invalid");
      }
    }

    String environment = normalizeLowercase(command.environment());
    String databaseName = command.databaseName().trim();
    AssetDatabaseMapper.DatabaseRow duplicated = assetDatabaseMapper.findByUniqueKey(environment, command.hostId(), command.port(), databaseName);
    if (duplicated != null && !duplicated.getId().equals(currentId)) {
      throw new IllegalArgumentException("database already exists on the selected host and port in this environment");
    }

    ConnectionCredentialPair connectionCredentials = resolveConnectionCredentials(command, existing);

    return new ValidatedDatabaseCommand(
        databaseName,
        databaseType,
        environment,
        command.hostId(),
        command.port(),
        trimToNull(command.instanceName()),
        credentialId,
        command.ownerName().trim(),
        lifecycleStatus,
        connectivityStatus,
        connectionCredentials.connectionUsername(),
        connectionCredentials.connectionPassword(),
        trimToNull(command.description()),
        command.lastCheckedAt());
  }

  private ConnectionCredentialPair resolveConnectionCredentials(DatabaseCommand command,
                                                                AssetDatabaseMapper.DatabaseRow existing) {
    String requestedConnectionUsername = trimToNull(command.connectionUsername());
    String rawConnectionPassword = command.connectionPassword();
    String trimmedConnectionPassword = trimToNull(rawConnectionPassword);

    if (existing == null) {
      if (requestedConnectionUsername == null && trimmedConnectionPassword == null) {
        return new ConnectionCredentialPair(null, null);
      }
      if (requestedConnectionUsername == null || trimmedConnectionPassword == null) {
        throw new IllegalArgumentException("connectionUsername and connectionPassword must be provided together");
      }
      return validateConnectionCredentials(
          requestedConnectionUsername,
          databaseConnectionSecretProtector.seal(trimmedConnectionPassword));
    }

    if (requestedConnectionUsername == null && rawConnectionPassword == null) {
      return validateConnectionCredentials(existing.getConnectionUsername(), existing.getConnectionPassword());
    }

    if (rawConnectionPassword != null && trimmedConnectionPassword == null) {
      if (requestedConnectionUsername == null) {
        return validateConnectionCredentials(existing.getConnectionUsername(), existing.getConnectionPassword());
      }
      if (existing.getConnectionPassword() == null) {
        throw new IllegalArgumentException("connectionUsername and connectionPassword must be provided together");
      }
      return validateConnectionCredentials(requestedConnectionUsername, existing.getConnectionPassword());
    }

    if (requestedConnectionUsername == null || trimmedConnectionPassword == null) {
      throw new IllegalArgumentException("connectionUsername and connectionPassword must be provided together");
    }

    return validateConnectionCredentials(
        requestedConnectionUsername,
        databaseConnectionSecretProtector.seal(trimmedConnectionPassword));
  }

  private ConnectionCredentialPair validateConnectionCredentials(String connectionUsername, String connectionPassword) {
    if ((connectionUsername == null) != (connectionPassword == null)) {
      throw new IllegalArgumentException("connectionUsername and connectionPassword must be provided together");
    }
    return new ConnectionCredentialPair(connectionUsername, connectionPassword);
  }

  private AssetDatabaseMapper.DatabaseRow requireDatabase(Long id) {
    AssetDatabaseMapper.DatabaseRow row = assetDatabaseMapper.findById(id);
    if (row == null) {
      throw new IllegalArgumentException("database does not exist");
    }
    return row;
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim();
  }

  private String normalizeLowercase(String value) {
    String normalized = trimToNull(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
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
                           LocalDateTime lastHeartbeat,
                           Boolean hasMonitorFacts,
                           LocalDateTime latestMonitorFactAt) {
  }

  public record DatabasePage(int current, int size, long total, List<DatabaseRecord> records, DatabaseSummary summary) {
  }

  public record DatabaseSummary(long managedDatabases, long warningDatabases, long onlineDatabases) {
  }

  public record DatabaseRecord(Long id,
                               String databaseName,
                               String databaseType,
                               String environment,
                               Long hostId,
                               String hostName,
                               Integer port,
                               String instanceName,
                               Long credentialId,
                               String credentialName,
                               String ownerName,
                               String lifecycleStatus,
                               String connectivityStatus,
                               String connectionUsername,
                               String description,
                               LocalDateTime lastCheckedAt,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
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

  public sealed interface DatabaseCommand permits CreateDatabaseCommand, UpdateDatabaseCommand {
    String databaseName();

    String databaseType();

    String environment();

    Long hostId();

    Integer port();

    String instanceName();

    Long credentialId();

    String ownerName();

    String lifecycleStatus();

    String connectivityStatus();

    String connectionUsername();

    String connectionPassword();

    String description();

    LocalDateTime lastCheckedAt();
  }

  public record CreateDatabaseCommand(String databaseName,
                                      String databaseType,
                                      String environment,
                                      Long hostId,
                                      Integer port,
                                      String instanceName,
                                      Long credentialId,
                                      String ownerName,
                                      String lifecycleStatus,
                                      String connectivityStatus,
                                      String connectionUsername,
                                      String connectionPassword,
                                      String description,
                                      LocalDateTime lastCheckedAt) implements DatabaseCommand {
  }

  public record UpdateDatabaseCommand(String databaseName,
                                      String databaseType,
                                      String environment,
                                      Long hostId,
                                      Integer port,
                                      String instanceName,
                                      Long credentialId,
                                      String ownerName,
                                      String lifecycleStatus,
                                      String connectivityStatus,
                                      String connectionUsername,
                                      String connectionPassword,
                                      String description,
                                      LocalDateTime lastCheckedAt) implements DatabaseCommand {
  }

  public record ValidatedDatabaseCommand(String databaseName,
                                         String databaseType,
                                         String environment,
                                         Long hostId,
                                         Integer port,
                                         String instanceName,
                                         Long credentialId,
                                         String ownerName,
                                         String lifecycleStatus,
                                         String connectivityStatus,
                                         String connectionUsername,
                                         String connectionPassword,
                                         String description,
                                         LocalDateTime lastCheckedAt) {
  }

  private record ConnectionCredentialPair(String connectionUsername, String connectionPassword) {
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

# EnvOps Database Connectivity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real database connectivity checks to the EnvOps database asset page, including single check, batch check on selected rows/current page/all filtered rows, and automatic `online` / `offline` snapshot updates.

**Architecture:** Keep the feature inside the existing asset module. Extend `asset_database` with direct connection credentials, add a reversible secret protector for database passwords, add one orchestration service plus per-database checkers, and wire the existing database asset page to the new APIs. Use synchronous request/response execution and return detailed batch results without adding an async task system.

**Tech Stack:** Spring Boot 3.3, MyBatis, JUnit 5, Mockito, JDBC drivers for MySQL/PostgreSQL/Oracle/SQL Server, MongoDB Java sync driver, Jedis, Vue 3, TypeScript, Naive UI, Vitest

---

## File structure

### Backend files

- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectionSecretProtector.java`
  - Reversible encryption for database asset passwords. Keep `CredentialSecretProtector` unchanged because it is one-way and cannot support runtime connection checks.
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectivityService.java`
  - Orchestrates single and batch checks, aggregates results, updates `connectivity_status` and `last_checked_at`.
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityChecker.java`
  - Common checker contract.
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityTarget.java`
  - Normalized runtime target for one check.
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityProbeResult.java`
  - Success/failure payload from checker implementations.
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectionFactory.java`
  - Resolves checker by `databaseType`.
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/AbstractJdbcConnectivityChecker.java`
  - Shared JDBC connect-and-validate helper for the four relational databases.
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/MySqlConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/PostgreSqlConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/OracleConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/SqlServerConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/MongoDbConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/RedisConnectivityChecker.java`
- Create: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectionSecretProtectorTest.java`
- Create: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java`
- Modify: `backend/envops-asset/pom.xml`
  - Add driver dependencies and test dependencies.
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/AssetApplicationService.java`
  - Extend database CRUD contracts with `connectionUsername` and `connectionPassword`, preserve old password on blank update, and expose `connectionUsername` in `DatabaseRecord`.
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/interfaces/AssetController.java`
  - Add connectivity-check endpoints and extend create/update request bodies.
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/infrastructure/mapper/AssetDatabaseMapper.java`
  - Persist new connection fields, include host IP for runtime checks, fetch rows by IDs/query, update connectivity snapshot.
- Modify: `backend/envops-boot/src/main/resources/schema.sql`
- Modify: `backend/envops-boot/src/test/resources/schema.sql`
  - Add `connection_username` and `connection_password` columns to `asset_database`.
- Modify: `backend/envops-boot/src/main/resources/data.sql`
- Modify: `backend/envops-boot/src/test/resources/data.sql`
  - Seed connection usernames and sealed passwords for database rows.
- Modify: `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`
  - Add integration tests for create/update credential fields and new connectivity-check endpoints.

### Frontend files

- Create: `frontend/src/views/asset/database-connectivity.spec.ts`
  - Page behavior test for row check, selected check, current-page check, query check, and password form semantics.
- Modify: `frontend/src/service/api/asset.ts`
  - Add single and batch connectivity-check API functions.
- Modify: `frontend/src/typings/api/asset.d.ts`
  - Add connection fields to database create/update payloads, add connectivity-check response types.
- Modify: `frontend/src/typings/app.d.ts`
  - Extend locale typing for new form fields, buttons, modal copy, and messages.
- Modify: `frontend/src/views/asset/database/index.vue`
  - Add connection credential fields, row action button, batch buttons, row selection, result modal, and new request flow.
- Modify: `frontend/src/views/asset/database-contract.spec.ts`
  - Assert the new API/types/locale keys exist and the page source references the connectivity-check APIs.
- Modify: `frontend/src/locales/langs/zh-cn.ts`
- Modify: `frontend/src/locales/langs/en-us.ts`
  - Update hero/tag wording and add new labels/messages.

### Docs files

- Modify: `README.md`
- Modify: `docs/envops-项目详细说明.md`
- Modify: `docs/envops-开发技术说明.md`
- Modify: `docs/envops-用户操作手册.md`
- Modify: `release/0.0.4-release-notes.md`
  - Update the scope from manual-only connectivity status to real connectivity checks with explicit boundaries.

---

### Task 1: Extend database asset persistence for connection credentials

**Files:**
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectionSecretProtector.java`
- Create: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectionSecretProtectorTest.java`
- Modify: `backend/envops-asset/pom.xml`
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/AssetApplicationService.java`
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/interfaces/AssetController.java`
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/infrastructure/mapper/AssetDatabaseMapper.java`
- Modify: `backend/envops-boot/src/main/resources/schema.sql`
- Modify: `backend/envops-boot/src/test/resources/schema.sql`
- Modify: `backend/envops-boot/src/main/resources/data.sql`
- Modify: `backend/envops-boot/src/test/resources/data.sql`
- Test: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectionSecretProtectorTest.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`

- [ ] **Step 1: Write the failing tests for reversible password storage and CRUD payload fields**

Add the reversible secret round-trip test:

```java
package com.img.envops.modules.asset.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectionSecretProtectorTest {
  @Test
  void sealAndRevealRoundTripUsesReversibleCiphertext() {
    DatabaseConnectionSecretProtector protector =
        new DatabaseConnectionSecretProtector("test-only-envops-credential-protection-secret-12345");

    String sealed = protector.seal("Orders@123456");

    assertThat(sealed)
        .startsWith("sealed:v1:")
        .isNotEqualTo("Orders@123456");
    assertThat(protector.reveal(sealed)).isEqualTo("Orders@123456");
  }
}
```

Add the integration test that proves the API accepts `connectionUsername`, never returns `connectionPassword`, and persists an encrypted value:

```java
@Test
void createDatabasePersistsConnectionUsernameAndSealedPassword() throws Exception {
  String accessToken = login();

  mockMvc.perform(post("/api/assets/databases")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "databaseName": "mysql_detectable",
                "databaseType": "mysql",
                "environment": "staging",
                "hostId": 3,
                "port": 3308,
                "instanceName": "mysql-stg-detect",
                "credentialId": 2,
                "ownerName": "Platform DBA",
                "lifecycleStatus": "managed",
                "connectivityStatus": "unknown",
                "connectionUsername": "orders_app",
                "connectionPassword": "Orders@123456"
              }
              """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data.connectionUsername").value("orders_app"))
      .andExpect(jsonPath("$.data.connectionPassword").doesNotExist());

  String stored = jdbcTemplate.queryForObject(
      "SELECT connection_password FROM asset_database WHERE database_name = ?",
      String.class,
      "mysql_detectable");

  org.assertj.core.api.Assertions.assertThat(stored)
      .startsWith("sealed:v1:")
      .isNotEqualTo("Orders@123456");
}
```

Add the update test that proves blank password preserves the stored ciphertext:

```java
@Test
void updateDatabaseKeepsExistingConnectionPasswordWhenBlank() throws Exception {
  String accessToken = login();

  mockMvc.perform(post("/api/assets/databases")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "databaseName": "mysql_keep_password",
                "databaseType": "mysql",
                "environment": "staging",
                "hostId": 3,
                "port": 3309,
                "instanceName": "mysql-stg-keep",
                "credentialId": 2,
                "ownerName": "Platform DBA",
                "lifecycleStatus": "managed",
                "connectivityStatus": "unknown",
                "connectionUsername": "orders_app",
                "connectionPassword": "Orders@123456"
              }
              """))
      .andExpect(status().isOk());

  Long databaseId = jdbcTemplate.queryForObject(
      "SELECT id FROM asset_database WHERE database_name = ?",
      Long.class,
      "mysql_keep_password");
  String before = jdbcTemplate.queryForObject(
      "SELECT connection_password FROM asset_database WHERE id = ?",
      String.class,
      databaseId);

  mockMvc.perform(put("/api/assets/databases/{id}", databaseId)
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "databaseName": "mysql_keep_password",
                "databaseType": "mysql",
                "environment": "staging",
                "hostId": 3,
                "port": 3309,
                "instanceName": "mysql-stg-keep",
                "credentialId": 2,
                "ownerName": "Platform DBA",
                "lifecycleStatus": "managed",
                "connectivityStatus": "unknown",
                "connectionUsername": "orders_app_2",
                "connectionPassword": ""
              }
              """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.connectionUsername").value("orders_app_2"));

  String after = jdbcTemplate.queryForObject(
      "SELECT connection_password FROM asset_database WHERE id = ?",
      String.class,
      databaseId);

  org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before);
}
```

- [ ] **Step 2: Run the targeted tests to verify they fail**

Run:

```bash
mvn -f backend/pom.xml -pl envops-asset -Dtest=DatabaseConnectionSecretProtectorTest test
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AssetControllerTest#createDatabasePersistsConnectionUsernameAndSealedPassword+updateDatabaseKeepsExistingConnectionPasswordWhenBlank test
```

Expected:
- `DatabaseConnectionSecretProtectorTest` fails because the new class does not exist.
- `AssetControllerTest` fails because `connectionUsername` is not in the response and `connection_password` is not in the schema.

- [ ] **Step 3: Implement reversible storage, schema changes, and CRUD contract fields**

Add the new reversible protector instead of modifying `CredentialSecretProtector`:

```java
package com.img.envops.modules.asset.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DatabaseConnectionSecretProtector {
  private static final String PREFIX = "sealed:v1:";
  private static final String CIPHER = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final byte[] aesKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public DatabaseConnectionSecretProtector(
      @Value("${envops.security.credential-protection-secret}") String protectionSecret) {
    this.aesKey = deriveKey(protectionSecret);
  }

  DatabaseConnectionSecretProtector(String protectionSecret) {
    this.aesKey = deriveKey(protectionSecret);
  }

  public String seal(String rawSecret) {
    if (!StringUtils.hasText(rawSecret)) {
      throw new IllegalArgumentException("connectionPassword is required");
    }

    try {
      byte[] iv = new byte[IV_LENGTH];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] encrypted = cipher.doFinal(rawSecret.trim().getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
      return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to seal database connection password", exception);
    }
  }

  public String reveal(String sealedSecret) {
    if (!StringUtils.hasText(sealedSecret) || !sealedSecret.startsWith(PREFIX)) {
      throw new IllegalArgumentException("connectionPassword is invalid");
    }

    try {
      byte[] payload = Base64.getUrlDecoder().decode(sealedSecret.substring(PREFIX.length()));
      byte[] iv = new byte[IV_LENGTH];
      byte[] encrypted = new byte[payload.length - IV_LENGTH];
      System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
      System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to reveal database connection password", exception);
    }
  }

  private byte[] deriveKey(String protectionSecret) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(protectionSecret.trim().getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to derive database connection key", exception);
    }
  }
}
```

Extend `asset_database` in both schema files:

```sql
ALTER TABLE asset_database ADD COLUMN connection_username VARCHAR(128);
ALTER TABLE asset_database ADD COLUMN connection_password VARCHAR(512);
```

The actual `CREATE TABLE asset_database` block should become:

```sql
CREATE TABLE asset_database (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    database_name VARCHAR(128) NOT NULL,
    database_type VARCHAR(32) NOT NULL,
    environment VARCHAR(32) NOT NULL,
    host_id BIGINT NOT NULL,
    port INT NOT NULL,
    instance_name VARCHAR(128),
    credential_id BIGINT,
    owner_name VARCHAR(128) NOT NULL,
    lifecycle_status VARCHAR(32) NOT NULL,
    connectivity_status VARCHAR(32) NOT NULL,
    connection_username VARCHAR(128),
    connection_password VARCHAR(512),
    description VARCHAR(255),
    last_checked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_asset_database_env_host_port_name UNIQUE (environment, host_id, port, database_name),
    CONSTRAINT fk_asset_database_host FOREIGN KEY (host_id) REFERENCES asset_host (id),
    CONSTRAINT fk_asset_database_credential FOREIGN KEY (credential_id) REFERENCES asset_credential (id)
);
```

Update `AssetDatabaseMapper` entity, row, select, insert, and update columns to include:

```java
"       db.connection_username AS connectionUsername,",
"       db.connection_password AS connectionPassword,",
```

and:

```java
@Insert("""
    INSERT INTO asset_database (
      database_name,
      database_type,
      environment,
      host_id,
      port,
      instance_name,
      credential_id,
      owner_name,
      lifecycle_status,
      connectivity_status,
      connection_username,
      connection_password,
      description,
      last_checked_at,
      created_at,
      updated_at
    )
    VALUES (
      #{databaseName},
      #{databaseType},
      #{environment},
      #{hostId},
      #{port},
      #{instanceName},
      #{credentialId},
      #{ownerName},
      #{lifecycleStatus},
      #{connectivityStatus},
      #{connectionUsername},
      #{connectionPassword},
      #{description},
      #{lastCheckedAt},
      #{createdAt},
      #{updatedAt}
    )
    """)
```

In `AssetApplicationService`, extend the database record and commands:

```java
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
```

Store a sealed password on create, and preserve the old sealed password on update when the incoming password is blank:

```java
String connectionUsername = trimToNull(command.connectionUsername());
String sealedConnectionPassword = resolveConnectionPassword(
    trimToNull(command.connectionPassword()),
    existing == null ? null : existing.getConnectionPassword());

if ((connectionUsername == null) != (sealedConnectionPassword == null)) {
  throw new IllegalArgumentException("connectionUsername and connectionPassword must be provided together");
}
```

with:

```java
private String resolveConnectionPassword(String rawConnectionPassword, String existingConnectionPassword) {
  if (rawConnectionPassword != null) {
    return databaseConnectionSecretProtector.seal(rawConnectionPassword);
  }
  return existingConnectionPassword;
}
```

Extend `AssetController` request records and command construction:

```java
public record CreateDatabaseRequest(String databaseName,
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
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCheckedAt) {
}
```

Seed both `data.sql` files with usernames and sealed passwords that match the main/test protection secret:

```sql
INSERT INTO asset_database (
    database_name,
    database_type,
    environment,
    host_id,
    port,
    instance_name,
    credential_id,
    owner_name,
    lifecycle_status,
    connectivity_status,
    connection_username,
    connection_password,
    description,
    last_checked_at,
    created_at,
    updated_at
)
VALUES
    ('order_prod', 'mysql', 'production', 1, 3306, 'mysql-prd-a', 1, 'Platform DBA', 'managed', 'online', 'orders_app', 'sealed:v1:AAECAwQFBgcICQoLTKCsbFniikAG4U1bagXGI0YxWptS3soGkARmBx0', '订单主库生产实例', TIMESTAMP '2026-04-18 09:10:00', TIMESTAMP '2026-04-14 10:00:00', TIMESTAMP '2026-04-18 09:10:00');

-- For `backend/envops-boot/src/test/resources/data.sql`, use the ciphertext generated from the test secret:
-- 'sealed:v1:DA0ODxAREhMUFRYXrutM6GT44Wdz4AuN399AIyxM5Pjjtq_XGXg9PX8'
```

Add the test dependency to `backend/envops-asset/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 4: Run the targeted tests again to verify they pass**

Run:

```bash
mvn -f backend/pom.xml -pl envops-asset -Dtest=DatabaseConnectionSecretProtectorTest test
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AssetControllerTest#createDatabasePersistsConnectionUsernameAndSealedPassword+updateDatabaseKeepsExistingConnectionPasswordWhenBlank test
```

Expected:
- Both commands exit `0`.
- The controller test confirms `connectionUsername` is returned and `connectionPassword` is still omitted.

- [ ] **Step 5: Commit this slice if commits are authorized for this batch**

```bash
git add backend/envops-asset/pom.xml \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectionSecretProtector.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/AssetApplicationService.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/interfaces/AssetController.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/infrastructure/mapper/AssetDatabaseMapper.java \
  backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectionSecretProtectorTest.java \
  backend/envops-boot/src/main/resources/schema.sql \
  backend/envops-boot/src/main/resources/data.sql \
  backend/envops-boot/src/test/resources/schema.sql \
  backend/envops-boot/src/test/resources/data.sql \
  backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java && \
git commit -m "$(cat <<'EOF'
feat: store database connection credentials on asset records
EOF
)"
```

### Task 2: Add the connectivity-check domain service and checker implementations

**Files:**
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectivityService.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityTarget.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityProbeResult.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectionFactory.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/AbstractJdbcConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/MySqlConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/PostgreSqlConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/OracleConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/SqlServerConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/MongoDbConnectivityChecker.java`
- Create: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/RedisConnectivityChecker.java`
- Create: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java`
- Modify: `backend/envops-asset/pom.xml`
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/infrastructure/mapper/AssetDatabaseMapper.java`
- Test: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java`

- [ ] **Step 1: Write the failing unit test for single and batch execution semantics**

Create `DatabaseConnectivityServiceTest.java` with fake checkers and mocked mapper/protector behavior:

```java
package com.img.envops.modules.asset.application;

import com.img.envops.modules.asset.application.connectivity.DatabaseConnectionFactory;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityChecker;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityProbeResult;
import com.img.envops.modules.asset.application.connectivity.DatabaseConnectivityTarget;
import com.img.envops.modules.asset.infrastructure.mapper.AssetDatabaseMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseConnectivityServiceTest {
  @Test
  void checkSelectedDatabasesAggregatesSuccessFailureAndSkipped() {
    AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
    DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
    DatabaseConnectivityChecker mysqlChecker = new StubChecker("mysql", true, "connected");
    DatabaseConnectivityChecker postgresqlChecker = new StubChecker("postgresql", false, "认证失败");
    DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(mysqlChecker, postgresqlChecker));
    DatabaseConnectivityService service = new DatabaseConnectivityService(mapper, factory, secretProtector);

    AssetDatabaseMapper.DatabaseRow mysql = databaseRow(11L, "order_prod", "mysql", "10.20.1.11", 3306, "orders_app", "sealed:mysql");
    AssetDatabaseMapper.DatabaseRow postgres = databaseRow(12L, "traffic_gate", "postgresql", "10.20.1.12", 5432, "traffic_app", "sealed:pg");
    AssetDatabaseMapper.DatabaseRow missing = databaseRow(13L, "session_hub", "redis", "10.20.1.13", 6379, null, null);

    when(mapper.findDatabasesByIds(List.of(11L, 12L, 13L))).thenReturn(List.of(mysql, postgres, missing));
    when(secretProtector.reveal("sealed:mysql")).thenReturn("Orders@123456");
    when(secretProtector.reveal("sealed:pg")).thenReturn("Traffic@123456");

    DatabaseConnectivityService.DatabaseConnectivityReport report = service.checkSelectedDatabases(List.of(11L, 12L, 13L));

    assertThat(report.summary().total()).isEqualTo(3);
    assertThat(report.summary().success()).isEqualTo(1);
    assertThat(report.summary().failed()).isEqualTo(1);
    assertThat(report.summary().skipped()).isEqualTo(1);
    assertThat(report.results()).extracting(DatabaseConnectivityService.DatabaseConnectivityItem::message)
        .contains("connected", "认证失败", "缺少连接用户名或密码");

    verify(mapper).updateConnectivitySnapshot(eq(11L), eq("online"), any(LocalDateTime.class));
    verify(mapper).updateConnectivitySnapshot(eq(12L), eq("offline"), any(LocalDateTime.class));
  }

  private static AssetDatabaseMapper.DatabaseRow databaseRow(Long id,
                                                             String databaseName,
                                                             String databaseType,
                                                             String hostIpAddress,
                                                             Integer port,
                                                             String connectionUsername,
                                                             String connectionPassword) {
    AssetDatabaseMapper.DatabaseRow row = new AssetDatabaseMapper.DatabaseRow();
    row.setId(id);
    row.setDatabaseName(databaseName);
    row.setDatabaseType(databaseType);
    row.setHostIpAddress(hostIpAddress);
    row.setPort(port);
    row.setConnectionUsername(connectionUsername);
    row.setConnectionPassword(connectionPassword);
    row.setEnvironment("production");
    row.setInstanceName(databaseName);
    return row;
  }

  private record StubChecker(String databaseType, boolean success, String message) implements DatabaseConnectivityChecker {
    @Override
    public String databaseType() {
      return databaseType;
    }

    @Override
    public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
      return success ? DatabaseConnectivityProbeResult.success(message) : DatabaseConnectivityProbeResult.failure(message);
    }
  }
}
```

- [ ] **Step 2: Run the unit test to verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-asset -Dtest=DatabaseConnectivityServiceTest test
```

Expected:
- The build fails because `DatabaseConnectivityService`, `DatabaseConnectionFactory`, and the new mapper methods do not exist yet.

- [ ] **Step 3: Implement the service, factory, mapper helpers, and concrete checkers**

Add the checker contract and target records:

```java
package com.img.envops.modules.asset.application.connectivity;

public interface DatabaseConnectivityChecker {
  String databaseType();

  DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target);
}
```

```java
package com.img.envops.modules.asset.application.connectivity;

public record DatabaseConnectivityTarget(Long databaseId,
                                         String databaseName,
                                         String databaseType,
                                         String environment,
                                         String hostIpAddress,
                                         Integer port,
                                         String instanceName,
                                         String connectionUsername,
                                         String connectionPassword) {
}
```

```java
package com.img.envops.modules.asset.application.connectivity;

public record DatabaseConnectivityProbeResult(boolean success, String message) {
  public static DatabaseConnectivityProbeResult success(String message) {
    return new DatabaseConnectivityProbeResult(true, message);
  }

  public static DatabaseConnectivityProbeResult failure(String message) {
    return new DatabaseConnectivityProbeResult(false, message);
  }
}
```

Create a factory keyed by `databaseType`:

```java
@Component
public class DatabaseConnectionFactory {
  private final Map<String, DatabaseConnectivityChecker> checkers;

  public DatabaseConnectionFactory(List<DatabaseConnectivityChecker> checkers) {
    this.checkers = checkers.stream().collect(Collectors.toUnmodifiableMap(DatabaseConnectivityChecker::databaseType, Function.identity()));
  }

  public DatabaseConnectivityChecker getChecker(String databaseType) {
    DatabaseConnectivityChecker checker = checkers.get(databaseType);
    if (checker == null) {
      throw new IllegalArgumentException("Unsupported databaseType for connectivity check: " + databaseType);
    }
    return checker;
  }
}
```

Implement the orchestration service with nested response records so the controller can return it directly:

```java
@Service
public class DatabaseConnectivityService {
  private final AssetDatabaseMapper assetDatabaseMapper;
  private final DatabaseConnectionFactory databaseConnectionFactory;
  private final DatabaseConnectionSecretProtector databaseConnectionSecretProtector;

  public DatabaseConnectivityService(AssetDatabaseMapper assetDatabaseMapper,
                                     DatabaseConnectionFactory databaseConnectionFactory,
                                     DatabaseConnectionSecretProtector databaseConnectionSecretProtector) {
    this.assetDatabaseMapper = assetDatabaseMapper;
    this.databaseConnectionFactory = databaseConnectionFactory;
    this.databaseConnectionSecretProtector = databaseConnectionSecretProtector;
  }

  public DatabaseConnectivityReport checkOneDatabase(Long id) {
    return run(assetDatabaseMapper.findDatabasesByIds(List.of(id)));
  }

  public DatabaseConnectivityReport checkSelectedDatabases(List<Long> ids) {
    return run(assetDatabaseMapper.findDatabasesByIds(ids));
  }

  public DatabaseConnectivityReport checkDatabasesByQuery(String keyword,
                                                          String environment,
                                                          String databaseType,
                                                          String lifecycleStatus,
                                                          String connectivityStatus) {
    return run(assetDatabaseMapper.findAllDatabasesByQuery(keyword, environment, databaseType, lifecycleStatus, connectivityStatus));
  }

  private DatabaseConnectivityReport run(List<AssetDatabaseMapper.DatabaseRow> rows) {
    List<DatabaseConnectivityItem> results = rows.stream().map(this::checkRow).toList();
    long success = results.stream().filter(item -> item.status().equals("success")).count();
    long failed = results.stream().filter(item -> item.status().equals("failed")).count();
    long skipped = results.stream().filter(item -> item.status().equals("skipped")).count();
    return new DatabaseConnectivityReport(new DatabaseConnectivitySummary(results.size(), success, failed, skipped), results);
  }

  private DatabaseConnectivityItem checkRow(AssetDatabaseMapper.DatabaseRow row) {
    if (row.getConnectionUsername() == null || row.getConnectionPassword() == null) {
      return DatabaseConnectivityItem.skipped(row.getId(), row.getDatabaseName(), row.getDatabaseType(), row.getEnvironment(), "缺少连接用户名或密码");
    }

    LocalDateTime checkedAt = LocalDateTime.now();
    String rawPassword = databaseConnectionSecretProtector.reveal(row.getConnectionPassword());
    DatabaseConnectivityTarget target = new DatabaseConnectivityTarget(
        row.getId(),
        row.getDatabaseName(),
        row.getDatabaseType(),
        row.getEnvironment(),
        row.getHostIpAddress(),
        row.getPort(),
        row.getInstanceName(),
        row.getConnectionUsername(),
        rawPassword);
    DatabaseConnectivityProbeResult probeResult = databaseConnectionFactory.getChecker(row.getDatabaseType()).check(target);
    String snapshot = probeResult.success() ? "online" : "offline";
    assetDatabaseMapper.updateConnectivitySnapshot(row.getId(), snapshot, checkedAt);
    return new DatabaseConnectivityItem(row.getId(), row.getDatabaseName(), row.getDatabaseType(), row.getEnvironment(), probeResult.success() ? "success" : "failed", probeResult.message(), snapshot, checkedAt);
  }

  public record DatabaseConnectivityReport(DatabaseConnectivitySummary summary, List<DatabaseConnectivityItem> results) {
  }

  public record DatabaseConnectivitySummary(long total, long success, long failed, long skipped) {
  }

  public record DatabaseConnectivityItem(Long databaseId,
                                         String databaseName,
                                         String databaseType,
                                         String environment,
                                         String status,
                                         String message,
                                         String connectivityStatus,
                                         LocalDateTime checkedAt) {
    static DatabaseConnectivityItem skipped(Long databaseId,
                                            String databaseName,
                                            String databaseType,
                                            String environment,
                                            String message) {
      return new DatabaseConnectivityItem(databaseId, databaseName, databaseType, environment, "skipped", message, "unknown", null);
    }
  }
}
```

Add mapper support for host IP, ID lists, all-query fetch, and snapshot updates:

```java
@Select({
    "<script>",
    "SELECT db.id,",
    "       db.database_name AS databaseName,",
    "       db.database_type AS databaseType,",
    "       db.environment,",
    "       db.host_id AS hostId,",
    "       host.host_name AS hostName,",
    "       host.ip_address AS hostIpAddress,",
    "       db.port,",
    "       db.instance_name AS instanceName,",
    "       db.credential_id AS credentialId,",
    "       credential.name AS credentialName,",
    "       db.owner_name AS ownerName,",
    "       db.lifecycle_status AS lifecycleStatus,",
    "       db.connectivity_status AS connectivityStatus,",
    "       db.connection_username AS connectionUsername,",
    "       db.connection_password AS connectionPassword,",
    "       db.description,",
    "       db.last_checked_at AS lastCheckedAt,",
    "       db.created_at AS createdAt,",
    "       db.updated_at AS updatedAt",
    "FROM asset_database db",
    "LEFT JOIN asset_host host ON host.id = db.host_id",
    "LEFT JOIN asset_credential credential ON credential.id = db.credential_id",
    "WHERE db.id IN",
    "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
    "ORDER BY db.id DESC",
    "</script>"
})
List<DatabaseRow> findDatabasesByIds(@Param("ids") List<Long> ids);

@Update("""
    UPDATE asset_database
    SET connectivity_status = #{connectivityStatus},
        last_checked_at = #{lastCheckedAt},
        updated_at = #{lastCheckedAt}
    WHERE id = #{id}
    """)
int updateConnectivitySnapshot(@Param("id") Long id,
                               @Param("connectivityStatus") String connectivityStatus,
                               @Param("lastCheckedAt") LocalDateTime lastCheckedAt);
```

Add the runtime dependencies in `backend/envops-asset/pom.xml`:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
</dependency>
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

Use a shared JDBC checker helper:

```java
abstract class AbstractJdbcConnectivityChecker implements DatabaseConnectivityChecker {
  protected abstract String jdbcUrl(DatabaseConnectivityTarget target);

  protected String validationSql() {
    return "SELECT 1";
  }

  @Override
  public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
    try (Connection connection = DriverManager.getConnection(jdbcUrl(target), target.connectionUsername(), target.connectionPassword());
         Statement statement = connection.createStatement()) {
      statement.setQueryTimeout(5);
      statement.execute(validationSql());
      return DatabaseConnectivityProbeResult.success("connected");
    } catch (SQLException exception) {
      return DatabaseConnectivityProbeResult.failure(resolveMessage(exception));
    }
  }

  protected String resolveMessage(SQLException exception) {
    String message = exception.getMessage();
    if (message != null && message.toLowerCase(Locale.ROOT).contains("timeout")) {
      return "连接超时";
    }
    if (message != null && message.toLowerCase(Locale.ROOT).contains("login")) {
      return "认证失败";
    }
    return "数据库拒绝连接";
  }
}
```

Concrete checker examples:

```java
@Component
public class MySqlConnectivityChecker extends AbstractJdbcConnectivityChecker {
  @Override
  public String databaseType() {
    return "mysql";
  }

  @Override
  protected String jdbcUrl(DatabaseConnectivityTarget target) {
    return "jdbc:mysql://%s:%d/%s?connectTimeout=5000&socketTimeout=5000&sslMode=DISABLED"
        .formatted(target.hostIpAddress(), target.port(), target.databaseName());
  }
}
```

```java
@Component
public class OracleConnectivityChecker extends AbstractJdbcConnectivityChecker {
  @Override
  public String databaseType() {
    return "oracle";
  }

  @Override
  protected String jdbcUrl(DatabaseConnectivityTarget target) {
    String serviceName = target.instanceName() == null || target.instanceName().isBlank() ? target.databaseName() : target.instanceName();
    return "jdbc:oracle:thin:@//%s:%d/%s".formatted(target.hostIpAddress(), target.port(), serviceName);
  }

  @Override
  protected String validationSql() {
    return "SELECT 1 FROM DUAL";
  }
}
```

```java
@Component
public class MongoDbConnectivityChecker implements DatabaseConnectivityChecker {
  @Override
  public String databaseType() {
    return "mongodb";
  }

  @Override
  public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
    String connectionString = "mongodb://%s:%s@%s:%d/%s?serverSelectionTimeoutMS=5000"
        .formatted(URLEncoder.encode(target.connectionUsername(), StandardCharsets.UTF_8),
            URLEncoder.encode(target.connectionPassword(), StandardCharsets.UTF_8),
            target.hostIpAddress(),
            target.port(),
            target.databaseName());
    try (MongoClient client = MongoClients.create(connectionString)) {
      client.getDatabase(target.databaseName()).runCommand(new Document("ping", 1));
      return DatabaseConnectivityProbeResult.success("connected");
    } catch (Exception exception) {
      return DatabaseConnectivityProbeResult.failure("认证失败");
    }
  }
}
```

```java
@Component
public class RedisConnectivityChecker implements DatabaseConnectivityChecker {
  @Override
  public String databaseType() {
    return "redis";
  }

  @Override
  public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
        .user(target.connectionUsername())
        .password(target.connectionPassword())
        .timeoutMillis(5000)
        .build();
    try (JedisPooled jedis = new JedisPooled(new HostAndPort(target.hostIpAddress(), target.port()), config)) {
      jedis.ping();
      return DatabaseConnectivityProbeResult.success("connected");
    } catch (Exception exception) {
      return DatabaseConnectivityProbeResult.failure("认证失败");
    }
  }
}
```

- [ ] **Step 4: Run the unit test again to verify it passes**

Run:

```bash
mvn -f backend/pom.xml -pl envops-asset -Dtest=DatabaseConnectivityServiceTest test
```

Expected:
- The command exits `0`.
- The service test proves success, failure, and skipped records are aggregated correctly.

- [ ] **Step 5: Commit this slice if commits are authorized for this batch**

```bash
git add backend/envops-asset/pom.xml \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectivityService.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityChecker.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityTarget.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectivityProbeResult.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/DatabaseConnectionFactory.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/AbstractJdbcConnectivityChecker.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/MySqlConnectivityChecker.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/PostgreSqlConnectivityChecker.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/OracleConnectivityChecker.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/SqlServerConnectivityChecker.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/MongoDbConnectivityChecker.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/connectivity/RedisConnectivityChecker.java \
  backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java \
  backend/envops-asset/src/main/java/com/img/envops/modules/asset/infrastructure/mapper/AssetDatabaseMapper.java && \
git commit -m "$(cat <<'EOF'
feat: add database connectivity check orchestration
EOF
)"
```

### Task 3: Expose connectivity-check endpoints and integration contracts

**Files:**
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/interfaces/AssetController.java`
- Modify: `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`

- [ ] **Step 1: Write the failing controller tests for single, selected, page, and query checks**

Add a mocked connectivity-check response shape to `AssetControllerTest` and assert the new endpoints:

```java
@MockBean
private DatabaseConnectivityService databaseConnectivityService;

@Test
void checkDatabaseConnectivityReturnsSingleResult() throws Exception {
  String accessToken = login();
  DatabaseConnectivityService.DatabaseConnectivityReport report =
      new DatabaseConnectivityService.DatabaseConnectivityReport(
          new DatabaseConnectivityService.DatabaseConnectivitySummary(1, 1, 0, 0),
          List.of(new DatabaseConnectivityService.DatabaseConnectivityItem(
              6L,
              "session_hub",
              "redis",
              "sandbox",
              "success",
              "connected",
              "online",
              LocalDateTime.parse("2026-04-21T10:15:00"))));
  when(databaseConnectivityService.checkOneDatabase(6L)).thenReturn(report);

  mockMvc.perform(post("/api/assets/databases/{id}/connectivity-check", 6L)
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data.summary.total").value(1))
      .andExpect(jsonPath("$.data.results[0].databaseName").value("session_hub"))
      .andExpect(jsonPath("$.data.results[0].status").value("success"))
      .andExpect(jsonPath("$.data.results[0].connectivityStatus").value("online"));
}
```

Add one batch endpoint test and reuse the same response shape for `selected`, `page`, and `query`:

```java
@Test
void checkDatabaseConnectivityBySelectedRowsReturnsBatchReport() throws Exception {
  String accessToken = login();
  DatabaseConnectivityService.DatabaseConnectivityReport report =
      new DatabaseConnectivityService.DatabaseConnectivityReport(
          new DatabaseConnectivityService.DatabaseConnectivitySummary(2, 1, 0, 1),
          List.of(
              new DatabaseConnectivityService.DatabaseConnectivityItem(1L, "order_prod", "mysql", "production", "success", "connected", "online", LocalDateTime.parse("2026-04-21T10:15:00")),
              new DatabaseConnectivityService.DatabaseConnectivityItem(2L, "traffic_gate", "postgresql", "production", "skipped", "缺少连接用户名或密码", "unknown", null)));
  when(databaseConnectivityService.checkSelectedDatabases(List.of(1L, 2L))).thenReturn(report);

  mockMvc.perform(post("/api/assets/databases/connectivity-check:selected")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "ids": [1, 2]
              }
              """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.summary.total").value(2))
      .andExpect(jsonPath("$.data.summary.skipped").value(1))
      .andExpect(jsonPath("$.data.results[1].message").value("缺少连接用户名或密码"));
}
```

Add similar tests for:
- `POST /api/assets/databases/connectivity-check:page` with `{ "ids": [ ... ] }`
- `POST /api/assets/databases/connectivity-check:query` with `{ "keyword": "prod", "environment": "production", ... }`

- [ ] **Step 2: Run the controller tests to verify they fail**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AssetControllerTest#checkDatabaseConnectivityReturnsSingleResult+checkDatabaseConnectivityBySelectedRowsReturnsBatchReport test
```

Expected:
- The build fails because the controller does not inject `DatabaseConnectivityService` and the new routes do not exist.

- [ ] **Step 3: Implement the connectivity-check endpoints in `AssetController`**

Inject the service and add request DTOs:

```java
private final AssetApplicationService assetApplicationService;
private final DatabaseConnectivityService databaseConnectivityService;

public AssetController(AssetApplicationService assetApplicationService,
                       DatabaseConnectivityService databaseConnectivityService) {
  this.assetApplicationService = assetApplicationService;
  this.databaseConnectivityService = databaseConnectivityService;
}

public record BatchDatabaseConnectivityRequest(List<Long> ids) {
}

public record QueryDatabaseConnectivityRequest(String keyword,
                                               String environment,
                                               String databaseType,
                                               String lifecycleStatus,
                                               String connectivityStatus) {
}
```

Add the four endpoints:

```java
@PostMapping("/databases/{id}/connectivity-check")
public R<DatabaseConnectivityService.DatabaseConnectivityReport> checkDatabaseConnectivity(@PathVariable Long id) {
  return R.ok(databaseConnectivityService.checkOneDatabase(id));
}

@PostMapping("/databases/connectivity-check:selected")
public R<DatabaseConnectivityService.DatabaseConnectivityReport> checkSelectedDatabases(@RequestBody BatchDatabaseConnectivityRequest request) {
  return R.ok(databaseConnectivityService.checkSelectedDatabases(request.ids()));
}

@PostMapping("/databases/connectivity-check:page")
public R<DatabaseConnectivityService.DatabaseConnectivityReport> checkCurrentPageDatabases(@RequestBody BatchDatabaseConnectivityRequest request) {
  return R.ok(databaseConnectivityService.checkSelectedDatabases(request.ids()));
}

@PostMapping("/databases/connectivity-check:query")
public R<DatabaseConnectivityService.DatabaseConnectivityReport> checkQueryDatabases(@RequestBody QueryDatabaseConnectivityRequest request) {
  return R.ok(databaseConnectivityService.checkDatabasesByQuery(
      request.keyword(),
      request.environment(),
      request.databaseType(),
      request.lifecycleStatus(),
      request.connectivityStatus()));
}
```

- [ ] **Step 4: Run the integration tests again to verify they pass**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AssetControllerTest#checkDatabaseConnectivityReturnsSingleResult+checkDatabaseConnectivityBySelectedRowsReturnsBatchReport test
```

Expected:
- The command exits `0`.
- The JSON shape for the new endpoints matches the controller tests.

- [ ] **Step 5: Commit this slice if commits are authorized for this batch**

```bash
git add backend/envops-asset/src/main/java/com/img/envops/modules/asset/interfaces/AssetController.java \
  backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java && \
git commit -m "$(cat <<'EOF'
feat: expose database connectivity check endpoints
EOF
)"
```

### Task 4: Add connectivity actions and password fields to the database asset page

**Files:**
- Create: `frontend/src/views/asset/database-connectivity.spec.ts`
- Modify: `frontend/src/service/api/asset.ts`
- Modify: `frontend/src/typings/api/asset.d.ts`
- Modify: `frontend/src/typings/app.d.ts`
- Modify: `frontend/src/views/asset/database/index.vue`
- Modify: `frontend/src/views/asset/database-contract.spec.ts`
- Modify: `frontend/src/locales/langs/zh-cn.ts`
- Modify: `frontend/src/locales/langs/en-us.ts`
- Test: `frontend/src/views/asset/database-connectivity.spec.ts`
- Test: `frontend/src/views/asset/database-contract.spec.ts`

- [ ] **Step 1: Write the failing frontend tests for new APIs, selection, and password semantics**

Extend the source-contract test with the new API and type expectations:

```ts
it('includes connectivity-check APIs and response typings', () => {
  expect(assetApiSource).toContain('fetchCheckAssetDatabase');
  expect(assetApiSource).toContain('fetchCheckSelectedAssetDatabases');
  expect(assetApiSource).toContain('fetchCheckCurrentPageAssetDatabases');
  expect(assetApiSource).toContain('fetchCheckQueriedAssetDatabases');
  expect(assetTypingSource).toContain('interface DatabaseConnectivityCheckSummary');
  expect(assetTypingSource).toContain('connectionUsername?: string;');
  expect(assetTypingSource).toContain('connectionPassword?: string;');
  expect(zhLocaleSource).toContain("checkCurrentPage: '检测当前页'");
  expect(enLocaleSource).toContain("checkCurrentPage: 'Check current page'");
});
```

Create a page-behavior test file with stubbed API calls:

```ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createApp, defineComponent, h, nextTick } from 'vue';

const mocks = vi.hoisted(() => ({
  fetchGetAssetDatabases: vi.fn(),
  fetchCreateAssetDatabase: vi.fn(),
  fetchUpdateAssetDatabase: vi.fn(),
  fetchDeleteAssetDatabase: vi.fn(),
  fetchGetAssetHosts: vi.fn(),
  fetchGetAssetCredentials: vi.fn(),
  fetchCheckAssetDatabase: vi.fn(),
  fetchCheckSelectedAssetDatabases: vi.fn(),
  fetchCheckCurrentPageAssetDatabases: vi.fn(),
  fetchCheckQueriedAssetDatabases: vi.fn()
}));

it('submits a single connectivity check and refreshes the list', async () => {
  mocks.fetchGetAssetDatabases.mockResolvedValue({ error: null, data: buildDatabasePage() });
  mocks.fetchGetAssetHosts.mockResolvedValue({ error: null, data: { records: [] } });
  mocks.fetchGetAssetCredentials.mockResolvedValue({ error: null, data: [] });
  mocks.fetchCheckAssetDatabase.mockResolvedValue({
    error: null,
    data: {
      summary: { total: 1, success: 1, failed: 0, skipped: 0 },
      results: [{ databaseId: 1, databaseName: 'order_prod', databaseType: 'mysql', environment: 'production', status: 'success', message: 'connected', connectivityStatus: 'online', checkedAt: '2026-04-21T10:15:00' }]
    }
  });

  const { container } = await mountDatabasePage();
  const checkButton = Array.from(container.querySelectorAll('button')).find(button => button.textContent?.includes('page.envops.assetDatabase.actions.check'));
  checkButton?.dispatchEvent(new MouseEvent('click'));
  await settleRender();

  expect(mocks.fetchCheckAssetDatabase).toHaveBeenCalledWith(1);
  expect(mocks.fetchGetAssetDatabases).toHaveBeenCalledTimes(2);
});
```

Add a password edit semantic test:

```ts
it('keeps the password field blank when editing an existing database row', async () => {
  mocks.fetchGetAssetDatabases.mockResolvedValue({ error: null, data: buildDatabasePage() });
  mocks.fetchGetAssetHosts.mockResolvedValue({ error: null, data: { records: [] } });
  mocks.fetchGetAssetCredentials.mockResolvedValue({ error: null, data: [] });

  const { container } = await mountDatabasePage();
  const editButton = Array.from(container.querySelectorAll('button')).find(button => button.textContent?.includes('page.envops.assetDatabase.actions.edit'));
  editButton?.dispatchEvent(new MouseEvent('click'));
  await settleRender();

  const passwordInput = container.querySelector('input[data-form-field="connectionPassword"]') as HTMLInputElement;
  expect(passwordInput.value).toBe('');
});
```

- [ ] **Step 2: Run the targeted frontend tests to verify they fail**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/asset/database-contract.spec.ts src/views/asset/database-connectivity.spec.ts
```

Expected:
- The contract test fails because the new API functions and typings do not exist.
- The page-behavior test fails because the page does not render the new buttons or password field.

- [ ] **Step 3: Implement the frontend APIs, types, page logic, and locale copy**

Extend `frontend/src/service/api/asset.ts`:

```ts
export function fetchCheckAssetDatabase(id: number) {
  return request<Api.Asset.DatabaseConnectivityCheckResponse>({
    url: `/api/assets/databases/${id}/connectivity-check`,
    method: 'post'
  });
}

export function fetchCheckSelectedAssetDatabases(ids: number[]) {
  return request<Api.Asset.DatabaseConnectivityCheckResponse>({
    url: '/api/assets/databases/connectivity-check:selected',
    method: 'post',
    data: { ids }
  });
}

export function fetchCheckCurrentPageAssetDatabases(ids: number[]) {
  return request<Api.Asset.DatabaseConnectivityCheckResponse>({
    url: '/api/assets/databases/connectivity-check:page',
    method: 'post',
    data: { ids }
  });
}

export function fetchCheckQueriedAssetDatabases(query: Api.Asset.DatabaseQuery) {
  return request<Api.Asset.DatabaseConnectivityCheckResponse>({
    url: '/api/assets/databases/connectivity-check:query',
    method: 'post',
    data: {
      keyword: query.keyword || null,
      environment: query.environment || null,
      databaseType: query.databaseType || null,
      lifecycleStatus: query.lifecycleStatus || null,
      connectivityStatus: query.connectivityStatus || null
    }
  });
}
```

Extend `frontend/src/typings/api/asset.d.ts`:

```ts
interface DatabaseRecord {
  id: number;
  databaseName: string;
  databaseType: 'mysql' | 'postgresql' | 'oracle' | 'sqlserver' | 'mongodb' | 'redis' | string;
  environment: 'production' | 'staging' | 'sandbox' | string;
  hostId: number;
  hostName: string | null;
  port: number;
  instanceName: string | null;
  credentialId: number | null;
  credentialName: string | null;
  ownerName: string;
  lifecycleStatus: 'managed' | 'disabled' | string;
  connectivityStatus: 'unknown' | 'online' | 'warning' | 'offline' | string;
  connectionUsername: string | null;
  description: string | null;
  lastCheckedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

interface CreateDatabaseParams {
  databaseName: string;
  databaseType: string;
  environment: string;
  hostId: number | null;
  port: number | null;
  instanceName?: string;
  credentialId?: number | null;
  ownerName: string;
  lifecycleStatus: string;
  connectivityStatus: string;
  connectionUsername?: string;
  connectionPassword?: string;
  description?: string;
  lastCheckedAt?: string;
}

interface DatabaseConnectivityCheckSummary {
  total: number;
  success: number;
  failed: number;
  skipped: number;
}

interface DatabaseConnectivityCheckItem {
  databaseId: number;
  databaseName: string;
  databaseType: string;
  environment: string;
  status: 'success' | 'failed' | 'skipped';
  message: string;
  connectivityStatus: 'unknown' | 'online' | 'warning' | 'offline' | string;
  checkedAt: string | null;
}

interface DatabaseConnectivityCheckResponse {
  summary: DatabaseConnectivityCheckSummary;
  results: DatabaseConnectivityCheckItem[];
}
```

In `frontend/src/views/asset/database/index.vue`, extend the form model and add new actions:

```ts
type DatabaseFormModel = {
  databaseName: string;
  databaseType: string;
  environment: string;
  hostId: number | null;
  port: number | null;
  instanceName: string;
  credentialId: number | null;
  ownerName: string;
  lifecycleStatus: string;
  connectivityStatus: string;
  connectionUsername: string;
  connectionPassword: string;
  description: string;
  lastCheckedAt: string;
};

const selectedDatabaseIds = ref<number[]>([]);
const checking = ref(false);
const resultModalVisible = ref(false);
const connectivityReport = ref<Api.Asset.DatabaseConnectivityCheckResponse | null>(null);
```

Build the payload so blank password is omitted:

```ts
function buildPayload(): Api.Asset.CreateDatabaseParams {
  return {
    databaseName: formModel.databaseName.trim(),
    databaseType: formModel.databaseType.trim(),
    environment: formModel.environment.trim(),
    hostId: formModel.hostId,
    port: formModel.port,
    instanceName: formModel.instanceName.trim() || undefined,
    credentialId: formModel.credentialId,
    ownerName: formModel.ownerName.trim(),
    lifecycleStatus: formModel.lifecycleStatus.trim(),
    connectivityStatus: formModel.connectivityStatus.trim(),
    connectionUsername: formModel.connectionUsername.trim() || undefined,
    connectionPassword: formModel.connectionPassword.trim() || undefined,
    description: formModel.description.trim() || undefined,
    lastCheckedAt: formModel.lastCheckedAt.trim() || undefined
  };
}
```

Require credential pairs when either side is present:

```ts
if (Boolean(formModel.connectionUsername.trim()) !== Boolean(formModel.connectionPassword.trim()) && editingDatabaseId.value === null) {
  window.$message?.warning(t('page.envops.assetDatabase.messages.fillConnectionPair'));
  return false;
}

if (editingDatabaseId.value !== null && !formModel.connectionUsername.trim() && formModel.connectionPassword.trim()) {
  window.$message?.warning(t('page.envops.assetDatabase.messages.fillConnectionUsername'));
  return false;
}
```

Add the connectivity-check handlers:

```ts
async function runConnectivityCheck(request: Promise<Service.RequestResult<Api.Asset.DatabaseConnectivityCheckResponse>>) {
  checking.value = true;

  try {
    const response = await request;
    if (!response.error) {
      connectivityReport.value = response.data;
      resultModalVisible.value = response.data.results.length > 1;
      window.$message?.success(t('page.envops.assetDatabase.messages.checkFinished'));
      await loadPageData();
    }
  } finally {
    checking.value = false;
  }
}

async function handleCheckDatabase(record: Api.Asset.DatabaseRecord) {
  await runConnectivityCheck(fetchCheckAssetDatabase(record.id));
}

async function handleCheckSelected() {
  await runConnectivityCheck(fetchCheckSelectedAssetDatabases(selectedDatabaseIds.value));
}

async function handleCheckCurrentPage() {
  await runConnectivityCheck(fetchCheckCurrentPageAssetDatabases(tableRows.value.map(item => item.id)));
}

async function handleCheckFiltered() {
  await runConnectivityCheck(fetchCheckQueriedAssetDatabases({ ...query }));
}
```

Add the table selection and action buttons:

```vue
<NSpace>
  <NButton type="primary" @click="handleSearch">{{ t('common.search') }}</NButton>
  <NButton @click="handleResetFilters">{{ t('common.reset') }}</NButton>
  <NButton :disabled="!selectedDatabaseIds.length" :loading="checking" @click="handleCheckSelected">
    {{ t('page.envops.assetDatabase.actions.checkSelected') }}
  </NButton>
  <NButton :disabled="!tableRows.length" :loading="checking" @click="handleCheckCurrentPage">
    {{ t('page.envops.assetDatabase.actions.checkCurrentPage') }}
  </NButton>
  <NButton :loading="checking" @click="handleCheckFiltered">
    {{ t('page.envops.assetDatabase.actions.checkAllFiltered') }}
  </NButton>
</NSpace>
```

```vue
<th>
  <NCheckbox
    :checked="selectedDatabaseIds.length === tableRows.length && tableRows.length > 0"
    @update:checked="checked => {
      selectedDatabaseIds = checked ? tableRows.map(item => item.id) : [];
    }"
  />
</th>
```

```vue
<td>
  <NCheckbox
    :checked="selectedDatabaseIds.includes(item.id)"
    @update:checked="checked => {
      selectedDatabaseIds = checked
        ? [...selectedDatabaseIds, item.id]
        : selectedDatabaseIds.filter(id => id !== item.id);
    }"
  />
</td>
```

```vue
<NButton text type="primary" :loading="checking" @click="handleCheckDatabase(item)">
  {{ t('page.envops.assetDatabase.actions.check') }}
</NButton>
```

Add the new password fields to the drawer and leave password blank on edit:

```ts
function createDefaultFormModel(): DatabaseFormModel {
  return {
    databaseName: '',
    databaseType: 'mysql',
    environment: 'sandbox',
    hostId: null,
    port: 3306,
    instanceName: '',
    credentialId: null,
    ownerName: '',
    lifecycleStatus: 'managed',
    connectivityStatus: 'unknown',
    connectionUsername: '',
    connectionPassword: '',
    description: '',
    lastCheckedAt: ''
  };
}

function fillForm(record: Api.Asset.DatabaseRecord) {
  Object.assign(formModel, {
    databaseName: record.databaseName,
    databaseType: record.databaseType,
    environment: record.environment,
    hostId: record.hostId,
    port: record.port,
    instanceName: record.instanceName || '',
    credentialId: record.credentialId,
    ownerName: record.ownerName,
    lifecycleStatus: record.lifecycleStatus,
    connectivityStatus: record.connectivityStatus,
    connectionUsername: record.connectionUsername || '',
    connectionPassword: '',
    description: record.description || '',
    lastCheckedAt: record.lastCheckedAt || ''
  } satisfies DatabaseFormModel);
}
```

Add locale keys in both locale files, for example in Chinese:

```ts
actions: {
  create: '新建数据库',
  edit: '编辑',
  save: '保存',
  check: '检测',
  checkSelected: '批量检测已选',
  checkCurrentPage: '检测当前页',
  checkAllFiltered: '检测全部筛选结果',
  closeResult: '关闭结果'
},
form: {
  connectionUsername: '连接用户名',
  connectionPassword: '连接密码',
  placeholders: {
    connectionUsername: '例如：orders_app',
    connectionPassword: '留空表示沿用已保存密码'
  }
},
messages: {
  fillRequired: '请先填写数据库名、类型、环境、主机、端口、归属团队、纳管状态和连通性状态',
  fillConnectionPair: '如需保存连接凭据，请同时填写连接用户名和连接密码',
  fillConnectionUsername: '修改连接密码前请先填写连接用户名',
  checkFinished: '数据库连通性检测已执行完成'
},
result: {
  title: '检测结果',
  total: '总数',
  success: '成功',
  failed: '失败',
  skipped: '跳过',
  message: '结果说明'
}
```

Update the hero tags to reflect real checks instead of manual-only copy:

```ts
tags: {
  registryReady: '资产登记已就绪',
  connectivityCheckReady: '支持真实连通性检测',
  warningManual: 'warning 仍为人工标记'
}
```

- [ ] **Step 4: Run the targeted frontend tests, typecheck, and build**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/asset/database-contract.spec.ts src/views/asset/database-connectivity.spec.ts
pnpm --dir frontend typecheck
pnpm --dir frontend build
```

Expected:
- Vitest exits `0`.
- TypeScript typecheck exits `0`.
- The production build exits `0`.

- [ ] **Step 5: Commit this slice if commits are authorized for this batch**

```bash
git add frontend/src/service/api/asset.ts \
  frontend/src/typings/api/asset.d.ts \
  frontend/src/typings/app.d.ts \
  frontend/src/views/asset/database/index.vue \
  frontend/src/views/asset/database-contract.spec.ts \
  frontend/src/views/asset/database-connectivity.spec.ts \
  frontend/src/locales/langs/zh-cn.ts \
  frontend/src/locales/langs/en-us.ts && \
git commit -m "$(cat <<'EOF'
feat: add database connectivity actions to asset page
EOF
)"
```

### Task 5: Sync the release docs and run the full verification set

**Files:**
- Modify: `README.md`
- Modify: `docs/envops-项目详细说明.md`
- Modify: `docs/envops-开发技术说明.md`
- Modify: `docs/envops-用户操作手册.md`
- Modify: `release/0.0.4-release-notes.md`
- Test: `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`
- Test: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectionSecretProtectorTest.java`
- Test: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java`
- Test: `frontend/src/views/asset/database-contract.spec.ts`
- Test: `frontend/src/views/asset/database-connectivity.spec.ts`

- [ ] **Step 1: Update the docs to match the new real-check boundary**

Update the README database scope bullets to this wording:

```md
- 资产中心当前覆盖主机、凭据、分组、标签与数据库资源五类入口，其中数据库资源支持登记、筛选、编辑、删除、主机/凭据关联，以及真实数据库连通性检测。
- 数据库资源当前支持单个检测、批量检测已选、检测当前页、检测全部筛选结果。
- 检测成功回写 `online`，检测失败回写 `offline`，`warning` 仍保留为人工标记。
- 当前只支持基础直连，不支持 SSL、连接串、MongoDB 副本集、Redis Sentinel / Cluster 等高级连接能力。
```

Update `docs/envops-开发技术说明.md` to replace the manual-only boundary block with:

```md
- 前端页面支持列表、筛选、创建、编辑、删除，以及主机/凭据依赖加载
- 当前支持页面发起单个检测、批量检测已选、检测当前页、检测全部筛选结果
- 检测通过真实数据库连接和认证执行，成功写 `online`，失败写 `offline`
- `warning` 仍为人工维护状态，不由检测流程自动写入
- 当前只支持基础直连，不支持 SSL、连接串、副本集、Sentinel、Cluster 等高级能力
```

Update `release/0.0.4-release-notes.md` highlights with:

```md
8. 资产中心数据库资源页已支持六类数据库的真实连通性检测，覆盖单检、已选批检、当前页批检和全部筛选结果批检。
```

- [ ] **Step 2: Run the full backend verification set**

Run:

```bash
mvn -f backend/pom.xml -pl envops-asset -Dtest=DatabaseConnectionSecretProtectorTest,DatabaseConnectivityServiceTest test
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AssetControllerTest test
```

Expected:
- Both commands exit `0`.
- `AssetControllerTest` covers database CRUD plus the new connectivity-check endpoints.

- [ ] **Step 3: Run the full frontend verification set**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/asset/database-contract.spec.ts src/views/asset/database-connectivity.spec.ts src/store/modules/__tests__/route-envops.spec.ts
pnpm --dir frontend typecheck
pnpm --dir frontend build
```

Expected:
- All three commands exit `0`.
- The page contract tests, connectivity interaction tests, route tests, typecheck, and build are all green.

- [ ] **Step 4: Manually verify the page behavior in the browser**

Run the app with the documented local boot commands, then verify these exact flows:

```bash
ENVOPS_SECURITY_TOKEN_SECRET=change-this-token-secret-2026-1234 \
ENVOPS_CREDENTIAL_PROTECTION_SECRET=change-this-credential-secret-2026 \
ENVOPS_SERVER_PORT=18080 \
bash backend/scripts/run-envops-boot.sh
pnpm --dir frontend dev
```

Manual checks:
- Log in with `envops-admin / EnvOps@123`.
- Open `/asset/database`.
- Create a database asset with `connectionUsername` and `connectionPassword`, save, reopen edit drawer, confirm password field is blank.
- Trigger one row-level connectivity check and confirm the result updates `connectivityStatus` and `lastCheckedAt`.
- Select multiple rows and run `批量检测已选`.
- Run `检测当前页`.
- Apply a filter and run `检测全部筛选结果`.
- Confirm the result modal lists success, failed, and skipped rows with readable messages.

- [ ] **Step 5: Commit this slice if commits are authorized for this batch**

```bash
git add README.md \
  docs/envops-项目详细说明.md \
  docs/envops-开发技术说明.md \
  docs/envops-用户操作手册.md \
  release/0.0.4-release-notes.md && \
git commit -m "$(cat <<'EOF'
docs: describe database connectivity checks in envops 0.0.4
EOF
)"
```

## Self-review checklist

- Spec coverage:
  - Direct credential storage on `asset_database`: Task 1
  - Reversible secret storage instead of one-way hashing: Task 1
  - Six real checkers and synchronous execution: Task 2
  - Single, selected, page, query endpoints: Task 3
  - Page row action, selected/current-page/query actions, result modal, password semantics: Task 4
  - Docs and verification: Task 5
- Placeholder scan:
  - No `TBD`, `TODO`, or “similar to task N” shortcuts remain.
- Type consistency:
  - Backend uses `connectionUsername` / `connectionPassword` consistently in controller, service, mapper, and frontend payload typings.
  - Batch response uses `DatabaseConnectivityReport` / `DatabaseConnectivitySummary` / `DatabaseConnectivityItem` consistently.

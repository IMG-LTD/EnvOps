# EnvOps v0.0.6 Unified Task Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal but real unified task center that shows Deploy, database connectivity, and Traffic action tasks in one place, with a lightweight detail drawer and deep links back to the source modules.

**Architecture:** Add a dedicated `envops-task` module with its own unified task projection table, list/detail API, preview builders, and write helpers. Keep Deploy, database connectivity, and Traffic execution models as they are, but have each module write into the unified projection at the approved lifecycle points, then replace the current deploy-only Task Center frontend with a unified list, minimal filters, a detail drawer, and source-route deep links.

**Tech Stack:** Spring Boot 3.3, MyBatis, H2 schema/data SQL, JUnit 5, MockMvc, Vue 3, TypeScript, Naive UI, Vitest, elegant-router

---

## File Map

### Backend
- Create: `backend/envops-task/pom.xml`
  - New module definition for unified task center code.
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationService.java`
  - Owns unified list query, detail query, status normalization, and list/detail DTOs.
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskRecorder.java`
  - Owns create/update/upsert writes into `unified_task_center`, including SecurityContext fallback for `triggeredBy`.
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskDetailPreviewFactory.java`
  - Builds Deploy / database connectivity / Traffic preview payloads and serializes them to JSON.
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/interfaces/UnifiedTaskCenterController.java`
  - Exposes `/api/task-center/tasks` and `/api/task-center/tasks/{id}`.
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterEntity.java`
  - Write-side entity for the unified task table.
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterRow.java`
  - Read-side row projection for list/detail queries.
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/mapper/UnifiedTaskCenterMapper.java`
  - MyBatis mapper for inserts, updates, list/detail reads, and Deploy backfill reads.
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/bootstrap/UnifiedTaskCenterBackfillRunner.java`
  - Startup runner that backfills missing Deploy projections only.
- Create Test: `backend/envops-task/src/test/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationServiceTest.java`
  - Covers status mapping and preview shaping.
- Modify: `backend/pom.xml`
  - Add `envops-task` to the reactor modules list.
- Modify: `backend/envops-boot/pom.xml`
  - Include `envops-task` so the unified controller and services load in Boot tests.
- Modify: `backend/envops-deploy/pom.xml`
  - Depend on `envops-task`.
- Modify: `backend/envops-asset/pom.xml`
  - Depend on `envops-task`.
- Modify: `backend/envops-traffic/pom.xml`
  - Depend on `envops-task`.
- Modify: `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskApplicationService.java`
  - Sync projection on create, approve, reject, and backfill-facing summary shaping.
- Modify: `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskExecutionApplicationService.java`
  - Sync projection on execute start, finish, cancel, retry, and rollback task creation.
- Modify: `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/interfaces/DeployTaskController.java`
  - Remove the old deploy-owned `/api/task-center/tasks` endpoint.
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectivityService.java`
  - Create one unified task for each real single check or batch check.
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java`
  - Create one unified task for each real `preview` / `apply` / `rollback` action.
- Modify: `backend/envops-boot/src/main/resources/schema.sql`
  - Add `unified_task_center` table.
- Modify: `backend/envops-boot/src/main/resources/data.sql`
  - Seed Deploy data that backfill can project and sample unified rows where direct fixture coverage is needed.
- Modify: `backend/envops-boot/src/test/resources/schema.sql`
  - Mirror `unified_task_center` in test schema.
- Modify: `backend/envops-boot/src/test/resources/data.sql`
  - Seed deploy, database, traffic, and unified task fixtures for list/detail tests.
- Modify Test: `backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java`
  - Replace deploy-only task center assertions with unified list/detail and Deploy sync coverage.
- Modify Test: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`
  - Cover Traffic task recording success/failure semantics.
- Modify Test: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java`
  - Cover one-batch-one-task semantics and batch failure aggregation.

### Frontend
- Modify: `frontend/src/typings/api/task.d.ts`
  - Replace deploy-only task types with unified list/detail types.
- Modify: `frontend/src/service/api/task.ts`
  - Keep list API and add detail API for drawer loading.
- Modify: `frontend/src/views/task/shared/query.ts`
  - Replace deploy-only query model with `keyword + taskType + status + startedFrom + startedTo + page + pageSize`.
- Modify: `frontend/src/views/task/center/index.vue`
  - Replace deploy-only page behavior with unified list, minimal filters, detail drawer, and `sourceRoute` deep link.
- Modify: `frontend/src/locales/langs/zh-cn.ts`
  - Replace deploy-only Task Center copy with unified wording.
- Modify: `frontend/src/locales/langs/en-us.ts`
  - Replace deploy-only Task Center copy with unified wording.
- Modify Test: `frontend/src/views/task/task-contract.spec.ts`
  - Rewrite source-text assertions, mocked APIs, and router mocks for unified behavior.

### Documentation
- Modify: `README.md`
  - Update current-baseline Task Center wording.
- Modify: `docs/envops-开发技术说明.md`
  - Update backend/frontend scope and validation commands.
- Modify: `docs/envops-用户操作手册.md`
  - Update user flow, filters, detail drawer, and source-module deep links.
- Create: `release/0.0.6-release-notes.md`
  - Record unified task center scope, boundaries, and validation commands.

---

### Task 1: Add the `envops-task` module and unified task schema foundation

**Files:**
- Create: `backend/envops-task/pom.xml`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationService.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskRecorder.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskDetailPreviewFactory.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/interfaces/UnifiedTaskCenterController.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterEntity.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterRow.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/mapper/UnifiedTaskCenterMapper.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/bootstrap/UnifiedTaskCenterBackfillRunner.java`
- Create Test: `backend/envops-task/src/test/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationServiceTest.java`
- Modify: `backend/pom.xml`
- Modify: `backend/envops-boot/pom.xml`
- Modify: `backend/envops-boot/src/main/resources/schema.sql`
- Modify: `backend/envops-boot/src/main/resources/data.sql`
- Modify: `backend/envops-boot/src/test/resources/schema.sql`
- Modify: `backend/envops-boot/src/test/resources/data.sql`

- [ ] **Step 1: Write the failing unit test for status mapping and preview shaping**

```java
package com.img.envops.modules.task.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class UnifiedTaskCenterApplicationServiceTest {

  private final UnifiedTaskDetailPreviewFactory previewFactory = new UnifiedTaskDetailPreviewFactory();

  @Test
  void normalizesDeployAndTrafficStatusesIntoFourUnifiedStates() {
    assertThat(UnifiedTaskCenterApplicationService.normalizeStatus("PENDING_APPROVAL")).isEqualTo("pending");
    assertThat(UnifiedTaskCenterApplicationService.normalizeStatus("RUNNING")).isEqualTo("running");
    assertThat(UnifiedTaskCenterApplicationService.normalizeStatus("SUCCESS")).isEqualTo("success");
    assertThat(UnifiedTaskCenterApplicationService.normalizeStatus("REJECTED")).isEqualTo("failed");
  }

  @Test
  void buildsReadableDatabaseBatchPreview() {
    Map<String, Object> preview = previewFactory.buildDatabaseConnectivityPreview(
        true,
        "批量检测 20 条，成功 16，失败 3，跳过 1",
        20,
        16,
        3,
        1,
        "/asset/database",
        "3 databases failed authentication");

    assertThat(preview)
        .containsEntry("mode", "batch")
        .containsEntry("summary", "批量检测 20 条，成功 16，失败 3，跳过 1")
        .containsEntry("sourceRoute", "/asset/database")
        .containsEntry("errorSummary", "3 databases failed authentication");
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test`
Expected: FAIL because `envops-task` and unified task classes do not exist yet.

- [ ] **Step 3: Add the new module to Maven and Boot**

```xml
<!-- backend/pom.xml -->
<modules>
  <module>envops-boot</module>
  <module>envops-common</module>
  <module>envops-framework</module>
  <module>envops-system</module>
  <module>envops-asset</module>
  <module>envops-monitor</module>
  <module>envops-app</module>
  <module>envops-deploy</module>
  <module>envops-exec</module>
  <module>envops-traffic</module>
  <module>envops-task</module>
</modules>
```

```xml
<!-- backend/envops-task/pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.img.envops</groupId>
    <artifactId>envops-parent</artifactId>
    <version>0.0.4-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>

  <artifactId>envops-task</artifactId>
  <name>envops-task</name>

  <dependencies>
    <dependency>
      <groupId>com.img.envops</groupId>
      <artifactId>envops-common</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.mybatis.spring.boot</groupId>
      <artifactId>mybatis-spring-boot-starter</artifactId>
      <version>${mybatis-spring-boot.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

```xml
<!-- backend/envops-boot/pom.xml -->
<dependency>
  <groupId>com.img.envops</groupId>
  <artifactId>envops-task</artifactId>
  <version>${project.version}</version>
</dependency>
```

- [ ] **Step 4: Add the unified task center table to main/test schema**

```sql
CREATE TABLE unified_task_center (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_type VARCHAR(64) NOT NULL,
  task_name VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  triggered_by VARCHAR(128) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  finished_at TIMESTAMP NULL,
  summary VARCHAR(500) NOT NULL,
  detail_preview CLOB NOT NULL,
  source_id BIGINT NULL,
  source_route VARCHAR(255) NOT NULL,
  module_name VARCHAR(64) NOT NULL,
  error_summary VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_unified_task_center_started_at ON unified_task_center (started_at);
CREATE INDEX idx_unified_task_center_type_status ON unified_task_center (task_type, status);
CREATE INDEX idx_unified_task_center_source_ref ON unified_task_center (task_type, source_id);
```

```sql
INSERT INTO unified_task_center (
  id, task_type, task_name, status, triggered_by, started_at, finished_at,
  summary, detail_preview, source_id, source_route, module_name, error_summary,
  created_at, updated_at
) VALUES
  (
    9001, 'deploy', 'Deploy checkout-gateway to prod', 'success', 'envops-admin',
    TIMESTAMP '2026-04-20 10:00:00', TIMESTAMP '2026-04-20 10:12:00',
    '发布 checkout-gateway 到 prod，3 台主机，已完成',
    '{"app":"checkout-gateway","environment":"prod","targetCount":3,"successCount":3,"failCount":0,"rawStatus":"SUCCESS","sourceRoute":"/deploy/task?taskId=3001"}',
    3001, '/deploy/task?taskId=3001', 'deploy', null,
    TIMESTAMP '2026-04-20 10:00:00', TIMESTAMP '2026-04-20 10:12:00'
  ),
  (
    9002, 'database_connectivity', '批量数据库连通性检测', 'failed', 'envops-admin',
    TIMESTAMP '2026-04-21 09:00:00', TIMESTAMP '2026-04-21 09:02:00',
    '批量检测 20 条，成功 16，失败 3，跳过 1',
    '{"mode":"batch","summary":"批量检测 20 条，成功 16，失败 3，跳过 1","total":20,"success":16,"failed":3,"skipped":1,"sourceRoute":"/asset/database","errorSummary":"3 databases failed authentication"}',
    null, '/asset/database', 'asset', '3 databases failed authentication',
    TIMESTAMP '2026-04-21 09:00:00', TIMESTAMP '2026-04-21 09:02:00'
  ),
  (
    9003, 'traffic_action', 'Traffic Apply', 'failed', 'envops-admin',
    TIMESTAMP '2026-04-22 08:30:00', TIMESTAMP '2026-04-22 08:31:00',
    'Apply checkout-gateway，策略 weighted_routing，插件 REST',
    '{"action":"apply","app":"checkout-gateway","strategy":"weighted_routing","plugin":"REST","rollbackTokenAvailable":false,"sourceRoute":"/traffic/controller","errorSummary":"rollbackToken is required from traffic rest service"}',
    1001, '/traffic/controller', 'traffic', 'rollbackToken is required from traffic rest service',
    TIMESTAMP '2026-04-22 08:30:00', TIMESTAMP '2026-04-22 08:31:00'
  );
```

- [ ] **Step 5: Create the minimal application, recorder, and preview classes**

```java
package com.img.envops.modules.task.application;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UnifiedTaskCenterApplicationService {

  public static String normalizeStatus(String status) {
    String normalized = status == null ? "" : status.trim().toUpperCase();
    return switch (normalized) {
      case "RUNNING" -> "running";
      case "SUCCESS" -> "success";
      case "FAILED", "REJECTED", "CANCELLED" -> "failed";
      default -> "pending";
    };
  }

  public record UnifiedTaskRecord(
      Long id,
      String taskType,
      String taskName,
      String status,
      String triggeredBy,
      String startedAt,
      String finishedAt,
      String summary,
      String sourceRoute,
      String errorSummary) {}

  public record UnifiedTaskDetail(
      Long id,
      String taskType,
      String taskName,
      String status,
      String triggeredBy,
      String startedAt,
      String finishedAt,
      String summary,
      Map<String, Object> detailPreview,
      String sourceRoute,
      String errorSummary) {}

  public record UnifiedTaskPage(Integer page, Integer pageSize, Long total, List<UnifiedTaskRecord> records) {}
}
```

```java
package com.img.envops.modules.task.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UnifiedTaskDetailPreviewFactory {
  private final ObjectMapper objectMapper = new ObjectMapper();

  public Map<String, Object> buildDeployPreview(
      String app,
      String environment,
      long targetCount,
      long successCount,
      long failCount,
      String rawStatus,
      String sourceRoute) {
    Map<String, Object> preview = new LinkedHashMap<>();
    preview.put("app", app);
    preview.put("environment", environment);
    preview.put("targetCount", targetCount);
    preview.put("successCount", successCount);
    preview.put("failCount", failCount);
    preview.put("rawStatus", rawStatus);
    preview.put("sourceRoute", sourceRoute);
    return preview;
  }

  public Map<String, Object> buildDatabaseConnectivityPreview(
      boolean batch,
      String summary,
      long total,
      long success,
      long failed,
      long skipped,
      String sourceRoute,
      String errorSummary) {
    Map<String, Object> preview = new LinkedHashMap<>();
    preview.put("mode", batch ? "batch" : "single");
    preview.put("summary", summary);
    preview.put("total", total);
    preview.put("success", success);
    preview.put("failed", failed);
    preview.put("skipped", skipped);
    preview.put("sourceRoute", sourceRoute);
    preview.put("errorSummary", errorSummary);
    return preview;
  }

  public Map<String, Object> buildTrafficActionPreview(
      String action,
      String app,
      String strategy,
      String plugin,
      boolean rollbackTokenAvailable,
      String sourceRoute,
      String errorSummary) {
    Map<String, Object> preview = new LinkedHashMap<>();
    preview.put("action", action);
    preview.put("app", app);
    preview.put("strategy", strategy);
    preview.put("plugin", plugin);
    preview.put("rollbackTokenAvailable", rollbackTokenAvailable);
    preview.put("sourceRoute", sourceRoute);
    preview.put("errorSummary", errorSummary);
    return preview;
  }

  public String toJson(Map<String, Object> preview) {
    try {
      return objectMapper.writeValueAsString(preview);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("failed to serialize detail preview", exception);
    }
  }
}
```

```java
package com.img.envops.modules.task.application;

import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterEntity;
import com.img.envops.modules.task.infrastructure.mapper.UnifiedTaskCenterMapper;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UnifiedTaskRecorder {

  public record CreateCommand(
      String taskType,
      String taskName,
      String status,
      String triggeredBy,
      LocalDateTime startedAt,
      String summary,
      String detailPreview,
      Long sourceId,
      String sourceRoute,
      String moduleName,
      String errorSummary) {}

  public record UpdateCommand(
      Long id,
      String status,
      LocalDateTime finishedAt,
      String summary,
      String detailPreview,
      String errorSummary) {}

  public record UpsertBySourceCommand(
      String taskType,
      Long sourceId,
      String taskName,
      String status,
      String triggeredBy,
      LocalDateTime startedAt,
      LocalDateTime finishedAt,
      String summary,
      String detailPreview,
      String sourceRoute,
      String moduleName,
      String errorSummary) {}

  public record UpdateBySourceCommand(
      String taskType,
      Long sourceId,
      String status,
      LocalDateTime finishedAt,
      String summary,
      String detailPreview,
      String errorSummary) {}

  private final UnifiedTaskCenterMapper unifiedTaskCenterMapper;

  public UnifiedTaskRecorder(UnifiedTaskCenterMapper unifiedTaskCenterMapper) {
    this.unifiedTaskCenterMapper = unifiedTaskCenterMapper;
  }

  public Long start(CreateCommand command) {
    UnifiedTaskCenterEntity entity = toEntity(command);
    unifiedTaskCenterMapper.insert(entity);
    return entity.getId();
  }

  public void update(UpdateCommand command) {
    unifiedTaskCenterMapper.updateById(
        command.id(),
        command.status(),
        command.finishedAt(),
        command.summary(),
        command.detailPreview(),
        command.errorSummary());
  }

  public void upsertBySource(UpsertBySourceCommand command) {
    UnifiedTaskCenterEntity existing = unifiedTaskCenterMapper.findEntityBySource(command.taskType(), command.sourceId());
    if (existing == null) {
      UnifiedTaskCenterEntity entity = new UnifiedTaskCenterEntity();
      entity.setTaskType(command.taskType());
      entity.setTaskName(command.taskName());
      entity.setStatus(command.status());
      entity.setTriggeredBy(resolveTriggeredBy(command.triggeredBy()));
      entity.setStartedAt(command.startedAt());
      entity.setFinishedAt(command.finishedAt());
      entity.setSummary(command.summary());
      entity.setDetailPreview(command.detailPreview());
      entity.setSourceId(command.sourceId());
      entity.setSourceRoute(command.sourceRoute());
      entity.setModuleName(command.moduleName());
      entity.setErrorSummary(command.errorSummary());
      unifiedTaskCenterMapper.insert(entity);
      return;
    }

    unifiedTaskCenterMapper.updateById(
        existing.getId(),
        command.status(),
        command.finishedAt(),
        command.summary(),
        command.detailPreview(),
        command.errorSummary());
  }

  public void updateBySource(UpdateBySourceCommand command) {
    UnifiedTaskCenterEntity existing = unifiedTaskCenterMapper.findEntityBySource(command.taskType(), command.sourceId());
    if (existing == null) {
      throw new IllegalArgumentException("unified task not found for source: " + command.taskType() + ":" + command.sourceId());
    }
    unifiedTaskCenterMapper.updateById(
        existing.getId(),
        command.status(),
        command.finishedAt(),
        command.summary(),
        command.detailPreview(),
        command.errorSummary());
  }

  private UnifiedTaskCenterEntity toEntity(CreateCommand command) {
    UnifiedTaskCenterEntity entity = new UnifiedTaskCenterEntity();
    entity.setTaskType(command.taskType());
    entity.setTaskName(command.taskName());
    entity.setStatus(command.status());
    entity.setTriggeredBy(resolveTriggeredBy(command.triggeredBy()));
    entity.setStartedAt(command.startedAt());
    entity.setSummary(command.summary());
    entity.setDetailPreview(command.detailPreview());
    entity.setSourceId(command.sourceId());
    entity.setSourceRoute(command.sourceRoute());
    entity.setModuleName(command.moduleName());
    entity.setErrorSummary(command.errorSummary());
    return entity;
  }

  private String resolveTriggeredBy(String triggeredBy) {
    if (StringUtils.hasText(triggeredBy)) {
      return triggeredBy.trim();
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return "system";
    }
    return authentication.getName().trim();
  }
}
```

- [ ] **Step 6: Add the controller and mapper skeleton with the correct response type**

```java
package com.img.envops.modules.task.interfaces;

import com.img.envops.common.response.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task-center/tasks")
public class UnifiedTaskCenterController {

  @GetMapping
  public R<Object> getTasks() {
    return R.ok(null);
  }
}
```

```java
package com.img.envops.modules.task.infrastructure.mapper;

import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UnifiedTaskCenterMapper {

  @Insert("""
      INSERT INTO unified_task_center (
        task_type, task_name, status, triggered_by, started_at, finished_at,
        summary, detail_preview, source_id, source_route, module_name, error_summary,
        created_at, updated_at
      ) VALUES (
        #{taskType}, #{taskName}, #{status}, #{triggeredBy}, #{startedAt}, #{finishedAt},
        #{summary}, #{detailPreview}, #{sourceId}, #{sourceRoute}, #{moduleName}, #{errorSummary},
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(UnifiedTaskCenterEntity entity);

  @Select("""
      SELECT id, task_type AS taskType, task_name AS taskName, status, triggered_by AS triggeredBy,
             started_at AS startedAt, finished_at AS finishedAt, summary, detail_preview AS detailPreview,
             source_id AS sourceId, source_route AS sourceRoute, module_name AS moduleName,
             error_summary AS errorSummary
      FROM unified_task_center
      WHERE task_type = #{taskType}
        AND source_id = #{sourceId}
      ORDER BY id DESC
      LIMIT 1
      """)
  UnifiedTaskCenterEntity findEntityBySource(String taskType, Long sourceId);

  @Update("""
      UPDATE unified_task_center
      SET status = #{status},
          finished_at = #{finishedAt},
          summary = #{summary},
          detail_preview = COALESCE(#{detailPreview}, detail_preview),
          error_summary = #{errorSummary},
          updated_at = CURRENT_TIMESTAMP
      WHERE id = #{id}
      """)
  int updateById(Long id, String status, LocalDateTime finishedAt, String summary, String detailPreview, String errorSummary);
}
```

- [ ] **Step 7: Run the unit test to verify it passes**

Run: `mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test`
Expected: PASS with `BUILD SUCCESS`.

- [ ] **Step 8: Commit**

```bash
git add backend/pom.xml backend/envops-task backend/envops-boot/pom.xml backend/envops-boot/src/main/resources/schema.sql backend/envops-boot/src/main/resources/data.sql backend/envops-boot/src/test/resources/schema.sql backend/envops-boot/src/test/resources/data.sql
git commit -m "$(cat <<'EOF'
feat: add unified task center backend foundation
EOF
)"
```

### Task 2: Replace the deploy-only Task Center API with a unified list/detail API

**Files:**
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationService.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/interfaces/UnifiedTaskCenterController.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterRow.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/mapper/UnifiedTaskCenterMapper.java`
- Modify: `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/interfaces/DeployTaskController.java`
- Modify Test: `backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java`

- [ ] **Step 1: Write the failing Boot API test for unified list and detail**

```java
@Test
void getTaskCenterTasksReturnsDeployDatabaseAndTrafficRows() throws Exception {
  mockMvc.perform(get("/api/task-center/tasks")
          .param("taskType", "traffic_action")
          .param("status", "failed")
          .param("keyword", "checkout")
          .param("startedFrom", "2026-04-20T00:00:00")
          .param("startedTo", "2026-04-23T00:00:00")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data.records[0].taskType").value("traffic_action"))
      .andExpect(jsonPath("$.data.records[0].summary").isNotEmpty())
      .andExpect(jsonPath("$.data.records[0].sourceRoute").value("/traffic/controller"));
}

@Test
void getTaskCenterTaskDetailReturnsDrawerPayload() throws Exception {
  mockMvc.perform(get("/api/task-center/tasks/9001")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value("0000"))
      .andExpect(jsonPath("$.data.taskType").value("deploy"))
      .andExpect(jsonPath("$.data.detailPreview.app").value("checkout-gateway"))
      .andExpect(jsonPath("$.data.sourceRoute").value("/deploy/task?taskId=3001"));
}
```

- [ ] **Step 2: Run the API test to verify it fails**

Run: `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest test`
Expected: FAIL because `/api/task-center/tasks` is still owned by Deploy and there is no unified detail endpoint.

- [ ] **Step 3: Delete the old deploy-owned task center endpoint**

```java
// Delete this method from DeployTaskController entirely:
// @GetMapping("/api/task-center/tasks")
// public R<DeployTaskApplicationService.TaskCenterPage> getTaskCenterTasks(...) { ... }
```

- [ ] **Step 4: Implement the unified query and detail contract**

```java
package com.img.envops.modules.task.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import com.img.envops.modules.task.infrastructure.mapper.UnifiedTaskCenterMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UnifiedTaskCenterApplicationService {

  public record UnifiedTaskQuery(
      String keyword,
      String taskType,
      String status,
      LocalDateTime startedFrom,
      LocalDateTime startedTo,
      Integer page,
      Integer pageSize) {}

  private final UnifiedTaskCenterMapper unifiedTaskCenterMapper;
  private final ObjectMapper objectMapper;

  public UnifiedTaskCenterApplicationService(UnifiedTaskCenterMapper unifiedTaskCenterMapper, ObjectMapper objectMapper) {
    this.unifiedTaskCenterMapper = unifiedTaskCenterMapper;
    this.objectMapper = objectMapper;
  }

  public UnifiedTaskPage getTasks(UnifiedTaskQuery query) {
    int page = query == null || query.page() == null ? 1 : query.page();
    int pageSize = query == null || query.pageSize() == null ? 10 : query.pageSize();
    long total = unifiedTaskCenterMapper.countByQuery(
        query == null ? null : query.keyword(),
        query == null ? null : query.taskType(),
        query == null ? null : query.status(),
        query == null ? null : query.startedFrom(),
        query == null ? null : query.startedTo());
    List<UnifiedTaskRecord> records = unifiedTaskCenterMapper.findByQuery(
            query == null ? null : query.keyword(),
            query == null ? null : query.taskType(),
            query == null ? null : query.status(),
            query == null ? null : query.startedFrom(),
            query == null ? null : query.startedTo(),
            pageSize,
            (page - 1) * pageSize)
        .stream()
        .map(this::toListRecord)
        .toList();
    return new UnifiedTaskPage(page, pageSize, total, records);
  }

  public UnifiedTaskDetail getTaskDetail(Long id) {
    UnifiedTaskCenterRow row = unifiedTaskCenterMapper.findDetailById(id);
    return new UnifiedTaskDetail(
        row.getId(),
        row.getTaskType(),
        row.getTaskName(),
        row.getStatus(),
        row.getTriggeredBy(),
        row.getStartedAt().toString(),
        row.getFinishedAt() == null ? null : row.getFinishedAt().toString(),
        row.getSummary(),
        readPreview(row.getDetailPreview()),
        row.getSourceRoute(),
        row.getErrorSummary());
  }

  private UnifiedTaskRecord toListRecord(UnifiedTaskCenterRow row) {
    return new UnifiedTaskRecord(
        row.getId(),
        row.getTaskType(),
        row.getTaskName(),
        row.getStatus(),
        row.getTriggeredBy(),
        row.getStartedAt().toString(),
        row.getFinishedAt() == null ? null : row.getFinishedAt().toString(),
        row.getSummary(),
        row.getSourceRoute(),
        row.getErrorSummary());
  }

  private Map<String, Object> readPreview(String preview) {
    try {
      return objectMapper.readValue(preview, new TypeReference<Map<String, Object>>() {});
    } catch (Exception exception) {
      throw new IllegalArgumentException("failed to parse detail preview", exception);
    }
  }
}
```

```java
@GetMapping
public R<UnifiedTaskCenterApplicationService.UnifiedTaskPage> getTasks(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String taskType,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedFrom,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedTo,
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "10") Integer pageSize) {
  return R.ok(unifiedTaskCenterApplicationService.getTasks(
      new UnifiedTaskCenterApplicationService.UnifiedTaskQuery(
          keyword,
          taskType,
          status,
          startedFrom,
          startedTo,
          page,
          pageSize)));
}

@GetMapping("/{id}")
public R<UnifiedTaskCenterApplicationService.UnifiedTaskDetail> getTaskDetail(@PathVariable Long id) {
  return R.ok(unifiedTaskCenterApplicationService.getTaskDetail(id));
}
```

- [ ] **Step 5: Implement the mapper SQL against the unified table**

```java
@Select({
    "<script>",
    "SELECT COUNT(*) FROM unified_task_center utc",
    "<where>",
    "  <if test='keyword != null and keyword != \"\"'>",
    "    AND (LOWER(utc.task_name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))",
    "      OR LOWER(utc.summary) LIKE LOWER(CONCAT('%', #{keyword}, '%')))",
    "  </if>",
    "  <if test='taskType != null and taskType != \"\"'>AND utc.task_type = #{taskType}</if>",
    "  <if test='status != null and status != \"\"'>AND utc.status = #{status}</if>",
    "  <if test='startedFrom != null'>AND utc.started_at &gt;= #{startedFrom}</if>",
    "  <if test='startedTo != null'>AND utc.started_at &lt;= #{startedTo}</if>",
    "</where>",
    "</script>"
})
long countByQuery(String keyword, String taskType, String status, LocalDateTime startedFrom, LocalDateTime startedTo);
```

```java
@Select({
    "<script>",
    "SELECT utc.id, utc.task_type AS taskType, utc.task_name AS taskName, utc.status, utc.triggered_by AS triggeredBy,",
    "       utc.started_at AS startedAt, utc.finished_at AS finishedAt, utc.summary, utc.detail_preview AS detailPreview,",
    "       utc.source_route AS sourceRoute, utc.error_summary AS errorSummary",
    "FROM unified_task_center utc",
    "<where>",
    "  <if test='keyword != null and keyword != \"\"'>",
    "    AND (LOWER(utc.task_name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))",
    "      OR LOWER(utc.summary) LIKE LOWER(CONCAT('%', #{keyword}, '%')))",
    "  </if>",
    "  <if test='taskType != null and taskType != \"\"'>AND utc.task_type = #{taskType}</if>",
    "  <if test='status != null and status != \"\"'>AND utc.status = #{status}</if>",
    "  <if test='startedFrom != null'>AND utc.started_at &gt;= #{startedFrom}</if>",
    "  <if test='startedTo != null'>AND utc.started_at &lt;= #{startedTo}</if>",
    "</where>",
    "ORDER BY utc.started_at DESC, utc.id DESC",
    "LIMIT #{limit} OFFSET #{offset}",
    "</script>"
})
List<UnifiedTaskCenterRow> findByQuery(String keyword, String taskType, String status, LocalDateTime startedFrom, LocalDateTime startedTo, Integer limit, Integer offset);

@Select("""
    SELECT utc.id, utc.task_type AS taskType, utc.task_name AS taskName, utc.status, utc.triggered_by AS triggeredBy,
           utc.started_at AS startedAt, utc.finished_at AS finishedAt, utc.summary, utc.detail_preview AS detailPreview,
           utc.source_route AS sourceRoute, utc.error_summary AS errorSummary
    FROM unified_task_center utc
    WHERE utc.id = #{id}
    """)
UnifiedTaskCenterRow findDetailById(Long id);
```

- [ ] **Step 6: Run the API test to verify it passes**

Run: `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest test`
Expected: PASS with unified list/detail assertions.

- [ ] **Step 7: Commit**

```bash
git add backend/envops-task backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/interfaces/DeployTaskController.java backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java
git commit -m "$(cat <<'EOF'
feat: expose unified task center api
EOF
)"
```

### Task 3: Backfill Deploy history and sync Deploy lifecycle into the unified projection

**Files:**
- Modify: `backend/envops-deploy/pom.xml`
- Modify: `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskApplicationService.java`
- Modify: `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskExecutionApplicationService.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskRecorder.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskDetailPreviewFactory.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/mapper/UnifiedTaskCenterMapper.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/bootstrap/UnifiedTaskCenterBackfillRunner.java`
- Modify Test: `backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java`

- [ ] **Step 1: Write the failing Deploy sync tests**

```java
@Test
void createDeployTaskCreatesPendingUnifiedProjection() throws Exception {
  mockMvc.perform(post("/api/deploy/tasks")
          .header("Authorization", "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "taskName":"deploy-task-center-create",
                "taskType":"INSTALL",
                "appId":1,
                "versionId":1,
                "hostIds":[1],
                "batchStrategy":"ALL",
                "batchSize":0,
                "params":{
                  "sshUser":"envops",
                  "privateKeyPath":"/tmp/demo.pem",
                  "remoteBaseDir":"/srv/releases"
                }
              }
              """))
      .andExpect(status().isOk());

  mockMvc.perform(get("/api/task-center/tasks")
          .param("taskType", "deploy")
          .param("keyword", "deploy-task-center-create")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.records[0].status").value("pending"));
}

@Test
void backfillsDeployRowsMissingUnifiedProjection() throws Exception {
  mockMvc.perform(get("/api/task-center/tasks")
          .param("taskType", "deploy")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.records[0].taskType").value("deploy"))
      .andExpect(jsonPath("$.data.records[0].sourceRoute").value(org.hamcrest.Matchers.containsString("/deploy/task")));
}

@Test
void executeDeployTaskUpdatesUnifiedProjectionToRunningThenFinished() throws Exception {
  mockMvc.perform(post("/api/deploy/tasks/3001/execute")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk());

  mockMvc.perform(get("/api/task-center/tasks")
          .param("taskType", "deploy")
          .param("keyword", "checkout-gateway")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.records[0].status").value(org.hamcrest.Matchers.anyOf(
          org.hamcrest.Matchers.is("running"),
          org.hamcrest.Matchers.is("success"),
          org.hamcrest.Matchers.is("failed"))));
}
```

- [ ] **Step 2: Run the Deploy sync tests to verify they fail**

Run: `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest test`
Expected: FAIL because new Deploy tasks, backfilled tasks, and runtime updates are not yet synchronized into `unified_task_center`.

- [ ] **Step 3: Add the `envops-task` dependency to Deploy**

```xml
<dependency>
  <groupId>com.img.envops</groupId>
  <artifactId>envops-task</artifactId>
  <version>${project.version}</version>
</dependency>
```

- [ ] **Step 4: Implement Deploy backfill for missing projections only**

```java
@Select("""
    SELECT dt.id AS sourceId,
           dt.task_name AS taskName,
           dt.status,
           dt.operator_name AS operatorName,
           ad.app_name AS appName,
           COALESCE(
             NULLIF(TRIM(MAX(CASE WHEN dtp.param_key = 'environment' THEN dtp.param_value END)), ''),
             NULLIF(TRIM(MAX(CASE WHEN dtp.param_key = 'env' THEN dtp.param_value END)), ''),
             NULLIF(TRIM(MAX(CASE WHEN dtp.param_key = 'profile' THEN dtp.param_value END)), ''),
             NULLIF(TRIM(MAX(CASE WHEN dtp.param_key = 'namespace' THEN dtp.param_value END)), '')
           ) AS environment,
           dt.target_count AS targetCount,
           dt.success_count AS successCount,
           dt.fail_count AS failCount,
           dt.created_at AS createdAt,
           dt.started_at AS startedAt,
           dt.finished_at AS finishedAt
    FROM deploy_task dt
    JOIN app_definition ad ON ad.id = dt.app_id
    LEFT JOIN deploy_task_param dtp ON dtp.task_id = dt.id
    WHERE dt.deleted = 0
      AND NOT EXISTS (
        SELECT 1
        FROM unified_task_center utc
        WHERE utc.task_type = 'deploy'
          AND utc.source_id = dt.id
      )
    GROUP BY dt.id, dt.task_name, dt.status, dt.operator_name, ad.app_name,
             dt.target_count, dt.success_count, dt.fail_count, dt.created_at, dt.started_at, dt.finished_at
    ORDER BY dt.id ASC
    """)
List<DeployBackfillRow> findDeployRowsMissingProjection();
```

```java
@Component
public class UnifiedTaskCenterBackfillRunner implements ApplicationRunner {
  private final UnifiedTaskCenterMapper unifiedTaskCenterMapper;
  private final UnifiedTaskRecorder unifiedTaskRecorder;
  private final UnifiedTaskDetailPreviewFactory previewFactory;

  @Override
  public void run(ApplicationArguments args) {
    for (UnifiedTaskCenterMapper.DeployBackfillRow row : unifiedTaskCenterMapper.findDeployRowsMissingProjection()) {
      String unifiedStatus = UnifiedTaskCenterApplicationService.normalizeStatus(row.status());
      String summary = String.format(
          "发布任务 %s，目标 %d，成功 %d，失败 %d",
          row.taskName(),
          row.targetCount(),
          row.successCount(),
          row.failCount());
      unifiedTaskRecorder.upsertBySource(new UnifiedTaskRecorder.UpsertBySourceCommand(
          "deploy",
          row.sourceId(),
          row.taskName(),
          unifiedStatus,
          row.operatorName(),
          row.startedAt() == null ? row.createdAt() : row.startedAt(),
          row.finishedAt(),
          summary,
          previewFactory.toJson(previewFactory.buildDeployPreview(
              row.appName(),
              row.environment(),
              row.targetCount(),
              row.successCount(),
              row.failCount(),
              row.status(),
              "/deploy/task?taskId=" + row.sourceId())),
          "/deploy/task?taskId=" + row.sourceId(),
          "deploy",
          unifiedStatus.equals("failed") ? row.status() : null));
    }
  }
}
```

- [ ] **Step 5: Sync Deploy create/approve/reject and execution lifecycle into the unified projection**

```java
// DeployTaskApplicationService.createDeployTask(...)
String environment = firstNonBlank(
    validated.params().get("environment"),
    validated.params().get("env"),
    validated.params().get("profile"),
    validated.params().get("namespace"));
String summary = String.format("发布 %s 到 %s，目标 %d 台主机，等待执行", validated.app().getAppName(), environment == null ? "default" : environment, validated.hostIds().size());
unifiedTaskRecorder.upsertBySource(new UnifiedTaskRecorder.UpsertBySourceCommand(
    "deploy",
    entity.getId(),
    entity.getTaskName(),
    "pending",
    resolvedOperatorName,
    now,
    null,
    summary,
    unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDeployPreview(
        validated.app().getAppName(),
        environment,
        entity.getTargetCount(),
        entity.getSuccessCount(),
        entity.getFailCount(),
        entity.getStatus(),
        "/deploy/task?taskId=" + entity.getId())),
    "/deploy/task?taskId=" + entity.getId(),
    "deploy",
    null));
```

```java
// DeployTaskApplicationService.decideApproval(...)
String unifiedStatus = STATUS_REJECTED.equals(decisionStatus) ? "failed" : "pending";
String environment = firstNonBlank(
    params.get("environment"),
    params.get("env"),
    params.get("profile"),
    params.get("namespace"));
unifiedTaskRecorder.updateBySource(new UnifiedTaskRecorder.UpdateBySourceCommand(
    "deploy",
    taskId,
    unifiedStatus,
    STATUS_REJECTED.equals(decisionStatus) ? now : null,
    STATUS_REJECTED.equals(decisionStatus) ? "发布任务已拒绝" : "发布任务已审批，等待执行",
    unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDeployPreview(
        row.getAppName(),
        environment,
        row.getTargetCount(),
        row.getSuccessCount(),
        row.getFailCount(),
        decisionStatus,
        "/deploy/task?taskId=" + taskId)),
    STATUS_REJECTED.equals(decisionStatus) ? comment : null));
```

```java
// DeployTaskExecutionApplicationService.executeDeployTask(...)
runInTransaction(() -> {
  markTaskRunning(taskId, resolvedOperatorName);
  unifiedTaskRecorder.updateBySource(new UnifiedTaskRecorder.UpdateBySourceCommand(
      "deploy",
      taskId,
      "running",
      null,
      "发布任务执行中",
      null,
      null));
});
```

```java
// DeployTaskExecutionApplicationService.runTask(...)
String taskStatus = failCount > 0 ? STATUS_FAILED : STATUS_SUCCESS;
finalizeTask(taskId, taskStatus, successCount, failCount, operatorName, "Task finished with status " + taskStatus);
DeployTaskApplicationService.DeployTaskRecord record = deployTaskApplicationService.getDeployTaskRecord(taskId);
String environment = record.getEnvironment();
unifiedTaskRecorder.updateBySource(new UnifiedTaskRecorder.UpdateBySourceCommand(
    "deploy",
    taskId,
    UnifiedTaskCenterApplicationService.normalizeStatus(taskStatus),
    LocalDateTime.now(),
    String.format("发布 %s 到 %s，目标 %d，成功 %d，失败 %d", record.getAppName(), environment == null ? "default" : environment, successCount + failCount, successCount, failCount),
    unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDeployPreview(
        record.getAppName(),
        environment,
        successCount + failCount,
        successCount,
        failCount,
        taskStatus,
        "/deploy/task?taskId=" + taskId)),
    STATUS_FAILED.equals(taskStatus) ? "Deploy host execution failed" : null));
```

```java
// DeployTaskExecutionApplicationService.cancelPendingTask(...) and requestTaskCancellation(...)
unifiedTaskRecorder.updateBySource(new UnifiedTaskRecorder.UpdateBySourceCommand(
    "deploy",
    taskId,
    updated == 0 ? "pending" : "failed",
    updated == 0 ? null : now,
    updated == 0 ? "发布任务等待取消" : "发布任务已取消",
    null,
    updated == 0 ? null : "Task cancelled"));
```

```java
// DeployTaskExecutionApplicationService.createRollbackTask(...)
unifiedTaskRecorder.upsertBySource(new UnifiedTaskRecorder.UpsertBySourceCommand(
    "deploy",
    rollbackTaskId,
    rollbackTaskName,
    "pending",
    operatorName,
    now,
    null,
    "回滚任务已创建，等待执行",
    unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDeployPreview(
        appName,
        environment,
        targetCount,
        0,
        0,
        "PENDING",
        "/deploy/task?taskId=" + rollbackTaskId)),
    "/deploy/task?taskId=" + rollbackTaskId,
    "deploy",
    null));
```

- [ ] **Step 6: Run the Deploy sync tests to verify they pass**

Run: `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest test`
Expected: PASS with create, backfill, and runtime synchronization covered.

- [ ] **Step 7: Commit**

```bash
git add backend/envops-deploy/pom.xml backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskApplicationService.java backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskExecutionApplicationService.java backend/envops-task backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java
git commit -m "$(cat <<'EOF'
feat: sync deploy tasks into unified task center
EOF
)"
```

### Task 4: Record database connectivity checks as unified tasks

**Files:**
- Modify: `backend/envops-asset/pom.xml`
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectivityService.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskRecorder.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskDetailPreviewFactory.java`
- Modify Test: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java`

- [ ] **Step 1: Write the failing unit test for one-batch-one-task semantics**

```java
@Test
void checkSelectedDatabasesCreatesExactlyOneFailedUnifiedBatchTask() {
  AssetDatabaseMapper mapper = mock(AssetDatabaseMapper.class);
  DatabaseConnectionSecretProtector secretProtector = mock(DatabaseConnectionSecretProtector.class);
  DatabaseConnectivityChecker mysqlChecker = new StubChecker("mysql", true, "connected");
  DatabaseConnectivityChecker postgresqlChecker = new StubChecker("postgresql", false, "认证失败");
  DatabaseConnectionFactory factory = new DatabaseConnectionFactory(List.of(mysqlChecker, postgresqlChecker));
  UnifiedTaskRecorder unifiedTaskRecorder = mock(UnifiedTaskRecorder.class);
  UnifiedTaskDetailPreviewFactory previewFactory = new UnifiedTaskDetailPreviewFactory();

  DatabaseConnectivityService service = new DatabaseConnectivityService(
      mapper,
      factory,
      secretProtector,
      unifiedTaskRecorder,
      previewFactory);

  when(unifiedTaskRecorder.start(any())).thenReturn(9002L);
  when(mapper.findDatabasesByIds(List.of(11L, 12L, 13L))).thenReturn(List.of(
      databaseRow(11L, "order_prod", "mysql", "10.20.1.11", 3306, "orders_app", "sealed:mysql"),
      databaseRow(12L, "traffic_gate", "postgresql", "10.20.1.12", 5432, "traffic_app", "sealed:pg"),
      databaseRow(13L, "session_hub", "redis", "10.20.1.13", 6379, null, null)));
  when(secretProtector.reveal("sealed:mysql")).thenReturn("Orders@123456");
  when(secretProtector.reveal("sealed:pg")).thenReturn("Traffic@123456");

  DatabaseConnectivityService.DatabaseConnectivityReport report = service.checkSelectedDatabases(List.of(11L, 12L, 13L));

  assertThat(report.summary().total()).isEqualTo(3);
  assertThat(report.summary().failed()).isEqualTo(1);

  verify(unifiedTaskRecorder, times(1)).start(any());
  verify(unifiedTaskRecorder, times(1)).update(any());
}
```

- [ ] **Step 2: Run the database connectivity unit test to verify it fails**

Run: `mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectivityServiceTest test`
Expected: FAIL because database connectivity checks do not write unified task records yet.

- [ ] **Step 3: Add the `envops-task` dependency to Asset and inject the recorder**

```xml
<dependency>
  <groupId>com.img.envops</groupId>
  <artifactId>envops-task</artifactId>
  <version>${project.version}</version>
</dependency>
```

```java
public DatabaseConnectivityService(AssetDatabaseMapper assetDatabaseMapper,
                                   DatabaseConnectionFactory databaseConnectionFactory,
                                   DatabaseConnectionSecretProtector databaseConnectionSecretProtector,
                                   UnifiedTaskRecorder unifiedTaskRecorder,
                                   UnifiedTaskDetailPreviewFactory unifiedTaskDetailPreviewFactory) {
  this.assetDatabaseMapper = assetDatabaseMapper;
  this.databaseConnectionFactory = databaseConnectionFactory;
  this.databaseConnectionSecretProtector = databaseConnectionSecretProtector;
  this.unifiedTaskRecorder = unifiedTaskRecorder;
  this.unifiedTaskDetailPreviewFactory = unifiedTaskDetailPreviewFactory;
}
```

- [ ] **Step 4: Create one unified task for each real single or batch check**

```java
public DatabaseConnectivityReport checkOneDatabase(Long id) {
  if (id == null || id < 1) {
    throw new IllegalArgumentException("id is required");
  }

  List<AssetDatabaseMapper.DatabaseRow> rows = uniqueRows(assetDatabaseMapper.findDatabasesByIds(List.of(id)));
  if (rows.isEmpty()) {
    throw new IllegalArgumentException("database not found: " + id);
  }

  AssetDatabaseMapper.DatabaseRow row = rows.get(0);
  Long unifiedTaskId = unifiedTaskRecorder.start(new UnifiedTaskRecorder.CreateCommand(
      "database_connectivity",
      "检测数据库连通性",
      "running",
      null,
      LocalDateTime.now(),
      "数据库连通性检测执行中",
      unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDatabaseConnectivityPreview(
          false,
          "数据库连通性检测执行中",
          1,
          0,
          0,
          0,
          "/asset/database",
          null)),
      row.getId(),
      "/asset/database",
      "asset",
      null));

  DatabaseConnectivityReport report = run(rows);
  finishDatabaseConnectivityTask(unifiedTaskId, false, row.getId(), report);
  return report;
}
```

```java
public DatabaseConnectivityReport checkSelectedDatabases(List<Long> ids) {
  if (ids == null || ids.isEmpty()) {
    throw new IllegalArgumentException("ids are required");
  }
  List<Long> normalizedIds = ids.stream().filter(id -> id != null && id > 0).distinct().toList();
  if (normalizedIds.isEmpty()) {
    throw new IllegalArgumentException("ids are required");
  }

  List<AssetDatabaseMapper.DatabaseRow> rows = uniqueRows(assetDatabaseMapper.findDatabasesByIds(normalizedIds));
  Long unifiedTaskId = unifiedTaskRecorder.start(new UnifiedTaskRecorder.CreateCommand(
      "database_connectivity",
      "批量数据库连通性检测",
      "running",
      null,
      LocalDateTime.now(),
      "批量数据库连通性检测执行中",
      unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDatabaseConnectivityPreview(
          true,
          "批量数据库连通性检测执行中",
          rows.size(),
          0,
          0,
          0,
          "/asset/database",
          null)),
      null,
      "/asset/database",
      "asset",
      null));

  DatabaseConnectivityReport report = run(rows);
  finishDatabaseConnectivityTask(unifiedTaskId, true, null, report);
  return report;
}
```

- [ ] **Step 5: Finalize the unified database task with the approved summary and failure semantics**

```java
private void finishDatabaseConnectivityTask(
    Long unifiedTaskId,
    boolean batch,
    Long sourceId,
    DatabaseConnectivityReport report) {
  String summary = String.format(
      "%s %d 条，成功 %d，失败 %d，跳过 %d",
      batch ? "批量检测" : "检测",
      report.summary().total(),
      report.summary().success(),
      report.summary().failed(),
      report.summary().skipped());
  String status = report.summary().failed() > 0 ? "failed" : "success";
  String errorSummary = report.summary().failed() > 0 ? "数据库连通性检测存在失败项" : null;

  unifiedTaskRecorder.update(new UnifiedTaskRecorder.UpdateCommand(
      unifiedTaskId,
      status,
      LocalDateTime.now(),
      summary,
      unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildDatabaseConnectivityPreview(
          batch,
          summary,
          report.summary().total(),
          report.summary().success(),
          report.summary().failed(),
          report.summary().skipped(),
          "/asset/database",
          errorSummary)),
      errorSummary));
}
```

- [ ] **Step 6: Run the database connectivity unit test to verify it passes**

Run: `mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectivityServiceTest test`
Expected: PASS with one-batch-one-task and batch failure semantics covered.

- [ ] **Step 7: Commit**

```bash
git add backend/envops-asset/pom.xml backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectivityService.java backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java backend/envops-task
git commit -m "$(cat <<'EOF'
feat: record database connectivity tasks in task center
EOF
)"
```

### Task 5: Record Traffic actions as unified tasks

**Files:**
- Modify: `backend/envops-traffic/pom.xml`
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskRecorder.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskDetailPreviewFactory.java`
- Modify Test: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`

- [ ] **Step 1: Write the failing Traffic task-center tests**

```java
@Test
void applyPolicyCreatesUnifiedSuccessTaskWithRollbackAvailability() throws Exception {
  trafficRestServer.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json")
      .setBody("""
          {"success":true,"message":"apply ok","rollbackToken":"rbk-001"}
          """));

  mockMvc.perform(post("/api/traffic/policies/1001/apply")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk());

  mockMvc.perform(get("/api/task-center/tasks")
          .param("taskType", "traffic_action")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.records[0].status").value("success"))
      .andExpect(jsonPath("$.data.records[0].summary").value(org.hamcrest.Matchers.containsString("Apply")));
}

@Test
void applyPolicyWithoutRollbackTokenCreatesFailedUnifiedTask() throws Exception {
  trafficRestServer.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json")
      .setBody("""
          {"success":true,"message":"apply ok"}
          """));

  mockMvc.perform(post("/api/traffic/policies/1001/apply")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isBadRequest());

  mockMvc.perform(get("/api/task-center/tasks")
          .param("taskType", "traffic_action")
          .param("status", "failed")
          .header("Authorization", "Bearer " + accessToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.records[0].errorSummary").value(org.hamcrest.Matchers.containsString("rollbackToken")));
}
```

- [ ] **Step 2: Run the Traffic tests to verify they fail**

Run: `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test`
Expected: FAIL because Traffic actions do not create unified task records yet.

- [ ] **Step 3: Add the `envops-task` dependency to Traffic**

```xml
<dependency>
  <groupId>com.img.envops</groupId>
  <artifactId>envops-task</artifactId>
  <version>${project.version}</version>
</dependency>
```

- [ ] **Step 4: Create unified tasks only after the request passes the non-executable guards**

```java
public TrafficPolicyActionRecord previewPolicy(Long policyId) {
  TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
  validateMvpScope(policy);
  TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "preview");

  Long unifiedTaskId = unifiedTaskRecorder.start(new UnifiedTaskRecorder.CreateCommand(
      "traffic_action",
      "Traffic Preview",
      "running",
      null,
      LocalDateTime.now(),
      "Preview 执行中",
      unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildTrafficActionPreview(
          "preview",
          policy.getApp(),
          policy.getStrategy(),
          policy.getPluginType(),
          false,
          "/traffic/controller",
          null)),
      policy.getId(),
      "/traffic/controller",
      "traffic",
      null));

  try {
    TrafficPluginResult pluginResult = plugin.preview(buildActionRequest(policy));
    TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "PREVIEW", normalizeOptionalText(pluginResult.rollbackToken()));
    finishTrafficTask(unifiedTaskId, "preview", policy, pluginResult, "success", null);
    return new TrafficPolicyActionRecord("preview", updatedPolicy, pluginResult);
  } catch (RuntimeException exception) {
    finishTrafficTask(unifiedTaskId, "preview", policy, null, "failed", exception.getMessage());
    throw exception;
  }
}
```

```java
public TrafficPolicyActionRecord applyPolicy(Long policyId) {
  TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
  validateMvpScope(policy);
  TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "apply");

  Long unifiedTaskId = unifiedTaskRecorder.start(new UnifiedTaskRecorder.CreateCommand(
      "traffic_action",
      "Traffic Apply",
      "running",
      null,
      LocalDateTime.now(),
      "Apply 执行中",
      unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildTrafficActionPreview(
          "apply",
          policy.getApp(),
          policy.getStrategy(),
          policy.getPluginType(),
          false,
          "/traffic/controller",
          null)),
      policy.getId(),
      "/traffic/controller",
      "traffic",
      null));

  try {
    TrafficPluginResult pluginResult = plugin.apply(buildActionRequest(policy));
    String rollbackToken = normalizeOptionalText(pluginResult.rollbackToken());
    if (rollbackToken == null) {
      finishTrafficTask(unifiedTaskId, "apply", policy, pluginResult, "failed", "rollbackToken is required from traffic rest service");
      throw new IllegalArgumentException("rollbackToken is required from traffic rest service for apply: " + policyId);
    }
    TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "ENABLED", rollbackToken);
    finishTrafficTask(unifiedTaskId, "apply", policy, pluginResult, "success", null);
    return new TrafficPolicyActionRecord("apply", updatedPolicy, pluginResult);
  } catch (RuntimeException exception) {
    finishTrafficTask(unifiedTaskId, "apply", policy, null, "failed", exception.getMessage());
    throw exception;
  }
}
```

```java
public TrafficPolicyActionRecord rollbackPolicy(Long policyId) {
  TrafficPolicyMapper.TrafficPolicyRow policy = requirePolicy(policyId);
  validateMvpScope(policy);
  String rollbackToken = normalizeOptionalText(policy.getRollbackToken());
  if (rollbackToken == null) {
    throw new IllegalArgumentException("rollbackToken is required for policy: " + policyId);
  }
  TrafficPlugin plugin = requirePluginSupport(policy.getPluginType(), "rollback");

  Long unifiedTaskId = unifiedTaskRecorder.start(new UnifiedTaskRecorder.CreateCommand(
      "traffic_action",
      "Traffic Rollback",
      "running",
      null,
      LocalDateTime.now(),
      "Rollback 执行中",
      unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildTrafficActionPreview(
          "rollback",
          policy.getApp(),
          policy.getStrategy(),
          policy.getPluginType(),
          true,
          "/traffic/controller",
          null)),
      policy.getId(),
      "/traffic/controller",
      "traffic",
      null));

  try {
    TrafficPluginResult pluginResult = plugin.rollback(new TrafficRollbackRequest(policy.getApp(), rollbackToken, "manual rollback"));
    TrafficPolicyRecord updatedPolicy = updatePolicyState(policy.getId(), "ROLLED_BACK", rollbackToken);
    finishTrafficTask(unifiedTaskId, "rollback", policy, pluginResult, "success", null);
    return new TrafficPolicyActionRecord("rollback", updatedPolicy, pluginResult);
  } catch (RuntimeException exception) {
    finishTrafficTask(unifiedTaskId, "rollback", policy, null, "failed", exception.getMessage());
    throw exception;
  }
}
```

- [ ] **Step 5: Shape the Traffic preview payload and completion update once**

```java
private void finishTrafficTask(
    Long unifiedTaskId,
    String action,
    TrafficPolicyMapper.TrafficPolicyRow policy,
    TrafficPluginResult pluginResult,
    String status,
    String errorSummary) {
  boolean rollbackTokenAvailable = pluginResult != null && normalizeOptionalText(pluginResult.rollbackToken()) != null;
  String summary = String.format(
      "%s %s，策略 %s，插件 %s",
      action.substring(0, 1).toUpperCase() + action.substring(1),
      policy.getApp(),
      policy.getStrategy(),
      policy.getPluginType());

  unifiedTaskRecorder.update(new UnifiedTaskRecorder.UpdateCommand(
      unifiedTaskId,
      status,
      LocalDateTime.now(),
      summary,
      unifiedTaskDetailPreviewFactory.toJson(unifiedTaskDetailPreviewFactory.buildTrafficActionPreview(
          action,
          policy.getApp(),
          policy.getStrategy(),
          policy.getPluginType(),
          rollbackTokenAvailable,
          "/traffic/controller",
          errorSummary)),
      errorSummary));
}
```

- [ ] **Step 6: Run the Traffic tests to verify they pass**

Run: `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test`
Expected: PASS with Traffic success/failure records visible in Task Center.

- [ ] **Step 7: Commit**

```bash
git add backend/envops-traffic/pom.xml backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java backend/envops-task backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java
git commit -m "$(cat <<'EOF'
feat: record traffic actions in task center
EOF
)"
```

### Task 6: Replace the deploy-only frontend Task Center with the unified list, filters, and detail drawer

**Files:**
- Modify: `frontend/src/typings/api/task.d.ts`
- Modify: `frontend/src/service/api/task.ts`
- Modify: `frontend/src/views/task/shared/query.ts`
- Modify: `frontend/src/views/task/center/index.vue`
- Modify: `frontend/src/locales/langs/zh-cn.ts`
- Modify: `frontend/src/locales/langs/en-us.ts`
- Modify Test: `frontend/src/views/task/task-contract.spec.ts`

- [ ] **Step 1: Write the failing frontend contract tests for unified filters and drawer flow**

```ts
it('drives unified task center filters and detail drawer flow', () => {
  expect(taskCenterPage).toContain("startedFrom");
  expect(taskCenterPage).toContain("startedTo");
  expect(taskCenterPage).toContain("fetchGetTaskCenterTaskDetail");
  expect(taskCenterPage).toContain("showTaskDetailDrawer.value = true");
  expect(taskCenterPage).toContain("router.push(activeTaskDetail.value.sourceRoute)");
  expect(taskCenterPage).not.toContain('filterForm.priority');
  expect(taskCenterPage).not.toContain("routerPushByKey('deploy_task'");
});

it('keeps Task Center copy aligned with the unified scope we ship', () => {
  const taskCenterZhBlock = extractSection(zhLocaleSource, 'taskCenter', 'trafficController');
  const taskCenterEnBlock = extractSection(enLocaleSource, 'taskCenter', 'trafficController');

  expect(taskCenterZhBlock).toContain('统一任务中心');
  expect(taskCenterZhBlock).toContain('数据库检测');
  expect(taskCenterZhBlock).toContain('Traffic 动作');
  expect(taskCenterEnBlock).toContain('Unified Task Center');
  expect(taskCenterEnBlock).toContain('Database Connectivity');
  expect(taskCenterEnBlock).toContain('Traffic Action');
});
```

- [ ] **Step 2: Run the frontend contract tests to verify they fail**

Run: `pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts`
Expected: FAIL because the page still uses deploy-only filters and direct Deploy navigation.

- [ ] **Step 3: Replace deploy-only typings and add the detail API**

```ts
export interface TaskCenterListQuery {
  keyword?: string;
  taskType?: 'deploy' | 'database_connectivity' | 'traffic_action';
  status?: 'pending' | 'running' | 'success' | 'failed';
  startedFrom?: string;
  startedTo?: string;
  page: number;
  pageSize: number;
}

export interface TaskCenterPage {
  page: number;
  pageSize: number;
  total: number;
  records: TaskCenterRecord[];
}

export interface TaskCenterRecord {
  id: number;
  taskType: string;
  taskName: string;
  status: string;
  triggeredBy: string;
  startedAt?: string | null;
  finishedAt?: string | null;
  summary: string;
  sourceRoute: string;
  errorSummary?: string | null;
}

export interface TaskCenterDetail extends TaskCenterRecord {
  detailPreview: Record<string, unknown>;
}
```

```ts
export function fetchGetTaskCenterTasks(params: Api.Task.TaskCenterListQuery) {
  return request<Api.Task.TaskCenterPage>({
    url: '/api/task-center/tasks',
    params
  });
}

export function fetchGetTaskCenterTaskDetail(id: number) {
  return request<Api.Task.TaskCenterDetail>({
    url: `/api/task-center/tasks/${id}`
  });
}
```

- [ ] **Step 4: Replace route-query normalization with the approved minimal filter set**

```ts
export type TaskCenterRouteQuery = {
  keyword: string;
  taskType: string;
  status: string;
  startedFrom: string;
  startedTo: string;
  page: number;
  pageSize: number;
};

export function normalizeTaskCenterRouteQuery(query: Record<string, unknown>): TaskCenterRouteQuery {
  return {
    keyword: normalizeString(query.keyword),
    taskType: normalizeString(query.taskType),
    status: normalizeString(query.status),
    startedFrom: normalizeString(query.startedFrom),
    startedTo: normalizeString(query.startedTo),
    page: normalizePositiveInt(query.page, 1),
    pageSize: normalizePositiveInt(query.pageSize, 10)
  };
}

export function toTaskCenterApiQuery(query: TaskCenterRouteQuery): Api.Task.TaskCenterListQuery {
  return {
    ...(query.keyword ? { keyword: query.keyword } : {}),
    ...(query.taskType ? { taskType: query.taskType as Api.Task.TaskCenterListQuery['taskType'] } : {}),
    ...(query.status ? { status: query.status as Api.Task.TaskCenterListQuery['status'] } : {}),
    ...(query.startedFrom ? { startedFrom: query.startedFrom } : {}),
    ...(query.startedTo ? { startedTo: query.startedTo } : {}),
    page: query.page,
    pageSize: query.pageSize
  };
}
```

- [ ] **Step 5: Implement the unified page behavior with drawer-first interaction**

```ts
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useRouterPush } from '@/hooks/common/router';
import { fetchGetTaskCenterTaskDetail, fetchGetTaskCenterTasks } from '@/service/api';
import { normalizeTaskCenterRouteQuery, toTaskCenterApiQuery } from '@/views/task/shared/query';

const route = useRoute();
const router = useRouter();
const { routerPushByKey } = useRouterPush();
const { t } = useI18n();

const filterForm = reactive({
  keyword: '',
  taskType: null as string | null,
  status: null as string | null,
  startedFrom: null as string | null,
  startedTo: null as string | null
});

const taskList = ref<Api.Task.TaskCenterRecord[]>([]);
const total = ref(0);
const loading = ref(false);
const showTaskDetailDrawer = ref(false);
const taskDetailLoading = ref(false);
const activeTaskDetail = ref<Api.Task.TaskCenterDetail | null>(null);

async function handleOpenTaskDetail(taskId: number) {
  taskDetailLoading.value = true;
  const { data, error } = await fetchGetTaskCenterTaskDetail(taskId);
  if (!error) {
    activeTaskDetail.value = data ?? null;
    showTaskDetailDrawer.value = true;
  }
  taskDetailLoading.value = false;
}

async function openSourceDetail() {
  if (!activeTaskDetail.value?.sourceRoute) {
    return;
  }
  await router.push(activeTaskDetail.value.sourceRoute);
}
</script>
```

```vue
<NButton text type="primary" @click="handleOpenTaskDetail(item.id)">
  {{ t('page.envops.taskCenter.actions.openTaskDetail') }}
</NButton>

<NDrawer v-model:show="showTaskDetailDrawer" :width="520">
  <NDrawerContent :title="activeTaskDetail?.taskName || ''">
    <NSpin :show="taskDetailLoading">
      <NDescriptions bordered :column="1">
        <NDescriptionsItem :label="t('page.envops.taskCenter.fields.taskType')">
          {{ activeTaskDetail?.taskType || '-' }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="t('page.envops.taskCenter.fields.status')">
          {{ activeTaskDetail?.status || '-' }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="t('page.envops.taskCenter.fields.triggeredBy')">
          {{ activeTaskDetail?.triggeredBy || '-' }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="t('page.envops.taskCenter.fields.summary')">
          {{ activeTaskDetail?.summary || '-' }}
        </NDescriptionsItem>
        <NDescriptionsItem v-if="activeTaskDetail?.errorSummary" :label="t('page.envops.taskCenter.fields.errorSummary')">
          {{ activeTaskDetail.errorSummary }}
        </NDescriptionsItem>
      </NDescriptions>
      <NCode :code="JSON.stringify(activeTaskDetail?.detailPreview ?? {}, null, 2)" language="json" />
      <NButton type="primary" @click="openSourceDetail">
        {{ t('page.envops.taskCenter.actions.openSourceDetail') }}
      </NButton>
    </NSpin>
  </NDrawerContent>
</NDrawer>
```

- [ ] **Step 6: Update the Task Center test harness and locale copy**

```ts
// frontend/src/views/task/task-contract.spec.ts
const fetchGetTaskCenterTaskDetail = vi.fn();
const routerPush = vi.fn(async (_path: string) => {});

vi.mock('vue-router', async () => {
  const { reactive } = await import('vue');
  const route = reactive(mocks.route);
  mocks.route = route;

  return {
    useRoute: () => route,
    useRouter: () => ({ push: mocks.routerPush })
  };
});

vi.mock('@/service/api', () => ({
  fetchGetTaskCenterTasks: mocks.fetchGetTaskCenterTasks,
  fetchGetTaskCenterTaskDetail: mocks.fetchGetTaskCenterTaskDetail,
  // keep the other existing mocks
}));
```

```ts
// frontend/src/locales/langs/zh-cn.ts
taskCenter: {
  title: '统一任务中心',
  subtitle: '统一查看 Deploy、数据库检测和 Traffic 动作任务。',
  filters: {
    keyword: '关键词',
    taskType: '任务类型',
    status: '状态',
    startedFrom: '开始时间起',
    startedTo: '开始时间止'
  },
  actions: {
    openTaskDetail: '查看任务详情',
    openSourceDetail: '查看原始详情'
  },
  fields: {
    taskType: '任务类型',
    status: '状态',
    triggeredBy: '发起人',
    summary: '摘要',
    errorSummary: '失败原因'
  },
  taskTypes: {
    deploy: 'Deploy',
    databaseConnectivity: '数据库检测',
    trafficAction: 'Traffic 动作'
  }
}
```

```ts
// frontend/src/locales/langs/en-us.ts
taskCenter: {
  title: 'Unified Task Center',
  subtitle: 'View Deploy, Database Connectivity, and Traffic Action tasks in one place.',
  filters: {
    keyword: 'Keyword',
    taskType: 'Task Type',
    status: 'Status',
    startedFrom: 'Started From',
    startedTo: 'Started To'
  },
  actions: {
    openTaskDetail: 'View Task Detail',
    openSourceDetail: 'View Source Detail'
  },
  fields: {
    taskType: 'Task Type',
    status: 'Status',
    triggeredBy: 'Triggered By',
    summary: 'Summary',
    errorSummary: 'Failure Reason'
  },
  taskTypes: {
    deploy: 'Deploy',
    databaseConnectivity: 'Database Connectivity',
    trafficAction: 'Traffic Action'
  }
}
```

- [ ] **Step 7: Run the frontend contract tests to verify they pass**

Run: `pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts`
Expected: PASS with unified filter, drawer, and source-route assertions.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/typings/api/task.d.ts frontend/src/service/api/task.ts frontend/src/views/task/shared/query.ts frontend/src/views/task/center/index.vue frontend/src/locales/langs/zh-cn.ts frontend/src/locales/langs/en-us.ts frontend/src/views/task/task-contract.spec.ts
git commit -m "$(cat <<'EOF'
feat: add unified task center frontend
EOF
)"
```

### Task 7: Sync docs and release material to the unified Task Center behavior

**Files:**
- Modify: `README.md`
- Modify: `docs/envops-开发技术说明.md`
- Modify: `docs/envops-用户操作手册.md`
- Create: `release/0.0.6-release-notes.md`

- [ ] **Step 1: Write the failing doc contract check**

```bash
python - <<'PY'
from pathlib import Path
readme = Path('README.md').read_text(encoding='utf-8')
tech = Path('docs/envops-开发技术说明.md').read_text(encoding='utf-8')
manual = Path('docs/envops-用户操作手册.md').read_text(encoding='utf-8')
assert 'deploy-only 队列视图' not in readme
assert 'deploy-only 队列视图' not in tech
assert 'deploy-only 队列视图' not in manual
assert '统一任务中心' in readme
assert 'database_connectivity' in tech
assert '轻量详情抽屉' in manual
PY
```

- [ ] **Step 2: Run the doc check to verify it fails**

Run: `python - <<'PY'
from pathlib import Path
readme = Path('README.md').read_text(encoding='utf-8')
tech = Path('docs/envops-开发技术说明.md').read_text(encoding='utf-8')
manual = Path('docs/envops-用户操作手册.md').read_text(encoding='utf-8')
assert 'deploy-only 队列视图' not in readme
assert 'deploy-only 队列视图' not in tech
assert 'deploy-only 队列视图' not in manual
assert '统一任务中心' in readme
assert 'database_connectivity' in tech
assert '轻量详情抽屉' in manual
PY`
Expected: FAIL because current docs still describe a deploy-only Task Center.

- [ ] **Step 3: Update the README current-baseline wording**

```md
## 当前对外口径

2. **Task Center**
   - 当前提供最小真实统一任务中心。
   - 统一展示 Deploy、数据库连通性检测、Traffic 动作三类任务。
   - 支持任务类型、状态、时间范围、关键词筛选。
   - 点击任务后先打开轻量详情抽屉，再通过“查看原始详情”返回原模块。
```

- [ ] **Step 4: Update the technical and user docs**

```md
### 5.3 Task Center

Task Center 当前已收敛为最小真实统一任务中心：

- 统一纳入 `deploy`、`database_connectivity`、`traffic_action` 三类任务
- 统一状态口径：`pending`、`running`、`success`、`failed`
- Deploy 历史任务通过统一投影补录
- 数据库检测与 Traffic 动作从当前版本起记录新增任务
- 数据库批量检测按一批一条记录
- 页面先展示统一列表，再打开轻量详情抽屉，最后再深链回原模块
```

```md
### 4.3 Task Center

Task Center 当前可统一查看三类真实任务：

- Deploy 任务
- 数据库连通性检测任务
- Traffic 动作任务

点击任务后会先打开轻量详情抽屉，查看基础信息、摘要、失败原因或汇总信息；如果需要更深内容，可继续点击“查看原始详情”跳回原模块。
```

- [ ] **Step 5: Create the 0.0.6 release notes**

```md
# EnvOps 0.0.6 Release Notes

## Summary

- Task Center 从 deploy-only 队列视图升级为最小真实统一任务中心
- 统一纳入 Deploy、数据库连通性检测、Traffic 动作三类任务
- 新增轻量详情抽屉与原模块深链
- Deploy 历史已补录，数据库检测与 Traffic 从 0.0.6 起记录新增任务

## Validation

- `mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test`
- `mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectivityServiceTest test`
- `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest,AssetControllerTest,TrafficControllerTest test`
- `pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
```

- [ ] **Step 6: Run the doc check to verify it passes**

Run: `python - <<'PY'
from pathlib import Path
readme = Path('README.md').read_text(encoding='utf-8')
tech = Path('docs/envops-开发技术说明.md').read_text(encoding='utf-8')
manual = Path('docs/envops-用户操作手册.md').read_text(encoding='utf-8')
assert 'deploy-only 队列视图' not in readme
assert 'deploy-only 队列视图' not in tech
assert 'deploy-only 队列视图' not in manual
assert '统一任务中心' in readme
assert 'database_connectivity' in tech
assert '轻量详情抽屉' in manual
PY`
Expected: PASS with docs aligned to current behavior.

- [ ] **Step 7: Commit**

```bash
git add README.md docs/envops-开发技术说明.md docs/envops-用户操作手册.md release/0.0.6-release-notes.md
git commit -m "$(cat <<'EOF'
docs: update task center materials for 0.0.6
EOF
)"
```

### Task 8: Run final verification against the approved scope only

**Files:**
- Test: `backend/envops-task/src/test/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationServiceTest.java`
- Test: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`
- Test: `frontend/src/views/task/task-contract.spec.ts`
- Test: `frontend/src/store/modules/__tests__/route-envops.spec.ts`

- [ ] **Step 1: Run the unified task backend unit verification**

Run: `mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test`
Expected: PASS with status mapping and preview shaping covered.

- [ ] **Step 2: Run the database connectivity service verification**

Run: `mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectivityServiceTest test`
Expected: PASS with one-batch-one-task and failure semantics covered.

- [ ] **Step 3: Run the Boot integration verification**

Run: `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest,AssetControllerTest,TrafficControllerTest test`
Expected: PASS with unified list/detail, Deploy synchronization, Asset controller regression, and Traffic task recording covered.

- [ ] **Step 4: Run the frontend targeted verification**

Run: `pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
Expected: PASS with unified copy, filter model, drawer behavior, and route regression covered.

- [ ] **Step 5: Run frontend typecheck and build**

Run: `pnpm --dir frontend typecheck && pnpm --dir frontend build`
Expected: PASS with no type errors and a successful production build.

- [ ] **Step 6: Compare the implementation against the approved v0.0.6 scope**

```text
Checklist:
- Only three task types are visible: deploy / database_connectivity / traffic_action
- Unified list fields include task type, task name, status, triggeredBy, startedAt, finishedAt, summary, actions
- Filters include task type, status, time range, keyword
- Drawer shows base info, summary, failure reason or batch aggregate, and task-specific preview
- Deep link goes back to the source module
- Deploy history is backfilled
- Database and Traffic only record new tasks from v0.0.6 onward
- Batch DB checks appear as one batch = one task
- No retry, cancel, orchestration, full historical backfill, or new execution engine abstraction was added to Task Center
```

Expected: Every approved requirement maps to code or docs, with no out-of-scope additions.

- [ ] **Step 7: Do not create a verification-only commit**

```text
If verification finds a defect, fix it in the owning task, rerun that task’s verification command, and keep the fix in that task’s commit flow. Do not add a catch-all “verification fixes” commit.
```

---

## Self-Review

### Spec coverage
- Unified task table + module-side write adapters: Task 1, Task 3, Task 4, Task 5
- Unified list + lightweight detail drawer + source-module deep link: Task 2, Task 6
- Deploy history backfill only: Task 3
- Database and Traffic record only new tasks from v0.0.6 onward: Task 4, Task 5
- Database batch checks appear as one batch = one task: Task 4
- Success and failure are both recorded: Task 3, Task 4, Task 5
- Minimal filters only: Task 2, Task 6
- Docs and release materials stay aligned: Task 7
- Final verification commands are explicit: Task 8

### Placeholder scan
- Removed the old `same pattern`, `existing insert path`, `where possible`, and deploy-only carryovers.
- Every task lists exact files, concrete code/test snippets, commands, and expected outcomes.
- The plan no longer depends on nonexistent helpers like `currentOperatorName()`.

### Type consistency
- Unified task types stay `deploy`, `database_connectivity`, `traffic_action` throughout.
- Unified statuses stay `pending`, `running`, `success`, `failed` throughout.
- Frontend filter model consistently uses `keyword`, `taskType`, `status`, `startedFrom`, `startedTo`, `page`, `pageSize`.
- Database and Traffic deep links remain page-level (`/asset/database`, `/traffic/controller`) because the current UI has no row-focused route contract there.
- Deploy source routes remain task-focused (`/deploy/task?taskId=...`) because that route already exists.

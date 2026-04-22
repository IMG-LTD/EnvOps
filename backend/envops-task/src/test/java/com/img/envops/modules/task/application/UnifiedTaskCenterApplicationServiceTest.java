package com.img.envops.modules.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterEntity;
import com.img.envops.modules.task.infrastructure.mapper.UnifiedTaskCenterMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class UnifiedTaskCenterApplicationServiceTest {

  private final UnifiedTaskDetailPreviewFactory previewFactory = new UnifiedTaskDetailPreviewFactory();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void normalizesDeployAndTrafficStatusesIntoFourUnifiedStates() {
    assertThat(UnifiedTaskCenterApplicationService.normalizeStatus("PENDING_APPROVAL")).isEqualTo("pending");
    assertThat(UnifiedTaskCenterApplicationService.normalizeStatus("RUNNING")).isEqualTo("running");
    assertThat(UnifiedTaskCenterApplicationService.normalizeStatus("CANCEL_REQUESTED")).isEqualTo("running");
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

  @Test
  void upsertBySourceRejectsNullSourceId() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    UnifiedTaskRecorder recorder = new UnifiedTaskRecorder(mapper);

    UnifiedTaskRecorder.UpsertBySourceCommand command = new UnifiedTaskRecorder.UpsertBySourceCommand(
        "deploy",
        null,
        "Deploy order-service",
        "running",
        "envops-admin",
        LocalDateTime.of(2026, 4, 22, 10, 0),
        null,
        "deploy started",
        "{}",
        "/deploy/task?taskId=2001",
        "deploy",
        null);

    assertThatThrownBy(() -> recorder.upsertBySource(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sourceId is required for by-source task writes: deploy");

    verify(mapper, never()).findEntityBySource(any(), any());
    verify(mapper, never()).insert(any());
  }

  @Test
  void updateBySourceRejectsNullSourceId() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    UnifiedTaskRecorder recorder = new UnifiedTaskRecorder(mapper);

    UnifiedTaskRecorder.UpdateBySourceCommand command = new UnifiedTaskRecorder.UpdateBySourceCommand(
        "deploy",
        null,
        "success",
        LocalDateTime.of(2026, 4, 22, 10, 5),
        "deploy finished",
        "{}",
        null);

    assertThatThrownBy(() -> recorder.updateBySource(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("sourceId is required for by-source task writes: deploy");

    verify(mapper, never()).findEntityBySource(any(), any());
  }

  @Test
  void upsertBySourceUsesSecurityContextFallbackForTriggeredBy() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    UnifiedTaskRecorder recorder = new UnifiedTaskRecorder(mapper);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("release-admin", "N/A"));

    when(mapper.findEntityBySource("deploy", 2001L)).thenReturn(null);

    recorder.upsertBySource(new UnifiedTaskRecorder.UpsertBySourceCommand(
        "deploy",
        2001L,
        "Deploy order-service",
        "running",
        null,
        LocalDateTime.of(2026, 4, 22, 10, 0),
        null,
        "deploy started",
        "{}",
        "/deploy/task?taskId=2001",
        "deploy",
        null));

    verify(mapper).insert(argThat(entity -> "release-admin".equals(entity.getTriggeredBy())));
    verify(mapper).findEntityBySource("deploy", 2001L);
  }

  @Test
  void startFallsBackToSystemWhenSecurityContextIsMissing() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    UnifiedTaskRecorder recorder = new UnifiedTaskRecorder(mapper);

    recorder.start(new UnifiedTaskRecorder.CreateCommand(
        "traffic_action",
        "Traffic Apply",
        "failed",
        null,
        LocalDateTime.of(2026, 4, 22, 8, 30),
        "apply failed",
        "{}",
        3001L,
        "/traffic/controller",
        "traffic",
        "rollbackToken is required"));

    verify(mapper).insert(argThat(entity -> "system".equals(entity.getTriggeredBy())));
  }

  @Test
  void upsertBySourceRetriesOnDuplicateKeyAndUpdatesExistingRow() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    UnifiedTaskRecorder recorder = new UnifiedTaskRecorder(mapper);
    UnifiedTaskCenterEntity existing = new UnifiedTaskCenterEntity();
    existing.setId(9001L);

    when(mapper.findEntityBySource("deploy", 2001L))
        .thenReturn(null)
        .thenReturn(existing);
    when(mapper.insert(any(UnifiedTaskCenterEntity.class)))
        .thenThrow(new DuplicateKeyException("duplicate source ref"));

    recorder.upsertBySource(new UnifiedTaskRecorder.UpsertBySourceCommand(
        "deploy",
        2001L,
        "Deploy order-service",
        "success",
        "envops-admin",
        LocalDateTime.of(2026, 4, 22, 10, 0),
        LocalDateTime.of(2026, 4, 22, 10, 5),
        "deploy finished",
        "{\"app\":\"order-service\"}",
        "/deploy/task?taskId=2001",
        "deploy",
        null));

    verify(mapper).insert(any(UnifiedTaskCenterEntity.class));
    verify(mapper).updateById(
        eq(9001L),
        eq("success"),
        eq(LocalDateTime.of(2026, 4, 22, 10, 5)),
        eq("deploy finished"),
        eq("{\"app\":\"order-service\"}"),
        eq(null));
  }

  @Test
  void updateBySourceUpdatesExistingRow() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    UnifiedTaskRecorder recorder = new UnifiedTaskRecorder(mapper);
    UnifiedTaskCenterEntity existing = new UnifiedTaskCenterEntity();
    existing.setId(9002L);

    when(mapper.findEntityBySource("deploy", 2001L)).thenReturn(existing);

    recorder.updateBySource(new UnifiedTaskRecorder.UpdateBySourceCommand(
        "deploy",
        2001L,
        "failed",
        LocalDateTime.of(2026, 4, 22, 10, 6),
        "deploy failed",
        "{\"error\":\"ssh timeout\"}",
        "ssh timeout"));

    verify(mapper).updateById(
        eq(9002L),
        eq("failed"),
        eq(LocalDateTime.of(2026, 4, 22, 10, 6)),
        eq("deploy failed"),
        eq("{\"error\":\"ssh timeout\"}"),
        eq("ssh timeout"));
  }
}

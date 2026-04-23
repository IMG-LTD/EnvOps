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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.task.application.tracking.DeployTrackingViewAssembler;
import com.img.envops.modules.task.application.tracking.TrackingViewSupport;
import com.img.envops.modules.task.application.tracking.TrafficActionTrackingViewAssembler;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterEntity;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import com.img.envops.modules.task.infrastructure.mapper.UnifiedTaskCenterMapper;
import java.time.LocalDateTime;
import java.util.List;
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
  void recorderUpdatesTrackingSnapshotWithoutChangingTaskStatus() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    UnifiedTaskRecorder recorder = new UnifiedTaskRecorder(mapper);

    recorder.updateTrackingSnapshot(new UnifiedTaskRecorder.TrackingSnapshotCommand(
        9001L,
        "[{\"label\":\"任务开始\",\"status\":\"success\"}]",
        "Deploy 日志摘要",
        "/deploy/task?taskId=2001&detailTab=logs"));

    verify(mapper).updateTrackingSnapshot(
        eq(9001L),
        eq("[{\"label\":\"任务开始\",\"status\":\"success\"}]"),
        eq("Deploy 日志摘要"),
        eq("/deploy/task?taskId=2001&detailTab=logs"));
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

  @Test
  void trackingDetailIncludesBaseInfoTimelineSummaryAndSourceLinks() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();
    TrackingViewSupport support = new TrackingViewSupport(objectMapper);
    UnifiedTaskCenterApplicationService service = new UnifiedTaskCenterApplicationService(
        mapper,
        objectMapper,
        List.of(new DeployTrackingViewAssembler(support)));
    UnifiedTaskCenterRow row = trackingRow(
        "deploy",
        "Deploy order-service",
        "success",
        LocalDateTime.of(2026, 4, 22, 10, 0),
        LocalDateTime.of(2026, 4, 22, 10, 5));
    row.setSourceId(2001L);
    row.setSourceRoute("/deploy/task?taskId=2001");
    row.setDetailPreview("{\"app\":\"order-service\",\"environment\":\"prod\"}");
    row.setTrackingTimeline("["
        + "{\"label\":\"任务开始\",\"status\":\"success\",\"occurredAt\":\"2026-04-22T10:00:00\",\"description\":\"开始发布\"},"
        + "{\"label\":\"任务完成\",\"status\":\"success\",\"occurredAt\":\"2026-04-22T10:05:00\",\"description\":\"发布完成\"}"
        + "]");
    row.setTrackingLogSummary("Deploy 日志摘要");
    row.setLogRoute("/deploy/task?taskId=2001&detailTab=logs");
    when(mapper.findById(9001L)).thenReturn(row);

    UnifiedTaskCenterApplicationService.UnifiedTaskTrackingDetail detail = service.getTaskTracking(9001L);

    assertThat(detail.id()).isEqualTo(9001L);
    assertThat(detail.basicInfo().taskType()).isEqualTo("deploy");
    assertThat(detail.basicInfo().taskName()).isEqualTo("Deploy order-service");
    assertThat(detail.basicInfo().status()).isEqualTo("success");
    assertThat(detail.basicInfo().triggeredBy()).isEqualTo("envops-admin");
    assertThat(detail.basicInfo().startedAt()).isEqualTo("2026-04-22T10:00:00");
    assertThat(detail.basicInfo().finishedAt()).isEqualTo("2026-04-22T10:05:00");
    assertThat(detail.timeline())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem::label)
        .containsExactly("任务开始", "任务完成");
    assertThat(detail.logSummary()).isEqualTo("Deploy 日志摘要");
    assertThat(detail.logRoute()).isEqualTo("/deploy/task?taskId=2001&detailTab=logs");
    assertThat(detail.detailPreview())
        .containsEntry("app", "order-service")
        .containsEntry("environment", "prod");
    assertThat(detail.sourceLinks())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink::type)
        .containsExactly("source", "log");
    assertThat(detail.sourceLinks())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink::route)
        .containsExactly("/deploy/task?taskId=2001", "/deploy/task?taskId=2001&detailTab=logs");
    assertThat(detail.degraded()).isFalse();
  }

  @Test
  void deployHistoryTrackingFallsBackWhenSnapshotIsMissing() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();
    TrackingViewSupport support = new TrackingViewSupport(objectMapper);
    UnifiedTaskCenterApplicationService service = new UnifiedTaskCenterApplicationService(
        mapper,
        objectMapper,
        List.of(new DeployTrackingViewAssembler(support)));
    UnifiedTaskCenterRow row = trackingRow(
        "deploy",
        "Deploy order-service",
        "success",
        LocalDateTime.of(2026, 4, 22, 10, 0),
        LocalDateTime.of(2026, 4, 22, 10, 5));
    row.setSourceId(2001L);
    row.setSourceRoute("/deploy/task?taskId=2001");
    row.setSummary("deploy finished");
    when(mapper.findById(9001L)).thenReturn(row);

    UnifiedTaskCenterApplicationService.UnifiedTaskTrackingDetail detail = service.getTaskTracking(9001L);

    assertThat(detail.timeline())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem::label)
        .containsExactly("任务开始", "任务完成");
    assertThat(detail.timeline())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem::occurredAt)
        .containsExactly("2026-04-22T10:00:00", "2026-04-22T10:05:00");
    assertThat(detail.logSummary()).isEqualTo("deploy finished");
    assertThat(detail.logRoute()).isEqualTo("/deploy/task?taskId=2001&detailTab=logs");
    assertThat(detail.sourceLinks())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink::type)
        .containsExactly("source", "log");
    assertThat(detail.sourceLinks())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink::route)
        .containsExactly("/deploy/task?taskId=2001", "/deploy/task?taskId=2001&detailTab=logs");
    assertThat(detail.degraded()).isTrue();
  }

  @Test
  void nonDeployOldTrackingFallsBackWithoutPromisingFullHistory() {
    UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
    ObjectMapper objectMapper = new ObjectMapper();
    TrackingViewSupport support = new TrackingViewSupport(objectMapper);
    UnifiedTaskCenterApplicationService service = new UnifiedTaskCenterApplicationService(
        mapper,
        objectMapper,
        List.of(new TrafficActionTrackingViewAssembler(support)));
    UnifiedTaskCenterRow row = trackingRow(
        "traffic_action",
        "Traffic Apply",
        "failed",
        LocalDateTime.of(2026, 4, 22, 8, 30),
        LocalDateTime.of(2026, 4, 22, 8, 31));
    row.setSourceRoute("/traffic/controller");
    row.setSummary("traffic apply failed");
    row.setErrorSummary("rollbackToken is required");
    when(mapper.findById(9003L)).thenReturn(row);

    UnifiedTaskCenterApplicationService.UnifiedTaskTrackingDetail detail = service.getTaskTracking(9003L);

    assertThat(detail.timeline())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem::label)
        .containsExactly("任务开始", "任务失败");
    assertThat(detail.logSummary()).isEqualTo("traffic apply failed；rollbackToken is required");
    assertThat(detail.sourceLinks())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink::type)
        .containsExactly("source");
    assertThat(detail.sourceLinks())
        .extracting(UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink::route)
        .containsExactly("/traffic/controller");
    assertThat(detail.degraded()).isTrue();
  }

  private UnifiedTaskCenterRow trackingRow(
      String taskType,
      String taskName,
      String status,
      LocalDateTime startedAt,
      LocalDateTime finishedAt) {
    UnifiedTaskCenterRow row = new UnifiedTaskCenterRow();
    row.setId(9001L);
    row.setTaskType(taskType);
    row.setTaskName(taskName);
    row.setStatus(status);
    row.setTriggeredBy("envops-admin");
    row.setStartedAt(startedAt);
    row.setFinishedAt(finishedAt);
    return row;
  }
}

package com.img.envops.modules.task.application;

import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterEntity;
import com.img.envops.modules.task.infrastructure.mapper.UnifiedTaskCenterMapper;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
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

  public record TrackingSnapshotCommand(
      Long id,
      String trackingTimeline,
      String trackingLogSummary,
      String logRoute) {}

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

  public void updateTrackingSnapshot(TrackingSnapshotCommand command) {
    unifiedTaskCenterMapper.updateTrackingSnapshot(
        command.id(),
        command.trackingTimeline(),
        command.trackingLogSummary(),
        command.logRoute());
  }

  public void upsertBySource(UpsertBySourceCommand command) {
    Long sourceId = requireSourceId(command.taskType(), command.sourceId());
    for (int attempt = 0; attempt < 5; attempt++) {
      UnifiedTaskCenterEntity existing = unifiedTaskCenterMapper.findEntityBySource(command.taskType(), sourceId);
      if (existing != null) {
        updateExisting(existing.getId(), command.status(), command.finishedAt(), command.summary(), command.detailPreview(), command.errorSummary());
        return;
      }
      try {
        unifiedTaskCenterMapper.insert(toEntity(command));
        return;
      } catch (DuplicateKeyException exception) {
        if (attempt == 4) {
          throw exception;
        }
      }
    }
    throw new IllegalStateException("failed to upsert unified task for source");
  }

  public void updateBySource(UpdateBySourceCommand command) {
    Long sourceId = requireSourceId(command.taskType(), command.sourceId());
    UnifiedTaskCenterEntity existing = unifiedTaskCenterMapper.findEntityBySource(command.taskType(), sourceId);
    if (existing == null) {
      throw new IllegalArgumentException("unified task not found for source: " + command.taskType() + ":" + sourceId);
    }
    updateExisting(existing.getId(), command.status(), command.finishedAt(), command.summary(), command.detailPreview(), command.errorSummary());
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

  private UnifiedTaskCenterEntity toEntity(UpsertBySourceCommand command) {
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
    return entity;
  }

  private void updateExisting(
      Long id,
      String status,
      LocalDateTime finishedAt,
      String summary,
      String detailPreview,
      String errorSummary) {
    unifiedTaskCenterMapper.updateById(id, status, finishedAt, summary, detailPreview, errorSummary);
  }

  private Long requireSourceId(String taskType, Long sourceId) {
    if (sourceId == null) {
      throw new IllegalArgumentException("sourceId is required for by-source task writes: " + taskType);
    }
    return sourceId;
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

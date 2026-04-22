package com.img.envops.modules.task.infrastructure.bootstrap;

import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService;
import com.img.envops.modules.task.application.UnifiedTaskDetailPreviewFactory;
import com.img.envops.modules.task.application.UnifiedTaskRecorder;
import com.img.envops.modules.task.infrastructure.mapper.UnifiedTaskCenterMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class UnifiedTaskCenterBackfillRunner implements ApplicationRunner {
  private static final int BATCH_SIZE = 200;

  private final UnifiedTaskCenterMapper unifiedTaskCenterMapper;
  private final UnifiedTaskRecorder unifiedTaskRecorder;
  private final UnifiedTaskDetailPreviewFactory previewFactory;

  public UnifiedTaskCenterBackfillRunner(
      UnifiedTaskCenterMapper unifiedTaskCenterMapper,
      UnifiedTaskRecorder unifiedTaskRecorder,
      UnifiedTaskDetailPreviewFactory previewFactory) {
    this.unifiedTaskCenterMapper = unifiedTaskCenterMapper;
    this.unifiedTaskRecorder = unifiedTaskRecorder;
    this.previewFactory = previewFactory;
  }

  @Override
  public void run(ApplicationArguments args) {
    long afterSourceId = 0L;
    while (true) {
      var rows = unifiedTaskCenterMapper.findDeployRowsMissingProjection(afterSourceId, BATCH_SIZE);
      if (rows.isEmpty()) {
        return;
      }
      for (UnifiedTaskCenterMapper.DeployBackfillRow row : rows) {
        String unifiedStatus = UnifiedTaskCenterApplicationService.normalizeStatus(row.getStatus());
        int targetCount = defaultZero(row.getTargetCount());
        int successCount = defaultZero(row.getSuccessCount());
        int failCount = defaultZero(row.getFailCount());
        String sourceRoute = "/deploy/task?taskId=" + row.getSourceId();
        unifiedTaskRecorder.upsertBySource(new UnifiedTaskRecorder.UpsertBySourceCommand(
            "deploy",
            row.getSourceId(),
            row.getTaskName(),
            unifiedStatus,
            row.getOperatorName(),
            row.getStartedAt() == null ? row.getCreatedAt() : row.getStartedAt(),
            row.getFinishedAt(),
            String.format("发布 %s 到 %s，目标 %d，成功 %d，失败 %d",
                row.getAppName(),
                row.getEnvironment() == null ? "default" : row.getEnvironment(),
                targetCount,
                successCount,
                failCount),
            previewFactory.toJson(previewFactory.buildDeployPreview(
                row.getAppName(),
                row.getEnvironment(),
                targetCount,
                successCount,
                failCount,
                row.getStatus(),
                sourceRoute)),
            sourceRoute,
            "deploy",
            "failed".equals(unifiedStatus) ? row.getStatus() : null));
        afterSourceId = row.getSourceId();
      }
    }
  }

  private int defaultZero(Integer value) {
    return value == null ? 0 : value;
  }
}

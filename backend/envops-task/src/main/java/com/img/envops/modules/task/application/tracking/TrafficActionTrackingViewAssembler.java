package com.img.envops.modules.task.application.tracking;

import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.TrackingViewParts;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TrafficActionTrackingViewAssembler implements UnifiedTaskTrackingViewAssembler {
  private final TrackingViewSupport support;

  public TrafficActionTrackingViewAssembler(TrackingViewSupport support) {
    this.support = support;
  }

  @Override
  public boolean supports(String taskType) {
    return "traffic_action".equalsIgnoreCase(taskType);
  }

  @Override
  public TrackingViewParts assemble(UnifiedTaskCenterRow row) {
    List<UnifiedTaskTimelineItem> snapshotTimeline = support.parseTimeline(row);
    boolean hasSnapshot = !snapshotTimeline.isEmpty();
    boolean hasTrackingLogSummary = StringUtils.hasText(row.getTrackingLogSummary());
    return new TrackingViewParts(
        hasSnapshot ? snapshotTimeline : support.fallbackTimeline(row),
        support.fallbackLogSummary(row),
        row.getLogRoute(),
        support.sourceLinks(row.getSourceRoute(), row.getLogRoute()),
        !hasSnapshot || !hasTrackingLogSummary);
  }
}

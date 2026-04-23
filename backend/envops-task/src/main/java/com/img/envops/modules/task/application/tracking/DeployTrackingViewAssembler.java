package com.img.envops.modules.task.application.tracking;

import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.TrackingViewParts;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DeployTrackingViewAssembler implements UnifiedTaskTrackingViewAssembler {
  private final TrackingViewSupport support;

  public DeployTrackingViewAssembler(TrackingViewSupport support) {
    this.support = support;
  }

  @Override
  public boolean supports(String taskType) {
    return "deploy".equalsIgnoreCase(taskType);
  }

  @Override
  public TrackingViewParts assemble(UnifiedTaskCenterRow row) {
    List<UnifiedTaskTimelineItem> snapshotTimeline = support.parseTimeline(row);
    String logRoute = resolveLogRoute(row);
    List<UnifiedTaskSourceLink> sourceLinks = ensureLogSourceLink(support.sourceLinks(row.getSourceRoute(), logRoute), logRoute);
    boolean hasSnapshot = !snapshotTimeline.isEmpty();
    boolean hasTrackingLogSummary = StringUtils.hasText(row.getTrackingLogSummary());
    return new TrackingViewParts(
        hasSnapshot ? snapshotTimeline : support.fallbackTimeline(row),
        support.fallbackLogSummary(row),
        logRoute,
        sourceLinks,
        !hasSnapshot || !hasTrackingLogSummary);
  }

  private String resolveLogRoute(UnifiedTaskCenterRow row) {
    if (StringUtils.hasText(row.getLogRoute())) {
      return row.getLogRoute().trim();
    }
    if (row.getSourceId() != null) {
      return "/deploy/task?taskId=" + row.getSourceId() + "&detailTab=logs";
    }
    return null;
  }

  private List<UnifiedTaskSourceLink> ensureLogSourceLink(List<UnifiedTaskSourceLink> sourceLinks, String logRoute) {
    if (!StringUtils.hasText(logRoute)) {
      return sourceLinks;
    }
    boolean hasLogLink = sourceLinks.stream().anyMatch(link -> "log".equals(link.type()));
    if (hasLogLink) {
      return sourceLinks;
    }
    List<UnifiedTaskSourceLink> links = new ArrayList<>(sourceLinks);
    links.add(new UnifiedTaskSourceLink("log", "任务日志", logRoute));
    return List.copyOf(links);
  }
}

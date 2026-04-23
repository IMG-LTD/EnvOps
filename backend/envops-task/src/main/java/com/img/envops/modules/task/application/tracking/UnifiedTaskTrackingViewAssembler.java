package com.img.envops.modules.task.application.tracking;

import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.TrackingViewParts;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;

public interface UnifiedTaskTrackingViewAssembler {
  boolean supports(String taskType);

  TrackingViewParts assemble(UnifiedTaskCenterRow row);
}

package com.img.envops.modules.task.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnifiedTaskCenterController {
  private final UnifiedTaskCenterApplicationService unifiedTaskCenterApplicationService;

  public UnifiedTaskCenterController(UnifiedTaskCenterApplicationService unifiedTaskCenterApplicationService) {
    this.unifiedTaskCenterApplicationService = unifiedTaskCenterApplicationService;
  }

  @GetMapping("/api/task-center/tasks")
  public R<UnifiedTaskCenterApplicationService.UnifiedTaskPage> getTasks(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String taskType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedTo,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    return R.ok(unifiedTaskCenterApplicationService.getTasks(new UnifiedTaskCenterApplicationService.UnifiedTaskQuery(
        keyword,
        taskType,
        status,
        startedFrom,
        startedTo,
        page,
        pageSize)));
  }

  @GetMapping("/api/task-center/tasks/{id}")
  public R<UnifiedTaskCenterApplicationService.UnifiedTaskDetail> getTaskDetail(@PathVariable Long id) {
    return R.ok(unifiedTaskCenterApplicationService.getTaskDetail(id));
  }
}

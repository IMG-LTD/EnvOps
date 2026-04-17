package com.img.envops.modules.monitor.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.monitor.application.MonitorApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
  private final MonitorApplicationService monitorApplicationService;

  public MonitorController(MonitorApplicationService monitorApplicationService) {
    this.monitorApplicationService = monitorApplicationService;
  }

  @PostMapping("/detect-tasks")
  public R<MonitorApplicationService.DetectTaskRecord> createDetectTask(@RequestBody CreateDetectTaskRequest request) {
    return R.ok(monitorApplicationService.createDetectTask(new MonitorApplicationService.CreateDetectTaskCommand(
        request.taskName(),
        request.hostId(),
        request.schedule())));
  }

  @GetMapping("/detect-tasks")
  public R<List<MonitorApplicationService.DetectTaskRecord>> getDetectTasks() {
    return R.ok(monitorApplicationService.getDetectTasks());
  }

  @GetMapping("/hosts/{hostId}/facts/latest")
  public R<MonitorApplicationService.HostFactRecord> getLatestHostFact(@PathVariable Long hostId) {
    return R.ok(monitorApplicationService.getLatestHostFact(hostId));
  }

  public record CreateDetectTaskRequest(String taskName, Long hostId, String schedule) {
  }
}

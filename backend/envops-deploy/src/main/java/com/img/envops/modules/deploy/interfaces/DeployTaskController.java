package com.img.envops.modules.deploy.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.deploy.application.DeployTaskApplicationService;
import com.img.envops.modules.deploy.application.DeployTaskExecutionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
public class DeployTaskController {
  private final DeployTaskApplicationService deployTaskApplicationService;
  private final DeployTaskExecutionApplicationService deployTaskExecutionApplicationService;

  public DeployTaskController(DeployTaskApplicationService deployTaskApplicationService,
                              DeployTaskExecutionApplicationService deployTaskExecutionApplicationService) {
    this.deployTaskApplicationService = deployTaskApplicationService;
    this.deployTaskExecutionApplicationService = deployTaskExecutionApplicationService;
  }

  @GetMapping("/api/deploy/tasks")
  public R<List<DeployTaskApplicationService.DeployTaskRecord>> getDeployTasks() {
    return R.ok(deployTaskApplicationService.getDeployTasks());
  }

  @GetMapping("/api/deploy/tasks/{id}")
  public R<DeployTaskApplicationService.DeployTaskRecord> getDeployTask(@PathVariable Long id) {
    return R.ok(deployTaskApplicationService.getDeployTask(id));
  }

  @PostMapping("/api/deploy/tasks")
  public R<DeployTaskApplicationService.DeployTaskRecord> createDeployTask(@RequestBody CreateDeployTaskRequest request,
                                                                            Principal principal) {
    if (request != null && request.taskType() != null && "ROLLBACK".equals(request.taskType().trim())) {
      throw new IllegalArgumentException("taskType ROLLBACK must be created via rollback API");
    }
    String operatorName = principal == null ? null : principal.getName();
    return R.ok(deployTaskApplicationService.createDeployTask(new DeployTaskApplicationService.CreateDeployTaskCommand(
        request.taskName(),
        request.taskType(),
        request.appId(),
        request.versionId(),
        request.hostIds(),
        request.batchStrategy(),
        request.batchSize(),
        request.params()), operatorName));
  }

  @PostMapping("/api/deploy/tasks/{id}/approve")
  public R<DeployTaskApplicationService.DeployTaskRecord> approveDeployTask(@PathVariable Long id,
                                                                             @RequestBody(required = false) ApprovalDecisionRequest request,
                                                                             Principal principal) {
    String operatorName = principal == null ? null : principal.getName();
    String comment = request == null ? null : request.comment();
    return R.ok(deployTaskApplicationService.approveDeployTask(id, comment, operatorName));
  }

  @PostMapping("/api/deploy/tasks/{id}/reject")
  public R<DeployTaskApplicationService.DeployTaskRecord> rejectDeployTask(@PathVariable Long id,
                                                                            @RequestBody(required = false) ApprovalDecisionRequest request,
                                                                            Principal principal) {
    String operatorName = principal == null ? null : principal.getName();
    String comment = request == null ? null : request.comment();
    return R.ok(deployTaskApplicationService.rejectDeployTask(id, comment, operatorName));
  }

  @PostMapping("/api/deploy/tasks/{id}/execute")
  public R<DeployTaskApplicationService.DeployTaskRecord> executeDeployTask(@PathVariable Long id, Principal principal) {
    String operatorName = principal == null ? null : principal.getName();
    return R.ok(deployTaskExecutionApplicationService.executeDeployTask(id, operatorName));
  }

  @PostMapping("/api/deploy/tasks/{id}/cancel")
  public R<DeployTaskApplicationService.DeployTaskRecord> cancelDeployTask(@PathVariable Long id, Principal principal) {
    String operatorName = principal == null ? null : principal.getName();
    return R.ok(deployTaskExecutionApplicationService.cancelDeployTask(id, operatorName));
  }

  @PostMapping("/api/deploy/tasks/{id}/retry")
  public R<DeployTaskApplicationService.DeployTaskRecord> retryDeployTask(@PathVariable Long id, Principal principal) {
    String operatorName = principal == null ? null : principal.getName();
    return R.ok(deployTaskExecutionApplicationService.retryDeployTask(id, operatorName));
  }

  @PostMapping("/api/deploy/tasks/{id}/rollback")
  public R<DeployTaskApplicationService.DeployTaskRecord> rollbackDeployTask(@PathVariable Long id, Principal principal) {
    String operatorName = principal == null ? null : principal.getName();
    return R.ok(deployTaskExecutionApplicationService.rollbackDeployTask(id, operatorName));
  }

  @GetMapping("/api/task-center/tasks")
  public R<List<DeployTaskApplicationService.TaskCenterRecord>> getTaskCenterTasks() {
    return R.ok(deployTaskApplicationService.getTaskCenterTasks());
  }

  @GetMapping("/api/deploy/tasks/{id}/hosts")
  public R<List<DeployTaskApplicationService.DeployTaskHostRecord>> getDeployTaskHosts(@PathVariable Long id) {
    return R.ok(deployTaskApplicationService.getDeployTaskHosts(id));
  }

  @GetMapping("/api/deploy/tasks/{id}/logs")
  public R<List<DeployTaskApplicationService.DeployTaskLogRecord>> getDeployTaskLogs(@PathVariable Long id) {
    return R.ok(deployTaskApplicationService.getDeployTaskLogs(id));
  }

  public record CreateDeployTaskRequest(String taskName,
                                        String taskType,
                                        Long appId,
                                        Long versionId,
                                        List<Long> hostIds,
                                        String batchStrategy,
                                        Integer batchSize,
                                        Map<String, Object> params) {
  }

  public record ApprovalDecisionRequest(String comment) {
  }
}

package com.img.envops.modules.deploy.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.deploy.application.DeployTaskApplicationService;
import com.img.envops.modules.deploy.application.DeployTaskExecutionApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
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
  public R<DeployTaskApplicationService.DeployTaskPage> getDeployTasks(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String taskType,
      @RequestParam(required = false) Long appId,
      @RequestParam(required = false) String environment,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer pageSize,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    return R.ok(deployTaskApplicationService.getDeployTasks(new DeployTaskApplicationService.DeployTaskQuery(
        keyword,
        status,
        taskType,
        appId,
        environment,
        createdFrom,
        createdTo,
        page,
        pageSize,
        sortBy,
        sortOrder)));
  }

  @GetMapping("/api/deploy/tasks/{id}")
  public R<DeployTaskApplicationService.DeployTaskDetailRecord> getDeployTask(@PathVariable Long id) {
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
  public R<DeployTaskApplicationService.TaskCenterPage> getTaskCenterTasks(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String sourceType,
      @RequestParam(required = false) String taskType,
      @RequestParam(required = false) String priority,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer pageSize,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    return R.ok(deployTaskApplicationService.getTaskCenterTasks(new DeployTaskApplicationService.TaskCenterQuery(
        keyword,
        status,
        sourceType,
        taskType,
        priority,
        page,
        pageSize,
        sortBy,
        sortOrder)));
  }

  @GetMapping("/api/deploy/tasks/{id}/hosts")
  public R<DeployTaskApplicationService.DeployTaskHostPage> getDeployTaskHosts(@PathVariable Long id,
                                                                                @RequestParam(required = false) String status,
                                                                                @RequestParam(required = false) String keyword,
                                                                                @RequestParam(required = false) Integer page,
                                                                                @RequestParam(required = false) Integer pageSize) {
    return R.ok(deployTaskApplicationService.getDeployTaskHosts(id, new DeployTaskApplicationService.DeployTaskHostQuery(
        status,
        keyword,
        page,
        pageSize)));
  }

  @GetMapping("/api/deploy/tasks/{id}/logs")
  public R<DeployTaskApplicationService.DeployTaskLogPage> getDeployTaskLogs(@PathVariable Long id,
                                                                              @RequestParam(required = false) Long hostId,
                                                                              @RequestParam(required = false) String keyword,
                                                                              @RequestParam(required = false) Integer page,
                                                                              @RequestParam(required = false) Integer pageSize) {
    return R.ok(deployTaskApplicationService.getDeployTaskLogs(id, new DeployTaskApplicationService.DeployTaskLogQuery(
        hostId,
        keyword,
        page,
        pageSize)));
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

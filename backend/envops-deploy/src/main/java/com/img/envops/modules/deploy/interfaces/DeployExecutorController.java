package com.img.envops.modules.deploy.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.deploy.application.DeployExecutorApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DeployExecutorController {
  private final DeployExecutorApplicationService deployExecutorApplicationService;

  public DeployExecutorController(DeployExecutorApplicationService deployExecutorApplicationService) {
    this.deployExecutorApplicationService = deployExecutorApplicationService;
  }

  @GetMapping("/api/deploy/executors")
  public R<List<DeployExecutorApplicationService.ExecutorDirectoryRecord>> getExecutors() {
    return R.ok(deployExecutorApplicationService.getExecutors());
  }
}

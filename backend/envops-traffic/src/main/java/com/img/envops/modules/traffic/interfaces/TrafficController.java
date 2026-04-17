package com.img.envops.modules.traffic.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.traffic.application.TrafficApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {
  private final TrafficApplicationService trafficApplicationService;

  public TrafficController(TrafficApplicationService trafficApplicationService) {
    this.trafficApplicationService = trafficApplicationService;
  }

  @GetMapping("/policies")
  public R<List<TrafficApplicationService.TrafficPolicyRecord>> getPolicies() {
    return R.ok(trafficApplicationService.getPolicies());
  }

  @GetMapping("/plugins")
  public R<List<TrafficApplicationService.TrafficPluginDirectoryRecord>> getPlugins() {
    return R.ok(trafficApplicationService.getPlugins());
  }
}

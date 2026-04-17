package com.img.envops.modules.system.interfaces.route;

import com.img.envops.common.response.R;
import com.img.envops.modules.system.application.route.RouteApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
  private final RouteApplicationService routeApplicationService;

  public RouteController(RouteApplicationService routeApplicationService) {
    this.routeApplicationService = routeApplicationService;
  }

  @GetMapping("/getConstantRoutes")
  public R<List<RouteApplicationService.MenuRoute>> getConstantRoutes() {
    return R.ok(routeApplicationService.getConstantRoutes());
  }

  @GetMapping("/getUserRoutes")
  public R<RouteApplicationService.UserRoute> getUserRoutes(Authentication authentication) {
    String principal = authentication == null ? null : authentication.getName();
    return R.ok(routeApplicationService.getUserRoutes(principal));
  }
}

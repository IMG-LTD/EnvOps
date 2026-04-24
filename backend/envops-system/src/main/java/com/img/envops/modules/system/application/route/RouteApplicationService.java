package com.img.envops.modules.system.application.route;

import com.img.envops.modules.system.application.auth.AuthApplicationService;
import com.img.envops.modules.system.infrastructure.mapper.RouteMenuMapper;
import com.img.envops.modules.system.infrastructure.mapper.RouteMenuMapper.RouteRow;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper.UserAuthRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RouteApplicationService {
  private final RouteMenuMapper routeMenuMapper;
  private final AuthApplicationService authApplicationService;

  public RouteApplicationService(RouteMenuMapper routeMenuMapper, AuthApplicationService authApplicationService) {
    this.routeMenuMapper = routeMenuMapper;
    this.authApplicationService = authApplicationService;
  }

  public List<MenuRoute> getConstantRoutes() {
    return buildRoutes(routeMenuMapper.findConstantRoutes());
  }

  public UserRoute getUserRoutes(String tokenOrUsername) {
    UserAuthRow user = authApplicationService.requireUser(tokenOrUsername);
    List<RouteRow> routeRows = routeMenuMapper.findUserRoutesByUserId(user.getUserId());
    List<MenuRoute> routes = buildRoutes(routeRows);
    String home = routeRows.stream()
        .filter(row -> Boolean.TRUE.equals(row.getHomeFlag()))
        .map(RouteRow::getRouteName)
        .findFirst()
        .orElse(routes.isEmpty() ? "home" : routes.get(0).name());

    return new UserRoute(routes, home);
  }

  private List<MenuRoute> buildRoutes(List<RouteRow> routeRows) {
    Map<Long, MenuRouteBuilder> builderMap = routeRows.stream()
        .map(this::toBuilder)
        .collect(Collectors.toMap(MenuRouteBuilder::id, Function.identity(), (left, right) -> left));

    List<MenuRouteBuilder> roots = new ArrayList<>();
    for (RouteRow row : routeRows) {
      MenuRouteBuilder current = builderMap.get(row.getId());
      Long parentId = row.getParentId();
      if (parentId != null && builderMap.containsKey(parentId)) {
        builderMap.get(parentId).children().add(current);
      } else {
        roots.add(current);
      }
    }

    return roots.stream().map(MenuRouteBuilder::build).toList();
  }

  private MenuRouteBuilder toBuilder(RouteRow row) {
    return new MenuRouteBuilder(
        row.getId(),
        new ArrayList<>(),
        new MenuRoute(
            String.valueOf(row.getId()),
            row.getRouteName(),
            row.getRoutePath(),
            row.getComponent(),
            new Meta(
                row.getTitle(),
                buildI18nKey(row.getRouteName()),
                row.getIcon(),
                row.getRouteOrder() == null ? 0 : row.getRouteOrder(),
                Boolean.TRUE.equals(row.getHideInMenu()),
                row.getActiveMenu()),
            null));
  }

  private String buildI18nKey(String routeName) {
    return routeName == null ? null : "route." + routeName;
  }

  private record MenuRouteBuilder(Long id, List<MenuRouteBuilder> children, MenuRoute route) {
    private MenuRoute build() {
      List<MenuRoute> builtChildren = children.stream().map(MenuRouteBuilder::build).toList();
      return new MenuRoute(route.id(), route.name(), route.path(), route.component(), route.meta(),
          builtChildren.isEmpty() ? null : builtChildren);
    }
  }

  public record UserRoute(List<MenuRoute> routes, String home) {
  }

  public record MenuRoute(String id,
                          String name,
                          String path,
                          String component,
                          Meta meta,
                          List<MenuRoute> children) {
  }

  public record Meta(String title, String i18nKey, String icon, Integer order, Boolean hideInMenu, String activeMenu) {
  }
}

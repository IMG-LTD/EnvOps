package com.img.envops.modules.system.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RouteMenuMapper {

  @Select("""
      SELECT id,
             parent_id AS parentId,
             route_name AS routeName,
             route_path AS routePath,
             component,
             title,
             icon,
             route_order AS routeOrder,
             home_flag AS homeFlag,
             hide_in_menu AS hideInMenu,
             active_menu AS activeMenu
      FROM sys_menu_route
      WHERE route_type = 'CONSTANT'
      ORDER BY route_order, id
      """)
  List<RouteRow> findConstantRoutes();

  @Select("""
      SELECT DISTINCT m.id,
             m.parent_id AS parentId,
             m.route_name AS routeName,
             m.route_path AS routePath,
             m.component,
             m.title,
             m.icon,
             m.route_order AS routeOrder,
             m.home_flag AS homeFlag,
             m.hide_in_menu AS hideInMenu,
             m.active_menu AS activeMenu
      FROM sys_menu_route m
      JOIN sys_permission p ON p.route_name = m.route_name
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE m.route_type = 'USER'
        AND ur.user_id = #{userId}
        AND p.permission_type = 'menu'
        AND p.enabled = TRUE
        AND r.enabled = TRUE
      ORDER BY m.route_order, m.id
      """)
  List<RouteRow> findUserRoutesByUserId(@Param("userId") Long userId);

  class RouteRow {
    private Long id;
    private Long parentId;
    private String routeName;
    private String routePath;
    private String component;
    private String title;
    private String icon;
    private Integer routeOrder;
    private Boolean homeFlag;
    private Boolean hideInMenu;
    private String activeMenu;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getParentId() {
      return parentId;
    }

    public void setParentId(Long parentId) {
      this.parentId = parentId;
    }

    public String getRouteName() {
      return routeName;
    }

    public void setRouteName(String routeName) {
      this.routeName = routeName;
    }

    public String getRoutePath() {
      return routePath;
    }

    public void setRoutePath(String routePath) {
      this.routePath = routePath;
    }

    public String getComponent() {
      return component;
    }

    public void setComponent(String component) {
      this.component = component;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getIcon() {
      return icon;
    }

    public void setIcon(String icon) {
      this.icon = icon;
    }

    public Integer getRouteOrder() {
      return routeOrder;
    }

    public void setRouteOrder(Integer routeOrder) {
      this.routeOrder = routeOrder;
    }

    public Boolean getHomeFlag() {
      return homeFlag;
    }

    public void setHomeFlag(Boolean homeFlag) {
      this.homeFlag = homeFlag;
    }

    public Boolean getHideInMenu() {
      return hideInMenu;
    }

    public void setHideInMenu(Boolean hideInMenu) {
      this.hideInMenu = hideInMenu;
    }

    public String getActiveMenu() {
      return activeMenu;
    }

    public void setActiveMenu(String activeMenu) {
      this.activeMenu = activeMenu;
    }
  }
}

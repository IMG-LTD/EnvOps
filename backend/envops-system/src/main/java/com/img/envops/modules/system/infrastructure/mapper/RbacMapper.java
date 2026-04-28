package com.img.envops.modules.system.infrastructure.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RbacMapper {

  @Select("""
      SELECT DISTINCT p.permission_key
      FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_user_role ur ON ur.role_id = r.id
      JOIN sys_user u ON u.id = ur.user_id
      WHERE u.user_name = #{userName}
        AND u.status = 'ACTIVE'
        AND r.enabled = TRUE
        AND p.enabled = TRUE
      ORDER BY p.permission_key
      """)
  List<String> findEffectivePermissionKeysByUserName(@Param("userName") String userName);

  @Select("""
      SELECT DISTINCT p.permission_key
      FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE ur.user_id = #{userId}
        AND r.enabled = TRUE
        AND p.enabled = TRUE
      ORDER BY p.permission_key
      """)
  List<String> findEffectivePermissionKeysByUserId(@Param("userId") Long userId);

  @Select("""
      SELECT DISTINCT p.permission_key
      FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      JOIN sys_role r ON r.id = rp.role_id
      JOIN sys_user_role ur ON ur.role_id = r.id
      JOIN sys_user u ON u.id = ur.user_id
      WHERE u.user_name = #{userName}
        AND u.status = 'ACTIVE'
        AND r.enabled = TRUE
        AND p.enabled = TRUE
        AND p.permission_type = 'action'
      ORDER BY p.permission_key
      """)
  List<String> findEffectiveActionPermissionKeysByUserName(@Param("userName") String userName);

  @Select("""
      SELECT id AS roleId,
             role_key AS roleKey,
             role_name AS roleName,
             description,
             enabled,
             built_in AS builtIn,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM sys_role
      ORDER BY id
      """)
  List<RoleRow> findRoles();

  @Select("""
      SELECT id AS roleId,
             role_key AS roleKey,
             role_name AS roleName,
             description,
             enabled,
             built_in AS builtIn,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM sys_role
      WHERE id = #{roleId}
      """)
  RoleRow findRoleById(@Param("roleId") Long roleId);

  @Select("""
      SELECT id AS roleId,
             role_key AS roleKey,
             role_name AS roleName,
             description,
             enabled,
             built_in AS builtIn,
             created_at AS createdAt,
             updated_at AS updatedAt
      FROM sys_role
      WHERE role_key = #{roleKey}
      """)
  RoleRow findRoleByKey(@Param("roleKey") String roleKey);

  @Select("SELECT COALESCE(MAX(id), 0) + 1 FROM sys_role")
  Long nextRoleId();

  @Insert("""
      INSERT INTO sys_role (id, role_key, role_name, description, enabled, built_in, created_at, updated_at)
      VALUES (#{roleId}, #{roleKey}, #{roleName}, #{description}, #{enabled}, #{builtIn}, #{createdAt}, #{updatedAt})
      """)
  int insertRole(RoleEntity entity);

  @Update("""
      UPDATE sys_role
      SET role_name = #{roleName},
          description = #{description},
          enabled = #{enabled},
          updated_at = #{updatedAt}
      WHERE id = #{roleId}
      """)
  int updateRole(RoleEntity entity);

  @Select("""
      SELECT id AS permissionId,
             permission_key AS permissionKey,
             permission_name AS permissionName,
             permission_type AS permissionType,
             module_key AS moduleKey,
             parent_key AS parentKey,
             route_name AS routeName,
             action_key AS actionKey,
             sort_order AS sortOrder,
             enabled
      FROM sys_permission
      WHERE enabled = TRUE
      ORDER BY sort_order, id
      """)
  List<PermissionRow> findEnabledPermissions();

  @Select("""
      SELECT id AS permissionId,
             permission_key AS permissionKey,
             permission_name AS permissionName,
             permission_type AS permissionType,
             module_key AS moduleKey,
             parent_key AS parentKey,
             route_name AS routeName,
             action_key AS actionKey,
             sort_order AS sortOrder,
             enabled
      FROM sys_permission
      WHERE permission_key = #{permissionKey}
      """)
  PermissionRow findPermissionByKey(@Param("permissionKey") String permissionKey);

  @Select("""
      SELECT p.permission_key
      FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      WHERE rp.role_id = #{roleId}
      ORDER BY p.sort_order, p.id
      """)
  List<String> findRolePermissionKeys(@Param("roleId") Long roleId);

  @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
  int deleteRolePermissions(@Param("roleId") Long roleId);

  @Insert("""
      INSERT INTO sys_role_permission (role_id, permission_id)
      SELECT #{roleId}, id
      FROM sys_permission
      WHERE permission_key = #{permissionKey}
      """)
  int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionKey") String permissionKey);

  @Select("""
      SELECT COUNT(*)
      FROM sys_user u
      JOIN sys_user_role ur ON ur.user_id = u.id
      JOIN sys_role r ON r.id = ur.role_id
      WHERE u.status = 'ACTIVE'
        AND r.role_key = 'SUPER_ADMIN'
        AND r.enabled = TRUE
      """)
  int countActiveSuperAdminUsers();

  class RoleRow {
    private Long roleId;
    private String roleKey;
    private String roleName;
    private String description;
    private Boolean enabled;
    private Boolean builtIn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRoleId() {
      return roleId;
    }

    public void setRoleId(Long roleId) {
      this.roleId = roleId;
    }

    public String getRoleKey() {
      return roleKey;
    }

    public void setRoleKey(String roleKey) {
      this.roleKey = roleKey;
    }

    public String getRoleName() {
      return roleName;
    }

    public void setRoleName(String roleName) {
      this.roleName = roleName;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    public Boolean getBuiltIn() {
      return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
      this.builtIn = builtIn;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }

  class RoleEntity {
    private Long roleId;
    private String roleKey;
    private String roleName;
    private String description;
    private Boolean enabled;
    private Boolean builtIn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRoleId() {
      return roleId;
    }

    public void setRoleId(Long roleId) {
      this.roleId = roleId;
    }

    public String getRoleKey() {
      return roleKey;
    }

    public void setRoleKey(String roleKey) {
      this.roleKey = roleKey;
    }

    public String getRoleName() {
      return roleName;
    }

    public void setRoleName(String roleName) {
      this.roleName = roleName;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    public Boolean getBuiltIn() {
      return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
      this.builtIn = builtIn;
    }

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }

  class PermissionRow {
    private Long permissionId;
    private String permissionKey;
    private String permissionName;
    private String permissionType;
    private String moduleKey;
    private String parentKey;
    private String routeName;
    private String actionKey;
    private Integer sortOrder;
    private Boolean enabled;

    public Long getPermissionId() {
      return permissionId;
    }

    public void setPermissionId(Long permissionId) {
      this.permissionId = permissionId;
    }

    public String getPermissionKey() {
      return permissionKey;
    }

    public void setPermissionKey(String permissionKey) {
      this.permissionKey = permissionKey;
    }

    public String getPermissionName() {
      return permissionName;
    }

    public void setPermissionName(String permissionName) {
      this.permissionName = permissionName;
    }

    public String getPermissionType() {
      return permissionType;
    }

    public void setPermissionType(String permissionType) {
      this.permissionType = permissionType;
    }

    public String getModuleKey() {
      return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
      this.moduleKey = moduleKey;
    }

    public String getParentKey() {
      return parentKey;
    }

    public void setParentKey(String parentKey) {
      this.parentKey = parentKey;
    }

    public String getRouteName() {
      return routeName;
    }

    public void setRouteName(String routeName) {
      this.routeName = routeName;
    }

    public String getActionKey() {
      return actionKey;
    }

    public void setActionKey(String actionKey) {
      this.actionKey = actionKey;
    }

    public Integer getSortOrder() {
      return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
      this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }
  }
}

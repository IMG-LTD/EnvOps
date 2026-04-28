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
public interface UserAuthMapper {

  @Select("""
      SELECT id AS userId,
             user_name AS userName,
             password,
             phone,
             team_key AS teamKey,
             login_type AS loginType,
             status,
             last_login_at AS lastLoginAt
      FROM sys_user
      WHERE user_name = #{userName}
      """)
  UserAuthRow findByUserName(@Param("userName") String userName);

  @Select("""
      SELECT id AS userId,
             user_name AS userName,
             password,
             phone,
             team_key AS teamKey,
             login_type AS loginType,
             status,
             last_login_at AS lastLoginAt
      FROM sys_user
      WHERE phone = #{phone}
      """)
  UserAuthRow findByPhone(@Param("phone") String phone);

  @Select("""
      SELECT id AS userId,
             user_name AS userName,
             password,
             phone,
             team_key AS teamKey,
             login_type AS loginType,
             status,
             last_login_at AS lastLoginAt
      FROM sys_user
      WHERE id = #{userId}
      """)
  UserAuthRow findById(@Param("userId") Long userId);

  @Select("""
      SELECT r.role_key
      FROM sys_role r
      JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE ur.user_id = #{userId}
      ORDER BY r.id
      """)
  List<String> findRoleKeysByUserId(@Param("userId") Long userId);

  @Select("""
      SELECT r.role_key
      FROM sys_role r
      JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE ur.user_id = #{userId}
        AND r.enabled = TRUE
      ORDER BY r.id
      """)
  List<String> findEnabledRoleKeysByUserId(@Param("userId") Long userId);

  @Select("""
      SELECT u.id AS userId,
             u.user_name AS userName,
             u.phone AS phone,
             u.team_key AS teamKey,
             u.login_type AS loginType,
             u.status AS status,
             u.last_login_at AS lastLoginAt,
             r.role_key AS roleKey
      FROM sys_user u
      LEFT JOIN sys_user_role ur ON ur.user_id = u.id
      LEFT JOIN sys_role r ON r.id = ur.role_id
      ORDER BY u.id, r.id
      """)
  List<UserListRow> findAllUsers();

  @Select("""
      SELECT u.id AS userId,
             u.user_name AS userName,
             u.phone AS phone,
             u.team_key AS teamKey,
             u.login_type AS loginType,
             u.status AS status,
             u.last_login_at AS lastLoginAt,
             r.role_key AS roleKey
      FROM sys_user u
      LEFT JOIN sys_user_role ur ON ur.user_id = u.id
      LEFT JOIN sys_role r ON r.id = ur.role_id
      WHERE u.id = #{userId}
      ORDER BY u.id, r.id
      """)
  List<UserListRow> findUserRowsByUserId(@Param("userId") Long userId);

  @Select("""
      SELECT id AS roleId,
             role_key AS roleKey,
             role_name AS roleName,
             enabled AS enabled,
             built_in AS builtIn
      FROM sys_role
      ORDER BY id
      """)
  List<RoleRow> findAllRoles();

  @Select("""
      SELECT r.id AS roleId,
             r.role_key AS roleKey,
             r.role_name AS roleName,
             r.enabled AS enabled,
             r.built_in AS builtIn
      FROM sys_role r
      JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE ur.user_id = #{userId}
      ORDER BY r.id
      """)
  List<RoleRow> findRolesByUserId(@Param("userId") Long userId);

  @Select("""
      SELECT id AS roleId,
             role_key AS roleKey,
             role_name AS roleName,
             enabled AS enabled,
             built_in AS builtIn
      FROM sys_role
      WHERE enabled = TRUE
      ORDER BY id
      """)
  List<RoleRow> findEnabledRoles();

  @Select("""
      SELECT COUNT(*)
      FROM sys_user u
      JOIN sys_user_role ur ON ur.user_id = u.id
      JOIN sys_role r ON r.id = ur.role_id
      WHERE u.status = 'ACTIVE'
        AND r.role_key = 'SUPER_ADMIN'
        AND r.enabled = TRUE
      """)
  Integer countActiveEnabledSuperAdminUsers();

  @Select("""
      SELECT COALESCE(MAX(id), 0) + 1
      FROM sys_user
      """)
  Long nextUserId();

  @Insert("""
      INSERT INTO sys_user (id, user_name, password, phone, team_key, login_type, status, last_login_at)
      VALUES (#{id}, #{userName}, #{password}, #{phone}, #{teamKey}, #{loginType}, #{status}, #{lastLoginAt})
      """)
  int insertUser(UserEntity entity);

  @Update("""
      UPDATE sys_user
      SET user_name = #{userName},
          password = #{password},
          phone = #{phone},
          team_key = #{teamKey},
          login_type = #{loginType},
          status = #{status},
          last_login_at = #{lastLoginAt}
      WHERE id = #{id}
      """)
  int updateUser(UserEntity entity);

  @Delete("""
      DELETE FROM sys_user_role
      WHERE user_id = #{userId}
      """)
  int deleteUserRoles(@Param("userId") Long userId);

  @Insert("""
      INSERT INTO sys_user_role (user_id, role_id)
      VALUES (#{userId}, #{roleId})
      """)
  int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

  @Update("""
      UPDATE sys_user
      SET login_type = #{loginType},
          last_login_at = CURRENT_TIMESTAMP
      WHERE id = #{userId}
      """)
  void recordUserLogin(@Param("userId") Long userId, @Param("loginType") String loginType);

  class UserAuthRow {
    private Long userId;
    private String userName;
    private String password;
    private String phone;
    private String teamKey;
    private String loginType;
    private String status;
    private LocalDateTime lastLoginAt;

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getTeamKey() {
      return teamKey;
    }

    public void setTeamKey(String teamKey) {
      this.teamKey = teamKey;
    }

    public String getLoginType() {
      return loginType;
    }

    public void setLoginType(String loginType) {
      this.loginType = loginType;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
      return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
      this.lastLoginAt = lastLoginAt;
    }
  }

  class UserListRow {
    private Long userId;
    private String userName;
    private String phone;
    private String teamKey;
    private String loginType;
    private String status;
    private LocalDateTime lastLoginAt;
    private String roleKey;

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getTeamKey() {
      return teamKey;
    }

    public void setTeamKey(String teamKey) {
      this.teamKey = teamKey;
    }

    public String getLoginType() {
      return loginType;
    }

    public void setLoginType(String loginType) {
      this.loginType = loginType;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
      return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
      this.lastLoginAt = lastLoginAt;
    }

    public String getRoleKey() {
      return roleKey;
    }

    public void setRoleKey(String roleKey) {
      this.roleKey = roleKey;
    }
  }

  class RoleRow {
    private Long roleId;
    private String roleKey;
    private String roleName;
    private Boolean enabled;
    private Boolean builtIn;

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
  }

  class UserEntity {
    private Long id;
    private String userName;
    private String password;
    private String phone;
    private String teamKey;
    private String loginType;
    private String status;
    private LocalDateTime lastLoginAt;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getTeamKey() {
      return teamKey;
    }

    public void setTeamKey(String teamKey) {
      this.teamKey = teamKey;
    }

    public String getLoginType() {
      return loginType;
    }

    public void setLoginType(String loginType) {
      this.loginType = loginType;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
      return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
      this.lastLoginAt = lastLoginAt;
    }
  }
}

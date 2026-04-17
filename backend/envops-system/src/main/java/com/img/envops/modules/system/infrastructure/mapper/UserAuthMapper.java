package com.img.envops.modules.system.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAuthMapper {

  @Select("""
      SELECT id AS userId,
             user_name AS userName,
             password
      FROM sys_user
      WHERE user_name = #{userName}
      """)
  UserAuthRow findByUserName(@Param("userName") String userName);

  @Select("""
      SELECT r.role_key
      FROM sys_role r
      JOIN sys_user_role ur ON ur.role_id = r.id
      WHERE ur.user_id = #{userId}
      ORDER BY r.id
      """)
  List<String> findRoleKeysByUserId(@Param("userId") Long userId);

  class UserAuthRow {
    private Long userId;
    private String userName;
    private String password;

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
  }
}

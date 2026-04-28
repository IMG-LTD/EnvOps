package com.img.envops.modules.system.interfaces.user;

import com.img.envops.common.response.R;
import com.img.envops.modules.system.application.user.UserApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/users")
public class UserController {
  private final UserApplicationService userApplicationService;

  public UserController(UserApplicationService userApplicationService) {
    this.userApplicationService = userApplicationService;
  }

  @GetMapping
  public R<List<UserApplicationService.SystemUserRecord>> getUsers() {
    return R.ok(userApplicationService.getUsers());
  }

  @GetMapping("/{id}/roles")
  public R<UserApplicationService.UserRoleAssignment> getUserRoles(@PathVariable Long id) {
    return R.ok(userApplicationService.getUserRoles(id));
  }

  @PutMapping("/{id}/roles")
  public R<UserApplicationService.UserRoleAssignment> replaceUserRoles(@PathVariable Long id,
                                                                       @RequestBody(required = false) ReplaceSystemUserRolesRequest request) {
    return R.ok(userApplicationService.replaceUserRoles(id, new UserApplicationService.ReplaceUserRolesCommand(
        request == null ? null : request.roleIds())));
  }

  @PostMapping
  public R<UserApplicationService.SystemUserRecord> createUser(@RequestBody(required = false) CreateSystemUserRequest request) {
    return R.ok(userApplicationService.createUser(new UserApplicationService.CreateSystemUserCommand(
        request == null ? null : request.userName(),
        request == null ? null : request.password(),
        request == null ? null : request.phone(),
        request == null ? null : request.teamKey(),
        request == null ? null : request.loginType(),
        request == null ? null : request.status(),
        request == null ? null : request.roles())));
  }

  @PutMapping("/{id}")
  public R<UserApplicationService.SystemUserRecord> updateUser(@PathVariable Long id,
                                                               @RequestBody(required = false) UpdateSystemUserRequest request) {
    return R.ok(userApplicationService.updateUser(id, new UserApplicationService.UpdateSystemUserCommand(
        request == null ? null : request.userName(),
        request == null ? null : request.password(),
        request == null ? null : request.phone(),
        request == null ? null : request.teamKey(),
        request == null ? null : request.loginType(),
        request == null ? null : request.status(),
        request == null ? null : request.roles())));
  }

  public record CreateSystemUserRequest(String userName,
                                        String password,
                                        String phone,
                                        String teamKey,
                                        String loginType,
                                        String status,
                                        List<String> roles) {
  }

  public record UpdateSystemUserRequest(String userName,
                                        String password,
                                        String phone,
                                        String teamKey,
                                        String loginType,
                                        String status,
                                        List<String> roles) {
  }

  public record ReplaceSystemUserRolesRequest(List<Long> roleIds) {
  }
}

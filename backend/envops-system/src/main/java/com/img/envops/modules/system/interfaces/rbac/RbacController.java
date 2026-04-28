package com.img.envops.modules.system.interfaces.rbac;

import com.img.envops.common.response.R;
import com.img.envops.modules.system.application.rbac.RbacApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/rbac")
public class RbacController {
  private final RbacApplicationService rbacApplicationService;

  public RbacController(RbacApplicationService rbacApplicationService) {
    this.rbacApplicationService = rbacApplicationService;
  }

  @GetMapping("/roles")
  public R<List<RbacApplicationService.RoleRecord>> getRoles() {
    return R.ok(rbacApplicationService.getRoles());
  }

  @PostMapping("/roles")
  public R<RbacApplicationService.RoleRecord> createRole(@RequestBody(required = false) CreateRoleRequest request) {
    return R.ok(rbacApplicationService.createRole(new RbacApplicationService.CreateRoleCommand(
        request == null ? null : request.roleKey(),
        request == null ? null : request.roleName(),
        request == null ? null : request.description(),
        request == null ? null : request.enabled())));
  }

  @PutMapping("/roles/{id}")
  public R<RbacApplicationService.RoleRecord> updateRole(@PathVariable Long id,
                                                         @RequestBody(required = false) UpdateRoleRequest request) {
    return R.ok(rbacApplicationService.updateRole(id, new RbacApplicationService.UpdateRoleCommand(
        request == null ? null : request.roleName(),
        request == null ? null : request.description(),
        request == null ? null : request.enabled())));
  }

  @GetMapping("/permissions")
  public R<List<RbacApplicationService.PermissionModule>> getPermissionTree() {
    return R.ok(rbacApplicationService.getPermissionTree());
  }

  @GetMapping("/roles/{id}/permissions")
  public R<RbacApplicationService.RolePermissions> getRolePermissions(@PathVariable Long id) {
    return R.ok(rbacApplicationService.getRolePermissions(id));
  }

  @PutMapping("/roles/{id}/permissions")
  public R<RbacApplicationService.RolePermissions> replaceRolePermissions(@PathVariable Long id,
                                                                          @RequestBody(required = false) ReplaceRolePermissionsRequest request) {
    return R.ok(rbacApplicationService.replaceRolePermissions(id, new RbacApplicationService.ReplaceRolePermissionsCommand(
        request == null ? null : request.permissionKeys())));
  }

  public record CreateRoleRequest(String roleKey,
                                  String roleName,
                                  String description,
                                  Boolean enabled) {
  }

  public record UpdateRoleRequest(String roleName,
                                  String description,
                                  Boolean enabled) {
  }

  public record ReplaceRolePermissionsRequest(List<String> permissionKeys) {
  }
}

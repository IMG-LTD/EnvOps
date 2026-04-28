package com.img.envops.modules.system.application.rbac;

import com.img.envops.common.exception.ConflictException;
import com.img.envops.common.exception.NotFoundException;
import com.img.envops.common.security.PermissionKeys;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper.PermissionRow;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper.RoleEntity;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper.RoleRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RbacApplicationService {
  private static final String SUPER_ADMIN_ROLE_KEY = "SUPER_ADMIN";
  private static final Pattern ROLE_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");
  private static final Set<String> SUPER_ADMIN_REQUIRED_PERMISSION_KEYS = Set.of(
      PermissionKeys.Menu.SYSTEM_RBAC,
      PermissionKeys.Action.SYSTEM_ROLE_MANAGE);

  private final RbacMapper rbacMapper;

  public RbacApplicationService(RbacMapper rbacMapper) {
    this.rbacMapper = rbacMapper;
  }

  public List<RoleRecord> getRoles() {
    return rbacMapper.findRoles().stream().map(this::toRoleRecord).toList();
  }

  @Transactional
  public RoleRecord createRole(CreateRoleCommand command) {
    NormalizedRoleMutation mutation = normalizeCreateCommand(command);
    if (rbacMapper.findRoleByKey(mutation.roleKey()) != null) {
      throw new ConflictException("roleKey already exists: " + mutation.roleKey());
    }

    Long roleId = rbacMapper.nextRoleId();
    LocalDateTime now = LocalDateTime.now();
    RoleEntity entity = new RoleEntity();
    entity.setRoleId(roleId);
    entity.setRoleKey(mutation.roleKey());
    entity.setRoleName(mutation.roleName());
    entity.setDescription(mutation.description());
    entity.setEnabled(mutation.enabled());
    entity.setBuiltIn(false);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    rbacMapper.insertRole(entity);

    return toRoleRecord(requireRole(roleId));
  }

  @Transactional
  public RoleRecord updateRole(Long roleId, UpdateRoleCommand command) {
    if (roleId == null) {
      throw new IllegalArgumentException("roleId is required");
    }

    RoleRow existingRole = requireRole(roleId);
    NormalizedRoleMutation mutation = normalizeUpdateCommand(command);
    if (isSuperAdmin(existingRole) && !mutation.enabled()) {
      throw new ConflictException("SUPER_ADMIN role must remain enabled");
    }

    RoleEntity entity = new RoleEntity();
    entity.setRoleId(roleId);
    entity.setRoleKey(existingRole.getRoleKey());
    entity.setRoleName(mutation.roleName());
    entity.setDescription(mutation.description());
    entity.setEnabled(mutation.enabled());
    entity.setBuiltIn(existingRole.getBuiltIn());
    entity.setCreatedAt(existingRole.getCreatedAt());
    entity.setUpdatedAt(LocalDateTime.now());
    rbacMapper.updateRole(entity);

    ensureActiveSuperAdminStillExists();
    return toRoleRecord(requireRole(roleId));
  }

  public List<PermissionModule> getPermissionTree() {
    Map<String, List<PermissionRow>> rowsByModule = new LinkedHashMap<>();
    for (PermissionRow row : rbacMapper.findEnabledPermissions()) {
      rowsByModule.computeIfAbsent(row.getModuleKey(), ignored -> new ArrayList<>()).add(row);
    }

    List<PermissionModule> modules = new ArrayList<>();
    for (Map.Entry<String, List<PermissionRow>> entry : rowsByModule.entrySet()) {
      modules.add(new PermissionModule(entry.getKey(), resolveModuleName(entry.getKey(), entry.getValue()), buildPermissionNodes(entry.getValue())));
    }
    return List.copyOf(modules);
  }

  public RolePermissions getRolePermissions(Long roleId) {
    RoleRow role = requireRoleId(roleId);
    return new RolePermissions(role.getRoleId(), role.getRoleKey(), rbacMapper.findRolePermissionKeys(role.getRoleId()));
  }

  @Transactional
  public RolePermissions replaceRolePermissions(Long roleId, ReplaceRolePermissionsCommand command) {
    RoleRow role = requireRoleId(roleId);
    List<String> permissionKeys = normalizePermissionKeys(command);
    ensurePermissionsAreEnabled(permissionKeys);
    ensureSuperAdminPermissionsRemain(role, permissionKeys);

    rbacMapper.deleteRolePermissions(role.getRoleId());
    for (String permissionKey : permissionKeys) {
      rbacMapper.insertRolePermission(role.getRoleId(), permissionKey);
    }

    ensureActiveSuperAdminStillExists();
    return getRolePermissions(role.getRoleId());
  }

  private String resolveModuleName(String moduleKey, List<PermissionRow> rows) {
    return rows.stream()
        .filter(row -> Objects.equals("menu", row.getPermissionType()))
        .filter(row -> Objects.equals(moduleKey, row.getPermissionKey()))
        .findFirst()
        .map(PermissionRow::getPermissionName)
        .orElse(moduleKey);
  }

  private List<PermissionNode> buildPermissionNodes(List<PermissionRow> rows) {
    Map<String, MutablePermissionNode> menuNodesByKey = new LinkedHashMap<>();
    List<MutablePermissionNode> moduleNodes = new ArrayList<>();

    for (PermissionRow row : rows) {
      if (Objects.equals("menu", row.getPermissionType())) {
        MutablePermissionNode menuNode = new MutablePermissionNode(row, new ArrayList<>());
        menuNodesByKey.put(row.getPermissionKey(), menuNode);
        moduleNodes.add(menuNode);
      }
    }

    for (PermissionRow row : rows) {
      if (!Objects.equals("action", row.getPermissionType())) {
        continue;
      }

      MutablePermissionNode actionNode = new MutablePermissionNode(row, new ArrayList<>());
      MutablePermissionNode parentMenu = StringUtils.hasText(row.getParentKey()) ? menuNodesByKey.get(row.getParentKey()) : null;
      if (parentMenu == null) {
        moduleNodes.add(actionNode);
      } else {
        parentMenu.children().add(actionNode);
      }
    }

    return moduleNodes.stream().map(MutablePermissionNode::toRecord).toList();
  }

  private RoleRow requireRoleId(Long roleId) {
    if (roleId == null) {
      throw new IllegalArgumentException("roleId is required");
    }
    return requireRole(roleId);
  }

  private RoleRow requireRole(Long roleId) {
    RoleRow role = rbacMapper.findRoleById(roleId);
    if (role == null) {
      throw new NotFoundException("role not found: " + roleId);
    }
    return role;
  }

  private NormalizedRoleMutation normalizeCreateCommand(CreateRoleCommand command) {
    if (command == null || !StringUtils.hasText(command.roleKey())) {
      throw new IllegalArgumentException("roleKey is required");
    }
    return new NormalizedRoleMutation(
        normalizeRoleKey(command.roleKey()),
        normalizeRoleName(command.roleName()),
        normalizeDescription(command.description()),
        command.enabled() == null || command.enabled());
  }

  private NormalizedRoleMutation normalizeUpdateCommand(UpdateRoleCommand command) {
    if (command == null || command.enabled() == null) {
      throw new IllegalArgumentException("roleName and enabled are required");
    }
    return new NormalizedRoleMutation(
        null,
        normalizeRoleName(command.roleName()),
        normalizeDescription(command.description()),
        command.enabled());
  }

  private String normalizeRoleKey(String roleKey) {
    String normalizedRoleKey = roleKey.trim()
        .replaceAll("[\\s-]+", "_")
        .toUpperCase(Locale.ROOT);
    if (!ROLE_KEY_PATTERN.matcher(normalizedRoleKey).matches()) {
      throw new IllegalArgumentException("roleKey must use uppercase letters, numbers and underscores");
    }
    return normalizedRoleKey;
  }

  private String normalizeRoleName(String roleName) {
    if (!StringUtils.hasText(roleName)) {
      throw new IllegalArgumentException("roleName is required");
    }
    return roleName.trim();
  }

  private String normalizeDescription(String description) {
    return StringUtils.hasText(description) ? description.trim() : null;
  }

  private List<String> normalizePermissionKeys(ReplaceRolePermissionsCommand command) {
    if (command == null || command.permissionKeys() == null) {
      throw new IllegalArgumentException("permissionKeys is required");
    }

    LinkedHashSet<String> permissionKeys = new LinkedHashSet<>();
    for (String permissionKey : command.permissionKeys()) {
      if (!StringUtils.hasText(permissionKey)) {
        throw new IllegalArgumentException("permissionKey is required");
      }
      permissionKeys.add(permissionKey.trim());
    }
    return List.copyOf(permissionKeys);
  }

  private void ensurePermissionsAreEnabled(List<String> permissionKeys) {
    if (permissionKeys.isEmpty()) {
      return;
    }

    Set<String> enabledPermissionKeys = new LinkedHashSet<>(rbacMapper.findEnabledPermissionKeysByKeys(permissionKeys));
    if (enabledPermissionKeys.size() == permissionKeys.size()) {
      return;
    }

    List<String> missingPermissionKeys = permissionKeys.stream()
        .filter(permissionKey -> !enabledPermissionKeys.contains(permissionKey))
        .toList();
    throw new IllegalArgumentException("unknown or disabled permission keys: " + missingPermissionKeys);
  }

  private void ensureSuperAdminPermissionsRemain(RoleRow role, List<String> permissionKeys) {
    if (!isSuperAdmin(role)) {
      return;
    }

    Set<String> permissionKeySet = new LinkedHashSet<>(permissionKeys);
    List<String> missingRequiredPermissionKeys = SUPER_ADMIN_REQUIRED_PERMISSION_KEYS.stream()
        .filter(permissionKey -> !permissionKeySet.contains(permissionKey))
        .toList();
    if (!missingRequiredPermissionKeys.isEmpty()) {
      throw new ConflictException("SUPER_ADMIN must keep permissions: " + missingRequiredPermissionKeys);
    }
  }

  private void ensureActiveSuperAdminStillExists() {
    if (rbacMapper.countActiveSuperAdminUsers() < 1) {
      throw new ConflictException("at least one active enabled SUPER_ADMIN user is required");
    }
  }

  private boolean isSuperAdmin(RoleRow role) {
    return role != null && Objects.equals(SUPER_ADMIN_ROLE_KEY, role.getRoleKey());
  }

  private RoleRecord toRoleRecord(RoleRow row) {
    return new RoleRecord(
        row.getRoleId(),
        row.getRoleKey(),
        row.getRoleName(),
        row.getDescription(),
        row.getEnabled(),
        row.getBuiltIn(),
        row.getCreatedAt(),
        row.getUpdatedAt());
  }

  public record RoleRecord(Long id,
                           String roleKey,
                           String roleName,
                           String description,
                           Boolean enabled,
                           Boolean builtIn,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
  }

  public record CreateRoleCommand(String roleKey,
                                  String roleName,
                                  String description,
                                  Boolean enabled) {
  }

  public record UpdateRoleCommand(String roleName,
                                  String description,
                                  Boolean enabled) {
  }

  public record PermissionModule(String moduleKey,
                                 String moduleName,
                                 List<PermissionNode> permissions) {
  }

  public record PermissionNode(Long id,
                               String permissionKey,
                               String permissionName,
                               String permissionType,
                               String moduleKey,
                               String parentKey,
                               String routeName,
                               String actionKey,
                               Integer sortOrder,
                               Boolean enabled,
                               List<PermissionNode> children) {
  }

  public record RolePermissions(Long roleId,
                                String roleKey,
                                List<String> permissionKeys) {
  }

  public record ReplaceRolePermissionsCommand(List<String> permissionKeys) {
  }

  private record NormalizedRoleMutation(String roleKey,
                                        String roleName,
                                        String description,
                                        Boolean enabled) {
  }

  private record MutablePermissionNode(PermissionRow row,
                                       List<MutablePermissionNode> children) {
    private PermissionNode toRecord() {
      return new PermissionNode(
          row.getPermissionId(),
          row.getPermissionKey(),
          row.getPermissionName(),
          row.getPermissionType(),
          row.getModuleKey(),
          row.getParentKey(),
          row.getRouteName(),
          row.getActionKey(),
          row.getSortOrder(),
          row.getEnabled(),
          children.stream().map(MutablePermissionNode::toRecord).toList());
    }
  }
}

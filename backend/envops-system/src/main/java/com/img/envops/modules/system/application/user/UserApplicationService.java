package com.img.envops.modules.system.application.user;

import com.img.envops.common.exception.ConflictException;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper.RoleRow;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper.UserAuthRow;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper.UserListRow;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.regex.Pattern;

@Service
public class UserApplicationService {
  private static final List<String> TEAM_KEYS = List.of("PLATFORM", "RELEASE", "TRAFFIC", "QA", "ENVOPS", "SRE", "FINTECH");
  private static final List<String> LOGIN_TYPES = List.of("PASSWORD", "PASSWORD_OTP", "SSO");
  private static final List<String> USER_STATUSES = List.of("ACTIVE", "REVIEW", "DISABLED");
  private static final Pattern PHONE_PATTERN = Pattern.compile(
      "^[1](([3][0-9])|([4][01456789])|([5][012356789])|([6][2567])|([7][0-8])|([8][0-9])|([9][012356789]))[0-9]{8}$");

  private final UserAuthMapper userAuthMapper;
  private final PasswordEncoder passwordEncoder;

  public UserApplicationService(UserAuthMapper userAuthMapper, PasswordEncoder passwordEncoder) {
    this.userAuthMapper = userAuthMapper;
    this.passwordEncoder = passwordEncoder;
  }

  public List<SystemUserRecord> getUsers() {
    return aggregateUsers(userAuthMapper.findAllUsers());
  }

  public UserRoleAssignment getUserRoles(Long userId) {
    requireUser(userId);
    List<UserRoleRecord> roles = userAuthMapper.findRolesByUserId(userId).stream()
        .map(this::toUserRoleRecord)
        .toList();
    return new UserRoleAssignment(
        userId,
        roles,
        roles.stream().map(UserRoleRecord::id).toList(),
        roles.stream().map(UserRoleRecord::roleKey).toList());
  }

  @Transactional
  public UserRoleAssignment replaceUserRoles(Long userId, ReplaceUserRolesCommand command) {
    requireUser(userId);
    if (command == null || command.roleIds() == null || command.roleIds().isEmpty()) {
      throw new IllegalArgumentException("roleIds must not be empty");
    }

    Map<Long, RoleRow> enabledRoles = userAuthMapper.findEnabledRoles().stream()
        .collect(java.util.stream.Collectors.toMap(RoleRow::getRoleId, role -> role, (left, right) -> left, LinkedHashMap::new));

    List<RoleBinding> roles = command.roleIds().stream()
        .distinct()
        .map(roleId -> {
          RoleRow role = enabledRoles.get(roleId);
          if (role == null) {
            throw new IllegalArgumentException("roleId is invalid: " + roleId);
          }
          return new RoleBinding(role.getRoleId(), role.getRoleKey());
        })
        .toList();

    replaceUserRoles(userId, roles);
    ensureActiveSuperAdminExists();
    return getUserRoles(userId);
  }

  @Transactional
  public SystemUserRecord createUser(CreateSystemUserCommand command) {
    NormalizedUserMutation mutation = normalizeCreateCommand(command);
    ensureUniqueUserName(mutation.userName(), null);
    ensureUniquePhone(mutation.phone(), null);

    Long userId = userAuthMapper.nextUserId();
    userAuthMapper.insertUser(buildUserEntity(
        userId,
        mutation.userName(),
        mutation.password(),
        mutation.phone(),
        mutation.teamKey(),
        mutation.loginType(),
        mutation.status(),
        null));
    replaceUserRoles(userId, mutation.roles());

    return getUser(userId);
  }

  @Transactional
  public SystemUserRecord updateUser(Long userId, UpdateSystemUserCommand command) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    UserAuthRow existingUser = requireUser(userId);
    NormalizedUserMutation mutation = normalizeUpdateCommand(command, existingUser.getPassword());
    ensureUniqueUserName(mutation.userName(), userId);
    ensureUniquePhone(mutation.phone(), userId);

    userAuthMapper.updateUser(buildUserEntity(
        userId,
        mutation.userName(),
        mutation.password(),
        mutation.phone(),
        mutation.teamKey(),
        mutation.loginType(),
        mutation.status(),
        existingUser.getLastLoginAt()));
    replaceUserRoles(userId, mutation.roles());
    ensureActiveSuperAdminExists();

    return getUser(userId);
  }

  private UserAuthMapper.UserEntity buildUserEntity(Long id,
                                                    String userName,
                                                    String password,
                                                    String phone,
                                                    String teamKey,
                                                    String loginType,
                                                    String status,
                                                    LocalDateTime lastLoginAt) {
    UserAuthMapper.UserEntity entity = new UserAuthMapper.UserEntity();
    entity.setId(id);
    entity.setUserName(userName);
    entity.setPassword(password);
    entity.setPhone(phone);
    entity.setTeamKey(teamKey);
    entity.setLoginType(loginType);
    entity.setStatus(status);
    entity.setLastLoginAt(lastLoginAt);
    return entity;
  }

  private SystemUserRecord getUser(Long userId) {
    return aggregateUsers(userAuthMapper.findUserRowsByUserId(userId)).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
  }

  private List<SystemUserRecord> aggregateUsers(List<UserListRow> rows) {
    Map<Long, UserAccumulator> users = new LinkedHashMap<>();

    for (UserListRow row : rows) {
      UserAccumulator accumulator = users.computeIfAbsent(
          row.getUserId(),
          id -> new UserAccumulator(
              id,
              row.getUserName(),
              row.getPhone(),
              row.getTeamKey(),
              row.getLoginType(),
              row.getStatus(),
              row.getLastLoginAt(),
              new ArrayList<>()));

      if (StringUtils.hasText(row.getRoleKey())) {
        accumulator.roles().add(row.getRoleKey().trim());
      }
    }

    return users.values().stream().map(UserAccumulator::build).toList();
  }

  private UserAuthRow requireUser(Long userId) {
    UserAuthRow user = userAuthMapper.findById(userId);
    if (user == null) {
      throw new IllegalArgumentException("user not found: " + userId);
    }
    return user;
  }

  private void ensureUniqueUserName(String userName, Long currentUserId) {
    UserAuthRow existingUser = userAuthMapper.findByUserName(userName);
    if (existingUser != null && !Objects.equals(existingUser.getUserId(), currentUserId)) {
      throw new ConflictException("userName already exists: " + userName);
    }
  }

  private void ensureUniquePhone(String phone, Long currentUserId) {
    UserAuthRow existingUser = userAuthMapper.findByPhone(phone);
    if (existingUser != null && !Objects.equals(existingUser.getUserId(), currentUserId)) {
      throw new ConflictException("phone already exists: " + phone);
    }
  }

  private void replaceUserRoles(Long userId, List<RoleBinding> roles) {
    userAuthMapper.deleteUserRoles(userId);
    for (RoleBinding role : roles) {
      userAuthMapper.insertUserRole(userId, role.roleId());
    }
  }

  private UserRoleRecord toUserRoleRecord(RoleRow row) {
    return new UserRoleRecord(row.getRoleId(), row.getRoleKey(), row.getRoleName(), row.getEnabled(), row.getBuiltIn());
  }

  private void ensureActiveSuperAdminExists() {
    Integer count = userAuthMapper.countActiveEnabledSuperAdminUsers();
    if (count == null || count <= 0) {
      throw new ConflictException("at least one active SUPER_ADMIN user is required");
    }
  }

  private NormalizedUserMutation normalizeCreateCommand(CreateSystemUserCommand command) {
    if (command == null
        || !StringUtils.hasText(command.userName())
        || !StringUtils.hasText(command.password())
        || !StringUtils.hasText(command.phone())
        || !StringUtils.hasText(command.teamKey())
        || !StringUtils.hasText(command.loginType())
        || !StringUtils.hasText(command.status())
        || command.roles() == null
        || command.roles().isEmpty()) {
      throw new IllegalArgumentException("userName, password, phone, teamKey, loginType, status and roles are required");
    }

    return new NormalizedUserMutation(
        normalizeUserName(command.userName()),
        normalizePassword(command.password(), true),
        normalizePhone(command.phone()),
        normalizeTeamKey(command.teamKey()),
        normalizeAllowedValue(command.loginType(), LOGIN_TYPES, "loginType"),
        normalizeAllowedValue(command.status(), USER_STATUSES, "status"),
        normalizeRoles(command.roles()));
  }

  private NormalizedUserMutation normalizeUpdateCommand(UpdateSystemUserCommand command, String existingPassword) {
    if (command == null
        || !StringUtils.hasText(command.userName())
        || !StringUtils.hasText(command.phone())
        || !StringUtils.hasText(command.teamKey())
        || !StringUtils.hasText(command.loginType())
        || !StringUtils.hasText(command.status())
        || command.roles() == null
        || command.roles().isEmpty()) {
      throw new IllegalArgumentException("userName, phone, teamKey, loginType, status and roles are required");
    }

    return new NormalizedUserMutation(
        normalizeUserName(command.userName()),
        normalizePassword(command.password(), false, existingPassword),
        normalizePhone(command.phone()),
        normalizeTeamKey(command.teamKey()),
        normalizeAllowedValue(command.loginType(), LOGIN_TYPES, "loginType"),
        normalizeAllowedValue(command.status(), USER_STATUSES, "status"),
        normalizeRoles(command.roles()));
  }

  private String normalizeUserName(String userName) {
    return userName.trim();
  }

  private String normalizePassword(String password, boolean required) {
    if (!required && !StringUtils.hasText(password)) {
      return null;
    }

    if (!StringUtils.hasText(password)) {
      throw new IllegalArgumentException("password is required");
    }

    return passwordEncoder.encode(password.trim());
  }

  private String normalizePassword(String password, boolean required, String fallbackPassword) {
    String normalizedPassword = normalizePassword(password, required);
    return normalizedPassword == null ? fallbackPassword : normalizedPassword;
  }

  private String normalizePhone(String phone) {
    String normalizedPhone = phone.trim();
    if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
      throw new IllegalArgumentException("phone is invalid");
    }
    return normalizedPhone;
  }

  private String normalizeTeamKey(String teamKey) {
    return normalizeAllowedValue(teamKey, TEAM_KEYS, "teamKey").toLowerCase(Locale.ROOT);
  }

  private String normalizeAllowedValue(String value, List<String> allowedValues, String fieldName) {
    String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
    if (!allowedValues.contains(normalizedValue)) {
      throw new IllegalArgumentException(fieldName + " must be one of " + allowedValues);
    }
    return normalizedValue;
  }

  private List<RoleBinding> normalizeRoles(List<String> roles) {
    LinkedHashSet<String> roleKeys = new LinkedHashSet<>();
    for (String role : roles) {
      if (StringUtils.hasText(role)) {
        roleKeys.add(role.trim().toUpperCase(Locale.ROOT));
      }
    }

    if (roleKeys.isEmpty()) {
      throw new IllegalArgumentException("roles must not be empty");
    }

    Map<String, RoleRow> availableRoles = new LinkedHashMap<>();
    for (RoleRow role : userAuthMapper.findEnabledRoles()) {
      availableRoles.put(role.getRoleKey(), role);
    }

    List<RoleBinding> normalizedRoles = new ArrayList<>();
    for (String roleKey : roleKeys) {
      RoleRow role = availableRoles.get(roleKey);
      if (role == null) {
        throw new IllegalArgumentException("roles must be one of " + availableRoles.keySet());
      }
      normalizedRoles.add(new RoleBinding(role.getRoleId(), roleKey));
    }

    return List.copyOf(normalizedRoles);
  }

  public record UserRoleAssignment(Long userId,
                                   List<UserRoleRecord> roles,
                                   List<Long> roleIds,
                                   List<String> roleKeys) {
  }

  public record UserRoleRecord(Long id, String roleKey, String roleName, Boolean enabled, Boolean builtIn) {
  }

  public record ReplaceUserRolesCommand(List<Long> roleIds) {
  }

  public record SystemUserRecord(Long id,
                                 String userName,
                                 String phone,
                                 String teamKey,
                                 String loginType,
                                 String status,
                                 LocalDateTime lastLoginAt,
                                 List<String> roles) {
  }

  public record CreateSystemUserCommand(String userName,
                                        String password,
                                        String phone,
                                        String teamKey,
                                        String loginType,
                                        String status,
                                        List<String> roles) {
  }

  public record UpdateSystemUserCommand(String userName,
                                        String password,
                                        String phone,
                                        String teamKey,
                                        String loginType,
                                        String status,
                                        List<String> roles) {
  }

  private record UserAccumulator(Long id,
                                 String userName,
                                 String phone,
                                 String teamKey,
                                 String loginType,
                                 String status,
                                 LocalDateTime lastLoginAt,
                                 List<String> roles) {
    private SystemUserRecord build() {
      return new SystemUserRecord(id, userName, phone, teamKey, loginType, status, lastLoginAt, List.copyOf(roles));
    }
  }

  private record RoleBinding(Long roleId, String roleKey) {
  }

  private record NormalizedUserMutation(String userName,
                                        String password,
                                        String phone,
                                        String teamKey,
                                        String loginType,
                                        String status,
                                        List<RoleBinding> roles) {
  }
}

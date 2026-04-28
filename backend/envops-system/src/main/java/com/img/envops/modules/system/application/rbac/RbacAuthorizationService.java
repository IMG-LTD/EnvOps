package com.img.envops.modules.system.application.rbac;

import com.img.envops.framework.security.EffectivePermissionService;
import com.img.envops.modules.system.infrastructure.mapper.RbacMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class RbacAuthorizationService implements EffectivePermissionService {
  private final RbacMapper rbacMapper;

  public RbacAuthorizationService(RbacMapper rbacMapper) {
    this.rbacMapper = rbacMapper;
  }

  @Override
  public Set<String> findEffectivePermissionKeys(String username) {
    if (!StringUtils.hasText(username)) {
      return Set.of();
    }

    return new LinkedHashSet<>(rbacMapper.findEffectivePermissionKeysByUserName(username.trim()));
  }

  public Set<String> findEffectiveActionPermissionKeys(String username) {
    if (!StringUtils.hasText(username)) {
      return Set.of();
    }

    return new LinkedHashSet<>(rbacMapper.findEffectiveActionPermissionKeysByUserName(username.trim()));
  }
}

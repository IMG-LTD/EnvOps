package com.img.envops.framework.security;

import java.util.Set;

public interface EffectivePermissionService {
  Set<String> findEffectivePermissionKeys(String username);
}

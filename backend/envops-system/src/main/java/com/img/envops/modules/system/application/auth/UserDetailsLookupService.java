package com.img.envops.modules.system.application.auth;

import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper.UserAuthRow;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserDetailsLookupService implements UserDetailsService {
  private final UserAuthMapper userAuthMapper;

  public UserDetailsLookupService(UserAuthMapper userAuthMapper) {
    this.userAuthMapper = userAuthMapper;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (!StringUtils.hasText(username)) {
      throw new UsernameNotFoundException("User not found");
    }

    UserAuthRow user = userAuthMapper.findByUserName(username.trim());
    if (user == null) {
      throw new UsernameNotFoundException("User not found: " + username);
    }

    return User.withUsername(user.getUserName())
        .password(user.getPassword())
        .authorities(loadAuthorities(user.getUserId()))
        .build();
  }

  private List<GrantedAuthority> loadAuthorities(Long userId) {
    Set<String> authorities = new LinkedHashSet<>();
    for (String roleKey : userAuthMapper.findRoleKeysByUserId(userId)) {
      if (!StringUtils.hasText(roleKey)) {
        continue;
      }

      String normalizedRole = roleKey.trim();
      authorities.add(normalizedRole);
      if (!normalizedRole.startsWith("ROLE_")) {
        authorities.add("ROLE_" + normalizedRole);
      }
    }

    return authorities.stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList();
  }
}

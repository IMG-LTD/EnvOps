package com.img.envops.modules.system.application.auth;

import com.img.envops.common.exception.UnauthorizedException;
import com.img.envops.framework.security.JwtTokenService;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper.UserAuthRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class AuthApplicationService {
  private static final String DEFAULT_BUTTON = "envops:dashboard:view";

  private final UserAuthMapper userAuthMapper;
  private final JwtTokenService jwtTokenService;

  public AuthApplicationService(UserAuthMapper userAuthMapper, JwtTokenService jwtTokenService) {
    this.userAuthMapper = userAuthMapper;
    this.jwtTokenService = jwtTokenService;
  }

  public LoginToken login(LoginCommand command) {
    if (command == null || !StringUtils.hasText(command.userName()) || !StringUtils.hasText(command.password())) {
      throw new IllegalArgumentException("userName and password are required");
    }

    UserAuthRow user = userAuthMapper.findByUserName(command.userName().trim());
    if (user == null || !Objects.equals(user.getPassword(), command.password())) {
      throw new UnauthorizedException("Invalid username or password");
    }

    return new LoginToken(jwtTokenService.createAccessToken(user.getUserName()));
  }

  public UserInfo getUserInfo(String userName) {
    UserAuthRow user = requireUser(userName);
    List<String> roles = userAuthMapper.findRoleKeysByUserId(user.getUserId());

    return new UserInfo(
        String.valueOf(user.getUserId()),
        user.getUserName(),
        roles,
        List.of(DEFAULT_BUTTON));
  }

  public UserAuthRow requireUser(String userName) {
    if (!StringUtils.hasText(userName)) {
      throw new UnauthorizedException("Unauthorized");
    }

    UserAuthRow user = userAuthMapper.findByUserName(userName.trim());
    if (user == null) {
      throw new UnauthorizedException("Unauthorized");
    }

    return user;
  }

  public record LoginCommand(String userName, String password) {
  }

  public record LoginToken(String token) {
  }

  public record UserInfo(String userId, String userName, List<String> roles, List<String> buttons) {
  }
}

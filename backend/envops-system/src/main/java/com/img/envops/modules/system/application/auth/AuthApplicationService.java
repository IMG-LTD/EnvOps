package com.img.envops.modules.system.application.auth;

import com.img.envops.common.exception.UnauthorizedException;
import com.img.envops.framework.security.JwtTokenService;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper;
import com.img.envops.modules.system.infrastructure.mapper.UserAuthMapper.UserAuthRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthApplicationService {
  private static final String DEFAULT_BUTTON = "envops:dashboard:view";
  private static final int VERIFICATION_CODE_EXPIRE_SECONDS = 300;
  private static final Pattern PHONE_PATTERN = Pattern.compile(
      "^[1](([3][0-9])|([4][01456789])|([5][012356789])|([6][2567])|([7][0-8])|([8][0-9])|([9][012356789]))[0-9]{8}$");
  private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");

  private final UserAuthMapper userAuthMapper;
  private final JwtTokenService jwtTokenService;
  private final Map<String, VerificationCodeSession> verificationCodeSessions = new ConcurrentHashMap<>();

  public AuthApplicationService(UserAuthMapper userAuthMapper, JwtTokenService jwtTokenService) {
    this.userAuthMapper = userAuthMapper;
    this.jwtTokenService = jwtTokenService;
  }

  public LoginToken login(LoginCommand command) {
    if (command == null || !StringUtils.hasText(command.userName()) || !StringUtils.hasText(command.password())) {
      throw new IllegalArgumentException("userName and password are required");
    }

    UserAuthRow user = userAuthMapper.findByUserName(command.userName().trim());
    if (user == null || !Objects.equals(user.getPassword(), command.password()) || !isActiveUser(user)) {
      throw new UnauthorizedException("Invalid username or password");
    }

    userAuthMapper.recordUserLogin(user.getUserId(), "PASSWORD");
    return new LoginToken(jwtTokenService.createAccessToken(user.getUserName()));
  }

  public SendCodeResult sendCode(SendCodeCommand command) {
    String phone = normalizePhone(command == null ? null : command.phone());
    String verificationCode = buildVerificationCode(phone);

    verificationCodeSessions.put(
        phone,
        new VerificationCodeSession(
            verificationCode,
            LocalDateTime.now().plusSeconds(VERIFICATION_CODE_EXPIRE_SECONDS)));

    return new SendCodeResult(maskPhone(phone), VERIFICATION_CODE_EXPIRE_SECONDS);
  }

  public LoginToken codeLogin(CodeLoginCommand command) {
    String phone = normalizePhone(command == null ? null : command.phone());
    String code = normalizeCode(command == null ? null : command.code());
    VerificationCodeSession session = verificationCodeSessions.get(phone);

    if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
      verificationCodeSessions.remove(phone);
      throw new UnauthorizedException("Verification code expired or not requested");
    }

    if (!Objects.equals(session.code(), code)) {
      throw new UnauthorizedException("Invalid phone or verification code");
    }

    UserAuthRow user = userAuthMapper.findByPhone(phone);
    if (user == null || !isActiveUser(user)) {
      verificationCodeSessions.remove(phone);
      throw new UnauthorizedException("Invalid phone or verification code");
    }

    verificationCodeSessions.remove(phone);
    userAuthMapper.recordUserLogin(user.getUserId(), "PASSWORD_OTP");
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

  private boolean isActiveUser(UserAuthRow user) {
    return Objects.equals("ACTIVE", user.getStatus());
  }

  private String normalizePhone(String phone) {
    if (!StringUtils.hasText(phone)) {
      throw new IllegalArgumentException("phone is required");
    }

    String normalizedPhone = phone.trim();
    if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
      throw new IllegalArgumentException("phone is invalid");
    }

    return normalizedPhone;
  }

  private String normalizeCode(String code) {
    if (!StringUtils.hasText(code)) {
      throw new IllegalArgumentException("code is required");
    }

    String normalizedCode = code.trim();
    if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
      throw new IllegalArgumentException("code must be 6 digits");
    }

    return normalizedCode;
  }

  private String buildVerificationCode(String phone) {
    return phone.substring(phone.length() - 6);
  }

  private String maskPhone(String phone) {
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }

  public record LoginCommand(String userName, String password) {
  }

  public record SendCodeCommand(String phone) {
  }

  public record CodeLoginCommand(String phone, String code) {
  }

  public record LoginToken(String token) {
  }

  public record SendCodeResult(String maskedPhone, Integer expireSeconds) {
  }

  public record UserInfo(String userId, String userName, List<String> roles, List<String> buttons) {
  }

  private record VerificationCodeSession(String code, LocalDateTime expiresAt) {
  }
}

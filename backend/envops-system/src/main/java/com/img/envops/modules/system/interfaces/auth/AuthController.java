package com.img.envops.modules.system.interfaces.auth;

import com.img.envops.common.response.R;
import com.img.envops.modules.system.application.auth.AuthApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthApplicationService authApplicationService;

  public AuthController(AuthApplicationService authApplicationService) {
    this.authApplicationService = authApplicationService;
  }

  @PostMapping("/login")
  public R<AuthApplicationService.LoginToken> login(@RequestBody LoginRequest request) {
    return R.ok(authApplicationService.login(
        new AuthApplicationService.LoginCommand(request.userName(), request.password())));
  }

  @GetMapping("/getUserInfo")
  public R<AuthApplicationService.UserInfo> getUserInfo(Authentication authentication) {
    String principal = authentication == null ? null : authentication.getName();
    return R.ok(authApplicationService.getUserInfo(principal));
  }

  public record LoginRequest(String userName, String password) {
  }
}

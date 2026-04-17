package com.img.envops.framework.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtTokenService {
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ACCESS_TYPE = "access";
  private static final String REFRESH_TYPE = "refresh";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int MIN_TOKEN_SECRET_LENGTH = 32;
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final byte[] secretKey;
  private final long accessTokenTtlSeconds;
  private final long refreshTokenTtlSeconds;

  public JwtTokenService(
      @Value("${envops.security.token-secret}") String tokenSecret,
      @Value("${envops.security.access-token-ttl-seconds:1800}") long accessTokenTtlSeconds,
      @Value("${envops.security.refresh-token-ttl-seconds:604800}") long refreshTokenTtlSeconds) {
    if (accessTokenTtlSeconds <= 0 || refreshTokenTtlSeconds <= 0) {
      throw new IllegalArgumentException("token ttl must be positive");
    }

    this.secretKey = resolveSecretKey(tokenSecret);
    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
  }

  public String createAccessToken(String username) {
    return createToken(username, ACCESS_TYPE, accessTokenTtlSeconds);
  }

  public String createRefreshToken(String username) {
    return createToken(username, REFRESH_TYPE, refreshTokenTtlSeconds);
  }

  public String resolveAccessToken(HttpServletRequest request) {
    return resolveAccessToken(request.getHeader(HttpHeaders.AUTHORIZATION));
  }

  public String resolveAccessToken(String authorizationHeader) {
    if (!StringUtils.hasText(authorizationHeader)) {
      return null;
    }

    String headerValue = authorizationHeader.trim();
    if (!headerValue.startsWith(BEARER_PREFIX)) {
      return null;
    }

    String token = headerValue.substring(BEARER_PREFIX.length()).trim();
    return extractUsernameFromAccessToken(token) == null ? null : token;
  }

  public String extractUsernameFromAccessToken(String token) {
    return extractUsername(token, ACCESS_TYPE);
  }

  public String extractUsernameFromRefreshToken(String token) {
    return extractUsername(token, REFRESH_TYPE);
  }

  private String createToken(String username, String tokenType, long ttlSeconds) {
    if (!StringUtils.hasText(username)) {
      throw new IllegalArgumentException("username is required");
    }

    String resolvedUsername = username.trim();
    long expiresAt = Instant.now().plusSeconds(ttlSeconds).toEpochMilli();
    String typePart = encode(tokenType);
    String usernamePart = encode(resolvedUsername);
    String expiresAtPart = String.valueOf(expiresAt);
    String signingInput = String.join(".", typePart, usernamePart, expiresAtPart);

    return signingInput + "." + encode(sign(signingInput));
  }

  private String extractUsername(String token, String expectedType) {
    TokenPayload tokenPayload = parseToken(token);
    if (tokenPayload == null) {
      return null;
    }
    if (!expectedType.equals(tokenPayload.type())) {
      return null;
    }
    if (tokenPayload.expiresAt() < Instant.now().toEpochMilli()) {
      return null;
    }

    return tokenPayload.username();
  }

  private TokenPayload parseToken(String token) {
    if (!StringUtils.hasText(token)) {
      return null;
    }

    String[] parts = token.trim().split("\\.");
    if (parts.length != 4) {
      return null;
    }

    String signingInput = String.join(".", parts[0], parts[1], parts[2]);
    byte[] actualSignature = decodeBytes(parts[3]);
    if (actualSignature == null) {
      return null;
    }

    byte[] expectedSignature = sign(signingInput);
    if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
      return null;
    }

    String type = decodeToString(parts[0]);
    String username = decodeToString(parts[1]);
    Long expiresAt = parseLong(parts[2]);
    if (!StringUtils.hasText(type) || !StringUtils.hasText(username) || expiresAt == null || expiresAt <= 0) {
      return null;
    }

    return new TokenPayload(type, username.trim(), expiresAt);
  }

  private byte[] sign(String signingInput) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
      return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to sign token", exception);
    }
  }

  private byte[] resolveSecretKey(String tokenSecret) {
    if (!StringUtils.hasText(tokenSecret)) {
      throw new IllegalArgumentException("envops.security.token-secret must not be blank");
    }

    String resolvedTokenSecret = tokenSecret.trim();
    if (resolvedTokenSecret.length() < MIN_TOKEN_SECRET_LENGTH) {
      throw new IllegalArgumentException(
          "envops.security.token-secret must be at least " + MIN_TOKEN_SECRET_LENGTH + " characters");
    }

    return resolvedTokenSecret.getBytes(StandardCharsets.UTF_8);
  }

  private String encode(String value) {
    return URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private String encode(byte[] value) {
    return URL_ENCODER.encodeToString(value);
  }

  private String decodeToString(String value) {
    byte[] decoded = decodeBytes(value);
    return decoded == null ? null : new String(decoded, StandardCharsets.UTF_8);
  }

  private byte[] decodeBytes(String value) {
    try {
      return URL_DECODER.decode(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private Long parseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private record TokenPayload(String type, String username, long expiresAt) {
  }
}

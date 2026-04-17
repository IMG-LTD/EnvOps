package com.img.envops.modules.asset.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Component
public class CredentialSecretProtector {
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int MIN_SECRET_LENGTH = 32;
  private static final String PROTECTED_PREFIX = "protected:v1:";
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

  private final byte[] protectionKey;

  public CredentialSecretProtector(
      @Value("${envops.security.credential-protection-secret}") String protectionSecret) {
    this.protectionKey = resolveProtectionKey(protectionSecret);
  }

  public String protect(String rawSecret) {
    if (!StringUtils.hasText(rawSecret)) {
      throw new IllegalArgumentException("secret is required");
    }

    return PROTECTED_PREFIX + encode(sign(rawSecret.trim()));
  }

  private byte[] sign(String value) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(protectionKey, HMAC_ALGORITHM));
      return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to protect credential secret", exception);
    }
  }

  private byte[] resolveProtectionKey(String protectionSecret) {
    if (!StringUtils.hasText(protectionSecret)) {
      throw new IllegalArgumentException("envops.security.credential-protection-secret must not be blank");
    }

    String resolvedProtectionSecret = protectionSecret.trim();
    if (resolvedProtectionSecret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalArgumentException(
          "envops.security.credential-protection-secret must be at least " + MIN_SECRET_LENGTH + " characters");
    }

    return resolvedProtectionSecret.getBytes(StandardCharsets.UTF_8);
  }

  private String encode(byte[] value) {
    return URL_ENCODER.encodeToString(value);
  }
}

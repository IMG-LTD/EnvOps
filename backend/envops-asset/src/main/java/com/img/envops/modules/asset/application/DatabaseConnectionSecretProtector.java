package com.img.envops.modules.asset.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DatabaseConnectionSecretProtector {
  private static final String SECRET_PROPERTY = "envops.security.credential-protection-secret";
  private static final int MIN_SECRET_LENGTH = 32;
  private static final String SEALED_PREFIX = "sealed:v1:";
  private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  private static final int IV_LENGTH_BYTES = 12;
  private static final int TAG_LENGTH_BITS = 128;
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final byte[] aesKey;
  private final SecureRandom secureRandom = new SecureRandom();

  @Autowired
  public DatabaseConnectionSecretProtector(
      @Value("${envops.security.credential-protection-secret}") String protectionSecret) {
    this(protectionSecret, true);
  }

  DatabaseConnectionSecretProtector(String protectionSecret, boolean unused) {
    this.aesKey = deriveKey(protectionSecret);
  }

  public String seal(String rawSecret) {
    if (!StringUtils.hasText(rawSecret)) {
      throw new IllegalArgumentException("connectionPassword is required");
    }

    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] ciphertext = cipher.doFinal(rawSecret.trim().getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
      return SEALED_PREFIX + URL_ENCODER.encodeToString(payload);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to seal database connection password", exception);
    }
  }

  public String reveal(String sealedSecret) {
    if (!StringUtils.hasText(sealedSecret) || !sealedSecret.startsWith(SEALED_PREFIX)) {
      throw new IllegalArgumentException("connectionPassword is invalid");
    }

    try {
      byte[] payload = URL_DECODER.decode(sealedSecret.substring(SEALED_PREFIX.length()));
      if (payload.length <= IV_LENGTH_BYTES) {
        throw new IllegalArgumentException("connectionPassword is invalid");
      }

      byte[] iv = new byte[IV_LENGTH_BYTES];
      byte[] ciphertext = new byte[payload.length - IV_LENGTH_BYTES];
      System.arraycopy(payload, 0, iv, 0, iv.length);
      System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);

      Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to reveal database connection password", exception);
    }
  }

  private byte[] deriveKey(String protectionSecret) {
    if (!StringUtils.hasText(protectionSecret)) {
      throw new IllegalArgumentException(SECRET_PROPERTY + " must not be blank");
    }

    String resolvedProtectionSecret = protectionSecret.trim();
    if (resolvedProtectionSecret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalArgumentException(
          SECRET_PROPERTY + " must be at least " + MIN_SECRET_LENGTH + " characters");
    }

    try {
      return MessageDigest.getInstance("SHA-256").digest(resolvedProtectionSecret.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to derive database connection key", exception);
    }
  }
}

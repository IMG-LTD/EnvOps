package com.img.envops.modules.asset.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseConnectionSecretProtectorTest {
  private static final String PROTECTION_SECRET = "test-only-envops-credential-protection-secret-12345";

  @Test
  void sealAndRevealRoundTripUsesReversibleCiphertext() {
    DatabaseConnectionSecretProtector protector = new DatabaseConnectionSecretProtector(PROTECTION_SECRET, true);

    String sealed = protector.seal("Orders@123456");

    assertThat(sealed)
        .startsWith("sealed:v1:")
        .isNotEqualTo("Orders@123456");
    assertThat(protector.reveal(sealed)).isEqualTo("Orders@123456");
  }

  @Test
  void sealUsesRandomIvSoSamePasswordProducesDifferentCiphertexts() {
    DatabaseConnectionSecretProtector protector = new DatabaseConnectionSecretProtector(PROTECTION_SECRET, true);

    String first = protector.seal("Orders@123456");
    String second = protector.seal("Orders@123456");

    assertThat(first).startsWith("sealed:v1:");
    assertThat(second).startsWith("sealed:v1:");
    assertThat(first).isNotEqualTo(second);
    assertThat(protector.reveal(first)).isEqualTo("Orders@123456");
    assertThat(protector.reveal(second)).isEqualTo("Orders@123456");
  }

  @Test
  void revealRejectsInvalidPrefix() {
    DatabaseConnectionSecretProtector protector = new DatabaseConnectionSecretProtector(PROTECTION_SECRET, true);

    assertThatThrownBy(() -> protector.reveal("protected:v1:not-supported"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("connectionPassword is invalid");
  }
}

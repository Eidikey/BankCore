package com.bankcore.auth.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher hasher = new RefreshTokenHasher();

    @Test
    void hashesDeterministicallyToSixtyFourHexCharacters() {
        String token = "2f0d6f2e-6f0f-4d6b-9f2a-3b6c1d7e8f90";

        String first = hasher.hash(token);
        String second = hasher.hash(token);

        assertThat(first).hasSize(64).isEqualTo(second);
        assertThat(first).matches("[0-9a-f]{64}");
    }

    @Test
    void differentTokensProduceDifferentHashes() {
        assertThat(hasher.hash("token-a")).isNotEqualTo(hasher.hash("token-b"));
    }
}

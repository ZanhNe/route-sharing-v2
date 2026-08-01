package com.zanh.route_sharing.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenHasherTest {
    @Test
    void hashesAndMatchesWithoutStoringRawToken() {
        String hash = RefreshTokenHasher.sha256("secret-token");
        assertThat(hash).hasSize(64).doesNotContain("secret-token");
        assertThat(RefreshTokenHasher.matches("secret-token", hash)).isTrue();
        assertThat(RefreshTokenHasher.matches("other-token", hash)).isFalse();
    }
}

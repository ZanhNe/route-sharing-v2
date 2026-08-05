package com.zanh.route_sharing.domain.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TrangThaiYeuCauTest {

    @ParameterizedTest
    @EnumSource(value = TrangThaiYeuCau.class, names = {
            "PENDING", "ACCEPTED", "ON_BOARD", "DISPUTED"
    })
    void givenUnfinishedStatus_whenCheckingBlockingRule_thenNewRequestIsBlocked(
            TrangThaiYeuCau status) {
        assertThat(status.blocksNewRequest()).isTrue();
        assertThat(status.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = TrangThaiYeuCau.class, mode = EnumSource.Mode.EXCLUDE, names = {
            "PENDING", "ACCEPTED", "ON_BOARD", "DISPUTED"
    })
    void givenFinishedStatus_whenCheckingBlockingRule_thenNewRequestIsNotBlocked(
            TrangThaiYeuCau status) {
        assertThat(status.blocksNewRequest()).isFalse();
        assertThat(status.isTerminal()).isTrue();
    }
}

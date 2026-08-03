package com.zanh.route_sharing.dto.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageMetaTest {

    @Test
    void givenMiddlePage_whenCreatingMetadata_thenPageInformationIsCalculated() {
        // Arrange
        int page = 1;
        int size = 10;
        long totalElements = 25L;

        // Act
        PageMeta meta = PageMeta.of(page, size, totalElements);

        // Assert
        assertThat(meta.totalPages()).isEqualTo(3);
        assertThat(meta.first()).isFalse();
        assertThat(meta.last()).isFalse();
    }

    @Test
    void givenEmptyResult_whenCreatingMetadata_thenPageIsBothFirstAndLast() {
        // Arrange
        int page = 0;
        int size = 10;

        // Act
        PageMeta meta = PageMeta.of(page, size, 0L);

        // Assert
        assertThat(meta.totalPages()).isZero();
        assertThat(meta.first()).isTrue();
        assertThat(meta.last()).isTrue();
    }

    @Test
    void givenLastPage_whenCreatingMetadata_thenLastFlagIsTrue() {
        // Arrange
        int page = 2;
        int size = 10;

        // Act
        PageMeta meta = PageMeta.of(page, size, 25L);

        // Assert
        assertThat(meta.last()).isTrue();
    }

    @ParameterizedTest(name = "page={0}, size={1}, total={2}")
    @MethodSource("invalidArguments")
    void givenInvalidArguments_whenCreatingMetadata_thenIllegalArgumentIsThrown(
            int page,
            int size,
            long totalElements) {
        // Act & Assert
        assertThatThrownBy(() -> PageMeta.of(page, size, totalElements))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidArguments() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(-1, 10, 0L),
                org.junit.jupiter.params.provider.Arguments.of(0, 0, 0L),
                org.junit.jupiter.params.provider.Arguments.of(0, 10, -1L));
    }
}

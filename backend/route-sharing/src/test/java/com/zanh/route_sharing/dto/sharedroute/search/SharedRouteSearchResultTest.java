package com.zanh.route_sharing.dto.sharedroute.search;

import com.zanh.route_sharing.dto.response.PageMeta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedRouteSearchResultTest {

    @Test
    void givenNullItems_whenResultIsCreated_thenItemsAreNormalizedToEmptyList() {
        // Arrange
        PageMeta meta = PageMeta.of(0, 10, 0L);

        // Act
        SharedRouteSearchResult result = new SharedRouteSearchResult(null, meta);

        // Assert
        assertThat(result.items()).isEmpty();
    }

    @Test
    void givenMissingMetadata_whenResultIsCreated_thenIllegalArgumentIsThrown() {
        // Act & Assert
        assertThatThrownBy(() -> new SharedRouteSearchResult(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

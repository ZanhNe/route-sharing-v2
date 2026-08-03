package com.zanh.route_sharing.repository.sharedroute;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import com.zanh.route_sharing.repository.*;

import static com.zanh.route_sharing.testsupport.sharedroute.SharedRouteSearchRowBuilder.aSearchRow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedRouteSearchPageTest {

    @Test
    void givenNullRows_whenPageIsCreated_thenRowsAreNormalizedToEmptyList() {
        // Act
        SharedRouteSearchPage page = new SharedRouteSearchPage(null, 0L);

        // Assert
        assertThat(page.rows()).isEmpty();
    }

    @Test
    void givenMutableRows_whenPageIsCreated_thenRowsAreDefensivelyCopied() {
        // Arrange
        List<SharedRouteSearchRow> source = new ArrayList<>();
        source.add(aSearchRow().build());

        // Act
        SharedRouteSearchPage page = new SharedRouteSearchPage(source, 1L);
        source.clear();

        // Assert
        assertThat(page.rows()).hasSize(1);
        assertThatThrownBy(() -> page.rows().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void givenNegativeTotalElements_whenPageIsCreated_thenIllegalArgumentIsThrown() {
        // Act & Assert
        assertThatThrownBy(() -> new SharedRouteSearchPage(List.of(), -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

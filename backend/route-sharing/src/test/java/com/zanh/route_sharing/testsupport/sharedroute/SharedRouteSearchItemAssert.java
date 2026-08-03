package com.zanh.route_sharing.testsupport.sharedroute;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.domain.enums.LoaiGhepTuyen;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchItemResponse;
import org.assertj.core.api.AbstractAssert;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public final class SharedRouteSearchItemAssert
        extends AbstractAssert<SharedRouteSearchItemAssert, SharedRouteSearchItemResponse> {

    private SharedRouteSearchItemAssert(SharedRouteSearchItemResponse actual) {
        super(actual, SharedRouteSearchItemAssert.class);
    }

    public static SharedRouteSearchItemAssert assertThatSearchItem(
            SharedRouteSearchItemResponse actual) {
        return new SharedRouteSearchItemAssert(actual);
    }

    public SharedRouteSearchItemAssert hasMatchType(LoaiGhepTuyen expected) {
        isNotNull();
        assertThat(actual.matchType()).isEqualTo(expected);
        return this;
    }

    public SharedRouteSearchItemAssert hasDropoffType(LoaiDiemTha expected) {
        isNotNull();
        assertThat(actual.dropoffType()).isEqualTo(expected);
        return this;
    }

    public SharedRouteSearchItemAssert hasProposedDropoffAddress(String expected) {
        isNotNull();
        assertThat(actual.proposedDropoff().address()).isEqualTo(expected);
        return this;
    }

    public SharedRouteSearchItemAssert hasProposedDropoffWithoutAddress() {
        isNotNull();
        assertThat(actual.proposedDropoff().address()).isNull();
        return this;
    }

    public SharedRouteSearchItemAssert hasPickupDeviation(String expected) {
        isNotNull();
        assertThat(actual.pickupDeviationMeters())
                .isEqualByComparingTo(new BigDecimal(expected));
        return this;
    }

    public SharedRouteSearchItemAssert hasDestinationDeviation(String expected) {
        isNotNull();
        assertThat(actual.destinationDeviationMeters())
                .isEqualByComparingTo(new BigDecimal(expected));
        return this;
    }

    public SharedRouteSearchItemAssert hasSharedSegment(String expected) {
        isNotNull();
        assertThat(actual.sharedSegmentMeters())
                .isEqualByComparingTo(new BigDecimal(expected));
        return this;
    }
}

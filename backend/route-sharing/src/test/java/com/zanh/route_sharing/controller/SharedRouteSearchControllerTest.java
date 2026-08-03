package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.sharedroute.search.SearchSharedRoutesRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchResult;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRouteSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com.zanh.route_sharing.testsupport.sharedroute.CustomUserDetailsMother.activeUser;
import static com.zanh.route_sharing.testsupport.sharedroute.SearchSharedRoutesRequestBuilder.aSearchRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharedRouteSearchControllerTest {

    @Test
    void givenAuthenticatedActor_whenSearching_thenActorIdIsForwardedAndOkIsReturned() {
        // Arrange
        SharedRouteSearchService searchService = mock(SharedRouteSearchService.class);
        SharedRouteSearchController sut = new SharedRouteSearchController(searchService);
        CustomUserDetails principal = activeUser(
                9L,
                SharedRouteSearchController.SEARCH_PERMISSION);
        SearchSharedRoutesRequest request = aSearchRequest().build();
        SharedRouteSearchResult result = new SharedRouteSearchResult(
                List.of(),
                PageMeta.of(0, 10, 0L));

        when(searchService.search(9L, request, 0, 10)).thenReturn(result);

        // Act
        var response = sut.search(principal, request, 0, 10);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(searchService).search(9L, request, 0, 10);
    }
}

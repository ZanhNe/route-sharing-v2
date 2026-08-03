package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.sharedroute.search.SearchSharedRoutesRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchItemResponse;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchResult;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRouteSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shared-routes")
@RequiredArgsConstructor
@Validated
public class SharedRouteSearchController {

        private final SharedRouteSearchService searchService;

        @PostMapping("/search")
        @PreAuthorize("hasAuthority('SEARCH_SHARED_ROUTE')")
        public ResponseEntity<ApiResponse<List<SharedRouteSearchItemResponse>>> search(
                        @AuthenticationPrincipal CustomUserDetails principal,
                        @Valid @RequestBody SearchSharedRoutesRequest request,
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {

                SharedRouteSearchResult result = searchService.search(
                                principal.getId(),
                                request,
                                page,
                                size);

                ApiResponse<List<SharedRouteSearchItemResponse>> body = ApiResponse.of(
                                HttpStatus.OK.value(),
                                result.items(),
                                "Tìm lộ trình chia sẻ thành công.",
                                result.meta());

                return ResponseEntity.ok(body);
        }
}

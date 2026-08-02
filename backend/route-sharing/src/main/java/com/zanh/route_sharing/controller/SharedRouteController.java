package com.zanh.route_sharing.controller;

import com.zanh.route_sharing.dto.response.ApiResponse;
import com.zanh.route_sharing.dto.sharedroute.CreateSharedRouteRequest;
import com.zanh.route_sharing.dto.sharedroute.SharedRouteResponse;
import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.SharedRouteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/shared-routes")
@RequiredArgsConstructor
public class SharedRouteController {
    private final SharedRouteService sharedRouteService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_SHARED_ROUTE')")
    public ResponseEntity<ApiResponse<SharedRouteResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateSharedRouteRequest request) {
        SharedRouteResponse data = sharedRouteService.createSharedRoute(
                principal.getId(),
                request);

        URI location = URI.create(
                "/api/v1/shared-routes/" + data.id());

        ApiResponse<SharedRouteResponse> body = ApiResponse.success(
                HttpStatus.CREATED.value(),
                data,
                "Tạo lộ trình chia sẻ thành công.");

        return ResponseEntity
                .created(location)
                .body(body);
    }
}

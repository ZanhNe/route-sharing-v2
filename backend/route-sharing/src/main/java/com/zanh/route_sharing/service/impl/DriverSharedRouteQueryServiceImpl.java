package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.TrangThaiLoTrinh;
import com.zanh.route_sharing.dto.sharedroute.driverquery.DriverSharedRouteDetailResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.sharedroute.driverquery.DriverSharedRouteQueryRepository;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRoutePageSnapshot;
import com.zanh.route_sharing.repository.sharedroute.driverquery.model.DriverSharedRouteQueryCriteria;
import com.zanh.route_sharing.service.DriverSharedRouteQueryService;
import com.zanh.route_sharing.service.sharedroute.driverquery.DriverSharedRouteResponseMapper;
import com.zanh.route_sharing.service.sharedroute.driverquery.model.DriverSharedRoutePageResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DriverSharedRouteQueryServiceImpl implements DriverSharedRouteQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final DriverSharedRouteQueryRepository repository;
    private final DriverSharedRouteResponseMapper responseMapper;

    public DriverSharedRouteQueryServiceImpl(
            DriverSharedRouteQueryRepository repository,
            DriverSharedRouteResponseMapper responseMapper) {
        this.repository = repository;
        this.responseMapper = responseMapper;
    }

    @Override
    public DriverSharedRoutePageResult listOwnRoutes(
            Long actorUserId,
            TrangThaiLoTrinh status,
            int page,
            int size) {
        requirePositive(actorUserId, "actorUserId");
        requirePage(page, size);

        DriverSharedRoutePageSnapshot snapshot = repository.findPage(
                new DriverSharedRouteQueryCriteria(actorUserId, status, page, size));
        return responseMapper.toPage(snapshot);
    }

    @Override
    public DriverSharedRouteDetailResponse getOwnRouteDetail(Long actorUserId, Long routeId) {
        requirePositive(actorUserId, "actorUserId");
        requirePositive(routeId, "routeId");

        return repository.findDetail(actorUserId, routeId)
                .map(responseMapper::toDetail)
                .orElseThrow(DriverSharedRouteQueryServiceImpl::sharedRouteNotFound);
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw invalidQuery(fieldName + " phải là số dương.");
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0) {
            throw invalidQuery("page phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw invalidQuery("size phải nằm trong khoảng từ 1 đến 50.");
        }
    }

    private static BusinessException invalidQuery(String message) {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_SHARED_ROUTE_QUERY",
                message);
    }

    private static BusinessException sharedRouteNotFound() {
        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "SHARED_ROUTE_NOT_FOUND",
                "Không tìm thấy lộ trình chia sẻ.");
    }
}

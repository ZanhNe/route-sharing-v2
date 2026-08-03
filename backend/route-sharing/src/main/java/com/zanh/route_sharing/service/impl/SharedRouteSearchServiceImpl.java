package com.zanh.route_sharing.service.impl;

import com.zanh.route_sharing.domain.enums.LoaiDiemTha;
import com.zanh.route_sharing.dto.response.PageMeta;
import com.zanh.route_sharing.dto.sharedroute.search.SearchPointRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SearchPointResponse;
import com.zanh.route_sharing.dto.sharedroute.search.SearchSharedRoutesRequest;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteDriverResponse;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchItemResponse;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteSearchResult;
import com.zanh.route_sharing.dto.sharedroute.search.SharedRouteVehicleResponse;
import com.zanh.route_sharing.exception.BusinessException;
import com.zanh.route_sharing.repository.SharedRouteSearchContext;
import com.zanh.route_sharing.repository.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.SharedRouteSearchPage;
import com.zanh.route_sharing.repository.SharedRouteSearchRepository;
import com.zanh.route_sharing.repository.SharedRouteSearchRow;
import com.zanh.route_sharing.service.SharedRouteSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharedRouteSearchServiceImpl implements SharedRouteSearchService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SharedRouteSearchRepository repository;
    private final Clock clock;

    @Override
    public SharedRouteSearchResult search(
            Long actorUserId,
            SearchSharedRoutesRequest request,
            int page,
            int size) {

        requirePaging(page, size);
        requireActor(actorUserId);
        requireRequest(request);
        requireDistinctEndpoints(request.pickup(), request.destination());

        Instant now = clock.instant();
        if (!request.desiredDepartureTime().isAfter(now)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "DESIRED_DEPARTURE_NOT_IN_FUTURE",
                    "Thời gian khởi hành mong muốn phải nằm trong tương lai.");
        }

        LocalDate membershipDate = LocalDate.ofInstant(now, BUSINESS_ZONE);
        SharedRouteSearchContext context = repository.findSearchContext(
                actorUserId,
                request.schoolId(),
                membershipDate)
                .orElseThrow(() -> error(
                        HttpStatus.FORBIDDEN,
                        "SHARED_ROUTE_SEARCH_NOT_ELIGIBLE",
                        "Bạn không có hồ sơ thành viên hợp lệ tại trường đã chọn "
                                + "hoặc trường chưa có cấu hình tìm lộ trình."));

        Duration tolerance = Duration.ofMinutes(context.departureToleranceMinutes());
        Instant departureFrom = request.desiredDepartureTime().minus(tolerance);
        Instant departureTo = request.desiredDepartureTime().plus(tolerance);

        if (departureFrom.isBefore(now)) {
            departureFrom = now;
        }

        SharedRouteSearchCriteria criteria = new SharedRouteSearchCriteria(
                actorUserId,
                request.schoolId(),
                request.pickup().latitude(),
                request.pickup().longitude(),
                request.destination().latitude(),
                request.destination().longitude(),
                now,
                membershipDate,
                departureFrom,
                departureTo,
                context,
                page,
                size);

        SharedRouteSearchPage resultPage = repository.search(criteria);
        List<SharedRouteSearchItemResponse> items = resultPage.rows().stream()
                .map(row -> toResponse(row, request))
                .toList();

        return new SharedRouteSearchResult(
                items,
                PageMeta.of(page, size, resultPage.totalElements()));
    }

    private static SharedRouteSearchItemResponse toResponse(
            SharedRouteSearchRow row,
            SearchSharedRoutesRequest request) {

        String proposedDropoffAddress = row.dropoffType() == LoaiDiemTha.DIEM_DICH_CUOI_CUNG
                ? request.destination().address()
                : null;

        return new SharedRouteSearchItemResponse(
                row.sharedRouteId(),
                row.matchType(),
                row.dropoffType(),

                new SharedRouteDriverResponse(
                        row.driverId(),
                        row.driverName(),
                        row.driverAvatarUrl()),

                new SharedRouteVehicleResponse(
                        row.vehicleId(),
                        row.licensePlate(),
                        row.actualColor(),
                        row.brandName(),
                        row.modelName()),

                new SearchPointResponse(
                        row.originLatitude(),
                        row.originLongitude(),
                        row.originAddress()),

                new SearchPointResponse(
                        row.driverDestinationLatitude(),
                        row.driverDestinationLongitude(),
                        row.driverDestinationAddress()),

                new SearchPointResponse(
                        row.pickupProjectionLatitude(),
                        row.pickupProjectionLongitude(),
                        null),

                new SearchPointResponse(
                        row.proposedDropoffLatitude(),
                        row.proposedDropoffLongitude(),
                        proposedDropoffAddress),

                row.routeGeoJson(),
                row.expectedDepartureTime(),
                row.remainingSeats(),

                row.suggestedSupportPerKm(),
                row.pickupDeviationMeters(),
                row.destinationDeviationMeters(),
                row.sharedSegmentMeters());
    }

    private static void requireActor(Long actorUserId) {
        if (actorUserId == null || actorUserId <= 0) {
            throw error(
                    HttpStatus.UNAUTHORIZED,
                    "AUTHENTICATED_USER_REQUIRED",
                    "Không xác định được người dùng đang đăng nhập.");
        }
    }

    private static void requireRequest(SearchSharedRoutesRequest request) {
        if (request == null
                || request.schoolId() == null
                || request.pickup() == null
                || request.destination() == null
                || request.desiredDepartureTime() == null) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SEARCH_CRITERIA",
                    "Tiêu chí tìm lộ trình chưa đầy đủ.");
        }
    }

    private static void requireDistinctEndpoints(
            SearchPointRequest pickup,
            SearchPointRequest destination) {

        boolean sameLatitude = equalDecimal(pickup.latitude(), destination.latitude());
        boolean sameLongitude = equalDecimal(pickup.longitude(), destination.longitude());

        if (sameLatitude && sameLongitude) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SEARCH_ENDPOINTS",
                    "Điểm đón và điểm đến phải khác nhau.");
        }
    }

    private static boolean equalDecimal(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private static void requirePaging(int page, int size) {
        if (page < 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAGE",
                    "page phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > 50) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PAGE_SIZE",
                    "size phải nằm trong khoảng từ 1 đến 50.");
        }
    }

    private static BusinessException error(
            HttpStatus status,
            String code,
            String message) {
        return new BusinessException(status, code, message);
    }
}

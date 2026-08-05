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
import com.zanh.route_sharing.security.AuthenticatedPrincipalValidator;
import com.zanh.route_sharing.repository.sharedroute.search.SharedRouteSearchRepository;
import com.zanh.route_sharing.repository.sharedroute.common.model.SharedRouteMatchingContext;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchPage;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchRow;
import com.zanh.route_sharing.service.SharedRouteSearchService;
import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                AuthenticatedPrincipalValidator.requireUserId(actorUserId);
                requireRequest(request);
                requireDistinctEndpoints(request.pickup(), request.destination());

                Instant now = clock.instant();
                Instant desiredDepartureTime = request.desiredDepartureTime();
                if (!desiredDepartureTime.isAfter(now)) {
                        throw error(
                                        HttpStatus.BAD_REQUEST,
                                        "DESIRED_DEPARTURE_NOT_IN_FUTURE",
                                        "Thời gian khởi hành mong muốn phải nằm trong tương lai.");
                }

                LocalDate requestedTravelDate = LocalDate.ofInstant(
                                desiredDepartureTime,
                                BUSINESS_ZONE);

                SharedRouteMatchingContext context = repository.findSearchContext(
                                actorUserId,
                                request.schoolId(),
                                requestedTravelDate)
                                .orElseThrow(() -> error(
                                                HttpStatus.FORBIDDEN,
                                                "SHARED_ROUTE_SEARCH_NOT_ELIGIBLE",
                                                "Bạn không có hồ sơ thành viên hợp lệ tại trường đã chọn "
                                                                + "trong ngày dự kiến đi hoặc trường chưa có cấu hình tìm lộ trình."));

                Duration tolerance = Duration.ofMinutes(context.departureToleranceMinutes());
                Instant departureFrom = desiredDepartureTime.minus(tolerance);
                Instant departureTo = desiredDepartureTime.plus(tolerance);

                // Không bao giờ tìm route đã ở trong quá khứ.
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
                                requestedTravelDate,
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

        private static void requireRequest(SearchSharedRoutesRequest request) {
                if (request == null
                                || request.schoolId() == null
                                || request.schoolId() <= 0
                                || request.desiredDepartureTime() == null
                                || !isValidPoint(request.pickup())
                                || !isValidPoint(request.destination())) {
                        throw error(
                                        HttpStatus.BAD_REQUEST,
                                        "INVALID_SEARCH_CRITERIA",
                                        "Tiêu chí tìm lộ trình chưa đầy đủ hoặc không hợp lệ.");
                }
        }

        private static boolean isValidPoint(SearchPointRequest point) {
                if (point == null
                                || point.latitude() == null
                                || point.longitude() == null
                                || point.address() == null
                                || point.address().isBlank()
                                || point.address().length() > 500) {
                        return false;
                }

                return Wgs84Coordinates.isValid(point.latitude(), point.longitude());
        }

        private static void requireDistinctEndpoints(
                        SearchPointRequest pickup,
                        SearchPointRequest destination) {

                if (Wgs84Coordinates.same(
                                pickup.latitude(),
                                pickup.longitude(),
                                destination.latitude(),
                                destination.longitude())) {
                        throw error(
                                        HttpStatus.BAD_REQUEST,
                                        "INVALID_SEARCH_ENDPOINTS",
                                        "Điểm đón và điểm đến phải khác nhau.");
                }
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

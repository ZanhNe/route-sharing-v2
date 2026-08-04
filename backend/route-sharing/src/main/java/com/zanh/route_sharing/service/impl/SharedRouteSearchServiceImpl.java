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
import com.zanh.route_sharing.repository.sharedroute.search.SharedRouteSearchRepository;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchContext;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchCriteria;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchPage;
import com.zanh.route_sharing.repository.sharedroute.search.model.SharedRouteSearchRow;
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

        private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
        private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
        private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
        private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

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
                Instant desiredDepartureTime = request.desiredDepartureTime();
                if (!desiredDepartureTime.isAfter(now)) {
                        throw error(
                                        HttpStatus.BAD_REQUEST,
                                        "DESIRED_DEPARTURE_NOT_IN_FUTURE",
                                        "Thời gian khởi hành mong muốn phải nằm trong tương lai.");
                }

                /*
                 * Membership được kiểm tra theo ngày hành khách dự kiến đi, không phải
                 * ngày bấm nút tìm kiếm. Repository tiếp tục tái kiểm tra actor và tài xế
                 * theo ngày khởi hành thực tế của từng route candidate để xử lý đúng cửa
                 * sổ thời gian có thể đi qua nửa đêm.
                 */
                LocalDate requestedTravelDate = LocalDate.ofInstant(
                                desiredDepartureTime,
                                BUSINESS_ZONE);

                SharedRouteSearchContext context = repository.findSearchContext(
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

                /*
                 * Điểm thả trung gian là projection do PostGIS tính nên chưa có địa chỉ
                 * đáng tin cậy. Không được lấy địa chỉ destination của hành khách gắn vào
                 * một tọa độ khác. Với CUNG_DIEM_DEN, proposedDropoff chính là destination
                 * hành khách nên giữ được địa chỉ từ request.
                 */
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

                return isBetween(point.latitude(), MIN_LATITUDE, MAX_LATITUDE)
                                && isBetween(point.longitude(), MIN_LONGITUDE, MAX_LONGITUDE);
        }

        private static boolean isBetween(
                        BigDecimal value,
                        BigDecimal minimum,
                        BigDecimal maximum) {
                return value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
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

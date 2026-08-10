package com.zanh.route_sharing.service.tripformation;

import com.zanh.route_sharing.utils.spatial.Wgs84Coordinates;
import com.zanh.route_sharing.domain.enums.LoaiDiemDung;
import com.zanh.route_sharing.service.tripformation.model.PlannedTripStop;
import com.zanh.route_sharing.service.tripformation.model.TripFormationBookingSnapshot;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class TripStopOrderingPolicy {

    private static final double INDEX_EPSILON = 1.0e-12d;

    public List<PlannedTripStop> order(
            LineString originalRoute,
            Point driverOrigin,
            String driverOriginAddress,
            Point driverDestination,
            String driverDestinationAddress,
            List<TripFormationBookingSnapshot> acceptedBookings) {
        requireLineString(originalRoute);
        requirePoint(driverOrigin, "driverOrigin");
        requirePoint(driverDestination, "driverDestination");
        Objects.requireNonNull(driverOriginAddress, "driverOriginAddress không được trống");
        Objects.requireNonNull(driverDestinationAddress, "driverDestinationAddress không được trống");
        if (acceptedBookings == null || acceptedBookings.isEmpty()) {
            throw new IllegalArgumentException("Phải có ít nhất một booking ACCEPTED để sắp điểm dừng");
        }

        LengthIndexedLine indexed = new LengthIndexedLine(originalRoute);
        double startIndex = indexed.getStartIndex();
        double endIndex = indexed.getEndIndex();
        List<Candidate> candidates = new ArrayList<>(2 + acceptedBookings.size() * 2);
        candidates.add(new Candidate(
                LoaiDiemDung.DRIVER_START,
                null,
                copy(driverOrigin),
                driverOriginAddress,
                startIndex));

        for (TripFormationBookingSnapshot booking : acceptedBookings) {
            Objects.requireNonNull(booking, "booking không được trống");
            requirePoint(booking.pickup(), "pickup");
            requirePoint(booking.dropoff(), "dropoff");
            double pickupIndex = indexed.project(booking.pickup().getCoordinate());
            double dropoffIndex = indexed.project(booking.dropoff().getCoordinate());
            if (pickupIndex > dropoffIndex + INDEX_EPSILON) {
                throw new IllegalArgumentException(
                        "Pickup phải nằm trước hoặc cùng vị trí với dropoff theo chiều tuyến tài xế");
            }
            candidates.add(new Candidate(
                    LoaiDiemDung.PICKUP,
                    booking.rideRequestId(),
                    copy(booking.pickup()),
                    booking.pickupAddress(),
                    pickupIndex));
            candidates.add(new Candidate(
                    LoaiDiemDung.DROPOFF,
                    booking.rideRequestId(),
                    copy(booking.dropoff()),
                    booking.dropoffAddress(),
                    dropoffIndex));
        }

        candidates.add(new Candidate(
                LoaiDiemDung.DRIVER_END,
                null,
                copy(driverDestination),
                driverDestinationAddress,
                endIndex));

        candidates.sort(Comparator
                .comparingDouble(Candidate::routeIndex)
                .thenComparingInt(candidate -> rolePriority(candidate.type()))
                .thenComparing(candidate -> candidate.rideRequestId() == null
                        ? Long.MIN_VALUE
                        : candidate.rideRequestId()));

        List<PlannedTripStop> ordered = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            Candidate candidate = candidates.get(index);
            ordered.add(new PlannedTripStop(
                    index + 1,
                    candidate.type(),
                    candidate.rideRequestId(),
                    candidate.point(),
                    candidate.address(),
                    candidate.routeIndex()));
        }
        if (ordered.get(0).type() != LoaiDiemDung.DRIVER_START
                || ordered.get(ordered.size() - 1).type() != LoaiDiemDung.DRIVER_END) {
            throw new IllegalArgumentException("DRIVER_START/DRIVER_END không nằm đúng biên của tuyến");
        }
        return List.copyOf(ordered);
    }

    private static int rolePriority(LoaiDiemDung type) {
        return switch (type) {
            case DRIVER_START -> 0;
            case PICKUP -> 1;
            case DROPOFF -> 2;
            case DRIVER_END -> 3;
        };
    }

    private static void requirePoint(Point point, String field) {
        if (point == null || point.isEmpty() || point.getSRID() != Wgs84Coordinates.SRID) {
            throw new IllegalArgumentException(field + " phải là Point SRID 4326 hợp lệ");
        }
        Coordinate coordinate = point.getCoordinate();
        if (coordinate == null || !Double.isFinite(coordinate.x) || !Double.isFinite(coordinate.y)) {
            throw new IllegalArgumentException(field + " chứa tọa độ không hợp lệ");
        }
    }

    private static void requireLineString(LineString lineString) {
        if (lineString == null
                || lineString.isEmpty()
                || lineString.getSRID() != Wgs84Coordinates.SRID
                || lineString.getNumPoints() < 2
                || lineString.getLength() <= 0.0d) {
            throw new IllegalArgumentException("Tuyến gốc phải là LineString SRID 4326 hợp lệ");
        }
    }

    private static Point copy(Point source) {
        Point copy = (Point) source.copy();
        copy.setSRID(Wgs84Coordinates.SRID);
        return copy;
    }

    private record Candidate(
            LoaiDiemDung type,
            Long rideRequestId,
            Point point,
            String address,
            double routeIndex) {
    }
}

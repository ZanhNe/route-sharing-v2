package com.zanh.route_sharing.utils.spatial;

import java.math.BigDecimal;

/**
 * Provider-neutral WGS84 range predicates.
 *
 * <p>The methods do not throw application exceptions. Each boundary keeps
 * responsibility for translating an invalid coordinate to its own error
 * contract (for example HTTP 400 for client input and HTTP 502 for provider
 * output).</p>
 */
public final class Wgs84Coordinates {

    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    private Wgs84Coordinates() {
    }

    public static boolean isValid(BigDecimal latitude, BigDecimal longitude) {
        return isValidLatitude(latitude) && isValidLongitude(longitude);
    }

    public static boolean isValidLatitude(BigDecimal latitude) {
        return between(latitude, MIN_LATITUDE, MAX_LATITUDE);
    }

    public static boolean isValidLongitude(BigDecimal longitude) {
        return between(longitude, MIN_LONGITUDE, MAX_LONGITUDE);
    }

    public static boolean isValidLongitudeLatitude(double longitude, double latitude) {
        return Double.isFinite(longitude)
                && Double.isFinite(latitude)
                && longitude >= -180.0d
                && longitude <= 180.0d
                && latitude >= -90.0d
                && latitude <= 90.0d;
    }

    public static boolean same(
            BigDecimal firstLatitude,
            BigDecimal firstLongitude,
            BigDecimal secondLatitude,
            BigDecimal secondLongitude) {
        return equal(firstLatitude, secondLatitude)
                && equal(firstLongitude, secondLongitude);
    }

    private static boolean between(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        return value != null
                && value.compareTo(minimum) >= 0
                && value.compareTo(maximum) <= 0;
    }

    private static boolean equal(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }
}

package com.zanh.route_sharing.service.realtime;

import com.zanh.route_sharing.security.CustomUserDetails;
import com.zanh.route_sharing.service.triplocation.TripLocationObserverAccess;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TripLocationSubscriptionAuthorizer {

    private static final String VIEW_OWN_TRIP = "VIEW_OWN_TRIP";
    private static final String TRIP_PREFIX = "/user/queue/trips/";
    private static final Pattern EXACT_LOCATION_DESTINATION = Pattern.compile(
            "^/user/queue/trips/([1-9][0-9]*)/location$");

    private final TripLocationObserverAccess observerAccess;

    public TripLocationSubscriptionAuthorizer(TripLocationObserverAccess observerAccess) {
        this.observerAccess = observerAccess;
    }

    public boolean isLocationDestinationCandidate(String destination) {
        return destination != null
                && destination.startsWith(TRIP_PREFIX)
                && destination.contains("/location");
    }

    public void authorize(Authentication authentication, String destination) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw denied();
        }
        Matcher matcher = EXACT_LOCATION_DESTINATION.matcher(destination == null ? "" : destination);
        if (!matcher.matches()) {
            throw denied();
        }
        if (authentication.getAuthorities().stream().noneMatch(a -> VIEW_OWN_TRIP.equals(a.getAuthority()))) {
            throw denied();
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw denied();
        }
        Long tripId;
        try {
            tripId = Long.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw denied();
        }
        try {
            if (!observerAccess.canSubscribe(principal.getId(), tripId)) {
                throw denied();
            }
        } catch (BadCredentialsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BadCredentialsException("Không thể subscribe vị trí chuyến đi", exception);
        }
    }

    private static BadCredentialsException denied() {
        return new BadCredentialsException("Không thể subscribe vị trí chuyến đi");
    }
}

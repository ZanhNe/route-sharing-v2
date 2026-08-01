package com.zanh.route_sharing.security;

import java.time.Instant;

public record IssuedToken(String value, String jwtId, Instant expiresAt) {
}

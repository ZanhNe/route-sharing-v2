package com.zanh.route_sharing.dto.auth.session;

import com.zanh.route_sharing.security.IssuedToken;
import com.zanh.route_sharing.security.TokenPair;
import com.zanh.route_sharing.service.iam.auth.VerifiedAccountCredential;

public record AuthSessionResponse(
        AuthAccountResponse account,
        AuthSessionContextResponse session,
        AuthTokenResponse token) {

    public static AuthSessionResponse onboarding(
            VerifiedAccountCredential account,
            IssuedToken token,
            String nextAction) {
        return new AuthSessionResponse(
                AuthAccountResponse.from(account),
                new AuthSessionContextResponse("ONBOARDING", nextAction),
                AuthTokenResponse.onboarding(token));
    }

    public static AuthSessionResponse full(VerifiedAccountCredential account, TokenPair pair) {
        return new AuthSessionResponse(
                AuthAccountResponse.from(account),
                new AuthSessionContextResponse("FULL", "ENTER_APP"),
                AuthTokenResponse.full(pair));
    }
}

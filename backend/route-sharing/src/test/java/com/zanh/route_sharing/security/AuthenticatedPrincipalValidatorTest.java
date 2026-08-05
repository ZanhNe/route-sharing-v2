package com.zanh.route_sharing.security;

import com.zanh.route_sharing.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedPrincipalValidatorTest {

    @Test
    void givenPositiveUserId_whenValidating_thenAccepted() {
        assertThatCode(() -> AuthenticatedPrincipalValidator.requireUserId(1L))
                .doesNotThrowAnyException();
    }

    @Test
    void givenMissingOrNonPositiveUserId_whenValidating_thenUnauthorizedIsReturned() {
        assertThatThrownBy(() -> AuthenticatedPrincipalValidator.requireUserId(0L))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException businessError = (BusinessException) error;
                    assertThat(businessError.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(businessError.getCode()).isEqualTo("AUTHENTICATED_USER_REQUIRED");
                });
    }
}

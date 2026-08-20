package com.zanh.route_sharing.integration.mail;

public class VerificationEmailDeliveryException extends RuntimeException {
    private final VerificationEmailFailureCategory category;

    public VerificationEmailDeliveryException(VerificationEmailFailureCategory category, String message) {
        super(message);
        this.category = category;
    }

    public VerificationEmailDeliveryException(
            VerificationEmailFailureCategory category,
            String message,
            Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public VerificationEmailFailureCategory getCategory() {
        return category;
    }
}

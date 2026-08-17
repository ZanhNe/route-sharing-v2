package com.zanh.route_sharing.security.dropoff;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Locale;

@Component
public class SecureRandomDropoffCodeGenerator implements DropoffCodeGenerator {
    private static final int CODE_SPACE = 1_000_000;
    private final SecureRandom secureRandom;
    public SecureRandomDropoffCodeGenerator() { this(new SecureRandom()); }
    SecureRandomDropoffCodeGenerator(SecureRandom secureRandom) { this.secureRandom = secureRandom; }
    @Override public String generate() { return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(CODE_SPACE)); }
}

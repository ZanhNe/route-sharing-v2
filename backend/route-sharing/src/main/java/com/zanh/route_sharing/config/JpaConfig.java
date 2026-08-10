package com.zanh.route_sharing.config;

import com.zanh.route_sharing.utils.time.TimePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Optional;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "jpaDateTimeProvider", modifyOnCreate = true)
public class JpaConfig {
    @Bean
    AuditorAware<Long> auditorAware() {
        return new AuditorAwareImpl();
    }

    @Bean
    DateTimeProvider jpaDateTimeProvider(Clock applicationClock) {
        return () -> Optional.of(TimePolicy.now(applicationClock));
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}

package com.lklass.global.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lklass.policy")
public record EnrollmentPolicyProperties(
        Duration pendingPaymentTtl,
        Duration cancellationPeriod
) {
}

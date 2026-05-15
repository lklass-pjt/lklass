package com.lklass.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lklass.scheduler")
public record SchedulerProperties(
        String courseStatusCron,
        String pendingExpirationCron
) {
}

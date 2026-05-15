package com.lklass.global.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lklass.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenTtl
) {
}

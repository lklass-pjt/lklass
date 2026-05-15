package com.lklass.domain.auth.config;

import com.lklass.global.security.DomainSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class AuthSecurityConfigurer implements DomainSecurityConfigurer {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize) {
        authorize.requestMatchers("/api/auth/signup", "/api/auth/login").permitAll();
    }
}

package com.lklass.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PageableConfig {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            // 외부 API는 page=1부터 시작하고, 기본 조회는 최신 생성순으로 통일한다.
            resolver.setOneIndexedParameters(true);
            resolver.setMaxPageSize(MAX_SIZE);
            resolver.setFallbackPageable(PageRequest.of(
                    DEFAULT_PAGE,
                    DEFAULT_SIZE,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            ));
        };
    }
}

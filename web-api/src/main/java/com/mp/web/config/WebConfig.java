package com.mp.web.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${core.api.timeout:30000}")
    private int timeout;

    @Value("${core.api.key}")
    private String apiKey;

    @Value("${core.api.url}")
    private String coreApiUrl;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .connectTimeout(Duration.ofMillis(timeout))
                .readTimeout(Duration.ofMillis(timeout))
                .build();

        restTemplate.getInterceptors().add((request, body, execution) -> {
            // Only add API key for core-api requests
            if (request.getURI().toString().startsWith(coreApiUrl)) {
                request.getHeaders().set("X-API-Key", apiKey);
            }
            return execution.execute(request, body);
        });

        return restTemplate;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}

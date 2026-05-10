package com.mp.web.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    @Value("${core.api.timeout:30000}")
    private int timeout;

    @Value("${core.api.key}")
    private String apiKey;

    @Value("${core.api.url}")
    private String coreApiUrl;

    @Value("${bff.http.max-total:200}")
    private int httpMaxTotal;

    @Value("${bff.http.max-per-route:50}")
    private int httpMaxPerRoute;

    @Value("${bff.http.keep-alive-seconds:30}")
    private int keepAliveSeconds;

    /**
     * Pooled HTTP client. Replaces JDK HttpURLConnection (one TCP+TLS handshake
     * per request) with Apache HttpClient 5's connection pool, which keeps
     * connections to core-api warm and amortizes the BFF hop's overhead.
     */
    @Bean
    public CloseableHttpClient httpClient() {
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(timeout, TimeUnit.MILLISECONDS))
                .setSocketTimeout(Timeout.of(timeout, TimeUnit.MILLISECONDS))
                .setTimeToLive(Timeout.of(keepAliveSeconds, TimeUnit.SECONDS))
                .build();

        PoolingHttpClientConnectionManager pool = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(httpMaxTotal)
                .setMaxConnPerRoute(httpMaxPerRoute)
                .setDefaultConnectionConfig(connConfig)
                .build();

        RequestConfig reqConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.of(timeout, TimeUnit.MILLISECONDS))
                .setConnectionRequestTimeout(Timeout.of(timeout, TimeUnit.MILLISECONDS))
                .build();

        return HttpClients.custom()
                .setConnectionManager(pool)
                .setDefaultRequestConfig(reqConfig)
                .evictIdleConnections(Timeout.of(keepAliveSeconds, TimeUnit.SECONDS))
                .build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(Duration.ofMillis(timeout));

        RestTemplate restTemplate = builder
                .requestFactory(() -> factory)
                .connectTimeout(Duration.ofMillis(timeout))
                .readTimeout(Duration.ofMillis(timeout))
                .build();

        restTemplate.getInterceptors().add((request, body, execution) -> {
            if (request.getURI().toString().startsWith(coreApiUrl)) {
                request.getHeaders().set("X-API-Key", apiKey);
            }
            return execution.execute(request, body);
        });

        return restTemplate;
    }
}

package com.barlog.barlog_back.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** HTTP client for the Python pose-analysis microservice (barlog-analysis, FR-015). */
@Component
public class AnalysisClient {

    private final RestClient http;

    public AnalysisClient(@Value("${barlog.analysis.url:http://localhost:8001}") String baseUrl,
                          @Value("${barlog.analysis.timeout-ms:300000}") int timeoutMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(timeoutMs);
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(f).build();
    }

    /**
     * Returns the raw JSON result; throws on network errors or non-2xx responses.
     * exercise "AUTO" lets the service detect it; hint is used when detection is unsure.
     */
    public String analyze(String absoluteVideoPath, String exercise, String hint) {
        return http.post().uri("/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("videoPath", absoluteVideoPath, "exercise", exercise, "hint", hint))
                .retrieve()
                .body(String.class);
    }
}

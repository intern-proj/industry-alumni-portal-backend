package com.nsbm.api_gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/system")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SystemTelemetryController {

    private final WebClient webClient;
    private final ReactiveDiscoveryClient discoveryClient;

    private static final List<ServiceTarget> SERVICES = List.of(
            new ServiceTarget("api-gateway", "API Gateway & Routing", 8080, "http://localhost:8080/actuator/health"),
            new ServiceTarget("auth-service", "Auth & Identity (2FA/JWT)", 8081, "http://localhost:8081/actuator/health"),
            new ServiceTarget("user-service", "User Profile & Academic Config", 8082, "http://localhost:8082/actuator/health"),
            new ServiceTarget("vacancy-service", "Placement & Vacancies", 8087, "http://localhost:8087/actuator/health"),
            new ServiceTarget("application-service", "Candidate Application Pipeline", 8084, "http://localhost:8084/actuator/health"),
            new ServiceTarget("event-management-service", "Events, Venues & Speakers", 8085, "http://localhost:8085/actuator/health"),
            new ServiceTarget("event-participation-service", "Event Check-in & QR Attendance", 8086, "http://localhost:8086/actuator/health"),
            new ServiceTarget("certificate-service", "Digital Badges & Certificate Registry", 8083, "http://localhost:8083/actuator/health"),
            new ServiceTarget("platform-management-service", "Institutional Approvals & Verifications", 8088, "http://localhost:8088/actuator/health"),
            new ServiceTarget("audit-storage-service", "Immutable Audit & Security Storage", 8089, "http://localhost:8089/actuator/health"),
            new ServiceTarget("ai-service", "AI Vector Search & Flyer OCR", 8000, "http://localhost:8000/health")
    );

    @Autowired
    public SystemTelemetryController(@Autowired(required = false) ReactiveDiscoveryClient discoveryClient) {
        this.webClient = WebClient.builder().build();
        this.discoveryClient = discoveryClient;
    }

    @GetMapping("/telemetry")
    public Mono<ResponseEntity<Map<String, Object>>> getSystemTelemetry() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        long usedMemoryBytes = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemoryBytes = memoryBean.getHeapMemoryUsage().getMax();
        long memoryMB = Math.round(usedMemoryBytes / (1024.0 * 1024.0));
        long maxMemoryMB = Math.round(maxMemoryBytes / (1024.0 * 1024.0));

        long uptimeSec = runtimeBean.getUptime() / 1000;
        long days = uptimeSec / 86400;
        long hours = (uptimeSec % 86400) / 3600;
        long mins = (uptimeSec % 3600) / 60;
        String uptimeFormatted = (days > 0 ? days + "d " : "") + hours + "h " + mins + "m";

        return pingAllServices().collectList().map(serviceList -> {
            long upCount = serviceList.stream().filter(s -> "UP".equals(s.get("status"))).count();
            long downCount = serviceList.size() - upCount;
            double avgLatency = serviceList.stream()
                    .mapToLong(s -> (long) s.get("latencyMs"))
                    .average()
                    .orElse(0.0);

            Map<String, Object> telemetry = new LinkedHashMap<>();
            telemetry.put("status", downCount == 0 ? "UP" : "DEGRADED");
            telemetry.put("uptimeFormatted", uptimeFormatted);
            telemetry.put("uptimeSeconds", uptimeSec);
            telemetry.put("memoryMB", memoryMB);
            telemetry.put("maxMemoryMB", maxMemoryMB);
            telemetry.put("totalServices", serviceList.size());
            telemetry.put("upServices", upCount);
            telemetry.put("downServices", downCount);
            telemetry.put("avgLatencyMs", Math.round(avgLatency));
            telemetry.put("timestamp", Instant.now().toString());
            telemetry.put("services", serviceList);

            return ResponseEntity.ok(telemetry);
        });
    }

    @GetMapping("/mesh")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getMeshHealth() {
        return pingAllServices().collectList().map(ResponseEntity::ok);
    }

    private Flux<Map<String, Object>> pingAllServices() {
        return Flux.fromIterable(SERVICES).flatMap(this::pingService);
    }

    private Mono<Map<String, Object>> pingService(ServiceTarget target) {
        if ("api-gateway".equals(target.id())) {
            return pingUrl(target, "http://localhost:8080/actuator/health");
        }

        if (discoveryClient != null) {
            return discoveryClient.getInstances(target.id())
                    .next()
                    .flatMap(instance -> {
                        String base = instance.getUri().toString();
                        String healthUrl = "ai-service".equalsIgnoreCase(target.id()) 
                                ? base + "/health" 
                                : base + "/actuator/health";
                        return pingUrl(target, healthUrl);
                    })
                    .switchIfEmpty(Mono.defer(() -> pingUrl(target, target.url())));
        }

        return pingUrl(target, target.url());
    }

    private Mono<Map<String, Object>> pingUrl(ServiceTarget target, String pingUrl) {
        long start = System.currentTimeMillis();
        return webClient.get()
                .uri(pingUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(3000))
                .map(body -> {
                    long elapsed = System.currentTimeMillis() - start;
                    String status = "UP".equalsIgnoreCase(String.valueOf(body.get("status"))) ? "UP" : "DEGRADED";
                    Map<String, Object> res = new LinkedHashMap<>();
                    res.put("id", target.id());
                    res.put("name", target.name());
                    res.put("port", target.port());
                    res.put("status", status);
                    res.put("latency", elapsed + "ms");
                    res.put("latencyMs", elapsed);
                    res.put("details", body.get("components") != null ? body.get("components") : body);
                    res.put("timestamp", Instant.now().toString());
                    return res;
                })
                .onErrorResume(err -> {
                    long elapsed = System.currentTimeMillis() - start;
                    Map<String, Object> res = new LinkedHashMap<>();
                    res.put("id", target.id());
                    res.put("name", target.name());
                    res.put("port", target.port());
                    res.put("status", "DOWN");
                    res.put("latency", elapsed + "ms");
                    res.put("latencyMs", elapsed);
                    res.put("error", err.getMessage());
                    res.put("timestamp", Instant.now().toString());
                    return Mono.just(res);
                });
    }

    private record ServiceTarget(String id, String name, int port, String url) {}
}

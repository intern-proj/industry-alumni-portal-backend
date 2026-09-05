package com.nsbm.notification_service.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class DomainUrlResolver {

    private static final String DEFAULT_FRONTEND_URL = "https://wonderful-wave-0320abf00.3.azurestaticapps.net";
    private static final String DEFAULT_BACKEND_URL = "https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io";

    private final String frontendUrl;
    private final String backendUrl;

    public DomainUrlResolver(
            @Value("${app.frontend.url:" + DEFAULT_FRONTEND_URL + "}") String frontendUrl,
            @Value("${app.backend.url:${API_GATEWAY_URL:" + DEFAULT_BACKEND_URL + "}}") String backendUrl) {
        this.frontendUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? trimTrailingSlash(frontendUrl) : DEFAULT_FRONTEND_URL;
        this.backendUrl = (backendUrl != null && !backendUrl.isBlank()) ? trimTrailingSlash(backendUrl) : DEFAULT_BACKEND_URL;
        log.info("Initialized DomainUrlResolver with Frontend: '{}', Backend: '{}'", this.frontendUrl, this.backendUrl);
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Resolves a relative path or legacy URL into an active frontend link.
     */
    public String resolveFrontendUrl(String linkOrPath) {
        if (linkOrPath == null || linkOrPath.isBlank() || "#".equals(linkOrPath.trim())) {
            return frontendUrl;
        }
        String trimmed = linkOrPath.trim();

        // If it starts with localhost or placeholder NSBM portal domain
        if (trimmed.startsWith("http://localhost:5173") ||
            trimmed.startsWith("http://localhost:3000") ||
            trimmed.startsWith("http://127.0.0.1:5173") ||
            trimmed.startsWith("http://127.0.0.1:3000") ||
            trimmed.startsWith("https://portal.nsbm.ac.lk")) {
            return trimmed.replaceFirst("https?://[^/]+", frontendUrl);
        }

        // If it's a relative path
        if (trimmed.startsWith("/")) {
            return frontendUrl + trimmed;
        }

        return trimmed;
    }

    /**
     * Sanitizes any HTML content by replacing localhost and dead placeholder domains
     * with the dynamically configured active cloud endpoints.
     */
    public String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        String result = html;

        // Replace frontend localhost & dead domains
        result = result.replace("http://localhost:5173", frontendUrl)
                       .replace("http://localhost:3000", frontendUrl)
                       .replace("http://127.0.0.1:5173", frontendUrl)
                       .replace("http://127.0.0.1:3000", frontendUrl)
                       .replace("https://portal.nsbm.ac.lk", frontendUrl);

        // Replace backend API gateway localhost domains
        result = result.replace("http://localhost:8080", backendUrl)
                       .replace("http://127.0.0.1:8080", backendUrl);

        return result;
    }

    /**
     * Sanitizes plain text content by replacing localhost and dead placeholder domains.
     */
    public String sanitizeText(String text) {
        return sanitizeHtml(text);
    }
}

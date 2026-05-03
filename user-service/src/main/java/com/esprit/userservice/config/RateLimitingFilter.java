package com.esprit.userservice.config;

import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import com.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bucket4j rate-limiting filter.
 *
 * Each unique IP address gets its own token bucket:
 *   - Capacity : 20 requests
 *   - Refill   : 20 tokens every 1 minute
 *
 * When a client exceeds the limit the filter returns HTTP 429 Too Many Requests
 * without forwarding the request to any controller.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // One bucket per IP — created lazily on first request
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);

        if (bucket.tryConsume(1)) {
            // Token consumed — allow the request through
            filterChain.doFilter(request, response);
        } else {
            // Bucket empty — reject with 429
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Too Many Requests\","
              + "\"message\":\"Rate limit exceeded. Max 20 requests per minute.\","
              + "\"status\":429}"
            );
        }
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(
            20,                              // 20 tokens capacity
            Refill.greedy(20, Duration.ofMinutes(1))  // refill 20 per minute
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

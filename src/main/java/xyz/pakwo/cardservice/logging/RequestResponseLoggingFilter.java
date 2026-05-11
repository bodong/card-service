package xyz.pakwo.cardservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import xyz.pakwo.cardservice.util.SensitiveDataMasker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * @author sarwo.wibowo
 **/
@Component
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final int REQUEST_CACHE_LIMIT = 10 * 1024;   // 10 KB
    private static final int LOG_BODY_LIMIT = 10 * 1024;        // 10 KB

    private static final Set<String> VISIBLE_MEDIA_TYPES = Set.of(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
    );

    private final SensitiveDataMasker sensitiveDataMasker;

    public RequestResponseLoggingFilter(SensitiveDataMasker sensitiveDataMasker) {
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);
        MDC.put(CORRELATION_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            try {
                logRequestAndResponse(wrappedRequest, wrappedResponse, correlationId, durationMs);
            } finally {
                wrappedResponse.copyBodyToResponse();
                MDC.remove(CORRELATION_ID_KEY);
            }
        }
    }

    private void logRequestAndResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, String correlationId, long durationMs) {
        String requestBody = getRequestBody(request);
        String responseBody = getResponseBody(response);

        log.info(
                "HTTP transaction completed | correlationId={} | method={} | uri={} | query={} | status={} | durationMs={} | requestBody={} | responseBody={}",
                correlationId,
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                response.getStatus(),
                durationMs,
                requestBody,
                responseBody
        );
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (StringUtils.hasText(correlationId)) {
            return correlationId;
        }

        return UUID.randomUUID().toString();
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        if (!isVisibleContentType(request.getContentType())) {
            return "[unsupported content type]";
        }

        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        String body = toString(content, request.getCharacterEncoding());
        return sanitizeBody(body);
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        if (!isVisibleContentType(response.getContentType())) {
            return "[unsupported content type]";
        }

        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        String body = toString(content, response.getCharacterEncoding());
        return sanitizeBody(body);
    }

    private boolean isVisibleContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return true;
        }
        return VISIBLE_MEDIA_TYPES.stream().anyMatch(contentType::startsWith);
    }

    private String toString(byte[] content, String characterEncoding) {
        Charset charset = StringUtils.hasText(characterEncoding) ? Charset.forName(characterEncoding) : StandardCharsets.UTF_8;
        return new String(content, charset);
    }

    private String sanitizeBody(String body) {
        String masked = sensitiveDataMasker.maskJson(body);
        return truncate(masked, LOG_BODY_LIMIT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...[truncated]";
    }
}
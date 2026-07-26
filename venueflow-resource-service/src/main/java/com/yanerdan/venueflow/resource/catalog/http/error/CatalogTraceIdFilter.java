package com.yanerdan.venueflow.resource.catalog.http.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("persistence")
public final class CatalogTraceIdFilter extends OncePerRequestFilter {

  public static final String MDC_KEY = "traceId";
  public static final String RESPONSE_HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = canonicalOrNew(request.getHeader(RESPONSE_HEADER));

    MDC.put(MDC_KEY, traceId);
    response.setHeader(RESPONSE_HEADER, traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private static String canonicalOrNew(String value) {
    if (value != null && value.length() == 36) {
      try {
        String canonical = UUID.fromString(value).toString();
        if (canonical.equals(value)) {
          return canonical;
        }
      } catch (IllegalArgumentException ignored) {
        // Replace malformed client trace identifiers.
      }
    }
    return UUID.randomUUID().toString();
  }
}

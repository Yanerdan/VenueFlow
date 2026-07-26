package com.yanerdan.venueflow.booking.web;

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
public final class BookingTraceIdFilter extends OncePerRequestFilter {

  static final String HEADER = "X-Trace-Id";
  static final String MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = canonicalOrNew(request.getHeader(HEADER));
    MDC.put(MDC_KEY, traceId);
    response.setHeader(HEADER, traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  static String currentTraceId() {
    String value = MDC.get(MDC_KEY);
    return value == null ? UUID.randomUUID().toString() : value;
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

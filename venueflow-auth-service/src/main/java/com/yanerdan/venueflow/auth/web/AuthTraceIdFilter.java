package com.yanerdan.venueflow.auth.web;

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
public final class AuthTraceIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = canonicalOrNew(request.getHeader("X-Trace-Id"));
    MDC.put("traceId", traceId);
    response.setHeader("X-Trace-Id", traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove("traceId");
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

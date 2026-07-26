package com.yanerdan.venueflow.search.web;

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
@Profile("search")
public final class SearchTraceIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = canonical(request.getHeader(HEADER));
    response.setHeader(HEADER, traceId);
    MDC.put("traceId", traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove("traceId");
    }
  }

  static String canonical(String candidate) {
    try {
      return UUID.fromString(candidate).toString();
    } catch (RuntimeException exception) {
      return UUID.randomUUID().toString();
    }
  }
}

package com.yanerdan.venueflow.search.application;

public final class SearchUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SearchUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}

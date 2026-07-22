package com.yanerdan.venueflow.resource.catalog.error;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CatalogException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final CatalogErrorCode code;
  private final LinkedHashMap<String, Serializable> details;

  public CatalogException(
      CatalogErrorCode code,
      String message,
      Map<String, ? extends Serializable> details,
      Throwable cause) {
    super(message, cause);
    this.code = Objects.requireNonNull(code, "code must not be null");
    this.details = new LinkedHashMap<>(Objects.requireNonNull(details, "details must not be null"));
  }

  public CatalogErrorCode getCode() {
    return code;
  }

  public Map<String, Serializable> getDetails() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(details));
  }
}

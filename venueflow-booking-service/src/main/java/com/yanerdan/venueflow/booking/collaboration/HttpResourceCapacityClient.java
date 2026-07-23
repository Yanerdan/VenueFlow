package com.yanerdan.venueflow.booking.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.booking.application.BookingErrorCode;
import com.yanerdan.venueflow.booking.application.BookingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence")
public class HttpResourceCapacityClient implements ResourceCapacityClient {
  private final HttpClient client;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final Duration requestTimeout;
  private final int lookupAttempts;

  public HttpResourceCapacityClient(
      ObjectMapper objectMapper,
      @Value("${venueflow.collaborators.resource-base-url}") String baseUrl,
      @Value("${venueflow.collaborators.connect-timeout-ms:1000}") long connectTimeoutMs,
      @Value("${venueflow.collaborators.request-timeout-ms:2000}") long requestTimeoutMs,
      @Value("${venueflow.collaborators.lookup-attempts:2}") int lookupAttempts) {
    this.objectMapper = objectMapper.copy();
    this.baseUrl = baseUrl;
    this.client =
        HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
    this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
    this.lookupAttempts = Math.max(1, lookupAttempts);
  }

  @Override
  public void allocate(long slotId, String operationId, int quantity) {
    try {
      write(slotId, "allocations", operationId, quantity);
    } catch (HttpTimeoutException exception) {
      for (int attempt = 0; attempt < lookupAttempts; attempt++) {
        Optional<ResourceOperation> operation = findOperation(slotId, operationId);
        if (operation.isPresent()) {
          ResourceOperation value = operation.orElseThrow();
          if ("ALLOCATE".equals(value.operationType()) && value.quantity() == quantity) return;
          throw new BookingException(
              BookingErrorCode.BOOKING_CAPACITY_UNAVAILABLE,
              "Resource operation conflicts with the booking request");
        }
      }
      throw new BookingException(
          BookingErrorCode.BOOKING_ALLOCATION_OUTCOME_UNKNOWN,
          "Resource allocation outcome is unknown",
          exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw unavailable(exception);
    } catch (BookingException exception) {
      throw exception;
    } catch (Exception exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public void release(long slotId, String operationId, int quantity) {
    try {
      write(slotId, "releases", operationId, quantity);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw unavailable(exception);
    } catch (BookingException exception) {
      throw exception;
    } catch (Exception exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public Optional<ResourceOperation> findOperation(long slotId, String operationId) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(
                  URI.create(
                      baseUrl
                          + "/api/v1/resource-slots/"
                          + slotId
                          + "/allocation-operations/"
                          + operationId))
              .timeout(requestTimeout)
              .GET()
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 404) return Optional.empty();
      if (response.statusCode() != 200) throw unavailable(null);
      return Optional.of(objectMapper.readValue(response.body(), ResourceOperation.class));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw unavailable(exception);
    } catch (BookingException exception) {
      throw exception;
    } catch (Exception exception) {
      throw unavailable(exception);
    }
  }

  private void write(long slotId, String action, String operationId, int quantity)
      throws IOException, InterruptedException {
    String body = objectMapper.writeValueAsString(new CapacityChange(operationId, quantity));
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(baseUrl + "/api/v1/resource-slots/" + slotId + "/" + action))
            .timeout(requestTimeout)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 200 && response.statusCode() < 300) return;
    if (response.statusCode() == 409) {
      throw new BookingException(
          BookingErrorCode.BOOKING_CAPACITY_UNAVAILABLE, "Resource capacity was rejected");
    }
    throw unavailable(null);
  }

  private static BookingException unavailable(Throwable cause) {
    return new BookingException(
        BookingErrorCode.BOOKING_DOWNSTREAM_UNAVAILABLE,
        "Resource capacity service is unavailable",
        cause);
  }

  private record CapacityChange(String operationId, int quantity) {}
}

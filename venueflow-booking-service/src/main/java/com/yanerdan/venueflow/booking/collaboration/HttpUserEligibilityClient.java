package com.yanerdan.venueflow.booking.collaboration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.booking.application.BookingErrorCode;
import com.yanerdan.venueflow.booking.application.BookingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & !governance")
public class HttpUserEligibilityClient implements UserEligibilityClient {
  private final HttpClient client;
  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final Duration requestTimeout;

  public HttpUserEligibilityClient(
      ObjectMapper objectMapper,
      @Value("${venueflow.collaborators.user-base-url}") String baseUrl,
      @Value("${venueflow.collaborators.connect-timeout-ms:1000}") long connectTimeoutMs,
      @Value("${venueflow.collaborators.request-timeout-ms:2000}") long requestTimeoutMs) {
    this.objectMapper =
        objectMapper.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    this.baseUrl = baseUrl;
    this.client =
        HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
    this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
  }

  @Override
  public boolean isBookingPermitted(long userId) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(
                  URI.create(baseUrl + "/api/v1/users/" + userId + "/booking-eligibility"))
              .timeout(requestTimeout)
              .GET()
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400 && response.statusCode() < 500) return false;
      if (response.statusCode() != 200) throw unavailable(null);
      EligibilityResponse result =
          objectMapper.readValue(response.body(), EligibilityResponse.class);
      return result.bookingPermitted();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw unavailable(exception);
    } catch (BookingException exception) {
      throw exception;
    } catch (Exception exception) {
      throw unavailable(exception);
    }
  }

  private static BookingException unavailable(Throwable cause) {
    return new BookingException(
        BookingErrorCode.BOOKING_DOWNSTREAM_UNAVAILABLE,
        "User eligibility service is unavailable",
        cause);
  }

  private record EligibilityResponse(boolean bookingPermitted) {}
}

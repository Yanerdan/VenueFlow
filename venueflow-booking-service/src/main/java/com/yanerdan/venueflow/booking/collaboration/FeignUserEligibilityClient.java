package com.yanerdan.venueflow.booking.collaboration;

import com.yanerdan.venueflow.booking.application.BookingErrorCode;
import com.yanerdan.venueflow.booking.application.BookingException;
import feign.FeignException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & governance")
public class FeignUserEligibilityClient implements UserEligibilityClient {

  private final FeignUserEligibilityApi api;

  FeignUserEligibilityClient(FeignUserEligibilityApi api) {
    this.api = api;
  }

  @Override
  public boolean isBookingPermitted(long userId) {
    try {
      return api.eligibility(userId).bookingPermitted();
    } catch (FeignException.BadRequest | FeignException.NotFound exception) {
      return false;
    } catch (FeignException exception) {
      throw unavailable(exception);
    }
  }

  private static BookingException unavailable(Throwable cause) {
    return new BookingException(
        BookingErrorCode.BOOKING_DOWNSTREAM_UNAVAILABLE,
        "User eligibility service is unavailable",
        cause);
  }
}

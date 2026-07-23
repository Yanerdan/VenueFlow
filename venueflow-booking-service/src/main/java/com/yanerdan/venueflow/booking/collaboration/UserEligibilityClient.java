package com.yanerdan.venueflow.booking.collaboration;

public interface UserEligibilityClient {
  boolean isBookingPermitted(long userId);
}

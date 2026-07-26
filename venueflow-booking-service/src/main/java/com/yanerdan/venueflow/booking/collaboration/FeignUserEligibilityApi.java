package com.yanerdan.venueflow.booking.collaboration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "venueflow-user-service", contextId = "userEligibility")
interface FeignUserEligibilityApi {

  @GetMapping("/api/v1/users/{userId}/booking-eligibility")
  EligibilityResponse eligibility(@PathVariable long userId);

  record EligibilityResponse(boolean bookingPermitted) {}
}

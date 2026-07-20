package com.yanerdan.venueflow.common.core.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

  @Test
  void givenDomainAndReason_whenCreatingCode_thenValueIsPreserved() {
    var code = new ErrorCode("BOOKING_SLOT_SOLD_OUT");

    assertEquals("BOOKING_SLOT_SOLD_OUT", code.value());
    assertEquals(code.value(), code.toString());
  }

  @Test
  void givenMalformedValue_whenCreatingCode_thenCreationIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new ErrorCode("invalid"));
    assertThrows(NullPointerException.class, () -> new ErrorCode(null));
  }
}

package com.yanerdan.venueflow.booking.web;

import com.yanerdan.venueflow.booking.application.BookingReservationService;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Profile("persistence")
@RequestMapping("/api/v1/bookings")
public class BookingReservationController {
  private final BookingReservationService service;

  public BookingReservationController(BookingReservationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<SuccessEnvelope<BookingResponse>> create(
      @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
      @Valid @RequestBody CreateBookingRequest request) {
    BookingReservationService.CreateResult result =
        service.create(idempotencyKey, request.userId(), request.slotId(), request.quantity());
    return ResponseEntity.status(result.replay() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(SuccessEnvelope.of(BookingResponse.from(result.reservation())));
  }

  @GetMapping("/{bookingNo}")
  public SuccessEnvelope<BookingResponse> get(@PathVariable @NotBlank String bookingNo) {
    return SuccessEnvelope.of(BookingResponse.from(service.get(bookingNo)));
  }

  @PostMapping("/{bookingNo}/cancellation")
  public SuccessEnvelope<BookingResponse> cancel(@PathVariable @NotBlank String bookingNo) {
    return SuccessEnvelope.of(BookingResponse.from(service.cancel(bookingNo)));
  }

  @PostMapping("/{bookingNo}/confirmation")
  public SuccessEnvelope<BookingResponse> confirm(@PathVariable @NotBlank String bookingNo) {
    return SuccessEnvelope.of(BookingResponse.from(service.confirm(bookingNo)));
  }

  public record CreateBookingRequest(
      @Positive long userId, @Positive long slotId, @Positive int quantity) {}

  public record BookingResponse(
      String bookingNo,
      long userId,
      long slotId,
      int quantity,
      String status,
      long version,
      LocalDateTime createdAt,
      LocalDateTime expireAt,
      LocalDateTime confirmedAt,
      LocalDateTime cancelledAt,
      LocalDateTime expiredAt,
      LocalDateTime updatedAt) {
    static BookingResponse from(BookingReservation reservation) {
      return new BookingResponse(
          reservation.bookingNo(),
          reservation.userId(),
          reservation.slotId(),
          reservation.quantity(),
          reservation.status().name(),
          reservation.version(),
          reservation.createdAt(),
          reservation.expireAt(),
          reservation.confirmedAt(),
          reservation.cancelledAt(),
          reservation.expiredAt(),
          reservation.updatedAt());
    }
  }

  public record SuccessEnvelope<T>(String code, String message, T data, String traceId) {
    static <T> SuccessEnvelope<T> of(T data) {
      return new SuccessEnvelope<>("OK", "success", data, UUID.randomUUID().toString());
    }
  }
}

package com.yanerdan.venueflow.booking.web;

import com.yanerdan.venueflow.booking.application.BookingReservationService;
import com.yanerdan.venueflow.booking.domain.BookingReservation;
import com.yanerdan.venueflow.booking.domain.BookingStatus;
import com.yanerdan.venueflow.booking.persistence.BookingApprovalAction;
import com.yanerdan.venueflow.booking.persistence.BookingApprovalStageSnapshot;
import com.yanerdan.venueflow.booking.persistence.BookingRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    boolean detailsAbsent =
        request.activityTitle() == null
            && request.purpose() == null
            && request.contactName() == null
            && request.contactPhone() == null
            && request.note() == null;
    BookingReservationService.CreateResult result =
        detailsAbsent
            ? service.create(idempotencyKey, request.userId(), request.slotId(), request.quantity())
            : service.create(
                idempotencyKey,
                request.userId(),
                request.slotId(),
                request.quantity(),
                request.activityTitle(),
                request.purpose(),
                request.contactName(),
                request.contactPhone(),
                request.note());
    return ResponseEntity.status(result.replay() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(SuccessEnvelope.of(response(result.reservation())));
  }

  @GetMapping("/{bookingNo}")
  public SuccessEnvelope<BookingResponse> get(@PathVariable @NotBlank String bookingNo) {
    return SuccessEnvelope.of(response(service.get(bookingNo)));
  }

  @GetMapping("/{bookingNo}/approval-actions")
  public SuccessEnvelope<List<BookingApprovalAction>> approvalActions(
      @PathVariable @NotBlank String bookingNo) {
    return SuccessEnvelope.of(service.approvalActions(bookingNo));
  }

  @GetMapping("/{bookingNo}/approval-stages")
  public SuccessEnvelope<List<BookingApprovalStageSnapshot>> approvalStages(
      @PathVariable @NotBlank String bookingNo) {
    return SuccessEnvelope.of(service.approvalStages(bookingNo));
  }

  @GetMapping
  public SuccessEnvelope<BookingPageResponse> history(
      @RequestParam @Positive long userId,
      @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    BookingRepository.BookingHistoryPage page = service.history(userId, pageNumber, pageSize);
    return SuccessEnvelope.of(
        new BookingPageResponse(
            page.items().stream().map(this::response).toList(),
            page.totalElements(),
            page.pageNumber(),
            page.pageSize()));
  }

  @PostMapping("/{bookingNo}/cancellation")
  public SuccessEnvelope<BookingResponse> cancel(
      @PathVariable @NotBlank String bookingNo,
      @Valid @RequestBody(required = false) ReviewActionRequest request) {
    BookingReservation result =
        request == null || request.reviewNote() == null
            ? service.cancel(bookingNo)
            : service.cancel(bookingNo, request.reviewNote());
    return SuccessEnvelope.of(response(result));
  }

  @PostMapping("/{bookingNo}/confirmation")
  public SuccessEnvelope<BookingResponse> confirm(
      @PathVariable @NotBlank String bookingNo,
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestHeader(value = "X-User-Id", required = false) String trustedUserId,
      @Valid @RequestBody(required = false) ReviewActionRequest request) {
    BookingRoleGuard.requireApprover(role);
    service.requireApprovalScope(bookingNo, trustedUserId, role);
    BookingReservation result =
        request == null || request.reviewNote() == null
            ? service.confirm(bookingNo)
            : service.confirm(bookingNo, request.reviewNote(), role, trustedUserId);
    return SuccessEnvelope.of(response(result));
  }

  @PostMapping("/{bookingNo}/rejection")
  public SuccessEnvelope<BookingResponse> reject(
      @PathVariable @NotBlank String bookingNo,
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestHeader(value = "X-User-Id", required = false) String trustedUserId,
      @Valid @RequestBody RejectionRequest request) {
    BookingRoleGuard.requireApprover(role);
    service.requireApprovalScope(bookingNo, trustedUserId, role);
    return SuccessEnvelope.of(
        response(service.reject(bookingNo, request.reason(), role, trustedUserId)));
  }

  @PostMapping("/{bookingNo}/check-in")
  public SuccessEnvelope<BookingResponse> checkIn(
      @PathVariable @NotBlank String bookingNo,
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestHeader(value = "X-User-Id", required = false) String trustedUserId) {
    BookingRoleGuard.requireApprover(role);
    service.requireApprovalScope(bookingNo, trustedUserId, role);
    return SuccessEnvelope.of(response(service.checkIn(bookingNo)));
  }

  @GetMapping("/management")
  public SuccessEnvelope<BookingPageResponse> managementHistory(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestHeader(value = "X-User-Id", required = false) String trustedUserId,
      @RequestParam(required = false) BookingStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    BookingRoleGuard.requireApprover(role);
    BookingRepository.BookingHistoryPage page =
        trustedUserId == null
            ? service.managementHistory(status, pageNumber, pageSize)
            : service.managementHistory(status, trustedUserId, role, pageNumber, pageSize);
    return SuccessEnvelope.of(
        new BookingPageResponse(
            page.items().stream().map(this::response).toList(),
            page.totalElements(),
            page.pageNumber(),
            page.pageSize()));
  }

  public record CreateBookingRequest(
      @Positive long userId,
      @Positive long slotId,
      @Positive int quantity,
      @Size(max = 160) String activityTitle,
      @Size(max = 500) String purpose,
      @Size(max = 120) String contactName,
      @Size(max = 32) String contactPhone,
      @Size(max = 1000) String note) {
    public CreateBookingRequest(long userId, long slotId, int quantity) {
      this(userId, slotId, quantity, null, null, null, null, null);
    }
  }

  public record ReviewActionRequest(@Size(max = 1000) String reviewNote) {}

  public record RejectionRequest(
      @NotBlank(message = "Rejection reason must not be blank") @Size(max = 1000) String reason) {}

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
      LocalDateTime completedAt,
      LocalDateTime updatedAt,
      String activityTitle,
      String purpose,
      String contactName,
      String contactPhone,
      String note,
      String reviewDecision,
      String reviewNote,
      String reviewerRole,
      LocalDateTime reviewedAt,
      Long resourceId,
      String ownerDepartment,
      String assignedApproverExternalUserId,
      String approvalMode,
      String finalAssignedApproverExternalUserId,
      int currentApprovalStep,
      int totalApprovalSteps,
      List<BookingApprovalStageSnapshot> approvalStages) {
    static BookingResponse from(
        BookingReservation reservation, List<BookingApprovalStageSnapshot> approvalStages) {
      approvalStages = approvalStages == null ? List.of() : List.copyOf(approvalStages);
      int totalSteps =
          approvalStages.isEmpty()
              ? ("TWO_STAGE".equals(reservation.approvalMode()) ? 2 : 1)
              : approvalStages.size();
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
          reservation.completedAt(),
          reservation.updatedAt(),
          reservation.activityTitle(),
          reservation.applicationPurpose(),
          reservation.contactName(),
          reservation.contactPhone(),
          reservation.applicationNote(),
          reservation.reviewDecision(),
          reservation.reviewNote(),
          reservation.reviewerRole(),
          reservation.reviewedAt(),
          reservation.resourceId(),
          reservation.ownerDepartment(),
          reservation.assignedApproverExternalUserId(),
          reservation.approvalMode(),
          reservation.finalAssignedApproverExternalUserId(),
          reservation.currentApprovalStep(),
          totalSteps,
          approvalStages);
    }
  }

  private BookingResponse response(BookingReservation reservation) {
    return BookingResponse.from(reservation, service.approvalStages(reservation.bookingNo()));
  }

  public record BookingPageResponse(
      List<BookingResponse> items, long totalElements, int pageNumber, int pageSize) {
    public BookingPageResponse {
      items = List.copyOf(items);
    }
  }

  public record SuccessEnvelope<T>(String code, String message, T data, String traceId) {
    static <T> SuccessEnvelope<T> of(T data) {
      return new SuccessEnvelope<>("OK", "success", data, BookingTraceIdFilter.currentTraceId());
    }
  }
}

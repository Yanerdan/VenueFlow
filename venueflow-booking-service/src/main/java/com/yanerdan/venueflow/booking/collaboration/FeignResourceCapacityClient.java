package com.yanerdan.venueflow.booking.collaboration;

import com.yanerdan.venueflow.booking.application.BookingErrorCode;
import com.yanerdan.venueflow.booking.application.BookingException;
import feign.FeignException;
import feign.RetryableException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence & governance")
public class FeignResourceCapacityClient implements ResourceCapacityClient {

  private final FeignResourceCapacityApi api;
  private final int lookupAttempts;

  FeignResourceCapacityClient(
      FeignResourceCapacityApi api,
      @Value("${venueflow.collaborators.lookup-attempts:2}") int lookupAttempts) {
    this.api = api;
    this.lookupAttempts = Math.max(1, lookupAttempts);
  }

  @Override
  public void allocate(long slotId, String operationId, int quantity) {
    try {
      api.allocate(slotId, new FeignResourceCapacityApi.CapacityChange(operationId, quantity));
    } catch (RetryableException exception) {
      for (int attempt = 0; attempt < lookupAttempts; attempt++) {
        Optional<ResourceOperation> operation = findOperation(slotId, operationId);
        if (operation.isPresent()) {
          ResourceOperation value = operation.orElseThrow();
          if ("ALLOCATE".equals(value.operationType()) && value.quantity() == quantity) {
            return;
          }
          throw new BookingException(
              BookingErrorCode.BOOKING_CAPACITY_UNAVAILABLE,
              "Resource operation conflicts with the booking request");
        }
      }
      throw new BookingException(
          BookingErrorCode.BOOKING_ALLOCATION_OUTCOME_UNKNOWN,
          "Resource allocation outcome is unknown",
          exception);
    } catch (FeignException.Conflict exception) {
      throw capacityRejected(exception);
    } catch (FeignException exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public void release(long slotId, String operationId, int quantity) {
    try {
      api.release(slotId, new FeignResourceCapacityApi.CapacityChange(operationId, quantity));
    } catch (FeignException.Conflict exception) {
      throw capacityRejected(exception);
    } catch (FeignException exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public Optional<ResourceOperation> findOperation(long slotId, String operationId) {
    try {
      FeignResourceCapacityApi.OperationResponse value = api.operation(slotId, operationId);
      return Optional.of(
          new ResourceOperation(value.operationId(), value.operationType(), value.quantity()));
    } catch (FeignException.NotFound exception) {
      return Optional.empty();
    } catch (FeignException exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public ResourceSlot findSlot(long slotId) {
    try {
      FeignResourceCapacityApi.SlotResponse value = api.slot(slotId);
      if (value.id() != slotId) {
        throw invalidSlot();
      }
      return new ResourceSlot(
          value.id(),
          value.resourceId(),
          value.ownerDepartment(),
          value.approverExternalUserId(),
          value.approvalMode(),
          value.finalApproverExternalUserId(),
          value.bookingNotice(),
          value.minAdvanceHours(),
          value.maxAdvanceDays(),
          value.maxDurationMinutes(),
          value.approvalStages(),
          value.startAt(),
          value.endAt());
    } catch (BookingException exception) {
      throw exception;
    } catch (IllegalArgumentException exception) {
      throw invalidSlot();
    } catch (FeignException exception) {
      throw unavailable(exception);
    }
  }

  private static BookingException capacityRejected(Throwable cause) {
    return new BookingException(
        BookingErrorCode.BOOKING_CAPACITY_UNAVAILABLE, "Resource capacity was rejected", cause);
  }

  private static BookingException unavailable(Throwable cause) {
    return new BookingException(
        BookingErrorCode.BOOKING_DOWNSTREAM_UNAVAILABLE,
        "Resource capacity service is unavailable",
        cause);
  }

  private static BookingException invalidSlot() {
    return new BookingException(
        BookingErrorCode.BOOKING_RESOURCE_CONTRACT_INVALID, "Resource slot response is invalid");
  }
}

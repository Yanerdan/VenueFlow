package com.yanerdan.venueflow.booking.collaboration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ResourceCapacityClient {
  void allocate(long slotId, String operationId, int quantity);

  void release(long slotId, String operationId, int quantity);

  Optional<ResourceOperation> findOperation(long slotId, String operationId);

  ResourceSlot findSlot(long slotId);

  record ResourceOperation(String operationId, String operationType, int quantity) {}

  record ApprovalStage(int stageOrder, String stageName, String approverExternalUserId) {}

  record ResourceSlot(
      long slotId,
      Long resourceId,
      String ownerDepartment,
      String approverExternalUserId,
      String approvalMode,
      String finalApproverExternalUserId,
      String bookingNotice,
      Integer minAdvanceHours,
      Integer maxAdvanceDays,
      Integer maxDurationMinutes,
      List<ApprovalStage> approvalStages,
      Instant startAt,
      Instant endAt) {
    public ResourceSlot {
      approvalStages = approvalStages == null ? List.of() : List.copyOf(approvalStages);
      if (slotId <= 0 || startAt == null || endAt == null || !endAt.isAfter(startAt)) {
        throw new IllegalArgumentException("Resource slot facts are invalid");
      }
    }

    public ResourceSlot(long slotId, Instant startAt, Instant endAt) {
      this(slotId, null, null, null, "DIRECT", null, null, 0, 90, 480, List.of(), startAt, endAt);
    }

    public ResourceSlot(
        long slotId,
        Long resourceId,
        String ownerDepartment,
        String approverExternalUserId,
        String approvalMode,
        String finalApproverExternalUserId,
        String bookingNotice,
        Integer minAdvanceHours,
        Integer maxAdvanceDays,
        Integer maxDurationMinutes,
        Instant startAt,
        Instant endAt) {
      this(
          slotId,
          resourceId,
          ownerDepartment,
          approverExternalUserId,
          approvalMode,
          finalApproverExternalUserId,
          bookingNotice,
          minAdvanceHours,
          maxAdvanceDays,
          maxDurationMinutes,
          List.of(),
          startAt,
          endAt);
    }

    public ResourceSlot(
        long slotId,
        Long resourceId,
        String ownerDepartment,
        String approverExternalUserId,
        String approvalMode,
        String finalApproverExternalUserId,
        Instant startAt,
        Instant endAt) {
      this(
          slotId,
          resourceId,
          ownerDepartment,
          approverExternalUserId,
          approvalMode,
          finalApproverExternalUserId,
          null,
          0,
          90,
          480,
          List.of(),
          startAt,
          endAt);
    }
  }
}

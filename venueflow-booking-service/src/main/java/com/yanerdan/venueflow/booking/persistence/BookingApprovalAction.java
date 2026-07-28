package com.yanerdan.venueflow.booking.persistence;

import java.time.LocalDateTime;

public record BookingApprovalAction(
    int approvalStep,
    String actorExternalUserId,
    String actorRole,
    String decision,
    String reviewNote,
    LocalDateTime createdAt) {}

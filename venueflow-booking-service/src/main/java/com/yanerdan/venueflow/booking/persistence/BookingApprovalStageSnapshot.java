package com.yanerdan.venueflow.booking.persistence;

import java.time.LocalDateTime;

public record BookingApprovalStageSnapshot(
    int stageOrder,
    String stageName,
    String approverExternalUserId,
    String stageStatus,
    LocalDateTime decidedAt) {}

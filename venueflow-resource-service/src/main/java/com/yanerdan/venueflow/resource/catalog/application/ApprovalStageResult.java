package com.yanerdan.venueflow.resource.catalog.application;

public record ApprovalStageResult(
    int stageOrder, String stageName, String approverExternalUserId) {}

package com.yanerdan.venueflow.booking.expiration.application;

public record ExpirationSummary(
    int claimed, int expired, int retried, int lost, int leaseReclaimed) {}

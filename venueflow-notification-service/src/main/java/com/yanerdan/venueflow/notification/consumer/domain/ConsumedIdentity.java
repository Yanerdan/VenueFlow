package com.yanerdan.venueflow.notification.consumer.domain;

public record ConsumedIdentity(String eventType, int eventVersion, String payloadHash) {}

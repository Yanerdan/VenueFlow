package com.yanerdan.venueflow.search.application;

public record ResourceDocument(
    Long resourceId,
    String resourceNo,
    Long categoryId,
    String name,
    String description,
    String location,
    Integer capacity,
    String status,
    Long version,
    String updatedAt) {}

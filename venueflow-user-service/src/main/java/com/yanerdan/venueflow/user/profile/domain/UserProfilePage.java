package com.yanerdan.venueflow.user.profile.domain;

import java.util.List;

public record UserProfilePage(
    List<UserProfile> items, int pageNumber, int pageSize, long totalElements) {}

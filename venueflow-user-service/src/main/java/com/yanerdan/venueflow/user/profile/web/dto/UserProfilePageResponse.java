package com.yanerdan.venueflow.user.profile.web.dto;

import com.yanerdan.venueflow.user.profile.domain.UserProfilePage;
import java.util.List;

public record UserProfilePageResponse(
    List<UserProfileResponse> items, int pageNumber, int pageSize, long totalElements) {

  public static UserProfilePageResponse from(UserProfilePage page) {
    return new UserProfilePageResponse(
        page.items().stream().map(UserProfileResponse::from).toList(),
        page.pageNumber(),
        page.pageSize(),
        page.totalElements());
  }
}

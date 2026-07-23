package com.yanerdan.venueflow.user.profile.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.ExternalUserId;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import com.yanerdan.venueflow.user.profile.domain.UserProfileId;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserBookingEligibilityViewTest {

  @Test
  void createsBoundedViewWithoutExternalIdentityOrDisplayName() {
    LocalDateTime timestamp = LocalDateTime.of(2026, 7, 23, 10, 30);

    UserProfile profile =
        new UserProfile(
            new UserProfileId(42L),
            new ExternalUserId("customer-123"),
            "Alice",
            AccountStatus.ACTIVE,
            BookingEligibility.ELIGIBLE,
            3L,
            timestamp.minusHours(1),
            timestamp);

    UserBookingEligibilityView view = UserBookingEligibilityView.from(profile);

    assertThat(view.userProfileId()).isEqualTo(42L);

    assertThat(view.accountStatus()).isEqualTo(AccountStatus.ACTIVE);

    assertThat(view.bookingEligibility()).isEqualTo(BookingEligibility.ELIGIBLE);

    assertThat(view.bookingPermitted()).isTrue();

    assertThat(view.version()).isEqualTo(3L);

    assertThat(view.updatedAt()).isEqualTo(timestamp);
  }
}

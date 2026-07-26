package com.yanerdan.venueflow.booking.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;

class ReconciliationAdminCommandTest {
  private final ReconciliationService service = Mockito.mock(ReconciliationService.class);

  @Test
  void noActionMakesNoChange() {
    new ReconciliationAdminCommand(service, "", "", false).run(new DefaultApplicationArguments());

    verify(service, never()).runOnce(Mockito.anyString(), Mockito.any());
  }

  @Test
  void runRequiresExplicitConfirmationAndReason() {
    ReconciliationAdminCommand command =
        new ReconciliationAdminCommand(service, "RUN", "operator repair", false);

    assertThatThrownBy(() -> command.run(new DefaultApplicationArguments()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(service, never()).runOnce(Mockito.anyString(), Mockito.any());
  }

  @Test
  void confirmedRunUsesSharedService() {
    new ReconciliationAdminCommand(service, "RUN", "operator repair", true)
        .run(new DefaultApplicationArguments());

    verify(service).runOnce("OPERATOR", "operator repair");
  }
}

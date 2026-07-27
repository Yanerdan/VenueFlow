package com.yanerdan.venueflow.search.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class SearchControllerProxyabilityTest {

  @Test
  void validatedControllerRemainsProxyable() {
    assertThat(Modifier.isFinal(SearchController.class.getModifiers())).isFalse();
  }
}

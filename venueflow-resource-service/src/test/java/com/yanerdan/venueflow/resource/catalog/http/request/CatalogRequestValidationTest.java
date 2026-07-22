package com.yanerdan.venueflow.resource.catalog.http.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.yanerdan.venueflow.resource.catalog.application.ResourcePageQuery;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CatalogRequestValidationTest {

  @Test
  void rejectsBlankCategoryFields() {
    CreateCategoryRequest request = new CreateCategoryRequest(" ", "");

    Set<ConstraintViolation<CreateCategoryRequest>> violations = validate(request);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("code", "name");
  }

  @Test
  void acceptsValidCategoryCreationRequest() {
    CreateCategoryRequest request = new CreateCategoryRequest("MEETING_ROOM", "Meeting Room");

    assertThat(validate(request)).isEmpty();

    assertThat(request.toCommand().code()).isEqualTo("MEETING_ROOM");

    assertThat(request.toCommand().name()).isEqualTo("Meeting Room");
  }

  @Test
  void rejectsInvalidResourceCreationFields() {
    CreateResourceRequest request = new CreateResourceRequest(" ", null, "", null, null, 0);

    Set<ConstraintViolation<CreateResourceRequest>> violations = validate(request);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("resourceNo", "categoryId", "name", "capacity");
  }

  @Test
  void acceptsValidResourceCreationRequest() {
    CreateResourceRequest request =
        new CreateResourceRequest(
            "ROOM-A-101", 1L, "Room A101", "First-floor meeting room", "Building A", 10);

    assertThat(validate(request)).isEmpty();

    assertThat(request.toCommand().resourceNo()).isEqualTo("ROOM-A-101");

    assertThat(request.toCommand().categoryId()).isEqualTo(1L);

    assertThat(request.toCommand().capacity()).isEqualTo(10);
  }

  @Test
  void rejectsInvalidResourcePageParameters() {
    ResourcePageRequest request = new ResourcePageRequest(-1, 101, 0L, ResourceStatus.ACTIVE);

    Set<ConstraintViolation<ResourcePageRequest>> violations = validate(request);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("page", "size", "categoryId");
  }

  @Test
  void appliesDefaultResourcePageParameters() {
    ResourcePageRequest request = new ResourcePageRequest(null, null, null, null);

    assertThat(validate(request)).isEmpty();

    ResourcePageQuery query = request.toQuery();

    assertThat(query.page()).isEqualTo(ResourcePageQuery.DEFAULT_PAGE);

    assertThat(query.size()).isEqualTo(ResourcePageQuery.DEFAULT_SIZE);

    assertThat(query.categoryId()).isNull();
    assertThat(query.status()).isNull();
  }

  @Test
  void rejectsInvalidStatusChangeRequest() {
    ChangeResourceStatusRequest request = new ChangeResourceStatusRequest(null, 0L);

    Set<ConstraintViolation<ChangeResourceStatusRequest>> violations = validate(request);

    assertThat(violations)
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("targetStatus", "expectedVersion");
  }

  @Test
  void acceptsValidStatusChangeRequest() {
    ChangeResourceStatusRequest request =
        new ChangeResourceStatusRequest(ResourceStatus.ACTIVE, 1L);

    assertThat(validate(request)).isEmpty();

    assertThat(request.toCommand(100L).resourceId()).isEqualTo(100L);

    assertThat(request.toCommand(100L).targetStatus()).isEqualTo(ResourceStatus.ACTIVE);

    assertThat(request.toCommand(100L).expectedVersion()).isEqualTo(1L);
  }

  private static <T> Set<ConstraintViolation<T>> validate(T value) {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();

      return Set.copyOf(validator.validate(value));
    }
  }
}

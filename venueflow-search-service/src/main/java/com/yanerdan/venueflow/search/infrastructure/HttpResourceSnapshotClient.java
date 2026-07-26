package com.yanerdan.venueflow.search.infrastructure;

import com.yanerdan.venueflow.search.application.ResourceDocument;
import com.yanerdan.venueflow.search.application.ResourceSnapshotClient;
import com.yanerdan.venueflow.search.application.ResourceSnapshotPage;
import com.yanerdan.venueflow.search.application.SearchUnavailableException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("search")
public final class HttpResourceSnapshotClient implements ResourceSnapshotClient {

  private final RestClient client;

  public HttpResourceSnapshotClient(
      RestClient.Builder builder,
      @Value("${venueflow.search.resource-base-uri}") String resourceBaseUri) {
    client = builder.baseUrl(resourceBaseUri).build();
  }

  @Override
  public ResourceDocument get(Long resourceId) {
    try {
      Snapshot response =
          client
              .get()
              .uri("/api/v1/resources/{resourceId}", resourceId)
              .retrieve()
              .body(Snapshot.class);
      if (response == null) {
        throw new SearchUnavailableException("Resource returned no snapshot", null);
      }
      return response.toDocument();
    } catch (SearchUnavailableException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new SearchUnavailableException("Resource snapshot is unavailable", exception);
    }
  }

  @Override
  public ResourceSnapshotPage page(int page, int size) {
    try {
      SnapshotPage response =
          client
              .get()
              .uri(
                  uri ->
                      uri.path("/api/v1/resources")
                          .queryParam("page", page)
                          .queryParam("size", size)
                          .build())
              .retrieve()
              .body(SnapshotPage.class);
      if (response == null) {
        throw new SearchUnavailableException("Resource returned no page", null);
      }
      return new ResourceSnapshotPage(
          response.items().stream().map(Snapshot::toDocument).toList(), response.totalElements());
    } catch (SearchUnavailableException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new SearchUnavailableException("Resource page is unavailable", exception);
    }
  }

  record Snapshot(
      Long id,
      String resourceNo,
      Long categoryId,
      String name,
      String description,
      String location,
      Integer capacity,
      String status,
      Long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    ResourceDocument toDocument() {
      return new ResourceDocument(
          id,
          resourceNo,
          categoryId,
          name,
          description,
          location,
          capacity,
          status,
          version,
          updatedAt == null ? null : updatedAt.toString());
    }
  }

  record SnapshotPage(List<Snapshot> items, long totalElements) {}
}

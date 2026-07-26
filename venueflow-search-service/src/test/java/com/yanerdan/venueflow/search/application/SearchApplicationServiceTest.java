package com.yanerdan.venueflow.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchApplicationServiceTest {

  @Test
  void ignoresDuplicateEventAndProjectsLatestSnapshot() {
    FakeIndex index = new FakeIndex();
    FakeResources resources = new FakeResources(List.of(document(7L, 3L)));
    SearchApplicationService service = new SearchApplicationService(index, resources);

    service.project("6fd7ad0d-1861-4be3-9842-1ca94df76912", 7L, 2L);
    service.project("6fd7ad0d-1861-4be3-9842-1ca94df76912", 7L, 2L);

    assertThat(index.live).containsExactly(document(7L, 3L));
    assertThat(resources.getCalls).isEqualTo(1);
  }

  @Test
  void rebuildSwitchesAliasOnlyAfterExactCountValidation() {
    FakeIndex index = new FakeIndex();
    FakeResources resources = new FakeResources(List.of(document(1L, 1L), document(2L, 1L)));

    SearchApplicationService.RebuildResult result =
        new SearchApplicationService(index, resources).rebuild();

    assertThat(result.indexedDocuments()).isEqualTo(2);
    assertThat(index.switchedTo).isEqualTo(result.index());
  }

  @Test
  void rejectsInvalidOrUnboundedQueries() {
    assertThatThrownBy(() -> new ResourceSearchQuery(null, null, null, 0, 101))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ResourceSearchQuery(null, null, "invalid", 0, 20))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static ResourceDocument document(long id, long version) {
    return new ResourceDocument(
        id,
        "R-" + id,
        1L,
        "Room " + id,
        "Description",
        "A",
        10,
        "ACTIVE",
        version,
        "2026-07-26T12:00:00");
  }

  private static final class FakeResources implements ResourceSnapshotClient {
    private final List<ResourceDocument> documents;
    private int getCalls;

    private FakeResources(List<ResourceDocument> documents) {
      this.documents = documents;
    }

    @Override
    public ResourceDocument get(Long resourceId) {
      getCalls++;
      return documents.stream()
          .filter(document -> document.resourceId().equals(resourceId))
          .findFirst()
          .orElseThrow();
    }

    @Override
    public ResourceSnapshotPage page(int page, int size) {
      return page == 0
          ? new ResourceSnapshotPage(documents, documents.size())
          : new ResourceSnapshotPage(List.of(), documents.size());
    }
  }

  private static final class FakeIndex implements SearchIndex {
    private final List<ResourceDocument> live = new ArrayList<>();
    private final List<ResourceDocument> rebuilding = new ArrayList<>();
    private final Set<String> inbox = new HashSet<>();
    private String switchedTo;

    @Override
    public ResourceSearchPage search(ResourceSearchQuery query) {
      return new ResourceSearchPage(live, query.page(), query.size(), live.size());
    }

    @Override
    public void upsert(ResourceDocument document) {
      live.removeIf(existing -> existing.resourceId().equals(document.resourceId()));
      live.add(document);
    }

    @Override
    public boolean alreadyProcessed(String eventId) {
      return inbox.contains(eventId);
    }

    @Override
    public void markProcessed(String eventId) {
      inbox.add(eventId);
    }

    @Override
    public String createRebuildIndex() {
      return "venueflow-resource-test";
    }

    @Override
    public void upsert(String index, ResourceDocument document) {
      rebuilding.add(document);
    }

    @Override
    public long count(String index) {
      return rebuilding.size();
    }

    @Override
    public void switchAliases(String index) {
      switchedTo = index;
    }
  }
}

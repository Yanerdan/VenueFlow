package com.yanerdan.venueflow.search.application;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("search")
public final class SearchApplicationService {

  private final SearchIndex index;
  private final ResourceSnapshotClient resources;

  public SearchApplicationService(SearchIndex index, ResourceSnapshotClient resources) {
    this.index = index;
    this.resources = resources;
  }

  public ResourceSearchPage search(ResourceSearchQuery query) {
    return index.search(query);
  }

  public void project(String eventId, Long resourceId, Long eventVersion) {
    if (index.alreadyProcessed(eventId)) {
      return;
    }
    ResourceDocument latest = resources.get(resourceId);
    if (latest.version() < eventVersion) {
      throw new SearchUnavailableException("Resource snapshot is behind its event", null);
    }
    index.upsert(latest);
    index.markProcessed(eventId);
  }

  public RebuildResult rebuild() {
    String physicalIndex = index.createRebuildIndex();
    int page = 0;
    int size = 100;
    long imported = 0;
    long expected;
    do {
      ResourceSnapshotPage snapshot = resources.page(page, size);
      expected = snapshot.totalElements();
      if (snapshot.items().isEmpty() && imported < expected) {
        throw new SearchUnavailableException("Resource rebuild page made no progress", null);
      }
      snapshot.items().forEach(item -> index.upsert(physicalIndex, item));
      imported += snapshot.items().size();
      page++;
    } while (imported < expected);
    long indexed = index.count(physicalIndex);
    if (indexed != imported || imported != expected) {
      throw new SearchUnavailableException("Rebuild count validation failed", null);
    }
    index.switchAliases(physicalIndex);
    return new RebuildResult(physicalIndex, indexed);
  }

  public record RebuildResult(String index, long indexedDocuments) {}
}

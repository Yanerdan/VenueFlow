package com.yanerdan.venueflow.search.application;

public interface SearchIndex {

  ResourceSearchPage search(ResourceSearchQuery query);

  void upsert(ResourceDocument document);

  boolean alreadyProcessed(String eventId);

  void markProcessed(String eventId);

  String createRebuildIndex();

  void upsert(String index, ResourceDocument document);

  long count(String index);

  void switchAliases(String index);
}

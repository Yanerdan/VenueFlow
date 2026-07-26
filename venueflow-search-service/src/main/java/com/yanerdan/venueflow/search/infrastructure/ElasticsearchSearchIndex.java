package com.yanerdan.venueflow.search.infrastructure;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.search.application.ResourceDocument;
import com.yanerdan.venueflow.search.application.ResourceSearchPage;
import com.yanerdan.venueflow.search.application.ResourceSearchQuery;
import com.yanerdan.venueflow.search.application.SearchIndex;
import com.yanerdan.venueflow.search.application.SearchUnavailableException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("search")
public final class ElasticsearchSearchIndex implements SearchIndex {

  private final AtomicReference<Rest5Client> client;
  private final AtomicReference<ObjectMapper> objectMapper;
  private final String readAlias;
  private final String writeAlias;
  private final String inboxIndex;

  public ElasticsearchSearchIndex(
      Rest5Client client,
      ObjectMapper objectMapper,
      @Value("${venueflow.search.read-alias}") String readAlias,
      @Value("${venueflow.search.write-alias}") String writeAlias,
      @Value("${venueflow.search.inbox-index}") String inboxIndex) {
    this.client = new AtomicReference<>(client);
    this.objectMapper = new AtomicReference<>(objectMapper);
    this.readAlias = safeName(readAlias);
    this.writeAlias = safeName(writeAlias);
    this.inboxIndex = safeName(inboxIndex);
  }

  @Override
  public ResourceSearchPage search(ResourceSearchQuery query) {
    Map<String, Object> bool = new LinkedHashMap<>();
    List<Object> must = new ArrayList<>();
    List<Object> filters = new ArrayList<>();
    if (query.text() != null && !query.text().isBlank()) {
      must.add(
          Map.of(
              "multi_match",
              Map.of(
                  "query",
                  query.text(),
                  "fields",
                  List.of("name^3", "description", "location", "resourceNo"))));
    } else {
      must.add(Map.of("match_all", Map.of()));
    }
    if (query.categoryId() != null) {
      filters.add(Map.of("term", Map.of("categoryId", query.categoryId())));
    }
    if (query.status() != null) {
      filters.add(Map.of("term", Map.of("status", query.status())));
    }
    bool.put("must", must);
    if (!filters.isEmpty()) {
      bool.put("filter", filters);
    }
    Map<String, Object> body =
        Map.of(
            "from", Math.multiplyExact(query.page(), query.size()),
            "size", query.size(),
            "track_total_hits", true,
            "query", Map.of("bool", bool),
            "sort", List.of(Map.of("_score", "desc"), Map.of("resourceId", "asc")));
    JsonNode response = perform("POST", "/" + readAlias + "/_search", body);
    List<ResourceDocument> items = new ArrayList<>();
    response.path("hits").path("hits").forEach(hit -> items.add(toDocument(hit.path("_source"))));
    return new ResourceSearchPage(
        items,
        query.page(),
        query.size(),
        response.path("hits").path("total").path("value").asLong());
  }

  @Override
  public void upsert(ResourceDocument document) {
    upsert(writeAlias, document);
  }

  @Override
  public boolean alreadyProcessed(String eventId) {
    Request request = new Request("HEAD", "/" + inboxIndex + "/_doc/" + safeId(eventId));
    try {
      return client.get().performRequest(request).getStatusCode() == 200;
    } catch (ResponseException exception) {
      if (exception.getResponse().getStatusCode() == 404) {
        return false;
      }
      throw unavailable(exception);
    } catch (IOException exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public void markProcessed(String eventId) {
    Request request = new Request("PUT", "/" + inboxIndex + "/_doc/" + safeId(eventId));
    request.addParameter("op_type", "create");
    request.setJsonEntity(json(Map.of("processedAt", Instant.now().toString())));
    try {
      client.get().performRequest(request);
    } catch (ResponseException exception) {
      if (exception.getResponse().getStatusCode() != 409) {
        throw unavailable(exception);
      }
    } catch (IOException exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public String createRebuildIndex() {
    String index =
        "venueflow-resource-"
            + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(java.time.ZoneOffset.UTC)
                .format(Instant.now())
            + "-"
            + UUID.randomUUID().toString().substring(0, 8);
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("resourceId", Map.of("type", "long"));
    properties.put("resourceNo", Map.of("type", "keyword"));
    properties.put("categoryId", Map.of("type", "long"));
    properties.put("name", Map.of("type", "text"));
    properties.put("description", Map.of("type", "text"));
    properties.put("location", Map.of("type", "text"));
    properties.put("capacity", Map.of("type", "integer"));
    properties.put("status", Map.of("type", "keyword"));
    properties.put("version", Map.of("type", "long"));
    properties.put("updatedAt", Map.of("type", "date", "format", "strict_date_optional_time"));
    perform(
        "PUT",
        "/" + index,
        Map.of(
            "settings", Map.of("number_of_shards", 1, "number_of_replicas", 0),
            "mappings", Map.of("dynamic", "strict", "properties", properties)));
    return index;
  }

  @Override
  public void upsert(String index, ResourceDocument document) {
    Request request = new Request("PUT", "/" + safeName(index) + "/_doc/" + document.resourceId());
    request.addParameter("version", document.version().toString());
    request.addParameter("version_type", "external_gte");
    request.setJsonEntity(json(document));
    try {
      client.get().performRequest(request);
    } catch (ResponseException exception) {
      if (exception.getResponse().getStatusCode() != 409) {
        throw unavailable(exception);
      }
    } catch (IOException exception) {
      throw unavailable(exception);
    }
  }

  @Override
  public long count(String index) {
    return perform("GET", "/" + safeName(index) + "/_count", null).path("count").asLong();
  }

  @Override
  public void switchAliases(String index) {
    List<Object> actions =
        List.of(
            Map.of("remove", Map.of("index", "*", "alias", readAlias, "must_exist", false)),
            Map.of("remove", Map.of("index", "*", "alias", writeAlias, "must_exist", false)),
            Map.of("add", Map.of("index", safeName(index), "alias", readAlias)),
            Map.of(
                "add",
                Map.of("index", safeName(index), "alias", writeAlias, "is_write_index", true)));
    perform("POST", "/_aliases", Map.of("actions", actions));
  }

  private JsonNode perform(String method, String endpoint, Object body) {
    Request request = new Request(method, endpoint);
    if (body != null) {
      request.setJsonEntity(json(body));
    }
    try {
      Response response = client.get().performRequest(request);
      if (response.getEntity() == null) {
        return objectMapper.get().createObjectNode();
      }
      return objectMapper
          .get()
          .readTree(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
    } catch (IOException | ParseException exception) {
      throw unavailable(exception);
    }
  }

  private String json(Object value) {
    try {
      return objectMapper.get().writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Search JSON serialization failed", exception);
    }
  }

  private ResourceDocument toDocument(JsonNode source) {
    return objectMapper.get().convertValue(source, ResourceDocument.class);
  }

  private static SearchUnavailableException unavailable(Exception exception) {
    return new SearchUnavailableException("Elasticsearch is unavailable", exception);
  }

  private static String safeName(String value) {
    if (value == null || !value.matches("[a-z0-9][a-z0-9_-]{0,127}")) {
      throw new IllegalArgumentException("Elasticsearch index or alias name is invalid");
    }
    return value;
  }

  private static String safeId(String value) {
    try {
      return UUID.fromString(value).toString();
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("eventId must be a UUID", exception);
    }
  }
}

# VenueFlow Search Service

Search owns only a rebuildable Elasticsearch projection. MySQL in Resource Service remains the
source of truth.

Default verification is connection-free:

```powershell
.\mvnw.cmd -pl venueflow-search-service -am clean verify
java -jar venueflow-search-service\target\venueflow-search-service-0.1.0-SNAPSHOT.jar
```

Use `SPRING_PROFILES_ACTIVE=search` only after RabbitMQ, Elasticsearch 9.2.8, and Resource Service
are configured. Public search is `GET /api/v1/search/resources`; full rebuild is the un-routed
operator endpoint `POST /api/v1/admin/search/rebuild`.

An ES outage returns `SEARCH_UNAVAILABLE`. It never becomes an empty successful result, and
Resource writes remain available because publication uses the Resource transactional Outbox.

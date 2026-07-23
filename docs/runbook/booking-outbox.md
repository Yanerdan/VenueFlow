# Booking Outbox Runbook

## 启停边界

仅启用 `persistence` 时，Booking 会记录 `NEW` 事件但不连接 RabbitMQ。只有同时启用
`persistence,messaging` 并提供全部 `VENUEFLOW_RABBITMQ_*` 凭据后才发布。生产启用前，
消费者必须先创建匹配 `booking.reservation.#` 的队列绑定；Booking 只拥有持久 exchange
`venueflow.events.v1`。

停用时先设置 `VENUEFLOW_OUTBOX_ENABLED=false` 或移除 `messaging`。不得删除 V002 或
Outbox 数据。

## 安全检查

命令只输出元数据，不输出 payload、headers、凭据、URL 或异常正文：

```powershell
java -jar venueflow-booking-service/target/venueflow-booking-service-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=persistence --spring.main.web-application-type=none `
  --venueflow.outbox.admin.action=inspect `
  --venueflow.outbox.admin.event-id=<event-uuid>
```

## 重排 DEAD 事件

必须先预览，再确认；两步都要求事件 ID 和操作原因。确认不会改变 event ID、payload、
headers、创建时间或历史 retry_count。

```powershell
# preview
java -jar venueflow-booking-service/target/venueflow-booking-service-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=persistence --spring.main.web-application-type=none `
  --venueflow.outbox.admin.action=requeue `
  --venueflow.outbox.admin.event-id=<event-uuid> `
  --venueflow.outbox.admin.reason="<ticket-or-incident>"

# confirm
java -jar venueflow-booking-service/target/venueflow-booking-service-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=persistence --spring.main.web-application-type=none `
  --venueflow.outbox.admin.action=requeue `
  --venueflow.outbox.admin.event-id=<event-uuid> `
  --venueflow.outbox.admin.reason="<ticket-or-incident>" `
  --venueflow.outbox.admin.confirm=true
```

## 故障处理

- backlog/oldest age 持续增长：保持 API 运行在 `persistence`，检查 broker 和消费者绑定。
- `UNROUTABLE`：先恢复消费者队列绑定，再重排终态事件。
- `CONFIRM_TIMEOUT`、`CONFIRM_NACK`、`BROKER_UNAVAILABLE`：检查 RabbitMQ 健康与凭据；
  瞬时故障由有界重试处理。
- `DEAD`：检查安全元数据，修复原因，预览后显式确认重排。

内部指标包括 backlog、oldest eligible age、claimed、confirmed、returned、retry、dead
和 failure outcome；Actuator 暴露范围保持受限。

发布语义是至少一次。进程可能在 broker ACK 后、MySQL 标记前停止，从而再次发布相同
event ID。后续消费者必须按 event ID 去重；C11 不承诺 exactly-once。

## 验证

```powershell
.\mvnw.cmd -pl venueflow-booking-service -am clean verify
.\mvnw.cmd -pl venueflow-booking-service verify -Pmysql-it
.\mvnw.cmd -pl venueflow-booking-service verify -Poutbox-it
```

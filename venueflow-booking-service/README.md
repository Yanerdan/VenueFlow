# VenueFlow Booking Service

默认 `skeleton` profile 仅提供端口 8084 和受限健康探针，不连接外部服务。显式
`persistence` profile 使用 Booking 自有 MySQL schema，并通过有界 Java HTTP 客户端读取
User 预约资格、调用 Resource 容量台账。

## 验证

```powershell
.\mvnw.cmd -pl venueflow-booking-service -am clean verify
.\mvnw.cmd -pl venueflow-booking-service -am verify -Pmysql-it
```

默认命令不需要 Docker。`mysql-it` 使用隔离 MySQL 验证 V001、幂等创建、重放、查询和取消。

## Persistence 变量

```text
VENUEFLOW_BOOKING_DB_URL
VENUEFLOW_BOOKING_DB_USERNAME
VENUEFLOW_BOOKING_DB_PASSWORD
VENUEFLOW_USER_SERVICE_BASE_URL
VENUEFLOW_RESOURCE_SERVICE_BASE_URL
VENUEFLOW_COLLABORATOR_CONNECT_TIMEOUT_MS
VENUEFLOW_COLLABORATOR_REQUEST_TIMEOUT_MS
VENUEFLOW_RESOURCE_LOOKUP_ATTEMPTS
```

创建预约使用 `Idempotency-Key` Header。Resource 写请求不会自动重试；分配响应超时后按
operationId 查询结果。当前版本只有 `CONFIRMED -> CANCELLED`，不包含认证、消息或超时任务。

操作与补偿检查见 [Booking 预约 Runbook](../docs/runbook/booking-reservation.md)。

## C11 Booking Outbox

`persistence` 会在 MySQL 事务中追加确认/取消事件，但不连接 RabbitMQ。只有显式启用
`persistence,messaging` 并提供 `VENUEFLOW_RABBITMQ_*` 变量后才会启动有界发布器。
Booking 只声明持久 topic exchange `venueflow.events.v1`，队列和绑定归消费者所有。

```powershell
.\mvnw.cmd -pl venueflow-booking-service -am clean verify
.\mvnw.cmd -pl venueflow-booking-service verify -Pmysql-it
.\mvnw.cmd -pl venueflow-booking-service verify -Poutbox-it
```

启停、检查、DEAD 事件重排及故障处理见
[Booking Outbox Runbook](../docs/runbook/booking-outbox.md)。

## C14 Booking capacity reconciliation

`persistence,reconciliation` adds an opt-in leased worker for durable allocation/cancellation
recovery intents. Scheduling remains off unless `VENUEFLOW_RECONCILIATION_ENABLED=true`.
Operator `PREVIEW` is read-only; `RUN` requires a bounded reason and explicit confirmation.

```powershell
.\mvnw.cmd -pl venueflow-booking-service test
.\mvnw.cmd -pl venueflow-booking-service -Preconciliation-it verify
```

Configuration, issue handling, shutdown, and rollback are documented in the
[Booking reconciliation runbook](../docs/runbook/booking-capacity-reconciliation.md).

## C15 pending confirmation and expiration

Creation now returns `PENDING_CONFIRMATION` with `expireAt`. Confirm with
`POST /api/v1/bookings/{bookingNo}/confirmation`; cancellation accepts pending or confirmed
reservations. The opt-in `persistence,expiration` runtime safely releases overdue holds before
committing `EXPIRED`. Scheduling remains disabled unless `VENUEFLOW_EXPIRATION_ENABLED=true`.
See the [expiration runbook](../docs/runbook/booking-timeout-expiration.md).

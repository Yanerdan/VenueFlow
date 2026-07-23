# VenueFlow Notification Service

`venueflow-notification-service` 是独立可执行的 Notification Service 骨架。C12 仅建立
服务边界，不消费 Booking 事件，也不发送站内信或邮件。

## 启动

默认 `skeleton` profile 监听 `8085`，无需 Docker、MySQL、RabbitMQ 或其他服务：

```powershell
.\mvnw.cmd -pl venueflow-notification-service -am clean package
java -jar venueflow-notification-service\target\venueflow-notification-service-0.1.0-SNAPSHOT.jar
```

端口冲突时可在当前终端覆盖：

```powershell
$env:SERVER_PORT = "18085"
java -jar venueflow-notification-service\target\venueflow-notification-service-0.1.0-SNAPSHOT.jar
```

## 健康探针

```text
GET http://127.0.0.1:8085/actuator/health/liveness
GET http://127.0.0.1:8085/actuator/health/readiness
```

两项探针应返回 `UP`。Actuator Web 不暴露 `env`、`configprops`、`loggers`、`mappings`
或 `metrics`。

## 验证

默认验证完全不使用 Docker 或外部基础设施：

```powershell
.\mvnw.cmd -pl venueflow-notification-service -am clean verify
```

验证覆盖应用上下文、配置和依赖边界、HTTP 探针以及实际可执行 JAR 启停。

## 当前边界

C12 不包含数据库、Migration、AMQP listener、队列或绑定、`ConsumedEvent`、手动 ACK、
重试/DLQ、通知记录、邮件模拟、跨服务调用或 Compose 应用容器。C11 Booking Outbox
仍只负责可靠发布；上述消费端能力将在 C13 单独实现。

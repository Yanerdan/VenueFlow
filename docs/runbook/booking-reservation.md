# Booking 预约 Runbook

## 启动

显式创建 `venueflow_booking` schema 和最小权限用户，在未提交的本地环境中设置
`VENUEFLOW_BOOKING_DB_*`、User/Resource base URL，然后使用
`SPRING_PROFILES_ACTIVE=persistence` 启动 Booking Service。缺少任一必需变量时启动失败是
预期行为。

## 验证

默认验证不启动 Docker：

```powershell
.\mvnw.cmd clean verify
```

显式 MySQL 集成验证使用 Testcontainers 创建隔离数据库：

```powershell
.\mvnw.cmd -pl venueflow-resource-service -am clean verify -Pmysql-it
.\mvnw.cmd -pl venueflow-booking-service -am clean verify -Pmysql-it
```

## 不确定的容量分配

Booking 不重试容量写请求。写响应超时后查询：

```text
GET /api/v1/resource-slots/{slotId}/allocation-operations/{operationId}
```

返回 `BOOKING_ALLOCATION_OUTCOME_UNKNOWN` 时，使用日志中的 requestId、slotId 和 operationId
查询 Resource。确认占用存在但 Booking 不存在时，以确定性的 release operationId 执行一次
幂等释放。

## 补偿失败

`BOOKING_COMPENSATION_REQUIRED` 表示 Booking 本地落库失败且释放未确认。禁止重复分配；先查询
Resource operation，再按确定事实执行一次幂等释放并记录人工处理结果。C10 尚无后台对账，
后续 Outbox/对账 Change 才提供自动恢复。

## 安全

不得在工单、日志或提交中记录数据库密码、完整环境文件、JDBC 凭据或原始下游响应。

# VenueFlow Auth Service

`venueflow-auth-service` 是独立可执行的 Auth Service 骨架。C17 只建立服务边界，不提供
登录、密码、Token、JWT 或授权能力。

## 启动

默认 `skeleton` profile 监听 `8081`，无需 Docker、数据库、密钥或其他服务：

```powershell
.\mvnw.cmd -pl venueflow-auth-service -am clean package
java -jar venueflow-auth-service\target\venueflow-auth-service-0.1.0-SNAPSHOT.jar
```

端口冲突时可设置 `$env:SERVER_PORT = "18081"`。

## 健康探针

```text
GET http://127.0.0.1:8081/actuator/health/liveness
GET http://127.0.0.1:8081/actuator/health/readiness
```

两项探针应返回 `UP`，其他 Actuator Web 端点不公开。

## 验证

```powershell
.\mvnw.cmd -pl venueflow-auth-service -am clean verify
```

默认验证完全不使用 Docker 或外部基础设施。

## 当前边界

C17 不包含凭据表、Migration、密码哈希、登录/退出 API、Access/Refresh Token、JWT
密钥、角色、锁定、撤销、Gateway、User 调用、Nacos、Redis、消息或应用容器。

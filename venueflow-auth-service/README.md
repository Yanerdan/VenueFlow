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

C17 只建立了骨架。C18 在显式 `persistence` profile 下增加 Auth 自有 MySQL V001、
BCrypt 凭据、失败锁定、RS256 Access JWT、单次轮换 Refresh Token，以及注册、登录、
刷新和退出 API；默认 `skeleton` 行为不变。

## C18 persistence 启动

在未提交的环境中设置 `VENUEFLOW_AUTH_DB_URL/USERNAME/PASSWORD`、PKCS#8
`JWT_PRIVATE_KEY` 和 X.509 `JWT_PUBLIC_KEY`，然后：

```powershell
$env:SPRING_PROFILES_ACTIVE = "persistence"
java -jar target\venueflow-auth-service-0.1.0-SNAPSHOT.jar
```

API：

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

真实 MySQL 验证：`.\mvnw.cmd -pl venueflow-auth-service verify -Pauth-it`。操作与密钥说明
见 [Auth runbook](../docs/runbook/auth-credential-token-lifecycle.md)。

C18 不包含角色、跨服务授权、Gateway、下游 JWT Filter、找回密码、MFA、邮件、Redis、
Nacos/Feign、消息或应用容器。

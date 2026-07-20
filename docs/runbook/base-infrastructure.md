# Base Infrastructure Runbook

本 Runbook 操作 VenueFlow C02 的单节点开发基础设施：MySQL、Redis、RabbitMQ management 和 Nacos standalone。它适用于本机 Docker 或 VMware Ubuntu infra-node，不代表生产高可用、备份或容灾方案。

## 1. 前置条件与版本

- Docker Engine/Desktop 可用，Docker Compose v2 或更高。
- 至少为 base profile 预留 4GB 内存；16GB 宿主机不要同时常驻 Elasticsearch、完整观测栈和全部 Java 服务。
- 镜像版本事实位于 `deploy/versions.env`：MySQL 8.4.10、Redis 7.4.9 bookworm、RabbitMQ 4.1.8 management、Nacos 3.1.1。
- 镜像标签已在 2026-07-20 验证 Linux amd64/arm64 manifest，并在 Windows Docker Desktop Linux/amd64 实际启动。

## 2. 本地配置

复制示例，不要提交 `.env`：

```powershell
Copy-Item .env.example .env
```

至少替换 MySQL、Redis、RabbitMQ 和 Nacos 的认证占位符。Nacos token 必须是长度足够的 Base64 值。示例默认 `INFRA_BIND_ADDRESS=127.0.0.1`，不会监听所有宿主机接口。

标准宿主端口是 3306、6379、5672、15672、8848、9848、9849。若本机已有服务，修改 `.env` 中对应的 `*_PORT`，不要停止未知进程。容器内部仍使用标准端口。

在 VMware infra-node 上，把 `INFRA_BIND_ADDRESS` 改为该机器的 Host-only IP，并同步宿主防火墙；不得改为 `0.0.0.0`。应用连接地址使用 infra-node Host-only IP 和所选宿主端口。

## 3. 启动前检查

Windows PowerShell：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/bootstrap/validate-base-infrastructure.ps1 -EnvFile .env
```

Linux、macOS 或 Git Bash：

```bash
sh scripts/bootstrap/validate-base-infrastructure.sh .env deploy/versions.env
```

检查会拒绝缺失/占位 secret、`latest` 或浮动镜像、通配地址、非法端口、错误服务集合、缺失健康检查/资源边界和自动删卷命令。它不会输出 secret 值。

## 4. 启动与 smoke

Windows：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-test/base-infrastructure-smoke.ps1 -EnvFile .env -TimeoutSeconds 300
```

POSIX：

```bash
TIMEOUT_SECONDS=300 sh scripts/smoke-test/base-infrastructure-smoke.sh .env
```

脚本会收敛到固定 project `venueflow-base`，启动显式 `base` profile，等待四个容器 healthy，再执行：

- MySQL：认证后的 `SELECT 1`；
- Redis：认证后的 `PING`；
- RabbitMQ：`rabbitmq-diagnostics check_running`；
- Nacos：容器内部 console 8080 的 `/v3/console/health/liveness`。

Nacos 3.1.1 的对外服务 API 仍是 8848；console 8080 仅用于容器内健康检查，本 Compose 不发布该端口。

## 5. 状态与资源

```powershell
docker compose --env-file deploy/versions.env --env-file .env -f deploy/compose/compose.yml --profile base ps
docker stats --no-stream venueflow-base-mysql-1 venueflow-base-redis-1 venueflow-base-rabbitmq-1 venueflow-base-nacos-1
```

2026-07-20 在 Windows Docker Desktop Linux/amd64、四组件空闲状态的一次采样：MySQL 458 MiB、Redis 6 MiB、RabbitMQ 99 MiB、Nacos 819 MiB，合计约 1.35 GiB。该值仅是环境记录，不是性能承诺；Compose 配置上限合计约 3.63 GiB。

## 6. 正常停止与重启

正常停止会删除容器和 Compose 网络，但保留命名卷：

```powershell
docker compose --env-file deploy/versions.env --env-file .env -f deploy/compose/compose.yml --profile base down --timeout 30
```

再次运行 smoke 会复用：

```text
venueflow-base_mysql-data
venueflow-base_redis-data
venueflow-base_rabbitmq-data
venueflow-base_nacos-data
```

已验证 `down`/重启前后四个卷的创建时间不变。**不要在普通停止或自动化中使用 `down --volumes`、`down -v` 或批量删卷。** 若确需清除测试数据，先停止并明确列出上述 project/卷目标，再由操作者二次确认后单独处理。

## 7. 故障排查

### 镜像拉取超时

先确认精确标签存在，不要回退到 `latest`。若 Docker Desktop 的 registry mirror 超时，可在不修改 daemon 配置的前提下测试官方 `registry-1.docker.io`；仍失败时保留错误并稍后重试。

### 端口被占用

检查现有监听进程，然后在 `.env` 修改对应宿主 `*_PORT`。不要终止未知数据库、Redis 或代理进程。静态检查会确保端口仍绑定在 `INFRA_BIND_ADDRESS`。

### 容器长期 starting/unhealthy

```powershell
docker compose --env-file deploy/versions.env --env-file .env -f deploy/compose/compose.yml --profile base ps
docker compose --env-file deploy/versions.env --env-file .env -f deploy/compose/compose.yml --profile base logs --tail 80 nacos
```

smoke 使用全局超时，失败时输出有界状态和日志，不无限重试、不自动删卷。认证错误、端口冲突和镜像错误应先修正配置再重试。

### Nacos 健康端点

Nacos 3.1.1 的旧 `/nacos/v1/console/health/liveness` 返回 410，新 console 健康端点位于内部 `http://127.0.0.1:8080/v3/console/health/liveness`。不要为了兼容旧探针重新开启已弃用 API。

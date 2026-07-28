# VenueFlow 校园资源预约平台

VenueFlow 是面向学校部委、学院和职能部门的校园空间统一预约系统。它覆盖资源发布、开放时段、师生申请、管理审批、签到核销、消息通知和搜索，并提供申请人端与学校管理端两个浏览器工作区。

## 当前能力

- 申请人端：注册登录、校园身份资料维护、资源检索、完整用途申请、提交/撤回、进度与审批意见查询。
- 管理端：运营总览与报表、申请详情、带意见审批/驳回、人员目录、签到核销、资源发布和开放时段管理。
- 后端：Gateway、Auth、User、Resource、Booking、Notification、Search 七个独立服务。
- 可靠性：预约幂等、容量台账、Outbox、消费去重、超时释放、补偿协调、缓存与搜索投影。
- 工程化：Maven Wrapper、Flyway、Docker 本地基础设施、质量门禁、健康探针和可选可观测性。

本阶段定位为可完整演示的校园管理产品。正式校园 SSO、组织同步、可配置多级审批、压测、安全加固和发布材料留待后续建设。

## 最快启动

环境要求：Windows PowerShell、JDK 21、Docker Desktop 和 OpenSSL。

```powershell
.\scripts\local-dev\start.ps1
python -m http.server 3000 --directory venueflow-web
```

打开：

- 申请人端：<http://127.0.0.1:3000/>
- 管理端：<http://127.0.0.1:3000/admin.html>
- Gateway：<http://127.0.0.1:8080>

本地脚本会创建演示管理员：

```text
用户名：campus.admin
密码：Campus-Admin-2026!
```

该凭据只用于本地演示，不应部署到共享或生产环境。普通用户可在申请人端自行注册。

查看状态、执行全链路验收和停止服务：

```powershell
.\scripts\local-dev\status.ps1
.\scripts\local-dev\smoke.ps1
.\scripts\local-dev\stop.ps1
```

## 手工验收路径

1. 在申请人端注册账号，填写校园身份，选择资源与时段，并填写活动名称、用途和联系人后提交。
2. 使用演示管理员登录，在“申请审批”打开详情，填写审批意见后通过或填写原因后驳回。
3. 回到申请人端刷新“我的申请”，确认状态变为“审批通过”。
4. 管理员在允许的签到窗口执行“签到核销”，确认申请进入“已核销”。
5. 在“资源管理”和“开放时段”中新建资源、发布资源并配置开放时间。

完整说明见 [校园管理端运行手册](docs/runbook/campus-administration.md)。

资源管理支持配置归属部门和指定审批人。新预约会保存责任快照，普通审批人只接收分配给自己的申请，系统管理员保留全局管理视图。

运营报表提供申请量、待审批量、通过率、参与人数、资源排行、部门分布和最近审批记录；统计范围自动遵循审批权限。

## 构建与测试

默认验证不连接 Docker 或外部服务：

```powershell
.\mvnw.cmd clean verify
node --test venueflow-web/test/*.test.js
```

只构建可执行包：

```powershell
.\mvnw.cmd -DskipTests package --no-transfer-progress
```

## 服务端口

| 服务 | 端口 | 职责 |
|---|---:|---|
| Gateway | 8080 | 路由、JWT 校验、CORS、可信身份上下文 |
| Auth | 8081 | 凭证、校园角色、Access/Refresh Token |
| User | 8082 | 用户资料与预约资格 |
| Resource | 8083 | 资源目录、时段和容量 |
| Booking | 8084 | 申请、审批、取消、超时与核销 |
| Notification | 8085 | 可靠事件消费与站内消息 |
| Search | 8086 | Elasticsearch 资源检索 |

业务联调时，各业务服务应使用 `persistence` 及其需要的附加 profile；推荐直接使用 `scripts/local-dev/start.ps1`，避免在 IDEA 中漏配数据库、消息或 JWT 环境变量。

## 目录

- `venueflow-web/`：零依赖申请人端和管理端。
- `venueflow-*-service/`：领域微服务。
- `venueflow-gateway/`：统一入口。
- `scripts/local-dev/`：本地一键启动、数据种子、验收和停止脚本。
- `deploy/`：基础设施 Compose 与版本锁。
- `docs/runbook/`：运行和故障处理手册。
- `openspec/`：主规范、变更提案和归档。

## 安全提示

密钥、数据库密码和本地运行文件位于未跟踪目录或环境变量中。不要提交 `.env`、`secrets/`、私钥、真实账号或生产凭据，也不要将本地演示管理员用于任何共享环境。

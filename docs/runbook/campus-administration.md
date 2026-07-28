# 校园管理端运行手册

## 启动

```powershell
.\scripts\local-dev\start.ps1
python -m http.server 3000 --directory venueflow-web
```

脚本会启动 MySQL、Redis、RabbitMQ、Elasticsearch、七个应用服务，执行 Flyway 迁移，写入演示资源，并创建本地管理员 `campus.admin / Campus-Admin-2026!`。

如果此前已有 `secrets/local-dev/local-dev.env`，启动脚本也会补充本地管理员配置。修改管理员配置后需要重启 Auth Service，并重新登录以获得包含新角色的 JWT。

## 工作区

- `http://127.0.0.1:3000/`：师生申请人工作区。
- `http://127.0.0.1:3000/admin.html`：学校管理工作区。

管理端使用与申请人端相同的 `sessionStorage` 会话；在同一标签页从用户端进入管理端时不需要重复登录。`APPLICANT` 不能访问管理端，`APPROVER` 可审批，`RESOURCE_MANAGER` 可维护资源，`SYSTEM_ADMIN` 拥有当前全部管理能力。

## 业务状态

| 状态 | 用户端含义 | 管理端动作 |
|---|---|---|
| `PENDING_CONFIRMATION` | 等待审批 | 通过或驳回 |
| `CONFIRMED` | 审批通过 | 签到核销或取消 |
| `COMPLETED` | 已完成使用 | 无 |
| `CANCELLED` | 已撤回/取消 | 无 |
| `EXPIRED` | 审批窗口超时 | 无 |

管理审批复用 Booking 现有可靠状态机、容量释放和 Outbox 通知，不维护第二份预约事实。

## 验收

```powershell
.\scripts\local-dev\status.ps1
.\scripts\local-dev\smoke.ps1
```

`smoke.ps1` 会创建一次性申请人、建立资料、提交申请，再以管理员查询审批队列并审批，通过搜索、通知、令牌刷新和退出完成全链路验证。

## 常见问题

- 管理端提示没有权限：退出后使用管理员重新登录；旧令牌不会自动获得新角色。
- 管理端没有资源：运行 `scripts/local-dev/seed.ps1`。
- 浏览器请求失败：确认前端通过 `127.0.0.1:3000` 访问，Gateway 为 `127.0.0.1:8080`。
- Auth 启动失败：确认 MySQL 可用，且 Auth 的数据库和 JWT 密钥环境变量齐全。
- 新建资源后用户端不可见：资源默认是草稿，需要在资源管理中点击“发布”。

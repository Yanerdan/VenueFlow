# VenueFlow Handoff

## 当前状态

- 分支：`main`
- 最新归档：C25 `add-campus-administration-foundation`
- C01-C25 均已归档，当前没有活动 OpenSpec change。
- 本地七个服务、Gateway 和前端已完成真实联调。

## 已完成产品链路

- Auth：注册、登录锁定、RS256 Access JWT、Refresh 轮换、退出、校园角色。
- Gateway：显式业务路由、JWT 校验、可信用户/角色/追踪头、CORS 与请求边界。
- User：资料与预约资格。
- Resource：分类、资源、开放时段、容量台账、缓存与搜索事件。
- Booking：幂等申请、管理员审批、取消、超时释放、补偿协调、签到核销与全局管理查询。
- Notification：Outbox 可靠投递、消费去重、重试/DLQ 与站内消息。
- Search：Elasticsearch 投影、搜索与重建。
- Web：零构建申请人端和校园管理端，覆盖资源、时段、申请、审批、资源维护和运营概览。
- Engineering：Docker 本地基础设施、Maven 质量门禁、故障脚本和可选可观测性。

## C25 交付

- Auth V002 新增 `APPLICANT`、`APPROVER`、`RESOURCE_MANAGER`、`SYSTEM_ADMIN`。
- Gateway 从已验证 JWT 传播可信 `X-Role`，并放行受认证的资源管理路由。
- Booking 提供有界、按状态筛选的全局管理查询；确认和签到要求审批角色。
- `venueflow-web/admin.html` 提供运营总览、申请审批、资源管理和开放时段。
- 用户端改为“提交申请、等待审批”，不再向普通用户显示审批与签到动作。
- 本地启动自动创建 `campus.admin / Campus-Admin-2026!`，仅供本地演示。
- README 与 `docs/runbook/campus-administration.md` 是当前验收入口。

## 最近验证

```powershell
.\mvnw.cmd -pl venueflow-auth-service -am test -DskipITs
.\mvnw.cmd -pl venueflow-gateway -am test -DskipITs
.\mvnw.cmd -pl venueflow-booking-service -am test -DskipITs
node --test venueflow-web/test/*.test.js
.\scripts\local-dev\smoke.ps1
openspec.cmd validate add-campus-administration-foundation --strict
git diff --check
```

结果：

- Auth：9 项默认测试通过。
- Gateway：6 项默认测试通过。
- Booking：51 项默认测试通过。
- Frontend：6 项测试通过。
- 本地七服务均为 `UP`，完整烟测全部 `PASS`。
- 浏览器验收覆盖申请人首页、管理总览、审批筛选、资源分类表单和开放时段，无控制台错误。
- 一次聚焦 `verify` 因运行中的 Gateway 锁定自身 JAR 而无法重新打包；停止服务后由 `start.ps1` 全量打包成功，不是代码失败。

## 下一阶段建议

应用构建的下一大步应是校园组织与可配置审批：院系/部门、资源归属、申请用途与联系人、审批意见、多级审批模板、审计视图和基础报表。正式 SSO、安全加固、压测、真实试用和发布材料仍按当前决定暂缓。

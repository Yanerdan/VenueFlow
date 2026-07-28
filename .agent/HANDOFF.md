# VenueFlow Handoff

## 当前状态

- 分支：`main`
- 最新交付：C28 `add-resource-ownership-and-scoped-approval`
- C01-C28 均已完成并归档，当前没有活动 OpenSpec change。
- 本地七个服务、Gateway 和零构建前端已完成真实持久化联调。

## 已完成产品链路

- Auth：注册、登录锁定、RS256 JWT、Refresh 轮换、退出和校园角色。
- Gateway：显式路由、JWT 校验、可信 UUID 用户身份/角色/追踪头和 CORS。
- User：校园身份资料、本人资料维护、人员目录和预约资格。
- Resource：分类、资源、开放时段、容量、资源归属部门和指定审批人。
- Booking：完整申请、责任快照、范围化审批、驳回/取消、超时、补偿和签到核销。
- Notification：Outbox 投递、消费去重、重试/DLQ 和站内消息。
- Search：Elasticsearch 投影、搜索和重建。
- Web：申请人端及管理端，覆盖身份、人员、资源归属、时段、完整申请、审批详情和运营概览。

## C28 交付

- Resource V005/V006 增加归属部门和 UUID 审批人身份，提供乐观锁更新接口。
- 单时段 DTO 返回父资源责任，Booking V007/V008 在创建预约时保存责任快照。
- `APPROVER` 的管理查询、通过、驳回和签到由 Booking 按可信用户 UUID 强制限制；`SYSTEM_ADMIN` 保留全局权限。
- 管理端资源卡片可编辑归属，审批详情展示部门和指定审批人。
- README、校园管理手册、前端测试和真实持久化冒烟脚本已更新。

## 最近验证

```powershell
.\mvnw.cmd -pl venueflow-resource-service,venueflow-booking-service -am test -DskipITs
node --test venueflow-web/test/*.test.js
powershell -ExecutionPolicy Bypass -File scripts/local-dev/smoke.ps1
openspec.cmd validate --all --strict
git diff --check
```

结果：

- Resource：74 项测试通过。
- Booking：52 项测试通过。
- Frontend：9 项测试通过。
- 七个服务均为 `UP`，真实持久化冒烟链路通过。
- 浏览器验收覆盖资源归属表单、责任回显和审批详情，控制台无错误。

## 下一阶段建议

下一大步建设基础审计与运营报表：审批操作审计查询、资源利用率、申请通过率和部门维度统计。正式 SSO、安全加固、压测、真实试用和发布材料继续暂缓。

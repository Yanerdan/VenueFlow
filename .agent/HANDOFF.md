# VenueFlow Handoff

## 当前状态

- 分支：`main`
- 最新归档：C26 `add-campus-identity-and-user-directory`
- C01-C26 均已归档，当前没有活动 OpenSpec change。
- 本地七个服务、Gateway 和前端已完成真实联调。

## 已完成产品链路

- Auth：注册、登录锁定、RS256 Access JWT、Refresh 轮换、退出、校园角色。
- Gateway：显式业务路由、JWT 校验、可信用户/角色/追踪头、CORS 与请求边界。
- User：校园身份资料、本人资料维护、人员目录与预约资格。
- Resource：分类、资源、开放时段、容量台账、缓存与搜索事件。
- Booking：幂等申请、管理员审批、取消、超时释放、补偿协调、签到核销与全局管理查询。
- Notification：Outbox 可靠投递、消费去重、重试/DLQ 与站内消息。
- Search：Elasticsearch 投影、搜索与重建。
- Web：零构建申请人端和校园管理端，覆盖校园资料、人员目录、资源、时段、申请、具名审批、资源维护和运营概览。
- Engineering：Docker 本地基础设施、Maven 质量门禁、故障脚本和可选可观测性。

## C26 交付

- User V002 增加可选唯一学工号、人员类型、院系、电话和邮箱，并兼容既有资料。
- 本人可通过可信外部身份和乐观锁版本维护校园资料。
- `APPROVER`、`SYSTEM_ADMIN` 可按姓名、学工号或院系查询有界人员目录。
- 申请人注册和个人资料中心已接入校园身份字段。
- 管理端新增人员目录，并以姓名和院系替代审批列表中的裸用户 ID。
- README、校园管理运行手册和本地烟测已同步更新。

## 最近验证

```powershell
.\mvnw.cmd -pl venueflow-user-service -am test -DskipITs
node --test venueflow-web/test/*.test.js
.\scripts\local-dev\smoke.ps1
openspec.cmd validate --all --strict
git diff --check
```

结果：

- User：73 项默认测试通过。
- Frontend：7 项测试通过。
- OpenSpec：30 个 change/spec 严格校验全部通过。
- 本地七服务均为 `UP`，资料、人员目录、资源时段、预约审批、通知和会话烟测全部 `PASS`。
- 浏览器验收覆盖校园资料注册、个人资料中心、管理端人员目录和状态展示，无控制台错误。

## 下一阶段建议

应用构建的下一大步应是申请业务表单与审批意见：申请用途、活动名称、联系人、备注、驳回原因、审批意见和申请详情视图；之后再建设资源归属、可配置多级审批、审计视图和基础报表。正式 SSO、安全加固、压测、真实试用和发布材料仍按当前决定暂缓。

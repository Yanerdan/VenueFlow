# VenueFlow Handoff

## 当前状态

- 分支：`main`
- 最新交付：C27 `add-booking-application-details-and-review-notes`
- C01-C27 均已完成并归档，当前没有活动 OpenSpec change。
- 本地七个服务、Gateway 和零构建前端已完成真实持久化联调。

## 已完成产品链路

- Auth：注册、登录锁定、RS256 Access JWT、Refresh 轮换、退出、校园角色。
- Gateway：显式业务路由、JWT 校验、可信用户/角色/追踪头、CORS 与请求边界。
- User：校园身份资料、本人资料维护、人员目录与预约资格。
- Resource：分类、资源、开放时段、容量台账、缓存与搜索事件。
- Booking：完整申请资料、管理审批意见、驳回/取消、超时释放、补偿协调、签到核销与全局查询。
- Notification：Outbox 可靠投递、消费去重、重试/DLQ 与站内消息。
- Search：Elasticsearch 投影、搜索与重建。
- Web：零构建申请人端和校园管理端，覆盖身份、人员、资源、时段、完整申请、审批详情和运营概览。
- Engineering：Docker 本地基础设施、Maven 质量门禁、故障脚本和可选可观测性。

## C27 交付

- Booking V006 增加活动名称、用途、联系人、联系电话、补充说明及审批结论字段，兼容历史预约。
- 创建请求校验并持久化完整申请资料，申请字段参与幂等哈希。
- 管理员可携带审批意见通过，或填写必填原因驳回；取消也可记录说明。
- 申请人端新增完整申请表和历史详情；管理端新增审批详情抽屉和通过/驳回操作。
- README、校园管理运行手册、前端测试和持久化冒烟脚本已同步更新。

## 最近验证

```powershell
.\mvnw.cmd -pl venueflow-booking-service -am test -DskipITs --no-transfer-progress
node --test venueflow-web/test/*.test.js
.\scripts\local-dev\smoke.ps1
openspec.cmd validate --all --strict
git diff --check
```

结果：

- Booking：51 项测试通过。
- Frontend：8 项测试通过。
- 本地七个服务均为 `UP`，完整持久化冒烟链路全部 `PASS`。
- 双角色浏览器验收完成：注册、完整申请、历史详情、管理审批详情、审批意见和状态变更均通过，控制台无错误。

## 下一阶段建议

应用构建的下一大步是资源归属与组织化审批：为资源配置归属部门和负责人，将待办限制到负责范围，并支持简化的可配置多级审批。随后建设审计视图和基础统计报表。正式 SSO、安全加固、压测、真实试用和发布材料继续按当前决策暂缓。

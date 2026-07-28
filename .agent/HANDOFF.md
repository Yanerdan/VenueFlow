# VenueFlow Handoff

## 当前状态

- 分支：`main`
- C01-C32 均已完成、同步并归档；当前没有活动 OpenSpec change。
- C32：`add-campus-booking-rules-and-final-polish`
- 本地 Gateway、Auth、User、Resource、Booking、Notification、Search 全部为 `UP`。
- 在暂缓正式 SSO、组织同步、安全加固、压测、真实试用和发布材料的约束下，当前应用构建阶段已经完成。

## 当前产品能力

- 申请人：注册登录、校园资料、资源搜索、申请须知与预约规则、时段容量、完整申请、撤回、审批进度和有序审批轨迹。
- 管理人员：角色化运营总览与报表、人员与角色、直接/两级审批、签到核销、资源归属、预约规则和开放时段。
- 预约规则：资源支持申请须知、最少提前小时数、最多提前天数和单次最长使用分钟数；Booking 在容量占用前校验。
- 审批策略：资源支持 `DIRECT` 与固定 `TWO_STAGE`；两级审批人必须不同，预约提交时保存不可变审批链快照。
- 授权体验：审批人员、资源管理员和系统管理员只显示适用的工作台入口；后端已有审批范围约束。
- 可靠性：幂等、容量台账、Outbox、消费去重、超时释放、补偿、缓存和搜索投影。

## C32 验证

```powershell
.\mvnw.cmd test
node --test venueflow-web/test/*.test.js
powershell -ExecutionPolicy Bypass -File scripts/local-dev/smoke.ps1
openspec.cmd validate --all --strict
```

- 全仓 Maven 测试通过；Resource 80 项、Booking 60 项均为零失败。
- Frontend：14 项测试通过，三个脚本语法检查通过。
- 持久化 smoke：7 个服务全部 `UP`，`BookingRules = PASS`，注册、资料、角色、预约、两级审批、轨迹、报表、搜索、通知和令牌链路全部通过。
- 浏览器：用户端正确展示申请须知与规则；系统管理员工作台正确展示规则编辑和完整角色导航。
- OpenSpec：37 个 change/spec 项严格校验通过。

## 后续范围

应用主功能已收口。后续工作均属于此前明确暂缓的上线准备：正式 SSO 与组织同步、安全审计与加固、压测、真实师生试用、部署与发布材料。若继续开发，建议先从真实 SSO/组织数据接入中选择一个独立变更，不再扩展当前通用业务模型。

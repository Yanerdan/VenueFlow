# VenueFlow Handoff

## 当前状态

- 分支：`main`
- C01-C33 均已完成、同步并归档；当前没有活动 OpenSpec change。
- C33：`add-semester-showcase-data`
- Gateway、Auth、User、Resource、Booking、Notification、Search 本地服务均可持久化运行。
- 在暂缓正式 SSO、组织同步、安全加固、压测、真实试用和发布材料的约束下，应用构建阶段已经完成。

## 当前产品能力

- 申请人：注册登录、校园资料、资源检索、申请须知与预约规则、时段容量、完整申请、撤回、审批进度、通知和审批轨迹。
- 管理人员：角色化总览与学期报表、人员目录与角色、直接/两级审批、签到核销、资源归属、预约规则和开放时段。
- 可靠性：幂等、容量台账、Outbox、消费去重、超时释放、补偿、缓存和搜索投影。
- 运营包装：16 位合成人员、10 个校园资源、72 条跨四个月预约，以及配套审批、通知和未来开放时段。
- 用户端和管理端均明确标注数据为合成演示数据，不冒充真实个人或学校运营记录。

## C33 验证

```powershell
powershell -ExecutionPolicy Bypass -File scripts/local-dev/seed.ps1
node --test venueflow-web/test/*.test.js
powershell -ExecutionPolicy Bypass -File scripts/local-dev/smoke.ps1
openspec.cmd validate --all --strict
```

- 重复播种两次后总量稳定：16 位人员、10 个资源、72 个未来时段、72 条预约、86 条审批动作、66 条通知。
- 浏览器验证：申请人资源目录、管理总览、运营报表和人员目录均正确展示学期数据。
- 报表仅使用当前有效资源排名，并优先展示学期演示审批记录。

## 后续范围

应用主功能与演示包装已经收口。后续工作属于此前明确暂缓的上线准备：正式 SSO 与组织同步、安全审计与加固、压测、真实师生试用、部署和发布材料。

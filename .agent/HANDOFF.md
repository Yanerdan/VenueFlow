# VenueFlow Handoff

## 当前状态

- 分支：`main`
- C01-C35 均已完成、同步并归档；当前没有活动 OpenSpec change。
- C35：`complete-daily-booking-operations`
- Gateway、Auth、User、Resource、Booking、Notification、Search 本地服务均可持久化运行。
- 在暂缓正式 SSO、组织同步、安全加固、压测、真实试用和发布材料的约束下，应用构建阶段已经完成。

## 当前产品能力

- 申请人：演示账号快速进入、日期/人数/分类检索、按日开放时段、申请草稿恢复、再次申请、历史筛选、审批进度、可跳转已读消息和审批轨迹。
- 管理人员：角色化总览与学期报表、人员目录与角色、直接/两级审批、签到核销、资源资料/归属/规则维护、最多 12 周周期时段和 CSV 导出。
- 可靠性：幂等、容量台账、Outbox、消费去重、超时释放、补偿、缓存和搜索投影。
- 运营包装：16 位合成人员、1 个绑定个人历史的可登录申请人、10 个校园资源、72 条跨四个月预约，以及配套审批、通知和未来开放时段。
- 用户端和管理端均明确标注数据为合成演示数据，不冒充真实个人或学校运营记录。

## C35 验证

```powershell
node --test venueflow-web/test/*.test.js
.\mvnw.cmd -pl venueflow-resource-service -am -DskipITs test spotless:check spotbugs:check
bash scripts/quality/verify-repository.sh
openspec.cmd validate --all --strict
```

- 资源服务 84 项测试、Spotless 与 SpotBugs 通过；前端 16 项测试和仓库质量策略通过。
- 浏览器验证：容量筛选、历史状态筛选、再次申请、消息已读/定位、资源编辑入口、周期时段上限和导出入口均可用，控制台无错误。
- 全仓 `clean verify` 在运行中的 Gateway JAR 文件锁处停止；这是 Windows 对运行产物的占用，不是代码或测试失败，未为验证中断现有服务。

## 后续范围

应用主功能与演示包装已经收口。后续工作属于此前明确暂缓的上线准备：正式 SSO 与组织同步、安全审计与加固、压测、真实师生试用、部署和发布材料。

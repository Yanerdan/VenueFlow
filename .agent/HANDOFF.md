# VenueFlow Handoff

## 当前状态

- 分支：`main`
- C01–C40 已完成、同步并归档；当前没有活动 OpenSpec change。
- 最近功能变更：C40 `operationalize-local-service-governance`。
- Gateway、Auth、User、Resource、Booking、Notification、Search 均可在持久化模式运行。
- 已实现标准 OIDC + PKCE、组织架构全量/增量同步、资源级 1–5 级审批与审批链快照。
- 已实现可选 `-Governance` 本地模式：鉴权 Nacos 3 幂等初始化、8 个 Data ID、
  双 Resource 实例、注册状态检查与自动恢复的故障切换验收；默认静态模式不依赖 Nacos。
- C38 已完成申请人/管理员双视角精细验收：改进搜索兜底、日期和状态筛选、消息定位、操作文案、表格分页、审批详情、资源配置折叠、无障碍语义及合成数据容量一致性。
- 当前工作重点已从功能建设转为作品包装；真实压测、安全审计、学校联调、试用和生产发布仍明确不在已完成范围内。

## 产品能力

- 申请人：注册/登录、OIDC 登录、资料维护、资源检索与收藏、规则/时段查看、申请草稿、提交/撤回/取消/复用/改期、审批轨迹、通知与日历导出。
- 管理人员：运营总览与报表、组织树和人员角色、资源资料/归属/规则、开放排期、1–5 级审批配置、审批处理、签到核销与 CSV 导出。
- 可靠性：幂等、容量台账、事务 Outbox、消费者去重、超时释放、补偿对账、缓存与搜索投影。
- 演示包装：合成组织、人员、资源、预约、审批、通知和未来排期；界面与文档均明确标注其为合成数据。

## 最近验证

```powershell
.\mvnw.cmd clean verify
node --test venueflow-web/test/*.test.js
openspec.cmd validate --all --strict
.\scripts\local-dev\smoke.ps1
```

- 当前本地 Surefire XML：278 tests，0 failures，0 errors。
- 前端：19 tests passed。
- OpenSpec：39 个主规格严格校验通过。
- 治理验收：静态与治理双模式业务 smoke 通过；双 Resource 实例摘除、Gateway 续读和
  实例恢复通过。
- 全链路 smoke：注册、资料、人员目录、资源/时段、规则、预约、报表、角色、多级审批、搜索、通知、刷新/登出、展示预约取消与容量释放均通过。
- 浏览器：申请人和系统管理员主流程、验证/筛选/空状态/分页/审批/通知跳转均已回归，控制台 0 warning / 0 error。

## 作品材料

- GitHub 首屏：`README.md`
- 架构说明：`docs/architecture/`
- 项目案例、简历文案、演示脚本和面试问答：`docs/resume/`
- 管理操作：`docs/runbook/campus-administration.md`

## 后续边界

功能和作品叙事已具备完整演示条件。若转向真实上线，应单独规划学校 IdP/组织源联调、安全审计、容量压测、备份恢复、生产编排、真实用户试用和发布治理，不能用现有合成演示结果替代这些工作。

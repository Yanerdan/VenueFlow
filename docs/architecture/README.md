# VenueFlow 架构文档

本目录描述 VenueFlow 的关键架构视图。文档重点是服务边界、状态所有权和失败语义，而不是重复接口清单。

| 文档 | 回答的问题 |
|---|---|
| [系统上下文](system-context.md) | 谁使用系统，系统依赖哪些外部主体？ |
| [容器视图](container-view.md) | 7 个进程如何分工和通信？ |
| [预约时序](booking-sequence.md) | 一次预约如何避免重复与容量超卖？ |
| [事件流](event-flow.md) | 通知和搜索如何实现最终一致性？ |
| [数据模型](data-model.md) | 哪个服务拥有哪类数据？ |
| [部署视图](deployment.md) | 本地演示环境如何组成，生产化还缺什么？ |

设计决策记录位于 [`docs/adr`](../adr/)，运行和故障处置位于 [`docs/runbook`](../runbook/)。

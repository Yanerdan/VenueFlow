## Context

仓库刚完成 OpenSpec 初始化，尚无应用代码和已归档规格。宿主机为 16GB Windows 10，已安装 JDK 17、Docker Desktop 和 VMware Workstation；Docker 引擎当前未运行，现有 VMware 虚拟机为 CentOS 7、4GB、约 20GB 且仅 NAT。规范目标环境则是 JDK 21、Ubuntu 24.04、Docker Compose 和 NAT + Host-only。

本次仅修订工程规范和对应 OpenSpec 工件，不改变主机软件、VMware 虚拟机、Git 分支或运行数据。

## Goals / Non-Goals

**Goals:**

- 使版本矩阵内部兼容，并保留 Boot 4 主线。
- 让16GB本机与 VMware 拓扑能够按明确运行模式执行。
- 让预约幂等和容量占用具有可验证、可对账的最小数据基础。
- 消除首次 Change、v0.1.0 路线和文档维护规则之间的矛盾。

**Non-Goals:**

- 不安装 JDK 21，不启动 Docker，不创建或修改 VMware 虚拟机。
- 不生成 Maven 工程、服务代码、数据库 Migration 或 Compose 文件。
- 不决定 Elasticsearch 9.2 的最终补丁和镜像 digest；该值在首次 search Change 中通过依赖解析和冒烟测试锁定。

## Decisions

1. **保留 Boot 4 主线，将 ES 改为 9.2.x。** Spring Data 2025.1 对应 Elasticsearch 9.2.2 基线。相比整体回退 Boot 3.5，调整尚未落地的 ES 成本更低。具体补丁在 search Change 中锁定，避免在未解析依赖树前制造第二个错误版本。

2. **采用运行模式而非宣称16GB可全量常驻。** `dev-lite` 只运行最小依赖；`integration` 选择一个 Docker 后端；`demo-drill` 按场景开启搜索或观测组件。Docker Desktop 与 VMware 不作为默认同时运行的两个容器后端。

3. **VMware地址参数化。** 文档提供当前 VMnet1 的 `192.168.72.0/24` 示例，但以环境变量为事实来源，不强制修改宿主机现有 VMnet。

4. **幂等先取得请求所有权，再调用 Resource。** Booking 通过带请求指纹的唯一幂等记录确定单一执行者；Resource 通过 `operation_id` 和容量占用明细确保占用、查询和释放幂等。

5. **v0.1.0 是里程碑，不等于单个 Change。** 该里程碑依次包含工程基线、基础设施和 Resource Service 骨架三个 Change。

6. **允许一次性 pre-bootstrap 例外。** 首次 Git/OpenSpec 基线尚不存在时，允许本 Change 修订规范；完成基线后恢复“规范修改必须走 Change”的常规规则。

## Risks / Trade-offs

- [ES 9.2.x 的具体补丁尚未锁定] → 在引入 Search Service 前解析 Spring Boot 依赖树并执行连接冒烟测试，再写入版本锁文件。
- [容量占用明细增加表和状态管理复杂度] → 这是证明幂等释放和跨服务对账所需的最小复杂度，不在本 Change 中实现。
- [当前 OpenSpec Change 发生在首次 Git 提交前] → 在规范中明确这是一次性引导例外，并保留完整 Change 工件。
- [本机实际路径已从中文目录改为 `C:\Users\yxz\Documents\VenueFlow`] → 规范不记录绝对本机路径，避免再次绑定个人环境。

## Migration Plan

1. 更新规范版本和相应章节。
2. 运行 OpenSpec validate，检查 Change 工件结构。
3. 通过文本一致性检查确认旧 ES 版本、硬编码地址、错误状态名和冲突路线均已处理。
4. 若验证失败，回退本 Change 对规范文件的文本修改；本次没有数据库或运行环境迁移。

## Open Questions

- Elasticsearch 9.2.x 的最终补丁和镜像 digest 由后续 Search Change 确认。
- 新 Ubuntu infra-node 的最终内存和磁盘配置由基础设施 Change 根据当时可用内存与磁盘实测确认。

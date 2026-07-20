## ADDED Requirements

### Requirement: Compatible technology baseline
工程规范 MUST 使用内部兼容的 Spring 与 Elasticsearch 版本组合，并在引入 Search Service 前锁定具体 Elasticsearch 补丁版本和镜像 digest。

#### Scenario: Resolve search dependency baseline
- **WHEN** Search Service Change 开始设计
- **THEN** 依赖树和连接冒烟测试证明 Spring Data 客户端与 Elasticsearch Server 兼容后，具体版本才写入锁文件

### Requirement: Executable 16GB environment profile
工程规范 MUST 为16GB宿主机定义分场景运行模式，并禁止把 Docker Desktop 与 VMware 双后端同时全量运行作为默认条件。

#### Scenario: Run lightweight development
- **WHEN** 开发者在16GB宿主机执行日常开发
- **THEN** 仅启动当前任务所需的 Java 服务和基础设施 profile，搜索与观测组件默认关闭

### Requirement: Parameterized VMware network
工程规范 MUST 通过环境参数表达 Host-only 网段和节点地址，不得把 `192.168.80.0/24` 作为所有机器的固定事实。

#### Scenario: Use existing VMnet1 network
- **WHEN** 宿主机 VMnet1 使用 `192.168.72.0/24`
- **THEN** infra-node 可通过参数配置为该网段中的稳定地址，而无需先修改宿主机网段

### Requirement: Verifiable booking idempotency
Booking MUST 在调用 Resource 前原子取得幂等请求的执行权，幂等键 MUST 具有用户、操作类型和请求指纹作用域；Resource MUST 使用唯一操作 ID 记录可查询、可释放和可对账的容量占用。

#### Scenario: Concurrent duplicate booking requests
- **WHEN** 相同用户以相同幂等键和相同请求内容并发提交预约
- **THEN** 只有一个执行者调用容量占用，所有请求最终返回同一预约结果且容量只扣减一次

#### Scenario: Reuse key with different payload
- **WHEN** 相同用户以既有幂等键提交不同请求内容
- **THEN** 系统返回幂等键冲突且不得再次占用容量

### Requirement: Consistent staged bootstrap
v0.1.0 MUST 被定义为由工程基线、基础设施和 Resource Service 骨架三个有序 Change 组成的里程碑，首次 Change 不得提前创建基础设施或业务服务。

#### Scenario: Start a new repository
- **WHEN** 仓库开始执行 v0.1.0
- **THEN** 首先完成 `bootstrap-engineering-baseline`，之后才依次进行基础设施和 Resource Service Skeleton Change

### Requirement: One-time pre-bootstrap governance exception
在首个 Git/OpenSpec 基线建立前，规范 MUST 允许一次可审计的 pre-bootstrap 修订；基线建立后，后续规范修改 MUST 通过 OpenSpec Change。

#### Scenario: Revise the initial uncommitted specification
- **WHEN** OpenSpec 已初始化但仓库尚无首次提交且规范存在阻断性冲突
- **THEN** 可以通过本 Change 修订规范并保留 proposal、spec、design 和 tasks 工件

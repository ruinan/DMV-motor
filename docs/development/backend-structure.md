# Spring Boot 后端结构

## 1. 目标

这份文档定义 `apps/api` 的 Spring Boot 后端结构，  
服务于 MVP 阶段的代码落地。

它回答这些问题：

- 后端代码按什么模块组织
- 每个模块内部按什么分层组织
- controller、application、domain、infrastructure 各自做什么
- DTO、Entity、Domain Model 是否混用
- 哪些地方应该做事务边界

这份文档不定义：

- HTTP 字段细节
- SQL 字段类型
- 具体鉴权厂商
- 前端页面实现

## 2. 总体原则

- 整体形态采用模块化单体
- 前后端分离，后端只暴露 API 和内部调度能力
- 按业务模块拆包，不按技术类型先拆全局大包
- 规则判断放在业务模块内部，不散落在 controller 或数据库访问层
- 同步主链路和异步收敛链路分开表达

## 3. 推荐顶层目录

当前推荐 `apps/api` 使用如下结构：

```text
apps/api/
  pom.xml
  src/
    main/
      java/
        com/dmvmotor/api/
          ApiApplication.java
          common/
          authaccess/
          content/
          practice/
          mistakereview/
          mockexam/
          progressreadiness/
          reminder/
          aisupport/
          memoryexport/
      resources/
        application.yml
        db/
          migration/
    test/
      java/
```

**当前仓库现状：**

仓库根目录目前是一个 Maven archetype 骨架，`pom.xml` 的 `groupId` 为 `org.example`，代码位于根目录的 `src/` 下，尚未按上述结构整理。

进入代码实现前需要：

1. 按 `tech-stack-decision.md` 第 8 节整理目录结构，将代码移入 `apps/api/`
2. 将 `pom.xml` 的 `groupId` 改为 `com.dmvmotor`，`artifactId` 改为 `api`
3. 包根路径统一使用 `com.dmvmotor.api`

## 4. 顶层包职责

### `common`

只放跨模块共用但不承载具体业务规则的内容：

- 通用异常
- 通用响应模型
- 时间、ID、分页等基础工具
- 统一配置
- 数据库基础配置
- 安全上下文基础抽象

不应该放：

- 具体业务规则
- 某个模块私有 DTO
- 某个模块专属 service

### 业务模块包

当前按这些模块组织：

- `authaccess`
- `content`
- `practice`
- `mistakereview`
- `mockexam`
- `progressreadiness`
- `reminder`
- `aisupport`
- `memoryexport`

这和已有技术架构文档保持一致。

## 5. 每个业务模块内部的分层

每个模块内部统一采用下面结构：

```text
<module>/
  controller/
  application/
  domain/
  infrastructure/
```

必要时可以补：

- `job/`
- `scheduler/`
- `listener/`

但不要一开始就过度铺满。

## 6. 各层职责

### `controller`

负责：

- 接收 HTTP 请求
- 参数校验
- 调 application 层用例
- 返回响应 DTO

不负责：

- 业务规则判断
- 跨表事务编排
- 直接写 SQL

### `application`

负责：

- 编排一个完整用例
- 组织事务边界
- 调用 domain service / repository
- 组合多个模块允许暴露的能力

适合放在这里的例子：

- 开始一次 practice session
- 提交一道练习题
- 开始一次 mock exam
- 完成 review task

### `domain`

负责：

- 业务核心规则
- 聚合状态变化
- 领域对象表达
- 不依赖 Web 或数据库细节的判断逻辑

适合放在这里的例子：

- access pass 是否有效
- mock quota 是否允许消耗
- mistake record 如何激活 / 失活
- review task 状态流转是否合法

### `infrastructure`

负责：

- repository 实现
- SQL / jOOQ / MyBatis mapper
- 第三方服务接入
- 持久化 entity
- 外部 client

不负责：

- 最终业务规则拍板
- 把 controller 请求直接串到底层表操作

## 7. DTO、Entity、Domain Model 的边界

当前建议明确分开，不混用。

### Request / Response DTO

只存在于 `controller` 层附近，用于：

- 入参接收
- 出参返回

### Domain Model

存在于 `domain` 层，用于表达：

- User
- AccessPass
- PracticeSession
- MistakeRecord
- ReviewTask
- MockAttempt

它不应该直接等同数据库表对象。

### Persistence Entity / Record

存在于 `infrastructure` 层，用于：

- 数据库存取
- ORM / SQL 映射

不建议让 controller 直接返回数据库 entity。

## 8. 推荐的模块协作方式

模块之间只允许通过 application 暴露的用例接口或 domain-facing port 协作，  
不建议跨模块直接访问对方数据库实现细节。

例如：

- `practice` 可以调用 `mistakereview` 的“记录答错输入”能力
- `mockexam` 可以调用 `progressreadiness` 的“刷新快照”能力
- `authaccess` 可以提供“当前访问状态判断”能力给其他模块

不建议：

- `practice` 直接改 `mockexam` 的表
- `mockexam` 直接依赖 `practice` 的 repository 实现

## 9. 事务边界建议

事务应该放在 `application` 层，而不是 controller 层。

### 需要强事务的一类链路

- 创建 access pass
- 开始 mock exam 并扣减 quota
- 提交 practice answer 并写入 attempt
- 完成 review task 并更新任务状态

### 不要强塞进同步事务的一类链路

- readiness snapshot 刷新
- progress snapshot 刷新
- reminder task 生成
- AI 文案生成
- memory export 生成

这些更适合异步收敛。

## 10. 同步链路和异步链路

### 同步链路

当前应该同步完成的链路：

- 登录鉴权后的访问校验
- 拉题
- 提交单题答案并返回结果
- mock 开始时 quota 消耗与 attempt 创建
- mock 交卷并返回真实分数
- 查询当前 review pack

### 异步链路

当前建议异步收敛的链路：

- review task 刷新
- review pack 刷新
- progress snapshot 刷新
- readiness snapshot 刷新
- reminder task 生成
- AI 解释和建议生成
- memory export 生成

MVP 阶段即使暂时没有消息队列，也可以先做成：

- 事务提交后触发的后台任务
- 定时补偿任务

## 11. 数据访问建议

当前更推荐：

- 复杂读写用 `jOOQ`
- 或者显式 SQL 的 `MyBatis`

不推荐一开始把核心业务完全压在自动 ORM 映射上。

原因：

- 这个项目规则和查询都偏明确
- 表关系清晰
- 很多链路需要控制 SQL 和事务边界

## 12. 示例包结构

以 `practice` 模块为例：

```text
practice/
  controller/
    PracticeController.java
    dto/
  application/
    StartPracticeUseCase.java
    SubmitPracticeAnswerUseCase.java
    ResumePracticeUseCase.java
  domain/
    PracticeSession.java
    PracticeAttempt.java
    PracticeDomainService.java
    PracticeRepository.java
  infrastructure/
    persistence/
      PracticeSessionRecord.java
      PracticeAttemptRecord.java
      PracticeRepositoryJooq.java
```

以 `mockexam` 模块为例：

```text
mockexam/
  controller/
    MockExamController.java
    dto/
  application/
    StartMockExamUseCase.java
    SubmitMockExamUseCase.java
    ExitMockExamUseCase.java
  domain/
    MockAttempt.java
    MockQuotaPolicy.java
    MockExamRepository.java
  infrastructure/
    persistence/
    client/
```

## 13. 测试结构建议

当前建议：

- domain 层做单元测试
- application 层做集成测试
- controller 层做少量 API 层验证

必须优先覆盖的规则：

- access pass 有效期判断
- mock quota 扣减时机
- mock 退出即结束
- practice 提交结果写入
- mistake record 激活 / 更新
- review task 状态流转
- readiness 不等于单次高分

## 14. 当前不建议做的结构

当前不建议：

- 一开始拆成多服务仓库
- 先搭 CQRS / Event Sourcing
- 把所有类都抽成接口
- 建全局 `service`, `repository`, `entity`, `controller` 大平铺目录

这些做法会让 MVP 阶段复杂度显著上升。

## 15. 当前结论

Spring Boot 后端当前应采用：

- 模块化单体
- 按业务模块拆包
- 模块内采用 `controller / application / domain / infrastructure`
- 事务边界放在 application 层
- DTO、Domain Model、Persistence Entity 明确分开
- 同步链路只保留主闭环，复杂收敛逻辑走异步

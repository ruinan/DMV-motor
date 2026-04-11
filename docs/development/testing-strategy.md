# 测试策略

> 这是 DMV Motor 后端的测试规范。测试是控制论意义上的负反馈机制——
> 它精确定义系统目标态，度量实现偏差，驱动修正，直到系统收敛。
>
> 最后更新：2026-04-10

---

## 1. 核心原则

### 测试是目标态的唯一权威定义

测试不是验证代码"有没有写"，而是**精确描述系统应有的行为**。

- 先写测试，再写实现（TDD）
- 测试先红后绿，不允许测试从未失败过
- 如果需求变了，先改测试，再改实现
- 测试绿 = 系统已收敛到目标态

### 测试是约束，不是保险

不追求覆盖率数字。测试应覆盖：

- 所有业务规则（不是所有代码行）
- 所有对外 API 的 contract（路径、状态码、字段格式）
- 所有边界条件和错误路径
- 不覆盖：框架行为、trivial getter/setter、Spring Boot 自动配置

---

## 2. 测试分层

本项目采用**两层测试**，明确划分职责。

### Layer 1 — 单元测试（Domain / Application 逻辑）

**测什么：** 纯业务逻辑，无 I/O

| 适用场景 | 示例 |
|---------|------|
| 状态机转换规则 | Practice session: `started → in_progress → completed` |
| 评分算法 | Mock exam 得分计算、pass/fail 判定 |
| Readiness 计算 | 多维度分数合并、gate 检查 |
| 业务规则校验 | Access pass quota 扣减逻辑 |
| 工具函数 | 日期计算、语言 fallback 规则 |

**怎么写：**

- 纯 JUnit 5，不加任何 Spring 注解
- 不依赖 DB、HTTP、外部服务
- 测试名称格式：`methodName_condition_expectedBehavior`
- 快，毫秒级，可随时运行

**不测什么：** Repository 查询、Controller HTTP 行为、Spring 注入

```
位置：src/test/java/com/dmvmotor/api/{module}/domain/
      src/test/java/com/dmvmotor/api/{module}/application/
```

### Layer 2 — 集成测试（Controller + DB 全链路）

**测什么：** HTTP 请求进、DB 写/读出的完整链路

| 适用场景 | 示例 |
|---------|------|
| API contract 验证 | 路径、状态码、响应字段、错误码 |
| 数据持久化正确性 | 写入后再查询验证 |
| 跨模块边界行为 | 提交答案 → 错题记录自动生成 |
| 并发与状态冲突 | 重复提交答案、已结束 session 操作 |

**怎么写：**

- 继承 `IntegrationTestBase`
- 使用 `MockMvc` 发请求，`JdbcTemplate` 验证 DB 状态
- 每个测试前调用 `fixtures.truncateAll()` 清空数据
- 使用 `TestFixtures` 构建测试数据，不要在测试里写 raw SQL

```
位置：src/test/java/com/dmvmotor/api/{module}/controller/
```

---

## 3. 测试隔离

### 为什么不用 @Transactional 回滚

MockMvc 集成测试中，HTTP 请求在独立事务中执行（Controller 的事务），与测试方法的事务不同。
若测试方法持有一个未提交的事务，Controller 看不到数据，测试必然失败。

**因此：集成测试不用事务回滚，用显式清空。**

### 清空策略

每个集成测试类的 `@BeforeEach` 第一行调用：

```java
fixtures.truncateAll();
```

`truncateAll()` 使用 `TRUNCATE ... RESTART IDENTITY CASCADE`，一条语句、级联处理 FK、重置序列。

### 单元测试天然隔离

单元测试无状态，不需要隔离机制。

---

## 4. 测试数据管理：TestFixtures

**原则：测试代码不直接写 SQL。**

测试数据通过 `TestFixtures`（Spring `@Component`）构建，提供命名语义化的 builder 方法。

```java
// 好的写法
Long topicId = fixtures.insertTopic("TRAFFIC_SIGNS");
Long questionId = fixtures.insertQuestion(topicId, "B");
fixtures.insertEnVariant(questionId, "What does a red octagon mean?", ...);

// 不好的写法（散落在测试里的 raw SQL）
jdbc.execute("INSERT INTO topics ...");
```

**builder 方法设计原则：**

- 提供合理默认值（不是 null），只暴露测试关心的参数
- 返回数据库生成的 ID，供后续引用
- 不暴露数据库内部字段（如 `created_at`）

---

## 5. 命名规范

### 测试类名

```
{被测类}Test.java
```

### 测试方法名

```
{被测方法或场景}_{条件}_{预期结果}
```

示例：

```java
listTopics_empty_returnsEmptyList()
getQuestion_notFound_returns404()
submitAnswer_wrongChoice_recordsMistake()
startSession_freeTrial_returnsFirstQuestion()
```

### 测试数据常量

测试数据不用魔法字符串，用常量或 fixtures 返回的真实 ID。

---

## 6. 什么测试不该写

| 不要测 | 原因 |
|--------|------|
| Spring Bean 能否注入 | 框架保证，`contextLoads` 测一次够了 |
| jOOQ 能否连接 DB | Testcontainers 启动即验证 |
| Getter/Setter | trivial，无业务价值 |
| 与实现一一对应的测试 | 测行为，不测实现 |
| 100% 覆盖率填充测试 | 测试的价值在于约束，不在于数量 |

---

## 7. 测试的负反馈闭环

```
需求 / API Contract
       │
       ▼
  写测试（红）         ← 目标态定义
       │
       ▼
  写实现代码
       │
       ▼
  运行测试（绿？）
       │
    ┌──┴──┐
   失败   通过
    │      │
    ▼      ▼
  修实现  Done ✓
    │
    └──────┘（循环直到收敛）
```

测试永远不因为"实现太难改"而被修改（除非需求变更）。
实现偏离测试 = 系统发散 = 必须修正。

---

## 8. CI 要求（未来）

- 所有测试在 CI 必须通过，才允许合并
- 集成测试使用 Testcontainers，不依赖外部服务
- 测试时间目标：全套 < 3 分钟

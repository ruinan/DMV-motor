# 技术选型决策

## 1. 目标

这份文档定义 MVP 阶段的技术栈选择，服务于后续 Spring Boot 开发落地。

它回答这些问题：

- 后端用什么语言和框架
- Web 端用什么技术
- 数据库和部署放在哪里
- Web 与后端是同仓还是拆仓
- 当前还缺哪些面向开发的实现文档

这份文档不定义：

- 具体业务规则
- 页面视觉稿
- 详细类图和表结构 SQL
- CI/CD 具体命令

## 2. 当前推荐结论

当前推荐技术组合为：

- 后端：`Java 21 + Spring Boot`
- 构建：`Maven`
- Web：`Next.js`
- 数据库：`PostgreSQL`
- ORM / 数据访问：优先 `jOOQ` 或 `MyBatis`，不建议一开始重度依赖自动 ORM
- 鉴权：接第三方认证，业务表统一落内部 `user_id`
- 部署：`GCP Cloud Run`
- 数据库托管：`GCP Cloud SQL for PostgreSQL`
- 对象存储：`GCS`

## 3. 为什么后端选 Spring Boot

这个项目当前更像规则密集型业务系统，而不是高并发基础设施。

Spring Boot 更适合它的原因是：

- 模块化单体实现成熟，适合当前已确定的模块边界
- 事务、校验、配置、测试、数据库接入都很成熟
- 更适合处理 access、quota、review、readiness 这类强规则链路
- 比 Rust 更适合当前阶段快速落地和频繁改规则
- 与当前仓库已有 `pom.xml` 信号一致，切换成本更低

## 4. 为什么 Web 推荐 Next.js

当前 Web 端主要承担：

- 登录态持有
- 页面展示和交互流程
- 调后端 API
- 基础 SEO / 着陆页能力

Next.js 适合当前阶段，因为：

- React 生态成熟，适合快速做产品界面
- 同时能支持营销页和应用页
- 在 GCP 上容器化部署也直接
- 和 Spring Boot 做前后端分离边界清晰

## 5. 为什么数据库选 PostgreSQL

这个项目的核心对象天然更适合关系型建模：

- 用户
- access pass
- question / topic
- practice attempt
- mistake record
- review task / review pack
- mock attempt
- readiness / progress snapshot

PostgreSQL 适合当前阶段，因为：

- 事务能力稳定
- 复杂查询和关联表达能力强
- JSON 字段可用于少量 snapshot / payload
- 在 GCP Cloud SQL 上托管成熟

## 6. GCP 部署建议

当前建议部署形态：

- `api` 服务部署到 `Cloud Run`
- `web` 服务部署到 `Cloud Run`
- `PostgreSQL` 放 `Cloud SQL`
- 文件或导出结果放 `GCS`

当前不建议一开始就做：

- GKE
- 微服务拆分
- 自建消息系统
- 复杂 service mesh

原因很直接：

- MVP 规模不需要
- 运维复杂度会显著高于业务价值

## 7. Web 和后端是否同仓

当前推荐：

- 同一个 repo
- 分成两个顶层目录

推荐结构：

- `apps/api`
- `apps/web`
- `docs`

不建议当前就拆成两个 repo。

原因：

- 现在产品和接口都还在快速变化
- 同仓更适合一起改文档、API、页面和部署配置
- 单人或小团队阶段，同仓协作成本更低
- 前后端接口变化不容易失配

只有在下面情况出现后，才值得考虑拆仓：

- 前后端团队明显分离
- 发布节奏完全不同
- Web 不再只服务这个后端
- 仓库体积和 CI 时间明显失控

## 8. 当前建议的仓库结构

当前推荐把仓库逐步整理成：

```text
DMV-Motor/
  apps/
    api/
      pom.xml
      src/
    web/
      package.json
      src/
  docs/
  infra/
```

说明：

- `apps/api` 放 Spring Boot 服务
- `apps/web` 放 Next.js
- `docs` 保持当前产品与技术文档
- `infra` 放部署配置、Dockerfile、GCP 相关脚本

## 9. 面向 Spring Boot 开发还缺哪些文档

当前文档已经把产品规则讲得比较完整，  
但对真正开始开发来说，还缺这些实现级文档：

### 9.1 后端代码组织文档

建议新增一份文档，明确：

- 包结构
- 模块结构
- controller / application / domain / infrastructure 分层
- DTO 和 domain object 的边界

建议文件名：

- `docs/development/backend-structure.md`

### 9.2 API 字段级约定文档

现有 [api.md](/C:/Users/nanru/OneDrive/Documents/DMV-Motor/docs/api.md) 还是产品层边界，  
还缺真正给 Spring Boot 开发用的：

- 请求字段
- 响应字段
- 错误码
- 鉴权头
- 分页格式

建议文件名：

- `docs/development/api-contract.md`

### 9.3 数据库落表文档

现有 [db-schema.md](/C:/Users/nanru/OneDrive/Documents/DMV-Motor/docs/db-schema.md) 已经很接近，  
但还缺真正可落 migration 的：

- 字段类型
- 非空约束
- 唯一约束
- 索引建议
- 审计字段规范

建议文件名：

- `docs/development/database-physical-design.md`

### 9.4 鉴权接入文档

当前只定了“第三方认证 + 内部 user_id”，  
还缺：

- 选哪家认证
- 登录回调流程
- token 验证方式
- 用户首次登录创建流程

建议文件名：

- `docs/development/auth-integration.md`

### 9.5 GCP 部署文档

当前只定了方向，没有落实施：

- Cloud Run 服务划分
- 环境变量
- Secret Manager
- Cloud SQL 连接方式
- 日志 / 监控 / 告警最低配置

建议文件名：

- `docs/development/gcp-deployment.md`

### 9.6 测试策略文档

这个项目规则很多，必须提前定测试边界：

- 单元测试测什么
- 集成测试测什么
- 哪些链路必须做端到端验证
- mock 配额、access、review、readiness 哪些必须防回归

建议文件名：

- `docs/development/testing-strategy.md`

## 10. 当前建议的下一步

如果按 Spring Boot 路线推进，当前最值得先补的是：

1. `backend-structure.md`
2. `api-contract.md`
3. `auth-integration.md`
4. `gcp-deployment.md`

然后再正式开始搭 `apps/api` 和 `apps/web` 的代码骨架。

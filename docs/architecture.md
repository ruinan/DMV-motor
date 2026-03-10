# GCP MVP 架构

## 1. 推荐结论

MVP 阶段推荐：

- Web：Next.js
- API：Node.js + TypeScript
- Auth：Firebase Auth
- Database：Firestore
- AI：Vertex AI / Gemini
- Hosting / Compute：Cloud Run
- Static Assets：Cloud Storage
- Observability：Cloud Logging + Cloud Monitoring

这是当前个人或小团队在 GCP 上做该类 MVP 的主流轻量方案。  
原因不是“最先进”，而是“最快落地、最少运维、最贴合产品目标”。

## 2. 为什么选这套

### Next.js

- 适合快速做登录、dashboard、练习、结果页
- 前端和轻量 API 可以共用 TypeScript 领域模型
- 后续可以做 SSR 落地页和 SEO 页面，但不影响 MVP 主流程

### Node.js + TypeScript API

- 与 Next.js、Firebase、Firestore、Vertex AI 配合最顺
- 开发速度快，适合快速验证题目编排和复习逻辑
- 对这个项目而言，业务重心不在复杂企业级事务，而在学习编排

### Cloud Run

- 不需要管理服务器
- 适合 API、异步 worker、后台任务服务
- 随流量扩缩，MVP 成本可控

### Firebase Auth

- 登录接入快
- 支持 Google 和邮箱登录
- 与前端和 GCP 生态集成成本低

### Firestore

- 适合用户资料、错题记录、复习队列、学习会话这类文档型数据
- MVP 阶段不必先上更重的关系型模型
- 读写模式与本项目高度匹配

### Vertex AI / Gemini

- 用于结构化错因解释和总结
- 与 GCP 权限、审计、调用链整合简单

## 3. 服务分工

### 前端 Web

职责：

- 登录
- Dashboard
- 练习、复习、模拟考试页面
- 结果页和总结页展示

部署：

- 可单独一个 Next.js Cloud Run 服务

### API 服务

职责：

- 题目获取
- 提交答案
- 计算正确率和掌握度
- 生成 `review_queue`
- 提供错题本、复习包、总结接口

部署：

- 一个 Cloud Run 服务即可

### AI Worker

职责：

- 消费异步任务
- 调 Vertex AI 生成 `ai_feedback`
- 回写结构化结果

部署：

- MVP 可先和 API 放在一个服务里，通过异步 job 触发
- 稍后再拆成独立 Cloud Run worker

### 内容存储

职责：

- 存 `questions`、`user_attempts`、`weak_topic_profiles` 等

部署：

- Firestore

### 静态资源

职责：

- 题目插图
- OG 图
- 运营静态资源

部署：

- Cloud Storage

## 4. 建议的最小部署拓扑

### MVP 最简版

- `frontend-service`：Next.js on Cloud Run
- `api-service`：Node.js API on Cloud Run
- `Firestore`
- `Firebase Auth`
- `Vertex AI`
- `Cloud Logging / Monitoring`

这已经足够上线。

### 稍进一版

增加：

- `ai-worker-service`
- `Cloud Tasks` 或 Pub/Sub

作用：

- 把 AI 解释和总结异步化
- 避免答题接口被模型延迟拖慢

## 5. 请求流转示例

### 练习答题

1. 前端请求 `GET /practice/questions`
2. API 从 Firestore 读取用户弱点和题目池，返回题组
3. 用户提交答案到 `POST /practice/attempts`
4. API 判分，写入 `user_attempt`
5. API 更新 `weak_topic_profile`
6. API 生成或更新 `review_queue`
7. API 可异步发任务给 AI worker 生成短解释

### 模拟考试

1. 前端请求创建 `mock_exam_session`
2. API 返回固定题组
3. 用户交卷
4. API 计算成绩和弱点
5. API 同步返回基础总结
6. AI worker 异步生成更自然语言的薄弱点说明

## 6. 哪些地方后续可扩展

### 数据层

当出现以下情况时，再考虑补 `Cloud SQL / Postgres`：

- 需要复杂运营报表
- 需要题目审核流程和强关系内容管理
- 需要复杂 join 和多维统计分析

MVP 阶段不建议一开始就上 Cloud SQL，原因是会增加迁移、建模、运维成本，但并不能明显提高第一版通过率闭环。

### 计算层

当 AI 调用量明显上升时：

- 拆分 AI worker
- 增加队列
- 增加缓存模板

### 内容层

后续可以补一个内部内容管理后台，用于：

- 题目录入
- 题目审核
- topic 维护
- 解析版本管理

但这不是第一版用户侧 MVP 的阻塞项。

## 7. 安全与权限

- 前端使用 Firebase Auth 登录
- API 校验 Firebase ID Token
- 所有 `user_*` 数据按 `user_id` 隔离
- AI 输入只传必要字段，不传无关隐私
- 日志中避免记录完整题干和用户敏感信息

## 8. 监控重点

MVP 重点看这些指标：

- 每日活跃学习用户数
- 每个用户的平均答题数
- 复习包完成率
- 模拟考试完成率
- 模拟考试平均分
- 弱点 topic 收敛速度
- AI 调用成功率和平均耗时

因为这些指标直接反映产品是否真的在提升通过率，而不是只是在“有人使用”。

## 9. 为什么这套架构适合这个项目

这个项目的目标很窄，所以架构也应该窄：

- 题目系统
- 学习状态系统
- 复习调度系统
- AI 解释系统

不需要更重的微服务体系，不需要多云，不需要复杂数据中台。  
MVP 最重要的是尽快跑通“练题 -> 错题强化 -> 模拟考试 -> 通过率提升”的业务闭环。

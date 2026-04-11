# API 合同

> **文档分层说明**
>
> 这份文档（`docs/development/api-contract.md`）是**开发级 API 合同**，定义具体 HTTP 路径、字段、错误码和格式约定，是 controller 实现和前端联调的直接依据。
>
> 产品层 API 边界定义见 [`docs/api.md`](../api.md)。如果两者有出入，以本文档为准并反馈修正 `api.md`。

## 0. 测试覆盖 Checklist

> 每个接口实现后，在对应行填入测试类名并标 ✅。
> 未实现标 ⬜，进行中标 🔄。
> 覆盖率要求：指令 ≥ 90%，分支 ≥ 90%（JaCoCo 强制，`mvn test` 不达标则 BUILD FAILURE）。

### Content（内容基础层）

| 接口 | 测试类 | 状态 |
|------|--------|------|
| `GET /api/v1/topics` | `TopicControllerTest` | ✅ |
| `GET /api/v1/questions/{id}` | `QuestionControllerTest` | ✅ |

### Auth / Account

| 接口 | 测试类 | 状态 |
|------|--------|------|
| `GET /api/v1/me` | `AccountControllerTest` | ✅ |
| `POST /api/v1/me/reset-learning` | `AccountControllerTest` | ✅ |
| `PUT /api/v1/me/language` | `AccountControllerTest` | ✅ |

### Access

| 接口 | 测试类 | 状态 |
|------|--------|------|
| `GET /api/v1/access` | `AccessControllerTest` | ✅ |

### Practice

| 接口 | 测试类 | 状态 |
|------|--------|------|
| `POST /api/v1/practice/sessions` | `PracticeSessionControllerTest` | ✅ |
| `GET /api/v1/practice/sessions/{id}/next-question` | `PracticeSessionControllerTest` | ✅ |
| `POST /api/v1/practice/sessions/{id}/answers` | `PracticeSessionControllerTest` | ✅ |
| `GET /api/v1/practice/sessions/{id}` | `PracticeSessionControllerTest` | ✅ |
| `POST /api/v1/practice/sessions/{id}/complete` | `PracticeSessionControllerTest` | ✅ |

### Mistakes

| 接口 | 测试类 | 状态 |
|------|--------|------|
| `GET /api/v1/mistakes` | — | ⬜ |

### Review

| 接口 | 测试类 | 状态 |
|------|--------|------|
| `GET /api/v1/review/pack` | — | ⬜ |
| `GET /api/v1/review/tasks/{id}/questions` | — | ⬜ |
| `POST /api/v1/review/tasks/{id}/answers` | — | ⬜ |
| `POST /api/v1/review/tasks/{id}/complete` | — | ⬜ |

### Mock Exam

| 接口 | 测试类 | 状态 |
|------|--------|------|
| `GET /api/v1/mock-exams/access` | — | ⬜ |
| `POST /api/v1/mock-exams/attempts` | — | ⬜ |
| `POST /api/v1/mock-exams/attempts/{id}/answers` | — | ⬜ |
| `POST /api/v1/mock-exams/attempts/{id}/submit` | — | ⬜ |
| `POST /api/v1/mock-exams/attempts/{id}/exit` | — | ⬜ |

### Summary / Progress / Readiness

| 接口 | 测试类 | 状态 |
|------|--------|------|
| `GET /api/v1/summary` | — | ⬜ |
| `GET /api/v1/readiness` | — | ⬜ |

---

## 1. 目标

这份文档定义 Spring Boot 后端对 Web 的 API 合同，  
用于指导 controller、request DTO、response DTO 和前端联调。

它回答这些问题：

- API 采用什么风格
- 哪些接口需要登录
- 未登录用户可以访问哪些接口
- 请求和响应的统一格式是什么
- 错误码和分页怎么约定
- MVP 阶段核心接口的 request / response 结构是什么

这份文档不定义：

- 具体数据库表结构
- controller 类名
- 内部 service 调用关系
- OpenAPI 工具生成细节

## 2. 总体约定

### API 风格

当前采用：

- REST 风格
- JSON 请求 / 响应
- `/api/v1` 作为统一前缀

### 时间格式

统一使用：

- ISO-8601 UTC 时间字符串

例如：

- `2026-03-12T16:30:00Z`

### ID 格式

MVP 阶段对外只要求：

- 字符串 ID

不在 API 层暴露数据库自增细节。

## 3. 认证与访问级别

当前接口分成三类：

### 公开接口

不需要登录即可访问。

适用于：

- 产品落地页内容
- 固定免费体验题集
- 支持前端首屏启动所需的基础配置

### 登录接口

必须登录后才能访问。

适用于：

- 保存学习状态
- 错题记录
- review
- mock exam
- readiness / summary
- memory export

### 已登录但按访问权限受限接口

需要登录，但仍要额外校验 access pass 或 quota。

适用于：

- 完整 review
- mock exam
- 完整 readiness
- 完整 summary

## 4. 未登录用户规则

当前明确约定：

- 未登录用户可以进入固定免费体验集
- 未登录用户不进入完整学习闭环
- 未登录用户不创建长期学习账户状态
- 一旦需要保存学习记录，必须先登录

这意味着：

- 匿名体验只服务“先感知产品价值”
- 长期学习状态只属于登录用户

## 5. 认证头约定

当前建议：

- `Authorization: Bearer <token>`

后端统一从认证中间层解析出：

- 外部身份
- 内部 `user_id`

业务 controller 不直接处理第三方 token 细节。

## 6. 统一响应格式

成功响应统一为：

```json
{
  "success": true,
  "data": {},
  "meta": {}
}
```

失败响应统一为：

```json
{
  "success": false,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "Access pass is required.",
    "details": {}
  }
}
```

说明：

- `data` 只放当前接口主结果
- `meta` 放分页、语言、调试辅助信息
- `error.code` 给前后端做稳定判断
- `error.message` 给日志和基础展示用

## 7. 统一错误码

当前建议至少保留这些错误码：

- `UNAUTHORIZED`
- `FORBIDDEN`
- `ACCESS_DENIED`
- `FREE_LIMIT_REACHED`
- `MOCK_QUOTA_EXHAUSTED`
- `MOCK_ALREADY_ENDED`
- `SESSION_NOT_FOUND`
- `SESSION_NOT_RESUMABLE`
- `QUESTION_ALREADY_SUBMITTED`
- `VALIDATION_ERROR`
- `RESOURCE_NOT_FOUND`
- `CONFLICT_STATE`
- `INTERNAL_ERROR`

## 8. 分页格式

列表接口统一采用：

- `page`
- `page_size`

响应 `meta` 统一返回：

```json
{
  "page": 1,
  "page_size": 20,
  "total": 120
}
```

MVP 阶段如果列表很短，也允许某些接口先不分页，  
但 Mistakes、History 这类列表建议直接按统一格式做。

## 9. 语言参数约定

当前统一使用：

- `language`

可选值：

- `en`
- `zh`

规则：

- 若请求显式传 `language`，优先使用请求值
- 若未传，使用用户偏好
- 匿名态若也没有显式值，则使用系统默认语言规则

## 10. 核心接口分组

当前接口按这些分组组织：

- Auth / Account
- Access
- Practice
- Mistakes
- Review
- Mock Exam
- Summary / Progress / Readiness
- Language

## 11. Auth / Account 接口

### `GET /api/v1/me`

作用：

- 获取当前登录用户账户状态

认证：

- 需要登录

响应 `data`：

```json
{
  "user_id": "usr_xxx",
  "email": "user@example.com",
  "language": "en",
  "access": {
    "state": "free_trial",
    "has_active_pass": false,
    "expires_at": null,
    "mock_remaining": 0
  },
  "learning": {
    "has_in_progress_practice": true,
    "has_in_progress_review": false
  }
}
```

### `POST /api/v1/me/reset-learning`

作用：

- 重置学习主状态

认证：

- 需要登录

请求 `body`：

```json
{
  "confirm": true
}
```

响应 `data`：

```json
{
  "reset": true
}
```

## 12. Access 接口

### `GET /api/v1/access`

作用：

- 返回当前访问状态

认证：

- 登录用户可调用
- 匿名用户可返回匿名体验状态

响应 `data`：

```json
{
  "state": "free_trial",
  "has_active_pass": false,
  "mock_remaining": 0,
  "can_use_review": false,
  "can_use_mock_exam": false
}
```

说明：

- 免费体验集可反复访问，无消耗计数，因此不返回 `free_practice_remaining` 字段
- `state` 可选值：`free_trial`（无付费 pass）、`active`（pass 有效）、`expired`（pass 已过期）

## 13. Practice 接口

### `POST /api/v1/practice/sessions`

作用：

- 开始一次练习

认证：

- 匿名用户可调用固定免费体验
- 登录用户可调用完整练习

请求 `body`：

```json
{
  "entry_type": "free_trial",
  "language": "en"
}
```

响应 `data`：

```json
{
  "session_id": "ps_xxx",
  "entry_type": "free_trial",
  "status": "in_progress",
  "language": "en",
  "next_question": {
    "question_id": "q_xxx",
    "variant_id": "qv_xxx",
    "stem": "Question stem",
    "choices": [
      { "key": "A", "text": "Choice A" },
      { "key": "B", "text": "Choice B" },
      { "key": "C", "text": "Choice C" }
    ]
  }
}
```

### `GET /api/v1/practice/sessions/{session_id}/next-question`

作用：

- 拉取当前 session 下一题

认证：

- 与 session 对应身份一致

响应 `data`：

```json
{
  "question_id": "q_xxx",
  "variant_id": "qv_xxx",
  "stem": "Question stem",
  "choices": [
    { "key": "A", "text": "Choice A" },
    { "key": "B", "text": "Choice B" },
    { "key": "C", "text": "Choice C" }
  ],
  "progress": {
    "answered_count": 8
  }
}
```

说明：

- 结构与创建 session 时返回的 `next_question` 一致，额外带 `progress` 字段
- 若当前 session 已无更多题，返回 `404` 并附 `error.code: SESSION_COMPLETED`

### `POST /api/v1/practice/sessions/{session_id}/answers`

作用：

- 提交单题答案并立即返回结果

请求 `body`：

```json
{
  "question_id": "q_xxx",
  "variant_id": "qv_xxx",
  "selected_choice_key": "B"
}
```

响应 `data`：

```json
{
  "question_id": "q_xxx",
  "is_correct": false,
  "correct_choice_key": "C",
  "explanation": {
    "type": "short",
    "text": "You should yield in this situation."
  },
  "progress": {
    "answered_count": 8
  }
}
```

### `GET /api/v1/practice/sessions/{session_id}`

作用：

- 获取当前 session 状态

### `POST /api/v1/practice/sessions/{session_id}/complete`

作用：

- 完成当前练习 session

## 14. Mistakes 接口

### `GET /api/v1/mistakes`

作用：

- 获取活跃错题列表

认证：

- 需要登录

请求参数：

- `page`
- `page_size`
- `topic_id` 可选

响应 `data`：

```json
{
  "items": [
    {
      "mistake_id": "mr_xxx",
      "question_id": "q_xxx",
      "topic_id": "topic_xxx",
      "wrong_count": 3,
      "last_wrong_at": "2026-03-12T10:00:00Z",
      "source": "practice"
    }
  ]
}
```

响应 `meta`：

```json
{
  "page": 1,
  "page_size": 20,
  "total": 42
}
```

## 15. Review 接口

### `GET /api/v1/review/pack`

作用：

- 获取当前 review pack

认证：

- 需要登录
- 需要允许使用 review

响应 `data`：

```json
{
  "review_pack_id": "rp_xxx",
  "status": "ready",
  "target_question_count": 20,
  "completed_question_count": 4,
  "tasks": [
    {
      "review_task_id": "rt_xxx",
      "type": "same_topic_retry",
      "topic_id": "topic_xxx",
      "priority": 90,
      "status": "in_progress"
    }
  ]
}
```

### `GET /api/v1/review/tasks/{task_id}/questions`

作用：

- 获取某个 review task 下的题目列表，供前端渲染

认证：

- 需要登录
- 需要允许使用 review

响应 `data`：

```json
{
  "review_task_id": "rt_xxx",
  "task_type": "same_topic_retry",
  "topic_id": "topic_xxx",
  "questions": [
    {
      "question_id": "q_xxx",
      "variant_id": "qv_xxx",
      "stem": "Question stem",
      "choices": [
        { "key": "A", "text": "Choice A" },
        { "key": "B", "text": "Choice B" },
        { "key": "C", "text": "Choice C" }
      ]
    }
  ]
}
```

说明：

- 一次性返回该 task 的所有题目（review task 题量少，不需要 next-question 模式）
- 前端拿到题目后逐题作答，通过下方的 answers 接口逐题上报

### `POST /api/v1/review/tasks/{task_id}/answers`

作用：

- 提交 review 任务中单题答案并立即返回结果

认证：

- 需要登录
- 需要允许使用 review

请求 `body`：

```json
{
  "question_id": "q_xxx",
  "variant_id": "qv_xxx",
  "selected_choice_key": "B"
}
```

响应 `data`：

```json
{
  "question_id": "q_xxx",
  "is_correct": false,
  "correct_choice_key": "C",
  "explanation": {
    "type": "short",
    "text": "You should yield in this situation."
  },
  "task_progress": {
    "answered_count": 2,
    "target_count": 3
  }
}
```

说明：

- 格式与 practice answer 基本一致，额外返回当前 task 内的作答进度
- review 答案同样写入 `practice_attempts`，`entry_source` 标记为 `review`

### `POST /api/v1/review/tasks/{task_id}/complete`

作用：

- 完成一个 review task

响应 `data`：

```json
{
  "review_task_id": "rt_xxx",
  "completed": true,
  "next_action": {
    "type": "continue_review",
    "label": "Continue review"
  }
}
```

## 16. Mock Exam 接口

### `GET /api/v1/mock-exams/access`

作用：

- 查询当前是否允许开始 mock

认证：

- 需要登录

响应 `data`：

```json
{
  "allowed": true,
  "mock_remaining": 3,
  "reason": null
}
```

### `POST /api/v1/mock-exams/attempts`

作用：

- 开始一次 mock exam

认证：

- 需要登录
- 需要有效 access pass
- 需要剩余 quota

请求 `body`：

```json
{
  "language": "en"
}
```

响应 `data`：

```json
{
  "mock_attempt_id": "ma_xxx",
  "status": "in_progress",
  "mock_remaining_after_start": 2,
  "questions": [
    {
      "question_id": "q_xxx",
      "variant_id": "qv_xxx",
      "stem": "Question stem",
      "choices": [
        { "key": "A", "text": "Choice A" },
        { "key": "B", "text": "Choice B" }
      ]
    }
  ]
}
```

### `POST /api/v1/mock-exams/attempts/{attempt_id}/answers`

作用：

- 逐题上报答案（不返回判分结果）
- 每答完一题立即调用，后端持久化到 `mock_attempt_results`
- 此设计确保崩溃/断网时已答数据不丢失

认证：

- 需要登录

请求 `body`：

```json
{
  "question_id": "q_xxx",
  "variant_id": "qv_xxx",
  "selected_choice_key": "A"
}
```

响应 `data`：

```json
{
  "saved": true,
  "answered_count": 12
}
```

说明：

- 后端只存储答案，不返回对错，保留考试压力感
- 允许同一题重复提交（用最后一次为准），应对前端重试场景

### `POST /api/v1/mock-exams/attempts/{attempt_id}/submit`

作用：

- 交卷：锁定本次 attempt，触发后端计分
- 无需携带答案，答案已通过逐题接口持久化

认证：

- 需要登录

请求 `body`：无

响应 `data`：

```json
{
  "mock_attempt_id": "ma_xxx",
  "status": "submitted",
  "score_percent": 86,
  "correct_count": 43,
  "wrong_count": 7,
  "weak_topics": [
    {
      "topic_id": "topic_xxx",
      "label": "Road positioning"
    }
  ],
  "next_action": {
    "type": "review",
    "label": "Review weak topics first"
  }
}
```

### `POST /api/v1/mock-exams/attempts/{attempt_id}/exit`

作用：

- 用户确认退出并结束本次 mock
- 答案已通过逐题接口持久化，此接口只做状态锁定

认证：

- 需要登录

请求 `body`：无

响应 `data`：

```json
{
  "mock_attempt_id": "ma_xxx",
  "status": "ended_by_exit",
  "quota_consumed": true,
  "answered_count": 3
}
```

说明：

- 崩溃/断网场景：后端 attempt 保持 `in_progress`，已上报的逐题答案完整保留；超时后系统将 attempt 标记为 `expired`，已有答案仍可回流到 review 系统

## 17. Summary / Progress / Readiness 接口

### `GET /api/v1/summary`

作用：

- 获取 summary 页面主数据

认证：

- 登录用户可获取基础 summary
- 完整 summary 受访问权限控制

响应 `data`：

```json
{
  "completion_score": 72,
  "readiness_score": 64,
  "is_ready_candidate": false,
  "weak_topics": [
    {
      "topic_id": "topic_xxx",
      "label": "Intersections"
    }
  ],
  "next_action": {
    "type": "review",
    "label": "Finish review pack"
  }
}
```

### `GET /api/v1/readiness`

作用：

- 获取 readiness 状态

响应 `data`：

```json
{
  "readiness_score": 64,
  "is_ready_candidate": false,
  "missing_gates": [
    "MOCK_SCORE_NOT_STABLE",
    "KEY_TOPIC_COVERAGE_LOW"
  ]
}
```

## 18. Language 接口

### `PUT /api/v1/me/language`

作用：

- 修改当前用户语言偏好

认证：

- 需要登录

请求 `body`：

```json
{
  "language": "zh"
}
```

响应 `data`：

```json
{
  "language": "zh"
}
```

## 19. HTTP 状态码建议

当前建议：

- `200` 成功查询或提交
- `201` 成功创建新 session / attempt
- `400` 参数错误
- `401` 未登录
- `403` 已登录但无权限
- `404` 资源不存在
- `409` 状态冲突
- `429` 配额或频率限制
- `500` 服务器错误

## 20. 当前结论

MVP API 合同当前应采用：

- REST + JSON
- `/api/v1` 前缀
- `Bearer Token` 认证
- 统一成功 / 失败响应结构
- 匿名体验与登录闭环明确分开
- 先把 Practice、Review、Mock、Summary 这些主链路接口定稳

# API 设计

本文按 REST 风格设计，默认前缀为 `/api/v1`。  
认证方式：前端携带 Firebase Auth Bearer Token，后端校验后获得 `user_id`。

## 统一响应约定

成功：

```json
{
  "success": true,
  "data": {}
}
```

失败：

```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "question_id is required"
  }
}
```

## 1. 获取练习题

`GET /api/v1/practice/questions`

### Query 参数

- `mode`: `diagnostic` / `targeted` / `mixed`
- `topic_code`: 可选
- `size`: 默认 `10`

### 作用

返回一组练习题。  
如果不传 `topic_code`，系统根据 `weak_topic_profile` 和 `review_queue` 自动编排。

### Response 示例

```json
{
  "success": true,
  "data": {
    "session_id": "prac_20260310_001",
    "mode": "targeted",
    "recommended_focus": "right_of_way",
    "questions": [
      {
        "question_id": "q_101",
        "topic_code": "right_of_way",
        "subtopic_code": "4_way_stop",
        "question_type": "scenario_best_action",
        "stem": "At a four-way stop, two vehicles arrive before you and remain stopped. What should you do next on a motorcycle?",
        "options": [
          { "option_id": "q_101_a", "label": "A", "text": "Go first if you can accelerate quickly" },
          { "option_id": "q_101_b", "label": "B", "text": "Wait for the vehicles with the right-of-way to proceed" },
          { "option_id": "q_101_c", "label": "C", "text": "Wave everyone through and then go" },
          { "option_id": "q_101_d", "label": "D", "text": "Move around the stopped vehicles" }
        ]
      }
    ]
  }
}
```

## 2. 提交答案

`POST /api/v1/practice/attempts`

### Request 示例

```json
{
  "session_id": "prac_20260310_001",
  "question_id": "q_101",
  "selected_option_id": "q_101_a",
  "response_time_ms": 8200,
  "confidence_level": 2
}
```

### 处理逻辑

- 规则引擎即时判分
- 写入 `user_attempt`
- 更新 `weak_topic_profile`
- 必要时生成 `review_queue`
- 可异步触发 `ai_feedback`

### Response 示例

```json
{
  "success": true,
  "data": {
    "question_id": "q_101",
    "is_correct": false,
    "correct_option_id": "q_101_b",
    "topic_code": "right_of_way",
    "mistake_detected": {
      "mistake_reason_code": "question_intent_misread",
      "trap_type": "plausible_but_wrong"
    },
    "explanation": {
      "short": "这题考的是谁先拥有路权，不是考车辆加速能力。你选了“看起来能快速通过”的答案，但 DMV 口径优先看路权顺序。 ",
      "source_type": "ai_plus_rule"
    },
    "review_action": {
      "queued": true,
      "strategy_code": "contrast_questions",
      "scheduled_for": "2026-03-11T08:00:00Z"
    }
  }
}
```

## 3. 获取错题本

`GET /api/v1/mistakes`

### Query 参数

- `topic_code`: 可选
- `mistake_reason_code`: 可选
- `limit`: 默认 `20`

### Response 示例

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "attempt_id": "att_9001",
        "question_id": "q_101",
        "topic_code": "right_of_way",
        "question_type": "scenario_best_action",
        "selected_option_id": "q_101_a",
        "correct_option_id": "q_101_b",
        "mistake_reason_code": "question_intent_misread",
        "answered_at": "2026-03-10T09:21:00Z",
        "explanation_short": "你把“最安全看起来合理”误当成“最符合规则的答案”。"
      }
    ]
  }
}
```

## 4. 获取今日复习

`GET /api/v1/review/today`

### 作用

返回当日应完成的复习包，按优先级排序。  
复习包由错题重做、相似规则辨析题、弱知识点新题混合组成。

### Response 示例

```json
{
  "success": true,
  "data": {
    "review_date": "2026-03-10",
    "total_items": 12,
    "focus_topics": ["right_of_way", "lane_splitting"],
    "items": [
      {
        "review_item_id": "rq_001",
        "queue_type": "wrong_question",
        "strategy_code": "same_question_retry",
        "priority": 95,
        "question": {
          "question_id": "q_101",
          "stem": "At a four-way stop...",
          "options": [
            { "option_id": "q_101_a", "label": "A", "text": "Go first if you can accelerate quickly" },
            { "option_id": "q_101_b", "label": "B", "text": "Wait for the vehicles with the right-of-way to proceed" }
          ]
        }
      },
      {
        "review_item_id": "rq_002",
        "queue_type": "weak_topic_drill",
        "strategy_code": "contrast_questions",
        "priority": 88,
        "topic_code": "lane_splitting"
      }
    ]
  }
}
```

## 5. 获取模拟考试题

`POST /api/v1/mock-exams`

### Request 示例

```json
{
  "exam_type": "california_m1_mock",
  "size": 25
}
```

### Response 示例

```json
{
  "success": true,
  "data": {
    "mock_exam_session_id": "mock_20260310_001",
    "total_questions": 25,
    "time_limit_seconds": 1800,
    "questions": [
      {
        "question_id": "q_301",
        "topic_code": "helmet_equipment",
        "question_type": "legality",
        "stem": "Which statement best matches California rules for motorcycle eye protection?",
        "options": [
          { "option_id": "q_301_a", "label": "A", "text": "Only riders under 18 need it" },
          { "option_id": "q_301_b", "label": "B", "text": "It is required unless the motorcycle has a compliant windshield" },
          { "option_id": "q_301_c", "label": "C", "text": "It is optional if riding during the day" },
          { "option_id": "q_301_d", "label": "D", "text": "It applies only on freeways" }
        ]
      }
    ]
  }
}
```

## 6. 提交模拟考试

`POST /api/v1/mock-exams/{mockExamSessionId}/submit`

### Request 示例

```json
{
  "answers": [
    {
      "question_id": "q_301",
      "selected_option_id": "q_301_c",
      "response_time_ms": 11000
    },
    {
      "question_id": "q_302",
      "selected_option_id": "q_302_b",
      "response_time_ms": 7000
    }
  ]
}
```

### Response 示例

```json
{
  "success": true,
  "data": {
    "mock_exam_session_id": "mock_20260310_001",
    "score_percent": 76,
    "correct_count": 19,
    "wrong_count": 6,
    "pass_estimate": false,
    "weak_topics": [
      {
        "topic_code": "lane_splitting",
        "wrong_count": 3,
        "mistake_reason_code": "similar_rule_confusion"
      },
      {
        "topic_code": "right_of_way",
        "wrong_count": 2,
        "mistake_reason_code": "question_intent_misread"
      }
    ],
    "next_actions": [
      "先完成 8 题 lane splitting 对比强化",
      "再完成 5 题 right of way 场景题"
    ]
  }
}
```

## 7. 获取学习总结

`GET /api/v1/summary`

### Query 参数

- `window_days`: 默认 `7`

### Response 示例

```json
{
  "success": true,
  "data": {
    "window_days": 7,
    "attempt_count": 86,
    "accuracy": 0.81,
    "readiness_score": 72,
    "strong_topics": ["helmet_equipment", "alcohol_drugs"],
    "weak_topics": ["lane_splitting", "right_of_way"],
    "recommended_next_step": "完成今日复习包后再做一次 25 题模拟考试"
  }
}
```

## 8. 获取薄弱点分析

`GET /api/v1/weak-topics`

### Response 示例

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "topic_code": "lane_splitting",
        "weak_score": 92,
        "mastery_score": 38,
        "recent_wrong_count": 5,
        "consecutive_wrong_count": 3,
        "mistake_pattern": [
          "similar_rule_confusion",
          "distractor_bias"
        ],
        "recommended_action": "contrast_drill",
        "high_risk_reason": "你会把“看起来谨慎”的选项误选为 DMV 最符合规则的答案。"
      }
    ]
  }
}
```

## 9. 可补充接口

如果后续需要，可以补：

- `GET /api/v1/questions/{id}`：查看题目详情
- `POST /api/v1/review/{reviewItemId}/complete`：显式完成某个复习项
- `GET /api/v1/mock-exams/{id}`：查看历史模拟考试详情

## 10. 为什么这套 API 足够 MVP

这套接口已经覆盖最关键闭环：

- 能拿题
- 能交答案
- 能看错题
- 能看今日复习
- 能做模拟考试
- 能拿到学习总结和薄弱点

第一版不需要更多接口，因为这些接口已经直接支撑“提高通过率”的核心业务流程。

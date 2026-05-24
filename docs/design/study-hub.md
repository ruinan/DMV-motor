# Study Hub Redesign — Design Document

> **Status**: Phase B design frozen 2026-05-23 — all 6 open decisions resolved (§6). Implementation cleared to start at B0.
> **Source plan**: `~/.claude/plans/sorted-tinkering-frost.md` (per-developer; not in repo).
> **Origin**: User identified that `/dashboard` (currently "Today's review pack") under-delivers the core product value — a learner doesn't see *what they've mastered*, *how far from exam-ready they are*, or *what they should do next*. Existing review pack flow asks the user to self-direct review; users want automatic interleaving.

---

## 1. Vision

The post-login surface (currently `/dashboard`, labelled "Study" / "学习" in the sidebar) must answer **three questions in one glance**:

1. **How much do I actually know?** — knowledge coverage gauge against the DMV M1 Motorcycle Handbook (sub-topic granularity, not just 8 broad topics)
2. **How close am I to passing?** — readiness score gauge driven by the existing engine (`docs/parameters.md §7-§8`)
3. **What's the latest thing I did, and how did I do?** — practice + mock history with 10-item cap and aggregate stats beyond

Review is *not* its own surface — it's a side-effect of weighted question selection. T1.2 (commit `0a88764`) already weights `findNextUnansweredQuestion` toward active-mistake topics. When a user has unfinished business in a sub-topic, the next practice / mock will pull from it automatically. The user doesn't have to navigate to a separate "review" page and remember to click tasks.

This deletes the manual-review UX entirely (`/review` route + sidebar tab + ReviewPackView component) while keeping the backend mastery deactivation logic (T1.3 commit `5a79e07`) so the auto-clear behaviour stays.

---

## 2. Information Architecture Change

**Before** (current):

```
Sidebar tabs (4): Study (/dashboard) | Practice | Exam (/mock) | Review (/review) [+ Settings via avatar]
/dashboard content: "Today's review pack" — progress bar + active-task grid + completed-today
/review content: manual review pack — list of today's review tasks
```

**After** (Phase B):

```
Sidebar tabs (3): Study (/dashboard) | Practice | Exam (/mock) [+ Settings via avatar]
/dashboard content: Study Hub — two top gauges + three section blocks (see §3)
/review: 404 — deleted from frontend. Backend ReviewService stays (T1.3 deactivation hook still calls it internally; no external callers).
/mistakes: kept, gains a "Practice these" CTA that pre-fills a practice session focused on the listed mistakes.
```

This collapses the "browse the review tasks → click one → answer" flow into "practice → automatic weak-topic surfacing". The user clicks **one button** (Start Practice) instead of choosing from a list of review tasks.

---

## 3. Study Hub Page Layout

`/dashboard` page renders (in vertical order):

### Hero — Two side-by-side gauges

| Element | Source data | Notes |
|---|---|---|
| **Knowledge Coverage** (donut) | New `GET /api/v1/topics/mastery` returns `{topics: [{topic_id, mastery_percent, is_mastered, sub_topics: [{...mastery_percent, is_mastered}]}]}`. Donut shows `mastered_sub_topic_count / total (16)`. | Expandable into the topic grid (8 topics × ~2 sub-topics each, color-coded by mastery state). Click → drilldown panel. |
| **Readiness** (gauge) | Existing `GET /api/v1/readiness` for paid users; locked card with "Get a pass" CTA for free-trial. | Reuses gates rendering from current `/progress` page (commit `1cdc5d4`). Gates: 2-mock-avg 85%, key-cov 90%, review 80%, persistent mistakes. |

### Section 1 — Latest practice / Resume CTA

- If there's an in-progress practice session (`learning.has_in_progress_practice` from `/me`, enriched to include `session_id / answered_count / total_count / entry_type`):
  - **Resume card**: large PlayCircle icon + "Resume your practice (7/30 answered)" + "Continue" button → deep-links to `/practice?resume=<sessionId>`
  - This subsumes the original T-new banner concept.
- Otherwise:
  - **Start card**: small CTA "Start practice" → `/practice`
  - Subtitle hints at next-action ("You have 3 active mistakes in TRAFFIC_CONTROLS — practice will surface them automatically")

### Section 2 — Practice history (with 10-cap + aggregate fallback)

- 10 most recent sessions as horizontal cards / strip:
  - Card content: date (relative), accuracy %, question count, entry_type chip (free-trial vs full), click → session detail page (existing `/[lang]/practice` session view)
- Below cards: aggregate stats — `X total sessions • Y current active mistakes spanning Z topics`
- For sessions beyond 10: no per-session card, just the aggregate (storage / cognitive load constraint per user instruction "超过10个只有数据")

### Section 3 — Mock exam history (with 10-cap + sparkline)

- 10 most recent attempts as a horizontal strip:
  - Strip: small score-percent badges (color-coded green ≥85, amber 70-84, red <70)
  - Sparkline: simple SVG line of score % over time (no charting library if avoidable)
- Below: aggregate — `X total mocks • last 3 average Y%`
- Beyond 10: aggregate only

### Backend endpoints needed

| Endpoint | Returns | Status |
|---|---|---|
| `GET /api/v1/topics/mastery` | Nested topic + sub-topic mastery | New (B5) |
| `GET /api/v1/practice/sessions/history?limit=10` | Recent N sessions w/ accuracy | New (B5) |
| `GET /api/v1/mock-exams/attempts/history?limit=10` | Recent N attempts w/ scores | New (B5) |
| `GET /api/v1/practice/stats` | Aggregate totals (sessions, mistakes by topic) | New (B5) |
| `GET /api/v1/mock-exams/stats` | Aggregate (total attempts, recent avg) | New (B5) |
| `GET /api/v1/me` (extended) | Add `in_progress_practice: {session_id, answered_count, total_count, entry_type}` | Extend AccountService (B5 or B6) |
| `GET /api/v1/readiness` | Existing | Reuse |

All endpoints clamp `limit` server-side (default 10, max 50) to prevent memory blow-up on power users.

---

## 4. AI Question Generation — Control-Theory Pipeline

User's framing: questions must be auto-validated through **negative-feedback gates**, with the DMV runbook as the sole source of truth. No hand-review.

### Pipeline

```
Inputs:    sub_topic_id, target_count (default 5), retry_budget (default 3)
Output:    list<GeneratedQuestion> guaranteed to pass all gates
           OR: log "could not generate N questions for sub-topic X after retries"

1. QuestionGenerator (DeepSeek)
   Prompt: relevant runbook chunks + sub-topic definition → produce N×4 candidate questions
   Each candidate: stem, 4 choices (A-D), correct_choice_key, explanation, en + zh variants

2. FormatValidator (deterministic, no LLM)
   Checks: JSON shape, 4 unique non-empty choices, correct_choice_key ∈ {A,B,C,D},
            explanation length 50-500 chars, both en+zh present.
   Failures filtered immediately.

3. CoverageJudge (DeepSeek mini-prompt)
   For each remaining candidate: "Does this question test the specified sub-topic? Reply JSON {pass, reason}".
   Failure → drop with logged reason; on full batch failure trigger regen with feedback.

4. DifficultyJudge (DeepSeek mini-prompt)
   "Rate distractor plausibility 1-5. Score 1-2 means at least one distractor is obviously wrong."
   < 3 → drop. The harder the question, the better the sub-topic discrimination.

5. RunbookFactChecker (DeepSeek mini-prompt + retrieval)
   Pull the most relevant 1-2 chunks for this sub-topic.
   "Given these handbook excerpts, does the marked correct answer match the handbook? Reply JSON {pass, reason}".
   Failure → drop. Runbook is authority — if the model has an opinion different from runbook, model loses.

6. Pipeline orchestrator
   Generate 4× target_count up front (oversample to account for gate filtering).
   Filter through gates 2→5.
   If passed count < target after first iteration: regen with feedback (concatenate failure reasons into prompt) up to retry_budget.
   Emit final passing list to V16 seed migration.

Cost estimate: ~80 questions × ~4 LLM calls each (gen + 3 judges) × ~2× oversample = ~640 calls × ~$0.002 ≈ $1.30 total.
```

### Why this pipeline shape

- **Format validator is deterministic** — saves LLM calls on shape errors, and shape errors are common in LLM JSON output.
- **Coverage + difficulty + fact-check are three orthogonal axes** — a question can be well-formed, on-topic, but factually wrong; or factually right but trivial. Three judges catch three failure modes.
- **Fact-checker uses retrieved runbook chunks** — keeps the judge prompt small (don't paste the whole runbook every call) and grounds in the source of truth.
- **Retry-with-feedback closes the control loop** — failed candidates inform the next generation prompt, so the system improves rather than just retrying blindly.
- **Oversample to absorb gate filtering** — generate more than target so even with a 60% pass rate we still hit the count.

### What the pipeline does NOT do (yet)

- No human-in-the-loop sampling — user explicitly opted out.
- No multilingual cross-check (en variant says X, zh variant says Y) — could add as 5th gate later if drift becomes a problem.
- No deduplication against existing 53 questions — open gap; B4 should at minimum normalize-and-hash stems to flag duplicates. Add to B4 scope.
- No "is this question fair / unbiased" gate — out of scope; revisit if reports come in.

---

## 5. Phasing & Verification

| Phase | Description | Verify | Pause? |
|---|---|---|---|
| B-1 | Design persistence (this doc + memory + Notion) | this file exists; memory updated; Notion shows Phase B entry | — |
| B0 | Vendor DMV M1 handbook to `docs/dmv-m1-handbook.md` | file has 8+ section headings | — |
| B1 | AI draft 16 sub-topics → `docs/sub-topics.md` | draft file written | **YES — user approval** |
| B2 | V13 sub_topics schema + V14 seed | `mvn clean verify` green; jOOQ regenerated | — |
| B3 | V15 retag 53 questions via AI | all 53 questions have non-null sub_topic_id | — |
| B4 | aiqgen module + V16 seed (~80 new questions) | mvn green; 5 random V16 questions spot-checked | — |
| B5 | Sub-topic mastery + 5 new endpoints | mvn green; curl smoke | **YES — API shape sign-off** |
| B6 | Study Hub UI rewrite + delete /review | npm lint + build clean; manual UI smoke | **YES — visual review** |
| B7 | Cleanup + memory wrap | CLAUDE.md updated; Notion closed; progress §30.3 has full commit trail | — |

End-to-end verify before declaring Phase B done:
- 250+ unit + 7 IT all green
- Frontend lint + build clean
- Manual: login → land on Study Hub → see real mastery numbers → start practice → answer 1 wrong → next-question auto-pulls from same weak sub-topic → mastery updates after enough correct attempts → sub-topic chip changes color on Study Hub
- DeepSeek cost dashboard: total Phase B AI calls under $5

---

## 6. Frozen decisions (2026-05-23, before B0)

1. **Sub-topic list** — Claude drafts 16 sub-topics directly from the vendored DMV M1 handbook (`docs/dmv-m1-handbook.md`). The handbook is the sole authority; sub-topic names + scope follow its section headings, not external taxonomy. User reviews the draft after B1 but the source-of-truth is fixed: runbook → sub-topics, no detours.

2. **Difficulty judge rubric** — "Hard" means *all four choices look defensible at first glance* — closely-worded distractors that force the user to discriminate fine details. A question with even one obviously-wrong distractor fails the gate. Concretely the judge prompt asks: "Is at least one distractor clearly wrong on first read? If yes, fail." This is a binary gate, not the 1-5 scale originally sketched.

3. **Sparkline implementation** — **Hand-rolled SVG** (~30-line React component). No charting library. 10-point line with `scaleLinear` over `[min, max]`, single `<polyline>` element, fixed viewBox, color encoded per point only when below 70%. Skip hover tooltips — the "last 3 avg X%" text below the sparkline already gives the precise number. Zero bundle impact, fits "minimal implementation" principle.

4. **Review module deprecation strategy** — **`@Deprecated`, not delete**. `ReviewController` / `ReviewService` / `ReviewRepository` get `@Deprecated(forRemoval = true, since = "Phase B")` annotations. Internal callers (notably `ReviewService.completeTask`'s mastery-deactivation hook from T1.3 commit `5a79e07`) keep functioning until a follow-up cleanup migration. Frontend deletes the `/review` route + sidebar tab + ReviewPackView, but backend endpoints continue serving 200 to anyone who has a saved URL (defensive). Final delete deferred to a post-Phase-B cleanup pass.

5. **`POST /sessions` `topic_filter` parameter** — **Approved**. Schema: optional `topic_filter: number[]` (array of topic IDs). When supplied, `findNextUnansweredQuestion` constrains the question pool to those topics before applying T1.2 personalization weighting. Empty array or missing field = current behavior (no filter). Used by `/mistakes` "Practice these" CTA which pre-fills the topic IDs of the user's active mistakes. Capped at 8 topics server-side.

6. **Sub-topic mastery threshold** — **Separate config `app.mastery.subtopic.*`** with smaller window:
   - `subtopic-correct-rate-threshold = 0.80` (same as topic, two-gate consistency)
   - `subtopic-recent-attempts-window = 4` (vs topic's 8 — sub-topics have ~5-8 questions, an 8-attempt window forces excessive repetition)
   - `subtopic-recent-attempts-correct-threshold = 3` (≥3 of last 4 correct)

   Two-gate design mirrors topic-level: overall rate ≥80% AND recent 4 attempts ≥3 correct. Initial values; B5 may recalibrate after observing real user data. Topic-level mastery (T1.3) remains untouched — sub-topic is a new orthogonal dimension.

---

## 7. What this redesign explicitly removes

- Manual review pack flow (the surface, not the backend mastery hook)
- "Today's review pack" landing copy
- /review route + sidebar tab
- ReviewPackView, ReviewTaskRunner frontend components (delete)

What stays from the current `/dashboard`:
- nothing visually — full rewrite
- conceptually: the "what should I do next" hint, now expressed via Resume CTA + the implicit "practice will surface weak topics" promise

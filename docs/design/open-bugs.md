# Open bugs / TODO — multi-exam validation (session 6, 2026-06-05)

Logged when token ran low. Pick up here next session. Frontend dev :3000, backend
local :8080 (V29). All prior session-6 fixes pushed (`7bfcb4e` is the last commit
in git; see below for UNCOMMITTED work in the working tree).

## ⚠️ UNCOMMITTED work in the working tree (do NOT lose)

- **B6 — practice question-number off-by-one (FIX WRITTEN, not committed/built).**
  File: `apps/web/src/app/[lang]/practice/PracticeFlow.tsx`.
  Symptom: answer Q14 (wrong) → feedback screen header read "第 15 / 30 题" while
  still showing Q14's feedback (question didn't jump but the counter did). The
  progress bar ("已答题 14/30") was correct.
  Root: in the `feedback` phase `answeredCount` is already incremented to include
  the just-answered question, but the header still computed `answeredCount + 1`,
  reading one ahead of the question on screen.
  Fix already applied: added `displayNumber = Math.min(isFeedback ? answeredCount
  : answeredCount + 1, totalCount)` and the header uses it.
  **NEXT: `npm run lint && npm run build`, then commit + push.**

## Bugs still to fix

- **B7 — switching exam does NOT change practice questions ("只换了UI没换题目").**
  Switching M↔C changes the theme (data-exam) but practice still shows the OLD
  exam's questions; user "can't switch C and M" in practice.
  My session-6 fix (`1608165`) made `useSetExam` navigate to `/dashboard` after
  switching, which should unmount the old session. So the remaining suspect is
  BACKEND scoping: **is `/me`'s `in_progress_practice` scoped by the current
  exam?** If not, after switching to C the dashboard still surfaces the M session's
  Resume card → user resumes → M questions under C theme. ALSO verify the practice
  *pool* (`POST /practice/sessions` start) is scoped to the current exam.
  TO INVESTIGATE: backend `/me` in_progress_practice query + practice session
  start pool selection — both must filter by `ExamContext.resolveExamId(userId)`.
  (Multi-exam foundation §38 claimed practice is exam-scoped; verify in_progress
  on /me specifically.)

- **B8 — mock exam likely has the SAME off-by-one** as B6 (user suspects).
  VERIFY in `apps/web/src/app/[lang]/(app)/mock/[attemptId]/MockExam.tsx` whether
  the per-question "N / total" number is consistent with the question shown after
  answering. (Mock is index-based, so it may be fine — confirm.)

- **B9 — mock attempt URL shows no exam identity.**
  `http://localhost:3000/zh/mock/17` gives no clue it's CA M1 vs CA C. Show the
  exam label (state + license, e.g. "加州 C 类（小汽车）") on the mock attempt
  page (header) so the user always knows which exam they're taking.

- **B10 — guard against manual URL tampering on a mock attempt.**
  If the user manually edits the mock URL (e.g. changes the attempt id / navigates
  away mid-attempt), show a Chrome confirm popup (beforeunload-style) warning that
  leaving VOIDS this mock — i.e. it's treated as an interruption/abandonment
  (scored as-is / terminated). Tie into the existing exit-confirm + the
  auto-terminate semantics. Decide: navigating to another attempt id = void the
  current in_progress attempt.

## Already done this session (pushed)

f0cad03 B5 mock result → Study Hub on all terminal paths ·
891e63d AI cooldown auto-retry ·
1608165 switch→dashboard + /practice data-exam theme ·
c0ee6eb language dropdown selector + de-cram header ·
7bfcb4e per-exam readiness "counted" hint

## Session 6b batch (2026-06-06) — Study Hub / readiness / mock (BIG, mostly backend)

**Root cause found: `SummaryService` (readiness + completion) is NOT exam-scoped.**
`apps/api/.../progressreadiness/application/SummaryService.java` — every query
filters by `user_id` + `learning_cycle` but NOT `exam_id`: `mockStats`,
`keyCoverageStats`, `basicPracticeRatio`, `recentStabilityRatio`,
`hasPersistentMistake`, `findWeakTopics`. In multi-exam this MIXES M1 + C data, so
readiness/coverage are wrong after adding CA-C. **Fix: thread the current exam
(ExamContext.resolveExamId) into all of these and filter `exam_id` (questions,
sessions, mock_attempts, mistake_records all carry exam_id from §38).**

- **B11 — mock result not counted immediately on dashboard.** User finished a mock,
  "近3次平均 0% / 3 次模考" still 0%. Two suspects: (a) the dashboard mock-stats
  endpoint (MockHistoryDao) exam-scoping/status filter — "3 次" but "0%" suggests
  the 3 are non-scored (exit/failure, excluded from avg) OR an exam mismatch;
  (b) front-end invalidation (B5 covered it, verify it actually refetches). Note
  SummaryService.mockStats only counts status in (submitted, ended_by_timeout) —
  correct per spec, but VERIFY the completed mock got one of those statuses.
- **B12 — coverage must include MOCK answers, not just practice ("模考也算覆盖哦").**
  `keyCoverageStats` + the topic-mastery donut count only PRACTICE_ATTEMPTS. Mock
  answers don't count toward coverage. Change coverage = distinct questions
  answered in practice OR mock (UNION mock attempt answers). Applies to the donut
  endpoint (topics/mastery + SubTopicMasteryEvaluator) too.
- **B13 — coverage donut shows 0/16 and doesn't update.** Driven by topic-mastery
  endpoint (sub-topic). Verify exam-scoping + that it counts mock (B12) + why 0.
- **B14 — mock question count must STRICTLY match DMV.** Currently CA_C_30Q and
  CA_M1_30Q both 30. DECISION + data: real counts — CA Class C ≈ 36 Q (pass 30),
  CA motorcycle ≈ 25 Q (pass 21). Confirm official numbers, then per-exam mock
  template question count + `exams.pass_threshold` per exam.
- **B15 — practice session size → 20 (was free 15 / paid 30).** "多了记不住." Make
  practice 20 per session. (Backend caps per entry_type; update + frontend copy.)
- **B16 — let the user choose by-topic at the START of each new practice.**
  topic_filter already exists server-side; add a pre-session chooser: "练习全部 /
  按知识点" + topic pick, asked each time Start practice is pressed.
- **B17 — question content not centered ("题目内容没有居中").** Practice question
  uses `mx-auto max-w-xl text-center` (looks centered) — so check MOCK question +
  the choices list + any other answering surface for missing centering. Sweep all
  answering surfaces.

**Readiness cost answer (for the user):** computed on demand per `/summary` call,
pure SQL (~9 indexed queries scoped by user+cycle), NO AI/LLM cost. The only fixed
cost is the Cloud SQL instance (per-instance, independent of row count / how often
we compute). Client react-query staleTime (60s) already avoids refetch storms. So
"每次都算" is fine — it's cheap arithmetic, not a paid API. If it ever gets hot,
add a short server-side cache or a materialized readiness column, but not needed
now. Precision comes from ReadinessProperties weights + 4 gates (docs/parameters.md
§7-§8), nothing hardcoded.

## Progress (2026-06-06)

Shipped this session: B17 centering (`8b3cf2e`), B15 practice→20 (`9f8ff60`),
B16/B6 done earlier. **B12/B13 coverage-counts-mock: IMPLEMENTED** in
`PracticeHistoryRepository` (subTopicStats + lastNAttemptsForSubTopic now add a
mock path: mock_attempt_results ⋈ mock_attempts, exam-correct via the question's
sub-topic) + TDD test `TopicControllerTest.getMastery_countsMockAnswers_toward
Coverage`. **Both compile + test-compile clean; tests NOT run yet — Docker Desktop
was down all session, so Testcontainers can't start. Run `mvn test` once Docker is
up to verify.**

STILL TODO (backend, need Docker):
- **B11** — mock not in dashboard "近3次平均" / readiness. `SummaryService.mockStats`
  only counts status in (submitted, ended_by_timeout) — correct, but the user's CA-C
  mock likely ended_by_failure (linear auto-terminate at >15% wrong) → excluded.
  VERIFY the completed mock's status + that the dashboard mock-stats endpoint
  (MockHistoryDao) is exam-scoped. Maybe the "0%" is correct-but-confusing (failed
  mocks don't count) → if so it's a UX wording fix, not a calc bug.
- **SummaryService exam-scoping** — mockStats/keyCoverageStats/basicPracticeRatio/
  recentStabilityRatio/hasPersistentMistake/findWeakTopics filter user+cycle but NOT
  exam_id → mixes M1+C. Thread ExamContext.resolveExamId + filter exam_id (questions/
  sessions/mock_attempts have it; mistake_records — verify column). Also make
  keyCoverageStats + basicPracticeRatio count mock answers (same principle as B12).
- **B14** — mock question count strictly DMV. Official (web-confirmed 2026):
  CA Class C = **36 Q / pass 30 (83%)** adult, **46 Q / pass 38** under-18 (persona
  is under-18 — CONFIRM which tier); CA Motorcycle M1 = **30 Q / pass 24 (80%)**
  (our 30 is right; threshold 85%→80%). Needs V30: per-exam mock template question
  count + exams.pass_threshold. DECISION: which C tier (36 vs 46)?
- **B16** — by-topic chooser at practice start (frontend; topic_filter exists).

## Backlog (from earlier)
U4 anonymous free-practice exam choice · D1 dashboard engagement (streak/daily
goal/next-best-action) · Phase 2 per-exam billing + paid remote backup.

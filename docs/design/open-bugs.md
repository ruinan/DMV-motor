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

## Backlog (from earlier)
U4 anonymous free-practice exam choice · D1 dashboard engagement (streak/daily
goal/next-best-action) · Phase 2 per-exam billing + paid remote backup.

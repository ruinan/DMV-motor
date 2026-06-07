# Subscription model — per-exam, jurisdiction-aware

Status: PROPOSAL (2026-06-06). Drives Phase 2 billing. Supersedes the "one global
pass" stub in `access_passes`. Decisions still open are marked **DECIDE**.

## Why

Today an `access_pass` is global: one active pass unlocks *everything* (no
`exam_id`). The product needs subscriptions **per exam** ("按考试类型来"), and the
catalog must grow beyond California — to other states AND to **nationwide** exams
(e.g. a private-pilot / FAA written test, USCIS citizenship, CDL) that aren't tied
to any state. So the model has to express two things cleanly: **what an exam is**
(jurisdiction × type) and **what a user has paid for** (per exam).

## 1. Exam identity — jurisdiction × type

An exam is a (jurisdiction, exam_type) pair, not "state × license_class".

- `jurisdiction`: a **state** (`CA`, `WA`, …) OR **national** (`US`). Add
  `exams.scope ∈ {state, national}`; `state_code` is NULL when `scope=national`.
  - CA Class C → scope=state, state_code=CA, type=C
  - CA Motorcycle → scope=state, state_code=CA, type=M1
  - Private pilot (future) → scope=national, state_code=NULL, type=PPL
- `exam_type` (today `license_class`): the test itself (C, M1, PPL, CDL_A, …).
  Consider renaming `license_class`→`exam_type` later (it's no longer always a
  *license class*); not urgent — keep the column, widen the meaning.
- Display name stays `name_en`/`name_zh` (already free-form), e.g. "California
  Class C (Car)", "Private Pilot (FAA)".

**Grouping for UI**: group exams by jurisdiction — "California", "Washington",
"Nationwide" — so the catalog reads naturally as states are added.

## 2. Two independent gates (don't conflate)

The user said "不 activate 不让切换" + "active 方式回头看（掏钱 or 其他）". That's
actually TWO concepts — keep them separate:

| Concept | Field | Meaning | Effect |
|---|---|---|---|
| **Availability** | `exams.status` (active / coming_soon) | Have WE built/launched this exam (content ready)? | coming_soon exams are *shown* in the catalog as "Coming soon" but **can't be switched to** |
| **Entitlement** | `access_passes.exam_id` (per user) | Has the USER unlocked full features for this exam? | gates full practice / mock / AI for that exam |

- WA Class C before we build it = `status=coming_soon` → visible, not switchable.
- CA Class C with no user pass = `status=active`, no entitlement → switchable +
  **free trial**, but full features locked until subscribed.

**DECIDE**: can a user switch their `current_exam` to an active exam they haven't
paid for? Recommended **YES** — switching is free; you can browse + free-trial any
*available* exam, and full features (full practice / mocks / AI) are what the pass
buys. (This keeps U4 anonymous/free-trial-any-exam consistent and lets users
sample before paying.) The earlier "不允许切换" was about *coming_soon* exams, not
unpaid ones.

## 3. Entitlement = per-exam pass

`access_passes` gains `exam_id` (FK exams). A pass entitles ONE exam.

- `getAccess(userId)` → `getAccess(userId, examId)`: active pass **for that exam**.
  Today's call sites already resolve the current exam via `ExamContext`; thread it
  in. (Mirrors what we just did to SummaryService/practice scoping.)
- Full practice / mock start / AI explain+review-plan check the pass for the
  **current** exam, not a global flag.
- Mock quota (`mock_exam_total_count` / `used_count`) stays on the pass → per exam.
- Free trial is per exam already (questions flagged `allow_in_free_trial`, V30 did
  CA-C). No entitlement needed for the trial.

**Migration**: existing global passes (exam_id NULL) → treat NULL as "all exams"
during a grace window, OR backfill to the user's current_exam. Since billing is
pre-launch and the only passes are dev grant-pass test rows, simplest is: add the
column, make new passes carry exam_id, and have getAccess treat `exam_id IS NULL`
as legacy-global (so dev backdoor keeps working) — or update the dev backdoor to
grant per-exam. **DECIDE** (low stakes pre-launch).

## 4. Pricing & prep cycle — per exam

Per the strategy memory (per-exam price + prep cycle + multi-buy discount; under-18
persona). Put commercial attributes on the exam:

- `exams.price_cents`, `exams.currency` (default usd), `exams.prep_cycle_days`
  (the subscription window length, e.g. 30/60/90 — the "准备周期").
- A purchase creates an `access_pass` with `starts_at = now`,
  `expires_at = now + prep_cycle_days`, `exam_id`, and the mock quota.

**Multi-buy discount / bundles** (future): a `bundles` concept — e.g. "all
California exams" or "any N exams" at a discount. Model later as either (a) a
`bundle` row that, on purchase, mints N per-exam passes, or (b) a discount rule at
checkout. Don't build until single-exam purchase works. Nationwide exams are
standalone (not in a state bundle).

## 5. Rollout phases

1. **Schema** (additive): `exams.scope/price_cents/currency/prep_cycle_days`;
   `access_passes.exam_id`. Backfill CA exams as scope=state. No behavior change yet.
2. **Per-exam access**: `getAccess(userId, examId)` + thread through practice/mock/
   AI gates + `/me` access payload becomes per-current-exam. Sidebar "Free/Subscribed"
   badge (B23①, done) reflects the *current exam's* entitlement.
3. **Catalog UI** (settings): group by jurisdiction; show per-exam state
   (Subscribed / Free / Coming soon); coming_soon not switchable; unpaid switchable
   + "Subscribe to unlock full" CTA. (B19/B20.)
4. **Checkout**: real purchase flow (still stubbed today) → mints a per-exam pass.
   Then multi-buy/bundles.

## 6. Open decisions (DECIDE)

- D1: switching to an unpaid *active* exam — allowed (recommended yes, free trial).
- D2: legacy NULL-exam_id pass semantics (all-exams grace vs backfill).
- D3: bundle model (mint-N-passes vs checkout discount).
- D4: exact prices + prep-cycle lengths per exam (and under-18 pricing).
- D5: nationwide exam codes (US? FAA? per-agency jurisdiction?) — start with `US`.

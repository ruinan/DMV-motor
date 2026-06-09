# Session 9 — bug/feature batch plan (2026-06-08)

User-reported batch (verbatim intent preserved). Build order is CTO's call —
"开始一个一个开发", each item TDD + green + commit + push.

## Where we are
- prod = revision `00073-pzg` / Flyway **V32** (Cloud SQL intentionally STOPPED to
  save money → prod DB 500s until next deploy). Local backend at **V34**.
- Uncommitted in working tree: **reauth gate on dev grant/revoke pass** (DevController +
  DevControllerTest, 6/6 green last session) → lands as the first commit of this batch.
- Approved-but-queued (NOT in this batch): **reCAPTCHA Enterprise bot verification**
  (key `6LdHnRQt…` already created for project dmv-motor-prod, no code yet). Resumes
  after this batch.

---

## Item 0 — land in-flight reauth on dev grant/revoke pass  ✅ done, commit only
Already implemented + tested. `reauthGuard.requireRecentReauth()` on `/dev/grant-pass` +
`/dev/revoke-pass` (subscription change → recent password proof), mirroring the Stripe
checkout/cancel gating. Just needs a clean baseline + commit + push.

---

## Item A — bug2: mistakes never clear + show mastery progress  (HIGHEST VALUE)

**Root cause (confirmed):** `PracticeService.submitAnswer` *creates* a mistake on every
wrong answer but **never deactivates** one. The only caller of
`MistakeListRepository.deactivateForTopic(...)` is the **deprecated** `ReviewService.
completeTask` (the `/review` route tree the Study-Hub frontend deleted). So in the live
practice flow active mistakes accumulate forever → user is permanently "stuck on
交通标志与信号" and the next-step recommendation (driven by active-mistake topic counts)
never updates "无论我做多少次".

**Fix — two parts:**

1. **Wire mistake-resolution into the live practice path (backend).**
   After a *correct* `submitAnswer` for the answered question's topic, re-evaluate that
   TOPIC's mastery using the existing topic-level `MasteryEvaluator` (window 8, rate ≥80%,
   ≥6/8 recent) over the user's practice history for `(topic, cycle)`; if mastered,
   `mistakeListRepo.deactivateForTopic(userId, topicId, cycle)`. Mirrors what
   `ReviewService` did, but triggered from the path users actually use. Topic-level (not
   sub-topic) because `deactivateForTopic` is topic-scoped and that's the gate that
   un-sticks the recommendation.
   - Reuse `PracticeHistoryRepository.topicStats` + `lastNAttemptsForTopic` (already feed
     ReviewService).
   - **TDD:** user with an active mistake in topic T → answer enough T questions correctly
     to cross the gate → `countActive` drops, recommendation no longer surfaces T. Also a
     not-yet-mastered case stays active.

2. **Expose topic mastery PROGRESS for a progress bar (backend + frontend).**
   Extend `GET /topics/mastery` with, per topic, the gate components: `accuracyPercent`
   (correct/attempted), `recentCorrect`/`recentWindow`, the thresholds, and a derived
   `masteryProgressPercent` = clamp(min(accuracy/threshold, recentCorrect/recentThreshold)).
   Frontend renders a per-topic progress bar (coverage + accuracy) with "还差 X 道正确就能
   进入下一个 topic" so the blockage is legible. Connects to bug4's practice-mode copy.
   - **TDD:** mastery view returns progress fields; 70%-accuracy topic shows < 100 and not
     mastered.

**Scope:** progress bar reflects the TOPIC-level gate (the one that clears mistakes), shown
on the Study Hub mastery view. Does not change the sub-topic donut's mastery booleans.

---

## Item B — bug4: practice modes + 10 questions/session + paid-gate next-step

User: connect "你在 1 个知识点有 12 道未消化的错题…自动加权薄弱点" to #2; offer practice-mode
choices for **paid** ("随机练习已学内容" / "加强错题复习"); **free = pure random only**;
**10 questions each time**; **no next-step suggestion for free**.

1. **10 questions/session.** `FREE_TRIAL_QUESTION_CAP` and `FULL_QUESTION_CAP` → **10**
   (currently 15 / 20, comment drifted to 30). Update `capFor` + comments.
   - **TDD:** `capFor` returns 10 for both; session ends after 10 answers.

2. **Practice `mode` (backend-enforced).** Add a `mode` to start-practice:
   - `random` — pure random within the pool, NO weak-point weighting. Default; the ONLY
     mode free users get (forced server-side regardless of requested mode).
   - `review_mistakes` — current personalized weighting (mistakes + key topics); **paid**.
   - `random_learned` — unweighted but pool restricted to already-covered topics; **paid**.
   `PracticeQuestionSelector` gains a non-weighted random path + a covered-topics filter;
   the existing weighted query becomes the `review_mistakes` branch. Non-paid requests are
   downgraded to `random` server-side (limits enforced on backend, not just UI).
   - **TDD:** free start ignores `mode` → random; paid `review_mistakes` weights toward
     mistake topics; paid `random_learned` restricts to covered topics.

3. **Connect copy to mode (bug4 ↔ bug2).** The "N undigested mistakes, weighting weak
   points" line only shows when the chosen mode actually weights mistakes, and links to the
   topic-progress view (#2). Free idle screen says "pure random practice".

4. **Next-step recommendation = paid-only.** Hide the EngagementStrip next-step card for
   free users AND have `GET /recommendations` return empty for users without an active pass
   (no weak-point leak; backend-enforced). Streak + daily goal stay for everyone.
   - **TDD:** `/recommendations` empty for no-pass user; non-empty for paid.

**Scope:** mode picker only on the signed-in practice idle screen; anonymous keeps the
existing per-exam free-trial buttons (pure random). Daily-goal/streak unchanged.

---

## Item C — bug3: streak needs a unit
Show a unit on the streak number ("天" / "days") so "连续打卡" reads clearly. i18n string +
`EngagementStrip` layout tweak. Trivial; bundle near Item B's frontend.

---

## Item D — bug1: backup → single latest, restorable, auto-synced, incremental
User: settings shouldn't allow unlimited backups — keep **one latest**; allow **restore**
from it; make it **system-driven / user-imperceptible** (if the client lacks the latest,
download then load); **incremental** uploads (don't hammer the backend); a backend job to
**periodically compress**.

**Reality check:** the server is already the source of truth for progress (practice/mock/
mistakes live in Postgres keyed by user_id) — so cross-device "restore" is mostly moot
(just log in). The genuinely valuable restore is **learning-cycle-reset / fresh-cycle
recovery** + a portable snapshot. The current V33 model is an *append-only list of derived
summaries* with a manual "Backup now" button and no restore.

**Recommended design (single-slot cloud-save):**
- **V35 `progress_backups`**: `UNIQUE(user_id, exam_id)`, `payload` (gzip'd JSON bytea),
  `content_hash`, `updated_at`. Upsert (`ON CONFLICT DO UPDATE`) → always exactly one
  latest row per user+exam.
- **Restorable payload**: the bits that survive a reset — active mistake list (question_id,
  topic_id, wrong_count, source) + readiness/completion + mock/practice summaries.
  Compressed → small rows.
- **Auto-sync (transparent)**: frontend fires a debounced background backup after session/
  mock completion; backend computes payload + hash; **if hash == stored, no-op** (this IS
  the "incremental" — only persist on change). No manual "Backup now" as the primary path;
  keep a Restore button + "last synced" status.
- **Restore** `POST /backup/restore`: re-applies the backed-up active mistakes into the
  user's CURRENT cycle (idempotent upsert, same dedup as practice). Owner-gated.
- **Periodic compaction**: a `@Scheduled` nightly job that prunes superseded rows / drops
  the old V33 append-list / re-compresses. Minimal.
- **Settings UI**: replace the snapshot list with "Last backed up: <time>" + Restore.

**Decision flags (will confirm before heavy build):**
- (D1) restore semantics = re-apply mistakes into current cycle (vs full attempt replay —
  rejected as risky on a live DB). **Recommended: mistakes-only.**
- (D2) require reauth on restore? **Recommended: no** (only re-adds your own mistakes).
- (D3) new V35 table vs migrate V33. **Recommended: new table, deprecate V33 list.**

Ordered after A/B/C because it's the heaviest + has the only real ambiguity.

---

## Item E — feature1: privacy policy + terms (legal-facing)
Add `/[lang]/privacy` + `/[lang]/terms` pages with real content reflecting actual data
practices: Firebase auth (email/uid), practice/mock data, Stripe payments, DeepSeek AI
(with the no-user-id privacy contract), cookies/localStorage, retention, deletion/reset
rights, and **minors** (target users are <18 → COPPA/CCPA / “Do Not Sell” + parental
consent for under-13). Footer links, EN + ZH. **Marked "needs legal review before
production"** — drafted to be close to compliant, not legal advice.

---

## Execution order
0. Land reauth (done) → commit + push.
1. **Item A** — bug2 (mistake resolution + mastery progress bar).
2. **Item B** — bug4 (10q + practice modes + paid-gate next-step).
3. **Item C** — bug3 (streak unit), bundled with B's frontend.
4. **Item D** — bug1 (backup single-slot/restore/auto-sync) — confirm D1–D3 first.
5. **Item E** — feature1 (privacy + terms).

Then resume queued **reCAPTCHA bot verification** (approved).

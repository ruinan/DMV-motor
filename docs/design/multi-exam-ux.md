# Multi-Exam UX & Monetization Design

> Status: PROPOSAL (2026-06-04) — awaiting user approval before implementation.
> Drives the multi-exam experience now that the app covers more than one exam
> (California Class C car + M1 motorcycle, expanding to other states/licenses).
> Source: user direction across the 2026-06-04 testing session.

---

## 0. Known bugs — fix FIRST (user direction: 先修 bug，再 UIUX)

| ID | Bug | Likely cause / fix |
|---|---|---|
| **B1** | Reset learning state → **readiness not cleared to 0** | `resetLearning` bumps `reset_count` (cycle), but readiness (and the reset action) must (a) be computed for the current cycle and (b) **invalidate the frontend query cache** after reset so the UI reflects the cleared state. |
| **B2** | After clear/reset, practice stats still show **"0 sessions · 3 mistakes"** | The practice-stats query counts sessions by current cycle (→0) but the **mistakes count isn't cycle-scoped** (→3). Filter mistakes by current `learning_cycle` (+ exam). |
| **B3** | Mock **per-question review (逐题复盘) empty** after finishing — only AI plan, no questions, no loading | `MockReview` returns `null` when `items` is empty (no loading/empty state). Backend DOES return questions+saved_answers for finished attempts, so verify question_id matching / the `?review=1` link; add a **loading + empty state**; review loads from cache (no AI) — **only deep-dive calls AI**. |
| **B4** | Readiness **"应考准备度" info icon not clickable** | The info/tooltip trigger on the readiness ring doesn't open. |
| **B-reset-UX** | (related to B1/B2) | Add a **confirm-warning dialog** before reset/clear ("don't casually clear — you'll lose important data"; paid users are backed up, free users aren't). |

---

## 1. User persona (NEW requirement)

- **Primary segment: under-18 learners who are internet-savvy.** They use
  computers/the web fluently.
- **Implication for UX**: we can use a richer, desktop-capable interface —
  dropdowns, dashboards, multi-exam management, keyboard affordances. Do **not**
  dumb it down to a lowest-common-denominator mobile-only flow. Still responsive,
  but desktop-first is acceptable.
- Persona recorded in memory `project_strategy.md`.

---

## 2. Monetization model (per-exam) — direction, not yet built

- **Each exam (state × license) is a separately priced product.** Buying it
  grants access to that one exam.
- **Each exam has its own prep cycle** (access pass = duration/quota), scoped per
  exam — reuse the existing access-pass/quota machinery, keyed by `exam_id`.
- **Multi-buy discount**: buying several exams together = a bundle discount.
- **Free trial is per-exam**: a limited amount of free practice per exam before
  purchase (the existing free-trial concept, now scoped by exam).
- **Paid users get remote backup/sync** of their key data so a reset/clear (or a
  new device) doesn't lose it: the **last 3 mock results**, the **last N practice
  results** (N ≈ 3, the history-retention count), and the **overall learning
  progress + readiness/score**. Free users only have local/current-cycle data and
  are warned before clearing (see bug B-reset). This is the value behind "don't
  casually clear history."
- **Today**: payment is still a stub (user pinned "先不做付费"). This model only
  shapes the UX below (owned vs not-owned exam states); the billing build is a
  later phase (with payment + anti-abuse, per roadmap §34-C).

---

## 3. Exam-selection entry points — where & when

| Entry point | Who | Purpose |
|---|---|---|
| `/start` onboarding (DONE) | logged-in, no exam | First, forced choice after login |
| **Anonymous free-practice chooser** (NEW) | anonymous | Pick which exam to free-practice from the landing CTA (today it silently defaults to M1) |
| **Prominent in-app switcher** (REDESIGN) | logged-in | Always-visible current exam + switch; replaces the too-subtle sidebar dropdown |
| `/me` settings exam section (exists) | logged-in | Full list; later: which exams owned, prep-cycle status, **buy more** |
| Dashboard header chip (exists) | logged-in | Shows current exam prominently |

---

## 4. When can you switch — rules

- **Allowed anytime** from the prominent switcher, EXCEPT:
  - **During a mock-exam attempt** (focus mode): switcher hidden; switching not
    allowed mid-exam.
  - **Mid practice session**: switching abandons the in-progress session →
    **confirm dialog** ("Switching will end your current practice session").
- **Ownership (future billing)**: switching to an **owned** exam → full access.
  Switching to a **not-yet-owned** exam → drops into that exam's **free trial**
  (limited) with a clear "Unlock full access" CTA. Anonymous users move freely
  between exams' free trials.
- Switching **re-scopes everything** (questions, history, mistakes, mock,
  progress, recommendations, reminders) — already implemented backend-side.

---

## 5. Prominent exam switcher (redesign)

Problem: the current switcher is small muted text under the brand in the sidebar
— users miss it and feel "stuck" on one exam.

Proposal:
- **Sidebar**: a bordered **exam card/button** at the top (exam icon + name +
  chevron), visually a clear control — not muted text. Tinted with the exam's
  accent color (see §6).
- **Mobile**: same prominent control in the top bar.
- **Switch surface**: opens a panel/sheet listing exams. Each row: icon, name,
  and (future) owned/free-trial badge + price. Switching shows the re-scope
  loading state (already have `useSetExam` → invalidate all).
- Keep the dashboard "Preparing for: <exam> ▾" chip as a secondary entry.

---

## 6. Per-exam theming (visual context cue)

Goal: the user always **knows which exam they're in** at a glance.

- Each exam gets a **distinct accent palette** (and a subtle background tint).
  - Class C (Car): blue (current `--primary: #1b5e9b`).
  - M1 (Motorcycle): a warm hue (e.g., amber/orange).
  - Future exams: additional hues from a fixed, accessible palette.
- **Implementation**: a `data-exam="<state>-<license>"` (or a palette key) on the
  app-shell root; CSS overrides the token values (`--primary`, `--accent`, a
  faint `--background` tint) per exam. Keep WCAG contrast on text/buttons.
- The switcher, dashboard accents, and CTAs pick up the exam color. Marketing/
  landing stays neutral (multi-exam).
- Anonymous free-practice also themes by the chosen exam.

---

## 7. Anonymous free-practice exam choice (NEW)

- Today: landing "Start free practice" → `/practice` → backend defaults to M1.
- Proposal: anonymous users can **choose the exam to free-practice** — either a
  small chooser before practice or a prominent switcher on the practice screen
  (reusing the §5 control, no login required). The exam picks the theme (§6) and
  scopes the free questions.

---

## 8. AI cooldown on the button (concrete fix)

Today: on `RATE_LIMITED`, a separate text message shows ("AI is cooling down…").
The user wants it **on the button**:
- On cooldown, the AI button goes **disabled** and shows **"AI cooling down (Ns)"**
  with a **live countdown** (seconds from the backend's retry-after).
- When the countdown hits 0, the button **auto-re-enables**.
- Applies to all AI buttons (explain + the deep-dive aspect buttons), via the
  shared `ai-explain-block` + the three AI surfaces.
- Backend already returns the wait seconds in the error message — surface it as a
  number; ideally expose it as a structured field (e.g. `retry_after_seconds`)
  rather than parsing the message.

---

## 9. Proposed sequencing

**Phase 1 — UX (no payment), implement after approval:**
1. AI cooldown button (small, self-contained).
2. Prominent exam switcher (sidebar + mobile).
3. Per-exam theming (`data-exam` + token overrides; pick the M1 palette).
4. Anonymous free-practice exam choice.

**Phase 2 — Monetization (later, with payment + anti-abuse):**
5. Per-exam access pass + prep cycle + free-trial-per-exam gating.
6. Multi-buy bundle discount + "buy more exams" in /me.
7. Owned/free-trial badges in the switcher; switch-to-unowned → free-trial + unlock CTA.

---

## Open decisions for the user

1. **Theming**: OK to assign Class C = blue, M1 = amber/orange? Any brand colors?
2. **Switcher form**: sidebar exam-card + slide-out panel (proposed) vs a simpler
   prominent dropdown?
3. **Anonymous free-practice**: chooser-before-practice vs switch-on-practice-screen?
4. **Sequencing**: start Phase 1 in the order above, or reprioritize?

# Study Hub v1 Design

Date: 2026-05-12
Status: Active frontend workstream

## Vision

Study Hub is the post-login home for DMV Motor. It should answer three user questions without requiring the user to manage study mechanics:

1. How much have I covered?
2. Am I ready to take the California M1 written exam?
3. What happened in my recent practice and mock attempts?

Review is not a primary destination. Review is a selection strategy inside Practice. Users should not need to decide to "go review"; the system should automatically bias practice toward weak topics, recent mistakes, high-risk knowledge, and mock-exposed gaps.

## UI-First Decision

Build the Study Hub UI first using current APIs and carefully designed empty, locked, and loading states. Backend schema expansion, sub-topic mastery, and AI-generated question growth come later after the frontend loop exposes the functional holes.

This deliberately avoids blocking the Study Hub on:

- Sub-topic schema.
- California DMV M1 handbook ingestion.
- AI question generation and validation.
- Retagging existing questions.
- New history endpoints.

## Information Architecture

Primary app destinations:

- Study: global state, coverage, readiness, recent history, next action.
- Practice: starts or resumes smart practice. Practice absorbs review behavior.
- Exam: mock exam simulation and result history.
- Settings: account, subscription/access, language, learning reset.

The visible `/review` destination should be de-emphasized during v1 and eventually removed or converted into an internal implementation detail after smart practice fully replaces the standalone review pack workflow.

## Study Hub Shape

Top-level signals:

- Knowledge Coverage: current coverage/mastery view. In v1 this can use existing summary/completion data; later it should become sub-topic mastery.
- Readiness: pass-readiness state. Paid users see readiness data; unpaid users see a locked card with upgrade copy.

Core sections:

- Latest Practice: current/in-progress practice when available, otherwise a start-practice CTA.
- Practice History: latest 10 practice sessions with detailed cards. Older practice data should be summarized as aggregate stats, not shown as endless detail.
- Mock History: latest 10 mock attempts with score trend or compact score cards. Older mock data should be aggregate stats.

Supporting links:

- Mistakes can remain accessible for manual inspection, but they should not be the main way to recover weak areas.
- Progress can be folded into Study or kept as a secondary detail page linked from Study.

## Frontend Implementation Slice

First implementation should:

- Rewrite `apps/web/src/app/[lang]/(app)/dashboard/Dashboard.tsx` into a Study Hub.
- Read existing theme and design tokens before making UI changes.
- Preserve the established app-shell style: restrained operational UI, `bg-card`, `border-border`, `text-muted-foreground`, `bg-primary`, lucide icons, compact cards.
- Add route parent mapping so `/review*`, `/mistakes`, and `/progress` highlight Study if those routes still exist.
- Use current APIs where available and show explicit "backend needed" empty states where APIs are missing.
- Keep mobile usable: no hidden required action below long card lists; section hierarchy must scan quickly.

## Backend Follow-Up

After the UI loop is visible, fill backend gaps:

- Active/latest practice details.
- Practice history endpoint capped at 10 detailed sessions.
- Mock history endpoint capped at 10 detailed attempts.
- Aggregate stats for older practice/mock data.
- Knowledge coverage endpoint, eventually sub-topic based.
- Smart practice selection that absorbs review behavior.

## Open Decisions

- Whether `/review` is removed entirely or only hidden until smart practice fully replaces it.
- Whether Settings remains in the main tab list or moves to the user menu.
- Exact shape of the Knowledge Coverage card before sub-topic mastery exists.
- Whether mock score trend uses a simple inline SVG sparkline or compact cards only for v1.

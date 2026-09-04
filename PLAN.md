# SproochenCoach Admin Feature Plan

## Purpose

This file records what has already been implemented for the current admin/backend roadmap and what still needs to be built.

## Current Findings

### 1. Token Usage Tracking

Status: implemented for the current admin dashboard scope.

Implemented:

- `AiUsage` exists as a JPA entity mapped to the `ai_usage` table.
- `AiUsageRepo` supports paged per-user lookup and full per-user summary lookup.
- `AiUsageService` records chat token usage for the authenticated user when token data is available.
- `AiUsageService` supports generic usage units for non-chat calls.
- `AiUsageCostService` estimates USD cost from configurable provider/model prices.
- `AiChatClient` extracts token usage from OpenRouter-compatible responses and Anthropic/Kimi-compatible responses.
- `AiImageClient` records one usage row after successful image generation.
- `AudioExerciseGenerationService` records one usage row after successful ElevenLabs TTS generation.
- `SpeakingService.transcribeAudio(...)` records one usage row after successful Groq Whisper transcription.
- Recording endpoints accept optional `durationSeconds`; Groq STT records audio seconds when supplied and falls back to uploaded bytes otherwise.
- Admin endpoints expose per-user AI usage:
  - `GET /api/admin/users/{id}/ai-usage/summary`
  - `GET /api/admin/users/{id}/ai-usage`
- `GET /api/admin/users/{id}/ai-usage` supports optional `from`, `to`, `provider`, and `model` filters.
- AI usage filters use Spring Data JPA Specifications instead of hardcoded JPQL query strings.
- `AdminAiUsageDto` exposes `estimatedCostUsd`.
- `AdminUserDetailDto` includes an `aiUsage` summary with `totalEstimatedCostUsd`.

Still future scope:

- `AiUsage` stores `userId` as a scalar instead of a `ManyToOne` relationship to `AppUser`; acceptable for an MVP audit table, but less expressive for future joins.
- There is no provider/model aggregation endpoint yet.
- Cost values are estimates based on configured rates. They must be reviewed when provider pricing changes.

Recommendation:

- Keep the current `AiUsage` table.
- Keep provider prices in configuration so they can be updated when pricing changes.
- Add provider/model aggregation later only if the dashboard needs charts or grouped totals.

### 2. Prompt Management

Status: implemented for the current admin dashboard scope.

Implemented:

- Prompt content is stored in text files under `src/main/resources/prompts`.
- `PromptTemplate` stores an admin-editable overlay for a known prompt key.
- `PromptTemplateRepo` persists prompt overlays in `prompt_templates`.
- `PromptTemplateService` validates editable content so admins can change teaching guidance, not technical prompt rules.
- `AdminPromptController` exposes prompt CRUD under `/api/admin/prompts`.
- Prompt CRUD mutations are audited.
- `PromptFileService` merges the locked file prompt with the optional admin-editable guidance.
- Exercise, speaking, listening, vocabulary, image-description, and transcription services resolve prompt content per request, so admin updates apply without restart.

Still future scope:

- No prompt versioning, publish state, or rollback path.
- The admin can only create overlays for known prompt keys, not arbitrary new AI prompt surfaces.
- `BaseModel` was not used because it is not currently marked as a JPA mapped superclass.

Recommendation:

- Keep prompt files as locked technical defaults and database rows as editable teaching guidance only.
- Add versioning later before allowing larger prompt edits or rollback from the admin UI.
- If `BaseModel` should be reused later, first convert it to `@MappedSuperclass` in a separate cleanup.

### 3. Admin Audit Trail

Status: implemented for current admin mutations.

Implemented:

- `AppUser.adminDisabled` exists.
- `PATCH /api/admin/users/{id}/status` updates the target user's admin-disabled state.
- Self-disable is blocked for admins.
- JWT/account validation prevents admin-disabled users from authenticating.
- `AdminAuditLog` records actor user ID, target user ID, action, old value, new value, optional reason, and timestamp.
- User status changes are logged in the same transaction as the status update.
- Prompt overlay and exercise config mutations are audited.
- `GET /api/admin/audit-logs` exposes paged audit logs with optional `actorUserId`, `targetUserId`, `targetType`, `targetId`, and `action` filters.

Still future scope:

- Audit logs store scalar user IDs, not user snapshots.

Recommendation:

- Keep audit logging focused on mutable admin actions.
- Add new action names as each new admin mutation is introduced.
- Consider storing actor/target email snapshots later if admin history must survive user deletion or email changes.

### 4. Completed Exercise Tracking

Status: implemented for the current progress dashboard scope.

Implemented:

- `ExerciseAttempt` records generated exercise instances with user, exercise type, level, topic, answer type, status, optional learner answer, optional score, and lifecycle timestamps.
- Generated text, vocabulary, listening, speaking, and image-description responses now include `attemptId`.
- `POST /api/progress/exercises/{attemptId}/complete` marks the current user's attempt as `COMPLETED`.
- Speaking and image-description evaluations can receive optional `attemptId` and mark that attempt as `EVALUATED`.
- Evaluation without `attemptId` still records a standalone evaluated attempt for backward compatibility.
- `GET /api/progress/me` now reports generated, completed, and evaluated counts from attempts.
- Admin can view per-user attempts through the existing `GET /api/admin/users/{id}/progress` endpoint.

Still future scope:

- Existing legacy `UserProgress` rows are not migrated into `ExerciseAttempt`.
- There is no persisted full generated exercise payload yet.
- There is no dedicated answer-correctness evaluation for text/vocabulary/listening completion yet.

Recommendation:

- Keep `ExerciseAttempt` as the lifecycle source of truth.
- Add payload persistence later only if the frontend needs history replay.
- Add answer checking per exercise type after the attempt lifecycle stabilizes.

### 5. Editable Exercise Config

Status: implemented.

Implemented:

- `ExerciseLevelConfig`, `ExerciseTopicConfig`, and `ExerciseTypeConfig` store editable exercise configuration in the database.
- `ExerciseConfigDataInitializer` seeds current MVP levels, topics, and exercise types.
- `GET /api/admin/exercise-config` returns configured levels, topics, and exercise types with `editable=true`.
- Admin CRUD endpoints exist for levels, topics, and exercise types under `/api/admin/exercise-config`.
- Exercise config mutations are audited.
- `ExerciseRequestDto` now uses string codes instead of Java enum fields.
- Exercise generation validates level, topic, type, enabled state, and topic-to-level relationship against database config.
- Legacy enum models were removed because the frontend contract will move to string codes.

Still future scope:

- There is no ordering/display-priority field yet.
- Existing generated attempts store string codes; if codes are renamed later, historical attempts keep the old code.

Recommendation:

- Prefer disabling config rows over deleting them once real learner history exists.
- Add display ordering before the admin UI needs drag-and-drop ordering.
- Add audit snapshots later if config rename history must remain human-readable after labels change.
- Audit config mutations if admins start editing production data regularly.

### 6. Launch-Critical Application Logic Review

Status: critical issue fixed.

Implemented:

- Public user-management routes were reviewed for launch risk.
- `GET /api/users`, `GET /api/users/{id}`, and `PUT /api/users/{id}` are now admin-only.
- `/api/users/me` remains the authenticated current-user endpoint for normal users.
- Forbidden method-security failures now return the unified `ApiResponse` shape with HTTP `403`.
- Learner exercise screens now have read-only `GET /api/exercise-config`.
- `GET /api/exercise-config` returns only enabled levels, topics, and exercise types.
- Admin exercise config remains under `/api/admin/exercise-config` for protected editing.
- AI chat provider selection now happens per authenticated request instead of globally.
- BASIC users route through `ai.chat.basic.provider` / `ai.chat.basic.model`.
- Premium users route through `ai.chat.premium.provider` / `ai.chat.premium.model`.
- Image provider selection is separated into `ai.image.provider`.

Recommendation:

- Keep general user administration under admin-only access.
- Add a dedicated `PUT /api/users/me` later if normal users need profile editing.
- Do not reuse ID-based user update routes for self-service profile editing unless ownership checks are explicit.
- Update the frontend practice config service to call `/api/exercise-config`, not `/api/admin/exercise-config`.
- Keep provider/model values in deployment configuration, not the admin dashboard.

### 7. AI Quota and Cost-Control Plan

Status: implemented for backend MVP quota enforcement.

Purpose:

- Prevent free/basic users from generating unlimited provider costs.
- Keep Stripe subscription revenue connected to AI usage limits.
- Reuse existing `AiUsage` records instead of introducing a separate usage ledger too early.
- Keep quota rules in backend configuration first; avoid admin-editable quotas until the product pricing is stable.
- Count successful provider requests in the first version; do not count tokens, exact USD, or frontend button clicks as quota units.
- Treat MVP quota enforcement as best-effort under concurrent requests; a small temporary overage is acceptable before adding atomic reservations.
- Fail closed when successful provider usage cannot be recorded, because `AiUsage` is now quota accounting, not only observability.

#### Phase 1: Define Product Quotas

Status: implemented.

Goal:

- Decide what each plan is allowed to consume before writing enforcement code.

Implemented:

- DONE: `UserPlanTier` now defines `BASIC` and `PREMIUM`.
- DONE: `UserPlanTierResolver` now centralizes the current user's tier decision.
- DONE: `AiModelRouter` now uses `UserPlanTierResolver` instead of duplicating subscription logic.
- DONE: Premium access is still based on subscription statuses handled by `SubscriptionAccessService`: `active` and `trialing`.
- DONE: Quota windows are defined:
  - `BASIC`: daily limits.
  - `PREMIUM`: monthly limits.
- DONE: Quota categories are defined:
  - Chat exercise generation.
  - Speaking/listening TTS audio generation.
  - STT recording transcription.
  - Image generation.
- DONE: Consumed quota units are defined:
  - `CHAT`: one successful LLM provider call.
  - `TTS`: one successful ElevenLabs generation.
  - `STT`: one successful transcription.
  - `IMAGE`: one successful image generation.
- DONE: Conservative launch defaults are defined below.
- DONE: Launch quota numbers are adjustable assumptions, not permanent product promises.

Follow-up:

- DONE: Backend quota configuration, usage counting, and enforcement are implemented in Phases 2-5.

Recommended MVP defaults:

- `BASIC`: 20 chat generations per day.
- `BASIC`: 5 audio generations per day.
- `BASIC`: 5 STT evaluations per day.
- `BASIC`: 0 or 2 image generations per day.
- `PREMIUM`: 300 chat generations per month.
- `PREMIUM`: 75 audio generations per month.
- `PREMIUM`: 75 STT evaluations per month.
- `PREMIUM`: 10 image generations per month.

Review after launch:

- Increase limits only after real `AiUsage` cost data confirms the subscription price has enough margin.
- Prefer raising limits later over launching with generous limits and reducing them after users subscribe.

#### Phase 2: Add Quota Configuration

Status: implemented.

Goal:

- Make limits configurable per environment without changing code.

Implemented:

- DONE: Added properties under `ai.quota.basic.*` and `ai.quota.premium.*`.
- DONE: Added one property per feature category and quota window.
- DONE: Added `AiQuotaProperties` for typed quota configuration.
- DONE: Kept quota values in `application.properties` for local defaults.
- DONE: Added environment-variable overrides for production quota values.
- DONE: Kept provider/model selection separate from quota limits.
- DONE: Tier resolution remains shared through `UserPlanTierResolver`.

Still To Do:

- Avoid storing production quota overrides in Git; set them in the deployment environment.

Example properties:

- `ai.quota.basic.chat.daily-limit=20`
- `ai.quota.basic.tts.daily-limit=5`
- `ai.quota.basic.stt.daily-limit=5`
- `ai.quota.basic.image.daily-limit=0`
- `ai.quota.premium.chat.monthly-limit=300`
- `ai.quota.premium.tts.monthly-limit=75`
- `ai.quota.premium.stt.monthly-limit=75`
- `ai.quota.premium.image.monthly-limit=10`

#### Phase 3: Add Usage Counting Queries

Status: implemented.

Goal:

- Count current-period usage efficiently from existing `AiUsage` rows.

Implemented:

- DONE: Added JPA Specification-based counting by `userId`, quota category, and date range.
- DONE: Added canonical quota category enum values: `CHAT`, `TTS`, `STT`, and `IMAGE`.
- DONE: Mapped current usage records into categories:
  - Chat: `recordChatUsage(...)` from `AiChatClient`.
  - TTS: ElevenLabs usage from `AudioExerciseGenerationService`.
  - STT: Groq Whisper usage from `SpeakingService`.
  - Image: image usage from `AiImageClient`.
- DONE: Added `AiUsageService.countUserQuotaUsage(...)` for quota-window counting.
- DONE: Kept cost estimation separate from quota counting.
- DONE: Chat usage now records successful provider calls even when token metadata is missing.

Still To Do:

- Add optimized aggregate queries later only if `AiUsage` grows large enough to make Specification counts slow.

#### Phase 4: Implement Quota Enforcement Service

Status: implemented.

Goal:

- Centralize quota checks so feature services do not duplicate business rules.

Implemented:

- DONE: Created `AiQuotaService`.
- DONE: Reused `AiQuotaCategory` enum values: `CHAT`, `TTS`, `STT`, `IMAGE`.
- DONE: Resolved the current user's tier through `UserPlanTierResolver`.
- DONE: Shared tier resolution now prevents `AiModelRouter` and `AiQuotaService` from disagreeing.
- DONE: Resolved the active quota window for the tier and category.
- DONE: Counted usage for the current user/category/window through `AiUsageService.countUserQuotaUsage(...)`.
- DONE: Added dedicated `AiQuotaExceededException`.
- DONE: Returned clear API error messages such as `Daily AI limit reached for chat` and `Monthly premium AI limit reached for chat`.
- DONE: Added `AiQuotaExceededException` to `GlobalExceptionHandler` using HTTP `429 Too Many Requests`.

Still To Do:

- Add quota reservations only if concurrent overage becomes a real abuse/cost problem.

#### Phase 5: Add Quota Checks Before Provider Calls

Status: implemented.

Goal:

- Block expensive calls before reaching OpenRouter, Kimi, Groq, or ElevenLabs.

Implemented:

- DONE: Checked `CHAT` quota before `AiChatClient.complete(...)` sends a chat request.
- DONE: Checked `TTS` quota before ElevenLabs TTS generation.
- DONE: Checked `STT` quota before Groq transcription.
- DONE: Checked `IMAGE` quota before image generation.
- DONE: Kept usage recording after successful provider calls.
- DONE: Kept failed provider calls out of quota consumption for the MVP.
- DONE: Failed closed when successful provider calls cannot be recorded in `AiUsage`.
- DONE: Failed closed when usage recording has no authenticated user to attach quota consumption to.
- DONE: Logged quota rejections with user ID, category, tier, current count, and limit.
- DONE: Documented that the MVP check-then-call-then-record flow can allow minor overage under simultaneous requests.
- DONE: Deferred Redis, database locks, and quota reservations until abuse or real cost data justifies the complexity.

#### Phase 6: Expose Quota Status to the Frontend

Status: implemented.

Goal:

- Let the UI show remaining usage before users hit errors.

Implemented:

- DONE: Added `GET /api/users/me/ai-quota`.
- DONE: Returned current tier, window start/end, limit, used, and remaining per category.
- DONE: Kept quota status out of `/api/users/me` so the existing user response stays small.

Frontend Still To Do:

- Disable expensive actions when remaining quota is zero.
- Show upgrade messaging for BASIC users when premium would increase the limit.

#### Phase 7: Admin Observability

Status: implemented for first admin visibility.

Goal:

- Let the admin see whether usage limits protect costs.

Implemented:

- DONE: Added quota usage to the existing admin user detail DTO.
- DONE: Added direct admin quota endpoint `GET /api/admin/users/{id}/ai-quota`.
- DONE: Kept manual quota overrides out of scope for the first implementation.

Still To Do:

- Add filters by category and date range only if current admin AI usage views are not enough.
- Add a simple cost-risk indicator later if admins need dashboard warnings.

#### Phase 8: Tests and Edge Cases

Status: implemented for focused backend quota coverage.

Goal:

- Prove quota behavior before public release.

Implemented:

- DONE: Unit tested quota-window calculation for daily and monthly limits.
- DONE: Unit tested BASIC vs PREMIUM tier resolution.
- DONE: Unit tested quota exceeded behavior.
- DONE: Added endpoint-level `429` test for quota-exceeded AI endpoint behavior.
- DONE: Verified quota enforcement has no admin role exemption path.
- DONE: Verified quota counting does not break when chat provider token usage is missing.
- DONE: Unit tested fail-closed behavior when `AiUsage` persistence fails.
- DONE: Unit tested fail-closed behavior when no authenticated user is available for usage recording.
- DONE: Used an injectable server-side `Clock` for consistent quota-window behavior.
- DONE: Documented that concurrent requests may exceed quota slightly as expected MVP behavior.

Still To Do:

- Add full Spring Security integration coverage after test configuration no longer depends on real provider secrets.

Implementation result:

- DONE: Implemented `CHAT`, `TTS`, `STT`, and `IMAGE` backend quota checks.
- DONE: Exposed frontend quota status after backend enforcement was in place.
- DONE: Kept the first version simple: count requests, not tokens or exact USD.
- DONE: Kept atomic quota reservations out of scope for the MVP.

## Recommended Implementation Order

### Phase 1: Finish Admin Observability

- Done: record successful chat, image, TTS, and STT calls in `ai_usage`.
- Done: keep token fields for chat models and use `usageUnit`/`usageAmount` for non-chat usage.
- Done: `GET /api/admin/users/{id}/ai-usage` supports optional `from`, `to`, `provider`, and `model` filters.
- Done: optional AI usage filters are implemented with Spring Data JPA Specifications.
- Done: recording endpoints accept optional `durationSeconds` and use it for Groq STT usage when provided.
- Done: estimate `estimatedCostUsd` per usage row from configurable rates in `application.properties`.
- Done: expose `totalEstimatedCostUsd` in the admin AI usage summary.
- Keep provider prices in configuration because model pricing changes over time.

### Phase 2: Add Prompt Management

- Done: add `PromptTemplate` entity and `PromptTemplateRepo`.
- Done: expose admin prompt CRUD under `/api/admin/prompts`.
- Done: validate editable prompt content to block schema, JSON, provider, and security instructions.
- Done: merge admin teaching guidance with locked prompt files at runtime.
- Done: keep technical prompt rules in files so admin edits cannot change output contracts.

### Phase 3: Add Audit Trail

- Done: create `AdminAuditLog` entity and repository.
- Done: add `AdminAuditService.recordUserStatusChange(...)`.
- Done: record actor ID, target ID, action, old value, new value, timestamp, and optional reason for admin status changes.
- Done: audit prompt overlay and exercise config mutations.
- Done: expose paged audit log lookup through `GET /api/admin/audit-logs`.

### Phase 4: Model Real Exercise Completion

- Done: add `ExerciseAttempt` entity for generated/completed/evaluated lifecycle state.
- Done: generated exercise responses include a durable `attemptId`.
- Done: add `POST /api/progress/exercises/{attemptId}/complete`.
- Done: speaking and image-description evaluations can update an existing attempt through optional `attemptId`.
- Done: progress dashboard distinguishes generated, completed, and evaluated counts.

### Phase 5: Make Exercise Config Editable

- Done: add database-backed level/topic/type config entities.
- Done: seed current MVP config rows at startup.
- Done: expose admin CRUD and enable/disable through `/api/admin/exercise-config`.
- Done: audit exercise config mutations.
- Done: migrate `ExerciseRequestDto` from enum fields to string codes.
- Done: remove legacy exercise config enum models.
- Done: validate exercise generation requests against enabled database config.

## Design Notes

- Keep controllers thin: route admin endpoints through services, and delegate specialized behavior to focused services such as `AiUsageService`, `PromptTemplateService`, `AdminAuditService`, and `UserProgressService`.
- Keep prompt-editing logic separate from AI feature services. Those services consume validated prompt content; they do not own prompt administration.
- Keep `ExerciseAttempt` as the progress lifecycle source of truth and treat legacy `UserProgress` as deprecated unless migrated later.
- For the MVP, read-only exercise config remains acceptable. Full CRUD is a schema and API contract change.

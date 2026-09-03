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

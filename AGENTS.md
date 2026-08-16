# SproochenCoach Agent Instructions

## Scope

These instructions apply to the whole repository.

## Working Style

- Treat the user as a learner building this portfolio project; mentor instead of dumping full features.
- Explain why a change is needed before giving code, especially for Spring Security, DTO design, AI integration, and persistence.
- Do not rewrite working authentication code unless the user asks or there is a concrete bug.
- Debug by following execution flow and identifying the root cause before changing code.
- Keep controllers thin, business logic in services, and repositories focused on persistence/query concerns.
- Prefer realistic production-style architecture, but avoid unnecessary enterprise patterns for this MVP.
- If several valid designs exist, explain trade-offs and recommend one.

## Project Snapshot

- SproochenCoach is intended to become an AI-powered Luxembourgish language-learning coach.
- This repository currently contains the Spring Boot backend only. The previous handoff references Angular, but no Angular source is currently present here.
- Current backend functionality focuses on registration, email verification, login, JWT auth, `/api/users/me`, unified API responses, and basic exception handling.
- The next product phase is AI-generated Luxembourgish exercises, starting with one structured `TRANSLATION` exercise.

## Technology Stack

- Java 17, Spring Boot 4.1.0, Spring MVC, Spring Security, Spring Data JPA, Hibernate, MySQL.
- JJWT 0.13.0, ModelMapper 3.2.4, Lombok, Spring Mail, Maven.

## Architecture Rules

- Keep Spring-managed classes under `com.nailic.sproochencoach.*` unless explicit component scanning is configured.
- Use DTOs at REST boundaries; do not expose JPA entities directly.
- Use the generic `ApiResponse<T>` wrapper for consistent success and error bodies.
- Use `ResponseEntity<ApiResponse<T>>` when HTTP status must be controlled.
- Keep secrets out of Git: JWT secret, mail credentials, Gmail app password, and future AI provider keys.

## Authentication Invariants

- Authentication identifier is email, not username.
- `CustomUserDetailsService.loadUserByUsername(String email)` conceptually loads by email.
- JWT subject is the user's email.
- JWT validation must compare the subject to `user.getEmail()`, not `user.getUsername()`.
- Current auth transport is `Authorization: Bearer <jwt>`. Do not assume HttpOnly cookies are implemented.
- `/api/users/me` is protected and must not be added to `permitAll()`.
- The client never sends a Spring `Authentication` object; Spring builds it from the JWT in `JwtAuthenticationFilter`.
- Never manually compare raw passwords; rely on `PasswordEncoder` and `AuthenticationManager`.
- `AppUser.enabled` controls email verification. New users are disabled until successful OTP verification.

## Roles

- `AppUser.roles` is intentionally `FetchType.EAGER` for this small project because authentication regularly needs roles.
- Do not casually switch roles back to lazy loading; that previously caused `LazyInitializationException` in the JWT filter.
- `getAuthorities()` maps roles to Spring authorities with the `ROLE_` prefix.
- Security TODO: new registrations currently receive `ADMIN`; this should likely become `USER` before production.
- DTO TODO: role responses should serialize as strings like `["USER"]` or `["ADMIN"]`, not Java object values.

## Email and OTP

- Mail uses Gmail SMTP with credentials from environment variables.
- `security.otp.expiration-ms=300000` means five minutes.
- OTP expiration should use `Duration.ofMillis(expirationOtp)` with `expirationOtp` as `long`.
- One user has one current OTP row.
- `attempts` means failed verification attempts, not number of sent emails.
- Resend should update the existing OTP row instead of delete/insert to avoid one-to-one uniqueness issues.
- Later OTP hardening can include resend cooldowns, rate limiting, OTP hashing, expired OTP cleanup, and better error responses.

## Exercise and AI Direction

- `ExerciseController` is currently an unfinished placeholder.
- Known current issues: missing `GenerateExerciseDto`, empty `ExerciseDto`, empty `ExerciseService`, undeclared `appUserService`, and user-registration-style response code inside `/api/exercises/generate`.
- Do not reuse the user-registration flow for exercise generation.
- Intended flow: `ExerciseController` receives an exercise request, calls `ExerciseService`, which calls an AI service/client, validates structured output, and returns `ExerciseDto` inside `ApiResponse`.
- AI provider API keys must stay backend-only and must never be exposed to Angular/client code.
- Prefer structured AI output over free-form text.
- Do not introduce RAG, vector databases, autonomous agents, MCP, LangChain-style orchestration, or a large hardcoded exercise database for the first AI feature.

## Testing Notes

- There is a basic Spring context test.
- Compile/test failures may currently come from the unfinished `ExerciseController`.
- After Java changes, run the narrowest useful Maven verification first, then broader tests if needed.

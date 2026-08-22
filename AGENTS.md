# SproochenCoach Agent Instructions

## Scope

These instructions apply to the whole repository.

## Working Style

- Treat the user as a learner building this portfolio project; mentor instead of dumping full features.
- Explain why a change is needed before giving code, especially for Spring Security, DTO design, AI integration, and persistence.
- Debug by following execution flow and identifying the root cause before changing code.
- Keep controllers thin, business logic in services, and repositories focused on persistence/query concerns.
- Prefer realistic production-style architecture, but avoid unnecessary enterprise patterns for this MVP.
- If several valid designs exist, explain trade-offs and recommend one.
- Do not rewrite working authentication, OTP, JWT, `/me`, or AI integration code unless the user asks or there is a concrete bug.

## Project Snapshot

- SproochenCoach is an AI-powered Luxembourgish language-learning coach.
- This repository currently contains the Spring Boot backend only. The previous handoff references Angular, but no Angular source is currently present here.
- Current backend functionality includes registration, email verification, login, JWT auth, `/api/users/me`, unified API responses, exception handling, AI-generated exercises, speaking/listening prompt generation, TTS audio, recording upload, STT transcription, and AI speaking evaluation.
- The first AI product direction is structured Luxembourgish exercises and speaking practice, not RAG, vector search, autonomous agents, LangChain-style orchestration, or a large hardcoded exercise database.

## Technology Stack

- Java 17, Spring Boot 4.1.0, Spring MVC, Spring Security, Spring Data JPA, Hibernate, MySQL.
- JJWT 0.13.0, ModelMapper 3.2.4, Lombok, Spring Mail, Spring `RestClient`, Maven.
- Jackson usage includes Spring Boot 4 / Jackson 3 style `tools.jackson.databind.ObjectMapper`.

## Architecture Rules

- Keep Spring-managed classes under `com.nailic.sproochencoach.*` unless explicit component scanning is configured.
- Use DTOs at REST boundaries; do not expose JPA entities directly.
- Use the generic `ApiResponse<T>` wrapper for consistent success and error bodies.
- Use `ResponseEntity<ApiResponse<T>>` when HTTP status must be controlled.
- Keep secrets out of Git: JWT secret, mail credentials, Gmail app password, OpenRouter key, Groq key, ElevenLabs key, and future AI provider keys.
- Use SLF4J logging instead of `System.out.println()` or `printStackTrace()`. Raw successful AI/provider payloads should usually be `debug`; provider HTTP errors, invalid provider response structures, JSON parse failures, and provider call failures should be `error` with the exact high-level reason.
- Logging must not expose passwords, raw JWTs, OTP codes, API keys, provider secrets, or raw audio bytes. Prefer user IDs, masked emails, counts, response sizes, and high-level failure reasons.
- Keep frontend/backend contracts discoverable through OpenAPI. `/v3/api-docs` is the source of truth for generated frontend TypeScript types.

## Authentication Invariants

- Authentication identifier is email, not username.
- `CustomUserDetailsService.loadUserByUsername(String email)` conceptually loads by email even though the method name comes from Spring Security.
- JWT subject is the user's email.
- JWT validation must compare the subject to `user.getEmail()`, not `user.getUsername()`.
- Current auth transport is `Authorization: Bearer <jwt>`. Do not assume HttpOnly cookies are implemented.
- The client never sends a Spring `Authentication` object; Spring builds it from the JWT in `JwtAuthenticationFilter`.
- Never manually compare raw passwords; rely on `PasswordEncoder` and `AuthenticationManager`.
- `AppUser.enabled` controls email verification. New users are disabled until successful OTP verification.

## Security Flow

- Login route is `POST /api/users/login`; it authenticates with `AuthenticationManager` using email and password.
- `JwtAuthenticationFilter` reads `Authorization: Bearer <jwt>`, extracts the email subject, loads the user by email, validates the token, and stores a `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.
- `/api/users/me` is protected and must not be added to `permitAll()`. It receives Spring's injected `Authentication`, casts the principal to `AppUser`, and returns `ResponseUserDto`.
- `SecurityConfig` currently permits unauthenticated `POST` requests to `/api/users/login`, `/api/users/addUser`, `/api/users/sendOtp`, `/api/users/resendOtp`, and `/api/users/verifyOtp`; other routes require authentication.

## Roles

- `AppUser.roles` is intentionally `FetchType.EAGER` for this small project because authentication regularly needs roles.
- Do not casually switch roles back to lazy loading; that previously caused `LazyInitializationException` in the JWT filter.
- `getAuthorities()` maps roles to Spring authorities with the `ROLE_` prefix.
- Security TODO: new registrations currently receive `ADMIN`; this should become `USER` before production.
- DTO TODO: role responses should serialize as strings like `["USER"]` or `["ADMIN"]`, not Java object values.

## User and OTP Flows

- Registration route is `POST /api/users/addUser`; it checks duplicate username/email, hashes passwords with `PasswordEncoder`, saves the user, and returns `ResponseUserDto`.
- Mail uses Gmail SMTP with credentials from environment variables.
- `security.otp.expiration-ms=300000` means five minutes and should be handled with `Duration.ofMillis(expirationOtp)` where `expirationOtp` is `long`.
- One user has one current OTP row via a unique `Otp.user` one-to-one association.
- OTP generation uses a six-digit integer from `ThreadLocalRandom.current().nextInt(100000, 1_000_000)`.
- `attempts` means failed verification attempts, not number of sent emails. Verification stops after five failed attempts.
- Successful verification sets `user.enabled=true`, saves the user, and deletes the OTP.
- Resend updates the existing OTP row: replace code, reset attempts to `0`, reset creation date, then save. Do not delete/insert because the one-to-one uniqueness constraint can fail before delete flushes.
- Later OTP hardening can include resend cooldowns, rate limiting, OTP hashing, expired OTP cleanup, and better error responses.

## API Response and Exceptions

- `ApiResponse<T>` is a record with `success`, `message`, and `data`.
- `GlobalExceptionHandler` is under `com.nailic.sproochencoach.exceptions`, so it is inside component scan.
- `UserAlreadyExistsException` maps to `409 Conflict`.
- `OpenRouterError` extends `RuntimeException` and carries an integer `statusCode`; malformed upstream AI content should generally become `502 Bad Gateway`.
- Add dedicated provider exceptions later for Groq and ElevenLabs instead of overusing `OpenRouterError`.

## OpenAPI Contract

- Springdoc exposes OpenAPI JSON at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`.
- `SecurityConfig` permits the OpenAPI and Swagger UI endpoints without JWT so the frontend can generate types during development.
- `OpenApiConfig` defines API metadata and the reusable JWT bearer security scheme.
- Frontend work should generate or inspect TypeScript types from `/v3/api-docs` instead of manually copying DTOs.
- See `docs/api-contract.md` for the frontend type-generation workflow.

## AI Providers

- OpenRouter handles exercise generation, speaking prompt generation, and speaking evaluation.
- Groq Whisper handles speech-to-text transcription.
- ElevenLabs handles text-to-speech audio.
- Each provider has its own `RestClient` bean. Because there are multiple `RestClient` beans, use explicit constructor injection with `@Qualifier`.
- OpenRouter bean name is `openRouterRestClient` and uses `Authorization: Bearer <OPENROUTER_API_KEY>`.
- Groq bean name is `groqRestClient` and uses `Authorization: Bearer <GROQ_API_KEY>`.
- ElevenLabs bean name is `ttsRestClient` and uses `xi-api-key: <ELEVENLABS_API_KEY>`, not Bearer auth.
- Known typo: the ElevenLabs config class is currently named `TtsResctClient`; do not rename it unless the user asks or the rename is part of a focused cleanup.

## OpenRouter DTOs and Parsing

- OpenRouter request bodies use `AiBody`, `MessageBody`, and `AIRoleEnum`.
- `AIRoleEnum` serializes roles as `system`, `user`, and `assistant`.
- OpenRouter responses follow the OpenAI-compatible shape `choices[0].message.content`.
- The `content` string is expected to be strict JSON generated by the model, then parsed with `objectMapper.readValue(...)` into application DTOs.
- Always validate null/empty OpenRouter responses before accessing `choices[0]`.
- Prompt rules should strongly require strict JSON only: no markdown, no code fences, no comments, no trailing commas, quoted property names, and no text before or after JSON.

## Exercise Generation

- Exercise endpoint is `POST /api/exercises/generate`.
- Request DTO is `ExerciseRequestDto` with enum fields `LevelEnum level`, `TopicEnum topic`, and `ExerciseTypeEnum type`.
- Current exercise response DTO is `GeneratedExerciseDto` with `question`, `type`, `options`, `expectedAnswer`, and `hint`.
- Existing enums include levels `A1`, `A2`, `B1`; exercise types include `TRANSLATION`, `MULTIPLE_CHOICE`, `FILL_IN_THE_BLANK`, and `SHORT_ANSWER`.
- `TopicEnum` also carries a level, so `level` and `topic` currently have overlapping information. Do not redesign this unless asked.
- `ExerciseService` still owns text-only exercise generation.
- `AudioExerciseGenerationService` owns the shared OpenRouter + ElevenLabs flow for audio-backed exercises such as speaking and listening.

## Speaking Practice

- Speaking endpoints currently live under `/api/exercises`, not `/api/speaking`.
- `POST /api/exercises/practice` accepts `ExerciseRequestDto` and returns `SpeakingDto` inside `ApiResponse`.
- `AudioExerciseDto` extends `GeneratedExerciseDto` and adds `questionTranslation` plus `byte[] audio`.
- `SpeakingDto` extends `AudioExerciseDto` for speaking-specific typing, but currently adds no extra fields.
- `questionTranslation` is the canonical field name. Do not reintroduce older plural naming.
- `SpeakingService` loads the speaking prompt and delegates OpenRouter + ElevenLabs audio generation to `AudioExerciseGenerationService`.
- `TtsRequest` uses field `model_id`; ElevenLabs expects JSON property `model_id`, not `modelId`.
- ElevenLabs currently uses model `eleven_multilingual_v2`.
- `byte[] audio` is serialized by Jackson as Base64 in the JSON response. This is acceptable for short MVP clips; use separate audio URL/storage later for longer audio.

## Listening Practice

- Listening endpoints currently live under `/api/exercises`.
- `POST /api/exercises/listening` accepts `ExerciseRequestDto` and returns `AudioExerciseDto` inside `ApiResponse`.
- `ListeningService` should not extend `SpeakingService`; listening and speaking share mechanics but are separate product features.
- `ListeningService` loads `ai.prompts.listening-generation` and delegates OpenRouter + ElevenLabs audio generation to `AudioExerciseGenerationService`.
- Listening responses include `questionTranslation` because the frontend needs the English translation of the Luxembourgish listening text.

## Recording and Evaluation

- Recording endpoint is `POST /api/exercises/recording` and consumes `multipart/form-data`.
- The audio form field name is `audio`; the frontend has used `recording.webm` with `audio/webm;codecs=opus`.
- `SpeakingService.transcribeAudio(MultipartFile audio)` sends multipart data to Groq with `file`, `model=whisper-large-v3`, `response_format=text`, and currently `language=lb` plus a Luxembourgish transcription prompt.
- Whisper/Groq can mis-transcribe Luxembourgish into German or English; this is a major product limitation and may require a stronger Luxembourgish-capable STT provider.
- Speaking evaluation sends the transcript to OpenRouter and expects `SpeakingEvaluation` with `transcript`, integer `score`, English `feedback`, and `List<String> corrections`.
- `corrections` must remain `List<String>` because the model returns a JSON array.

## Known Technical Debt

- Replace `openrouter/free` with a specific stronger Luxembourgish-capable model when quality matters.
- Improve Luxembourgish STT accuracy; prompt tweaks alone may not solve Whisper limitations.
- Change default registration role from `ADMIN` to `USER` before production.
- Clean role DTO serialization.
- Extract remaining duplicated OpenRouter HTTP/error/parsing logic from `ExerciseService`, `AudioExerciseGenerationService`, and speaking evaluation into an `AiClient` or `OpenRouterClient` after features stabilize.
- Add Bean Validation annotations where missing, especially request DTOs used by auth and exercises.
- Add provider-specific errors for Groq and ElevenLabs.
- Add timeouts/retry strategy for external API calls.
- Add OTP resend cooldown/rate limiting and eventually OTP hashing.
- Eventually persist exercise/speaking history and user progress.

## Testing Notes

- There is a basic Spring context test.
- After Java changes, run the narrowest useful Maven verification first, usually `mvn -q -DskipTests compile`, then broader tests if needed.
- Do not fix unrelated test/build failures unless they block the requested task or the user asks.

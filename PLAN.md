# SproochenCoach Development Plan

## Purpose

This file tracks project progress and next steps. `AGENTS.md` keeps durable instructions for future agents; this file keeps implementation status, roadmap, and technical debt.

## Done So Far

### Backend Foundation

- Created a Java 17 Spring Boot backend under `com.nailic.sproochencoach`.
- Configured Spring MVC, Spring Security, Spring Data JPA, Hibernate, MySQL, Maven, Lombok, ModelMapper, JJWT, and Spring Mail.
- Added `ApiResponse<T>` as the shared response wrapper.
- Added `GlobalExceptionHandler` for duplicate-user errors.

### User and Authentication

- Implemented user registration through `/api/users/addUser`.
- Added duplicate username and email checks.
- Added password hashing with `PasswordEncoder`.
- Switched authentication identity to email.
- Implemented login through `/api/users/login`.
- Implemented JWT generation with the user's email as the token subject.
- Implemented JWT validation against `user.getEmail()`.
- Added `JwtAuthenticationFilter` to authenticate Bearer-token requests.
- Added protected `/api/users/me` for restoring current-user state from a valid JWT.
- Kept `/api/users/me` protected, not public.

### Email Verification and OTP

- Added `AppUser.enabled` email-verification behavior.
- Added OTP entity and repository.
- Added OTP email sending through Gmail SMTP.
- Added OTP verification with expiration and failed-attempt counting.
- Added OTP resend by updating the existing OTP row.

### Known Current State

- Authentication/security backend is mostly complete for the MVP.
- This repository currently does not contain Angular frontend source.
- `ExerciseController` is currently unfinished and likely prevents clean compilation.

## Current Problems to Fix

### Exercise Placeholder

- `GenerateExerciseDto` does not exist.
- `ExerciseDto` is empty.
- `ExerciseService` is empty.
- `ExerciseController` references `appUserService` without declaring it.
- `/api/exercises/generate` currently returns user-registration-style logic instead of exercise-generation logic.

### Technical Debt

- New users currently receive `ADMIN`; change this to `USER` before production.
- `ResponseUserDto` mixes user data and JWT; later split into `UserResponseDto` and `LoginResponseDto`.
- Role DTO mapping may return object strings instead of role names.
- Add Bean Validation to request DTOs gradually.
- Expand global exception handling as real application exceptions appear.
- Add OTP resend cooldown/rate limiting later.

## Next Phase Goal

Implement the first real product feature:

```http
POST /api/exercises/generate
```

The endpoint should generate a structured Luxembourgish exercise using a backend-only AI integration.

Initial request shape:

```json
{
  "level": "A2",
  "topic": "daily routine",
  "type": "TRANSLATION"
}
```

Initial response shape:

```json
{
  "type": "TRANSLATION",
  "level": "A2",
  "topic": "daily routine",
  "question": "Translate into Luxembourgish: I wake up early every day.",
  "expectedAnswer": "Ech erwächen all Dag fréi.",
  "explanation": "..."
}
```

## Recommended Next Steps

### Step 1: Repair the Exercise Contract

- Decide the exact request DTO name, preferably `ExerciseGenerationRequest`.
- Define `ExerciseDto` fields for the first `TRANSLATION` exercise.
- Add simple enums only if they improve validation: `LanguageLevel` and `ExerciseType`.
- Refactor `ExerciseController` so it depends on `ExerciseService`, not `AppUserService`.

### Step 2: Add a Temporary Non-AI Service Implementation

- Make `ExerciseService.generateExercise(...)` return one hardcoded structured exercise.
- Use this only to verify the REST contract before adding AI.
- Confirm the endpoint returns `ApiResponse<ExerciseDto>`.

### Step 3: Choose and Configure AI Provider

- Choose the AI provider/model.
- Store the API key in an environment variable, never in Git.
- Add backend configuration properties for the provider.
- Decide timeout and basic error-handling behavior.

### Step 4: Create AI Service Layer

- Add a dedicated AI client/service behind `ExerciseService`.
- Build prompts from `level`, `topic`, and `type`.
- Require structured JSON output from the model.
- Parse, validate, and map the AI response into `ExerciseDto`.

### Step 5: Test the Backend Flow

- Test `/api/exercises/generate` with a valid Bearer JWT.
- Test invalid request values.
- Test AI failures and malformed AI output.
- Run Maven compile/tests after the placeholder is repaired.

### Step 6: Connect the Client

- Add the Angular/client form after the backend contract is stable.
- Send `Authorization: Bearer <jwt>`.
- Render question, expected answer, and explanation.

## Later Features

- Add answer evaluation with structured feedback.
- Save generated exercises and user answers.
- Track learning history and progress.
- Add personalized exercise generation.
- Consider stronger auth storage such as HttpOnly cookies only after the Bearer JWT MVP is stable.

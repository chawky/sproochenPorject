# Backend API Contract

The backend exposes an OpenAPI contract for frontend type generation.

## Runtime Endpoints

- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `GET /swagger-ui.html`

When running locally, use:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/swagger-ui.html
```

## Frontend Type Generation

In the Angular/frontend repo, generate TypeScript types from the backend contract instead of manually copying DTOs.

Recommended minimal setup:

```bash
npm install -D openapi-typescript
```

Add a script in the frontend `package.json`:

```json
{
  "scripts": {
    "api:types": "openapi-typescript http://localhost:8080/v3/api-docs -o src/app/api/backend-schema.ts"
  }
}
```

Then run:

```bash
npm run api:types
```

## Frontend Codex Instruction

Tell Codex in the frontend repo:

```text
Before changing API calls or DTO types, regenerate or inspect src/app/api/backend-schema.ts.
Do not invent backend endpoint paths or response shapes.
The backend source of truth is /v3/api-docs.
```

## Current Important Contracts

- Response wrapper is `ApiResponse<T>`.
- Speaking endpoint: `POST /api/exercises/practice`.
- Listening endpoint: `POST /api/exercises/listening`.
- Audio responses include Base64 audio in `data.audio`.
- Audio responses include English translation in `data.questionTranslation`.

## Redo an evaluated speaking or image-description exercise

`POST /api/exercises/{attemptId}/retry` has no request body. Use the existing
authentication transport (cookie-authenticated requests also require CSRF).
It returns HTTP 200 with:

```json
{
  "success": true,
  "message": "Exercise retry created successfully.",
  "data": { "attemptId": 124, "retryOfAttemptId": 123 }
}
```

The original attempt must belong to the current user, have status `EVALUATED`,
and have category `SPEAKING` or `IMAGE_DESCRIPTION`. Missing attempts, other
owners, unsupported categories and other statuses return HTTP 400 using the
existing `ApiResponse` error wrapper. The backend copies metadata into a fresh
`GENERATED` attempt with empty results; the original history remains unchanged.

Keep the same loaded question/audio/image and replace the active attempt ID only
after success. Submit the next recording to the existing evaluation endpoint.
Image description evaluation still requires the original `imageDescription`
multipart field. This endpoint neither generates nor returns exercise content.

Retries do not consume generation quota or the AI technical request allowance;
subsequent evaluation still uses the existing evaluation rate limit and guards.
Each successful retry request creates a new attempt (the endpoint is not
idempotent); avoid double submission or automatic retries after ambiguous
network failures. An evaluated retry can itself be retried.

Attempt history DTOs now include nullable `retryOfAttemptId`, referencing the
immediate source attempt. Original generations have null linkage. Quota counts
exclude linked retry rows, including retries created in later quota windows.

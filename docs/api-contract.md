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

# Backend Go-Live Plan

Repository: `chawky/sproochenPorject`  
Branch reviewed: `master`  
Purpose: remaining backend work required before a public production launch.

## Priority 0 — Critical fixes before launch

### 1. Protect authentication endpoints from abuse

Status: partially implemented.

Already done:

- OTP send/resend has email and IP rate limiting.
- OTP verification has a maximum attempt count.
- Unknown email responses are generic for send/resend.

Still required:

- Add rate limiting to `POST /api/users/login`.
- Decide whether OTP rate limiting must survive backend restarts. The current limiter is in-memory, so counters reset on redeploy/restart.
- If the app will run multiple backend replicas, move OTP/login rate limiting to a shared store such as Redis or a database-backed counter.
- Verify the real client IP is being obtained safely behind Railway + Nginx. Do not rely on an arbitrary client-supplied `X-Forwarded-For` value.

Acceptance:

- Repeated failed logins are throttled.
- OTP abuse cannot bypass limits by simply restarting/redeploying the backend.
- Normal users are not accidentally sharing one global IP limit because all requests appear to come from the frontend container.

### 2. Rotate any credentials that were previously exposed

Status: required before public launch.

Already done:

- `.env` is ignored by Git.

Still required:

- Rotate any API keys/secrets that were previously committed, pasted, or otherwise exposed.
- Rotate at minimum the JWT secret and any provider keys that may have been exposed.
- Confirm production secrets exist only in Railway/environment variables.
- Confirm no real secret is committed anywhere in current Git history that is still active.

Acceptance:

- All production secrets are fresh.
- No active production credential is present in Git.

## Priority 1 — Required production functionality

### 3. Finish the profile-update flow

Status: backend side complete.

Already done:

- `PUT /api/users/me` exists.
- The authenticated principal determines the user ID.
- `PUT /api/users/{id}` remains `ADMIN`-only.
- A backend test verifies that `/me` updates the authenticated user's ID.

Backend action:

- No additional backend change is required unless frontend testing exposes a DTO/validation problem.
- After the frontend is changed, test `PUT /api/users/me` with a normal `USER` account on Railway.

### 4. Production email / Resend

Status: code complete, production domain pending.

Already done:

- Resend HTTP integration exists.
- `RESEND_API_KEY` and `APP_EMAIL_FROM` are environment-driven.
- OTP email send/resend flows exist.

Still required:

- Buy the final domain, currently planned as `letztalklux.com`.
- Verify that domain in Resend.
- Set `APP_EMAIL_FROM=noreply@letztalklux.com`, or the chosen sender.
- Send a real registration OTP from the Railway deployment.
- Test resend, wrong OTP, expired OTP, max-attempt behavior, and successful verification.

### 5. Production Stripe

Status: implementation exists; keep `TEST` mode until final launch.

Already done:

- Checkout endpoint exists.
- Stripe webhook endpoint exists.
- Subscription cancellation exists.
- Stripe configuration comes from environment variables.
- Processed Stripe events are persisted for webhook idempotency.

Before switching live:

- Run a complete Stripe `TEST`-mode flow on Railway.
- Verify successful checkout changes the user to `PREMIUM` via webhook.
- Verify duplicate webhook delivery is safe.
- Verify cancellation behavior.
- Verify refresh/re-login shows the correct subscription state.
- Create/confirm `LIVE` Product and Price.
- Configure the `LIVE` webhook endpoint.
- Replace `STRIPE_API_KEY`, `STRIPE_PRICE_ID`, `STRIPE_PRODUCT_ID`, and `STRIPE_WEBHOOK_SECRET` together.
- Perform one controlled real payment test after all other go-live checks pass.
- Do not mix test-mode and live-mode Stripe IDs.

### 6. Custom domain and CORS

Status: pending.

Already done:

- Allowed origins are configurable through `SECURITY_CORS_ALLOWED_ORIGINS`.
- Wildcard CORS is no longer used.

Still required:

- Attach the final custom domain to the frontend Railway service.
- Set backend `SECURITY_CORS_ALLOWED_ORIGINS=https://letztalklux.com`.
- During migration, optionally allow both the Railway frontend URL and the final domain.
- Remove the old Railway origin later if it should no longer be accepted.
- Verify preflight and authenticated requests from the final domain.

## Priority 2 — Production verification

### 7. Full Railway end-to-end test

Status: not complete.

Test at least:

- Registration.
- OTP delivery.
- OTP verification.
- Login.
- Logout.
- Session restoration after refresh.
- Profile update through `/api/users/me`.
- All exercise types.
- AI generation providers used by the app.
- `BASIC` quota behavior.
- `PREMIUM` quota/restriction behavior.
- Progress recording.
- Admin authorization.
- Disabled-user behavior.
- Stripe `TEST` checkout and webhook.
- Cancellation.
- Multiple users, not only the developer account.

Recommended personas:

- New `BASIC` user.
- Existing `BASIC` user.
- `PREMIUM` user.
- `ADMIN` user.
- Disabled user.

### 8. Persistence and restart tests

Status: infrastructure exists; final validation required.

Already done:

- Railway MySQL service exists.
- Persistent volume is attached.
- Spring Boot connects to Railway MySQL.

Still required:

- Create test data.
- Redeploy/restart the backend.
- Confirm data remains.
- Restart/redeploy MySQL only if safe and verify persistence.
- Redeploy backend without redeploying frontend and verify frontend API calls recover automatically.

### 9. Error-safety checks

Status: centralized exception handling exists; externally verify.

Still required:

- Trigger invalid input and verify no Java stack trace is returned to clients.
- Trigger invalid/expired/malformed JWT and verify clean `401` responses.
- Trigger forbidden `USER` to `ADMIN` access and verify `403`.
- Trigger provider failures and verify no provider secrets/tokens appear in responses or logs.
- Verify production Swagger/OpenAPI is disabled as intended.

### 10. Health and observability

Status: mostly complete.

Already done:

- Public `GET /actuator/health` is allowed.
- Other actuator paths are not broadly public.

Still required:

- Confirm `/actuator/health` returns `200` after fresh deployment.
- Confirm Railway health/restart behavior is acceptable.
- Review production logs for accidental PII/secrets.
- Add simple alerting/monitoring later if real users depend on the service.

## Priority 3 — Important hardening, but can be staged

### 11. AI quota concurrency race

Status: known issue.

Current pattern can allow concurrent requests to all pass:

1. Count usage.
2. Allow.
3. Provider call.
4. Record usage.

Before meaningful public scale or if provider cost abuse is a concern:

- Replace with an atomic reservation/counter.
- Prefer DB atomic update/insert with a unique user/category/window counter, or Redis `INCR` + `TTL`.
- Decide whether failed provider calls release the reservation.

This is not necessarily an MVP launch blocker for a small controlled beta, but it becomes important as traffic grows.

### 12. Move auth away from browser localStorage

Status: cross-cutting frontend + backend improvement.

For a real public paid product, prefer:

- `HttpOnly`, `Secure`, `SameSite` authentication cookie.
- Cookie-based `/me` session restoration.
- Appropriate CSRF strategy if cookies are used for authentication.
- Stop returning/storing long-lived bearer tokens in JavaScript-accessible storage.

This is a larger change and should be done deliberately rather than rushed immediately before launch.

## Backend launch gate

Do not switch to public live payments until all of these are true:

- Login/OTP abuse controls are acceptable.
- Active exposed secrets have been rotated.
- Frontend uses `/api/users/me` and profile update works for `USER`.
- Resend sends from the verified production domain.
- Final CORS origin is configured.
- Railway E2E flows pass with several users.
- Database persistence/restart test passes.
- Error responses expose no stack traces/secrets.
- Stripe `TEST` flow passes end to end.

Only then switch all Stripe configuration to `LIVE`.

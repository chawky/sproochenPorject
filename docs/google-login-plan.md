# Google Login Implementation Plan

## Goal

Add "Continue with Google" without replacing the existing authentication architecture.

The backend should:

- Accept a Google Identity Services ID token from the Angular SPA.
- Verify the Google token server-side.
- Link or create an `AppUser`.
- Issue the existing HttpOnly `access_token` JWT cookie.
- Preserve email/password login, OTP verification, `/me`, admin authorization, Stripe ownership, AI quotas, and user progress.

## Current Repo Facts

- Normal login is `POST /api/users/login` in `AppUserController`.
- Normal login returns `ApiResponse<ResponseUserDto>` and sets the JWT through `JwtCookieService`.
- `JwtCookieService` creates the `access_token` cookie as HttpOnly.
- `JwtAuthenticationFilter` already reads JWTs from the `access_token` cookie.
- `SecurityConfig` is stateless and protects all routes except explicitly permitted public routes.
- `AppUser` currently stores `email` but does not store a Google OpenID Connect subject.
- User creation already assigns the default `USER` role in `AppUserService`.
- Flyway is enabled and `spring.jpa.hibernate.ddl-auto=validate`, so schema changes need migrations.

## Target Flow

```text
Continue with Google
        |
Google authenticates user in Angular
        |
Angular receives Google ID token
        |
POST /api/users/google-login
        |
Backend verifies signature, issuer, expiration, and audience
        |
Backend reads Google's stable sub plus verified email
        |
Backend finds existing Google-linked AppUser, creates a new AppUser,
or returns account-linking-required for an existing password account
        |
Backend generates existing app JWT
        |
Backend sets existing HttpOnly access_token cookie
        |
Angular calls /api/users/me
```

## Security Decisions

### Use Google `sub` as the identity key

Do not permanently identify Google accounts by email. Email can change, and email is not the stable Google account identifier.

Implementation rule:

- `google_subject` is the durable external identity.
- `email` is used for new-account creation only when Google says `email_verified=true`.
- After linking, future Google logins must find the user by `google_subject`.

### Do not auto-link existing accounts by email

If Google returns a verified email that already exists as an app account without `google_subject`, do not attach the Google account during public Google login.

Safer rule:

- Public Google login may create a new account for a new verified email.
- Public Google login may authenticate an account already linked by `google_subject`.
- Public Google login must not link an existing password account solely because the email matches.
- Existing users must first authenticate normally, then link Google from a protected endpoint such as `POST /api/users/me/google-link`.

This avoids making account-linking decisions based only on provider email equivalence.

### Do not return JWT in JSON

The Google login endpoint must match current cookie auth:

- Set `Set-Cookie: access_token=...`.
- Return `ApiResponse<ResponseUserDto>` in the body.
- Do not include JWT in the response body.

### Verify token server-side

The backend must not trust frontend claims directly. It must verify:

- Google signature.
- Issuer is Google.
- Token is not expired.
- Audience equals configured Google web client ID.

Use Google's Java verifier rather than hand-rolling JWT/JWK validation.

### Keep Google as identity only

Google should not own app authorization. The backend still owns:

- Roles.
- Admin access.
- Stripe subscription.
- AI quotas.
- User progress.
- Account disabled state.

## Backend Implementation Plan

### 1. Add Configuration

Update `src/main/resources/application.properties`:

```properties
security.google.client-id=${GOOGLE_CLIENT_ID:}
```

Add a matching constant in `AppConstants.PropertyPlaceholders`:

```java
public static final String SECURITY_GOOGLE_CLIENT_ID = "${security.google.client-id:}";
```

Production/deployment requirement:

- Set `GOOGLE_CLIENT_ID` to the Google OAuth web client ID used by Angular.
- Do not commit the client secret. This flow does not need a Google client secret.

### 2. Add Google verifier dependency

Add the official Google ID token verification library to `pom.xml`.

Preferred implementation:

- Use `GoogleIdTokenVerifier`.
- Configure it with the backend's expected web client ID.
- Let the library handle Google public key retrieval and signature validation.

Keep the dependency minimal. Do not add Spring OAuth2 Client for this architecture unless the whole flow is changed to Authorization Code login.

### 3. Add database column

Create a new Flyway migration, for example:

```text
src/main/resources/db/migration/V2__add_google_subject_to_app_users.sql
```

Migration:

```sql
ALTER TABLE app_users
    ADD COLUMN google_subject VARCHAR(255) NULL;

CREATE UNIQUE INDEX uk_app_users_google_subject
    ON app_users (google_subject);
```

Do not make `google_subject` non-null because existing email/password users do not have Google accounts linked.

### 4. Update `AppUser`

Add:

```java
@Column(name = "google_subject", unique = true)
private String googleSubject;
```

Do not expose `googleSubject` in `ResponseUserDto`. It is an internal login identifier.

### 5. Update `AppUserRepo`

Add:

```java
Optional<AppUser> findByGoogleSubject(String googleSubject);
boolean existsByGoogleSubject(String googleSubject);
```

Keep existing email methods unchanged because password login, OTP, password reset, admin lookup, and `/me` still depend on email.

### 6. Add request DTO

Add `GoogleLoginRequestDto`:

```java
public record GoogleLoginRequestDto(
        @NotBlank String idToken
) {}
```

This keeps the REST boundary explicit and avoids reusing `RequestUserDto`, which is designed for email/password registration and login.

### 7. Add Google token verification service

Add a focused service, for example `GoogleIdentityService`.

Responsibilities:

- Accept raw `idToken`.
- Verify using configured Google client ID.
- Return a small internal value object with:
  - `subject`
  - `email`
  - `emailVerified`
  - `firstName`
  - `lastName`

Failure behavior:

- Blank token: validation fails before service.
- Missing `GOOGLE_CLIENT_ID`: fail safely with an internal configuration error.
- Invalid token: throw an authentication exception that maps to `401`.
- Unverified email: reject with `403` or `400`; do not create/link users from unverified Google emails.

Logging:

- Log invalid Google token failures at `warn`.
- Do not log raw Google ID tokens.
- Log masked emails and high-level reasons only.

### 8. Add Google auth service method

Add a method such as:

```java
AuthenticatedUser loginWithGoogle(String idToken)
```

Recommended location:

- Either add it to `AppUserService` if keeping the MVP small.
- Or add `GoogleLoginService` if `AppUserService` starts becoming too broad.

Use this decision rule:

- If implementation stays under a few cohesive methods, use `AppUserService`.
- If token verification, linking, and account creation make the class messy, extract `GoogleLoginService`.

### 9. Public Google login matching rules

Use this exact order:

1. Verify Google ID token.
2. Extract `sub`, `email`, `email_verified`, given name, family name.
3. Try `findByGoogleSubject(sub)`.
4. If found:
   - Reject if `adminDisabled=true`.
   - Issue app JWT.
5. If not found and `email_verified=false`:
   - Reject.
   - Do not create an account.
6. If not found and `email_verified=true`, try `findByEmail(email)`.
7. If matching email user exists:
   - Do not link automatically.
   - Return `409 Conflict` with a safe message such as "Account already exists. Log in first to link Google."
   - Do not issue an app JWT cookie.
8. If no matching email user exists:
   - Create new `AppUser`.
   - Set `googleSubject=sub`.
   - Set `email=email`.
   - Set `enabled=true`.
   - Set default role `USER`.
   - Fill `firstName` and `lastName` if present.
   - Generate a collision-safe username.
   - Set `password=null`.
   - Issue app JWT.

Important:

- Do not create duplicate users for the same Google `sub`.
- Do not overwrite an existing user's roles, subscription, quota, progress, or address fields during public Google login.
- Do not change current email/password registration behavior.
- Make password authentication explicitly reject users with `password=null` using the existing generic bad-credentials behavior.

### 10. Protected Google account linking

Add a protected endpoint only if the UI needs users to connect Google to an existing password account:

```text
POST /api/users/me/google-link
```

Rules:

1. Require the current app user to be authenticated with the existing app session.
2. Verify the submitted Google ID token server-side.
3. Require `email_verified=true`.
4. Require the Google email to equal the current user's email.
5. Reject if the Google `sub` is already linked to another user.
6. Reject if the current user already has a different `googleSubject`.
7. Set `googleSubject=sub`.
8. Preserve password, roles, subscription, progress, quotas, and profile fields.
9. Return `ApiResponse<ResponseUserDto>`.

Do not expose this as a public account-linking shortcut.

### 11. Username generation for Google-created users

Use the simplest collision-safe rule:

1. Start from the email local-part before `@`.
2. Normalize to a safe username string.
3. If taken, append a short numeric or ID suffix until unique.

This is only for display/internal uniqueness. Authentication remains email/password or Google `sub`.

### 12. Issue JWT cookie through existing service

Reuse existing code:

- Generate app JWT with `JwtService.generateToken(user)`.
- Return `AuthenticatedUser`.
- Controller sets cookie with `JwtCookieService.accessTokenCookie(...)`.

This keeps Google login behavior aligned with normal login and prevents token transport regressions.

### 13. Add controller endpoint

Add to `AppUserController`:

```text
POST /api/users/google-login
```

Request:

```json
{
  "idToken": "google_id_token"
}
```

Success response:

```json
{
  "success": true,
  "message": "Google login successful",
  "data": {
    "id": 1,
    "username": "user",
    "email": "user@example.com",
    "emailVerified": true,
    "roles": ["USER"]
  }
}
```

Headers:

```text
Set-Cookie: access_token=...; HttpOnly; Path=/; SameSite=Lax
```

Do not return the JWT in JSON.

### 14. Rate-limit Google login

Protect `POST /api/users/google-login` from day one.

Minimal backend rule:

- Resolve client IP with existing `ClientIpResolver`.
- Before token verification, check the existing login IP rate limit.
- On invalid token, unverified email, existing-account conflict, disabled account, or other rejected Google auth attempt, record a failed auth attempt for the client IP.
- If a verified email is available, also record against that email.
- On success, clear the email failure history like normal login.

Prefer reusing or slightly extending `LoginRateLimitService` over adding a new rate-limit subsystem.

### 15. Update security config

In `SecurityConfig`:

- Permit `POST /api/users/google-login`.
- Ignore CSRF for `POST /api/users/google-login`, consistent with existing login endpoints.
- Keep `/api/users/me` protected.
- Do not remove existing cookie/JWT filter behavior.

### 16. Add exception handling

Add a small exception type if needed, for example:

```java
InvalidGoogleTokenException extends AuthenticationException
```

Expected mappings:

- Invalid Google token: `401`.
- Google email not verified: `403` or `400`.
- Existing account disabled by admin: `403`.
- Existing password account needs explicit linking: `409`.
- Duplicate/link conflict from protected linking: `409`.
- Missing backend Google client ID: `500` with generic message, logged as configuration error.

Do not leak provider internals or raw token values in error responses.

## Angular Contract Plan

Angular should:

- Load Google Identity Services script.
- Use the same Google web client ID configured as `GOOGLE_CLIENT_ID` in the backend.
- Request an ID token.
- Send it to `POST /api/users/google-login`.
- Use `withCredentials: true` so the backend cookie is stored.
- After success, call `GET /api/users/me` to hydrate current user state.

Angular should not:

- Store the app JWT in localStorage/sessionStorage.
- Send Google `sub` directly as proof of identity.
- Treat frontend-decoded Google claims as trusted.

## Regression Checklist

### Authentication

- Existing `POST /api/users/login` still sets the HttpOnly `access_token` cookie.
- Existing `POST /api/users/logout` still clears the cookie.
- Existing `/api/users/me` still requires authentication.
- Existing JWT validation still compares subject to user email.
- Password login still rejects unverified email/password users.
- Password login rejects Google-only users with `password=null` using the same generic bad-credentials behavior.
- Public Google login does not auto-link existing password accounts by email.

### Authorization

- Admin endpoints still require `ROLE_ADMIN`.
- New Google users receive `USER`, not `ADMIN`.
- Existing admin users keep admin role when they explicitly link Google from an authenticated session.
- Admin-disabled users cannot log in with Google.

### Persistence

- Existing users can have `google_subject=NULL`.
- New Google users get unique `google_subject`.
- Same Google account cannot create multiple users.
- Protected email/password account linking does not delete OTPs, password reset tokens, subscriptions, progress, or roles.

### API Contract

- Google login returns `ApiResponse<ResponseUserDto>`.
- Google login does not expose `googleSubject`.
- Google login does not expose app JWT in JSON.
- OpenAPI includes the new endpoint and request DTO.

### Security

- Invalid Google tokens fail closed.
- Tokens for another Google client ID fail audience validation.
- Expired tokens fail.
- Unverified Google emails are rejected.
- Raw ID tokens are never logged.
- `GOOGLE_CLIENT_ID` is read from environment/config, not hardcoded.

## Test Plan

### Unit tests

Add tests around the service that handles Google login:

- Existing user found by `googleSubject` returns an app JWT.
- Existing user found only by verified email returns account-linking-required and does not set a cookie.
- Protected linking sets `googleSubject` only for the currently authenticated matching-email user.
- Protected linking rejects a Google `sub` already attached to another user.
- New verified Google user is created with `enabled=true` and role `USER`.
- New Google user is created with `password=null`.
- Password login rejects a Google-only account with generic bad credentials.
- Unverified Google email is rejected.
- Admin-disabled user is rejected.
- Rejected Google login attempts are rate-limited by IP.
- Duplicate username generation resolves collision.
- Invalid Google token is rejected.

Mock the Google verifier service in these tests. Do not call Google over the network in unit tests.

### Controller tests

Extend `AppUserControllerTest`:

- `POST /api/users/google-login` returns success body and `Set-Cookie`.
- Response body does not contain JWT.
- Invalid token returns an error response.
- Request validation rejects blank `idToken`.

### Security tests

Extend `SecurityConfigTest`:

- `POST /api/users/google-login` is permitted without authentication.
- `/api/users/me` remains protected.
- Existing public auth endpoints remain public.

### Persistence/migration checks

Run:

```powershell
mvn -q -DskipTests compile
mvn test
```

If test scope is too slow during implementation, run narrow tests first:

```powershell
mvn -q -Dtest=AppUserServiceTest,AppUserControllerTest,SecurityConfigTest test
```

Then run the full test suite before considering the feature complete.

## Implementation Order

1. Add config property and app constant.
2. Add Google verifier dependency.
3. Add Flyway migration.
4. Add `googleSubject` to `AppUser`.
5. Add repository lookup methods.
6. Add `GoogleLoginRequestDto`.
7. Add Google token verification service.
8. Add Google login/create service logic.
9. Add protected Google linking service logic if the UI needs account linking now.
10. Add controller endpoint using `JwtCookieService`.
11. Add IP rate limiting to the public Google login endpoint.
12. Permit and CSRF-ignore the public endpoint in `SecurityConfig`.
13. Add exception mapping if current handlers are not precise enough.
14. Add focused unit/controller/security tests.
15. Run compile and tests.
16. Update frontend to call the endpoint with credentials.
17. Test manually through Angular with real Google client ID.

## Manual Verification

Use a local Angular app on the configured CORS origin:

1. Start backend with `GOOGLE_CLIENT_ID` set.
2. Start Angular on `http://localhost:4200`.
3. Click "Continue with Google".
4. Confirm backend response is successful.
5. Confirm browser stores `access_token` as HttpOnly cookie.
6. Confirm `GET /api/users/me` succeeds without manually attaching an Authorization header.
7. Confirm refresh still keeps the user logged in while cookie is valid.
8. Confirm logout clears the cookie.

## Rollback Plan

If the feature causes issues:

- Remove the Angular Google button first.
- Leave backend endpoint unused.
- If backend must be rolled back, revert code changes but keep the nullable `google_subject` column unless there is a strong reason to remove it.
- A nullable unused column is safer than a destructive rollback.

## Do Not Change During This Feature

- Do not rewrite JWT generation.
- Do not rewrite `JwtAuthenticationFilter`.
- Do not move auth from cookie back to JSON tokens.
- Do not make `/api/users/me` public.
- Do not replace email/password login.
- Do not change OTP verification.
- Do not auto-enable an existing password account from public Google login.
- Do not add Spring OAuth2 Login unless intentionally switching architectures.

## Open Decisions Before Coding

- Should Google-created users be allowed to later set a password through the existing password reset flow?
  - Recommendation: yes, because password reset already proves email ownership.
- Should Google login rate-limit invalid attempts by IP?
  - Recommendation: yes; reuse or slightly extend the existing login IP rate limiting from the first patch.
- Should accounts with existing unverified email/password registration be auto-enabled from public Google login?
  - Recommendation: no; return account-linking-required and require normal authentication before linking.

## Official References

- Google Identity Services backend verification: https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
- Google backend authentication guidance: https://developers.google.com/identity/sign-in/web/backend-auth
- Google Java `GoogleIdTokenVerifier`: https://docs.cloud.google.com/java/docs/reference/google-api-client/latest/com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier

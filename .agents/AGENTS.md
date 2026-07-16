# SereneMind – Agent Rules

## API Security Review (Mandatory)

After creating or modifying **any API endpoint** in this project, perform an
explicit security review pass before declaring the task complete.

### Delivery Policy
- **Always aim to deliver complete, production-ready, secure code.**
- If a security fix is not feasible in the current context (e.g., requires an
  external service not yet integrated), **still deliver the working code** but
  clearly flag the issue with a `⚠️ SECURITY FLAG` comment block so the user
  knows what needs to be addressed later.

---

### Security Checklist

#### ✅ 1. Authentication & Authorization
- All endpoints that touch user data must require a valid JWT Bearer token.
- Ownership must always be verified — the logged-in user may only access their
  own resources (e.g., `journal.getUser().getEmail().equals(principal.getEmail())`).
- Admin-only endpoints must be protected with role checks (`ROLE_ADMIN`).
- Never trust the client to tell you which user owns a resource — always derive
  ownership from the authenticated principal.

#### ✅ 2. Input Validation
- All `@RequestBody` fields must be validated with Bean Validation
  (`@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Max`, etc.).
- Query parameters and path variables must be type-safe (Spring handles Long/int
  coercion; add `@Min(1)` for IDs where applicable).
- Never pass raw user input directly into JPQL/SQL — always use `@Param` bindings.
- Validate string lengths to prevent oversized payloads from hitting the DB.

#### ✅ 3. Sensitive Data Exposure
- Passwords, tokens, keys, and secrets must **never** appear in any response DTO.
- Encrypted fields (e.g., `encryptedText`) must always be decrypted server-side;
  raw cipher text must never be sent to the client.
- Response DTOs should expose only the minimum fields required (data minimisation).
- Avoid including internal IDs that could leak DB structure (use opaque UUIDs
  where sensitive, though Long IDs are acceptable for journal entries).

#### ✅ 4. IDOR (Insecure Direct Object Reference)
- Every fetch-by-ID operation must validate that the resource belongs to the
  requesting user before returning or mutating it.
- Return `403 Forbidden` (not `404`) for ownership violations — 404 would confirm
  resource existence to an attacker.

#### ✅ 5. Error Handling & Information Leakage
- Global exception handlers must return only generic, user-safe messages.
- Stack traces, class names, and SQL details must never appear in API responses.
- Use consistent error response shapes across all endpoints.

#### ✅ 6. Encryption at Rest
- Sensitive journal content must be encrypted with `EncryptionUtil` before
  persisting to the DB.
- Encryption keys must come from environment variables / secrets manager,
  never hard-coded in source files.

#### ✅ 7. Rate Limiting & Abuse Prevention
- Computationally expensive endpoints (AI analysis trigger, file uploads) must
  be flagged for rate-limiting even if the implementation is deferred.
- Add a `⚠️ SECURITY FLAG: Add rate limiting` comment near such endpoints.

#### ✅ 8. Mass Assignment Protection
- Request DTOs must only contain fields the user is permitted to set.
- Never bind a request body directly to an entity — always go through a DTO and
  map fields explicitly.

---

### How to Report Findings

After the checklist pass, append a brief security summary at the end of the
relevant section in the walkthrough or inline in code comments, e.g.:

```
// Security review: Ownership validated ✅ | Input validated ✅
//                  No sensitive data in response ✅ | IDOR protected ✅
```

If something cannot be fixed immediately:
```
// ⚠️ SECURITY FLAG: This endpoint has no rate limit.
//    Add @RateLimiter or a Bucket4j filter before production deployment.
//    Risk: Medium — a malicious user could spam AI analysis calls.
```

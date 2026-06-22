# Muolana — REST API Specification

**Base URL:** `https://muolana.onrender.com/api/v1` (production) · `http://localhost:8080/api/v1` (local)  
**Auth:** `Authorization: Bearer <JWT>` — RS256, issued by `/auth/login` or `/auth/refresh`  
**Content-Type:** `application/json`  
**Conventions:**
- Plural nouns only — no verbs in paths
- URI versioning: `/api/v1/`
- `POST` → `201 Created` + `Location` header
- `PUT` → full replacement → `200 OK`
- `PATCH` → partial update → `200 OK`
- `DELETE` → `204 No Content`
- `GET` list → `200 OK` + pagination wrapper
- `GET` single → `200 OK` or `404 Not Found`
- Explicit request/response DTOs — entity classes never exposed
- `401` unauthenticated · `403` forbidden · `409` conflict · `422` validation

---

## 1. Auth — `/api/v1/auth`

> Open endpoints (no JWT required).

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `POST` | `/api/v1/auth/register` | `{ email, password, fullName }` | `201` + `{ id, email, role }` | Register citizen account |
| `POST` | `/api/v1/auth/login` | `{ email, password }` | `200` + `{ accessToken, refreshToken, expiresIn }` | Login, receive RS256 JWT |
| `POST` | `/api/v1/auth/refresh` | `{ refreshToken }` | `200` + `{ accessToken, refreshToken, expiresIn }` | Rotate refresh token |
| `POST` | `/api/v1/auth/logout` | `{ refreshToken }` | `204` | Revoke refresh token, add jti to revocations |

**Error responses (all auth endpoints):**

```json
// 400 Bad Request — missing field
{ "status": 400, "error": "Bad Request", "message": "email is required" }

// 401 Unauthorized — wrong credentials
{ "status": 401, "error": "Unauthorized", "message": "Invalid credentials" }

// 409 Conflict — email already registered
{ "status": 409, "error": "Conflict", "message": "Email already in use" }
```

---

## 2. Sessions — `/api/v1/sessions`

> Conversation module. Open (no JWT required — sessions are anonymous by default).

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `POST` | `/api/v1/sessions` | `{}` (empty — title auto-generated) | `201` + `{ id, title, status, createdAt }` + `Location: /sessions/{id}` | Start a new chat session |
| `GET` | `/api/v1/sessions` | — | `200` + `{ data: [...], page, size, totalElements }` | List caller's sessions (paginated) |
| `GET` | `/api/v1/sessions/{sessionId}` | — | `200` + session object | Get session metadata |
| `PATCH` | `/api/v1/sessions/{sessionId}` | `{ title?, status? }` | `200` + updated session | Rename session or mark ENDED |
| `DELETE` | `/api/v1/sessions/{sessionId}` | — | `204` | Delete session and all its messages |

---

## 3. Messages — `/api/v1/sessions/{sessionId}/messages`

> Conversation + RAG modules. Submitting a message triggers the full RAG pipeline.  
> Open (no JWT required — same as sessions).

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `POST` | `/api/v1/sessions/{sessionId}/messages` | `{ content }` | `201` + `{ id, role: "USER", content, createdAt }` | Submit user query; triggers async RAG |
| `GET` | `/api/v1/sessions/{sessionId}/messages` | — | `200` + `{ data: [...], page, size, totalElements }` | Get full message history (paginated, asc) |
| `GET` | `/api/v1/sessions/{sessionId}/messages/{messageId}` | — | `200` + message object | Get single message with RAG metadata |

**SSE stream (chat response):**

```
GET /api/v1/sessions/{sessionId}/messages/{messageId}/stream
Accept: text/event-stream

data: {"type":"citation","ref":"Land Act 2009, s.18(2)","url":"https://..."}
data: {"type":"citation","ref":"Constitution 2011, Art. 28"}
data: {"type":"chunk","content":"Under South Sudan law..."}
data: {"type":"chunk","content":" landlords must provide..."}
data: {"type":"done","guardTriggered":false}
```

> `url` on `citation` events is optional (only present when the source document has a `sourceUrl`).  
> If the hallucination guard fires (cosine scores below threshold), stream emits `{"type":"not_found","message":"..."}` instead of citations and chunks.  
> On error: `{"type":"error","message":"..."}`.  
> SSE timeout: 300 s. nginx must be configured with `proxy_buffering off` and `proxy_read_timeout 310s`.

**Synchronous structured answer (alternative to SSE):**

```
GET /api/v1/sessions/{sessionId}/messages/{messageId}/answer
```

Returns a fully typed JSON object instead of a stream. Useful for non-browser clients.

```json
{
  "answer": "Under South Sudan law...",
  "citations": [
    { "law": "Land Act 2009", "article": "s.18(2)", "summary": "Landlord obligations..." }
  ],
  "category": "Land",
  "disclaimer": "Legal information only, not legal advice."
}
```

**Message response shape:**

```json
{
  "id": "uuid",
  "sessionId": "uuid",
  "role": "ASSISTANT",
  "content": "Under South Sudan law...",
  "lawChunksUsed": ["uuid-chunk-1", "uuid-chunk-2"],
  "cosineScores": [0.91, 0.87],
  "guardTriggered": false,
  "model": "gemini-2.0-flash",
  "inputTokens": null,
  "outputTokens": null,
  "createdAt": "2026-06-17T09:41:00Z"
}
```

---

## 4. Lawyers — `/api/v1/lawyers`

> Lawyer module. `GET` endpoints are fully public (no JWT).

### 4.1 Directory (public read)

| Method | Path | Query params | Success | Description |
|--------|------|-------------|---------|-------------|
| `GET` | `/api/v1/lawyers` | `city`, `area`, `feeType`, `language`, `page`, `size` | `200` + paginated list | Browse active, verified lawyers |
| `GET` | `/api/v1/lawyers/{lawyerId}` | — | `200` + lawyer profile | Get full lawyer profile |
| `GET` | `/api/v1/lawyers/{lawyerId}/reviews` | `page`, `size` | `200` + paginated reviews | Get public reviews for a lawyer |

### 4.2 Specialisations (sub-resource)

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `GET` | `/api/v1/lawyers/{lawyerId}/specialisations` | — | `200` + `[{ id, area }]` | List practice areas |

### 4.3 Referrals (citizen action)

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `POST` | `/api/v1/lawyers/{lawyerId}/referrals` | `{ sessionId, channel, consentGiven: true }` | `201` + `{ id, status, createdAt }` | Send context package to lawyer (consent required) |
| `GET` | `/api/v1/sessions/{sessionId}/referrals` | — | `200` + `[{ id, lawyerId, status, createdAt }]` | List referrals made from a session |

### 4.4 Reviews (citizen action)

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `POST` | `/api/v1/lawyers/{lawyerId}/reviews` | `{ sessionId?, rating, reviewText?, isAnonymous }` | `201` + `{ id, rating, createdAt }` | Submit a review (1–5 stars) |

### 4.5 Admin — lawyer management (`ADMIN` role only)

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `POST` | `/api/v1/admin/lawyers` | `{ fullName, barNumber, locationCity, feeType, bio?, ... }` | `201` + lawyer object + `Location` | Create lawyer profile (status: PENDING) |
| `GET` | `/api/v1/admin/lawyers` | `status`, `page`, `size` | `200` + paginated list | List all lawyers (any status) |
| `GET` | `/api/v1/admin/lawyers/{lawyerId}` | — | `200` + lawyer object | Get any lawyer by id |
| `PUT` | `/api/v1/admin/lawyers/{lawyerId}` | full lawyer object | `200` + updated | Full replace of lawyer profile |
| `PATCH` | `/api/v1/admin/lawyers/{lawyerId}` | `{ status?, locationCity?, feeType?, bio?, ... }` | `200` + updated | Partial update |
| `PATCH` | `/api/v1/admin/lawyers/{lawyerId}/approve` | `{}` | `200` + `{ id, status: "ACTIVE" }` | Approve pending lawyer |
| `PATCH` | `/api/v1/admin/lawyers/{lawyerId}/suspend` | `{}` | `200` + `{ id, status: "SUSPENDED" }` | Suspend active lawyer |
| `DELETE` | `/api/v1/admin/lawyers/{lawyerId}` | — | `204` | Permanently remove lawyer |
| `POST` | `/api/v1/admin/lawyers/{lawyerId}/specialisations` | `{ area }` | `201` + `{ id, area }` | Add practice area |
| `DELETE` | `/api/v1/admin/lawyers/{lawyerId}/specialisations/{id}` | — | `204` | Remove practice area |
| `POST` | `/api/v1/admin/lawyers/{lawyerId}/languages` | `{ language }` | `201` + `{ id, language }` | Add language |
| `DELETE` | `/api/v1/admin/lawyers/{lawyerId}/languages/{id}` | — | `204` | Remove language |
| `POST` | `/api/v1/admin/lawyers/{lawyerId}/contact-methods` | `{ channel, value, isPrimary }` | `201` + contact object | Add contact method (channel: PHONE, EMAIL, WHATSAPP) |
| `DELETE` | `/api/v1/admin/lawyers/{lawyerId}/contact-methods/{id}` | — | `204` | Remove contact method |

---

## 5. Legal documents — `/api/v1/admin/legal-documents`

> Admin module. `ADMIN` role only.  
> Files are stored in MinIO (S3-compatible). Accepted formats: `pdf`, `docx`.

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `POST` | `/api/v1/admin/legal-documents` | `multipart/form-data`: `metadata` (JSON) + `file` | `201` + `{ id, status: "PENDING" }` + `Location` | Upload legal document |
| `GET` | `/api/v1/admin/legal-documents` | — | `200` + paginated list | List documents (filter: `status`, `category`) |
| `GET` | `/api/v1/admin/legal-documents/{docId}` | — | `200` + document object | Get document metadata |
| `GET` | `/api/v1/admin/legal-documents/{docId}/file` | — | `200` + binary stream | Download raw file (PDF or DOCX) |
| `PATCH` | `/api/v1/admin/legal-documents/{docId}/verify` | `{}` | `200` + `{ id, status: "VERIFIED" }` | Verify doc → triggers ingestion pipeline |
| `PATCH` | `/api/v1/admin/legal-documents/{docId}/reject` | `{ rejectionReason }` | `200` + `{ id, status: "REJECTED" }` | Reject document with reason |
| `DELETE` | `/api/v1/admin/legal-documents/{docId}` | — | `204` | Delete document and all its chunks |
| `GET` | `/api/v1/admin/legal-documents/{docId}/chunks` | `page`, `size` | `200` + paginated list of chunks | Inspect ingested vector chunks |

**Upload metadata fields:**

```json
{
  "title": "Land Act 2009",
  "shortName": "Land Act",
  "category": "LAND",
  "versionLabel": "2009",
  "sourceUrl": "https://..."
}
```

Categories: `CONSTITUTIONAL`, `CRIMINAL`, `CIVIL`, `LAND`, `FAMILY`, `COMMERCIAL`, `EMPLOYMENT`, `ADMINISTRATIVE`, `OTHER`.

---

## 6. Analytics — `/api/v1/admin/analytics`

> Admin module. `ADMIN` role only. All endpoints are `GET` — read-only.

| Method | Path | Query params | Success | Description |
|--------|------|-------------|---------|-------------|
| `GET` | `/api/v1/admin/analytics/queries` | `from`, `to`, `outcome`, `page`, `size` | `200` + paginated query log | Raw query log with RAG outcomes |
| `GET` | `/api/v1/admin/analytics/queries/summary` | `from`, `to` | `200` + `{ total, answered, notFound, errorRate }` | Aggregate query outcome counts |
| `GET` | `/api/v1/admin/analytics/queries/top-categories` | `from`, `to`, `limit` | `200` + `[{ category, count }]` | Top legal categories by query volume |
| `GET` | `/api/v1/admin/analytics/queries/knowledge-gaps` | `from`, `to`, `limit` | `200` + `[{ queryText, count }]` | Most frequent not-found queries |
| `GET` | `/api/v1/admin/analytics/sessions` | `from`, `to` | `200` + `{ total, active, ended, avgMessagesPerSession }` | Session summary |
| `GET` | `/api/v1/admin/analytics/lawyers/funnel` | `from`, `to` | `200` + `{ promptsShown, buttonTapped, contacted }` | Lawyer referral conversion funnel |

---

## 7. Users — `/api/v1/admin/users`

> Admin module. `ADMIN` role only.

| Method | Path | Request body | Success | Description |
|--------|------|-------------|---------|-------------|
| `GET` | `/api/v1/admin/users` | — | `200` + paginated list | List all users (filter: `role`, `isActive`) |
| `GET` | `/api/v1/admin/users/{userId}` | — | `200` + user object | Get user by id |
| `PATCH` | `/api/v1/admin/users/{userId}` | `{ role?, isActive? }` | `200` + updated user | Change role or deactivate |

---

## 8. Standard error response shape

All errors follow a consistent envelope:

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Validation failed",
  "timestamp": "2026-06-17T09:41:00Z",
  "path": "/api/v1/sessions",
  "fieldErrors": [
    { "field": "content", "message": "must not be blank" }
  ]
}
```

| Status | When |
|--------|------|
| `400` | Malformed JSON / wrong type / unsupported file format |
| `401` | Missing or expired JWT |
| `403` | Valid JWT but wrong role |
| `404` | Resource not found |
| `409` | Duplicate (email, bar number) |
| `422` | Bean validation failure (`@Valid`) |
| `500` | Unexpected server error |

---

## 9. Pagination response envelope

All `GET` collection endpoints return:

```json
{
  "data": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8,
  "last": false
}
```

Query params: `?page=0&size=20&sort=createdAt,desc`

---

## 10. Security matrix

| Endpoint group | `CITIZEN` (JWT) | `ADMIN` (JWT) | Anonymous |
|----------------|-----------------|----------------|-----------|
| `/auth/**` | — | — | Open |
| `/sessions/**` | Open | Open | Open |
| `GET /lawyers`, `GET /lawyers/**` | Open | Open | Open |
| `POST /lawyers/{id}/referrals` | Yes | — | — |
| `POST /lawyers/{id}/reviews` | Yes | — | — |
| `/admin/**` | — | Full | — |
| `/actuator/health` | — | — | Open |
| Static assets, Thymeleaf pages | — | — | Open |

> Sessions and messages are open to support anonymous citizen usage — no registration required to use the chat.

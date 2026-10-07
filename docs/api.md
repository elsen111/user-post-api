# Post API — Endpoint Reference

Complete reference for the Post API REST interface (version `v1`).

- **Base URL (local):** `http://localhost:8080`
- **API prefix:** `/api/v1`
- **Interactive docs:** `/swagger-ui.html` · **OpenAPI JSON:** `/v3/api-docs`

For setup, configuration and architecture see [`README.md`](./README.md).

---

## Table of Contents

1. [Conventions](#conventions)
    - [Request format](#request-format)
    - [Response envelope](#response-envelope)
    - [Pagination & sorting](#pagination--sorting)
    - [Data types](#data-types)
2. [Authentication](#authentication)
3. [Rate Limiting](#rate-limiting)
4. [Endpoint Index](#endpoint-index)
5. [Authentication Endpoints](#authentication-endpoints)
    - [Register](#register)
    - [Login](#login)
    - [Refresh token](#refresh-token)
    - [Logout](#logout)
6. [User Endpoints](#user-endpoints)
    - [Get current user](#get-current-user)
    - [Update current user](#update-current-user)
    - [Delete current user](#delete-current-user)
    - [List a user's posts](#list-a-users-posts)
7. [Post Endpoints](#post-endpoints)
    - [Create post](#create-post)
    - [List posts (public feed)](#list-posts-public-feed)
    - [Get post](#get-post)
    - [List posts by user](#list-posts-by-user)
    - [Update post](#update-post)
    - [Delete post](#delete-post)
8. [Comment Endpoints](#comment-endpoints)
    - [Create comment](#create-comment)
    - [List comments for a post](#list-comments-for-a-post)
    - [Update comment](#update-comment)
    - [Delete comment](#delete-comment)
9. [Admin Endpoints](#admin-endpoints)
    - [List all users](#list-all-users)
    - [Delete any post](#delete-any-post)
    - [Delete any comment](#delete-any-comment)
10. [Schemas](#schemas)
11. [Error Reference](#error-reference)
12. [End-to-End Example](#end-to-end-example)
13. [Behavior Notes](#behavior-notes)

---

## Conventions

### Request format

| Item | Value |
| --- | --- |
| Content type | `Content-Type: application/json` for all requests with a body |
| Character encoding | UTF-8 |
| Authentication header | `Authorization: Bearer <accessToken>` |
| Identifiers | UUID v4 strings, e.g. `3f2b8c1e-9a4d-4c5e-8b7a-1d2e3f4a5b6c` |
| Text input | Leading/trailing whitespace is trimmed on stored text fields |

### Response envelope

**Every** response — success or error — is wrapped in the same JSON envelope:

| Field | Type | Description |
| --- | --- | --- |
| `success` | boolean | `true` for successful requests, `false` for errors |
| `message` | string | Human-readable summary (do not parse programmatically; rely on the HTTP status) |
| `data` | object \| array \| null | Payload on success; `null` when there is none; field errors on validation failure |
| `timestamp` | string (ISO-8601 instant) | Server time the response was generated, e.g. `2026-10-08T12:00:00.123456Z` |

Success example:

```json
{
  "success": true,
  "message": "Post retrieved successfully",
  "data": { "id": "…", "title": "…" },
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

Success with no payload (`DELETE`, `logout`):

```json
{
  "success": true,
  "message": "Post deleted successfully",
  "data": null,
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

Error example — see [Error Reference](#error-reference).

### Pagination & sorting

All list endpoints are paginated using standard Spring Data query parameters:

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `page` | integer ≥ 0 | `0` | Zero-based page index |
| `size` | integer ≥ 1 | `20` | Items per page (server maximum: 2000) |
| `sort` | string | unsorted | `property,direction` — e.g. `createdAt,desc`. Repeat the parameter for multiple sort keys |

Sortable properties are the entity fields: for posts `createdAt`, `updatedAt`, `title`; for comments `createdAt`, `updatedAt`; for users `username`, `email`, `createdAt`, `updatedAt`, `role`.

Paginated responses place a `PageResponse` in `data`:

```json
{
  "success": true,
  "message": "Posts retrieved successfully",
  "data": {
    "content": [ /* items */ ],
    "page": 0,
    "size": 20,
    "totalElements": 134,
    "totalPages": 7,
    "first": true,
    "last": false
  },
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

> Without an explicit `sort`, ordering isn't guaranteed. Always pass `sort=createdAt,desc` when you need a stable, newest-first feed.

### Data types

| Type | Format | Example |
| --- | --- | --- |
| UUID | RFC 4122 string | `3f2b8c1e-9a4d-4c5e-8b7a-1d2e3f4a5b6c` |
| Entity timestamps (`createdAt`, `updatedAt`) | ISO-8601 local date-time, **no zone offset** | `2026-10-08T12:00:00.123456` |
| Envelope `timestamp` | ISO-8601 instant (UTC, `Z`) | `2026-10-08T12:00:00.123456Z` |
| Role | enum | `USER`, `ADMIN` |

---

## Authentication

The API uses **JWT access tokens** plus **rotating opaque refresh tokens**.

| | Access token | Refresh token |
| --- | --- | --- |
| Purpose | Authorize API calls | Obtain a new token pair |
| Lifetime (default) | 15 minutes (`expiresIn: 900` seconds) | 7 days |
| Sent as | `Authorization: Bearer …` header | JSON body of `/auth/refresh` and `/auth/logout` |
| Revocable | No (expires naturally) | Yes (logout, or consumed on refresh) |

**Lifecycle**

```
register / login ──▶ { accessToken, refreshToken }
        │
        ├─ call API with  Authorization: Bearer <accessToken>
        │
        ├─ 401 (expired) ─▶ POST /auth/refresh { refreshToken }
        │                       └─▶ NEW { accessToken, refreshToken }   (old refresh token is now revoked)
        │
        └─ POST /auth/logout { refreshToken }   (requires a valid access token)
```

**Authorization levels used in this document**

| Label | Meaning |
| --- | --- |
| 🌐 **Public** | No token needed |
| 🔑 **Bearer** | Any authenticated user |
| 👤 **Bearer (owner)** | Authenticated *and* must own the resource; otherwise `403` |
| 🛡️ **Bearer (ADMIN)** | Authenticated user with role `ADMIN`; otherwise `403` |

An invalid, malformed or expired access token is treated as *no authentication*: protected endpoints respond `401 Authentication is required`.

---

## Rate Limiting

Applied per client IP on every `/api/**` request via a token bucket.

| Bucket | Endpoints | Default limit |
| --- | --- | --- |
| Auth | `POST /auth/login`, `/auth/register`, `/auth/refresh` | 10 requests per 60 s |
| General | All other `/api/**` endpoints (including `/auth/logout`) | 100 requests per 60 s |

Exceeding a limit returns **`429 Too Many Requests`**:

```json
{
  "success": false,
  "message": "Too many requests. Please try again later.",
  "data": null,
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

The response does not include a `Retry-After` header; back off and retry after the refill window (default 60 s). Limits are configurable — see the README.

---

## Endpoint Index

| # | Method | Path | Auth | Success | Summary |
| --- | --- | --- | --- | --- | --- |
| 1 | `POST` | `/api/v1/auth/register` | 🌐 Public | `201` | Create an account |
| 2 | `POST` | `/api/v1/auth/login` | 🌐 Public | `200` | Log in |
| 3 | `POST` | `/api/v1/auth/refresh` | 🌐 Public | `200` | Rotate refresh token |
| 4 | `POST` | `/api/v1/auth/logout` | 🔑 Bearer | `200` | Revoke refresh token |
| 5 | `GET` | `/api/v1/users/me` | 🔑 Bearer | `200` | Get own profile |
| 6 | `PATCH` | `/api/v1/users/me` | 🔑 Bearer | `200` | Update own profile |
| 7 | `DELETE` | `/api/v1/users/me` | 🔑 Bearer | `200` | Delete own account |
| 8 | `GET` | `/api/v1/users/{userId}/posts` | 🔑 Bearer | `200` | List a user's posts |
| 9 | `POST` | `/api/v1/posts` | 🔑 Bearer | `201` | Create a post |
| 10 | `GET` | `/api/v1/posts` | 🌐 Public | `200` | Global feed |
| 11 | `GET` | `/api/v1/posts/{postId}` | 🔑 Bearer | `200` | Get a post |
| 12 | `GET` | `/api/v1/posts/user/{userId}` | 🔑 Bearer | `200` | List a user's posts |
| 13 | `PATCH` | `/api/v1/posts/{postId}` | 👤 Owner | `200` | Update a post |
| 14 | `DELETE` | `/api/v1/posts/{postId}` | 👤 Owner | `200` | Delete a post |
| 15 | `POST` | `/api/v1/posts/{postId}/comments` | 🔑 Bearer | `201` | Create a comment |
| 16 | `GET` | `/api/v1/posts/{postId}/comments` | 🔑 Bearer | `200` | List a post's comments |
| 17 | `PATCH` | `/api/v1/comments/{commentId}` | 👤 Owner | `200` | Update a comment |
| 18 | `DELETE` | `/api/v1/comments/{commentId}` | 👤 Owner | `200` | Delete a comment |
| 19 | `GET` | `/api/v1/admin/users` | 🛡️ ADMIN | `200` | List all users |
| 20 | `DELETE` | `/api/v1/admin/posts/{postId}` | 🛡️ ADMIN | `200` | Delete any post |
| 21 | `DELETE` | `/api/v1/admin/comments/{commentId}` | 🛡️ ADMIN | `200` | Delete any comment |

---

## Authentication Endpoints

### Register

`POST /api/v1/auth/register` — 🌐 Public

Creates a user with role `USER` and immediately returns a token pair.

**Request body**

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `username` | string | ✅ | 3–50 characters, not blank. Trimmed. Unique (case-insensitive) |
| `email` | string | ✅ | Valid email, max 255. Trimmed and lower-cased. Unique (case-insensitive) |
| `password` | string | ✅ | 8–100 characters, not blank |

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "S3cretPass!"
}
```

**Response `201 Created`**

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9…",
    "refreshToken": "r4nd0m-opaque-refresh-token",
    "expiresIn": 900,
    "user": {
      "id": "3f2b8c1e-9a4d-4c5e-8b7a-1d2e3f4a5b6c",
      "username": "alice",
      "email": "alice@example.com",
      "role": "USER"
    }
  },
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

`expiresIn` is the access-token lifetime in **seconds**.

**Errors**

| Status | Message | Cause |
| --- | --- | --- |
| `400` | `Validation failed` (+ `data.fields`) | Constraint violation |
| `409` | `Username is already in use` / `Email is already in use` | Duplicate |
| `429` | `Too many requests. Please try again later.` | Auth rate limit |

**cURL**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"S3cretPass!"}'
```

---

### Login

`POST /api/v1/auth/login` — 🌐 Public

**Request body**

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `email` | string | ✅ | Valid email (case-insensitive) |
| `password` | string | ✅ | Not blank |

```json
{ "email": "alice@example.com", "password": "S3cretPass!" }
```

**Response `200 OK`** — same `AuthResponse` shape as [Register](#register), message `Login successful`.

**Errors**

| Status | Message | Cause |
| --- | --- | --- |
| `400` | `Validation failed` | Missing/invalid fields |
| `401` | `Invalid email or password` | Wrong credentials *or* unknown email (deliberately indistinguishable) |
| `403` | `User account is disabled` | Account has `enabled = false` |
| `429` | `Too many requests…` | Auth rate limit |

**cURL**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"S3cretPass!"}'
```

---

### Refresh token

`POST /api/v1/auth/refresh` — 🌐 Public

Exchanges a valid refresh token for a **new access token and a new refresh token**. The submitted refresh token is revoked (single use). Store the new refresh token and discard the old one.

**Request body**

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `refreshToken` | string | ✅ | Not blank |

```json
{ "refreshToken": "r4nd0m-opaque-refresh-token" }
```

**Response `200 OK`** — `AuthResponse` (new `accessToken` **and** new `refreshToken`), message `Token refreshed successfully`.

**Errors**

| Status | Message | Cause |
| --- | --- | --- |
| `400` | `Validation failed` | Blank/missing `refreshToken` |
| `401` | `Invalid refresh token` | Token not recognised |
| `401` | `Refresh token is expired or revoked` | Expired, already used, or logged out |
| `429` | `Too many requests…` | Auth rate limit |

**cURL**

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"r4nd0m-opaque-refresh-token"}'
```

---

### Logout

`POST /api/v1/auth/logout` — 🔑 Bearer

Revokes the supplied refresh token. Only that token is revoked (not all of the user's sessions). Unknown tokens are ignored silently, so the call is idempotent. This endpoint **requires a valid access token** in addition to the refresh token in the body.

**Request body** — same as [Refresh token](#refresh-token).

**Response `200 OK`**

```json
{
  "success": true,
  "message": "Logout successful",
  "data": null,
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

**Errors**

| Status | Message | Cause |
| --- | --- | --- |
| `400` | `Validation failed` | Blank/missing `refreshToken` |
| `401` | `Authentication is required` | Missing/expired access token |

**cURL**

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"r4nd0m-opaque-refresh-token"}'
```

---

## User Endpoints

### Get current user

`GET /api/v1/users/me` — 🔑 Bearer

**Response `200 OK`** — [`UserResponse`](#userresponse), message `User retrieved successfully`.

```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": "3f2b8c1e-9a4d-4c5e-8b7a-1d2e3f4a5b6c",
    "username": "alice",
    "email": "alice@example.com",
    "role": "USER",
    "enabled": true,
    "createdAt": "2026-10-08T11:58:02.114233",
    "updatedAt": "2026-10-08T11:58:02.114233"
  },
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

**Errors:** `401` (missing/invalid token), `404 User not found` (account no longer exists).

```bash
curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Update current user

`PATCH /api/v1/users/me` — 🔑 Bearer

Updates the authenticated user's username and email. **Both fields are required** (the whole profile is submitted). Passwords cannot be changed here.

**Request body**

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `username` | string | ✅ | 3–50 characters, not blank |
| `email` | string | ✅ | Valid email, max 255 |

```json
{ "username": "alice_w", "email": "alice.w@example.com" }
```

**Response `200 OK`** — updated [`UserResponse`](#userresponse), message `User updated successfully`.

**Errors**

| Status | Message | Cause |
| --- | --- | --- |
| `400` | `Validation failed` | Constraint violation |
| `401` | `Authentication is required` | Missing/invalid token |
| `409` | `Username is already in use` / `Email is already in use` | Taken by another user (your own current values never conflict) |

> ⚠️ **Changing your email invalidates your current access token.** The token's subject is the email, so after an email change the old access token is no longer accepted. Log in again (or use `/auth/refresh`) to get a token for the new email.

```bash
curl -X PATCH http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice_w","email":"alice.w@example.com"}'
```

---

### Delete current user

`DELETE /api/v1/users/me` — 🔑 Bearer

Permanently deletes the authenticated account. Cascades to the user's posts, comments (including comments on those posts) and refresh tokens. **Irreversible.**

**Response `200 OK`**

```json
{
  "success": true,
  "message": "User deleted successfully",
  "data": null,
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

**Errors:** `401`, `404 User not found`.

```bash
curl -X DELETE http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### List a user's posts

`GET /api/v1/users/{userId}/posts` — 🔑 Bearer

Returns a page of posts authored by the given user. Equivalent to [`GET /posts/user/{userId}`](#list-posts-by-user).

| Parameter | In | Type | Description |
| --- | --- | --- | --- |
| `userId` | path | UUID | Author's ID |
| `page`, `size`, `sort` | query | — | See [Pagination](#pagination--sorting) |

**Response `200 OK`** — `PageResponse<`[`PostResponse`](#postresponse)`>`, message `User posts retrieved successfully`. An unknown `userId` yields an **empty page**, not `404`.

**Errors:** `400 Invalid request parameter` (malformed UUID), `401`.

```bash
curl "http://localhost:8080/api/v1/users/$USER_ID/posts?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## Post Endpoints

### Create post

`POST /api/v1/posts` — 🔑 Bearer

**Request body**

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `title` | string | ✅ | Not blank, max 150 characters |
| `content` | string | ✅ | Not blank, max 10 000 characters |

```json
{ "title": "Hello, world", "content": "This is my first post." }
```

**Response `201 Created`** — [`PostResponse`](#postresponse), message `Post created successfully`.

```json
{
  "success": true,
  "message": "Post created successfully",
  "data": {
    "id": "7a1c0f52-6d3e-4b8a-9f10-2c4d5e6f7a8b",
    "userId": "3f2b8c1e-9a4d-4c5e-8b7a-1d2e3f4a5b6c",
    "username": "alice",
    "title": "Hello, world",
    "content": "This is my first post.",
    "createdAt": "2026-10-08T12:01:10.532118",
    "updatedAt": "2026-10-08T12:01:10.532118"
  },
  "timestamp": "2026-10-08T12:01:10.540000Z"
}
```

**Errors:** `400 Validation failed`, `401 Authentication is required`.

```bash
curl -X POST http://localhost:8080/api/v1/posts \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Hello, world","content":"This is my first post."}'
```

---

### List posts (public feed)

`GET /api/v1/posts` — 🌐 Public

Returns all posts on the platform. No authentication required.

| Parameter | In | Type | Description |
| --- | --- | --- | --- |
| `page`, `size`, `sort` | query | — | See [Pagination](#pagination--sorting) |

**Response `200 OK`** — `PageResponse<`[`PostResponse`](#postresponse)`>`, message `Posts retrieved successfully`.

```bash
curl "http://localhost:8080/api/v1/posts?page=0&size=10&sort=createdAt,desc"
```

---

### Get post

`GET /api/v1/posts/{postId}` — 🔑 Bearer

| Parameter | In | Type | Description |
| --- | --- | --- | --- |
| `postId` | path | UUID | Post ID |

**Response `200 OK`** — [`PostResponse`](#postresponse), message `Post retrieved successfully`.

**Errors:** `400 Invalid request parameter` (malformed UUID), `401`, `404 Post not found`.

```bash
curl http://localhost:8080/api/v1/posts/$POST_ID -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### List posts by user

`GET /api/v1/posts/user/{userId}` — 🔑 Bearer

Same behavior and response as [`GET /users/{userId}/posts`](#list-a-users-posts).

```bash
curl "http://localhost:8080/api/v1/posts/user/$USER_ID?sort=createdAt,desc" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Update post

`PATCH /api/v1/posts/{postId}` — 👤 Bearer (owner)

Only the post's author may update it. **Both `title` and `content` are required** — the request replaces both values.

**Request body**

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `title` | string | ✅ | Not blank, max 150 |
| `content` | string | ✅ | Not blank, max 10 000 |

```json
{ "title": "Hello, world (edited)", "content": "Updated text." }
```

**Response `200 OK`** — updated [`PostResponse`](#postresponse) (`updatedAt` advances), message `Post updated successfully`.

**Errors**

| Status | Message | Cause |
| --- | --- | --- |
| `400` | `Validation failed` / `Invalid request parameter` | Bad body or UUID |
| `401` | `Authentication is required` | No/invalid token |
| `403` | `You do not have permission to modify this post` | Caller is not the author |
| `404` | `Post not found` | Unknown ID |

```bash
curl -X PATCH http://localhost:8080/api/v1/posts/$POST_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Hello, world (edited)","content":"Updated text."}'
```

---

### Delete post

`DELETE /api/v1/posts/{postId}` — 👤 Bearer (owner)

Permanently deletes the post **and all of its comments**. Only the author may delete (admins use [`/admin/posts/{postId}`](#delete-any-post)).

**Response `200 OK`** — message `Post deleted successfully`, `data: null`.

**Errors:** `401`, `403 You do not have permission to modify this post`, `404 Post not found`.

```bash
curl -X DELETE http://localhost:8080/api/v1/posts/$POST_ID -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## Comment Endpoints

### Create comment

`POST /api/v1/posts/{postId}/comments` — 🔑 Bearer

| Parameter | In | Type | Description |
| --- | --- | --- | --- |
| `postId` | path | UUID | Post being commented on |

**Request body**

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `content` | string | ✅ | Not blank, max 3 000 characters |

```json
{ "content": "Great post!" }
```

**Response `201 Created`** — [`CommentResponse`](#commentresponse), message `Comment created successfully`.

```json
{
  "success": true,
  "message": "Comment created successfully",
  "data": {
    "id": "b2d4f6a8-1c3e-4a5b-8c7d-9e0f1a2b3c4d",
    "postId": "7a1c0f52-6d3e-4b8a-9f10-2c4d5e6f7a8b",
    "userId": "3f2b8c1e-9a4d-4c5e-8b7a-1d2e3f4a5b6c",
    "username": "alice",
    "content": "Great post!",
    "createdAt": "2026-10-08T12:05:44.201907",
    "updatedAt": "2026-10-08T12:05:44.201907"
  },
  "timestamp": "2026-10-08T12:05:44.210000Z"
}
```

**Errors:** `400 Validation failed`, `401`, `404 Post not found`.

```bash
curl -X POST http://localhost:8080/api/v1/posts/$POST_ID/comments \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Great post!"}'
```

---

### List comments for a post

`GET /api/v1/posts/{postId}/comments` — 🔑 Bearer

| Parameter | In | Type | Description |
| --- | --- | --- | --- |
| `postId` | path | UUID | Post ID |
| `page`, `size`, `sort` | query | — | See [Pagination](#pagination--sorting) (e.g. `sort=createdAt,asc` for chronological order) |

**Response `200 OK`** — `PageResponse<`[`CommentResponse`](#commentresponse)`>`, message `Comments retrieved successfully`.

**Errors:** `400 Invalid request parameter`, `401`, `404 Post not found`.

```bash
curl "http://localhost:8080/api/v1/posts/$POST_ID/comments?sort=createdAt,asc" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Update comment

`PATCH /api/v1/comments/{commentId}` — 👤 Bearer (owner)

**Request body**

| Field | Type | Required | Constraints |
| --- | --- | --- | --- |
| `content` | string | ✅ | Not blank, max 3 000 |

```json
{ "content": "Great post — edited!" }
```

**Response `200 OK`** — updated [`CommentResponse`](#commentresponse), message `Comment updated successfully`.

**Errors**

| Status | Message | Cause |
| --- | --- | --- |
| `400` | `Validation failed` / `Invalid request parameter` | Bad body or UUID |
| `401` | `Authentication is required` | No/invalid token |
| `403` | `You do not have permission to modify this comment` | Caller is not the author |
| `404` | `Comment not found` | Unknown ID |

```bash
curl -X PATCH http://localhost:8080/api/v1/comments/$COMMENT_ID \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Great post — edited!"}'
```

---

### Delete comment

`DELETE /api/v1/comments/{commentId}` — 👤 Bearer (owner)

**Response `200 OK`** — message `Comment deleted successfully`, `data: null`.

**Errors:** `401`, `403 You do not have permission to modify this comment`, `404 Comment not found`.

```bash
curl -X DELETE http://localhost:8080/api/v1/comments/$COMMENT_ID -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

## Admin Endpoints

All admin endpoints require a valid access token for a user whose role is `ADMIN`. Regular users receive `403`; unauthenticated callers receive `401`. See the README for how to promote a user to `ADMIN`.

### List all users

`GET /api/v1/admin/users` — 🛡️ ADMIN

| Parameter | In | Type | Description |
| --- | --- | --- | --- |
| `page`, `size`, `sort` | query | — | See [Pagination](#pagination--sorting) |

**Response `200 OK`** — `PageResponse<`[`UserResponse`](#userresponse)`>`.

```json
{
  "success": true,
  "message": "Users fetched sucessfully",
  "data": {
    "content": [
      {
        "id": "3f2b8c1e-9a4d-4c5e-8b7a-1d2e3f4a5b6c",
        "username": "alice",
        "email": "alice@example.com",
        "role": "ADMIN",
        "enabled": true,
        "createdAt": "2026-10-08T11:58:02.114233",
        "updatedAt": "2026-10-08T11:59:30.000000"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-10-08T12:10:00.000000Z"
}
```

> The success `message` is currently spelled `Users fetched sucessfully` (sic) by the server. Don't depend on message text.

**Errors:** `401 Authentication is required`, `403 You do not have permission to access this resource`.

```bash
curl "http://localhost:8080/api/v1/admin/users?size=50&sort=createdAt,desc" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### Delete any post

`DELETE /api/v1/admin/posts/{postId}` — 🛡️ ADMIN

Removes any post (and its comments) regardless of author.

**Response `200 OK`** — message `Post deleted successfully`, `data: null`.

**Errors:** `401`, `403`, `404 Post not found: <postId>`.

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/posts/$POST_ID -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### Delete any comment

`DELETE /api/v1/admin/comments/{commentId}` — 🛡️ ADMIN

**Response `200 OK`** — message `Comment deleted successfully`, `data: null`.

**Errors:** `401`, `403`, `404 Comment not found: <commentId>`.

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/comments/$COMMENT_ID -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## Schemas

### AuthResponse

| Field | Type | Description |
| --- | --- | --- |
| `tokenType` | string | Always `"Bearer"` |
| `accessToken` | string | JWT access token |
| `refreshToken` | string | Opaque refresh token (shown only in this response — store it securely) |
| `expiresIn` | integer | Access-token lifetime in seconds |
| `user` | object | `{ id: UUID, username: string, email: string, role: "USER" \| "ADMIN" }` |

### UserResponse

| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | User ID |
| `username` | string | Display name |
| `email` | string | Email address (lower-case) |
| `role` | `USER` \| `ADMIN` | Authorization role |
| `enabled` | boolean | Whether the account may log in |
| `createdAt` | local date-time | Creation time |
| `updatedAt` | local date-time | Last modification time |

### PostResponse

| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | Post ID |
| `userId` | UUID | Author's user ID |
| `username` | string | Author's username |
| `title` | string | Title (≤ 150 chars) |
| `content` | string | Body (≤ 10 000 chars) |
| `createdAt` | local date-time | Creation time |
| `updatedAt` | local date-time | Last modification time |

### CommentResponse

| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | Comment ID |
| `postId` | UUID | Parent post ID |
| `userId` | UUID | Author's user ID |
| `username` | string | Author's username |
| `content` | string | Body (≤ 3 000 chars) |
| `createdAt` | local date-time | Creation time |
| `updatedAt` | local date-time | Last modification time |

### PageResponse&lt;T&gt;

| Field | Type | Description |
| --- | --- | --- |
| `content` | array&lt;T&gt; | Items on this page |
| `page` | integer | Zero-based page index |
| `size` | integer | Requested page size |
| `totalElements` | integer | Total matching items |
| `totalPages` | integer | Total number of pages |
| `first` | boolean | `true` on the first page |
| `last` | boolean | `true` on the last page |

### Validation error `data`

```json
{ "fields": { "<fieldName>": "<message>" } }
```

---

## Error Reference

### Error envelope

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "fields": {
      "username": "Username must be between 3 and 50 characters",
      "password": "Password must be between 8 and 100 characters"
    }
  },
  "timestamp": "2026-10-08T12:00:00.123456Z"
}
```

`data` is `null` for every error **except** bean-validation failures on request bodies, where it contains the `fields` map (one message per invalid field).

### Status codes

| Status | Meaning | Typical messages |
| --- | --- | --- |
| `400 Bad Request` | Invalid input | `Validation failed`; `Request body is malformed or contains invalid data`; `Invalid request parameter` (e.g. non-UUID path value); `Request validation failed` |
| `401 Unauthorized` | Not authenticated / bad token | `Authentication is required`; `Invalid email or password`; `Invalid refresh token`; `Refresh token is expired or revoked` |
| `403 Forbidden` | Authenticated but not allowed | `You do not have permission to access this resource` (role); `You do not have permission to modify this post` / `…comment` (ownership); `User account is disabled` |
| `404 Not Found` | Resource missing | `Post not found`; `Comment not found`; `User not found`; `Post not found: <id>` / `Comment not found: <id>` (admin) |
| `409 Conflict` | Duplicate / constraint | `Username is already in use`; `Email is already in use`; `The request violates a database constraint` |
| `429 Too Many Requests` | Rate limit hit | `Too many requests. Please try again later.` |
| `500 Internal Server Error` | Unhandled failure | `An unexpected error occurred` |

### Status matrix by endpoint

| Endpoint | 400 | 401 | 403 | 404 | 409 | 429 |
| --- | :-: | :-: | :-: | :-: | :-: | :-: |
| `POST /auth/register` | ✅ | | | | ✅ | ✅ |
| `POST /auth/login` | ✅ | ✅ | ✅ (disabled) | | | ✅ |
| `POST /auth/refresh` | ✅ | ✅ | | | | ✅ |
| `POST /auth/logout` | ✅ | ✅ | | | | ✅ |
| `GET /users/me` | | ✅ | | ✅ | | ✅ |
| `PATCH /users/me` | ✅ | ✅ | | ✅ | ✅ | ✅ |
| `DELETE /users/me` | | ✅ | | ✅ | | ✅ |
| `GET /users/{id}/posts`, `GET /posts/user/{id}` | ✅ | ✅ | | | | ✅ |
| `POST /posts` | ✅ | ✅ | | ✅ | | ✅ |
| `GET /posts` | | | | | | ✅ |
| `GET /posts/{id}` | ✅ | ✅ | | ✅ | | ✅ |
| `PATCH /posts/{id}` | ✅ | ✅ | ✅ | ✅ | | ✅ |
| `DELETE /posts/{id}` | ✅ | ✅ | ✅ | ✅ | | ✅ |
| `POST /posts/{id}/comments` | ✅ | ✅ | | ✅ | | ✅ |
| `GET /posts/{id}/comments` | ✅ | ✅ | | ✅ | | ✅ |
| `PATCH /comments/{id}` | ✅ | ✅ | ✅ | ✅ | | ✅ |
| `DELETE /comments/{id}` | ✅ | ✅ | ✅ | ✅ | | ✅ |
| `GET /admin/users` | | ✅ | ✅ | | | ✅ |
| `DELETE /admin/posts/{id}` | ✅ | ✅ | ✅ | ✅ | | ✅ |
| `DELETE /admin/comments/{id}` | ✅ | ✅ | ✅ | ✅ | | ✅ |

---

## End-to-End Example

A complete session using `curl` and [`jq`](https://jqlang.github.io/jq/):

```bash
BASE=http://localhost:8080/api/v1

# 1. Register and capture tokens
RESP=$(curl -s -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"S3cretPass!"}')
ACCESS_TOKEN=$(echo "$RESP" | jq -r .data.accessToken)
REFRESH_TOKEN=$(echo "$RESP" | jq -r .data.refreshToken)
USER_ID=$(echo "$RESP" | jq -r .data.user.id)

# 2. Create a post
POST_ID=$(curl -s -X POST $BASE/posts \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Hello","content":"First post"}' | jq -r .data.id)

# 3. Comment on it
COMMENT_ID=$(curl -s -X POST $BASE/posts/$POST_ID/comments \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"Nice!"}' | jq -r .data.id)

# 4. Read the public feed (no auth) and the comments (auth)
curl -s "$BASE/posts?sort=createdAt,desc" | jq .
curl -s "$BASE/posts/$POST_ID/comments" -H "Authorization: Bearer $ACCESS_TOKEN" | jq .

# 5. Access token expired? Rotate — and keep the NEW refresh token
RESP=$(curl -s -X POST $BASE/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
ACCESS_TOKEN=$(echo "$RESP" | jq -r .data.accessToken)
REFRESH_TOKEN=$(echo "$RESP" | jq -r .data.refreshToken)

# 6. Clean up and log out
curl -s -X DELETE $BASE/comments/$COMMENT_ID -H "Authorization: Bearer $ACCESS_TOKEN"
curl -s -X DELETE $BASE/posts/$POST_ID       -H "Authorization: Bearer $ACCESS_TOKEN"
curl -s -X POST   $BASE/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

### Client implementation tips

- **Handle `401` once, transparently:** on a `401` from a protected call, try `/auth/refresh` once, replace *both* stored tokens, and retry the original request. If refresh also returns `401`, send the user to login.
- **Serialize refresh calls.** Refresh tokens are single-use; two concurrent refreshes with the same token will make one fail.
- **Store refresh tokens securely** (e.g. HTTP-only storage on the client platform); they are valid for days.
- **Rely on HTTP status codes**, not `message` strings.

---

## Behavior Notes

Details that are easy to miss when integrating:

1. **`PATCH` = full replacement of the documented fields.** `PATCH /posts/{id}`, `/comments/{id}` and `/users/me` validate every field as required; send the complete set.
2. **Two routes list a user's posts** — `GET /users/{userId}/posts` and `GET /posts/user/{userId}` are equivalent. Neither returns `404` for an unknown user; they return an empty page.
3. **Public vs. protected reads.** Only `GET /posts` (the feed) is public. Reading a single post or its comments requires a token.
4. **Owner-only writes apply to admins too** on the regular endpoints; admins moderate through `/admin/**`.
5. **Deletes are hard and cascading** (user → posts/comments/tokens; post → comments).
6. **Email changes invalidate the current access token** (the token subject is the email).
7. **Logout revokes one refresh token only** and the already-issued access token remains valid until expiry.
8. **Timestamps** on entities have no zone offset (server-local, serialized as ISO local date-time); the envelope `timestamp` is UTC with `Z`.
9. **Sorting** only supports real entity properties (see [Pagination & sorting](#pagination--sorting)). Unknown sort properties are not specially handled and will surface as an error response.
10. **Routing errors** (unknown URL, wrong HTTP method, unsupported `Content-Type`) are not mapped individually and may be reported as a generic `500 An unexpected error occurred`.
11. **Case-insensitive identity.** Emails are normalized to lower-case; username and email uniqueness is checked case-insensitively.

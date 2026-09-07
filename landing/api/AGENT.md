# Moderation agent API

The submission pipeline is agent-ready: two endpoints, one bearer token.

## Auth

```
Authorization: Bearer <ADMIN_TOKEN>
```

`ADMIN_TOKEN` (or `CRASH_ADMIN_TOKEN` as fallback) — the same token the admin
page uses. Give the agent its own token value if you want separate audit:
set both `ADMIN_TOKEN` and `CRASH_ADMIN_TOKEN` and hand the agent `ADMIN_TOKEN`.

## Pull the review queue

```
GET https://nothing-modes.vercel.app/api/submissions?status=pending
```

Each item includes:

| field          | meaning                                            |
|----------------|----------------------------------------------------|
| `id`           | submission uuid                                    |
| `type`         | `template` or `glyph`                              |
| `payload`      | the sanitized content JSON to analyze              |
| `analysis`     | our static-analyzer output (advisory — re-verify)  |
| `handle`/`email` | author attribution + private contact             |
| `summary`/`capabilities` | content digest + required device capabilities |

Analyze `payload` yourself — look for phishing URLs, premium-rate SMS, privacy
exfiltration (location/contact patterns), denial-of-device (e.g. timeouts that
lock the screen permanently), and anything outside the action allowlist.
`analysis.findings` lists what the static pass already caught.

## Post a decision

```
POST https://nothing-modes.vercel.app/api/moderate
Content-Type: application/json

{
  "id": "<uuid>",
  "decision": "approve" | "reject",
  "reason": "shown to the author",
  "moderatedBy": "agent:<your-name>"
}
```

Only `pending` items can be decided. The author is emailed automatically when
they provided an address. Every decision is recorded with `moderated_by` —
humans show as `admin`, agents show as whatever name they pass.

## Public surface (no auth)

- `GET /api/library?type=template|glyph&q=…` — approved index, sanitized
- `GET /api/item?id=<uuid>` — one approved item incl. payload (counts downloads)
- `POST /api/share` — submit; `{type,title,handle,email?,github?,payload}`;
  returns `201` (queued), `409` (duplicate), `422` (rejected + findings),
  `429` (rate-limited)

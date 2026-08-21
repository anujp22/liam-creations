# A12 — admin authentication: options and trade-offs

**Status: memo. No code has been written for this. It is your decision.**

Read this when you have twenty minutes, pick one, and tell me. Nothing else in the
launch work depends on it, so it is not blocking — but it is the last real security
question left, and the answer gets harder to change once the shop is live.

---

## What is there today

`SecurityConfig` uses **HTTP Basic** against a single in-memory user whose username and
password come from `ADMIN_USERNAME` / `ADMIN_PASSWORD`. The session is stateless: there
is no server-side session at all.

The browser side (`frontend/src/api/admin.ts`) base64-encodes `username:password` once
at login, keeps it in `sessionStorage` under `lc-admin-auth`, and attaches it as an
`Authorization: Basic …` header on every admin request. `sessionStorage` means it is
gone when the tab closes.

As of this branch, `AdminCredentialsValidator` (A13) refuses to let the app start in
production with a blank, defaulted, or short password.

## What is actually wrong with it

Ranked by how much they would matter to this shop, not by how alarming they sound.

1. **The real password crosses the wire on every single request.** Not a token derived
   from it — the password itself, base64-encoded, which is encoding and not encryption.
   Over HTTPS that is encrypted in transit, so this is not "anyone can read it". But it
   means every admin request is another chance for the password to end up somewhere it
   should not: a proxy log, an error report, a misconfigured load balancer.

2. **It sits in `sessionStorage`, readable by any JavaScript on the page.** One
   cross-site scripting hole anywhere in the admin UI hands over the password, not just
   a session. A stolen session can be revoked; a stolen password is the shop.

3. **There is no logout that means anything server-side.** "Log out" clears
   `sessionStorage`. If the credentials leaked, clearing the browser does nothing —
   the only revocation is changing `ADMIN_PASSWORD` and redeploying.

4. **Nothing slows down guessing.** `ApiRateLimitFilter` caps `/api/admin/**` at 60
   requests a minute per IP, which is a throttle, not a lockout, and it is per-IP.
   A12 is partly why A13 enforces a long password: length is currently the only real
   defence.

**What is not wrong with it:** it is not "insecure" in the sense of being broken. Behind
HTTPS, with a long password, this is a defensible setup for a single-operator shop. The
question is whether you want better, not whether you must have it.

---

## Option 1 — Keep Basic, mitigate the sharp edges

**Effort: about half a day. Lowest risk.**

- Enforce HTTPS and add HSTS so the credentials can never travel in clear text, even
  once, even by accident (this is part of A15 regardless).
- Move the stored credential from `sessionStorage` to a JavaScript variable held in
  `AdminAuthContext`. It then dies on refresh — the owner logs in again after every
  reload, which is mildly annoying but removes the XSS-readable copy entirely.
- Add a per-username failed-attempt lockout on top of the existing per-IP limit, so
  guessing is bounded by attempts rather than by source address.
- Scrub `Authorization` from `RequestLoggingFilter` output and confirm the platform's
  access logs do not capture headers.

**Fixes:** 2 and 4, most of 1.
**Does not fix:** the password still crosses the wire every request; there is still no
real logout.
**Good if:** you want the risk meaningfully reduced this week without new moving parts.

## Option 2 — Short-lived bearer token

**Effort: one to two days.**

Login exchanges the password for a signed token valid for, say, 12 hours. Every later
request carries the token. The password crosses the wire exactly once per login.

- Needs a signing secret in the environment — one more thing that must be set correctly
  in production, and one more thing to rotate.
- The token still lives in the browser, so an XSS hole still steals *something* — but it
  steals a credential that expires by itself and can be invalidated by rotating the
  signing key.
- Real logout becomes possible.
- The frontend needs to handle expiry: a 401 mid-session must send the owner to the
  login screen without losing the product they were editing.

**Fixes:** 1, 3, most of 2.
**Cost:** a secret to manage, an expiry path to get right, and self-rolled token code
unless a library is added.
**Good if:** you want the password to stop travelling, and you accept a bit more machinery.

## Option 3 — Server-side session cookie

**Effort: one to two days. My recommendation.**

Login sets an `HttpOnly`, `Secure`, `SameSite=Strict` cookie. Spring Security has done
this for twenty years; it is close to the default rather than something to invent.

- **`HttpOnly` means JavaScript cannot read it at all.** This is the one option that
  genuinely closes the XSS-steals-the-credential hole rather than shrinking it.
- Logout is a real server-side operation.
- The app stops being stateless, which means either sticky sessions or a shared session
  store if it is ever run as more than one instance. For a shop this size that is a
  hypothetical, but it is the honest cost.
- CSRF protection has to be turned back on for admin routes — it is currently disabled,
  which is fine for header-based auth and *not* fine for cookies. This is the part most
  likely to be got wrong, and the part I would test hardest.

**Fixes:** 1, 2, 3.
**Cost:** CSRF must be re-enabled and verified; the app is no longer stateless.
**Good if:** you want the strongest of the three and are willing to spend the day.

---

## What I would do

**Option 3**, with **Option 1's HTTPS and logging items done regardless** — those are
A15 work that needs doing whichever way you go.

The reasoning: the worst realistic outcome here is an XSS hole in the admin UI leaking a
password that cannot be revoked without a redeploy. Options 1 and 2 shrink that;
`HttpOnly` cookies remove it. The cost is CSRF, which is a known problem with a known
answer, and statelessness, which this shop will not notice.

**If you would rather not spend the time now:** Option 1 is a genuinely reasonable place
to stop. Behind HTTPS with a 12-plus character password, the practical risk is low, and
"we did the cheap mitigations and moved on" is a real answer, not a cop-out. What I would
not do is leave it exactly as it is once the shop is public and the password is one the
owner also uses elsewhere.

---

## Related, not the same question

**`pg_trgm` for product search (from A14).** The storefront's search runs
`LOWER(title) LIKE '%term%'`. A leading wildcard cannot use a B-tree index, so this is a
sequential scan no matter what indexes exist — measured at 3–7 ms across 50,000 rows,
which is fine now and will stay fine for a catalog of hundreds.

Fixing it properly means `CREATE EXTENSION pg_trgm` plus a GIN index. That is a database
extension, so it needs to be available and permitted on whichever managed Postgres you
end up on (RDS and Cloud SQL both allow it), and it is a decision rather than a
migration. **Not urgent.** Revisit if the catalog reaches a few thousand products or if
search starts feeling slow — not before.

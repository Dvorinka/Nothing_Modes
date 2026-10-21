# Nothing Modes — Zero-Cost Outreach Plan

Goal: put Nothing Modes in front of people who are *already searching* for it —
Nothing owners who miss Samsung Modes & Routines, Android users who want a free
Tasker/MacroDroid alternative, and the Glyph community looking for apps that make
the lights useful. Budget: $0. Everything below uses free channels only.

Last updated: 2026-10-12

---

## 1. What we are selling (positioning)

The app in one line:

> **Nothing Modes — free, open-source automation for Nothing phones (works on
> every Android). The "Modes & Routines" Nothing OS never shipped, plus Glyph.**

Why people will pick it over incumbents — verified pain points:

| Competitor | Their ceiling | Our answer |
|---|---|---|
| Tasker | Paid (~$3.49–5), steep learning curve | Free, GPL-3.0, explicit WHEN/ONLY IF/THEN builder |
| MacroDroid | Free tier caps at 5 macros, proprietary | Unlimited automations, open source |
| Automate (LlamaLab) | Freemium, flowchart UI confuses casual users | Template catalog + JSON import/export |
| Google Home routines | Weak, cloud-only, unreliable | On-device engine, works offline, no account |
| Samsung Modes & Routines | Samsung-only | Same concept, any Android, plus Glyph |
| Glyph toys (Dot Hub, Glyphify…) | Single-purpose novelties | Glyph is an *actuator* inside real automations |

Unique angles nobody else has:

1. **The only automation app with first-class Glyph support** — stripe zones,
   25×25 / 13×13 Glyph Matrix, Glyph Toy integration. Android Authority literally
   published "Glyph Matrix still feels like a gimmick" — we are the counter-argument.
2. **Fully free AND open source (GPL-3.0)** — no macro caps, no account, no
   telemetry, no cloud. The FOSS angle matters to the exact audience that
   sideloads apps.
3. **Honest capability model** — asks the device what it can do; Shizuku optional
   with graceful degradation. "Fails honestly" is a differentiator vs. Tasker
   silently no-op'ing on Nothing OS (documented XDA complaint, see §5.4).
4. **Nothing-native design language** — looks like it shipped with Nothing OS.
   Show screenshots; the subreddit responds to this aesthetic.

Assets already in hand: landing site (`nothing-modes.vercel.app`) with Rybbit
analytics, `art/screenshots/`, fastlane feature graphic, `brag-output/brag.mp4`
launch video, `templates/` community catalog + `landing/library.html` template
sharing site, GitHub Releases with signed APKs (Obtainium-compatible out of the
box), self-hosted F-Droid repo, listing on ShizuPortal (awesome-shizuku) — done.

---

## 2. Foundations — distribution and listings (do these first)

These make every later post more effective: a reply that links to an IzzyOnDroid
or Play listing converts better than a raw APK link.

### 2.1 IzzyOnDroid repo — high priority

Official F-Droid is blocked (documented in `landing/fdroid/README.md`):
`play-services-location` for geofences + proprietary `glyph-matrix-sdk` jar.
IzzyOnDroid tolerates both — it exists precisely for apps in this gap.

- File an inclusion issue: https://gitlab.com/IzzyOnDroid/repo/-/issues
- Checklist before filing:
  - [ ] Release APK attached to GitHub release, **< 30 MB**
  - [ ] fastlane metadata present — already in tree, short_description ≤ 80 chars (currently 78, OK)
  - [ ] Release tags match `versionName` — already true (`v0.19.10`)
  - [ ] Self-updater must be **opt-in with explicit consent** — verify the
        UpdateManager flow asks before downloading; if it auto-checks without a
        toggle, add one and note it in the issue
- Payoff: r/fossdroid crowd installs from Izzy by default; listing = permanent
  credibility + auto-updates for that audience.
- Keep the self-hosted repo either way; document both.

### 2.2 Obtainium

Already works — Obtainium pulls GitHub releases natively. No work except:
- Add "Install via Obtainium" to the README and landing download section
  (one line: `github.com/Dvorinka/Nothing_Modes` in Obtainium → done).
- Submit the app config to the Obtainium crowdsourced app configs wiki.

### 2.3 Google Play — finish the in-progress build

`Play Store compliance.md` roadmap exists; landing already says "Play Store
build is in progress." Play listing matters less for installs than for
*legitimacy* — several channels below (r/androidapps "approved sources" rule,
blog reviewers) trust a Play link more than an APK. Note that the `play` flavor
drops lock-screen + self-update + some permissions; keep the GitHub build as
the "full" version and say so in every post.

### 2.4 Directory listings — one afternoon, permanent SEO

- **AlternativeTo** — create the app page, then mark it as an alternative on the
  pages for Tasker (56 listed alternatives, high traffic), MacroDroid, Automate,
  Samsung Modes and Routines, and Apple Shortcuts. Free account, crowdsourced —
  this is where "X alternative" searches land.
- **Slant.co** — add to "best Android automation apps" questions.
- **Fossdroid.com / FOSS app lists** — PRs to `awesome-android`,
  `awesome-shizuku` (already listed — confirm link points at repo),
  offa/android-foss or equivalent curated lists.
- **StackShare / SaaSHub** — low effort, minor SEO juice, optional.

---

## 3. Reddit — where the searchers are

### 3.1 Target subreddits and their rules (verified)

| Sub | Size | Self-promo policy | Action |
|---|---|---|---|
| r/NothingTech | ~85–110k | Standard Reddit rules; community tolerates genuine dev posts | **Launch post** + reply to automation threads |
| r/nothing | ~85k | Same | Cross-post variant, spaced 3–5 days later |
| r/droidappshowcase | small but purpose-built | Dev showcase IS the point | **First post goes here** — safest launchpad |
| r/fossdroid | ~50k | Welcomes devs; use **Application Release** flair for own apps | Launch post after IzzyOnDroid listing lands |
| r/androidapps | ~565k | **No top-level self-promo** — removed + ban risk; replies OK | Reply-only, in recommendation threads, disclose authorship |
| r/tasker | ~40k | Dev replies tolerated if relevant; no drive-by ads | Reply only in "free alternative / leaving Tasker" threads |
| r/MacroDroid | medium | Same | Reply in "5-macro limit" complaint threads |
| r/shizuku | small | Relevant — app is a Shizuku consumer | Short post or replies; already on awesome-shizuku |
| r/Android / r/GooglePixel | large | No self-promo at all | Skip posting; reply only if a thread directly asks |

### 3.2 Reply targets — threads that already exist

Search terms to run weekly on reddit (or via F5bot, §6):

- `"nothing phone" automation` / `"nothing" "modes and routines"`
- `"tasker alternative"`, `"macrodroid alternative"`, `"free automation app"`
- `"glyph" app` / `"glyph matrix" apps`

Known relevant threads to answer first:

- r/NothingTech + r/nothing recurring "is there a routines app?" threads
- The Reddit thread analyzed at reddit.sentinel-team.org/posts/1twxgkc —
  *"Why Is Samsung the Only OEM With a Siri Shortcuts Equivalent?"*
- Any "MacroDroid 5 macro limit" or "Tasker too complicated" complaint

**Reply formula** (do not deviate — this is what survives mod review):

1. Answer their actual question first, one sentence.
2. One line: "Full disclosure — I built this." Then name + link.
3. Say what it does *for their stated use case* in one sentence, not a feature list.
4. Offer the JSON template if it matches their scenario (templates/ catalog).

Template:

> Nothing OS doesn't ship a routines app, so the usual answers are Tasker (paid,
> steep) or MacroDroid (5 macros free). Disclosure: I built **Nothing Modes** —
> free, open-source, no caps. For your car case: WHEN Bluetooth connects to the
> car → THEN Wi-Fi on, location on. There's a template for exactly that.
> [GitHub link]. Glyph owners can also tie the lights to any mode.

### 3.3 The launch post (r/droidappshowcase, r/fossdroid, r/NothingTech)

Do NOT post the same text three times — Reddit flags duplicate submissions.
Write one core post, then rewrite the middle for each audience:

- **droidappshowcase version** — feedback-first: "built this, roast it."
- **fossdroid version** — lead with GPL-3.0, no telemetry, IzzyOnDroid/GitHub
  install, why official F-Droid can't carry it (GMS + Glyph SDK — this honesty
  plays well there).
- **NothingTech version** — lead with the pain: "Miss Samsung Modes & Routines?
  Nothing OS doesn't have one, so I built it." Screenshots + brag.mp4 video.
  Post on a weekday morning EU/US overlap.

Post structure:

1. Title states the problem, not the app: *"Nothing OS has no routines app, so
   I built one — free and open source"*
2. 3 bullets max of what it does (Sleep/Work modes, triggers, Glyph)
3. What it is NOT (no account, no cloud, no telemetry)
4. Links: GitHub + landing + install options
5. Ask: feedback + what templates people want

### 3.4 Reddit etiquette — the rules that keep you unbanned

- Disclose authorship every single time. One undeclared promo post poisons the well.
- One reply per thread, never paste identical replies across threads.
- If the account is new: spend ~2 weeks commenting normally first, or posts age
  in spam filters. Keep promo <10% of account activity.
- Never argue with "why would I not just use Tasker" — answer honestly
  (paid, closed, no Glyph); concede where Tasker is genuinely better.
- Space subreddit posts 3–5 days apart, not same-day carpet-bombing.

---

## 4. Nothing's own channels — the warmest audience

### 4.1 nothing.community (official forum)

Nothing **explicitly invites** developers to showcase Glyph work under the
**Co-Creation** tag (per their Glyph Dev Kit announcement). This is free,
sanctioned promotion to the exact target user.

Reply to these existing threads (they are literally asking for this app):

| Thread | What they want |
|---|---|
| nothing.community/d/30933-automationroutines-app-on-nothing-phones | Auto-DND at night, BT→car, routines like Samsung |
| nothing.community/d/31935-nothing-routines-app | Shortcuts/Modes&Routines equivalent for Phone 2a |
| nothing.community/d/1674-routines-automations | Location-based Wi-Fi toggling; told "use Tasker" |
| nothing.community/d/14869-useful-macros-for-nothing-phones | MacroDroid users sharing macros — offer JSON templates |
| nothing.community/d/40160-how-to-use-community-made-glyph-toys-a-guide | Glyph Toy audience — mention the Toy integration |

Then one showcase post under Co-Creation: build story + screenshots + GitHub.
Forum users there skew less technical — lead with "it just works, no account".

### 4.2 Official Nothing Discord (~70k members)

Join, find the community-projects/showcase channel, post once with screenshots
and the GitHub link. Also answer automation questions in support channels when
they appear — Discord search "tasker" / "routine" / "automate" monthly.

### 4.3 XDA Forums

- Post in **Android Apps & Games** (dev announcements are the norm there) and
  the Nothing Phone device subforums.
- Reply to xdaforums.com/t/4619413 — *"Neither Tasker nor MacroDroid work
  properly under rooted Nothing OS"* — the honest-failure/capability-detection
  angle is a direct answer to that complaint class.
- XDA also runs a front-page portal; a polished XDA thread occasionally gets
  picked up by their news writers.

---

## 5. Press and blogs — pitch the angle, not the app

Verified: Android Authority already covers third-party Glyph apps (Glyphify
review, "Glyph Matrix is a gimmick" opinion piece, Phone 3 Glyph Matrix
explainer). Pocket-lint ran a "5 Glyph Matrix Toys" roundup. These writers are
actively looking for Glyph software — pitch a *story*, not a press release.

### 5.1 Pitch list

| Outlet | Angle that fits their coverage |
|---|---|
| Android Authority | "Glyph Matrix felt like a gimmick — this app turns it into an automation output" (directly answers their own complaint article) |
| Pocket-lint | Add to/next round of their Glyph Toys coverage — but pitch modes+Glyph, not a toy |
| Android Police | Their automation workflow coverage (Tasker/MacroDroid comparisons); pitch the free-FOSS contender |
| 9to5Google | "Nothing OS still lacks routines; this community app fills it" |
| XDA Portal | Automation + Shizuku technical angle |
| How-To Geek / Beebom / Android Police listicles | "Best Nothing Phone apps" / "best automation apps" inclusions |
| It's FOSS / FOSS blogs | GPL + self-hosted F-Droid repo story |
| Product Hunt | One coordinated launch day — schedule after Play/Izzy listings are live |

### 5.2 Pitch template (keep under 120 words)

> Subject: The "Modes & Routines" Nothing OS never shipped — free, open source
>
> Hi [name] — you covered Glyph apps for the Phone 3 [link their article].
> Nothing owners keep asking for a Samsung-style Modes & Routines app (their
> forums are full of threads). I built one: Nothing Modes — GPL-3.0, no
> account, no telemetry, works offline. It also drives the Glyph Matrix from
> automations, which makes the lights useful instead of decorative.
> Code: github.com/Dvorinka/Nothing_Modes — happy to answer anything.

### 5.3 Owned content (SEO) — capture the searches ourselves

The landing page already ranks for "nothing modes." Extend it (or a /blog
section on the same Vercel site, $0) with three posts targeting real queries:

1. **"Samsung Modes & Routines on a Nothing phone"** — the exact query the
   forum threads show people typing. How-to, with screenshots.
2. **"Free Tasker alternative that isn't MacroDroid"** — honest comparison
   table (including where Tasker still wins).
3. **"Make the Glyph Matrix actually useful"** — ride the "gimmick" discourse;
   show automations driving the Matrix.

Each post ends with install links. Long-tail, zero maintenance, compounds.

Also worth 30 minutes: `llms.txt` on the landing domain and an
AlternativeTo-style comparison section on the site — AI assistants are
increasingly where "what app do I use for X" gets answered.

---

## 6. Monitoring — so threads find us (all free)

| Tool | Setup |
|---|---|
| **F5bot** | Free email alerts for keyword mentions on Reddit/HN/Lobsters. Keywords: `nothing phone automation`, `modes and routines`, `tasker alternative`, `macrodroid alternative`, `glyph matrix app`, `nothing routines` |
| **Google Alerts** | `nothing phone routines`, `"nothing phone" automation app` — catches blog coverage and new forum threads |
| **Saved Reddit searches** | Weekly manual pass over r/NothingTech, r/nothing, r/androidapps, r/tasker for the terms above |
| **GitHub traffic/stars** | Insights → traffic sources tells you which posts convert |
| **Rybbit** (already on landing) | Watch referrer spikes after each post |

When F5bot fires: reply within a few hours using the §3.2 formula. Speed
matters — first helpful answer gets the upvotes.

---

## 7. Content calendar — suggested order, ~2 weeks of effort

- **Day 1–2** — Foundations: file IzzyOnDroid issue, AlternativeTo listing,
  Obtainium README line, awesome-list PRs. Confirm Play build status.
- **Day 3** — nothing.community: reply to the 5 threads in §4.1 (each
  personalized), then the Co-Creation showcase post.
- **Day 4** — r/droidappshowcase launch post (safest first post; harvests
  feedback + testimonials for later posts).
- **Day 7** — r/fossdroid Application Release post (after Izzy listing lands
  or while pending — say which).
- **Day 9–10** — r/NothingTech main post (screenshots + brag.mp4), Nothing
  Discord showcase, XDA Apps & Games thread.
- **Day 12+** — r/nothing cross-post variant, press pitches (§5.2),
  Product Hunt scheduling if Play listing is live.
- **Ongoing** — F5bot replies, weekly saved-search pass, one SEO post per week
  for three weeks.

Every post links back to `nothing-modes.vercel.app` (the funnel hub) and the
GitHub repo; every repo/landing page links the install paths (GitHub APK,
Obtainium, self-hosted F-Droid, Izzy, Play when live).

---

## 8. What NOT to do

- No astroturfing, no alt accounts, no "my friend made this." GPL dev posts
  work *because* they're honest; the FOSS audience checks post history.
- No top-level promo posts in r/androidapps, r/Android, r/GooglePixel —
  removal + ban, and mods talk to each other.
- Don't oversell vs Tasker in r/tasker — concede its depth, win on
  free/open/Glyph/simple.
- Don't promise the Play version's missing features (lock screen, self-update)
  in posts that link the Play build — say "full build on GitHub."
- Don't mass-post the same text in one day (§3.4).

## 9. Success signals to watch

- GitHub stars / release download counts (the real KPI for a FOSS app)
- Landing referrers in Rybbit
- IzzyOnDroid listing going live; inclusion in community "best Nothing apps"
  lists and Glyph Toy roundups
- Someone *else* recommending the app in a thread — that's the channel working
  on its own

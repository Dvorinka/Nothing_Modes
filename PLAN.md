# NOTHING MODES — Master Execution Plan

> Production-quality, open-source Android automation app for Nothing phones.
> Inspired by Samsung Modes & Routines, Argus, Tasker, MacroDroid, Easer.
> Nothing OS design language. Shizuku capabilities. Glyph/Glyph Matrix integration.

---

## 0. Executive Summary

**What:** Nothing Modes — an automation app where the user creates Modes (persistent state configurations) and Routines (event-based automations) using a trigger + conditions + actions model, with a Nothing OS-inspired UI, capability-based feature detection, and optional Shizuku/Glyph integrations.

**How:** Fork/derive from Argus architecture (GPL-3.0, Kotlin multi-module), adapt the engine, replace the UI with a Nothing design system, add Nothing Glyph/Glyph Matrix providers, add capability detection, state restoration, conflict resolution, import/export, widgets, Quick Settings tiles, and optional AI rule generation.

**Where:** Local machine (code writing, editing, git, docs, planning) + Proxmox (heavy builds, CI/CD, instrumented tests, emulator). RAM-aware distribution.

**Primary target:** Nothing Phone (3) / Android 16-17 / Nothing OS 4.1-5.0.
**Secondary targets:** Phone (4a), (4a) Pro, (3a), (3a) Pro, (2a), (2a) Plus, (2), (1) — graceful degradation.

---

## 1. Environment & Resources

### Local Machine

| Resource | Value |
|---|---|
| OS | Linux 7.0.0-30-generic (Ubuntu 26.04) |
| RAM | 13 GiB total, ~7.5 GiB available |
| CPU | 16 cores |
| Disk | 15 GiB free (97% full — tight) |
| Java | OpenJDK 21.0.12 |
| Android SDK | ~/Android/Sdk (API 28-37, build-tools 35-37, NDK 27-28, emulator, system-image android-36.1) |
| Gradle | Not installed (use wrapper) |
| Kotlin | Not installed (comes with AGP) |
| gh | Authenticated as Dvorinka |
| Workspace | /home/tdvorak/Desktop/PROG+HTML/Nothing_Modes (local repo root) |

### Proxmox Remote

| Resource | Value |
|---|---|
| Host | 100.77.149.142 (NetBird), fallback 109.164.55.142 |
| RAM | 19 GiB total, ~14 GiB available |
| CPU | 8 cores |
| Disk | 12 GiB free (86% full) |
| Tools | git, devin, gh installed |
| Missing | Java, Android SDK, Gradle — must install for CI/CD |
| SSH key | ~/.ssh/proxmox_devin |

### Existing Relevant Repos

| Repo | Relevance |
|---|---|
| JackRushante/argus | PRIMARY architecture reference. GPL-3.0. Kotlin multi-module: engine-core, automation-android, brain-android, data, ui, device-tools, core-shizuku, app. |
| renyuneyun/Easer | SECONDARY reference. Historical automation concepts. License review needed before copying. |
| RikkaApps/Shizuku | Shizuku integration reference. |
| Nothing-Developer-Programme/Glyph-Developer-Kit | Glyph SDK for Phone (1)/(2) era devices. |
| Nothing-Developer-Programme/GlyphMatrix-Developer-Kit | Glyph Matrix SDK for Phone (3)+. |
| Dvorinka/Nothing-GlyphMatrix | User's existing repo. 10 Glyph toys, Glyph Matrix SDK AAR in libs/, AGP 8.10.1, Java. Package com.nothing.glyphmatrix. Has sdk/ directory with original SDK. |

---

## 2. Repository Setup

### Paths

- **Local repo:** `/home/tdvorak/Desktop/PROG+HTML/Nothing_Modes` (current workspace, already exists)
- **GitHub repo:** `Dvorinka/Nothing_Modes` (Dvorinka is the GitHub account/org)

### GitHub Repository

1. Create `Dvorinka/Nothing_Modes` on GitHub (private initially, public on release).
2. Initialize local repo at `/home/tdvorak/Desktop/PROG+HTML/Nothing_Modes`.
3. Set origin to `git@github.com:Dvorinka/Nothing_Modes.git`.
4. Create `main` branch with initial commit (PLAN.md, .gitignore, README.md).
5. All development work happens on feature branches.
6. PRs filed against `main` via `gh pr create`.

### Branch Strategy

```
main                    — stable, always builds, release-ready
develop                 — integration branch (optional, can skip if small team)
feature/phase-XX-name   — one branch per phase or sub-phase
proxmox-<timestamp>     — Proxmox remote work branches (auto-created by skill)
```

### Commit Discipline

- Conventional Commits format.
- Authorship trailer: `Authored By: TDvorak <info@tdvorak.dev>` (per tdvorak-fullstack rules).
- One logical change per commit.
- Build must pass before push (pre-push gate).

---

## 3. Technology Stack

| Layer | Choice | Reason |
|---|---|---|
| Language | Kotlin 2.0+ | Matches Argus, modern Android standard, null safety, coroutines. |
| UI | Jetpack Compose | Declarative, custom design systems, animation support. Replaces Argus's Material 3 with NothingTheme. |
| Build | Gradle Kotlin DSL + AGP 8.10.1+ | Matches existing setup, version catalog (libs.versions.toml). |
| Min SDK | 31 (Android 12) | Covers all Nothing phones (Phone 1 launched on Android 12). |
| Target SDK | 36 (Android 16) | Current stable. Bump to 37 when Android 17 is final. |
| Compile SDK | 36 | Match target. |
| Persistence | Room + DataStore | Room for automations/modes/logs (structured). DataStore for preferences (async, type-safe). |
| Scheduling | AlarmManager + WorkManager | AlarmManager for exact time triggers. WorkManager for deferrable/background. No polling loops. |
| DI | Hilt (Dagger) | Matches Argus, Android-standard, compile-time verification. |
| Serialization | Kotlin Serialization | Matches Argus, for JSON import/export format. |
| Annotation processing | KSP | Matches Argus, faster than KAPT. |
| Testing | JUnit5 + MockK + Turbine + Compose Testing | Unit, integration, UI tests. |
| Async | Kotlin Coroutines + Flow | Matches Argus, Android-standard. |

### Nothing-Specific

| Component | Source |
|---|---|
| Glyph Matrix SDK | AAR from Dvorinka/Nothing-GlyphMatrix/libs/ or GlyphMatrix-Developer-Kit |
| Glyph SDK | From Glyph-Developer-Kit (older devices) |
| Glyph Matrix SDK license | Audit needed (Nothing Developer Programme terms) |

---

## 4. Architecture Overview

### Module Structure (derived from Argus, adapted for Nothing Modes)

```
Nothing_Modes/
├── engine-core/              — Pure JVM Kotlin. NO Android deps.
│   ├── domain/               — Trigger, Condition, Action, Mode, Routine typed models
│   ├── runtime/              — ProgramInterpreter, deterministic execution
│   ├── validation/           — DraftValidator, bounds checking, security invariants
│   ├── scheduling/           — CronSchedule, DST handling, time trigger logic
│   ├── conflict/             — ConflictResolver, priority ordering, state ownership
│   ├── snapshot/             — StateSnapshot, restoration logic
│   └── fingerprint/          — SHA-256 rule fingerprinting
│
├── automation-android/       — Android runtime library
│   ├── triggers/             — AlarmManager, receivers (boot, charging, screen, connectivity, notifications)
│   ├── conditions/           — Android condition evaluators (battery, wifi, bluetooth, screen, etc.)
│   ├── scheduler/            — AlarmManager + WorkManager bridge
│   └── lifecycle/            — AutomationService, BOOT_COMPLETED receiver
│
├── capabilities/             — Capability abstraction layer (NEW, not in Argus)
│   ├── CapabilityManager     — Central registry, runtime detection
│   ├── CapabilityDetector     — Device/OS/SDK/permission detection
│   ├── PermissionManager     — Permission state tracking, guided setup
│   ├── controllers/          — One controller per feature domain
│   │   ├── BrightnessController (interface)
│   │   │   ├── AndroidModernBrightnessController  (API 33+)
│   │   │   └── AndroidLegacyBrightnessController   (API < 33)
│   │   ├── ExtraDimController
│   │   ├── DndController
│   │   ├── VolumeController
│   │   ├── ScreenTimeoutController
│   │   ├── DarkModeController
│   │   ├── ScreenStateController
│   │   └── ...
│   └── resolver/             — CapabilityResolver: Public API → Nothing API → Shizuku → Unsupported
│
├── nothing-integrations/     — Nothing-specific providers (NEW)
│   ├── NothingGlyphProvider      — Glyph SDK wrapper (Phone 1/2)
│   ├── NothingGlyphMatrixProvider — Glyph Matrix SDK wrapper (Phone 3+)
│   ├── NothingDeviceDetector     — Nothing device identification, OS version detection
│   └── NothingCapabilities       — Nothing-specific capability flags
│
├── core-shizuku/             — Shizuku gateway (from Argus, adapted)
│   ├── ShizukuGateway         — Single privileged gateway, shell UID
│   ├── ShizukuQueue           — Single-writer queue
│   └── ShizukuCommands        — Typed commands (no arbitrary shell)
│
├── data/                     — Room persistence (from Argus, extended)
│   ├── entities/              — Automation, Mode, Routine, ExecutionLog, StateSnapshot
│   ├── dao/                   — Room DAOs
│   ├── converters/            — Type converters for typed models
│   └── migration/             — Versioned migrations
│
├── brain-android/            — LLM transport (from Argus, optional)
│   ├── providers/             — OpenAI compat, Anthropic, custom bridge
│   ├── compiler/              — NL → structured rule draft
│   └── validator/             — Draft validation before user approval
│
├── import-export/            — JSON import/export (NEW)
│   ├── Exporter               — Automation → portable JSON
│   ├── Importer               — JSON → validated automation
│   ├── EaserImporter          — Best-effort Easer format conversion
│   └── schema/                — JSON schema, version migration
│
├── ui/                       — Jetpack Compose UI (replaced from Argus)
│   ├── theme/                 — NothingTheme, NothingTypography, NothingSpacing, NothingShapes, NothingMotion, NothingIcons
│   ├── components/            — NothingComponents (reusable Nothing-style composables)
│   ├── screens/               — Home, ModeEditor, RoutineEditor, ActionPicker, TriggerPicker, Settings, Compatibility, ExecutionLog, Permissions
│   ├── navigation/            — Navigation graph
│   └── preview/               — Compose previews
│
├── widget/                   — Home screen widget (NEW, later phase)
│   └── ModesWidget            — Active mode display, quick activation
│
├── quicksettings/            — Quick Settings tiles (NEW, later phase)
│   └── ModeTileService        — One tile per selected mode
│
└── app/                      — Application module
    ├── di/                    — Hilt modules, wiring
    ├── MainActivity           — Single activity, Compose navigation
    ├── Application            — Hilt entry, initialization
    └── manifest/              — Permissions, receivers, services
```

### Key Architectural Principles

1. **Automation engine is NOT Nothing-specific.** The engine (engine-core) knows nothing about Nothing phones. It operates on typed triggers/conditions/actions.

2. **Capability layer sits between engine and system.** Every action goes through CapabilityManager → resolver → controller. No direct system manipulation from UI or engine.

3. **Nothing integrations are isolated.** All Nothing SDK calls go through NothingGlyphProvider / NothingGlyphMatrixProvider. Never scattered.

4. **Shizuku is optional.** The app works without Shizuku. Shizuku is only used when no public API exists and the feature is important enough.

5. **Capability detection is runtime.** Never assume a feature exists based on OS version alone. Test at runtime, store in DeviceCapabilities.

6. **Resolution order:** Public Android API → Nothing API → Shizuku → documented workaround → unsupported.

### Data Flow

```
User creates Mode/Routine
  → UI validates input
  → engine-core validates draft (DraftValidator)
  → data persists to Room
  → automation-android schedules triggers (AlarmManager/WorkManager/receivers)
  → trigger fires
  → engine-core evaluates conditions (ConditionEvaluator)
  → engine-core executes actions via CapabilityManager
  → capabilities resolves each action:
      → Android API controller (preferred)
      → Nothing provider (if Nothing-specific)
      → Shizuku gateway (if privileged needed)
      → UNSUPPORTED (if nothing works)
  → data logs execution result
  → UI shows execution log
```

---

## 5. RAM-Aware Task Distribution

### Principle

Local machine: 7.5 GiB available RAM, 15 GiB disk, 16 cores.
Proxmox: 14 GiB available RAM, 12 GiB disk, 8 cores.

**Rule: anything that spawns a JVM daemon eating >3 GiB goes to Proxmox.**

### Local Tasks (low RAM, code-focused)

| Task | RAM estimate | Why local |
|---|---|---|
| Writing Kotlin/Java source | <500 MiB | Text editing, no compilation |
| Editing XML resources, manifests | <100 MiB | Text editing |
| Writing documentation | <100 MiB | Markdown |
| Git operations (commit, branch, merge) | <100 MiB | CLI |
| GitHub PR creation (gh) | <100 MiB | CLI |
| Architecture planning, design | <100 MiB | Thinking + writing |
| Code review, inspection | <200 MiB | Reading files |
| Research (web, repo inspection) | <200 MiB | Browsing |
| Lightweight unit tests (engine-core, no Android) | ~1-2 GiB | Gradle test, JVM only |
| Compose UI preview (single file) | ~2 GiB | IDE preview, not full build |

### Proxmox Tasks (high RAM, build-focused)

| Task | RAM estimate | Why Proxmox |
|---|---|---|
| Full Gradle build (assembleDebug) | 4-8 GiB | Gradle daemon + AGP + Kotlin compiler + R8 |
| Lint analysis (lintDebug) | 3-6 GiB | JVM-heavy analysis |
| R8/ProGuard full shrinking | 4-6 GiB | JVM optimization |
| Release APK assembly (assembleRelease) | 5-8 GiB | Full build + R8 + signing |
| Instrumented tests (emulator) | 4-8 GiB | Emulator (2-4 GiB) + test runner |
| Full test suite (all modules) | 3-5 GiB | Gradle test across modules |
| Dependency resolution / Gradle sync | 2-4 GiB | Network + parsing |
| CI/CD pipeline execution | 4-8 GiB | Full build + test + lint |

### Workflow

```
LOCAL (me, continuously):
  1. Write code, edit files, write docs
  2. Commit locally
  3. Push to GitHub
  4. Dispatch build/test to Proxmox via /proxmox skill
  5. Continue working on next task while Proxmox builds
  6. Check Proxmox results, integrate fixes
  7. Repeat

PROXMOX (remote, async):
  1. Receive dispatched task
  2. Sync repo from GitHub
  3. Run build/test/lint
  4. Report results (PR comment or log)
  5. If fixing: commit, push, PR
```

### Proxmox Setup Required (Phase 0)

1. Install Java 17 (or 21) on Proxmox.
2. Install Android SDK (cmdline-tools, platform-tools, build-tools, platforms API 31+36).
3. Install Gradle (or rely on wrapper).
4. Set ANDROID_HOME, JAVA_HOME environment.
5. Accept Android SDK licenses.
6. Install GitHub Actions self-hosted runner.
7. Configure runner labels: `self-hosted, linux, android`.

---

## 6. Subagent Strategy

### Models Available

| Model | Profile | Use case |
|---|---|---|
| GLM 5.2 High | `glm-general` / `glm-explore` / `glm-tester` | Research, architecture audit, code review, documentation, planning, test writing. Free quota. |
| SWE 1.7 | (via Proxmox dispatch) | Heavy implementation passes, complex debugging, large refactors. Paid but powerful. |

### Rules (from project prompt + AGENTS.md)

- MAX 1 subagent at a time. Never parallel.
- GLM 5.2 profiles are free — prefer these for most tasks.
- SWE 1.7 via Proxmox dispatch for heavy engineering.
- Combine: GLM 5.2 for research/audit/review, SWE 1.7 for implementation.
- Always inspect subagent output, verify, integrate before next subagent.
- Do most work inline (myself). Subagents only for genuinely parallelizable, large tasks.

### Subagent Usage Plan by Phase

| Phase | Subagent | Model | Task |
|---|---|---|---|
| 0 — Research | glm-explore | GLM 5.2 High | Audit Argus modules, Easer, Shizuku, Nothing SDKs. Produce findings report. |
| 1 — Argus foundation | None | — | Inline. Clone Argus, strip UI, adapt module names, get compiling. |
| 2 — Core engine | None | — | Inline. Implement first end-to-end automation (Sleep/Morning). |
| 3 — Android capabilities | glm-general | GLM 5.2 High | Implement capability controllers (brightness, extra dim, DND, volume). |
| 4 — Shizuku | None | — | Inline. Adapt core-shizuku from Argus, test connection states. |
| 5 — Glyph | glm-general | GLM .2 High | Implement NothingGlyphProvider from Glyph SDK. |
| 6 — Glyph Matrix | glm-general | GLM 5.2 High | Implement NothingGlyphMatrixProvider from Glyph Matrix SDK. |
| 7 — Persistence | None | — | Inline. Room entities, DAOs, migrations. |
| 8 — UI | SWE 1.7 (Proxmox) | SWE 1.7 | Heavy Compose UI implementation. Nothing design system + all screens. |
| 9 — Permissions UX | None | — | Inline. Guided permission setup. |
| 10 — State restoration | None | — | Inline. Snapshot system, restore logic. |
| 11 — Conflict handling | None | — | Inline. Priority, ordering, logging. |
| 12 — Widgets | None | — | Inline. Home screen widget. |
| 13 — Quick Settings | None | — | Inline. QS tiles. |
| 14 — AI | glm-general | GLM 5.2 High | Adapt brain-android, NL → rule draft compiler. |
| 15 — Easer import | glm-explore | GLM 5.2 High | Research Easer format, implement importer. |
| 16 — Compatibility | glm-explore | GLM 5.2 High | Device capability matrix, detection logic. |
| 17 — Testing | glm-tester | GLM 5.2 High | Write comprehensive test suites. |
| 18 — Security | None | — | Inline. Security review. |
| 19 — Performance | None | — | Inline. Battery/perf review. |
| 20 — Release | SWE 1.7 (Proxmox) | SWE 1.7 | Release build, signing, APK generation, final QA. |

### Subagent Task Definition Template

Before each subagent:
```
TASK: <narrowly scoped task>
DELIVERABLES: <expected outputs>
FILES/MODULES: <what it may modify>
ACCEPTANCE CRITERIA: <how to verify>
MODEL: <GLM 5.2 High or SWE 1.7>
```

After each subagent:
```
1. Inspect output (read changed files)
2. Run build/test where appropriate
3. Integrate result (merge, fix conflicts)
4. Verify acceptance criteria met
5. Only then start next subagent
```

---

## 7. CI/CD Pipeline

### Self-Hosted Runner on Proxmox

1. **Install runner:**
   ```bash
   # On Proxmox
   mkdir -p /root/actions-runner && cd /root/actions-runner
   curl -o actions-runner-linux-x64.tar.gz -L \
     https://github.com/actions/runner/releases/download/v2.321.0/actions-runner-linux-x64-2.321.0.tar.gz
   tar xzf actions-runner-linux-x64.tar.gz
   ./config.sh --url https://github.com/Dvorinka/Nothing_Modes \
     --token <REGISTRATION_TOKEN> --labels "self-hosted,linux,android"
   ./run.sh &
   # Or install as systemd service:
   ./svc.sh install
   ./svc.sh start
   ```

2. **Install Android toolchain on Proxmox:**
   ```bash
   # Java 17 (LTS, F-Droid compatible)
   apt install openjdk-17-jdk
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

   # Android cmdline-tools
   mkdir -p /root/android-sdk/cmdline-tools
   cd /root/android-sdk/cmdline-tools
   wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
   unzip commandlinetools-linux-11076708_latest.zip
   mv cmdline-tools latest

   export ANDROID_HOME=/root/android-sdk
   yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
     "platform-tools" "build-tools;36.0.0" "platforms;android-36"
   yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
   ```

3. **Workflow file:** `.github/workflows/ci.yml`
   ```yaml
   name: CI
   on: [push, pull_request]
   jobs:
     build:
       runs-on: [self-hosted, linux, android]
       steps:
         - uses: actions/checkout@v4
         - name: Setup JDK
           uses: actions/setup-java@v4
           with:
             distribution: 'temurin'
             java-version: '17'
         - name: Setup Android SDK
           uses: android-actions/setup-android@v3
         - name: Cache Gradle
           uses: actions/cache@v4
           with:
             path: |
               ~/.gradle/caches
               ~/.gradle/wrapper
             key: gradle-${{ hashFiles('**/*.gradle.kts', '**/libs.versions.toml') }}
         - name: Build
           run: ./gradlew assembleDebug
         - name: Lint
           run: ./gradlew lintDebug
         - name: Unit tests
           run: ./gradlew testDebugUnitTest
         - name: Upload APK
           uses: actions/upload-artifact@v4
           with:
             name: debug-apk
             path: app/build/outputs/apk/debug/*.apk
   ```

4. **Release workflow:** `.github/workflows/release.yml`
   ```yaml
   name: Release
   on:
     push:
       tags: ['v*']
   jobs:
     release:
       runs-on: [self-hosted, linux, android]
       steps:
         - uses: actions/checkout@v4
         - name: Build release APK
           run: ./gradlew assembleRelease
         - name: Sign APK
           # Signing config in build.gradle.kts or via secrets
         - name: Upload
           uses: actions/upload-artifact@v4
           with:
             name: release-apk
             path: app/build/outputs/apk/release/*.apk
   ```

### CI/CD vs Proxmox Skill Dispatch

Two mechanisms, complementary:

| Mechanism | When | How |
|---|---|---|
| GitHub Actions CI | Automatic on every push/PR | Self-hosted runner on Proxmox |
| Proxmox skill dispatch | Manual, for heavy tasks | `/proxmox <task> using <model>` |

CI runs automatically. Proxmox dispatch is for ad-hoc heavy work (full builds, emulator tests, debugging) that I trigger while continuing to work locally.

---

## 8. Phase Execution Plan

### Phase 0 — Research

**Goal:** Understand all reference projects, Nothing SDKs, Android APIs, target devices.

**Runs where:** Local (research) + 1 GLM 5.2 subagent for parallel audit.

**Tasks:**
1. Clone Argus, inspect every module (engine-core, automation-android, brain-android, data, ui, device-tools, core-shizuku, app).
2. Document Argus data model, engine, triggers, conditions, actions, scheduler, persistence, Shizuku integration, receivers, validation, test architecture.
3. Inspect Easer — event/state modeling, action/trigger patterns, edge cases. License review (GPL? LGPL?).
4. Inspect Shizuku — API surface, permission model, binding lifecycle.
5. Inspect Glyph Developer Kit — API surface, supported devices, capabilities.
6. Inspect Glyph Matrix Developer Kit — API surface, Phone (3) specifics, matrix dimensions.
7. Inspect Dvorinka/Nothing-GlyphMatrix — how SDK AAR is used, simulator pattern.
8. Research Android 16/17 API changes — Extra Dim, DND, brightness, notification policy, scheduling restrictions.
9. Research Nothing OS 4.1/5.0 feature differences — what's available, what's restricted.
10. Research Nothing Playground/Essential Apps ecosystem — integration possibilities.
11. Produce `docs/argus-integration.md`, `docs/easer.md`, `docs/shizuku.md`, `docs/nothing-sdk.md`, `docs/compatibility.md`.

**Deliverables:**
- Architecture audit document
- Argus module map (what to reuse, what to adapt, what to replace)
- Nothing SDK API surface documentation
- Compatibility matrix initial population
- License audit (THIRD_PARTY_NOTICES.md skeleton)

**Acceptance:** All reference repos inspected, findings documented, architecture decisions made.

---

### Phase 1 — Argus Foundation

**Goal:** Get Argus-derived architecture compiling as Nothing Modes.

**Runs where:** Local (code changes) + Proxmox (first build verification).

**Tasks:**
1. Create project at `/Dvorinka/Nothing_Modes` (or fallback).
2. Initialize Gradle multi-module project matching the module structure in section 4.
3. Port engine-core from Argus — domain models, runtime, validation, scheduling, fingerprinting.
4. Port automation-android — triggers, receivers, scheduler bridge.
5. Port core-shizuku — Shizuku gateway.
6. Port data — Room entities, DAOs.
7. Create app module — minimal wiring, Hilt setup.
8. Create placeholder ui module — empty Compose, no screens yet.
9. Create capabilities module — skeleton interfaces (BrightnessController, etc.).
10. Create nothing-integrations module — skeleton providers.
11. Configure version catalog (libs.versions.toml).
12. Configure build.gradle.kts for all modules.
13. First build: `./gradlew assembleDebug` — verify compilation.
14. First test: `./gradlew :engine-core:test` — verify engine tests pass.

**Deliverables:**
- Compiling multi-module project
- engine-core tests passing
- Minimal app that launches (blank screen)

**Acceptance:** `./gradlew assembleDebug` succeeds. `./gradlew test` passes. App installs and launches.

---

### Phase 2 — Core Automation Engine

**Goal:** First complete end-to-end automation: Sleep/Morning mode.

**Runs where:** Local (implementation) + Proxmox (build + test).

**Tasks:**
1. Define typed action models: DndAction, ExtraDimAction, BrightnessAction, GlyphAction.
2. Define typed trigger models: TimeTrigger, RecurringTimeTrigger, DayFilter.
3. Define typed condition models: BatteryCondition, ChargingCondition, TimeCondition, DayCondition.
4. Implement TriggerMatcher for time triggers.
5. Implement ConditionEvaluator for initial conditions.
6. Implement ProgramInterpreter — execute action list, collect results.
7. Implement CronSchedule — recurring time, weekdays, weekends, custom days.
8. Implement AlarmManager scheduling — exact alarm for time triggers, setAndAllowWhileIdle for Doze compatibility.
9. Implement BOOT_COMPLETED receiver — reschedule all automations after reboot.
10. Implement AutomationService — receives trigger, evaluates conditions, executes actions.
11. Create first automation: Sleep (22:30 → DND ON, Extra Dim ON, Brightness 10%, Glyph OFF).
12. Create second automation: Morning (07:00 → DND OFF, Extra Dim OFF, Brightness RESTORE, Glyph RESTORE).
13. Wire to capability controllers (stub implementations for now — return SUCCESS).
14. Test: create automation, schedule, verify trigger fires, verify actions execute.

**Deliverables:**
- End-to-end Sleep/Morning automation working (with stub controllers)
- Trigger → condition → action → persistence → scheduler flow proven
- engine-core unit tests for all components

**Acceptance:** Unit test creates Sleep automation, simulates 22:30 trigger, verifies action execution order. Same for Morning.

---

### Phase 3 — Android Capabilities

**Goal:** Replace stub controllers with real Android API implementations.

**Runs where:** Local (implementation) + Proxmox (build) + 1 GLM 5.2 subagent for controller implementation.

**Tasks:**
1. Implement CapabilityManager — central registry, runtime detection, caching.
2. Implement CapabilityDetector — device model, Android version, Nothing OS version, available permissions, Shizuku status, Nothing SDK availability.
3. Implement PermissionManager — permission state tracking, rationale display.
4. Implement BrightnessController:
   - AndroidModernBrightnessController (API 33+ — Settings.System.SCREEN_BRIGHTNESS, canWrite check)
   - AndroidLegacyBrightnessController (API < 33 — same API, different permission flow)
5. Implement ExtraDimController:
   - Check if EXTRA_DIM is available (SpectrumController or Settings.Secure)
   - Fallback to Shizuku if system setting not writable
   - Mark UNSUPPORTED if neither works
6. Implement DndController:
   - NotificationManager.setNotificationPolicy / setInterruptionFilter
   - Requires Notification Policy Access (ACCESS_NOTIFICATION_POLICY)
7. Implement VolumeController:
   - AudioManager.setStreamVolume for media, ring, alarm, notification
   - Vibration control where controllable
8. Implement ScreenTimeoutController:
   - Settings.System.SCREEN_OFF_TIMEOUT
   - canWrite check, fallback to Shizuku
9. Implement DarkModeController:
   - UiModeManager.setNightMode (API 31+ — may be restricted)
   - Fallback to Shizuku
10. Implement ScreenStateController:
   - Read screen state (display manager)
   - Screen on/off actions where permitted
11. Implement CapabilityResolver — resolution chain: Public API → Nothing API → Shizuku → Unsupported.
12. Wire all controllers through Hilt DI.
13. Test each controller with capability detection.

**Deliverables:**
- All MVP controllers implemented with real Android APIs
- Capability detection working
- Graceful degradation for unsupported features

**Acceptance:** Each controller tested. Brightness, DND, Extra Dim, Volume all work on Phone (3). Unsupported features show UNSUPPORTED status, not crash.

---

### Phase 4 — Shizuku Integration

**Goal:** Shizuku works as fallback for privileged operations.

**Runs where:** Local (implementation) + Proxmox (build + test).

**Tasks:**
1. Adapt core-shizuku from Argus — ShizukuGateway, ShizukuQueue, typed commands.
2. Implement Shizuku connection lifecycle:
   - Detect Shizuku installed (package manager check)
   - Detect Shizuku running (bind service)
   - Detect permission granted (Shizuku.checkSelfPermission)
   - Handle Shizuku process restart
   - Handle Shizuku unavailable
3. Implement typed Shizuku commands (NO arbitrary shell):
   - SetExtraDimCommand
   - SetBrightnessCommand
   - SetScreenTimeoutCommand
   - SetDarkModeCommand
   - Other privileged settings commands
4. Wire Shizuku into CapabilityResolver as fallback.
5. Implement Shizuku permission request flow in UI.
6. Test all states: installed/not installed, granted/revoked, running/stopped, process restarted, phone rebooted.

**Deliverables:**
- Shizuku integration working
- All Shizuku states handled gracefully
- No arbitrary shell execution exposed

**Acceptance:** Extra Dim works via Shizuku when public API is blocked. App degrades gracefully when Shizuku unavailable. No crash on any Shizuku state.

---

### Phase 5 — Nothing Glyph Integration

**Goal:** Glyph ON/OFF and Glyph API functionality on supported devices.

**Runs where:** Local (implementation) + Proxmox (build) + 1 GLM 5.2 subagent.

**Tasks:**
1. Obtain Glyph SDK from Glyph-Developer-Kit (AAR or source).
2. Implement NothingGlyphProvider:
   - Initialize Glyph SDK
   - Glyph ON/OFF
   - Glyph channel control (where API supports)
   - Glyph animation/pattern (where API supports)
3. Implement NothingDeviceDetector:
   - Detect Nothing device by model/manufacturer
   - Detect Nothing OS version
   - Detect Glyph support (Phone 1, Phone 2)
4. Implement NothingCapabilities — Glyph support flag.
5. Wire NothingGlyphProvider into CapabilityResolver.
6. Implement GlyphAction in engine-core.
7. Test on Phone (3) if available (Phone 3 uses Glyph Matrix, not legacy Glyph — but check backward compat).

**Deliverables:**
- NothingGlyphProvider working on supported devices
- Graceful UNSUPPORTED on non-Nothing or non-Glyph devices

**Acceptance:** Glyph action executes on Nothing device with Glyph. Returns UNSUPPORTED on other devices. No crash on non-Nothing hardware.

---

### Phase 6 — Nothing Glyph Matrix Integration

**Goal:** Glyph Matrix functionality on Phone (3).

**Runs where:** Local (implementation) + Proxmox (build) + 1 GLM 5.2 subagent.

**Tasks:**
1. Obtain Glyph Matrix SDK from GlyphMatrix-Developer-Kit or Dvorinka/Nothing-GlyphMatrix/libs/.
2. Study SDK API — matrix dimensions, frame API, animation API, channel mapping.
3. Implement NothingGlyphMatrixProvider:
   - Initialize Glyph Matrix SDK
   - Matrix frame control
   - Pattern/animation playback
   - Channel-specific control
4. Add GlyphMatrixCapabilities — matrix dimensions, supported features.
5. Implement GlyphMatrixAction in engine-core (typed, not string).
6. Wire NothingGlyphMatrixProvider into CapabilityResolver.
7. Implement Glyph Matrix simulator (for non-Phone-3 testing — reference Dvorinka/Nothing-GlyphMatrix simulator pattern).
8. Test on Phone (3).
9. Document exact limitations (what the API allows, what it doesn't).

**Deliverables:**
- NothingGlyphMatrixProvider working on Phone (3)
- Simulator for development without hardware
- Limitations documented

**Acceptance:** Glyph Matrix action executes on Phone (3). Simulator works on any device. Limitations documented in docs/nothing-sdk.md.

---

### Phase 7 — Persistence

**Goal:** Automations survive app restart, process death, reboot.

**Runs where:** Local (implementation) + Proxmox (build + test).

**Tasks:**
1. Define Room entities:
   - AutomationEntity (id, name, type [mode/routine], enabled, priority, fingerprint)
   - TriggerEntity (automationId, type, params JSON)
   - ConditionEntity (automationId, type, params JSON, combinator [AND/OR/NOT])
   - ActionEntity (automationId, type, params JSON, order)
   - ExecutionLogEntity (id, automationId, timestamp, results JSON, duration, status)
   - StateSnapshotEntity (id, automationId, setting, previousValue, timestamp)
2. Implement DAOs with proper queries.
3. Implement type converters (Kotlin Serialization for typed models ↔ JSON).
4. Implement Database with versioned migrations.
5. Implement AutomationRepository — CRUD operations, query by type, query by enabled.
6. Implement ExecutionLogRepository — append-only, query recent, query by automation.
7. Implement StateSnapshotRepository — save, restore, delete by automation.
8. Use DataStore for app preferences (Shizuku consent, AI consent, theme, etc.).
9. Test: create automation, kill process, reopen, verify automation exists.
10. Test: create automation, reboot (simulate via BOOT_COMPLETED receiver), verify rescheduled.

**Deliverables:**
- Full Room persistence layer
- DataStore for preferences
- Migrations framework
- Persistence tests passing

**Acceptance:** Automation survives process death and reboot. Execution logs persist. State snapshots persist.

---

### Phase 8 — Nothing UI

**Goal:** Polished Nothing OS-style UI. Not generic Material 3.

**Runs where:** Local (design + implementation) + Proxmox (build) + SWE 1.7 subagent for heavy Compose work.

**Tasks:**
1. Research Nothing OS 5 design language — Geist typography, monochrome palette, geometric layouts, micrographics, technical/hardware-inspired visual language.
2. Investigate Geist font licensing — use legally distributable alternative if needed (e.g., Space Grotesk, JetBrains Mono for technical labels).
3. Create design system:
   - NothingTheme — color scheme (monochrome, wallpaper-aware), dark default
   - NothingTypography — large headings, precise labels, dot-matrix accents (restrained)
   - NothingSpacing — centralized spacing tokens (no scattered 16.dp)
   - NothingShapes — geometric, minimal rounding
   - NothingMotion — smooth, restrained animations
   - NothingIcons — technical/hardware-inspired icon set
   - NothingComponents — reusable composables (NothingCard, NothingSwitch, NothingButton, NothingListTile, NothingStatusIndicator)
4. Implement screens:
   - **HomeScreen** — active modes, inactive modes, next scheduled automation, quick activation
   - **ModeEditorScreen** — Name → WHEN → DO workflow
   - **RoutineEditorScreen** — Name → WHEN (event) → DO workflow
   - **ActionPickerScreen** — categorized actions (Display, Sound, Connectivity, Apps, Glyph, Device, Advanced), search, unavailable actions shown with explanation
   - **TriggerPickerScreen** — categorized triggers (Time, Device, Connectivity, Apps, Notifications, Location)
   - **SettingsScreen** — General, Automation, Permissions, Shizuku, Nothing Integration, Notifications, Backup & Restore, AI, About, Compatibility
   - **CompatibilityScreen** — detailed device/OS/capability status (deep in Settings, not dominant)
   - **ExecutionLogScreen** — observable automation execution history with results
   - **PermissionsScreen** — guided permission setup, what's missing, fix buttons
5. Implement navigation graph (Compose Navigation).
6. Implement state management (ViewModels + StateFlow, matching Argus pattern).
7. Visual refinement pass — spacing, motion, typography, color.
8. Accessibility — content descriptions, touch targets, contrast.

**Deliverables:**
- Complete Nothing-style Compose UI
- All screens functional
- Design system centralized
- No scattered magic numbers

**Acceptance:** UI looks like Nothing OS, not generic Material 3. All screens navigate correctly. Design tokens centralized. Accessibility verified.

---

### Phase 9 — Permissions UX

**Goal:** User can identify exactly what permissions are missing and fix them.

**Runs where:** Local (implementation) + Proxmox (build).

**Tasks:**
1. Implement PermissionManager UI integration:
   - Show required permissions per automation
   - Show granted/denied status
   - Show rationale text
   - One-tap fix buttons (launch system settings)
2. Permission categories:
   - Notification Policy Access (DND)
   - Notification Access (notification triggers)
   - Write Settings (brightness, screen timeout)
   - Shizuku (privileged operations)
   - Exact Alarm (Android 12+ — SCHEDULE_EXACT_ALARM)
   - Foreground Service (automation service)
   - Boot (RECEIVE_BOOT_COMPLETED)
   - Accessibility (only if needed, with explanation)
3. Implement permission check before automation execution:
   - If missing: show notification "Automation X needs permission Y"
   - Do not crash, do not silently fail
4. Implement onboarding permission setup (first launch).

**Deliverables:**
- Guided permission setup
- Per-automation permission display
- No crashes from missing permissions

**Acceptance:** User can see exactly what's missing. Fix buttons work. Automation with missing permissions shows clear message, doesn't crash.

---

### Phase 10 — State Restoration

**Goal:** Modes restore previous values on deactivation.

**Runs where:** Local (implementation) + Proxmox (build + test).

**Tasks:**
1. Implement StateSnapshot system:
   - Before mode activation: snapshot current values of all settings the mode will change
   - Store snapshots in StateSnapshotEntity
   - On mode deactivation: restore from snapshots
2. Handle BrightnessAction.RESTORE — read snapshot, restore brightness.
3. Handle GlyphAction.RESTORE — read snapshot, restore glyph state.
4. Handle all RESTORE action variants.
5. Handle overlapping modes:
   - Mode A activates (snapshots brightness=70)
   - Mode B activates (snapshots brightness=10, which was set by A)
   - Mode B deactivates (restores brightness=10)
   - Mode A deactivates (restores brightness=70)
6. Handle interrupted execution:
   - Process killed mid-execution: on next start, check for incomplete snapshots
   - Offer to restore or discard
7. Handle reboot:
   - Snapshots persist in Room
   - On boot, check for active modes and their snapshots
8. Test: activate Sleep, verify snapshot taken. Deactivate, verify restored. Kill process, verify recovery.

**Deliverables:**
- State snapshot/restore system
- Overlapping mode handling
- Interrupted execution recovery
- Reboot persistence

**Acceptance:** Sleep mode snapshots brightness, restores on deactivation. Overlapping modes restore correctly. Process death recovery works.

---

### Phase 11 — Conflict Handling

**Goal:** Deterministic conflict resolution when multiple automations modify the same setting.

**Runs where:** Local (implementation) + Proxmox (build + test).

**Tasks:**
1. Implement ConflictResolver:
   - Priority field on automation (user-configurable)
   - Higher priority wins for same setting
   - Same priority: last-activated wins (with log entry)
2. Implement execution ordering:
   - Sort actions by automation priority
   - Within automation: execute in order
3. Implement state ownership:
   - Track which automation "owns" each setting
   - When owner deactivates, restore to previous owner's value or snapshot
4. Implement conflict logging:
   - Log when conflict occurs
   - Show in execution log: "Brightness set by Sleep (priority 5) overrode Work (priority 3)"
5. Test: two automations modify brightness, verify higher priority wins. Verify log entry.

**Deliverables:**
- Conflict resolver with priority
- State ownership tracking
- Conflict logging

**Acceptance:** Two automations modifying brightness: higher priority wins, logged. Lower priority deactivation doesn't override higher priority.

---

### Phase 12 — Home Screen Widget

**Goal:** Widget shows active mode and allows quick activation.

**Runs where:** Local (implementation) + Proxmox (build + test).

**Tasks:**
1. Implement ModesWidget (Glance or RemoteViews):
   - Show active mode name and status
   - Show next scheduled automation
   - Quick-activate buttons for top modes
2. Widget updates:
   - On mode activation/deactivation
   - On schedule change
   - Periodic (WorkManager, every 30 min — not polling, just widget refresh)
3. Test widget after reboot — verify it shows correct state.

**Deliverables:**
- Home screen widget
- Works after reboot

**Acceptance:** Widget shows active mode. Quick-activate works. Survives reboot.

---

### Phase 13 — Quick Settings Tiles

**Goal:** User can activate modes from Quick Settings without opening app.

**Runs where:** Local (implementation) + Proxmox (build + test).

**Tasks:**
1. Implement ModeTileService (TileService):
   - One tile per user-selected mode
   - Tile state: active/inactive
   - Tap to toggle mode
2. Tile configuration:
   - User selects which modes get tiles (Settings)
   - Max tiles limited by Android (usually 3-5)
3. Test from locked phone where Android permits.

**Deliverables:**
- Quick Settings tiles for selected modes
- Toggle from QS panel

**Acceptance:** Tile appears in QS panel. Tap toggles mode. State updates correctly.

---

### Phase 14 — AI Rule Generation (Optional)

**Goal:** Natural language → structured rule draft → user approval.

**Runs where:** Local (implementation) + Proxmox (build) + 1 GLM 5.2 subagent.

**Tasks:**
1. Adapt brain-android from Argus:
   - LLM transport (OpenAI compat, Anthropic, custom bridge)
   - Provider catalog
   - Encrypted key store
2. Implement NL → rule draft compiler:
   - User types: "Every weekday at 7 turn off DND, turn off Extra Dim and restore my brightness."
   - LLM generates: TRIGGER(07:00, MON-FRI) + ACTIONS(DND OFF, ExtraDim OFF, Brightness RESTORE)
   - Output is typed draft, NOT shell commands
3. Implement DraftValidator (from Argus):
   - Closed vocabulary check
   - Bounds on every field
   - Security invariants
4. User preview:
   - Show generated rule rendered from types (not LLM paraphrase)
   - SHA-256 fingerprint
   - Approve/Reject buttons
5. AI NEVER executes. AI only generates drafts. Execution is deterministic.
6. Settings: AI consent, provider config, API key (encrypted).

**Deliverables:**
- Optional AI rule generation
- Draft → validate → preview → approve flow
- AI never executes

**Acceptance:** User types NL, gets typed draft, approves, automation saved. AI cannot execute anything. App works fully without AI.

---

### Phase 15 — Easer Import

**Goal:** Best-effort import of Easer automations.

**Runs where:** Local (implementation) + Proxmox (build) + 1 GLM 5.2 subagent for Easer format research.

**Tasks:**
1. Research Easer data format (JSON/XML, structure, field names).
2. Implement EaserImporter:
   - Parse Easer export file
   - Map Easer events → Nothing Modes triggers
   - Map Easer conditions → Nothing Modes conditions
   - Map Easer operations → Nothing Modes actions
   - Unsupported items: visible in import preview, not silently dropped
3. Import flow: select file → parse → preview (with unsupported items marked) → user confirms → save.
4. Never execute arbitrary code from imported files.

**Deliverables:**
- EaserImporter with best-effort conversion
- Unsupported items visible in preview
- No arbitrary code execution

**Acceptance:** Easer file imports with supported items converted. Unsupported items shown. User confirms before saving.

---

### Phase 16 — Compatibility

**Goal:** Capability matrix for all Nothing devices. Graceful degradation.

**Runs where:** Local (implementation) + Proxmox (build) + 1 GLM 5.2 subagent for device research.

**Tasks:**
1. Populate compatibility matrix:
   - Phone (3): Android 16/17, Nothing OS 4.1/5.0, Glyph Matrix, Glyph, DND, Extra Dim, Brightness, Shizuku
   - Phone (4a), (4a) Pro, (3a), (3a) Pro, (2a), (2a) Plus: investigate capabilities
   - Phone (2), (1): legacy support, no new OS 5 features
2. Implement DeviceCompatibilityScreen (deep in Settings):
   - Device model, Android version, Nothing OS version
   - Per-capability status: Supported / Limited / Requires permission / Unsupported
   - Explanation for each limitation
3. Implement runtime capability detection for each device.
4. Ensure compatibility does NOT dominate the app — it's in Settings → About & Compatibility.

**Deliverables:**
- Compatibility matrix documented
- Device Compatibility screen
- Runtime detection for all devices

**Acceptance:** Phone (3) shows all supported capabilities. Older devices show graceful degradation. No false "supported" claims.

---

### Phase 17 — Testing

**Goal:** Comprehensive test coverage.

**Runs where:** Local (test writing) + Proxmox (test execution) + 1 GLM 5.2 tester subagent.

**Tasks:**
1. Unit tests (engine-core):
   - TriggerMatcher: all trigger types
   - ConditionEvaluator: all condition types, AND/OR/NOT
   - ProgramInterpreter: action execution, result collection
   - CronSchedule: recurring, DST, edge cases
   - DraftValidator: valid/invalid drafts, bounds, security
   - ConflictResolver: priority, ordering
   - StateSnapshot: save, restore, overlapping
2. Integration tests (automation-android):
   - Trigger → condition → action flow
   - AlarmManager scheduling
   - BOOT_COMPLETED rescheduling
   - Receiver registration
3. Capability tests (capabilities):
   - Each controller: success, permission denied, unsupported
   - CapabilityResolver: resolution chain
   - CapabilityDetector: device detection
4. Shizuku tests (core-shizuku):
   - Connected, disconnected, permission revoked, process restarted
5. Nothing integration tests (nothing-integrations):
   - Glyph provider (with simulator)
   - Glyph Matrix provider (with simulator)
6. Persistence tests (data):
   - CRUD operations
   - Migration tests
   - Process death recovery
7. Import/Export tests:
   - Export → import roundtrip
   - Invalid file handling
   - Easer import with unsupported items
8. UI tests (ui):
   - Compose testing for each screen
   - Navigation graph
   - Design system consistency
9. Doze/battery saver tests:
   - Verify alarms fire under Doze (setAndAllowWhileIdle)
   - Verify behavior under battery saver
10. Reboot tests:
    - Automations rescheduled after reboot
    - State snapshots preserved

**Deliverables:**
- Unit, integration, UI test suites
- All tests passing

**Acceptance:** `./gradlew test` passes. `./gradlew connectedAndroidTest` passes (on emulator). Coverage > 70% on engine-core.

---

### Phase 18 — Security/Privacy Audit

**Goal:** No security vulnerabilities.

**Runs where:** Local (audit) + Proxmox (build verification).

**Tasks:**
1. Review imported JSON:
   - No arbitrary code execution
   - Validated before saving
   - Schema versioning
2. Review AI-generated rules:
   - Draft validation before approval
   - No shell command generation
   - Closed vocabulary
3. Review Shizuku:
   - No arbitrary shell execution
   - Typed commands only
   - User consent for privileged operations
4. Review exported components:
   - No unnecessary exported activities/services/receivers
   - Deep links validated
5. Review notification listener:
   - No logging of notification content (beyond what's needed for triggers)
   - No network transmission of notification data
6. Review accessibility (if used):
   - Justification documented
   - Minimal data access
7. Review local storage:
   - API keys encrypted
   - No secrets in logs
   - No sensitive data in plain text
8. Review broadcasts:
   - No sensitive data in broadcast intents
   - Permissions on custom broadcasts

**Deliverables:**
- Security audit report
- All findings addressed or documented

**Acceptance:** No critical security issues. All exported components justified. No arbitrary code execution paths.

---

### Phase 19 — Performance/Battery Audit

**Goal:** Minimal battery impact, fast startup, low memory.

**Runs where:** Local (audit) + Proxmox (profiling).

**Tasks:**
1. Startup time: measure cold start, optimize if > 2 seconds.
2. Memory: profile heap usage, check for leaks.
3. Background CPU: verify no polling loops, no unnecessary wakeups.
4. Battery: verify event-driven architecture, no permanent foreground service (unless needed).
5. Database: check query performance, add indexes if needed.
6. Scheduler: verify alarms are exact only when needed, inexact otherwise.
7. UI rendering: check for jank, overdraw, unnecessary recomposition.

**Deliverables:**
- Performance report
- Battery impact assessment
- Optimizations applied

**Acceptance:** No polling loops. No unnecessary foreground service. Startup < 2 seconds. No obvious memory leaks.

---

### Phase 20 — Release

**Goal:** Installable, signed release APK.

**Runs where:** Local (configuration) + Proxmox (release build) + SWE 1.7 subagent for final QA.

**Tasks:**
1. Configure signing (keystore generation, or user-provided).
2. Configure ProGuard/R8 rules.
3. Set version name/code.
4. Set application ID (com.nothing.modes or com.dvorinka.nothingmodes).
5. Verify manifest: permissions, exported components, icons, splash screen.
6. Build release APK: `./gradlew assembleRelease`.
7. Install APK on Phone (3).
8. Final smoke test:
   - Install
   - Grant permissions
   - Create Sleep mode
   - Activate
   - Verify actions execute
   - Deactivate
   - Verify restoration
   - Reboot
   - Verify automation rescheduled
   - Test Shizuku
   - Test Glyph
   - Test Glyph Matrix
9. Generate release notes.
10. Create GitHub release with APK artifact.

**Deliverables:**
- Signed release APK
- GitHub release
- Final smoke test passed

**Acceptance:** APK installs on Phone (3). All core workflows work. Release published.

---

## 9. Documentation Plan

All documentation in `docs/`:

| Document | Phase | Content |
|---|---|---|
| README.md | 1 | Project overview, build instructions, install |
| docs/architecture.md | 1 | Module structure, data flow, design decisions |
| docs/argus-integration.md | 0 | Reused/adapted/replaced components from Argus |
| docs/easer.md | 0/15 | Easer research, import format, limitations |
| docs/shizuku.md | 4 | Shizuku integration, commands, states |
| docs/nothing-sdk.md | 5/6 | Glyph + Glyph Matrix SDK, API surface, limitations |
| docs/compatibility.md | 16 | Device matrix, capability detection |
| docs/permissions.md | 9 | Permission requirements, rationale |
| docs/automation-format.md | 7 | JSON import/export schema |
| docs/development.md | 1 | Build setup, dev environment, contributing |
| docs/testing.md | 17 | Test strategy, running tests, coverage |
| docs/security.md | 18 | Security audit, threat model |
| docs/design.md | 8 | Nothing design system, tokens, components |
| docs/release.md | 20 | Release process, signing, distribution |
| THIRD_PARTY_NOTICES.md | 0 | License audit for all dependencies + reference repos |

---

## 10. License Audit

| Source | License | Action |
|---|---|---|
| Argus (JackRushante) | GPL-3.0 | Derivative must stay GPL-3.0. Reuse architecture, attribute. |
| Easer (renyuneyun) | GPL-3.0 (verify) | License review before copying any code. Implement independently if needed. |
| Shizuku (RikkaApps) | Apache-2.0 (verify) | Use as dependency, attribute. |
| Glyph Developer Kit (Nothing) | Check Nothing Developer Programme terms | Use SDK as dependency, do not extract proprietary assets. |
| Glyph Matrix Developer Kit (Nothing) | Check terms | Same as above. |
| Dvorinka/Nothing-GlyphMatrix | User's own repo | Can reference SDK AAR, simulator patterns. |

**Nothing Modes license:** GPL-3.0 (required if deriving from Argus).

---

## 11. Definition of Done

- [ ] Clean build (`./gradlew assembleDebug` succeeds)
- [ ] Release build (`./gradlew assembleRelease` succeeds, signed)
- [ ] Unit tests pass (`./gradlew test`)
- [ ] Integration tests pass
- [ ] UI tests pass where applicable
- [ ] Phone (3) tested on hardware
- [ ] Android 16 tested
- [ ] Android 17 tested (where available)
- [ ] Nothing OS 4.1 tested
- [ ] Nothing OS 5.0 tested (where available)
- [ ] Shizuku tested (all states)
- [ ] Glyph tested
- [ ] Glyph Matrix tested
- [ ] DND tested
- [ ] Extra Dim tested
- [ ] Brightness tested
- [ ] Scheduling tested (AlarmManager, Doze, reboot)
- [ ] Reboot tested
- [ ] Process death tested
- [ ] Permissions tested
- [ ] State restoration tested
- [ ] Conflict handling tested
- [ ] Import/export tested
- [ ] Error handling tested (no swallowed exceptions)
- [ ] Compatibility screen implemented
- [ ] Documentation complete (all docs/ files)
- [ ] License audit complete (THIRD_PARTY_NOTICES.md)
- [ ] Security audit complete
- [ ] Battery review complete
- [ ] CI/CD pipeline running on Proxmox
- [ ] Release APK generated
- [ ] Release APK installed on Phone (3)
- [ ] Final physical-device smoke test completed
- [ ] GitHub release published

---

## 12. Execution Rhythm

```
REPEAT UNTIL DONE:

  1. LOCAL: Pick next task from current phase
  2. LOCAL: Implement (write code, edit files)
  3. LOCAL: Commit locally
  4. LOCAL: Push to GitHub
  5. PROXMOX: Dispatch build+test via /proxmox skill (if RAM-heavy)
     OR: CI auto-triggers on push
  6. LOCAL: Continue to next task (do not wait for Proxmox)
  7. CHECK: Proxmox/CI results
     - If pass: continue
     - If fail: fix locally, re-push
  8. INTEGRATE: Pull any Proxmox branch work, merge
  9. PHASE COMPLETE: Verify all phase deliverables, move to next phase
  10. NEVER STOP: If blocked, document, implement fallback, continue

SUBAGENT USAGE (max 1 at a time):
  - When a phase needs a subagent: dispatch GLM 5.2 or SWE 1.7
  - Wait for completion, inspect, verify, integrate
  - Then continue inline or dispatch next subagent
```

---

## 13. Immediate Next Steps (After Plan Approval)

1. **Create GitHub repo** — `gh repo create Dvorinka/Nothing_Modes --private`.
2. **Initialize local repo** — git init at current workspace, initial commit with PLAN.md.
3. **Set up Proxmox Android toolchain** — install Java 17, Android SDK, configure self-hosted runner.
4. **Begin Phase 0** — research Argus, Easer, Shizuku, Nothing SDKs.
5. **Dispatch GLM 5.2 subagent** for Argus module audit (if needed for parallel research).
6. **Begin Phase 1** — scaffold project structure, port Argus engine-core.

---

## 14. Risk Register

| Risk | Mitigation |
|---|---|
| Disk space (15G local, 12G Proxmox) | Source code only locally. Build artifacts on Proxmox. Clean Gradle caches periodically. |
| No physical Phone (3) for testing | Use emulator (android-36.1 system image available). Document what needs hardware verification. |
| Nothing SDK license restrictions | Use SDK as dependency only. Do not extract proprietary assets. Document terms. |
| Argus GPL-3.0 contamination | Nothing Modes is GPL-3.0. No issue if we accept GPL. |
| Android 17 not final | Target SDK 36 (Android 16). Bump when 37 is final. |
| Extra Dim API restrictions | Investigate public API, system settings, Shizuku. If all fail: UNSUPPORTED with explanation. |
| Shizuku not installed | App works without Shizuku. Degrade gracefully. |
| Glyph Matrix SDK availability | Already have AAR in Dvorinka/Nothing-GlyphMatrix. Verify license. |
| Low RAM during local Gradle build | Send all full builds to Proxmox. Local does code only. |

---

*This plan is the single source of truth for execution. Update it as phases complete and findings emerge. Do not deviate from the architecture principles in section 4 without documenting the reason.*

# RogueScape Cleanup & Plugin Hub Readiness Plan

_Date: 2026-06-14. Produced from a multi-agent audit of the committed source tree._

## TL;DR

RogueScape is functionally healthy — build + ~35 tests green, jar ~2.4 MB. But it carries
**two ~2.6k-LOC god files** (`RogueScapePlugin` 2739, `RogueScapePanel` 2554), a **3.8k-LOC UI
cluster** with heavy duplication, several **dead "future-stage" packages**, leftover **vendored
template scaffolding**, and a handful of **Plugin Hub launch blockers** (the worst: `developerMode`
defaults to `true`).

The single most important constraint: **no test instantiates `RogueScapePlugin`** — the engine,
libraries, view-models and panel layout are well covered, but the plugin's orchestration (event
handlers, `startRun`/`dispatchAction`, inventory-diff, enforcement) is unpinned. So the high-risk
plugin decomposition must be preceded by a characterization-test seam.

## Decisions taken (2026-06-14)

These govern the workstreams below where they differ from the original menu of options:

1. **Sequence:** execute the full plan W1→W10 in order, across increments, on branch `cleanup/hub-readiness`.
2. **Future-stage packages → wire up (not delete).** W2 becomes feature work: unify scoring on
   `ScoringRules`, route seeded races through `SeededRunGenerator`, build `RunRecap` at run-end and
   consolidate the ad-hoc recap paths, wire the local leaderboard, route live events through the
   adapter pipeline, and standardize on `DeterministicRng`.
3. **Native widget windows → keep + refactor.** The W1 "gate off" step is dropped; instead the
   always-on custom-builder window now has a real `inGameWindowsEnabled` toggle (default true), and
   W4 regains the deferred native-window refactors. (The `RogueScapeWidgetWindow` SPIKE stays behind
   `experimentalJournalTab` for now because it re-renders the same content as the overlay window —
   enabling it would double-render; W4 decides its fate.)

### Progress

- **W1 — DONE.** `developerMode` default → false; added `inGameWindowsEnabled` (default true) and
  wired the custom-builder window to it; removed dead `goal`/`weekly` mode branches + stale hint;
  added `LICENSE` (BSD 2-Clause), `README.md`, and a placeholder 48×72 `icon.png`; set
  `support=` to the repo issues URL. Suite green (283 tests).
- **W2 — DONE (down-payments).** A 5-agent seam map found that the full live wiring touches the
  untested plugin orchestration, so W2 landed only the core-contained, tested pieces:
  - **RNG determinism:** `RewardDrafter.shuffle` promoted to the shared deterministic Fisher-Yates;
    `RelicDraftGenerator` + `SeededRunGenerator` now use `DeterministicRng` (no more `java.util.Random`).
  - **Scoring unified:** `ScoringPreset.forMode` (UNSPECIFIED→BALANCED, SEEDED_RACE→SPEEDRUN,
    CUSTOM_CREATOR→CREATOR_CHAOS); `RogueScapeRun.effectiveScore()` delegates to `ScoringRules`
    (cleared rooms/bosses now score) with a timed overload for the SPEEDRUN bonus (W6 supplies time).
    New `RogueScapeRunScoringTest`. **Note:** displayed scores now include room/boss bonuses.
  - **Recap correctness:** `RunRecap.snapshot` score now matches the live `effectiveScore`.
  - **Seeded races:** `SEEDED_RACE` is now selectable from the Mode dropdown (was UI-unreachable);
    it already produced deterministic routes (`java.util.Random` is spec-stable), so no risky
    re-route through `SeededRunGenerator` was needed.
  - **Re-scope vs original W2:** routing the live path through `SeededRunGenerator`/`ChallengeCodec`
    was dropped — it would risk the balanced-room-kind and region-rule contracts for no determinism
    gain. **Moved to W6/W7** (need the test seam): leaderboard run-end recording + history UI,
    re-pointing the live recap renderers at `RunRecap`, the SPEEDRUN time bonus, and the adapter
    event pipeline. Suite green (288 tests).
- **W3 — DONE.** Removed the vendored PluginIdeaHub framework: re-homed the HUD overlay as
  `RogueScapeSummaryOverlay` (literal "RogueScape" title + the plugin's own `overlayLines`); dropped
  the `uiModel` field and the vestigial `recordManualAction` call; deleted `RogueScapePrototype`,
  `com.pluginideahub.prototype.*` (4 classes), and `com.pluginideahub.ui.PluginIdeaHub*` (2 classes).
  Retargeted the test: removed the 3 prototype-scaffolding tests and renamed the file to
  `RogueScapeRunSessionTest` (it always mostly tested the real engine). The plugin is now
  self-contained under `com.pluginideahub.roguescape`. Suite green (285 tests).
- **W5 — DONE.** Extracted three self-contained collaborators out of `RogueScapePlugin`:
  `bridge.ShortestPathBridge` (the reflective Shortest Path integration), `ui.RoomTargetMapMarker`
  (the world-map marker + icon), and `ui.RewardPresenter` (stateless reward/relic display mapping +
  the `ITEM_*` icon constants). Plugin keeps thin delegations. `RogueScapePlugin` is down from
  **2739 → ~2230 lines**. Added `RewardPresenterTest`. Suite green (288 tests).

## Plugin Hub size limit (the research question)

**You are well within limits — size is not a workstream, just a one-line readiness check.**

| Limit | Value | This repo |
|---|---|---|
| Plugin **jar** | **10 MiB** (binary; hard build-fail; warns at ~8 MiB) | ~2.4 MB ✅ |
| **Source** archive | 10 MiB (files that would overflow are silently skipped) | ~3.3 MB ✅ |
| `icon.png` | ≤ **256 KiB** and **48×72 px** (hard build-fail) | not present yet ⚠️ |

- The 10 MiB jar cap is enforced by the Hub packager (`net.runelite.pluginhub.packager.Plugin`),
  not documented as a number in the README. It can be raised per-plugin via a `jarSizeLimitMiB=`
  key in the commit descriptor, but only with maintainer agreement — don't rely on it.
- The 2.2 MB `ui-sheet.png` is the dominant payload but bundles fine. `gradle-wrapper.jar` lives at
  repo root, not in `src/main/resources`, so it is **not** in the plugin jar.

**Other Hub constraints worth knowing (manual review + automated API ban):**
- No `new okhttp3.OkHttpClient()` / `new Gson()` — must `@Inject` the client's shared instances.
- Any non-runelite-client dependency needs SHA-256 dependency verification + manual maintainer sign-off; avoid new deps.
- Banned APIs checked against bytecode: `WidgetInfo`/`WidgetID` (use `ComponentID`/`InterfaceID`), `Client.getVar`, the whole `account`/`SessionManager` surface, etc.
- Reflection isn't banned, but the review is manual — the Shortest Path reflection bridge is allowed yet should stay isolated/auditable.

Sources: [plugin-hub-tooling Plugin.java](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java), [disallowed-apis.txt](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/resources/net/runelite/pluginhub/packager/disallowed-apis.txt), [plugin-hub README](https://github.com/runelite/plugin-hub/blob/master/README.md).

## The "1 big file" problem (not yet fixed)

| File | LOC | Shape |
|---|---:|---|
| `RogueScapePlugin.java` | 2739 | God class doing ~17 jobs: wiring, a 9-handler event hub, per-tick loop, menu enforcement, inventory-provenance, chat interpretation, run lifecycle, custom-zone persistence, a reflective Shortest Path bridge, world-map marker, ~1100 LOC of overlay/window/custom-builder view construction, reward presentation, journal diagnostics |
| `RogueScapePanel.java` | 2554 | 4 concerns: Swing widget toolkit, static presentation maps, read-only active-run renderer, and **~half the file is Swing-free run-builder state** that doesn't belong in a Swing class |
| UI cluster (4 files) | 3848 | Two parallel UI stacks (Graphics2D overlays + native widget injection) rendering largely the same model twice, with verbatim-duplicated draw/mouse/injection code |

Root coupling problem in the plugin: shared mutable state (`rogueRun`, `runSession`, `runLoop`,
`currentRegionId`, provenance signals, shortest-path status) read everywhere. The highest-leverage
move is a read-only **`RunContext`** seam so collaborators can read run state without referencing
the plugin — every risky extraction depends on it.

## The plan — 10 workstreams, 3 tracks

Two tracks can run in parallel; the god-file decomposition is strictly ordered by the test-coverage gap.

### Track 1 — Launch readiness (no structural risk, ship in days)

**W1. Config + metadata + gating** _(S, low)_ — the actual launch blockers:
- Flip `RogueScapeConfig.java:165` `developerMode()` `true → false` (**P0**: DEV TOOLS exposed to all users).
- Gate the always-on custom-builder window: `RogueScapePlugin.java:213` `() -> true → config::experimentalJournalTab` (its SPIKE sibling is already gated).
- Add `LICENSE` (BSD 2-Clause), `README.md`, `icon.png` (48×72, <256 KiB) at repo root; set `support=` in `runelite-plugin.properties`.
- Hide parked `Goal`/`Weekly` modes (they silently fall back to Scavenger).

**W2. Dead-code + parked-feature cleanup (deletion only)** _(M, low)_:
- Delete unused `placeholderTab()` (`RogueScapePlugin.java:2241-2248`).
- Delete the four unwired "future-stage" subpackages with no `src/main` consumer + their tests:
  `ScoringRules`/`ScoringPreset`, `core.seed`, `core.race`, `core.recap`, and the adapter event pipeline
  (`AdapterTranslator`/`ObservedEvent`/`ObservedEventKind`/`RegionTracker`). Keep `InventoryDiff` + `ProvenanceSignalTracker`.
  _(Alternative: move to a non-shipped sourceset as a roadmap instead of deleting.)_

**W3. Remove vendored PluginIdeaHub framework** _(M, low)_:
- Re-home the 47-line `PluginIdeaHubOverlay` as a small `roguescape.ui` overlay; drop the `uiModel` field and the `recordManualAction` call; delete `RogueScapePrototype`, `com.pluginideahub.prototype.*`, `com.pluginideahub.ui.PluginIdeaHub*`.
- Retarget `RogueScapePrototypeTest` at `RogueScapeRunSession` directly.

### Track 2 — UI cluster (orthogonal, anytime, guarded by existing render tests)

**W4. Extract stateless/shared UI helpers** _(M, low)_:
- `OverlayDraw` (drawDiamond + 3 word-wrap copies + one `darken`), `NativeWidgetCanvas` (byte-identical widget primitives), `WindowGeometry` (duplicated viewport injection plumbing), `RewardCardRenderer`/`RewardGlyphs`.
- **Defer** MouseSupport bases / Block-model promotion / CustomBuilderLayoutKit / PreviewPainter split — those invest in the experimental native windows that should be gated off (W1).

### Track 3 — God-file decomposition (strictly ordered)

**Plugin ladder:**
- **W5. Wave A — self-contained, low-risk** _(M, low)_: `ShortestPathBridge`, `RoomTargetMapMarker`, `RewardPresenter`. Nearly pure; add focused unit tests.
- **W6. RunContext seam + plugin characterization tests** _(M, medium)_ — **the linchpin**: introduce the read-only `RunContext`; add the first-ever tests pinning inventory-diff, enforcement gating, chat interpretation, and `startRun`/`reset`/`dispatch` transitions.
- **W7. Wave B — pure-logic services** _(L, medium)_: `OverlayTextModel`, `InventoryProvenanceTracker`, `MenuEnforcementController`, `ChatEventInterpreter`, `CustomRoomZoneService`. Plugin `@Subscribe` handlers shrink to delegations.
- **W10. Wave C — keystone** _(XL, high)_: `InGameWindowTabFactory`, `CustomBuilderController`, and finally `RunController` (owns the `runSession`/`rogueRun`/`runLoop` triple + lifecycle). Plugin reduces to wiring + thin handlers.

**Panel ladder (guarded by `RogueScapePanelLayoutTest` — preserve `render()`/`selectedMode()`/`selectedPreset()` + section order):**
- **W8. Wave A — leaf utilities + self-contained sections** _(M, low)_: `PanelWidgetFactory`, `PanelActionPresenter`, `RunSeedCodec` (unit-test round-trip), `ZoneBuilderSection`, `RelicCatalogSection`.
- **W9. Wave B — extract `CustomRunSpec`, then `RunBuilderSection`** _(L, high)_: lift the ~120 Swing-free getters/mutators into a `core.CustomRunSpec` model (the canonical run-builder store, read/written by both plugin and custom-builder window), re-point both callers, then collapse `RunBuilderSection` to layout + listeners.

### Sequencing & shippability

```
Track 1:  W1 → W2 → W3            (launch gate; W1 is a tiny standalone PR)
Track 2:  W4                      (anytime, parallel)
Plugin:   W5 → W6 → W7 → W10
Panel:    W8 → W9
```

W9 and W10 both touch `RogueScapePlugin` run construction — land `CustomRunSpec` (W9) first, rebase
`RunController` (W10) on top. Every workstream ends with `./gradlew test --offline`; W9/W10 also get a
manual end-to-end run in the test client. **Each workstream is independently shippable** — stop after
any one and the plugin still builds, tests pass, behavior unchanged.

## Key risks / decisions

1. **Coverage gap is real** — the plugin orchestration is untested, so W6 (RunContext + characterization tests) is **mandatory before W7/W10**. Skipping it makes the keystone extractions blind.
2. **Delete vs keep the future-stage packages** (scoring/seed/race/recap/adapter pipeline) — they pass tests but ship as dead weight implying features the plugin lacks. Recommendation: delete (or move to a non-shipped sourceset). Wiring them up is *feature work*, not cleanup.
3. **Experimental native widget windows** — `RogueScapeWidgetWindow` is a literal SPIKE; the custom-builder window is wired always-on. Gate both off for launch. If gated off, **don't** refactor them (W4's deferred items, W10's `CustomBuilderController`) — wasted motion.
4. **Deferred as out-of-scope:** run-stack rename (`RunSession`/`Run`/`RunLoop` churn), the `SidePanelViewModel` string-prefix protocol typing, RNG-determinism fixes (moot if seed/race are deleted).

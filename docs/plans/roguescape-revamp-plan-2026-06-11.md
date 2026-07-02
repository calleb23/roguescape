# RogueScape Revamp Plan — Full Sweep Audit (2026-06-11)

> **Update (same day): Codex base adopted.** After this plan was written, a newer
> externally-developed RogueScape tree (`github.com/calleb23/roguescape`, a strict evolution of
> this repo's plugin) was audited head-to-head and **imported as the new base** of
> `plugins/roguescape`. It adds `core/campaign` (8 campaigns), `core/task`, `core/unlock`,
> a custom-builder widget window, and grows the test suite to 250 green core tests. The Phase A
> determinism fixes were re-applied on top (plus one newly found case:
> `seed/SeededRunGenerator` shuffled relics via `java.util.Random`), `RelicEngine` now uses
> `LinkedHashMap`, and `developerMode` defaults to `false`. See
> `docs/audits/codex-roguescape-audit-2026-05-31.md` and
> `docs/roguescape/roguescape-gameplay-function-checklist.md` for the imported tree's own docs.
>
> Phase consequences: line counts below refer to the pre-import tree (`RogueScapePlugin` is now
> 2,739 lines; `RogueScapePanel` 2,554; `SidePanelViewModel` 561 and flagged as the worst
> god class in core). The Phase D "delete the widget spikes" decision is **softened**: the new
> audit found the widget windows properly gated and organized — the open P1s are now
> (1) a config toggle for the always-on `RogueScapeCustomBuilderWidgetWindow`,
> (2) menu-enforcement blocking made opt-in / warning-first by default,
> (3) decoupling the plugin from the shared `com.pluginideahub` framework classes so it can
> ship standalone. New test gaps to close: `RoomTask`, `RunUnlockGenerator`, and an
> end-to-end seeded-pipeline determinism test.

A full audit of `plugins/roguescape` (96 main classes / ~12.7k LOC, 35 test classes / ~3.8k LOC)
covering the pure core, the RuneLite adapter/UI layer, and roadmap alignment.

## Verdict: revamp, do NOT start again

- **The pure core (`core/`) is genuinely good**: clean layering (zero RuneLite imports,
  verified), composition over inheritance, immutable value objects, builder patterns, real
  content (20 relics + 20 modifiers, 30 rooms, 20 bosses), and 196 meaningful unit tests —
  all green. Rewriting it would throw away the best part of the codebase.
- **The RuneLite adapter + UI layer is the weak half** and is where the revamp effort goes:
  a 1,319-line god-class plugin, four competing UI surfaces, an experimental widget-window
  spike that is a Plugin Hub rejection risk, and run-state leaking into persisted config.

## What was fixed in this pass (Phase A — done)

Determinism alignment (roadmap Stage 10 hygiene). Three call sites bypassed the project's own
`DeterministicRng` convention, so shared seeds were not guaranteed reproducible cross-JVM:

- `RunRouteBuilder.buildRoute` — used `java.util.Random` + `String.hashCode` for seeded routes.
- `reward/RelicDraftGenerator.relicDraft` — used `Collections.shuffle(pool, new Random(seed))`.
- `RogueScapeRunLoop.draftSeed` — derived per-stage draft seeds from `String.hashCode`.

All now use `DeterministicRng` (new `shuffle()` method; `RewardDrafter.shuffle` delegates to it).
New `DeterministicRngTest` pins the hash and shuffle outputs so the seed contract cannot drift
silently. All 196 core tests pass.

> Note: changing the RNG changes which routes/relics a given seed text produces. Fine pre-release;
> after release, seed outputs must be treated as frozen.

## Environment constraint (affects how the rest gets done)

The Claude Code remote environment's network policy blocks `repo.runelite.net`
(`x-deny-reason: host_not_allowed`), so `./gradlew test` cannot resolve `net.runelite:client`
and **nothing that imports RuneLite can be compiled or tested in remote sessions**. Core-only
work was verified with a standalone javac+JUnit harness.

Two remedies, in preference order:

1. **Allow `repo.runelite.net` in the environment's network policy** (Claude Code on the web →
   environment settings). Unblocks full Gradle builds remotely.
2. **Phase B below** (core/adapter Gradle split) makes the core testable without RuneLite even
   in restricted environments, and is worth doing regardless.

Until one of these is in place, adapter/UI refactors should be done (or at least compiled)
locally.

## Audit findings → phased plan

### Phase B — Build split: make the core a RuneLite-free Gradle module

Today one source set compiles everything against the RuneLite jar, so the pure core can't be
built/tested without `repo.runelite.net`.

- Split into `:core` (plain Java, JUnit only) and the root RuneLite plugin module depending on it,
  or at minimum add a `coreTest` source set + task that compiles `core/**` without the RuneLite
  dependency.
- Remove the core's one impure edge: `core/RogueScapePrototype` depends on the legacy shared
  `com.pluginideahub.prototype` framework and is only used for the rule-card deck at plugin
  startup. Either drop it (preferred — it's ideation-era scaffolding) or move it out of `core/`.

### Phase C — Break up the god class (`RogueScapePlugin`, 1,319 lines, ~10 responsibilities)

Keep the plugin class as wiring only; extract:

- **EventAdapter** — all `@Subscribe` handlers, delegating to core (`onGameTick` at ~303–360
  currently does inventory diffing + region tracking + enforcement + loop ticking in one method).
- **MenuEnforcer** — menu filtering/blocking (currently mixed with custom-room menu injection
  in `onMenuEntryAdded`, ~429–512). The existing menu manipulation itself is Hub-compliant
  (RUNELITE-type entries, filter-based blocking) — keep that approach.
- **UICoordinator** — overlay/panel/window lifecycle + refresh.
- **Move `windowTabs()` (~965–1025) into core view models** (`core/ui`) — it hardcodes the whole
  tab/block layout inside the adapter; `SidePanelViewModel`/`OverlayViewModel` is where testable
  UI state belongs.

### Phase D — Consolidate UI surfaces (currently four, partially duplicated)

| Surface | Status | Decision |
|---|---|---|
| Swing side panel (`RogueScapePanel`, 1,188 lines) | Active | **Keep** (control surface) |
| `RogueScapeWindowOverlay` (Graphics2D window) | Active | **Keep** (sole in-game window) |
| `RogueScapeWidgetWindow` (738-line widget-API spike) | Experimental flag | **Delete** — duplicate of WindowOverlay and the main Hub-rejection risk (builds a full custom UI from 20+ dynamic child widgets) |
| Journal tab injection (`RogueScapeJournalTabAdapter`, `RogueScapeJournalWidgetProbe`) | Spike/diagnostic | **Remove or strictly dev-flag**; probe's dump methods are dev-only and shouldn't ship |

The reward overlay and room-mask/world-map overlays stay (Graphics2D, safe).

### Phase E — Hub-safety & hygiene pass

- **Config orphan state**: `useCustomRoomForCurrentRun` persists via ConfigManager and survives
  restarts after the in-memory run is gone — reset it on run end / shutdown, and on startup if no
  run exists. Keep run state in memory only.
- **Threading**: clarify `RogueScapeCustomRoomEditorState.onChange` callback origin; remove the
  redundant nested `SwingUtilities.invokeLater` (`RogueScapePanel:144`). Follow the
  `clientThread.invoke()` pattern already used correctly in mouse handlers.
- **Resource cleanup**: clear `RogueScapeIcons` caches in `shutDown()`; make mouse-listener
  unregistration exception-safe.
- **Config organization**: add `@ConfigSection`s before the option list grows further.

### Phase F — Close the roadmap gameplay gaps (all stages have code; these are the real holes)

1. **Hard enforcement wiring** — `RogueScapeEnforcementRules` + `MenuEnforcementEvaluator` exist
   and are tested in core but are not fully merged into the live menu flow (current behavior is
   mostly warnings). This is the corrected product direction (see
   `docs/rules/safety-boundaries.md`) and the highest-value gameplay item.
2. **Max-main bank-draft fairness** (roadmap §9) — `BankDraftPool`/`BankItemClassifier` exist but
   the documented fairness rules (value tiers, category balancing, minimum eligible counts,
   progressive unlocks) are not encoded. Naive random-from-bank must not ship as final.
3. **Expose strictness/provenance choices in config/UI** — `StrictnessMode` and
   `ProvenanceHint.UNKNOWN` handling exist in core but aren't user-facing decisions yet
   (roadmap §18 open questions).
4. **Core polish** (small): migrate the legacy `Reward`/`Relic`/`Violation` inner classes out of
   `RogueScapeRunSession`; `HashMap` → `LinkedHashMap` in `RelicEngine` for codebase consistency;
   document relic restriction precedence (category beats item-id in `RelicEngine.adjustLegality`).

### Phase G — Later (unchanged from roadmap)

- Companion web app (Stage 12) — schemas are ready; stays deferred until enforcement + fairness
  land.
- Integrity/replay backend (client-attests/server-judges) — design locked, not started.
- Scoring weight tuning — needs playtesting, not code archaeology.

## Suggested order of work

1. ~~Phase A — determinism fixes~~ (done this pass)
2. Phase B (build split) — small, unblocks everything else with tests.
3. Phase D delete-the-spikes (WidgetWindow, journal injection) — pure deletion, shrinks the
   plugin class before refactoring it.
4. Phase C god-class split + windowTabs→core.
5. Phase E hygiene items.
6. Phase F gameplay gaps (enforcement first), each as its own roadmap-stage-named slice.

# RogueScape — Build Direction & Architecture

The main accepted plugin direction. **"RuneScape, but every run is a roguelike."**
A Plugin Hub-safe OSRS roguelike run builder: rooms, boss stages, reward chests, action
enforcement, relics/modifiers, recaps, seeds, race/high-score support, and an eventual companion
web app.

> Do not treat the plugin as a small inventory tracker.

## Lineage

RogueScape began as the **One Inventory Roguelike** prototype. The implementation was renamed
to RogueScape (folder `plugins/roguescape`, package `com.pluginideahub.roguescape`). Historical
research docs and some package notes may still reference the old `oneinventory` name; do not mix
broad feature work with a package rename unless the rename is the explicit task.

## North-star and background docs

- North-star roadmap: `research/one_inventory_roguelike_end_state_roadmap.md`
- Older development pass / idea background: `research/one_inventory_roguelike_development_pass.md`
- Existing/similar plugin recon: `research/one_inventory_existing_plugin_recon.md`
- Enforcement/content/cosmetics plan: `research/roguescape_enforcement_content_cosmetics_plan.md`
- Passive wiring plan: `docs/plans/roguescape-passive-runelite-wiring-plan-2026-05-28.md`

## Architecture: layered split

The code follows the roadmap's "pure core, thin adapters" rule. Keep the core independent of
RuneLite.

### Layer 1 — Pure Java core (`core/`, no RuneLite imports, unit-tested)

- `RogueScapeRunSession` — Stage 1 engine: run identity, route, timeline, score, recap.
- `RogueScapeRun` — Stages 2/3 orchestrator. **Composes** the Stage-1 session (composition,
  not inheritance, to protect the stable Stage-1 test surface) and adds region policy + run
  constraints. Items are **not** classified as legal/illegal — disallowed actions are blocked at
  the menu (see `enforcement/`); observed items simply count toward objectives and score.
- `item/` — `ItemDelta`, `ProvenanceHint`, `InventorySnapshot`, starter kit. Neutral item/inventory
  carriers; provenance hints distinguish objective types (combat drop vs. shop vs. gathered).
- `region/` — room & boss libraries, region rules, custom-room selection, room-mask rules.
- `reward/` — chest types, reward drafter, bank-draft pool, item classifier, fairness policy,
  deterministic RNG.
- `relic/` — relic engine, relic & modifier libraries, effects.
- `seed/` — seeded run generator, challenge codec/definition.
- `recap/` — recap model, run history, export.
- `race/` — leaderboard, weekly events.
- `adapter/` — the seam: `AdapterTranslator`, `ObservedEvent`, `ProvenanceSignalTracker`,
  `RegionTracker` (converts RuneLite signals into core `ItemDelta`s).
- `enforcement/` — `MenuEnforcementEvaluator`/`Decision` (hard-enforcement: block bank/trade/GE
  menu entries).
- `ui/` — pure view models (`SidePanelViewModel`, `OverlayViewModel`) — testable UI state.

### Layer 2 — RuneLite adapter

`RogueScapePlugin` (event wiring, menu enforcement, journal injection), `RogueScapeConfig`.

### Layer 3 — UI

`RogueScapePanel` (themed Swing side panel) and `ui/` overlay/Swing classes
(`RogueScapeTheme`, `CollapsibleSection`, `StatBar`, room-mask & world-map overlays, journal
adapters).

## Roadmap discipline

Every coding task must name the roadmap stage it implements (see roadmap §16, Stages 0–12).
Do not pull in later stages silently. Use the prompt format from roadmap §20:

```text
Implement Stage N: <name> from research/one_inventory_roguelike_end_state_roadmap.md.
Scope: <specific slice>.
Non-goals: <later-stage features to avoid>.
Verification: focused tests + full Gradle if code changes.
```

## Max-main bank-draft caution

The max-main mode is important but must be balanced. Do **not** implement naive "random item
from bank" as final behavior. The roadmap (§9) calls out fairness/economy design: minimum
eligible item counts, value-tier chests, buy-in budgets, category balancing, progressive unlock
tiers, and creator restrictions.

## Companion web app caution

A companion web app is a **late-stage** target (`web/roguescape` is a stub). Early plugin code
should prepare only by keeping challenge/run/recap JSON schemas clean and stable. Do not build
the web app before the plugin core/export foundations are stable.

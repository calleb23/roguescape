# PROGRESS — RogueScape handover

**Session 2026-07-02** — Consolidated everything into THIS repo as the one canonical version, built
the Boss Ladder MVP core (chunks 1–7), made every window phase an open-book journal spread, and
rebuilt the Contract page to Caleb's start-ui sketch (route browsing + live curse picking).

## Commits this session (oldest → newest)

```
9a8466f Import the current RogueScape from PluginIdeaHub — this repo is now canonical
232edff Every window phase is an open-book two-page spread (book mode 680x430)
9c6e056 MVP chunks 1+2: curse catalog + relics as restriction-removers
203d669 MVP chunks 3-5: BossLadderRun state machine, restriction reward drafts, gear tiers
3f22628 MVP chunks 6+7: LoadoutCheck fight gate + enforcement derives from RunRestrictions
f2ffa22 docs: lock collections taxonomy + journey-spread direction; ref/ sketches
124e106 docs: supplies = a Boss Ladder upgrade lane; found-only in Dungeon Crawl
914c9a5 docs: Boss Ladder is bosses only — prep ROOM cut, prep PHASE stays
3d1f978 Run page becomes the journey spread (per ref sketch)
bdbfb4d Journey spread v2 — Upgrades|Relics columns, boss portrait cards, bottom-anchored info
20e7c96 Purge lawful/legal/illegal/suspicious vocabulary (LegalRegion→AllowedRegion etc.)
2ec271f 3D boss chatheads in the widget window (WidgetModelType.NPC_CHATHEAD + BossLibrary npcIds)
b226bfd Route pinned to Begin; widget window learns the spread blocks
192f758 Boss Ladder route is bosses only in CODE (0 rooms / 3 bosses)
3c2a8d3 Solid window (no click-through), DEV_ENTER_ROOM/DEV_BOSS_KILL, Custom=coming soon
faf8c95 Route catalogue + Dungeon Crawl rename + widget window actually opens
a2d5988 Contract page to sketch: seal rows, one-route browse, Prev/Next
835c5b8 Four room kinds (WEAPON/ARMOUR/SUPPLY/CRAFTING), curse picking w/ hand-drawn circles
3eaf5f9 Journal window front door: panel button + un-gated Collection Log right-click
dd5f57b /wrapRS skill
```

## Current state (honest)

- **Repo**: this is THE RogueScape (`calleb23/roguescape`, main). PluginIdeaHub's copy is deleted
  (pointer README). W1–W9 refactor history: branch `cleanup/hub-readiness`. **326 tests green.**
- **Works live (painted book window, RuneLite dev client via `run-client.bat`)**: Contract page
  (mode rows w/ wax seals, route browse No. x of 12 with smart names, curse picking with
  hand-drawn circles → curses ENFORCE from Begin via menu blocking), journey spread mid-run,
  reward + recap spreads, solid-to-clicks window, "Open the Journal" panel button, DEV TOOLS can
  step a full run (enter room / kill boss / complete / reward / next / fail).
- **Core-only (built + tested, NOT wired to a live mode)**: `core/ladder/` — BossLadderRun
  (PREP→FIGHT→REWARD), LadderRewardDrafter, GearTiers, LoadoutCheck. BANK_DRAFT mode still runs
  on the generic RunLoop; the ladder state machine is not driving it yet (that is MVP C9's rest).
- **Ugly/broken**: the experimental widget window (real widgets, 3D chatheads) looks terrible —
  Collection Log sprites, no paper skin. NPC ids: 14/20 filled, unverified in-client; 6 are 0.
  Custom mode = Coming Soon (old builder disconnected; internal "Scavenger"/"Rewarded" state
  strings survive in the custom-builder/seed codec — rename deferred deliberately).
- **UI-only route catalogue**: 12 fixed seeds per mode (`crawl-route-N` / `ladder-route-N`),
  smart names via `core/seed/RouteNames`.

## Decisions locked this session (all in docs/plans/roguescape-gameplay-design.md)

- **Scavenger renamed Dungeon Crawl**; "Rewarded" renamed **Boss Ladder** everywhere user-facing.
- **Boss Ladder = bosses only** (3): no prep room; the prep PHASE (bank/GE open, loadout gate)
  stays between bosses.
- **Collections taxonomy**: Upgrades = equipment (+ Supplies lane in Boss Ladder only; four lanes
  Weapon/Armour/Jewellery/Supplies); Relics = everything non-equipment (binary permits incl.
  slots, prayers, spellbook); Curses = setup burdens; supplies in DC = whatever you find. No
  slot caps. Old bank/prayer/potion "unlock" reward type is to be folded into relics (not yet
  done in code — see next steps).
- **Room kinds = exactly WEAPON / ARMOUR / SUPPLY / CRAFTING (+BOSS)**.
- **Route selection = full choice** (browse the whole catalogue), never a roll mechanic. Drawn
  map = a future route TYPE with random rooms (its own thing).
- **Terminology**: lawful/legal/illegal/suspicious permanently banned; permitted/forbidden/
  allowed/blocked only. **Motto: less is more** — cut wording everywhere.
- The painted overlay stays primary until the widget window gets a proper sprite skin.

## In-flight / half-done

- **Per-category upgrade lanes** (Weapon/Armour/Jewellery/Supplies): `RunRestrictions` still has
  ONE `gearTierCap`; `RelicLibrary.armouryKey()` + `deepPockets()` are still relics but the
  taxonomy says tier raises = upgrades, slots = relic (Deep Pockets stays). Rework pending.
- **Boss images in the painted band**: mechanism reads
  `src/main/resources/com/pluginideahub/roguescape/ui/boss/<slug>.png` — no assets bundled yet
  (inked-initial fallback shows).
- **Supplies-lane grading scale** (consumables have no equip req) — mechanism undecided, flagged
  in the design doc.

## Next steps (agreed queue)

1. **MVP C9 remainder**: make BANK_DRAFT actually run on `core/ladder/BossLadderRun` — prep
   phase live (bank/GE allowance via `BossLadderRun.decide`), `LoadoutCheck` invoked at
   PREP→FIGHT, `BossKillChatMatcher` → `recordBossKill()`, ladder book spread (prep vs fight).
2. **Upgrade lanes rework** (see in-flight) + retire the UNLOCK reward type into relics/upgrades.
3. **Widget-book skinning pass**: bundle paper/seal sprites so the real-widget window looks like
   the journal — then it becomes primary (it alone can render 3D models). Verify/fix boss NPC ids
   in-client while there.
4. Start-tier picker on the Contract (StartTier exists in core, no UI).
5. Custom mode rebuild around restrictions (currently Coming Soon).

## Open questions for Caleb

- Ladder length (currently 3 bosses) and which bosses per catalogue route — curation.
- Supplies-lane grading scale (food/potion tiers by what measure?).
- Boss portrait art source for the painted band (bundle wiki renders? licensing call).

## Gotchas

- The **painted overlay cannot render 3D models** — only the widget window can
  (`WidgetType.MODEL` + `WidgetModelType.NPC_CHATHEAD`). Flag "Experimental Quest-tab UI" shows it.
- Render previews: `./gradlew test` writes PNGs to `build/ui-preview/` (window-contract-book,
  window-run-book, window-reward-book, window-recap-book) — eyeball UI without the client.
- `ref/*.kra` = Caleb's Krita sketches (THE UI contract) — unzip `mergedimage.png` to view.
- Dev client: `run-client.bat` (Windows). Wikisync port-bind errors in the log are harmless.
- The journey spread's left page = Upgrades|Relics nested COLUMNS + FILL-pinned curse strip;
  FILL bottom-anchoring uses `estimateHeight` in `RogueScapeWindowOverlay` — new block kinds
  MUST be added to that estimator or FILL misplaces content.
- Commit + push after every chunk — uncommitted work is how this project historically lost days.

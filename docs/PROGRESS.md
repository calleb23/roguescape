# PROGRESS — RogueScape handover

**Session 2026-07-03** — Widget window skinned as the journal (verified in-client), boss NPC ids
all filled+verified, the LOBBY sculptor spike (clear an area, rebuild with cache models), and TWO
grill sessions that locked the whole content catalog + mode×mechanic matrix — then implemented
them in core with the laws as executable tests.

## Commits this session (oldest → newest)

```
8e4d2f6 Widget window wears the journal skin (runtime-generated sprite overrides)
38cbc89 Review fixes: page-bottom guards in COLUMNS, unconditional skin unregister
473ee19 Masthead + tile polish from first in-client screenshot
e3e3055 Room sculptor spike — clear an area and build back with cache models
5f4a7c4 Rename: the sculpted space is the LOBBY, rooms belong to routes
00925c2 docs: lock the content catalog (lanes, bands, coverage law, 12 curses, mixed drafts)
c126ca0 The content catalog in code
eed1724 Review fixes: drafted permits actually permit, no-filler chests, stuck-state guards
b75b3be docs: lock the mode x mechanic matrix
59a20db The mode matrix in code (DC shackles, curse beats prep, offering rule, recap facts)
```

## Current state (honest)

- **342 tests green.** Design laws live as tests: `CatalogCoverageTest` (12 curses, every curse
  easeable, total freedom reachable, exact band ladder), `ModeMatrixTest` (DC shackle set,
  offering rule, curse-beats-prep).
- **Widget window** (`ui/RogueScapeWidgetWindow` + `ui/RogueScapeWidgetSkin`): 680x430 journal
  spread, skin generated at runtime from RogueScapePaper into sprite overrides (negative ids,
  registered at startUp / unregistered at shutDown). Verified in-client by Caleb ("hell of a lot
  better"); masthead/tabs/tile-wrap polish landed after his screenshot, NOT yet re-verified
  in-client. All 20 boss chathead NPC ids filled, verified against gameval.NpcID.
- **Lobby sculptor** (`RogueScapeLobbySculptor`, DEV TOOLS buttons): clear area r=3 / blank
  floors / place model (config: sculptModelId, sculptOrientation) / restore. Edit plan keyed by
  WorldPoint, re-applies after scene reloads. NO renderer hook needed (RuneLiteObjects render
  under GPU plugin). **Untested in-client** — key question: do tile removals show under the GPU
  plugin without an extra reload (requestSceneReload exists as the fallback).
- **Core catalog** (all pure-Java, tested): 4 upgrade lanes on one level scale (equip req /
  creation req), GradeBands 1-70, per-lane caps on RunRestrictions, StartTier caps all lanes;
  18 permit-only relics (full coverage law); 12 curses; LadderRewardDrafter = pure mixed pool of
  LadderRewardCards; UNLOCK reward type deleted; legacy scoring relics quarantined in
  LegacyRelics; ModeShackles.dungeonCrawl() = the 9-restriction DC baseline; curse beats prep in
  BossLadderRun; Curse.offerable() = the offering rule; RunRecap carries mode + curse rows.
- **Still not wired live**: BANK_DRAFT mode still runs the generic RunLoop, not
  core/ladder/BossLadderRun (MVP C9 remainder). Custom mode = Coming Soon.
- **Ugly/known**: Jewellery/Supplies lane caps don't bind at the gate until the ADAPTER supplies
  creation levels (LoadoutCheck.Item javadoc documents the semantics). One Style curse defaults
  to melee (Contract style picker pending). Widget window pockets/cards not pixel-perfect vs
  painted book.

## Decisions locked this session (all in docs/plans/roguescape-gameplay-design.md)

- **Terminology: routes have ROOMS; the sculpted physical space is the LOBBY.**
- **Catalog** (grill 1): one universal level scale for lanes (equip req; creation req for
  jewellery/supplies); fixed bands 1→5→10→20→30→40→50→60→70; StartTier = same band all lanes;
  relic law = every restriction has an easer, permits only, Armoury Key retired; 12 curses;
  chests = pure mixed pool, honest shrinking, no filler.
- **Mode matrix** (grill 2): NO lanes/StartTier in Dungeon Crawl; DC standard shackle set
  (bank/GE/trade, prayer + 3 high prayers, book bound Standard, potions; FOOD FREE); curse
  offering rule (only if it adds something — hides FIVE in DC incl. Dry Throat; the rule is the
  law, not the list); curse beats the BL prep allowance; mid-run curses = v2; scoring =
  pluggable scoreboards over recap facts (points/time/first-to-beat); Custom = chassis + free
  shackles + route, home of GRINDS mode and CA mode (player declares goal, plugin generates the
  route, editable before start).

## In-flight / half-done

- **Lobby sculptor**: committed but never run in-client. The GPU-reload question decides whether
  clearArea needs to chain requestSceneReload().
- **Legacy unlock flags**: RogueScapeRun.currentRestrictions() still honours stage-clear
  auto-unlocks as easers (RunUnlockGenerator.forClearedStage) — retire when Scavenger reworks.
- **Widget window polish**: latest masthead/tab/tile fixes (473ee19) await an in-client look.

## Next steps (agreed queue)

1. **In-client verification pass** (needs Caleb at home): widget window after 473ee19; lobby
   sculptor buttons (esp. with GPU plugin on — report whether cleared tiles vanish without a
   region hop); boss chatheads.
2. **MVP C9 remainder**: BANK_DRAFT runs on core/ladder/BossLadderRun — construct with the
   chosen curses (curse-beats-prep needs them), prep allowance live via decide(), LoadoutCheck
   at PREP→FIGHT, BossKillChatMatcher → recordBossKill(), draft UI uses LadderRewardCard
   (raises + relics), ladder book spread (prep vs fight).
3. **Contract UI catch-up**: StartTier picker; One Style's style picker; curse list from
   Curse.offerable(baseline) instead of a static list.
4. **Adapter supplies creation levels** for jewellery/supplies so those lane caps bind
   (LoadoutCheck.Item, BankItemClassifier territory).
5. **Lobby next step once spike verified**: blueprint format (tile grid + model list) so a
   cleared area rebuilds as a designed lobby — this is where lobby meets Custom.

## Open questions for Caleb

- Lobby: which physical spot becomes THE lobby location, and what should the first designed
  lobby contain?
- Boss curation per catalogue route (still open from last session).
- Grinds/CA modes: what does "declaring a grind goal" look like concretely (skill+target? item
  count?) — needed before the route-generator API is sketched.

## Gotchas

- **Sculptor vocabulary**: lobby ≠ room. `RogueScapeLobbySculptor`, `DEV_LOBBY_*`.
- **RuneLite widget setOpacity is INVERTED**: 0 = opaque, 255 = invisible.
- The widget skin is generated at runtime (RogueScapeWidgetSkin) — no bundled art; preview PNG:
  `./gradlew test` → build/ui-preview/widget-skin-book-bg.png (+ the window-*-book previews).
- The painted overlay cannot render 3D models — only the widget window can (NPC_CHATHEAD).
- `RunRestrictions` is rebuilt per call in the run-loop world — easing MUST be applied after
  curses inside currentRestrictions() (it is; don't "optimize" it away).
- BossLadderRun must be constructed WITH the curse set or curse-beats-prep silently degrades to
  prep-beats-curse (the 2-arg constructor passes null).
- ref/*.kra = Krita sketches (THE UI contract) — unzip mergedimage.png to view. No new sketches
  this session.
- Dev client: run-client.bat; wikisync port-bind errors are harmless.
- Commit + push after every chunk — the lobby spike was lost once to an uncommitted session.

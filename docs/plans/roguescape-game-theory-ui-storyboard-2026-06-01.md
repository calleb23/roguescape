# RogueScape — Game Theory & UI Storyboard

Status: DESIGN / STORYBOARD (not a coding task). Companion to
`research/one_inventory_roguelike_end_state_roadmap.md`.

Purpose: lock down **the actual game theory** (what decisions the player makes and why)
and **how the three UI surfaces talk to the game and the plugin**, then walk a full run
frame-by-frame. Each frame ends with a copy-paste **image prompt** so you can generate a
visual board in ChatGPT (or any image model).

This document is grounded in the code that already exists in
`plugins/roguescape` — it does not invent new mechanics. Where something is a roadmap
target rather than built today, it is marked **[TARGET]**.

---

## Part A — The Mental Model: three surfaces, one engine

RogueScape is **not** a minigame that replaces OSRS. The player plays real OSRS; RogueScape
is a **referee + roguelike layer** sitting on top. Nothing is automated — the plugin never
clicks, routes, or fights. It does four things only:

1. **Gates progression** — you advance room→reward→room→boss through explicit actions.
2. **Judges legality** — every item you gain is classified legal / suspicious / illegal.
3. **Enforces rules** — it blocks bank/trade/GE menu entries and warns on region exit.
4. **Records the story** — timeline + score + recap for sharing.

Everything the player sees lives on **three surfaces**, all fed by one pure-Java engine
(`RogueScapeRun` / `RogueScapeRunLoop`):

```
                       ┌─────────────────────────────┐
                       │   PURE ENGINE (no RuneLite)  │
                       │  RogueScapeRun + RunLoop      │
                       │  phase • legality • score     │
                       └──────────────┬───────────────┘
                                      │ view models
        ┌─────────────────────────────┼─────────────────────────────┐
        ▼                             ▼                             ▼
 ┌───────────────┐          ┌──────────────────┐         ┌────────────────────┐
 │  SIDE PANEL    │          │   OVERLAY / HUD   │         │  IN-GAME ENFORCEMENT │
 │ (the cockpit)  │          │ (the live readout)│         │ (the hands of the    │
 │ SidePanelVM     │          │ OverlayViewModel  │         │  referee)            │
 │ tabs+buttons    │          │ goal/room/score   │         │ MenuEnforcement-     │
 │ drives state    │          │ warnings          │         │ Evaluator, region    │
 │ via PanelAction │          │ read-only         │         │ mask overlay         │
 └───────────────┘          └──────────────────┘         └────────────────────┘
        │                                                            │
        │ PanelAction (START_RUN, COMPLETE_STAGE, CHOOSE_REWARD_n…)  │ blocks/warns
        └───────────────► ENGINE ◄───────────────────────────────────┘
                            ▲
                            │ ObservedEvent (inventory delta, region change, death…)
                  RuneLite adapter (AdapterTranslator)
```

**Direction of data flow is the key to the whole design:**

- **Engine → UI** is one-way for display (panel rows, overlay fields).
- **Panel → Engine** is the *only* way the player drives the meta-state machine — via the
  `PanelAction` enum (the buttons).
- **Game world → Engine** is one-way observation — RuneLite events become `ObservedEvent`s,
  which the engine turns into legality verdicts. The game never directly mutates run state;
  it only *informs* it.
- **Engine → Game world** is enforcement only — the engine tells the menu evaluator which
  click-options to strip (bank/trade/GE) during restricted phases.

This separation is the safety story: the player's *gameplay* is never automated, only
*judged and gated*.

---

## Part B — The Game Theory (the decisions that make it a roguelike)

A roguelike is "interesting decisions under RNG and permanent consequence." RogueScape's
decisions, in order of how often the player faces them:

### 1. The Chest Draft — the core risk/reward beat
After every cleared stage the run enters `BASE_REWARD`. The drafter
(`RewardDrafter`) rolls **three options**; the player picks one or skips
(`CHOOSE_REWARD_1/2/3` / `SKIP_REWARD`). Chest categories that exist today:
`WEAPON, ARMOUR, SUPPLY, FOOD, POTION, AMMO, UTILITY, RELIC, MODIFIER, BANK_UNLOCK`.

**The decision:** do I take the power spike (weapon) now and risk being under-supplied at
the boss, or bank consumables to survive? You can only take one — the other two are gone.
This is the "build emerges from forced choices" loop.

### 2. Legality discipline — the always-on tension
Every item gained is judged by `LegalityClassifier` against the current room/region and
permission window. States range from `LEGAL_REGION_GAIN` to `SUSPICIOUS_UNKNOWN` to
`ILLEGAL_OUT_OF_REGION`. Under `STRICT` a suspicious ground item can *end the run*; under
`BALANCED` it pauses for approval; under `TRUST` it only logs.

**The decision:** that ground item might be free loot — or another player's drop that
contaminates the run. Grab it or leave it? This is the "one mistake and the run dies"
fantasy that makes clean runs feel earned.

### 3. Relic drafting — run personality vs. safety
Relics (`RelicEffectKind`: `ONE_SHOT_MERCY, CATEGORY_LIMIT, SCORING_BIAS, RESTRICTION,
PERMISSION`) change the *rules*, never the mechanics. A **permission** relic (One Bank
Mercy) buys safety; a **restriction** relic (Four-Food Limit, Cursed Backpack) hurts but
pays out **scoring bias**.

**The decision:** play safe, or accept a handicap for a higher score ceiling? This is the
risk-it-for-the-leaderboard knob.

### 4. Route / region commitment — opportunity cost
Each room is a legal OSRS area. Leaving locks that room's loot opportunity (and in STRICT
can fail the run). Boss stages appear after N rooms.

**The decision:** squeeze the current room dry (time cost) or push forward (loot cost)?

### 5. The meta-objective — three ways to "win"
`RunCompletionReason`: `GOAL_COMPLETE, ROUTE_COMPLETE, BOSS_LIST_COMPLETE, SCORE_TARGET,
TIME_SURVIVAL, MANUAL_SUCCESS`. In race/seeded mode players optimize different axes (first
win / fastest / highest score / cleanest), so the *same seed* produces different optimal
play.

> **The 10-second viewer hook:** "I start with nothing. Each room I scavenge gear. After
> every fight I pick one of three chests. If I die, bank, trade, or grab a dirty item — the
> run is over. How far does this build go?"

---

## Part C — The State Machine (what drives every frame)

This is the real loop from `RogueScapeRunLoop` + `RunState`. Every storyboard frame below
is one node here.

```
        ┌────────────┐  START_RUN
        │   LOBBY     │ ───────────────┐
        │ (no run)    │                ▼
        └────────────┘        ┌──────────────────┐
                              │   ROOM_ACTIVE      │◄──────────┐
              COMPLETE_STAGE  │  (scavenge/skill)  │           │ NEXT_STAGE
                       ┌──────┴─────────┬──────────┘           │
                       ▼                ▼                       │
              ┌──────────────┐   ┌──────────────┐      ┌──────────────┐
              │ BOSS_ACTIVE   │   │ BASE_REWARD   │      │ BASE_REWARD   │
              │ (fight)       │   │ (after room)  │      │ (after boss)  │
              └──────┬───────┘   └──────┬───────┘      └──────┬───────┘
                     │ COMPLETE_STAGE   │ CHOOSE_REWARD_n      │
                     └──────────────────┴── SKIP_REWARD ───────┘
                                      │
                                      ▼  (last stage cleared / goal met)
                              ┌──────────────┐        any rule break / death
                              │ RUN_COMPLETE  │        ┌──────────────┐
                              │   (recap)     │        │  RUN_FAILED   │
                              └──────┬───────┘        │   (recap)     │
                                     │ RESET_RUN       └──────┬───────┘
                                     └────────────► LOBBY ◄──── RESET_RUN
```

Mapping of which **PanelAction buttons** are live in each node (from
`SidePanelViewModel.active`):

| Phase / State | Live buttons | Enforcement active |
|---|---|---|
| LOBBY | `START_RUN` | none |
| ROOM_ACTIVE | `COMPLETE_STAGE`, `FAIL_RUN`, `RESET_RUN` | bank/trade/GE blocked; region warn |
| BOSS_ACTIVE | `COMPLETE_STAGE`, `FAIL_RUN`, `RESET_RUN` | same |
| BASE_REWARD (unresolved) | `CHOOSE_REWARD_1/2/3`, `SKIP_REWARD`, `FAIL_RUN`, `RESET_RUN` | relaxed (at base) |
| BASE_REWARD (resolved) | `NEXT_STAGE`, `FAIL_RUN`, `RESET_RUN` | relaxed |
| RUN_COMPLETE | `RESET_RUN` | off |
| RUN_FAILED | `RESET_RUN` | off |

---

## Part D — Storyboard: a full Fresh-Source Boss Run, frame by frame

Mode used for the board: **RogueScape: Fresh-Source Boss Run** (the roadmap's recommended
first public mode). Goal: *Defeat Sarachnis with only legally-scavenged gear.* Strictness:
**STRICT**.

For each frame: **what's happening in-game**, **what each surface shows**, and a
**🎨 IMAGE PROMPT** you can paste into ChatGPT.

> Art-direction note to reuse in every prompt: *"OSRS-inspired but clean modern game UI.
> Dark slate panel with brass/amber accents, fantasy-serious tone with a wink of goblin
> humor. Side panel docked on the right of a RuneScape-style 3D scene. Bold readable HUD.
> 16:9 storyboard frame, numbered caption bar at the bottom."*

---

### Frame 0 — Title / Concept Card
**Purpose:** establish the pitch before any gameplay.

🎨 **IMAGE PROMPT**
> Title card for a fantasy roguelike OSRS plugin called "RogueScape — RuneScape, but every
> run is a roguelike." A naked low-level adventurer stands at the mouth of a glowing dungeon
> made of stitched-together RuneScape regions (Lumbridge, a swamp, a boss arena) floating as
> roguelike "rooms" connected by a branching path R1→R2→B1→Boss. Brass-and-slate UI frame.
> Epic but slightly comedic. Bottom caption bar: "Start with nothing. Every room is a
> gamble. One mistake ends the run."

---

### Frame 1 — The Lobby (pre-run setup)
**In-game:** player is standing in Lumbridge, run not started. `SidePanelViewModel.lobby()`.
**Side panel (RUN tab):** `═══ ROGUESCAPE ═══`, Mode: FRESH_SOURCE, Goal: Defeat Sarachnis,
a "Rules:" block, and one big **[ Start Run ]** button.
**Overlay:** dormant / faint watermark.
**Enforcement:** none yet.

```
 SIDE PANEL (lobby)
 ┌──────────────────────────┐
 │ ═══ ROGUESCAPE ═══        │
 │                          │
 │ Mode: FRESH_SOURCE        │
 │ Goal: Defeat Sarachnis    │
 │                          │
 │ Rules:                    │
 │   Start naked             │
 │   Loot only in-room       │
 │   No bank / trade / GE    │
 │   Death = run over        │
 │                          │
 │   [  ▶ START RUN  ]       │
 └──────────────────────────┘
```

🎨 **IMAGE PROMPT**
> RuneScape-style scene: a barely-equipped adventurer in Lumbridge at dawn. Docked on the
> right, a dark slate "RogueScape" side panel titled ROGUESCAPE with fields Mode:
> FRESH_SOURCE, Goal: Defeat Sarachnis, a rules list (start naked, loot only in-room, no
> bank/trade/GE, death = run over), and a large glowing brass "START RUN" button. Calm
> "before the storm" mood. Caption: "Frame 1 — The Lobby: set the rules, then commit."

---

### Frame 2 — Run Start / Inventory Snapshot
**In-game:** player clicks Start Run (`START_RUN`). Engine snapshots inventory
(`InventorySnapshot`), locks the route, sets phase `ROOM_ACTIVE`, starts the timer.
**Side panel:** flips from lobby to live RUN tab — GOAL, State: RUNNING, Timer: 00:00.
**Overlay:** lights up: goal, room, score 0, region-legal ✓.
**Enforcement:** bank/trade/GE menu entries now stripped.

🎨 **IMAGE PROMPT**
> Same adventurer; a brass "RUN STARTED" banner sweeps across the screen and a translucent
> snapshot icon photographs his empty inventory. The HUD overlay ignites top-left showing
> GOAL: Defeat Sarachnis, ROOM: —, SCORE: 0, a green "REGION LEGAL" pip, and a running timer
> 00:00. Side panel now shows State: RUNNING. Energetic "the run begins" feel. Caption:
> "Frame 2 — Snapshot taken. The referee is watching."

---

### Frame 3 — ROOM_ACTIVE: scavenging in a legal region
**In-game:** R1 = Lumbridge Swamp supply room. Player kills cows/gathers, picks up drops.
Each pickup → `ItemDelta` → classified `LEGAL_REGION_GAIN`. Legal count ticks up.
**Side panel (RUN tab status block):** "CURRENT: Room", Room name, Region id, then the
live **You CAN / You CANNOT** rules list, and a **[ Complete Stage ]** button.
**Overlay:** ROOM: Lumbridge Swamp, SCORE rising, REGION LEGAL ✓.
**Enforcement:** region-mask overlay tints the legal area; bank/trade still blocked.

```
 SIDE PANEL (RUN, room active)        OVERLAY (HUD)
 ┌──────────────────────────┐         ┌───────────────────────────┐
 │ CURRENT: Room             │         │ ⚔ Defeat Sarachnis         │
 │ Room: Lumbridge Swamp      │         │ Room: Lumbridge Swamp      │
 │ Region: 12849             │         │ Score: 120   ● REGION OK   │
 │                          │         └───────────────────────────┘
 │ You CAN:                  │
 │  ✓ Fight monsters here    │
 │  ✓ Pick up drops          │
 │  ✓ Use resources here     │
 │ You CANNOT:               │
 │  ✗ Bank / deposit box     │
 │  ✗ Leave the room region  │
 │  ✗ Trade / GE             │
 │  ✗ Ground items outside   │
 │   [  ✓ COMPLETE STAGE  ]  │
 └──────────────────────────┘
```

🎨 **IMAGE PROMPT**
> RuneScape Lumbridge Swamp at night, glowing green "legal zone" mask tinting the ground
> inside a boundary. The adventurer loots a dropped item with a green ✓ "LEGAL" tag floating
> over it. Right side panel lists "You CAN: fight, pick up drops, use resources" in green and
> "You CANNOT: bank, leave region, trade/GE" in red, plus a "COMPLETE STAGE" button. HUD
> top-left: Room: Lumbridge Swamp, Score: 120, green REGION OK pip. Caption: "Frame 3 —
> Scavenge legally. Everything you grab is judged."

---

### Frame 4 — The Temptation (legality verdict in action)
**In-game:** a ground item appears that the plugin can't prove the player dropped — another
player's pile nearby. `LegalityClassifier` flags `SUSPICIOUS_UNKNOWN`.
**Side panel:** a status warning row appears.
**Overlay:** red warning line "1 suspicious item(s)"; REGION pip stays green but a warning
banner flashes.
**Enforcement (STRICT):** the pickup option on the *foreign* pile can be stripped/blocked,
or grabbing it trips a fail. This is the single most important "drama" beat.

🎨 **IMAGE PROMPT**
> Tense close-up: two loot piles on the ground in a RuneScape field. One glows green with a
> "✓ LEGAL" tag, the other pulses ominous red/amber with a "⚠ SUSPICIOUS — not yours?" tag and
> a faded "do-not-pick-up" cross over the right-click menu. The HUD flashes a red warning bar:
> "1 suspicious item — STRICT mode: this can end your run." The adventurer's hand hovers,
> hesitating. Caption: "Frame 4 — The Temptation: clean run or quick loot?"

---

### Frame 5 — BASE_REWARD: the three-chest draft
**In-game:** player hits Complete Stage. Engine → `BASE_REWARD`; `RewardDrafter` rolls 3.
**Side panel:** "Choose one reward before the next room." Three numbered rows, each
`label [chestType]`, mapped to **[1][2][3]** buttons, plus **[ Skip ]**.
**Overlay:** dims slightly to spotlight the choice.
**Enforcement:** relaxed — you're "at base," not in a region.

```
 SIDE PANEL (RUN, BASE_REWARD)
 ┌──────────────────────────┐
 │ Choose one reward:        │
 │ 1) Bronze Scimitar [WEAPON]│
 │ 2) Leather Body  [ARMOUR] │
 │ 3) 8x Trout      [SUPPLY] │
 │  [1]   [2]   [3]   [Skip] │
 └──────────────────────────┘
```

🎨 **IMAGE PROMPT**
> Three ornate fantasy chests float side by side over a dark velvet "reward draft" screen,
> each labelled: WEAPON (a glowing scimitar), ARMOUR (a leather chestplate), SUPPLY (a stack
> of cooked trout). A brass banner reads "CHOOSE ONE — the others are lost." OSRS roguelike
> style, dramatic spotlight, the two un-chosen chests faded. Side panel mirrors the three
> options as numbered buttons. Caption: "Frame 5 — The Draft: one pick builds your run."

---

### Frame 6 — Choice locked → NEXT_STAGE
**In-game:** player picks **[1] Bronze Scimitar** (`CHOOSE_REWARD_1`). Unlock recorded as
`LEGAL_ROOM_REWARD`; reward resolved.
**Side panel:** "Reward resolved. Move to the next room when ready." → **[ Next Stage ]**.
**Overlay:** score bumps; the new item shows as legal.

🎨 **IMAGE PROMPT**
> The WEAPON chest bursts open in golden light; a bronze scimitar flies to the adventurer's
> hand with a green "LEGAL — ROOM REWARD" tag. The other two chests crumble to dust. Side
> panel shows "Reward resolved" and a glowing "NEXT STAGE" button. Triumphant micro-moment.
> Caption: "Frame 6 — Locked in. Two paths gone, one build forming."

---

### Frame 7 — BOSS_ACTIVE: the checkpoint fight
**In-game:** next stage is B1 = Sarachnis. Phase `BOSS_ACTIVE`. Player fights for real;
plugin watches death state.
**Side panel:** "CURRENT: Boss", boss room rules, **[ Complete Stage ]** (player confirms
the kill — plugin never auto-detects a solve for them, only observes).
**Overlay:** big bold BOSS banner, score, region pip, relic count.
**Enforcement:** bank/trade/GE still blocked; leaving arena warns.

🎨 **IMAGE PROMPT**
> Epic boss arena: the adventurer (now in scavenged bronze + leather) faces Sarachnis, a
> giant spider boss, in a webbed cavern. A bold red "BOSS — SARACHNIS" HUD banner across the
> top with a health-style bar, SCORE, and relic icons. Side panel: "CURRENT: Boss" with a
> "COMPLETE STAGE" button. High-stakes, cinematic. Caption: "Frame 7 — The Checkpoint: die
> here and the whole run dies."

---

### Frame 8a — RUN_COMPLETE (victory recap)
**In-game:** boss cleared, goal met → `RUN_COMPLETE` (`GOAL_COMPLETE`).
**Side panel:** "✓ RUN COMPLETE!" + recap rows (Time, Score, Rooms x/y, Bosses x/y,
legal/illegal, relics) + **[ Reset Run ]**.
**Overlay:** victory state.
**[TARGET]** Recap tab exports Markdown/JSON and a shareable card.

🎨 **IMAGE PROMPT**
> Victory recap screen, fantasy "run summary card" style. Big "RUN COMPLETE" header, a
> portrait of the surviving adventurer, and a stat sheet: Goal ✓ Defeat Sarachnis, Time
> 14:32, Score 1,840, Rooms 3/3, Bosses 1/1, Items Legal 24 / Illegal 0, Relics: none.
> A "Best Unlock: Bronze Scimitar → upgraded to Abyssal whip" line. Brass border, designed to
> look like a shareable Discord/YouTube thumbnail. Caption: "Frame 8 — Victory Recap: the
> story you share."

---

### Frame 8b — RUN_FAILED (the goblin chaos ending)
**In-game:** alternate timeline — the player grabbed the suspicious item in Frame 4, or died,
or opened the bank. → `RUN_FAILED`.
**Side panel:** "✗ RUN FAILED" + same recap rows + fail reason + **[ Reset Run ]**.
**Tone:** UI stays serious; the *fail line* is goblin comedy ("The build has been
contaminated." / "Illegal shark entered the ecosystem." / "Bank goblin caught you.").

🎨 **IMAGE PROMPT**
> Defeat screen with dark humor: a smug goblin holding a stolen shark stands over the
> adventurer's dropped gear, a red "RUN FAILED" stamp slapped across the screen. Stat card
> below reads Fail Reason: "Illegal shark entered the ecosystem", Rooms 2/3, Score 640
> (forfeited). Serious UI frame, comedic content. Caption: "Frame 8b — One mistake. One
> inventory. Sit."

---

### Frame 9 — The Surfaces Side-by-Side (system diagram for viewers)
**Purpose:** one explainer image showing the three surfaces + engine, so a video can explain
"how the plugin works" in five seconds.

🎨 **IMAGE PROMPT**
> Clean infographic, three labeled UI panels connected by glowing arrows to a central brass
> "RogueScape Engine" gear: (1) "SIDE PANEL — the cockpit: tabs RUN/ZONES/RELICS, buttons
> drive the run", (2) "OVERLAY HUD — the live readout: goal, room, score, warnings", (3)
> "ENFORCEMENT — the referee's hands: blocks bank/trade/GE, warns on region exit". Arrows
> show: panel buttons → engine, game events → engine, engine → enforcement. Modern dark
> fantasy infographic. Caption: "Frame 9 — How it fits together: play the game, the plugin
> referees."

---

## Part E — Surface contracts (for whoever builds/extends the UI)

So the storyboard maps cleanly onto code, here is what each surface is *allowed* to do:

**Side panel (`RogueScapePanel` ← `SidePanelViewModel`)**
- Renders row lists per tab (`RUN`, `ZONES`, `RELICS`) + typed live fields (goal, timer,
  score, floor x/y, bosses x/y).
- The *only* surface that emits `PanelAction`s. Every button maps to exactly one action that
  the engine validates against the current phase — a disabled action is simply absent from
  `enabledActions`.
- **[TARGET]** roadmap §12 wants 7 tabs (Run, Inventory/Unlocks, Rooms, Rewards, Relics,
  Recap, Race). Today there are 3; the storyboard's Recap/Inventory beats currently live as
  rows inside the RUN tab.

**Overlay / HUD (`RogueScapeWindowOverlay` ← `OverlayViewModel`)**
- Read-only. Shows goal, current room, score, relic count, legal/suspicious/illegal counts,
  region-legal flag, and a warnings list. Never has buttons. Bold and creator-friendly.

**Enforcement (`MenuEnforcementEvaluator` + region/room-mask overlays)**
- Pure decision in core (`MenuEnforcementDecision`): given phase + rules + a menu entry,
  return allow/block. The adapter applies it by removing menu entries. Never clicks for the
  player. Region-mask overlay paints the legal area.

**Engine inputs (`AdapterTranslator` ← RuneLite events)**
- `ItemContainerChanged` → inventory deltas → legality. `WorldPoint`/region change → region
  legal flag. Death/`GameStateChanged` → fail. These are *observations*, not commands.

---

## Part F — Open storyboard decisions (quick to answer, none block image generation)

1. **Which mode gets the second board?** Bank-Draft (max main) is the most visually unique
   — the "unlock your own bank" chest beat. Worth its own storyboard.
2. **Recap card art style** — do you want the shareable card to look like an OSRS quest-
   complete scroll, a Hades-style run summary, or a Slay-the-Spire map?
3. **Tone calibration** — how heavy is the goblin comedy in *failure* art vs. the serious
   tone everywhere else? Frames 4 and 8b are the levers.
4. **HUD density** — minimal (goal+room+score) for clean gameplay, or rich (all counts +
   relics) for explainer videos? Could be a config toggle.

---

## How to use this with ChatGPT

1. Paste the **art-direction note** (Part D intro) once as a style preamble.
2. Then paste **one 🎨 IMAGE PROMPT per message** in order, Frame 0 → 9.
3. Ask ChatGPT to keep the adventurer, panel style, and color palette **consistent across
   frames** (mention "same character and UI style as the previous image").
4. Drop the resulting images into a slide deck or a single tall board in frame order — that
   is your storyboard.

When you're ready to turn any frame into real behavior, use the roadmap's coding-prompt
format and name the stage (e.g. "Implement Stage 8: Bold Overlay/HUD Polish — Frames 2/3/7
HUD states").

# RogueScape — Design Conversation Decisions Log

Status: LIVING DESIGN NOTES (discussion record). Companion to
`roguescape-game-theory-ui-storyboard-2026-06-01.md` and the north-star roadmap
`research/one_inventory_roguelike_end_state_roadmap.md`.

This file captures decisions reached in design discussion so they are not lost. It is a
record of *intent and rationale*, not a coding spec. Update it as the discussion continues.

---

## 1. Core thesis — it's a RULES game, not a CONTENT game (LOCKED)

The plugin cannot create rooms, loot tables, or bosses. Rooms are real OSRS areas, loot is
real OSRS drops. The only levers RogueScape has are:

- **Permissions** — what you may do right now (use a weapon, enter a region, bank once).
- **Restrictions / modifiers** — what you must give up (fewer food slots, no prayer pots).
- **Scoring** — what behavior is rewarded.
- **RNG** — applied creatively over the above.

RogueScape turns normal OSRS into a **constrained optimization puzzle with permadeath.**
All fun must come from those levers. The creativity is in how cleverly we combine them.

## 2. Modes (LOCKED direction)

- The plugin will **not ship until finished** (no incremental public release pressure).
- Built around **multiple main modes** under one engine.
- Mode names marked **(placeholder)** will be renamed before ship.

### Ranked / Competitive modes

- **Scavenger** *(placeholder name)* — scavenge supplies in rooms, then fight bosses.
  Begin with a constrained loadout budget; earn everything in-run. Three tiers:
  - **Fully Naked** — start with nothing at all.
  - **Low Gear** — minimal starting equipment.
  - **Mid Gear** — supply-run focused, easier and more accessible.
  Draft gives upgrades and artifacts, not items. Room tasks generate currency.
- **Rewarded** *(placeholder name)* — fight bosses to unlock random equipment and
  supplies, use them to fight more bosses. Rewards are RNG item boxes (e.g. armour box =
  random armour piece, buyable or already in bank). Long-form campaign across all 68 main
  bosses. Multi-session by design.
- **Cursed Runs** — dedicated leaderboard category for runs where cursed/handicap cards
  have been taken. Design detail deferred; confirmed as a real competitive bracket.
- **Weekly Challenge** — a featured configuration (seed) broadcast to all players that
  week. Everyone plays under the same ruleset; RNG outcomes (chests, tasks) are still
  random per player. Has its own leaderboard tab.

### Unranked / Personal modes

- **Goal Run** — player inputs 1–3 current OSRS grinds (a level target, a drop goal, a
  quest unlock). The run generator maps those goals to relevant regions, tasks, and a boss
  that sits on the path to each goal. Unranked by design. Selectable from the normal
  lobby wizard.
- **Custom** — fully manual mapping; creator builds the run. Handled in a separate
  complex UI. Unranked / shareable via seed.

Boards are **partitioned by mode + ruleset** — Scavenger and Rewarded runs are not
comparable.

## 3. Enforcement (LOCKED — and enforcement is SILENT)

The plugin enforces legality by **blocking menu entries and actions** — the player
physically cannot do the wrong thing. There is no such thing as a "dirty run" or a
"clean run" as a score metric; violations are not possible, so they cannot be scored.
Enforcement is felt, not read.

Item provenance is tracked internally via:
- `NpcLootReceived` / `LootReceived` — ties dropped items to the specific monster killed.
- `TileItem` ownership flags — distinguishes your drop vs. another player's vs. world spawn.
- Inventory/container deltas for skilling/shop gains.

This tracking drives the enforcement layer and event log, not a cleanliness score.

## 4. Integrity / anti-cheat model (LOCKED)

The hard problem for a leaderboard is **trusting a score computed on the player's machine.**

- **Client attestation / plugin hashing does NOT work.** The client reports its own hash;
  a modified client just reports the legit hash. This is the remote attestation problem.
- Hashing IS valid for **distribution integrity**, not for a server trusting client data.
- **The model: client witnesses, server judges, community verifies.**
  1. Plugin submits a **raw event log** (observations + choices), never a score.
  2. **Backend replays** the log through the authoritative scoring engine.
  3. **Server plausibility checks** reject the physically impossible.
  4. **VOD requirement for records / top of ranked.**
  5. Optional: hash-chained append-only log, external corroboration later.
- Threat model is "clout inflation," not a funded adversary — this posture is proportionate.

## 5. Scoring (DIRECTION — tuning needs a running game)

Score is the heart (leaderboard-centric game). Reward **drama and risk, not efficiency**.

**Score is a combination of multiple factors** so no single metric dominates:
- **Time** — how long the run took.
- **Damage dealt** — offensive performance.
- **Damage received** — defensive performance / risk taken.
- **Leftover supplies** — efficiency of resource use.
- **Risk accepted** — handicap modifiers act as **score multipliers**. The main engine of
  build variety.

Exact weights must be balanced through play-testing. The goal is a score that feels
earned from overall performance, not one that can be gamed by optimising a single stat.

There is **no cleanliness score component** — enforcement is silent and total.
**Scoring weights are part of the ruleset** so weekly events can re-weight the meta.

## 6. Leaderboards (LOCKED)

**Permanent mode leaderboards** — two categories per board:
- Highest Score
- Fastest Time

**Weekly Challenge leaderboard** — three categories:
- Highest Score
- Fastest Time
- First Completion (who finished it first chronologically that week — day-one race incentive)

Ranked = curated preset modes. Custom / Goal Run = unranked / shareable only.
Cursed Runs get their own leaderboard bracket.
Lives in the **web app** (shipping with the plugin).

---

## ACCOUNT PROGRESS PRINCIPLE (LOCKED)

> **Account progress is a first-class citizen.** Every room should be somewhere worth
> being in OSRS. Every task should be something worth doing in OSRS. The roguelike layer
> adds meaning on top — it does not replace the value underneath.

RogueScape is a **structured lens over content you would be doing anyway.** Same XP, same
drops, same account progress, but with direction, stakes, and a story to share. Modifiers
must stay OSRS-legal and must not manufacture artificial tedium without real game value.

---

## RUN STRUCTURE

### 7. Time zones (LOCKED)

Three run lengths, each targeting a different play session:

- **Short** — under 1 hour. ~2–3 rooms + 1 boss. Daily hit, quick roguelike loop.
- **Medium** — aimed at 1–2 hours but can stretch to a full session (~6 hours).
  ~4–8 rooms, 1–2 bosses. Standard roguelike run. Most modes live here.
- **Long** — multi-session. Rewarded mode (all 68 bosses) lives here. Campaign shape.

Time zones are not strictly tied to modes — a Scavenger run can be short or medium
depending on how many rooms the run assigns. Long is effectively Rewarded mode only.

### 8. Boss pool (LOCKED)

All **68 main OSRS bosses** are in the pool. May expand to include everything tracked on
the hiscores (not yet decided). Room regions to be defined separately.

### 9. Run fail states (LOCKED)

Only two ways a run ends in failure:
- **Death** — run over immediately.
- **Forfeit** — player manually ends the run.

No other fail conditions. Silent enforcement means illegal actions are blocked before they
happen, not penalised after.

### 10. Seeds and custom sharing (LOCKED)

- A **seed** is a shareable configuration code for a custom run. It encodes the ruleset,
  mode settings, room list, boss selection, and active modifiers — not RNG outcomes.
- RNG outcomes (chest rolls, task rolls) are always random per player per run. Seeds
  never encode predetermined outcomes.
- **Preset modes** are standardised for everyone and need no seed.
- **Custom modes** use seeds as their export/share format.
- **Weekly Challenge** is a featured seed broadcast to all players — everyone plays the
  same configuration that week, but with their own random outcomes.

---

## UI DECISIONS

### 11. UI surfaces & their jobs

- **Overlay / HUD** — simple glanceable readout while playing: timer + score overview.
  Not complex. Stream-safe.
- **Side panel (Swing)** — the *cockpit*: high-level overview + settings + add custom runs.
- **Enforcement** — menu blocks + region mask; felt, not read.

### 12. Lobby wizard (LOCKED)

Normal mode run start uses a **step-by-step tile wizard** — same big-tile visual language
as the reward draft. Each step is one screen of tiles; pick one and advance.

Four mode tiles on step 1:
- **Scavenger** — scavenge supplies in rooms, fight bosses
- **Rewarded** — fight bosses, unlock random gear, fight more bosses
- **Goal Run** — generate a run around your current OSRS grinds
- **Weekly Challenge** — this week's featured run (zero follow-up steps, straight to start)

**Scavenger** follow-up steps: tier (Fully Naked / Low Gear / Mid Gear) → optional
starting modifier → start.

**Goal Run** follow-up step: simple search/input for 1–3 OSRS goals → run generates → start.

**Custom** breaks out to a **separate complex UI** entirely — not the wizard. Lots of
configuration levers, handled in dedicated real estate.

### 13. What's actually possible in the RuneLite UI (LOCKED feasibility)

- **Side panel:** full Swing, ~225px wide, vertical.
- **Overlays (Graphics2D):** powerful — styled boxes, transparency, custom fonts, PNG art,
  icons, score bars, simple animations. Can project into the world.
- **`RuneLiteObject`**: spawn client-side model entities (fake NPCs / props). Cosmetic and
  local only; no collision; no terrain edits; clickable via menu entries.
- **NOT possible:** new map rooms/areas, real server-side NPCs, authentic OSRS server dialog.

### 14. Art / assets strategy (LOCKED)

- **Item icons free at runtime** via `ItemManager.getImage(itemId)`.
- **Real OSRS font** via `FontManager`.
- **Relic / modifier / artifact icons** → game-icons.net (CC-licensed).
- **UI frames / chest art** → itch.io fantasy GUI kits or Kenney.nl (CC0).
- **Mascot / NPC** → reskin existing OSRS model; AI-gen 2D portrait.
- **Do NOT bundle ripped Jagex cache assets** in the repo.

### 15. The Hub (LOCKED concept; LATER build stage)

- Client-side visual hub at **Emir's Arena** (dead Duel Arena, cheap ring-of-dueling teleport).
- Clickable model stations: run-giver NPC, shop NPC, relic/artifact NPC, reward chest,
  leaderboard board.
- **Hub-and-spoke topology:** runs route `R1 → HUB → R2 → HUB → B1 → ...` Hub transit is
  the **sanctioned legal transition** between disconnected regions.
- Plugin cannot teleport the player. Player travels themselves; plugin detects arrival.

### 16. THE BACKBONE PRINCIPLE (LOCKED — most important UI decision)

- **The UIs are the backbone; the hub is just a diegetic doorway.**
- Each UI is built once and surfaced two ways: pop-up where the player stands, OR via a
  hub NPC click — same window either way.
- **Build UIs first** (fully playable as pop-ups) → hub added later as a visual layer.

---

## REWARD SYSTEM

### 17. Reward taxonomy (DIRECTION)

Three distinct reward types:

- **Upgrades** — expand run operating parameters (extra food slot, weapon switch slot,
  potion slot). Start constrained; draft your way to a real loadout. Scavenger runs primarily.
- **Artifacts** — positive permissions and boons. Bought at the in-run shop (see §19).
- **Modifiers** — restrictions and challenges. Make the run harder, boost score, or add
  a twist.

Draft contents are **mode-dependent**:
- **Scavenger draft** — upgrades and artifacts. Items come from scavenging.
- **Rewarded draft** — RNG item boxes (random equipment / supplies) plus modifiers.

### 18. Banned mechanics — default OFF, artifacts unlock them (DIRECTION)

Certain OSRS mechanics are **banned by default**. Artifacts restore them.

Confirmed banned-by-default:
- **Prayers**
- **Combo foods**

Further candidates (not yet locked): special attacks, certain potion tiers, mode-specific bans.

### 19. In-run currency economy (DIRECTION)

Currency earned within a run, spent at the in-run shop. Does not carry between runs.

**Sources:** room clear (flat) + room tasks (optional bounties) + modifiers (multiplier).

**Sink:** in-run shop — spend on artifacts.

**Three playstyles emerge naturally:**
- Artifact hunter — takes modifiers, completes tasks, hits the shop.
- Speed runner — skips modifiers and shop, blasts rooms.
- Hybrid — one or two modifiers for budget, one shop visit, then races.

### 20. Room tasks (DIRECTION — detail TBD)

Optional bounty objectives within each room, themed to the region. Completing them pays
currency bonus on top of flat room-clear coins. Because tasks map to real OSRS activities,
they generate real XP and drops alongside the currency. Fixed-vs-random approach TBD.

### 21. Modifier list (LOCKED — 12 modifiers, tuning via play-testing)

| Modifier | Effect |
|---|---|
| Cursed Backpack | Reduced usable inventory slots |
| Four-Food Limit | Max 4 food items carried at any time |
| One Weapon | No weapon switches for the run |
| No Potions | Potions banned for the run |
| No Special Attack | Spec bar disabled |
| Style Lock | Must use one combat style only (chosen at run start) |
| No Overhead Prayers | Overhead prayers banned even if prayer artifact is bought |
| Tighter Clock | Reduces the allotted room time limit |
| No Retreat | Cannot exit a room once entered until stage complete |
| Unlucky | Draft shows 2 cards instead of 3 |
| No Drops | Cannot pick up ground items; chest rewards only |
| Bone Collector | Must bury all bones dropped in each room |

### 22. Artifact list (DRAFT — subject to play-testing)

Artifacts bought at the in-run shop. Small set; each meaningful.

| Category | Artifact | Effect |
|---|---|---|
| Permission | Banker's Pass | Use the bank once during current room phase |
| Permission | Trader's License | Buy from any in-world shop once |
| Permission | Prayer Phylactery | Access a prayer altar once mid-room |
| Permission | The Fence | Convert one inventory item to coins at market value |
| Draft | Second Opinion | Reroll one card in your next draft |
| Draft | Lucky Pull | See four cards in your next draft instead of three |
| Draft | Burden of Greed | Take all three draft cards; must take a modifier next draft |
| Economy | Windfall | Double coins from your next room clear |
| Economy | Scout | Reveal next room's task before committing to enter |
| Safety | Insurance | Survive one death this run; supplies halved on revival |

---

## Topic status tracker

| # | Topic | Status |
|---|-------|--------|
| 1 | HUD | locked — simple timer + score overview (§11) |
| 2 | Reward draft UI | next to design — mockup incoming from Caleb |
| 3 | Side panel | direction noted (§11), detail pending |
| 4 | Recap | parked (later) |
| 5 | Web app | big topic, pending |
| 6 | Art / theme / feasibility | resolved (§13–§14) |
| 7 | Currency economy | locked (§19) |
| 8 | Banned mechanics / artifact unlocks | locked (§18) |
| 9 | Artifact list | draft in §22, needs play-testing |
| 10 | Cursed runs | confirmed as concept, design deferred |
| 11 | Goal Run mode | locked as unranked personal mode (§2) |
| 12 | Room tasks | direction noted (§20), fixed-vs-random TBD |
| 13 | Modifiers | locked — 12 modifiers (§21) |
| 14 | Run time zones | locked — short/medium/long (§7) |
| 15 | Leaderboard categories | locked (§6) |
| 16 | Seeds | locked (§10) |
| 17 | Boss pool | locked — 68 main bosses (§8) |
| 18 | Room pool / regions | direction noted, detail coming later |
| 19 | Run fail states | locked — death or forfeit only (§9) |
| 20 | Lobby wizard | locked (§12) |
| 21 | Mode names | placeholder — Scavenger / Rewarded; rename before ship |
| 22 | Scoring metrics | direction set (§5); weights need play-testing |

Open theme question still unanswered: **OSRS-native chunky style** vs. **clean
modern-fantasy skin**. The RogueScape NPC personality (goblin merchant / hooded gambler /
wise guide) also still open.

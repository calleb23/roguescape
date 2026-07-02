# RogueScape — session handover

A single reference for everything decided and built in this work session. Branch:
**`claude/plugin-hub-latest-iteration-enr7f5`** (all commits pushed).

---

## 0. The one constraint that governs everything
RogueScape **cannot change underlying RuneScape mechanics** (no % healing, no damage numbers —
RuneLite can't). The entire design is **restrictions + easing restrictions**:
- You start **shackled**; the plugin **blocks** disallowed actions, or **fails the run** if you do
  one it can't pre-block.
- **Relics are restriction-REMOVERS**, never power-adds. The "build" = the shape of what you've
  un-shackled. Synergy = **permission combos** (unlock prayer + food → you can tank).
- It's a **subtractive roguelike.** Keep reminding me of this.

## Verification caveat (important)
The pure-Java **core compiles and is unit-tested here (237 tests pass)** via `javac`/JUnit. The
**RuneLite adapter layer cannot be compiled** in this sandbox — `repo.runelite.net` is egress-blocked
and there's no cached jar. So every adapter change (plugin/panel/overlay/window) is **grep-verified,
not compiler-checked**. A local Gradle build is the final word on those.

---

## 1. Code shipped this session (14 commits)

**Pre-run Route Briefing** (`core/briefing/`)
- `RunBriefing` + `RunBriefingBuilder`: a "nothing left to interpretation" contract shown before a
  run — exact route room-by-room, each room's clear condition, the rules, win/lose. Built from a real
  preview run so it can't drift from what actually starts. Rendered in the window's CONTRACT tab.

**Removed the entire legality + strictness system** (the big pivot)
- Deleted `core/legality/` (StrictnessMode, ItemLegality, LegalityClassifier, LegalityContext,
  ItemEvent). Moved neutral carriers (ItemDelta, ProvenanceHint, InventorySnapshot, StarterKit) to
  `core/item/`.
- Gutted `RogueScapeRun`: no strictness, no legal/illegal/suspicious judgment, no run-fails-on-item.
  `applyItemDelta` just records a gain now; counts collapsed to a single `itemsCollected`.
- Decoupled relics (`RelicEngine.adjustLegality` gone), scoring (illegal penalty gone), recap, race,
  and both UI view models. Strictness removed from config, panel, custom builder, briefing, overlay.
- Reason: pivot from a **referee that judges items** to an **enforcer that blocks actions**.

**Simplified the live-run UI**
- Collapsed the ~13-line per-room rules dump to **one line**; dropped the raw region id.
- Removed the **Build** tab entirely; folded **Pockets** (relics) into **The Tally**.

**Removed all preset content**
- `CampaignLibrary` emptied (the auto-generated presets had no game context). Every run now
  auto-generates; the campaign mechanism stays so authored runs drop in later. (A curated
  "Goblin Rat" Scavenger run was built mid-session, then removed under this decision.)

**Built the restriction/permission vocabulary** (`core/restriction/`) — the subtractive spine
- `Restriction`: data-driven catalog of everything a run can forbid (bank/trade/GE, food/potions,
  teleports, leave-region, ground-pickup, prayer + Piety/Rigour/Augury, shield, gear-tier cap,
  ammo/runes, inventory limit, spellbook, combat style). Each carries a **Family** and an
  **Enforcement mode** (BLOCK / FAIL).
- `RestrictionOutcome` (ALLOW/BLOCK/FAIL), `Spellbook`, `CombatStyle`.
- `RunRestrictions`: the live "what am I allowed to do" state. Curses/loadout `restrict(...)`; relics
  `permit(...)` / raise caps / add slots / swap spellbook / permit a style. `decide()` + `*Allowed`
  queries give the adapter block-or-fail verdicts. Fully unit-tested.

---

## 2. The locked design

### Two modes
- **Scavenger** — power is **FOUND** in rooms; build from scratch.
- **Boss Ladder** (renamed from "Rewarded") — climb a boss ladder; rewards **unlock gear tiers /
  ease restrictions** that you then **self-source** (GE/bank). No climax needed — cosmetics + hard
  content are the payoff.

### The four upgrade categories (wield / consume / make)
- **Weapon** — a weapon you pick up and wield.
- **Armour** — armour you pick up and wear.
- **Supplies** — anything **consumed/used-up**: food, potions, runes, ammo, **and crafting materials**
  (bars/ores).
- **Crafting** — an item you **make and then wear/wield** (provenance = you made it). Enables the
  loop: gather bars (Supplies) → reach an anvil room → smith your upgrade (Crafting).
- Categories exist mainly for **tracking**; per-category counts + multipliers (ammo/runes counted
  higher) are a balance knob.

### Rewards — one mechanic, different pools
A reward node = draft **1 of N**. **Scavenger pool = relics** (gear is scavenged from rooms, so no
loot chest — the chest *is* the relic draft). **Boss Ladder pool = gear-tier unlocks + relics**.

### Relics / Curses / Coins / Cosmetics
- **Relics** = restriction-removers (permission unlocks). Synergy = permission combos.
- **Curses** = the unified, stackable restriction toolkit. **Setup-only.** Each scores. Optional
  later: a "wildcard/daily" mode that randomly deals a curse loadout.
- **Coins** = RNG mitigation. Earned from **doing activities** (the grinds) + speed; spent on
  **rerolls with escalating cost**; never disables curses.
- **Cosmetics** = meta-progression payoff (finish the ladder → unlock a hat/title/journal stamp).
  Plugin-side flair, not game items.

### Room types (all ACCEPTED)
collection · branching route · competing objectives · **Shrine** (prayer-tier unlocks
Piety/Rigour/Augury; magic altar switches spellbook Standard/Ancient/Lunar/Arceuus; general shrine
eases a restriction) · **Elite** (extra local restriction for a better reward) · **Event** (cursed-path
decision node) · **Shop = gamble** (reroll / buy a permission / random spin instead of an upgrade).

### Push-your-luck (all ACCEPTED — "bust" matters because breaking a rule ends the run)
cursed chest (relic + new restriction, keep opening) · go-deeper (optional stacked-restriction room) ·
self-imposed timer dare · coin wager.

### Enforcement model
Two tools, **no honour system**: **BLOCK** (prevent the menu action) and **FAIL** (detect → end run).
Prayers/spellbook/combat-style are mostly detect-and-fail; bank/trade/GE/gear/inventory are pre-block.

### Levers are DATA, not hardcoded
Every lever lives in one central **tuning surface** the engine reads, the briefing displays, and
**Custom mode edits**. Balance **values are deferred to playtest** — the goal was to identify and
expose the levers, not fix numbers. Start tiers **None/Low/Medium/High** (jump straight to high-end).

### UI direction (the Journal)
- In-game window = **bare-bones, tabs removed** — only the live two-page **journal spread** (left page
  = what I have / my choices; right page = the world / route / context).
- **Sidebar stays** as a simple glanceable live tracker (room, mode, progress) + config/options/map.
- Build waits on a **hand-drawn mockup** (you're drawing one) so we build the layout once.

### The retention hook (a SEPARATE use-case, not the spine)
Custom mode lets people structure real **skilling grinds** (Scavenger) and **combat achievements**
(Boss Ladder) as runs. The normal modes stay fun-first; the grind/CA framing is an opt-in section.

### Known risks still open
- **Curation cost** — OSRS has empty places; good random rooms are hard.
- **Soft-locks** — branches/random rooms can strand you. Mitigations: validated generation (only offer
  completable branches), a bail option (abandon a room at a cost), curated=campaign vs random=endless.

---

## 3. Roadmap — what's next (suggested order)
1. **Curses → `RunRestrictions`** — define the curse catalog as "this curse adds these restrictions."
   (pure-core, testable)
2. **Relics → `permit(...)`** — redefine relics as restriction-removers over the new vocabulary;
   retire the old stat-flavored relic effects. (pure-core, testable)
3. **Wire `RunRestrictions` into `RogueScapeRun`** + migrate the old enforcement to `decide(...)`.
   (needs the live client to fully verify)
4. **Four categories** — land them with the first real rooms (not the placeholders), which also fixes
   in-game item tracking.
5. **Journal spread UI** — after the mockup. Tabs out, two-page spread, sidebar tracker.
6. **Tile-precise custom areas** — replace region-chunk rooms with painted tile sets (RuneLite
   `getCollisionMaps` is per-tile but local-only; the Shortest Path plugin has a full-world tile set).
7. **Cosmetics**, then the **coins/shop**, then the **wildcard/daily** curse mode.

## 4. Doc index (where the detail lives)
- `roguescape-gameplay-design.md` — the master design (modes, rewards, relics, curses, rooms,
  push-your-luck, levers, gaps).
- `roguescape-next-phase-plan.md` — categories, pick mechanic, tracking, transitions, sidebar,
  tile-areas.
- `roguescape-journal-spread-layout.md` — the two-page UI spread.
- `docs/roguescape/mockups/concept-journal-*.png` — existing visual concepts.

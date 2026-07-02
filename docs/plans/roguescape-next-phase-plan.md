# RogueScape — next-phase plan (gameplay model, tracking, transitions, custom areas)

Captures the direction set while the player builds out content + draws the UI. Most of this is
**plan, not build** — flagged per item. The in-game window/overlay is the RuneLite layer, which
cannot be compiled in the headless dev sandbox, so visual work waits on the UI drawing.

## 0. Presets removed — DONE
Auto-generated presets had no real game-design context, so `CampaignLibrary` is now empty: every
run auto-generates from the room/boss libraries. The campaign mechanism stays so authored runs drop
straight in later. `RunPreset` is kept but vestigial (only affects auto-gen sizing). **Rooms are
left in place but are known-wrong placeholders** — they get rebuilt with the area work below.

## 1. Room upgrade categories → exactly four  (PLAN — confirm, then build)
The current `RoomKind` set (REGION/COMBAT/SUPPLY/WEAPON/ARMOUR/SHOP/SKILLING) is muddled — "region"
in particular doesn't make sense as an upgrade restriction. Replace the room's upgrade restriction
with **four categories** (what kind of upgrade you may collect in that room), plus BOSS:

The split is **wear/wield vs. consume vs. make** (confirmed):

| Category | What counts | Test |
|----------|-------------|------|
| **Weapon**   | a weapon you pick up and wield | item is a weapon |
| **Armour**   | armour you pick up and wear | item is armour |
| **Supplies** | anything **consumed / used up**: food, potions, **runes, ammo**, *and crafting materials* (bars, ores) you'll consume to make something | item is a consumable or a raw material |
| **Crafting** | an item you **create and then wear/wield** — a smithed/crafted weapon or piece of armour | provenance = you made it (OBSERVED_CRAFTED) and the result is equipment |

Drop REGION, COMBAT, SHOP, SKILLING as room categories.

- **Weapon/Armour** = found/looted ready-made gear (by item type).
- **Supplies** = consumed items. Ammo and runes are supplies (they're spent). **Crafting materials
  (bars/ores) are supplies too** — they're consumed in the making.
- **Crafting** is distinguished from Weapon/Armour by *how you got it*: you **made** it. The output
  is worn/used. This enables the intended loop: gather bars in a **Supplies** room → carry them to a
  room with an anvil → **Craft** your upgrade there.

This is a core, testable change. It also fixes most of the tracking problem (below) because
acceptance becomes simple and correct. **Build it alongside the real rooms/areas** (the current
rooms are throwaway placeholders), so `RoomKind` is reduced to the four + BOSS at the same time the
real content goes in — rather than remapping placeholders that get deleted.

## 2. The "your pick" room mechanic  (PLAN)
A room grants **one upgrade of its category** — and the item *is* the choice:
- You fight / gather / wait, and the qualifying item you pick up becomes your pick, stamping the room.
- Not a 3-option reward grid — it's "grab the one you want." Patience can win a better drop.
- An **optional per-room timer** (already supported) creates the tension: take the safe early item,
  or hold out for something better before the clock runs.
- Core change: room objective = collect 1 item matching the room category; the qualifying pickup
  completes it. (Decide whether you can keep upgrading your pick until you leave / the timer ends.)

## 3. In-game item tracking — the current blocker  (PLAN, needs in-game test)
Symptom: items picked up in-zone aren't tracked; categories reject them.
- Likely cause: objective category matching uses the old muddled kinds, and/or the adapter's
  inventory-diff wiring. Fixing §1 makes acceptance simple and correct.
- After §1 lands, verify the adapter path: `RogueScapePlugin` inventory diff → `applyItemDelta` →
  `recordCurrentStageItemGain`. This requires **in-game testing** — it can't be verified in the
  headless sandbox (RuneLite is egress-blocked here).

## 4. Smooth, guided room transitions  (PLAN)
Room → room is abrupt today. Goal: a guided hand-off into the next room.
- Lean on what exists: clear "Travel to X" guidance + a marked destination (the Shortest Path
  bridge already sends a target). Add a gentle arrival beat so entering the next room feels framed,
  not snapped.
- Longer-term: a small custom "staging area" at a location between rooms. Plan it; don't build yet —
  keep the first run dead simple.

## 5. In-game window = bare-bones; sidebar = simple tracker + config  (DECIDED; build with the UI drawing)
- **Remove tabs entirely** from the in-game window. It shows only the live play spread (the journal
  two-page layout — see `roguescape-journal-spread-layout.md`). Nothing else.
- The **sidebar (Swing side panel) is NOT removed.** It keeps a **simple, glanceable live tracker** —
  the room you're in, the mode you're doing, basic progress — so you can track what you're doing at
  a glance without opening the big window. The big window is for when you want the full detail.
- The sidebar also holds **configuration / options / route-building / map selection**.
- Exact sidebar look is TBD — we design it later. Implementation waits on the UI drawing so we build
  the layout once, not twice.

## 6. Custom areas at tile precision (map drawing)  (RESEARCHED — plan only, per request)
Goal: draw real custom areas (and have the masked/"blurred" area follow the drawing) instead of
whole 64×64 region chunks, so areas can be distinguished precisely.

**Is tile information available? Yes.**
- `Client.getCollisionMaps()` → `CollisionData[]` per plane: per-tile movement flags, but only over
  the **104×104 local scene** around the player (precise, yet local-only).
- The **Shortest Path plugin** ships a precomputed **full-world walkable-tile dataset** — that's how
  it pathfinds anywhere; the repo already talks to it via `PluginMessage`.
- For *drawing* areas you don't even need collision: store the **set of tiles** the user paints
  (`WorldPoint`s, or a `WorldArea` polygon list) instead of region IDs.

**Approach (future build):**
1. Replace `RogueScapeCustomRoomSelection` (a set of region-ID chunks) with a **tile-set / polygon**
   model (WorldPoints or WorldAreas).
2. **World-map editor:** click/drag on the world map → add/remove tiles (snap to tile); the world-map
   overlay converts map pixels → WorldPoints. Optionally constrain to walkable tiles via
   `getCollisionMaps()` (in-scene) or the SP dataset (anywhere) if we integrate it.
3. **Mask overlay** renders per-tile instead of per-chunk.

**Verdict:** feasible; the region-ID room model is a placeholder the tile model supersedes. Don't get
locked into the region system. Planned, **not** implemented.

## Suggested order once content/UI are ready
1. §1 four categories (core, tested) → unblocks §2/§3.
2. §2 the pick mechanic (core, tested).
3. §3 verify in-game tracking (needs the live client).
4. §5 the bare-bones window + sidebar (after the UI drawing).
5. §6 tile-based areas (editor + overlay).
4. §4 transition polish folds in alongside.

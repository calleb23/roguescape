# RogueScape Plugin Overview

## One-Sentence Pitch

RogueScape turns OSRS into a guided roguelike run: travel room-to-room, survive restrictions, earn random rewards, unlock new freedoms, and build a unique run using RuneLite overlays, map guidance, and custom UI windows.

## What RogueScape Is

RogueScape is a RuneLite roguelike layer for Old School RuneScape.

The player starts a run, gets a route of rooms and bosses, plays under restrictions, earns random rewards, unlocks new permissions, and progresses through a run like a campaign or dungeon crawl.

## Core Player Flow

1. Choose a run type.
2. Press Start Run.
3. Travel to the target room.
4. Timer starts when entering the allowed region.
5. Complete the room objective or let the timer expire.
6. Choose a random reward.
7. Move to the next room or boss.
8. Finish the route and complete the run.

## The Main Loop

```text
Choose Run
  -> Start Run
  -> Travel to Room
  -> Enter Allowed Region
  -> Room Timer Starts
  -> Complete Objective / Timer Expires
  -> Reward Draft
  -> Next Stage
  -> Run Complete
```

## UI Responsibilities

The UI should always answer:

1. Where am I meant to be?
2. What am I allowed to do?
3. What is my current objective?
4. What reward/progress did I earn?

## UI Surfaces

Side Panel:

- Run builder
- Live run status
- Build/artifacts/modifiers/progression
- Custom route and zone tools

Pop-out Window:

- Large RuneScape-style presentation window
- Run builder
- Live run display
- Artifact/reward views

HUD Overlay:

- Current room
- Current objective
- Timer
- Region status

Reward Window:

- Card-based random reward choices
- Relics, supplies, unlocks

Map/Scene Visuals:

- Green active room tiles
- Grey outside-room dimming
- Green world-map region highlight
- Clickable world-map destination marker

## Travel Phase Example

```text
RogueScape
Floor 2 / 7

Phase: Travel
Destination: Draynor Village
Region: 12338
Timer: 05:00 once you enter

Objective waits there:
Collect 2 legal supplies
```

## Room Active Example

```text
RogueScape
Phase: Room
Room: Draynor Village
Timer: 04:12

Objective:
Supplies 1 / 2

Allowed:
- Pick up drops in room
- Use resources found here

Blocked:
- Bank
- Trade / GE
- Ground items outside room
- Walk while outside room
```

## Reward Draft Example

```text
Reward Chest
Choose one reward

[Unlock: Prayer Access]
[Supply: Sharks x5]
[Relic: Blood Relic]
```

Rewards appear after room completion, timer expiry, or stage completion depending on the run rules.

## Custom Builder

Custom mode should stay in its own focused window.

Custom options include:

- Game mode: Scavenger or Reward
- Loadout
- Rooms
- Room allowance type
- Bosses
- Mixed route ordering
- Modifiers
- Constraints
- Time limit
- Seed

## Custom Builder Route Example

```text
Route:
1. Draynor Village [Supply]
2. Obor [Boss]
3. Varrock East [Weapons]
4. Bryophyta [Boss]
```

Bosses are mixed route options, not only final stages.

## Systems Connection

```text
Run Builder
  -> Route Builder
  -> Run Loop
  -> HUD / Panel / Pop-out Window
  -> Map Visuals
  -> Reward Drafts
  -> Unlock System
  -> Enforcement Rules
```

The important connection:

```text
Route stage
  -> region rule
  -> travel target
  -> map highlight
  -> room timer starts on entry
  -> enforcement arms
  -> objective/reward flow
```

## Unlock System

Unlocks turn room clears into meaningful progression.

Examples:

- Supply rooms can unlock potion/food rules.
- Shop rooms can unlock trade/shop access.
- Combat rooms can unlock prayer or combat freedoms.
- Reward drafts can grant relics, supplies, or permissions.

Unlocks affect enforcement:

```text
Prayer locked -> prayer blocked
Prayer unlocked -> prayer allowed

Potions locked -> potion blocked
Potions unlocked -> potion allowed

Bank locked -> bank blocked
Bank unlocked -> bank allowed
```

## Map Guidance

RogueScape gives destination guidance through:

- HUD target text
- Green legal-room tiles
- Grey outside-room dimming
- World-map green room region
- Clickable RogueScape map marker
- Optional Shortest Path bridge

## Testing Checklist

Start Run:

- Side panel start
- Pop-out window start
- Custom builder start

Travel:

- HUD destination appears
- Map marker appears
- Green region appears on world map
- Timer does not drain before entry

Room Active:

- Timer starts on entry
- Green tiles show legal room
- Outside room is dimmed
- Enforcement blocks outside-room actions

Rewards:

- Room completion opens reward
- Timer expiry opens reward
- Reward choice applies unlock/relic/supply

Next Stage:

- Next stage returns to Travel
- New destination marker appears

## Still Needs Iteration

- Shortest Path live bridge confirmation
- Walk-blocking edge cases
- More accurate room region data
- Exact destination tiles instead of region centers
- Reward tables and balancing
- More magical reward/window animations
- More polished artifact/modifier presentation


# RogueScape

RogueScape turns Old School RuneScape into a roguelike challenge run. You travel a generated
**route** of rooms and boss stages, clear each stage's objective under a timer, then **draft a
reward** — a relic, an unlock, supplies, or a bank item — that changes the rules for the rest of
the run. Found power becomes your build.

It is a planning, tracking, and rule-enforcement layer over normal gameplay: RogueScape decides the
route, shows you where to go, watches what you pick up, and scores the run. It does not automate or
play the game for you.

## Modes

| Mode | Shape | Fantasy |
|---|---|---|
| **Scavenger** | 3 rooms → 1 boss | Rooms build the run, the boss tests the build |
| **Boss Ladder** | Bosses only (prep *phase* between them) | Climb the ladder; rewards un-shackle you, you gear up between fights |
| **Custom** | Player-built mixed route | Build your own route of rooms, bosses, zones, mods, and constraints |

## Features

- **Route generation** per mode, with optional named presets and a **seed** (identical seeds
  reproduce the same route).
- **Travel → Active → Reward → Next** run loop: a stage's timer starts when you enter its allowed
  region, completes on its objective (item/XP/shop/combat or boss defeat), then opens a reward draft.
- **Destination HUD + world map**: the current room's region is highlighted, with an optional
  greyed-out mask over tiles outside the room, and an optional [Shortest Path](https://github.com/Skretzo/shortest-path)
  bridge that targets the room for you.
- **Reward drafts**: relics (run modifiers), unlocks (e.g. bank/prayer/potions), supplies, and bank
  items, applied to your run state and to the rule enforcement.
- **Region & rule enforcement**: bank/trade/prayer/potion/GE/pickup/walk restrictions follow your
  unlocks and the active region. Enforcement is integrity tooling for the challenge, not anti-cheat.
- **Custom builder**: a side-panel and draggable in-game window for assembling a route, choosing room
  allowances, ordering rooms/bosses, picking starting modifiers, and setting strictness/time/bank
  constraints.
- **Recaps & scoring** for finished runs.

## Configuration

Open the RogueScape side panel from the RuneLite toolbar, or configure under
**Settings → Plugin Hub → RogueScape**. Notable options:

- **Grey Out Outside Room** / **Room Mask Opacity** — the passive off-room visual aid.
- **In-game Windows** — show the draggable in-game builder window (on by default).
- **Allow Bank Unlocks**, **Flag Pre-run Supplies** — restriction tuning.
- **Developer Mode** — exposes DEV TOOLS for stepping a run without playing (off by default; for UI
  development).
- **Experimental Quest-tab UI** — opt-in widget probe; off by default.

## Building & running

This is a standard RuneLite external plugin (Gradle).

```sh
./gradlew test          # run the unit-test suite
./gradlew build         # build the plugin jar
./gradlew run           # launch a RuneLite dev client with the plugin (see run-client scripts)
```

## Safety notes

- RogueScape is **passive tracking + visual aids + opt-in rule enforcement**. It does not perform
  any gameplay actions for you.
- Region rule enforcement may filter or block right-click menu entries during an active run to keep
  the challenge honest; it is scoped to the run and never touches login/account/session APIs.

## License

[BSD 2-Clause](LICENSE).

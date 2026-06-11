# RogueScape Gameplay Function Checklist

This is the working checklist for getting **Scavenger**, **Rewarded**, and **Custom** into tester hands.

Goal and Weekly are parked for now.

## Status Key

```text
Working   = implemented and covered enough to test in-game
Partial   = implemented, but needs live testing or wiring polish
Missing   = not implemented yet
Parked    = intentionally not part of the current tester build
```

---

## Current Tester Modes

| Mode | Status | Intended Shape | Notes |
|---|---:|---|---|
| Scavenger | Working | 3 rooms + 1 boss | Ready for in-game tester pass |
| Rewarded | Working | 1 prep room + 2 bosses | Ready for in-game tester pass |
| Custom | Working | Player-built mixed route | Ready for in-game tester pass |
| Goal | Parked | TBD | Hidden/de-emphasized for tester build |
| Weekly | Parked | TBD | Hidden/de-emphasized for tester build |

---

## Global Run Function Checklist

| # | Function | Status | Applies To | Acceptance Criteria |
|---:|---|---:|---|---|
| 1 | Mode selection | Working | Scavenger, Rewarded, Custom | UI exposes only current tester modes; selected mode maps to correct run mode |
| 2 | Route generation | Working | Scavenger, Rewarded, Custom | Scavenger creates 3 rooms + 1 boss; Rewarded creates 1 prep room + 2 bosses; Custom preserves mixed room/boss order |
| 3 | Custom route building | Working | Custom | Rooms and bosses can be added into one mixed route list and the factory honors that order |
| 4 | Room type/allowance selection | Working | Custom | Each custom room stores Supply/Armour/Weapons/Skilling/All/Shopping and maps to gameplay rules/objectives |
| 5 | Travel phase | Working | All | Run starts each stage in Travel; timer does not drain before entry |
| 6 | Destination HUD | Working | All | HUD shows target room, objective, allowed region, and timer-waits message |
| 7 | World map destination | Working | All | Current room region is green on world map; jumpable map marker targets the region center |
| 8 | Shortest Path bridge | Working | All | Attempts to set external Shortest Path target during Travel, clears it on reset, and reports status |
| 9 | Region entry detection | Working | All | Entering allowed region transitions Travel -> Room/Boss active |
| 10 | Room timer start | Working | All timed stages | Timer starts on region entry, not on run start |
| 11 | Room objective tracking | Working | Scavenger, Custom | Item/XP/shop/combat/weapon/armour progress can complete room objectives |
| 12 | Boss objective tracking | Working | Rewarded, Scavenger, Custom | Boss defeat signal and boss kill chat gate boss completion |
| 13 | Region enforcement | Working | All | Bank/trade/prayer/potion/GE/pickup/walk restrictions follow unlocks, Travel, and active region |
| 14 | Scene room mask | Working | All | Non-allowed visible tiles are greyed; allowed tiles are not painted green |
| 15 | Reward draft generation | Working | All | Stage completion/timer expiry can create reward draft |
| 16 | Reward choice application | Working | All | Chosen relic/unlock/supply/bank item applies to run state |
| 17 | Unlock application | Working | All | Unlocks update enforcement rules |
| 18 | Next-stage transition | Working | All | Resolved reward allows moving to next Travel phase |
| 19 | Run completion | Working | All | Final stage opens its reward, then resolved reward completes the route |
| 20 | Failure/abandon/reset | Working | All | Manual fail, reset, death chat detection, and time-limit failure are wired |

---

## Scavenger Mode Checklist

Intended fantasy:

```text
Rooms build the run. Boss tests the build.
```

Default route:

```text
Room -> Room -> Room -> Boss
```

| Step | Function | Status | Expected Behavior |
|---:|---|---:|---|
| 1 | Select Scavenger | Working | Side panel/pop-out selects Scavenger |
| 2 | Start run | Working | Run creates and enters Travel phase |
| 3 | Generate route | Working | 3 rooms + 1 boss |
| 4 | Travel to room | Working | HUD/map shows destination |
| 5 | Enter room region | Working | Timer starts and enforcement arms |
| 6 | Complete room objective | Working | Room progress updates from legal item/source events |
| 7 | Claim reward | Working | Reward draft appears and selected option applies |
| 8 | Unlock rules update | Working | Prayer/potions/bank/trade/etc respond to unlocks |
| 9 | Repeat rooms | Working | Next room returns to Travel phase |
| 10 | Boss stage | Working | Boss requires defeat signal |
| 11 | Complete run | Working | Final boss reward resolves, then run completes |

### Scavenger Test Script

```text
1. Pick Scavenger.
2. Press Start Run.
3. Confirm HUD says Travel.
4. Open world map and confirm green target region/marker.
5. Walk into target region.
6. Confirm timer starts.
7. Complete or force complete first room.
8. Choose a reward.
9. Confirm unlock/relic/supply updates UI.
10. Press Next Stage.
11. Repeat until boss.
12. Defeat/complete boss and confirm run end.
```

---

## Rewarded Mode Checklist

Intended fantasy:

```text
Short prep. Boss-chain pressure. Rewards carry survival.
```

Default route:

```text
Supply/prep room -> Boss -> Boss
```

| Step | Function | Status | Expected Behavior |
|---:|---|---:|---|
| 1 | Select Rewarded | Working | Side panel/pop-out selects Rewarded |
| 2 | Start run | Working | Run creates and enters Travel phase |
| 3 | Generate route | Working | 1 prep/supply room + 2 bosses |
| 4 | Prep room reward | Working | First room creates useful reward/unlock |
| 5 | Boss 1 | Working | Boss completion creates reward stop |
| 6 | Reward after boss | Working | Reward draft appears |
| 7 | Boss 2 | Working | Final boss creates final reward |
| 8 | Run finish | Working | Final reward resolves, then route completes |

### Rewarded Test Script

```text
1. Pick Rewarded.
2. Press Start Run.
3. Confirm route has one prep room and two bosses.
4. Enter prep room and start timer.
5. Complete prep objective or let timer expire.
6. Pick reward.
7. Go to boss 1.
8. Trigger boss defeat/completion.
9. Pick reward.
10. Go to boss 2.
11. Complete boss 2 and confirm run complete.
```

---

## Custom Mode Checklist

Intended fantasy:

```text
Build your own RogueScape route, then play the normal run loop through that route.
```

Custom components:

```text
Game mode
Loadout
Rooms
Room allowance/type
Bosses
Route order
Modifiers
Strictness
Bank start toggle
Time limit
Seed
Custom region/zone
```

| Step | Function | Status | Expected Behavior |
|---:|---|---:|---|
| 1 | Open custom builder | Working | Builder window opens separately |
| 2 | Select custom game mode | Working | Scavenger/Rewarded labels stored |
| 3 | Select loadout | Working | Starter kit changes |
| 4 | Add rooms | Working | Room appears in route list |
| 5 | Select room allowance | Working | Allowance is stored with room |
| 6 | Add bosses | Working | Boss appears in same mixed route list |
| 7 | Reorder route | Working | Up/down controls affect route order |
| 8 | Add modifiers | Working | Starting curses/relic modifiers apply |
| 9 | Set constraints | Working | Strictness/bank/time/boss cap stored |
| 10 | Start custom run | Working | Start action runs on the client thread; custom factory starts in Travel |
| 11 | Custom route gameplay | Working | Each stage follows normal Travel -> Active -> Reward -> Next flow |
| 12 | Custom zone | Working | Selected custom regions apply to non-boss stages |

### Custom Test Script

```text
1. Open Custom.
2. Add a Supply room.
3. Add a boss.
4. Add a Weapon room.
5. Move the boss between rooms.
6. Set time limit.
7. Pick a loadout.
8. Start Custom Run.
9. Confirm route order matches builder order.
10. Confirm first stage starts in Travel.
11. Play through first room reward.
12. Confirm next stage is the boss.
```

---

## Interconnection Checklist

This is the most important section. These functions must work together, not just individually.

| Chain | Status | Must Prove |
|---|---:|---|
| Mode -> Route | Working | Selected mode creates expected route shape |
| Route -> Region Rules | Working | Every stage has region rules |
| Region Rules -> Map/HUD | Working | Destination region appears in overlays |
| Travel -> Entry Detection | Working | Correct region starts stage |
| Entry Detection -> Timer | Working | Timer starts on entry |
| Timer/Objectives -> Reward | Working | Completion or timeout opens reward |
| Reward -> Unlocks | Working | Chosen rewards mutate run state |
| Unlocks -> Enforcement | Working | Menu rules change after unlock |
| Reward -> Next Stage | Working | Next stage enters Travel |
| Custom Builder -> Route | Working | Mixed ordered route is honored |
| Custom Zone -> Enforcement/Mask | Working | Custom region selection affects current run |

---

## Known Live-Test Risks

These are expected to need hands-on RuneLite testing:

```text
Shortest Path bridge may not work depending on how the external plugin is loaded.
Walk blocking is intentionally blunt and may need tile/menu-action tuning.
Boss defeat detection may need more boss-specific chat/loot signals.
Some room region IDs may be wrong or too broad.
Timer expiry reward flow needs feel testing.
Custom region selection needs repeated in-game validation.
```

---

## Immediate Priority Order

1. Start each tester mode successfully.
2. Confirm each mode route shape.
3. Confirm Travel -> Active transition.
4. Confirm timer starts on entry.
5. Confirm objective completion path.
6. Confirm timer expiry reward path.
7. Confirm reward choice changes state.
8. Confirm next-stage travel path.
9. Confirm boss completion path.
10. Confirm run completion.

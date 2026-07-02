# RogueScape — Wording Schema (S7)

The UI is **an adventurer's journal**. Every string on screen should sound like it was
written in that journal — or stamped onto it. This file is the contract for all UI copy;
when adding a string, pick words from the lexicon below.

## Voice rules

1. **The journal describes; stamps command.** Body text is calm logbook register
   ("No run underway."). Buttons and stamps are short imperatives ("Begin the Run",
   "Abandon Run", "Take It").
2. **One name per thing, everywhere.** Use the lexicon. Never mix "stage/room/floor"
   when "chapter" is meant; never mix "legal/lawful" — it's *lawful/forbidden* on the page.
3. **Casing:** prose is sentence case; the journal's proper nouns are Title Case
   (The Ledger, The Record, The Passport, The Hourglass, The Chest); ONLY section
   headers and stamp text render in caps (the components uppercase them — write
   strings in normal case).
4. **Flavor is one short clause; function comes first.** "The chest is open — choose
   one before you go." Never bury the verb a player needs.
5. **No tech words on the page**: no "UI", "view", "mode preset", "status", "config".
   (The RuneLite config panel is exempt — it stays plainly descriptive for reviewers.)
6. **Numbers stay plain** ("2 of 4", "06:18"). No roman numerals outside chapter ordinals.
7. **Punctuation:** full sentences end with periods; no exclamation marks; em-dashes for
   the flavor clause; ellipses ("…") only on quiet links and windowed lists.

## Lexicon

| Concept | The journal calls it | Never |
|---|---|---|
| Run mode | **a contract** (you *sign* one) | mode, preset |
| One stage of the route | **a chapter**; the closing boss is **the Final chapter** | stage, floor, step |
| Completing a stage | the chapter is **stamped** | cleared, done |
| Moving on | **turn the page** | next stage |
| The route list | **The Record** | route, list |
| The all-stamps grid (long runs) | **The Passport** | progress grid |
| The stats block | **The Ledger** | status, stats |
| Run timer | **The Hourglass** / time **afoot** | timer |
| Reward draft moment | **The Chest** (it *opens*; skipping = *leave the chest*) | reward screen |
| Relic storage | **pockets** (relics are *tucked away*) | inventory, slots |
| Negative modifiers | **curses**, written in red | debuffs, modifiers |
| Legal / illegal items | **lawful / forbidden** | legal, illegal |
| Allowed/blocked actions | **Permitted here / Forbidden here** | You CAN / You CANNOT |
| Score | tally (drawn as tally marks) | points |
| Objective | **the task** | objective, goal |
| End-of-run summary | **the recap** (functional, shared with players) | — |
| Seed | **the seed** (functional, shared with players) | — |

Color semantics: green ✓ = permitted/done, red ✗ = forbidden/curse, stamp red = the
player's own marks (CHOSEN, CLEAR, ribbon), gold = treasure/score.

## Applied mapping (current → new)

### Side panel
| Current | New |
|---|---|
| section "Run Builder" | "Pick Your Contract" |
| fieldLabel "Mode" | *(removed — the contracts are the label)* |
| section "Live Run" | "The Ledger" |
| section "Build" | "The Build" |
| section "Artifacts" | "Relic Pockets" |
| section "Modifiers" | "Curses" |
| section "Progression" | "The Tally" |
| section "Relics" | "The Catalogue" |
| section "Starting Curses" | "Starting Curses" *(kept)* |
| section "Route" | "The Route" |
| builder tabs Run/Route/Zone/Mods | Run/Route/Zone/Curses |
| contract subs ("Earn power room by room"…) | lowercase promises: "earn power, room by room" / "short prep, boss loot" / "draw your own route" / "same seed, same fate" / "one region at a time" |
| "▶ START RUN" / startRunLabel variants | "Begin the Run" |
| "↻ Reset Run" | "Reset the Journal" |
| "✗ End Run" | "Abandon Run" |
| "✓ Complete Stage" | "Stamp the Chapter" |
| "▶ Continue" | "Turn the Page" |
| "⟲ Skip Reward" | "Leave the Chest" |
| "✦ Reward 1/2/3" | "Take Reward 1/2/3" |
| "Copy Recap" | "Copy Recap" *(kept)* |
| "Run not started." / "Pick a run, then START." | "No run underway." / "Sign a contract, stamp it, begin." |
| ledger labels Legal/Illegal | Lawful/Forbidden |

### View model (status rows)
| Current | New |
|---|---|
| "✓ RUN COMPLETE!" | "✓ The run is complete." |
| "✗ RUN FAILED" | "✗ The run has failed." |
| "✗ Over X limit! (relic)" | "✗ Over the X limit — a curse bites." |
| "Travel to: X" | "Travel to X." |
| "Objective waits there: …" | "The task waits there: …" |
| "Objective: …" | "The task: …" |
| "Objective complete - claim your reward when ready." | "The task is done — the chest can open." |
| "You CAN:" / "You CANNOT:" | "Permitted here:" / "Forbidden here:" |
| "Choose one reward before the next room." | "Choose one before the page turns." |
| "Choose one final reward before the run completes." | "Choose one final reward — the journal is nearly full." |
| recap "Time:" / "Rooms:" / "Bosses:" / "Items legal/illegal:" | "Time afoot:" / "Chapters: a of b" / "Bosses: a of b" / "Standing: all lawful" or "N forbidden" |
| "Relics (n):" | "Relics in pocket (n):" |

### In-game window
| Current | New |
|---|---|
| tabs RUN CONTROL/LIVE RUN/BUILD/ARTIFACTS/MODIFIERS/PROGRESSION | ACTIONS/THE ENTRY/THE BUILD/POCKETS/CURSES/THE TALLY |
| heading "REWARD READY" | "The Chest Opens" |
| pageTitle "The chest opens" (reward takeover) | *(kept)* |
| action tiles ("Complete Stage — Finish the active stage." etc.) | "Stamp the Chapter — the task here is done." / "Leave the Chest — pass on the spoils." / "Turn the Page — travel onward." / "Copy Recap — the tale, to the clipboard." / "Abandon Run — strike this journal." / "Reset — open a fresh journal." |
| rail "Floor: a / b" / "Cleared:" / "Legal/Illegal" | "Chapter: a of b" / "Stamped:" / "Lawful/Forbidden" |
| rail "Signal:" | "Noted:" |
| start tile "START RUN — Begin selected route — Click to launch" | "Begin the Run — sign and stamp — the route is drawn" |
| reward titles "CLAIM YOUR RELIC"/"ROLL SUPPLIES"/"CHOOSE AN UNLOCK"/"UNLOCK BANK ITEM" | "The chest holds a relic"/"The chest holds supplies"/"The chest holds a key"/"The chest holds bank spoils" |
| reward subtitles | "choose one — the rest crumble to dust" (all chest types) |
| overlay "Confirm Choice" stamp | "Take It" |
| overlay "Skip Reward" link | "or leave the chest…" |
| overlay "Selected: X" | "« X calls to you »" |

### Out of scope this pass
- Core strings (run-loop notes, RecapExport markdown, legality reasons) — a later "core
  voice" pass, since recap exports are shared artifacts and tests pin them.
- RuneLite config panel text (kept plainly functional on purpose).
- Custom-builder widget window deep copy (functional today; revisit with playtests).

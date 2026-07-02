# RogueScape — gameplay design & lever catalog

Planning only; no code. Balance **values are deferred to playtest** — this doc locks the *structure*
and the *levers*, not the numbers.

## Grill-session decisions (locked 2026-07-02) — collections taxonomy & the journey spread

The three things a run accumulates, split by **domain** (not mechanics):

- **UPGRADES = equipment (and, in Boss Ladder, supplies).** The build's kit story, one tall list.
  - *Boss Ladder:* ladder steps in **four lanes: Weapon / Armour / Jewellery / Supplies**
    ("May now wield rune-grade weapons", "May now bring better supplies"). A lane raise is an
    upgrade, never a relic. Weapon/Armour/Jewellery tier = equip-level requirement; the Supplies
    lane needs its own grading scale (consumables have no equip req) — authoring/playtest data.
  - *Scavenger:* the gear you **found & kept** in rooms (the four-category collection log);
    supplies are simply **whatever you can find** — no supply upgrades in Scav.
- **RELICS = everything that isn't equipment.** Named artifacts, each a binary permit: prayer
  back, a high prayer (Piety/Rigour/Augury), spellbook swap, bank/GE/trade back, teleports,
  food/potions permitted, inventory slots (Deep Pockets stays a relic). The old separate
  bank/prayer/potion "unlock" reward type is **retired — those are relics now**.
- **CURSES = setup burdens** (unchanged): chosen before the run, add restrictions, score.
- **Supplies:** in Boss Ladder they are an upgrade lane (above); in Scavenger they are just
  items — tracked in the tally, no panel. The *permission* to use a consumable class at all
  (food allowed, potions allowed) remains relic territory in both modes.
- **No slot caps** on relics/curses — pockets and strips grow/scroll; the sketch counts were
  incidental.
- **Relics in Scavenger** get their footing from (a) combat-toggle locks Scav starts with —
  prayers and spellbook are locked until relics/shrines open them — and (b) **mid-run curse
  accumulation** (cursed chests / event rooms add restrictions during the run) so later drafts
  always have something to ease.

**The journey spread** (the Run phase's two pages, from ref/current journey info.kra):
- *Left page — what I have:* the **Upgrades** list (tall), the **Relic** pockets (column of wax
  seals), the **Curses** strip (bottom row of seals).
- *Right page — the journey:* "Route —" title; the **boss line-up band** (current boss unlocked,
  the rest padlocked; Boss Ladder gets a side arrow to scroll the ladder; Scavenger may show the
  boss portrait during its boss fight purely for flavour); **Room [current]**; the **Info** block
  under it carries the objective + the rules of this place.

## Grill-session decisions (locked)
- **The whole game = restrictions + RNG.** Everything reduces to those two.
- **Rise of POWER, not challenge.** A run **starts shackled** (restrictions on) and relics **earn back
  freedom.** This is the intended feel and it makes relics-as-removers meaningful.
- **Item source detection** = *context, not heuristics*: on pickup, check **you're in the right area**
  and the item's **provenance** (was it a monster drop / gathered / crafted / bought) matches the
  objective. Simpler than a legality engine.
- **Gear-tier cap = equip-level requirement.** "No armour above 60 Defence req" — a tiny general rule
  using item equip reqs. (Name-matching by material is a fallback for the few top tiers.)
- **Boss Ladder has a PREP phase** between bosses: bank/GE temporarily allowed to gear up; on entering
  the fight the plugin **validates the full worn+carried loadout** against the run's restrictions,
  then re-locks.
- **Death ends the run.** (OSRS item loss on death is a separate, real consequence.)
- **Cross-room dependency soft-locks are accepted as "risk you took"** — *provided the player had the
  chance*. Runs that are impossible from the start are the thing to avoid, not risks you can blow.
- **Room content curation is the player's job** (owner has ideas in progress).
- **MVP = Boss Ladder first** (expected most popular; also the tighter first slice — see MVP section).

## CORE CONSTRAINT — read this first (it shapes everything)
RogueScape **cannot change underlying RuneScape gameplay.** No % healing, no damage multipliers, no
stat edits — RuneLite can't do that. The entire design space is **restrictions and the easing of
restrictions**:

- You take the base game and **add restrictions** (self-imposed or plugin-forced).
- The plugin **enforces** them: it **blocks** the disallowed action, or **fails the run** if you do
  something you aren't allowed to do. (This is *why* legality was removed — it's a straight blocker
  now, not a judge.) **Not an honour system.**
- **Relics are restriction-REMOVERS**, never power-adds. A relic lets you *use something you otherwise
  couldn't* (eat food, use prayer, equip a higher tier, carry more, use a second combat style…).

So RogueScape is a **subtractive roguelike**: you start shackled and earn back permissions. The power
fantasy is going from heavily-restricted to free. Keep reminding me of this — every mechanic must be
expressible as *add a restriction*, *ease a restriction*, or *block/fail on a restriction*.

## The two modes
- **Scavenger** — power is **FOUND** in rooms. Build a character from scratch.
- **Boss Ladder** (was "Rewarded") — climb a boss ladder; rewards **ease restrictions / unlock gear
  tiers** that you then self-source (GE/bank). No climax needed — the climax *is* doing hard OSRS
  content; payoff is **cosmetics** + proving skill (great for YouTubers/races).

## The build = the shape of what you've un-shackled
This replaces "relic stat synergy" (impossible here). Depth comes from **which permissions you unlock
and in what order**. Synergy = **permission combos** that enable a strategy:
- Unlock Prayer + Unlock Food → you can tank a harder fight.
- Unlock a Ranged weapon + Unlock Ammo → ranged becomes viable at all.
- Raise gear-tier cap + Unlock a teleport → reach and use better gear from a farther room.

**The thing worth designing carefully is the restriction/permission vocabulary** — a clean set of
restrictions and the relics that ease them, chosen so combos matter. This is pure-core and testable.

Example restriction vocabulary (each is block-or-fail enforced):
no bank/trade/GE · no prayer (or specific prayers) · no food / no potions · no teleports · gear-tier
cap · inventory-slot limit · stay in room region · one combat style · no shield · no ammo/runes…
Each relic eases exactly one (unlock prayer, +N slots, raise tier cap, unlock food, bank once…).

## Enforcement: two tools, no honour system
- **Block** — the action never happens (grey out / cancel the menu entry: bank, trade, GE, leaving the
  region, picking up over a slot limit, equipping above the tier cap).
- **Fail** — some things can't be pre-blocked, so detection ends the run (you ate when food was
  banned → run over). Same deterrent, different timing.
- A few things are genuinely hard to police (true real-time combat-style policing). Where pre-block is
  impossible, use fail-on-detection. Flag those per restriction when authoring.

## Rewards — one mechanic, different pools (unchanged)
A reward node = draft **1 of N**. Pool differs by mode:
- **Scavenger** = relics (permission unlocks). Gear is scavenged from rooms, so the draft is pure
  un-shackling. No separate loot chest — the chest *is* the relic draft.
- **Boss Ladder** = gear-tier unlocks + relics, mixed. The unlock is "you may now use addy"; you then
  source the item yourself. RNG-hope lives here; coins/rerolls soften it.

## Rooms & tension (all inside the restriction paradigm)
- **Collection rooms.** Objective = collect N of a category. **Categories are for tracking**; only one
  may be offered and that's fine. Counts differ per category and need multipliers — 1 weapon ≈ 2
  armour ≈ 5 crafts ≈ 10 supplies, and ammo/runes counted far higher (10 runes is nothing). The
  Supplies→Crafting chain (carry bars, smith pieces next room) lives here. (Balance later.)
- **Branching route.** Classic map: start node → choose among routes; some routes reach the shop/shrine
  and some don't. Adds the hard choice you (rightly) wanted. **Caveats you flagged:** many OSRS spots
  have nothing → curation is hard, and branches can **soft-lock** a run. Mitigations below.
- **Competing objectives.** A room offers two objectives you can't both finish (grab the weapon *or*
  the armour) — a real fork even within one room.
- **Shrine room.** An OSRS altar/shrine location (prayer altar, Dark Altar, a statue). You start with
  high-tier toggles **locked** and unlock them here — a perfect subtractive fit:
  - **Prayer shrine** → unlock a high prayer, one per style: **Piety** (melee), **Rigour** (ranged),
    **Augury** (magic). Until unlocked, the plugin blocks/fails activating it.
  - **Magic altar** → switch/unlock your **spellbook**: **Standard, Ancient, Lunar, Arceuus** (all
    four). Style-switching is a real in-game lever, so "which book you're allowed" is a clean
    restriction, and the altar is where you change it.
  - General shrine → ease any one restriction for the rest of the run, or swap a restriction for a relic.
- **Elite room.** Same as a collection room but with an extra restriction active *only here* (no food
  this room, tight timer) for a better reward. A self-contained risk node.
- **Event room.** A pure decision node (OSRS "random encounter" flavour): take the cursed path (add a
  permanent restriction) for a reward, or the safe path for nothing.
- **Shop / gamble room.** Spend coins: reroll a draft, buy a specific permission, or **gamble** — pay
  coins for a random permission (could be great or junk). You like "pick the shop instead of one more
  upgrade" — so visiting the shop *is* the gamble: trade a guaranteed upgrade for a coin spin.

### Push-your-luck — concrete examples (restriction paradigm)
The "bust" has teeth because breaking a restriction **ends the run**.
- **Stack the curse, stack the loot.** A reward node offers the safe relic, or **open the cursed
  chest**: a random relic *plus* a new permanent restriction. Keep opening for more relics — each adds
  a restriction — stop before you can't function.
- **Go deeper.** Append an optional extra room with a stacked restriction (no food + tier-locked) for a
  big reward. Decline freely; accept and a slip ends the run.
- **Self-imposed timer dare.** A room has no timer by default; opt to add a tight one → beat it for a
  bonus relic, miss it and forfeit the room's reward.
- **Coin wager.** At the gamble room, wager coins on a constrained kill (do it one-handed / no prayer);
  win doubles coins + a relic, lose forfeits the coins.

## Soft-lock risk (real constraint) + mitigations
OSRS rooms are uneven and branches/random nodes can strand a player. Mitigations:
- **Validated generation** — the route builder guarantees every branch is completable for the current
  start tier/restrictions before offering it.
- **Bail option** — abandon a room you can't finish (at a coin/score cost) without ending the run, so
  you're never hard-stuck.
- **Curated > random for quality.** Curated runs = a **campaign**; the **endless run** is fully random
  and accepts more jank. Random mode is "select the right rooms," not "any room."

## Levers (data, not hardcoded — Custom mode edits them, the briefing shows them)
- **Global:** run length, time limits, seed, **start tier (None/Low/Med/High)**, reward cadence,
  reward magnitude, floors (a pacing lever, not a commitment), draft size, coin/reroll rates.
- **Scavenger:** area(s), room count & size, allowed categories per room, woven skilling tasks,
  per-category collection counts + multipliers.
- **Boss Ladder:** boss list/order/count, per-boss reward weighting, gear-tier ladder per category,
  per-boss restrictions.
- **Curses:** the restriction toolkit — setup-only, stackable, each scores. Families: resource / gear /
  combat-mechanical (CA-style) / movement-room.

## Coins & rerolls (RNG mitigation)
Earned from **activities** (doing the tasks/skilling) + speed. Spend on rerolls (escalating cost),
chosen relics, gambles. Never disables curses.

## Cosmetics — the meta-progression / payoff
Completing runs (esp. the Boss Ladder) unlocks **cosmetics** (a hat, a title, a stamp on the journal).
This is the reason to finish hard content and the closest thing to a "climax." Plugin-Hub-safe:
cosmetics are plugin-side flair, not game items.

## Grinds & CAs — a separate use-case, NOT the spine
The normal (non-custom) modes are for **fun** and are not built around grinding. The bigger *hook* is
that **Custom mode** lets people structure real **skilling grinds** (Scavenger) and **combat
achievements** (Boss Ladder) as runs. Keep this a clearly separate section/use-case — don't let it
make the default experience feel like a chore tracker.

## Curse randomness (optional, later)
Default curses are self-imposed (chosen). Optionally add a **wildcard / daily** mode that randomly
deals a curse loadout for bonus reward — fun variety without muddying the default.

## Deferred to playtest (exist as adjustable knobs; values TBD)
Relic cadence, room/boss counts, floors-or-not per run, reward magnitudes, per-category counts &
multipliers, coin/reroll numbers, curse weights.

## Confirmed this round
- Design is **subtractive**: restrictions + easing them; relics = restriction-removers; enforcement is
  **block or fail**, not honour system.
- "Rewarded" is now **Boss Ladder**; it needs no climax — cosmetics are the payoff.
- Branching + competing objectives are in (with soft-lock mitigations).
- Categories exist mainly for **tracking**; per-category counts/multipliers are a balance lever.
- Grinds/CAs are a **separate use-case**, not the centre of the normal modes.
- **Cosmetic unlock system** is in scope as meta-progression.
- **All room types are ACCEPTED and in scope:** collection, branching route, competing objectives,
  **Shrine** (prayer-tier unlocks Piety/Rigour/Augury + spellbook switch Standard/Ancient/Lunar/
  Arceuus), **Elite** (extra local restriction for better reward), **Event** (cursed-path decision
  node), **Shop = gamble** (reroll / buy permission / random spin instead of a guaranteed upgrade).
- **All push-your-luck mechanics are ACCEPTED and in scope:** cursed chest (relic + new restriction,
  keep opening), go-deeper (optional stacked-restriction room), self-imposed timer dare, coin wager.

## Gaps reassessed (after the restriction-paradigm correction)
- *In-room decisions* — addressed by branching + competing objectives + push-your-luck. Good.
- *Relic synergy* — reframed: synergy = **permission combos**, not stats. The vocabulary is the work.
- *Stakes* — addressed: breaking a restriction **ends the run**; cosmetics give a reason to finish.
- *Climax* — accepted as unnecessary; cosmetics + hard content are the payoff.
- *Room variety* — addressed by shrine/elite/event/shop/gamble types.
- *Still open:* curation cost (OSRS has empty places) and soft-locks — managed by validated generation,
  bail option, and curated-over-random.

## MVP — Boss Ladder first, in small chunks
Boss Ladder is the tighter first slice: it leans on the restriction spine + boss-kill detection and
**sidesteps the hard Scavenger problems** (room curation, pickup provenance, crafting detection). Goal
of the MVP: *fight a short boss ladder under restrictions, with a prep phase and restriction-easing
rewards, and have it feel like a run.*

Chunks (each small; ✅ = pure-core & testable here, 🔌 = needs the live client):
1. ✅ **Curses → RunRestrictions.** A small curse catalog; each curse = "adds these restrictions."
   A run's starting `RunRestrictions` is assembled from the chosen curses + start tier.
2. ✅ **Relics as restriction-removers.** A small relic pool where each relic = `permit(...)` / raise a
   cap. Retire the old stat-flavored relic effects.
3. ✅ **Boss Ladder run state machine.** Ordered bosses; PREP → FIGHT → REWARD → next. PREP permits
   bank/GE; FIGHT enforces restrictions; kill → reward.
4. ✅ **Reward draft over restrictions.** Draft 1-of-N: a card either eases a restriction (relic) or
   raises the gear-tier cap (unlock). Picking mutates `RunRestrictions`.
5. ✅ **Gear-tier model (equip-level based).** Tier = max equip-level requirement; cap check is pure.
6. 🔌 **Loadout validation gate.** At PREP→FIGHT, read worn+carried items and validate against the
   restrictions (tier cap, allowed supplies, spellbook/style). Fail or block entry.
7. 🔌 **Enforcement wiring.** `RunRestrictions.decide()` → menu block / run-fail in the plugin.
8. 🔌 **Boss-kill detection.** Reuse `BossKillChatMatcher`.
9. 🔌 **Boss Ladder briefing + live UI.** The ladder, current boss, active restrictions, prep vs fight.

Build order: 1 → 2 → 3 → 4 → 5 (all pure-core, land + test here), then 6–9 against a local client.

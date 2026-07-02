# RogueScape — The Journal: two-page spread layout

Status: **accepted direction, not yet built.** This captures the consolidated "open book"
theme so implementation has a shared target.

## The idea

Every screen is an **open journal — a two-page spread**: a left page and a right page divided by a
central spine. There are no (or minimal) tabs; the run *phase* decides what each page holds.

One consistent rule keeps it readable:

> **Left page = "what I have / my choices."  Right page = "the world / the route / context."**

The renderer already supports this: `RogueScapeWindowOverlay.Block.columns(left, right)` draws two
side-by-side block columns, and `RogueScapePaper` provides the page texture, ink rules, wax seals,
stamps, ribbon bookmarks, and tally marks. The job is to make each phase render as **one spread**
instead of a tab strip of single columns.

## The four spreads

### 1. Contract (pre-run / lobby)
- **Left:** the run options (Scavenger / Boss Ladder / Custom) as selectable entries, a **preset**
  selector at the bottom, and a **Begin** stamp.
- **Right:** the live briefing of the currently-selected route — the exact rooms, the rules, and
  win/lose. Updates as the selection changes. (This is the existing `RunBriefing`.)

### 2. The Run (travel / room / boss active)
- **Left — what I have:** upgrades picked, relics in pocket, run stats (score, items, time).
- **Right — the route:** rooms cleared (stamped with the ribbon on the current one), the current
  room + its task/progress, and the rooms still to come. The collapsed **"rules of this place"**
  line lives here under the current room.

### 3. The Reward (upgrade pick)
- **Left:** the three reward options as cards + a **Choose** stamp.
- **Right:** context for the choice — previously picked upgrades/relics and running info; and when a
  card is focused, **what that reward can give** (its potential drops/effect). Click a card → the
  right page details it.

### 4. The Recap (run complete / failed)
- **Left:** run stats — score, items, time, bosses, outcome.
- **Right:** the route log — which rooms were reached, time per room, where it ended — plus a
  **Rerun / New page** stamp.

## Shared affordances (reuse the paper toolkit)
- **Stamps** for the one primary action per spread: Begin, Stamp the Chapter, Choose, Rerun. This
  also resolves the "Actions tab" question — the action sits on the page it belongs to.
- **Ribbon bookmark** marks the current room in the route log.
- **Tally marks** (gate-five) for score; **wax seal** for a completed run; **pockets** for relics.

## Confirmed
- **Tabs: removed entirely.** The in-game window is bare-bones — only the live play spread, nothing
  else.
- **The sidebar stays** as a simple, glanceable live tracker (room you're in, mode, basic progress)
  so you can follow along without opening the big window — and it also holds configuration / options
  / route-building / map selection. Exact look TBD. See `roguescape-next-phase-plan.md` §5.

## Open decisions (confirm before building the overlay)
2. **Window width:** a real two-page spread needs horizontal room — is the current window wide
   enough, or do we widen it / enforce a min width?
3. **Reward "potential drops":** we have reward *labels* today (relic/supply/unlock drafts) but no
   per-reward drop table. Showing "what can drop" needs a data source — is that this scope or later?
4. **Action placement:** confirm the primary action moves onto the spread (no separate Actions tab).

## Suggested build order
- **A. Data contract (pure core, unit-tested):** extend the view model to emit explicit
  `leftPage` / `rightPage` content per phase. No RuneLite; fully testable. This is the safe
  foundation and can land before any visual work.
- **B. Overlay render:** draw a single two-page spread from `leftPage`/`rightPage` via `columns()` +
  `RogueScapePaper`; retire the tab strip.
- **C. Per-phase polish:** ribbon/stamp affordances, the reward drop preview, the recap route log.

> Note: the overlay (`RogueScapeWindowOverlay`, `RogueScapeWindowContent`) is the RuneLite layer,
> which cannot be compiled in the headless dev sandbox (the RuneLite Maven repo is egress-blocked).
> Step A is verifiable there; steps B/C need a local Gradle build to confirm.

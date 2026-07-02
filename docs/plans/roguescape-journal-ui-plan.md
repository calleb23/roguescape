# RogueScape — Journal UI: Mockup-Fidelity Plan (2026-06-11)

Goal: take the in-game UI from "paper palette swap" (current state, commit `4c3642b`)
to **100% of the approved journal mockups**. Wording polish is deferred to the final slice.

**The mockups are the contract** and live in `docs/roguescape/mockups/`
(`concept-journal-panel.png`, `concept-journal-live.png`, `concept-journal-reward.png`,
`concept-journal-longrun.png`), together with `PaperConcepts.java` — the throwaway
Graphics2D program that generated them. Whoever implements a slice should open the
matching PNG, build, re-render the real previews (`build/ui-preview/*.png`), and compare
side by side before committing. The generator doubles as a reference implementation for
every visual element (seals, stamps, chapter lines, tally marks, pockets).

**Environment note:** remote sessions must bootstrap the local RuneLite client first —
follow `docs/rules/remote-runelite-toolchain.md`, then build with
`./gradlew test -PruneLiteVersion=1.12.29-SNAPSHOT`. S1 is complete (commit `6eb68cc`).

## Gap analysis — mockup vs current

| # | Mockup element | Current state | Surface |
|---|---|---|---|
| 1 | Big serif header + italic subtitle + red wax seal, no colored band | Old header bar w/ crest + ON | Panel |
| 2 | Section headers are centered ink `— TITLE —` rules (no bars, no ✦) | Tan CollapsibleSection bars | Panel |
| 3 | Contracts: paper cards w/ wax-seal icon, tack pin, slight tilt, red CHOSEN ring + stamp | Flat bordered text cards | Panel |
| 4 | Chapters: numeral + dotted leader + strike-through when done + tilted CLEAR stamp + ribbon at current + dotted slot ahead; boss = red "Final." | Plain route text rows | Panel + window |
| 5 | Seed as handwriting over a dotted line | Boxed text field | Panel |
| 6 | BEGIN THE RUN: large tilted red rubber stamp | Green stamp-bordered button | Panel |
| 7 | Ledger rows: `label ……… value` dashed leaders; score as tally marks | Plain stat rows | Panel + window |
| 8 | Relic pockets: stitched dashed slots holding wax seals | Text list | Panel + window artifacts |
| 9 | Aged-paper texture visible across the panel | Flat paper color (texture only in window) | Panel |
| 10 | Live page = "Chapter II — Canifis" entry: ribbon bookmark, diary objective, ✓/✗ rule margin notes, THE RECORD list, hourglass, ABANDON stamp | Generic tab blocks | Window |
| 11 | Reward = two-page spread: tall loot cards (seal top, ink sketch, desc) on left page, ledger on right | Tab w/ 3 banner cards | Window + reward overlay |
| 12 | Long runs: windowed chapter list + passport stamp grid (68+ slots, bosses red) | Nothing | Window + panel |

## Progress (Opus 4.8 session)

- S1 panel canvas — DONE (`6eb68cc`)
- S2 contracts — DONE (`7f8cb64`): ui/ContractCard, wax seals + CHOSEN stamp.
- S3 chapters — DONE (`bb7b425`): SidePanelViewModel.Chapter + ui/ChapterList,
  table-of-contents + passport grid, wired as a panel Chapters section.
- S4 ledger/pockets — DONE (`d126432`): LedgerRow leaders, tally score, seed line,
  PocketStrip in ARTIFACTS.
- S5 stamps — DONE (`7c5b623`): ui/StampButton for all loud actions.
- S6a window chapters — DONE (`a05a0f2`): CHAPTERS block kind, THE RECORD in the live tab.
- S6b loot cards — DONE (`6a2c68e`): seal-topped paper reward cards.
- S6c — DONE (`c90aee2` + follow-up): diary-entry live page (PAGE_TITLE ribbon masthead,
  COLUMNS two-column layout, HOURGLASS, NOTE; full ✓/✗ rules as margin notes; serif ink
  HEADING/TEXT across all tabs) and the reward overlay as a literal two-page spread (left:
  title + loot cards + red CHOSEN frame/stamp + Confirm stamp + skip link; right: The
  Ledger with leader lines, wax-dotted modifier notes, relic pockets). Campaign quick-pick
  chips are sealed notes with shrink-to-fit names. Also fixed: panel row alignment, section
  max-height stretching, and headless render staleness.
- S7 wording — DONE: docs/roguescape/wording-schema.md defines the journal voice (rules +
  lexicon: contracts/chapters/stamped/The Record/The Passport/The Ledger/The Hourglass/
  The Chest/pockets/curses/lawful-forbidden/the task) and the full current→new mapping,
  applied across the view model, panel, window content, reward overlay, and plugin
  signals; tests updated where they pinned old copy. Out of scope (documented in the
  schema): core run-note/recap-export strings, config panel text, custom-builder deep copy.
- **Remaining:** in-game screenshot validation by the user; contrast/tilt taste calls.

## Build slices (each: implement → render-verify → commit)

1. ~~**S1 Panel canvas**~~ **DONE** (`6eb68cc`) — paper texture painted by the panel (opaque-sweep of child rows),
   ink-rule section headers replacing CollapsibleSection bars (collapse stays, the title
   line is the toggle), journal header w/ wax seal. Kills gaps 1, 2, 9.
2. **S2 Contracts** — `ContractCard` component (custom-paint: seal, tack, tilt, CHOSEN
   ring/stamp), replaces mode cards; quick-pick chips become small sealed notes. Gap 3.
3. **S3 Chapters** — `ChapterList` painter (numerals, leaders, strike-through, CLEAR
   stamps, ribbon, dotted slots, red Final) fed by the live route; used in the panel
   route area and the window's THE RECORD; windowed view + passport grid for long runs.
   Gaps 4, 12.
4. **S4 Ledger & pockets** — leader-line stat rows, tally score, seed-on-a-line input,
   relic pocket strip (panel + window artifacts tab). Gaps 5, 7, 8.
5. **S5 Stamps** — BEGIN THE RUN / ABANDON RUN / TAKE IT as painted tilted stamps
   (custom JButton paint + overlay hit-rects). Gap 6.
6. **S6 Window pages** — live tab becomes the chapter-entry page (ribbon, diary line,
   rule margin notes, record column, hourglass); reward flow becomes the two-page
   spread with tall seal-topped loot cards (window + reward overlay). Gaps 10, 11.
7. **S7 Wording + polish pass** — copy review with the user, spacing/tilt tuning,
   in-game screenshot validation.

## Rules of the build

- Every slice re-renders the real-content previews; composition is judged against the
  mockups before committing.
- `RogueScapePaper` is the single painting vocabulary — new visuals get added there,
  not inlined.
- Wording stays as-is until S7 (user request).
- Scene overlays (room mask, world map) keep their current look until the journal core
  is done.

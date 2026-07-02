---
name: wrapRS
description: Session end sequence for RogueScape. Commits and pushes all work, overwrites docs/PROGRESS.md with a complete handover (state, locked decisions, in-flight work, next steps), and confirms it is safe to clear context. Run at the end of every RogueScape session before /clear.
---

# wrapRS — RogueScape session wrap-up

You are ending a RogueScape work session. Produce a handover so the next chat (with zero context)
can continue seamlessly. Work in `C:\Users\Caleb\Documents\GitHub\plugins\roguescape` (the ONE
canonical repo — github `calleb23/roguescape`, branch `main`).

## Steps, in order

### 1. Secure the work
- `git status` — if anything is uncommitted, run the test suite (`./gradlew test`); if green,
  commit with a proper message and push to `origin main`. If red, fix or commit to a WIP branch —
  never leave work only in the working tree, that is how sessions get lost.
- Confirm `main` is in sync with `origin/main`.

### 2. Overwrite docs/PROGRESS.md
Replace the whole file (do not append) with, in this order:

1. **Session date + one-line summary** of what this session accomplished.
2. **Commits this session** — hash + first line, oldest → newest.
3. **Current state of the plugin** — a short honest map: what works live, what is core-only,
   what is broken/ugly. Include test count.
4. **Decisions locked this session** — anything the user decided (design, naming, UI direction).
   If a decision belongs in `docs/plans/roguescape-gameplay-design.md`, make sure it was also
   written there (add it if it only lives in chat).
5. **In-flight / half-done** — anything started but not finished, with the exact file(s) and what
   remains.
6. **Next steps** — the agreed queue, most important first, each with enough context to start
   cold (file paths, the approach already chosen).
7. **Open questions for Caleb** — decisions the next session must ask before building.
8. **Gotchas** — non-obvious things the next agent must know (e.g. "the painted overlay cannot
   render 3D models — only the widget window can", "render previews land in build/ui-preview/",
   "ref/*.kra are Krita sketches — unzip mergedimage.png to view").

### 3. Sync the sketches
If `ref/` gained new `.kra` sketches this session, confirm they are committed and referenced in
PROGRESS.md (the UI is built from these).

### 4. Update auto-memory
Update the auto-memory project files if the repo/canonical situation or working rhythm changed
this session (do not duplicate what PROGRESS.md already records — memory is for cross-session
facts, PROGRESS.md is the work handover).

### 5. Commit the handover + confirm
- Commit PROGRESS.md (+ any doc updates) and push.
- End with a short message: the one-line session summary, the pushed HEAD hash, and
  "**Safe to /clear — next session: read docs/PROGRESS.md first.**"

## Style
- Honest over flattering: broken things get listed as broken.
- Terse: the next agent reads this cold; every line must earn its place (less is more —
  Caleb's standing instruction).

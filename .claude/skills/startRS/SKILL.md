---
name: startRS
description: Session start sequence for RogueScape. Reads the handover and design context, verifies the repo state, and declares the session focus. Run at the start of every RogueScape session before doing any work.
---

# startRS — RogueScape session start

You are starting a RogueScape work session with zero context. Orient BEFORE doing any work.
Repo: `C:\Users\Caleb\Documents\GitHub\plugins\roguescape` (the ONE canonical repo —
github `calleb23/roguescape`, branch `main`).

## Steps, in order

### 1. Read the handover
- `docs/PROGRESS.md` — the previous session's full handover: state, locked decisions,
  in-flight work, the agreed next-step queue, open questions, gotchas. This is the source of
  truth for "where we are".

### 2. Read the design context (skim, don't re-summarise)
- `docs/plans/roguescape-gameplay-design.md` — the master design; the **Grill-session decisions
  (locked)** sections at the top are binding.
- Any `ref/*.kra` newer than the last session (Krita sketches — unzip `mergedimage.png` to
  view). Sketches are THE UI contract.

### 3. Verify the repo
- `git status` + `git log --oneline -5` — confirm clean tree, `main` in sync with `origin/main`.
  If there IS uncommitted work, surface it to Caleb before anything else (it may be his).
- If the session will touch code: `./gradlew test` once to confirm the baseline is green, and
  note the test count.

### 4. Declare the session focus
Post a SHORT orientation (less is more):
- One line: where the project stands (from PROGRESS.md).
- The top item of the next-step queue and any open questions blocking it.
- Ask Caleb what this session's focus is — recommend the queue's top item as the default.

## Rules
- Do NOT start building until the focus is agreed.
- The permitted/forbidden vocabulary only — lawful/legal/illegal/suspicious are banned words.
- Commit + push after every meaningful chunk; end the session with /wrapRS.

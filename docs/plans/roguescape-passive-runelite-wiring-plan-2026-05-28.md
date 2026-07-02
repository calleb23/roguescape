# RogueScape Passive RuneLite Wiring Implementation Plan

> **For Hermes:** Use Claude Code for the implementation slices. Hermes should keep Claude on this plan, verify each slice independently, and stop Claude from expanding into side panel/web-app/content-library work.

**Goal:** Make the existing RogueScape/One Inventory pure Java run engine observe a real RuneLite inventory/region session passively and show live legality counts in the existing overlay.

**Architecture:** Keep the tested core independent from RuneLite. Add thin RuneLite adapter code in `OneInventoryRoguelikePlugin` plus tiny pure helper seams where needed. The first milestone is not a full side panel; it is an observe-only live loop: RuneLite inventory/region signal -> pure adapter/core -> overlay summary.

**Tech Stack:** Java 11 source compatibility, RuneLite client APIs, JUnit 4, Gradle, WSL Java 17 for builds.

---

## Current State Summary

Known-good baseline before this plan:

- Full tests pass: `582 tests, 0 failures, 0 errors, 0 skipped`.
- Main accepted product direction is RogueScape, still implemented under `plugins/oneinventory/...`.
- `OneInventoryRoguelikePlugin` currently starts a `OneInventoryRunSession`, creates `PluginIdeaHubOverlay`, and has a TODO for `ItemContainerChanged` wiring.
- Pure core already has:
  - `RogueScapeRun.applyItemDelta(...)`
  - `InventorySnapshot`
  - `InventoryDiff.positiveDeltas(...)`
  - `AdapterTranslator`
  - `OverlayViewModel`
  - `SidePanelViewModel`
- Missing live product behavior:
  - no `@Subscribe` handlers;
  - no `Client` injection;
  - no previous inventory snapshot;
  - no current region capture;
  - overlay does not display the newer `RogueScapeRun` legality counts.

## Hard Boundaries / Non-Goals

Do **not** build these in this slice:

- full Swing side panel;
- companion web app;
- online leaderboard;
- route solving or gameplay advice;
- menu manipulation/action blocking;
- new plugin folders;
- big rename from `oneinventory` to `roguescape`;
- large relic/reward content library;
- broad cleanup of the old deleted `src/main/java/com/pluginideahub/...` prototype tree.

This pass must remain Plugin Hub-safe: passive observation only.

---

## Verification Commands

Use WSL Java 17:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test --tests '*AdapterSeamTest' --tests '*RogueScapeRunTest' --tests '*OneInventoryRoguelikePrototypeTest'
./gradlew test
```

Optional dev-client smoke after compile/tests:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew run
```

Manual smoke target: enable the One Inventory/RogueScape plugin, gain an inventory item, and verify overlay count/log changes without the plugin blocking any action.

---

## Slice 0: Readiness Check / Guard Rails

**Objective:** Make Claude prove it understands the repo state before editing.

**Files:**
- Read: `CLAUDE.md`
- Read: `research/one_inventory_roguelike_end_state_roadmap.md`
- Read: `research/roguescape_overnight_handoff.md`
- Read: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`
- Read: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/core/RogueScapeRun.java`
- Read: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/core/adapter/InventoryDiff.java`
- Read: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/core/legality/InventorySnapshot.java`

**Step 1: Inspect current status**

Run:

```bash
git status --short
```

Expected: repo has many existing uncommitted migration changes. Do not revert, delete, or reorganize unrelated files.

**Step 2: Confirm baseline tests**

Run:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test --tests '*AdapterSeamTest' --tests '*RogueScapeRunTest' --tests '*OneInventoryRoguelikePrototypeTest'
```

Expected: PASS. If failing, stop and report exact failures before coding.

**Step 3: Report intended touched files**

Expected intended touch set for the first implementation pass:

- `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`
- maybe one small helper/test file under `plugins/oneinventory/src/test/java/com/pluginideahub/oneinventory/`
- maybe `research/roguescape_overnight_handoff.md` or this plan if documenting completion

No broad edits.

---

## Slice 1: Wrap Plugin Session in `RogueScapeRun`

**Objective:** Make the plugin entrypoint use the richer `RogueScapeRun` orchestrator while preserving existing overlay startup behavior.

**Files:**
- Modify: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`
- Test/Verify: existing tests first; add a focused test only if a suitable non-RuneLite seam is extracted.

**Step 1: Replace plugin field model minimally**

In `OneInventoryRoguelikePlugin`, keep:

```java
private OneInventoryRunSession runSession;
```

Add:

```java
private RogueScapeRun rogueRun;
```

Import:

```java
import com.pluginideahub.oneinventory.core.RogueScapeRun;
```

**Step 2: Wrap after starting the existing session**

In `startUp()` after:

```java
runSession = OneInventoryRunSession.start(config.goalText(), seed, mode);
```

add:

```java
rogueRun = RogueScapeRun.wrap(runSession);
```

Do not remove `runSession`; keep legacy overlay continuity.

**Step 3: Clear both on shutdown**

In `shutDown()` add:

```java
rogueRun = null;
```

near `runSession = null;`.

**Step 4: Compile/test**

Run:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test --tests '*OneInventoryRoguelikePrototypeTest'
```

Expected: PASS.

---

## Slice 2: Add a RuneLite Inventory Snapshot Helper

**Objective:** Convert RuneLite inventory containers into the existing pure `InventorySnapshot` type.

**Files:**
- Modify: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`

**Step 1: Add fields/imports**

Likely imports:

```java
import com.pluginideahub.oneinventory.core.legality.InventorySnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
```

Inject:

```java
@Inject
private Client client;
```

Add field:

```java
private InventorySnapshot previousInventorySnapshot = new InventorySnapshot();
```

**Step 2: Add helper method**

Add a private method near the bottom of `OneInventoryRoguelikePlugin`:

```java
private InventorySnapshot currentInventorySnapshot()
{
	ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
	Map<String, Integer> quantities = new LinkedHashMap<>();
	if (inventory == null)
	{
		return new InventorySnapshot(quantities);
	}

	for (Item item : inventory.getItems())
	{
		if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
		{
			continue;
		}
		String itemId = String.valueOf(item.getId());
		quantities.put(itemId, quantities.getOrDefault(itemId, 0) + item.getQuantity());
	}
	return new InventorySnapshot(quantities);
}
```

Use item IDs as snapshot keys. Do not try to look up names in this slice unless the RuneLite API method is already obvious and compile-safe.

**Step 3: Initialize on startup**

At the end of `startUp()` after `rogueRun` is initialized:

```java
previousInventorySnapshot = currentInventorySnapshot();
rogueRun.setStartSnapshot(previousInventorySnapshot);
```

**Step 4: Clear on shutdown**

In `shutDown()`:

```java
previousInventorySnapshot = new InventorySnapshot();
```

**Step 5: Compile/test**

Run:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test --tests '*OneInventoryRoguelikePrototypeTest'
```

Expected: PASS. If RuneLite API names differ, adjust only this helper.

---

## Slice 3: Add Passive `ItemContainerChanged` Wiring

**Objective:** On inventory changes, compute positive item gains and feed them to `RogueScapeRun.applyItemDelta(...)`.

**Files:**
- Modify: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`

**Step 1: Add imports**

Likely imports:

```java
import com.google.common.eventbus.Subscribe;
import com.pluginideahub.oneinventory.core.adapter.InventoryDiff;
import com.pluginideahub.oneinventory.core.legality.ItemDelta;
import com.pluginideahub.oneinventory.core.legality.ProvenanceHint;
import java.util.List;
import net.runelite.api.events.ItemContainerChanged;
```

**Step 2: Add subscriber skeleton**

Add:

```java
@Subscribe
public void onItemContainerChanged(ItemContainerChanged event)
{
	if (rogueRun == null)
	{
		return;
	}
	if (event.getContainerId() != InventoryID.INVENTORY.getId())
	{
		return;
	}

	InventorySnapshot nextSnapshot = currentInventorySnapshot();
	List<ItemDelta> deltas = InventoryDiff.positiveDeltas(previousInventorySnapshot, nextSnapshot, ProvenanceHint.UNKNOWN);
	for (ItemDelta delta : deltas)
	{
		rogueRun.applyItemDelta(delta);
	}
	previousInventorySnapshot = nextSnapshot;
	refreshOverlaySummary();
}
```

If `InventoryID.INVENTORY.getId()` does not compile against this RuneLite version, inspect the RuneLite API or existing examples and use the correct container ID comparison. Do not broaden to all containers.

**Step 3: Add `refreshOverlaySummary()` helper**

Add a private helper:

```java
private void refreshOverlaySummary()
{
	if (uiModel == null || runSession == null || rogueRun == null)
	{
		return;
	}
	uiModel.recordManualAction(
		"Items legal/suspicious/illegal: "
			+ rogueRun.legalCount()
			+ "/"
			+ rogueRun.suspiciousCount()
			+ "/"
			+ rogueRun.illegalCount()
	);
	uiModel.recordManualAction(runSession.overlaySummary());
}
```

If this spams overlay notes too hard, simplify to a single latest summary line using the existing UI model API after inspecting `PluginIdeaHubUiModel`.

**Step 4: Compile/test**

Run:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test --tests '*AdapterSeamTest' --tests '*RogueScapeRunTest' --tests '*OneInventoryRoguelikePrototypeTest'
```

Expected: PASS.

---

## Slice 4: Preserve Item Names Where Safe

**Objective:** Improve overlay/item logs by using RuneLite item names instead of numeric IDs when the API supports it cleanly.

**Files:**
- Modify: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`
- Prefer no new core changes unless necessary.

**Step 1: Inspect available item-name API**

Look for current RuneLite method availability. Likely options:

```java
client.getItemDefinition(item.getId()).getName()
```

**Step 2: If compile-safe, add helper**

```java
private String itemName(int itemId)
{
	try
	{
		return client.getItemDefinition(itemId).getName();
	}
	catch (RuntimeException ex)
	{
		return String.valueOf(itemId);
	}
}
```

**Step 3: Do not force this into `InventorySnapshot`**

`InventorySnapshot` is keyed by ID only. If names are needed in `ItemDelta`, build deltas manually in the plugin or add a tiny pure helper that maps IDs to names without pulling RuneLite into core.

Recommended small approach:

- keep `InventoryDiff` for ID/quantity math;
- before applying each delta, replace with:

```java
ItemDelta namedDelta = new ItemDelta(
	delta.itemId(),
	itemName(Integer.parseInt(delta.itemId())),
	delta.quantity(),
	currentRegionNote(),
	delta.provenanceHint()
);
rogueRun.applyItemDelta(namedDelta);
```

Guard `NumberFormatException` by falling back to original `delta`.

**Step 4: Compile/test**

Run targeted tests again.

---

## Slice 5: Add Region Capture on Game Tick

**Objective:** Keep `RogueScapeRun.currentRegionId` updated from the local player's `WorldPoint`.

**Files:**
- Modify: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`

**Step 1: Add imports**

Likely imports:

```java
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
```

**Step 2: Add subscriber**

```java
@Subscribe
public void onGameTick(GameTick tick)
{
	if (rogueRun == null || client == null || client.getLocalPlayer() == null)
	{
		return;
	}
	WorldPoint location = client.getLocalPlayer().getWorldLocation();
	if (location == null)
	{
		return;
	}
	rogueRun.moveToRegion(String.valueOf(location.getRegionID()));
}
```

Do not add region route-solving or advice. This only records the current region ID.

**Step 3: Add location note helper**

Optional helper for item events:

```java
private String currentRegionNote()
{
	return rogueRun == null || rogueRun.currentRegionId().isEmpty()
		? "unknown region"
		: "region " + rogueRun.currentRegionId();
}
```

**Step 4: Compile/test**

Run targeted tests, then full tests.

---

## Slice 6: Game State Reset Safety

**Objective:** Avoid stale snapshots across login/hop/logout transitions.

**Files:**
- Modify: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`

**Step 1: Add imports**

Likely imports:

```java
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
```

**Step 2: Add subscriber**

```java
@Subscribe
public void onGameStateChanged(GameStateChanged event)
{
	if (event.getGameState() == GameState.LOGGED_IN)
	{
		previousInventorySnapshot = currentInventorySnapshot();
		if (rogueRun != null)
		{
			rogueRun.setStartSnapshot(previousInventorySnapshot);
		}
	}
}
```

Do not auto-fail/reset the run here yet; just prevent stale false deltas.

**Step 3: Compile/test**

Run targeted + full tests.

---

## Slice 7: Minimal Focused Unit Test for Plugin-Free Snapshot Logic if Extracted

**Objective:** Only if Claude extracts a pure helper for snapshot/delta naming, test it. Do not write brittle tests around RuneLite `Client` mocks unless cheap and stable.

**Files:**
- Maybe create: `plugins/oneinventory/src/test/java/com/pluginideahub/oneinventory/core/adapter/InventorySnapshotTranslatorTest.java`

**Rule:** If no pure helper is extracted, skip this slice. Existing `AdapterSeamTest` already covers `InventoryDiff`.

**Test shape if helper exists:**

```java
@Test
public void translatesPositiveInventoryDeltasWithStableNames()
{
	// before: item 100 x1
	// after: item 100 x3, item 200 x1
	// expect two positive deltas with quantities 2 and 1
}
```

Run:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test --tests '*InventorySnapshotTranslatorTest' --tests '*AdapterSeamTest'
```

---

## Slice 8: Overlay Output Tightening

**Objective:** Make the existing overlay visibly prove live tracking without building the full side panel.

**Files:**
- Modify: `plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java`
- Maybe inspect: `src/main/java/com/pluginideahub/ui/PluginIdeaHubUiModel.java`

**Step 1: Inspect UI model API**

Read:

```text
src/main/java/com/pluginideahub/ui/PluginIdeaHubUiModel.java
```

**Step 2: Prefer stable compact overlay lines**

Final overlay should show some combination of:

```text
Goal: ...
State: ACTIVE
Region: <id or unknown>
Items L/S/I: 3/1/0
Latest: <item name> x<count> <legality>
```

If `PluginIdeaHubUiModel` cannot support stable lines without note spam, create a small plugin-local `List<String> overlayLines()` supplier and pass that to `PluginIdeaHubOverlay` instead of `uiModel::overlayLines`.

**Step 3: Do not overbuild**

No tabs, no Swing panel, no export buttons yet.

**Step 4: Compile/test**

Run full Gradle.

---

## Slice 9: Documentation Receipt

**Objective:** Update handoff state so future Claude sessions do not redo or misunderstand this pass.

**Files:**
- Modify: `research/roguescape_overnight_handoff.md` or add a small dated section to a new handoff doc if preferred.

**Add a compact note:**

```markdown
### Stage 6 live RuneLite wiring pass — DONE/PARTIAL

Implemented:
- `Client` injection in `OneInventoryRoguelikePlugin`;
- inventory snapshot initialization;
- `ItemContainerChanged` listener for inventory gains only;
- positive delta forwarding to `RogueScapeRun.applyItemDelta`;
- `GameTick` region capture;
- `GameStateChanged` snapshot reset;
- compact overlay legality counts.

Verification:
- targeted tests: ...
- full `./gradlew test`: ...

Manual smoke:
- not run / run with result ...

Deferred:
- full Swing side panel;
- richer bank/trade/GE/death provenance;
- manual approval UI;
- web app.
```

---

## Claude Code Execution Prompt

Use this prompt for the first Claude Code implementation run. Keep it as one bounded pass; if Claude drifts, stop after Slice 3 and verify.

```text
You are working in /mnt/c/Users/Caleb/Documents/GitHub/PluginIdeaHub.

Task: Implement the first passive RuneLite wiring milestone for RogueScape / One Inventory. Follow docs/plans/roguescape-passive-runelite-wiring-plan-2026-05-28.md.

Read first:
- CLAUDE.md
- research/one_inventory_roguelike_end_state_roadmap.md
- research/roguescape_overnight_handoff.md
- docs/plans/roguescape-passive-runelite-wiring-plan-2026-05-28.md
- plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/OneInventoryRoguelikePlugin.java
- plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/core/RogueScapeRun.java
- plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/core/adapter/InventoryDiff.java
- plugins/oneinventory/src/main/java/com/pluginideahub/oneinventory/core/legality/InventorySnapshot.java

Implement only Slices 1-6, then do Slice 8 if it stays small. Slice 7 is optional only if you extract a pure helper worth testing. Slice 9 should be a compact handoff note.

Hard boundaries:
- Passive observation only. No blocking, no menu manipulation, no route solving, no gameplay advice.
- Do not build the full side panel.
- Do not build the web app.
- Do not rename oneinventory to roguescape.
- Do not clean/revert unrelated migration changes or deleted old prototype files.
- Do not create new plugin folders.
- Keep changes focused mainly to OneInventoryRoguelikePlugin.java plus minimal docs/tests if needed.

Implementation target:
- Wrap the existing OneInventoryRunSession in RogueScapeRun.
- Inject RuneLite Client.
- Initialize and maintain an InventorySnapshot of InventoryID.INVENTORY.
- Add @Subscribe ItemContainerChanged and process only inventory container events.
- Compute positive deltas with InventoryDiff.positiveDeltas(..., ProvenanceHint.UNKNOWN).
- Feed gains into rogueRun.applyItemDelta(...).
- Add GameTick region capture using local player's WorldPoint region ID.
- Add GameStateChanged snapshot refresh on LOGGED_IN to avoid stale deltas.
- Make overlay show compact live proof: region and legal/suspicious/illegal item counts, plus existing run summary if practical.

Verification:
Run:
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew test --tests '*AdapterSeamTest' --tests '*RogueScapeRunTest' --tests '*OneInventoryRoguelikePrototypeTest'
./gradlew test

Receipt required:
- files changed;
- exact event subscribers added;
- exact tests run and result;
- any RuneLite API compile adjustment made;
- what is still deferred.
```

---

## Hermes Verification Checklist After Claude

Hermes must independently verify:

- `OneInventoryRoguelikePlugin.java` contains `@Subscribe` handlers for inventory and region/game state where planned.
- `ItemContainerChanged` handler filters to inventory only.
- No menu/action-blocking APIs were introduced.
- `RogueScapeRun.applyItemDelta` is called from live plugin wiring.
- Overlay includes legality counts or equivalent live proof.
- Full Gradle test passes with Java 17.
- Handoff doc was updated if code changed.

If Claude times out or stops early, inspect `git status --short` and `git diff --stat` before retrying. Continue with a narrower prompt naming the exact incomplete slice.

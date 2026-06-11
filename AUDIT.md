# RogueScape Plugin Audit

Audit target: `C:\Users\Caleb\Documents\GitHub\plugins\roguescape`

Date: 2026-05-31

## Executive summary

RogueScape is a substantial RuneLite plugin prototype, not a complete standalone plugin repository yet. The copied source tree is healthier than the original `PluginIdeaHub\plugins\roguescape` working tree: in a temporary clean host using the shared `PluginIdeaHub/src` framework, the copied RogueScape source compiles and its RogueScape test suite passes.

The biggest issue is packaging. This folder only contains `src/` plus run scripts. It is missing the Gradle wrapper/project files, `runelite-plugin.properties`, license/readme/icon metadata, and shared framework classes that the source imports from `com.pluginideahub.*`. In its current folder, the run scripts walk two directories up and look for `gradlew`, which does not exist at `C:\Users\Caleb\Documents\GitHub`.

If the goal is a Plugin Hub-ready RuneLite plugin, the next move should be to turn this into its own repository/project based on the official RuneLite plugin template, then either vendor or remove the remaining `PluginIdeaHub` framework dependencies.

## Verification performed

1. Confirmed copied folder contents:
   - `src/`
   - `run-client.bat`
   - `run-client.sh`

2. Confirmed no standalone plugin/build metadata in the copied folder:
   - no `build.gradle`
   - no `settings.gradle`
   - no Gradle wrapper
   - no `runelite-plugin.properties`
   - no `.codex-plugin/plugin.json`

3. Built the copied plugin in a temporary throwaway RuneLite-style host:
   - copied `PluginIdeaHub/build.gradle`, Gradle wrapper, shared `src/`, and copied `plugins/roguescape`
   - ran `.\gradlew.bat test --tests "com.pluginideahub.roguescape.*"`
   - result: build successful

4. Tried the original `PluginIdeaHub` host directly:
   - original host currently fails compile because its `RogueScapeWidgetWindow.java` calls `itemModel(...)` without a matching method
   - the copied audit target does not have that compile failure

## Findings

### P0: The copied folder is not runnable where it lives

Files:
- `run-client.bat`
- `run-client.sh`

Both scripts assume the plugin sits at `PluginIdeaHub/plugins/roguescape` and do `../..` to reach a Gradle project. In the new location, `../..` resolves to `C:\Users\Caleb\Documents\GitHub`, not `PluginIdeaHub`, so `gradlew.bat`/`gradlew` will not be found.

Fix:
- Make `roguescape` a standalone Gradle project, or
- create a top-level host project under `C:\Users\Caleb\Documents\GitHub` that includes `plugins/roguescape`, or
- update the scripts to call the original host intentionally.

Best path: standalone Gradle project.

### P0: Missing RuneLite Plugin Hub metadata

The official RuneLite Plugin Hub flow expects a plugin repository with `runelite-plugin.properties` containing display name, author, description, tags, plugin class, and build info. This copied folder has none.

Required starter metadata:

```properties
displayName=RogueScape
author=Caleb
support=
description=Roguelike OSRS challenge runs with rooms, relics, rewards, and recaps.
tags=challenge,creator,roguelike,run,regions
plugins=com.pluginideahub.roguescape.RogueScapePlugin
build=standard
```

The original monorepo does register `com.pluginideahub.roguescape.RogueScapePlugin`, but inside a huge shared `runelite-plugin.properties` line with many prototypes. That is not the shape you want for a focused public plugin.

### P0: Hidden dependency on shared PluginIdeaHub framework classes

Files:
- `src/main/java/com/pluginideahub/roguescape/RogueScapePlugin.java`
- `src/main/java/com/pluginideahub/roguescape/core/RogueScapePrototype.java`

Imports outside the copied plugin package:

```java
import com.pluginideahub.ui.PluginIdeaHubOverlay;
import com.pluginideahub.ui.PluginIdeaHubUiModel;
import com.pluginideahub.prototype.core.PrototypeDeck;
import com.pluginideahub.prototype.core.PrototypeRuleCard;
```

The copied folder does not include those classes. The temporary build passed only because I included the original `PluginIdeaHub/src` shared code.

Fix options:
- Copy the small shared framework classes into this plugin repository.
- Better: remove the prototype framework dependency and replace it with RogueScape-owned equivalents.
- Best for Plugin Hub: keep the plugin self-contained and only depend on RuneLite plus JUnit for tests.

### P1: Original working tree and copied folder have drifted

The original `PluginIdeaHub` repository has local modifications in:

```text
plugins/roguescape/src/main/java/com/pluginideahub/roguescape/RogueScapePlugin.java
plugins/roguescape/src/main/java/com/pluginideahub/roguescape/ui/RogueScapeRewardOverlay.java
plugins/roguescape/src/main/java/com/pluginideahub/roguescape/ui/RogueScapeWidgetWindow.java
```

The original currently fails compilation because `RogueScapeWidgetWindow` calls `itemModel(...)` without a method in that exact working tree state. The copied folder has the older/simple item graphic path and compiles in the clean temporary host.

Fix:
- Decide which version is canonical.
- If the 3D item model work is desired, port the complete helper method and imports into the copied plugin.
- If stability is the priority, keep the copied version and ignore the original local experiment.

### P1: `latest.release` makes builds non-reproducible

File:
- original `PluginIdeaHub/build.gradle`

The host build uses:

```groovy
def runeLiteVersion = 'latest.release'
```

This is convenient during exploration, but it means tomorrow's dependency resolution can break yesterday's plugin. For Plugin Hub work, pin the RuneLite version used by the template or the Plugin Hub check flow.

Fix:
- Start from the official template.
- Keep versions pinned.
- Avoid depending on private or shifting APIs unless necessary.

### P1: Widget injection is high risk for Plugin Hub review

Files:
- `RogueScapePlugin.java`
- `RogueScapeJournalTabAdapter.java`
- `RogueScapeWidgetWindow.java`

The plugin injects dynamic widgets into the side journal and viewport, hooks collection-log menu entries, and has an experimental in-game window. This is cool, but it is the part most likely to break across RuneLite updates or draw review questions.

Good news:
- It is gated behind `experimentalJournalTab`, default false.

Risk:
- The code references `InterfaceID` constants directly.
- It creates dynamic children and re-injects them tick-driven.
- It relies on live client widget structure.

Recommendation:
- Keep the normal RuneLite sidebar panel as the primary public UX.
- Keep the journal/widget window behind a developer/experimental flag until it has in-game soak testing.
- For first Plugin Hub submission, consider removing or disabling widget injection entirely.

### P1: Menu enforcement may be interpreted as gameplay-affecting

Files:
- `RogueScapePlugin.java`
- `core/enforcement/*`

During active run phases, the plugin filters/block-consumes menu entries for rule enforcement. It is framed as challenge integrity, but any plugin that modifies menus during gameplay deserves extra scrutiny.

Recommendation:
- For public safety, default to warning-only.
- Make blocking opt-in and clearly labeled.
- Consider shipping passive overlays/alerts first, then add enforcement only after checking Plugin Hub expectations.

### P2: Developer mode defaults on

File:
- `RogueScapeConfig.java`

`developerMode()` returns true. That is fine for local iteration but not for a public plugin. It exposes simulation controls by default.

Fix:
- Default developer mode to false for standalone/public builds.

### P2: Sidebar has placeholder/stub sections

File:
- `RogueScapePanel.java`

The panel still describes some sections as stubs or future systems. That is normal for a prototype, but a public plugin should avoid UI that promises unfinished systems.

Fix:
- Hide unfinished sections.
- Or relabel as "Coming later" only in a private/dev build.

### P2: Placeholder item art and rarity heuristics

File:
- `RogueScapePlugin.java`

Relic icons are currently represented by real OSRS item IDs chosen as placeholders, and rarity is inferred from effect count.

This is acceptable for local prototyping, but for polish:
- create a deterministic relic metadata layer
- give each relic an explicit icon/item id
- give each relic an explicit rarity/tier

### P2: Test coverage is strong for core, thinner for live integration

Current shape:
- 96 main Java files
- 35 test Java files
- 874 test/assert hits from quick grep

Strength:
- good pure Java tests around run loop, legality, rewards, relics, routes, masks, UI rendering helpers

Gap:
- live RuneLite integration cannot be fully covered by unit tests
- widget injection and menu filtering are the riskiest runtime surfaces

Recommendation:
- Add a small manual smoke-test checklist to `README.md`.
- Keep widget/menu code isolated behind config flags.
- Add tests for any pure logic around menu decisions and config-driven gating.

## Plugin Hub readiness checklist

Minimum standalone repo:

```text
roguescape/
  build.gradle
  settings.gradle
  gradlew
  gradlew.bat
  gradle/
  runelite-plugin.properties
  README.md
  LICENSE
  icon.png
  src/main/java/...
  src/main/resources/...
  src/test/java/...
```

Code cleanup before public release:

1. Remove or vendor `PluginIdeaHub` shared dependencies.
2. Pin the RuneLite build setup through the official template.
3. Add focused `runelite-plugin.properties`.
4. Make developer mode default false.
5. Keep experimental widget injection disabled or remove it for first submission.
6. Decide warning-only vs blocking enforcement.
7. Replace placeholder relic art/tier heuristics.
8. Add README with feature list, setup, safety notes, and manual smoke test.
9. Add license, preferably BSD 2-Clause for Plugin Hub convention.
10. Run tests and development client from the standalone repo.

## Recommended next implementation pass

1. Scaffold standalone Gradle/RuneLite project files into this copied folder.
2. Move/copy the few shared framework classes or refactor them out.
3. Create a focused `runelite-plugin.properties`.
4. Fix scripts to run from this folder.
5. Run full tests.
6. Launch the developer client with `RogueScapePluginTestClient`.
7. Only after that, revisit the original working tree's 3D widget model experiment.

## Sources checked

- RuneLite Plugin Hub repository README: https://github.com/runelite/plugin-hub
- RuneLite Developer Guide: https://github.com/runelite/runelite/wiki/Developer-Guide

package com.pluginideahub.roguescape.core;

/**
 * Read-only snapshot of the live run-state triple (session/run/loop) plus the player's current
 * region id. Immutable value object — collaborators read run state through this handle instead of
 * reaching back into the plugin's mutable fields, which is what lets them be unit-tested without
 * instantiating the {@code @Inject}-heavy plugin.
 *
 * <p>In the lobby (no active run) all three handles are null and the region is {@code ""}. The
 * handles returned by {@link #run()}/{@link #session()}/{@link #loop()} are the LIVE engine objects
 * (themselves the legitimate mutation target, e.g. {@code run().applyItemDelta(...)}); this type's
 * read-only-ness is about field/lifecycle ownership — a collaborator cannot null, replace, start or
 * reset the run, only the owner can. It holds no RuneLite types and never imports the root plugin
 * package, so the {@code core} package stays dependency-clean.
 *
 * <p>The ambient {@link #currentRegionId()} is the plugin-tracked value shown in overlays (kept in
 * sync by {@code onGameTick}); it is deliberately NOT derived from {@code run().currentRegionId()}
 * so the seam preserves existing behavior exactly.
 */
public final class RunContext
{
	private final RogueScapeRunSession session; // null in lobby
	private final RogueScapeRun run;            // null in lobby
	private final RogueScapeRunLoop loop;       // null in lobby
	private final String currentRegionId;       // never null; "" when unknown/lobby

	private RunContext(RogueScapeRunSession session, RogueScapeRun run, RogueScapeRunLoop loop, String currentRegionId)
	{
		this.session = session;
		this.run = run;
		this.loop = loop;
		this.currentRegionId = currentRegionId == null ? "" : currentRegionId;
	}

	/** Lobby: no active run. */
	public static RunContext lobby()
	{
		return new RunContext(null, null, null, "");
	}

	/** Active-run snapshot. Any handle may still be null; gate reads via {@link #hasRun()}. */
	public static RunContext active(RogueScapeRunSession session, RogueScapeRun run, RogueScapeRunLoop loop,
		String currentRegionId)
	{
		return new RunContext(session, run, loop, currentRegionId);
	}

	public RogueScapeRunSession session()
	{
		return session;
	}

	public RogueScapeRun run()
	{
		return run;
	}

	public RogueScapeRunLoop loop()
	{
		return loop;
	}

	/** Plugin-tracked ambient region id; never null ("" when unknown/lobby). */
	public String currentRegionId()
	{
		return currentRegionId;
	}

	/** True only when the full run-state triple is present — the canonical "a run is live" guard. */
	public boolean hasRun()
	{
		return run != null && session != null && loop != null;
	}
}

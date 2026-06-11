package com.pluginideahub.roguescape.core.race;

import com.pluginideahub.roguescape.core.seed.ChallengeDefinition;

/**
 * Stage 11 — definition of a weekly race event. Pairs an event id and human label with the
 * underlying {@link ChallengeDefinition}. Event windows are recorded as ISO-8601 strings
 * because the plugin avoids pulling in {@code java.time.*} formatters at this stage.
 */
public final class WeeklyEvent
{
	private final String eventId;
	private final String label;
	private final String startsAtIso;
	private final String endsAtIso;
	private final ChallengeDefinition challenge;

	public WeeklyEvent(String eventId, String label, String startsAtIso, String endsAtIso, ChallengeDefinition challenge)
	{
		if (eventId == null || eventId.isEmpty()) throw new IllegalArgumentException("eventId required");
		if (challenge == null) throw new IllegalArgumentException("challenge required");
		this.eventId = eventId;
		this.label = label == null ? eventId : label;
		this.startsAtIso = startsAtIso == null ? "" : startsAtIso;
		this.endsAtIso = endsAtIso == null ? "" : endsAtIso;
		this.challenge = challenge;
	}

	public String eventId() { return eventId; }
	public String label() { return label; }
	public String startsAtIso() { return startsAtIso; }
	public String endsAtIso() { return endsAtIso; }
	public ChallengeDefinition challenge() { return challenge; }
}

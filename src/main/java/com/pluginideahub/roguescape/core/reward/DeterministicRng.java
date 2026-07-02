package com.pluginideahub.roguescape.core.reward;

/**
 * Stage 4/10 — small deterministic xorshift RNG. We don't use {@code java.util.Random}
 * because its long-term seed/stream guarantees aren't stable across JVMs/versions, and we
 * want bit-identical reproducibility for seeds shared across players.
 */
public final class DeterministicRng
{
	private long state;

	public DeterministicRng(long seed)
	{
		// Splitmix-flavored seeding so very small / zero seeds produce non-zero state.
		long z = seed + 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		this.state = z == 0 ? 1L : (z ^ (z >>> 31));
	}

	public DeterministicRng(String seedText)
	{
		this(hash(seedText));
	}

	public long nextLong()
	{
		long x = state;
		x ^= x << 13;
		x ^= x >>> 7;
		x ^= x << 17;
		state = x;
		return x;
	}

	public int nextInt(int boundExclusive)
	{
		if (boundExclusive <= 0) throw new IllegalArgumentException("bound must be positive");
		long bits = nextLong() & 0x7FFFFFFFFFFFFFFFL;
		return (int) (bits % boundExclusive);
	}

	/** Fisher–Yates shuffle driven by this RNG, for seed-stable ordering of pools. */
	public <T> void shuffle(java.util.List<T> list)
	{
		for (int i = list.size() - 1; i > 0; i--)
		{
			int j = nextInt(i + 1);
			java.util.Collections.swap(list, i, j);
		}
	}

	public static long hash(String text)
	{
		if (text == null) return 0L;
		long h = 0xCBF29CE484222325L;
		for (int i = 0; i < text.length(); i++)
		{
			h ^= text.charAt(i);
			h *= 0x100000001B3L;
		}
		return h;
	}
}

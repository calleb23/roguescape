package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public class ModifierLibraryTest
{
	private static Set<BankItemCategory> restrictedCategories(Relic relic)
	{
		Set<BankItemCategory> out = new HashSet<>();
		for (RelicEffect e : relic.effects())
		{
			if (e.kind() == RelicEffectKind.RESTRICTION)
			{
				out.addAll(e.categories());
			}
		}
		return out;
	}

	@Test
	public void noFoodIsASingleFoodRestriction()
	{
		Relic relic = ModifierLibrary.noFood();
		assertEquals("mod-no-food", relic.relicId());
		assertEquals(1, relic.effects().size());
		RelicEffect e = relic.effects().get(0);
		assertEquals(RelicEffectKind.RESTRICTION, e.kind());
		assertTrue(e.categories().contains(BankItemCategory.FOOD));
	}

	@Test
	public void noArmourRestrictsFiveArmourSlots()
	{
		Relic relic = ModifierLibrary.noArmour();
		Set<BankItemCategory> restricted = restrictedCategories(relic);
		assertEquals(5, restricted.size());
		assertTrue(restricted.contains(BankItemCategory.HELMET));
		assertTrue(restricted.contains(BankItemCategory.BODY));
		assertTrue(restricted.contains(BankItemCategory.LEGS));
		assertTrue(restricted.contains(BankItemCategory.BOOTS));
		assertTrue(restricted.contains(BankItemCategory.GLOVES));
		assertFalse("shield is not part of noArmour", restricted.contains(BankItemCategory.SHIELD));
	}

	@Test
	public void oneWeaponOnlyAppliesACategoryLimitOfOnePerStyle()
	{
		Relic relic = ModifierLibrary.oneWeaponOnly();
		assertEquals(3, relic.effects().size());
		for (RelicEffect e : relic.effects())
		{
			assertEquals(RelicEffectKind.CATEGORY_LIMIT, e.kind());
			assertEquals(1, e.magnitude());
		}
		Set<BankItemCategory> limited = new HashSet<>();
		for (RelicEffect e : relic.effects()) limited.addAll(e.categories());
		assertTrue(limited.contains(BankItemCategory.MELEE_WEAPON));
		assertTrue(limited.contains(BankItemCategory.RANGED_WEAPON));
		assertTrue(limited.contains(BankItemCategory.MAGIC_WEAPON));
	}
}

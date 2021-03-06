package com.etheller.warsmash.viewer5.handlers.w3x.simulation.util;

import java.awt.image.BufferedImage;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackInstant;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectile;

public interface SimulationRenderController {
	CAttackProjectile createAttackProjectile(CSimulation simulation, float launchX, float launchY, float launchFacing,
			CUnit source, CUnitAttackMissile attack, AbilityTarget target, float damage, int bounceIndex,
			CUnitAttackListener attackListener);

	CUnit createUnit(CSimulation simulation, final War3ID typeId, final int playerIndex, final float x, final float y,
			final float facing);

	void createInstantAttackEffect(CSimulation cSimulation, CUnit source, CUnitAttackInstant attack, CWidget target);

	void spawnDamageSound(CWidget damagedDestructable, String weaponSound, String armorType);

	void spawnUnitConstructionSound(CUnit constructingUnit, CUnit constructedStructure);

	void removeUnit(CUnit unit);

	void removeDestructable(CDestructable dest);

	BufferedImage getBuildingPathingPixelMap(War3ID rawcode);

	BufferedImage getDestructablePathingPixelMap(War3ID rawcode);

	BufferedImage getDestructablePathingDeathPixelMap(War3ID rawcode);

	void spawnUnitConstructionFinishSound(CUnit constructedStructure);

	void spawnBuildingDeathEffect(CUnit cUnit);

	void spawnGainLevelEffect(CUnit cUnit);

	void spawnUnitReadySound(CUnit trainedUnit);

	void unitRepositioned(CUnit cUnit);

	void spawnGainResourceTextTag(CUnit gainingUnit, ResourceType resourceType, int amount);

	void spawnEffectOnUnit(CUnit unit, String effectPath);

	void spawnSpellEffectOnUnit(CUnit unit, War3ID alias);

	void spawnUIUnitGetItemSound(CUnit cUnit, CItem item);

	void spawnUIUnitDropItemSound(CUnit cUnit, CItem item);

	void spawnAbilitySoundEffect(CUnit caster, War3ID alias);

	void unitPreferredSelectionReplacement(CUnit unit, CUnit newUnit);

	void heroRevived(CUnit trainedUnit);

	void heroDeathEvent(CUnit cUnit);

}

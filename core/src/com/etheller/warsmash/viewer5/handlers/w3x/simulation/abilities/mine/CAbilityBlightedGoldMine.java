package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbstractGenericNoIconAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public class CAbilityBlightedGoldMine extends AbstractGenericNoIconAbility {
	private int gold;
	private final int goldPerInterval;
	private final float intervalDuration;
	private final int maxNumberOfMiners;
	private final float radiusOfMiningRing;

	public CAbilityBlightedGoldMine(final int handleId, final War3ID alias, final int goldPerInterval,
			final float intervalDuration, final int maxNumberOfMiners, final float radiusOfMiningRing) {
		super(handleId, alias);
		this.goldPerInterval = goldPerInterval;
		this.intervalDuration = intervalDuration;
		this.maxNumberOfMiners = maxNumberOfMiners;
		this.radiusOfMiningRing = radiusOfMiningRing;
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {

	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {

	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
//		final boolean empty = this.activeMiners.isEmpty();
//		if (empty != this.wasEmpty) {
//			if (empty) {
//				unit.getUnitAnimationListener().removeSecondaryTag(SecondaryTag.WORK);
//			}
//			else {
//				unit.getUnitAnimationListener().addSecondaryTag(SecondaryTag.WORK);
//			}
//			this.wasEmpty = empty;
//		}
//		for (int i = this.activeMiners.size() - 1; i >= 0; i--) {
//			final CBehaviorHarvest activeMiner = this.activeMiners.get(i);
//			if (game.getGameTurnTick() >= activeMiner.getPopoutFromMineTurnTick()) {
//
//				final int goldMined = Math.min(this.gold, activeMiner.getGoldCapacity());
//				this.gold -= goldMined;
//				if (this.gold <= 0) {
//					unit.setLife(game, 0);
//				}
//				activeMiner.popoutFromMine(goldMined);
//				this.activeMiners.remove(i);
//			}
//		}
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId, final CWidget target) {
		return null;
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget point) {
		return null;
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int orderId) {
		return null;
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		receiver.notAnActiveAbility();
	}

	@Override
	public void onCancelFromQueue(final CSimulation game, final CUnit unit, final int orderId) {
	}

	public int getGold() {
		return this.gold;
	}

	public void setGold(final int gold) {
		this.gold = gold;
	}

}

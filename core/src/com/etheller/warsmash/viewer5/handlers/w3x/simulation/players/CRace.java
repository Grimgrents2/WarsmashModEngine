package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

public enum CRace {
	HUMAN(1),
	ORC(2),
	UNDEAD(3),
	NIGHTELF(4),
	DEMON(5),
	OTHER(7);

	private int id;

	private CRace(final int id) {
		this.id = id;
	}

	public static CRace[] VALUES = values();

	public int getId() {
		return this.id;
	}

	public static CRace parseRace(final int race) {
		// TODO: this is bad time complexity (slow) but we're only doing it on startup
		for (final CRace raceEnum : values()) {
			if (raceEnum.getId() == race) {
				return raceEnum;
			}
		}
		return null;
	}
}

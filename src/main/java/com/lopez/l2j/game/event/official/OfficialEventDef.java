package com.lopez.l2j.game.event.official;

import java.util.List;

public class OfficialEventDef {

	private final OfficialEventType type;
	private final String name;
	private final String description;
	private OfficialEventState state;
	private final List<EventDropEntry> drops;
	private final List<EventRewardExchange> exchanges;

	public OfficialEventDef(OfficialEventType type, String name, String description,
							OfficialEventState state, List<EventDropEntry> drops,
							List<EventRewardExchange> exchanges) {
		this.type = type;
		this.name = name;
		this.description = description;
		this.state = state;
		this.drops = drops;
		this.exchanges = exchanges;
	}

	public OfficialEventType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public OfficialEventState getState() {
		return state;
	}

	public void setState(OfficialEventState state) {
		this.state = state;
	}

	public List<EventDropEntry> getDrops() {
		return drops;
	}

	public List<EventRewardExchange> getExchanges() {
		return exchanges;
	}
}

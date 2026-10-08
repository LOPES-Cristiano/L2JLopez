package com.lopez.l2j.network.game.security;

/**
 * Os 10 canais criticos de protecao anti-flood granulares de alta frequencia - Onda C11.
 *
 * <p>Baseado nas protecoes industriais do L2jFrozen (flood.properties):</p>
 * <ul>
 *   <li>USE_ITEM: Troca rapida de armas e cancel de animacao.</li>
 *   <li>USE_POTION: Autoclick de pocoes de HP/MP/CP.</li>
 *   <li>SUBCLASS_CHANGE: Exploit de troca rapida de subclasse em vilas.</li>
 *   <li>REQUEST_ENCHANT: Tentativa de corrupcao/flood de pacotes de encantamento.</li>
 *   <li>WAREHOUSE_TRANSACTION: Tentativas de race condition em trade/armazem.</li>
 *   <li>SOCIAL_ACTION: Flood visual de animacoes e dados (RollDice).</li>
 *   <li>HERO_VOICE: Flood de chat global de heroi (% / Say2).</li>
 *   <li>TRADE_REQUEST: Spam de solicitacoes de negociacao/trade.</li>
 *   <li>MOVE_ACTION: Packet flood e speedhack de movimentacao.</li>
 *   <li>MULTISELL: Exploit de compra automatizada e concorrencia em NPCs.</li>
 * </ul>
 */
public enum FloodProtectorChannel {

	USE_ITEM("UseItem", 100, 15, PunishmentType.KICK),
	USE_POTION("UsePotion", 200, 10, PunishmentType.ACTION_FAILED),
	SUBCLASS_CHANGE("SubclassChange", 1000, 5, PunishmentType.KICK),
	REQUEST_ENCHANT("RequestEnchant", 500, 8, PunishmentType.KICK),
	WAREHOUSE_TRANSACTION("WarehouseTransaction", 500, 10, PunishmentType.KICK),
	SOCIAL_ACTION("SocialAction", 1500, 6, PunishmentType.ACTION_FAILED),
	HERO_VOICE("HeroVoice", 10000, 3, PunishmentType.ACTION_FAILED),
	TRADE_REQUEST("TradeRequest", 1000, 5, PunishmentType.ACTION_FAILED),
	MOVE_ACTION("MoveAction", 50, 25, PunishmentType.ACTION_FAILED),
	MULTISELL("MultiSell", 200, 10, PunishmentType.KICK);

	public enum PunishmentType {
		NONE,
		ACTION_FAILED,
		LOG_WARNING,
		KICK,
		TEMP_BAN
	}

	private final String channelName;
	private final long intervalMs;
	private final int punishmentLimit;
	private final PunishmentType punishment;

	FloodProtectorChannel(String channelName, long intervalMs, int punishmentLimit, PunishmentType punishment) {
		this.channelName = channelName;
		this.intervalMs = intervalMs;
		this.punishmentLimit = punishmentLimit;
		this.punishment = punishment;
	}

	public String channelName() {
		return channelName;
	}

	public long intervalMs() {
		return intervalMs;
	}

	public int punishmentLimit() {
		return punishmentLimit;
	}

	public PunishmentType punishment() {
		return punishment;
	}
}

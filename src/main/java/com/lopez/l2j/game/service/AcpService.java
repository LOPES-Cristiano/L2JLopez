package com.lopez.l2j.game.service;

import com.lopez.l2j.game.effect.ConsumableTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de Auto Combat Potion (.acp) - Onda 3 (QoL & Conforto In-Game).
 * Permite ao jogador configurar percentuais de ativacao automatica de pocoes de CP, HP e MP.
 * 100% In-Game: consome apenas itens regulares do inventario, respeitando os cooldowns oficiais.
 */
@Service
public class AcpService {

	private static final Logger log = LoggerFactory.getLogger(AcpService.class);

	// IDs de pocoes regulares do Interlude
	public static final int ITEM_GREATER_CP = 5592;
	public static final int ITEM_CP_POTION = 5591;
	public static final int ITEM_QUICK_HEAL_HP = 1540;
	public static final int ITEM_GREATER_HEAL_HP = 1539;
	public static final int ITEM_HEAL_HP = 1061;
	public static final int ITEM_MANA_POTION = 728;
	public static final int ITEM_MANA_DRUG = 726;

	public record AcpConfig(boolean enabled, int hpPercent, int cpPercent, int mpPercent) {}

	private final CharacterVariablesService variablesService;
	private final Map<Integer, AcpConfig> cache = new ConcurrentHashMap<>();

	@Autowired
	public AcpService(@Autowired(required = false) CharacterVariablesService variablesService) {
		this.variablesService = variablesService;
	}

	public AcpConfig getConfig(PlayerCharacter player) {
		if (player == null) {
			return new AcpConfig(false, 70, 80, 60);
		}
		return cache.computeIfAbsent(player.objectId(), id -> loadConfig(player.objectId()));
	}

	private AcpConfig loadConfig(int charId) {
		if (variablesService != null) {
			boolean enabled = "true".equalsIgnoreCase(variablesService.getVariable(charId, "acp_enabled", "false"));
			int hp = parseClamped(variablesService.getVariable(charId, "acp_hp", "70"), 0, 100, 70);
			int cp = parseClamped(variablesService.getVariable(charId, "acp_cp", "80"), 0, 100, 80);
			int mp = parseClamped(variablesService.getVariable(charId, "acp_mp", "60"), 0, 100, 60);
			return new AcpConfig(enabled, hp, cp, mp);
		}
		return new AcpConfig(false, 70, 80, 60);
	}

	private int parseClamped(String val, int min, int max, int def) {
		try {
			int n = Integer.parseInt(val);
			return Math.max(min, Math.min(max, n));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public void setEnabled(PlayerCharacter player, boolean enabled) {
		if (player == null) return;
		AcpConfig cur = getConfig(player);
		AcpConfig updated = new AcpConfig(enabled, cur.hpPercent(), cur.cpPercent(), cur.mpPercent());
		cache.put(player.objectId(), updated);
		if (variablesService != null) {
			variablesService.setVariable(player.objectId(), "acp_enabled", String.valueOf(enabled));
		}
	}

	public void setThresholds(PlayerCharacter player, Integer hp, Integer cp, Integer mp) {
		if (player == null) return;
		AcpConfig cur = getConfig(player);
		int newHp = hp != null ? Math.max(0, Math.min(100, hp)) : cur.hpPercent();
		int newCp = cp != null ? Math.max(0, Math.min(100, cp)) : cur.cpPercent();
		int newMp = mp != null ? Math.max(0, Math.min(100, mp)) : cur.mpPercent();
		AcpConfig updated = new AcpConfig(cur.enabled(), newHp, newCp, newMp);
		cache.put(player.objectId(), updated);
		if (variablesService != null) {
			if (hp != null) variablesService.setVariable(player.objectId(), "acp_hp", String.valueOf(newHp));
			if (cp != null) variablesService.setVariable(player.objectId(), "acp_cp", String.valueOf(newCp));
			if (mp != null) variablesService.setVariable(player.objectId(), "acp_mp", String.valueOf(newMp));
		}
	}

	/**
	 * Verifica e consome pocoes conforme os limiares configurados.
	 */
	public void checkAndConsume(GameSession session) {
		if (session == null) return;
		PlayerCharacter active = session.character();
		if (active == null || active.isDead() || active.isAlikeDead()) {
			return;
		}
		AcpConfig cfg = getConfig(active);
		if (!cfg.enabled()) {
			return;
		}

		long now = System.currentTimeMillis();

		// 1. Verificacao de CP
		if (active.maxCp() > 0 && cfg.cpPercent() > 0) {
			double cpRatio = (active.currentCp() / active.maxCp()) * 100.0;
			if (cpRatio <= cfg.cpPercent()) {
				tryUsePotion(session, ITEM_GREATER_CP, ITEM_CP_POTION);
			}
		}

		// 2. Verificacao de HP
		if (active.maxHp() > 0 && cfg.hpPercent() > 0) {
			double hpRatio = (active.currentHp() / active.maxHp()) * 100.0;
			if (hpRatio <= cfg.hpPercent()) {
				tryUsePotion(session, ITEM_QUICK_HEAL_HP, ITEM_GREATER_HEAL_HP, ITEM_HEAL_HP);
			}
		}

		// 3. Verificacao de MP
		if (active.maxMp() > 0 && cfg.mpPercent() > 0) {
			double mpRatio = (active.currentMp() / active.maxMp()) * 100.0;
			if (mpRatio <= cfg.mpPercent()) {
				tryUsePotion(session, ITEM_MANA_POTION, ITEM_MANA_DRUG);
			}
		}
	}

	private void tryUsePotion(GameSession session, int... itemIds) {
		PlayerCharacter active = session.character();
		if (active == null || active.inventory() == null) return;

		for (int itemId : itemIds) {
			var opt = active.inventory().byItemId(itemId);
			if (opt.isPresent() && opt.get().count() > 0) {
				var consumable = ConsumableTable.get(itemId);
				if (consumable.isPresent()) {
					session.useConsumable(consumable.get());
					return;
				}
			}
		}
	}

	/**
	 * Renderiza painel interativo HTML do .acp
	 */
	public String renderHtml(PlayerCharacter player) {
		AcpConfig cfg = getConfig(player);
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=260 cellpadding=2 cellspacing=0>");
		sb.append("<tr><td align=center><font color=\"LEVEL\"><b>SISTEMA AUTO-POTION (.ACP)</b></font></td></tr>");
		sb.append("<tr><td align=center><font color=\"AAAAAA\">Consumo automatico de pocoes do inventario.</font></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260 bgcolor=222222 border=1 cellpadding=4 cellspacing=0>");
		sb.append("<tr><td><b>Status:</b></td><td align=center>");
		if (cfg.enabled()) {
			sb.append("<font color=\"00FF00\"><b>ATIVADO</b></font></td><td>");
			sb.append("<button value=\"Desativar\" action=\"bypass -h voiced_acp off\" width=65 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\">");
		} else {
			sb.append("<font color=\"FF0000\"><b>DESATIVADO</b></font></td><td>");
			sb.append("<button value=\"Ativar\" action=\"bypass -h voiced_acp on\" width=65 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\">");
		}
		sb.append("</td></tr>");

		sb.append("<tr><td><b>CP Minimo:</b></td><td align=center><font color=\"FFFF00\">").append(cfg.cpPercent()).append("%</font></td>");
		sb.append("<td align=center><a action=\"bypass -h voiced_acp cp 80\">[80%]</a> <a action=\"bypass -h voiced_acp cp 90\">[90%]</a></td></tr>");

		sb.append("<tr><td><b>HP Minimo:</b></td><td align=center><font color=\"FF5555\">").append(cfg.hpPercent()).append("%</font></td>");
		sb.append("<td align=center><a action=\"bypass -h voiced_acp hp 60\">[60%]</a> <a action=\"bypass -h voiced_acp hp 75\">[75%]</a></td></tr>");

		sb.append("<tr><td><b>MP Minimo:</b></td><td align=center><font color=\"5599FF\">").append(cfg.mpPercent()).append("%</font></td>");
		sb.append("<td align=center><a action=\"bypass -h voiced_acp mp 50\">[50%]</a> <a action=\"bypass -h voiced_acp mp 70\">[70%]</a></td></tr>");
		sb.append("</table><br>");

		sb.append("<table width=260>");
		sb.append("<tr><td align=center><font color=\"888888\">Comandos rapidos no chat:</font></td></tr>");
		sb.append("<tr><td align=center><font color=\"AAAAAA\">.acp on | .acp off | .acp hp 70 | .acp cp 80 | .acp mp 50</font></td></tr>");
		sb.append("</table>");
		sb.append("</body></html>");
		return sb.toString();
	}
}

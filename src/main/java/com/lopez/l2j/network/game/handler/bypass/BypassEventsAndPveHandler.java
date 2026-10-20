package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.arenaduel.ArenaDuelService;
import com.lopez.l2j.game.boss.BossManager;
import com.lopez.l2j.game.boss.epic.AntharasService;
import com.lopez.l2j.game.boss.epic.BaiumService;
import com.lopez.l2j.game.boss.epic.ValakasService;
import com.lopez.l2j.game.event.pvp.CtfEventService;
import com.lopez.l2j.game.event.pvp.DmEventService;
import com.lopez.l2j.game.event.pvp.TvtEventService;
import com.lopez.l2j.game.instance.frintezza.FrintezzaService;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.pve.DungeonService;
import com.lopez.l2j.game.pve.FusionItemService;
import com.lopez.l2j.game.pve.SoloFarmService;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import com.lopez.l2j.network.game.packet.GameServerPacket.NpcHtmlMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handler modular para bypasses de eventos PvP (TvT, CTF, DM, Arena 1x1),
 * conteudos PvE instanciados (Dungeon, SoloFarm) e entradas de Grand Bosses Epicos.
 */
@Component
public class BypassEventsAndPveHandler implements IBypassHandler {

	private DungeonService dungeonService;
	private SoloFarmService soloFarmService;
	private AntharasService antharasService;
	private ValakasService valakasService;
	private BaiumService baiumService;
	private FrintezzaService frintezzaService;

	private FusionItemService fusionItemService;

	public BypassEventsAndPveHandler() {
		this(null, null, null, null, null, null, null);
	}

	@Autowired(required = false)
	public BypassEventsAndPveHandler(
			@Autowired(required = false) DungeonService dungeonService,
			@Autowired(required = false) SoloFarmService soloFarmService,
			@Autowired(required = false) AntharasService antharasService,
			@Autowired(required = false) ValakasService valakasService,
			@Autowired(required = false) BaiumService baiumService,
			@Autowired(required = false) FrintezzaService frintezzaService,
			@Autowired(required = false) FusionItemService fusionItemService) {
		this.dungeonService = dungeonService != null ? dungeonService : new DungeonService();
		this.soloFarmService = soloFarmService != null ? soloFarmService : new SoloFarmService();
		this.antharasService = antharasService;
		this.valakasService = valakasService;
		this.baiumService = baiumService;
		this.frintezzaService = frintezzaService;
		this.fusionItemService = fusionItemService != null ? fusionItemService : new FusionItemService();
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null || command.isBlank()) {
			return false;
		}
		String lower = command.trim().toLowerCase(Locale.ROOT);
		return lower.startsWith("evt_tvt_")
				|| lower.startsWith("evt_ctf_")
				|| lower.startsWith("evt_dm_")
				|| lower.startsWith("evt_arena_")
				|| lower.startsWith("pve_dungeon_")
				|| lower.startsWith("pve_solofarm_")
				|| lower.startsWith("epic_")
				|| lower.startsWith("custom_fusion_");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null || command == null) {
			return false;
		}
		PlayerCharacter active = session.activeCharacter();
		var ctx = session.context();
		String trimmed = command.trim();
		String[] parts = trimmed.split("\\s+");
		String action = parts[0].toLowerCase(Locale.ROOT);
		String arg = parts.length > 1 ? parts[1].trim() : "";

		switch (action) {
			case "evt_tvt_join" -> {
				TvtEventService tvt = ctx != null && ctx.tvt() != null ? ctx.tvt() : new TvtEventService();
				var res = tvt.register(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "TvT", "Registro TvT: " + res.name()));
				session.send(new NpcHtmlMessage(0, buildTvtHtml(tvt)));
				return true;
			}
			case "evt_tvt_leave" -> {
				TvtEventService tvt = ctx != null && ctx.tvt() != null ? ctx.tvt() : new TvtEventService();
				boolean ok = tvt.unregister(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "TvT",
						ok ? "Inscricao no TvT cancelada." : "Voce nao esta inscrito no TvT."));
				session.send(new NpcHtmlMessage(0, buildTvtHtml(tvt)));
				return true;
			}
			case "evt_ctf_join" -> {
				CtfEventService ctf = ctx != null && ctx.ctf() != null ? ctx.ctf() : new CtfEventService();
				var res = ctf.register(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "CTF", "Registro CTF: " + res.name()));
				session.send(new NpcHtmlMessage(0, buildCtfHtml(ctf)));
				return true;
			}
			case "evt_ctf_leave" -> {
				CtfEventService ctf = ctx != null && ctx.ctf() != null ? ctx.ctf() : new CtfEventService();
				boolean ok = ctf.unregister(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "CTF",
						ok ? "Inscricao no CTF cancelada." : "Voce nao esta inscrito no CTF."));
				session.send(new NpcHtmlMessage(0, buildCtfHtml(ctf)));
				return true;
			}
			case "evt_dm_join" -> {
				DmEventService dm = ctx != null && ctx.dm() != null ? ctx.dm() : new DmEventService();
				var res = dm.register(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "DM", "Registro DeathMatch: " + res.name()));
				session.send(new NpcHtmlMessage(0, buildDmHtml(dm)));
				return true;
			}
			case "evt_dm_leave" -> {
				DmEventService dm = ctx != null && ctx.dm() != null ? ctx.dm() : new DmEventService();
				boolean ok = dm.unregister(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "DM",
						ok ? "Inscricao no DeathMatch cancelada." : "Voce nao esta inscrito no DeathMatch."));
				session.send(new NpcHtmlMessage(0, buildDmHtml(dm)));
				return true;
			}
			case "evt_arena_join" -> {
				ArenaDuelService arena = ctx != null && ctx.arenaDuel() != null ? ctx.arenaDuel() : new ArenaDuelService();
				boolean ok = arena.register(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "Arena",
						ok ? "Voce entrou na fila 1x1 do Coliseu!" : "Nao foi possivel entrar na fila 1x1."));
				session.send(new NpcHtmlMessage(0, buildArenaHtml(arena, active)));
				return true;
			}
			case "evt_arena_leave" -> {
				ArenaDuelService arena = ctx != null && ctx.arenaDuel() != null ? ctx.arenaDuel() : new ArenaDuelService();
				boolean ok = arena.unregister(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "Arena",
						ok ? "Voce saiu da fila 1x1 do Coliseu." : "Voce nao estava na fila 1x1."));
				session.send(new NpcHtmlMessage(0, buildArenaHtml(arena, active)));
				return true;
			}
			case "pve_dungeon_enter" -> {
				int dungeonId = 1;
				if (!arg.isEmpty()) {
					try {
						dungeonId = Integer.parseInt(arg);
					} catch (NumberFormatException ignored) {}
				}
				var res = dungeonService.enterDungeon(active, dungeonId);
				session.send(new CreatureSay(0, CreatureSay.ALL, "Dungeon", res.message()));
				return true;
			}
			case "pve_solofarm_enter", "pve_solofarm_buy" -> {
				int mobCount = SoloFarmService.MIN_BUY;
				if (!arg.isEmpty()) {
					try {
						mobCount = Integer.parseInt(arg);
					} catch (NumberFormatException ignored) {}
				}
				var res = soloFarmService.buyFarmPackage(active, mobCount);
				session.send(new CreatureSay(0, CreatureSay.ALL, "SoloFarm", res.message()));
				return true;
			}
			case "epic_enter" -> {
				return handleEpicEnter(arg.toLowerCase(Locale.ROOT), session, active);
			}
			case "epic_antharas_enter" -> {
				return handleEpicEnter("antharas", session, active);
			}
			case "epic_valakas_enter" -> {
				return handleEpicEnter("valakas", session, active);
			}
			case "epic_baium_enter" -> {
				return handleEpicEnter("baium", session, active);
			}
			case "epic_baium_wake" -> {
				BaiumService baium = resolveBaium();
				boolean ok = baium != null && baium.awakenBaium(active);
				session.send(new CreatureSay(0, CreatureSay.ALL, "Baium",
						ok ? "Baium despertou de seu sono!" : "Nao foi possivel despertar Baium."));
				return true;
			}
			case "epic_frintezza_enter" -> {
				return handleEpicEnter("frintezza", session, active);
			}
			case "custom_fusion_list" -> {
				session.send(new NpcHtmlMessage(0, fusionItemService.buildFusionListHtml(active)));
				return true;
			}
			case "custom_fusion_exec" -> {
				int idx = 0;
				try {
					if (!arg.isBlank()) {
						idx = Integer.parseInt(arg);
					}
				} catch (NumberFormatException ignored) {}
				var res = fusionItemService.executeFusion(active, idx);
				session.send(new NpcHtmlMessage(0, fusionItemService.buildFusionFeedbackHtml(active, res)));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private boolean handleEpicEnter(String target, GameSession session, PlayerCharacter active) {
		switch (target) {
			case "antharas" -> {
				AntharasService antharas = resolveAntharas();
				boolean ok = antharas != null && antharas.enterLair(active);
				if (ok) {
					session.teleportToCoordinates(active.x(), active.y(), active.z());
					session.send(new CreatureSay(0, CreatureSay.ALL, "Antharas", "Voce entrou no Covil de Antharas!"));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Antharas",
							"Requisitos nao cumpridos (Portal Stone) ou Antharas indisponivel."));
				}
				return true;
			}
			case "valakas" -> {
				ValakasService valakas = resolveValakas();
				boolean ok = valakas != null && valakas.enterLair(active);
				if (ok) {
					session.teleportToCoordinates(active.x(), active.y(), active.z());
					session.send(new CreatureSay(0, CreatureSay.ALL, "Valakas", "Voce entrou no Covil de Valakas!"));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Valakas",
							"Requisitos nao cumpridos (Floating Stone) ou Valakas indisponivel."));
				}
				return true;
			}
			case "baium" -> {
				BaiumService baium = resolveBaium();
				boolean ok = baium != null && baium.enterFloor(active);
				if (ok) {
					session.teleportToCoordinates(active.x(), active.y(), active.z());
					session.send(new CreatureSay(0, CreatureSay.ALL, "Baium", "Voce entrou no 14o andar da Torre da Insolencia!"));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Baium",
							"Requisitos nao cumpridos (Blooded Fabric) ou Baium ja em combate."));
				}
				return true;
			}
			case "frintezza" -> {
				FrintezzaService frintezza = resolveFrintezza(session);
				List<PlayerCharacter> partyMembers = new ArrayList<>();
				if (session.party() != null) {
					for (var m : session.party().members()) {
						if (m.character() != null) {
							partyMembers.add(m.character());
						}
					}
				}
				boolean ok = frintezza != null && frintezza.enterTomb(active, partyMembers.isEmpty() ? null : partyMembers);
				if (ok) {
					session.teleportToCoordinates(active.x(), active.y(), active.z());
					session.send(new CreatureSay(0, CreatureSay.ALL, "Frintezza", "Voce entrou no Ultimo Tumulo Imperial!"));
				} else {
					session.send(new CreatureSay(0, CreatureSay.ALL, "Frintezza",
							"Requisitos nao cumpridos (Nivel 74+, Force Field Removal Scroll) ou Frintezza indisponivel."));
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private AntharasService resolveAntharas() {
		if (antharasService != null) return antharasService;
		if (BossManager.getInstance() != null && BossManager.getInstance().antharasService() != null) {
			return BossManager.getInstance().antharasService();
		}
		return null;
	}

	private ValakasService resolveValakas() {
		if (valakasService != null) return valakasService;
		if (BossManager.getInstance() != null && BossManager.getInstance().valakasService() != null) {
			return BossManager.getInstance().valakasService();
		}
		return null;
	}

	private BaiumService resolveBaium() {
		if (baiumService != null) return baiumService;
		if (BossManager.getInstance() != null && BossManager.getInstance().baiumService() != null) {
			return BossManager.getInstance().baiumService();
		}
		return null;
	}

	private FrintezzaService resolveFrintezza(GameSession session) {
		if (session != null && session.context() != null && session.context().frintezza() != null) {
			return session.context().frintezza();
		}
		if (frintezzaService != null) return frintezzaService;
		if (BossManager.getInstance() != null && BossManager.getInstance().frintezzaService() != null) {
			return BossManager.getInstance().frintezzaService();
		}
		return null;
	}

	public static String buildTvtHtml(TvtEventService tvt) {
		String base = tvt.buildStatusHtml();
		if (!base.contains("evt_tvt_join")) {
			String buttons = "<br><button value=\"Registrar\" action=\"bypass -h evt_tvt_join\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"><br>"
					+ "<button value=\"Sair\" action=\"bypass -h evt_tvt_leave\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
			return base.replace("</center></body></html>", buttons);
		}
		return base;
	}

	public static String buildCtfHtml(CtfEventService ctf) {
		String base = ctf.buildStatusHtml();
		if (!base.contains("evt_ctf_join")) {
			String buttons = "<br><button value=\"Registrar\" action=\"bypass -h evt_ctf_join\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"><br>"
					+ "<button value=\"Sair\" action=\"bypass -h evt_ctf_leave\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
			return base.replace("</center></body></html>", buttons);
		}
		return base;
	}

	public static String buildDmHtml(DmEventService dm) {
		String base = dm.buildStatusHtml();
		if (!base.contains("evt_dm_join")) {
			String buttons = "<br><button value=\"Registrar\" action=\"bypass -h evt_dm_join\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"><br>"
					+ "<button value=\"Sair\" action=\"bypass -h evt_dm_leave\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
			return base.replace("</center></body></html>", buttons);
		}
		return base;
	}

	public static String buildArenaHtml(ArenaDuelService arena, PlayerCharacter player) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">=== Coliseu 1x1 (Arena Duel) ===</font><br><br>");
		sb.append("Jogadores na fila: <font color=\"8FA888\">").append(arena.getQueueSize()).append("</font><br>");
		sb.append("Status: ").append(arena.isRegistered(player.objectId()) ? "<font color=\"8FA888\">Na Fila</font>" : "Fora da Fila").append("<br><br>");
		sb.append("<button value=\"Entrar na Fila 1x1\" action=\"bypass -h evt_arena_join\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"><br>");
		sb.append("<button value=\"Sair da Fila 1x1\" action=\"bypass -h evt_arena_leave\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"><br>");
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public static String buildDungeonHtml(DungeonService dungeonService) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">=== Dungeons Instanciadas ===</font><br><br>");
		var map = dungeonService.allDungeons();
		if (map.isEmpty()) {
			sb.append("Nenhuma dungeon carregada.<br>");
			sb.append("<button value=\"Entrar Dungeon #1\" action=\"bypass -h pve_dungeon_enter 1\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"><br>");
		} else {
			sb.append("<table width=270>");
			for (var d : map.values()) {
				sb.append("<tr>");
				sb.append("<td><font color=\"LEVEL\">").append(d.name()).append("</font> (").append(d.type()).append(")<br1>");
				sb.append("Taxa: ").append(d.entryFeeCount()).append(" Adena | CD: ").append(d.cooldownMinutes()).append("m</td>");
				sb.append("<td align=right><button value=\"Entrar\" action=\"bypass -h pve_dungeon_enter ").append(d.id())
						.append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
		}
		sb.append("</center></body></html>");
		return sb.toString();
	}

	public static String buildSoloFarmHtml() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>");
		sb.append("<font color=\"LEVEL\">=== Arena SoloFarm Instanciada ===</font><br><br>");
		sb.append("Custo por monstro: <font color=\"8FA888\">").append(SoloFarmService.PRICE_PER_MOB_ADENA).append(" Adena</font><br>");
		sb.append("Pacotes disponiveis (").append(SoloFarmService.MIN_BUY).append(" a ").append(SoloFarmService.MAX_BUY).append(" monstros):<br><br>");
		sb.append("<button value=\"Entrar SoloFarm (100 Mobs)\" action=\"bypass -h pve_solofarm_enter 100\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"><br>");
		sb.append("<button value=\"Comprar 100 Monstros\" action=\"bypass -h pve_solofarm_buy 100\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"><br>");
		sb.append("<button value=\"Comprar 500 Monstros\" action=\"bypass -h pve_solofarm_buy 500\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"><br>");
		sb.append("<button value=\"Comprar 1000 Monstros\" action=\"bypass -h pve_solofarm_buy 1000\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"><br>");
		sb.append("</center></body></html>");
		return sb.toString();
	}
}

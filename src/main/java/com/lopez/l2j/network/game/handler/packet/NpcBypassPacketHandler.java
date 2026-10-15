package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.combat.CombatService;
import com.lopez.l2j.game.door.DoorInstance;
import com.lopez.l2j.game.door.DoorTable;
import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.item.Inventory;
import com.lopez.l2j.game.item.ItemInstance;
import com.lopez.l2j.game.item.ItemSlots;
import com.lopez.l2j.game.item.ItemTemplate;
import com.lopez.l2j.game.model.ExperienceTable;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.model.PlayerStats;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.npc.NpcTemplate;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.skill.SkillTemplate;
import com.lopez.l2j.game.subclass.SubClass;
import com.lopez.l2j.game.teleport.TeleportLocation;
import com.lopez.l2j.game.teleport.TeleportLocationTable;
import com.lopez.l2j.game.template.CharTemplate;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBypassToServer;
import com.lopez.l2j.network.game.packet.GameClientPacket.RequestBBSwrite;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Handler modular para interacoes de dialogo com NPCs, telas HTML (Seven Signs, Festival,
 * Gatekeepers, Quests, Class Master, Buffer, etc.) e processamento de pacotes RequestBypassToServer.
 */
public class NpcBypassPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(NpcBypassPacketHandler.class);

	private final GameSession session;
	private int lastInteractedNpcObjId;

	public NpcBypassPacketHandler(GameSession session) {
		this.session = session;
	}

	private void send(GameServerPacket packet) {
		session.send(packet);
	}

	private PlayerCharacter active() {
		return session.activeChar();
	}

	private GameSession.Context ctx() {
		return session.context();
	}

	public void showNpcHtml(NpcInstance npc, int val) {
		if (npc != null) {
			this.lastInteractedNpcObjId = npc.objectId();
		}
		if (npc != null && showGrandBossEntryNpcHtml(npc)) {
			return;
		}
		if (com.lopez.l2j.game.boss.BossManager.getInstance() != null
				&& com.lopez.l2j.game.boss.BossManager.getInstance().showBossNpcHtml(active(), npc, p -> { if (p instanceof GameServerPacket gsp) send(gsp); })) {
			return;
		}
		if (isSevenSignsPriest(npc.npcId(), npc.name())) {
			showSevenSignsNpcHtml(npc, val);
			return;
		}
		if (npc.npcId() == 31111) {
			showGatekeeperSpiritInHtml(npc);
			return;
		}
		if (npc.npcId() == 31112) {
			showGatekeeperSpiritOutHtml(npc);
			return;
		}
		if (npc.npcId() == 31092) {
			showBlackMarketeerHtml(npc);
			return;
		}
		if (npc.npcId() == 31113) {
			showMerchantOfMammonHtml(npc);
			return;
		}
		if (npc.npcId() == 31126) {
			showBlacksmithOfMammonHtml(npc);
			return;
		}
		if ((npc.npcId() >= 31127 && npc.npcId() <= 31136) || (npc.npcId() >= 31137 && npc.npcId() <= 31146)) {
			showFestivalNpcHtml(npc, val);
			return;
		}
		int castleId = getNpcCastleId(npc);

		if (npc.npcId() == 30483 && val == 0) {
			if (active() != null && !active().isGm() && Config.CRUMA_TOWER_LEVEL_RESTRICT > 0 && active().level() >= Config.CRUMA_TOWER_LEVEL_RESTRICT) {
				String bigLvlHtm = ctx().htmls() != null ? ctx().htmls().getHtml("teleporter/30483-biglvl.htm") : null;
				if (bigLvlHtm == null) {
					bigLvlHtm = "<html><body>Gatekeeper Mozella:<br><br>You can't enter, your level is too high!<br><br>(Characters who are level %allowedmaxlvl% and above cannot enter in Cruma Tower.)</body></html>";
				}
				bigLvlHtm = bigLvlHtm.replace("%allowedmaxlvl%", String.valueOf(Config.CRUMA_TOWER_LEVEL_RESTRICT));
				String rendered = ctx().htmls() != null
						? ctx().htmls().render(bigLvlHtm, npc.objectId(), npc.name(), active().name(), castleId, npc.npcId())
						: bigLvlHtm;
				send(new NpcHtmlMessage(npc.objectId(), rendered));
				return;
			}
		}

		// 0. Village Newbie Guides (30598-30602): conclui etapa final do Tutorial (tutorial_quest_ex -> 5, 200 SS/100 SpS + 12 Newbie Travel Tokens, limpa seta do radar)
		if (val == 0 && ctx().questManager() != null
				&& com.lopez.l2j.game.quest.impl.Quest255Tutorial.VILLAGE_NEWBIE_GUIDES.contains(npc.npcId())) {
			var q255 = ctx().questManager().getQuest(com.lopez.l2j.game.quest.impl.Quest255Tutorial.QUEST_NAME);
			if (q255 instanceof com.lopez.l2j.game.quest.impl.Quest255Tutorial tutQuest) {
				tutQuest.onVillageNewbieGuideTalk(npc, session);
			}
		}

		// 1. Quests com first talk prioritário (Tutorial, Newbie Helper e diálogos dinâmicos)
		if (val == 0 && ctx().questManager() != null) {
			Collection<com.lopez.l2j.game.quest.Quest> firstTalkQuests = ctx().questManager().getQuestsForFirstTalkNpc(npc.npcId());
			if (firstTalkQuests == null || firstTalkQuests.isEmpty()) {
				firstTalkQuests = ctx().questManager().getAllQuests();
			}
			for (var q : firstTalkQuests) {
				if (q.hasFirstTalkNpc(npc.npcId())) {
					String qHtm = q.notifyFirstTalk(npc, session);
					if (qHtm != null && !qHtm.isBlank() && !"noquest".equalsIgnoreCase(qHtm) && !"no-quest".equalsIgnoreCase(qHtm)) {
						String resolved = resolveQuestHtml(q, qHtm, npc);
						if (resolved != null && !resolved.isBlank()) {
							String rendered = ctx().htmls() != null
									? ctx().htmls().render(resolved, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId(), q.getName())
									: resolved;
							send(new NpcHtmlMessage(npc.objectId(), rendered));
							return;
						}
					}
				}
			}
		}

		if (npc.npcId() == 50005 || (npc.template() != null && npc.template().type() != null
				&& npc.template().type().toLowerCase(java.util.Locale.ROOT).contains("buffervip"))) {
			boolean isVip = active() != null && (active().isVip() || active().isGm()
					|| (ctx().vip() != null && ctx().vip().isVip(active())));
			if (!isVip && ctx().htmls() != null) {
				String noVip = ctx().htmls().getHtml("mods/buffervip/no-vip.htm");
				if (noVip != null && !noVip.isBlank()) {
					String rendered = ctx().htmls().render(noVip, npc.objectId(), npc.name(),
							active() != null ? active().name() : "Player", castleId, npc.npcId());
					send(new NpcHtmlMessage(npc.objectId(), rendered));
					return;
				}
			}
		}

		if (ctx().htmls() != null) {
			String raw = ctx().htmls().getNpcHtml(npc.npcId(), npc.template().type(), val);
			if (raw != null && !raw.isBlank()) {
				String rendered = ctx().htmls().render(raw, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId());
				send(new NpcHtmlMessage(npc.objectId(), rendered));
				return;
			}
		}

		String defHtml = ctx().htmls() != null ? ctx().htmls().getHtml("npcdefault.htm") : null;
		if (defHtml == null) {
			defHtml = "<html><body>%npc_name%:<br><br>I have nothing to say to you.<br><a action=\"bypass -h npc_%objectId%_Quest\">Quest</a></body></html>";
		}
		String rendered = ctx().htmls() != null
				? ctx().htmls().render(defHtml, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId())
				: defHtml;
		send(new NpcHtmlMessage(npc.objectId(), rendered));
	}

	private boolean showGrandBossEntryNpcHtml(NpcInstance npc) {
		var bm = com.lopez.l2j.game.boss.BossManager.getInstance();
		int npcId = npc.npcId();
		switch (npcId) {
			case 13001 -> {
				var antharas = bm != null ? bm.antharasService() : null;
				String statusStr = antharas != null ? antharas.getStatus().name() : "NOTSPAWN";
				String html = "<html><body>Heart of Warding:<br>"
						+ "Status de Antharas: <font color=\"LEVEL\">" + statusStr + "</font><br>"
						+ "Apenas guerreiros portando a <font color=\"LEVEL\">Portal Stone</font> podem atravessar.<br><br>"
						+ "<a action=\"bypass -h epic_antharas_enter\">Entrar no Covil de Antharas</a><br>"
						+ "<a action=\"bypass -h boss_antharas_enter\">Enter Antharas' Lair</a>"
						+ "</body></html>";
				send(new NpcHtmlMessage(npc.objectId(), html));
				return true;
			}
			case 31385, 31540 -> {
				var valakas = bm != null ? bm.valakasService() : null;
				String statusStr = valakas != null ? valakas.getStatus().name() : "NOTSPAWN";
				String title = npcId == 31385 ? "Heart of Volcano" : "Watcher of Valakas Klein";
				String html = "<html><body>" + title + ":<br>"
						+ "Status de Valakas: <font color=\"LEVEL\">" + statusStr + "</font><br>"
						+ "Apenas campeoes portando a <font color=\"LEVEL\">Floating Stone</font> podem atravessar.<br><br>"
						+ "<a action=\"bypass -h epic_valakas_enter\">Entrar no Covil de Valakas</a><br>"
						+ "<a action=\"bypass -h boss_valakas_enter\">Enter Valakas' Lair</a>"
						+ "</body></html>";
				send(new NpcHtmlMessage(npc.objectId(), html));
				return true;
			}
			case 31862 -> {
				var baium = bm != null ? bm.baiumService() : null;
				String statusStr = baium != null ? baium.getStatus().name() : "NOTSPAWN";
				String html = "<html><body>Angelic Vortex:<br>"
						+ "Status de Baium: <font color=\"LEVEL\">" + statusStr + "</font><br>"
						+ "Requisito: <font color=\"LEVEL\">Blooded Fabric</font>.<br><br>"
						+ "<a action=\"bypass -h epic_baium_enter\">Subir ao 14o Andar (Baium)</a><br>"
						+ "<a action=\"bypass -h boss_baium_enter\">Ascend to the 14th Floor</a>"
						+ "</body></html>";
				send(new NpcHtmlMessage(npc.objectId(), html));
				return true;
			}
			case 29025 -> {
				var baium = bm != null ? bm.baiumService() : null;
				String statusStr = baium != null ? baium.getStatus().name() : "NOTSPAWN";
				String html = "<html><body>Stone Statue of Baium:<br>"
						+ "Status de Baium: <font color=\"LEVEL\">" + statusStr + "</font><br>"
						+ "O antigo imperador permanece petrificado em sono profundo.<br><br>"
						+ "<a action=\"bypass -h epic_baium_wake\">Despertar Baium</a><br>"
						+ "<a action=\"bypass -h boss_baium_awaken\">Touch the statue and awaken Baium</a>"
						+ "</body></html>";
				send(new NpcHtmlMessage(npc.objectId(), html));
				return true;
			}
			case 32011 -> {
				var frintezza = ctx() != null && ctx().frintezza() != null
						? ctx().frintezza()
						: (bm != null ? bm.frintezzaService() : null);
				String statusStr = frintezza != null ? frintezza.getStatus().name() : "ALIVE";
				String html = "<html><body>Imperial Tomb Guide:<br>"
						+ "Status do Tumulo Imperial (Frintezza): <font color=\"LEVEL\">" + statusStr + "</font><br>"
						+ "Requisito: Nivel 74+ e <font color=\"LEVEL\">Frintezza's Magic Force Field Removal Scroll</font>.<br><br>"
						+ "<a action=\"bypass -h epic_frintezza_enter\">Entrar no Salao Imperial (Frintezza)</a>"
						+ "</body></html>";
				send(new NpcHtmlMessage(npc.objectId(), html));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private boolean handleGrandBossEpicBypass(String cmd) {
		if (cmd == null || active() == null) {
			return false;
		}
		var bm = com.lopez.l2j.game.boss.BossManager.getInstance();
		switch (cmd) {
			case "epic_antharas_enter" -> {
				var antharas = bm != null ? bm.antharasService() : null;
				boolean ok = antharas != null && antharas.enterLair(active());
				if (ok) {
					teleportToCoordinates(active().x(), active().y(), active().z());
					send(new CreatureSay(0, CreatureSay.ALL, "Antharas", "Voce entrou no Covil de Antharas!"));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "Antharas", "Requisitos nao cumpridos (Portal Stone) ou covil inacessivel."));
				}
				return true;
			}
			case "epic_valakas_enter" -> {
				var valakas = bm != null ? bm.valakasService() : null;
				boolean ok = valakas != null && valakas.enterLair(active());
				if (ok) {
					teleportToCoordinates(active().x(), active().y(), active().z());
					send(new CreatureSay(0, CreatureSay.ALL, "Valakas", "Voce entrou no Covil de Valakas!"));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "Valakas", "Requisitos nao cumpridos (Floating Stone) ou covil inacessivel."));
				}
				return true;
			}
			case "epic_baium_enter" -> {
				var baium = bm != null ? bm.baiumService() : null;
				boolean ok = baium != null && baium.enterFloor(active());
				if (ok) {
					teleportToCoordinates(active().x(), active().y(), active().z());
					send(new CreatureSay(0, CreatureSay.ALL, "Baium", "Voce ascendeu ao 14o Andar da Torre da Insolencia!"));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "Baium", "Requisitos nao cumpridos (Blooded Fabric) ou Baium ja em combate."));
				}
				return true;
			}
			case "epic_baium_wake" -> {
				var baium = bm != null ? bm.baiumService() : null;
				boolean ok = baium != null && baium.awakenBaium(active());
				send(new CreatureSay(0, CreatureSay.ALL, "Baium",
						ok ? "Baium foi despertado!" : "Nao foi possivel despertar Baium."));
				return true;
			}
			case "epic_frintezza_enter" -> {
				var frintezza = ctx() != null && ctx().frintezza() != null
						? ctx().frintezza()
						: (bm != null ? bm.frintezzaService() : null);
				List<PlayerCharacter> partyMembers = new ArrayList<>();
				if (session.party() != null) {
					for (var m : session.party().members()) {
						if (m.character() != null) {
							partyMembers.add(m.character());
						}
					}
				}
				boolean ok = frintezza != null && frintezza.enterTomb(active(), partyMembers.isEmpty() ? null : partyMembers);
				if (ok) {
					teleportToCoordinates(active().x(), active().y(), active().z());
					send(new CreatureSay(0, CreatureSay.ALL, "Frintezza", "Voce entrou no Ultimo Tumulo Imperial!"));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "Frintezza", "Requisitos nao cumpridos (Nivel 74+ e Force Field Removal Scroll) ou tumulo inacessivel."));
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private boolean isSevenSignsPriest(int npcId, String name) {
		if (com.lopez.l2j.game.html.HtmCache.isSevenSignsNpc(npcId)) {
			return true;
		}
		if (name != null) {
			String lower = name.toLowerCase(java.util.Locale.ROOT);
			return lower.contains("dawn") || lower.contains("dusk");
		}
		return false;
	}

	private void showSevenSignsNpcHtml(NpcInstance npc, int val) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		boolean isDawn = (npc.npcId() >= 31078 && npc.npcId() <= 31084)
				|| npc.npcId() == 31168 || npc.npcId() == 31692 || npc.npcId() == 31694 || npc.npcId() == 31997
				|| (npc.name() != null && npc.name().toLowerCase(java.util.Locale.ROOT).contains("dawn"));

		var ss = ctx().sevenSigns();
		int playerCabal = com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int activePeriod = com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMPETITION;
		int compWinner = com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int sealGnosisOwner = com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;

		if (ss != null) {
			activePeriod = ss.activePeriod();
			compWinner = ss.previousWinner();
			sealGnosisOwner = ss.gnosisOwner();
			var pData = ss.getPlayerData(active().objectId()).orElse(null);
			if (pData != null) {
				playerCabal = pData.cabal();
			}
		}

		String prefix = isDawn ? "dawn_priest_" : "dusk_priest_";
		String file;

		if (isDawn) {
			if (playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) {
				if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS) {
					file = prefix + "5.htm";
				} else if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION) {
					if (compWinner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) {
						file = (compWinner != sealGnosisOwner) ? prefix + "2c.htm" : prefix + "2a.htm";
					} else {
						file = prefix + "2b.htm";
					}
				} else {
					file = prefix + "1b.htm";
				}
			} else if (playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) {
				file = prefix + "3a.htm";
			} else {
				if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS) {
					file = prefix + "5.htm";
				} else if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION) {
					file = (compWinner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) ? prefix + "4.htm" : prefix + "2b.htm";
				} else {
					file = prefix + "1a.htm";
				}
			}
		} else {
			if (playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) {
				if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS) {
					file = prefix + "5.htm";
				} else if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION) {
					if (compWinner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) {
						file = (compWinner != sealGnosisOwner) ? prefix + "2c.htm" : prefix + "2a.htm";
					} else {
						file = prefix + "2b.htm";
					}
				} else {
					file = prefix + "1b.htm";
				}
			} else if (playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) {
				file = prefix + "3a.htm";
			} else {
				if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS) {
					file = prefix + "5.htm";
				} else if (activePeriod == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION) {
					file = (compWinner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) ? prefix + "4.htm" : prefix + "2b.htm";
				} else {
					file = prefix + "1a.htm";
				}
			}
		}

		String path = "seven_signs/" + file;
		String htm = ctx().htmls() != null ? ctx().htmls().getHtml(path) : null;
		if (htm == null && ctx().htmls() != null) {
			htm = ctx().htmls().getIndexedHtml(file);
		}
		if (htm != null && ctx().htmls() != null) {
			String rendered = ctx().htmls().render(htm, npc.objectId(), npc.name(), active().name());
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			send(new NpcHtmlMessage(npc.objectId(), "<html><body>" + npc.name() + ":<br>The Seven Signs competition is underway.</body></html>"));
		}
	}

	private void showGatekeeperSpiritInHtml(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		var ss = ctx().sevenSigns();
		if (ss == null || !ss.isSealValidationPeriod()) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/spirit_null.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode passar neste momento."));
			}
			return;
		}
		int winner = ss.previousWinner();
		int avarice = ss.avariceOwner();
		int playerCabal = ss.getPlayerCabal(active().objectId());

		if (winner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN
				&& avarice == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN
				&& playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/spirit_dawn.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				return;
			}
		} else if (winner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK
				&& avarice == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK
				&& playerCabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/spirit_dusk.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				return;
			}
		}
		String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/spirit_null.htm") : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui permissao para passar."));
		}
	}

	private void showGatekeeperSpiritOutHtml(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/spirit_exit.htm") : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
		} else {
			teleportToCoordinates(172373, -17833, -4901);
		}
	}

	private void showMerchantOfMammonHtml(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		var ss = ctx().sevenSigns();
		int playerCabal = ss != null ? ss.getPlayerCabal(active().objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int compWinner = ss != null ? ss.previousWinner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int avariceOwner = ss != null ? ss.avariceOwner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		boolean isValidation = ss != null && ss.isSealValidationPeriod();

		boolean canAccess = active().isGm() || (isValidation && playerCabal != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL
				&& playerCabal == compWinner && avariceOwner == compWinner);
		String file = canAccess ? "seven_signs/mammmerch_1.htm" : "seven_signs/mammmerch_2.htm";
		String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), canAccess ? "May the blessings of Mammon be with you." : "Only members of the winning cabal possessing the Seal of Avarice may trade."));
		}
	}

	private void showBlacksmithOfMammonHtml(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		var ss = ctx().sevenSigns();
		int playerCabal = ss != null ? ss.getPlayerCabal(active().objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int compWinner = ss != null ? ss.previousWinner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		int gnosisOwner = ss != null ? ss.gnosisOwner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
		boolean isValidation = ss != null && ss.isSealValidationPeriod();

		boolean canAccess = active().isGm() || (isValidation && playerCabal != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL
				&& playerCabal == compWinner && gnosisOwner == compWinner);
		String file = canAccess ? "seven_signs/mammblack_1.htm" : "seven_signs/mammblack_2.htm";
		String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), canAccess ? "I can forge the finest weapons and armor for you." : "Only members of the winning cabal possessing the Seal of Gnosis may use my smithing services."));
		}
	}

	private void showBlackMarketeerHtml(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/blkmrkt_1.htm") : null;
		if (raw != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "Welcome to the black market."));
		}
	}

	private void showFestivalNpcHtml(NpcInstance npc, int val) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		boolean isWitch = (npc.npcId() >= 31132 && npc.npcId() <= 31136) || (npc.npcId() >= 31142 && npc.npcId() <= 31146);
		if (isWitch) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/festival_witch.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "Derrote monstros para coletar oferendas de sangue!"));
			}
			return;
		}

		boolean isDawn = (npc.npcId() >= 31127 && npc.npcId() <= 31131);
		int tier = isDawn ? Math.max(0, Math.min(4, npc.npcId() - 31127)) : Math.max(0, Math.min(4, npc.npcId() - 31137));
		var ss = ctx().sevenSigns();

		if (ss != null && ss.isSealValidationPeriod()) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/festival_2a.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				return;
			}
		}

		String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/festival_1.htm") : null;
		if (raw != null) {
			int blue = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_FEE_STONES[tier][0];
			int green = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_FEE_STONES[tier][1];
			int red = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_FEE_STONES[tier][2];
			String rendered = raw.replace("%blueStoneNeeded%", String.valueOf(blue))
					.replace("%greenStoneNeeded%", String.valueOf(green))
					.replace("%redStoneNeeded%", String.valueOf(red))
					.replace("%minFestivalPartyMembers%", "1")
					.replace("%festivalType%", isDawn ? "Lords of Dawn" : "Revolutionaries of Dusk");
			rendered = ctx().htmls().render(rendered, npc.objectId(), npc.name(), active().name());
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "Festival of Darkness - Nivel maximo: "
					+ com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_MAX_LEVELS[tier]));
		}
	}

	private void handleFestivalBypass(NpcInstance npc, String arg) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		var ss = ctx().sevenSigns();
		if (ss == null || !ss.isCompetitionPeriod()) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/festival_2a.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Inscricoes do festival estao fechadas."));
			}
			return;
		}

		boolean isDawn = (npc.npcId() >= 31127 && npc.npcId() <= 31131);
		int tier = isDawn ? Math.max(0, Math.min(4, npc.npcId() - 31127)) : Math.max(0, Math.min(4, npc.npcId() - 31137));
		String[] parts = arg.trim().split("\\s+");
		int action = parts.length > 0 ? GameSession.parseIntSafe(parts[0], 1) : 1;

		if (action == 1) {
			showFestivalNpcHtml(npc, 0);
			return;
		}

		if (action == 2) {
			int stoneId = parts.length > 1 ? GameSession.parseIntSafe(parts[1], 6360) : 6360;
			int stoneIdx = (stoneId == 6362) ? 2 : ((stoneId == 6361) ? 1 : 0);
			int cost = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_FEE_STONES[tier][stoneIdx];

			int playerCabal = ss.getPlayerCabal(active().objectId());
			int expectedCabal = isDawn ? com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK;
			if (playerCabal != expectedCabal) {
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/festival_2d.htm") : null;
				if (raw != null) {
					String r = raw.replace("%festivalType%", isDawn ? "Lords of Dawn" : "Revolutionaries of Dusk");
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(r, npc.objectId(), npc.name(), active().name())));
					return;
				}
			}

			if (active().level() > com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_MAX_LEVELS[tier]) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Seu nivel (" + active().level() + ") excede o limite deste tier ("
						+ com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_MAX_LEVELS[tier] + ")."));
				return;
			}

			if (!session.consumeItem(stoneId, cost)) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pedras de selo insuficientes para a taxa de entrada."));
				return;
			}

			int feePoints = cost * (stoneId == 6362 ? 10 : (stoneId == 6361 ? 5 : 3));
			ss.addAccumulatedBonus(tier, feePoints);

			int[] loc = com.lopez.l2j.game.sevensigns.SevenSignsManager.FESTIVAL_ARENA_LOCS[tier];
			teleportToCoordinates(loc[0], loc[1], loc[2]);
			send(new CreatureSay(0, CreatureSay.ALL, "Festival", "Sua equipe entrou no Festival of Darkness! Derrote os monstros e fale com a Bruxa ao terminar."));
			return;
		}

		if (action == 3) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/festival_3a.htm") : null;
			if (raw != null) {
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "O placar e registrado com a Bruxa ao concluir o desafio dentro da arena."));
			}
			return;
		}

		if (action == 4) {
			StringBuilder sb = new StringBuilder("<html><body>Festival Guide:<br>These are the top scores of the week, for the ");
			String tierName = switch (tier) {
				case 0 -> "Level 31 or lower";
				case 1 -> "Level 42 or lower";
				case 2 -> "Level 53 or lower";
				case 3 -> "Level 64 or lower";
				default -> "No Level Limit";
			};
			sb.append(tierName).append(" festival.<br><br>");
			int dScore = ss.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN, tier);
			int kScore = ss.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK, tier);
			sb.append(dScore > 0 ? "Dawn: Score " + dScore + "<br>" : "Dawn: No record exists. Score 0<br>");
			sb.append(kScore > 0 ? "Dusk: Score " + kScore + "<br>" : "Dusk: No record exists. Score 0<br>");
			sb.append("<br><a action=\"bypass -h npc_").append(npc.objectId()).append("_Chat 0\">Go back</a></body></html>");
			send(new NpcHtmlMessage(npc.objectId(), sb.toString()));
			return;
		}

		if (action == 5) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/festival_5.htm") : null;
			if (raw != null) {
				StringBuilder rows = new StringBuilder();
				for (int i = 0; i < 5; i++) {
					int dawn = ss.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN, i);
					int dusk = ss.getHighestScore(com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK, i);
					String tName = switch (i) {
						case 0 -> "31 or lower";
						case 1 -> "42 or lower";
						case 2 -> "53 or lower";
						case 3 -> "64 or lower";
						default -> "No limits";
					};
					String res = (dawn > dusk) ? "Children of Dawn" : (dusk > dawn ? "Children of Dusk" : "None");
					rows.append("<tr><td width=\"100\" align=\"center\">").append(tName)
							.append("</td><td align=\"center\" width=\"35\">").append(dusk)
							.append("</td><td align=\"center\" width=\"35\">").append(dawn)
							.append("</td><td align=\"center\" width=\"130\">").append(res)
							.append("</td></tr>");
				}
				String rendered = raw.replace("%statsTable%", rows.toString());
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(rendered, npc.objectId(), npc.name(), active().name())));
			}
			return;
		}

		if (action == 6) {
			String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/festival_6.htm") : null;
			if (raw != null) {
				StringBuilder rows = new StringBuilder();
				for (int i = 0; i < 5; i++) {
					int bonus = ss.getAccumulatedBonus(i);
					String tName = switch (i) {
						case 0 -> "31 or lower";
						case 1 -> "42 or lower";
						case 2 -> "53 or lower";
						case 3 -> "64 or lower";
						default -> "No limits";
					};
					rows.append("<tr><td align=\"center\" width=\"150\">").append(tName)
							.append("</td><td align=\"center\" width=\"150\">").append(bonus)
							.append("</td></tr>");
				}
				String rendered = raw.replace("%bonusTable%", rows.toString());
				send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(rendered, npc.objectId(), npc.name(), active().name())));
			}
			return;
		}

		if (action == 7) {
			teleportToCoordinates(83400, 147943, -3404);
		}
	}

	private void handleFestivalDescBypass(NpcInstance npc, String arg) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		String clean = arg.trim();
		if (clean.equals("6")) {
			send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "Mais monstros se aproximam! Prepare-se!"));
			return;
		}
		if (clean.equals("7")) {
			var bo = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.BLOOD_OFFERING_ID).findFirst().orElse(null);
			int count = bo != null ? (int) bo.count() : 0;
			if (bo != null) {
				ctx().inventories().destroyItem(active().inventory(), bo.objectId(), count, "FestivalOffering");
				send(ItemList.of(active().inventory().items(), true));
			}

			int tier = 0;
			if (npc.npcId() >= 31132 && npc.npcId() <= 31136) {
				tier = npc.npcId() - 31132;
			} else if (npc.npcId() >= 31142 && npc.npcId() <= 31146) {
				tier = npc.npcId() - 31142;
			} else if (npc.npcId() >= 31127 && npc.npcId() <= 31131) {
				tier = npc.npcId() - 31127;
			} else if (npc.npcId() >= 31137 && npc.npcId() <= 31141) {
				tier = npc.npcId() - 31137;
			}
			tier = Math.max(0, Math.min(4, tier));

			var ss = ctx().sevenSigns();
			if (ss != null && count > 0) {
				int cabal = ss.getPlayerCabal(active().objectId());
				java.util.List<String> memberNames = new java.util.ArrayList<>();
				if (session.party() != null) {
					for (var m : session.party().members()) {
						if (m.character() != null) {
							memberNames.add(m.character().name());
						}
					}
				}
				if (memberNames.isEmpty()) {
					memberNames.add(active().name());
				}
				ss.addFestivalScore(cabal, tier, count, memberNames);
			}

			boolean isDawn = (npc.npcId() >= 31132 && npc.npcId() <= 31136);
			int targetX = isDawn ? -80157 : -81261;
			int targetY = isDawn ? 111344 : 86531;
			int targetZ = isDawn ? -4901 : -5157;
			teleportToCoordinates(targetX, targetY, targetZ);
			send(new CreatureSay(0, CreatureSay.ALL, "Festival", "Desafio concluido! Voce entregou " + count + " Blood Offerings."));
			return;
		}
		String descHtm = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/festival/desc_" + clean + ".htm") : null;
		if (descHtm != null) {
			send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(descHtm, npc.objectId(), npc.name(), active().name())));
		}
	}

	private void handleSevenSignsDescBypass(NpcInstance npc, String arg) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		int val = 1;
		try {
			val = Integer.parseInt(arg.trim());
		} catch (Exception ignored) {
		}
		String path = "seven_signs/desc_" + val + ".htm";
		String raw = ctx().htmls() != null ? ctx().htmls().getHtml(path) : null;
		if (raw == null && ctx().htmls() != null) {
			raw = ctx().htmls().getIndexedHtml("desc_" + val + ".htm");
		}
		if (raw != null && ctx().htmls() != null) {
			String rendered = ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name());
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			showSevenSignsNpcHtml(npc, 0);
		}
	}

	private void handleSevenSignsBypass(NpcInstance npc, String arg) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		boolean isDawn = (npc.npcId() >= 31078 && npc.npcId() <= 31084)
				|| npc.npcId() == 31168 || npc.npcId() == 31692 || npc.npcId() == 31694 || npc.npcId() == 31997
				|| (npc.name() != null && npc.name().toLowerCase(java.util.Locale.ROOT).contains("dawn"));

		String[] parts = arg.trim().split("\\s+");
		int cmd = 0;
		try {
			if (parts.length > 0 && !parts[0].isBlank()) {
				cmd = Integer.parseInt(parts[0]);
			}
		} catch (Exception ignored) {
		}

		var ss = ctx().sevenSigns();

		switch (cmd) {
			case 1 -> {
				String file = "seven_signs/signs_1.htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 2 -> {
				long adena = active().inventory().adena();
				if (adena >= 500) {
					ctx().inventories().consumeItem(active().inventory(), 57, 500, "SevenSignsRecord");
					var added = ctx().inventories().addItem(active().inventory(), 5707, 1, "SevenSignsRecord");
					if (added != null) {
						send(new InventoryUpdate(List.of(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
						session.refreshWeightAndPenalties();
					}
					String file = isDawn ? "seven_signs/signs_2_dawn.htm" : "seven_signs/signs_2_dusk.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
					} else {
						showSevenSignsNpcHtml(npc, 0);
					}
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui Adena suficiente (500 Adena)."));
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 3, 33, 34 -> {
				if (ss != null && (ss.activePeriod() == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION
						|| ss.activePeriod() == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS)) {
					String noFile = isDawn ? "seven_signs/signs_33_dawn_no.htm" : "seven_signs/signs_33_dusk_no.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(noFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}
				int currentCabal = ss != null ? ss.getPlayerCabal(active().objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				if (currentCabal != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) {
					String memberFile = isDawn ? "seven_signs/signs_33_dawn_member.htm" : "seven_signs/signs_33_dusk_member.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(memberFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}
				String file = isDawn ? "seven_signs/signs_3_dawn.htm" : "seven_signs/signs_3_dusk.htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 19 -> {
				int seal = 1;
				if (parts.length > 2) {
					try {
						seal = Integer.parseInt(parts[2]);
					} catch (Exception ignored) {
					}
				} else if (parts.length > 1) {
					try {
						seal = Integer.parseInt(parts[1]);
					} catch (Exception ignored) {
					}
				}
				String sName = (seal == 2) ? "Gnosis" : (seal == 3 ? "Strife" : "Avarice");
				String file = "seven_signs/signs_19_" + sName + "_" + (isDawn ? "dawn" : "dusk") + ".htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 20 -> {
				StringBuilder sb = new StringBuilder("<html><body>");
				sb.append(isDawn ? "The Priest Of Dawn:<br><font color=\"LEVEL\">[ State Seals ]</font><br>"
						: "Priestess Of The Sunset:<br><font color=\"LEVEL\">[ State Seals ]</font><br>");
				if (ss != null) {
					sb.append("[Seal of Avarice: ").append(ss.avariceOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN ? "Dawn" : (ss.avariceOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK ? "Dusk" : "No owner")).append("]<br>");
					sb.append("[Seal of Gnosis: ").append(ss.gnosisOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN ? "Dawn" : (ss.gnosisOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK ? "Dusk" : "No owner")).append("]<br>");
					sb.append("[Seal of Strife: ").append(ss.strifeOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN ? "Dawn" : (ss.strifeOwner() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK ? "Dusk" : "No owner")).append("]<br>");
				}
				sb.append("<br><a action=\"bypass -h npc_").append(npc.objectId()).append("_Chat 0\">Back</a></body></html>");
				send(new NpcHtmlMessage(npc.objectId(), sb.toString()));
			}
			case 4 -> {
				if (ss != null && (ss.activePeriod() == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_SEAL_VALIDATION
						|| ss.activePeriod() == com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMP_RESULTS)) {
					String noFile = isDawn ? "seven_signs/signs_33_dawn_no.htm" : "seven_signs/signs_33_dusk_no.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(noFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}

				boolean hasCastle = active().clanId() > 0 && ctx().clans() != null
						&& ctx().clans().byClanId(active().clanId()).map(cl -> cl.castleId() > 0).orElse(false);

				if (isDawn) {
					if (!hasCastle) {
						var cert = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.CERTIFICATE_OF_APPROVAL_ID).findFirst().orElse(null);
						if (cert != null) {
							ctx().inventories().destroyItem(active().inventory(), cert.objectId(), 1, "SevenSignsDawnJoin");
							send(ItemList.of(active().inventory().items(), true));
						} else if (active().inventory().adena() >= 50000) {
							ctx().inventories().consumeItem(active().inventory(), 57, 50000, "SevenSignsDawnJoin");
							send(ItemList.of(active().inventory().items(), true));
						} else {
							String feeFile = "seven_signs/signs_33_dawn_fee.htm";
							String raw = ctx().htmls() != null ? ctx().htmls().getHtml(feeFile) : null;
							if (raw != null) {
								send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
							} else {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce necessita de um Certificado de Aprovacao ou 50.000 Adena para se juntar a Dawn."));
							}
							return;
						}
					}
				} else {
					if (hasCastle) {
						String noFile = "seven_signs/signs_33_dusk_no.htm";
						String raw = ctx().htmls() != null ? ctx().htmls().getHtml(noFile) : null;
						if (raw != null) {
							send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						} else {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Membros de clas com castelo nao podem se juntar a Dusk."));
						}
						return;
					}
				}

				int seal = 1;
				if (parts.length > 2) {
					try {
						seal = Integer.parseInt(parts[2]);
					} catch (Exception ignored) {
					}
				} else if (parts.length > 1) {
					try {
						seal = Integer.parseInt(parts[1]);
					} catch (Exception ignored) {
					}
				}
				int cabal = isDawn ? com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK;
				if (ss != null) {
					ss.registerPlayer(active().objectId(), cabal, seal);
				}
				String file = isDawn ? "seven_signs/signs_4_dawn.htm" : "seven_signs/signs_4_dusk.htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 5 -> {
				int cabal = ss != null ? ss.getPlayerCabal(active().objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				int myCabal = isDawn ? com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK;
				if (cabal != myCabal) {
					String noFile = isDawn ? "seven_signs/signs_5_dawn_no.htm" : "seven_signs/signs_5_dusk_no.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(noFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}
				String file = isDawn ? "seven_signs/signs_5_dawn.htm" : "seven_signs/signs_5_dusk.htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 6 -> {
				if (ss != null && ss.activePeriod() != com.lopez.l2j.game.sevensigns.SevenSignsManager.PERIOD_COMPETITION) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Pedras de selo so podem ser entregues durante o periodo de competicao."));
					showSevenSignsNpcHtml(npc, 0);
					return;
				}

				int subChoice = parts.length > 1 ? GameSession.parseIntSafe(parts[1], 4) : 4;
				int blueCount = 0;
				int greenCount = 0;
				int redCount = 0;

				var blueItem = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_BLUE_ID).findFirst().orElse(null);
				var greenItem = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_GREEN_ID).findFirst().orElse(null);
				var redItem = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_RED_ID).findFirst().orElse(null);

				if (subChoice == 1 && blueItem != null) {
					blueCount = (int) blueItem.count();
				} else if (subChoice == 2 && greenItem != null) {
					greenCount = (int) greenItem.count();
				} else if (subChoice == 3 && redItem != null) {
					redCount = (int) redItem.count();
				} else if (subChoice == 4) {
					blueCount = blueItem != null ? (int) blueItem.count() : 0;
					greenCount = greenItem != null ? (int) greenItem.count() : 0;
					redCount = redItem != null ? (int) redItem.count() : 0;
				}

				if (blueCount == 0 && greenCount == 0 && redCount == 0) {
					String noStones = isDawn ? "seven_signs/signs_6_dawn_no_stones.htm" : "seven_signs/signs_6_dusk_no_stones.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(noStones) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}

				if (blueCount > 0 && blueItem != null) ctx().inventories().destroyItem(active().inventory(), blueItem.objectId(), blueCount, "SevenSignsStones");
				if (greenCount > 0 && greenItem != null) ctx().inventories().destroyItem(active().inventory(), greenItem.objectId(), greenCount, "SevenSignsStones");
				if (redCount > 0 && redItem != null) ctx().inventories().destroyItem(active().inventory(), redItem.objectId(), redCount, "SevenSignsStones");

				if (ss != null) {
					ss.contributeStones(active().objectId(), blueCount, greenCount, redCount);
				}
				send(ItemList.of(active().inventory().items(), true));

				String file = isDawn ? "seven_signs/signs_6_dawn.htm" : "seven_signs/signs_6_dusk.htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 7 -> {
				long amount = parts.length > 1 ? GameSession.parseLongSafe(parts[1], 0) : 0;
				if (amount <= 0) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Quantidade invalida."));
					send(new ActionFailed());
					return;
				}
				var aaItem = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID).findFirst().orElse(null);
				if (aaItem == null || aaItem.count() < amount) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui Ancient Adena suficiente."));
					send(new ActionFailed());
					return;
				}
				var consumed = ctx().inventories().destroyItem(active().inventory(), aaItem.objectId(), (int) amount, "AncientAdenaToAdena");
				var added = ctx().inventories().addItem(active().inventory(), 57, (int) amount, "AncientAdenaToAdena");
				List<ItemInfo> updates = new ArrayList<>();
				if (consumed != null) updates.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
				if (added != null) updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
				if (!updates.isEmpty()) {
					send(new InventoryUpdate(updates));
				}
				session.refreshWeightAndPenalties();
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce trocou " + amount + " Ancient Adena por " + amount + " Adena."));
			}
			case 9 -> {
				int cabal = ss != null ? ss.getPlayerCabal(active().objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				int winner = ss != null ? ss.previousWinner() : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				boolean inValidation = ss != null && ss.isSealValidationPeriod();

				if (!inValidation || cabal != winner || winner == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) {
					String failedFile = isDawn ? "seven_signs/signs_9_dawn_b.htm" : "seven_signs/signs_9_dusk_b.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(failedFile) : null;
					if (raw == null && ctx().htmls() != null) raw = ctx().htmls().getHtml("seven_signs/signs_9_b.htm");
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}

				int aa = ss != null ? ss.claimAncientAdena(active().objectId()) : 0;
				if (aa > 0) {
					var added = ctx().inventories().addItem(active().inventory(), com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, aa, "SevenSignsReward");
					if (added != null) {
						send(new InventoryUpdate(List.of(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED))));
						session.refreshWeightAndPenalties();
					}
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce recebeu " + aa + " Ancient Adena!"));

					String file = isDawn ? "seven_signs/signs_9_dawn_a.htm" : "seven_signs/signs_9_dusk_a.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
					if (raw == null && ctx().htmls() != null) raw = ctx().htmls().getHtml("seven_signs/signs_9_a.htm");
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
					} else {
						showSevenSignsNpcHtml(npc, 0);
					}
				} else {
					String noStonesFile = isDawn ? "seven_signs/signs_9_dawn_b.htm" : "seven_signs/signs_9_dusk_b.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(noStonesFile) : null;
					if (raw == null && ctx().htmls() != null) raw = ctx().htmls().getHtml("seven_signs/signs_9_b.htm");
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
					} else {
						showSevenSignsNpcHtml(npc, 0);
					}
				}
			}
			case 10 -> {
				String htm;
				if (ss != null && ss.isSealValidationPeriod()) {
					htm = (ss.gnosisOwner() != com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) ? "seven_signs/" + npc.npcId() + "_gnosis.htm" : "seven_signs/" + npc.npcId() + ".htm";
				} else {
					int cabal = ss != null ? ss.getPlayerCabal(active().objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
					htm = (cabal == com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL) ? "seven_signs/" + (isDawn ? "dawn" : "dusk") + "_priest_tp_no.htm" : "seven_signs/" + npc.npcId() + ".htm";
				}
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(htm) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 17 -> {
				int sub = parts.length > 1 ? GameSession.parseIntSafe(parts[1], 1) : 1;
				if (sub == 4) {
					var bItem = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_BLUE_ID).findFirst().orElse(null);
					var gItem = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_GREEN_ID).findFirst().orElse(null);
					var rItem = active().inventory().items().stream().filter(it -> it.itemId() == com.lopez.l2j.game.sevensigns.SevenSignsManager.SEAL_STONE_RED_ID).findFirst().orElse(null);
					int bC = bItem != null ? (int) bItem.count() : 0;
					int gC = gItem != null ? (int) gItem.count() : 0;
					int rC = rItem != null ? (int) rItem.count() : 0;
					int totalAA = (bC * 3) + (gC * 5) + (rC * 10);
					if (totalAA > 0) {
						List<ItemInfo> updates = new ArrayList<>();
						if (bC > 0) {
							var c = ctx().inventories().destroyItem(active().inventory(), bItem.objectId(), bC, "ExchangeSS");
							if (c != null) updates.add(ItemInfo.of(c.item(), c.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						if (gC > 0) {
							var c = ctx().inventories().destroyItem(active().inventory(), gItem.objectId(), gC, "ExchangeSS");
							if (c != null) updates.add(ItemInfo.of(c.item(), c.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						if (rC > 0) {
							var c = ctx().inventories().destroyItem(active().inventory(), rItem.objectId(), rC, "ExchangeSS");
							if (c != null) updates.add(ItemInfo.of(c.item(), c.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						}
						var added = ctx().inventories().addItem(active().inventory(), com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, totalAA, "ExchangeSS");
						if (added != null) updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
						if (!updates.isEmpty()) {
							send(new InventoryUpdate(updates));
						}
						session.refreshWeightAndPenalties();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce recebeu " + totalAA + " Ancient Adena!"));
					} else {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui pedras de selo."));
					}
					return;
				}
				String color = (sub == 3) ? "red" : (sub == 2 ? "green" : "blue");
				int stoneId = (sub == 3) ? 6362 : (sub == 2 ? 6361 : 6360);
				int val = (sub == 3) ? 10 : (sub == 2 ? 5 : 3);
				var it = active().inventory().items().stream().filter(i -> i.itemId() == stoneId).findFirst().orElse(null);
				long count = it != null ? it.count() : 0;
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/signs_17.htm") : null;
				if (raw != null) {
					String rendered = raw.replace("%stoneColor%", color)
							.replace("%stoneValue%", String.valueOf(val))
							.replace("%stoneCount%", String.valueOf(count))
							.replace("%stoneItemId%", String.valueOf(stoneId));
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(rendered, npc.objectId(), npc.name(), active().name())));
				}
			}
			case 18 -> {
				if (parts.length >= 3) {
					int stoneId = GameSession.parseIntSafe(parts[1], 6360);
					long amount = GameSession.parseLongSafe(parts[2], 0);
					if (amount > 0) {
						var it = active().inventory().items().stream().filter(i -> i.itemId() == stoneId).findFirst().orElse(null);
						if (it != null && it.count() >= amount) {
							int rate = (stoneId == 6362) ? 10 : (stoneId == 6361 ? 5 : 3);
							long aaTotal = amount * rate;
							var consumed = ctx().inventories().destroyItem(active().inventory(), it.objectId(), (int) amount, "ExchangeSS");
							var added = ctx().inventories().addItem(active().inventory(), com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, (int) aaTotal, "ExchangeSS");
							List<ItemInfo> updates = new ArrayList<>();
							if (consumed != null) updates.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
							if (added != null) updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
							if (!updates.isEmpty()) {
								send(new InventoryUpdate(updates));
							}
							session.refreshWeightAndPenalties();
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce trocou " + amount + " pedras por " + aaTotal + " Ancient Adena!"));
							return;
						}
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Quantidade insuficiente de pedras de selo."));
			}
			case 21 -> {
				if (active().level() < 60) {
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml("seven_signs/signs_20.htm") : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}
				java.util.Calendar cal = java.util.Calendar.getInstance();
				int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
				boolean isNight = (hour >= 20 || hour < 4);
				String file = isNight ? "seven_signs/signs_23.htm" : "seven_signs/signs_22.htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				}
			}
			case 22 -> {
				long amount = parts.length > 1 ? GameSession.parseLongSafe(parts[1], 0) : 0;
				if (amount > 0 && amount <= 500000) {
					if (active().inventory().adena() >= amount) {
						var consumed = ctx().inventories().consumeItem(active().inventory(), 57, (int) amount, "BlackMarketeerAA");
						var added = ctx().inventories().addItem(active().inventory(), com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, (int) amount, "BlackMarketeerAA");
						List<ItemInfo> updates = new ArrayList<>();
						if (consumed != null) updates.add(ItemInfo.of(consumed.item(), consumed.removed() ? ItemInfo.REMOVED : ItemInfo.MODIFIED));
						if (added != null) updates.add(ItemInfo.of(added.item(), added.created() ? ItemInfo.ADDED : ItemInfo.MODIFIED));
						if (!updates.isEmpty()) {
							send(new InventoryUpdate(updates));
						}
						session.refreshWeightAndPenalties();
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce adquiriu " + amount + " Ancient Adena!"));
						return;
					}
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Adena insuficiente ou limite diario de 500.000 excedido."));
			}
			case 8 -> {
				int cabal = ss != null ? ss.getPlayerCabal(active().objectId()) : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_NULL;
				int requiredCabal = isDawn ? com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DAWN : com.lopez.l2j.game.sevensigns.SevenSignsManager.CABAL_DUSK;
				if (cabal != requiredCabal) {
					String noFile = isDawn ? "seven_signs/dawn_priest_3a.htm" : "seven_signs/dusk_priest_3a.htm";
					String raw = ctx().htmls() != null ? ctx().htmls().getHtml(noFile) : null;
					if (raw != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}
				String file = isDawn ? "seven_signs/signs_8_dawn.htm" : "seven_signs/signs_8_dusk.htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(file) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
				} else {
					showSevenSignsNpcHtml(npc, 0);
				}
			}
			case 11 -> {
				if (parts.length >= 4) {
					try {
						int x = Integer.parseInt(parts[1]);
						int y = Integer.parseInt(parts[2]);
						int z = Integer.parseInt(parts[3]);
						int cost = parts.length >= 5 ? Integer.parseInt(parts[4]) : 0;
						if (cost > 0) {
							if (!session.consumeItem(com.lopez.l2j.game.sevensigns.SevenSignsManager.ANCIENT_ADENA_ID, cost)) {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao possui Ancient Adena suficiente (" + cost + " necessarias)."));
								send(new ActionFailed());
								return;
							}
						}
						if (ss != null && !active().isGm() && ss.isInside7sDungeon(x, y, z)) {
							boolean isNecropolis = false;
							for (var mLoc : com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_MERCHANT_LOCS) {
								double distSq = Math.pow(x - mLoc.x(), 2) + Math.pow(y - mLoc.y(), 2);
								if (distSq <= 16_000_000) {
									isNecropolis = true;
									break;
								}
							}
							var access = ss.checkDungeonEntry(active().objectId(), isNecropolis);
							if (access != com.lopez.l2j.game.sevensigns.SevenSignsManager.DungeonAccess.ALLOWED) {
								String htm = isNecropolis ? "seven_signs/necro_no.htm" : "seven_signs/cata_no.htm";
								String raw = ctx().htmls() != null ? ctx().htmls().getHtml(htm) : null;
								if (raw != null) {
									send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(raw, npc.objectId(), npc.name(), active().name())));
								} else {
									send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao tem permissao para entrar nos Sete Selos."));
								}
								send(new ActionFailed());
								return;
							}
						}
						teleportToCoordinates(x, y, z);
						return;
					} catch (Exception ex) {
						log.warn("Erro ao processar SevenSigns 11 bypass: {}", arg, ex);
					}
				}
				showSevenSignsNpcHtml(npc, 0);
			}
			default -> showSevenSignsNpcHtml(npc, 0);
		}
	}


	public void onBbsWrite(RequestBBSwrite p) {
		session.chatHandler().handleBbsWrite(p);
	}

	private void handleAioBuffBypass(String cmd) {
		if (active() == null || ctx().aio() == null) {
			return;
		}
		if (!active().isAio()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Apenas personagens AIO podem utilizar esta acao."));
			return;
		}
		boolean inPeace = ctx().zones() == null || ctx().zones().isInsidePeace(active().x(), active().y(), active().z());
		if (!ctx().aio().canCastBuffs(active(), inPeace)) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Personagens AIO apenas podem usar habilidades de buff dentro de Peace Zones (Cidades)."));
			return;
		}
		String[] parts = cmd.trim().split("\\s+");
		if (parts.length < 2) {
			return;
		}
		try {
			int skillId = Integer.parseInt(parts[1]);
			var sk = ctx().skillService() != null ? ctx().skillService().known(active(), skillId).orElse(null) : null;
			if (sk == null && ctx().skillService() != null) {
				int lvl = ctx().aio().getAioSkillMap().getOrDefault(skillId, 1);
				sk = ctx().skillService().skill(skillId, lvl).orElse(null);
			}
			if (sk != null) {
				session.castSkill(sk, true);
			}
		} catch (Exception e) {
			log.warn("Erro ao processar bypass voiced_aiobuff: {}", e.getMessage());
		}
	}

	public void handleBbsCommand(String command) {
		if (ctx().communityBoard() != null && active() != null) {
			String html = ctx().communityBoard().handleCommand(active(), command);
			if (html != null) {
				send(new GameServerPacket.ShowBoard(html));
			}
		}
	}

	public void onBypass(RequestBypassToServer p) {
		if (!session.inWorld() || p.command() == null || p.command().isBlank()) {
			send(new ActionFailed());
			return;
		}
		String rawCmd = p.command().trim();
		String cmd = rawCmd;
		if (ctx().bypassEncoder() != null) {
			var dec = ctx().bypassEncoder().decode(rawCmd, session.sessionBypasses(), false);
			if (!dec.isValid()) {
				log.warn("Tentativa de bypass invalido/forjado de {} (char: {}): '{}'", session.ip(),
						active() != null ? active().name() : "null", rawCmd);
				send(new ActionFailed());
				return;
			}
			cmd = dec.command();
		}
		if (cmd.startsWith("-h ")) {
			cmd = cmd.substring(3).trim();
		} else if (cmd.startsWith("-h")) {
			cmd = cmd.substring(2).trim();
		}
		if (handleGrandBossEpicBypass(cmd)) {
			return;
		}
		if (ctx().bypassHandlers() != null && ctx().bypassHandlers().execute(cmd, session)) {
			return;
		}
		if (com.lopez.l2j.game.boss.BossManager.getInstance() != null
				&& com.lopez.l2j.game.boss.BossManager.getInstance().handleBypass(session, cmd)) {
			return;
		}
		if (cmd.startsWith("admin_")) {
			session.handleAdminCommand(cmd.substring(6).trim());
			return;
		}
		if (cmd.startsWith("voiced_menutoggle ")) {
			if (ctx().preferences() != null && active() != null) {
				String toggleType = cmd.substring(18).trim().toLowerCase(java.util.Locale.ROOT);
				switch (toggleType) {
					case "autoloot" -> ctx().preferences().toggleAutoLoot(active().objectId());
					case "trade" -> ctx().preferences().toggleTradeRefusal(active().objectId());
					case "blockbuff" -> ctx().preferences().toggleBlockBuffs(active().objectId());
					case "party" -> ctx().preferences().toggleBlockParty(active().objectId());
					case "exp" -> ctx().preferences().toggleBlockExp(active().objectId());
				}
				send(new NpcHtmlMessage(0, ctx().preferences().buildMenuHtml(active().objectId(), active().name())));
			}
			return;
		}
		if (cmd.startsWith("antibot_validate ")) {
			if (ctx().botsPrevention() != null && active() != null) {
				int selected = Integer.parseInt(cmd.substring(17).trim());
				boolean ok = ctx().botsPrevention().validateAnswer(active().objectId(), selected);
				if (ok) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Validacao Anti-Bot bem-sucedida!"));
				} else {
					var punishment = ctx().botsPrevention().checkPunishment(active().objectId());
					punishment.ifPresent(pType -> {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Validacao Anti-Bot falhou. Punicao: " + pType.name()));
						if (pType == com.lopez.l2j.game.service.BotsPreventionService.PunishmentType.KICK) {
							session.kick();
						}
					});
				}
			}
			return;
		}
		if (cmd.equals("voiced_tvtjoin") && ctx().tvt() != null && active() != null) {
			var res = ctx().tvt().register(active());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "TvT: " + res.name()));
			send(new NpcHtmlMessage(0, ctx().tvt().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_tvtleave") && ctx().tvt() != null && active() != null) {
			boolean ok = ctx().tvt().unregister(active());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao no TvT cancelada." : "Nao inscrito no TvT."));
			send(new NpcHtmlMessage(0, ctx().tvt().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_ctfjoin") && ctx().ctf() != null && active() != null) {
			var res = ctx().ctf().register(active());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "CTF: " + res.name()));
			send(new NpcHtmlMessage(0, ctx().ctf().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_ctfleave") && ctx().ctf() != null && active() != null) {
			boolean ok = ctx().ctf().unregister(active());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao no CTF cancelada." : "Nao inscrito no CTF."));
			send(new NpcHtmlMessage(0, ctx().ctf().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_dmjoin") && ctx().dm() != null && active() != null) {
			var res = ctx().dm().register(active());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "DM: " + res.name()));
			send(new NpcHtmlMessage(0, ctx().dm().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_dmleave") && ctx().dm() != null && active() != null) {
			boolean ok = ctx().dm().unregister(active());
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Inscricao no DM cancelada." : "Nao inscrito no DM."));
			send(new NpcHtmlMessage(0, ctx().dm().buildStatusHtml()));
			return;
		}
		if (cmd.equals("voiced_getaiogoods") && ctx().aio() != null && active() != null && active().isAio()) {
			for (var item : ctx().aio().getAioGoods()) {
				ctx().inventories().addItem(active().inventory(), item.itemId(), item.count(), "AioGoods");
			}
			send(ItemList.of(active().inventory().items(), false));
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Consumiveis de AIOx entregues no inventario."));
			return;
		}
		if (cmd.startsWith("voiced_aiobuff") && ctx().aio() != null && active() != null) {
			handleAioBuffBypass(cmd);
			return;
		}
		if (cmd.startsWith("_bbs") || cmd.startsWith("bbs_")) {
			handleBbsCommand(cmd);
			return;
		}
		if (cmd.startsWith("achieve_claim ")) {
			if (ctx().achievements() != null && active() != null) {
				var res = ctx().achievements().claimAchievement(active(), cmd.substring(14).trim());
				if (res.success()) {
					send(ItemList.of(active().inventory().items(), false));
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", res.message()));
				send(new NpcHtmlMessage(0, ctx().achievements().generateHtml(active())));
			}
			return;
		}
		if (cmd.startsWith("event_exchange ")) {
			if (ctx().officialEvent() != null && active() != null) {
				int exId = Integer.parseInt(cmd.substring(15).trim());
				var res = ctx().officialEvent().exchangeReward(active(), exId);
				if (res.success()) {
					send(ItemList.of(active().inventory().items(), false));
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", res.message()));
				send(new NpcHtmlMessage(0, ctx().officialEvent().generateHtml(active())));
			}
			return;
		}
		if (cmd.startsWith("l2day_exchange ")) {
			if (ctx().l2day() != null && active() != null) {
				String word = cmd.substring(15).trim();
				var res = ctx().l2day().exchangeWord(active(), word);
				if (res.success()) {
					send(ItemList.of(active().inventory().items(), false));
				}
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", res.message()));
				send(new NpcHtmlMessage(0, ctx().l2day().generateHtml(active())));
			}
			return;
		}
		if (cmd.startsWith("voiced_roulette")) {
			if (ctx().roulette() != null && active() != null) {
				if (cmd.contains("spin")) {
					var result = ctx().roulette().spin(active());
					if (result.success()) {
						send(ItemList.of(active().inventory().items(), false));
					}
					send(new NpcHtmlMessage(0, ctx().roulette().generateResultHtml(result)));
				} else {
					send(new NpcHtmlMessage(0, ctx().roulette().generateMainHtml(active())));
				}
			}
			return;
		}
		if (cmd.startsWith("voiced_reset")) {
			if (ctx().characterReset() != null && active() != null) {
				boolean ok = ctx().characterReset().performReset(active());
				if (ok) {
					send(ItemList.of(active().inventory().items(), false));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Reset efetuado com sucesso! Nivel reiniciado para 1."));
					send(new UserInfo(active(), ctx().characters().template(active())));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel realizar o reset."));
				}
			}
			return;
		}
		if (cmd.startsWith("wh_passkey_unlock ")) {
			if (active() != null) {
				String pin = cmd.substring("wh_passkey_unlock ".length()).trim();
				boolean ok = com.lopez.l2j.game.service.WarehousePasskeyService.getInstance().verifyPasskey(active().objectId(), pin);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Armazem desbloqueado com sucesso!" : "Senha incorreta."));
			}
			return;
		}
		if (cmd.startsWith("wh_passkey_set ")) {
			if (active() != null) {
				String pin = cmd.substring("wh_passkey_set ".length()).trim();
				boolean ok = com.lopez.l2j.game.service.WarehousePasskeyService.getInstance().setPasskey(active().objectId(), pin);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Senha do armazem definida com sucesso!" : "PIN invalido."));
			}
			return;
		}
		if (cmd.startsWith("learn_clan_skill ")) {
			String[] parts = cmd.substring("learn_clan_skill ".length()).trim().split("\\s+");
			int skillId = Integer.parseInt(parts[0]);
			int skillLvl = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
			session.partyClanHandler().handleLearnClanSkill(skillId, skillLvl);
			return;
		}
		if (cmd.startsWith("create_subpledge ")) {
			String[] parts = cmd.substring("create_subpledge ".length()).trim().split("\\s+");
			int pledgeType = Integer.parseInt(parts[0]);
			String subName = parts.length > 1 ? parts[1] : "SubUnit";
			String leaderName = parts.length > 2 ? parts[2] : "";
			session.partyClanHandler().handleCreateSubPledge(pledgeType, subName, leaderName);
			return;
		}
		if (cmd.startsWith("ch_activate_func ")) {
			if (ctx().clanHallFunctions() != null && active() != null) {
				String[] parts = cmd.substring("ch_activate_func ".length()).trim().split("\\s+");
				int hallId = Integer.parseInt(parts[0]);
				int funcType = Integer.parseInt(parts[1]);
				int funcLvl = Integer.parseInt(parts[2]);
				if (ctx().clanHallFunctions().activateFunction(active(), hallId, funcType, funcLvl)) {
					send(ItemList.of(active().inventory().items(), false));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Funcao do Clan Hall ativada com sucesso."));
				}
			}
			return;
		}
		if (cmd.startsWith("ch_siege_register ")) {
			if (ctx().clanHallSieges() != null && active() != null && active().clanId() > 0) {
				int hallId = Integer.parseInt(cmd.substring("ch_siege_register ".length()).trim());
				boolean ok = ctx().clanHallSieges().registerClan(active(), hallId) == com.lopez.l2j.game.clan.clanhall.siege.ClanHallSiegeService.RegisterResult.SUCCESS;
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Cla registrado na Siege do Clan Hall!" : "Nao foi possivel registrar na Siege."));
			}
			return;
		}
		if (cmd.startsWith("merc_buy_ticket ")) {
			if (ctx().mercenaries() != null && active() != null) {
				String[] parts = cmd.substring("merc_buy_ticket ".length()).trim().split("\\s+");
				int castleId = Integer.parseInt(parts[0]);
				int ticketId = Integer.parseInt(parts[1]);
				int count = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
				if (ctx().mercenaries().buyMercenaryTicket(active(), castleId, ticketId, count)) {
					send(ItemList.of(active().inventory().items(), false));
				}
			}
			return;
		}
		if (cmd.startsWith("castle_claim_crown ")) {
			if (ctx().crowns() != null && active() != null) {
				int castleId = Integer.parseInt(cmd.substring("castle_claim_crown ".length()).trim());
				ctx().crowns().rewardLordCrown(active(), castleId);
				send(ItemList.of(active().inventory().items(), false));
			}
			return;
		}
		if (cmd.startsWith("fort_activate_func ")) {
			if (ctx().fortresses() != null && active() != null) {
				String[] parts = cmd.substring("fort_activate_func ".length()).trim().split("\\s+");
				int fortId = Integer.parseInt(parts[0]);
				int type = Integer.parseInt(parts[1]);
				int level = Integer.parseInt(parts[2]);
				int lease = parts.length > 3 ? Integer.parseInt(parts[3]) : 50_000;
				if (ctx().fortresses().activateFunction(active(), fortId, type, level, lease, 1, 7L * 86_400_000L)) {
					send(ItemList.of(active().inventory().items(), false));
				}
			}
			return;
		}
		if (cmd.startsWith("fort_claim_blood_oath ")) {
			if (ctx().fortresses() != null && active() != null) {
				int fortId = Integer.parseInt(cmd.substring("fort_claim_blood_oath ".length()).trim());
				if (ctx().fortresses().rewardBloodOath(active(), fortId, 1)) {
					send(ItemList.of(active().inventory().items(), false));
				}
			}
			return;
		}
		if (cmd.startsWith("fort_siege_register ")) {
			if (ctx().fortressSieges() != null && active() != null) {
				int fortId = Integer.parseInt(cmd.substring("fort_siege_register ".length()).trim());
				var clan = ctx().clans() != null ? ctx().clans().byClanId(active().clanId()).orElse(null) : null;
				if (clan != null && ctx().fortressSieges().registerAttacker(fortId, active(), clan)) {
					send(ItemList.of(active().inventory().items(), false));
				}
			}
			return;
		}
		if (cmd.startsWith("rift_enter ")) {
			if (ctx().dimensionalRift() != null && active() != null) {
				byte tierId = Byte.parseByte(cmd.substring("rift_enter ".length()).trim());
				var tier = com.lopez.l2j.game.instance.dimensionalrift.RiftTier.fromId(tierId);
				if (tier != null) {
					List<PlayerCharacter> members = new ArrayList<>();
					if (session.party() != null) {
						for (GameSession gs : session.party().members()) {
							if (gs != null && gs.active() != null) {
								members.add(gs.active());
							}
						}
					}
					if (members.isEmpty()) {
						members.add(active());
					}
					ctx().dimensionalRift().enterRift(active(), tier, members);
				}
			}
			return;
		}
		if (cmd.equals("rift_jump") && ctx().dimensionalRift() != null && active() != null) {
			List<PlayerCharacter> members = List.of(active());
			ctx().dimensionalRift().manualJump(active(), members);
			return;
		}
		if (cmd.equals("rift_leave") && ctx().dimensionalRift() != null && active() != null) {
			List<PlayerCharacter> members = List.of(active());
			ctx().dimensionalRift().leaveRift(active(), members);
			return;
		}
		if (cmd.startsWith("sepulcher_enter ")) {
			if (ctx().fourSepulchers() != null && active() != null) {
				int sepId = Integer.parseInt(cmd.substring("sepulcher_enter ".length()).trim());
				List<PlayerCharacter> members = new ArrayList<>();
				if (session.party() != null) {
					for (GameSession gs : session.party().members()) {
						if (gs != null && gs.active() != null) {
							members.add(gs.active());
						}
					}
				}
				if (members.isEmpty()) {
					members.add(active());
				}
				ctx().fourSepulchers().tryEnter(sepId, active(), members);
			}
			return;
		}
		if (cmd.equals("sepulcher_goblets") && ctx().fourSepulchers() != null && active() != null) {
			if (ctx().fourSepulchers().combineFourGoblets(active())) {
				send(ItemList.of(active().inventory().items(), false));
			}
			return;
		}
		if (cmd.startsWith("doll_upgrade ")) {
			if (active() != null) {
				int dollItemId = Integer.parseInt(cmd.substring("doll_upgrade ".length()).trim());
				var upgraded = com.lopez.l2j.game.pve.DollService.getInstance().upgradeDoll(active(), dollItemId);
				if (upgraded.isPresent()) {
					send(ItemList.of(active().inventory().items(), false));
					session.sendUserInfoAndBroadcastCharInfo();
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Boss Doll evoluida para Lv." + upgraded.get().level() + "!"));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce precisa de 2 Dolls iguais na mochila para evoluir."));
				}
			}
			return;
		}
		if (cmd.startsWith("lottery_buy ")) {
			if (ctx().lottery() != null && active() != null) {
				String[] parts = cmd.substring("lottery_buy ".length()).trim().split("\\s+");
				Set<Integer> nums = new java.util.HashSet<>();
				for (String partStr : parts) {
					nums.add(Integer.parseInt(partStr));
				}
				if (ctx().lottery().buyTicket(active(), nums)) {
					send(ItemList.of(active().inventory().items(), false));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Bilhete de loteria adquirido!"));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel comprar o bilhete de loteria."));
				}
			}
			return;
		}
		if (cmd.startsWith("lottery_claim ")) {
			if (ctx().lottery() != null && active() != null) {
				int round = Integer.parseInt(cmd.substring("lottery_claim ".length()).trim());
				int prize = ctx().lottery().claimPrize(active(), round);
				if (prize > 0) {
					send(ItemList.of(active().inventory().items(), false));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Premio de " + prize + " Adena recebido!"));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhum premio disponivel para esta rodada."));
				}
			}
			return;
		}
		if (cmd.startsWith("monrace_bet ")) {
			if (ctx().monsterRace() != null && active() != null) {
				String[] parts = cmd.substring("monrace_bet ".length()).trim().split("\\s+");
				int lane = Integer.parseInt(parts[0]);
				int amount = Integer.parseInt(parts[1]);
				if (ctx().monsterRace().placeBet(active(), lane, amount)) {
					send(ItemList.of(active().inventory().items(), false));
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Aposta registrada na raia " + lane + "!"));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel registrar a aposta."));
				}
			}
			return;
		}
		if (cmd.equals("monrace_run") && ctx().monsterRace() != null && active() != null) {
			int payout = ctx().monsterRace().runRaceAndSettlePlayer(active());
			if (payout > 0) {
				send(ItemList.of(active().inventory().items(), false));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce venceu " + payout + " Adena na Monster Race!"));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Corrida encerrada."));
			}
			return;
		}
		if (cmd.equals("fishchamp_claim") && ctx().fishingChampionship() != null && active() != null) {
			int reward = ctx().fishingChampionship().claimReward(active());
			if (reward > 0) {
				send(ItemList.of(active().inventory().items(), false));
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Recompensa do Campeonato de Pesca recebida: " + reward + " Adena!"));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nenhuma recompensa de pesca disponivel no momento."));
			}
			return;
		}
		if (cmd.startsWith("boat_board ")) {
			if (ctx().boat() != null && active() != null) {
				String[] parts = cmd.substring("boat_board ".length()).trim().split("\\s+");
				int boatId = Integer.parseInt(parts[0]);
				int ticketId = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
				int priceAdena = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
				if (ctx().boat().boardAndTravel(active(), boatId, ticketId, priceAdena)) {
					send(ItemList.of(active().inventory().items(), false));
				}
			}
			return;
		}
		if (cmd.startsWith("Link ") || cmd.startsWith("player_help ") || cmd.startsWith("player_help")) {
			String path = cmd.startsWith("Link ")
					? cmd.substring(5).trim()
					: (cmd.length() > 12 ? cmd.substring(12).trim() : "");
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			if (ctx().htmls() != null && !path.isEmpty()) {
				String htm = ctx().htmls().getHtml(path);
				if (htm == null) {
					htm = ctx().htmls().getHtml("help/" + path);
				}
				if (htm == null) {
					htm = ctx().htmls().getIndexedHtml(path);
				}
				if (htm == null) {
					htm = ctx().htmls().getHtml("default/" + path);
				}
				if (htm != null) {
					int npcObjId = active() != null ? active().objectId() : 0;
					String rendered = ctx().htmls().render(htm, npcObjId, "NPC",
							active() != null ? active().name() : "Player");
					send(new NpcHtmlMessage(npcObjId, rendered));
					return;
				}
			}
		}
		if (cmd.startsWith("scripts_Util:QuestGatekeeper") || cmd.startsWith("scripts_Util:Gatekeeper")) {
			handleScriptGatekeeper(cmd);
			return;
		}
		if (cmd.startsWith("operate_door")) {
			handleOperateDoor(cmd.length() > 12 ? cmd.substring(12).trim() : "");
			return;
		}
		if (cmd.startsWith("open_gate")) {
			handleDoorControl(true);
			return;
		}
		if (cmd.startsWith("scripts_services.Buffer:actBBS") || cmd.startsWith("scripts_services.Buffer:act")) {
			boolean isBbs = cmd.startsWith("scripts_services.Buffer:actBBS");
			String bArgs = isBbs
					? (cmd.length() > 30 ? cmd.substring(30).trim() : "")
					: (cmd.length() > 27 ? cmd.substring(27).trim() : "");
			int targetNpcId = session.targetObjectId() > 0 ? session.targetObjectId() : this.lastInteractedNpcObjId;
			var npc = targetNpcId > 0 && ctx().world() != null ? ctx().world().npc(targetNpcId).orElse(null) : null;
			if (ctx().schemeBuffer() != null) {
				ctx().schemeBuffer().handleLuceraBufferAct(active(), session, npc, bArgs, isBbs);
			}
			return;
		}
		if (cmd.startsWith("scripts_services.SupportMagic") || cmd.startsWith("SupportMagic")) {
			var npc = session.targetObjectId() > 0 && ctx().world() != null ? ctx().world().npc(session.targetObjectId()).orElse(null) : null;
			if (cmd.toLowerCase(java.util.Locale.ROOT).contains("protectionblessing")) {
				handleNewbieProtectionBlessing(npc);
			} else {
				handleNewbieSupportMagic(npc);
			}
			return;
		}
		if (cmd.startsWith("manor_menu_select") || cmd.startsWith("menu_select")) {
			var npc = session.targetObjectId() > 0 && ctx().world() != null ? ctx().world().npc(session.targetObjectId()).orElse(null) : null;
			if (cmd.contains("ask=-7") || cmd.contains("ask=255")) {
				handleNewbieGuideMenu(npc, cmd);
			} else if (!tryHandleQuestMenuSelect(npc, cmd)) {
				handleManorMenu(npc, cmd);
			}
			return;
		}
		if (cmd.equals("open_doors") || cmd.startsWith("open_doors")) {
			handleDoorControl(true);
			return;
		}
		if (cmd.equals("close_doors") || cmd.startsWith("close_doors")) {
			handleDoorControl(false);
			return;
		}
		if (cmd.startsWith("Quest ") || cmd.equals("Quest")) {
			String questArg = cmd.length() > 5 ? cmd.substring(5).trim() : "";
			int targetObjId = session.targetObjectId() > 0 ? session.targetObjectId() : this.lastInteractedNpcObjId;
			handleQuestBypass(targetObjId, questArg);
			return;
		}
		if (cmd.startsWith("Subclass ") || cmd.equals("Subclass")) {
			String subArg = cmd.length() > 8 ? cmd.substring(8).trim() : "";
			handleSubclassBypass(session.targetObjectId(), subArg);
			return;
		}
		if (cmd.startsWith("create_clan ") || cmd.startsWith("create_pledge ")) {
			String clanName = cmd.substring(cmd.indexOf(' ') + 1).trim();
			session.createClan(clanName);
			return;
		}
		if (cmd.equals("increase_clan_level") || cmd.startsWith("increase_clan_level ")) {
			session.increaseClanLevel();
			return;
		}
		if (cmd.equals("dissolve_clan") || cmd.startsWith("dissolve_clan ")) {
			session.dissolveClan();
			return;
		}
		if (cmd.startsWith("1stClass")) {
			showClassMasterMenu(session.targetObjectId(), 1);
			return;
		}
		if (cmd.startsWith("2ndClass")) {
			showClassMasterMenu(session.targetObjectId(), 2);
			return;
		}
		if (cmd.startsWith("3rdClass")) {
			showClassMasterMenu(session.targetObjectId(), 3);
			return;
		}
		if (cmd.startsWith("change_class")) {
			try {
				int targetClassId = Integer.parseInt(cmd.replace("change_class", "").trim());
				handleChangeClass(session.targetObjectId(), targetClassId);
				return;
			} catch (NumberFormatException ignored) {
			}
		}
		if (tryHandleMultiSellAction(null, cmd)) {
			return;
		}
		if (cmd.startsWith("npc") && !cmd.startsWith("npc_") && cmd.length() > 3 && Character.isDigit(cmd.charAt(3))) {
			cmd = "npc_" + cmd.substring(3);
		}
		if (cmd.startsWith("npc_0_") || cmd.startsWith("npc__")) {
			String action = cmd.startsWith("npc_0_") ? cmd.substring(6).trim() : cmd.substring(5).trim();
			String actionLower = action.toLowerCase(java.util.Locale.ROOT);
			if (tryHandleMultiSellAction(null, action)) {
				return;
			} else if (actionLower.startsWith("change_class")) {
				try {
					int targetClassId = Integer.parseInt(action.substring(12).trim());
					handleChangeClass(0, targetClassId);
					return;
				} catch (NumberFormatException ignored) {
				}
			} else if (actionLower.startsWith("1stclass")) {
				showClassMasterMenu(0, 1);
				return;
			} else if (actionLower.startsWith("2ndclass")) {
				showClassMasterMenu(0, 2);
				return;
			} else if (actionLower.startsWith("3rdclass")) {
				showClassMasterMenu(0, 3);
				return;
			} else if (actionLower.startsWith("subclass")) {
				String subArg = action.length() > 8 ? action.substring(8).trim() : "";
				handleSubclassBypass(0, subArg);
				return;
			} else if (actionLower.startsWith("quest")) {
				String questArg = action.length() > 5 ? action.substring(5).trim() : "";
				int targetObjId = session.targetObjectId() > 0 ? session.targetObjectId() : this.lastInteractedNpcObjId;
				handleQuestBypass(targetObjId, questArg);
				return;
			} else if (actionLower.equals("draw") || actionLower.startsWith("draw")) {
				session.onHennaList();
				return;
			} else if (actionLower.equals("removelist") || actionLower.startsWith("removelist")) {
				session.onHennaUnequipList();
				return;
			} else if (actionLower.startsWith("remove ")) {
				try {
					int slot = Integer.parseInt(action.substring(7).trim());
					session.onHennaRemoveBySlot(slot);
					return;
				} catch (NumberFormatException ignored) {
				}
			} else if (actionLower.startsWith("buy")) {
				int listId = 1;
				if (action.length() > 3) {
					try {
						listId = Integer.parseInt(action.substring(3).trim());
					} catch (NumberFormatException ignored) {
					}
				}
				session.showBuyList(null, listId);
				return;
			} else if (actionLower.startsWith("sell")) {
				session.showSellList(null);
				return;
			} else if (actionLower.startsWith("wear")) {
				int listId = 1;
				if (action.length() > 4) {
					try {
						listId = Integer.parseInt(action.substring(4).trim());
					} catch (NumberFormatException ignored) {
					}
				}
				session.showBuyList(null, listId);
				return;
			} else if (actionLower.startsWith("link")) {
				String path = action.length() > 4 ? action.substring(4).trim() : "";
				if (path.startsWith("/")) {
					path = path.substring(1);
				}
				if (ctx().htmls() != null && !path.isEmpty()) {
					String htm = ctx().htmls().getHtml(path);
					if (htm == null) {
						htm = ctx().htmls().getIndexedHtml(path);
					}
					if (htm == null) {
						htm = ctx().htmls().getHtml("default/" + path);
					}
					if (htm != null) {
						send(new NpcHtmlMessage(0, ctx().htmls().render(htm, 0, "NPC",
								active() != null ? active().name() : "Player")));
						return;
					}
				}
			}
		}
		if (cmd.startsWith("npc_")) {
			// Formato: npc_%objectId%_Chat 1 ou npc_%objectId%_Link ... etc
			String[] parts = cmd.split("_", 3);
			if (parts.length >= 3) {
				try {
					int npcObjId = Integer.parseInt(parts[1]);
					if (npcObjId > 0) {
						this.lastInteractedNpcObjId = npcObjId;
					}
					var npcOpt = ctx().world().npc(npcObjId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (active() != null && !active().isGm()) {
							if (npc.isDead() || Math.hypot(active().x() - npc.x(), active().y() - npc.y()) > 250.0) {
								send(new ActionFailed());
								return;
							}
						}
						String action = parts[2].trim();
						String actionLower = action.toLowerCase(java.util.Locale.ROOT);
						if (actionLower.startsWith("chat")) {
							int val = 0;
							if (action.length() > 4) {
								try {
									val = Integer.parseInt(action.substring(4).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							showNpcHtml(npc, val);
							return;
						} else if (actionLower.startsWith("link")) {
							String path = action.length() > 4 ? action.substring(4).trim() : "";
							if (path.startsWith("/")) {
								path = path.substring(1);
							}
							if (ctx().htmls() != null && !path.isEmpty()) {
								String htm = ctx().htmls().getHtml(path);
								if (htm == null) {
									htm = ctx().htmls().getIndexedHtml(path);
								}
								if (htm == null) {
									htm = ctx().htmls().getHtml("default/" + path);
								}
								if (htm != null) {
									int castleId = getNpcCastleId(npc);
									String rendered = ctx().htmls().render(htm, npc.objectId(), npc.name(),
											active() != null ? active().name() : "Player", castleId, npc.npcId());
									send(new NpcHtmlMessage(npc.objectId(), rendered));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (actionLower.startsWith("1stclass")) {
							showClassMasterMenu(npc.objectId(), 1);
							return;
						} else if (actionLower.startsWith("2ndclass")) {
							showClassMasterMenu(npc.objectId(), 2);
							return;
						} else if (actionLower.startsWith("3rdclass")) {
							showClassMasterMenu(npc.objectId(), 3);
							return;
						} else if (actionLower.startsWith("change_class")) {
							try {
								int targetClassId = Integer.parseInt(action.substring(12).trim());
								handleChangeClass(npc.objectId(), targetClassId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (actionLower.startsWith("sevensignsdesc")) {
							String descArg = action.length() > 14 ? action.substring(14).trim() : "";
							handleSevenSignsDescBypass(npc, descArg);
							return;
						} else if (actionLower.startsWith("sevensigns")) {
							String signArg = action.length() > 10 ? action.substring(10).trim() : "";
							handleSevenSignsBypass(npc, signArg);
							return;
						} else if (actionLower.startsWith("questlist")) {
							if (ctx().htmls() != null) {
								String qHtm = ctx().htmls().getHtml("adventurer_guildsman/questlist.htm");
								if (qHtm != null) {
									send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(qHtm, npc.objectId(), npc.name(), active().name())));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (actionLower.startsWith("quest")) {
							String questArg = action.length() > 5 ? action.substring(5).trim() : "";
							handleQuestBypass(npc.objectId(), questArg);
							return;
						} else if (actionLower.startsWith("subclass")) {
							String subArg = action.length() > 8 ? action.substring(8).trim() : "";
							handleSubclassBypass(npc.objectId(), subArg);
							return;
						} else if (actionLower.startsWith("enchantskilllist") || actionLower.startsWith("skillenchantlist")) {
							session.showEnchantSkillList(npc);
							return;
						} else if (actionLower.startsWith("skilllist") || actionLower.startsWith("fishskilllist")
								|| actionLower.startsWith("fishingskilllist")) {
							session.showSkillList(npc);
							return;
						} else if (actionLower.startsWith("cprecovery")) {
							if (active() != null) {
								if (session.consumeItem(57, 100)) {
									active().currentCp(active().maxCp());
									send(new StatusUpdate(active().objectId(), List.of(new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active().currentCp()))));
									send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Your CP has been restored."));
								} else {
									send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Not enough adena."));
								}
							}
							return;
						} else if (actionLower.startsWith("supportmagic") || actionLower.startsWith("scripts_services.supportmagic")) {
							if (actionLower.contains("protectionblessing")) {
								handleNewbieProtectionBlessing(npc);
							} else {
								handleNewbieSupportMagic(npc);
							}
							return;
						} else if (actionLower.startsWith("giveblessing")) {
							handleNewbieProtectionBlessing(npc);
							return;
						} else if (actionLower.startsWith("teleport")) {
							if (com.lopez.l2j.game.quest.impl.Quest255Tutorial.VILLAGE_NEWBIE_GUIDES.contains(npc.npcId())) {
								if (active() != null && active().level() >= 20 && ctx().htmls() != null) {
									String overHtm = ctx().htmls().getHtml("newbiehelper/guide_teleport_over001.htm");
									if (overHtm != null) {
										send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(overHtm, npc.objectId(), npc.name(), active().name())));
										return;
									}
								}
								handleQuestBypass(npc.objectId(), "1104_NewbieTravelToken");
								return;
							}
							showNpcHtml(npc, 1);
							return;
						} else if (actionLower.startsWith("npclocationinfo") || actionLower.startsWith("8001_npclocationinfo")) {
							String locArg = actionLower.startsWith("8001_npclocationinfo")
									? action.substring("8001_npclocationinfo".length()).trim()
									: action.substring("npclocationinfo".length()).trim();
							handleQuestBypass(npc.objectId(), locArg.isEmpty() ? "8001_NpcLocationInfo" : ("8001_NpcLocationInfo " + locArg));
							return;
						} else if (actionLower.startsWith("festivaldesc")) {
							String num = action.length() > 12 ? action.substring(12).trim() : "1";
							handleFestivalDescBypass(npc, num);
							return;
						} else if (actionLower.startsWith("festival ")) {
							String fArg = action.substring(9).trim();
							handleFestivalBypass(npc, fArg);
							return;
						} else if (actionLower.equals("festival")) {
							handleFestivalBypass(npc, "1");
							return;
						} else if (actionLower.equals("return") || actionLower.startsWith("return")) {
							teleportToCoordinates(83400, 147943, -3404);
							return;
						} else if (actionLower.startsWith("exitrift")) {
							if (active() != null) {
								teleportToCoordinates(-114790, -180576, -6781);
							}
							return;
						} else if (actionLower.startsWith("changeriftroom")) {
							if (active() != null) {
								send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "The Dimensional Rift space is shifting..."));
							}
							return;
						} else if (actionLower.startsWith("open_doors")) {
							handleDoorControl(true);
							return;
						} else if (actionLower.startsWith("close_doors")) {
							handleDoorControl(false);
							return;
						} else if (actionLower.equals("draw") || actionLower.startsWith("draw")) {
							session.onHennaList();
							return;
						} else if (actionLower.equals("removelist") || actionLower.startsWith("removelist")) {
							session.onHennaUnequipList();
							return;
						} else if (actionLower.startsWith("remove ")) {
							try {
								int slot = Integer.parseInt(action.substring(7).trim());
								session.onHennaRemoveBySlot(slot);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (actionLower.startsWith("goto")) {
							try {
								int teleId = Integer.parseInt(action.substring(4).trim());
								if (teleId == 450 || npc.npcId() == 31111) {
									teleportToCoordinates(184464, -13104, -4900);
									return;
								} else if (teleId == 451 || npc.npcId() == 31112) {
									teleportToCoordinates(172373, -17833, -4901);
									return;
								}
								teleportTo(teleId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (tryHandleMultiSellAction(npc, action)) {
							return;
						} else if (actionLower.startsWith("buy")) {
							int listId = 1;
							if (action.length() > 3) {
								try {
									listId = Integer.parseInt(action.substring(3).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							session.showBuyList(npc, listId);
							return;
						} else if (actionLower.startsWith("sell")) {
							session.showSellList(npc);
							return;
						} else if (actionLower.startsWith("wear")) {
							int listId = 1;
							if (action.length() > 4) {
								try {
									listId = Integer.parseInt(action.substring(4).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							session.showBuyList(npc, listId);
							return;
						} else if (actionLower.startsWith("depositc")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var depositable = active().inventory().items().stream()
									.filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST)
									.toList();
							send(new WareHouseDepositList(2, (int) active().inventory().adena(), depositable));
							return;
						} else if (actionLower.startsWith("withdrawc")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var stored = ctx().warehouse().getWarehouseItems(active().objectId());
							send(new WareHouseWithdrawalList(2, (int) active().inventory().adena(), stored));
							return;
						} else if (actionLower.startsWith("depositf") || actionLower.startsWith("withdrawf")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var items = actionLower.startsWith("depositf")
									? active().inventory().items().stream()
											.filter(it -> !it.isEquipped()
													&& it.template().type2() != ItemTemplate.TYPE2_QUEST)
											.toList()
									: ctx().warehouse().getWarehouseItems(active().objectId());
							send(actionLower.startsWith("depositf")
									? new WareHouseDepositList(1, (int) active().inventory().adena(), items)
									: new WareHouseWithdrawalList(1, (int) active().inventory().adena(), items));
							return;
						} else if (actionLower.startsWith("depositp") || actionLower.startsWith("deposit")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var depositable = active().inventory().items().stream()
									.filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST)
									.toList();
							send(new WareHouseDepositList(1, (int) active().inventory().adena(), depositable));
							return;
						} else if (actionLower.startsWith("withdrawp") || actionLower.startsWith("withdraw")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var stored = ctx().warehouse().getWarehouseItems(active().objectId());
							send(new WareHouseWithdrawalList(1, (int) active().inventory().adena(), stored));
							return;
						} else if (actionLower.startsWith("territorystatus")) {
							if (ctx().htmls() != null) {
								String tHtml = ctx().htmls().getHtml("territorystatus.htm");
								if (tHtml != null) {
									String rendered = ctx().htmls()
											.render(tHtml, npc.objectId(), npc.name(), active().name())
											.replace("%castlename%", "Giran")
											.replace("%territory%", "Giran")
											.replace("%clanleadername%", "Lord")
											.replace("%clanname%", "Ruling Clan")
											.replace("%taxpercent%", "0");
									send(new NpcHtmlMessage(npc.objectId(), rendered));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (actionLower.startsWith("augment")) {
							if (action.contains("2")) {
								send(new ExShowVariationCancelWindow());
							} else {
								send(new ExShowVariationMakeWindow());
							}
							return;
						} else if (actionLower.startsWith("create_clan")) {
							String cName = action.length() > 11 ? action.substring(11).trim() : "";
							if (!cName.isEmpty()) {
								session.createClan(cName);
							}
							return;
						} else if (actionLower.startsWith("increase_clan_level")) {
							session.increaseClanLevel();
							return;
						} else if (actionLower.startsWith("dissolve_clan")) {
							session.dissolveClan();
							return;
						} else if (actionLower.startsWith("getbuff")) {
							handleBufferGetBuff(npc, action.substring(7).trim());
							return;
						} else if (actionLower.equals("restore") || actionLower.startsWith("restore")
								|| actionLower.equals("heal") || actionLower.startsWith("heal")) {
							handleBufferRestore(npc);
							return;
						} else if (actionLower.equals("cancel") || actionLower.startsWith("cancel")
								|| actionLower.equals("cleanup") || actionLower.startsWith("cleanup")) {
							handleBufferCancel(npc);
							return;
						} else if (actionLower.equals("fighter") || actionLower.startsWith("fighter ")) {
							handleBufferSchemeApply(npc, 1, false);
							return;
						} else if (actionLower.equals("mage") || actionLower.startsWith("mage ")) {
							handleBufferSchemeApply(npc, 2, false);
							return;
						} else if (actionLower.startsWith("givebuffs")) {
							handleBufferGiveBuffs(npc, action.substring(9).trim());
							return;
						} else if (actionLower.startsWith("editschemes") || actionLower.startsWith("skillselect")
								|| actionLower.startsWith("skillunselect") || actionLower.startsWith("createscheme")
								|| actionLower.startsWith("deletescheme")) {
							handleBufferSchemeEdit(npc, action);
							return;
						} else if (actionLower.startsWith("support_back")) {
							showNpcHtml(npc, 0);
							return;
						} else if (actionLower.equals("support")) {
							handleBufferSchemeApply(npc, 2, false);
							return;
						} else if (actionLower.startsWith("support ")) {
							handleSupportMagic(npc, action.substring(8).trim());
							return;
						} else if (actionLower.startsWith("operate_door")) {
							handleOperateDoor(action.length() > 12 ? action.substring(12).trim() : "");
							return;
						} else if (actionLower.startsWith("open_gate")) {
							handleDoorControl(true);
							return;
						} else if (actionLower.startsWith("observesiege") || actionLower.startsWith("observeoracle") || actionLower.startsWith("observe")) {
							String obsArgs = actionLower.startsWith("observesiege")
									? action.substring(12).trim()
									: (actionLower.startsWith("observeoracle") ? action.substring(13).trim() : action.substring(7).trim());
							handleObservation(obsArgs);
							return;
						} else if (actionLower.startsWith("olympiaddesc")) {
							String num = action.length() > 12 ? action.substring(12).trim() : "0";
							handleOlympiadDesc(npc, num);
							return;
						} else if (actionLower.startsWith("olympiadnoble")) {
							String val = action.length() > 13 ? action.substring(13).trim() : "1";
							handleOlympiadNoble(npc, val);
							return;
						} else if (actionLower.startsWith("olympiad")) {
							handleOlympiadDesc(npc, "0");
							return;
						} else if (actionLower.startsWith("loto")) {
							String val = action.length() > 4 ? action.substring(4).trim() : "0";
							handleLoto(npc, val);
							return;
						} else if (actionLower.startsWith("give_crown")) {
							handleGiveCrown();
							return;
						} else if (actionLower.startsWith("receive_report")) {
							handleCastleReport(npc);
							return;
						} else if (actionLower.startsWith("banish_foreigner")) {
							handleBanishForeigner(npc);
							return;
						} else if (actionLower.startsWith("pvplist") || actionLower.startsWith("pklist") || actionLower.startsWith("onlinelist") || actionLower.startsWith("arenalist")) {
							handleRankingList(npc, action);
							return;
						} else if (actionLower.startsWith("manor_menu_select") || actionLower.startsWith("menu_select")) {
							if (action.contains("ask=-7") || action.contains("ask=255") || (npc != null && npc.template() != null && npc.template().type().toLowerCase().contains("newbie"))) {
								handleNewbieGuideMenu(npc, action);
							} else if (!tryHandleQuestMenuSelect(npc, action)) {
								handleManorMenu(npc, action);
							}
							return;
						} else if (actionLower.startsWith("auction") || actionLower.startsWith("bid") || actionLower.startsWith("selecteditems") || actionLower.startsWith("location") || actionLower.startsWith("list")) {
							handleAuctionAction(npc, action);
							return;
						}
					} else {
						String action = parts[2].trim();
						String actionLower = action.toLowerCase(java.util.Locale.ROOT);
						if (tryHandleMultiSellAction(null, action)) {
							return;
						} else if (actionLower.startsWith("change_class")) {
							try {
								int targetClassId = Integer.parseInt(action.substring(12).trim());
								handleChangeClass(npcObjId, targetClassId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (actionLower.startsWith("1stclass")) {
							showClassMasterMenu(npcObjId, 1);
							return;
						} else if (actionLower.startsWith("2ndclass")) {
							showClassMasterMenu(npcObjId, 2);
							return;
						} else if (actionLower.startsWith("3rdclass")) {
							showClassMasterMenu(npcObjId, 3);
							return;
						} else if (actionLower.startsWith("subclass")) {
							String subArg = action.length() > 8 ? action.substring(8).trim() : "";
							handleSubclassBypass(npcObjId, subArg);
							return;
						} else if (actionLower.startsWith("goto")) {
							try {
								int teleId = Integer.parseInt(action.substring(4).trim());
								teleportTo(teleId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (actionLower.startsWith("buy")) {
							int listId = 1;
							if (action.length() > 3) {
								try {
									listId = Integer.parseInt(action.substring(3).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							session.showBuyList(null, listId);
							return;
						} else if (actionLower.startsWith("sell")) {
							session.showSellList(null);
							return;
						} else if (actionLower.startsWith("wear")) {
							int listId = 1;
							if (action.length() > 4) {
								try {
									listId = Integer.parseInt(action.substring(4).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							session.showBuyList(null, listId);
							return;
						} else if (actionLower.startsWith("link")) {
							String path = action.length() > 4 ? action.substring(4).trim() : "";
							if (path.startsWith("/")) {
								path = path.substring(1);
							}
							if (ctx().htmls() != null && !path.isEmpty()) {
								String htm = ctx().htmls().getHtml(path);
								if (htm == null) {
									htm = ctx().htmls().getIndexedHtml(path);
								}
								if (htm == null) {
									htm = ctx().htmls().getHtml("default/" + path);
								}
								if (htm != null) {
									send(new NpcHtmlMessage(npcObjId, ctx().htmls().render(htm, npcObjId, "NPC",
											active() != null ? active().name() : "Player")));
									return;
								}
							}
						}
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		send(new ActionFailed());
	}

	private boolean tryHandleMultiSellAction(NpcInstance npc, String rawAction) {
		if (rawAction == null || rawAction.isBlank()) {
			return false;
		}
		String action = rawAction.trim();
		String lower = action.toLowerCase(java.util.Locale.ROOT);
		String listIdStr;
		if (lower.startsWith("newbie_exc_multisell")) {
			listIdStr = action.length() > 20 ? action.substring(20).trim() : "003";
			if (listIdStr.isEmpty()) {
				listIdStr = "003";
			}
		} else if (lower.startsWith("exc_multisell")) {
			listIdStr = action.length() > 13 ? action.substring(13).trim() : "";
		} else if (lower.startsWith("multisell_shadow") || lower.startsWith("buyshadow")) {
			session.showMultiSell(npc, 200);
			return true;
		} else if (lower.equals("exchange") || lower.startsWith("exchange ")) {
			session.showMultiSell(npc, 3);
			return true;
		} else if (lower.startsWith("multisell")) {
			listIdStr = action.length() > 9 ? action.substring(9).trim() : "";
		} else {
			return false;
		}

		if (listIdStr.isEmpty()) {
			return false;
		}
		String firstToken = listIdStr.split("\\s+")[0].trim();
		int listId;
		try {
			listId = Integer.parseInt(firstToken);
		} catch (NumberFormatException e) {
			if ("tournament".equalsIgnoreCase(firstToken)) {
				listId = 70000;
			} else {
				return false;
			}
		}
		session.showMultiSell(npc, listId);
		return true;
	}

	private void handleBufferGetBuff(NpcInstance npc, String args) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		if (npc != null && (npc.npcId() == 50005 || (npc.template() != null && "L2BufferVip".equalsIgnoreCase(npc.template().type())))) {
			if (!active().isVip() && active().accessLevel() <= 0) {
				if (ctx().htmls() != null) {
					String noVip = ctx().htmls().getHtml("mods/buffervip/no-vip.htm");
					if (noVip != null) {
						send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(noVip, npc.objectId(), npc.name(), active().name())));
						return;
					}
				}
				send(new ActionFailed());
				return;
			}
		}
		String[] parts = args.trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) {
			if (npc != null) showNpcHtml(npc, 0);
			return;
		}
		try {
			int skillId = Integer.parseInt(parts[0]);
			int skillLvl = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
			String returnHtml = parts.length > 2 ? parts[2] : null;

			if (ctx().schemeBuffer() != null) {
				ctx().schemeBuffer().applySingleBuff(active(), session, skillId, skillLvl, false);
			} else {
				var skillTable = ctx().skillService() != null ? ctx().skillService().table() : null;
				var skOpt = skillTable != null ? skillTable.get(skillId, skillLvl) : java.util.Optional.<com.lopez.l2j.game.skill.SkillTemplate>empty();
				if (skOpt.isPresent()) {
					session.applySkillEffects(skOpt.get(), false);
				} else {
					active().effects().addBuff(skillId, skillLvl, 7200_000L);
				}
			}
			int npcObjId = npc != null ? npc.objectId() : active().objectId();
			int cx = npc != null ? npc.x() : active().x();
			int cy = npc != null ? npc.y() : active().y();
			int cz = npc != null ? npc.z() : active().z();
			var anim = new MagicSkillUse(npcObjId, active().objectId(), skillId, skillLvl, 500, 0,
					cx, cy, cz, active().x(), active().y(), active().z());
			send(anim);
			if (ctx().world() != null) {
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, anim, false);
			}

			if (returnHtml != null && !returnHtml.isBlank() && ctx().htmls() != null) {
				String path = returnHtml.startsWith("data/html/") ? returnHtml.substring(10) : returnHtml;
				String htm = ctx().htmls().getHtml(path);
				if (htm == null) htm = ctx().htmls().getIndexedHtml(path);
				if (htm == null && !path.contains("/")) {
					String folder = (npc != null && npc.npcId() == 50005) ? "mods/buffervip/" : "mods/buffer/";
					htm = ctx().htmls().getHtml(folder + path);
				}
				if (htm != null) {
					String rendered = ctx().htmls().render(htm, npcObjId, npc != null ? npc.name() : "Buffer", active().name());
					send(new NpcHtmlMessage(npcObjId, rendered));
					return;
				}
			}
			if (npc != null) {
				showNpcHtml(npc, 0);
			}
		} catch (Exception ex) {
			log.warn("Erro ao aplicar buff {}: {}", args, ex.getMessage());
			send(new ActionFailed());
		}
	}

	private void handleBufferSchemeApply(NpcInstance npc, int schemeId, boolean targetPet) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		if (ctx().schemeBuffer() != null) {
			ctx().schemeBuffer().applyScheme(active(), session, schemeId, targetPet);
		}
		if (npc != null) {
			int cx = npc.x();
			int cy = npc.y();
			int cz = npc.z();
			var anim = new MagicSkillUse(npc.objectId(), active().objectId(), 1068, 3, 500, 0,
					cx, cy, cz, active().x(), active().y(), active().z());
			send(anim);
			showNpcHtml(npc, 0);
		}
	}

	private void handleBufferGiveBuffs(NpcInstance npc, String args) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		int schemeId = 1;
		boolean targetPet = false;
		if (!args.isEmpty()) {
			String[] tokens = args.split("\\s+");
			for (String tok : tokens) {
				if ("pet".equalsIgnoreCase(tok) || "summon".equalsIgnoreCase(tok)) {
					targetPet = true;
				} else if ("mage".equalsIgnoreCase(tok)) {
					schemeId = 2;
				} else if ("fighter".equalsIgnoreCase(tok)) {
					schemeId = 1;
				} else {
					try {
						int val = Integer.parseInt(tok);
						if (val >= 1 && val <= 4) {
							schemeId = val;
						}
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
		handleBufferSchemeApply(npc, schemeId, targetPet);
	}

	private void handleBufferSchemeEdit(NpcInstance npc, String action) {
		if (active() == null || ctx().schemeBuffer() == null) {
			send(new ActionFailed());
			return;
		}
		var bufferService = ctx().schemeBuffer();
		String[] tokens = action.trim().split("\\s+");
		String cmd = tokens[0].toLowerCase();
		int schemeId = 3;
		if ( cmd.equals("skillselect") && tokens.length >= 3) {
			try {
				schemeId = Integer.parseInt(tokens[1]);
				int skillId = Integer.parseInt(tokens[2]);
				bufferService.addSkillToScheme(active().objectId(), schemeId, skillId);
			} catch (NumberFormatException ignored) {
			}
		} else if (cmd.equals("skillunselect") && tokens.length >= 3) {
			try {
				schemeId = Integer.parseInt(tokens[1]);
				int skillId = Integer.parseInt(tokens[2]);
				bufferService.removeSkillFromScheme(active().objectId(), schemeId, skillId);
			} catch (NumberFormatException ignored) {
			}
		} else if (cmd.equals("deletescheme") && tokens.length >= 2) {
			try {
				schemeId = Integer.parseInt(tokens[1]);
				bufferService.clearScheme(active().objectId(), schemeId);
			} catch (NumberFormatException ignored) {
			}
		} else if (tokens.length >= 2) {
			try {
				schemeId = Integer.parseInt(tokens[1]);
			} catch (NumberFormatException ignored) {
			}
		}
		int npcObjId = npc != null ? npc.objectId() : 0;
		send(new NpcHtmlMessage(npcObjId, bufferService.renderSchemeEditHtml(active(), schemeId)));
	}

	private void handleBufferRestore(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		if (ctx().schemeBuffer() != null) {
			ctx().schemeBuffer().heal(active(), session);
		} else {
			active().currentHp(active().maxHp());
			active().currentMp(active().maxMp());
			active().currentCp(active().maxCp());
			send(new StatusUpdate(active().objectId(), List.of(
					new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active().currentHp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active().currentMp()),
					new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active().currentCp())
			)));
			send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Buffer", "HP, MP e CP totalmente restaurados."));
		}
		int npcObjId = npc != null ? npc.objectId() : active().objectId();
		int cx = npc != null ? npc.x() : active().x();
		int cy = npc != null ? npc.y() : active().y();
		int cz = npc != null ? npc.z() : active().z();
		send(new MagicSkillUse(npcObjId, active().objectId(), 1012, 1, 0, 0,
				cx, cy, cz, active().x(), active().y(), active().z()));
		if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleBufferCancel(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		if (ctx().schemeBuffer() != null) {
			ctx().schemeBuffer().cancelBuffs(active(), session);
		} else {
			active().effects().clear();
			send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Buffer", "Todos os buffs foram removidos."));
		}
		int npcObjId = npc != null ? npc.objectId() : active().objectId();
		int cx = npc != null ? npc.x() : active().x();
		int cy = npc != null ? npc.y() : active().y();
		int cz = npc != null ? npc.z() : active().z();
		send(new MagicSkillUse(npcObjId, active().objectId(), 1056, 1, 0, 0,
				cx, cy, cz, active().x(), active().y(), active().z()));
		if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleSupportMagic(NpcInstance npc, String args) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		String[] parts = args.trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) {
			if (npc != null) showNpcHtml(npc, 0);
			return;
		}
		try {
			int skillId = Integer.parseInt(parts[0]);
			int skillLvl = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
			var skillTable = ctx().skillService() != null ? ctx().skillService().table() : null;
			var skOpt = skillTable != null ? skillTable.get(skillId, skillLvl) : java.util.Optional.<com.lopez.l2j.game.skill.SkillTemplate>empty();
			if (skOpt.isPresent()) {
				var sk = skOpt.get();
				session.applySkillEffects(sk, false);
				int npcObjId = npc != null ? npc.objectId() : active().objectId();
				int cx = npc != null ? npc.x() : active().x();
				int cy = npc != null ? npc.y() : active().y();
				int cz = npc != null ? npc.z() : active().z();
				var anim = new MagicSkillUse(npcObjId, active().objectId(), sk.id(), sk.level(), 500, 0,
						cx, cy, cz, active().x(), active().y(), active().z());
				send(anim);
				if (ctx().world() != null) {
					ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, anim, false);
				}
			}
			if (ctx().htmls() != null && npc != null) {
				String doneHtm = ctx().htmls().getHtml("fortress/support-done.htm");
				if (doneHtm == null) doneHtm = ctx().htmls().getHtml("chamberlain/support-done.htm");
				if (doneHtm != null) {
					String rendered = ctx().htmls().render(doneHtm, npc.objectId(), npc.name(), active().name());
					send(new NpcHtmlMessage(npc.objectId(), rendered));
					return;
				}
			}
			if (npc != null) {
				showNpcHtml(npc, 0);
			}
		} catch (Exception ex) {
			send(new ActionFailed());
		}
	}

	private void handleOperateDoor(String args) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		var dt = com.lopez.l2j.game.door.DoorTable.getInstance();
		if (dt == null) {
			send(new ActionFailed());
			return;
		}
		String[] parts = args.trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) {
			handleDoorControl(true);
			return;
		}
		boolean open = !"0".equals(parts[0]);
		if (parts.length > 1) {
			for (int i = 1; i < parts.length; i++) {
				try {
					int doorId = Integer.parseInt(parts[i]);
					if (open) {
						dt.openDoor(doorId);
					} else {
						dt.closeDoor(doorId);
					}
				} catch (NumberFormatException ignored) {}
			}
		} else {
			handleDoorControl(open);
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", open ? "Portao aberto." : "Portao fechado."));
	}

	private void handleObservation(String args) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		String[] parts = args.trim().split("\\s+");
		if (parts.length >= 3) {
			try {
				int x = Integer.parseInt(parts[0]);
				int y = Integer.parseInt(parts[1]);
				int z = Integer.parseInt(parts[2]);
				int cost = parts.length > 3 ? Integer.parseInt(parts[3]) : 500;
				if (cost > 0 && !session.consumeItem(57, cost)) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Adena insuficiente para observacao."));
					return;
				}
				teleportToCoordinates(x, y, z);
			} catch (NumberFormatException ignored) {
			}
		}
	}

	private void handleOlympiadDesc(NpcInstance npc, String val) {
		if (ctx().htmls() == null || active() == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String htmName = "0".equals(val) ? "olympiad/noble_main.htm" : "olympiad/noble_desc" + val + ".htm";
		String raw = ctx().htmls().getHtml(htmName);
		if (raw == null) raw = ctx().htmls().getIndexedHtml(htmName);
		if (raw != null) {
			send(new NpcHtmlMessage(npcObjId, ctx().htmls().render(raw, npcObjId, npc != null ? npc.name() : "Olympiad Manager", active().name())));
		} else if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleOlympiadNoble(NpcInstance npc, String val) {
		if (active() == null) return;
		int v = 1;
		try { v = Integer.parseInt(val); } catch (Exception ignored) {}
		String npcName = npc != null ? npc.name() : "Olympiad Manager";
		switch (v) {
			case 1 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Sua inscricao nas Grandes Olimpiadas foi cancelada."));
			case 2 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Fila de espera atual: 0 participantes sem classe, 0 com classe."));
			case 3 -> {
				var noble = ctx().olympiad() != null ? ctx().olympiad().getNoble(active().objectId()) : java.util.Optional.<com.lopez.l2j.game.olympiad.OlympiadNoble>empty();
				int pts = noble.map(com.lopez.l2j.game.olympiad.OlympiadNoble::points).orElse(18);
				send(new CreatureSay(0, CreatureSay.ALL, npcName, "Seus pontos atuais de Nobreza nas Olimpiadas: " + pts));
			}
			case 4 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Voce foi registrado nas batalhas sem restricao de classe."));
			case 5 -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Voce foi registrado nas batalhas baseadas em classe."));
			default -> send(new CreatureSay(0, CreatureSay.ALL, npcName, "Operacao concluida."));
		}
		if (npc != null) {
			handleOlympiadDesc(npc, "0");
		}
	}

	private void handleLoto(NpcInstance npc, String val) {
		if (ctx().htmls() == null || active() == null || npc == null) return;
		int v = 0;
		try { v = Integer.parseInt(val); } catch (Exception ignored) {}
		String filename = switch (v) {
			case 0 -> npc.npcId() + "-1.htm";
			case 21, 22 -> npc.npcId() + "-5.htm";
			case 23 -> npc.npcId() + "-3.htm";
			case 24 -> npc.npcId() + "-4.htm";
			default -> npc.npcId() + "-1.htm";
		};
		String htm = ctx().htmls().getHtml("default/" + filename);
		if (htm == null) htm = ctx().htmls().getIndexedHtml(filename);
		if (htm != null) {
			String rendered = ctx().htmls().render(htm, npc.objectId(), npc.name(), active().name())
					.replace("%ticket_price%", "2000")
					.replace("%prize_5%", "100,000,000")
					.replace("%prize_4%", "5,000,000")
					.replace("%prize_3%", "500,000")
					.replace("%prize_2%", "20,000");
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			showNpcHtml(npc, 0);
		}
	}

	private void handleGiveCrown() {
		if (active() == null) return;
		var playerClan = (ctx().clans() != null && active().clanId() > 0) ? ctx().clans().byClanId(active().clanId()).orElse(null) : null;
		if (playerClan != null && playerClan.castleId() > 0 && playerClan.isLeader(active().objectId())) {
			if (ctx().crowns() != null) {
				ctx().crowns().rewardLordCrown(active(), playerClan.castleId());
				send(new CreatureSay(0, CreatureSay.ALL, "Chamberlain", "A Coroa do Senhor do Castelo e o Diadema foram entregues!"));
			}
		} else {
			send(new CreatureSay(0, CreatureSay.ALL, "Chamberlain", "Apenas o lider do cla soberano do castelo pode receber a coroa."));
		}
	}

	private void handleCastleReport(NpcInstance npc) {
		if (ctx().htmls() == null || active() == null || npc == null) return;
		String htm = ctx().htmls().getHtml("chamberlain/chamberlain-report.htm");
		if (htm != null) {
			String rendered = ctx().htmls().render(htm, npc.objectId(), npc.name(), active().name())
					.replace("%tax_income%", "0")
					.replace("%current_tax%", "0")
					.replace("%castle_income%", "0");
			send(new NpcHtmlMessage(npc.objectId(), rendered));
		} else {
			showNpcHtml(npc, 0);
		}
	}

	private void handleBanishForeigner(NpcInstance npc) {
		if (active() == null) return;
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Chamberlain", "Todos os nao-membros do cla foram banidos para fora do castelo."));
		if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleRankingList(NpcInstance npc, String action) {
		if (ctx().htmls() == null || active() == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String htm = ctx().htmls().getHtml("mods/status/50008-1.htm");
		if (htm != null) {
			StringBuilder sb = new StringBuilder("<table width=290>");
			var topList = ctx().characters() != null ? ctx().characters().findTopPvP(10) : List.<com.lopez.l2j.game.model.PlayerCharacter>of();
			int rank = 1;
			for (var p : topList) {
				sb.append("<tr>")
						.append("<td width=20 align=center>").append(rank++).append("</td>")
						.append("<td width=140 align=left>").append(p.name()).append("</td>")
						.append("<td width=60 align=center>").append(p.pvpKills()).append("</td>")
						.append("<td width=70 align=center><font color=\"8FA888\">ONLINE</font></td>")
						.append("</tr>");
			}
			sb.append("</table>");
			String rendered = ctx().htmls().render(htm, npcObjId, npc != null ? npc.name() : "Status", active().name())
					.replace("%pvplist%", sb.toString());
			send(new NpcHtmlMessage(npcObjId, rendered));
		} else if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleManorMenu(NpcInstance npc, String cmd) {
		if (active() == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String npcName = npc != null ? npc.name() : "Manor Manager";
		if (cmd.contains("ask=6")) { // Purchase harvester
			if (session.consumeItem(57, 20)) {
				ctx().inventories().addItem(active().inventory(), 4442, 1, "ManorHarvester");
				send(ItemList.of(active().inventory().items(), false));
				send(new CreatureSay(0, CreatureSay.ALL, npcName, "Harvester adquirido com sucesso!"));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, npcName, "Adena insuficiente para comprar o Harvester (20 adena)."));
			}
			return;
		}
		if (ctx().htmls() != null) {
			String htm = ctx().htmls().getHtml("manormanager/manager.htm");
			if (htm == null) htm = ctx().htmls().getHtml("chamberlain/manor/manor.htm");
			if (htm != null) {
				send(new NpcHtmlMessage(npcObjId, ctx().htmls().render(htm, npcObjId, npcName, active().name())));
				return;
			}
		}
		send(new CreatureSay(0, CreatureSay.ALL, npcName, "Sistema Manor em operacao normal."));
	}

	private void handleAuctionAction(NpcInstance npc, String action) {
		if (active() == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String npcName = npc != null ? npc.name() : "Auctioneer";
		if (action.startsWith("bid ") && action.length() > 4) {
			try {
				long bid = Long.parseLong(action.substring(4).trim());
				int hallId = 1;
				if (ctx().clanHalls() != null) {
					var res = ctx().clanHalls().bidOnAuction(active(), hallId, bid);
					send(new CreatureSay(0, CreatureSay.ALL, npcName, "Resultado do lance no Clan Hall: " + res.name()));
				}
			} catch (Exception ignored) {}
			return;
		}
		if (action.startsWith("bid1")) {
			if (ctx().htmls() != null) {
				String bHtm = ctx().htmls().getHtml("auction/AgitBid1.htm");
				if (bHtm != null) {
					long adena = active().inventory() != null ? active().inventory().adena() : 0;
					String rendered = ctx().htmls().render(bHtm, npcObjId, npcName, active().name())
							.replace("%PLEDGE_ADENA%", String.valueOf(adena))
							.replace("%AGIT_AUCTION_MINBID%", "10000000")
							.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + npcObjId + "_auction");
					send(new NpcHtmlMessage(npcObjId, rendered));
					return;
				}
			}
		}
		if (ctx().htmls() != null) {
			String aHtm = ctx().htmls().getHtml("auction/auction.htm");
			if (aHtm != null) {
				send(new NpcHtmlMessage(npcObjId, ctx().htmls().render(aHtm, npcObjId, npcName, active().name())));
				return;
			}
		}
		if (npc != null) showNpcHtml(npc, 0);
	}
	private int getNpcCastleId(NpcInstance npc) {
		if (npc == null) return 1;
		int id = npc.npcId();
		if (id >= 35092 && id <= 35141) return 1; // Gludio
		if (id >= 35142 && id <= 35183) return 2; // Dion
		if (id >= 35184 && id <= 35225) return 3; // Giran
		if (id >= 35226 && id <= 35273) return 4; // Oren
		if (id >= 35274 && id <= 35315) return 5; // Aden
		if (id >= 35316 && id <= 35357) return 6; // Innadril
		if (id >= 35358 && id <= 35400) return 7; // Goddard
		if (id >= 35504 && id <= 35554) return 8; // Rune
		if (id >= 35555 && id <= 35600) return 9; // Schuttgart

		if (ctx().castles() != null && ctx().zones() != null) {
			var castleOpt = ctx().castles().getCastleAt(npc.x(), npc.y(), npc.z(), ctx().zones());
			if (castleOpt.isPresent()) {
				return castleOpt.get().id();
			}
		}
		return 1;
	}

	private static final java.util.regex.Pattern QUEST_ACTION_LINK_PATTERN =
			java.util.regex.Pattern.compile("action=\"bypass(?:\\s+-h)?\\s+([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE);

	private boolean tryHandleQuestMenuSelect(NpcInstance npc, String cmd) {
		if (cmd == null || !cmd.startsWith("menu_select") || ctx().questManager() == null) {
			return false;
		}
		int askIdx = cmd.indexOf("ask=");
		if (askIdx < 0) {
			return false;
		}
		int start = askIdx + 4;
		int end = start;
		if (end < cmd.length() && (cmd.charAt(end) == '-' || cmd.charAt(end) == '+')) {
			end++;
		}
		while (end < cmd.length() && Character.isDigit(cmd.charAt(end))) {
			end++;
		}
		if (end == start) {
			return false;
		}
		try {
			int askId = Integer.parseInt(cmd.substring(start, end));
			if (askId <= 0 || askId == 255) {
				return false;
			}
			if (askId == 6 && npc != null && npc.template() != null
					&& npc.template().type().toLowerCase(java.util.Locale.ROOT).contains("manor")) {
				return false;
			}
			var q = ctx().questManager().getQuest(askId);
			if (q != null) {
				int targetObjId = npc != null ? npc.objectId()
						: (session.targetObjectId() > 0 ? session.targetObjectId() : this.lastInteractedNpcObjId);
				handleQuestBypass(targetObjId, q.getName() + " " + cmd.trim());
				return true;
			}
		} catch (NumberFormatException ignored) {
		}
		return false;
	}

	private static boolean isGlobalOrNpcBypass(String cmd) {
		if (cmd == null || cmd.isBlank()) {
			return true;
		}
		String lower = cmd.trim().toLowerCase(java.util.Locale.ROOT);
		return lower.startsWith("npc_")
				|| lower.startsWith("_bbs")
				|| lower.startsWith("bbs_")
				|| lower.startsWith("admin_")
				|| lower.startsWith("voiced_")
				|| lower.startsWith("scripts_")
				|| lower.startsWith("link ")
				|| lower.startsWith("player_help")
				|| lower.startsWith("manor_")
				|| (lower.startsWith("menu_select") && (lower.contains("ask=-") || lower.contains("ask=255")))
				|| lower.startsWith("create_clan")
				|| lower.startsWith("create_pledge")
				|| lower.startsWith("increase_clan_level")
				|| lower.startsWith("dissolve_clan")
				|| lower.startsWith("1stclass")
				|| lower.startsWith("2ndclass")
				|| lower.startsWith("3rdclass")
				|| lower.startsWith("change_class")
				|| lower.startsWith("subclass")
				|| lower.startsWith("epic_")
				|| lower.startsWith("boss_")
				|| lower.startsWith("olympiad")
				|| lower.startsWith("sevensigns")
				|| lower.startsWith("festival")
				|| lower.startsWith("goto ")
				|| lower.startsWith("buy")
				|| lower.startsWith("sell")
				|| lower.startsWith("wear")
				|| lower.startsWith("multisell")
				|| lower.startsWith("exc_multisell")
				|| lower.startsWith("chat")
				|| lower.startsWith("skilllist")
				|| lower.startsWith("enchantskilllist")
				|| lower.startsWith("fishskilllist")
				|| lower.startsWith("deposit")
				|| lower.startsWith("withdraw")
				|| lower.startsWith("augment")
				|| lower.startsWith("territorystatus")
				|| lower.startsWith("cprecovery")
				|| lower.startsWith("supportmagic")
				|| lower.startsWith("open_")
				|| lower.startsWith("close_")
				|| lower.startsWith("operate_door")
				|| lower.startsWith("observe")
				|| lower.startsWith("loto")
				|| lower.startsWith("give_crown")
				|| lower.startsWith("receive_report")
				|| lower.startsWith("banish_foreigner")
				|| lower.startsWith("pvplist")
				|| lower.startsWith("pklist")
				|| lower.startsWith("onlinelist")
				|| lower.startsWith("arenalist")
				|| lower.startsWith("auction")
				|| lower.startsWith("bid")
				|| lower.startsWith("restore")
				|| lower.startsWith("cancel")
				|| lower.startsWith("getbuff")
				|| lower.startsWith("support ")
				|| lower.startsWith("support_back")
				|| lower.startsWith("return")
				|| lower.startsWith("exitrift")
				|| lower.startsWith("changeriftroom")
				|| lower.startsWith("draw")
				|| lower.startsWith("remove")
				|| lower.startsWith("questlist")
				|| lower.startsWith("event_exchange")
				|| lower.startsWith("achieve_claim")
				|| lower.startsWith("antibot_");
	}

	private String normalizeQuestHtmlLinks(String html, com.lopez.l2j.game.quest.Quest q, NpcInstance npc) {
		if (html == null || html.isBlank()) {
			return html;
		}
		String converted = HtmCache.convertBracketLinks(html);
		if (!converted.contains("bypass")) {
			return converted;
		}
		String objToken = npc != null ? String.valueOf(npc.objectId()) : "%objectId%";
		String qName = (q != null && q.getName() != null && !q.getName().isBlank()) ? q.getName() : null;

		var matcher = QUEST_ACTION_LINK_PATTERN.matcher(converted);
		var sb = new StringBuilder(converted.length() + 64);
		while (matcher.find()) {
			String rawCmd = matcher.group(1).trim();
			String rewritten = null;
			if (rawCmd.regionMatches(true, 0, "Quest ", 0, 6)) {
				String rest = rawCmd.substring(6).trim();
				if (!rest.isEmpty()) {
					int sp = rest.indexOf(' ');
					if (sp > 0) {
						String folderOrName = rest.substring(0, sp).trim();
						String eventPart = rest.substring(sp + 1).trim();
						String targetQuest = qName != null ? qName : folderOrName;
						rewritten = "action=\"bypass -h npc_" + objToken + "_Quest " + targetQuest + " " + eventPart + "\"";
					} else {
						String targetQuest = qName != null ? qName : rest;
						rewritten = "action=\"bypass -h npc_" + objToken + "_Quest " + targetQuest + "\"";
					}
				}
			} else if (rawCmd.equalsIgnoreCase("Quest")) {
				rewritten = "action=\"bypass -h npc_" + objToken + "_Quest\"";
			} else if (qName != null && !isGlobalOrNpcBypass(rawCmd)) {
				rewritten = "action=\"bypass -h npc_" + objToken + "_Quest " + qName + " " + rawCmd + "\"";
			}
			if (rewritten != null) {
				matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(rewritten));
			} else {
				matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private String resolveQuestHtml(com.lopez.l2j.game.quest.Quest q, String rawResult, NpcInstance npc) {
		if (rawResult == null || rawResult.isBlank()) {
			return null;
		}
		if (rawResult.contains("<html") || rawResult.contains("<HTML") || rawResult.contains("<body") || rawResult.contains("<BODY")) {
			return normalizeQuestHtmlLinks(rawResult, q, npc);
		}
		if ("noquest".equalsIgnoreCase(rawResult) || "no-quest".equalsIgnoreCase(rawResult)) {
			return "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
		}
		if (ctx().htmls() == null) {
			return rawResult;
		}

		int qId = q != null ? q.getQuestId() : 0;
		String qName = q != null ? q.getName() : "";

		String h = ctx().htmls().getQuestHtml(qId, qName, rawResult);
		if (h != null && !h.isBlank()) {
			return normalizeQuestHtmlLinks(h, q, npc);
		}

		// Fallback seguro para evitar exibir texto cru do nome do arquivo para o jogador
		String def = ctx().htmls().getHtml("npcdefault.htm");
		if (def != null && !def.isBlank()) {
			return def;
		}
		return "<html><body>" + (npc != null ? npc.name() : "NPC") + ":<br><br>I have no tasks for you right now.</body></html>";
	}

	public void handleQuestBypass(int npcObjId, String questArg) {
		if (npcObjId > 0) {
			this.lastInteractedNpcObjId = npcObjId;
		}
		var npcOpt = ctx().world().npc(npcObjId);
		if (npcOpt.isEmpty() && this.lastInteractedNpcObjId > 0 && this.lastInteractedNpcObjId != npcObjId) {
			npcOpt = ctx().world().npc(this.lastInteractedNpcObjId);
		}
		if (npcOpt.isEmpty()) {
			send(new ActionFailed());
			return;
		}
		var npc = npcOpt.get();
		int castleId = getNpcCastleId(npc);

		if (questArg == null || questArg.isBlank()) {
			int npcId = npc.npcId();
			List<com.lopez.l2j.game.quest.Quest> options = new ArrayList<>();

			// 1. Quests que o jogador já está fazendo e que interagem com este NPC
			for (QuestState qs : session.getAllQuestStates()) {
				if (qs != null && qs.isStarted() && qs.getQuest() != null) {
					var q = qs.getQuest();
					if (q.hasTalkNpc(npcId) || q.hasStartNpc(npcId)) {
						if (!options.contains(q)) {
							options.add(q);
						}
					}
				}
			}

			// 2. Quests que este NPC pode iniciar (usando índice rápido por NPC + fallback)
			if (ctx().questManager() != null) {
				Collection<com.lopez.l2j.game.quest.Quest> startQuests = ctx().questManager().getQuestsForStartNpc(npcId);
				if (startQuests == null || startQuests.isEmpty()) {
					startQuests = ctx().questManager().getAllQuests();
				}
				for (var q : startQuests) {
					if (q.hasStartNpc(npcId)) {
						if (!options.contains(q)) {
							options.add(q);
						}
					}
				}
			}

			if (options.size() > 1) {
				StringBuilder sb = new StringBuilder("<html><body>");
				for (var q : options) {
					sb.append("<a action=\"bypass -h npc_").append(npc.objectId())
					  .append("_Quest ").append(q.getName()).append("\"> [")
					  .append(q.getDescr());
					QuestState qs = session.getQuestState(q.getName());
					if (qs != null) {
						if (qs.isStarted()) {
							sb.append(" (In progress)");
						} else if (qs.isCompleted()) {
							sb.append(" (Completed)");
						}
					}
					sb.append("]</a><br>");
				}
				sb.append("</body></html>");
				send(new NpcHtmlMessage(npc.objectId(), sb.toString()));
				return;
			} else if (options.size() == 1) {
				var q = options.get(0);
				QuestState qs = session.getQuestState(q.getName());
				boolean wasAlreadyCompleted = qs != null && qs.isCompleted();
				if (qs == null && q.hasStartNpc(npc.npcId())) {
					qs = q.newQuestState(session);
				}
				String html = q.notifyTalk(npc, session);
				if (wasAlreadyCompleted && (html == null || html.isBlank() || "noquest".equalsIgnoreCase(html) || "no-quest".equalsIgnoreCase(html))) {
					send(new NpcHtmlMessage(npc.objectId(), "<html><body>This quest has already been completed.</body></html>"));
					return;
				}
				if (html != null && !html.isBlank() && !"noquest".equalsIgnoreCase(html) && !"no-quest".equalsIgnoreCase(html)) {
					String resolved = resolveQuestHtml(q, html, npc);
					if (resolved != null && !resolved.isBlank()) {
						String rendered = ctx().htmls() != null
								? ctx().htmls().render(resolved, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId(), q.getName())
								: resolved;
						send(new NpcHtmlMessage(npc.objectId(), rendered));
						return;
					}
				}
			}

			String lType = npc.template() != null ? npc.template().type().toLowerCase(java.util.Locale.ROOT) : "";
			if (lType.contains("master") || lType.contains("trainer") || lType.contains("teacher")
					|| lType.contains("priest")) {
				String masterHtml = "<html><title>" + npc.name() + "</title><body>"
						+ "<font color=\"LEVEL\">" + npc.name() + " ("
						+ (npc.template().title().isEmpty() ? "Master" : npc.template().title())
						+ ")</font><br><br>"
						+ "Greetings, adventurer. How may I instruct you today?<br><br>"
						+ "<table width=240>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_SkillList\">Learn Skills</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_1stClass\">1st Class Transfer (Level 20)</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_2ndClass\">2nd Class Transfer (Level 40)</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_3rdClass\">3rd Class Transfer (Level 76)</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h npc_" + npcObjId
						+ "_Subclass 0\">Subclass</a></td></tr>"
						+ "<tr><td><a action=\"bypass -h create_clan 0\">Create Clan</a></td></tr>"
						+ "</table></body></html>";
				send(new NpcHtmlMessage(npcObjId, masterHtml));
				return;
			}
			if (lType.contains("teleport")) {
				showNpcHtml(npc, 1);
				return;
			}
			if (lType.contains("merchant") || lType.contains("trader") || lType.contains("grocer")) {
				session.showBuyList(npc, 1);
				return;
			}
			String noQuestHtm = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
			send(new NpcHtmlMessage(npc.objectId(), noQuestHtm));
			return;
		}

		if (questArg.startsWith("1101_teleport_to_race_track")) {
			teleportToCoordinates(12661, 181687, -3560);
			return;
		}

		if (questArg.startsWith("1103_OracleTeleport") || questArg.startsWith("1104_OracleTeleport")) {
			boolean isDawn = (npc.npcId() >= 31078 && npc.npcId() <= 31084)
					|| npc.npcId() == 31168 || npc.npcId() == 31692 || npc.npcId() == 31694 || npc.npcId() == 31997
					|| (npc.name() != null && npc.name().toLowerCase(java.util.Locale.ROOT).contains("dawn"));
			if (isDawn) {
				teleportToCoordinates(-80157, 111344, -4901);
			} else {
				teleportToCoordinates(-81261, 86531, -5157);
			}
			return;
		}

		if (questArg.startsWith("1108_CrumaTower")) {
			if (active() != null && !active().isGm() && Config.CRUMA_TOWER_LEVEL_RESTRICT > 0 && active().level() >= Config.CRUMA_TOWER_LEVEL_RESTRICT) {
				String bigLvlHtm = ctx().htmls() != null ? ctx().htmls().getHtml("teleporter/30483-biglvl.htm") : null;
				if (bigLvlHtm == null) {
					bigLvlHtm = "<html><body>Gatekeeper Mozella:<br><br>You can't enter, your level is too high!<br><br>(Characters who are level %allowedmaxlvl% and above cannot enter in Cruma Tower.)</body></html>";
				}
				bigLvlHtm = bigLvlHtm.replace("%allowedmaxlvl%", String.valueOf(Config.CRUMA_TOWER_LEVEL_RESTRICT));
				String rendered = ctx().htmls() != null
						? ctx().htmls().render(bigLvlHtm, npc.objectId(), npc.name(), active().name(), castleId, npc.npcId())
						: bigLvlHtm;
				send(new NpcHtmlMessage(npc.objectId(), rendered));
				return;
			}
			teleportToCoordinates(17724, 114004, -11672);
			return;
		}

		String[] parts = questArg.split("\\s+");
		String qName = parts[0];
		String event = questArg.length() > qName.length() ? questArg.substring(qName.length()).trim() : null;
		var q = ctx().questManager() != null ? ctx().questManager().getQuest(qName) : null;
		if (q != null) {
			QuestState qs = session.getQuestState(q.getName());
			boolean wasAlreadyCompleted = qs != null && qs.isCompleted();
			if (qs == null && q.hasStartNpc(npc.npcId())) {
				qs = q.newQuestState(session);
			}
			String html;
			if (event != null && !event.isEmpty()) {
				html = q.notifyEvent(event, npc, session);
			} else {
				html = q.notifyTalk(npc, session);
			}
			if (wasAlreadyCompleted && (event == null || event.isEmpty())
					&& (html == null || html.isBlank() || "noquest".equalsIgnoreCase(html) || "no-quest".equalsIgnoreCase(html))) {
				send(new NpcHtmlMessage(npc.objectId(), "<html><body>This quest has already been completed.</body></html>"));
				return;
			}
			if (html != null && !html.isBlank()) {
				if ("noquest".equalsIgnoreCase(html) || "no-quest".equalsIgnoreCase(html)) {
					String noQuestHtm = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
					send(new NpcHtmlMessage(npc.objectId(), noQuestHtm));
					return;
				}
				String resolved = resolveQuestHtml(q, html, npc);
				if (resolved != null && !resolved.isBlank()) {
					String rendered = ctx().htmls() != null
							? ctx().htmls().render(resolved, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId(), q.getName())
							: resolved;
					send(new NpcHtmlMessage(npc.objectId(), rendered));
					return;
				}
			}
			send(new ActionFailed());
			return;
		}

		if (ctx().htmls() != null) {
			String qHtml = null;
			if (parts.length >= 2) {
				String filename = parts[parts.length - 1];
				qHtml = ctx().htmls().getQuestHtml(0, parts[0], filename);
				if (qHtml == null) {
					qHtml = ctx().htmls().getHtml("village_master/" + parts[0] + "/" + filename);
				}
				if (qHtml == null) {
					qHtml = ctx().htmls().getHtml("quests/" + parts[0] + "/" + filename);
				}
				if (qHtml == null) {
					qHtml = ctx().htmls().getIndexedHtml(filename);
				}
			} else {
				qHtml = ctx().htmls().getHtml("teleporter/" + questArg + ".htm");
				if (qHtml == null) {
					qHtml = ctx().htmls().getHtml("default/" + questArg + ".htm");
				}
				if (qHtml == null) {
					qHtml = ctx().htmls().getQuestHtml(0, "", questArg);
				}
				if (qHtml == null) {
					qHtml = ctx().htmls().getIndexedHtml(npc.npcId() + "-01.htm");
				}
			}
			if (qHtml != null) {
				String rendered = ctx().htmls().render(qHtml, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId(), parts[0]);
				send(new NpcHtmlMessage(npc.objectId(), rendered));
				return;
			}
		}

		showNpcNoQuest(npc);
	}

	private void showNpcNoQuest(NpcInstance npc) {
		String defaultHtm = ctx().htmls() != null ? ctx().htmls().getHtml("noquest.htm") : null;
		if (defaultHtm == null) {
			defaultHtm = "<html><body>" + (npc != null && npc.name() != null ? npc.name() : "NPC")
					+ ":<br><br>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
		}
		send(new NpcHtmlMessage(npc != null ? npc.objectId() : 0, ctx().htmls() != null
				? ctx().htmls().render(defaultHtm, npc != null ? npc.objectId() : 0, npc != null ? npc.name() : "NPC", active() != null ? active().name() : "Player")
				: defaultHtm));
	}

	public void handleSubclassBypass(int npcObjId, String args) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		if (active().isDead() || active().isOlympiadMode() || session.isAutoAttacking() || active().isInCombat() || active().isStoreOpen() || active().isBuffShop()) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao pode gerenciar subclasses enquanto estiver em combate, morto ou em loja."));
			send(new ActionFailed());
			return;
		}

		if (args == null || args.isBlank() || args.equals("0")) {
			StringBuilder sb = new StringBuilder("<html><title>Subclass</title><body>");
			sb.append("<font color=\"LEVEL\">Subclass Management</font><br><br>");
			sb.append("A master is capable of awakening latent heroic powers through subclasses.<br><br>");
			sb.append("<table width=260>");
			int maxSubs = Config.MAX_SUBCLASSES > 0 ? Config.MAX_SUBCLASSES : 3;
			int subCount = active().getSubClasses().size();
			if (subCount < maxSubs) {
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 1\">Add a Subclass</a></td></tr>");
			}
			if (subCount > 0) {
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 2\">Change Subclass</a></td></tr>");
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 3\">Cancel / Replace a Subclass</a></td></tr>");
			}
			sb.append("</table></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		if (args.equals("1")) {
			int maxSubs = Config.MAX_SUBCLASSES > 0 ? Config.MAX_SUBCLASSES : 3;
			if (!active().isGm()) {
				if (active().level() < 75) {
					send(new NpcHtmlMessage(npcObjId, "<html><body>You must be at least level 75 to acquire a subclass.</body></html>"));
					return;
				}
				if (ctx().subClasses() != null && !ctx().subClasses().checkSubclassRequirements(active())) {
					send(new NpcHtmlMessage(npcObjId, "<html><body>You do not meet the requirements or quests to acquire a subclass.</body></html>"));
					return;
				}
			}
			if (active().subClasses().size() >= maxSubs) {
				send(new NpcHtmlMessage(npcObjId, "<html><body>You cannot add more than " + maxSubs + " subclasses.</body></html>"));
				return;
			}
			var subs = ctx().subClasses() != null ? ctx().subClasses().getAvailableSubClasses(active()) : java.util.List.<Integer>of();
			if (subs.isEmpty()) {
				send(new NpcHtmlMessage(npcObjId, "<html><body>There are no available subclasses for your character at this time.</body></html>"));
				return;
			}
			StringBuilder sb = new StringBuilder("<html><title>Add Subclass</title><body>Select the subclass you wish to acquire:<br><br><table width=260>");
			for (int targetCId : subs) {
				String cName = ctx().characters() != null ? ctx().characters().template(targetCId).map(CharTemplate::className).orElse("Class " + targetCId) : "Class " + targetCId;
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 1_").append(targetCId).append("\">")
						.append(cName).append("</a></td></tr>");
			}
			sb.append("</table></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		if (args.startsWith("1_")) {
			try {
				int maxSubs = Config.MAX_SUBCLASSES > 0 ? Config.MAX_SUBCLASSES : 3;
				int targetClassId = Integer.parseInt(args.substring(2).trim());
				int nextIndex = 1;
				for (int i = 1; i <= maxSubs; i++) {
					if (!active().subClasses().containsKey(i)) {
						nextIndex = i;
						break;
					}
				}
				int initLvl = Config.SUBCLASS_INIT_LEVEL > 0 ? Config.SUBCLASS_INIT_LEVEL : 40;
				long baseExp = com.lopez.l2j.game.model.ExperienceTable.expForLevel(initLvl);
				SubClass sc = new SubClass(targetClassId, baseExp, 0, initLvl, nextIndex);
				if (ctx().subClasses() != null) {
					ctx().subClasses().saveSubClass(active().objectId(), sc);
				}
				active().subClasses().put(nextIndex, sc);
				applySubClassSwitch(nextIndex, targetClassId, initLvl, baseExp, 0);

				send(SystemMessage.id(SystemMessage.ADD_NEW_SUBCLASS));
				send(new PlaySound("ItemSound.quest_fanfare_2"));
				var msuSub = new MagicSkillUse(active().objectId(), active().objectId(), 4339, 1, 0, 0,
						active().x(), active().y(), active().z(), active().x(), active().y(), active().z());
				send(msuSub);
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msuSub, false);
				send(new SocialAction(active().objectId(), 3));
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
						new SocialAction(active().objectId(), 3), false);
				if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse adicionada com sucesso!"));
				}
			} catch (Exception ex) {
				log.warn("Falha ao adicionar subclass: {}", ex.getMessage());
			}
			return;
		}

		if (args.equals("2")) {
			StringBuilder sb = new StringBuilder("<html><title>Change Subclass</title><body>Select the class you want to switch to:<br><br><table width=260>");
			if (active().isSubClassActive()) {
				var baseTpl = ctx().characters() != null ? ctx().characters().template(active().baseClassId()) : java.util.Optional.<CharTemplate>empty();
				String baseName = baseTpl.map(CharTemplate::className).orElse("Base Class (" + active().baseClassId() + ")");
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 2_0\">")
						.append(baseName).append(" (Main)</a></td></tr>");
			}
			for (var entry : active().subClasses().entrySet()) {
				int idx = entry.getKey();
				if (idx == active().classIndex()) {
					continue;
				}
				var sc = entry.getValue();
				var scTpl = ctx().characters() != null ? ctx().characters().template(sc.classId()) : java.util.Optional.<CharTemplate>empty();
				String name = scTpl.map(CharTemplate::className).orElse("Subclass " + sc.classId());
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 2_").append(idx).append("\">")
						.append(name).append(" (Lv. ").append(sc.level()).append(")</a></td></tr>");
			}
			sb.append("</table></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		if (args.startsWith("2_")) {
			try {
				int targetIndex = Integer.parseInt(args.substring(2).trim());
				if (targetIndex == 0) {
					if (active().isSubClassActive()) {
						var baseSub = ctx().subClasses() != null ? ctx().subClasses().loadSubClasses(active().objectId()).get(0) : null;
						int mainClassId = baseSub != null && baseSub.classId() > 0 ? baseSub.classId() : active().baseClassId();
						int mainLvl = baseSub != null && baseSub.level() > 0 ? baseSub.level() : Math.max(active().level(), 75);
						long mainExp = baseSub != null && baseSub.exp() > 0 ? baseSub.exp() : com.lopez.l2j.game.model.ExperienceTable.expForLevel(mainLvl);
						int mainSp = baseSub != null ? baseSub.sp() : active().sp();
						active().baseClassId(mainClassId);
						applySubClassSwitch(0, mainClassId, mainLvl, mainExp, mainSp);
						send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
						send(new PlaySound("ItemSound.quest_fanfare_2"));
						if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce retornou para sua classe principal."));
						}
					}
					return;
				}
				var targetSub = active().subClasses().get(targetIndex);
				if (targetSub != null) {
					applySubClassSwitch(targetIndex, targetSub.classId(), targetSub.level(), targetSub.exp(), targetSub.sp());
					send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
					send(new PlaySound("ItemSound.quest_fanfare_2"));
					if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
						send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse alterada com sucesso!"));
					}
				}
			} catch (Exception ex) {
				log.warn("Falha ao trocar subclass: {}", ex.getMessage());
			}
			return;
		}

		if (args.equals("3")) {
			StringBuilder sb = new StringBuilder("<html><title>Replace Subclass</title><body>Select the subclass you wish to replace:<br><br><table width=260>");
			for (var entry : active().subClasses().entrySet()) {
				var sc = entry.getValue();
				var scTpl = ctx().characters() != null ? ctx().characters().template(sc.classId()) : java.util.Optional.<CharTemplate>empty();
				String name = scTpl.map(CharTemplate::className).orElse("Subclass " + sc.classId());
				sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 3_").append(entry.getKey()).append("\">")
						.append(name).append(" (Lv. ").append(sc.level()).append(")</a></td></tr>");
			}
			sb.append("</table></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		if (args.startsWith("3_")) {
			try {
				int replaceIndex = Integer.parseInt(args.substring(2).trim());
				var subs = ctx().subClasses() != null ? ctx().subClasses().getAvailableSubClasses(active()) : java.util.List.<Integer>of();
				StringBuilder sb = new StringBuilder("<html><title>Replace Subclass</title><body>Select the new subclass to replace slot " + replaceIndex + ":<br><br><table width=260>");
				for (int choiceId : subs) {
					String cName = ctx().characters() != null ? ctx().characters().template(choiceId).map(CharTemplate::className).orElse("Class " + choiceId) : "Class " + choiceId;
					sb.append("<tr><td><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 4_").append(replaceIndex).append("_").append(choiceId).append("\">")
							.append(cName).append("</a></td></tr>");
				}
				sb.append("</table></body></html>");
				send(new NpcHtmlMessage(npcObjId, sb.toString()));
			} catch (Exception ex) {
				log.warn("Falha no menu de substituir subclass: {}", ex.getMessage());
			}
			return;
		}

		if (args.startsWith("4_")) {
			try {
				String[] parts = args.substring(2).split("_");
				int replaceIndex = Integer.parseInt(parts[0]);
				int newClassId = Integer.parseInt(parts[1]);
				int initLvl = Config.SUBCLASS_INIT_LEVEL > 0 ? Config.SUBCLASS_INIT_LEVEL : 40;
				long baseExp = com.lopez.l2j.game.model.ExperienceTable.expForLevel(initLvl);
				SubClass sc = new SubClass(newClassId, baseExp, 0, initLvl, replaceIndex);
				if (ctx().skills() != null) {
					ctx().skills().deleteAll(active().objectId(), replaceIndex);
				}
				if (ctx().shortcuts() != null) {
					ctx().shortcuts().deleteAll(active().objectId(), replaceIndex);
				}
				if (ctx().subClasses() != null) {
					ctx().subClasses().saveSubClass(active().objectId(), sc);
				}
				active().subClasses().put(replaceIndex, sc);
				applySubClassSwitch(replaceIndex, newClassId, initLvl, baseExp, 0);
				send(SystemMessage.id(SystemMessage.SUBCLASS_TRANSFER_COMPLETED));
				send(new PlaySound("ItemSound.quest_fanfare_2"));
				if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Subclasse substituida com sucesso!"));
				}
			} catch (Exception ex) {
				log.warn("Falha ao substituir subclass: {}", ex.getMessage());
			}
		}
	}

	public void applySubClassSwitch(int newIndex, int newClassId, int newLevel, long newExp, int newSp) {
		if (active() == null) {
			return;
		}
		saveCurrentClassProgress();

		if (ctx().buffRepository() != null) {
			ctx().buffRepository().saveBuffs(active().objectId(), active().classIndex(), active().effects().activeBuffs());
		}

		active().classIndex(newIndex);
		active().classId(newClassId);
		active().level(newLevel);
		active().exp(newExp);
		active().sp(newSp);

		if (ctx().skillService() != null) {
			ctx().skillService().load(active());
		} else {
			active().skills().clear();
		}

		var tplOpt = ctx().characters() != null ? ctx().characters().template(newClassId) : java.util.Optional.<CharTemplate>empty();
		if (tplOpt.isPresent()) {
			var tpl = tplOpt.get();
			session.rewardSkills(tpl, false);
		}
		session.updateArmorSetBonus();
		session.updateEquippedItemSkills();
		session.updateAugmentationBonus();
		session.loadHennasForCurrentSubclass();
		var charTpl = ctx().characters() != null ? ctx().characters().template(active()) : null;
		if (charTpl != null) {
			session.recalcMaxVitals(charTpl);
		}
		active().currentHp(active().maxHp());
		active().currentMp(active().maxMp());
		active().currentCp(active().maxCp());

		if (ctx().characters() != null) {
			ctx().characters().save(active(), true);
		}
		if (ctx().subClasses() != null && active().isSubClassActive()) {
			ctx().subClasses().saveSubClass(active().objectId(), new SubClass(newClassId, newExp, newSp, newLevel, newIndex));
		}

		active().effects().clear();
		session.refreshBuffs();

		var msuSwitch = new MagicSkillUse(active().objectId(), active().objectId(), 4383, 1, 0, 0,
				active().x(), active().y(), active().z(), active().x(), active().y(), active().z());
		send(msuSwitch);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msuSwitch, false);

		session.refreshWeightAndPenalties();
		session.sendSkillList();
		send(new HennaInfo(active(), session.getClassLevel(active()), ctx().hennaTrees()));
		send(new ShortCutInit(ctx().shortcuts() != null ? ctx().shortcuts().findByCharId(active().objectId(), active().classIndex()) : java.util.List.of()));
		session.sendUserInfoAndBroadcastCharInfo();
	}

	public void saveCurrentClassProgress() {
		if (active() == null) {
			return;
		}
		if (active().isSubClassActive()) {
			var sc = active().subClasses().get(active().classIndex());
			if (sc != null) {
				SubClass updated = new SubClass(active().classId(), active().exp(), active().sp(), active().level(), sc.classIndex());
				active().subClasses().put(sc.classIndex(), updated);
				if (ctx().subClasses() != null) {
					ctx().subClasses().saveSubClass(active().objectId(), updated);
				}
			}
		} else {
			int mainClass = active().baseClassId() > 0 ? active().baseClassId() : active().classId();
			active().baseClassId(mainClass);
			SubClass baseSub = new SubClass(mainClass, active().exp(), active().sp(), active().level(), 0);
			if (ctx().subClasses() != null) {
				ctx().subClasses().saveSubClass(active().objectId(), baseSub);
			}
		}
	}


	public void showClassMasterMenu(int npcObjId, int targetLevel) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		var curTpl = ctx().characters() != null ? ctx().characters().template(active().classId()).orElse(null) : null;
		int currentTier = curTpl != null ? curTpl.classTier() : 0;
		if (currentTier >= 3) {
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body><center><font color=\"LEVEL\">Class Master</font><br><br>");
			sb.append("Voce ja atingiu a classe maxima (3rd Class).<br>");
			if (active().level() >= 75) {
				sb.append("<br><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 0\">Gerenciar Subclasses</a><br>");
			}
			sb.append("</center></body></html>");
			send(new NpcHtmlMessage(npcObjId, sb.toString()));
			return;
		}

		int nextClassTier = currentTier + 1;
		int minLvl = switch (nextClassTier) {
			case 1 -> 20;
			case 2 -> 40;
			case 3 -> 76;
			default -> 1;
		};

		if (!active().isGm() && active().level() < minLvl) {
			String laterHtm = ctx().htmls() != null ? ctx().htmls().getHtml("classmaster/comebacklater.htm") : null;
			if (laterHtm != null) {
				String rendered = laterHtm.replace("%level%", String.valueOf(minLvl))
						.replace("%objectId%", String.valueOf(npcObjId));
				send(new NpcHtmlMessage(npcObjId, rendered));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Volte quando atingir nivel " + minLvl + "."));
			}
			return;
		}

		List<Integer> children = ctx().skillService() != null && ctx().skillService().trees() != null
				? ctx().skillService().trees().getChildClasses(active().classId())
				: List.of();
		if (children.isEmpty()) {
			String noMoreHtm = ctx().htmls() != null ? ctx().htmls().getHtml("classmaster/nomore.htm") : null;
			if (noMoreHtm != null) {
				send(new NpcHtmlMessage(npcObjId, noMoreHtm.replace("%objectId%", String.valueOf(npcObjId))));
			} else {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao ha mais mudancas de classe disponiveis."));
			}
			return;
		}

		StringBuilder menu = new StringBuilder();
		for (int cid : children) {
			String cname = ctx().characters() != null
					? ctx().characters().template(cid).map(CharTemplate::className).orElse("Class " + cid)
					: "Class " + cid;
			menu.append("<a action=\"bypass -h npc_").append(npcObjId).append("_change_class ").append(cid)
					.append("\">")
					.append(cname).append("</a><br>");
		}
		if (active().level() >= 75) {
			menu.append("<br><a action=\"bypass -h npc_").append(npcObjId).append("_Subclass 0\">Gerenciar Subclasses</a><br>");
		}
		String curName = curTpl != null ? curTpl.className() : "Class " + active().classId();
		String tpl = ctx().htmls() != null ? ctx().htmls().getHtml("classmaster/template.htm") : null;
		if (tpl != null) {
			String rendered = tpl.replace("%name%", curName)
					.replace("%menu%", menu.toString())
					.replace("%req_items%", "")
					.replace("%objectId%", String.valueOf(npcObjId));
			send(new NpcHtmlMessage(npcObjId, rendered));
		} else {
			String fallback = "<html><body><center>" + curName + " Class Master:</center><br>" + menu
					+ "</body></html>";
			send(new NpcHtmlMessage(npcObjId, fallback));
		}
	}

	public void handleChangeClass(int npcObjId, int newClassId) {
		handleChangeClass(npcObjId, newClassId, active() != null && active().isGm());
	}

	public void handleChangeClass(int npcObjId, int newClassId, boolean isGmOverride) {
		if (active() == null || active().isDead() || active().isOlympiadMode()) {
			send(new ActionFailed());
			return;
		}
		List<Integer> allowed = ctx().skillService() != null && ctx().skillService().trees() != null
				? ctx().skillService().trees().getChildClasses(active().classId())
				: List.of();
		if (!isGmOverride && (allowed.isEmpty() || !allowed.contains(newClassId))) {
			log.warn("{} tentou trocar para classe invalida: {} (atual: {})", active().name(), newClassId,
					active().classId());
			send(new ActionFailed());
			return;
		}
		var tplOpt = ctx().characters() != null ? ctx().characters().template(newClassId)
				: java.util.Optional.<CharTemplate>empty();
		if (tplOpt.isPresent()) {
			var tpl = tplOpt.get();
			int minLvl = switch (tpl.classTier()) {
				case 1 -> 20;
				case 2 -> 40;
				case 3 -> 76;
				default -> 1;
			};
			if (!isGmOverride && active().level() < minLvl) {
				send(new CreatureSay(0, CreatureSay.ALL, "SYS",
						"Nivel " + minLvl + " necessario para trocar para " + tpl.className() + "."));
				send(new ActionFailed());
				return;
			}
			active().classId(newClassId);
			if (!active().isSubClassActive()) {
				active().baseClassId(newClassId);
			} else {
				var sc = active().subClasses().get(active().classIndex());
				if (sc != null) {
					SubClass updated = new SubClass(newClassId, active().exp(), active().sp(), active().level(), active().classIndex());
					active().subClasses().put(active().classIndex(), updated);
					if (ctx().subClasses() != null) {
						ctx().subClasses().saveSubClass(active().objectId(), updated);
					}
				}
			}
			if (isGmOverride && (allowed.isEmpty() || !allowed.contains(newClassId))) {
				if (ctx().skillService() != null) {
					ctx().skillService().cleanInvalidSkills(active());
				}
			}
			session.rewardSkills(tpl, true);
			session.updateArmorSetBonus();
			session.updateEquippedItemSkills();
			session.updateAugmentationBonus();
			session.recalcMaxVitals(tpl);
			active().currentHp(active().maxHp());
			active().currentMp(active().maxMp());
			active().currentCp(active().maxCp());
		} else {
			active().classId(newClassId);
			if (!active().isSubClassActive()) {
				active().baseClassId(newClassId);
			} else {
				var sc = active().subClasses().get(active().classIndex());
				if (sc != null) {
					SubClass updated = new SubClass(newClassId, active().exp(), active().sp(), active().level(), active().classIndex());
					active().subClasses().put(active().classIndex(), updated);
					if (ctx().subClasses() != null) {
						ctx().subClasses().saveSubClass(active().objectId(), updated);
					}
				}
			}
		}
		if (ctx().characters() != null) {
			ctx().characters().save(active(), true);
		}
		active().recalcHennaStats(ctx().hennas(), ctx().hennaTrees());
		session.refreshWeightAndPenalties();
		session.sendSkillList();
		send(new HennaInfo(active(), session.getClassLevel(active()), ctx().hennaTrees()));
		send(new ShortCutInit(ctx().shortcuts() != null ? ctx().shortcuts().findByCharId(active().objectId(), active().classIndex()) : java.util.List.of()));
		session.sendUserInfoAndBroadcastCharInfo();

		// Som e efeitos visuais canonicos de Lineage 2
		send(new PlaySound("ItemSound.quest_fanfare_2"));
		var msuClass = new MagicSkillUse(active().objectId(), active().objectId(), 4339, 1, 0, 0,
				active().x(), active().y(), active().z(), active().x(), active().y(), active().z());
		send(msuClass);
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, msuClass, false);
		send(new SocialAction(active().objectId(), 3));
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
				new SocialAction(active().objectId(), 3), false);

		String newClassName = tplOpt.map(CharTemplate::className).orElse("Class " + newClassId);
		if (Config.SHOW_CLASS_CHANGE_MESSAGE) {
			send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Parabens! Voce agora e um " + newClassName + "!"));
		}
		if (Config.ANNOUNCE_CLASS_CHANGE) {
			ctx().world().broadcast(new CreatureSay(0, CreatureSay.ANNOUNCEMENT, "", active().name() + " avancou para a classe " + newClassName + "!"), x -> true);
		} else if (Config.ANNOUNCE_CLASS_CHANGE_AROUND) {
			ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
					new CreatureSay(0, CreatureSay.ALL, "SYS", active().name() + " avancou para a classe " + newClassName + "!"), false);
		}

		String okHtm = ctx().htmls() != null ? ctx().htmls().getHtml("classmaster/ok.htm") : null;
		if (okHtm != null) {
			send(new NpcHtmlMessage(npcObjId,
					okHtm.replace("%name%", newClassName).replace("%objectId%", String.valueOf(npcObjId))));
		}
	}

	public void teleportTo(int teleId) {
		if (active() != null && !Config.ALT_KARMA_PLAYER_CAN_USE_GK && active().karma() > 0) {
			send(new ActionFailed());
			return;
		}
		if (ctx().teleports() == null) {
			send(new ActionFailed());
			return;
		}
		var locOpt = ctx().teleports().get(teleId);
		if (locOpt.isEmpty()) {
			log.warn("Teleport id {} nao encontrado", teleId);
			send(new ActionFailed());
			return;
		}

		if (active() != null && !active().isGm() && Config.CRUMA_TOWER_LEVEL_RESTRICT > 0 && active().level() >= Config.CRUMA_TOWER_LEVEL_RESTRICT) {
			if (teleId == 124 || teleId == 286 || teleId == 291 || teleId == 287 || teleId == 292) {
				String bigLvlHtm = ctx().htmls() != null ? ctx().htmls().getHtml("teleporter/30483-biglvl.htm") : null;
				if (bigLvlHtm == null) {
					bigLvlHtm = "<html><body>Gatekeeper Mozella:<br><br>You can't enter, your level is too high!<br><br>(Characters who are level %allowedmaxlvl% and above cannot enter in Cruma Tower.)</body></html>";
				}
				bigLvlHtm = bigLvlHtm.replace("%allowedmaxlvl%", String.valueOf(Config.CRUMA_TOWER_LEVEL_RESTRICT));
				int npcObj = 0;
				String rendered = ctx().htmls() != null
						? ctx().htmls().render(bigLvlHtm, npcObj, "Gatekeeper Mozella", active().name())
						: bigLvlHtm;
				send(new NpcHtmlMessage(npcObj, rendered));
				send(new ActionFailed());
				return;
			}
		}
		var loc = locOpt.get();
		if (active() != null && !active().isGm() && ctx().sevenSigns() != null && ctx().sevenSigns().isInside7sDungeon(loc.locX(), loc.locY(), loc.locZ())) {
			boolean isNecropolis = false;
			for (var mLoc : com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_MERCHANT_LOCS) {
				double distSq = Math.pow(loc.locX() - mLoc.x(), 2) + Math.pow(loc.locY() - mLoc.y(), 2);
				if (distSq <= 16_000_000) {
					isNecropolis = true;
					break;
				}
			}
			var access = ctx().sevenSigns().checkDungeonEntry(active().objectId(), isNecropolis);
			if (access != com.lopez.l2j.game.sevensigns.SevenSignsManager.DungeonAccess.ALLOWED) {
				String htm = isNecropolis ? "seven_signs/necro_no.htm" : "seven_signs/cata_no.htm";
				String raw = ctx().htmls() != null ? ctx().htmls().getHtml(htm) : null;
				if (raw != null) {
					send(new NpcHtmlMessage(0, ctx().htmls().render(raw, 0, "Gatekeeper Ziggurat", active().name())));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao tem permissao para entrar nos Sete Selos."));
				}
				send(new ActionFailed());
				return;
			}
		}

		int price = loc.price();
		int costItem = loc.forNoble() ? 6651 : ItemTemplate.ADENA_ID;

		boolean isFree = false;
		if (!loc.forNoble()) {
			boolean freeTp = Config.getBoolean("FreeTeleporting", false);
			int minLvl = Config.getInt("FreeTeleportingMinLvL", 1);
			int maxLvl = Config.getInt("FreeTeleportingMaxLvL", 99);
			if (freeTp && active().level() >= minLvl && active().level() <= maxLvl) {
				isFree = true;
			}
		} else {
			boolean freeNobleTp = Config.getBoolean("NoblePassFreeTp", false);
			int minLvl = Config.getInt("NoblePassFreeTpMinLvL", 1);
			int maxLvl = Config.getInt("NoblePassFreeTpMaxLvL", 99);
			if (freeNobleTp && active().level() >= minLvl && active().level() <= maxLvl) {
				isFree = true;
			}
		}

		if (price > 0 && !isFree) {
			var consumed = ctx().inventories().consumeItem(active().inventory(), costItem, price, "Teleport");
			if (consumed == null) {
				send(SystemMessage.id(SystemMessage.YOU_NOT_ENOUGH_ADENA));
				send(new ActionFailed());
				return;
			}
			send(new InventoryUpdate(List.of(consumed.removed()
					? ItemInfo.of(consumed.item(), ItemInfo.REMOVED)
					: ItemInfo.of(consumed.item(), ItemInfo.MODIFIED))));
		}

		teleportToCoordinates(loc.locX(), loc.locY(), loc.locZ());
	}

	public void teleportToCoordinates(int targetX, int targetY, int targetZ) {
		session.teleportToLocation(targetX, targetY, targetZ);
	}

	private void handleScriptGatekeeper(String cmd) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		try {
			int spaceIdx = cmd.indexOf(' ');
			if (spaceIdx > 0) {
				String[] tokens = cmd.substring(spaceIdx + 1).trim().split("\\s+");
				if (tokens.length >= 3) {
					int x = Integer.parseInt(tokens[0]);
					int y = Integer.parseInt(tokens[1]);
					int z = Integer.parseInt(tokens[2]);
					if (tokens.length >= 5) {
						long cost = Long.parseLong(tokens[3]);
						int itemId = Integer.parseInt(tokens[4]);
						if (cost > 0 && !session.consumeItem(itemId, (int) cost)) {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "You do not have enough required items."));
							send(new ActionFailed());
							return;
						}
					} else if (tokens.length == 4) {
						long cost = Long.parseLong(tokens[3]);
						if (cost > 0 && !session.consumeItem(57, (int) cost)) {
							send(new CreatureSay(0, CreatureSay.ALL, "SYS", "You do not have enough adena."));
							send(new ActionFailed());
							return;
						}
					}
					if (active() != null && !active().isGm() && ctx().sevenSigns() != null && ctx().sevenSigns().isInside7sDungeon(x, y, z)) {
						boolean isNecropolis = false;
						for (var mLoc : com.lopez.l2j.game.sevensigns.SevenSignsManager.MAMMON_MERCHANT_LOCS) {
							double distSq = Math.pow(x - mLoc.x(), 2) + Math.pow(y - mLoc.y(), 2);
							if (distSq <= 16_000_000) {
								isNecropolis = true;
								break;
							}
						}
						var access = ctx().sevenSigns().checkDungeonEntry(active().objectId(), isNecropolis);
						if (access != com.lopez.l2j.game.sevensigns.SevenSignsManager.DungeonAccess.ALLOWED) {
							String htm = isNecropolis ? "seven_signs/necro_no.htm" : "seven_signs/cata_no.htm";
							String raw = ctx().htmls() != null ? ctx().htmls().getHtml(htm) : null;
							if (raw != null) {
								send(new NpcHtmlMessage(0, ctx().htmls().render(raw, 0, "Gatekeeper Ziggurat", active().name())));
							} else {
								send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Voce nao tem permissao para entrar nos Sete Selos."));
							}
							send(new ActionFailed());
							return;
						}
					}
					teleportToCoordinates(x, y, z);
					return;
				}
			}
		} catch (Exception e) {
			log.warn("Erro ao processar gatekeeper via script: {}", cmd, e);
		}
		send(new ActionFailed());
	}

	private void handleDoorControl(boolean open) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		com.lopez.l2j.game.door.DoorTable dt = com.lopez.l2j.game.door.DoorTable.getInstance();
		if (dt != null) {
			var nearby = dt.findDoorsAround(active().x(), active().y(), 1000);
			for (var door : nearby) {
				if (open) {
					dt.openDoor(door.doorId());
				} else {
					dt.closeDoor(door.doorId());
				}
			}
		}
		send(new CreatureSay(0, CreatureSay.ALL, "SYS", open ? "Portas abertas." : "Portas fechadas."));
	}

	private void handleNewbieGuideMenu(NpcInstance npc, String cmd) {
		if (active() == null) return;
		int npcObjId = npc != null ? npc.objectId() : 0;
		String npcName = npc != null ? npc.name() : "Newbie Guide";
		int npcId = npc != null ? npc.npcId() : 30598;

		int ask = 0;
		int reply = 0;
		try {
			int askIdx = cmd.indexOf("ask=");
			if (askIdx >= 0) {
				int ampIdx = cmd.indexOf('&', askIdx);
				String askStr = ampIdx >= 0 ? cmd.substring(askIdx + 4, ampIdx) : cmd.substring(askIdx + 4);
				ask = Integer.parseInt(askStr.trim());
			}
			int repIdx = cmd.indexOf("reply=");
			if (repIdx < 0) {
				repIdx = cmd.indexOf("reply_");
			}
			if (repIdx >= 0) {
				int start = repIdx + 6;
				int ampIdx = cmd.indexOf('&', start);
				String repStr = ampIdx >= 0 ? cmd.substring(start, ampIdx) : cmd.substring(start);
				reply = Integer.parseInt(repStr.trim());
			}
		} catch (Exception ignored) {}

		String folder = switch (npcId) {
			case 30598 -> "guide_human_cnacelot";
			case 30599, 30370 -> "guide_elf_roios";
			case 30600, 30129 -> "guide_delf_frankia";
			case 30601, 30528 -> "guide_dwarf_gullin";
			case 30602, 30573 -> "guide_orc_tanai";
			case 31076 -> "guide_gludin_nina";
			case 31077 -> "guide_gludio_euria";
			case 32327 -> "blessing_benefector";
			default -> "guide_human_cnacelot";
		};

		if (ask == -7) {
			int expectedRace = switch (npcId) {
				case 30598 -> 0; // Human
				case 30599, 30370 -> 1; // Elf
				case 30600, 30129 -> 2; // Dark Elf
				case 30602, 30573 -> 3; // Orc
				case 30601, 30528 -> 4; // Dwarf
				default -> -1;
			};
			int lvl = active().level();
			boolean isMage = npcId != 30601 && npcId != 30528 && active().isMage();
			int classLevel = com.lopez.l2j.game.model.ClassId.getClassLevel(active().classId());
			String file;
			if (expectedRace >= 0 && active().race() != expectedRace) {
				file = folder + "003.htm";
			} else if (lvl > 20 || classLevel > 0) {
				file = folder + "002.htm";
			} else if (isMage) {
				if (lvl <= 7) file = folder + "_m07.htm";
				else if (lvl <= 14) file = folder + "_m14.htm";
				else file = folder + "_m20.htm";
			} else {
				if (lvl <= 5) file = folder + "_f05.htm";
				else if (lvl <= 10) file = folder + "_f10.htm";
				else if (lvl <= 15) file = folder + "_f15.htm";
				else file = folder + "_f20.htm";
			}
			String htm = ctx().htmls() != null ? ctx().htmls().getHtml("newbiehelper/" + folder + "/" + file) : null;
			if (htm == null && ctx().htmls() != null) {
				htm = ctx().htmls().getIndexedHtml(file);
			}
			if (htm != null) {
				send(new NpcHtmlMessage(npcObjId, ctx().htmls().render(htm, npcObjId, npcName, active().name())));
				return;
			}
		} else if (ask == 255) {
			if (reply >= 31 && reply <= 66) {
				int[] radar = getNewbieGuideRadar(npcId, reply);
				if (radar != null) {
					send(new RadarControl(0, 1, radar[0], radar[1], radar[2]));
				}
				String file = folder + "_q0255_05.htm";
				String htm = ctx().htmls() != null ? ctx().htmls().getHtml("newbiehelper/" + folder + "/" + file) : null;
				if (htm == null && ctx().htmls() != null) {
					htm = ctx().htmls().getIndexedHtml(file);
				}
				if (htm != null) {
					send(new NpcHtmlMessage(npcObjId, ctx().htmls().render(htm, npcObjId, npcName, active().name())));
					return;
				}
			}
			String file = switch (reply) {
				case 10 -> folder + "_q0255_04.htm";
				case 11 -> folder + "_q0255_04a.htm";
				case 12 -> folder + "_q0255_04b.htm";
				case 13 -> folder + "_q0255_04c.htm";
				case 14 -> folder + "_q0255_04d.htm";
				case 15 -> folder + "_q0255_04e.htm";
				case 16 -> folder + "_q0255_04f.htm";
				case 17 -> folder + "_q0255_04g.htm";
				case 18 -> folder + "_q0255_04h.htm";
				case 19 -> folder + "_q0255_04i.htm";
				default -> folder + "_q0255_04.htm";
			};
			String htm = ctx().htmls() != null ? ctx().htmls().getHtml("newbiehelper/" + folder + "/" + file) : null;
			if (htm == null && ctx().htmls() != null) {
				htm = ctx().htmls().getIndexedHtml(file);
			}
			if (htm != null) {
				send(new NpcHtmlMessage(npcObjId, ctx().htmls().render(htm, npcObjId, npcName, active().name())));
				return;
			}
		}

		if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private int[] getNewbieGuideRadar(int npcId, int reply) {
		return switch (npcId) {
			case 30598 -> switch (reply) {
				case 31 -> new int[]{-84108, 244604, -3729};
				case 32 -> new int[]{-82236, 241573, -3728};
				case 33 -> new int[]{-82515, 241221, -3728};
				case 34 -> new int[]{-82319, 244709, -3727};
				case 35 -> new int[]{-82659, 244992, -3717};
				case 36 -> new int[]{-86114, 244682, -3727};
				case 37 -> new int[]{-86328, 244448, -3724};
				case 38 -> new int[]{-86322, 241215, -3727};
				case 39 -> new int[]{-85964, 240947, -3727};
				case 40 -> new int[]{-85026, 242689, -3729};
				case 41 -> new int[]{-83789, 240799, -3717};
				case 42 -> new int[]{-84204, 240403, -3717};
				case 43 -> new int[]{-86385, 243267, -3717};
				case 44 -> new int[]{-86733, 242918, -3717};
				case 45 -> new int[]{-84516, 245449, -3714};
				case 46 -> new int[]{-84729, 245001, -3726};
				case 47 -> new int[]{-84965, 245222, -3726};
				case 48 -> new int[]{-84981, 244764, -3726};
				case 49 -> new int[]{-85186, 245001, -3726};
				case 50 -> new int[]{-83326, 242964, -3718};
				case 51 -> new int[]{-83020, 242553, -3718};
				case 52 -> new int[]{-83175, 243065, -3718};
				case 53 -> new int[]{-82809, 242751, -3718};
				case 54 -> new int[]{-81895, 243917, -3721};
				case 55 -> new int[]{-81840, 243534, -3721};
				case 56 -> new int[]{-81512, 243424, -3720};
				case 57 -> new int[]{-84436, 242793, -3729};
				case 58 -> new int[]{-78939, 240305, -3443};
				case 59 -> new int[]{-85301, 244587, -3725};
				case 60 -> new int[]{-83163, 243560, -3728};
				case 61 -> new int[]{-97131, 258946, -3622};
				case 62 -> new int[]{-114685, 222291, -2925};
				case 63 -> new int[]{-84057, 242832, -3729};
				case 64 -> new int[]{-100332, 238019, -3573};
				case 65 -> new int[]{-82041, 242718, -3725};
				default -> null;
			};
			case 30599, 30370 -> switch (reply) {
				case 31 -> new int[]{46926, 51511, -2977};
				case 32 -> new int[]{44995, 51706, -2803};
				case 33 -> new int[]{45727, 51721, -2803};
				case 34 -> new int[]{42812, 51138, -2996};
				case 35 -> new int[]{45487, 46511, -2996};
				case 36 -> new int[]{47401, 51764, -2996};
				case 37 -> new int[]{42971, 51372, -2996};
				case 38 -> new int[]{47595, 51569, -2996};
				case 39 -> new int[]{45778, 46534, -2996};
				case 40 -> new int[]{44476, 47153, -2984};
				case 41 -> new int[]{42700, 50057, -2984};
				case 42 -> new int[]{42766, 50037, -2984};
				case 43 -> new int[]{44683, 46952, -2981};
				case 44 -> new int[]{44667, 46896, -2982};
				case 45 -> new int[]{45725, 52105, -2795};
				case 46 -> new int[]{44823, 52414, -2795};
				case 47 -> new int[]{45000, 52101, -2795};
				case 48 -> new int[]{45919, 52414, -2795};
				case 49 -> new int[]{44692, 52261, -2795};
				case 50 -> new int[]{47780, 49568, -2983};
				case 51 -> new int[]{47912, 50170, -2983};
				case 52 -> new int[]{47868, 50167, -2983};
				case 53 -> new int[]{28928, 74248, -3773};
				case 54 -> new int[]{43673, 49683, -3046};
				case 55 -> new int[]{45610, 49008, -3059};
				case 56 -> new int[]{50592, 54986, -3376};
				case 57 -> new int[]{42978, 49115, -2994};
				case 58 -> new int[]{46475, 50495, -3058};
				case 59 -> new int[]{45859, 50827, -3058};
				case 60 -> new int[]{51210, 82474, -3283};
				case 61 -> new int[]{49262, 53607, -3216};
				default -> null;
			};
			case 30600, 30129 -> switch (reply) {
				case 31 -> new int[]{9670, 15537, -4574};
				case 32 -> new int[]{15120, 15656, -4376};
				case 33 -> new int[]{17306, 13592, -3724};
				case 34 -> new int[]{15272, 16310, -4377};
				case 35 -> new int[]{6449, 19619, -3694};
				case 36 -> new int[]{-15404, 71131, -3445};
				case 37 -> new int[]{7496, 17388, -4377};
				case 38 -> new int[]{17102, 13002, -3743};
				case 39 -> new int[]{6532, 19903, -3693};
				case 40 -> new int[]{-15648, 71405, -3451};
				case 41 -> new int[]{7644, 18048, -4377};
				case 42 -> new int[]{-1301, 75883, -3566};
				case 43 -> new int[]{-1152, 76125, -3566};
				case 44 -> new int[]{10580, 17574, -4554};
				case 45 -> new int[]{12009, 15704, -4554};
				case 46 -> new int[]{11951, 15661, -4554};
				case 47 -> new int[]{10761, 17970, -4554};
				case 48 -> new int[]{10823, 18013, -4554};
				case 49 -> new int[]{11283, 14226, -4242};
				case 50 -> new int[]{10447, 14620, -4242};
				case 51 -> new int[]{11258, 14431, -4242};
				case 52 -> new int[]{10344, 14445, -4242};
				case 53 -> new int[]{10315, 14293, -4242};
				case 54 -> new int[]{10775, 14190, -4242};
				case 55 -> new int[]{11235, 14078, -4242};
				case 56 -> new int[]{11012, 14128, -4242};
				case 57 -> new int[]{13380, 17430, -4542};
				case 58 -> new int[]{13464, 17751, -4541};
				case 59 -> new int[]{13763, 17501, -4542};
				case 60 -> new int[]{-44225, 79721, -3652};
				case 61 -> new int[]{-44015, 79683, -3652};
				case 62 -> new int[]{25856, 10832, -3724};
				case 63 -> new int[]{12328, 14947, -4574};
				case 64 -> new int[]{13081, 18444, -4573};
				case 65 -> new int[]{12311, 17470, -4574};
				default -> null;
			};
			case 30601, 30528 -> switch (reply) {
				case 31 -> new int[]{115072, -178176, -906};
				case 32 -> new int[]{117847, -182339, -1537};
				case 33 -> new int[]{116617, -184308, -1569};
				case 34 -> new int[]{117826, -182576, -1537};
				case 35 -> new int[]{116378, -184308, -1571};
				case 36 -> new int[]{115183, -176728, -791};
				case 37 -> new int[]{114969, -176752, -790};
				case 38 -> new int[]{117366, -178725, -1118};
				case 39 -> new int[]{117378, -178914, -1120};
				case 40 -> new int[]{116226, -178529, -948};
				case 41 -> new int[]{116190, -178441, -948};
				case 42 -> new int[]{116016, -178615, -948};
				case 43 -> new int[]{116190, -178615, -948};
				case 44 -> new int[]{116103, -178407, -948};
				case 45 -> new int[]{116103, -178653, -948};
				case 46 -> new int[]{115468, -182446, -1434};
				case 47 -> new int[]{115315, -182155, -1444};
				case 48 -> new int[]{115271, -182692, -1445};
				case 49 -> new int[]{115900, -177316, -915};
				case 50 -> new int[]{116268, -177524, -914};
				case 51 -> new int[]{115741, -181645, -1344};
				case 52 -> new int[]{116192, -181072, -1344};
				case 53 -> new int[]{115205, -180024, -870};
				case 54 -> new int[]{114716, -180018, -871};
				case 55 -> new int[]{114832, -179520, -871};
				case 56 -> new int[]{115717, -183488, -1483};
				case 57 -> new int[]{115618, -183265, -1483};
				case 58 -> new int[]{114348, -178537, -813};
				case 59 -> new int[]{114990, -177294, -854};
				case 60 -> new int[]{114426, -178672, -812};
				case 61 -> new int[]{114409, -178415, -812};
				case 62 -> new int[]{117061, -181867, -1413};
				case 63 -> new int[]{116164, -184029, -1507};
				case 64 -> new int[]{115563, -182923, -1448};
				case 65 -> new int[]{112656, -174864, -611};
				case 66 -> new int[]{116852, -183595, -1566};
				default -> null;
			};
			case 30602, 30573 -> switch (reply) {
				case 31 -> new int[]{-45264, -112512, -235};
				case 32 -> new int[]{-46576, -117311, -242};
				case 33 -> new int[]{-47360, -113791, -237};
				case 34 -> new int[]{-47360, -113424, -235};
				case 35 -> new int[]{-45744, -117165, -236};
				case 36 -> new int[]{-46528, -109968, -250};
				case 37 -> new int[]{-45808, -110055, -255};
				case 38 -> new int[]{-45731, -113844, -237};
				case 39 -> new int[]{-45728, -113360, -237};
				case 40 -> new int[]{-45952, -114784, -199};
				case 41 -> new int[]{-45952, -114496, -199};
				case 42 -> new int[]{-45863, -112621, -200};
				case 43 -> new int[]{-45864, -112540, -199};
				case 44 -> new int[]{-43264, -112532, -220};
				case 45 -> new int[]{-43910, -115518, -194};
				case 46 -> new int[]{-43950, -115457, -194};
				case 47 -> new int[]{-44416, -111486, -222};
				case 48 -> new int[]{-43926, -111794, -222};
				case 49 -> new int[]{-43109, -113770, -221};
				case 50 -> new int[]{-43114, -113404, -221};
				case 51 -> new int[]{-46768, -113610, -3};
				case 52 -> new int[]{-46802, -114011, -112};
				case 53 -> new int[]{-46247, -113866, -21};
				case 54 -> new int[]{-46808, -113184, -112};
				case 55 -> new int[]{-45328, -114736, -237};
				case 56 -> new int[]{-44624, -111873, -238};
				default -> null;
			};
			default -> null;
		};
	}

	private void handleNewbieProtectionBlessing(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		int classLevel = com.lopez.l2j.game.model.ClassId.getClassLevel(active().classId());
		if (active().level() >= 39 || classLevel >= 2) {
			String msg = "<html><body>Newbie Guide:<br>The Blessing of Protection is only available to characters under level 39 who have not completed their second class transfer.</body></html>";
			send(new NpcHtmlMessage(npc != null ? npc.objectId() : 0, msg));
			return;
		}
		var skillTable = ctx().skillService() != null ? ctx().skillService().table() : null;
		if (skillTable != null) {
			skillTable.get(5182, 1).ifPresent(sk -> session.applySkillEffects(sk, false));
		}
		int npcObjId = npc != null ? npc.objectId() : active().objectId();
		int cx = npc != null ? npc.x() : active().x();
		int cy = npc != null ? npc.y() : active().y();
		int cz = npc != null ? npc.z() : active().z();
		send(new MagicSkillUse(npcObjId, active().objectId(), 5182, 1, 1000, 0,
				cx, cy, cz, active().x(), active().y(), active().z()));
	}

	private void handleNewbieSupportMagic(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		if (active().level() < 6 || active().level() > 25) {
			send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Newbie Guide",
					"Only characters between level 6 and 25 may receive beginner buffs."));
			return;
		}
		active().currentHp(active().maxHp());
		active().currentMp(active().maxMp());
		send(new StatusUpdate(active().objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active().currentHp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active().currentMp()))));

		var skillTable = ctx().skillService() != null ? ctx().skillService().table() : null;
		if (skillTable != null) {
			int lvl = active().level();
			boolean isMage = active().isMage();

			java.util.List<int[]> buffsToApply = new java.util.ArrayList<>();
			if (lvl >= 6) {
				buffsToApply.add(new int[]{1204, lvl >= 16 ? 2 : 1}); // Wind Walk
			}
			if (lvl >= 11) {
				buffsToApply.add(new int[]{1040, lvl >= 16 ? 2 : 1}); // Shield
			}

			if (isMage) {
				if (lvl >= 12) buffsToApply.add(new int[]{1048, 1}); // Bless the Soul
				if (lvl >= 13) buffsToApply.add(new int[]{1085, 1}); // Acumen
				if (lvl >= 14) buffsToApply.add(new int[]{1078, 1}); // Concentration
				if (lvl >= 15) buffsToApply.add(new int[]{1059, 1}); // Empower
				if (lvl >= 16) buffsToApply.add(new int[]{4338, 1}); // Life Cubic
			} else {
				if (lvl >= 12) buffsToApply.add(new int[]{1045, 1}); // Bless the Body
				if (lvl >= 13) buffsToApply.add(new int[]{1268, 1}); // Vampiric Rage
				if (lvl >= 14) buffsToApply.add(new int[]{1044, 1}); // Regeneration
				if (lvl >= 15) buffsToApply.add(new int[]{1086, 1}); // Haste
				if (lvl >= 16) buffsToApply.add(new int[]{4338, 1}); // Life Cubic
			}

			for (int[] b : buffsToApply) {
				var skOpt = skillTable.get(b[0], b[1]);
				if (skOpt.isPresent()) {
					session.applySkillEffects(skOpt.get(), false);
				}
			}
		}

		int npcObjId = npc != null ? npc.objectId() : active().objectId();
		int cx = npc != null ? npc.x() : active().x();
		int cy = npc != null ? npc.y() : active().y();
		int cz = npc != null ? npc.z() : active().z();
		send(new MagicSkillUse(npcObjId, active().objectId(), 1204, 1, 1000, 0,
				cx, cy, cz, active().x(), active().y(), active().z()));
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Newbie Guide",
				"May the blessings of the Einhasad protect your path."));
	}


}

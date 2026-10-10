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

		// 1. Quests com first talk prioritário (Tutorial, Newbie Helper e diálogos dinâmicos)
		if (val == 0 && ctx().questManager() != null) {
			for (var q : ctx().questManager().getAllQuests()) {
				if (q.hasFirstTalkNpc(npc.npcId())) {
					String qHtm = q.notifyFirstTalk(npc, session);
					if (qHtm != null && !qHtm.isBlank() && !"noquest".equalsIgnoreCase(qHtm) && !"no-quest".equalsIgnoreCase(qHtm)) {
						String resolved = resolveQuestHtml(q, qHtm, npc);
						if (resolved != null && !resolved.isBlank()) {
							String rendered = ctx().htmls() != null
									? ctx().htmls().render(resolved, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId())
									: resolved;
							send(new NpcHtmlMessage(npc.objectId(), rendered));
							return;
						}
					}
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
				int achId = Integer.parseInt(cmd.substring(14).trim());
				int adena = active().inventory().byItemId(57).map(ItemInstance::count).orElse(0);
				boolean ok = ctx().achievements().claimAchievement(active(), achId, adena, 0, 0, 0);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Conquista resgatada com sucesso!" : "Requisitos nao satisfeitos."));
				String html = ctx().achievements().generateHtml(active(), adena, 0, 0, 0);
				send(new NpcHtmlMessage(0, html));
			}
			return;
		}
		if (cmd.startsWith("event_exchange ")) {
			if (ctx().officialEvent() != null && active() != null) {
				int exId = Integer.parseInt(cmd.substring(15).trim());
				Map<Integer, Integer> inv = new HashMap<>();
				for (var item : active().inventory().items()) {
					inv.put(item.itemId(), item.count());
				}
				boolean ok = ctx().officialEvent().exchangeReward(active(), exId, inv);
				send(new CreatureSay(0, CreatureSay.ALL, "SYS", ok ? "Troca efetuada com sucesso!" : "Itens insuficientes."));
				send(new NpcHtmlMessage(0, ctx().officialEvent().generateHtml(active())));
			}
			return;
		}
		if (cmd.startsWith("voiced_roulette")) {
			if (ctx().roulette() != null && active() != null) {
				if (cmd.contains("spin")) {
					Map<Integer, Long> inv = new HashMap<>();
					int adena = active().inventory().byItemId(57).map(ItemInstance::count).orElse(0);
					inv.put(57, (long) adena);
					var result = ctx().roulette().spin(active(), ctx().roulette().getDefaultCostItemId(), ctx().roulette().getDefaultCostCount(), inv);
					if (result.success()) {
						ctx().inventories().consumeItem(active().inventory(), ctx().roulette().getDefaultCostItemId(), (int) ctx().roulette().getDefaultCostCount(), "RouletteSpin");
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
				Map<Integer, Long> inv = new HashMap<>();
				int adena = active().inventory().byItemId(57).map(ItemInstance::count).orElse(0);
				inv.put(57, (long) adena);
				boolean ok = ctx().characterReset().performReset(active(), inv);
				if (ok) {
					ctx().inventories().consumeItem(active().inventory(), 57, (int) (adena - inv.get(57)), "CharacterReset");
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Reset efetuado com sucesso! Nivel reiniciado para 1."));
					send(new UserInfo(active(), ctx().characters().template(active())));
				} else {
					send(new CreatureSay(0, CreatureSay.ALL, "SYS", "Nao foi possivel realizar o reset."));
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
		if (cmd.startsWith("scripts_services.SupportMagic") || cmd.startsWith("SupportMagic")) {
			var npc = session.targetObjectId() > 0 && ctx().world() != null ? ctx().world().npc(session.targetObjectId()).orElse(null) : null;
			handleNewbieSupportMagic(npc);
			return;
		}
		if (cmd.startsWith("manor_menu_select") || cmd.startsWith("menu_select")) {
			if (cmd.contains("ask=-7") || cmd.contains("ask=255")) {
				var npc = session.targetObjectId() > 0 && ctx().world() != null ? ctx().world().npc(session.targetObjectId()).orElse(null) : null;
				handleNewbieGuideMenu(npc, cmd);
			} else {
				handleManorMenu(null, cmd);
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
			handleQuestBypass(session.targetObjectId(), questArg);
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
		if (cmd.startsWith("npc_0_") || cmd.startsWith("npc__")) {
			String action = cmd.startsWith("npc_0_") ? cmd.substring(6) : cmd.substring(5);
			if (action.startsWith("change_class")) {
				try {
					int targetClassId = Integer.parseInt(action.replace("change_class", "").trim());
					handleChangeClass(0, targetClassId);
					return;
				} catch (NumberFormatException ignored) {
				}
			} else if (action.startsWith("1stClass")) {
				showClassMasterMenu(0, 1);
				return;
			} else if (action.startsWith("2ndClass")) {
				showClassMasterMenu(0, 2);
				return;
			} else if (action.startsWith("3rdClass")) {
				showClassMasterMenu(0, 3);
				return;
			} else if (action.startsWith("Subclass")) {
				String subArg = action.length() > 8 ? action.substring(8).trim() : "";
				handleSubclassBypass(0, subArg);
				return;
			} else if (action.equals("Draw") || action.startsWith("Draw")) {
				session.onHennaList();
				return;
			} else if (action.equals("RemoveList") || action.startsWith("RemoveList")) {
				session.onHennaUnequipList();
				return;
			} else if (action.startsWith("Remove ")) {
				try {
					int slot = Integer.parseInt(action.substring(7).trim());
					session.onHennaRemoveBySlot(slot);
					return;
				} catch (NumberFormatException ignored) {
				}
			}
		}
		if (cmd.startsWith("npc_")) {
			// Formato: npc_%objectId%_Chat 1 ou npc_%objectId%_Link ... etc
			String[] parts = cmd.split("_", 3);
			if (parts.length >= 3) {
				try {
					int npcObjId = Integer.parseInt(parts[1]);
					var npcOpt = ctx().world().npc(npcObjId);
					if (npcOpt.isPresent()) {
						var npc = npcOpt.get();
						if (active() != null && !active().isGm()) {
							if (npc.isDead() || Math.hypot(active().x() - npc.x(), active().y() - npc.y()) > 250.0) {
								send(new ActionFailed());
								return;
							}
						}
						String action = parts[2];
						if (action.startsWith("Chat")) {
							int val = 0;
							if (action.length() > 4) {
								try {
									val = Integer.parseInt(action.substring(4).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							showNpcHtml(npc, val);
							return;
						} else if (action.startsWith("Link")) {
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
									String rendered = ctx().htmls().render(htm, npc.objectId(), npc.name(),
											active().name());
									send(new NpcHtmlMessage(npc.objectId(), rendered));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("1stClass")) {
							showClassMasterMenu(npc.objectId(), 1);
							return;
						} else if (action.startsWith("2ndClass")) {
							showClassMasterMenu(npc.objectId(), 2);
							return;
						} else if (action.startsWith("3rdClass")) {
							showClassMasterMenu(npc.objectId(), 3);
							return;
						} else if (action.startsWith("change_class")) {
							try {
								int targetClassId = Integer.parseInt(action.replace("change_class", "").trim());
								handleChangeClass(npc.objectId(), targetClassId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("SevenSignsDesc")) {
							String descArg = action.length() > 14 ? action.substring(14).trim() : "";
							handleSevenSignsDescBypass(npc, descArg);
							return;
						} else if (action.startsWith("SevenSigns")) {
							String signArg = action.length() > 10 ? action.substring(10).trim() : "";
							handleSevenSignsBypass(npc, signArg);
							return;
						} else if (action.startsWith("Quest")) {
							String questArg = action.length() > 5 ? action.substring(5).trim() : "";
							handleQuestBypass(npc.objectId(), questArg);
							return;
						} else if (action.startsWith("Subclass")) {
							String subArg = action.length() > 8 ? action.substring(8).trim() : "";
							handleSubclassBypass(npc.objectId(), subArg);
							return;
						} else if (action.startsWith("EnchantSkillList")) {
							session.showEnchantSkillList(npc);
							return;
						} else if (action.startsWith("SkillList") || action.startsWith("FishSkillList")) {
							session.showSkillList(npc);
							return;
						} else if (action.startsWith("questlist")) {
							if (ctx().htmls() != null) {
								String qHtm = ctx().htmls().getHtml("adventurer_guildsman/questlist.htm");
								if (qHtm != null) {
									send(new NpcHtmlMessage(npc.objectId(), ctx().htmls().render(qHtm, npc.objectId(), npc.name(), active().name())));
									return;
								}
							}
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("CPRecovery")) {
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
						} else if (action.startsWith("SupportMagic")) {
							handleNewbieSupportMagic(npc);
							return;
						} else if (action.startsWith("FestivalDesc")) {
							String num = action.length() > 12 ? action.substring(12).trim() : "1";
							handleFestivalDescBypass(npc, num);
							return;
						} else if (action.startsWith("Festival ")) {
							String fArg = action.substring(9).trim();
							handleFestivalBypass(npc, fArg);
							return;
						} else if (action.equals("Festival")) {
							handleFestivalBypass(npc, "1");
							return;
						} else if (action.equals("Return") || action.startsWith("Return")) {
							teleportToCoordinates(83400, 147943, -3404);
							return;
						} else if (action.startsWith("ExitRift")) {
							if (active() != null) {
								teleportToCoordinates(-114790, -180576, -6781);
							}
							return;
						} else if (action.startsWith("ChangeRiftRoom")) {
							if (active() != null) {
								send(new CreatureSay(0, CreatureSay.ALL, npc.name(), "The Dimensional Rift space is shifting..."));
							}
							return;
						} else if (action.startsWith("open_doors")) {
							handleDoorControl(true);
							return;
						} else if (action.startsWith("close_doors")) {
							handleDoorControl(false);
							return;
						} else if (action.equals("Draw") || action.startsWith("Draw")) {
							session.onHennaList();
							return;
						} else if (action.equals("RemoveList") || action.startsWith("RemoveList")) {
							session.onHennaUnequipList();
							return;
						} else if (action.startsWith("Remove ")) {
							try {
								int slot = Integer.parseInt(action.substring(7).trim());
								session.onHennaRemoveBySlot(slot);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("goto")) {
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
						} else if (action.startsWith("Buy")) {
							int listId = 1;
							if (action.length() > 3) {
								try {
									listId = Integer.parseInt(action.substring(3).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							session.showBuyList(npc, listId);
							return;
						} else if (action.startsWith("Sell")) {
							session.showSellList(npc);
							return;
						} else if (action.startsWith("Wear")) {
							int listId = 1;
							if (action.length() > 4) {
								try {
									listId = Integer.parseInt(action.substring(4).trim());
								} catch (NumberFormatException ignored) {
								}
							}
							session.showBuyList(npc, listId);
							return;
						} else if (action.startsWith("exc_multisell") || action.startsWith("multisell")) {
							try {
								String listIdStr = action.startsWith("exc_multisell")
										? action.substring(13).trim()
										: action.substring(9).trim();
								int listId;
								try {
									listId = Integer.parseInt(listIdStr);
								} catch (NumberFormatException e) {
									if ("tournament".equalsIgnoreCase(listIdStr)) {
										listId = 70000;
									} else {
										return;
									}
								}
								session.showMultiSell(npc, listId);
								return;
							} catch (Exception ignored) {
								return;
							}
						} else if (action.startsWith("DepositP") || action.startsWith("Deposit")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var depositable = active().inventory().items().stream()
									.filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST)
									.toList();
							send(new WareHouseDepositList(1, (int) active().inventory().adena(), depositable));
							return;
						} else if (action.startsWith("WithdrawP") || action.startsWith("Withdraw")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var stored = ctx().warehouse().getWarehouseItems(active().objectId());
							send(new WareHouseWithdrawalList(1, (int) active().inventory().adena(), stored));
							return;
						} else if (action.startsWith("DepositC")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var depositable = active().inventory().items().stream()
									.filter(it -> !it.isEquipped() && it.template().type2() != ItemTemplate.TYPE2_QUEST)
									.toList();
							send(new WareHouseDepositList(2, (int) active().inventory().adena(), depositable));
							return;
						} else if (action.startsWith("WithdrawC")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var stored = ctx().warehouse().getWarehouseItems(active().objectId());
							send(new WareHouseWithdrawalList(2, (int) active().inventory().adena(), stored));
							return;
						} else if (action.startsWith("DepositF") || action.startsWith("WithdrawF")) {
							if (ctx().warehouse() == null) {
								send(new ActionFailed());
								return;
							}
							var items = action.startsWith("DepositF")
									? active().inventory().items().stream()
											.filter(it -> !it.isEquipped()
													&& it.template().type2() != ItemTemplate.TYPE2_QUEST)
											.toList()
									: ctx().warehouse().getWarehouseItems(active().objectId());
							send(action.startsWith("DepositF")
									? new WareHouseDepositList(1, (int) active().inventory().adena(), items)
									: new WareHouseWithdrawalList(1, (int) active().inventory().adena(), items));
							return;
						} else if (action.startsWith("TerritoryStatus")) {
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
						} else if (action.startsWith("Augment")) {
							if (action.contains("2")) {
								send(new ExShowVariationCancelWindow());
							} else {
								send(new ExShowVariationMakeWindow());
							}
							return;
						} else if (action.startsWith("create_clan")) {
							String cName = action.length() > 11 ? action.substring(11).trim() : "";
							if (!cName.isEmpty()) {
								session.createClan(cName);
							}
							return;
						} else if (action.startsWith("increase_clan_level")) {
							session.increaseClanLevel();
							return;
						} else if (action.startsWith("dissolve_clan")) {
							session.dissolveClan();
							return;
						} else if (action.startsWith("getbuff")) {
							handleBufferGetBuff(npc, action.substring(7).trim());
							return;
						} else if (action.equals("restore") || action.startsWith("restore")) {
							handleBufferRestore(npc);
							return;
						} else if (action.equals("cancel") || action.startsWith("cancel")) {
							handleBufferCancel(npc);
							return;
						} else if (action.startsWith("support_back")) {
							showNpcHtml(npc, 0);
							return;
						} else if (action.startsWith("support ")) {
							handleSupportMagic(npc, action.substring(8).trim());
							return;
						} else if (action.startsWith("operate_door")) {
							handleOperateDoor(action.length() > 12 ? action.substring(12).trim() : "");
							return;
						} else if (action.startsWith("open_gate")) {
							handleDoorControl(true);
							return;
						} else if (action.startsWith("observeSiege") || action.startsWith("observeOracle") || action.startsWith("observe")) {
							String obsArgs = action.startsWith("observeSiege")
									? action.substring(12).trim()
									: (action.startsWith("observeOracle") ? action.substring(13).trim() : action.substring(7).trim());
							handleObservation(obsArgs);
							return;
						} else if (action.startsWith("OlympiadDesc")) {
							String num = action.length() > 12 ? action.substring(12).trim() : "0";
							handleOlympiadDesc(npc, num);
							return;
						} else if (action.startsWith("OlympiadNoble")) {
							String val = action.length() > 13 ? action.substring(13).trim() : "1";
							handleOlympiadNoble(npc, val);
							return;
						} else if (action.startsWith("Olympiad")) {
							handleOlympiadDesc(npc, "0");
							return;
						} else if (action.startsWith("Loto")) {
							String val = action.length() > 4 ? action.substring(4).trim() : "0";
							handleLoto(npc, val);
							return;
						} else if (action.startsWith("give_crown")) {
							handleGiveCrown();
							return;
						} else if (action.startsWith("receive_report")) {
							handleCastleReport(npc);
							return;
						} else if (action.startsWith("banish_foreigner")) {
							handleBanishForeigner(npc);
							return;
						} else if (action.startsWith("pvplist") || action.startsWith("pklist") || action.startsWith("onlinelist") || action.startsWith("arenalist")) {
							handleRankingList(npc, action);
							return;
						} else if (action.startsWith("manor_menu_select") || action.startsWith("menu_select")) {
							if (action.contains("ask=-7") || action.contains("ask=255") || (npc != null && npc.template() != null && npc.template().type().toLowerCase().contains("newbie"))) {
								handleNewbieGuideMenu(npc, action);
							} else {
								handleManorMenu(npc, action);
							}
							return;
						} else if (action.startsWith("SupportMagic") || action.startsWith("scripts_services.SupportMagic")) {
							handleNewbieSupportMagic(npc);
							return;
						} else if (action.startsWith("auction") || action.startsWith("bid") || action.startsWith("selectedItems") || action.startsWith("location") || action.startsWith("list")) {
							handleAuctionAction(npc, action);
							return;
						}
					} else {
						String action = parts[2];
						if (action.startsWith("change_class")) {
							try {
								int targetClassId = Integer.parseInt(action.replace("change_class", "").trim());
								handleChangeClass(npcObjId, targetClassId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("1stClass")) {
							showClassMasterMenu(npcObjId, 1);
							return;
						} else if (action.startsWith("2ndClass")) {
							showClassMasterMenu(npcObjId, 2);
							return;
						} else if (action.startsWith("3rdClass")) {
							showClassMasterMenu(npcObjId, 3);
							return;
						} else if (action.startsWith("Subclass")) {
							String subArg = action.length() > 8 ? action.substring(8).trim() : "";
							handleSubclassBypass(npcObjId, subArg);
							return;
						} else if (action.startsWith("goto")) {
							try {
								int teleId = Integer.parseInt(action.substring(4).trim());
								teleportTo(teleId);
								return;
							} catch (NumberFormatException ignored) {
							}
						} else if (action.startsWith("exc_multisell") || action.startsWith("multisell")) {
							try {
								String listIdStr = action.startsWith("exc_multisell")
										? action.substring(13).trim()
										: action.substring(9).trim();
								int listId;
								try {
									listId = Integer.parseInt(listIdStr);
								} catch (NumberFormatException e) {
									if ("tournament".equalsIgnoreCase(listIdStr)) {
										listId = 70000;
									} else {
										return;
									}
								}
								session.showMultiSell(null, listId);
								return;
							} catch (Exception ignored) {
							}
						} else if (action.startsWith("Link")) {
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

	private void handleBufferGetBuff(NpcInstance npc, String args) {
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
			String returnHtml = parts.length > 2 ? parts[2] : null;

			var skillTable = ctx().skillService() != null ? ctx().skillService().table() : null;
			var skOpt = skillTable != null ? skillTable.get(skillId, skillLvl) : java.util.Optional.<com.lopez.l2j.game.skill.SkillTemplate>empty();
			if (skOpt.isPresent()) {
				var sk = skOpt.get();
				session.applySkillEffects(sk, false);
				int npcObjId = npc != null ? npc.objectId() : active().objectId();
				var anim = new MagicSkillUse(npcObjId, active().objectId(), sk.id(), sk.level(), 500, 0);
				send(anim);
				if (ctx().world() != null) {
					ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, anim, false);
				}
			}

			if (returnHtml != null && !returnHtml.isBlank() && ctx().htmls() != null) {
				String path = returnHtml.startsWith("data/html/") ? returnHtml.substring(10) : returnHtml;
				String htm = ctx().htmls().getHtml(path);
				if (htm == null) htm = ctx().htmls().getIndexedHtml(path);
				if (htm != null) {
					int npcObjId = npc != null ? npc.objectId() : 0;
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

	private void handleBufferRestore(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		active().currentHp(active().maxHp());
		active().currentMp(active().maxMp());
		active().currentCp(active().maxCp());
		send(new StatusUpdate(active().objectId(), List.of(
				new StatusUpdate.Attribute(StatusUpdate.CUR_HP, (int) active().currentHp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_MP, (int) active().currentMp()),
				new StatusUpdate.Attribute(StatusUpdate.CUR_CP, (int) active().currentCp())
		)));
		int npcObjId = npc != null ? npc.objectId() : active().objectId();
		send(new MagicSkillUse(npcObjId, active().objectId(), 1012, 1, 0, 0));
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Buffer", "HP, MP e CP totalmente restaurados."));
		if (npc != null) {
			showNpcHtml(npc, 0);
		}
	}

	private void handleBufferCancel(NpcInstance npc) {
		if (active() == null) {
			send(new ActionFailed());
			return;
		}
		active().effects().clear();
		int npcObjId = npc != null ? npc.objectId() : active().objectId();
		send(new MagicSkillUse(npcObjId, active().objectId(), 1056, 1, 0, 0));
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Buffer", "Todos os buffs foram removidos."));
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
				var anim = new MagicSkillUse(npcObjId, active().objectId(), sk.id(), sk.level(), 500, 0);
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
						.append("<td width=70 align=center><font color=\"00FF00\">ONLINE</font></td>")
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

	private String resolveQuestHtml(com.lopez.l2j.game.quest.Quest q, String rawResult, NpcInstance npc) {
		if (rawResult == null || rawResult.isBlank()) {
			return null;
		}
		if (rawResult.contains("<html") || rawResult.contains("<HTML") || rawResult.contains("<body") || rawResult.contains("<BODY")) {
			return rawResult;
		}
		if ("noquest".equalsIgnoreCase(rawResult) || "no-quest".equalsIgnoreCase(rawResult)) {
			return ctx().htmls() != null ? ctx().htmls().getHtml("npcdefault.htm") : null;
		}
		if (ctx().htmls() == null) {
			return rawResult;
		}

		String qName = q != null ? q.getName() : "";
		// 1. Tenta dentro da pasta da quest especifica e suas variacoes de alias
		if (!qName.isEmpty()) {
			String h = ctx().htmls().getHtml("quests/" + qName + "/" + rawResult);
			if (h != null) return h;
			h = ctx().htmls().getHtml("scripts/quests/" + qName + "/" + rawResult);
			if (h != null) return h;

			if (qName.startsWith("_")) {
				String noUnderscore = qName.substring(1);
				h = ctx().htmls().getHtml("quests/" + noUnderscore + "/" + rawResult);
				if (h != null) return h;
				h = ctx().htmls().getHtml("scripts/quests/" + noUnderscore + "/" + rawResult);
				if (h != null) return h;
			} else {
				String withUnderscore = "_" + qName;
				h = ctx().htmls().getHtml("quests/" + withUnderscore + "/" + rawResult);
				if (h != null) return h;
				h = ctx().htmls().getHtml("scripts/quests/" + withUnderscore + "/" + rawResult);
				if (h != null) return h;
			}
		}

		if (q != null && q.getQuestId() > 0) {
			int qId = q.getQuestId();
			String h = ctx().htmls().getHtml("quests/" + qId + "/" + rawResult);
			if (h != null) return h;
			h = ctx().htmls().getHtml(String.format("quests/%03d/%s", qId, rawResult));
			if (h != null) return h;
			h = ctx().htmls().getHtml(String.format("quests/q%03d/%s", qId, rawResult));
			if (h != null) return h;
			h = ctx().htmls().getHtml(String.format("quests/Q%03d/%s", qId, rawResult));
			if (h != null) return h;
		}

		// 2. Tenta pelo nome indexado (ex: 31554-01.htm, merc_kahmun_q0628_01.htm)
		String h = ctx().htmls().getIndexedHtml(rawResult);
		if (h != null) return h;

		// 3. Tenta caminhos diretos e alternativos
		h = ctx().htmls().getHtml(rawResult);
		if (h != null) return h;
		h = ctx().htmls().getHtml("default/" + rawResult);
		if (h != null) return h;

		// 4. Se contiver caminho com barra, tenta o basename no indice global
		int lastSlash = Math.max(rawResult.lastIndexOf('/'), rawResult.lastIndexOf('\\'));
		if (lastSlash >= 0 && lastSlash < rawResult.length() - 1) {
			String baseName = rawResult.substring(lastSlash + 1);
			h = ctx().htmls().getIndexedHtml(baseName);
			if (h != null) return h;
		}

		// Fallback seguro para evitar exibir texto cru do nome do arquivo para o jogador
		String def = ctx().htmls().getHtml("npcdefault.htm");
		if (def != null && !def.isBlank()) {
			return def;
		}
		return "<html><body>" + (npc != null ? npc.name() : "NPC") + ":<br><br>I have no tasks for you right now.</body></html>";
	}

	public void handleQuestBypass(int npcObjId, String questArg) {
		var npcOpt = ctx().world().npc(npcObjId);
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

			// 2. Quests que este NPC pode iniciar
			if (ctx().questManager() != null) {
				for (var q : ctx().questManager().getAllQuests()) {
					if (q.hasStartNpc(npcId)) {
						QuestState qs = session.getQuestState(q.getName());
						if (qs != null && qs.isCompleted()) {
							continue;
						}
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
						if (qs.isStarted() && qs.getCond() > 0) {
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
				if (qs == null && q.hasStartNpc(npc.npcId())) {
					qs = q.newQuestState(session);
				}
				String html = q.notifyTalk(npc, session);
				if (html != null && !html.isBlank() && !"noquest".equalsIgnoreCase(html) && !"no-quest".equalsIgnoreCase(html)) {
					String resolved = resolveQuestHtml(q, html, npc);
					if (resolved != null && !resolved.isBlank()) {
						String rendered = ctx().htmls() != null
								? ctx().htmls().render(resolved, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId())
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
			if (qs == null && q.hasStartNpc(npc.npcId())) {
				qs = q.newQuestState(session);
			}
			String html;
			if (event != null && !event.isEmpty()) {
				html = q.notifyEvent(event, npc, session);
			} else {
				html = q.notifyTalk(npc, session);
			}
			if (html != null && !html.isBlank()) {
				if ("noquest".equalsIgnoreCase(html) || "no-quest".equalsIgnoreCase(html)) {
					String noQuestHtm = "<html><body>You are either not on a quest that involves session NPC, or you don't meet session NPC's minimum quest requirements.</body></html>";
					send(new NpcHtmlMessage(npc.objectId(), noQuestHtm));
					return;
				}
				String resolved = resolveQuestHtml(q, html, npc);
				if (resolved != null && !resolved.isBlank()) {
					String rendered = ctx().htmls() != null
							? ctx().htmls().render(resolved, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId())
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
				qHtml = ctx().htmls().getIndexedHtml(filename);
				if (qHtml == null) {
					qHtml = ctx().htmls().getHtml("village_master/" + parts[0] + "/" + filename);
				}
				if (qHtml == null) {
					qHtml = ctx().htmls().getHtml("quests/" + parts[0] + "/" + filename);
				}
			} else {
				qHtml = ctx().htmls().getHtml("teleporter/" + questArg + ".htm");
				if (qHtml == null) {
					qHtml = ctx().htmls().getHtml("default/" + questArg + ".htm");
				}
				if (qHtml == null) {
					qHtml = ctx().htmls().getIndexedHtml(questArg);
				}
				if (qHtml == null) {
					qHtml = ctx().htmls().getIndexedHtml(npc.npcId() + "-01.htm");
				}
			}
			if (qHtml != null) {
				String rendered = ctx().htmls().render(qHtml, npc.objectId(), npc.name(), active() != null ? active().name() : "Player", castleId, npc.npcId());
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
					+ ":<br><br>You are either not on a quest that involves session NPC, or you don't meet session NPC's minimum quest requirements.</body></html>";
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
				send(new NpcHtmlMessage(npcObjId, "<html><body>There are no available subclasses for your character at session time.</body></html>"));
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
				send(new MagicSkillUse(active().objectId(), active().objectId(), 4339, 1, 0, 0));
				ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
						new MagicSkillUse(active().objectId(), active().objectId(), 4339, 1, 0, 0), false);
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

		send(new MagicSkillUse(active().objectId(), active().objectId(), 4383, 1, 0, 0));
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
				new MagicSkillUse(active().objectId(), active().objectId(), 4383, 1, 0, 0), false);

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
		send(new MagicSkillUse(active().objectId(), active().objectId(), 4339, 1, 0, 0));
		ctx().world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS,
				new MagicSkillUse(active().objectId(), active().objectId(), 4339, 1, 0, 0), false);
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
			case 30599 -> "guide_gludin_nina";
			case 30600 -> "guide_gludio_euria";
			case 30601, 30528 -> "guide_dwarf_gullin";
			case 30602, 30370 -> "guide_elf_roios";
			case 30129 -> "guide_delf_frankia";
			case 30573 -> "guide_orc_tanai";
			default -> "guide_human_cnacelot";
		};

		if (ask == -7) {
			int lvl = active().level();
			boolean isMage = active().isMage();
			String file;
			if (isMage) {
				if (lvl < 14) file = folder + "_m07.htm";
				else if (lvl < 20) file = folder + "_m14.htm";
				else file = folder + "_m20.htm";
			} else {
				if (lvl < 10) file = folder + "_f05.htm";
				else if (lvl < 15) file = folder + "_f10.htm";
				else if (lvl < 20) file = folder + "_f15.htm";
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

		send(new MagicSkillUse(npc != null ? npc.objectId() : 0, active().objectId(), 1204, 1, 1000, 0));
		send(new CreatureSay(0, CreatureSay.ALL, npc != null ? npc.name() : "Newbie Guide",
				"May the blessings of the Einhasad protect your path."));
	}


}

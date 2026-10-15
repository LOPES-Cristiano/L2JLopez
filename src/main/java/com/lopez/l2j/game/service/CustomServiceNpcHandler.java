package com.lopez.l2j.game.service;

import com.lopez.l2j.game.boss.BossManager;
import com.lopez.l2j.game.model.ClassId;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket.RadarControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * CustomServiceNpcHandler: Handler unificado para NPCs Custom com economia 100% IN-GAME (Zero Doações).
 * Cobre:
 * - 8000_RaidbossInfo (Radar & Respawn status)
 * - 8001_NpcLocationInfo (Localização de NPCs importantes)
 * - 7001..7003_ClassMaster (Avanço de 1ª, 2ª e 3ª classes por Adena)
 * - 5555_ClanManager, 5555_NoblesSeller, 5555_HeroSeller, 5555_AioSeller, 5555_VipSeller,
 *   5555_PkClean, 5555_KarmaClean, 5555_LvlUpSeller, 5555_LvlDownSeller, 5555_NameManager, 5555_ColorManager
 *   Todos operando estritamente com Adena (ID 57) e Gold Bar (ID 3470).
 */
@Service
public class CustomServiceNpcHandler extends Quest {

	private static final Logger log = LoggerFactory.getLogger(CustomServiceNpcHandler.class);

	public static final int ADENA = 57;
	public static final int GOLD_BAR = 3470;

	public static final Set<Integer> CUSTOM_NPCS = Set.of(
			50032, // Class Master
			50033, // Clan Manager
			50034, // Nobles Seller
			50035, // Hero Seller
			50036, // Aio Seller
			50037, // Vip Seller
			50038, // Pk / Karma Clean
			50039, // Level Manager (Up/Down)
			50040, // Name & Color Manager
			31729, 31730, 31731 // Raidboss & NPC Location Info
	);

	// Coordenadas dos principais Grand Bosses para Radar (8000_RaidbossInfo)
	public static final Map<Integer, int[]> BOSS_LOCATIONS = Map.of(
			29001, new int[] { 17724, 114804, -11670 },   // Queen Ant
			29006, new int[] { 42600, 214600, -3600 },    // Core
			29014, new int[] { 17980, 114890, -11700 },   // Orfen
			29019, new int[] { 177261, 114904, -7709 },   // Antharas
			29020, new int[] { 116033, 17447, 10107 },    // Baium
			29022, new int[] { 55312, 219168, -3223 },    // Zaken
			29028, new int[] { 212852, -114842, -1632 }   // Valakas
	);

	@Autowired(required = false)
	private BossManager bossManager;

	@Autowired
	public CustomServiceNpcHandler(@Autowired(required = false) QuestManager questManager) {
		super(5555, "CustomServiceNpcHandler", "Custom In-Game Services");
		for (int npcId : CUSTOM_NPCS) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}
		if (questManager != null) {
			questManager.registerQuest(this);
		}
	}

	@Override
	public String onTalk(NpcInstance npc, QuestState qs) {
		if (qs == null || qs.getPlayer() == null || npc == null) {
			return "<html><body>Saudações!</body></html>";
		}
		int npcId = npc.npcId();
		PlayerCharacter player = qs.getPlayer() != null ? qs.getPlayer().character() : null;
		if (player == null) {
			return "<html><body>Saudações!</body></html>";
		}

		return switch (npcId) {
			case 50032 -> getClassMasterHtml(player);
			case 50033 -> getClanManagerHtml(player);
			case 50034 -> getNoblesSellerHtml(player);
			case 50035 -> getHeroSellerHtml(player);
			case 50036 -> getAioSellerHtml(player);
			case 50037 -> getVipSellerHtml(player);
			case 50038 -> getPkKarmaCleanHtml(player);
			case 50039 -> getLevelManagerHtml(player);
			case 50040 -> getNameColorManagerHtml(player);
			case 31729, 31730, 31731 -> getRaidInfoHtml(player);
			default -> "<html><body>Olá aventureiro! Como posso ajuda-lo?</body></html>";
		};
	}

	@Override
	public String onAdvEvent(String event, NpcInstance npc, GameSession session) {
		if (session == null || session.character() == null || event == null) {
			return null;
		}
		PlayerCharacter player = session.character();

		if (event.startsWith("class_advance_")) {
			try {
				int targetClass = Integer.parseInt(event.substring(14));
				return handleClassAdvance(player, targetClass);
			} catch (Exception e) {
				return "<html><body>Classe inválida.</body></html>";
			}
		} else if ("buy_nobles".equalsIgnoreCase(event)) {
			return handleBuyNobles(player);
		} else if ("clean_pk".equalsIgnoreCase(event)) {
			return handleCleanPk(player);
		} else if ("clean_karma".equalsIgnoreCase(event)) {
			return handleCleanKarma(player);
		} else if ("level_up".equalsIgnoreCase(event)) {
			return handleLevelUp(player);
		} else if ("level_down".equalsIgnoreCase(event)) {
			return handleLevelDown(player);
		} else if (event.startsWith("radar_boss_")) {
			try {
				int bossId = Integer.parseInt(event.substring(11));
				return handleRadarBoss(session, bossId);
			} catch (Exception e) {
				return "<html><body>Boss não encontrado.</body></html>";
			}
		}

		return null;
	}

	public String getClassMasterHtml(PlayerCharacter player) {
		int lvl = player.level();
		int cId = player.classId();
		StringBuilder sb = new StringBuilder("<html><body><b>Class Master:</b><br>");
		if (lvl >= 20 && cId < 10) {
			sb.append("1ª Mudança de Classe disponível por 100.000 Adena.<br>");
			for (ClassId c : VillageMasterOccupationService.getNextClasses(cId)) {
				sb.append("<a action=\"bypass -h quest_CustomServiceNpcHandler class_advance_").append(c.getId())
						.append("\">Mudar para ").append(c.name()).append("</a><br>");
			}
		} else if (lvl >= 40 && cId >= 10 && cId < 88) {
			sb.append("2ª Mudança de Classe disponível por 1.000.000 Adena.<br>");
			for (ClassId c : VillageMasterOccupationService.getNextClasses(cId)) {
				sb.append("<a action=\"bypass -h quest_CustomServiceNpcHandler class_advance_").append(c.getId())
						.append("\">Mudar para ").append(c.name()).append("</a><br>");
			}
		} else if (lvl >= 76 && cId < 88) {
			sb.append("3ª Mudança de Classe disponível por 10 Barras de Ouro (Gold Bar).<br>");
			int thirdClass = cId + 1; // Mapeamento sequencial C4/Interlude
			sb.append("<a action=\"bypass -h quest_CustomServiceNpcHandler class_advance_").append(thirdClass)
					.append("\">Avançar para 3ª Classe</a><br>");
		} else {
			sb.append("Nenhuma mudança de classe disponível para o seu nível atual.");
		}
		sb.append("</body></html>");
		return sb.toString();
	}

	private String handleClassAdvance(PlayerCharacter player, int newClassId) {
		long costAdena = player.level() < 40 ? 100_000L : (player.level() < 76 ? 1_000_000L : 0L);
		int costGoldBar = player.level() >= 76 ? 10 : 0;

		var inv = player.inventory();
		if (inv == null) return "<html><body>Erro de inventário.</body></html>";

		if (costAdena > 0 && inv.getItemCount(ADENA) < costAdena) {
			return "<html><body>Você não possui Adena suficiente!</body></html>";
		}
		if (costGoldBar > 0 && inv.getItemCount(GOLD_BAR) < costGoldBar) {
			return "<html><body>Você não possui Gold Bars suficientes (10 necessárias)!</body></html>";
		}

		if (costAdena > 0) inv.destroyItemByItemId(ADENA, (int) costAdena);
		if (costGoldBar > 0) inv.destroyItemByItemId(GOLD_BAR, costGoldBar);

		player.classId(newClassId);
		log.info("ClassMaster: Jogador {} avançou para classe ID {}", player.name(), newClassId);
		return "<html><body>Parabéns! Sua classe foi atualizada com sucesso!</body></html>";
	}

	public String getNoblesSellerHtml(PlayerCharacter player) {
		return "<html><body><b>Noblesse Manager:</b><br>"
				+ "Torne-se Nobre por 50 Gold Bars (in-game).<br>"
				+ "<a action=\"bypass -h quest_CustomServiceNpcHandler buy_nobles\">Comprar Status de Nobre</a>"
				+ "</body></html>";
	}

	private String handleBuyNobles(PlayerCharacter player) {
		if (player.isNoble()) return "<html><body>Você já é Nobre!</body></html>";
		var inv = player.inventory();
		if (inv == null || inv.getItemCount(GOLD_BAR) < 50) {
			return "<html><body>Você precisa de 50 Gold Bars para se tornar Nobre.</body></html>";
		}
		inv.destroyItemByItemId(GOLD_BAR, 50);
		player.setNoble(true);
		log.info("NoblesSeller: Jogador {} adquiriu Noblesse", player.name());
		return "<html><body>Parabéns Nobre guerreiro! O status Noblesse foi ativado!</body></html>";
	}

	public String getPkKarmaCleanHtml(PlayerCharacter player) {
		return "<html><body><b>Limpador de PK / Karma:</b><br>"
				+ "Limpar Karma por 500.000 Adena.<br>"
				+ "<a action=\"bypass -h quest_CustomServiceNpcHandler clean_karma\">Limpar Karma</a><br><br>"
				+ "Limpar 1 PK por 1 Gold Bar.<br>"
				+ "<a action=\"bypass -h quest_CustomServiceNpcHandler clean_pk\">Limpar 1 PK</a>"
				+ "</body></html>";
	}

	private String handleCleanKarma(PlayerCharacter player) {
		if (player.karma() <= 0) return "<html><body>Você não possui Karma para limpar!</body></html>";
		var inv = player.inventory();
		if (inv == null || inv.getItemCount(ADENA) < 500_000) {
			return "<html><body>Você não possui Adena suficiente (500k necessário)!</body></html>";
		}
		inv.destroyItemByItemId(ADENA, 500_000);
		player.karma(0);
		return "<html><body>Seu Karma foi totalmente limpo!</body></html>";
	}

	private String handleCleanPk(PlayerCharacter player) {
		if (player.pkKills() <= 0) return "<html><body>Você não possui mortes PK registradas!</body></html>";
		var inv = player.inventory();
		if (inv == null || inv.getItemCount(GOLD_BAR) < 1) {
			return "<html><body>Você precisa de 1 Gold Bar para remover 1 PK!</body></html>";
		}
		inv.destroyItemByItemId(GOLD_BAR, 1);
		player.pkKills(player.pkKills() - 1);
		return "<html><body>1 PK foi removido com sucesso! Restantes: " + player.pkKills() + "</body></html>";
	}

	public String getLevelManagerHtml(PlayerCharacter player) {
		return "<html><body><b>Gerenciador de Nível:</b><br>"
				+ "<a action=\"bypass -h quest_CustomServiceNpcHandler level_up\">Subir 1 Nível (500k Adena)</a><br>"
				+ "<a action=\"bypass -h quest_CustomServiceNpcHandler level_down\">Descer 1 Nível (500k Adena)</a>"
				+ "</body></html>";
	}

	private String handleLevelUp(PlayerCharacter player) {
		if (player.level() >= 80) return "<html><body>Você já atingiu o nível máximo (80)!</body></html>";
		var inv = player.inventory();
		if (inv == null || inv.getItemCount(ADENA) < 500_000) {
			return "<html><body>Adena insuficiente!</body></html>";
		}
		inv.destroyItemByItemId(ADENA, 500_000);
		player.level(player.level() + 1);
		return "<html><body>Você subiu para o nível " + player.level() + "!</body></html>";
	}

	private String handleLevelDown(PlayerCharacter player) {
		if (player.level() <= 1) return "<html><body>Você já está no nível mínimo!</body></html>";
		var inv = player.inventory();
		if (inv == null || inv.getItemCount(ADENA) < 500_000) {
			return "<html><body>Adena insuficiente!</body></html>";
		}
		inv.destroyItemByItemId(ADENA, 500_000);
		player.level(player.level() - 1);
		return "<html><body>Você reduziu seu nível para " + player.level() + "!</body></html>";
	}

	public String getClanManagerHtml(PlayerCharacter player) {
		return "<html><body><b>Clan Manager:</b><br>Evolua seu clã e adquira habilidades por Adena e Gold Bars in-game.</body></html>";
	}

	public String getHeroSellerHtml(PlayerCharacter player) {
		return "<html><body><b>Hero Seller:</b><br>Status de Hero disponível exclusivamente nas Olimpíadas ou por eventos in-game.</body></html>";
	}

	public String getAioSellerHtml(PlayerCharacter player) {
		return "<html><body><b>AIO Buffer Manager:</b><br>Ative o status AIO com Gold Bars adquiridas em monstros de farm.</body></html>";
	}

	public String getVipSellerHtml(PlayerCharacter player) {
		return "<html><body><b>VIP Manager:</b><br>Ative seu VIP com moedas de eventos in-game.</body></html>";
	}

	public String getNameColorManagerHtml(PlayerCharacter player) {
		return "<html><body><b>Gerenciador de Título e Cores:</b><br>Personalize o visual do seu guerreiro com Gold Bars.</body></html>";
	}

	public String getRaidInfoHtml(PlayerCharacter player) {
		StringBuilder sb = new StringBuilder("<html><body><b>Radar de Grand Bosses:</b><br>");
		for (var entry : BOSS_LOCATIONS.entrySet()) {
			sb.append("<a action=\"bypass -h quest_CustomServiceNpcHandler radar_boss_").append(entry.getKey())
					.append("\">Localizar Boss #").append(entry.getKey()).append("</a><br>");
		}
		sb.append("</body></html>");
		return sb.toString();
	}

	private String handleRadarBoss(GameSession session, int bossId) {
		int[] coords = BOSS_LOCATIONS.get(bossId);
		if (coords != null) {
			session.send(new RadarControl(0, 1, coords[0], coords[1], coords[2]));
			return "<html><body>O radar foi apontado para as coordenadas do Boss no seu mapa!</body></html>";
		}
		return "<html><body>Coordenadas indisponíveis para este Boss.</body></html>";
	}
}

package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.cursed.CursedWeapon;
import com.lopez.l2j.game.cursed.CursedWeaponsManager;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler modular para pacotes e rotinas de ciclo de vida do personagem:
 * sincronizacao de coordenadas, persistencia periodica, log de itens,
 * recomendacoes,
 * queda de adena, instancias, cristas em NPCs e inicializacao de
 * titulo/anuncios
 * (options.properties, custom.properties, mods.properties, add-on.properties).
 */
@Component
public class CharacterPacketHandler {

	private static final Logger log = LoggerFactory.getLogger(CharacterPacketHandler.class);
	private static final int ADENA_ITEM_ID = 57;

	private final GameSession session;

	public CharacterPacketHandler() {
		this.session = null;
	}

	public CharacterPacketHandler(GameSession session) {
		this.session = session;
	}

	public boolean shouldAutoDeleteInvalidQuestData() {
		return Config.AUTO_DELETE_INVALID_QUEST_DATA;
	}

	public int getCoordSynchronizeMode() {
		return Config.COORD_SYNCHRONIZE;
	}

	public int getCharacterDataStoreIntervalMillis() {
		return Math.max(1_000, Config.CHARACTER_DATA_STORE_INTERVAL);
	}

	public boolean isLazyItemsUpdateEnabled() {
		return Config.LAZY_ITEMS_UPDATE;
	}

	public boolean shouldUpdateItemsOnCharStore() {
		return Config.UPDATE_ITEMS_ON_CHAR_STORE;
	}

	public boolean shouldFallDownOnDeath() {
		return Config.FALL_DOWN_ON_DEATH;
	}

	public boolean shouldRestorePlayerInstance() {
		return Config.RESTORE_PLAYER_INSTANCE;
	}

	public boolean canSummonPlayerToInstance(PlayerCharacter target) {
		if (target == null) {
			return false;
		}
		if (target.instanceId() > 0 && !Config.ALLOW_SUMMON_TO_INSTANCE) {
			return false;
		}
		return true;
	}

	public boolean shouldShowNpcCrest() {
		return Config.SHOW_NPC_CREST;
	}

	/**
	 * Aplica titulo inicial e cor de titulo ao criar/entrar com personagem novo
	 * (mods.properties: CUSTOM_START_TITLE, CUSTOM_TITLE_TEXT, TITLE_COLOR).
	 */
	public void applyStartTitleIfConfigured(PlayerCharacter player) {
		if (player == null) {
			return;
		}
		if (Config.CUSTOM_START_TITLE && Config.CUSTOM_TITLE_TEXT != null && !Config.CUSTOM_TITLE_TEXT.isBlank()) {
			player.title(Config.CUSTOM_TITLE_TEXT.trim());
		}
		if (Config.TITLE_COLOR != null && !Config.TITLE_COLOR.isBlank()) {
			try {
				int color = Integer.decode("0x" + Config.TITLE_COLOR.trim().replace("0x", "").replace("#", ""));
				if (color >= 0) {
					player.titleColor(color);
				}
			} catch (NumberFormatException ignored) {
			}
		}
	}

	/**
	 * Verifica se um evento de item deve ser gravado no log de auditoria
	 * (options.properties: LOG_ITEMS, IGNORE_LOG_ITEMS).
	 */
	public boolean shouldLogItemAction(int itemId, String action, PlayerCharacter actor) {
		if (!Config.LOG_ITEMS) {
			return false;
		}
		Set<Integer> ignored = getIgnoredLogItemIds();
		if (ignored.contains(itemId)) {
			return false;
		}
		log.info("[ItemAudit] actor={} action={} itemId={}",
				actor != null ? actor.name() : "system", action, itemId);
		return true;
	}

	public static Set<Integer> getIgnoredLogItemIds() {
		String raw = Config.IGNORE_LOG_ITEMS;
		if (raw == null || raw.isBlank()) {
			return Collections.emptySet();
		}
		Set<Integer> ids = new LinkedHashSet<>();
		for (String part : raw.split("[,;\\s]+")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				try {
					ids.add(Integer.parseInt(trimmed));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return ids;
	}

	/**
	 * Processa recomendacao entre jogadores respeitando ALT_RECOMMEND
	 * (custom.properties).
	 */
	public boolean evaluateRecommendation(PlayerCharacter giver, PlayerCharacter target) {
		if (giver == null || target == null || giver.objectId() == target.objectId()) {
			return false;
		}
		if (giver.recomLeft() <= 0 || target.recomHave() >= 255) {
			return false;
		}
		if (!Config.ALT_RECOMMEND && giver.recommendedToday().contains(target.objectId())) {
			return false;
		}
		giver.recomLeft(giver.recomLeft() - 1);
		giver.recommendedToday().add(target.objectId());
		target.recomHave(target.recomHave() + 1);
		return true;
	}

	/**
	 * Verifica se o jogador pode descartar um item no chao e calcula a taxa de drop
	 * de Adena
	 * (custom.properties: PLAYER_CAN_DROP_ADENA, PLAYER_RATE_DROP_ADENA).
	 */
	public boolean canPlayerDropItem(PlayerCharacter player, int itemId) {
		if (player == null || player.isDead()) {
			return false;
		}
		if (itemId == ADENA_ITEM_ID && !Config.PLAYER_CAN_DROP_ADENA && !player.isGm()) {
			return false;
		}
		return true;
	}

	public long calculatePlayerAdenaDropAmount(long baseAmount) {
		int rate = Math.max(1, Config.PLAYER_RATE_DROP_ADENA);
		return Math.max(0L, baseAmount * rate);
	}

	/**
	 * Verifica se anuncios de login estao habilitados para o personagem e se Hero
	 * skills
	 * permanecem ativas em subclass (add-on.properties).
	 */
	public boolean shouldAnnounceOnLogin(PlayerCharacter player, boolean isNewbie) {
		if (player == null) {
			return false;
		}
		if (Config.ANNOUNCE_HERO_LOGIN && player.isHero())
			return true;
		if (Config.ANNOUNCE_AIOX_LOGIN && player.isAio())
			return true;
		if (Config.ANNOUNCE_VIP_LOGIN && player.isVip())
			return true;
		if (Config.ANNOUNCE_LORD_LOGIN && player.isClanLeader() && player.clanId() > 0)
			return true;
		if (Config.ANNOUNCE_NEWBIE_LOGIN && (isNewbie || player.level() == 1))
			return true;
		return false;
	}

	public boolean isHeroSkillUsable(PlayerCharacter player) {
		if (player == null || !player.isHero()) {
			return false;
		}
		return !player.isSubClassActive() || Config.ALLOW_HERO_SKILL_ON_SUB;
	}

	/**
	 * Sincroniza estado de Cursed Weapons, Augmentation e Armor Sets no login do jogador (#277, #280).
	 */
	public void onPlayerLogin(GameSession targetSession, PlayerCharacter active) {
		GameSession s = targetSession != null ? targetSession : this.session;
		if (s == null || active == null) {
			return;
		}
		CursedWeaponsManager cwm = s.context() != null && s.context().cursedWeapons() != null
				? s.context().cursedWeapons()
				: CursedWeaponsManager.getInstance();
		if (cwm != null) {
			cwm.onPlayerLogin(active);
		}
		if (s.itemHandler() != null) {
			s.itemHandler().updateAugmentationBonus();
			s.itemHandler().updateArmorSetBonus();
		}
	}

	/**
	 * Processa notificacoes de desconexao em Olympiad e CTF (#419, #481).
	 */
	public void onPlayerDisconnect(GameSession targetSession, PlayerCharacter active) {
		GameSession s = targetSession != null ? targetSession : this.session;
		if (s == null || active == null || s.context() == null) {
			return;
		}
		var ctx = s.context();
		var olyGames = ctx.getBean(com.lopez.l2j.game.olympiad.OlympiadGameService.class);
		if (olyGames != null) {
			olyGames.onPlayerDisconnect(active.objectId());
		}
		if (ctx.ctf() != null) {
			ctx.ctf().onDisconnect(active.objectId());
		}
	}

	public void handleSendFriendMsg(GameClientPacket.RequestSendFriendMsg p) {
		if (session != null && session.socialHandler() != null && p != null) {
			session.socialHandler().handleSendFriendMsg(p);
		}
	}

	public void handleAutoSoulShot(GameClientPacket.RequestAutoSoulShot p) {
		if (session != null && session.itemHandler() != null && p != null) {
			session.itemHandler().handleAutoSoulShot(p);
		}
	}

	public void handleWarehouseWithdrawList(GameClientPacket.SendWareHouseWithDrawList p) {
		if (session != null && p != null) {
			session.onWareHouseWithdraw(p);
		}
	}

	public void handleWithdrawParty() {
		if (session != null && session.partyClanHandler() != null) {
			session.partyClanHandler().handleLeaveParty();
		}
	}

	public void handleOustPartyMember(GameClientPacket.RequestOustPartyMember p) {
		if (session != null && session.partyClanHandler() != null && p != null) {
			session.partyClanHandler().handleOustPartyMember(p);
		}
	}

	public void handleDismissParty() {
		if (session != null && session.partyClanHandler() != null) {
			session.partyClanHandler().handleDismissParty();
		}
	}

	public void handleQuestAbort(GameClientPacket.RequestQuestAbort p) {
		if (session == null || p == null || session.activeChar() == null || session.context() == null) {
			return;
		}
		var ctx = session.context();
		if (ctx.questManager() != null) {
			var q = ctx.questManager().getQuest(p.questId());
			if (q != null) {
				var st = session.getQuestState(q.getName());
				if (st != null) {
					st.exitQuest(true);
					ctx.questManager().deleteQuestState(st);
					ctx.questManager().sendQuestList(session);
				}
			}
		}
	}

	public void handleBypassBuildCmd(GameClientPacket.SendBypassBuildCmd p) {
		if (session == null || p == null || !session.inWorld() || session.activeChar() == null) {
			return;
		}
		if (!session.activeChar().isGm()) {
			session.send(new GameServerPacket.ActionFailed());
			return;
		}
		String cmd = p.command() == null ? "" : p.command().trim();
		if (cmd.isEmpty()) {
			return;
		}
		String fullCmd = cmd.startsWith("admin_") ? cmd : "admin_" + cmd;
		session.handleAdminCommand(fullCmd);
	}

	public void handleCursedWeaponList() {
		if (session != null && session.hennaHandler() != null) {
			session.hennaHandler().handleCursedWeaponList();
		}
	}

	public void handleCursedWeaponLocation() {
		if (session != null && session.hennaHandler() != null) {
			session.hennaHandler().handleCursedWeaponLocation();
		}
	}

	public void handlePledgePowerGradeList() {
		if (session != null && session.partyClanHandler() != null) {
			session.partyClanHandler().handlePledgePowerGradeList();
		}
	}

	public void handlePledgeMemberPowerInfo(GameClientPacket.RequestPledgeMemberPowerInfo p) {
		if (session != null && session.partyClanHandler() != null && p != null) {
			session.partyClanHandler().handlePledgeMemberPowerInfo(p);
		}
	}

	public void handlePledgePower(GameClientPacket.RequestPledgePower p) {
		if (session != null && session.partyClanHandler() != null && p != null) {
			session.partyClanHandler().handlePledgePower(p);
		}
	}
}

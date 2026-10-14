package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.zone.FlagZoneService;
import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket.MoveBackwardToLocation;
import com.lopez.l2j.network.game.packet.GameClientPacket.ValidatePosition;
import com.lopez.l2j.network.game.packet.GameServerPacket;

import java.util.function.Consumer;

/**
 * Handler para movimentação do jogador e verificação em tempo real de transições
 * de zonas especiais (como {@link FlagZoneService}).
 */
public class MovementPacketHandler {

	private final GameSession session;

	public MovementPacketHandler(GameSession session) {
		this.session = session;
	}

	public void handleMove(MoveBackwardToLocation p) {
		if (session == null) {
			return;
		}
		checkFlagZoneOnMove(session);
	}

	public void handleValidatePosition(ValidatePosition p) {
		if (session == null) {
			return;
		}
		checkFlagZoneOnMove(session);
	}

	/**
	 * Aciona a checagem de FlagZoneService durante a movimentação do jogador para
	 * ativar/desativar o pvpFlag em tempo real ao entrar ou sair de uma FlagZone.
	 */
	public static boolean checkFlagZoneOnMove(GameSession session) {
		if (session == null || !session.inWorld()) {
			return false;
		}
		PlayerCharacter active = session.activeChar();
		if (active == null || active.isDead()) {
			return false;
		}
		GameSession.Context ctx = session.context();
		if (ctx == null) {
			return false;
		}
		FlagZoneService flagZone = ctx.getBean(FlagZoneService.class);
		if (flagZone == null) {
			return false;
		}
		boolean changed = flagZone.updatePlayerZonePosition(active, ctx.zones(), session::send);
		if (changed) {
			session.sendUserInfoAndBroadcastCharInfo();
		}
		return changed;
	}

	/**
	 * Sobrecarga direta para checagem de transição de FlagZone em movimentação.
	 */
	public static boolean checkFlagZoneOnMove(
			PlayerCharacter player,
			FlagZoneService flagZoneService,
			ZoneTable zoneTable,
			Consumer<GameServerPacket> packetSender) {
		if (player == null || player.isDead() || flagZoneService == null) {
			return false;
		}
		return flagZoneService.updatePlayerZonePosition(player, zoneTable, packetSender);
	}
}

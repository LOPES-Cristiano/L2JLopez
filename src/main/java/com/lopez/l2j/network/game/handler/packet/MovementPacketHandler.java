package com.lopez.l2j.network.game.handler.packet;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.geodata.GeoEngine;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.game.zone.FlagZoneService;
import com.lopez.l2j.game.zone.ZoneTable;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameClientPacket.CannotMoveAnymore;
import com.lopez.l2j.network.game.packet.GameClientPacket.MoveBackwardToLocation;
import com.lopez.l2j.network.game.packet.GameClientPacket.ValidatePosition;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.ActionFailed;
import com.lopez.l2j.network.game.packet.GameServerPacket.StopMove;
import com.lopez.l2j.network.game.packet.GameServerPacket.ValidateLocation;

import java.util.function.Consumer;

/**
 * Handler para movimentação do jogador, validação de coordenadas com {@link GeoEngine},
 * parada forçada ({@link CannotMoveAnymore}) e verificação em tempo real de transições
 * de zonas especiais (como {@link FlagZoneService}).
 */
public class MovementPacketHandler {

	private final GameSession session;

	public MovementPacketHandler() {
		this.session = null;
	}

	public MovementPacketHandler(GameSession session) {
		this.session = session;
	}

	/**
	 * Valida se o comando de movimentação é permitido (bloqueio de teclado via Config,
	 * estado de teleporte/morte) e calcula o destino ajustado pelo {@link GeoEngine}.
	 */
	public GeoEngine.Location resolveMoveDestination(MoveBackwardToLocation p) {
		if (p == null) {
			return null;
		}
		if (p.moveMovement() == 0 && !Config.ALLOW_KEYBOARD_MOVEMENT) {
			if (session != null) {
				session.send(new ActionFailed());
			}
			return null;
		}
		int targetX = p.targetX();
		int targetY = p.targetY();
		int targetZ = p.targetZ();
		GeoEngine geo = GeoEngine.getInstance();
		if (geo != null) {
			if (geo.isBlockedByDynamicObjects(p.originX(), p.originY(), p.originZ(), targetX, targetY, targetZ)) {
				return new GeoEngine.Location(p.originX(), p.originY(), p.originZ());
			}
			if (geo.isEnabled()) {
				GeoEngine.Location checked = geo.moveCheck(
						p.originX(), p.originY(), p.originZ(),
						targetX, targetY, targetZ);
				if (checked != null) {
					return checked;
				}
			}
		}
		return new GeoEngine.Location(targetX, targetY, targetZ);
	}

	public void handleMove(MoveBackwardToLocation p) {
		if (session == null || p == null) {
			return;
		}
		if (p.moveMovement() == 0 && !Config.ALLOW_KEYBOARD_MOVEMENT) {
			session.send(new ActionFailed());
			return;
		}
		checkFlagZoneOnMove(session);
	}

	/**
	 * Valida pacote {@link ValidatePosition} com verificação de drift horizontal/vertical
	 * e colisão dinâmica via {@link GeoEngine}, enviando {@link ValidateLocation} se necessário.
	 *
	 * @return {@code true} se a posição foi aceita e sincronizada; {@code false} se houve snap-back
	 */
	public boolean validateAndSyncPosition(ValidatePosition p) {
		if (session == null || p == null || !session.inWorld()) {
			return false;
		}
		PlayerCharacter active = session.activeChar();
		if (active == null || active.isDead() || session.teleporting() || (p.x() == 0 && p.y() == 0)) {
			return false;
		}
		int realX = active.x();
		int realY = active.y();
		int realZ = active.z();
		double dx = p.x() - realX;
		double dy = p.y() - realY;
		double diffSq = dx * dx + dy * dy;

		GeoEngine geo = GeoEngine.getInstance();
		if (geo != null && geo.isBlockedByDynamicObjects(realX, realY, realZ, p.x(), p.y(), p.z())) {
			session.send(new ValidateLocation(active.objectId(), realX, realY, realZ, active.heading()));
			return false;
		}

		// Drift horizontal excessivo (> 500 unidades em modo estrito ou > 1000 unidades)
		int syncMode = Config.COORD_SYNCHRONIZE;
		double maxHorizontalDriftSq = (syncMode == 2) ? 250_000.0 : 1_000_000.0;
		if (diffSq > maxHorizontalDriftSq) {
			session.send(new ValidateLocation(active.objectId(), realX, realY, realZ, active.heading()));
			return false;
		}

		int resolvedZ = p.z();
		if (geo != null && geo.isEnabled()) {
			short geoZ = geo.getHeight(p.x(), p.y(), p.z());
			if (Math.abs(p.z() - geoZ) > 200) {
				if (Math.abs(realZ - geoZ) <= 200) {
					resolvedZ = geoZ;
				} else {
					session.send(new ValidateLocation(active.objectId(), realX, realY, realZ, active.heading()));
					return false;
				}
			} else {
				resolvedZ = geoZ;
			}
		}

		active.moveTo(p.x(), p.y(), resolvedZ);
		active.heading(p.heading());
		checkFlagZoneOnMove(session);
		return true;
	}

	public void handleValidatePosition(ValidatePosition p) {
		if (session == null) {
			return;
		}
		checkFlagZoneOnMove(session);
	}

	/**
	 * Processa parada de movimento reportada pelo cliente ({@link CannotMoveAnymore}),
	 * validando coordenadas contra drift/obstáculos e fazendo broadcast de {@link StopMove}.
	 */
	public void handleCannotMoveAnymore(CannotMoveAnymore p) {
		if (session == null || p == null || !session.inWorld()) {
			return;
		}
		PlayerCharacter active = session.activeChar();
		if (active == null || active.isDead() || session.teleporting()) {
			return;
		}
		int realX = active.x();
		int realY = active.y();
		int realZ = active.z();
		double dx = p.x() - realX;
		double dy = p.y() - realY;
		double diffSq = dx * dx + dy * dy;

		GeoEngine geo = GeoEngine.getInstance();
		boolean blocked = geo != null && geo.isBlockedByDynamicObjects(realX, realY, realZ, p.x(), p.y(), p.z());
		if (!blocked && diffSq <= 250_000.0 && (p.x() != 0 || p.y() != 0)) {
			int resolvedZ = p.z();
			if (geo != null && geo.isEnabled()) {
				short geoZ = geo.getHeight(p.x(), p.y(), p.z());
				if (Math.abs(p.z() - geoZ) <= 200) {
					resolvedZ = geoZ;
				}
			}
			active.moveTo(p.x(), p.y(), resolvedZ);
			active.heading(p.heading());
		}
		active.stopMove();
		StopMove stop = new StopMove(active.objectId(), active.x(), active.y(), active.z(), active.heading());
		session.send(stop);
		GameSession.Context ctx = session.context();
		if (ctx != null && ctx.world() != null) {
			ctx.world().broadcastAround(session, GameWorld.VISIBILITY_RADIUS, stop, false);
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

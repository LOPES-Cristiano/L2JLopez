package com.lopez.l2j.game.zone;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.CreatureSay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Servico de Zonas de Flag Automatica (Onda C6 - Repatriacao L2JRadical).
 *
 * <p>Regra de Negocio:</p>
 * <ul>
 *   <li>Zonas nobres de farm (ex: Primeval Isle, Imperial Tomb, Goddard Farm) onde a presenca
 *       do jogador atualiza automaticamente o status para flagado (nome roxo).</li>
 *   <li>Combate e abates dentro da Flag Zone nao geram pontos de Karma nem contagem de PK.</li>
 *   <li>Ao sair da zona, o flag entra em contagem regressiva padrao (20s).</li>
 * </ul>
 */
@Service
public class FlagZoneService {

	private static final Logger log = LoggerFactory.getLogger(FlagZoneService.class);

	private final ZoneTable zoneTable;
	private final List<Zone> customFlagZones = new CopyOnWriteArrayList<>();

	public FlagZoneService() {
		this(null);
	}

	@Autowired(required = false)
	public FlagZoneService(ZoneTable zoneTable) {
		this.zoneTable = zoneTable;
	}

	public void registerFlagZone(String name, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
		java.awt.Polygon poly = new java.awt.Polygon(
				new int[]{minX, maxX, maxX, minX},
				new int[]{minY, minY, maxY, maxY},
				4
		);
		ZoneShape shape = new ZoneShape(minZ, maxZ, poly);
		int id = customFlagZones.size() + 90000;
		Zone zone = new Zone(id, name, ZoneType.FLAG, false, false, 0, 0, List.of(shape));
		customFlagZones.add(zone);
		log.info("FlagZoneService: Registrada zona de flag '{}' [X: {}..{}, Y: {}..{}]", name, minX, maxX, minY, maxY);
	}

	public boolean isInsideFlagZone(int x, int y, int z) {
		for (Zone zone : customFlagZones) {
			if (zone.contains(x, y, z)) {
				return true;
			}
		}
		if (zoneTable != null) {
			return zoneTable.isInsideFlagZone(x, y, z);
		}
		return false;
	}

	public boolean isInsideFlagZone(PlayerCharacter player) {
		if (player == null) {
			return false;
		}
		return isInsideFlagZone(player.x(), player.y(), player.z());
	}

	/**
	 * Disparado quando o jogador entra nos limites de uma Flag Zone.
	 */
	public void onEnter(PlayerCharacter player, Consumer<GameServerPacket> packetSender) {
		if (player == null) {
			return;
		}
		player.pvpFlag(1);
		// Enquanto estiver na zona, o flag nao expira por tempo
		player.pvpFlagEndTime(Long.MAX_VALUE);

		if (packetSender != null) {
			packetSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Voce entrou em uma Zona de Flag Automatica (PvP liberado sem perda de Karma)!"));
		}
	}

	/**
	 * Disparado quando o jogador sai dos limites da Flag Zone.
	 */
	public void onExit(PlayerCharacter player, Consumer<GameServerPacket> packetSender) {
		if (player == null) {
			return;
		}
		// Inicia contagem regressiva retail de 20s para decair o flag
		player.pvpFlagEndTime(System.currentTimeMillis() + 20_000L);

		if (packetSender != null) {
			packetSender.accept(new CreatureSay(0, CreatureSay.ALL, "SYS",
					"Voce saiu da Zona de Flag Automatica."));
		}
	}

	/**
	 * Verifica se o combate entre atacante e alvo e isento de Karma e contagem de PK.
	 */
	public boolean isKarmaFree(PlayerCharacter attacker, PlayerCharacter target) {
		return isInsideFlagZone(target) || isInsideFlagZone(attacker);
	}

	public List<Zone> customFlagZones() {
		return List.copyOf(customFlagZones);
	}

	public void clearCustomZones() {
		customFlagZones.clear();
	}
}

package com.lopez.l2j.game.staticobject;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.model.PlayerCharacter;

/**
 * Representa um objeto estatico no cenario (StaticObject / L2StaticObjectInstance do L2JDream).
 * Exemplos: Mapas de vila (Town Map), Placas de sinalizacao (Signboard), Tronos e Estatuas.
 */
public class StaticObjectInstance {

	public static final int TYPE_TOWN_MAP = 0;
	public static final int TYPE_THRONE_STATUE = 1;
	public static final int TYPE_SIGNBOARD = 2;

	private final int objectId;
	private final int staticObjectId;
	private final int type;
	private final String texture;
	private final int x;
	private final int y;
	private final int z;
	private final int mapX;
	private final int mapY;

	public StaticObjectInstance(int objectId, int staticObjectId, int type, String texture,
			int x, int y, int z, int mapX, int mapY) {
		this.objectId = objectId;
		this.staticObjectId = staticObjectId;
		this.type = type;
		this.texture = texture != null ? texture : "";
		this.x = x;
		this.y = y;
		this.z = z;
		this.mapX = mapX;
		this.mapY = mapY;
	}

	public int objectId() {
		return objectId;
	}

	public int staticObjectId() {
		return staticObjectId;
	}

	public int type() {
		return type;
	}

	public String texture() {
		return texture;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	public int mapX() {
		return mapX;
	}

	public int mapY() {
		return mapY;
	}

	public boolean isTownMap() {
		return type == TYPE_TOWN_MAP;
	}

	public boolean isSignboard() {
		return type == TYPE_SIGNBOARD;
	}

	public boolean isThroneOrStatue() {
		return type == TYPE_THRONE_STATUE;
	}

	/**
	 * Verifica se o jogador pode sentar no trono deste objeto estatico,
	 * respeitando Config.ONLY_CLAN_LEADER_CAN_SIT_ON_THRONE (L2StaticObjectInstance do L2JDream).
	 */
	public boolean canSitOnThrone(PlayerCharacter player, Clan clan, int castleOwnerClanId) {
		if (!isThroneOrStatue() || player == null) {
			return false;
		}
		if (castleOwnerClanId > 0 && player.clanId() != castleOwnerClanId) {
			return false;
		}
		if (Config.ONLY_CLAN_LEADER_CAN_SIT_ON_THRONE) {
			return clan != null && clan.isLeader(player.objectId());
		}
		return true;
	}
}

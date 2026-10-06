package com.lopez.l2j.game.cursed;

import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.login.packet.PacketReader;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CursedWeaponsManagerTest {

	private CursedWeaponsManager manager;

	@BeforeEach
	void setUp() {
		manager = new CursedWeaponsManager("data/xml/world/cursedWeapons.xml", null);
	}

	@Test
	void loadsWeaponsFromXml() {
		assertThat(manager.allWeapons()).hasSize(2);
		assertThat(manager.isCursedWeapon(8190)).isTrue();
		assertThat(manager.isCursedWeapon(8689)).isTrue();
		assertThat(manager.isCursedWeapon(1234)).isFalse();

		var zariche = manager.getCursedWeapon(8190).orElseThrow();
		assertThat(zariche.name()).isEqualTo("Demonic Sword Zariche");
		assertThat(zariche.skillId()).isEqualTo(3603);
		assertThat(zariche.durationMinutes()).isGreaterThan(0);
	}

	@Test
	void forceDropAndActivateLifecycle() {
		var akamanah = manager.getCursedWeapon(8689).orElseThrow();
		assertThat(akamanah.isActive()).isFalse();
		assertThat(akamanah.isDropped()).isFalse();

		boolean dropped = manager.forceDrop(8689, 10000, 20000, -3000);
		assertThat(dropped).isTrue();
		assertThat(akamanah.isDropped()).isTrue();
		assertThat(akamanah.isActive()).isFalse();
		assertThat(akamanah.x()).isEqualTo(10000);
		assertThat(akamanah.y()).isEqualTo(20000);
		assertThat(akamanah.z()).isEqualTo(-3000);

		assertThat(manager.activeOrDroppedWeapons()).contains(akamanah);

		boolean activated = manager.activate(8689, 1001, "DarkSlayer");
		assertThat(activated).isTrue();
		assertThat(akamanah.isActive()).isTrue();
		assertThat(akamanah.isDropped()).isFalse();
		assertThat(akamanah.playerId()).isEqualTo(1001);
		assertThat(akamanah.playerName()).isEqualTo("DarkSlayer");
		assertThat(akamanah.endTime()).isGreaterThan(System.currentTimeMillis());

		manager.onKill(8689);
		assertThat(akamanah.kills()).isEqualTo(1);

		manager.endOfLife(8689);
		assertThat(akamanah.isActive()).isFalse();
		assertThat(akamanah.isDropped()).isFalse();
		assertThat(akamanah.playerId()).isEqualTo(0);
		assertThat(akamanah.kills()).isEqualTo(0);
	}

	@Test
	void encodesPacketsCorrectly() {
		var listPacket = new GameServerPacket.ExCursedWeaponList(List.of(8190, 8689));
		byte[] listBytes = listPacket.encode();
		var rList = new PacketReader(listBytes);
		assertThat(rList.readC()).isEqualTo(0xfe);
		assertThat(rList.readH()).isEqualTo(0x45);
		assertThat(rList.readD()).isEqualTo(2);
		assertThat(rList.readD()).isEqualTo(8190);
		assertThat(rList.readD()).isEqualTo(8689);

		var locInfo = new GameServerPacket.CursedWeaponLocationInfo(8190, 1, 1234, 5678, -99);
		var locPacket = new GameServerPacket.ExCursedWeaponLocation(List.of(locInfo));
		byte[] locBytes = locPacket.encode();
		var rLoc = new PacketReader(locBytes);
		assertThat(rLoc.readC()).isEqualTo(0xfe);
		assertThat(rLoc.readH()).isEqualTo(0x46);
		assertThat(rLoc.readD()).isEqualTo(1);
		assertThat(rLoc.readD()).isEqualTo(8190);
		assertThat(rLoc.readD()).isEqualTo(1);
		assertThat(rLoc.readD()).isEqualTo(1234);
		assertThat(rLoc.readD()).isEqualTo(5678);
		assertThat(rLoc.readD()).isEqualTo(-99);
	}
}

package com.lopez.l2j.game.boss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrandBossManagerTest {

	private GrandBossManager manager;

	@BeforeEach
	void setUp() {
		manager = new GrandBossManager(null, null);
	}

	@Test
	void initializesAllRetailGrandBosses() {
		assertThat(manager.allBosses()).hasSize(10);

		assertThat(manager.isGrandBoss(GrandBossManager.QUEEN_ANT)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.CORE)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.ORFEN)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.ANTHARAS)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.BAIUM)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.ZAKEN)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.VALAKAS)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.FRINTEZZA)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.VAN_HALTER)).isTrue();
		assertThat(manager.isGrandBoss(GrandBossManager.SAILREN)).isTrue();
		assertThat(manager.isGrandBoss(12345)).isFalse();
	}

	@Test
	void bossStatusLifecycle() {
		var antharas = manager.getBoss(GrandBossManager.ANTHARAS).orElseThrow();
		assertThat(antharas.name()).isEqualTo("Antharas");
		assertThat(antharas.locX()).isEqualTo(181323);
		assertThat(antharas.locY()).isEqualTo(114850);
		assertThat(antharas.locZ()).isEqualTo(-7670);

		assertThat(manager.getStatus(GrandBossManager.ANTHARAS)).isEqualTo(BossStatus.NOTSPAWN);

		manager.spawnBoss(GrandBossManager.ANTHARAS);
		assertThat(manager.getStatus(GrandBossManager.ANTHARAS)).isEqualTo(BossStatus.ALIVE);
		assertThat(antharas.isAlive()).isTrue();

		manager.onBossKilled(GrandBossManager.ANTHARAS);
		assertThat(manager.getStatus(GrandBossManager.ANTHARAS)).isEqualTo(BossStatus.INTERVAL);
		assertThat(antharas.isAlive()).isFalse();
		assertThat(antharas.respawnTime()).isGreaterThan(System.currentTimeMillis());

		manager.setStatus(GrandBossManager.ANTHARAS, BossStatus.WAITING);
		assertThat(manager.getStatus(GrandBossManager.ANTHARAS)).isEqualTo(BossStatus.WAITING);
	}
}

package com.lopez.l2j.game.manor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CastleManorManagerTest {

	private CastleManorManager manager;

	@BeforeEach
	void setUp() {
		manager = new CastleManorManager("data/xml/world/seeds.xml", null);
	}

	@Test
	void loadsSeedsFromXml() {
		assertThat(manager.allSeeds()).isNotEmpty();

		var seedOpt = manager.getSeed(5016);
		assertThat(seedOpt).isPresent();
		var seed = seedOpt.get();
		assertThat(seed.level()).isEqualTo(10);
		assertThat(seed.cropId()).isEqualTo(5073);
		assertThat(seed.matureId()).isEqualTo(5103);
		assertThat(seed.castleId()).isEqualTo(1); // Gludio
		assertThat(seed.reward1()).isEqualTo(1864);
		assertThat(seed.reward2()).isEqualTo(1878);

		var gludioSeeds = manager.getSeedsForCastle(1);
		assertThat(gludioSeeds).isNotEmpty();
	}

	@Test
	void seedProductionAndPurchase() {
		int castleId = 1;
		int seedId = 5016;

		manager.setSeedProduction(castleId, new SeedProduction(seedId, 100, 100, 200), CastleManorManager.PERIOD_CURRENT);

		var prod = manager.getSeedProduction(castleId, seedId, CastleManorManager.PERIOD_CURRENT).orElseThrow();
		assertThat(prod.amount()).isEqualTo(100);

		// Buy 10 seeds at 200 adena = 2000 adena
		long cost = manager.buySeed(castleId, seedId, 10);
		assertThat(cost).isEqualTo(2000L);
		assertThat(prod.amount()).isEqualTo(90);

		// Attempt to buy more than remaining (95 > 90) -> fails (-1)
		long fail = manager.buySeed(castleId, seedId, 95);
		assertThat(fail).isEqualTo(-1L);
		assertThat(prod.amount()).isEqualTo(90);
	}

	@Test
	void cropProcureAndSale() {
		int castleId = 1;
		int cropId = 5073;

		manager.setCropProcure(castleId, new CropProcure(cropId, 50, 50, 500, 1), CastleManorManager.PERIOD_CURRENT);

		var proc = manager.getCropProcure(castleId, cropId, CastleManorManager.PERIOD_CURRENT).orElseThrow();
		assertThat(proc.amount()).isEqualTo(50);

		// Sell 20 crops at 500 adena = 10000 adena
		long reward = manager.sellCrop(castleId, cropId, 20);
		assertThat(reward).isEqualTo(10000L);
		assertThat(proc.amount()).isEqualTo(30);

		// Attempt to sell more than wanted (35 > 30) -> fails (-1)
		long fail = manager.sellCrop(castleId, cropId, 35);
		assertThat(fail).isEqualTo(-1L);
		assertThat(proc.amount()).isEqualTo(30);
	}

	@Test
	void approvesNextPeriod() {
		int castleId = 1;
		int seedId = 5017;
		manager.setSeedProduction(castleId, new SeedProduction(seedId, 500, 500, 150), CastleManorManager.PERIOD_NEXT);

		assertThat(manager.getSeedProduction(castleId, seedId, CastleManorManager.PERIOD_CURRENT)).isEmpty();

		manager.approveNextPeriod();

		assertThat(manager.getSeedProduction(castleId, seedId, CastleManorManager.PERIOD_CURRENT)).isPresent();
		assertThat(manager.getSeedProduction(castleId, seedId, CastleManorManager.PERIOD_CURRENT).get().amount()).isEqualTo(500);
	}
}

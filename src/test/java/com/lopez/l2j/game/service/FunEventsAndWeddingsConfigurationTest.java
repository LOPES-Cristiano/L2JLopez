package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Onda 19: Testes de Configuracao de Fun Events, Loteria, Casamentos e Campeoes")
class FunEventsAndWeddingsConfigurationTest {

	@Test
	@DisplayName("Deve carregar corretamente parametros do Seven Signs e Festival")
	void testSevenSignsAndFestivalConfig() {
		assertThat(Config.ALT_CASTLE_FOR_DAWN).isTrue();
		assertThat(Config.ALT_CASTLE_FOR_DUSK).isTrue();
		assertThat(Config.ALT_REQUIRE_CLAN_CASTLE).isFalse();
		assertThat(Config.ALT_JOIN_DAWN_COST).isEqualTo(50000);
		assertThat(Config.STRICT_SEVEN_SIGNS).isTrue();
		assertThat(Config.ANNOUNCE_7S).isTrue();
		assertThat(Config.ALT_FESTIVAL_MIN_PLAYER).isEqualTo(5);
		assertThat(Config.ALT_MAX_PLAYER_CONTRIB).isEqualTo(1000000);
		assertThat(Config.ALT_DAWN_GATES_PDEF_MULT).isEqualTo(1.1);
		assertThat(Config.ALT_DUSK_GATES_PDEF_MULT).isEqualTo(0.8);
	}

	@Test
	@DisplayName("Deve carregar configuracoes da Loteria e PC Cafe")
	void testLotteryAndPcCafeConfig() {
		assertThat(Config.ALT_LOTTERY_PRIZE).isEqualTo(50000);
		assertThat(Config.ALT_LOTTERY_TICKET_PRICE).isEqualTo(2000);
		assertThat(Config.ALT_LOTTERY_5_NUMBER_RATE).isEqualTo(0.6);
		assertThat(Config.ALT_LOTTERY_4_NUMBER_RATE).isEqualTo(0.2);
		assertThat(Config.ALT_LOTTERY_3_NUMBER_RATE).isEqualTo(0.2);
		assertThat(Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE).isEqualTo(200);

		assertThat(Config.PC_CAFFE_ENABLED).isFalse();
		assertThat(Config.PC_CAFE_INTERVAL).isEqualTo(10);
		assertThat(Config.PC_CAFE_MIN_LEVEL).isEqualTo(20);
		assertThat(Config.PC_CAFE_MAX_LEVEL).isEqualTo(80);
	}

	@Test
	@DisplayName("Deve carregar parametros do Sistema de Casamentos")
	void testWeddingConfig() {
		assertThat(Config.ALLOW_WEDDING).isTrue();
		assertThat(Config.WEDDING_PRICE).isEqualTo(500000);
		assertThat(Config.WEDDING_PUNISH_INFIDELITY).isTrue();
		assertThat(Config.WEDDING_TELEPORT).isTrue();
		assertThat(Config.WEDDING_TELEPORT_PRICE).isEqualTo(500);
		assertThat(Config.WEDDING_TELEPORT_INTERVAL).isEqualTo(120);
		assertThat(Config.WEDDING_ALLOW_SAME_SEX).isFalse();
		assertThat(Config.WEDDING_FORMAL_WEAR).isTrue();
		assertThat(Config.WEDDING_DIVORCE_COSTS).isEqualTo(20);
		assertThat(Config.WEDDING_GIVE_BOW).isTrue();
		assertThat(Config.WEDDING_USE_NICK_COLOR).isTrue();
		assertThat(Config.WEDDING_NORMAL_PAIR_NICK_COLOR).isEqualTo("BF0000");
	}

	@Test
	@DisplayName("Deve carregar parametros estendidos de Champion mobs e eventos sazonais")
	void testChampionAndSeasonalEventsConfig() {
		assertThat(Config.CHAMPION_TITLE).isEqualTo("Champion");
		assertThat(Config.CHAMPION_HP_REGEN).isEqualTo(1.0);
		assertThat(Config.CHAMPION_SPECIAL_ITEM_ID).isEqualTo(6393);
		assertThat(Config.MEDAL_1_DROP_CHANCE).isEqualTo(10);
		assertThat(Config.MEDAL_2_DROP_CHANCE).isEqualTo(2);
		assertThat(Config.CRISTMAS_TREE_LIFE_TIME).isEqualTo(5);
		assertThat(Config.L2_DAY_DROP_CHANCE).isEqualTo(10);
		assertThat(Config.BIG_SQUASH_DROP_CHANCE).isEqualTo(10);
	}
}

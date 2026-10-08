package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Onda 20: Testes de Configuracao do Gameserver, Safe Reboot, Rift e Respawn")
class GameserverAndRiftConfigurationTest {

	@Test
	@DisplayName("Deve carregar configuracoes gerais do servidor e auditoria de banimento")
	void testServerAndAuditConfig() {
		assertThat(Config.SERVER_NAME).isEqualTo("L2JLopez");
		assertThat(Config.REQUEST_SERVER_ID).isEqualTo(1);
		assertThat(Config.MAXIMUM_ONLINE_USERS).isEqualTo(1000);
		assertThat(Config.ACCEPT_ALTERNATE_ID).isTrue();
		assertThat(Config.CHAR_MAX_NUMBER).isEqualTo(7);
		assertThat(Config.TIME_ZONE).isEqualTo("America/Sao_Paulo");
		assertThat(Config.BAN_CHAT_LOG).isTrue();
		assertThat(Config.BAN_ACCOUNT_LOG).isTrue();
		assertThat(Config.JAIL_LOG).isTrue();
		assertThat(Config.GLOBAL_BAN_TIME).isEqualTo(15);
	}

	@Test
	@DisplayName("Deve carregar protecoes do Safe Reboot e trono de clã")
	void testSafeRebootAndThroneConfig() {
		assertThat(Config.SAFE_REBOOT).isTrue();
		assertThat(Config.SAFE_REBOOT_TIME).isEqualTo(30);
		assertThat(Config.SAFE_REBOOT_DISABLE_ENCHANT).isTrue();
		assertThat(Config.SAFE_REBOOT_DISABLE_TELEPORT).isTrue();
		assertThat(Config.SAFE_REBOOT_DISABLE_CREATE_ITEM).isTrue();
		assertThat(Config.SAFE_REBOOT_DISABLE_TRANSACTION).isTrue();
		assertThat(Config.ONLY_CLAN_LEADER_CAN_SIT_ON_THRONE).isTrue();
	}

	@Test
	@DisplayName("Deve carregar regras de Dimensional Rift e custos de salas")
	void testDimensionalRiftConfig() {
		assertThat(Config.RIFT_MIN_PARTY_SIZE).isEqualTo(2);
		assertThat(Config.MAX_RIFT_JUMPS).isEqualTo(4);
		assertThat(Config.RIFT_SPAWN_DELAY).isEqualTo(10000);
		assertThat(Config.AUTO_JUMPS_DELAY_MIN).isEqualTo(480);
		assertThat(Config.AUTO_JUMPS_DELAY_MAX).isEqualTo(600);
		assertThat(Config.BOSS_ROOM_TIME_MULTIPLY).isEqualTo(1.5);
		assertThat(Config.RECRUIT_COST).isEqualTo(18);
		assertThat(Config.SOLDIER_COST).isEqualTo(21);
		assertThat(Config.HERO_COST).isEqualTo(33);
	}

	@Test
	@DisplayName("Deve carregar regras de respawn aleatorio na cidade e restauracao de status")
	void testRespawnAndClassMasterConfig() {
		assertThat(Config.RESPAWN_RANDOM_IN_TOWN).isTrue();
		assertThat(Config.RESPAWN_RANDOM_MAX_OFFSET).isEqualTo(20);
		assertThat(Config.RESPAWN_RESTORE_CP).isEqualTo(30);
		assertThat(Config.RESPAWN_RESTORE_HP).isEqualTo(70);
		assertThat(Config.RESPAWN_RESTORE_MP).isEqualTo(40);
		assertThat(Config.RAID_MINION_RESPAWN_TIME).isEqualTo(300000);
		assertThat(Config.USE_MONSTER_RND_SPAWN).isTrue();
		assertThat(Config.RND_SPAWN_ZONE).isEqualTo(300);
		assertThat(Config.SPAWN_CLASS_MASTER).isTrue();
		assertThat(Config.ALLOW_DIALOG_CLASS_MASTER).isTrue();
		assertThat(Config.CLASS_MASTER_POPUP_WINDOW).isTrue();
	}
}

package com.lopez.l2j.game.service;

import com.lopez.l2j.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Onda 21: Testes de Configuracao de Rede, Protocolos e Login Auth Server")
class NetworkAndAuthConfigurationTest {

	@Test
	@DisplayName("Deve carregar configuracoes de rede do Gameserver e limites de protocolo")
	void testGameserverNetworkConfig() {
		assertThat(Config.GAMESERVER_PORT).isEqualTo(7777);
		assertThat(Config.LOGIN_PORT).isEqualTo(9014);
		assertThat(Config.LOGIN_HOST).isEqualTo("127.0.0.1");
		assertThat(Config.MIN_PROTOCOL_VERSION).isEqualTo(730);
		assertThat(Config.MAX_PROTOCOL_VERSION).isEqualTo(746);
		assertThat(Config.MAXIMUM_DB_CONNECTIONS).isEqualTo(500);
	}

	@Test
	@DisplayName("Deve carregar protecoes contra forca bruta e flood no Login Server")
	void testLoginAuthServerSecurityConfig() {
		assertThat(Config.AUTH_SERVER_PORT).isEqualTo(2106);
		assertThat(Config.SHOW_LICENCE).isTrue();
		assertThat(Config.AUTO_CREATE_ACCOUNTS).isTrue();
		assertThat(Config.BRUT_PROTECTION).isTrue();
		assertThat(Config.DDOS_PROTECTION).isTrue();
		assertThat(Config.SESSION_TTL).isEqualTo(15);
		assertThat(Config.MAX_SESSIONS).isEqualTo(100);
		assertThat(Config.LOGIN_TRY_BEFORE_BAN).isEqualTo(5);
		assertThat(Config.LOGIN_BLOCK_AFTER_BAN).isEqualTo(600);
		assertThat(Config.MAX_ACCOUNT_REGISTRATION).isEqualTo(3000);
		assertThat(Config.ENABLE_FLOOD_PROTECTION).isTrue();
		assertThat(Config.FAST_CONNECTION_LIMIT).isEqualTo(15);
		assertThat(Config.NORMAL_CONNECTION_TIME).isEqualTo(500);
		assertThat(Config.FAST_CONNECTION_TIME).isEqualTo(250);
		assertThat(Config.MAX_CONNECTION_PER_IP).isEqualTo(5);
		assertThat(Config.INACTIVE_TIMEOUT).isEqualTo(3);
	}

	@Test
	@DisplayName("Deve carregar rede do servidor de login e conexoes de banco")
	void testLoginNetworkConfig() {
		assertThat(Config.LOGIN_AUTH_PORT).isEqualTo(9014);
		assertThat(Config.LOGIN_AUTH_HOSTNAME).isEqualTo("127.0.0.1");
		assertThat(Config.IP_UPDATE_TIME).isEqualTo(10);
		assertThat(Config.LOGIN_MAX_DB_CONNECTIONS).isEqualTo(1000);
	}
}

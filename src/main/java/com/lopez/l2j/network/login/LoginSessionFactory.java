package com.lopez.l2j.network.login;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.crypt.LoginCrypt;
import com.lopez.l2j.network.login.crypt.ScrambledRsaKeyPair;
import com.lopez.l2j.network.login.service.GameServerDirectory;
import com.lopez.l2j.network.login.service.LoginAccountService;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Cria sessoes de login. Mantem um pequeno conjunto de pares RSA (gerar 1024 bits por conexao seria caro),
 * criado so no primeiro uso, e sorteia um por sessao.
 */
@Component
public class LoginSessionFactory {

	static final int RSA_POOL_SIZE = 10;

	private final SecureRandom random = new SecureRandom();
	private final ServerProperties properties;
	private final LoginAccountService accounts;
	private final GameServerDirectory servers;
	private volatile List<ScrambledRsaKeyPair> pool;

	public LoginSessionFactory(ServerProperties properties, LoginAccountService accounts,
			GameServerDirectory servers) {
		this.properties = properties;
		this.accounts = accounts;
		this.servers = servers;
	}

	public LoginSession create(String clientIp) {
		List<ScrambledRsaKeyPair> keys = pool();
		ScrambledRsaKeyPair rsa = keys.get(random.nextInt(keys.size()));
		return new LoginSession(rsa, LoginCrypt.newSessionKey(random), properties.login().showLicence(), clientIp,
				accounts, servers, random);
	}

	private List<ScrambledRsaKeyPair> pool() {
		List<ScrambledRsaKeyPair> p = pool;
		if (p == null) {
			synchronized (this) {
				p = pool;
				if (p == null) {
					List<ScrambledRsaKeyPair> fresh = new ArrayList<>(RSA_POOL_SIZE);
					for (int i = 0; i < RSA_POOL_SIZE; i++) {
						fresh.add(ScrambledRsaKeyPair.generate(random));
					}
					pool = p = List.copyOf(fresh);
				}
			}
		}
		return p;
	}
}
package com.lopez.l2j.network.session;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.service.LoginAccountService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ponte login -> game no mesmo processo (substitui o protocolo interno AuthServerThread/GameServerThread
 * do legado). O login registra a chave ao enviar PlayOk; o game server a consome no AuthLogin. Se o
 * cliente nunca chegar ao game server, a conta e liberada apos {@link #PENDING_TTL}.
 */
@Component
public class SessionKeyRegistry {

	private static final Logger log = LoggerFactory.getLogger(SessionKeyRegistry.class);
	public static final Duration PENDING_TTL = Duration.ofSeconds(60);

	private record Pending(SessionKey key, Instant expiresAt) {
	}

	private final LoginAccountService accounts;
	private final boolean checkLoginKeys;
	private final Clock clock;
	private final Map<String, Pending> pending = new ConcurrentHashMap<>();

	@Autowired
	public SessionKeyRegistry(LoginAccountService accounts, ServerProperties properties) {
		this(accounts, properties.login().showLicence(), Clock.systemUTC());
	}

	public SessionKeyRegistry(LoginAccountService accounts, boolean checkLoginKeys, Clock clock) {
		this.accounts = accounts;
		this.checkLoginKeys = checkLoginKeys;
		this.clock = clock;
	}

	/** Chamado pelo login ao enviar PlayOk. */
	public void register(String account, SessionKey key) {
		pending.put(account, new Pending(key, clock.instant().plus(PENDING_TTL)));
	}

	/**
	 * Valida e consome a chave apresentada ao game server. Em caso de sucesso a conta continua reservada
	 * ate {@link #logout}.
	 */
	public boolean claim(String account, SessionKey presented) {
		Pending p = pending.get(account);
		if (p == null || p.expiresAt().isBefore(clock.instant()) || !p.key().matches(presented, checkLoginKeys)) {
			return false;
		}
		return pending.remove(account, p);
	}

	/** Chamado quando a conexao de jogo termina: libera a conta para um novo login. */
	public void logout(String account) {
		pending.remove(account);
		accounts.release(account);
	}

	/** Libera contas cujo cliente recebeu PlayOk mas nunca se conectou ao game server. */
	@Scheduled(fixedDelay = 15_000)
	public void expirePending() {
		Instant now = clock.instant();
		pending.forEach((account, p) -> {
			if (p.expiresAt().isBefore(now) && pending.remove(account, p)) {
				log.info("Chave de sessao de {} expirou sem conexao ao game server; liberando conta", account);
				accounts.release(account);
			}
		});
	}
}

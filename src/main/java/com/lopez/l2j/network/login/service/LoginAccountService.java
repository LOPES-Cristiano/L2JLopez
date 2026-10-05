package com.lopez.l2j.network.login.service;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.crypt.LegacyPasswordHasher;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Regras de autenticacao do login server (porta do AuthManager.loginValid/tryAuthLogin):
 * valida o nome, verifica senha e ban, cria conta automaticamente se configurado (com limite por IP)
 * e impede dois logins simultaneos da mesma conta.
 *
 * <p>Contas inexistentes e senha errada retornam o mesmo {@link AuthResult#INVALID_CREDENTIALS}, para nao
 * revelar quais usuarios existem.
 */
@Service
public class LoginAccountService {

	private static final Logger log = LoggerFactory.getLogger(LoginAccountService.class);
	private static final Pattern VALID_LOGIN = Pattern.compile("[a-z0-9_.\\-]{2,14}");

	private final AccountStore store;
	private final ServerProperties.Login config;
	private final Set<String> online = ConcurrentHashMap.newKeySet();
	private final Map<String, Integer> creationsByIp = new ConcurrentHashMap<>();

	@org.springframework.beans.factory.annotation.Autowired
	public LoginAccountService(AccountStore store, ServerProperties properties) {
		this(store, properties.login());
	}

	public LoginAccountService(AccountStore store, ServerProperties.Login config) {
		this.store = store;
		this.config = config;
	}

	/** Autentica e, em caso de sucesso, reserva a conta (libere com {@link #release}). */
	public Result authenticate(String login, String password, String ip) {
		if (login == null || password == null || !VALID_LOGIN.matcher(login).matches()) {
			return Result.of(AuthResult.INVALID_CREDENTIALS);
		}
		try {
			Optional<Account> found = store.find(login);
			Account account;
			if (found.isEmpty()) {
				if (!createIfAllowed(login, password, ip)) {
					return Result.of(AuthResult.INVALID_CREDENTIALS);
				}
				account = store.find(login).orElseThrow();
			} else {
				account = found.get();
				if (account.banned()) {
					log.info("Login recusado (conta banida): {}", login);
					return Result.of(AuthResult.BANNED);
				}
				if (!LegacyPasswordHasher.matches(password, account.passwordHash())) {
					log.info("Senha incorreta para {} de {}", login, ip);
					return Result.of(AuthResult.INVALID_CREDENTIALS);
				}
				creationsByIp.remove(ip);
			}
			if (!online.add(login)) {
				return Result.of(AuthResult.ALREADY_LOGGED_IN);
			}
			store.touch(login, ip, account.lastServerId());
			log.info("{} autenticado de {}", login, ip);
			return new Result(AuthResult.SUCCESS, account.accessLevel(), account.lastServerId());
		} catch (RuntimeException e) {
			log.error("Falha ao autenticar {}", login, e);
			return Result.of(AuthResult.SYSTEM_ERROR);
		}
	}

	public void release(String login) {
		online.remove(login);
	}

	public boolean isOnline(String login) {
		return online.contains(login);
	}

	public void recordLastServer(String login, String ip, int serverId) {
		store.touch(login, ip, serverId);
	}

	public int getAccessLevel(String login) {
		if (login == null) {
			return 0;
		}
		return store.find(login).map(Account::accessLevel).orElse(0);
	}

	private boolean createIfAllowed(String login, String password, String ip) {
		if (!config.autoCreateAccounts()) {
			return false;
		}
		int attempts = creationsByIp.merge(ip, 1, Integer::sum);
		if (attempts > config.maxAccountCreationsPerIp()) {
			log.warn("IP {} excedeu o limite de criacao de contas", ip);
			return false;
		}
		store.create(login, LegacyPasswordHasher.hash(password), ip);
		log.info("Conta criada automaticamente: {}", login);
		return true;
	}

	public record Result(AuthResult status, int accessLevel, int lastServerId) {
		static Result of(AuthResult status) {
			return new Result(status, 0, 0);
		}
	}
}
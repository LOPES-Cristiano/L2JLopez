package com.lopez.l2j.network.login.net;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.LoginSessionFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Servidor TCP do login (porta {@code l2.network.login-port}). Uma virtual thread por conexao; limite global
 * de conexoes simultaneas, protecao anti-flood por IP (FloodProtectedListener) e timeout de inatividade.
 * So sobe com {@code l2.login.listen=true}.
 */
@Component
@ConditionalOnProperty(prefix = "l2.login", name = "listen", havingValue = "true")
public class LoginServer implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(LoginServer.class);
	static final int MAX_CONNECTIONS = 1000;
	static final int IDLE_TIMEOUT_MS = 30_000;

	private static final class ForeignConnection {
		int connectionNumber = 1;
		long lastConnection;
		boolean isFlooding = false;

		ForeignConnection(long time) {
			this.lastConnection = time;
		}
	}

	private final int port;
	private final LoginSessionFactory sessions;
	private final Semaphore slots;
	private final Map<String, ForeignConnection> floodProtection = new ConcurrentHashMap<>();
	private volatile ServerSocket serverSocket;
	private volatile ExecutorService executor;
	private volatile boolean running;
	private volatile long lastIpUpdateCheckMs = System.currentTimeMillis();

	@org.springframework.beans.factory.annotation.Autowired
	public LoginServer(ServerProperties properties, LoginSessionFactory sessions) {
		this(properties.network().loginPort(), sessions);
	}

	/** Porta 0 = efemera (testes); use {@link #port()} depois de iniciar. */
	public LoginServer(int port, LoginSessionFactory sessions) {
		this.port = port;
		this.sessions = sessions;
		int maxAllowed = (Config.DDOS_PROTECTION && Config.MAX_SESSIONS > 0)
				? Math.max(10, Config.MAX_SESSIONS)
				: MAX_CONNECTIONS;
		this.slots = new Semaphore(maxAllowed);
	}

	public int port() {
		ServerSocket s = serverSocket;
		return s == null ? port : s.getLocalPort();
	}

	@Override
	public synchronized void start() {
		if (running) {
			return;
		}
		try {
			ServerSocket s = new ServerSocket();
			s.setReuseAddress(true);
			String bindHost = Config.AUTH_SERVER_HOSTNAME;
			if (bindHost == null || bindHost.isBlank() || "*".equals(bindHost.trim())) {
				bindHost = Config.LOGIN_AUTH_HOSTNAME;
			}
			if (bindHost != null && !bindHost.isBlank() && !"*".equals(bindHost.trim())
					&& !"0.0.0.0".equals(bindHost.trim()) && port != 0) {
				try {
					s.bind(new InetSocketAddress(bindHost.trim(), port));
				} catch (IOException bindEx) {
					s.bind(new InetSocketAddress(port));
				}
			} else {
				s.bind(new InetSocketAddress(port));
			}
			serverSocket = s;
		} catch (IOException e) {
			throw new IllegalStateException("Nao foi possivel abrir a porta de login " + port, e);
		}
		executor = Executors.newVirtualThreadPerTaskExecutor();
		running = true;
		Thread.ofPlatform().name("login-acceptor").daemon(true).start(this::acceptLoop);
		log.info("Login server escutando na porta {} (FloodProtection={}, DDoSProtection={}, MaxSessions={}, SessionTTL={}s, IpUpdateTime={}m)",
				serverSocket.getLocalPort(), Config.ENABLE_FLOOD_PROTECTION, Config.DDOS_PROTECTION,
				Config.MAX_SESSIONS, Config.SESSION_TTL, Config.IP_UPDATE_TIME);
	}

	private void acceptLoop() {
		while (running) {
			Socket client;
			try {
				client = serverSocket.accept();
			} catch (IOException e) {
				if (running) {
					log.warn("Falha no accept do login", e);
				}
				return;
			}
			checkPeriodicIpUpdate();
			String ip = client.getInetAddress().getHostAddress();
			if (Config.ENABLE_FLOOD_PROTECTION && isFloodBlocked(ip, client.getInetAddress().isLoopbackAddress())) {
				closeQuietly(client);
				continue;
			}
			if (!slots.tryAcquire()) {
				log.warn("Limite de conexoes/sessoes atingido (MaxSessions={}); recusando {}",
						Config.MAX_SESSIONS, client.getRemoteSocketAddress());
				releaseFloodConnection(ip);
				closeQuietly(client);
				continue;
			}
			try {
				int timeoutMs = Config.INACTIVE_TIMEOUT > 0
						? Math.min(IDLE_TIMEOUT_MS, Config.INACTIVE_TIMEOUT * 60_000)
						: IDLE_TIMEOUT_MS;
				client.setSoTimeout(timeoutMs);
				client.setTcpNoDelay(true);
				var connection = new LoginConnection(client, sessions.create(ip), () -> {
					releaseFloodConnection(ip);
					slots.release();
				});
				executor.execute(connection);
			} catch (IOException | RuntimeException e) {
				log.warn("Falha ao aceitar conexao de login", e);
				releaseFloodConnection(ip);
				closeQuietly(client);
				slots.release();
			}
		}
	}

	private boolean isFloodBlocked(String ip, boolean isLoopback) {
		long now = System.currentTimeMillis();
		ForeignConnection fc = floodProtection.get(ip);
		if (fc != null) {
			fc.connectionNumber += 1;
			if ((fc.connectionNumber > Config.FAST_CONNECTION_LIMIT
					&& (now - fc.lastConnection) < Config.NORMAL_CONNECTION_TIME)
					|| (now - fc.lastConnection) < Config.FAST_CONNECTION_TIME
					|| fc.connectionNumber > Config.MAX_CONNECTION_PER_IP) {
				fc.lastConnection = now;
				if (!fc.isFlooding) {
					fc.isFlooding = true;
					if (!isLoopback) {
						log.warn("Flood detectado do IP {} (conexoes={})", ip, fc.connectionNumber);
					}
				}
				if (!isLoopback) {
					fc.connectionNumber -= 1;
					return true;
				}
			}
			if (fc.isFlooding) {
				fc.isFlooding = false;
			}
			fc.lastConnection = now;
		} else {
			floodProtection.put(ip, new ForeignConnection(now));
		}
		return false;
	}

	private void releaseFloodConnection(String ip) {
		if (!Config.ENABLE_FLOOD_PROTECTION || ip == null) {
			return;
		}
		ForeignConnection fc = floodProtection.get(ip);
		if (fc != null) {
			fc.connectionNumber -= 1;
			if (fc.connectionNumber <= 0) {
				floodProtection.remove(ip);
			}
		}
	}

	private void checkPeriodicIpUpdate() {
		if (Config.IP_UPDATE_TIME <= 0) {
			return;
		}
		long now = System.currentTimeMillis();
		long intervalMs = Config.IP_UPDATE_TIME * 60_000L;
		if (now - lastIpUpdateCheckMs >= intervalMs) {
			lastIpUpdateCheckMs = now;
			long ttlMs = Math.max(1, Config.SESSION_TTL) * 1000L;
			floodProtection.entrySet().removeIf(e -> e.getValue().connectionNumber <= 0
					&& (now - e.getValue().lastConnection) > ttlMs);
		}
	}

	@Override
	public synchronized void stop() {
		running = false;
		closeQuietly(serverSocket);
		ExecutorService ex = executor;
		if (ex != null) {
			ex.shutdownNow();
		}
		floodProtection.clear();
		log.info("Login server parado");
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	private static void closeQuietly(java.io.Closeable c) {
		try {
			if (c != null) {
				c.close();
			}
		} catch (IOException ignored) {
			// nada a fazer
		}
	}
}

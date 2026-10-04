package com.lopez.l2j.network.login.net;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.network.login.LoginSessionFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
 * de conexoes simultaneas e timeout de inatividade. So sobe com {@code l2.login.listen=true}.
 */
@Component
@ConditionalOnProperty(prefix = "l2.login", name = "listen", havingValue = "true")
public class LoginServer implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(LoginServer.class);
	static final int MAX_CONNECTIONS = 1000;
	static final int IDLE_TIMEOUT_MS = 30_000;

	private final int port;
	private final LoginSessionFactory sessions;
	private final Semaphore slots = new Semaphore(MAX_CONNECTIONS);
	private volatile ServerSocket serverSocket;
	private volatile ExecutorService executor;
	private volatile boolean running;

	@org.springframework.beans.factory.annotation.Autowired
	public LoginServer(ServerProperties properties, LoginSessionFactory sessions) {
		this(properties.network().loginPort(), sessions);
	}

	/** Porta 0 = efemera (testes); use {@link #port()} depois de iniciar. */
	public LoginServer(int port, LoginSessionFactory sessions) {
		this.port = port;
		this.sessions = sessions;
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
			s.bind(new InetSocketAddress(port));
			serverSocket = s;
		} catch (IOException e) {
			throw new IllegalStateException("Nao foi possivel abrir a porta de login " + port, e);
		}
		executor = Executors.newVirtualThreadPerTaskExecutor();
		running = true;
		Thread.ofPlatform().name("login-acceptor").daemon(true).start(this::acceptLoop);
		log.info("Login server escutando na porta {}", serverSocket.getLocalPort());
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
			if (!slots.tryAcquire()) {
				log.warn("Limite de {} conexoes atingido; recusando {}", MAX_CONNECTIONS, client.getRemoteSocketAddress());
				closeQuietly(client);
				continue;
			}
			try {
				client.setSoTimeout(IDLE_TIMEOUT_MS);
				client.setTcpNoDelay(true);
				String ip = client.getInetAddress().getHostAddress();
				var connection = new LoginConnection(client, sessions.create(ip), slots::release);
				executor.execute(connection);
			} catch (IOException | RuntimeException e) {
				log.warn("Falha ao aceitar conexao de login", e);
				closeQuietly(client);
				slots.release();
			}
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

package com.lopez.l2j.network.game.net;

import com.lopez.l2j.config.ServerProperties;
import com.lopez.l2j.game.service.CharacterService;
import com.lopez.l2j.game.service.InventoryService;
import com.lopez.l2j.game.html.HtmCache;
import com.lopez.l2j.game.teleport.TeleportLocationTable;
import com.lopez.l2j.game.trade.BuyListTable;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.crypt.GameCrypt;
import com.lopez.l2j.network.session.SessionKeyRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Servidor TCP do jogo (porta {@code l2.network.game-port}). Uma virtual thread por conexao. So sobe com
 * {@code l2.game.listen=true}.
 */
@Component
@ConditionalOnProperty(prefix = "l2.game", name = "listen", havingValue = "true")
public class GameServer implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(GameServer.class);
	static final int MAX_CONNECTIONS = 2000;

	private final int port;
	private final GameSession.Context context;
	private final SecureRandom random = new SecureRandom();
	private final Semaphore slots = new Semaphore(MAX_CONNECTIONS);
	private volatile ServerSocket serverSocket;
	private volatile ExecutorService executor;
	private volatile boolean running;

	@Autowired
	public GameServer(ServerProperties p, SessionKeyRegistry sessionKeys, CharacterService characters,
			InventoryService inventories, GameWorld world, HtmCache htmls, TeleportLocationTable teleports,
			BuyListTable buylists, com.lopez.l2j.game.combat.CombatService combat,
			com.lopez.l2j.game.drop.DropService drops,
			com.lopez.l2j.game.shortcut.ShortCutRepository shortcuts,
			com.lopez.l2j.game.skill.SkillRepository skills,
			com.lopez.l2j.game.ai.NpcAiService npcAi,
			com.lopez.l2j.game.skill.SkillService skillService,
			@Autowired(required = false) com.lopez.l2j.game.multisell.MultiSellTable multisell) {
		this(p.network().gamePort(), new GameSession.Context(p.network().protocolMin(), p.network().protocolMax(),
				sessionKeys, characters, inventories, world, htmls, teleports, buylists, combat, drops, shortcuts, skills, npcAi,
				skillService, multisell, p.rates(), p.serverName()));
	}

	/** Porta 0 = efemera (testes); use {@link #port()} depois de iniciar. */
	public GameServer(int port, GameSession.Context context) {
		this.port = port;
		this.context = context;
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
			throw new IllegalStateException("Nao foi possivel abrir a porta do game server " + port, e);
		}
		executor = Executors.newVirtualThreadPerTaskExecutor();
		running = true;
		Thread.ofPlatform().name("game-acceptor").daemon(true).start(this::acceptLoop);
		log.info("Game server escutando na porta {} (protocolos {}-{})", serverSocket.getLocalPort(),
				context.protocolMin(), context.protocolMax());
	}

	private void acceptLoop() {
		while (running) {
			Socket client;
			try {
				client = serverSocket.accept();
			} catch (IOException e) {
				if (running) {
					log.warn("Falha no accept do game server", e);
				}
				return;
			}
			if (!slots.tryAcquire()) {
				log.warn("Limite de {} conexoes de jogo atingido; recusando {}", MAX_CONNECTIONS,
						client.getRemoteSocketAddress());
				closeQuietly(client);
				continue;
			}
			try {
				client.setTcpNoDelay(true);
				client.setKeepAlive(true);
				String ip = client.getInetAddress().getHostAddress();
				byte[] key = GameCrypt.newKey(random);
				var connection = new GameConnection(client, key, sink -> new GameSession(context, key, ip, sink),
						slots::release);
				executor.execute(connection);
			} catch (IOException | RuntimeException e) {
				log.warn("Falha ao aceitar conexao de jogo", e);
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
		log.info("Game server parado");
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

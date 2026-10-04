package com.lopez.l2j.network.game.net;

import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.crypt.GameCrypt;
import com.lopez.l2j.network.game.packet.GameClientPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uma conexao TCP de jogo. Enquadramento: 2 bytes LE com o tamanho total (cabecalho incluso) e corpo
 * cifrado com {@link GameCrypt} (sem padding/checksum). Envio sincronizado: outras sessoes podem mandar
 * pacotes a este cliente (chat) concorrentemente.
 */
final class GameConnection implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(GameConnection.class);

	static final int HEADER = 2;
	/** Maior pacote aceito do cliente; os legitimos ficam bem abaixo disso. */
	static final int MAX_FRAME = 16 * 1024;
	/** Antes do AuthLogin o cliente tem pouco tempo para se identificar; depois pode ficar ocioso. */
	static final int HANDSHAKE_TIMEOUT_MS = 30_000;

	private final Socket socket;
	private final GameCrypt crypt;
	private final GameSession session;
	private final Runnable onClose;
	private final Object writeLock = new Object();
	private OutputStream out;

	/** @param sessionFactory recebe o sink de envio desta conexao e devolve a sessao */
	GameConnection(Socket socket, byte[] cryptKey,
			Function<java.util.function.Consumer<GameServerPacket>, GameSession> sessionFactory, Runnable onClose) {
		this.socket = socket;
		this.crypt = new GameCrypt(cryptKey);
		this.session = sessionFactory.apply(this::send);
		this.onClose = onClose;
	}

	@Override
	public void run() {
		String peer = String.valueOf(socket.getRemoteSocketAddress());
		try (socket) {
			socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
			synchronized (writeLock) {
				out = socket.getOutputStream();
			}
			DataInputStream in = new DataInputStream(socket.getInputStream());
			boolean relaxedTimeout = false;
			while (true) {
				byte[] body = readFrame(in);
				crypt.decrypt(body, 0, body.length);
				if (!session.handle(body)) {
					return;
				}
				if (!relaxedTimeout && session.state() != GameClientPacket.State.CONNECTED) {
					socket.setSoTimeout(0); // cliente pode ficar parado na selecao/no mundo sem enviar nada
					relaxedTimeout = true;
				}
			}
		} catch (EOFException e) {
			log.debug("{} desconectou", peer);
		} catch (SocketTimeoutException e) {
			log.debug("{} nao completou o handshake a tempo", peer);
		} catch (IOException | IllegalArgumentException e) {
			log.debug("Conexao de jogo {} encerrada: {}", peer, e.toString());
		} catch (RuntimeException e) {
			log.error("Erro processando pacote de {}", peer, e);
		} finally {
			try {
				session.onDisconnect();
			} catch (RuntimeException e) {
				log.error("Erro ao finalizar sessao de {}", peer, e);
			}
			onClose.run();
		}
	}

	private static byte[] readFrame(DataInputStream in) throws IOException {
		int lo = in.readUnsignedByte();
		int hi = in.readUnsignedByte();
		int total = lo | (hi << 8);
		int size = total - HEADER;
		if (size < 1 || total > MAX_FRAME) {
			throw new IllegalArgumentException("tamanho de pacote invalido: " + total);
		}
		byte[] body = new byte[size];
		in.readFully(body);
		return body;
	}

	/** Cifra e envia; falhas de escrita fecham o socket (a thread de leitura encerra a sessao). */
	void send(GameServerPacket packet) {
		byte[] body = packet.encode();
		synchronized (writeLock) {
			if (out == null || socket.isClosed()) {
				return;
			}
			crypt.encrypt(body, 0, body.length);
			int total = body.length + HEADER;
			byte[] frame = new byte[total];
			frame[0] = (byte) total;
			frame[1] = (byte) (total >> 8);
			System.arraycopy(body, 0, frame, HEADER, body.length);
			try {
				out.write(frame);
				out.flush();
			} catch (IOException e) {
				log.debug("Falha ao enviar para {}: {}", socket.getRemoteSocketAddress(), e.toString());
				try {
					socket.close();
				} catch (IOException ignored) {
					// ja estamos encerrando
				}
			}
		}
	}
}

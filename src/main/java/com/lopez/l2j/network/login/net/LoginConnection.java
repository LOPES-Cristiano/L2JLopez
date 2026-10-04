package com.lopez.l2j.network.login.net;

import com.lopez.l2j.network.login.LoginSession;
import com.lopez.l2j.network.login.crypt.LoginCrypt;
import com.lopez.l2j.network.login.packet.LoginServerPacket;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uma conexao TCP de login. Enquadramento do protocolo L2: cada pacote e precedido por 2 bytes
 * little-endian com o tamanho TOTAL (cabecalho incluso); o corpo vai cifrado com Blowfish + checksum.
 * Qualquer violacao (tamanho absurdo, nao multiplo de 8, checksum errado, opcode invalido) fecha a conexao.
 */
final class LoginConnection implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(LoginConnection.class);

	/** Maior pacote aceito do cliente: o maior legitimo (RequestAuthLogin) tem ~176 bytes. */
	static final int MAX_FRAME = 1024;
	static final int HEADER = 2;

	private final Socket socket;
	private final LoginSession session;
	private final LoginCrypt crypt;
	private final Runnable onClose;

	LoginConnection(Socket socket, LoginSession session, Runnable onClose) {
		this.socket = socket;
		this.session = session;
		this.crypt = new LoginCrypt(session.blowfishKey());
		this.onClose = onClose;
	}

	@Override
	public void run() {
		String peer = String.valueOf(socket.getRemoteSocketAddress());
		try (socket) {
			DataInputStream in = new DataInputStream(socket.getInputStream());
			OutputStream out = socket.getOutputStream();
			send(out, session.init());
			while (true) {
				byte[] payload = readFrame(in);
				if (!crypt.decrypt(payload, 0, payload.length)) {
					log.warn("Checksum invalido de {}; fechando", peer);
					return;
				}
				LoginSession.Reply reply = session.handle(payload);
				for (LoginServerPacket packet : reply.packets()) {
					send(out, packet);
				}
				if (reply.close()) {
					return;
				}
			}
		} catch (EOFException e) {
			log.debug("{} desconectou", peer);
		} catch (SocketTimeoutException e) {
			log.debug("{} inativo; fechando", peer);
		} catch (IOException | IllegalArgumentException e) {
			log.debug("Conexao {} encerrada: {}", peer, e.toString());
		} finally {
			session.onDisconnect();
			onClose.run();
		}
	}

	private static byte[] readFrame(DataInputStream in) throws IOException {
		int lo = in.readUnsignedByte();
		int hi = in.readUnsignedByte();
		int total = lo | (hi << 8);
		int size = total - HEADER;
		if (total > MAX_FRAME || size < 8 || (size % 8) != 0) {
			throw new IllegalArgumentException("tamanho de pacote invalido: " + total);
		}
		byte[] payload = new byte[size];
		in.readFully(payload);
		return payload;
	}

	private void send(OutputStream out, LoginServerPacket packet) throws IOException {
		byte[] body = packet.encode();
		// folga para checksum (+4), chave do XOR pass (+4) e preenchimento ate multiplo de 8 (ate +8)
		byte[] buf = new byte[body.length + 16];
		System.arraycopy(body, 0, buf, 0, body.length);
		int size = crypt.encrypt(buf, 0, body.length);
		int total = size + HEADER;
		byte[] frame = new byte[total];
		frame[0] = (byte) total;
		frame[1] = (byte) (total >> 8);
		System.arraycopy(buf, 0, frame, HEADER, size);
		out.write(frame);
		out.flush();
	}
}

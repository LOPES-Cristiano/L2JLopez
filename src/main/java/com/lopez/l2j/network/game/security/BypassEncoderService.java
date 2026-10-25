package com.lopez.l2j.network.game.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico de codificacao e validacao de bypasses por sessao de jogador.
 * Protege o servidor contra ataques de injecao de bypasses forjados (Packet Injection).
 */
@Service
public class BypassEncoderService {

	private static final Logger log = LoggerFactory.getLogger(BypassEncoderService.class);

	public static final int MAX_BYPASSES_PER_SESSION = 512;

	private final Map<Integer, List<String>> playerSessionBypasses = new ConcurrentHashMap<>();

	public enum BypassType {
		ENCODED,
		ENCODED_BBS,
		SIMPLE_DIRECT,
		INVALID
	}

	public record DecodeResult(BypassType type, String command, boolean isValid) {
		public static DecodeResult valid(BypassType type, String command) {
			return new DecodeResult(type, command, true);
		}

		public static DecodeResult invalid(String rawCommand) {
			return new DecodeResult(BypassType.INVALID, rawCommand, false);
		}
	}

	// Prefixos de comandos permitidos diretamente pelo protocolo oficial do cliente
	private static final Set<String> ALLOWED_DIRECT_PREFIXES = Set.of(
			"_bbshome",
			"_bbsgetfav",
			"_bbslink",
			"_bbsloc",
			"_bbsclan",
			"_bbsmemo",
			"_bbsmail",
			"_bbsfriends",
			"_bbsblock",
			"_maillist_0_1_0_",
			"_friendlist_0_",
			"_bbsaddfav",
			"_bbsdelfav",
			"_mrsl",
			"_diary",
			"_match",
			"manor_menu_select",
			"_olympiad",
			"_dispel",
			"interface?",
			"arenawtt"
	);

	public List<String> getOrCreateSessionBypasses(int playerObjectId) {
		return playerSessionBypasses.computeIfAbsent(playerObjectId, k -> Collections.synchronizedList(new ArrayList<>()));
	}

	public void clearSessionBypasses(int playerObjectId) {
		List<String> list = playerSessionBypasses.remove(playerObjectId);
		if (list != null) {
			synchronized (list) {
				list.clear();
			}
		}
	}

	public String encodeHtmlForPlayer(int playerObjectId, String html, boolean isBbs) {
		List<String> list = getOrCreateSessionBypasses(playerObjectId);
		synchronized (list) {
			if (!isBbs) {
				list.clear();
			}
		}
		return encodeHtml(html, list, isBbs);
	}

	public DecodeResult decodeForPlayer(int playerObjectId, String bypass, boolean isBbs) {
		List<String> list = playerSessionBypasses.get(playerObjectId);
		return decode(bypass, list, isBbs);
	}

	public String encodeHtml(String html, List<String> sessionBypasses) {
		return encodeHtml(html, sessionBypasses, false);
	}

	/**
	 * Codifica todos os links "bypass -h ..." de um HTML, associando-os a lista de bypasses da sessao.
	 */
	public String encodeHtml(String html, List<String> sessionBypasses, boolean isBbs) {
		if (html == null || html.isEmpty() || sessionBypasses == null) {
			return html;
		}

		StringBuilder sb = new StringBuilder(html.length());
		char[] chars = html.toCharArray();
		int len = chars.length;
		int lastCopied = 0;
		int i = 0;

		while (i + 7 < len) {
			char quoteChar = chars[i];
			if ((quoteChar == '\"' || quoteChar == '\'')
					&& (chars[i + 1] == 'b' || chars[i + 1] == 'B')
					&& (chars[i + 2] == 'y' || chars[i + 2] == 'Y')
					&& (chars[i + 3] == 'p' || chars[i + 3] == 'P')
					&& (chars[i + 4] == 'a' || chars[i + 4] == 'A')
					&& (chars[i + 5] == 's' || chars[i + 5] == 'S')
					&& (chars[i + 6] == 's' || chars[i + 6] == 'S')
					&& Character.isWhitespace(chars[i + 7])) {

				int quoteEnd = i + 8;
				while (quoteEnd < len && chars[quoteEnd] != quoteChar) {
					quoteEnd++;
				}

				if (quoteEnd < len) {
					int bypassStart = i + 1;
					int bypassLen = quoteEnd - bypassStart;

					int cmdStart = 7; // após "bypass"
					while (cmdStart < bypassLen && Character.isWhitespace(chars[bypassStart + cmdStart])) {
						cmdStart++;
					}

					boolean hasDashH = false;
					if (cmdStart + 1 < bypassLen
							&& chars[bypassStart + cmdStart] == '-'
							&& (chars[bypassStart + cmdStart + 1] == 'h' || chars[bypassStart + cmdStart + 1] == 'H')) {
						hasDashH = true;
						cmdStart += 2;
						while (cmdStart < bypassLen && (chars[bypassStart + cmdStart] == ' ' || chars[bypassStart + cmdStart] == '\t')) {
							cmdStart++;
						}
					}

					String fullCommand = new String(chars, bypassStart + cmdStart, bypassLen - cmdStart);
					String cleanCommand = fullCommand;
					String paramSuffix = "";

					int paramIdx = fullCommand.indexOf(" $");
					if (paramIdx >= 0) {
						cleanCommand = fullCommand.substring(0, paramIdx);
						paramSuffix = fullCommand.substring(paramIdx);
					}

					sb.append(chars, lastCopied, bypassStart - lastCopied);
					lastCopied = quoteEnd;

					sb.append("bypass ");
					if (hasDashH) {
						sb.append("-h ");
					}

					char typePrefix = isBbs ? '1' : '0';
					sb.append(typePrefix);

					int index;
					synchronized (sessionBypasses) {
						int existingIdx = sessionBypasses.indexOf(cleanCommand);
						if (existingIdx >= 0) {
							index = existingIdx;
						} else {
							if (sessionBypasses.size() >= MAX_BYPASSES_PER_SESSION) {
								sessionBypasses.clear();
							}
							index = sessionBypasses.size();
							sessionBypasses.add(cleanCommand);
						}
					}

					sb.append(Integer.toHexString(index));
					sb.append(paramSuffix);

					i = quoteEnd;
					continue;
				}
			}
			i++;
		}

		sb.append(chars, lastCopied, len - lastCopied);
		return sb.toString();
	}

	/**
	 * Decodifica o comando enviado pelo jogador via RequestBypassToServer.
	 */
	public DecodeResult decode(String bypass, List<String> sessionBypasses, boolean isBbs) {
		if (bypass == null || bypass.trim().isEmpty()) {
			return DecodeResult.invalid(bypass);
		}

		for (int i = 0; i < bypass.length(); i++) {
			char c = bypass.charAt(i);
			if (c == '\0' || c == '\r' || c == '\n') {
				log.warn("Bypass rejeitado por conter caracteres de controle invalidos.");
				return DecodeResult.invalid(bypass);
			}
		}

		String trimmed = bypass.trim();
		if (trimmed.regionMatches(true, 0, "bypass ", 0, 7)) {
			trimmed = trimmed.substring(7).trim();
		}
		if (trimmed.startsWith("-h ") || trimmed.startsWith("-H ")) {
			trimmed = trimmed.substring(3).trim();
		} else if (trimmed.equalsIgnoreCase("-h")) {
			return DecodeResult.invalid(bypass);
		} else if (trimmed.startsWith("-h") || trimmed.startsWith("-H")) {
			trimmed = trimmed.substring(2).trim();
		}

		if (trimmed.isEmpty()) {
			return DecodeResult.invalid(bypass);
		}

		char firstChar = trimmed.charAt(0);
		if ((firstChar == '0' || firstChar == '1') && sessionBypasses != null && !sessionBypasses.isEmpty()) {
			BypassType type = (firstChar == '1') ? BypassType.ENCODED_BBS : BypassType.ENCODED;
			String token = trimmed.substring(1);
			String extraParams = "";

			int spaceIdx = token.indexOf(' ');
			if (spaceIdx >= 0) {
				extraParams = token.substring(spaceIdx);
				token = token.substring(0, spaceIdx);
			}

			if (!token.isEmpty()) {
				try {
					int index = Integer.parseInt(token, 16);
					synchronized (sessionBypasses) {
						if (index >= 0 && index < sessionBypasses.size()) {
							String baseCommand = sessionBypasses.get(index);
							String finalCommand = baseCommand + extraParams;
							return DecodeResult.valid(type, finalCommand);
						}
					}
					log.warn("Bypass codificado invalido (indice fora de alcance): index={} size={}",
							index, sessionBypasses.size());
					return DecodeResult.invalid(bypass);
				} catch (NumberFormatException ignored) {
					// Nao e token hexadecimal, segue para verificacao de comando direto
				}
			}
		}

		// Checagem de prefixos diretos permitidos
		for (String prefix : ALLOWED_DIRECT_PREFIXES) {
			if (trimmed.startsWith(prefix)) {
				return DecodeResult.valid(BypassType.SIMPLE_DIRECT, trimmed);
			}
		}

		// Valida se e um comando simples aceito pelo protocolo
		if (isCommonNpcChatBypass(trimmed)) {
			return DecodeResult.valid(BypassType.SIMPLE_DIRECT, trimmed);
		}

		// Permite comandos diretos conhecidos (ex: admin, voiced, multisell, etc)
		return DecodeResult.valid(BypassType.SIMPLE_DIRECT, trimmed);
	}

	public DecodeResult decode(String bypass, List<String> sessionBypasses) {
		return decode(bypass, sessionBypasses, false);
	}

	private boolean isCommonNpcChatBypass(String command) {
		String lower = command.toLowerCase(java.util.Locale.ROOT);
		return lower.startsWith("npc_") || lower.startsWith("chat ") || lower.startsWith("quest ")
				|| lower.startsWith("menu_select?") || lower.startsWith("link ")
				|| lower.startsWith("admin_") || lower.startsWith("voiced_")
				|| lower.startsWith("multisell") || lower.startsWith("exc_multisell")
				|| lower.startsWith("newbie_exc_multisell") || lower.startsWith("multisell_shadow")
				|| lower.startsWith("buyshadow") || lower.startsWith("exchange")
				|| lower.startsWith("arenachange") || lower.startsWith("olympiad_observe")
				|| lower.startsWith("receive_premium") || lower.startsWith("remove_clan_penalty");
	}
}

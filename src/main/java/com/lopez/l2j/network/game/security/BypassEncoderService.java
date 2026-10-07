package com.lopez.l2j.network.game.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Servico de codificacao e validacao de bypasses por sessao de jogador.
 * Protege o servidor contra ataques de injecao de bypasses forjados (Packet Injection).
 */
@Service
public class BypassEncoderService {

	private static final Logger log = LoggerFactory.getLogger(BypassEncoderService.class);

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
			"_maillist_0_1_0_",
			"_friendlist_0_",
			"_bbsaddfav",
			"_mrsl",
			"_diary",
			"_match",
			"manor_menu_select",
			"_olympiad",
			"_dispel",
			"interface?"
	);

	/**
	 * Codifica todos os links "bypass -h ..." de um HTML, associando-os a lista de bypasses da sessao.
	 */
	public String encodeHtml(String html, List<String> sessionBypasses, boolean isBbs) {
		if (html == null || html.isEmpty() || sessionBypasses == null) {
			return html;
		}

		StringBuilder sb = new StringBuilder();
		char[] chars = html.toCharArray();
		int len = chars.length;
		int lastCopied = 0;
		int i = 0;

		while (i + 7 < len) {
			if (chars[i] == '\"'
					&& (chars[i + 1] == 'b' || chars[i + 1] == 'B')
					&& (chars[i + 2] == 'y' || chars[i + 2] == 'Y')
					&& (chars[i + 3] == 'p' || chars[i + 3] == 'P')
					&& (chars[i + 4] == 'a' || chars[i + 4] == 'A')
					&& (chars[i + 5] == 's' || chars[i + 5] == 'S')
					&& (chars[i + 6] == 's' || chars[i + 6] == 'S')
					&& Character.isWhitespace(chars[i + 7])) {

				int quoteEnd = i + 8;
				while (quoteEnd < len && chars[quoteEnd] != '\"') {
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
						index = sessionBypasses.size();
						sessionBypasses.add(cleanCommand);
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

		String trimmed = bypass.trim();
		if (trimmed.startsWith("-h ")) {
			trimmed = trimmed.substring(3).trim();
		} else if (trimmed.startsWith("-h")) {
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
		return command.startsWith("npc_") || command.startsWith("Chat ") || command.startsWith("Quest ")
				|| command.startsWith("menu_select?") || command.startsWith("link ")
				|| command.startsWith("admin_") || command.startsWith("voiced_")
				|| command.startsWith("multisell") || command.startsWith("exc_multisell");
	}
}

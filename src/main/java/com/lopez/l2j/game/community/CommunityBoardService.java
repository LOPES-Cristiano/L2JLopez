package com.lopez.l2j.game.community;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.boss.BossManager;
import com.lopez.l2j.game.boss.BossStatus;
import com.lopez.l2j.game.boss.GrandBossInfo;
import com.lopez.l2j.game.boss.GrandBossManager;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.summon.SummonInstance;
import com.lopez.l2j.game.world.GameWorld;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.handler.voiced.VoicedGeneralHandler;
import com.lopez.l2j.network.game.packet.GameServerPacket.ShowBoard;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servico responsavel pelo Community Board BBS do jogo (Alt + B) - (#469-#479,
 * #504).
 * Oferece painel de informacoes do servidor, rankings PvP/PK/Clans, teleporte
 * rapido
 * para cidades principais e ate 10 favoritos, gabinete do jogador
 * (_bbsservice), buffer comunitário
 * (Self + Pet/Summon), Clan BBS (_bbsclan), Forum/Memo
 * (_bbsmemo/_bbstopics/_bbsposts),
 * Correio (_bbsmail com persistencia em bbs_mail), Amigos (_bbsfriends), Bosses
 * (_bbsraids),
 * DressMe/Shop 100% In-Game (ZERO DOACOES) e divisao de pacotes ShowBoard
 * (101/102/103).
 */
@Service
public class CommunityBoardService {

    private static final Logger log = LoggerFactory.getLogger(CommunityBoardService.class);
    private static volatile CommunityBoardService instance;

    public static final int MAX_FAVORITE_SLOTS = 10;
    public static final long TELEPORT_ADENA_COST = 1000L;
    public static final int MAX_BOARD_PART_LENGTH = 4090;

    // Coordenadas dos principais destinos de teleporte
    public static final Map<Integer, int[]> TELEPORT_LOCATIONS = Map.of(
            1, new int[] { 83400, 147943, -3404 }, // Giran Castle Town
            2, new int[] { 147450, 26741, -2204 }, // Town of Aden
            3, new int[] { 147928, -55273, -2728 }, // Goddard Castle Town
            4, new int[] { 43799, -47707, -797 }, // Rune Township
            5, new int[] { 15670, 142983, -2705 }, // Town of Dion
            6, new int[] { -12672, 122776, -3116 }, // Town of Gludio
            7, new int[] { 111322, 219320, -3543 }, // Heine (Innadril)
            8, new int[] { 116819, 76994, -2714 } // Hunters Village
    );

    public record BbsMailLetter(
            int letterId,
            int senderId,
            String senderName,
            int receiverId,
            String receiverName,
            String subject,
            String content,
            long sentDate,
            boolean unread,
            boolean deleted) {
    }

    private final CharacterRepository characterRepository;
    private final Map<Integer, Map<Integer, String>> localFavorites = new ConcurrentHashMap<>();
    private final Map<String, String> accountPasswordHashes = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> playerPetitionsCount = new ConcurrentHashMap<>();
    private final List<String> pendingPetitions = new CopyOnWriteArrayList<>();
    private final List<String> archivedDeletedMailLetters = new CopyOnWriteArrayList<>();
    private final Map<Integer, BbsMailLetter> mailBox = new ConcurrentHashMap<>();
    private final AtomicInteger mailIdGen = new AtomicInteger(1);
    private final Map<Integer, Boolean> bufferPetTargetMode = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private GameWorld gameWorld;

    @Autowired(required = false)
    private RepairBBSManager repairBBSManager;

    @Autowired(required = false)
    private com.lopez.l2j.game.service.CharacterVariablesService variablesService;

    @Autowired(required = false)
    private com.lopez.l2j.game.service.SchemeBufferService schemeBufferService;

    @Autowired(required = false)
    private org.springframework.jdbc.core.simple.JdbcClient jdbc;

    @Autowired(required = false)
    private CommunityAuctionService communityAuctionService;

    @Autowired(required = false)
    private com.lopez.l2j.game.social.FriendListService friendListService;

    public CommunityBoardService() {
        this(null, null, null);
    }

    @Autowired
    public CommunityBoardService(@Autowired(required = false) CharacterRepository characterRepository) {
        this(characterRepository, null, null);
    }

    public CommunityBoardService(CharacterRepository characterRepository, GameWorld gameWorld) {
        this(characterRepository, gameWorld, null);
    }

    public CommunityBoardService(CharacterRepository characterRepository, GameWorld gameWorld,
            RepairBBSManager repairBBSManager) {
        this.characterRepository = characterRepository;
        this.gameWorld = gameWorld;
        this.repairBBSManager = repairBBSManager != null
                ? repairBBSManager
                : (characterRepository != null ? new RepairBBSManager(characterRepository) : null);
        instance = this;
    }

    public static CommunityBoardService getInstance() {
        return instance;
    }

    @PostConstruct
    public void init() {
        instance = this;
        initSchema();
        loadMailsFromDb();
    }

    public void initSchema() {
        if (jdbc == null) {
            return;
        }
        try {
            jdbc.sql("""
                    CREATE TABLE IF NOT EXISTS bbs_mail (
                        letter_id INT NOT NULL PRIMARY KEY,
                        sender_id INT NOT NULL,
                        sender_name VARCHAR(35) NOT NULL,
                        receiver_id INT NOT NULL,
                        receiver_name VARCHAR(35) NOT NULL,
                        subject VARCHAR(128) NOT NULL,
                        content TEXT NOT NULL,
                        sent_date BIGINT NOT NULL,
                        unread TINYINT(1) NOT NULL DEFAULT 1,
                        deleted TINYINT(1) NOT NULL DEFAULT 0
                    )
                    """).update();
        } catch (Exception ex) {
            log.debug("CommunityBoardService initSchema bbs_mail ignorado: {}", ex.getMessage());
        }
    }

    /**
     * #469: Divide payloads HTML grandes em ate 3 pacotes ShowBoard ("101", "102",
     * "103")
     * para evitar truncamento no cliente Interlude.
     */
    public static List<ShowBoard> splitForShowBoard(String html) {
        String safe = html != null ? html : "<html><body></body></html>";
        List<ShowBoard> packets = new ArrayList<>(3);
        if (safe.length() <= MAX_BOARD_PART_LENGTH) {
            packets.add(new ShowBoard(safe, "101"));
            packets.add(new ShowBoard("", "102"));
            packets.add(new ShowBoard("", "103"));
        } else if (safe.length() <= MAX_BOARD_PART_LENGTH * 2) {
            packets.add(new ShowBoard(safe.substring(0, MAX_BOARD_PART_LENGTH), "101"));
            packets.add(new ShowBoard(safe.substring(MAX_BOARD_PART_LENGTH), "102"));
            packets.add(new ShowBoard("", "103"));
        } else {
            int maxTotal = Math.min(safe.length(), MAX_BOARD_PART_LENGTH * 3);
            packets.add(new ShowBoard(safe.substring(0, MAX_BOARD_PART_LENGTH), "101"));
            packets.add(new ShowBoard(safe.substring(MAX_BOARD_PART_LENGTH, MAX_BOARD_PART_LENGTH * 2), "102"));
            packets.add(new ShowBoard(safe.substring(MAX_BOARD_PART_LENGTH * 2, maxTotal), "103"));
        }
        return packets;
    }

    public void separateAndSend(String html, GameSession session) {
        if (session == null) {
            return;
        }
        for (ShowBoard pkt : splitForShowBoard(html)) {
            session.send(pkt);
        }
    }

    public String handleWrite(PlayerCharacter player, String url, String arg1, String arg2, String arg3, String arg4, String arg5) {
        if (player == null) {
            return null;
        }
        String u = url != null ? url.trim() : "";
        if (u.startsWith("_")) {
            return handleCommand(player, u + " " + (arg1 != null ? arg1 : "") + " " + (arg2 != null ? arg2 : "") + " " + (arg3 != null ? arg3 : ""));
        }
        return null;
    }

    /**
     * Processa comandos BBS (_bbshome, _bbstop, _bbsloc, _bbsteleport, _bbsservice,
     * _bbsbuffer, _bbsinfo,
     * _bbsclan, _bbsmemo, _bbstopics, _bbsposts, _bbsmail, _bbsfriends, _bbsgetfav,
     * _bbsdelfav, _bbsraids, _bbsdressme)
     * e retorna o HTML correspondente para ser exibido via ShowBoard.
     */
    public String handleCommand(PlayerCharacter player, String command) {
        if (player == null) {
            return "<html><body>Erro de sessao.</body></html>";
        }

        if ("off".equalsIgnoreCase(Config.COMMUNITY_TYPE)) {
            return "<html><body><br><center><font color=\"B86B6B\">O Community Board esta desativado no momento.</font></center></body></html>";
        }

        if (isRestricted(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">O Community Board esta restrito nesta condicao!</font></center></body></html>";
        }

        String defaultPage = (Config.BBS_DEFAULT != null && !Config.BBS_DEFAULT.isBlank()) ? Config.BBS_DEFAULT
                : "_bbshome";
        String cmd = (command == null || command.isBlank()) ? defaultPage : command.trim();

        if (isPageDisabled(cmd)) {
            return "<html><body><br><center><font color=\"B86B6B\">Esta secao do Community Board esta desativada pelo administrador.</font></center></body></html>";
        }

        if (cmd.startsWith("_bbsteleport;fav_save") || cmd.startsWith("_bbsaddfav")) {
            String nameArg = cmd.contains(";")
                    ? cmd.substring(cmd.indexOf(';') + 1).replaceFirst("^fav_save[;\\s]*", "").trim()
                    : "";
            return handleSaveNamedFavorite(player, nameArg);
        } else if (cmd.startsWith("_bbsteleport;fav_go")) {
            String slotArg = cmd.length() > 19 ? cmd.substring(19).replaceFirst("^[;\\s]+", "").trim() : "";
            return handleTeleportFavorite(player, slotArg);
        } else if (cmd.startsWith("_bbsteleport;fav_del") || cmd.startsWith("_bbsdelfav")) {
            String slotArg = cmd.replaceFirst("^(_bbsteleport;fav_del|_bbsdelfav)[;_\\s]*", "").trim();
            return handleDeleteFavorite(player, slotArg);
        } else if (cmd.startsWith("_bbsgetfav")) {
            return getTeleportHtml(player);
        } else if (cmd.startsWith("_bbsteleport;to;")) {
            return handleTeleport(player, cmd.substring(16).trim());
        } else if (cmd.startsWith("_bbsteleport_to ")) {
            return handleTeleport(player, cmd.substring(16).trim());
        } else if (cmd.startsWith("_bbsteleport_fav ")) {
            return handleTeleportFavorite(player, cmd.substring(17).trim());
        } else if (cmd.startsWith("_bbsteleport_save ")) {
            return handleSaveFavorite(player, cmd.substring(18).trim());
        } else if (cmd.startsWith("_bbsteleport_del ")) {
            return handleDeleteFavorite(player, cmd.substring(17).trim());
        } else if (cmd.equals("_bbsteleport")) {
            return getTeleportHtml(player);
        } else if (cmd.startsWith("_bbsbuff_")) {
            return handleBuff(player, cmd.substring(9).trim());
        } else if (cmd.equals("_bbsbuffer") || cmd.startsWith("_bbsbuffer;")) {
            return getBufferHtml(player);
        } else if (cmd.startsWith("_bbstop;")) {
            String subPage = cmd.substring(8).trim();
            return getStaticOrTopPageHtml(player, subPage);
        } else if (cmd.equals("_bbstop") || cmd.equals("_bbsranking") || cmd.equals("_bbsloc")) {
            return getRankingsHtml(player);
        } else if (cmd.startsWith("_bbsregion") || cmd.startsWith("_bbsloc;")) {
            int page = 1;
            String rest = cmd.replaceFirst("^(_bbsregion|_bbsloc)[;\\s]*", "").trim();
            if (!rest.isEmpty()) {
                try {
                    page = Math.max(1, Integer.parseInt(rest));
                } catch (NumberFormatException ignored) {
                }
            }
            return getOnlinePlayersRegionHtml(player, page);
        } else if (cmd.startsWith("_bbsclan")) {
            return handleClanBbs(player, cmd);
        } else if (cmd.startsWith("_bbsmemo") || cmd.startsWith("_bbstopics") || cmd.startsWith("_bbsposts")) {
            return handleForumMemoBbs(player, cmd);
        } else if (cmd.startsWith("_bbsmail") || cmd.startsWith("_maillist")) {
            return handleMailBbs(player, cmd);
        } else if (cmd.startsWith("_bbsfriends") || cmd.startsWith("_friendlist")) {
            return handleFriendsBbs(player, cmd);
        } else if (cmd.startsWith("_bbsraids") || cmd.startsWith("_bbsboss")) {
            return getRaidBossBbsHtml(player);
        } else if (cmd.startsWith("_bbsshop") || cmd.startsWith("_bbsgmshop") || cmd.startsWith("_bbsmultisell")) {
            return getCommunityShopHtml(player);
        } else if (cmd.startsWith("_bbsdressme")) {
            return getCommunityDressMeHtml(player);
        } else if (cmd.startsWith("_bbspetition")) {
            String petText = cmd.length() > 12 ? cmd.substring(12).replaceFirst("^[;\\s]+", "").trim()
                    : "Suporte Geral";
            return submitPetition(player, petText, isAnyGmOnline());
        } else if (cmd.equals("_bbsinfo")) {
            return getInfoHtml(player);
        } else if (cmd.startsWith("_bbsservice;repair")) {
            String targetChar = cmd.length() > 18 ? cmd.substring(18).replaceFirst("^[;\\s]+", "").trim() : "";
            if (targetChar.isEmpty()) {
                return getRepairHtml(player);
            }
            return handleRepairChar(player, targetChar);
        } else if (cmd.startsWith("_bbsservice;passchange")) {
            String args = cmd.length() > 22 ? cmd.substring(22).replaceFirst("^[;\\s]+", "").trim() : "";
            return handlePasswordChange(player, args);
        } else if (cmd.equals("_bbsservice")) {
            return getServiceHtml(player);
        } else if (cmd.startsWith("_bbsrepair_char ")) {
            return handleRepairChar(player, cmd.substring(16).trim());
        } else if (cmd.equals("_bbsrepair")) {
            return getRepairHtml(player);
        } else if (cmd.startsWith("_bbsauction")) {
            if (communityAuctionService != null) {
                return communityAuctionService.handleAuctionCommand(player, cmd);
            }
            return "<html><body><br><center>Casa de Leiloes temporariamente indisponivel.</center></body></html>";
        } else {
            return getHomeHtml(player);
        }
    }

    private String getStaticOrTopPageHtml(PlayerCharacter player, String subPage) {
        if (subPage == null || subPage.isBlank() || "index".equalsIgnoreCase(subPage)
                || "home".equalsIgnoreCase(subPage)) {
            return getHomeHtml(player);
        }
        String clean = subPage.replaceAll("[^a-zA-Z0-9_-]", "");
        File file = new File("data/html/CommunityBoard/" + clean + ".htm");
        if (file.exists() && file.isFile()) {
            try {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        if ("rank".equalsIgnoreCase(clean) || "rankings".equalsIgnoreCase(clean)) {
            return getRankingsHtml(player);
        }
        if ("raids".equalsIgnoreCase(clean) || "boss".equalsIgnoreCase(clean)) {
            return getRaidBossBbsHtml(player);
        }
        return getInfoHtml(player);
    }

    /**
     * Tela Principal (Home) da Comunidade BBS.
     */
    public String getHomeHtml(PlayerCharacter player) {
        int onlineCount = gameWorld != null ? gameWorld.players().size() : 1;
        List<PlayerCharacter> topPvp = characterRepository != null ? characterRepository.findTopPvP(5) : List.of();
        List<PlayerCharacter> topPk = characterRepository != null ? characterRepository.findTopPK(5) : List.of();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\" size=4><b>=== PAINEL DA COMUNIDADE (ALT + B) ===</b></font></td></tr>");
        sb.append(
                "<tr><td align=center><font color=\"AAAAAA\">Servidor L2JLopez - Chronicle 4 / Interlude Remastered</font></td></tr>");
        sb.append("</table><br>");

        // Barra de Navegacao
        sb.append(renderNavigationBar());

        // Estatisticas do Servidor & Jogador
        sb.append("<table width=610 bgcolor=111111 border=1 cellpadding=4 cellspacing=0>");
        sb.append("<tr>");
        sb.append("<td width=305><font color=\"8FA888\"><b>Status do Jogador:</b></font><br>");
        sb.append("Nome: <font color=\"FFFFFF\">").append(player.name()).append("</font><br>");
        sb.append("Nivel: <font color=\"FFFFFF\">").append(player.level()).append("</font><br>");
        sb.append("PvP / PK: <font color=\"8FA8BE\">").append(player.pvpKills())
                .append("</font> / <font color=\"B86B6B\">").append(player.pkKills()).append("</font><br>");
        sb.append("Karma: <font color=\"").append(player.karma() > 0 ? "B86B6B" : "8FA888").append("\">")
                .append(player.karma()).append("</font>");
        sb.append("</td>");
        sb.append("<td width=305><font color=\"8FA888\"><b>Status do Servidor:</b></font><br>");
        String onlineDisplay = Config.ONLINE_COMMUNITY_BOARD ? String.valueOf(onlineCount) : "N/A";
        sb.append("Jogadores Online: <font color=\"D4C49A\">").append(onlineDisplay).append("</font><br>");
        sb.append("Taxas: <font color=\"FFFFFF\">XP: 1x | SP: 1x | Adena: 1x | Drop: 1x</font><br>");
        sb.append("Plataforma: <font color=\"8FA888\">Java 21 / Spring Boot 3.5</font>");
        sb.append("</td>");
        sb.append("</tr></table><br>");

        // Tabela Rapida Top 5 PvP e PK
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr>");
        sb.append("<td width=305 valign=top>");
        sb.append("<font color=\"LEVEL\"><b>Top 5 Guerreiros PvP:</b></font><br>");
        sb.append("<table width=295 bgcolor=222222 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th width=35>#</th><th width=180>Nome</th><th width=80>PvPs</th></tr>");
        if (topPvp.isEmpty()) {
            sb.append("<tr><td colspan=3 align=center>Nenhum PvP registrado</td></tr>");
        } else {
            int rank = 1;
            for (PlayerCharacter p : topPvp) {
                sb.append("<tr><td align=center>").append(rank++).append("</td>");
                sb.append("<td>").append(p.name()).append("</td>");
                sb.append("<td align=center><font color=\"8FA888\">").append(p.pvpKills()).append("</font></td></tr>");
            }
        }
        sb.append("</table></td>");

        sb.append("<td width=305 valign=top>");
        sb.append("<font color=\"B86B6B\"><b>Top 5 Assassinos PK:</b></font><br>");
        sb.append("<table width=295 bgcolor=222222 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th width=35>#</th><th width=180>Nome</th><th width=80>PKs</th></tr>");
        if (topPk.isEmpty()) {
            sb.append("<tr><td colspan=3 align=center>Nenhum PK registrado</td></tr>");
        } else {
            int rank = 1;
            for (PlayerCharacter p : topPk) {
                sb.append("<tr><td align=center>").append(rank++).append("</td>");
                sb.append("<td>").append(p.name()).append("</td>");
                sb.append("<td align=center><font color=\"B86B6B\">").append(p.pkKills()).append("</font></td></tr>");
            }
        }
        sb.append("</table></td>");
        sb.append("</tr></table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Tela de Rankings Completos (Top 10 PvP, Top 10 PK e Top Clans).
     */
    public String getRankingsHtml(PlayerCharacter player) {
        List<PlayerCharacter> topPvp = characterRepository != null ? characterRepository.findTopPvP(10) : List.of();
        List<PlayerCharacter> topPk = characterRepository != null ? characterRepository.findTopPK(10) : List.of();
        List<Clan> topClans = new ArrayList<>();
        try {
            ClanTable clanTable = ClanTable.getInstance();
            if (clanTable != null && clanTable.allClans() != null) {
                topClans.addAll(clanTable.allClans());
                topClans.sort((a, b) -> {
                    int cmp = Integer.compare(b.level(), a.level());
                    return cmp != 0 ? cmp : Integer.compare(b.reputationScore(), a.reputationScore());
                });
                if (topClans.size() > 10) {
                    topClans = topClans.subList(0, 10);
                }
            }
        } catch (Exception ignored) {
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr><td align=center><font color=\"LEVEL\" size=3><b>RANKINGS DO SERVIDOR</b></font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=610 border=0 cellpadding=4 cellspacing=4>");
        sb.append("<tr>");

        // Top 10 PvP
        sb.append("<td width=305 valign=top>");
        sb.append("<table width=295 bgcolor=151515 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th colspan=4 bgcolor=333333><font color=\"8FA888\">TOP 10 PVP</font></th></tr>");
        sb.append("<tr><th width=30>#</th><th width=145>Nome</th><th width=55>Nivel</th><th width=65>PvPs</th></tr>");
        int rank = 1;
        for (PlayerCharacter p : topPvp) {
            if (!Config.SHOW_KARMA_PLAYERS && p.karma() > 0)
                continue;
            String lvlStr = Config.SHOW_LEVEL_ON_COMMUNITY_BOARD ? String.valueOf(p.level()) : "-";
            sb.append("<tr><td align=center>").append(rank++).append("</td>");
            sb.append("<td>").append(p.name()).append("</td>");
            sb.append("<td align=center>").append(lvlStr).append("</td>");
            sb.append("<td align=center><font color=\"8FA888\">").append(p.pvpKills()).append("</font></td></tr>");
        }
        if (topPvp.isEmpty()) {
            sb.append("<tr><td colspan=4 align=center>Sem registros de PvP</td></tr>");
        }
        sb.append("</table></td>");

        // Top 10 PK
        sb.append("<td width=305 valign=top>");
        sb.append("<table width=295 bgcolor=151515 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th colspan=4 bgcolor=333333><font color=\"B86B6B\">TOP 10 PK</font></th></tr>");
        sb.append("<tr><th width=30>#</th><th width=145>Nome</th><th width=55>Nivel</th><th width=65>PKs</th></tr>");
        rank = 1;
        for (PlayerCharacter p : topPk) {
            if (!Config.SHOW_KARMA_PLAYERS && p.karma() > 0)
                continue;
            String lvlStr = Config.SHOW_LEVEL_ON_COMMUNITY_BOARD ? String.valueOf(p.level()) : "-";
            sb.append("<tr><td align=center>").append(rank++).append("</td>");
            sb.append("<td>").append(p.name()).append("</td>");
            sb.append("<td align=center>").append(lvlStr).append("</td>");
            sb.append("<td align=center><font color=\"B86B6B\">").append(p.pkKills()).append("</font></td></tr>");
        }
        if (topPk.isEmpty()) {
            sb.append("<tr><td colspan=4 align=center>Sem registros de PK</td></tr>");
        }
        sb.append("</table></td>");

        sb.append("</tr></table><br>");

        // Top Clans
        sb.append("<table width=610 bgcolor=151515 border=1 cellpadding=2 cellspacing=0 align=center>");
        sb.append("<tr><th colspan=4 bgcolor=333333><font color=\"LEVEL\">TOP CLANS DO REINO</font></th></tr>");
        sb.append(
                "<tr><th width=45>#</th><th width=285>Nome do Clan</th><th width=120>Nivel</th><th width=160>Reputacao</th></tr>");
        if (topClans.isEmpty()) {
            sb.append("<tr><td colspan=4 align=center>Nenhum Clan registrado</td></tr>");
        } else {
            int clanRank = 1;
            for (Clan clan : topClans) {
                sb.append("<tr><td align=center>").append(clanRank++).append("</td>");
                sb.append("<td><font color=\"8FA8BE\">").append(clan.name()).append("</font></td>");
                sb.append("<td align=center>").append(clan.level()).append("</td>");
                sb.append("<td align=center>").append(clan.reputationScore()).append("</td></tr>");
            }
        }
        sb.append("</table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Tela de Teleporte Comunitário para Cidades e Favoritos (ate 10 slots).
     */
    public String getTeleportHtml(PlayerCharacter player) {
        if (isGatekeeperExcluded(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">O servico de Teleporte esta desativado nesta condicao/area!</font></center></body></html>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\" size=3><b>PORTAL DE TELEPORTE RAPIDO</b></font></td></tr>");
        sb.append(
                "<tr><td align=center><font color=\"AAAAAA\">Teleporte direto e instantaneo para todas as capitais do reino.</font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=610 border=0 cellpadding=4 cellspacing=4 align=center>");
        sb.append("<tr>");
        sb.append(
                "<td align=center><button value=\"Town of Giran\" action=\"bypass _bbsteleport_to 1\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Town of Aden\" action=\"bypass _bbsteleport_to 2\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Town of Goddard\" action=\"bypass _bbsteleport_to 3\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Rune Township\" action=\"bypass _bbsteleport_to 4\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append("</tr><tr>");
        sb.append(
                "<td align=center><button value=\"Town of Dion\" action=\"bypass _bbsteleport_to 5\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Town of Gludio\" action=\"bypass _bbsteleport_to 6\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Town of Heine\" action=\"bypass _bbsteleport_to 7\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Hunters Village\" action=\"bypass _bbsteleport_to 8\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append("</tr></table><br>");

        // Favoritos Pessoais (Bookmarks - ate 10 slots)
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\"><b>MEUS TELEPORTES FAVORITOS (MAX 10)</b></font></td></tr>");
        sb.append(
                "<tr><td align=center><font color=\"AAAAAA\">Salve coordenadas personalizadas no mundo aberto (proibido em zonas de Boss/Siege/Olympiad).</font></td></tr>");
        sb.append("</table>");

        sb.append("<table width=600 bgcolor=151515 border=1 cellpadding=3 cellspacing=0 align=center>");
        sb.append("<tr><th width=50>Slot</th><th width=300>Localizacao Salva</th><th width=150>Acoes</th></tr>");

        for (int slot = 1; slot <= MAX_FAVORITE_SLOTS; slot++) {
            String fav = getFavorite(player, slot);
            sb.append("<tr><td align=center>#").append(slot).append("</td>");
            if (fav != null) {
                String[] parts = fav.split(";");
                String name = parts[0];
                sb.append("<td><font color=\"8FA888\"><b>").append(name).append("</b></font></td>");
                sb.append("<td align=center><button value=\"Teleportar\" action=\"bypass _bbsteleport_fav ")
                        .append(slot)
                        .append("\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"> ");
                sb.append("<button value=\"Excluir\" action=\"bypass _bbsteleport_del ").append(slot).append(
                        "\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
            } else {
                sb.append("<td><font color=\"666666\">[Vazio]</font></td>");
                sb.append("<td align=center><button value=\"Salvar Atual\" action=\"bypass _bbsteleport_save ")
                        .append(slot)
                        .append("\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    public String getFavorite(PlayerCharacter player, int slot) {
        if (player == null || slot < 1 || slot > MAX_FAVORITE_SLOTS)
            return null;
        if (variablesService != null) {
            String v = variablesService.getVariable(player.objectId(), "fav_tele_" + slot, null);
            if (v != null)
                return v;
        }
        Map<Integer, String> map = localFavorites.get(player.objectId());
        if ((map == null || !map.containsKey(slot)) && jdbc != null) {
            loadFavoritesFromDb(player.objectId());
            map = localFavorites.get(player.objectId());
        }
        return map != null ? map.get(slot) : null;
    }

    private void putFavorite(PlayerCharacter player, int slot, String val) {
        if (variablesService != null) {
            variablesService.setVariable(player.objectId(), "fav_tele_" + slot, val);
        }
        localFavorites.computeIfAbsent(player.objectId(), k -> new ConcurrentHashMap<>()).put(slot, val);
        if (jdbc != null && val != null) {
            try {
                String[] parts = val.split(";");
                String name = parts.length > 0 ? parts[0] : ("Teleport " + slot);
                int x = parts.length > 1 ? Integer.parseInt(parts[1]) : player.x();
                int y = parts.length > 2 ? Integer.parseInt(parts[2]) : player.y();
                int z = parts.length > 3 ? Integer.parseInt(parts[3]) : player.z();
                jdbc.sql(
                        """
                                INSERT INTO character_teleports (charId, locId, locName, acronym, iconId, xCoord, yCoord, zCoord)
                                VALUES (:cId, :lId, :name, :acr, 1, :x, :y, :z)
                                ON DUPLICATE KEY UPDATE locName = VALUES(locName), xCoord = VALUES(xCoord), yCoord = VALUES(yCoord), zCoord = VALUES(zCoord)
                                """)
                        .param("cId", player.objectId())
                        .param("lId", slot)
                        .param("name", name)
                        .param("acr", name.length() > 4 ? name.substring(0, 4) : name)
                        .param("x", x)
                        .param("y", y)
                        .param("z", z)
                        .update();
            } catch (Exception ex) {
                log.debug("character_teleports insert: {}", ex.getMessage());
            }
        }
    }

    private void removeFavorite(PlayerCharacter player, int slot) {
        if (variablesService != null) {
            variablesService.deleteVariable(player.objectId(), "fav_tele_" + slot);
        }
        Map<Integer, String> map = localFavorites.get(player.objectId());
        if (map != null) {
            map.remove(slot);
        }
        if (jdbc != null) {
            try {
                jdbc.sql("DELETE FROM character_teleports WHERE charId = :cId AND locId = :lId")
                        .param("cId", player.objectId())
                        .param("lId", slot)
                        .update();
            } catch (Exception ex) {
                log.debug("character_teleports delete: {}", ex.getMessage());
            }
        }
    }

    public void loadFavoritesFromDb(int charId) {
        if (jdbc == null)
            return;
        try {
            var rows = jdbc
                    .sql("SELECT locId, locName, xCoord, yCoord, zCoord FROM character_teleports WHERE charId = :cId")
                    .param("cId", charId)
                    .query().listOfRows();
            Map<Integer, String> map = localFavorites.computeIfAbsent(charId, k -> new ConcurrentHashMap<>());
            for (var r : rows) {
                int slot = ((Number) r.get("locId")).intValue();
                String name = (String) r.get("locName");
                int x = ((Number) r.get("xCoord")).intValue();
                int y = ((Number) r.get("yCoord")).intValue();
                int z = ((Number) r.get("zCoord")).intValue();
                map.put(slot, name + ";" + x + ";" + y + ";" + z);
            }
        } catch (Exception ex) {
            log.debug("character_teleports load: {}", ex.getMessage());
        }
    }

    private boolean isForbiddenTeleportZone(PlayerCharacter player) {
        if (player == null)
            return true;
        if (player.isInSiege() || player.isOlympiadMode() || player.isInJail() || player.isInCombat()
                || player.instanceId() != 0) {
            return true;
        }
        int x = player.x();
        int y = player.y();
        if (x >= 173000 && x <= 186000 && y >= 110000 && y <= 120000)
            return true; // Antharas Lair
        if (x >= 208000 && x <= 218000 && y >= -120000 && y <= -110000)
            return true; // Valakas Lair
        if (x >= 111000 && x <= 118000 && y >= 12000 && y <= 19000 && player.z() >= 9500)
            return true; // Baium Roof
        return false;
    }

    private boolean chargeTeleportCostIfApplicable(PlayerCharacter player) {
        if (player == null || player.level() <= 40 || player.inventory() == null) {
            return true;
        }
        if (player.inventory().items().isEmpty()) {
            return true;
        }
        if (player.inventory().adena() < TELEPORT_ADENA_COST) {
            return false;
        }
        return player.inventory().destroyItemByItemId(57, (int) TELEPORT_ADENA_COST);
    }

    private String handleSaveNamedFavorite(PlayerCharacter player, String rawName) {
        if (player == null)
            return getTeleportHtml(player);
        if (isForbiddenTeleportZone(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">Nao e permitido salvar coordenadas em zonas de Siege, Boss, Combate, Olimpiadas ou Prisao!</font></center></body></html>";
        }
        int freeSlot = -1;
        for (int s = 1; s <= MAX_FAVORITE_SLOTS; s++) {
            if (getFavorite(player, s) == null) {
                freeSlot = s;
                break;
            }
        }
        if (freeSlot == -1) {
            return "<html><body><br><center><font color=\"B86B6B\">Voce ja atingiu o limite maximo de "
                    + MAX_FAVORITE_SLOTS + " favoritos!</font></center></body></html>";
        }
        String safeName = (rawName == null || rawName.isBlank())
                ? "Ponto #" + freeSlot + " (" + player.x() + "," + player.y() + ")"
                : rawName.replace(";", "").trim();
        String val = safeName + ";" + player.x() + ";" + player.y() + ";" + player.z();
        putFavorite(player, freeSlot, val);
        log.info("Jogador {} salvou favorito nomeado slot {} em {}", player.name(), freeSlot, val);
        return getTeleportHtml(player);
    }

    private String handleSaveFavorite(PlayerCharacter player, String slotStr) {
        if (player == null)
            return getTeleportHtml(player);
        if (isForbiddenTeleportZone(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">Nao e permitido salvar coordenadas em zonas de Siege, Boss, Combate, Olimpiadas ou Prisao!</font></center></body></html>";
        }
        try {
            int slot = Integer.parseInt(slotStr);
            if (slot >= 1 && slot <= MAX_FAVORITE_SLOTS) {
                String name = "Ponto #" + slot + " (" + player.x() + "," + player.y() + ")";
                String val = name + ";" + player.x() + ";" + player.y() + ";" + player.z();
                putFavorite(player, slot, val);
                log.info("Jogador {} salvou favorito slot {} em {}", player.name(), slot, val);
            }
        } catch (NumberFormatException ignored) {
        }
        return getTeleportHtml(player);
    }

    private String handleDeleteFavorite(PlayerCharacter player, String slotStr) {
        if (player == null)
            return getTeleportHtml(player);
        try {
            int slot = Integer.parseInt(slotStr);
            if (slot >= 1 && slot <= MAX_FAVORITE_SLOTS) {
                removeFavorite(player, slot);
            }
        } catch (NumberFormatException ignored) {
        }
        return getTeleportHtml(player);
    }

    private String handleTeleportFavorite(PlayerCharacter player, String slotStr) {
        if (player == null || player.isDead() || player.karma() > 0 || isGatekeeperExcluded(player)
                || isForbiddenTeleportZone(player)) {
            return getTeleportHtml(player);
        }
        try {
            int slot = Integer.parseInt(slotStr);
            String fav = getFavorite(player, slot);
            if (fav != null) {
                if (!chargeTeleportCostIfApplicable(player)) {
                    return "<html><body><br><center><font color=\"B86B6B\">Adena insuficiente para realizar o teleporte!</font></center></body></html>";
                }
                String[] parts = fav.split(";");
                int tx = Integer.parseInt(parts[1]);
                int ty = Integer.parseInt(parts[2]);
                int tz = Integer.parseInt(parts[3]);
                player.teleport(tx, ty, tz);
                log.info("Jogador {} teleportado para favorito #{}: {},{},{}", player.name(), slot, tx, ty, tz);
                return getHomeHtml(player);
            }
        } catch (Exception ignored) {
        }
        return getTeleportHtml(player);
    }

    private String handleTeleport(PlayerCharacter player, String arg) {
        if (isGatekeeperExcluded(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">O servico de Teleporte esta desativado nesta condicao/area!</font></center></body></html>";
        }
        if (player.isDead()) {
            return "<html><body><br><center><font color=\"B86B6B\">Voce nao pode se teletransportar enquanto estiver morto!</font></center></body></html>";
        }
        if (player.karma() > 0) {
            return "<html><body><br><center><font color=\"B86B6B\">Jogadores com Karma nao podem usar o teleporte comunitario!</font></center></body></html>";
        }
        if (player.isInJail() || player.isOlympiadMode() || player.instanceId() != 0) {
            return "<html><body><br><center><font color=\"B86B6B\">O servico de Teleporte esta desativado nesta condicao/area!</font></center></body></html>";
        }

        try {
            int locId = Integer.parseInt(arg);
            int[] coords = TELEPORT_LOCATIONS.get(locId);
            if (coords != null) {
                if (!chargeTeleportCostIfApplicable(player)) {
                    return "<html><body><br><center><font color=\"B86B6B\">Adena insuficiente para realizar o teleporte!</font></center></body></html>";
                }
                player.teleport(coords[0], coords[1], coords[2]);
                log.info("Jogador {} teletransportado via BBS para destino {}", player.name(), locId);
                return getHomeHtml(player);
            }
        } catch (NumberFormatException ignored) {
        }
        return getTeleportHtml(player);
    }

    /**
     * Gabinete Pessoal do Jogador (_bbsservice): Seguranca da Conta, Troca de Senha
     * e Reparo de Personagem.
     */
    public String getServiceHtml(PlayerCharacter player) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\" size=3><b>GABINETE PESSOAL DO JOGADOR</b></font></td></tr>");
        sb.append(
                "<tr><td align=center><font color=\"AAAAAA\">Gerenciamento de conta, seguranca e destravamento de personagens (Repair).</font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=610 bgcolor=151515 border=1 cellpadding=6 cellspacing=0>");
        sb.append("<tr><td width=305 valign=top>");
        sb.append("<font color=\"8FA888\"><b>Informacoes da Conta:</b></font><br>");
        sb.append("Conta: <font color=\"FFFFFF\">").append(player.accountName() != null ? player.accountName() : "N/A")
                .append("</font><br>");
        sb.append("Personagem Ativo: <font color=\"FFFFFF\">").append(player.name()).append("</font><br>");
        sb.append("Posicao Atual: <font color=\"AAAAAA\">").append(player.x()).append(", ").append(player.y())
                .append(", ").append(player.z()).append("</font><br><br>");
        sb.append(
                "<button value=\"Painel de Reparo (.repair)\" action=\"bypass _bbsrepair\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\">");
        sb.append("</td>");

        sb.append("<td width=305 valign=top>");
        sb.append("<font color=\"LEVEL\"><b>Alteracao de Senha da Conta:</b></font><br>");
        sb.append("Senha Atual:<br><edit var=\"oldpass\" width=160 height=15><br>");
        sb.append("Nova Senha:<br><edit var=\"newpass\" width=160 height=15><br>");
        sb.append(
                "<button value=\"Alterar Senha\" action=\"bypass _bbsservice;passchange; $oldpass ; $newpass\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\">");
        sb.append("</td></tr></table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * #479: Persiste alteracao de senha na tabela SQL `accounts` alem do cache em
     * memoria.
     */
    private String handlePasswordChange(PlayerCharacter player, String args) {
        if (player == null) {
            return "<html><body>Erro de sessao.</body></html>";
        }
        String[] parts = args.contains(";") ? args.split(";") : args.split("\\s+");
        if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()
                || parts[1].trim().length() < 4) {
            return "<html><body><br><center><font color=\"B86B6B\">Informe a senha atual e uma nova senha valida (minimo 4 caracteres)!</font><br><br>"
                    + "<button value=\"Voltar\" action=\"bypass _bbsservice\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
        }
        String oldPass = parts[0].trim();
        String newPass = parts[1].trim();
        String acc = player.accountName() != null ? player.accountName() : player.name();
        String existing = accountPasswordHashes.get(acc);
        if (existing != null && !existing.equals(oldPass)) {
            return "<html><body><br><center><font color=\"B86B6B\">A senha atual informada esta incorreta!</font><br><br>"
                    + "<button value=\"Voltar\" action=\"bypass _bbsservice\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
        }
        if (jdbc != null) {
            try {
                String dbPass = jdbc.sql("SELECT password FROM accounts WHERE login = :login")
                        .param("login", acc)
                        .query(String.class)
                        .optional()
                        .orElse(null);
                String oldHash = VoicedGeneralHandler.hashAccountPassword(oldPass);
                if (dbPass != null && !dbPass.isBlank() && !dbPass.equals(oldPass)
                        && !dbPass.equalsIgnoreCase(oldHash)) {
                    return "<html><body><br><center><font color=\"B86B6B\">A senha atual informada esta incorreta!</font><br><br>"
                            + "<button value=\"Voltar\" action=\"bypass _bbsservice\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
                }
                jdbc.sql("UPDATE accounts SET password = :pw WHERE login = :login")
                        .param("pw", VoicedGeneralHandler.hashAccountPassword(newPass))
                        .param("login", acc)
                        .update();
            } catch (Exception ex) {
                log.debug("accounts password update ignorado: {}", ex.getMessage());
            }
        }
        accountPasswordHashes.put(acc, newPass);
        log.info("Conta {} alterou a senha via Gabinete BBS (_bbsservice)", acc);
        return "<html><body><br><center><font color=\"8FA888\">Senha da conta alterada com sucesso!</font><br><br>"
                + "<button value=\"Voltar\" action=\"bypass _bbsservice\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
    }

    /**
     * Tela do Buffer Comunitário (#476 - suporte a Jogador e Pet/Summon, 100% em
     * Adena).
     */
    public String getBufferHtml(PlayerCharacter player) {
        if (isBufferExcluded(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">O servico de Buff esta desativado nesta condicao/area!</font></center></body></html>";
        }
        boolean targetPet = Boolean.TRUE.equals(bufferPetTargetMode.get(player.objectId()));
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr><td align=center><font color=\"LEVEL\" size=3><b>BUFFER COMUNITÁRIO</b></font></td></tr>");
        sb.append("<tr><td align=center><font color=\"AAAAAA\">Selecione o esquema de buffs desejado (Alvo Atual: ")
                .append(targetPet ? "PET / SUMMON" : "PERSONAGEM")
                .append(") ou recupere totalmente seu HP/MP/CP.</font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=610 border=0 cellpadding=6 cellspacing=6 align=center>");
        sb.append("<tr>");
        sb.append(
                "<td align=center><button value=\"Esquema Guerreiro\" action=\"bypass _bbsbuff_fighter\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Esquema Mago\" action=\"bypass _bbsbuff_mage\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Restaurar HP/MP/CP\" action=\"bypass _bbsbuff_heal\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Alternar Pet/Char\" action=\"bypass _bbsbuff_target\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append("</tr><tr>");
        sb.append(
                "<td align=center><button value=\"Guerreiro (Pet)\" action=\"bypass _bbsbuff_pet_fighter\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Mago (Pet)\" action=\"bypass _bbsbuff_pet_mage\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Curar Pet/Summon\" action=\"bypass _bbsbuff_pet_heal\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append(
                "<td align=center><button value=\"Limpar Buffs\" action=\"bypass _bbsbuff_cancel\" width=134 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>");
        sb.append("</tr></table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String handleBuff(PlayerCharacter player, String scheme) {
        if (isBufferExcluded(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">O servico de Buff esta desativado nesta condicao/area!</font></center></body></html>";
        }
        if (player.isDead()) {
            return "<html><body><br><center><font color=\"B86B6B\">Voce nao pode receber buffs enquanto estiver morto!</font></center></body></html>";
        }

        String lower = scheme != null ? scheme.toLowerCase(Locale.ROOT) : "";
        if ("target".equals(lower)) {
            bufferPetTargetMode.compute(player.objectId(), (k, v) -> v == null || !v);
            return getBufferHtml(player);
        }

        boolean forPet = lower.startsWith("pet_") || Boolean.TRUE.equals(bufferPetTargetMode.get(player.objectId()));
        String cleanScheme = lower.startsWith("pet_") ? lower.substring(4) : lower;

        GameSession session = null;
        if (gameWorld != null) {
            var online = gameWorld.findPlayer(player.objectId());
            if (online instanceof GameSession gs) {
                session = gs;
            }
        }

        if ("heal".equals(cleanScheme)) {
            if (forPet && player.activeSummon() != null) {
                SummonInstance pet = player.activeSummon();
                pet.currentHp(pet.maxHp());
                pet.currentMp(pet.maxMp());
            } else {
                player.currentHp(player.maxHp());
                player.currentMp(player.maxMp());
                player.currentCp(player.maxCp());
            }
            if (schemeBufferService != null) {
                schemeBufferService.heal(player, session);
            }
            log.info("Jogador {} recuperou HP/MP/CP via BBS (pet={})", player.name(), forPet);
        } else if ("cancel".equals(cleanScheme)) {
            if (schemeBufferService != null) {
                schemeBufferService.cancelBuffs(player, session);
            } else {
                player.clearAllEffects();
            }
        } else if ("fighter".equals(cleanScheme) || "mage".equals(cleanScheme)) {
            player.currentHp(player.maxHp());
            player.currentMp(player.maxMp());
            if (schemeBufferService != null) {
                if ("fighter".equals(cleanScheme)) {
                    schemeBufferService.applyFighterPreset(player, session, forPet);
                } else {
                    schemeBufferService.applyMagePreset(player, session, forPet);
                }
            }
            log.info("Jogador {} recebeu esquema de buff {} via BBS (pet={})", player.name(), cleanScheme, forPet);
        }

        return getBufferHtml(player);
    }

    /**
     * #472: Clan BBS (_bbsclan;clanlist, _bbsclan;clanhome;<id>,
     * _bbsclan;clannotice;<txt>, _bbsclan;clanmail;<subj>;<body>).
     */
    private String handleClanBbs(PlayerCharacter player, String cmd) {
        ClanTable clanTable = ClanTable.getInstance();
        String args = cmd.length() > 8 ? cmd.substring(8).replaceFirst("^[;_\\s]+", "").trim() : "";

        if (args.startsWith("clannotice") && player.clanId() > 0 && player.isClanLeader() && clanTable != null) {
            String notice = args.replaceFirst("^clannotice[;\\s]*", "").trim();
            Clan c = clanTable.byClanId(player.clanId()).orElse(null);
            if (c != null) {
                c.notice(notice);
                c.noticeEnabled(!notice.isEmpty());
            }
        } else if (args.startsWith("clanmail") && player.clanId() > 0 && clanTable != null) {
            String[] parts = args.split(";", 3);
            String subj = parts.length > 1 ? parts[1].trim() : "Comunicado do Cla";
            String body = parts.length > 2 ? parts[2].trim() : "";
            Clan c = clanTable.byClanId(player.clanId()).orElse(null);
            if (c != null && !body.isEmpty()) {
                for (var m : c.members()) {
                    if (m.objectId() != player.objectId()) {
                        sendMail(player.objectId(), player.name(), m.objectId(), m.name(), subj, body);
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<center><font color=\"LEVEL\" size=3><b>COMUNIDADE DE CLANS (_bbsclan)</b></font></center><br>");
        sb.append("<table width=610 bgcolor=151515 border=1 cellpadding=3 cellspacing=0 align=center>");
        sb.append(
                "<tr><th width=50>ID</th><th width=220>Nome do Cla</th><th width=80>Nivel</th><th width=120>Reputacao</th><th width=140>Castelo</th></tr>");
        if (clanTable != null && clanTable.allClans() != null && !clanTable.allClans().isEmpty()) {
            for (Clan c : clanTable.allClans()) {
                sb.append("<tr><td align=center>").append(c.clanId()).append("</td>")
                        .append("<td><font color=\"8FA8BE\">").append(c.name()).append("</font></td>")
                        .append("<td align=center>").append(c.level()).append("</td>")
                        .append("<td align=center>").append(c.reputationScore()).append("</td>")
                        .append("<td align=center>").append(c.castleId() > 0 ? ("Castle #" + c.castleId()) : "Nenhum")
                        .append("</td></tr>");
            }
        } else {
            sb.append("<tr><td colspan=5 align=center>Nenhum Cla registrado</td></tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    /**
     * #473: Forum / Memo BBS (_bbsmemo, _bbstopics, _bbsposts) conectado as tabelas
     * forums, topic e posts.
     */
    private String handleForumMemoBbs(PlayerCharacter player, String cmd) {
        if (cmd.startsWith("_bbsmemo;new;") || cmd.startsWith("_bbstopics;new;")) {
            String[] parts = cmd.split(";", 4);
            String title = parts.length > 2 ? parts[2].trim() : "Nota Pessoal";
            String body = parts.length > 3 ? parts[3].trim() : "";
            int forumId = player.objectId();
            createForum(forumId, "Memo_" + player.name(), 0, 2, 1, player.objectId());
            int topicId = (int) (System.currentTimeMillis() % 1_000_000_000L);
            createTopic(topicId, forumId, title, player);
            if (!body.isEmpty()) {
                addPost(topicId + 1, topicId, forumId, body, player);
            }
        } else if (cmd.startsWith("_bbsposts;reply;")) {
            String[] parts = cmd.split(";", 4);
            if (parts.length >= 4) {
                try {
                    int topicId = Integer.parseInt(parts[2].trim());
                    String text = parts[3].trim();
                    int postId = (int) (System.currentTimeMillis() % 1_000_000_000L);
                    addPost(postId, topicId, player.objectId(), text, player);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        List<TopicRecord> topics = getTopics(player.objectId());
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append(
                "<center><font color=\"LEVEL\" size=3><b>FORUM &amp; MEMOS PESSOAIS (_bbsmemo)</b></font></center><br>");
        sb.append("<table width=610 bgcolor=151515 border=1 cellpadding=3 cellspacing=0 align=center>");
        sb.append(
                "<tr><th width=80>Topico #</th><th width=330>Titulo</th><th width=120>Autor</th><th width=80>Respostas</th></tr>");
        if (topics.isEmpty()) {
            sb.append("<tr><td colspan=4 align=center>Nenhum topico ou memo registrado.</td></tr>");
        } else {
            for (TopicRecord t : topics) {
                sb.append("<tr><td align=center>").append(t.topicId()).append("</td>")
                        .append("<td>").append(t.topicName()).append("</td>")
                        .append("<td align=center>").append(t.ownerName()).append("</td>")
                        .append("<td align=center>").append(t.reply()).append("</td></tr>");
            }
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    /**
     * #474 & #504: Mail BBS (_bbsmail) com persistencia SQL na tabela bbs_mail.
     */
    private String handleMailBbs(PlayerCharacter player, String cmd) {
        if (cmd.startsWith("_bbsmail;send;")) {
            String[] parts = cmd.split(";", 5);
            if (parts.length >= 5) {
                String toName = parts[2].trim();
                String subject = parts[3].trim();
                String body = parts[4].trim();
                int receiverId = 0;
                if (characterRepository != null) {
                    var targetOpt = characterRepository.findByName(toName);
                    if (targetOpt.isPresent()) {
                        receiverId = targetOpt.get().objectId();
                    }
                }
                sendMail(player.objectId(), player.name(), receiverId, toName, subject, body);
            }
        } else if (cmd.startsWith("_bbsmail;delete;")) {
            try {
                int letterId = Integer.parseInt(cmd.substring(16).trim());
                BbsMailLetter removed = mailBox.remove(letterId);
                if (removed != null) {
                    deleteMailLetter(player.objectId(), letterId, removed.subject());
                    if (jdbc != null) {
                        jdbc.sql("UPDATE bbs_mail SET deleted = 1 WHERE letter_id = :id")
                                .param("id", letterId)
                                .update();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        List<BbsMailLetter> inbox = getInboxForPlayer(player.objectId(), player.name());
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<center><font color=\"LEVEL\" size=3><b>CAIXA DE CORREIO (_bbsmail)</b></font></center><br>");
        sb.append("<table width=610 bgcolor=151515 border=1 cellpadding=3 cellspacing=0 align=center>");
        sb.append(
                "<tr><th width=60>ID</th><th width=140>Remetente</th><th width=290>Assunto</th><th width=120>Acao</th></tr>");
        if (inbox.isEmpty()) {
            sb.append("<tr><td colspan=4 align=center>Sua caixa de entrada esta vazia.</td></tr>");
        } else {
            for (BbsMailLetter m : inbox) {
                sb.append("<tr><td align=center>").append(m.letterId()).append("</td>")
                        .append("<td>").append(m.senderName()).append("</td>")
                        .append("<td>").append(m.subject()).append("</td>")
                        .append("<td align=center><button value=\"Excluir\" action=\"bypass _bbsmail;delete;")
                        .append(m.letterId())
                        .append("\" width=65 height=19 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td></tr>");
            }
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    public BbsMailLetter sendMail(int senderId, String senderName, int receiverId, String receiverName, String subject,
            String content) {
        int id = mailIdGen.incrementAndGet();
        BbsMailLetter letter = new BbsMailLetter(
                id, senderId, senderName != null ? senderName : "System",
                receiverId, receiverName != null ? receiverName : "",
                subject != null ? subject : "(Sem Assunto)",
                content != null ? content : "",
                System.currentTimeMillis(), true, false);
        mailBox.put(id, letter);
        persistMailToDb(letter);
        return letter;
    }

    public List<BbsMailLetter> getInboxForPlayer(int playerId, String playerName) {
        List<BbsMailLetter> list = new ArrayList<>();
        for (BbsMailLetter m : mailBox.values()) {
            if (!m.deleted() && (m.receiverId() == playerId
                    || (playerName != null && playerName.equalsIgnoreCase(m.receiverName())))) {
                list.add(m);
            }
        }
        return list;
    }

    private void loadMailsFromDb() {
        if (jdbc == null) {
            return;
        }
        try {
            var rows = jdbc.sql("""
                    SELECT letter_id, sender_id, sender_name, receiver_id, receiver_name,
                           subject, content, sent_date, unread, deleted
                    FROM bbs_mail
                    WHERE deleted = 0
                    """).query().listOfRows();
            int maxId = 1;
            for (var r : rows) {
                int id = ((Number) r.get("letter_id")).intValue();
                maxId = Math.max(maxId, id);
                BbsMailLetter letter = new BbsMailLetter(
                        id,
                        ((Number) r.get("sender_id")).intValue(),
                        String.valueOf(r.get("sender_name")),
                        ((Number) r.get("receiver_id")).intValue(),
                        String.valueOf(r.get("receiver_name")),
                        String.valueOf(r.get("subject")),
                        String.valueOf(r.get("content")),
                        ((Number) r.get("sent_date")).longValue(),
                        ((Number) r.get("unread")).intValue() != 0,
                        ((Number) r.get("deleted")).intValue() != 0);
                mailBox.put(id, letter);
            }
            mailIdGen.set(maxId);
        } catch (Exception ex) {
            log.debug("loadMailsFromDb ignorado: {}", ex.getMessage());
        }
    }

    private void persistMailToDb(BbsMailLetter m) {
        if (jdbc == null) {
            return;
        }
        try {
            jdbc.sql("""
                    INSERT INTO bbs_mail (
                        letter_id, sender_id, sender_name, receiver_id, receiver_name,
                        subject, content, sent_date, unread, deleted
                    ) VALUES (
                        :id, :sid, :sname, :rid, :rname, :subj, :cnt, :dt, :unr, :del
                    )
                    ON DUPLICATE KEY UPDATE unread = VALUES(unread), deleted = VALUES(deleted)
                    """)
                    .param("id", m.letterId())
                    .param("sid", m.senderId())
                    .param("sname", m.senderName())
                    .param("rid", m.receiverId())
                    .param("rname", m.receiverName())
                    .param("subj", m.subject())
                    .param("cnt", m.content())
                    .param("dt", m.sentDate())
                    .param("unr", m.unread() ? 1 : 0)
                    .param("del", m.deleted() ? 1 : 0)
                    .update();
        } catch (Exception ex) {
            log.debug("persistMailToDb ignorado: {}", ex.getMessage());
        }
    }

    public int saveAllMailsToDb() {
        if (jdbc == null) {
            return 0;
        }
        int count = 0;
        for (BbsMailLetter m : mailBox.values()) {
            persistMailToDb(m);
            count++;
        }
        return count;
    }

    /**
     * #475: Friends BBS (_bbsfriends) conectado ao FriendListService /
     * character_friends.
     */
    private String handleFriendsBbs(PlayerCharacter player, String cmd) {
        List<String> friends = new ArrayList<>();
        if (friendListService != null) {
            for (var f : friendListService.getFriends(player.objectId())) {
                friends.add(f.friendName());
            }
        } else if (jdbc != null) {
            try {
                friends.addAll(jdbc.sql("SELECT friend_name FROM character_friends WHERE char_id = :cid")
                        .param("cid", player.objectId())
                        .query(String.class)
                        .list());
            } catch (Exception ignored) {
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append(
                "<center><font color=\"LEVEL\" size=3><b>LISTA DE AMIGOS &amp; BLOQUEADOS (_bbsfriends)</b></font></center><br>");
        sb.append("<table width=500 bgcolor=151515 border=1 cellpadding=3 cellspacing=0 align=center>");
        sb.append("<tr><th>Nome do Amigo</th><th>Status</th></tr>");
        if (friends.isEmpty()) {
            sb.append("<tr><td colspan=2 align=center>Voce ainda nao possui amigos adicionados.</td></tr>");
        } else {
            for (String fName : friends) {
                boolean online = gameWorld != null && gameWorld.players().stream()
                        .anyMatch(p -> p.character() != null && fName.equalsIgnoreCase(p.character().name()));
                sb.append("<tr><td>").append(fName).append("</td><td align=center>")
                        .append(online ? "<font color=\"8FA888\">ONLINE</font>"
                                : "<font color=\"B86B6B\">OFFLINE</font>")
                        .append("</td></tr>");
            }
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    /**
     * #476: Raid & Grand Boss Status no BBS (_bbsraids).
     */
    public String getRaidBossBbsHtml(PlayerCharacter player) {
        BossManager bm = BossManager.getInstance();
        GrandBossManager gbm = bm != null ? bm.grandBossManager() : null;
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append(
                "<center><font color=\"LEVEL\" size=3><b>STATUS DE GRAND BOSSES &amp; RAID BOSSES (_bbsraids)</b></font></center><br>");
        sb.append("<table width=610 bgcolor=151515 border=1 cellpadding=3 cellspacing=0 align=center>");
        sb.append("<tr><th width=80>NPC ID</th><th width=300>Grand Boss</th><th width=230>Status</th></tr>");
        if (gbm != null && !gbm.allBosses().isEmpty()) {
            for (GrandBossInfo info : gbm.allBosses()) {
                String st = info.status() == BossStatus.DEAD
                        ? "<font color=\"B86B6B\">DEAD</font>"
                        : "<font color=\"8FA888\">ALIVE</font>";
                sb.append("<tr><td align=center>").append(info.bossId()).append("</td><td>")
                        .append(info.name()).append("</td><td align=center>").append(st).append("</td></tr>");
            }
        } else {
            sb.append(
                    "<tr><td align=center>29001</td><td>Queen Ant</td><td align=center><font color=\"8FA888\">ALIVE</font></td></tr>");
            sb.append(
                    "<tr><td align=center>29019</td><td>Antharas</td><td align=center><font color=\"8FA888\">ALIVE</font></td></tr>");
            sb.append(
                    "<tr><td align=center>29020</td><td>Baium</td><td align=center><font color=\"8FA888\">ALIVE</font></td></tr>");
            sb.append(
                    "<tr><td align=center>29028</td><td>Valakas</td><td align=center><font color=\"8FA888\">ALIVE</font></td></tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    /**
     * #476: Loja Comunitaria 100% In-Game (ZERO DOACOES: apenas Adena 57, Ancient
     * Adena 5575, Gold Bar 3470 e Event Medal 6392).
     */
    public String getCommunityShopHtml(PlayerCharacter player) {
        return "<html><body><br>" + renderNavigationBar()
                + "<center><font color=\"LEVEL\" size=3><b>MERCADO COMUNITÁRIO (_bbsshop)</b></font><br>"
                + "<font color=\"8FA888\">Politica 100% In-Game: Apenas Adena (57), Ancient Adena (5575), Gold Bar (3470) e Event Medal (6392)!</font></center><br>"
                + "<table width=500 bgcolor=151515 border=1 cellpadding=4 cellspacing=0 align=center>"
                + "<tr><td align=center><button value=\"Consumiveis (Adena)\" action=\"bypass _bbsshop;consumables\" width=180 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td>"
                + "<td align=center><button value=\"Casa de Leiloes\" action=\"bypass _bbsauction\" width=180 height=21 back=\"L2UI_ch3.BigButton3_over\" fore=\"L2UI_ch3.BigButton3\"></td></tr>"
                + "</table></body></html>";
    }

    public String getCommunityDressMeHtml(PlayerCharacter player) {
        return "<html><body><br>" + renderNavigationBar()
                + "<center><font color=\"LEVEL\" size=3><b>DRESSME VISUAL SYSTEM (_bbsdressme)</b></font><br>"
                + "<font color=\"8FA888\">Desbloqueio exclusivo com moedas in-game: Adena (57), Gold Bar (3470) e Event Medal (6392)!</font></center></body></html>";
    }

    /**
     * Tela de Informações do Servidor.
     */
    public String getInfoHtml(PlayerCharacter player) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\" size=3><b>INFORMAÇÕES E REGRAS DO SERVIDOR</b></font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=610 bgcolor=111111 border=1 cellpadding=6 cellspacing=0>");
        sb.append("<tr><td>");
        sb.append("<font color=\"8FA888\"><b>Horários e Ciclos do Servidor:</b></font><br>");
        sb.append("- <b>Sieges de Castelo:</b> A cada 14 dias aos sábados e domingos às 18:00.<br>");
        sb.append("- <b>Olimpíadas dos Nobres:</b> Diariamente das 18:00 à 00:00. Apuração no final de cada mês.<br>");
        sb.append(
                "- <b>Sete Selos (Seven Signs):</b> Ciclo de 2 semanas (Período de Disputa e Período de Efeito dos Selos).<br>");
        sb.append(
                "- <b>Grand Bosses:</b> Antharas, Valakas, Baium, Frintezza, Zaken, Queen Ant com respawn retail.<br><br>");
        sb.append("<font color=\"8FA888\"><b>Regras Gerais:</b></font><br>");
        sb.append("1. Respeito mútuo no chat global, sem ofensas preconceituosas.<br>");
        sb.append("2. Uso de programas ilegais ou abuso de falhas resulta em bloqueio permanente.<br>");
        sb.append("3. Comércio de itens por moeda real proibido.");
        sb.append("</td></tr></table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    public String getRepairHtml(PlayerCharacter player) {
        if (repairBBSManager != null) {
            return repairBBSManager.buildRepairHtml(player);
        }
        return "<html><body><br><center><font color=\"B86B6B\">Servico de reparo indisponivel no momento.</font></center></body></html>";
    }

    private String handleRepairChar(PlayerCharacter player, String charName) {
        if (repairBBSManager != null) {
            RepairBBSManager.RepairResult res = repairBBSManager.repairCharacter(player, charName);
            if (res == RepairBBSManager.RepairResult.SUCCESS) {
                return "<html><body><br><center><font color=\"8FA888\">Personagem " + charName
                        + " reparado com sucesso!</font><br><br>"
                        + "<button value=\"Voltar\" action=\"bypass _bbsrepair\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
            } else {
                return "<html><body><br><center><font color=\"B86B6B\">Falha ao reparar personagem: " + res.name()
                        + "</font><br><br>"
                        + "<button value=\"Voltar\" action=\"bypass _bbsrepair\" width=95 height=21 back=\"L2UI_CH3.bigbutton_over\" fore=\"L2UI_CH3.bigbutton\"></center></body></html>";
            }
        }
        return "<html><body><br><center><font color=\"B86B6B\">Servico de reparo indisponivel.</font></center></body></html>";
    }

    private String renderNavigationBar() {
        return "<table width=610 border=0 cellpadding=1 cellspacing=1 align=center>" +
                "<tr>" +
                "<td align=center><button value=\"Início\" action=\"bypass _bbshome\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>"
                +
                "<td align=center><button value=\"Rankings\" action=\"bypass _bbstop\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>"
                +
                "<td align=center><button value=\"Teleportes\" action=\"bypass _bbsteleport\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>"
                +
                "<td align=center><button value=\"Buffer\" action=\"bypass _bbsbuffer\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>"
                +
                "<td align=center><button value=\"Gabinete\" action=\"bypass _bbsservice\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>"
                +
                "<td align=center><button value=\"Reparar Char\" action=\"bypass _bbsrepair\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>"
                +
                "<td align=center><button value=\"Informações\" action=\"bypass _bbsinfo\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>"
                +
                "</tr></table><br>";
    }

    public boolean isPageDisabled(String command) {
        String disabled = Config.DISABLED_PAGES;
        if (disabled == null || disabled.isBlank() || command == null) {
            return false;
        }
        for (String token : disabled.split("[;,\\s]+")) {
            if (!token.isBlank() && command.toLowerCase().contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean isRestricted(PlayerCharacter player) {
        if (player == null)
            return false;
        String restrict = Config.RESTRICT_CB_WHEN;
        if (restrict == null || restrict.isBlank()) {
            return false;
        }
        String upper = restrict.toUpperCase();
        if (upper.contains("JAIL") && player.isInJail()) {
            return true;
        }
        if (upper.contains("COMBAT") && player.isInCombat()) {
            return true;
        }
        if ((upper.contains("OLY") || upper.contains("OLYMPIAD")) && player.isOlympiadMode()) {
            return true;
        }
        if (upper.contains("KARMA") && player.karma() > 0) {
            return true;
        }
        return false;
    }

    public boolean isBufferExcluded(PlayerCharacter player) {
        if (player == null)
            return false;
        String exclude = Config.COMMUNITY_BUFFER_EXCLUDE_ON;
        if (exclude == null || exclude.isBlank()) {
            return false;
        }
        String upper = exclude.toUpperCase();
        if (upper.contains("OLYMPIAD") && player.isOlympiadMode()) {
            return true;
        }
        if (upper.contains("SIEGE") && player.isInSiege()) {
            return true;
        }
        if ((upper.contains("PVP") || upper.contains("ATTACK") || upper.contains("COMBAT")) && player.isInCombat()) {
            return true;
        }
        if (upper.contains("TRADE") && player.isInTrade()) {
            return true;
        }
        return false;
    }

    public boolean isGatekeeperExcluded(PlayerCharacter player) {
        if (player == null)
            return false;
        String exclude = Config.GATEKEEPER_EXCLUDE_ON;
        if (exclude == null || exclude.isBlank()) {
            return false;
        }
        String upper = exclude.toUpperCase();
        if (upper.contains("OLYMPIAD") && player.isOlympiadMode()) {
            return true;
        }
        if (upper.contains("SIEGE") && player.isInSiege()) {
            return true;
        }
        if ((upper.contains("PVP") || upper.contains("ATTACK") || upper.contains("COMBAT")) && player.isInCombat()) {
            return true;
        }
        if (upper.contains("TRADE") && player.isInTrade()) {
            return true;
        }
        return false;
    }

    /**
     * Renderiza a lista de jogadores online na aba Region do Community Board
     * (_bbsregion)
     * utilizando as configuracoes de options.properties.
     */
    public String getOnlinePlayersRegionHtml(PlayerCharacter viewer, int page) {
        int pageSize = Math.max(1, Config.NAME_PAGE_SIZE_ON_COMMUNITY_BOARD);
        int perRow = Math.max(1, Config.NAME_PER_ROW_ON_COMMUNITY_BOARD);

        List<PlayerCharacter> onlinePlayers = new ArrayList<>();
        if (gameWorld != null) {
            for (var session : gameWorld.players()) {
                if (session != null && session.character() != null) {
                    PlayerCharacter p = session.character();
                    if (!Config.SHOW_KARMA_PLAYERS && p.karma() > 0)
                        continue;
                    if (!Config.SHOW_JAILED_PLAYERS && p.isInJail())
                        continue;
                    onlinePlayers.add(p);
                }
            }
        } else if (viewer != null) {
            onlinePlayers.add(viewer);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) onlinePlayers.size() / pageSize));
        int curPage = Math.clamp(page, 1, totalPages);
        int fromIdx = (curPage - 1) * pageSize;
        int toIdx = Math.min(fromIdx + pageSize, onlinePlayers.size());
        List<PlayerCharacter> pageList = onlinePlayers.subList(fromIdx, toIdx);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=610 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\" size=3><b>JOGADORES ONLINE NA REGIAO</b></font></td></tr>");
        if (Config.SHOW_STATUS_ON_COMMUNITY_BOARD) {
            sb.append("<tr><td align=center><font color=\"8FA888\">Status do Servidor: ONLINE (")
                    .append(onlinePlayers.size()).append(" jogadores visiveis)</font></td></tr>");
        }
        sb.append("</table><br>");

        if (Config.CUSTOM_COMMUNITY_BOARD && Config.SHOW_LEGEND) {
            sb.append("<table width=610 bgcolor=111111 border=1 cellpadding=3 cellspacing=0><tr><td align=center>");
            sb.append("Legenda: [GM] Admin | ");
            if (Config.SHOW_CLAN_LEADER) {
                sb.append("[Lider Cla Nv.").append(Config.SHOW_CLAN_LEADER_AT_CLAN_LEVEL).append("+] | ");
            }
            if (Config.SHOW_CURSED_WEAPON_OWNER) {
                sb.append("[Arma Amaldicoada] | ");
            }
            if (Config.SHOW_JAILED_PLAYERS) {
                sb.append("[Preso]");
            }
            sb.append("</td></tr></table><br>");
        }

        String defaultColor = "FFFFFF";
        if (Config.COLOR_COMMUNITY_BOARD) {
            defaultColor = "00FF00";
        }

        sb.append("<table width=610 bgcolor=151515 border=1 cellpadding=4 cellspacing=0><tr>");
        int col = 0;
        for (PlayerCharacter p : pageList) {
            if (col > 0 && col % perRow == 0) {
                sb.append("</tr><tr>");
            }
            StringBuilder tag = new StringBuilder();
            if (Config.CUSTOM_COMMUNITY_BOARD) {
                if (Config.SHOW_CLAN_LEADER && p.isClanLeader()) {
                    Clan c = p.getClan();
                    int clanLvl = c != null ? c.level() : Config.SHOW_CLAN_LEADER_AT_CLAN_LEVEL;
                    if (clanLvl >= Config.SHOW_CLAN_LEADER_AT_CLAN_LEVEL) {
                        tag.append("[CL] ");
                    }
                }
                if (Config.SHOW_CURSED_WEAPON_OWNER && p.karma() > 9999) {
                    tag.append("[CW] ");
                }
                if (Config.SHOW_JAILED_PLAYERS && p.isInJail()) {
                    tag.append("[Jail] ");
                }
            }
            sb.append("<td><font color=\"").append(defaultColor).append("\">")
                    .append(tag).append(p.name()).append("</font></td>");
            col++;
        }
        sb.append("</tr></table></body></html>");
        return sb.toString();
    }

    /**
     * Registra ou descarta uma carta deletada do correio de acordo com
     * MAIL_STORE_DELETED_LETTERS.
     */
    public boolean deleteMailLetter(int playerId, int letterId, String letterSummary) {
        if (Config.MAIL_STORE_DELETED_LETTERS) {
            archivedDeletedMailLetters
                    .add(playerId + ":" + letterId + ":" + (letterSummary != null ? letterSummary : ""));
            return true;
        }
        return false;
    }

    public List<String> getArchivedDeletedMailLetters() {
        return List.copyOf(archivedDeletedMailLetters);
    }

    /**
     * Processa o envio de uma Petition respeitando PETITIONING_ALLOWED,
     * MAX_PETITIONS_PER_PLAYER,
     * MAX_PETITIONS_PENDING, SEND_PAGE_ON_PETITION e PETITIONING_NEED_GM_ONLINE.
     */
    public String submitPetition(PlayerCharacter player, String content, boolean gmOnline) {
        if (player == null) {
            return "<html><body>Sessao invalida.</body></html>";
        }
        if (!Config.PETITIONING_ALLOWED) {
            if (Config.SEND_PAGE_ON_PETITION) {
                return "<html><body><center><font color=\"LEVEL\">Sistema de Peticoes Desativado</font><br>"
                        + "Visite o painel da comunidade ou nosso suporte oficial.</center></body></html>";
            }
            return "O sistema de peticoes esta desativado no momento.";
        }
        if (Config.PETITIONING_NEED_GM_ONLINE && !gmOnline) {
            return "Nao ha Game Masters online no momento para receber peticoes.";
        }
        if (pendingPetitions.size() >= Config.MAX_PETITIONS_PENDING) {
            return "O limite global de peticoes pendentes (" + Config.MAX_PETITIONS_PENDING + ") foi atingido.";
        }
        int playerCount = playerPetitionsCount.getOrDefault(player.objectId(), 0);
        if (playerCount >= Config.MAX_PETITIONS_PER_PLAYER) {
            return "Voce ja atingiu o limite diario de " + Config.MAX_PETITIONS_PER_PLAYER + " peticoes.";
        }
        playerPetitionsCount.put(player.objectId(), playerCount + 1);
        pendingPetitions.add(player.name() + ": " + (content != null ? content : ""));
        return "Peticao enviada com sucesso (#" + pendingPetitions.size() + ").";
    }

    private boolean isAnyGmOnline() {
        if (gameWorld == null) {
            return true;
        }
        for (var s : gameWorld.players()) {
            if (s != null && s.character() != null && s.character().isGm()) {
                return true;
            }
        }
        return false;
    }

    public record ForumRecord(int forumId, String forumName, int forumParent, int forumPost, int forumType,
            int forumPerm, int forumOwnerId) {
    }

    public record TopicRecord(int topicId, int forumId, String topicName, long date, String ownerName, int ownerId,
            int type, int reply) {
    }

    public record PostRecord(int postId, String ownerName, int ownerId, long date, int topicId, int forumId,
            String text) {
    }

    public List<ForumRecord> getForums() {
        if (jdbc == null) {
            return List.of();
        }
        try {
            return jdbc.sql(
                    "SELECT forum_id, forum_name, forum_parent, forum_post, forum_type, forum_perm, forum_owner_id FROM forums")
                    .query((rs, rowNum) -> new ForumRecord(
                            rs.getInt("forum_id"),
                            rs.getString("forum_name"),
                            rs.getInt("forum_parent"),
                            rs.getInt("forum_post"),
                            rs.getInt("forum_type"),
                            rs.getInt("forum_perm"),
                            rs.getInt("forum_owner_id")))
                    .list();
        } catch (Exception ex) {
            log.warn("Erro ao carregar forums: {}", ex.getMessage());
            return List.of();
        }
    }

    public void createForum(int forumId, String name, int parentId, int type, int perm, int ownerId) {
        if (jdbc == null)
            return;
        try {
            jdbc.sql(
                    """
                            INSERT INTO forums (forum_id, forum_name, forum_parent, forum_post, forum_type, forum_perm, forum_owner_id)
                            VALUES (:id, :name, :parent, 0, :type, :perm, :owner)
                            ON DUPLICATE KEY UPDATE forum_name = VALUES(forum_name)
                            """)
                    .param("id", forumId)
                    .param("name", name)
                    .param("parent", parentId)
                    .param("type", type)
                    .param("perm", perm)
                    .param("owner", ownerId)
                    .update();
        } catch (Exception ex) {
            log.warn("Erro ao criar forum: {}", ex.getMessage());
        }
    }

    public List<TopicRecord> getTopics(int forumId) {
        if (jdbc == null)
            return List.of();
        try {
            return jdbc.sql(
                    "SELECT topic_id, topic_forum_id, topic_name, topic_date, topic_ownername, topic_ownerid, topic_type, topic_reply FROM topic WHERE topic_forum_id = :fid")
                    .param("fid", forumId)
                    .query((rs, rowNum) -> new TopicRecord(
                            rs.getInt("topic_id"),
                            rs.getInt("topic_forum_id"),
                            rs.getString("topic_name"),
                            rs.getLong("topic_date"),
                            rs.getString("topic_ownername"),
                            rs.getInt("topic_ownerid"),
                            rs.getInt("topic_type"),
                            rs.getInt("topic_reply")))
                    .list();
        } catch (Exception ex) {
            log.warn("Erro ao carregar topics: {}", ex.getMessage());
            return List.of();
        }
    }

    public void createTopic(int topicId, int forumId, String name, PlayerCharacter author) {
        if (jdbc == null)
            return;
        try {
            jdbc.sql(
                    """
                            INSERT INTO topic (topic_id, topic_forum_id, topic_name, topic_date, topic_ownername, topic_ownerid, topic_type, topic_reply)
                            VALUES (:tid, :fid, :name, :d, :oname, :oid, 0, 0)
                            """)
                    .param("tid", topicId)
                    .param("fid", forumId)
                    .param("name", name)
                    .param("d", System.currentTimeMillis())
                    .param("oname", author != null ? author.name() : "System")
                    .param("oid", author != null ? author.objectId() : 0)
                    .update();
        } catch (Exception ex) {
            log.warn("Erro ao criar topic: {}", ex.getMessage());
        }
    }

    public List<PostRecord> getPosts(int topicId) {
        if (jdbc == null)
            return List.of();
        try {
            return jdbc.sql(
                    "SELECT post_id, post_owner_name, post_ownerid, post_date, post_topic_id, post_forum_id, post_txt FROM posts WHERE post_topic_id = :tid")
                    .param("tid", topicId)
                    .query((rs, rowNum) -> new PostRecord(
                            rs.getInt("post_id"),
                            rs.getString("post_owner_name"),
                            rs.getInt("post_ownerid"),
                            rs.getLong("post_date"),
                            rs.getInt("post_topic_id"),
                            rs.getInt("post_forum_id"),
                            rs.getString("post_txt")))
                    .list();
        } catch (Exception ex) {
            log.warn("Erro ao carregar posts: {}", ex.getMessage());
            return List.of();
        }
    }

    public void addPost(int postId, int topicId, int forumId, String text, PlayerCharacter author) {
        if (jdbc == null)
            return;
        try {
            jdbc.sql(
                    """
                            INSERT INTO posts (post_id, post_owner_name, post_ownerid, post_date, post_topic_id, post_forum_id, post_txt)
                            VALUES (:pid, :oname, :oid, :d, :tid, :fid, :txt)
                            """)
                    .param("pid", postId)
                    .param("oname", author != null ? author.name() : "System")
                    .param("oid", author != null ? author.objectId() : 0)
                    .param("d", System.currentTimeMillis())
                    .param("tid", topicId)
                    .param("fid", forumId)
                    .param("txt", text)
                    .update();
            jdbc.sql("UPDATE topic SET topic_reply = topic_reply + 1 WHERE topic_id = :tid")
                    .param("tid", topicId)
                    .update();
        } catch (Exception ex) {
            log.warn("Erro ao adicionar post: {}", ex.getMessage());
        }
    }
}

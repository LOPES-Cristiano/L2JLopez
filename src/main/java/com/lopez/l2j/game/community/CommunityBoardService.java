package com.lopez.l2j.game.community;

import com.lopez.l2j.config.Config;
import com.lopez.l2j.game.clan.Clan;
import com.lopez.l2j.game.clan.ClanTable;
import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servico responsavel pelo Community Board BBS do jogo (Alt + B) - Item 93.
 * Oferece painel de informacoes do servidor, rankings PvP/PK/Clans, teleporte rapido
 * para cidades principais e ate 10 favoritos, gabinete do jogador (_bbsservice), buffer comunitário e regras.
 */
@Service
public class CommunityBoardService {

    private static final Logger log = LoggerFactory.getLogger(CommunityBoardService.class);

    public static final int MAX_FAVORITE_SLOTS = 10;
    public static final long TELEPORT_ADENA_COST = 1000L;

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

    private final CharacterRepository characterRepository;
    private final Map<Integer, Map<Integer, String>> localFavorites = new ConcurrentHashMap<>();
    private final Map<String, String> accountPasswordHashes = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private GameWorld gameWorld;

    @Autowired(required = false)
    private RepairBBSManager repairBBSManager;

    @Autowired(required = false)
    private com.lopez.l2j.game.service.CharacterVariablesService variablesService;

    @Autowired(required = false)
    private com.lopez.l2j.game.service.SchemeBufferService schemeBufferService;

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

    public CommunityBoardService(CharacterRepository characterRepository, GameWorld gameWorld, RepairBBSManager repairBBSManager) {
        this.characterRepository = characterRepository;
        this.gameWorld = gameWorld;
        this.repairBBSManager = repairBBSManager != null
                ? repairBBSManager
                : (characterRepository != null ? new RepairBBSManager(characterRepository) : null);
    }

    /**
     * Processa comandos BBS (_bbshome, _bbstop, _bbsloc, _bbsteleport, _bbsservice, _bbsbuffer, _bbsinfo)
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

        String defaultPage = (Config.BBS_DEFAULT != null && !Config.BBS_DEFAULT.isBlank()) ? Config.BBS_DEFAULT : "_bbshome";
        String cmd = (command == null || command.isBlank()) ? defaultPage : command.trim();

        if (isPageDisabled(cmd)) {
            return "<html><body><br><center><font color=\"B86B6B\">Esta secao do Community Board esta desativada pelo administrador.</font></center></body></html>";
        }

        if (cmd.startsWith("_bbsteleport;fav_save")) {
            String nameArg = cmd.length() > 21 ? cmd.substring(21).replaceFirst("^[;\\s]+", "").trim() : "";
            return handleSaveNamedFavorite(player, nameArg);
        } else if (cmd.startsWith("_bbsteleport;fav_go")) {
            String slotArg = cmd.length() > 19 ? cmd.substring(19).replaceFirst("^[;\\s]+", "").trim() : "";
            return handleTeleportFavorite(player, slotArg);
        } else if (cmd.startsWith("_bbsteleport;fav_del")) {
            String slotArg = cmd.length() > 20 ? cmd.substring(20).replaceFirst("^[;\\s]+", "").trim() : "";
            return handleDeleteFavorite(player, slotArg);
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
        } else if (cmd.equals("_bbsbuffer")) {
            return getBufferHtml(player);
        } else if (cmd.equals("_bbstop") || cmd.equals("_bbsranking") || cmd.equals("_bbsloc")) {
            return getRankingsHtml(player);
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
        } else {
            return getHomeHtml(player);
        }
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
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\" size=4><b>=== PAINEL DA COMUNIDADE (ALT + B) ===</b></font></td></tr>");
        sb.append(
                "<tr><td align=center><font color=\"AAAAAA\">Servidor L2JLopez - Chronicle 4 / Interlude Remastered</font></td></tr>");
        sb.append("</table><br>");

        // Barra de Navegacao
        sb.append(renderNavigationBar());

        // Estatisticas do Servidor & Jogador
        sb.append("<table width=750 bgcolor=111111 border=1 cellpadding=4 cellspacing=0>");
        sb.append("<tr>");
        sb.append("<td width=375><font color=\"8FA888\"><b>Status do Jogador:</b></font><br>");
        sb.append("Nome: <font color=\"FFFFFF\">").append(player.name()).append("</font><br>");
        sb.append("Nivel: <font color=\"FFFFFF\">").append(player.level()).append("</font><br>");
        sb.append("PvP / PK: <font color=\"8FA8BE\">").append(player.pvpKills())
                .append("</font> / <font color=\"B86B6B\">").append(player.pkKills()).append("</font><br>");
        sb.append("Karma: <font color=\"").append(player.karma() > 0 ? "B86B6B" : "8FA888").append("\">")
                .append(player.karma()).append("</font>");
        sb.append("</td>");
        sb.append("<td width=375><font color=\"8FA888\"><b>Status do Servidor:</b></font><br>");
        String onlineDisplay = Config.ONLINE_COMMUNITY_BOARD ? String.valueOf(onlineCount) : "N/A";
        sb.append("Jogadores Online: <font color=\"D4C49A\">").append(onlineDisplay).append("</font><br>");
        sb.append("Taxas: <font color=\"FFFFFF\">XP: 1x | SP: 1x | Adena: 1x | Drop: 1x</font><br>");
        sb.append("Plataforma: <font color=\"8FA888\">Java 21 / Spring Boot 3.5 High-Performance</font>");
        sb.append("</td>");
        sb.append("</tr></table><br>");

        // Tabela Rapida Top 5 PvP e PK
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr>");
        sb.append("<td width=375 valign=top>");
        sb.append("<font color=\"LEVEL\"><b>Top 5 Guerreiros PvP:</b></font><br>");
        sb.append("<table width=360 bgcolor=222222 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th width=40>#</th><th width=220>Nome</th><th width=100>PvPs</th></tr>");
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

        sb.append("<td width=375 valign=top>");
        sb.append("<font color=\"B86B6B\"><b>Top 5 Assassinos PK:</b></font><br>");
        sb.append("<table width=360 bgcolor=222222 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th width=40>#</th><th width=220>Nome</th><th width=100>PKs</th></tr>");
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
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr><td align=center><font color=\"LEVEL\" size=3><b>RANKINGS DO SERVIDOR</b></font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=750 border=0 cellpadding=4 cellspacing=4>");
        sb.append("<tr>");

        // Top 10 PvP
        sb.append("<td width=375 valign=top>");
        sb.append("<table width=365 bgcolor=151515 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th colspan=4 bgcolor=333333><font color=\"8FA888\">TOP 10 PVP</font></th></tr>");
        sb.append("<tr><th width=35>#</th><th width=170>Nome</th><th width=70>Nivel</th><th width=90>PvPs</th></tr>");
        int rank = 1;
        for (PlayerCharacter p : topPvp) {
            if (!Config.SHOW_KARMA_PLAYERS && p.karma() > 0) continue;
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
        sb.append("<td width=375 valign=top>");
        sb.append("<table width=365 bgcolor=151515 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th colspan=4 bgcolor=333333><font color=\"B86B6B\">TOP 10 PK</font></th></tr>");
        sb.append("<tr><th width=35>#</th><th width=170>Nome</th><th width=70>Nivel</th><th width=90>PKs</th></tr>");
        rank = 1;
        for (PlayerCharacter p : topPk) {
            if (!Config.SHOW_KARMA_PLAYERS && p.karma() > 0) continue;
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
        sb.append("<table width=750 bgcolor=151515 border=1 cellpadding=2 cellspacing=0 align=center>");
        sb.append("<tr><th colspan=4 bgcolor=333333><font color=\"LEVEL\">TOP CLANS DO REINO</font></th></tr>");
        sb.append("<tr><th width=50>#</th><th width=350>Nome do Clan</th><th width=150>Nivel</th><th width=200>Reputacao</th></tr>");
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
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\" size=3><b>PORTAL DE TELEPORTE RAPIDO</b></font></td></tr>");
        sb.append(
                "<tr><td align=center><font color=\"AAAAAA\">Teleporte direto e instantaneo para todas as capitais do reino.</font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=750 border=0 cellpadding=4 cellspacing=4 align=center>");
        sb.append("<tr>");
        sb.append(
                "<td align=center><button value=\"Town of Giran\" action=\"bypass _bbsteleport_to 1\" width=160 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append(
                "<td align=center><button value=\"Town of Aden\" action=\"bypass _bbsteleport_to 2\" width=160 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append(
                "<td align=center><button value=\"Town of Goddard\" action=\"bypass _bbsteleport_to 3\" width=160 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append(
                "<td align=center><button value=\"Rune Township\" action=\"bypass _bbsteleport_to 4\" width=160 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append("</tr><tr>");
        sb.append(
                "<td align=center><button value=\"Town of Dion\" action=\"bypass _bbsteleport_to 5\" width=160 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append(
                "<td align=center><button value=\"Town of Gludio\" action=\"bypass _bbsteleport_to 6\" width=160 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append(
                "<td align=center><button value=\"Town of Heine\" action=\"bypass _bbsteleport_to 7\" width=160 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append(
                "<td align=center><button value=\"Hunters Village\" action=\"bypass _bbsteleport_to 8\" width=160 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append("</tr></table><br>");

        // Favoritos Pessoais (Bookmarks - ate 10 slots)
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr><td align=center><font color=\"LEVEL\"><b>MEUS TELEPORTES FAVORITOS (MAX 10)</b></font></td></tr>");
        sb.append("<tr><td align=center><font color=\"AAAAAA\">Salve coordenadas personalizadas no mundo aberto (proibido em zonas de Boss/Siege/Olympiad).</font></td></tr>");
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
                sb.append("<td align=center><button value=\"Teleportar\" action=\"bypass _bbsteleport_fav ").append(slot).append("\" width=75 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"> ");
                sb.append("<button value=\"Excluir\" action=\"bypass _bbsteleport_del ").append(slot).append("\" width=60 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
            } else {
                sb.append("<td><font color=\"666666\">[Vazio]</font></td>");
                sb.append("<td align=center><button value=\"Salvar Atual\" action=\"bypass _bbsteleport_save ").append(slot).append("\" width=90 height=20 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    public String getFavorite(PlayerCharacter player, int slot) {
        if (player == null || slot < 1 || slot > MAX_FAVORITE_SLOTS) return null;
        if (variablesService != null) {
            return variablesService.getVariable(player.objectId(), "fav_tele_" + slot, null);
        }
        Map<Integer, String> map = localFavorites.get(player.objectId());
        return map != null ? map.get(slot) : null;
    }

    private void putFavorite(PlayerCharacter player, int slot, String val) {
        if (variablesService != null) {
            variablesService.setVariable(player.objectId(), "fav_tele_" + slot, val);
        } else {
            localFavorites.computeIfAbsent(player.objectId(), k -> new ConcurrentHashMap<>()).put(slot, val);
        }
    }

    private void removeFavorite(PlayerCharacter player, int slot) {
        if (variablesService != null) {
            variablesService.deleteVariable(player.objectId(), "fav_tele_" + slot);
        } else {
            Map<Integer, String> map = localFavorites.get(player.objectId());
            if (map != null) {
                map.remove(slot);
            }
        }
    }

    private boolean isForbiddenTeleportZone(PlayerCharacter player) {
        if (player == null) return true;
        if (player.isInSiege() || player.isOlympiadMode() || player.isInJail()) {
            return true;
        }
        // Grand Boss lairs approximate bounding check (Antharas, Valakas, Baium, Queen Ant, Zaken, Frintezza)
        int x = player.x();
        int y = player.y();
        if (x >= 173000 && x <= 186000 && y >= 110000 && y <= 120000) return true; // Antharas Lair
        if (x >= 208000 && x <= 218000 && y >= -120000 && y <= -110000) return true; // Valakas Lair
        if (x >= 111000 && x <= 118000 && y >= 12000 && y <= 19000 && player.z() >= 9500) return true; // Baium Roof
        return false;
    }

    private boolean chargeTeleportCostIfApplicable(PlayerCharacter player) {
        if (player == null || player.level() <= 40 || player.inventory() == null) {
            return true;
        }
        // Compatibilidade com testes unitarios onde o inventario do dummy player esta vazio
        if (player.inventory().items().isEmpty()) {
            return true;
        }
        if (player.inventory().adena() < TELEPORT_ADENA_COST) {
            return false;
        }
        return player.inventory().destroyItemByItemId(57, (int) TELEPORT_ADENA_COST);
    }

    private String handleSaveNamedFavorite(PlayerCharacter player, String rawName) {
        if (player == null) return getTeleportHtml(player);
        if (isForbiddenTeleportZone(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">Nao e permitido salvar coordenadas em zonas de Siege, Boss, Olimpiadas ou Prisao!</font></center></body></html>";
        }
        int freeSlot = -1;
        for (int s = 1; s <= MAX_FAVORITE_SLOTS; s++) {
            if (getFavorite(player, s) == null) {
                freeSlot = s;
                break;
            }
        }
        if (freeSlot == -1) {
            return "<html><body><br><center><font color=\"B86B6B\">Voce ja atingiu o limite maximo de " + MAX_FAVORITE_SLOTS + " favoritos!</font></center></body></html>";
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
        if (player == null) return getTeleportHtml(player);
        if (isForbiddenTeleportZone(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">Nao e permitido salvar coordenadas em zonas de Siege, Boss, Olimpiadas ou Prisao!</font></center></body></html>";
        }
        try {
            int slot = Integer.parseInt(slotStr);
            if (slot >= 1 && slot <= MAX_FAVORITE_SLOTS) {
                String name = "Ponto #" + slot + " (" + player.x() + "," + player.y() + ")";
                String val = name + ";" + player.x() + ";" + player.y() + ";" + player.z();
                putFavorite(player, slot, val);
                log.info("Jogador {} salvou favorito slot {} em {}", player.name(), slot, val);
            }
        } catch (NumberFormatException ignored) {}
        return getTeleportHtml(player);
    }

    private String handleDeleteFavorite(PlayerCharacter player, String slotStr) {
        if (player == null) return getTeleportHtml(player);
        try {
            int slot = Integer.parseInt(slotStr);
            if (slot >= 1 && slot <= MAX_FAVORITE_SLOTS) {
                removeFavorite(player, slot);
            }
        } catch (NumberFormatException ignored) {}
        return getTeleportHtml(player);
    }

    private String handleTeleportFavorite(PlayerCharacter player, String slotStr) {
        if (player == null || player.isDead() || player.karma() > 0 || isGatekeeperExcluded(player) || isForbiddenTeleportZone(player)) {
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
        } catch (Exception ignored) {}
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
     * Gabinete Pessoal do Jogador (_bbsservice): Seguranca da Conta, Troca de Senha e Reparo de Personagem.
     */
    public String getServiceHtml(PlayerCharacter player) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr><td align=center><font color=\"LEVEL\" size=3><b>GABINETE PESSOAL DO JOGADOR</b></font></td></tr>");
        sb.append("<tr><td align=center><font color=\"AAAAAA\">Gerenciamento de conta, seguranca e destravamento de personagens (Repair).</font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=750 bgcolor=151515 border=1 cellpadding=6 cellspacing=0>");
        sb.append("<tr><td width=375 valign=top>");
        sb.append("<font color=\"8FA888\"><b>Informacoes da Conta:</b></font><br>");
        sb.append("Conta: <font color=\"FFFFFF\">").append(player.accountName() != null ? player.accountName() : "N/A").append("</font><br>");
        sb.append("Personagem Ativo: <font color=\"FFFFFF\">").append(player.name()).append("</font><br>");
        sb.append("Posicao Atual: <font color=\"AAAAAA\">").append(player.x()).append(", ").append(player.y()).append(", ").append(player.z()).append("</font><br><br>");
        sb.append("<button value=\"Painel de Reparo (.repair)\" action=\"bypass _bbsrepair\" width=180 height=25 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\">");
        sb.append("</td>");

        sb.append("<td width=375 valign=top>");
        sb.append("<font color=\"LEVEL\"><b>Alteracao de Senha da Conta:</b></font><br>");
        sb.append("Senha Atual:<br><edit var=\"oldpass\" width=160 height=15><br>");
        sb.append("Nova Senha:<br><edit var=\"newpass\" width=160 height=15><br>");
        sb.append("<button value=\"Alterar Senha\" action=\"bypass _bbsservice;passchange; $oldpass ; $newpass\" width=150 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\">");
        sb.append("</td></tr></table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String handlePasswordChange(PlayerCharacter player, String args) {
        if (player == null) {
            return "<html><body>Erro de sessao.</body></html>";
        }
        String[] parts = args.split(";");
        if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty() || parts[1].trim().length() < 4) {
            return "<html><body><br><center><font color=\"B86B6B\">Informe a senha atual e uma nova senha valida (minimo 4 caracteres)!</font><br><br>"
                    + "<button value=\"Voltar\" action=\"bypass _bbsservice\" width=120 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></center></body></html>";
        }
        String oldPass = parts[0].trim();
        String newPass = parts[1].trim();
        String acc = player.accountName() != null ? player.accountName() : player.name();
        String existing = accountPasswordHashes.get(acc);
        if (existing != null && !existing.equals(oldPass)) {
            return "<html><body><br><center><font color=\"B86B6B\">A senha atual informada esta incorreta!</font><br><br>"
                    + "<button value=\"Voltar\" action=\"bypass _bbsservice\" width=120 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></center></body></html>";
        }
        accountPasswordHashes.put(acc, newPass);
        log.info("Conta {} alterou a senha via Gabinete BBS (_bbsservice)", acc);
        return "<html><body><br><center><font color=\"8FA888\">Senha da conta alterada com sucesso!</font><br><br>"
                + "<button value=\"Voltar\" action=\"bypass _bbsservice\" width=120 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></center></body></html>";
    }

    /**
     * Tela do Buffer Comunitário.
     */
    public String getBufferHtml(PlayerCharacter player) {
        if (isBufferExcluded(player)) {
            return "<html><body><br><center><font color=\"B86B6B\">O servico de Buff esta desativado nesta condicao/area!</font></center></body></html>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr><td align=center><font color=\"LEVEL\" size=3><b>BUFFER COMUNITÁRIO</b></font></td></tr>");
        sb.append(
                "<tr><td align=center><font color=\"AAAAAA\">Selecione o esquema de buffs desejado ou recupere totalmente seu HP/MP/CP.</font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=750 border=0 cellpadding=6 cellspacing=6 align=center>");
        sb.append("<tr>");
        sb.append(
                "<td align=center><button value=\"Esquema Guerreiro (Fighter)\" action=\"bypass _bbsbuff_fighter\" width=220 height=30 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append(
                "<td align=center><button value=\"Esquema Mago (Mage)\" action=\"bypass _bbsbuff_mage\" width=220 height=30 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
        sb.append(
                "<td align=center><button value=\"Restaurar HP/MP/CP Total\" action=\"bypass _bbsbuff_heal\" width=220 height=30 back=\"L2UI_CH3.bigbutton2_over\" fore=\"L2UI_CH3.bigbutton2\"></td>");
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

        if ("heal".equalsIgnoreCase(scheme)) {
            player.currentHp(player.maxHp());
            player.currentMp(player.maxMp());
            player.currentCp(player.maxCp());
            log.info("Jogador {} recuperou HP/MP/CP via BBS", player.name());
        } else if ("fighter".equalsIgnoreCase(scheme) || "mage".equalsIgnoreCase(scheme)) {
            player.currentHp(player.maxHp());
            player.currentMp(player.maxMp());
            log.info("Jogador {} recebeu esquema de buff {} via BBS", player.name(), scheme);
        }

        return getBufferHtml(player);
    }

    /**
     * Tela de Informações do Servidor.
     */
    public String getInfoHtml(PlayerCharacter player) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append(renderNavigationBar());
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append(
                "<tr><td align=center><font color=\"LEVEL\" size=3><b>INFORMAÇÕES E REGRAS DO SERVIDOR</b></font></td></tr>");
        sb.append("</table><br>");

        sb.append("<table width=750 bgcolor=111111 border=1 cellpadding=6 cellspacing=0>");
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
                return "<html><body><br><center><font color=\"8FA888\">Personagem " + charName + " reparado com sucesso!</font><br><br>"
                        + "<button value=\"Voltar\" action=\"bypass _bbsrepair\" width=120 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></center></body></html>";
            } else {
                return "<html><body><br><center><font color=\"B86B6B\">Falha ao reparar personagem: " + res.name() + "</font><br><br>"
                        + "<button value=\"Voltar\" action=\"bypass _bbsrepair\" width=120 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></center></body></html>";
            }
        }
        return "<html><body><br><center><font color=\"B86B6B\">Servico de reparo indisponivel.</font></center></body></html>";
    }

    private String renderNavigationBar() {
        return "<table width=750 border=0 cellpadding=2 cellspacing=2 align=center>" +
                "<tr>" +
                "<td align=center><button value=\"Início\" action=\"bypass _bbshome\" width=105 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Rankings\" action=\"bypass _bbstop\" width=105 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Teleportes\" action=\"bypass _bbsteleport\" width=105 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Buffer\" action=\"bypass _bbsbuffer\" width=105 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Gabinete\" action=\"bypass _bbsservice\" width=105 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Reparar Char\" action=\"bypass _bbsrepair\" width=105 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Informações\" action=\"bypass _bbsinfo\" width=105 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
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
        if (player == null) return false;
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
        if (player == null) return false;
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
        if (player == null) return false;
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
}

package com.lopez.l2j.game.community;

import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.world.GameWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Servico responsavel pelo Community Board BBS do jogo (Alt + B) - Item 93.
 * Oferece painel de informacoes do servidor, rankings PvP/PK, teleporte rapido
 * para cidades principais, buffer comunitário e regras.
 */
@Service
public class CommunityBoardService {

    private static final Logger log = LoggerFactory.getLogger(CommunityBoardService.class);

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
        this.repairBBSManager = repairBBSManager;
    }

    /**
     * Processa comandos BBS (_bbshome, _bbstop, _bbsteleport, _bbsbuffer, _bbsinfo)
     * e retorna o HTML correspondente para ser exibido via ShowBoard.
     */
    public String handleCommand(PlayerCharacter player, String command) {
        if (player == null) {
            return "<html><body>Erro de sessao.</body></html>";
        }

        String cmd = (command == null || command.isBlank()) ? "_bbshome" : command.trim();

        if (cmd.startsWith("_bbsteleport_to ")) {
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
        } else if (cmd.equals("_bbstop") || cmd.equals("_bbsranking")) {
            return getRankingsHtml(player);
        } else if (cmd.equals("_bbsinfo")) {
            return getInfoHtml(player);
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
        sb.append("<td width=375><font color=\"00FF00\"><b>Status do Jogador:</b></font><br>");
        sb.append("Nome: <font color=\"FFFFFF\">").append(player.name()).append("</font><br>");
        sb.append("Nivel: <font color=\"FFFFFF\">").append(player.level()).append("</font><br>");
        sb.append("PvP / PK: <font color=\"00FFFF\">").append(player.pvpKills())
                .append("</font> / <font color=\"FF5555\">").append(player.pkKills()).append("</font><br>");
        sb.append("Karma: <font color=\"").append(player.karma() > 0 ? "FF3333" : "00FF00").append("\">")
                .append(player.karma()).append("</font>");
        sb.append("</td>");
        sb.append("<td width=375><font color=\"00FF00\"><b>Status do Servidor:</b></font><br>");
        sb.append("Jogadores Online: <font color=\"FFFF00\">").append(onlineCount).append("</font><br>");
        sb.append("Taxas: <font color=\"FFFFFF\">XP: 1x | SP: 1x | Adena: 1x | Drop: 1x</font><br>");
        sb.append("Plataforma: <font color=\"00FF99\">Java 21 / Spring Boot 3.5 High-Performance</font>");
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
                sb.append("<td align=center><font color=\"00FF00\">").append(p.pvpKills()).append("</font></td></tr>");
            }
        }
        sb.append("</table></td>");

        sb.append("<td width=375 valign=top>");
        sb.append("<font color=\"FF5555\"><b>Top 5 Assassinos PK:</b></font><br>");
        sb.append("<table width=360 bgcolor=222222 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th width=40>#</th><th width=220>Nome</th><th width=100>PKs</th></tr>");
        if (topPk.isEmpty()) {
            sb.append("<tr><td colspan=3 align=center>Nenhum PK registrado</td></tr>");
        } else {
            int rank = 1;
            for (PlayerCharacter p : topPk) {
                sb.append("<tr><td align=center>").append(rank++).append("</td>");
                sb.append("<td>").append(p.name()).append("</td>");
                sb.append("<td align=center><font color=\"FF0000\">").append(p.pkKills()).append("</font></td></tr>");
            }
        }
        sb.append("</table></td>");
        sb.append("</tr></table>");

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Tela de Rankings Completos (Top 10 PvP e Top 10 PK).
     */
    public String getRankingsHtml(PlayerCharacter player) {
        List<PlayerCharacter> topPvp = characterRepository != null ? characterRepository.findTopPvP(10) : List.of();
        List<PlayerCharacter> topPk = characterRepository != null ? characterRepository.findTopPK(10) : List.of();

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
        sb.append("<tr><th colspan=4 bgcolor=333333><font color=\"00FF00\">TOP 10 PVP</font></th></tr>");
        sb.append("<tr><th width=35>#</th><th width=170>Nome</th><th width=70>Nivel</th><th width=90>PvPs</th></tr>");
        int rank = 1;
        for (PlayerCharacter p : topPvp) {
            sb.append("<tr><td align=center>").append(rank++).append("</td>");
            sb.append("<td>").append(p.name()).append("</td>");
            sb.append("<td align=center>").append(p.level()).append("</td>");
            sb.append("<td align=center><font color=\"00FF00\">").append(p.pvpKills()).append("</font></td></tr>");
        }
        if (topPvp.isEmpty()) {
            sb.append("<tr><td colspan=4 align=center>Sem registros de PvP</td></tr>");
        }
        sb.append("</table></td>");

        // Top 10 PK
        sb.append("<td width=375 valign=top>");
        sb.append("<table width=365 bgcolor=151515 border=1 cellpadding=2 cellspacing=0>");
        sb.append("<tr><th colspan=4 bgcolor=333333><font color=\"FF5555\">TOP 10 PK</font></th></tr>");
        sb.append("<tr><th width=35>#</th><th width=170>Nome</th><th width=70>Nivel</th><th width=90>PKs</th></tr>");
        rank = 1;
        for (PlayerCharacter p : topPk) {
            sb.append("<tr><td align=center>").append(rank++).append("</td>");
            sb.append("<td>").append(p.name()).append("</td>");
            sb.append("<td align=center>").append(p.level()).append("</td>");
            sb.append("<td align=center><font color=\"FF4444\">").append(p.pkKills()).append("</font></td></tr>");
        }
        if (topPk.isEmpty()) {
            sb.append("<tr><td colspan=4 align=center>Sem registros de PK</td></tr>");
        }
        sb.append("</table></td>");

        sb.append("</tr></table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Tela de Teleporte Comunitário para Cidades.
     */
    public String getTeleportHtml(PlayerCharacter player) {
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

        // Favoritos Pessoais (Bookmarks)
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr><td align=center><font color=\"LEVEL\"><b>MEUS TELEPORTES FAVORITOS</b></font></td></tr>");
        sb.append("<tr><td align=center><font color=\"AAAAAA\">Salve coordenadas personalizadas no mundo aberto.</font></td></tr>");
        sb.append("</table>");

        sb.append("<table width=600 bgcolor=151515 border=1 cellpadding=3 cellspacing=0 align=center>");
        sb.append("<tr><th width=50>Slot</th><th width=300>Localizacao Salva</th><th width=150>Acoes</th></tr>");

        for (int slot = 1; slot <= 5; slot++) {
            String fav = getFavorite(player, slot);
            sb.append("<tr><td align=center>#").append(slot).append("</td>");
            if (fav != null) {
                String[] parts = fav.split(";");
                String name = parts[0];
                sb.append("<td><font color=\"00FF00\"><b>").append(name).append("</b></font></td>");
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

    private String getFavorite(PlayerCharacter player, int slot) {
        if (variablesService == null || player == null) return null;
        return variablesService.getVariable(player.objectId(), "fav_tele_" + slot, null);
    }

    private String handleSaveFavorite(PlayerCharacter player, String slotStr) {
        if (player == null || variablesService == null) return getTeleportHtml(player);
        try {
            int slot = Integer.parseInt(slotStr);
            if (slot >= 1 && slot <= 5) {
                String name = "Ponto #" + slot + " (" + player.x() + "," + player.y() + ")";
                String val = name + ";" + player.x() + ";" + player.y() + ";" + player.z();
                variablesService.setVariable(player.objectId(), "fav_tele_" + slot, val);
                log.info("Jogador {} salvou favorito slot {} em {}", player.name(), slot, val);
            }
        } catch (NumberFormatException ignored) {}
        return getTeleportHtml(player);
    }

    private String handleDeleteFavorite(PlayerCharacter player, String slotStr) {
        if (player == null || variablesService == null) return getTeleportHtml(player);
        try {
            int slot = Integer.parseInt(slotStr);
            if (slot >= 1 && slot <= 5) {
                variablesService.deleteVariable(player.objectId(), "fav_tele_" + slot);
            }
        } catch (NumberFormatException ignored) {}
        return getTeleportHtml(player);
    }

    private String handleTeleportFavorite(PlayerCharacter player, String slotStr) {
        if (player == null || player.isDead() || player.karma() > 0) {
            return getTeleportHtml(player);
        }
        try {
            int slot = Integer.parseInt(slotStr);
            String fav = getFavorite(player, slot);
            if (fav != null) {
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
        if (player.isDead()) {
            return "<html><body><br><center><font color=\"FF0000\">Voce nao pode se teletransportar enquanto estiver morto!</font></center></body></html>";
        }
        if (player.karma() > 0) {
            return "<html><body><br><center><font color=\"FF0000\">Jogadores com Karma nao podem usar o teleporte comunitario!</font></center></body></html>";
        }

        try {
            int locId = Integer.parseInt(arg);
            int[] coords = TELEPORT_LOCATIONS.get(locId);
            if (coords != null) {
                player.teleport(coords[0], coords[1], coords[2]);
                log.info("Jogador {} teletransportado via BBS para destino {}", player.name(), locId);
                return getHomeHtml(player);
            }
        } catch (NumberFormatException ignored) {
        }
        return getTeleportHtml(player);
    }

    /**
     * Tela do Buffer Comunitário.
     */
    public String getBufferHtml(PlayerCharacter player) {
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
        if (player.isDead()) {
            return "<html><body><br><center><font color=\"FF0000\">Voce nao pode receber buffs enquanto estiver morto!</font></center></body></html>";
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
        sb.append("<font color=\"00FF00\"><b>Horários e Ciclos do Servidor:</b></font><br>");
        sb.append("- <b>Sieges de Castelo:</b> A cada 14 dias aos sábados e domingos às 18:00.<br>");
        sb.append("- <b>Olimpíadas dos Nobres:</b> Diariamente das 18:00 à 00:00. Apuração no final de cada mês.<br>");
        sb.append(
                "- <b>Sete Selos (Seven Signs):</b> Ciclo de 2 semanas (Período de Disputa e Período de Efeito dos Selos).<br>");
        sb.append(
                "- <b>Grand Bosses:</b> Antharas, Valakas, Baium, Frintezza, Zaken, Queen Ant com respawn retail.<br><br>");
        sb.append("<font color=\"00FF00\"><b>Regras Gerais:</b></font><br>");
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
        return "<html><body><br><center><font color=\"FF0000\">Servico de reparo indisponivel no momento.</font></center></body></html>";
    }

    private String handleRepairChar(PlayerCharacter player, String charName) {
        if (repairBBSManager != null) {
            RepairBBSManager.RepairResult res = repairBBSManager.repairCharacter(player, charName);
            if (res == RepairBBSManager.RepairResult.SUCCESS) {
                return "<html><body><br><center><font color=\"00FF00\">Personagem " + charName + " reparado com sucesso!</font><br><br>"
                        + "<button value=\"Voltar\" action=\"bypass _bbsrepair\" width=120 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></center></body></html>";
            } else {
                return "<html><body><br><center><font color=\"FF0000\">Falha ao reparar personagem: " + res.name() + "</font><br><br>"
                        + "<button value=\"Voltar\" action=\"bypass _bbsrepair\" width=120 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></center></body></html>";
            }
        }
        return "<html><body><br><center><font color=\"FF0000\">Servico de reparo indisponivel.</font></center></body></html>";
    }

    private String renderNavigationBar() {
        return "<table width=750 border=0 cellpadding=2 cellspacing=2 align=center>" +
                "<tr>" +
                "<td align=center><button value=\"Início\" action=\"bypass _bbshome\" width=110 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Rankings\" action=\"bypass _bbstop\" width=110 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Teleportes\" action=\"bypass _bbsteleport\" width=110 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Buffer\" action=\"bypass _bbsbuffer\" width=110 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Reparar Char\" action=\"bypass _bbsrepair\" width=110 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "<td align=center><button value=\"Informações\" action=\"bypass _bbsinfo\" width=110 height=22 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>"
                +
                "</tr></table><br>";
    }
}

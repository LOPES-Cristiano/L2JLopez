package com.lopez.l2j.game.community;

import com.lopez.l2j.game.model.CharacterRepository;
import com.lopez.l2j.game.model.PlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Desencalhe de Personagens no BBS (RepairBBSManager).
 * Permite que um jogador conectado repare outro personagem travado da mesma conta,
 * redefinindo suas coordenadas para a cidade mais próxima (Dion: 17867, 170259, -3503)
 * e limpando estados anômalos.
 */
@Service
public class RepairBBSManager {

    private static final Logger log = LoggerFactory.getLogger(RepairBBSManager.class);

    public static final int REPAIR_X = 17867;
    public static final int REPAIR_Y = 170259;
    public static final int REPAIR_Z = -3503;

    private final CharacterRepository characterRepository;

    public RepairBBSManager(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    public enum RepairResult {
        SUCCESS,
        CANNOT_REPAIR_CURRENT_CHAR,
        CHAR_NOT_FOUND,
        NOT_SAME_ACCOUNT,
        CHAR_IN_JAIL
    }

    /**
     * Executa o reparo de um personagem da mesma conta.
     */
    public RepairResult repairCharacter(PlayerCharacter activePlayer, String targetCharName) {
        if (activePlayer == null || targetCharName == null || targetCharName.isBlank()) {
            return RepairResult.CHAR_NOT_FOUND;
        }

        if (activePlayer.getName().equalsIgnoreCase(targetCharName.trim())) {
            return RepairResult.CANNOT_REPAIR_CURRENT_CHAR;
        }

        if (characterRepository == null) {
            return RepairResult.CHAR_NOT_FOUND;
        }

        Optional<PlayerCharacter> optChar = characterRepository.findByName(targetCharName.trim());
        if (optChar.isEmpty()) {
            return RepairResult.CHAR_NOT_FOUND;
        }

        PlayerCharacter target = optChar.get();
        if (!target.accountName().equalsIgnoreCase(activePlayer.accountName())) {
            log.warn("RepairBBS: Tentativa de reparar char de outra conta! Ativo: {} [{}], Alvo: {} [{}]",
                    activePlayer.getName(), activePlayer.accountName(), target.getName(), target.accountName());
            return RepairResult.NOT_SAME_ACCOUNT;
        }

        // Reposiciona para a vila segura
        target.setX(REPAIR_X);
        target.setY(REPAIR_Y);
        target.setZ(REPAIR_Z);

        characterRepository.saveState(target, false);
        log.info("RepairBBS: Personagem {} da conta {} reparado com sucesso para as coordenadas ({}, {}, {}).",
                target.getName(), target.accountName(), REPAIR_X, REPAIR_Y, REPAIR_Z);

        return RepairResult.SUCCESS;
    }

    /**
     * Gera a tela HTML do painel de reparo com a lista de personagens da mesma conta.
     */
    public String buildRepairHtml(PlayerCharacter activePlayer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><br>");
        sb.append("<table width=750 border=0 cellpadding=2 cellspacing=2>");
        sb.append("<tr><td align=center><font color=\"LEVEL\" size=3><b>DESENCALHE E REPARO DE PERSONAGENS</b></font></td></tr>");
        sb.append("<tr><td align=center><font color=\"AAAAAA\">Se outro personagem da sua conta estiver travado no mundo, clique para destrava-lo.</font></td></tr>");
        sb.append("</table><br>");

        List<PlayerCharacter> accountChars = characterRepository != null
                ? characterRepository.findByAccount(activePlayer.accountName())
                : List.of();

        sb.append("<table width=500 bgcolor=111111 border=1 cellpadding=4 cellspacing=0 align=center>");
        sb.append("<tr><th width=250>Nome do Personagem</th><th width=100>Nivel</th><th width=150>Acao</th></tr>");

        int count = 0;
        for (PlayerCharacter c : accountChars) {
            if (c.getName().equalsIgnoreCase(activePlayer.getName())) {
                continue; // Personagem atual em uso
            }
            count++;
            sb.append("<tr>");
            sb.append("<td><font color=\"00FFFF\">").append(c.getName()).append("</font></td>");
            sb.append("<td align=center>").append(c.getLevel()).append("</td>");
            sb.append("<td align=center><button value=\"Reparar Char\" action=\"bypass _bbsrepair_char ")
              .append(c.getName()).append("\" width=120 height=21 back=\"L2UI_CH3.smallbutton2_over\" fore=\"L2UI_CH3.smallbutton2\"></td>");
            sb.append("</tr>");
        }

        if (count == 0) {
            sb.append("<tr><td colspan=3 align=center><font color=\"AAAAAA\">Nenhum outro personagem cadastrado nesta conta.</font></td></tr>");
        }

        sb.append("</table>");
        sb.append("</body></html>");
        return sb.toString();
    }
}

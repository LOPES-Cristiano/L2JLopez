package com.lopez.l2j.network.game.handler.bypass;

import com.lopez.l2j.game.autofarm.AutoFarmService;
import com.lopez.l2j.network.game.GameSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Handler modular para todos os bypasses da interface grafica do AutoFarm Premium.
 * Suporta comandos:
 * - voiced_autofarm ...
 * - voice_autofarm, voice_farmstart, voice_farmstop, voice_farmacp, voice_changeSkillType,
 *   voice_refreshSkills, voice_removeSkill, voice_addSkill, voice_addNewSkill,
 *   voice_editFarmOption, voice_editSummonSkills, voice_removeSummonSkill,
 *   voice_addSummonSkill, voice_addNewSummonSkill, voice_editSummonFarmOption,
 *   voice_autosummonfarm, voice_buyfarm, voice_buyfarmTime, voice_tryFreeTime, voice_expendLimit
 */
@Component
public class BypassAutoFarmHandler implements IBypassHandler {

	private final AutoFarmService autoFarmService;

	@Autowired
	public BypassAutoFarmHandler(@Autowired(required = false) AutoFarmService autoFarmService) {
		this.autoFarmService = autoFarmService;
	}

	@Override
	public boolean canHandle(String command) {
		if (command == null || command.isBlank()) {
			return false;
		}
		String lower = command.trim().toLowerCase(Locale.ROOT);
		return lower.startsWith("voiced_autofarm")
				|| lower.startsWith("voice_autofarm")
				|| lower.startsWith("voice_farm")
				|| lower.startsWith("voice_changeskilltype")
				|| lower.startsWith("voice_refreshskills")
				|| lower.startsWith("voice_removeskill")
				|| lower.startsWith("voice_addskill")
				|| lower.startsWith("voice_addnewskill")
				|| lower.startsWith("voice_editfarmoption")
				|| lower.startsWith("voice_editsummonskills")
				|| lower.startsWith("voice_removesummonskill")
				|| lower.startsWith("voice_addsummonskill")
				|| lower.startsWith("voice_addnewsummonskill")
				|| lower.startsWith("voice_editsummonfarmoption")
				|| lower.startsWith("voice_autosummonfarm")
				|| lower.startsWith("voice_buyfarm")
				|| lower.startsWith("voice_tryfreetime")
				|| lower.startsWith("voice_expendlimit");
	}

	@Override
	public boolean handleBypass(String command, GameSession session) {
		if (session == null || session.activeCharacter() == null || autoFarmService == null) {
			return false;
		}
		autoFarmService.handleCommand(session, command);
		return true;
	}
}

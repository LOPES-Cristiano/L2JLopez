package com.lopez.l2j.game.party;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.network.game.GameSession;
import com.lopez.l2j.network.game.packet.GameServerPacket;
import com.lopez.l2j.network.game.packet.GameServerPacket.PartySmallWindowAdd;
import com.lopez.l2j.network.game.packet.GameServerPacket.PartySmallWindowAll;
import com.lopez.l2j.network.game.packet.GameServerPacket.PartySmallWindowDelete;
import com.lopez.l2j.network.game.packet.GameServerPacket.PartySmallWindowDeleteAll;
import com.lopez.l2j.network.game.packet.GameServerPacket.PartySmallWindowUpdate;
import com.lopez.l2j.network.game.packet.GameServerPacket.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Representa um grupo (party) de jogadores no mundo, com controle de lider,
 * distribuicao de drops/itens e divisao de XP/SP com bonus de grupo.
 */
public final class Party {

	public static final int MAX_MEMBERS = 9;
	public static final double PARTY_RANGE = 1500.0;

	public static final int ITEM_LOOTER = 0;
	public static final int ITEM_RANDOM = 1;
	public static final int ITEM_RANDOM_SPOIL = 2;
	public static final int ITEM_ORDER = 3;
	public static final int ITEM_ORDER_SPOIL = 4;

	private static final double[] BONUS_EXP_SP = {
			1.0,  // 1 membro (sem bonus)
			1.30, // 2 membros
			1.39, // 3 membros
			1.50, // 4 membros
			1.54, // 5 membros
			1.58, // 6 membros
			1.63, // 7 membros
			1.67, // 8 membros
			1.71  // 9 membros
	};

	private int leaderObjectId;
	private final int itemDistribution;
	private final List<GameSession> members = new CopyOnWriteArrayList<>();

	public Party(GameSession leader, GameSession firstMember, int itemDistribution) {
		this.leaderObjectId = leader.character().objectId();
		this.itemDistribution = itemDistribution;
		members.add(leader);
		members.add(firstMember);
	}

	public int leaderObjectId() {
		return leaderObjectId;
	}

	public int itemDistribution() {
		return itemDistribution;
	}

	public List<GameSession> members() {
		return members;
	}

	public int size() {
		return members.size();
	}

	public boolean isFull() {
		return members.size() >= MAX_MEMBERS;
	}

	public boolean isLeader(int objectId) {
		return leaderObjectId == objectId;
	}

	public boolean contains(int objectId) {
		for (var s : members) {
			if (s.character() != null && s.character().objectId() == objectId) {
				return true;
			}
		}
		return false;
	}

	public List<PlayerCharacter> characters() {
		List<PlayerCharacter> list = new ArrayList<>(members.size());
		for (var s : members) {
			if (s.character() != null) {
				list.add(s.character());
			}
		}
		return list;
	}

	/** Adiciona um novo membro ao grupo e atualiza as janelas de party dos clientes. */
	public synchronized boolean addMember(GameSession session) {
		if (isFull() || contains(session.character().objectId())) {
			return false;
		}
		var newChar = session.character();
		// Notifica membros existentes sobre o novo membro
		broadcast(new PartySmallWindowAdd(leaderObjectId, itemDistribution, newChar));
		broadcast(SystemMessage.of(SystemMessage.S1_JOINED_PARTY, new SystemMessage.Text(newChar.name())));

		members.add(session);

		// Envia a janela completa com todos os membros para o novo membro
		session.send(new PartySmallWindowAll(leaderObjectId, itemDistribution, characters(), newChar.objectId()));
		session.send(SystemMessage.of(SystemMessage.YOU_JOINED_PARTY, new SystemMessage.Text(leaderName())));
		return true;
	}

	/** Remove um membro do grupo. Se sobrarem menos de 2 membros, desfaz o grupo. */
	public synchronized void removeMember(GameSession session) {
		if (!members.remove(session)) {
			return;
		}
		var leavingChar = session.character();
		session.send(new PartySmallWindowDeleteAll());

		if (leavingChar != null) {
			broadcast(new PartySmallWindowDelete(leavingChar.objectId(), leavingChar.name()));
			broadcast(SystemMessage.of(SystemMessage.S1_LEFT_PARTY, new SystemMessage.Text(leavingChar.name())));
		}

		if (members.size() < 2) {
			disband();
		} else if (leavingChar != null && leavingChar.objectId() == leaderObjectId) {
			// Transfere a lideranca para o proximo membro
			var newLeader = members.get(0).character();
			if (newLeader != null) {
				leaderObjectId = newLeader.objectId();
				refreshPartyWindow();
			}
		}
	}

	/** Desfaz o grupo completamente. */
	public synchronized void disband() {
		for (var s : members) {
			s.send(new PartySmallWindowDeleteAll());
			s.send(SystemMessage.id(SystemMessage.PARTY_DISPERSED));
			s.party(null);
		}
		members.clear();
	}

	public void refreshPartyWindow() {
		var chars = characters();
		for (var s : members) {
			if (s.character() != null) {
				s.send(new PartySmallWindowAll(leaderObjectId, itemDistribution, chars, s.character().objectId()));
			}
		}
	}

	public void broadcastMemberUpdate(PlayerCharacter member) {
		broadcast(new PartySmallWindowUpdate(member));
	}

	public void broadcast(GameServerPacket packet) {
		for (var s : members) {
			s.send(packet);
		}
	}

	public void broadcastExcept(GameServerPacket packet, int exceptObjectId) {
		for (var s : members) {
			if (s.character() != null && s.character().objectId() != exceptObjectId) {
				s.send(packet);
			}
		}
	}

	/**
	 * Divide a experiencia e SP entre os membros do grupo presentes no raio do monstro derrotado,
	 * aplicando o bonus de grupo proporcional a quantidade de jogadores e seus niveis.
	 */
	public void distributeExpAndSp(long totalExp, int totalSp, PlayerCharacter killer, double ratePartyXp,
			double ratePartySp) {
		List<GameSession> inRange = new ArrayList<>();
		for (var s : members) {
			var c = s.character();
			if (c != null && !c.isDead()) {
				double dist = Math.hypot(c.x() - killer.x(), c.y() - killer.y());
				if (dist <= PARTY_RANGE) {
					inRange.add(s);
				}
			}
		}

		if (inRange.isEmpty()) {
			return;
		}

		if (inRange.size() == 1) {
			inRange.get(0).applyExpAndSp(totalExp, totalSp);
			return;
		}

		int count = inRange.size();
		double bonusMultiplier = BONUS_EXP_SP[Math.min(count - 1, BONUS_EXP_SP.length - 1)];
		long partyExp = Math.round(totalExp * bonusMultiplier * ratePartyXp);
		int partySp = (int) Math.round(totalSp * bonusMultiplier * ratePartySp);

		long sumLevels = inRange.stream().mapToLong(s -> s.character().level()).sum();
		if (sumLevels <= 0) {
			sumLevels = count;
		}

		for (var s : inRange) {
			double share = (double) s.character().level() / sumLevels;
			long memberExp = Math.max(1, Math.round(partyExp * share));
			int memberSp = Math.max(1, (int) Math.round(partySp * share));
			s.applyExpAndSp(memberExp, memberSp);
		}
	}

	private String leaderName() {
		for (var s : members) {
			if (s.character() != null && s.character().objectId() == leaderObjectId) {
				return s.character().name();
			}
		}
		return "";
	}
}

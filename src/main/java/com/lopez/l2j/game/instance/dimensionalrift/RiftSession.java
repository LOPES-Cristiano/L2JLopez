package com.lopez.l2j.game.instance.dimensionalrift;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa a sessao ativa de uma party no Dimensional Rift.
 */
public class RiftSession {
    private final int partyLeaderId;
    private final RiftTier tier;
    private final List<Integer> memberIds = new ArrayList<>();
    private final Set<Integer> completedRooms = new HashSet<>();
    private int currentRoom = 1;
    private int jumpsCount = 0;
    private boolean manualJumpUsed = false;
    private boolean bossRoom = false;
    private boolean completed = false;

    public RiftSession(int partyLeaderId, RiftTier tier, List<Integer> initialMembers) {
        this.partyLeaderId = partyLeaderId;
        this.tier = tier;
        if (initialMembers != null) {
            this.memberIds.addAll(initialMembers);
        }
    }

    public int getPartyLeaderId() {
        return partyLeaderId;
    }

    public RiftTier getTier() {
        return tier;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }

    public Set<Integer> getCompletedRooms() {
        return completedRooms;
    }

    public int getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(int currentRoom) {
        this.currentRoom = currentRoom;
    }

    public int getJumpsCount() {
        return jumpsCount;
    }

    public void incrementJumps() {
        this.jumpsCount++;
    }

    public boolean isManualJumpUsed() {
        return manualJumpUsed;
    }

    public void setManualJumpUsed(boolean manualJumpUsed) {
        this.manualJumpUsed = manualJumpUsed;
    }

    public boolean isBossRoom() {
        return bossRoom;
    }

    public void setBossRoom(boolean bossRoom) {
        this.bossRoom = bossRoom;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}

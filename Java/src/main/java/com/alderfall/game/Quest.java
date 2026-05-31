package com.alderfall.game;

public final class Quest {
    public enum ObjectiveKind {
        DEFEAT,
        GATHER,
        VISIT
    }

    public final String id;
    public final String title;
    public final String description;
    public final String target;
    public final int needed;
    public final int rewardGold;
    public final int rewardXp;
    public final ObjectiveKind objectiveKind;
    public final String objectiveMapId;
    public final String objectiveLocationKind;
    public final int objectiveLocationIndex;
    public final String objectiveAsset;
    public final String monsterKey;
    public final String startDialog;
    public final String progressDialog;
    public final String readyDialog;
    public final String completeDialog;
    public int progress;
    public boolean accepted;
    public boolean completed;

    public Quest(String id, String title, String description, String target, int needed, int rewardGold, int rewardXp) {
        this(id, title, description, target, needed, rewardGold, rewardXp,
                ObjectiveKind.DEFEAT, WorldMap.OVERWORLD_ID, null, 0, null, null,
                description,
                "Stay with it. The work is not done yet.",
                "That should be enough. Come back and claim the reward.",
                "Good work. Alderfall will feel that.");
    }

    public Quest(
            String id,
            String title,
            String description,
            String target,
            int needed,
            int rewardGold,
            int rewardXp,
            ObjectiveKind objectiveKind,
            String objectiveMapId,
            String objectiveLocationKind,
            int objectiveLocationIndex,
            String objectiveAsset,
            String monsterKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.target = target;
        this.needed = needed;
        this.rewardGold = rewardGold;
        this.rewardXp = rewardXp;
        this.objectiveKind = objectiveKind;
        this.objectiveMapId = objectiveMapId;
        this.objectiveLocationKind = objectiveLocationKind;
        this.objectiveLocationIndex = objectiveLocationIndex;
        this.objectiveAsset = objectiveAsset;
        this.monsterKey = monsterKey;
        this.startDialog = startDialog;
        this.progressDialog = progressDialog;
        this.readyDialog = readyDialog;
        this.completeDialog = completeDialog;
    }

    public Quest copy() {
        Quest quest = new Quest(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, objectiveMapId, objectiveLocationKind, objectiveLocationIndex,
                objectiveAsset, monsterKey, startDialog, progressDialog, readyDialog, completeDialog);
        quest.progress = progress;
        quest.accepted = accepted;
        quest.completed = completed;
        return quest;
    }

    public void record(String defeatedTarget) {
        if (accepted && !completed && target.equals(defeatedTarget)) {
            progress = Math.min(needed, progress + 1);
        }
    }

    public void recordGather(String gatheredTarget) {
        if (accepted && !completed && objectiveKind == ObjectiveKind.GATHER && target.equals(gatheredTarget)) {
            progress = Math.min(needed, progress + 1);
        }
    }

    public void recordVisit(String visitedTarget) {
        if (accepted && !completed && objectiveKind == ObjectiveKind.VISIT && target.equals(visitedTarget)) {
            progress = Math.min(needed, progress + 1);
        }
    }

    public boolean ready() {
        return accepted && !completed && progress >= needed;
    }

    public boolean hasWorldObjective() {
        return accepted && !completed && progress < needed && objectiveLocationKind != null && !objectiveLocationKind.isBlank();
    }

    public String objectiveAction() {
        return switch (objectiveKind) {
            case GATHER -> "Gather";
            case VISIT -> "Inspect";
            case DEFEAT -> "Hunt";
        };
    }
}

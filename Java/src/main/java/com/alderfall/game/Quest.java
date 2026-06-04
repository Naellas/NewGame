package com.alderfall.game;

import java.util.List;

public final class Quest {
    public enum ObjectiveKind {
        DEFEAT,
        RESCUE,
        DEFEND,
        GATHER,
        DELIVER,
        VISIT,
        SEARCH,
        TALK,
        ASK_AROUND,
        REPORT,
        ESCORT,
        CHOICE;

        public boolean combatObjective() {
            return this == DEFEAT || this == RESCUE || this == DEFEND;
        }

        public boolean gatherObjective() {
            return this == GATHER;
        }

        public boolean markedObjective() {
            return this == GATHER || this == VISIT || this == SEARCH || this == ESCORT;
        }

        public boolean inspectObjective() {
            return this == VISIT || this == SEARCH || this == ESCORT;
        }

        public boolean conversationObjective() {
            return this == DELIVER || this == TALK || this == ASK_AROUND || this == REPORT || this == CHOICE;
        }
    }

    public enum QuestType {
        SIDE,
        COMPANION,
        MAIN_STORY
    }

    public record QuestStage(
            String id,
            String title,
            String target,
            int needed,
            ObjectiveKind objectiveKind,
            String objectiveMapId,
            String objectiveLocationKind,
            int objectiveLocationIndex,
            String objectiveAsset,
            String monsterKey,
            String targetNpcId,
            String branchOutcomeKey,
            String startDialog,
            String progressDialog,
            String readyDialog,
            String completeDialog
    ) {
        public QuestStage {
            id = id == null || id.isBlank() ? "stage" : id.strip();
            title = title == null ? "" : title.strip();
            target = target == null ? "" : target.strip();
            needed = Math.max(1, needed);
            objectiveKind = objectiveKind == null ? ObjectiveKind.VISIT : objectiveKind;
            objectiveMapId = objectiveMapId == null || objectiveMapId.isBlank() ? WorldMap.OVERWORLD_ID : objectiveMapId;
            objectiveLocationKind = objectiveLocationKind == null ? "" : objectiveLocationKind.strip();
            objectiveAsset = objectiveAsset == null ? "" : objectiveAsset.strip();
            monsterKey = monsterKey == null ? "" : monsterKey.strip();
            targetNpcId = targetNpcId == null ? "" : targetNpcId.strip();
            branchOutcomeKey = branchOutcomeKey == null ? "" : branchOutcomeKey.strip();
            startDialog = startDialog == null ? "" : startDialog.strip();
            progressDialog = progressDialog == null ? "" : progressDialog.strip();
            readyDialog = readyDialog == null ? "" : readyDialog.strip();
            completeDialog = completeDialog == null ? "" : completeDialog.strip();
        }
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
    public final String targetNpcId;
    public final String branchOutcomeKey;
    public final String startDialog;
    public final String progressDialog;
    public final String readyDialog;
    public final String completeDialog;
    public final QuestType type;
    public final String chainOwnerId;
    public final String nextQuestId;
    public final List<QuestStage> stages;
    public int stageIndex;
    public int progress;
    public boolean accepted;
    public boolean completed;

    public Quest(String id, String title, String description, String target, int needed, int rewardGold, int rewardXp) {
        this(id, title, description, target, needed, rewardGold, rewardXp,
                ObjectiveKind.DEFEAT, WorldMap.OVERWORLD_ID, null, 0, null, null,
                description,
                "Stay with it. The work is not done yet.",
                "That should be enough. Come back and claim the reward.",
                "Good work. Alderfall will feel that.",
                QuestType.SIDE, null, null, "", "");
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
        this(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, objectiveMapId, objectiveLocationKind, objectiveLocationIndex,
                objectiveAsset, monsterKey, startDialog, progressDialog, readyDialog, completeDialog,
                QuestType.SIDE, null, null, "", "");
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
            String completeDialog,
            QuestType type,
            String chainOwnerId,
            String nextQuestId
    ) {
        this(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, objectiveMapId, objectiveLocationKind, objectiveLocationIndex,
                objectiveAsset, monsterKey, startDialog, progressDialog, readyDialog, completeDialog,
                type, chainOwnerId, nextQuestId, "", "");
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
            String completeDialog,
            QuestType type,
            String chainOwnerId,
            String nextQuestId,
            String targetNpcId,
            String branchOutcomeKey
    ) {
        this(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, objectiveMapId, objectiveLocationKind, objectiveLocationIndex,
                objectiveAsset, monsterKey, startDialog, progressDialog, readyDialog, completeDialog,
                type, chainOwnerId, nextQuestId, targetNpcId, branchOutcomeKey, List.of());
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
            String completeDialog,
            QuestType type,
            String chainOwnerId,
            String nextQuestId,
            String targetNpcId,
            String branchOutcomeKey,
            List<QuestStage> stages
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
        this.targetNpcId = targetNpcId == null ? "" : targetNpcId;
        this.branchOutcomeKey = branchOutcomeKey == null ? "" : branchOutcomeKey;
        this.startDialog = startDialog;
        this.progressDialog = progressDialog;
        this.readyDialog = readyDialog;
        this.completeDialog = completeDialog;
        this.type = type == null ? QuestType.SIDE : type;
        this.chainOwnerId = chainOwnerId;
        this.nextQuestId = nextQuestId;
        this.stages = stages == null || stages.isEmpty() ? List.of(defaultStage()) : List.copyOf(stages);
    }

    public Quest copy() {
        Quest quest = new Quest(id, title, description, target, needed, rewardGold, rewardXp,
                objectiveKind, objectiveMapId, objectiveLocationKind, objectiveLocationIndex,
                objectiveAsset, monsterKey, startDialog, progressDialog, readyDialog, completeDialog,
                type, chainOwnerId, nextQuestId, targetNpcId, branchOutcomeKey, stages);
        quest.stageIndex = stageIndex;
        quest.progress = progress;
        quest.accepted = accepted;
        quest.completed = completed;
        return quest;
    }

    private QuestStage defaultStage() {
        return new QuestStage(
                id + "_stage_1",
                title,
                target,
                needed,
                objectiveKind,
                objectiveMapId,
                objectiveLocationKind,
                objectiveLocationIndex,
                objectiveAsset,
                monsterKey,
                targetNpcId,
                branchOutcomeKey,
                startDialog,
                progressDialog,
                readyDialog,
                completeDialog
        );
    }

    public QuestStage activeStage() {
        int index = Math.max(0, Math.min(stageIndex, stages.size() - 1));
        return stages.get(index);
    }

    public boolean stagedQuest() {
        return stages.size() > 1;
    }

    public boolean finalStage() {
        return stageIndex >= stages.size() - 1;
    }

    public void advanceStage() {
        if (!finalStage()) {
            stageIndex++;
            progress = 0;
        }
    }

    public ObjectiveKind activeObjectiveKind() {
        return activeStage().objectiveKind();
    }

    public String activeTarget() {
        return activeStage().target();
    }

    public int activeNeeded() {
        return activeStage().needed();
    }

    public String activeObjectiveMapId() {
        return activeStage().objectiveMapId();
    }

    public String activeObjectiveLocationKind() {
        return activeStage().objectiveLocationKind();
    }

    public int activeObjectiveLocationIndex() {
        return activeStage().objectiveLocationIndex();
    }

    public String activeObjectiveAsset() {
        return activeStage().objectiveAsset();
    }

    public String activeMonsterKey() {
        return activeStage().monsterKey();
    }

    public String activeTargetNpcId() {
        return activeStage().targetNpcId();
    }

    public String activeBranchOutcomeKey() {
        return activeStage().branchOutcomeKey();
    }

    public String activeStartDialog() {
        return activeStage().startDialog();
    }

    public String activeProgressDialog() {
        return activeStage().progressDialog();
    }

    public String activeReadyDialog() {
        return activeStage().readyDialog();
    }

    public String activeCompleteDialog() {
        return activeStage().completeDialog();
    }

    public void record(String defeatedTarget) {
        if (accepted && !completed && activeObjectiveKind().combatObjective() && combatTargetMatches(defeatedTarget)) {
            progress = Math.min(activeNeeded(), progress + 1);
        }
    }

    private boolean combatTargetMatches(String defeatedTarget) {
        if (activeTarget().equals(defeatedTarget)) {
            return true;
        }
        if (activeMonsterKey() == null || activeMonsterKey().isBlank()) {
            return false;
        }
        String defeatedKey = GameData.monsterKeyForName(defeatedTarget);
        return activeMonsterKey().equals(defeatedKey) || activeMonsterKey().equals(defeatedTarget);
    }

    public void recordGather(String gatheredTarget) {
        if (accepted && !completed && activeObjectiveKind().gatherObjective() && activeTarget().equals(gatheredTarget)) {
            progress = Math.min(activeNeeded(), progress + 1);
        }
    }

    public void recordVisit(String visitedTarget) {
        if (accepted && !completed && activeObjectiveKind().inspectObjective() && activeTarget().equals(visitedTarget)) {
            progress = Math.min(activeNeeded(), progress + 1);
        }
    }

    public void recordConversation() {
        if (accepted && !completed && activeObjectiveKind().conversationObjective()) {
            progress = Math.min(activeNeeded(), progress + 1);
        }
    }

    public boolean ready() {
        return accepted && !completed && progress >= activeNeeded();
    }

    public boolean hasWorldObjective() {
        return accepted
                && !completed
                && progress < activeNeeded()
                && activeObjectiveKind().markedObjective()
                && activeObjectiveLocationKind() != null
                && !activeObjectiveLocationKind().isBlank();
    }

    public String objectiveAction() {
        return switch (activeObjectiveKind()) {
            case GATHER -> "Gather";
            case DELIVER -> "Deliver";
            case VISIT -> "Inspect";
            case SEARCH -> "Search";
            case TALK -> "Talk";
            case ASK_AROUND -> "Ask around";
            case REPORT -> "Report";
            case ESCORT -> "Escort";
            case RESCUE -> "Rescue";
            case DEFEND -> "Defend";
            case CHOICE -> "Decide";
            case DEFEAT -> "Hunt";
        };
    }

    public boolean companionQuest() {
        return type == QuestType.COMPANION;
    }

    public boolean mainStoryQuest() {
        return type == QuestType.MAIN_STORY;
    }

    public String outcomeKey() {
        String key = activeBranchOutcomeKey();
        return key == null || key.isBlank() ? id : key;
    }
}

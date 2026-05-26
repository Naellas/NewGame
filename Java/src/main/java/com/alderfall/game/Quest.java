package com.alderfall.game;

public final class Quest {
    public final String id;
    public final String title;
    public final String description;
    public final String target;
    public final int needed;
    public final int rewardGold;
    public final int rewardXp;
    public int progress;
    public boolean accepted;
    public boolean completed;

    public Quest(String id, String title, String description, String target, int needed, int rewardGold, int rewardXp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.target = target;
        this.needed = needed;
        this.rewardGold = rewardGold;
        this.rewardXp = rewardXp;
    }

    public Quest copy() {
        Quest quest = new Quest(id, title, description, target, needed, rewardGold, rewardXp);
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

    public boolean ready() {
        return accepted && !completed && progress >= needed;
    }
}

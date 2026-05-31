package com.alderfall.game;

public final class BattleActionAnimation {
    private static final double SPEED_MULTIPLIER = 0.70;

    public enum Stage {
        CAST,
        TRAVEL,
        IMPACT,
        DONE
    }

    public final Actor source;
    public final Actor target;
    public final String effectKind;
    public final int castFrames;
    public final int travelFrames;
    public final int impactFrames;
    private int frame;
    private boolean impactTriggered;

    public BattleActionAnimation(Actor source, Actor target, String effectKind, int castFrames, int travelFrames, int impactFrames) {
        this.source = source;
        this.target = target;
        this.effectKind = effectKind == null || effectKind.isBlank() ? "strike" : effectKind;
        this.castFrames = scaledFrameCount(castFrames);
        this.travelFrames = scaledFrameCount(travelFrames);
        this.impactFrames = scaledFrameCount(impactFrames);
    }

    public void tick() {
        if (stage() != Stage.DONE) {
            frame++;
        }
    }

    public int frame() {
        return frame;
    }

    public int totalFrames() {
        return castFrames + travelFrames + impactFrames;
    }

    public int remainingFrames() {
        return Math.max(0, totalFrames() - frame);
    }

    public int releaseRemainingFrames() {
        if (!released()) {
            return 0;
        }
        return Math.max(0, castFrames + travelFrames + impactFrames - frame);
    }

    public boolean released() {
        return frame >= castFrames && stage() != Stage.DONE;
    }

    public Stage stage() {
        if (frame < castFrames) {
            return Stage.CAST;
        }
        if (frame < castFrames + travelFrames) {
            return Stage.TRAVEL;
        }
        if (frame < castFrames + travelFrames + impactFrames) {
            return Stage.IMPACT;
        }
        return Stage.DONE;
    }

    public boolean shouldTriggerImpact() {
        return !impactTriggered && stage() == Stage.IMPACT;
    }

    public void markImpactTriggered() {
        impactTriggered = true;
    }

    public double castProgress() {
        return clamp(frame / (double) castFrames);
    }

    public double travelProgress() {
        return clamp((frame - castFrames) / (double) travelFrames);
    }

    public double impactProgress() {
        return clamp((frame - castFrames - travelFrames) / (double) impactFrames);
    }

    public boolean involves(Actor actor) {
        return actor != null && (actor == source || actor == target);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private int scaledFrameCount(int frames) {
        return Math.max(1, (int) Math.round(Math.max(1, frames) * SPEED_MULTIPLIER));
    }
}

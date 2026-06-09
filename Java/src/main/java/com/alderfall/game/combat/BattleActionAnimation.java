package com.alderfall.game;

import java.util.ArrayList;
import java.util.List;

public final class BattleActionAnimation {
    private static final double SPEED_MULTIPLIER = 1.10;

    public enum Stage {
        CAST,
        TRAVEL,
        IMPACT,
        DONE
    }

    public enum VisualMode {
        SINGLE,
        CHAIN,
        MULTI,
        AOE
    }

    public final Actor source;
    public final Actor target;
    public final List<Actor> targets;
    public final VisualMode visualMode;
    public final String effectKind;
    public final int castFrames;
    public final int travelFrames;
    public final int impactFrames;
    private int frame;
    private final boolean[] stepTriggered;
    private boolean impactTriggered;

    public BattleActionAnimation(Actor source, Actor target, String effectKind, int castFrames, int travelFrames, int impactFrames) {
        this(source, target, List.of(target), VisualMode.SINGLE, effectKind, castFrames, travelFrames, impactFrames);
    }

    public BattleActionAnimation(Actor source, Actor target, List<Actor> targets, VisualMode visualMode, String effectKind, int castFrames, int travelFrames, int impactFrames) {
        this.source = source;
        this.target = target;
        ArrayList<Actor> normalizedTargets = new ArrayList<>();
        if (targets != null) {
            for (Actor candidate : targets) {
                if (candidate != null && !normalizedTargets.contains(candidate)) {
                    normalizedTargets.add(candidate);
                }
            }
        }
        if (normalizedTargets.isEmpty() && target != null) {
            normalizedTargets.add(target);
        }
        this.targets = List.copyOf(normalizedTargets);
        this.visualMode = visualMode == null ? VisualMode.SINGLE : visualMode;
        this.effectKind = effectKind == null || effectKind.isBlank() ? "strike" : effectKind;
        this.castFrames = scaledFrameCount(castFrames);
        this.travelFrames = scaledFrameCount(travelFrames);
        this.impactFrames = scaledFrameCount(impactFrames);
        this.stepTriggered = new boolean[this.targets.size()];
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

    public int triggeredStepCount() {
        int count = 0;
        for (boolean triggered : stepTriggered) {
            if (triggered) {
                count++;
            }
        }
        return count;
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

    public List<AbilityResolutionStep> pendingImpactSteps() {
        ArrayList<AbilityResolutionStep> pending = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) {
            if (!stepTriggered[i] && stepReady(i)) {
                pending.add(new AbilityResolutionStep(i, targets.size(), source, targets.get(i), visualMode, stage(), eventProgress()));
            }
        }
        return pending;
    }

    public void markStepTriggered(int index) {
        if (index >= 0 && index < stepTriggered.length) {
            stepTriggered[index] = true;
        }
        if (allStepsTriggered()) {
            impactTriggered = true;
        }
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
        return actor != null && (actor == source || targets.contains(actor));
    }

    private boolean allStepsTriggered() {
        for (boolean triggered : stepTriggered) {
            if (!triggered) {
                return false;
            }
        }
        return true;
    }

    private boolean stepReady(int index) {
        int count = Math.max(1, targets.size());
        return switch (visualMode) {
            case CHAIN -> frame >= castFrames + Math.max(1, (int) Math.round(travelFrames * ((index + 1) / (double) count)));
            case MULTI -> frame >= castFrames + travelFrames + Math.max(0, (int) Math.round(impactFrames * (index / (double) count)));
            case AOE, SINGLE -> stage() == Stage.IMPACT || stage() == Stage.DONE;
        };
    }

    private double eventProgress() {
        int releaseFrames = Math.max(1, travelFrames + impactFrames);
        return clamp((frame - castFrames) / (double) releaseFrames);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private int scaledFrameCount(int frames) {
        return Math.max(1, (int) Math.round(Math.max(1, frames) * SPEED_MULTIPLIER));
    }
}

package com.alderfall.game;

public final class EffectStack {
    public final StatusEffect effect;
    public int turnsRemaining;
    public int power;

    public EffectStack(StatusEffect effect) {
        this(effect, effect.duration(), effect.power());
    }

    public EffectStack(StatusEffect effect, int turnsRemaining, int power) {
        this.effect = effect;
        this.turnsRemaining = turnsRemaining;
        this.power = power == 0 ? effect.power() : power;
    }

    public boolean tick() {
        turnsRemaining--;
        return turnsRemaining > 0;
    }

    public void refresh(Integer newPower) {
        turnsRemaining = effect.duration();
        if (newPower != null) {
            power = Math.max(power, newPower);
        }
    }
}

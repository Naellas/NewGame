package com.alderfall.game;

import java.util.List;
import java.util.Locale;

/** Shared deterministic quality calculation used by recipes and the workshop preview. */
public final class CraftingCalculation {
    private CraftingCalculation() {}
    public record Aptitude(String primary, String secondary, int primaryValue, int secondaryValue) {
        public double bonus() { return Math.min(30, primaryValue * 0.6 + secondaryValue * 0.4); }
    }
    public record Result(Profession profession, int level, int training, Aptitude aptitude, int tier,
                         int difficulty, double score, AssemblyCrafting.Quality quality) {
        public List<String> lines() {
            return List.of(profession.label() + ": " + level + " + " + training + " training",
                    "Profession: 10 x (" + level + " + " + training + ") = " + (10 * (level + training)),
                    "Attributes: 0.6 x " + aptitude.primary() + " " + aptitude.primaryValue(),
                    "          + 0.4 x " + aptitude.secondary() + " " + aptitude.secondaryValue(),
                    "Attribute bonus: +" + number(aptitude.bonus()) + " (cap 30)",
                    "Tier " + tier + " difficulty: -" + difficulty,
                    "Score: " + number(score) + " -> " + quality.label);
        }
        public String nextQuality() {
            int next = quality.ordinal() + 1;
            if (next >= AssemblyCrafting.Quality.values().length) return "Highest craftsmanship reached";
            AssemblyCrafting.Quality q = AssemblyCrafting.Quality.values()[next];
            return q.label + " at " + q.skill * 10 + " (need " + number(q.skill * 10 - score) + ")";
        }
    }
    public static Aptitude aptitude(Actor actor, Profession profession) {
        return switch (profession) {
            case SMITHING, ARMORCRAFT -> new Aptitude("STR", "CON", actor.strength, actor.constitution);
            case CARPENTRY -> new Aptitude("DEX", "STR", actor.dexterity, actor.strength);
            case TAILORING, WEAVING, JEWELLERY -> new Aptitude("DEX", "INT", actor.dexterity, actor.intelligence);
            case LEATHERWORKING -> new Aptitude("DEX", "CON", actor.dexterity, actor.constitution);
            case ALCHEMY -> new Aptitude("INT", "WIL", actor.intelligence, actor.willpower);
            default -> new Aptitude("DEX", "INT", actor.dexterity, actor.intelligence);
        };
    }
    public static Result calculate(Actor actor, Profession profession, int materialTier) {
        int tier = Math.max(1, Math.min(5, materialTier));
        int level = actor.professionLevel(profession.id());
        int training = actor.professionPracticeBonus(profession.id());
        Aptitude aptitude = aptitude(actor, profession);
        int difficulty = 4 * (tier - 1);
        double score = Math.max(0, 10 * (level + training) + aptitude.bonus() - difficulty);
        AssemblyCrafting.Quality quality = AssemblyCrafting.Quality.STANDARD;
        for (AssemblyCrafting.Quality candidate : AssemblyCrafting.Quality.values())
            if (score >= candidate.skill * 10) quality = candidate;
        return new Result(profession, level, training, aptitude, tier, difficulty, score, quality);
    }
    public static String number(double value) { return String.format(Locale.ROOT, "%.1f", value); }
}

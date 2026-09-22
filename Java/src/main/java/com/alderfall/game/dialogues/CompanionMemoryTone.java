package com.alderfall.game;

final class CompanionMemoryTone {
    private CompanionMemoryTone() {
    }

    static String describe(String outcome) {
        return switch (outcome) {
            case "truth" -> "you chose to name the truth before comfort";
            case "protect" -> "you chose to shield the living thing until it could stand";
            case "mercy" -> "you chose mercy without pretending the wound had vanished";
            case "accountability" -> "you chose consequences before easy softness";
            case "clause_copied" -> "you copied the red clause before the court could bury it again";
            case "public_record" -> "you forced the false seal into the public record";
            case "witness_first" -> "you hid the evidence until the witness could survive it";
            case "witness_protected" -> "you protected the clerk until his testimony could stand";
            case "testimony_public" -> "you made the clerk's testimony public before fear could edit it";
            case "leverage_traded" -> "you traded a frightened clerk's name for court access";
            case "survival_named" -> "you named desperation without calling it consent";
            case "legal_lie_named" -> "you called the contract legal enough to wound and false enough to fight";
            case "blame_signed" -> "you blamed the family for signing inside a trap";
            case "ledger_published" -> "you burned her contract and published the ledger";
            case "names_reclaimed" -> "you kept the records where victims could reclaim their names";
            case "safety_bargain" -> "you traded the records for immediate safety";
            case "records_burned" -> "you burned every record before anyone else could use it";
            case "refuge_ledger" -> "you built the public ledger desk at Oathstead";
            case "witness_bench" -> "you put witness days before any new oath";
            case "chosen_daily" -> "you left the promise open enough for Seraphine to choose it daily";
            default -> "you chose " + (outcome == null || outcome.isBlank()
                    ? "patience"
                    : outcome.replace('_', ' '));
        };
    }
}

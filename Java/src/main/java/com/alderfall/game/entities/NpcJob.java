package com.alderfall.game;

public record NpcJob(
        Kind kind,
        String roleLabel,
        int shiftStartMinutes,
        int shiftEndMinutes
) {
    public enum Kind {
        FARMER,
        WOODCUTTER,
        HERBALIST
    }

    public NpcJob {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        roleLabel = roleLabel == null || roleLabel.isBlank() ? defaultRoleLabel(kind) : roleLabel.strip();
        shiftStartMinutes = Math.max(0, Math.min(1439, shiftStartMinutes));
        shiftEndMinutes = Math.max(1, Math.min(1440, shiftEndMinutes));
        if (shiftEndMinutes <= shiftStartMinutes) {
            shiftEndMinutes = Math.min(1440, shiftStartMinutes + 1);
        }
    }

    public boolean activeAt(int minutes) {
        return minutes >= shiftStartMinutes && minutes < shiftEndMinutes;
    }

    public static NpcJob farmer() {
        return new NpcJob(Kind.FARMER, "Fieldhand", 390, 1110);
    }

    public static NpcJob woodcutter() {
        return new NpcJob(Kind.WOODCUTTER, "Woodworker", 420, 1080);
    }

    public static NpcJob herbalist() {
        return new NpcJob(Kind.HERBALIST, "Herbalist", 450, 1050);
    }

    private static String defaultRoleLabel(Kind kind) {
        return switch (kind) {
            case FARMER -> "Fieldhand";
            case WOODCUTTER -> "Woodworker";
            case HERBALIST -> "Herbalist";
        };
    }
}

package com.alderfall.game;

/** A gait continues across tile/direction changes and settles at the next planted step. */
final class LocomotionClock {
    static final int WALK_FRAMES = WorldCharacterAnimation.FRAMES;
    static final int CYCLE_TICKS = 40;
    record Selection(String action, int frame) { }
    private int lastTick = Integer.MIN_VALUE;
    private double phase;
    private boolean moving;
    private int startTicks = 6;
    private int settleTicks = 6;
    private double plantPhase;
    private boolean finishing;
    private boolean lastRequestedMoving;
    private Selection lastSelection;
    static final double STRIDE_TILES = .75;
    private double previousX = Double.NaN, previousY, distancePhase;
    private int positionTick = Integer.MIN_VALUE, settleStart = Integer.MIN_VALUE, stoppedPose;
    private boolean travelled;
    private Selection positionSelection;
    private final String walkingAction;

    LocomotionClock() { this(configuredStyle()); }
    LocomotionClock(boolean alternate) { this(alternate ? "alternate" : "original"); }
    LocomotionClock(String style) {
        walkingAction = switch (style) {
            case "original" -> "walk";
            case "alternate" -> "walk_alternate";
            case "articulated" -> "walk_articulated";
            default -> "walk_grounded";
        };
    }
    private static String configuredStyle() {
        String style = System.getProperty("alderfall.movementStyle");
        if (style != null) return style;
        String legacy = System.getProperty("alderfall.alternateMovement");
        return legacy == null ? "grounded" : Boolean.parseBoolean(legacy) ? "alternate" : "original";
    }

    /** A requested slow cadence must not force a planted leg beyond its anatomical reach. */
    static double groundedCadence(double requested,double dx,double dy) {
        double sum=Math.abs(dx)+Math.abs(dy);
        double lateral=sum<.0001?1:Math.abs(dx)/sum;
        double minimum=.5+.4*Math.max(0,2*lateral-1);
        return Math.max(minimum,GameConfig.clampWalkAnimationSpeed(requested));
    }

    // Front/rear views need shorter steps to fit the projected leg reach.
    static double strideTiles(double dx,double dy) {
        double distance=Math.hypot(dx,dy);
        return distance<.0001?STRIDE_TILES:STRIDE_TILES*distance/(Math.abs(dx)+2*Math.abs(dy));
    }

    /** Advance from interpolated world position, never from held keys or elapsed time. */
    Selection selectAt(int tick, double x, double y) {
        return selectAt(tick, x, y, 1.0);
    }

    Selection selectAt(int tick, double x, double y, double animationSpeed) {
        if (tick == positionTick && positionSelection != null) return positionSelection;
        double stepX=x-previousX,stepY=y-previousY;
        double distance = Double.isNaN(previousX) ? 0 : Math.hypot(x - previousX, y - previousY);
        previousX = x; previousY = y; positionTick = tick;
        if (distance > 2) { distance = 0; distancePhase = 0; travelled = false; settleStart = Integer.MIN_VALUE; }
        if (distance > .0001) {
            distancePhase = (distancePhase + distance * WALK_FRAMES / (walkingAction.equals("walk_grounded")?strideTiles(stepX,stepY):STRIDE_TILES)
                    * (walkingAction.equals("walk_grounded")?groundedCadence(animationSpeed,stepX,stepY):GameConfig.clampWalkAnimationSpeed(animationSpeed))) % WALK_FRAMES;
            travelled = true;
            return positionSelection = new Selection(walkingAction, (int) distancePhase);
        }
        if (travelled) {
            travelled = false; stoppedPose = (int) distancePhase; settleStart = tick;
        }
        if (settleStart != Integer.MIN_VALUE && tick - settleStart <= 8)
            return positionSelection = new Selection((walkingAction.equals("walk") ? "settle_" : "settle_" + walkingAction.substring(5) + "_") + stoppedPose,
                    Math.min(11, (tick - settleStart) * 11 / 8));
        return positionSelection = new Selection("idle", Math.floorMod(tick / 5, 24));
    }

    Selection select(int tick, boolean requestedMoving) {
        if (tick == lastTick && requestedMoving == lastRequestedMoving && lastSelection != null) return lastSelection;
        lastRequestedMoving = requestedMoving;
        lastSelection = advance(tick, requestedMoving);
        return lastSelection;
    }

    private Selection advance(int tick, boolean requestedMoving) {
        int dt = lastTick == Integer.MIN_VALUE ? 0 : Math.max(0, Math.min(4, tick - lastTick));
        lastTick = tick;
        if (requestedMoving) {
            if (!moving && !finishing && settleTicks >= 6) { phase = 0; startTicks = 0; }
            moving = true; finishing = false; settleTicks = 0;
            phase += dt * WALK_FRAMES / (double) CYCLE_TICKS;
            startTicks += dt;
            if (startTicks < 6) return new Selection("start_walk", Math.min(11, startTicks * 11 / 5));
            return new Selection("walk", Math.floorMod((int) phase, WALK_FRAMES));
        }
        if (moving) {
            moving = false; finishing = true;
            plantPhase = Math.ceil(phase / (WALK_FRAMES / 2.0)) * (WALK_FRAMES / 2.0);
            settleTicks = 0;
        }
        if (finishing) {
            phase = Math.min(plantPhase, phase + dt * WALK_FRAMES / (double) CYCLE_TICKS);
            if (phase < plantPhase) return new Selection("walk", Math.floorMod((int) phase, WALK_FRAMES));
            finishing = false;
        }
        if (settleTicks < 6) {
            int offset = Math.floorMod((int) plantPhase, WALK_FRAMES) >= 12 ? 12 : 0;
            Selection result = new Selection("stop_walk", offset + Math.min(11, settleTicks * 11 / 5));
            settleTicks += dt;
            return result;
        }
        return new Selection("idle", Math.floorMod(tick / 6, WALK_FRAMES));
    }

    void reset() {
        lastTick = Integer.MIN_VALUE; phase = 0; moving = false; finishing = false; startTicks = 6; settleTicks = 6;
        lastSelection = null;
        previousX = Double.NaN; distancePhase = 0; positionTick = Integer.MIN_VALUE;
        settleStart = Integer.MIN_VALUE; travelled = false; positionSelection = null;
    }
}

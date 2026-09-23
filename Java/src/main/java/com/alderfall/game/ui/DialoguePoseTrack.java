package com.alderfall.game.ui;

/** Timed rotation keyframes. Head and chest pivot around joints without position drift. */
public final class DialoguePoseTrack {
    public record Pose(double x, double y, double angle, double shoulder) {
        static final Pose REST = new Pose(0,0,0,0);
        Pose mix(Pose other, double t) {
            return new Pose(x+(other.x-x)*t,y+(other.y-y)*t,
                    angle+(other.angle-angle)*t,shoulder+(other.shoulder-shoulder)*t);
        }
    }
    private record Frame(long ms, Pose pose) {}
    private static Frame key(long ms, double degrees, double shoulder) {
        return new Frame(ms,new Pose(0,0,Math.toRadians(degrees),shoulder));
    }
    private static final Frame[] SPEAK = {
        key(0,0,0), key(280,-.5,-.3), key(620,1.4,1.3),
        key(1000,-.4,.5), key(1500,-1.2,1.5),
        key(2050,-1,1), key(2540,.5,.4), key(3100,0,0),
        key(3600,0,0)
    };
    private static final Frame[] LISTEN = {
        key(0,0,0), key(650,-1.3,.6), key(1250,-1.6,1),
        key(1780,-.3,1), key(2200,-1.2,.6),
        key(3200,-.7,.4), key(4200,0,0), key(4800,0,0)
    };
    private int mode;
    private long started, changed, last = -1;
    private Pose from = Pose.REST;

    public Pose update(long ms, boolean speaking, boolean listening) {
        int next = speaking ? 1 : listening ? 2 : 0;
        if (last < 0 || ms < last || ms-last > 500) {
            mode = next; started = changed = ms; from = Pose.REST;
        } else if (next != mode) {
            from = current(ms); mode = next; started = changed = ms;
        }
        last = ms;
        return current(ms);
    }

    private Pose current(long ms) {
        Pose target = sample(mode,ms-started);
        double t = Math.min(1,Math.max(0,(ms-changed)/220.0));
        return from.mix(target,t*t*(3-2*t));
    }

    public static Pose sample(int mode, long elapsedMs) {
        if (mode == 0) return Pose.REST;
        Frame[] frames = mode == 1 ? SPEAK : LISTEN;
        long time = Math.floorMod(elapsedMs,frames[frames.length-1].ms);
        int i = 0;
        while (i+1 < frames.length-1 && time >= frames[i+1].ms) i++;
        Frame a = frames[i], b = frames[i+1];
        Frame before = frames[Math.max(0,i-1)], after = frames[Math.min(frames.length-1,i+2)];
        double duration = b.ms-a.ms, t = (time-a.ms)/duration;
        return new Pose(curve(before,a,b,after,t,duration,0),curve(before,a,b,after,t,duration,1),
                curve(before,a,b,after,t,duration,2),curve(before,a,b,after,t,duration,3));
    }

    private static double value(Frame frame, int channel) {
        return switch(channel) { case 0 -> frame.pose.x; case 1 -> frame.pose.y;
            case 2 -> frame.pose.angle; default -> frame.pose.shoulder; };
    }
    private static double curve(Frame before, Frame a, Frame b, Frame after, double t, double duration, int channel) {
        double av=value(a,channel), bv=value(b,channel);
        if (av == bv) return av; // Deliberate rests have no spline overshoot.
        double m0 = before == a ? 0 : (bv-value(before,channel))/(b.ms-before.ms)*duration;
        double m1 = after == b ? 0 : (value(after,channel)-av)/(after.ms-a.ms)*duration;
        double t2=t*t,t3=t2*t;
        return (2*t3-3*t2+1)*av+(t3-2*t2+t)*m0+(-2*t3+3*t2)*bv+(t3-t2)*m1;
    }
}

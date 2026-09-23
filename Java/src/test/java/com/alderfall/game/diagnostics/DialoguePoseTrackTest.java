package com.alderfall.game;

import com.alderfall.game.ui.DialoguePoseTrack;

public final class DialoguePoseTrackTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static double distance(DialoguePoseTrack.Pose a, DialoguePoseTrack.Pose b) {
        return Math.abs(a.x()-b.x())+Math.abs(a.y()-b.y())+Math.abs(a.angle()-b.angle())+Math.abs(a.shoulder()-b.shoulder());
    }
    public static void main(String[] args) {
        for (int mode=1;mode<=2;mode++) {
            for (long ms=1;ms<10000;ms++) {
                var a=DialoguePoseTrack.sample(mode,ms-1);
                var b=DialoguePoseTrack.sample(mode,ms);
                check(distance(a,b)<.06,"Discontinuous keyframe at " + ms);
                check(b.x()==0 && b.y()==0,"Head has an independent sliding offset");
                check(Math.abs(b.angle())<.04 && Math.abs(b.x())<5 && Math.abs(b.y())<4,"Unbounded head pose");
            }
        }
        check(distance(DialoguePoseTrack.sample(1,510),DialoguePoseTrack.sample(1,526))>.0001,
                "Motion held across consecutive display frames");
        DialoguePoseTrack first=new DialoguePoseTrack(), second=new DialoguePoseTrack();
        first.update(0,true,false); second.update(0,true,false);
        for(int ms=16;ms<1000;ms+=16) first.update(ms,true,false);
        for(int ms=33;ms<1000;ms+=33) second.update(ms,true,false);
        var previous=first.update(1000,true,false);
        check(distance(previous,second.update(1000,true,false))<1e-10,"Motion depends on frame rate");
        check(distance(previous,first.update(1000,false,true))<1e-10,"Speaking/listening transition jumped");
        check(distance(first.update(1100,false,true),first.update(1100,false,true))==0,"Repeated update drifted");
        first.update(1200,false,false);
        check(distance(first.update(1450,false,false),DialoguePoseTrack.sample(0,0))==0,"Did not settle to rest");
        System.out.println("DialoguePoseTrackTest passed: continuous keyframes, per-frame motion, rate independence and transitions.");
    }
}

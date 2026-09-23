package com.alderfall.game.ui;

/** A whole-figure parent transform with local, planted foot constraints. */
public final class DialogueBodyMotion {
    /** Parent indices precede children; attachment physics is solved in this bind space. */
    public record Node(String name,int parent,double x,double y) {}
    public static Node[] skeleton(Rig rig,double faceX,double faceY,double mouthY) {
        Foot[] feet=rig.feet();
        return new Node[]{new Node("ground",-1,rig.centreX(),rig.groundY()),
            new Node("pelvis",0,faceX,470),new Node("chest",1,faceX,mouthY+135),
            new Node("neck",2,faceX,mouthY+30),new Node("head",3,faceX,faceY+15),
            new Node("left foot",0,feet[0].x()+feet[0].width()/2,rig.groundY()),
            new Node("right foot",0,feet[feet.length-1].x()+feet[feet.length-1].width()/2,rig.groundY())};
    }
    /** Vertical capsule, used only to block NEW crossings of a painted body boundary. */
    public record Collider(double x,double top,double bottom,double radius) {
        public double constrain(double restX,double y,double offset) {
            double dy=Math.max(0,Math.max(top-y,y-bottom));
            if(dy>=radius)return offset;
            double extent=Math.sqrt(radius*radius-dy*dy),left=x-extent,right=x+extent;
            // Existing painted overlap is intentional layering, not penetration to resolve.
            if(restX>left && restX<right)return offset;
            return restX<=left ? Math.min(offset,left-restX) : Math.max(offset,right-restX);
        }
    }
    public static Collider[] colliders(String sprite) {
        return switch(sprite) {
            case "npc_aria_dialogue_sprite" -> new Collider[]{new Collider(274,210,430,55),new Collider(208,590,900,35),new Collider(400,590,930,36)};
            case "npc_vesper_dialogue_sprite" -> new Collider[]{new Collider(230,255,465,48),new Collider(180,630,920,30),new Collider(318,630,925,30)};
            case "class_mage_dialogue_sprite" -> new Collider[]{new Collider(247,220,445,61),new Collider(180,650,925,32),new Collider(346,650,925,32)};
            default -> new Collider[0];
        };
    }
    public record Foot(double x, double y, double width, double height) {
        public boolean contains(double px,double py) {
            return px>=x && px<=x+width && py>=y && py<=y+height;
        }
        double pin(double px,double py) {
            double dx=Math.max(0,Math.max(x-px,px-x-width))/35;
            double dy=Math.max(0,Math.max(y-py,py-y-height))/(py<y ? 145 : 25);
            double t=Math.min(1,Math.sqrt(dx*dx+dy*dy));
            return 1-t*t*(3-2*t);
        }
    }
    public record Rig(double centreX, double groundY, Foot[] feet) {
        public Rig { feet=feet.clone(); }
        @Override public Foot[] feet(){return feet.clone();}
        public double pin(double x,double y){double value=0;for(Foot foot:feet)value=Math.max(value,foot.pin(x,y));return value;}
        public boolean planted(double x,double y){for(Foot foot:feet)if(foot.contains(x,y))return true;return false;}
    }
    public record Pose(double centreX,double groundY,double cosine,double sine) {
        public double sourceX(double x,double y,double pin) {
            return x+(1-pin)*(centreX+cosine*(x-centreX)+sine*(y-groundY)-x);
        }
        public double sourceY(double x,double y,double pin) {
            return y+(1-pin)*(groundY-sine*(x-centreX)+cosine*(y-groundY)-y);
        }
    }
    public static Rig rig(String sprite,double fallbackCentre,double artWidth) {
        return switch(sprite) {
            case "npc_aria_dialogue_sprite" -> new Rig(292,985,new Foot[]{new Foot(143,902,98,70),new Foot(374,928,106,72)});
            case "npc_vesper_dialogue_sprite" -> new Rig(233,985,new Foot[]{new Foot(136,925,91,75),new Foot(287,918,102,82)});
            case "class_mage_dialogue_sprite" -> new Rig(248,985,new Foot[]{new Foot(132,925,90,75),new Foot(305,923,103,77)});
            // Unknown stances retain a conservative foot band until individually mapped.
            default -> new Rig(fallbackCentre,985,new Foot[]{new Foot(0,890,artWidth,110)});
        };
    }
    public static Pose pose(Rig rig,DialogueAnimationLayers.Idle idle,double shoulder,int direction) {
        double angle=idle.sway()*1.4 + Math.toRadians(shoulder*.06)*direction;
        return new Pose(rig.centreX,rig.groundY,Math.cos(angle),Math.sin(angle));
    }
}

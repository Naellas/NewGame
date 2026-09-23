package com.alderfall.game.ui;

/** Parallel idle, gesture and bounded secondary motion. No simulation state enters saves. */
public final class DialogueAnimationLayers {
    public record Settings(boolean breathing, boolean blinking, boolean sway,
                           boolean gestures, boolean hands, boolean secondary, boolean softTissue) {
        public static final Settings STANDARD=new Settings(true,true,true,true,true,true,true);
        public static final Settings REDUCED=new Settings(true,true,false,false,false,false,false);
    }
    public record Idle(double breath, double sway, double breathingTilt) {}
    public static Idle idle(String sprite, double seconds, Settings settings) {
        double phase=Math.floorMod(sprite.hashCode(),997)/997.0*Math.PI*2;
        double period=4.0+Math.floorMod(sprite.hashCode(),13)*.08;
        double breath=settings.breathing ? .5+.5*Math.sin(seconds*Math.PI*2/period+phase) : 0;
        return new Idle(breath,settings.sway ? Math.sin(seconds*Math.PI*2/6.8+phase)*.0013 : 0,
                settings.breathing ? (breath-.5)*.0008 : 0);
    }
    private final String sprite;
    private final DialogueRigProfile profile;
    private final double[] position, velocity;
    private final double[][] chain, chainVelocity;
    private final double[][] nodeX,nodeY;
    private final DialogueBodyMotion.Collider[] colliders;
    private double clock, previousHead;
    private long last=-1;
    public DialogueAnimationLayers(String sprite, DialogueRigProfile profile) {
        this.sprite=sprite;this.profile=profile;
        position=new double[profile.parts().size()];velocity=new double[position.length];
        chain=new double[position.length][7];chainVelocity=new double[position.length][7];
        nodeX=new double[position.length][7];nodeY=new double[position.length][7];
        colliders=DialogueBodyMotion.colliders(sprite);
        for(int i=0;i<position.length;i++) {
            var part=profile.parts().get(i);double[] polygon=part.polygon();
            double bottom=part.pivotY(),tipX=part.pivotX();
            for(int p=1;p<polygon.length;p+=2)if(polygon[p]>bottom){bottom=polygon[p];tipX=polygon[p-1];}
            for(int n=0;n<7;n++){nodeX[i][n]=part.pivotX()+(tipX-part.pivotX())*n/6.0;nodeY[i][n]=part.pivotY()+(bottom-part.pivotY())*n/6.0;}
        }
    }
    public double[] update(long ms, double headAngle, boolean speaking, Settings settings) {
        if(last<0 || ms<last || ms-last>500) {
            java.util.Arrays.fill(position,0);java.util.Arrays.fill(velocity,0);
            for(int i=0;i<chain.length;i++){java.util.Arrays.fill(chain[i],0);java.util.Arrays.fill(chainVelocity[i],0);}
            clock=ms/1000.0;previousHead=headAngle;last=ms;
            return position.clone();
        }
        double now=ms/1000.0, step=1.0/120;
        while(clock+step<=now+1e-9) {
            clock+=step;
            Idle idle=idle(sprite,clock,settings);
            double parent=previousHead+(headAngle-previousHead)*Math.min(1,(clock-last/1000.0)/Math.max(.001,now-last/1000.0));
            for(int i=0;i<position.length;i++) {
                var part=profile.parts().get(i);
                boolean enabled=switch(part.kind()) {
                    case HAND -> settings.hands;
                    case SOFT_TISSUE -> settings.softTissue;
                    default -> settings.secondary;
                };
                if(!enabled){position[i]=velocity[i]=0;java.util.Arrays.fill(chain[i],0);java.util.Arrays.fill(chainVelocity[i],0);continue;}
                double target=switch(part.kind()) {
                    case HAIR -> -parent*18 + idle.sway*180;
                    case CLOTH -> idle.sway*220 + (idle.breath-.5)*.15;
                    case EQUIPMENT -> -parent*9 + idle.sway*200;
                    case HAND -> (speaking ? .75 : .16)*Math.sin(clock*2.2+i);
                    case SOFT_TISSUE -> (idle.breath-.5)*part.limit()*.75 + parent*3 + idle.sway*160;
                };
                target=Math.max(-part.limit(),Math.min(part.limit(),target));
                double frequency=part.kind()==DialogueRigProfile.Kind.SOFT_TISSUE ? 17 : part.kind()==DialogueRigProfile.Kind.HAND ? 10 : 8;
                double damping=part.kind()==DialogueRigProfile.Kind.SOFT_TISSUE ? .98 : .82;
                velocity[i]+=(frequency*frequency*(target-position[i])-2*damping*frequency*velocity[i])*step;
                position[i]+=velocity[i]*step;
                if(Math.abs(position[i])>part.limit()){position[i]=Math.copySign(part.limit(),position[i]);velocity[i]=0;}
                if(part.kind()==DialogueRigProfile.Kind.HAIR || part.kind()==DialogueRigProfile.Kind.CLOTH) {
                    // As in the movement-physics study: seven coupled horizontal
                    // nodes, a pinned root, damping, and continuous sampling between nodes.
                    chain[i][0]=chainVelocity[i][0]=0;
                    for(int node=1;node<7;node++) {
                        double amount=node/6.0;
                        double neighbours=(chain[i][node-1]+chain[i][Math.min(6,node+1)])*.5;
                        double wind=Math.sin(clock*1.8+i*.8-node*.15)*part.limit()*.16;
                        double force=(target*1.4+wind)*amount;
                        chainVelocity[i][node]+=(45*(force-chain[i][node])+70*(neighbours-chain[i][node])-12*chainVelocity[i][node])*step;
                        chain[i][node]+=chainVelocity[i][node]*step;
                        double limit=part.limit()*2.5*amount;
                        if(Math.abs(chain[i][node])>limit){chain[i][node]=Math.copySign(limit,chain[i][node]);chainVelocity[i][node]=0;}
                        for(var collider:colliders) {
                            double constrained=collider.constrain(nodeX[i][node],nodeY[i][node],chain[i][node]);
                            if(constrained!=chain[i][node]){chain[i][node]=constrained;chainVelocity[i][node]=0;}
                        }
                    }
                }
            }
        }
        previousHead=headAngle;last=ms;
        return position.clone();
    }
    public double[][] chainOffsets() {
        double[][] copy=new double[chain.length][];
        for(int i=0;i<chain.length;i++)copy[i]=chain[i].clone();
        return copy;
    }
}

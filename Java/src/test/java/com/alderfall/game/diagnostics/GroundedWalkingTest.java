package com.alderfall.game;

import java.nio.file.Path;

/** Physical gait checks independent of the rendered contact-sheet audit. */
public final class GroundedWalkingTest {
    public static void main(String[] args) {
        int count=0;
        for(String actor:CharacterAnimationAudit.ACTORS) {
            AssetStore assets=new AssetStore(Path.of("assets"));
            for(String direction:CharacterAnimationAudit.DIRECTIONS) {
                String name=actor+"_model_"+direction;
                int dx=direction.endsWith("left")?-1:direction.endsWith("right")?1:0;
                int dy=direction.startsWith("up")?-1:direction.startsWith("down")?1:0;
                double magnitude=Math.hypot(dx,dy);
                for(double speed:new double[]{.5,1,2}) {
                    var rig=new GroundedWalkingMotion(assets.spriteFit(name,192,144),dx,actor.matches("class_(mage|cleric)|npc_(lyra|maera|samir|vesper)"),name).configure(speed,66);
                    double distance=LocomotionClock.strideTiles(dx,dy)*48*144/66.0/LocomotionClock.groundedCadence(speed,dx,dy);
                    for(int leg=0;leg<2;leg++) {
                        double anchorX=Double.NaN,anchorY=Double.NaN;
                        for(int f=0;f<32;f++) {
                            var p=rig.leg(f,leg);
                            require(Double.isFinite(p.kneeX())&&Double.isFinite(p.kneeY()),"Invalid knee "+name);
                            if(dy==0)require(Math.abs(p.ankleX()-p.hipX())<30,"Side leg overextended at requested "+speed+": "+name);
                            double t=(f/32.0+leg*.5)%1;
                            if(t>=.6){anchorX=Double.NaN;continue;}
                            double x=p.ankleX()+dx/magnitude*distance*f/32.0;
                            double y=p.ankleY()+dy/magnitude*distance*f/32.0;
                            if(Double.isNaN(anchorX)){anchorX=x;anchorY=y;}
                            require(Math.abs(x-anchorX)<1e-7&&Math.abs(y-anchorY)<1e-7,"Sliding contact "+name);
                        }
                    }
                    count++;
                }
                require(assets.animatedSpriteFrameCount(name,"walk_grounded",192,144)==32,"Missing grounded action");
                var standing=assets.animatedSpriteFit(name,"idle",192,144,0);
                var settled=assets.animatedSpriteFit(name,"settle_grounded_10_66_8",192,144,11);
                require(java.util.Arrays.equals(standing.getRGB(0,0,192,144,null,0,192),settled.getRGB(0,0,192,144,null,0,192)),"Settle did not reach standing "+name);
            }
        }
        require(new LocomotionClock("grounded").selectAt(0,0,0).action().equals("idle"),"Spawn must stand");
        for(String style:new String[]{"grounded","articulated","alternate","original"}) {
            var gait=new LocomotionClock(style);gait.selectAt(0,0,0);
            String expected=style.equals("original")?"walk":"walk_"+style;
            require(gait.selectAt(1,.1,0).action().equals(expected),"Lost movement style "+style);
        }
        System.out.println("Grounded checks passed: "+count+" actor/direction/cadence combinations, two-axis contact, finite joints, settling and all four styles.");
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}

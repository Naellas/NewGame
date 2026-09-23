package com.alderfall.game;

import com.alderfall.game.ui.DialogueAnimationLayers;
import com.alderfall.game.ui.DialogueRigProfile;
import com.alderfall.game.ui.DialogueBodyMotion;
import java.util.Arrays;

public final class DialogueAnimationLayersTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args) {
        var settings=DialogueAnimationLayers.Settings.STANDARD;
        var collider=new DialogueBodyMotion.Collider(100,200,400,30);
        check(collider.constrain(60,300,20)==10,"Collider allowed entry from left");
        check(collider.constrain(140,300,-20)==-10,"Collider allowed entry from right");
        check(collider.constrain(100,300,2)==2,"Collider displaced existing painted overlap");
        check(collider.constrain(60,450,20)==20,"Collider blocked unrelated space");
        var body=DialogueBodyMotion.rig("npc_aria_dialogue_sprite",292,592);
        var nodes=DialogueBodyMotion.skeleton(body,290,80,113);
        for(int i=0;i<nodes.length;i++)check(nodes[i].parent()<i,"Cyclic skeleton");
        var bodyPose=DialogueBodyMotion.pose(body,new DialogueAnimationLayers.Idle(.5,.001,0),1,1);
        check(bodyPose.sourceX(200,700,body.pin(200,700))!=200,"Lower body is frozen");
        check(bodyPose.sourceX(190,940,body.pin(190,940))==190,"Pinned foot moved");
        var profile=DialogueRigProfile.forSprite("npc_aria_dialogue_sprite");
        var lips=profile.lips();
        check(Math.abs(lips.y(lips.leftX())-lips.leftY())<1e-9,"Left lip corner drift");
        check(Math.abs(lips.y(lips.middleX())-lips.middleY())<1e-9,"Middle lip seam drift");
        check(Math.abs(lips.y(lips.rightX())-lips.rightY())<1e-9,"Right lip corner drift");
        check(DialogueRigProfile.forSprite("class_knight_dialogue_sprite").parts().isEmpty(),"Uncalibrated armour gained soft physics");
        var first=DialogueAnimationLayers.idle("npc_aria_dialogue_sprite",0,settings);
        var next=DialogueAnimationLayers.idle("npc_aria_dialogue_sprite",1,settings);
        check(first.breath()!=next.breath() && first.sway()!=next.sway(),"Idle channels stopped without speech");
        check(!first.equals(DialogueAnimationLayers.idle("npc_vesper_dialogue_sprite",0,settings)),"Idle phases synchronized");
        boolean secondaryMoved=false,handMoved=false,softMoved=false;
        for(String sprite:new String[]{"npc_aria_dialogue_sprite","npc_vesper_dialogue_sprite","class_mage_dialogue_sprite"}) {
            var rig=DialogueRigProfile.forSprite(sprite);var physics=new DialogueAnimationLayers(sprite,rig);
            for(int ms=0;ms<12000;ms+=16) {
                double[] pose=physics.update(ms,Math.sin(ms*.003)*.025,ms<6000,settings);
                double[][] chains=physics.chainOffsets();
                for(int part=0;part<chains.length;part++) {
                    check(chains[part][0]==0,"Hair/cloth root detached");
                    for(double node:chains[part])check(Double.isFinite(node)&&Math.abs(node)<=rig.parts().get(part).limit()*2.5+1e-9,"Unbounded chain node");
                }
                check(Arrays.equals(pose,physics.update(ms,Math.sin(ms*.003)*.025,ms<6000,settings)),"Repeated repaint advanced physics");
                for(int i=0;i<pose.length;i++) {
                    var part=rig.parts().get(i);
                    check(Double.isFinite(pose[i])&&Math.abs(pose[i])<=part.limit()+1e-9,"Attachment escaped its limit");
                    if(ms>7000&&part.kind()==DialogueRigProfile.Kind.HAIR&&Math.abs(pose[i])>.001)secondaryMoved=true;
                    if(part.kind()==DialogueRigProfile.Kind.HAND&&Math.abs(pose[i])>.05)handMoved=true;
                    if(part.kind()==DialogueRigProfile.Kind.SOFT_TISSUE&&Math.abs(pose[i])>.005)softMoved=true;
                }
            }
            double[] reduced=physics.update(12000,0,false,DialogueAnimationLayers.Settings.REDUCED);
            check(Arrays.stream(reduced).allMatch(v->v==0),"Reduced motion did not disable attachments");
            check(Arrays.stream(physics.update(15000,0,false,settings)).allMatch(v->v==0),"Long pause failed to reset springs");
        }
        check(secondaryMoved&&handMoved&&softMoved,"A requested layer never animated");
        var a=new DialogueAnimationLayers("npc_aria_dialogue_sprite",profile);
        var b=new DialogueAnimationLayers("npc_aria_dialogue_sprite",profile);
        a.update(0,0,false,settings);b.update(0,0,false,settings);
        for(int ms=16;ms<480;ms+=16)a.update(ms,0,false,settings);
        for(int ms=40;ms<480;ms+=40)b.update(ms,0,false,settings);
        var pa=a.update(480,0,false,settings);var pb=b.update(480,0,false,settings);
        for(int i=0;i<pa.length;i++)check(Math.abs(pa[i]-pb[i])<1e-8,"Idle physics depends on repaint rate");
        System.out.println("DialogueAnimationLayersTest passed: lip anchors, parallel idle, bounded attachments, reduced motion and fixed-step stability.");
    }
}

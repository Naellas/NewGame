package com.alderfall.game;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/** Review-only profiles. A missing direction deliberately keeps the existing renderer. */
final class MovementLimbProfile {
    enum Surface { EXPOSED, ARMOR, HIDDEN_BY_ROBE }
    private static final double[] LEVELS={0,.22,.5,.68,1};
    private static final Map<String,MovementLimbProfile> PROFILES=Map.of(
        "npc_seraphine_model_right",new MovementLimbProfile("Seraphine / exposed leg pilot",Surface.EXPOSED,.55,new double[]{11,12,8,10,6},.90,.06,.3,6),
        "class_knight_model_right",new MovementLimbProfile("Knight / armor pilot",Surface.ARMOR,Double.NaN,new double[]{13,14,10,12,8},.88,.105,1,9),
        "class_mage_model_right",new MovementLimbProfile("Mage / covered leg pilot",Surface.HIDDEN_BY_ROBE,Double.NaN,new double[]{10,11,8,9,6},.90,.105,1,9)
    );
    final String label;
    final Surface surface;
    final double pelvisFraction,farShade,reachFraction,lengthSlack,swingLift;
    private final double[] widths;
    private MovementLimbProfile(String label,Surface surface,double pelvisFraction,double[] widths,double farShade,double reachFraction,double lengthSlack,double swingLift) {
        this.label=label;this.surface=surface;this.pelvisFraction=pelvisFraction;this.widths=widths.clone();
        this.farShade=farShade;this.reachFraction=reachFraction;this.lengthSlack=lengthSlack;this.swingLift=swingLift;
        if(widths.length!=LEVELS.length)throw new IllegalArgumentException("Missing limb contour landmarks");
        for(double w:widths)if(!Double.isFinite(w)||w<2||w>24)throw new IllegalArgumentException("Invalid limb contour");
    }
    private static final Map<String,MovementLimbProfile> CANDIDATES=new HashMap<>();
    static MovementLimbProfile find(String sprite){return PROFILES.containsKey(sprite)?PROFILES.get(sprite):CANDIDATES.get(sprite);}
    static MovementLimbProfile candidate(String sprite,Surface surface,double pelvisFraction,double[] widths) {
        var p=new MovementLimbProfile(sprite+" / measured candidate",surface,pelvisFraction,widths,.90,.085,.7,7);
        CANDIDATES.put(sprite,p);return find(sprite);
    }
    double[] contour(){return widths.clone();}
    static Set<String> sprites(){return PROFILES.keySet();}
    double diameter(double u) {
        u=Math.max(0,Math.min(1,u));
        for(int i=1;i<LEVELS.length;i++)if(u<=LEVELS[i]) {
            double t=(u-LEVELS[i-1])/(LEVELS[i]-LEVELS[i-1]);return widths[i-1]+(widths[i]-widths[i-1])*t;
        }
        return widths[widths.length-1];
    }
    double maximumWidth(){double max=0;for(double w:widths)max=Math.max(max,w);return max;}
    double colliderRadius(){return maximumWidth()/2+1;}
    double pelvis(double fallback,int canvasWidth){return Double.isNaN(pelvisFraction)?fallback:pelvisFraction*canvasWidth;}
}

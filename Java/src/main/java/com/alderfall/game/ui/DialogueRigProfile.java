package com.alderfall.game.ui;

import java.util.List;

/** Art-specific attachments. Coordinates use the alpha-cropped art at a height of 1000. */
public record DialogueRigProfile(Lips lips, List<Part> parts) {
    public DialogueRigProfile { parts=List.copyOf(parts); }
    public enum Kind { HAIR, CLOTH, EQUIPMENT, HAND, SOFT_TISSUE }
    public record Lips(double leftX, double leftY, double middleX, double middleY,
                       double rightX, double rightY) {
        public double y(double x) {
            // Quadratic through all three painted seam landmarks, including a tilted smile.
            double t=(x-leftX)/(rightX-leftX), m=(middleX-leftX)/(rightX-leftX);
            return leftY*(t-m)*(t-1)/m + middleY*t*(t-1)/(m*(m-1)) + rightY*t*(t-m)/(1-m);
        }
    }
    public record Part(String name, Kind kind, double pivotX, double pivotY,
                       double limit, double feather, double[] polygon) {
        public Part { polygon=polygon.clone(); }
        @Override public double[] polygon() { return polygon.clone(); }
    }
    private static Part part(String name, Kind kind, double px, double py, double limit, double... xy) {
        return new Part(name,kind,px,py,limit,7,xy);
    }
    private static final DialogueRigProfile BASE = new DialogueRigProfile(null,List.of());
    private static final DialogueRigProfile ARIA = new DialogueRigProfile(
        new Lips(282,111.3,294,111,305.5,107.8), List.of(
            part("Left hair",Kind.HAIR,239,87,1.1, 245,61,224,105,179,147,142,208,126,281,160,311,203,259,223,204,254,150),
            part("Belt leaves",Kind.EQUIPMENT,318,408,1.2, 309,410,333,418,350,465,342,490,318,466,306,436),
            part("Free left hand",Kind.HAND,137,459,1.6, 119,454,145,455,155,492,141,521,118,515,108,496),
            part("Left breast - fitted bodice",Kind.SOFT_TISSUE,242,195,.9, 222,201,248,191,263,211,257,246,236,254,217,234),
            part("Right breast - fitted bodice",Kind.SOFT_TISSUE,282,190,.9, 266,193,293,183,309,205,311,232,293,253,266,247),
            part("Left cape hem",Kind.CLOTH,157,403,1.6, 111,449,80,508,33,592,3,683,13,733,57,754,105,657,157,514),
            part("Right cape hem",Kind.CLOTH,437,425,1.6, 433,426,462,457,525,532,580,684,577,754,524,815,481,711,451,570)
        ));
    private static final DialogueRigProfile VESPER = new DialogueRigProfile(
        new Lips(212,162,221,161.4,229,158.6), List.of(
            part("Right hair",Kind.HAIR,287,149,1.0, 275,141,310,170,367,205,404,262,435,316,446,367,423,416,399,376,407,329,383,277,341,237,300,211),
            part("Loose feather edge",Kind.CLOTH,353,320,.7, 345,320,379,350,399,417,414,453,394,480,373,435,357,396),
            part("Waist pendant",Kind.EQUIPMENT,241,332,.8, 218,343,253,341,262,368,249,397,222,392,212,368),
            part("Free right hand",Kind.HAND,330,479,1.5, 318,475,343,482,345,507,334,531,319,539,309,521),
            part("Left breast - supported fabric",Kind.SOFT_TISSUE,194,232,1.3, 178,241,197,226,216,243,215,278,193,286,175,267),
            part("Right breast - supported fabric",Kind.SOFT_TISSUE,237,227,1.3, 218,231,247,226,262,253,254,277,239,292,218,279),
            part("Long feathered mantle",Kind.CLOTH,356,424,1.4, 379,475,415,549,422,689,438,808,410,911,370,847,345,711,359,537)
        ));
    private static final DialogueRigProfile MAGE = new DialogueRigProfile(
        new Lips(220,109,230,110,239,107.5), List.of(
            part("Hanging focus",Kind.EQUIPMENT,264,343,.9, 255,354,284,360,310,389,312,426,290,441,270,417,259,387),
            part("Free right hand",Kind.HAND,345,468,1.4, 331,463,359,470,357,501,340,527,319,528,321,503),
            part("Cape edge",Kind.CLOTH,395,314,.7, 397,314,418,354,439,421,432,472,417,496,406,449,402,391),
            part("Long right robe",Kind.CLOTH,387,426,1.5, 413,439,457,573,490,761,489,841,433,866,392,827,383,651),
            part("Long left robe",Kind.CLOTH,153,390,1.5, 125,394,86,574,17,773,42,829,120,878,165,743,192,520)
        ));

    public static DialogueRigProfile forSprite(String sprite) {
        return switch(sprite) {
            case "npc_aria_dialogue_sprite" -> ARIA;
            case "npc_vesper_dialogue_sprite" -> VESPER;
            case "class_mage_dialogue_sprite" -> MAGE;
            default -> BASE; // Never infer a free hand or a soft chest from an unknown outfit.
        };
    }
}

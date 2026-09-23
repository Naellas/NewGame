package com.alderfall.game.ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small, art-aligned facial poses and a foot-anchored idle for dialogue cutouts. */
public final class DialogueFigureAnimation {
    private final DialogueAnimationLayers.Settings settings;
    public DialogueFigureAnimation() { this(Boolean.getBoolean("alderfall.dialogueReducedMotion") ? DialogueAnimationLayers.Settings.REDUCED : DialogueAnimationLayers.Settings.STANDARD); }
    public DialogueFigureAnimation(DialogueAnimationLayers.Settings settings) {
        this.settings=java.util.Objects.requireNonNull(settings);
    }
    // Landmarks in alpha-cropped artwork, normalized to a height of 1000.
    private record Face(double lx, double ly, double rx, double ry, double mx, double my, double eyeW, double mouthW) {}
    private static final Map<String, Face> FACES = Map.ofEntries(
        face("npc_aria", 271,84,309,77,292,113,22,23),
        face("npc_calder", 255,62,283,56,273,89,19,22),
        face("npc_cassia", 262,83,298,78,284,116,21,22),
        face("npc_lyra", 251,84,290,78,274,117,22,25),
        face("npc_maera", 259,82,295,76,280,110,21,20),
        face("npc_rafiq", 310,80,342,72,330,110,21,25),
        face("npc_samir", 200,76,232,69,221,108,20,24),
        face("npc_seraphine", 242,85,280,77,265,115,22,24),
        face("npc_vesper", 197,140,233,132,220,161,20,17),
        face("class_cleric", 230,125,264,119,250,150,19,21),
        face("class_knight", 218,80,255,79,238,110,22,23),
        face("class_mage", 211,82,244,79,230,109,20,20),
        face("class_ranger", 234,83,270,80,253,111,19,21),
        face("class_rogue", 215,93,248,85,239,119,19,19),
        face("npc_story_ash_scribe_damar", 257,84,285,87,266,114,17,22),
        face("npc_story_bellwright_nessa", 166,109,195,106,183,137,18,21),
        face("npc_story_captain_elric_snowrest", 301,77,333,73,318,105,18,21),
        face("npc_story_elder_rowan", 295,74,329,77,313,107,18,19),
        face("npc_story_gravekeeper_hollis", 269,80,303,78,287,116,20,23),
        face("npc_story_maelis", 364,94,398,97,382,126,20,21),
        face("npc_story_mirella", 212,93,249,88,232,125,20,23),
        face("npc_story_odrick", 257,77,287,73,274,111,19,22),
        face("npc_story_selene", 272,96,310,98,290,130,21,19),
        face("npc_story_solari", 249,109,283,115,273,150,19,21),
        face("npc_story_vaelthara", 266,143,294,142,280,173,18,20),
        face("npc_story_ysra", 205,92,232,88,225,126,19,22)
    );
    private record Key(BufferedImage source, String sprite, int blink, int mouth) {}
    private final Map<Key, BufferedImage> poses = new LinkedHashMap<>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, BufferedImage> entry) { return size() > 48; }
    };
    private final Map<BufferedImage,Rectangle> portraitBounds=new LinkedHashMap<>(8,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<BufferedImage,Rectangle> entry){return size()>4;}
    };
    /** Party/banter framing of the same rig; mouth and gestures use the caller's reveal snapshot. */
    public void drawPortrait(Graphics2D graphics,BufferedImage source,String sprite,Rectangle target,
                             long timeMs,int mouth,boolean speaking,boolean listening,int direction) {
        if(target.width<=0 || target.height<=0)return;
        Rectangle b=portraitBounds.computeIfAbsent(source,DialogueFigureAnimation::bounds);
        Face face=FACES.get(sprite);
        double scale=b.height/1000.0;
        double centre=b.x+(face==null ? b.width*.5 : (face.lx+face.rx)*.5*scale);
        double top=b.y+(face==null ? 0 : Math.max(0,Math.min(face.ly,face.ry)-100)*scale);
        double cropHeight=Math.max(1,300*scale),cropWidth=cropHeight*target.width/target.height;
        Graphics2D g=(Graphics2D)graphics.create();
        g.clip(target);g.translate(target.x,target.y);g.scale(target.width/cropWidth,target.height/cropHeight);
        g.translate(-(centre-cropWidth*.5),-top);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        draw(g,source,sprite,0,0,timeMs,mouth,speaking,listening,direction);g.dispose();
    }
    private static final class Rig {
        BufferedImage source, rendered;
        Rectangle bounds;
        DialogueRigProfile profile;
        DialogueAnimationLayers layers;
        DialoguePartMask mask;
        DialogueBodyMotion.Rig body;
        DialogueBodyMotion.Node[] skeleton;
        float[] footPin;
        int[] firstPixel,lastPixel;
        long frame = -1;
        final DialoguePoseTrack track = new DialoguePoseTrack();
        DialoguePoseTrack.Pose pose;
        int mouth = -1, blink = -1;
    }
    private final Map<String, Rig> rigs = new LinkedHashMap<>(8, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Rig> entry) { return size() > 4; }
    };

    private static Map.Entry<String, Face> face(String id, double lx, double ly, double rx, double ry,
                                               double mx, double my, double ew, double mw) {
        return Map.entry(id + "_dialogue_sprite", new Face(lx,ly,rx,ry,mx,my,ew,mw));
    }

    /** Speech is gated by the same reveal snapshot used to render the text. */
    public static int mouthPose(String speech, boolean complete, long timeMs) {
        if (complete || speech == null || speech.isEmpty()
                || !Character.isLetterOrDigit(speech.codePointBefore(speech.length()))) return 0;
        return switch ((int) Math.floorMod(timeMs / 75, 4)) { case 1, 3 -> 1; case 2 -> 2; default -> 0; };
    }

    public static int blinkPose(String sprite, long timeMs) {
        long phase = Math.floorMod(timeMs + Math.floorMod(sprite.hashCode(), 4100), 4100);
        return phase < 45 || (phase >= 125 && phase < 170) ? 1 : phase < 125 ? 2 : 0;
    }

    public void draw(Graphics2D g, BufferedImage source, String sprite, int x, int y,
                     long timeMs, int mouth) {
        draw(g, source, sprite, x, y, timeMs, mouth, false, false, 0);
    }

    /** partnerDirection is +1 for the left figure, -1 for the right figure. */
    public void draw(Graphics2D g, BufferedImage source, String sprite, int x, int y,
                     long timeMs, int mouth, boolean speaking, boolean listening, int partnerDirection) {
        int blink = settings.blinking() ? blinkPose(sprite, timeMs) : 0;
        Face face = FACES.get(sprite);
        BufferedImage pose = source;
        if (face != null && (blink != 0 || mouth != 0)) {
            Key key = new Key(source, sprite, blink, mouth);
            pose = poses.computeIfAbsent(key, ignored -> makePose(source, face, DialogueRigProfile.forSprite(sprite), blink, mouth));
        }
        if (face != null) {
            Rig rig = rigs.computeIfAbsent(sprite + ":" + partnerDirection, ignored -> new Rig());
            if (rig.source != source) {
                rig.source = source;
                rig.bounds = bounds(source);
                rig.profile=DialogueRigProfile.forSprite(sprite);
                rig.layers=new DialogueAnimationLayers(sprite,rig.profile);
                rig.mask=new DialoguePartMask(rig.profile,rig.bounds,source.getWidth(),source.getHeight());
                double normalizedScale=rig.bounds.height/1000.0;
                rig.body=DialogueBodyMotion.rig(sprite,(face.lx+face.rx)*.5,rig.bounds.width/normalizedScale);
                rig.skeleton=DialogueBodyMotion.skeleton(rig.body,(face.lx+face.rx)*.5,(face.ly+face.ry)*.5,face.my);
                rig.footPin=new float[source.getWidth()*source.getHeight()];
                for(int row=0;row<source.getHeight();row++)for(int col=0;col<source.getWidth();col++)
                    rig.footPin[row*source.getWidth()+col]=(float)rig.body.pin((col-rig.bounds.x)/normalizedScale,(row-rig.bounds.y)/normalizedScale);
                // Conservative support envelope, including cubic taps and every bounded joint.
                // Empty margins need no inverse mapping or texture reconstruction.
                int sw=source.getWidth(),sh=source.getHeight(),padding=Math.max(4,(int)Math.ceil(45*normalizedScale));
                int[] first=new int[sh],last=new int[sh],sourcePixels=pixels(source);
                java.util.Arrays.fill(first,sw);java.util.Arrays.fill(last,-1);
                for(int row=0;row<sh;row++)for(int col=0;col<sw;col++)if((sourcePixels[row*sw+col]>>>24)!=0){first[row]=Math.min(first[row],col);last[row]=col;}
                rig.firstPixel=new int[sh];rig.lastPixel=new int[sh];
                for(int row=0;row<sh;row++) {
                    int start=sw,end=-1;
                    for(int nearby=Math.max(0,row-padding);nearby<=Math.min(sh-1,row+padding);nearby++)if(last[nearby]>=0){start=Math.min(start,first[nearby]-padding);end=Math.max(end,last[nearby]+padding);}
                    rig.firstPixel[row]=Math.max(0,start);rig.lastPixel[row]=Math.min(sw-1,end);
                }
                rig.rendered = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
                rig.frame = -1;
            }
            DialoguePoseTrack.Pose motion = rig.track.update(timeMs,speaking && settings.gestures(),listening && settings.gestures());
            if (timeMs != rig.frame || !motion.equals(rig.pose) || mouth != rig.mouth || blink != rig.blink) {
                DialogueAnimationLayers.Idle idle=DialogueAnimationLayers.idle(sprite,timeMs/1000.0,settings);
                double[] secondary=rig.layers.update(timeMs,motion.angle()+idle.sway()*2,speaking,settings);
                articulate(pose, rig, face, motion, partnerDirection, timeMs,
                        idle,secondary,rig.layers.chainOffsets());
                rig.frame = timeMs; rig.pose = motion; rig.mouth = mouth; rig.blink = blink;
            }
            pose = rig.rendered;
        }
        // Draw at native size: resampling the complete figure each repaint softened
        // clothes, weapons and hair even where no articulation was intended.
        g.drawImage(pose,x,y,null);
    }

    private static BufferedImage makePose(BufferedImage source, Face face, DialogueRigProfile profile, int blink, int mouth) {
        Rectangle bounds = bounds(source);
        int left = bounds.x, top = bounds.y;
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(source,0,0,null);
        if (!bounds.isEmpty()) {
            double scale = bounds.height / 1000.0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (blink > 0) {
                eye(g, source, left + face.lx * scale, top + face.ly * scale, face.eyeW * scale, blink);
                eye(g, source, left + face.rx * scale, top + face.ry * scale, face.eyeW * scale, blink);
            }
            if (mouth > 0) {
                double mx = left + face.mx * scale, my = top + face.my * scale;
                lips(result, source, mx, my, face.mouthW * scale, scale, mouth, profile.lips(), left, top);
            }
        }
        g.dispose();
        return result;
    }

    private static Rectangle bounds(BufferedImage source) {
        int left = source.getWidth(), top = source.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            if ((source.getRGB(x,y) >>> 24) > 8) {
                left = Math.min(left,x); right = Math.max(right,x); top = Math.min(top,y); bottom = Math.max(bottom,y);
            }
        }
        return right < left ? new Rectangle() : new Rectangle(left,top,right-left+1,bottom-top+1);
    }

    /** Stretch the painted lip seam and lower lip; keep the original palette and corners. */
    private static void lips(BufferedImage result, BufferedImage source, double cx, double cy,
                             double width, double scale, int mouth, DialogueRigProfile.Lips anchors, double left, double topOffset) {
        if(anchors!=null) {
            cx=left+(anchors.leftX()+anchors.rightX())*.5*scale;
            cy=topOffset+anchors.middleY()*scale;
            width=(anchors.rightX()-anchors.leftX())*scale;
        }
        double radiusX = width*.5, radiusY = Math.max(3,15*scale);
        int[] pixels = pixels(source);
        double opening = (mouth == 1 ? 3.5 : 6.5)*scale;
        // Fit one smooth lip line to the painted seam. Per-column dark-pixel
        // detection produces a jagged cavity when it follows texture or highlights.
        double sum=0,sumX=0,sumY=0,sumXX=0,sumXY=0;
        int search=Math.max(1,(int)Math.round(2*scale));
        for(int x=Math.max(0,(int)(cx-radiusX*.8));x<=Math.min(source.getWidth()-1,(int)(cx+radiusX*.8));x++) {
            double best=Double.POSITIVE_INFINITY, seam=cy;
            for(int dy=-search;dy<=search;dy++) {
                int py=Math.max(0,Math.min(source.getHeight()-1,(int)Math.round(cy)+dy));
                int color=pixels[py*source.getWidth()+x];
                double shade=((color>>>16)&255)*.3+((color>>>8)&255)*.59+(color&255)*.11+Math.abs(dy)*5;
                if(shade<best){best=shade;seam=py;}
            }
            double dx=x-cx;sum++;sumX+=dx;sumY+=seam;sumXX+=dx*dx;sumXY+=dx*seam;
        }
        double denominator=sum*sumXX-sumX*sumX;
        double slope=denominator>0 ? Math.max(-.35,Math.min(.35,(sum*sumXY-sumX*sumY)/denominator)) : 0;
        double center=sum>0 ? (sumY-slope*sumX)/sum : cy;
        for (int x = Math.max(0,(int)(cx-radiusX)); x <= Math.min(source.getWidth()-1,(int)(cx+radiusX)); x++) {
            double nx=(x-cx)/radiusX;
            double corner=Math.pow(Math.max(0,1-nx*nx),1.5);
            double seam=anchors==null ? center+slope*(x-cx)-.5*scale*nx*nx
                    : topOffset+anchors.y((x-left)/scale)*scale;
            double top=seam-opening*.18*corner, bottom=seam+opening*.82*corner;
            int seamColor=sampleSharp(pixels,source.getWidth(),source.getHeight(),x,seam);
            for (int y=Math.max(0,(int)(cy-radiusY));y<=Math.min(source.getHeight()-1,(int)(cy+radiusY));y++) {
                double shift=y <= top ? -opening*.18*corner : opening*.82*corner;
                double edge=y <= top ? top : bottom;
                double distance=Math.abs(y-edge);
                double blend=Math.max(0,1-distance/radiusY);
                double sy=y-shift*blend*blend;
                if(y>top && y<bottom)sy=seam;
                int painted=sampleSharp(pixels,source.getWidth(),source.getHeight(),x,sy);
                // Open a narrow cavity between the preserved upper and lower lip.
                // Feather its edge into the painted lips, rather than stamping an oval.
                double coverage=Math.max(0,Math.min(1,Math.min(y-top,bottom-y)+.5));
                if(coverage>0 && corner>.04) {
                    int red=(int)(((seamColor>>>16)&255)*.38), green=(int)(((seamColor>>>8)&255)*.27), blue=(int)((seamColor&255)*.30);
                    int r=(int)(((painted>>>16)&255)*(1-coverage)+red*coverage);
                    int g=(int)(((painted>>>8)&255)*(1-coverage)+green*coverage);
                    int b=(int)((painted&255)*(1-coverage)+blue*coverage);
                    painted=(painted&0xff000000)|(r<<16)|(g<<8)|b;
                }
                result.setRGB(x,y,painted);
            }
        }
    }

    /** Compose whole-body weight shift, local joints and attachments before one texture sample. */
    private static void articulate(BufferedImage pose, Rig rig, Face face, DialoguePoseTrack.Pose motion, int direction, long timeMs, DialogueAnimationLayers.Idle idle, double[] secondary, double[][] chains) {
        Graphics2D g = rig.rendered.createGraphics();
        g.setComposite(AlphaComposite.Src); g.drawImage(pose,0,0,null); g.dispose();
        Rectangle b = rig.bounds;
        if (b.isEmpty()) return;
        double scale = b.height / 1000.0;
        double cx = b.x + rig.skeleton[4].x()*scale;
        double cy = b.y + rig.skeleton[4].y()*scale;
        double neckY = b.y + rig.skeleton[3].y()*scale;
        double angle = motion.angle()*direction;
        double cosine = Math.cos(angle), sine = Math.sin(angle);
        double breath = idle.breath();
        double chestAngle = Math.toRadians(motion.shoulder()*.22)*direction + idle.breathingTilt();
        double chestCos = Math.cos(chestAngle), chestSin = Math.sin(chestAngle);
        double waistY = b.y+rig.skeleton[1].y()*scale;
        DialogueBodyMotion.Pose wholeBody=DialogueBodyMotion.pose(rig.body,idle,motion.shoulder(),direction);
        double rx = 105*scale, ry = 135*scale;
        int width = pose.getWidth(), height = pose.getHeight();
        int[] pixels = pixels(pose);
        int[] output = ((DataBufferInt)rig.rendered.getRaster().getDataBuffer()).getData();
        double[] partCos=new double[secondary.length],partSin=new double[secondary.length],partBottom=new double[secondary.length];
        for(int i=0;i<secondary.length;i++){
            partCos[i]=Math.cos(Math.toRadians(secondary[i]));partSin[i]=Math.sin(Math.toRadians(secondary[i]));
            double[] polygon=rig.profile.parts().get(i).polygon();
            for(int point=1;point<polygon.length;point+=2)partBottom[i]=Math.max(partBottom[i],polygon[point]);
        }
        for (int y = 0; y < height; y++) {
            double torso = Math.max(0,1-(y-b.y)/(b.height*.52));
            torso *= torso;
            for (int x = rig.firstPixel[y]; x <= rig.lastPixel[y]; x++) {
                double footPin=rig.footPin[y*width+x];
                if(footPin>=1)continue; // Foot artwork stays exactly at its original contact.
                double nx=(x-b.x)/scale,ny=(y-b.y)/scale;
                double baseX=b.x+wholeBody.sourceX(nx,ny,footPin)*scale;
                double baseY=b.y+wholeBody.sourceY(nx,ny,footPin)*scale;
                double dx = baseX-cx, dy = baseY-cy;
                double chest = Math.max(0,1-dx*dx/(160*160*scale*scale));
                chest *= chest;
                // The entire face follows one rigid head transform. Feather only outside
                // the head into hair/neck; the previous centre-weighted warp stretched cheeks.
                double radius = Math.sqrt(dx*dx/(rx*rx)+dy*dy/(ry*ry));
                double feather = Math.min(1,Math.max(0,(radius-.66)/.34));
                double head = 1-feather*feather*(3-2*feather);
                // Joint hierarchy: waist -> chest -> neck -> head. There is no
                // independent head translation; the head follows its parent's arc.
                double bodyWeight = Math.max(head,torso*chest);
                double bodyX = baseX + bodyWeight*(cx+chestCos*(baseX-cx)+chestSin*(baseY-waistY)-baseX);
                double bodyY = baseY + bodyWeight*(waistY-chestSin*(baseX-cx)+chestCos*(baseY-waistY)-baseY);
                double localX = bodyX-cx, localY = bodyY-neckY;
                double rotatedX = cx+cosine*localX+sine*localY;
                double rotatedY = neckY-sine*localX+cosine*localY;
                double sx = bodyX + head*(rotatedX-bodyX);
                double sy = bodyY + head*(rotatedY-bodyY);
                // Breathing remains active during speech, listening, and idle. The small
                // expansion is confined to the upper torso, never the whole image.
                double bx=(sx-cx)/(120*scale),by=(sy-(b.y+(face.my+135)*scale))/(95*scale);
                double breatheWeight=Math.max(0,1-bx*bx-by*by);breatheWeight*=breatheWeight;
                sx=cx+(sx-cx)/(1+(rig.profile.parts().isEmpty()?0:breath*.002)*breatheWeight);
                sy+=breath*.7*scale*breatheWeight;
                int maskX=(int)Math.round(sx),maskY=(int)Math.round(sy);
                if(maskX>=0&&maskY>=0&&maskX<width&&maskY<height) {
                    int pixel=maskY*width+maskX,partIndex=rig.mask.owner[pixel];
                    if(partIndex>=0) {
                        var part=rig.profile.parts().get(partIndex);
                        double amount=secondary[partIndex],weight=rig.mask.weight[pixel];
                        if(part.kind()==DialogueRigProfile.Kind.HAIR || part.kind()==DialogueRigProfile.Kind.CLOTH) {
                            double progress=Math.max(0,Math.min(1,((sy-b.y)/scale-part.pivotY())/Math.max(1,partBottom[partIndex]-part.pivotY())))*6;
                            int node=Math.min(5,(int)progress);double mix=progress-node;
                            sx-=(chains[partIndex][node]*(1-mix)+chains[partIndex][node+1]*mix)*scale*weight;
                        }
                        else if(part.kind()==DialogueRigProfile.Kind.SOFT_TISSUE)sy-=amount*scale*weight;
                        else {
                            double px=b.x+part.pivotX()*scale,py=b.y+part.pivotY()*scale;
                            double localPX=sx-px,localPY=sy-py;
                            double tx=px+partCos[partIndex]*localPX+partSin[partIndex]*localPY;
                            double ty=py-partSin[partIndex]*localPX+partCos[partIndex]*localPY;
                            sx+=(tx-sx)*weight;sy+=(ty-sy)*weight;
                        }
                    }
                }
                if(Math.abs(sx-x)+Math.abs(sy-y)>1e-9)
                    output[y*width+x] = sampleSharp(pixels,width,height,sx,sy);
            }
        }
    }

    /** Read source pixels without altering the cached artwork. */
    private static int[] pixels(BufferedImage image) {
        // Cached fit/pose images are contiguous ARGB; support other callers safely too.
        if (image.getType() == BufferedImage.TYPE_INT_ARGB && image.getRaster().getParent() == null
                && image.getRaster().getDataBuffer().getOffset() == 0)
            return ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
    }

    /** Catmull-Rom reconstruction keeps fine eyes, glasses and painted lips crisp under subpixel motion. */
    private static int sampleSharp(int[] pixels, int width, int height, double x, double y) {
        int ix = (int)Math.floor(x), iy = (int)Math.floor(y);
        double fx = x-ix;
        double w0 = cubic(fx+1), w1 = cubic(fx), w2 = cubic(fx-1), w3 = cubic(fx-2);
        double a = 0, r = 0, g = 0, b = 0;
        for (int j = -1; j <= 2; j++) {
            int py = iy+j;
            if (py < 0 || py >= height) continue;
            double wy = cubic(y-py);
            for (int i = -1; i <= 2; i++) {
                int px = ix+i;
                if (px < 0 || px >= width) continue;
                int color = pixels[py*width+px];
                double wx = switch (i) { case -1 -> w0; case 0 -> w1; case 1 -> w2; default -> w3; };
                double weight = wx*wy*(color>>>24);
                a += weight; r += ((color>>>16)&255)*weight;
                g += ((color>>>8)&255)*weight; b += (color&255)*weight;
            }
        }
        if (a < .5) return 0;
        return (channel(a)<<24) | (channel(r/a)<<16) | (channel(g/a)<<8) | channel(b/a);
    }

    private static int channel(double value) { return Math.max(0,Math.min(255,(int)Math.round(value))); }
    private static double cubic(double value) {
        double v = Math.abs(value);
        return v < 1 ? (1.5*v-2.5)*v*v+1 : v < 2 ? ((-.5*v+2.5)*v-4)*v+2 : 0;
    }

    private static void eye(Graphics2D g, BufferedImage source, double x, double y, double width, int blink) {
        double height = width * .44;
        int sampleX = Math.max(0,Math.min(source.getWidth()-1,(int)Math.round(x)));
        // Sample the cheek below the eye, not the eyebrow/hair above it.
        int sampleY = Math.max(0,Math.min(source.getHeight()-1,(int)Math.round(y+height*.9)));
        Color skin = new Color(source.getRGB(sampleX,sampleY),true);
        g.setColor(skin);
        g.fill(new Ellipse2D.Double(x-width*.55,y-height*.55,width*1.1,height));
        g.setColor(new Color((int)(skin.getRed()*.48),(int)(skin.getGreen()*.40),(int)(skin.getBlue()*.40),230));
        g.setStroke(new BasicStroke((float)Math.max(.65,width*.065),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        if (blink == 2) g.draw(new java.awt.geom.QuadCurve2D.Double(x-width/2,y,x,y+height*.3,x+width/2,y));
        else g.fill(new Ellipse2D.Double(x-width*.45,y,width*.9,height*.3));
    }
}

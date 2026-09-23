package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** Two complete jointed legs behind an independent clothing layer. */
final class GroundedWalkingMotion {
    record Leg(double hipX, double hipY, double kneeX, double kneeY, double ankleX, double ankleY) { }
    private final BufferedImage source, upper, lower, boot;
    private final int facing, top, bottom, body, hip, bootHeight, legWidth;
    private final boolean robes;
    private final boolean quarterView;
    private final String actorName;
    private final boolean outerCloth;
    private final double waistCoverage;
    private final double center, length, reach, footCenter;
    private final GroundedArmRig arms;
    private final boolean[][] cloak;
    private final double travelX, travelY;
    private double cycleTravel = .75 * 48 * 144 / 66.0;
    GroundedWalkingMotion configure(double speed,int displayHeight) {
        speed=LocomotionClock.groundedCadence(speed,travelX,travelY);
        cycleTravel=LocomotionClock.strideTiles(travelX,travelY)*48*144/(double)displayHeight/speed;
        return this;
    }

    GroundedWalkingMotion(BufferedImage source, int facing, boolean robes) {
        this(source, facing, robes, "");
    }

    GroundedWalkingMotion(BufferedImage source, int facing, boolean robes, String name) {
        actorName=name;
        outerCloth=!name.matches("(?:class_(?:ranger|rogue)|npc_(?:calder|bartender|blacksmith|citizen_man))_model_.*");
        waistCoverage=name.matches("npc_(?:bartender|blacksmith)_model_.*")?.80:name.startsWith("npc_calder")?.72:.655;
        this.source = source; this.facing = facing; cloak=GroundedPartMasks.cloak(source,name);
        double dy=name.contains("_model_up")?-1:name.contains("_model_down")?1:0;
        quarterView=facing!=0&&dy!=0;
        double magnitude=Math.hypot(facing,dy);
        travelX=magnitude==0?1:facing/magnitude;travelY=magnitude==0?0:dy/magnitude;
        int first = source.getHeight(), last = 0;
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++)
            if (opaque(x, y)) { first = Math.min(first, y); last = Math.max(last, y); }
        top = first; bottom = last; body = Math.max(1, bottom - top);
        // Clothing length never determines the anatomical hip or the length of a leg.
        hip = top + (int) Math.round(body * .55);
        center = averageX(top + (int) (body * .12), top + (int) (body * .23)) - facing * body * .025;
        this.robes = robes || continuousSkirt()
                || name.matches("npc_(?:baker|citizen_woman)_model_.*")
                || (name.contains("_model_up")&&name.matches("class_(?:ranger|rogue)_model_.*"));
        bootHeight = Math.max(5, (int) (body * .07));
        footCenter = bootAxis();
        legWidth = Math.max(5, (int) Math.round(body * .065));
        reach = body * .105;
        length = Math.hypot(bottom - bootHeight - hip + Math.abs(travelY) * reach, reach) / 2 + 1;
        upper = texture(hip, hip + (bottom - bootHeight - hip) / 2);
        lower = texture(hip + (bottom - bootHeight - hip) / 2, bottom - bootHeight);
        int bw = legWidth + 6;
        boot = new BufferedImage(bw, bootHeight + 1, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < boot.getHeight(); y++) for (int x = 0; x < bw; x++) {
            int sx = (int) Math.round(footCenter) + x - bw / 2;
            if (sx >= 0 && sx < source.getWidth()) boot.setRGB(x, y, source.getRGB(sx, bottom - bootHeight + y));
        }
        arms = new GroundedArmRig(source, facing, top, body, center, name);
    }

    // Ground contact is parameterized by distance and the actor's actual world viewport.
    private double phase(int frame) { return Math.floorMod(frame, 32) / 32.0; }
    private double footX(double t) {
        double contact = cycleTravel * .60 / 2;
        if (t < .60) return contact - cycleTravel * t;
        double u = (t - .60) / .40;
        // Hermite return meets the planted velocity at both ends.
        double a = -contact, b = contact, tangent = -cycleTravel * .40;
        return (2*u*u*u-3*u*u+1)*a + (u*u*u-2*u*u+u)*tangent
                + (-2*u*u*u+3*u*u)*b + (u*u*u-u*u)*tangent;
    }
    private double lift(double t) { return t < .60 ? 0 : 9 * Math.pow(Math.sin((t-.60)/.40*Math.PI), 2); }
    private double ankleBase() { return bottom-bootHeight-Math.abs(travelY)*cycleTravel*.30; }
    private double weightOffset(int frame) {
        double p=phase(frame),offset=1+2*Math.cos(p*Math.PI*4);
        for(int which=0;which<2;which++) {
            double t=(p+which*.5)%1,dx=travelX*footX(t);
            double ay=ankleBase()+travelY*footX(t)-lift(t)-(which==0?1:0);
            offset=Math.max(offset,ay-Math.sqrt(Math.max(1,4*length*length-dx*dx))-hip+.6);
        }
        return offset;
    }
    Leg leg(int frame,int which) {
        double t=(phase(frame)+which*.5)%1,lane=which==0?-1:1;
        double hx=center+lane*(facing==0?legWidth*.62:quarterView?legWidth*.48:1.5),hy=hip+weightOffset(frame);
        double ax=hx+travelX*footX(t),ay=ankleBase()+travelY*footX(t)-lift(t)-(which==0?1:0);
        double dx=ax-hx,dy=ay-hy,d=Math.max(.001,Math.hypot(dx,dy));
        double bend=Math.sqrt(Math.max(0,length*length-d*d/4));
        if(quarterView)bend=Math.min(bend,body*.04); // Depth bend is foreshortened in a quarter view.
        return new Leg(hx,hy,(hx+ax)/2+(facing==0?lane*Math.min(2,bend):facing*dy/d*bend),
                (hy+ay)/2-(facing==0?0:facing*dx/d*bend),ax,ay);
    }
    GroundedArmRig.Pose arm(int frame, int which) { return arms.pose(frame, which); }

    private double spineAngle(int frame) { return Math.sin(phase(frame)*Math.PI*2) * .018; }
    private double headAngle(int frame) { return -spineAngle(frame) * .85; }
    private AffineTransform spineTransform(int frame) {
        AffineTransform t = AffineTransform.getTranslateInstance(0, weightOffset(frame));
        t.rotate(spineAngle(frame), center, hip); return t;
    }
    private double[] point(AffineTransform t, double x, double y) {
        double[] p={x,y}; t.transform(p,0,p,0,1); return p;
    }
    // Pelvis -> chest -> neck -> head. Arms inherit chest motion; hands inherit wrists.
    double[][] skeleton(int frame) {
        AffineTransform t=spineTransform(frame);
        double nx=center, ny=top+body*.23;
        AffineTransform head=new AffineTransform(t); head.rotate(headAngle(frame),nx,ny);
        double[][] joints=new double[12][];
        joints[0]=point(t,center,hip); joints[1]=point(t,center,top+body*.34);
        joints[2]=point(t,nx,ny); joints[3]=point(head,center,top+body*.10);
        double oldBob=Math.round(-Math.pow(Math.sin(phase(frame)*Math.PI*2),2));
        for(int which=0;which<2;which++) {
            var p=arms.pose(frame,which); int i=4+which*4;
            joints[i]=point(t,p.shoulderX(),p.shoulderY()-oldBob);
            joints[i+1]=point(t,p.elbowX(),p.elbowY()-oldBob);
            joints[i+2]=point(t,p.wristX(),p.wristY()-oldBob);
            joints[i+3]=point(t,p.handX(),p.handY()-oldBob);
        }
        return joints;
    }
    private void drawArm(Graphics2D g,int frame,int which) {
        Graphics2D arm=(Graphics2D)g.create(); arm.transform(spineTransform(frame));
        arm.translate(0,-Math.round(-Math.pow(Math.sin(phase(frame)*Math.PI*2),2)));
        arms.draw(arm,frame,which); arm.dispose();
    }

    BufferedImage frame(int frame) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // Render the complete cape behind the legs, anchored to the torso at its top.
        g.drawImage(cloakFrame(frame),0,0,null);
        for (int which = 0; which < 2; which++) {
            Leg p = leg(frame, which);
            // The far limb is a complete limb even when it was hidden in the standing drawing.
            drawSegment(g, upper, p.hipX, p.hipY, p.kneeX, p.kneeY, which == 0);
            drawSegment(g, lower, p.kneeX, p.kneeY, p.ankleX, p.ankleY, which == 0);
            g.drawImage(which == 0 ? shade(boot) : boot, (int) Math.round(p.ankleX - boot.getWidth() / 2.0), (int) Math.round(p.ankleY), null);
        }
        drawArm(g, frame, 0);
        g.dispose();
        double phase = Math.floorMod(frame, WorldCharacterAnimation.FRAMES) * Math.PI * 2 / WorldCharacterAnimation.FRAMES;
        int bob = (int) Math.round(weightOffset(frame));
        int hem = top + (int) (body * .91);
        BufferedImage chest=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        BufferedImage head=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for (int y = top; y <= bottom; y++) for (int x = 0; x < source.getWidth(); x++) {
            int pixel = arms.torso().getRGB(x, y);
            if ((pixel >>> 24) <= 100 || cloak[y][x]) continue;
            if(GroundedPartMasks.equipment(actorName,x,y)){chest.setRGB(x,y,pixel);continue;}
            boolean clothing;
            if (robes) clothing = y < hem;
            else {
                double axis = center + (footCenter - center) * Math.max(0, (y - hip) / (double) Math.max(1, bottom - hip));
                clothing = y < top + (int)(body*waistCoverage) || (outerCloth && y < bottom - bootHeight - 3 && Math.abs(x - axis) > legWidth * 1.3);
            }
            if (!clothing) continue;
            if(y<hip) { (y<top+body*.22?head:chest).setRGB(x,y,pixel); continue; }
            // Only loose cloth below the belt may sway. The chest/abdomen never expand.
            double clothWeight = Math.max(0, (y - (top + body * .60)) / (body * .40));
            double cloth = Math.sin(phase - .35 - clothWeight * 1.2) * 2.5 * clothWeight;
            int tx = (int) Math.round(x + cloth), ty = y + bob - (int)Math.round(Math.max(0,bob-4)*clothWeight);
            if (tx >= 0 && tx < out.getWidth() && ty >= 0 && ty < out.getHeight()) out.setRGB(tx, ty, pixel);
        }
        g=out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(chest,spineTransform(frame),null);
        AffineTransform headTransform=spineTransform(frame);
        headTransform.rotate(headAngle(frame),center,top+body*.23);
        g.drawImage(head,headTransform,null);
        drawArm(g,frame,1); g.dispose();
        return out;
    }

    BufferedImage cloakFrame(int frame) {
        BufferedImage layer=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        double phase=phase(frame)*Math.PI*2;
        AffineTransform torso=spineTransform(frame);
        for(int y=top;y<=bottom;y++)for(int x=0;x<source.getWidth();x++)if(cloak[y][x]) {
            double weight=Math.max(0,(y-(top+body*.45))/(body*.55));
            double[] p=point(torso,x,y);
            int xx=(int)Math.round(p[0]+Math.sin(phase-.35-weight*1.2)*2.5*weight),yy=(int)Math.round(p[1]-Math.max(0,weightOffset(frame)-4)*weight);
            if(xx>=0&&xx<layer.getWidth()&&yy>=0&&yy<layer.getHeight())layer.setRGB(xx,yy,source.getRGB(x,y));
        }
        return layer;
    }

    private boolean continuousSkirt() {
        // A broad, unbroken lower silhouette is clothing, not two exposed legs.
        int half = Math.max(4, (int) (body * .07));
        for (double row : new double[]{.74, .81, .86}) {
            int count = 0, total = 0, y = top + (int) (body * row);
            for (int x = (int) center - half; x <= (int) center + half; x++) {
                total++;
                if (x >= 0 && x < source.getWidth() && opaque(x, y)) count++;
            }
            if (count < total * .95) return false;
        }
        return true;
    }

    private double bootAxis() {
        // At the sole row, a hammer or cloak usually no longer overlaps the feet.
        for(int y=bottom-1;y>=bottom-bootHeight;y--) {
            int bestStart=-1,bestEnd=-1;double best=-1;
            for(int x=0;x<source.getWidth();) {
                if(!opaque(x,y)){x++;continue;}
                int start=x;while(x<source.getWidth()&&opaque(x,y))x++;
                int end=x-1;double score=(end-start+1)-Math.abs((start+end)/2.0-center)*.08;
                if(score>best){best=score;bestStart=start;bestEnd=end;}
            }
            if(bestEnd-bestStart>=2)return (bestStart+bestEnd)/2.0;
        }
        return averageX(bottom-bootHeight+1,bottom);
    }
    private int nearestLegPixel(int x,int y) {
        for(int d=1;d<=legWidth*2;d++)for(int side:new int[]{-1,1}) {
            int xx=x+side*d;
            if(xx>=0&&xx<source.getWidth()&&opaque(xx,y)&&!cloak[y][xx]&&!GroundedPartMasks.equipment(actorName,xx,y))return source.getRGB(xx,y);
        }
        int pixel=source.getRGB((int)Math.round(footCenter),bottom-1);
        return (pixel>>>24)>100?pixel:0xff302b26;
    }

    private BufferedImage texture(int from, int to) {
        BufferedImage result = new BufferedImage(legWidth, Math.max(2, to - from + 1), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < result.getHeight(); y++) for (int x = 0; x < legWidth; x++) {
            double t = (from + y - hip) / (double) Math.max(1, bottom - hip);
            // Calder's forward-quarter hammer overlaps his near shin; sample the unobscured far leg.
            double sampleFoot=actorName.equals("npc_calder_model_down_left")?76:footCenter;
            int axis = (int) Math.round(center + (sampleFoot - center) * t);
            int sy = from + y;
            // Hidden trouser sections use the visible shin's palette, not robe fabric.
            if (robes) sy = Math.max(top, bottom - bootHeight - 3 + y % 3);
            int sx = Math.max(0, Math.min(source.getWidth() - 1, axis + x - legWidth / 2));
            int pixel = source.getRGB(sx, sy);
            if ((pixel >>> 24) < 100 || GroundedPartMasks.equipment(actorName,sx,sy) || cloak[sy][sx]) {
                pixel=nearestLegPixel(sx,sy);
            }
            if (x == 0 || x == legWidth - 1) pixel = darken(pixel, .72);
            result.setRGB(x, y, pixel);
        }
        return result;
    }

    private static void drawSegment(Graphics2D g, BufferedImage image, double x1, double y1, double x2, double y2, boolean far) {
        AffineTransform transform = new AffineTransform();
        transform.translate(x1, y1); transform.rotate(Math.atan2(y2 - y1, x2 - x1) - Math.PI / 2);
        transform.scale(1, (Math.hypot(x2 - x1, y2 - y1) + 2) / image.getHeight());
        transform.translate(-image.getWidth() / 2.0, -1);
        g.drawImage(far ? shade(image) : image, transform, null);
    }

    private static BufferedImage shade(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, darken(source.getRGB(x, y), .78));
        return out;
    }
    private static int darken(int p, double factor) { return (p & 0xff000000) | ((int) (((p >> 16) & 255) * factor) << 16) | ((int) (((p >> 8) & 255) * factor) << 8) | (int) ((p & 255) * factor); }
    private boolean opaque(int x, int y) { return (source.getRGB(x, y) >>> 24) > 100; }
    private double averageX(int from, int to) {
        double sum = 0; int count = 0;
        for (int y = from; y <= to; y++) for (int x = 0; x < source.getWidth(); x++) if (opaque(x, y)) { sum += x; count++; }
        return count == 0 ? source.getWidth() / 2.0 : sum / count;
    }
}

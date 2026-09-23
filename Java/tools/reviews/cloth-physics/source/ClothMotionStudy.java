package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** Two complete jointed legs behind an independent clothing layer. */
final class ClothMotionStudy {
    record Leg(double hipX, double hipY, double kneeX, double kneeY, double ankleX, double ankleY) { }
    private final BufferedImage source, upper, lower, boot;
    private final int facing, top, bottom, body, hip, bootHeight, legWidth;
    private final boolean robes;
    private final boolean quarterView;
    private final MovementLimbProfile profile;
    private final String actorName;
    private final boolean outerCloth;
    private final double waistCoverage;
    private final double center, pelvisX, length, reach, footCenter;
    private final StudyArmRig arms;
    private final boolean[][] cloak;
    private final double travelX, travelY;
    private boolean simulate;
    private double[][][] offsets;
    private final double[][] restEdges=new double[3][7];
    private final boolean[][] restEdgeReady=new boolean[3][7];
    ClothMotionStudy physics(boolean enabled){simulate=enabled;return this;}
    private double cycleTravel = .75 * 48 * 144 / 66.0;
    ClothMotionStudy configure(double speed,int displayHeight) {
        speed=LocomotionClock.groundedCadence(speed,travelX,travelY);
        cycleTravel=LocomotionClock.strideTiles(travelX,travelY)*48*144/(double)displayHeight/speed;
        return this;
    }

    ClothMotionStudy(BufferedImage source, int facing, boolean robes) {
        this(source, facing, robes, "");
    }

    ClothMotionStudy(BufferedImage source, int facing, boolean robes, String name) {
        this(source,facing,robes,name,name);
    }
    ClothMotionStudy(BufferedImage source,int facing,boolean robes,String name,String movementName) {
        actorName=name;
        profile=MovementLimbProfile.find(name);
        outerCloth=!name.matches("(?:class_(?:ranger|rogue)|npc_(?:seraphine|calder|bartender|blacksmith|citizen_man))_model_.*");
        waistCoverage=name.startsWith("npc_seraphine")?.59:name.matches("npc_(?:bartender|blacksmith)_model_.*")?.80:name.startsWith("npc_calder")?.72:.655;
        this.source = source; this.facing = facing; cloak=GroundedPartMasks.cloak(source,name);
        double dy=movementName.contains("_model_up")?-1:movementName.contains("_model_down")?1:0;
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
        // Her ponytail biases the generic head-row centroid rearward. Keep hair/arm
        // coordinates intact and anchor the legs to the visible pelvis instead.
        pelvisX=profile==null?center:profile.pelvis(center,source.getWidth());
        this.robes = robes || continuousSkirt()
                || name.matches("npc_(?:baker|citizen_woman)_model_.*")
                || (name.contains("_model_up")&&name.matches("class_(?:ranger|rogue)_model_.*"));
        bootHeight = Math.max(5, (int) (body * .07));
        footCenter = bootAxis();
        legWidth = Math.max(5, (int) Math.round(body * (name.startsWith("npc_seraphine")?.052:.065)));
        reach = body * (profile==null?.105:profile.reachFraction);
        length = Math.hypot(bottom - bootHeight - hip + Math.abs(travelY) * reach, reach) / 2 + (profile==null?1:profile.lengthSlack);
        arms = new StudyArmRig(source, facing, top, body, center, name);
        upper = texture(hip, hip + (bottom - bootHeight - hip) / 2);
        lower = texture(hip + (bottom - bootHeight - hip) / 2, bottom - bootHeight);
        int bw = legWidth + 6;
        boot = new BufferedImage(bw, bootHeight + 1, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < boot.getHeight(); y++) for (int x = 0; x < bw; x++) {
            int sx = (int) Math.round(footCenter) + x - bw / 2;
            if (sx >= 0 && sx < source.getWidth()) boot.setRGB(x, y, source.getRGB(sx, bottom - bootHeight + y));
        }
    }

    BufferedImage[] reviewLegLayers(){return new BufferedImage[]{upper,lower,boot};}

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
    private double lift(double t) { return t < .60 ? 0 : (profile==null?9:profile.swingLift) * Math.pow(Math.sin((t-.60)/.40*Math.PI), 2); }
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
        double hx=pelvisX+lane*(facing==0?legWidth*.62:quarterView?legWidth*.48:1.5),hy=hip+weightOffset(frame);
        double ax=hx+travelX*footX(t),ay=ankleBase()+travelY*footX(t)-lift(t)-(which==0?1:0);
        double dx=ax-hx,dy=ay-hy,d=Math.max(.001,Math.hypot(dx,dy));
        double bend=Math.sqrt(Math.max(0,length*length-d*d/4));
        if(quarterView)bend=Math.min(bend,body*.04); // Depth bend is foreshortened in a quarter view.
        return new Leg(hx,hy,(hx+ax)/2+(facing==0?lane*Math.min(2,bend):facing*dy/d*bend),
                (hy+ay)/2-(facing==0?0:facing*dx/d*bend),ax,ay);
    }
    StudyArmRig.Pose arm(int frame, int which) { return arms.pose(frame, which); }

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
        joints[0]=new double[]{pelvisX,hip+weightOffset(frame)}; joints[1]=point(t,center,top+body*.34);
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
        if(simulate&&offsets==null)bakePhysics();
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // Render the complete cape behind the legs, anchored to the torso at its top.
        g.drawImage(cloakFrame(frame),0,0,null);
        if(simulate)drawHair(g,frame);
        for (int which = 0; which < 2; which++) {
            Leg p = leg(frame, which);
            // The far limb is a complete limb even when it was hidden in the standing drawing.
            drawSegment(g, upper, p.hipX, p.hipY, p.kneeX, p.kneeY, which == 0);
            drawSegment(g, lower, p.kneeX, p.kneeY, p.ankleX, p.ankleY, which == 0);
            g.drawImage(which == 0 ? shade(boot) : boot, (int) Math.round(p.ankleX - boot.getWidth() / 2.0), (int) Math.round(p.ankleY), null);
        }
        // The rear-view cape is the foreground silhouette: legs must remain behind it.
        if(actorName.matches("class_(?:ranger|rogue)_model_up.*"))g.drawImage(cloakFrame(frame),0,0,null);
        drawArm(g, frame, 0);
        g.dispose();
        double phase = Math.floorMod(frame, WorldCharacterAnimation.FRAMES) * Math.PI * 2 / WorldCharacterAnimation.FRAMES;
        int bob = (int) Math.round(weightOffset(frame));
        int hem = top + (int) (body * .91);
        BufferedImage chest=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        BufferedImage head=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for (int y = top; y <= bottom; y++) for (int x = 0; x < source.getWidth(); x++) {
            int pixel = arms.torso().getRGB(x, y);
            if ((pixel >>> 24) <= 100 || cloak[y][x] || (simulate&&hairPixel(x,y))) continue;
            if(studyEquipment(x,y)){chest.setRGB(x,y,pixel);continue;}
            boolean clothing;
            if (robes) clothing = y < hem;
            else {
                double axis = center + (footCenter - center) * Math.max(0, (y - hip) / (double) Math.max(1, bottom - hip));
                clothing = y < top + (int)(body*waistCoverage) || (outerCloth && y < bottom - bootHeight - 3 && Math.abs(x - axis) > legWidth * 1.3);
            }
            if (!clothing) continue;
            if(simulate&&robes&&y>=hip)continue; // Render as one continuous row mesh below.
            if(y<hip) { (y<top+body*.22?head:chest).setRGB(x,y,pixel); continue; }
            // Only loose cloth below the belt may sway. The chest/abdomen never expand.
            double clothWeight = Math.max(0, (y - (top + body * .60)) / (body * .40));
            double cloth = (simulate?0:Math.sin(phase - .35 - clothWeight * 1.2) * 2.5 * clothWeight);
            int tx = (int) Math.round(x + cloth + (simulate&&robes?clothOffset(frame,y,x<center?0:1):0)), ty = y + bob - (int)Math.round(Math.max(0,bob-4)*clothWeight);
            if (tx >= 0 && tx < out.getWidth() && ty >= 0 && ty < out.getHeight()) out.setRGB(tx, ty, pixel);
        }
        if(simulate&&robes)drawRobe(out,frame,bob,hem);
        g=out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(chest,spineTransform(frame),null);
        AffineTransform headTransform=spineTransform(frame);
        headTransform.rotate(headAngle(frame),center,top+body*.23);
        g.drawImage(head,headTransform,null);
        drawArm(g,frame,1); g.dispose();
        return out;
    }

    // Inverse sampling fills every destination pixel: the robe has no independent centre seam.
    private void drawRobe(BufferedImage out,int frame,int bob,int hem) {
        for(int y=hip;y<hem;y++) {
            int left=source.getWidth(),right=-1;
            for(int x=0;x<source.getWidth();x++)if((arms.torso().getRGB(x,y)>>>24)>100&&!cloak[y][x]){left=Math.min(left,x);right=x;}
            if(right<=left)continue;
            double a=left+clothOffset(frame,y,0),b=right+clothOffset(frame,y,1);
            double weight=Math.max(0,(y-(top+body*.60))/(body*.40));
            int yy=y+bob-(int)Math.round(Math.max(0,bob-4)*weight);
            if(yy<0||yy>=out.getHeight())continue;
            for(int x=Math.max(0,(int)Math.ceil(a));x<=Math.min(out.getWidth()-1,(int)Math.floor(b));x++) {
                int sx=(int)Math.round(left+(x-a)*(right-left)/Math.max(1,b-a));
                sx=Math.max(left,Math.min(right,sx));
                int pixel=arms.torso().getRGB(sx,y);
                if((pixel>>>24)>100&&!cloak[y][sx])out.setRGB(x,yy,pixel);
            }
        }
    }

    BufferedImage cloakFrame(int frame) {
        BufferedImage layer=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        double phase=phase(frame)*Math.PI*2;
        AffineTransform torso=spineTransform(frame);
        for(int y=top;y<=bottom;y++)for(int x=0;x<source.getWidth();x++)if(cloak[y][x]) {
            double weight=Math.max(0,(y-(top+body*.45))/(body*.55));
            double[] p=point(torso,x,y);
            int xx=(int)Math.round(p[0]+(simulate?0:Math.sin(phase-.35-weight*1.2)*2.5*weight)+(simulate?clothOffset(frame,y,0):0)),yy=(int)Math.round(p[1]-Math.max(0,weightOffset(frame)-4)*weight);
            if(xx>=0&&xx<layer.getWidth()&&yy>=0&&yy<layer.getHeight())layer.setRGB(xx,yy,source.getRGB(x,y));
        }
        return layer;
    }

    // Review-only spring chains. Nodes keep source height; horizontal springs bend the sprite rows.
    // The innermost cape/hem edge collides with capsules along both complete legs.
    private double restY(int kind,int i){return kind==2?top+body*(.09+i*.065):top+body*(.52+i*.062);}
    private double restX(int kind,int i){
        if(kind==2)return center-body*.085;
        if(restEdgeReady[kind][i])return restEdges[kind][i];
        int row=(int)Math.round(restY(kind,i));double edge=kind==0?Double.POSITIVE_INFINITY:Double.NEGATIVE_INFINITY;
        for(int y=Math.max(top,row-1);y<=Math.min(bottom,row+1);y++)for(int x=0;x<source.getWidth();x++)
            if((robes?opaque(x,y):cloak[y][x]))edge=kind==0?Math.min(edge,x):Math.max(edge,x);
        restEdgeReady[kind][i]=true;return restEdges[kind][i]=Double.isFinite(edge)?edge:center;
    }
    private void bakePhysics() {
        offsets=new double[32][3][7];double[][] x=new double[3][7],velocity=new double[3][7];
        for(int step=0;step<32*24;step++) {
            int f=step%32;double[][] next=new double[3][7];
            for(int kind=0;kind<3;kind++)for(int i=1;i<7;i++) {
                if(kind==1&&!robes)continue; // No front robe panel on cape-only characters.
                double targetWind=(kind==2?-.12:-.08)+Math.sin(step*Math.PI/16-i*.6)*(kind==2?.18:.12);
                double neighbours=(x[kind][i-1]+x[kind][Math.min(6,i+1)])/2;
                velocity[kind][i]=velocity[kind][i]*.72-x[kind][i]*.08+(neighbours-x[kind][i])*.18+targetWind;
                double value=x[kind][i]+velocity[kind][i];
                double yy=restY(kind,i)+weightOffset(f);
                if(kind<2) {
                    double edge=restX(kind,i)+value;
                    for(int leg=0;leg<2;leg++) {
                        Leg p=leg(f,leg);
                        double boundary=kind==0?Math.min(capsuleX(p.hipX,p.hipY,p.kneeX,p.kneeY,yy,kind),capsuleX(p.kneeX,p.kneeY,p.ankleX,p.ankleY,yy,kind))
                            :Math.max(capsuleX(p.hipX,p.hipY,p.kneeX,p.kneeY,yy,kind),capsuleX(p.kneeX,p.kneeY,p.ankleX,p.ankleY,yy,kind));
                        if(Double.isFinite(boundary)&&(kind==0?edge>boundary:edge<boundary))edge=boundary;
                    }
                    value=edge-restX(kind,i);
                }
                if(Math.abs(value)>52)throw new IllegalStateException("Unstable cloth node");
                next[kind][i]=value;
                if(Math.abs(value-(x[kind][i]+velocity[kind][i]))>.01)velocity[kind][i]*=.2;
            }
            x=next;
            if(step>=32*23)for(int kind=0;kind<3;kind++)offsets[f][kind]=x[kind].clone();
        }
        // Project a cyclic clearance envelope. Moving only outward preserves collision
        // constraints and anticipates contact instead of snapping a node away on impact.
        for(int pass=0;pass<32;pass++)for(int f=0;f<32;f++)for(int kind=0;kind<(robes?2:1);kind++)for(int i=1;i<7;i++) {
            double prev=offsets[(f+31)%32][kind][i],next=offsets[(f+1)%32][kind][i];
            offsets[f][kind][i]=kind==0?Math.min(offsets[f][kind][i],Math.min(prev+4,next+4))
                :Math.max(offsets[f][kind][i],Math.max(prev-4,next-4));
        }
    }
    double colliderRadius(){return profile==null?legWidth*.55+1.5:profile.colliderRadius();}
    private double capsuleX(double ax,double ay,double bx,double by,double y,int side) {
        double radius=colliderRadius(),dx=bx-ax,dy=by-ay,length=Math.max(.0001,Math.hypot(dx,dy));
        double result=side==0?Double.POSITIVE_INFINITY:Double.NEGATIVE_INFINITY;
        double[] candidates=Math.abs(dy)<.0001?new double[]{0,1}:new double[]{0,1,(y-ay+radius*dx/length)/dy,(y-ay-radius*dx/length)/dy};
        for(double t:candidates) {
            t=Math.max(0,Math.min(1,t));double vertical=y-ay-t*dy;
            if(Math.abs(vertical)>radius)continue;
            double extent=Math.sqrt(Math.max(0,radius*radius-vertical*vertical));
            double edge=ax+t*dx+(side==0?-extent:extent);
            result=side==0?Math.min(result,edge):Math.max(result,edge);
        }
        return result;
    }
    private double clothOffset(int frame,double y,int kind) {
        if(offsets==null)bakePhysics();
        double u=Math.max(0,Math.min(6,(y-restY(kind,0))/(body*(kind==2?.065:.062))));
        int i=Math.min(5,(int)u);return offsets[Math.floorMod(frame,32)][kind][i]*(1-(u-i))+offsets[Math.floorMod(frame,32)][kind][i+1]*(u-i);
    }
    double[][] clothNodes(int frame) {
        if(offsets==null)bakePhysics();double[][] result=new double[21][2];
        for(int kind=0;kind<3;kind++)for(int i=0;i<7;i++)result[kind*7+i]=new double[]{restX(kind,i)+offsets[Math.floorMod(frame,32)][kind][i],restY(kind,i)+weightOffset(frame)};
        return result;
    }
    void verifyPhysics() {
        if(offsets==null)bakePhysics();
        double radius=colliderRadius();
        for(int f=0;f<32;f++) {
            double[][] nodes=clothNodes(f);
            for(int kind=0;kind<(robes?2:1);kind++)for(int i=1;i<7;i++)for(int which=0;which<2;which++) {
                Leg leg=leg(f,which);double[] n=nodes[kind*7+i];
                for(double[] segment:new double[][]{{leg.hipX,leg.hipY,leg.kneeX,leg.kneeY},{leg.kneeX,leg.kneeY,leg.ankleX,leg.ankleY}}) {
                    double dx=segment[2]-segment[0],dy=segment[3]-segment[1];
                    double t=Math.max(0,Math.min(1,((n[0]-segment[0])*dx+(n[1]-segment[1])*dy)/(dx*dx+dy*dy)));
                    if(Math.hypot(n[0]-segment[0]-t*dx,n[1]-segment[1]-t*dy)<radius-.01)throw new IllegalStateException("Cloth node penetrates leg");
                }
            }
            for(int kind=0;kind<3;kind++)for(int i=0;i<7;i++) {
                if(i==0&&offsets[f][kind][i]!=0)throw new IllegalStateException("Unpinned cloth root");
                if(Math.abs(offsets[f][kind][i]-offsets[(f+31)%32][kind][i])>15)throw new IllegalStateException("Discontinuous cloth loop");
            }
        }
    }
    private boolean hairPixel(int x,int y) {
        if(!actorName.equals("npc_seraphine_model_right")||x>96||y<top+body*.05||y>top+body*.45)return false;
        int p=source.getRGB(x,y),r=(p>>16)&255,g=(p>>8)&255,b=p&255;
        return (p>>>24)>100&&r<90&&g<80&&b<100&&r<g*1.6+12;
    }
    private void drawHair(Graphics2D g,int frame) {
        BufferedImage hair=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        AffineTransform t=spineTransform(frame);t.rotate(headAngle(frame),center,top+body*.23);
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if(hairPixel(x,y)) {
            double[] p=point(t,x,y);int xx=(int)Math.round(p[0]+clothOffset(frame,y,2)),yy=(int)Math.round(p[1]);
            if(xx>=0&&xx<hair.getWidth()&&yy>=0&&yy<hair.getHeight())hair.setRGB(xx,yy,source.getRGB(x,y));
        }
        g.drawImage(hair,0,0,null);
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
        // Front Calder's near boot is behind the hammer head; use the unobscured far boot.
        if(actorName.equals("npc_calder_model_down"))return 83;
        // At the sole row, a hammer or cloak usually no longer overlaps the feet.
        for(int y=bottom-1;y>=bottom-bootHeight;y--) {
            int bestStart=-1,bestEnd=-1;double best=-1;
            for(int x=0;x<source.getWidth();) {
                if(!opaque(x,y)){x++;continue;}
                int start=x;while(x<source.getWidth()&&opaque(x,y))x++;
                int end=x-1;double score=(end-start+1)-Math.abs((start+end)/2.0-(profile==null?center:pelvisX))*(profile==null?.08:.8);
                if(score>best){best=score;bestStart=start;bestEnd=end;}
            }
            if(bestEnd-bestStart>=2)return (bestStart+bestEnd)/2.0;
        }
        return averageX(bottom-bootHeight+1,bottom);
    }
    private boolean studyEquipment(int x,int y) {
        if(GroundedPartMasks.equipment(actorName,x,y))return true;
        if(!actorName.equals("npc_calder_model_down"))return false;
        double dx=-9,dy=30,t=Math.max(0,Math.min(1,((x-116)*dx+(y-85)*dy)/(dx*dx+dy*dy)));
        return Math.hypot(x-116-t*dx,y-85-t*dy)<3.5
            ||Math.pow((x-105)/13.0,2)+Math.pow((y-119)/9.0,2)<1;
    }
    private int nearestLegPixel(int x,int y) {
        for(int d=1;d<=legWidth*2;d++)for(int side:new int[]{-1,1}) {
            int xx=x+side*d;
            if(xx>=0&&xx<source.getWidth()&&opaque(xx,y)&&!cloak[y][xx]&&!studyEquipment(xx,y))return source.getRGB(xx,y);
        }
        int pixel=source.getRGB((int)Math.round(footCenter),bottom-1);
        return (pixel>>>24)>100?pixel:0xff302b26;
    }

    // The skeleton supplies length and pose, not a constant-width silhouette.
    // These source-calibrated cross sections retain thigh/calf volume and taper at joints.
    private BufferedImage shapedLegTexture(int from,int to) {
        int canvasWidth=(int)Math.ceil(profile.maximumWidth())+2;
        BufferedImage result=new BufferedImage(canvasWidth,Math.max(2,to-from+1),BufferedImage.TYPE_INT_ARGB);
        for(int row=0;row<result.getHeight();row++) {
            int anatomicalY=from+row;
            double u=(anatomicalY-hip)/(double)Math.max(1,bottom-bootHeight-hip);
            int diameter=(int)Math.round(profile.diameter(u));
            // Covered legs sample visible boot/trouser material, never invent robe-textured skin.
            int sy=profile.surface==MovementLimbProfile.Surface.HIDDEN_BY_ROBE
                ?bottom-bootHeight-3+row%3:anatomicalY;
            if(profile.surface==MovementLimbProfile.Surface.ARMOR&&u<.5)
                sy=hip+(int)Math.round((bottom-bootHeight-hip)*(.30+.40*u)); // Continuous visible armor patch; no repeated stripe rows.
            double sourceHip=actorName.equals("npc_calder_model_down")?86:pelvisX;
            double axis=sourceHip+(footCenter-sourceHip)*u;
            int start=(canvasWidth-diameter)/2;
            for(int x=0;x<diameter;x++) {
                int sx=(int)Math.round(axis+x-(diameter-1)/2.0);
                int pixel=0;
                // Exclude the cape and palm; retain source shading rather than adding black rails.
                for(int d=0;d<=8&&(pixel>>>24)<=100;d++)for(int side:new int[]{-1,1}) {
                    int xx=sx+side*d;
                    if(xx<0||xx>=source.getWidth()||Math.abs(xx-axis)>diameter*.65||cloak[sy][xx]||arms.ownerAt(xx,sy)>=0)continue;
                    int candidate=source.getRGB(xx,sy);
                    if((candidate>>>24)>100){pixel=candidate;break;}
                }
                if((pixel>>>24)<=100)pixel=arms.torso().getRGB((int)Math.round(axis),sy);
                if((pixel>>>24)<=100)pixel=nearestLegPixel((int)Math.round(axis),sy);
                result.setRGB(start+x,row,pixel);
            }
        }
        return result;
    }

    private BufferedImage texture(int from, int to) {
        if(profile!=null)return shapedLegTexture(from,to);
        BufferedImage result = new BufferedImage(legWidth, Math.max(2, to - from + 1), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < result.getHeight(); y++) for (int x = 0; x < legWidth; x++) {
            double t = (from + y - hip) / (double) Math.max(1, bottom - hip);
            // Calder's forward-quarter hammer overlaps his near shin; sample the unobscured far leg.
            double sampleFoot=actorName.equals("npc_calder_model_down_left")?76:footCenter;
            int axis = (int) Math.round(pelvisX + (sampleFoot - pelvisX) * t);
            int sy = from + y;
            // Hidden trouser sections use the visible shin's palette, not robe fabric.
            if (robes) sy = Math.max(top, bottom - bootHeight - 3 + y % 3);
            int sx = Math.max(0, Math.min(source.getWidth() - 1, axis + x - legWidth / 2));
            int pixel = source.getRGB(sx, sy);
            if ((pixel >>> 24) < 100 || studyEquipment(sx,sy) || cloak[sy][sx]) {
                pixel=nearestLegPixel(sx,sy);
            }
            if (x == 0 || x == legWidth - 1) pixel = darken(pixel, .72);
            result.setRGB(x, y, pixel);
        }
        return result;
    }

    private void drawSegment(Graphics2D g, BufferedImage image, double x1, double y1, double x2, double y2, boolean far) {
        AffineTransform transform = new AffineTransform();
        transform.translate(x1, y1); transform.rotate(Math.atan2(y2 - y1, x2 - x1) - Math.PI / 2);
        transform.scale(1, (Math.hypot(x2 - x1, y2 - y1) + 2) / image.getHeight());
        transform.translate(-image.getWidth() / 2.0, -1);
        g.drawImage(far ? shade(image) : image, transform, null);
    }

    private BufferedImage shade(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, darken(source.getRGB(x, y), profile==null?.78:profile.farShade));
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

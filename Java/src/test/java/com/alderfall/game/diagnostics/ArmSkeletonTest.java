package com.alderfall.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class ArmSkeletonTest {
    public static void main(String[] args) throws Exception {
        AssetStore assets = new AssetStore(Path.of("assets"));
        String[] focus = {"class_mage", "class_rogue", "npc_aria", "npc_calder", "npc_cassia", "npc_maera"};
        BufferedImage review = new BufferedImage(8 * 192, focus.length * 164, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = review.createGraphics(); g.setColor(new Color(28, 34, 40)); g.fillRect(0, 0, review.getWidth(), review.getHeight());
        BufferedImage diagonals = new BufferedImage(4 * 192, 14 * 164, BufferedImage.TYPE_INT_RGB);
        Graphics2D dg = diagonals.createGraphics(); dg.setColor(new Color(28, 34, 40)); dg.fillRect(0, 0, diagonals.getWidth(), diagonals.getHeight());
        int row = 0;
        for (String actor : CharacterAnimationAudit.ACTORS) {
            for (String direction : CharacterAnimationAudit.DIRECTIONS) {
                String name = actor + "_model_" + direction;
                if (!assets.hasSprite(name)) continue;
                int facing = direction.endsWith("left") ? -1 : direction.endsWith("right") ? 1 : 0;
                ArticulatedWalkingMotion rig = new ArticulatedWalkingMotion(assets.spriteFit(name, 192, 144), facing, false, name);
                for (int which = 0; which < 2; which++) {
                    var first = rig.arm(0, which);
                    double upper = upper(first), lower = lower(first);
                    for (int frame = 0; frame < 32; frame++) {
                        var p = rig.arm(frame, which); var next = rig.arm(frame + 1, which);
                        require(Math.abs(upper(p) - upper) < 1e-8 && Math.abs(lower(p) - lower) < 1e-8, "Arm length changes: " + name);
                        require(Math.hypot(next.wristX() - p.wristX(), next.wristY() - p.wristY()) < 4, "Wrist jumps: " + name);
                    }
                    if (facing != 0) require((rig.arm(0, which).wristX() - rig.arm(16, which).wristX())
                            * (rig.leg(0, which).ankleX() - rig.leg(16, which).ankleX()) < 0, "Arm swings with its leg instead of against it");
                }
                if (direction.contains("_")) {
                    var a = rig.leg(0, 0); var b = rig.leg(4, 0);
                    require((b.ankleX() - a.ankleX()) * facing < 0, "Planted diagonal foot slides forward in X");
                    require((b.ankleY() - a.ankleY()) * (direction.startsWith("down") ? 1 : -1) < 0, "Planted diagonal foot slides forward in Y");
                }
            }
            String[] dirs = {"down_left", "down_right", "up_left", "up_right"};
            for (int col = 0; col < dirs.length; col++) {
                dg.setColor(Color.WHITE); dg.drawString(actor + " " + dirs[col], col * 192 + 3, row * 164 + 14);
                dg.drawImage(assets.spriteFit(actor + "_model_" + dirs[col], 192, 144), col * 192, row * 164 + 20, null);
            }
            row++;
        }
        for (int r = 0; r < focus.length; r++) {
            String name = focus[r] + "_model_right";
            ArticulatedWalkingMotion rig = new ArticulatedWalkingMotion(assets.spriteFit(name, 192, 144), 1, false, name);
            g.setColor(Color.WHITE); g.drawString(focus[r] + " | render / joints at frames 0, 8, 16, 24", 4, r * 164 + 14);
            for (int col = 0; col < 8; col++) {
                int frame = col / 2 * 8, ox = col * 192, oy = r * 164 + 20;
                g.drawImage(assets.animatedSpriteFit(name, "walk_articulated", 192, 144, frame), ox, oy, null);
                if (col % 2 == 1) for (int which = 0; which < 2; which++) {
                    var p = rig.arm(frame, which); g.setColor(which == 0 ? Color.CYAN : Color.ORANGE);
                    line(g, ox, oy, p.shoulderX(), p.shoulderY(), p.elbowX(), p.elbowY());
                    line(g, ox, oy, p.elbowX(), p.elbowY(), p.wristX(), p.wristY());
                    dot(g, ox, oy, p.shoulderX(), p.shoulderY()); dot(g, ox, oy, p.elbowX(), p.elbowY()); dot(g, ox, oy, p.wristX(), p.wristY());
                }
            }
        }
        g.dispose(); dg.dispose();
        ImageIO.write(review, "png", Path.of("tools/reviews/characters/arm-rig-review.png").toFile());
        ImageIO.write(diagonals, "png", Path.of("tools/reviews/characters/diagonal-facing-review.png").toFile());
        bellyCheck();
        System.out.println("Arm rig passed: fixed upper/forearm lengths, continuous wrists, diagonal stance direction, and rigid abdomen.");
    }
    private static void bellyCheck() {
        BufferedImage source = new BufferedImage(192,144,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = source.createGraphics(); g.setColor(Color.GRAY); g.fillRect(79,6,34,131); g.dispose();
        ArticulatedWalkingMotion rig = new ArticulatedWalkingMotion(source,1,false,"class_mage_model_right");
        for (int f = 0; f < 32; f++) {
            BufferedImage pose = rig.frame(f);
            int bob = (int) Math.round(-Math.pow(Math.sin(f * Math.PI * 2 / 32), 2));
            for (int y = 61; y < 68; y++) for (int x = 99; x < 102; x++)
                require(pose.getRGB(x,y+bob) == Color.GRAY.getRGB(), "Abdomen pixels are displaced");
        }
    }
    private static double upper(WalkingArmRig.Pose p) { return Math.hypot(p.elbowX()-p.shoulderX(),p.elbowY()-p.shoulderY()); }
    private static double lower(WalkingArmRig.Pose p) { return Math.hypot(p.wristX()-p.elbowX(),p.wristY()-p.elbowY()); }
    private static void line(Graphics2D g,int x,int y,double ax,double ay,double bx,double by) { g.drawLine(x+(int)Math.round(ax),y+(int)Math.round(ay),x+(int)Math.round(bx),y+(int)Math.round(by)); }
    private static void dot(Graphics2D g,int x,int y,double ax,double ay) { g.fillOval(x+(int)Math.round(ax)-2,y+(int)Math.round(ay)-2,4,4); }
    private static void require(boolean value,String message) { if(!value) throw new IllegalStateException(message); }
}

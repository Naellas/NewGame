package com.alderfall.game;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** One body-height and ground baseline across directions, idle, walk and transitions. */
final class WorldCharacterAnimation {
    static final int WIDTH = 192, HEIGHT = 144, FRAMES = 32;
    private final BufferedImage neutral;
    private final BufferedImage[] keys;
    private final PixelMotionTween[] tweens;
    private final boolean robes;
    private final int facing;
    private final boolean authoredWalk;
    private final Map<String, BufferedImage> frames = new HashMap<>();
    private PixelMotionTween start;
    private final PixelMotionTween[] stops = new PixelMotionTween[2];
    private final Map<Integer, PixelMotionTween> settling = new HashMap<>();
    private ArticulatedWalkingMotion articulated;
    private final String name;
    private GroundedWalkingMotion grounded;
    private String groundedProfile="";
    private final Map<Integer,PixelMotionTween> groundedSettles=new HashMap<>();

    static boolean companion(String name) {
        return name.matches("npc_(?:aria|calder|cassia|lyra|maera|rafiq|samir|seraphine|vesper)_model_.*");
    }

    static BufferedImage actionPose(String name, BufferedImage source) {
        return proportions(normalize(source, new Rectangle(0, 0, source.getWidth(), source.getHeight())),
                compactTownModel(name));
    }

    private static boolean compactTownModel(String name) {
        // The ordinary-town atlas has oversized heads; named adventurers already use
        // adult proportions and must not receive a second head/body correction.
        return name.matches("npc_(?:baker|bartender|blacksmith|citizen_man|citizen_woman|innkeeper|merchant|quartermaster)_model_.*");
    }

    static boolean supports(String name) {
        return name != null && name.matches("(?:class_|npc_).*_model_(?:down|up|left|right|down_left|down_right|up_left|up_right)");
    }

    static boolean action(String action) {
        return action == null || action.isBlank() || action.matches("(?:walk_grounded(?:_[0-9]+_[0-9]+(?:_[0-7])?)?|settle_grounded_(?:[0-9]+_[0-9]+_(?:[0-7]_)?)?[0-9]+)") || action.equals("walk") || action.equals("idle")
                || action.equals("walk_alternate") || action.equals("walk_articulated") || action.equals("start_walk") || action.equals("stop_walk")
                || action.matches("settle_(?:(?:alternate|articulated)_)?\\d{1,2}");
    }

    static int frameCount(String action) {
        if ("start_walk".equals(action) || (action != null && action.startsWith("settle_"))) return 12;
        if ("idle".equals(action) || "stop_walk".equals(action)) return 24;
        return FRAMES;
    }

    WorldCharacterAnimation(String name, BufferedImage still, BufferedImage sheet, int count, boolean authoredWalk) {
        this.name = name;
        this.authoredWalk = authoredWalk;
        facing = name.endsWith("_left") ? -1 : name.endsWith("_right") ? 1 : 0;
        robes = name.matches("(?:class_(?:mage|cleric)|npc_(?:lyra|maera|samir|vesper))_model_.*");
        boolean town = compactTownModel(name);
        neutral = proportions(normalize(still, bounds(still)), town);
        count = Math.max(1, count);
        keys = new BufferedImage[count]; tweens = new PixelMotionTween[count];
        int cellW = sheet.getWidth() / count;
        Rectangle shared = null;
        boolean[] clipped = new boolean[count];
        int valid = 0;
        for (int i = 0; i < count; i++) {
            keys[i] = sheet.getSubimage(i * cellW, 0, cellW, sheet.getHeight());
            clipped[i] = clipped(keys[i]);
            if (!clipped[i]) { Rectangle b = bounds(keys[i]); shared = shared == null ? b : shared.union(b); valid++; }
        }
        // Legacy sheets intentionally pack shoulders/capes against cell edges. Only reject
        // isolated damaged cells; edge contact across most of a sheet is not evidence of damage.
        if (valid < Math.max(1, count / 2)) {
            shared = null;
            for (int i = 0; i < count; i++) {
                clipped[i] = false;
                Rectangle b = bounds(keys[i]); shared = shared == null ? b : shared.union(b);
            }
        }
        for (int i = 0; i < count; i++) if (!clipped[i]) keys[i] = proportions(normalize(keys[i], shared), town);
        // Do not display broken cells whose torso/weapon was cut at the source-sheet boundary.
        for (int i = 0; i < count; i++) if (clipped[i]) {
            int before = 1, after = 1;
            while (clipped[Math.floorMod(i - before, count)]) before++;
            while (clipped[(i + after) % count]) after++;
            keys[i] = new PixelMotionTween(keys[Math.floorMod(i - before, count)], keys[(i + after) % count])
                    .at(before / (double) (before + after));
        }
    }

    BufferedImage frame(String action, int frame) {
        if (action == null || action.isBlank()) return neutral;
        int selected = Math.floorMod(frame, frameCount(action));
        String key = action + ":" + selected;
        BufferedImage cached = frames.get(key);
        if (cached != null) return cached;
        BufferedImage result;
        if(action.startsWith("walk_grounded")||action.startsWith("settle_grounded")) {
            String[] parts=action.split("_"); boolean settle=action.startsWith("settle");
            int speed=parts.length>3?Integer.parseInt(parts[2]):10;
            int height=parts.length>3?Integer.parseInt(parts[3]):66;
            int directionCode=parts.length>=(settle?6:5)?Integer.parseInt(parts[4]):-1;
            String motionName=directionCode<0?name:name.replaceFirst("_model_.*$","_model_"+new String[]{"down","down_left","left","up_left","up","up_right","right","down_right"}[directionCode]);
            String profile=speed+":"+height+":"+directionCode;
            if(!profile.equals(groundedProfile)) {
                grounded=new GroundedWalkingMotion(neutral,facing,robes,motionName).configure(Math.max(.5,Math.min(2,speed/10.0)),Math.max(40,Math.min(100,height)));
                groundedProfile=profile;groundedSettles.clear();frames.keySet().removeIf(k->k.contains("grounded"));
            }
            if(settle) {
                int pose=Math.floorMod(Integer.parseInt(parts[parts.length-1]),FRAMES);
                result=groundedSettles.computeIfAbsent(pose,p->new PixelMotionTween(grounded.frame(p),neutral)).at(selected/11.0);
            } else result=grounded.frame(selected);
            frames.put(key,result);return result;
        }
        if (action.startsWith("settle_")) {
            boolean alternate = action.startsWith("settle_alternate_");
            boolean jointed = action.startsWith("settle_articulated_");
            int pose = Math.floorMod(Integer.parseInt(action.substring(action.lastIndexOf('_') + 1)), FRAMES);
            result = settling.computeIfAbsent(pose + (jointed ? FRAMES * 2 : alternate ? FRAMES : 0),
                    p -> new PixelMotionTween(jointed ? articulatedWalk(pose) : alternate ? alternateWalk(pose) : walk(pose), neutral)).at(Math.min(11, selected) / 11.0);
            frames.put(key, result); return result;
        }
        switch (action) {
            case "walk" -> result = walk(selected);
            case "walk_alternate" -> result = alternateWalk(selected);
            case "walk_articulated" -> result = articulatedWalk(selected);
            case "start_walk" -> {
                if (start == null) start = new PixelMotionTween(neutral, walk(4));
                result = start.at(selected / 11.0);
            }
            case "stop_walk" -> {
                int plant = selected / 12;
                if (stops[plant] == null) stops[plant] = new PixelMotionTween(walk(plant * FRAMES / 2), neutral);
                result = stops[plant].at((selected % 12) / 11.0);
            }
            default -> result = CharacterMotion.frame(neutral, "idle", selected);
        }
        frames.put(key, result); return result;
    }

    /** Town cutouts use a shorter head and longer body within the same ground-aligned canvas. */
    private static BufferedImage proportions(BufferedImage source, boolean town) {
        if (!town) return source;
        Rectangle b = bounds(source);
        BufferedImage out = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double center = b.getCenterX();
        for (int y = b.y; y < b.y + b.height; y++) {
            double t = (y - b.y) / (double) b.height;
            double sy = t < .17 ? t * .25 / .17 : .25 + (t - .17) * .75 / .83;
            double widthScale = .86 * (t < .17 ? .82 + .18 * t / .17 : 1);
            for (int x = 0; x < WIDTH; x++) {
                int sx = (int) Math.round(center + (x - center) / widthScale);
                int iy = Math.min(HEIGHT - 1, b.y + (int) Math.round(sy * b.height));
                if (sx >= 0 && sx < WIDTH) out.setRGB(x, y, source.getRGB(sx, iy));
            }
        }
        return out;
    }

    private BufferedImage walk(int frame) {
        double position = frame * keys.length / (double) FRAMES;
        int index = (int) position; double fraction = position - index;
        if (fraction < .0001) return authoredWalk ? keys[index] : WalkingStride.frame(keys[index], frame, facing, robes);
        if (tweens[index] == null) tweens[index] = new PixelMotionTween(keys[index], keys[(index + 1) % keys.length]);
        BufferedImage pose = tweens[index].at(fraction);
        return authoredWalk ? pose : WalkingStride.frame(pose, frame, facing, robes);
    }

    private BufferedImage alternateWalk(int frame) {
        // Keep one drawing throughout the cycle to avoid changing limb/equipment identity.
        return AlternateWalkingMotion.frame(neutral, frame, facing, robes);
    }

    private BufferedImage articulatedWalk(int frame) {
        if (articulated == null) articulated = new ArticulatedWalkingMotion(neutral, facing, robes, name);
        return articulated.frame(frame);
    }

    private static BufferedImage normalize(BufferedImage source, Rectangle bounds) {
        BufferedImage out = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double scale = Math.min(132.0 / Math.max(1, bounds.height), 180.0 / Math.max(1, bounds.width));
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.translate(WIDTH / 2.0 - bounds.getCenterX() * scale, 138 - (bounds.y + bounds.height) * scale);
        g.scale(scale, scale); g.drawImage(source, 0, 0, null); g.dispose();
        return out;
    }

    static boolean clipped(BufferedImage image) {
        int left = 0, right = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            if ((image.getRGB(0, y) >>> 24) > 100) left++;
            if ((image.getRGB(image.getWidth() - 1, y) >>> 24) > 100) right++;
        }
        return Math.max(left, right) > Math.max(4, image.getHeight() / 10);
    }

    private static Rectangle bounds(BufferedImage image) {
        int left = image.getWidth(), right = -1, top = image.getHeight(), bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if ((image.getRGB(x, y) >>> 24) > 32) {
                left = Math.min(left, x); right = Math.max(right, x); top = Math.min(top, y); bottom = Math.max(bottom, y);
            }
        }
        return right < left ? new Rectangle(0, 0, image.getWidth(), image.getHeight()) : new Rectangle(left, top, right-left+1, bottom-top+1);
    }
}

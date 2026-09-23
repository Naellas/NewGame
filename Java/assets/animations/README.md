# Battle Animation Strips

Pixelorama exports can go in this folder as horizontal PNG frame strips.

Name them with this pattern:

```text
<sprite>_<action>_anim.png
```

Examples:

```text
class_mage_model_cast_anim.png
class_knight_model_attack_anim.png
class_ranger_model_shoot_anim.png
skeleton_attack_anim.png
spider_hit_anim.png
```

Supported action names are `idle`, `attack`, `cast`, `shoot`, `defend`, `item`, and `hit`.

Use a single row of frames. Six frames is fine for quick attacks; 10 frames is better for casts and larger movement arcs.

For strips that are not six frames, add a sibling metadata file containing only the frame count:

```text
class_mage_model_cast_anim.png
class_mage_model_cast_anim.frames
```

The game falls back to the normal static sprite when a matching animation file is missing.

For padded strips, an optional sibling `<sprite>_<action>_anim.framebounds` file
can define a shared viewport inside every frame as `x y width height` in source
pixels. The same rectangle is used for every pose, preserving relative motion
and the ground baseline. Invalid or out-of-frame rectangles are ignored. This
does not change the sheet's equal-width frame division; specify `.frames` too.

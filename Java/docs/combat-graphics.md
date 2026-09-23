# Combat graphics

The battlefield uses shared foot anchors for sprites and effects. Party cards remain stationary during recoil; the active actor has a gold border. Group size controls enemy scale and placement (five enemies use two rear and three front slots). The bottom 180 logical pixels are reserved for actions and the log, arranged side by side.

Projectiles reach their target on the same frame as damage resolution. Chain hops and staggered multi-target hits use individual collision frames. Each collision has a fading core, expanding ring and directional debris; elemental effects add fire, frost shards or poison droplets, and healing uses softer rising motes. Area attacks also show a shared expanding wave. Hit flashes use the sprite's alpha silhouette, including its animation pose.

The environment library now contains 23 battle backgrounds. The latest pass regenerated seven legacy backgrounds and added twelve, alongside the four earlier meadow and biome-transition scenes. Browse `docs/battle-environments.html` to compare all 19 images from this pass.

Outdoor scenes cover grassland plains, woodland clearings, desert flats, frostfields, alpine uplands, marshland, badlands and beaches. The meadow-hills, forest-edge, coastal-meadow and desert-foothills variants remain available. Roads use a weighted sample of nearby land biomes rather than always showing grassland.

Caves inherit the dungeon profile's exterior biome: temperate limestone, forest roots, desert sandstone, glacier ice, marsh shale, volcanic basalt, coastal sea cave or mountain quartz. Dungeon themes take priority: crypts use burial chambers, sewers use maintenance platforms, and castles/prisons use the stone hall. Unknown dungeon profiles retain their fallback. Beach and submerged-sand tiles participate in exterior-biome detection.

Selection occurs once when the battle is created, checks map bounds and does not consume combat RNG. Artwork was generated using built-in image_gen. PNGs and exact prompts are in `assets/environments/battle/`; see `environment-refresh-prompts.md` for this pass and `combat-background-prompts.md` for the earlier transitions.

Run `com.alderfall.game.BattleSceneryTest` for biome mapping, road selection, dungeon precedence and asset decoding/cover checks.

Compile the Java project, then run `java -Djava.awt.headless=true -cp out com.alderfall.game.CombatGraphicsTest` from `Java` (quote the `-D` argument in PowerShell). The diagnostic checks formation bounds/overlap, backdrop selection and per-target damage/VFX timing, then exports actual GamePanel renders to `exports/combat-graphics/` for review. These are still captures; animation feel should also be reviewed in play.

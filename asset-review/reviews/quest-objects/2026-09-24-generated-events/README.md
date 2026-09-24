# Generated world quest objects

Accepted at 100% game zoom: leather purse, roadside ambush brush, sealed supply
cache and document/ledger bundle. Captures use the actual Swing renderer.

- [Road objects](road-objects.png): purse near the player, brush centered on the cobbled road.
- [Ambush popup](ambush-popup.png): entering the brush opens Fight before combat.
- [Purse interaction](deliberate-interaction.png): choices appear only after interaction.
- [Supply objectives](greyharbor_cache_run.png): three caches in Caches in the Reed Fog.
- [Document objective](camp_ledger.png): readable evidence in Ledger Under Canvas.

Reproduce from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/quest-objects/classes
Set-Location Java
java '-Djava.awt.headless=true' -cp temp/quest-objects/classes com.alderfall.game.WorldGenerationPreview temp/quest-objects/captures roaming
```

The exporter deliberately places valid events and enables the named quest stages
for review; it does not modify saves. Original sources, exact prompts and the
repeatable import recipe live under art-source/quest-objects/2026-09-24-world-events.

Validation: Java build, RoamingWorldEventTest, BattleDefeatTest, RenderCacheTest
and scripts/check.ps1 passed. Asset alpha/visibility, explicit purse activation,
ambush step/Fight/Escape handling, one-time rewards and same-biome selection are
covered. This adds artwork to existing quests, not new quest storylines.

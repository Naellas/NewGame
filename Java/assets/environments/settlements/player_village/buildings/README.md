# Player settlement buildings

Player-owned building art lives here, grouped by gameplay style. This placement
is the user's requested separation from NPC village/city architecture. Stable
asset IDs still allow NPC settlements to reuse older village-building sprites.

Each of the 18 playable types has six distinct stages: Camp, Small Village,
Village, Small Town, Large Town, Metropolis. Cottage, Workshop and Hall use the
reference's `settlement_residence`, `settlement_workshop` and `settlement_civic`
families in `shared/`. The other 15 styles use `settlement_<style>_tier1` through
`tier6` beneath their own folders. Footprints and saved building keys stay stable.

The relocated 54 legacy sprites retain their original filenames and bytes for
existing callers, imports and old authored maps. The 18 reference sprites also
retain their IDs. `config/asset-placements.json` routes both former shared paths
and older village paths here. No runtime duplicates or archived files were deleted.

[Visual comparison](../../../../../tools/reviews/player-settlement/index.html).
[Generation and relocation record](../../../../../../art-source/settlement-growth/2026-09-24-specialist-tiers/README.md).

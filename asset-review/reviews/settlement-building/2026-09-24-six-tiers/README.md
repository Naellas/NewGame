# Settlement building review

`manage.png` and `terrain.png` are actual 1920x1080 game renders from
SettlementPanelTest, using daylight and creative construction for the fixture.
The test invokes the Manage upgrade, terrain Raise and Height brush controls
and checks that a world click changes saved height data.

Reproduce with the build and SettlementPanelTest commands in
[the building guide](../../../../Java/docs/settlement-building.md). Captures are
written to `Java/temp/settlement-upgrade/`; these are the selected review copies.

Accepted: readable searchable management rows, costs/staffing/tier display,
terrain catalog thumbnails, generated architectural silhouettes and protected
height editing. Original sheets and exact prompts are in
[the source batch](../../../../art-source/settlement-growth/2026-09-24-six-tiers/README.md).
The art uses shared architectural families rather than distinct advanced art for
every trade. Paving patterns repeat; perfect texture edge matching is not claimed.

Tests: SettlementBuildTest and SettlementPanelTest pass on Java 21; shared water
and world-elevation diagnostics pass. The broader VillageTerrainTest pixel
comparison remains unresolved, as recorded in the building guide.

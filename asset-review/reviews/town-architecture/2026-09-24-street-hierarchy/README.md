# Town street hierarchy review

Briarbridge capture from TownLayoutTest --render, seed 0, daytime. Reproduce from
Java with scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-streets/classes,
then java -Djava.awt.headless=true -cp temp/town-streets/classes com.alderfall.game.TownLayoutTest --render.
Bulk seven-town captures remain in Java/temp/town-layout/review.

Reviewed: continuous public paving, cobble/worn stone/dirt hierarchy, regional
underlying ground, hedge continuity and road-facing upright lamp variants.
Existing winding street geometry is retained; the reference's compact radial
building blocks and rounded enclosure are not implemented by this pass.

Validation: production build, TownLayoutTest, TownExteriorRoomsTest,
RegionalSettlementTest --layout-only and TownNpcNavigationTest passed.
RoadSurfaceTest passes town material differentiation but later fails its sand
wrap-edge assertion. This also failed with elevation disabled in a temporary
probe; no assertion was weakened. Full repository check is blocked by existing
.vscode/settings.json; review links and structure checks pass separately.
The local JDK intermittently crashed; successful builds/tests used C1 or C2-only
compilation for the process, without changing game launch settings.

# Regional planting and town atmosphere

Actual GamePanel overviews at noon, 40% zoom, using existing runtime artwork.
All seven towns receive composed planting pockets near homes and suburban lots,
regional garden terrain, park trees, market goods and workshop details.
Greyharbor's snowy park and northern fountain have been corrected. Town use of
the legacy scrub-pine image (actually crystals) has been replaced with pine art.

Reproduce from Java/:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -IncludeTests -OutputDirectory temp/town-feeling/classes
java -XX:-TieredCompilation '-Djava.awt.headless=true' -cp temp/town-feeling/classes com.alderfall.game.TownLayoutTest --render
```

Captures are exported to temp/town-layout/review; copy the seven town overview
PNGs here. The exporter also produces suburb and cardinal-house closeups.

Review status: visually inspected by agent; user acceptance pending.
Validation: town layout, regional settlement connectivity, cardinal entrances,
exterior furniture/climate checks and NPC navigation. Normal main build succeeds.
Repository check reports the existing unrelated .vscode/settings.json violation;
no baseline or personal settings were changed.

- [briarbridge](town_briarbridge.png)
- [ironvale](town_ironvale.png)
- [moonspire](town_moonspire.png)
- [reedwatch](town_reedwatch.png)
- [embermarket](town_embermarket.png)
- [northwatch](town_northwatch.png)
- [greyharbor](town_greyharbor.png)

A HotSpot native access violation occurred during one exterior test run. The
final climate/furniture test was rerun with -Xint (interpreter mode); crash logs
remain in ignored Java/temp/town-feeling. No game VM settings were changed.


Working-plot refinement adds one barn/stable per town, three regional imagegen
sprites, enterable feed stores, a fenced paddock, herb gardens and smith workyards.
This is scenery and existing interior functionality, not livestock simulation.
Final exterior checks also passed using -XX:-TieredCompilation after a C1 VM crash.

Barn paddocks remain inside the south wall, within ten tiles of their barn doors.
A trial suburb placement was rejected because regional waterways separated barns
from available paddocks. The final lower-quarter infill preserves short roads.

Suburb views:

- [briarbridge suburbs](town_briarbridge-suburbs.png)
- [ironvale suburbs](town_ironvale-suburbs.png)
- [moonspire suburbs](town_moonspire-suburbs.png)
- [reedwatch suburbs](town_reedwatch-suburbs.png)
- [embermarket suburbs](town_embermarket-suburbs.png)
- [northwatch suburbs](town_northwatch-suburbs.png)
- [greyharbor suburbs](town_greyharbor-suburbs.png)

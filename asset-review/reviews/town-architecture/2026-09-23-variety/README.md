# Town variety and scale review
One actual Swing game capture for every town/asset role (84 unique sprites).
Reproduce from Java/ after compiling test roots:
~~~powershell
java -Djava.awt.headless=true -cp temp/town-variety/classes com.alderfall.game.TownArchitectureTest --render
~~~
The diagnostic stops the game timer, chooses Mage, sets high quality and daytime,
then places the camera by an existing entrance for each distinct role at zoom 80.
It does not save player state. Sprite routing is validated across seeds 0, 42, 2026.
Acceptance: twelve used sprites per town, transparent backgrounds, clear regional
styles, whole buildings drawn once, bounded overhang, readable entrances and paths.
These captures are curated visual evidence, not animation intermediates.

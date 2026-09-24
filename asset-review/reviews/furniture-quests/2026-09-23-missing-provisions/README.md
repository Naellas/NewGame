# Missing Provisions furniture interactions

Status: visually reviewed. [Ledger stage](ledger.png) shows a paper detail and
visible objective highlight on the actual pantry shelf. [Delivered supplies](delivered.png)
shows the returned parcel on the connected counter and the report marker on Senn.
The exporter sets preview stages directly; FurnitureQuestTest separately exercises
the actual interaction and save flows. No new art or imported assets were created.

Reproduce with the build and InteriorDesignPreview furniture-quest commands in
[the feature documentation](../../../../Java/docs/furniture-quests.md).
The exporter also captures the released and carried parcel stages under ignored
Java/temp/furniture-quests/captures. Captures use the actual generated Oakhaven inn
at seed 0 and the game's normal renderer; local display preferences can affect
materials. The existing interior exporter still accepts its normal theme names.

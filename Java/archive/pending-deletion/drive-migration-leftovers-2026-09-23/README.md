# Leftovers from the move to D:

The D: workspace contained the completed organization plus 3,122 files at retired
locations. Every active file from the completed C: workspace was present and
byte-identical. These additional files map to recorded migrations and were moved
here unchanged to prevent duplicate Java classes, obsolete tools, and old asset
families from becoming active again. Nothing was permanently deleted.

manifest.json records original relative paths, hashes, sizes, and intended
migration destinations. Some destinations were retired in an earlier archive
batch; a replacement path is historical context, not a promise of a live file.
To recover a file, verify its checksum and copy it from files/<original path>
into an isolated Java/temp task folder for review. Do not restore the whole batch
over active files: that would recreate the duplicate-class/build failure.

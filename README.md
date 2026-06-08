# Echoes of Alderfall

The active project is the Java port in `Java/`.

The old `Python/` prototype has been migrated for runtime purposes: assets, gameplay config, asset tools, and planning notes now live under `Java/`. Keep `Python/` only as a temporary review copy until you are ready to delete it from the workspace.

## Repository Hygiene

Generated build output, Java save/settings files, map-editor exports, compiled `.class` files, and Python tool caches are ignored from the root `.gitignore`.

Some generated artifacts may still be tracked from earlier commits. To stop tracking them while keeping the local files on disk, run this as a separate intentional cleanup step:

```powershell
git rm -r --cached out Java/tools/__pycache__
```

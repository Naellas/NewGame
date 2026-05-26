# Legacy Asset Tools

These scripts were copied from the Python prototype so the top-level `Python/` folder can be removed without losing the asset pipeline history.

Run tools from the `Java` project root:

```powershell
python tools/generate_cloud_assets.py
python tools/extract_sheet_assets.py
```

Some extraction and fidelity-refresh scripts require Pillow. The Java runtime does not use these scripts.

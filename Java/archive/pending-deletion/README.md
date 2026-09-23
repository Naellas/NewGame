# Pending deletion archive

Quarantine for retired project files awaiting a separate deletion decision.
Nothing here is loaded by the game. No automatic purge or expiry is configured.

`legacy-assets/` contains the existing 437-file cleanup bundle moved intact from
root `asset-review/` on 2026-09-23. Its manifest retains original repository paths,
reasons, replacement paths where known, byte sizes, and SHA-256 hashes. The
`verification.json` describes the original cleanup, not a new runtime migration.

Validate without restoring, from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File Java/archive/pending-deletion/legacy-assets/restore.ps1 -ValidateOnly
```

Omit `-ValidateOnly` to restore after reviewing the manifest. The script validates
the whole batch and refuses overwrites before moving any files.

New batches must have a README and `manifest.json` with original `path`, `reason`,
optional `replacement`, `bytes`, and `sha256`; store files under `files/<original
path>`. Check all consumers before quarantining runtime assets. Keep related
animation sidecars together. A duplicate hash alone is not permission to retire
a logical asset ID. Permanent deletion is a separate reviewed operation.

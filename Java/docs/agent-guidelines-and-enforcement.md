# Agent guidelines and enforcement

The repository's [AGENTS.md](../../AGENTS.md) defines placement, reuse, validation,
and cleanup rules. Codex discovers `AGENTS.md` instructions; more specific files
can override broader guidance. Start a fresh session after changing instructions
to ensure they are loaded. Other agent products may require configuration to read
this file. See the [official OpenAI documentation](https://learn.chatgpt.com/docs/agent-configuration/agents-md).

Instructions guide behavior; they are not a security boundary or a guarantee.
Use automated checks for objective rules and review for design decisions.

## What is installed in this working tree

- Root `AGENTS.md`: one short, shared policy for all project work.
- `Java/tools/checks/check_repository_hygiene.py`: a read-only, standard-library checker.
- `Java/tools/tests/test_repository_hygiene.py`: policy regression tests.
- `Java/tools/checks/check_review_site.py` and `test_repository_reviews.py`: local HTML
  link checks, retired Java exporter-path checks, and archive integrity checks.
- `Java/tools/checks/check_structure.py`: checks registered tools, internal imports,
  duplicate Java class identities, and ambiguous runtime asset stems.
- `Java/scripts/check.ps1`: one local entrypoint for the policy tests and all three checkers.
- `Java/config/repository-hygiene-baseline.json`: exact-path exceptions for the
  pre-existing working tree, including untracked work present at introduction.
- `.github/workflows/repository-hygiene.yml`: runs tests and the checker on pushes
  and pull requests once committed/pushed and GitHub Actions is enabled.

Run locally from the repository root:

```powershell
python Java/tools/checks/check_repository_hygiene.py
python Java/tools/checks/check_review_site.py
python Java/tools/checks/check_structure.py
python -m unittest discover -s Java/tools/tests -p "test_repository_*.py"
```

The checker finds tracked files (including force-added ignored files) and
non-ignored untracked files. It rejects new policy violations involving root
locations, runtime `source`/`sources`/`character-refresh` paths, compiled files,
tracked scratch/build/save/export files, and tracked personal settings. Deleted
files are skipped. Existing exceptions are exact rule/path pairs: they do not
allow new siblings. It returns 0 for pass, 1 for new violations, 2 for a check
that could not run. It does not modify files or update its own baseline.

The relocated review families may not grow back under `asset-review/`; only the
old character bookmark redirect is permitted. Review integrity checks cover
literal HTML links and archive checksums, while browser smoke checks cover
dynamic images, embedded tabs, and local-file behavior. Browser screenshots go
to ignored `Java/temp/review-checks/`. Archived objects live under
`Java/archive/pending-deletion/`; 437 legacy candidates have an exact-file
manifest and a restore script with `-ValidateOnly` support.
Three retired tools also have original-path checksum manifests in their own
archive batch. Retirement requires evidence about callers and replacement
workflows; an old modification date alone is insufficient.

Layout checks reject test/review diagnostics in production sources, new flat
tool files, and recreation of retired asset-family roots. Structural checks
reject new ambiguous asset stems; six existing duplicate stems are explicitly
bounded in `asset-duplicate-stems.json`. The 731 hygiene exceptions were moved
with their files without expanding the exception set.

The baseline is a snapshot of migration debt, not an endorsement. Remove obsolete
entries during cleanup. Deliberate additions or policy changes must have a
reviewable rationale; there is no refresh-baseline command.

## What this does not enforce

The checker does not establish asset reachability, detect semantic duplicate
tools, check image quality, validate every metadata sidecar, verify README
completeness, or enforce all folder conventions described in AGENTS.md. It also
does not inspect ignored local output directories, so the rule against creating
more `out-*` folders is agent guidance rather than a filesystem prohibition.
Path-based exceptions allow edits to grandfathered files. A local check reads
the working tree, not a frozen Git index; partial staging can differ. CI checks
the committed snapshot.

These limitations are intentional for the first version: prevent a few concrete
forms of growth without blocking the existing project. Add stricter checks as
the asset/source migration and manifests become established.

## Making it a merge gate

The workflow file is prepared locally. No GitHub repository settings, hooks,
commits, or pushes were changed by this task.

For server-side enforcement, the repository administrator should configure the
target branch to require a pull request and the successful `repository-hygiene`
job, with bypass/direct-push permissions limited appropriately. Require review
for changes to `AGENTS.md`, the checker/tests, baseline, ignore rules, and workflow.
Actual enforcement depends on the repository's available rules and permissions.

An optional local pre-commit hook can run the same command for earlier feedback,
but hooks are bypassable and must be installed per checkout. Avoid treating one
as the only gate. CI is also bypassable if contributors can disable its workflow
or change its baseline without independent review.

Review should answer three questions: did we reuse the existing responsibility;
are sources/runtime assets/review evidence separated; and can another developer
reproduce the result from the recorded commands? Passing this initial checker
does not answer those design questions.

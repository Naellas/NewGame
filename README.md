# Echoes of Alderfall

The active project is the Java port in `Java/`.

Worldbuilding: [Setting and story bible](Story%20premise/world-framework.md) and [NPC and companion rewrite brief](Story%20premise/narrative-rewrite-brief.md).

World encounters: [Faith, cults, camps, and dungeons](Story%20premise/faith-cults-and-dungeons.md) and [Regional bestiary and bosses](Story%20premise/regional-bestiary-and-bosses.md).

Implementation planning: [Quest objective refinement](Java/docs/quest-objective-refinement-plan.md).

The old `Python/` prototype has been migrated: assets, gameplay config, asset tools, and planning notes now live under `Java/`.

## Repository Hygiene

Planning: [Repository organization, duplicate findings, and proposed structure](Java/docs/repository-organization-report.md).

Agent guidance: [Working rules](AGENTS.md) and [automated checks and enforcement](Java/docs/agent-guidelines-and-enforcement.md).

Developer tools: [Character and monster review](Java/tools/reviews/characters/index.html),
including movement physics, dialogue, and calibration subsites.
Deletion candidates: [Archive and restoration instructions](Java/archive/pending-deletion/README.md).

Cleanup progress and validation: [Implemented layout](Java/docs/repository-organization-implementation.md).

Current source/tool/asset structure: [Second cleanup pass and tool retirement](Java/docs/structure-and-tool-retirement.md).

Generated build output (`out/`, `Java/out/`, and `Java/out-*/`), local saves (`saves/` and `Java/saves/`), Java settings, exports, scratch folders (`temp/` and `Java/temp/`), compiled `.class` files, Python tool caches, and JVM profiling/crash output are ignored from the root `.gitignore`.

Build output and caches can be deleted when the game and development tools are closed; the build scripts recreate them. Deleting saves removes local play progress. Review exports and scratch files for anything worth keeping before clearing them.

Keep game assets, configuration defaults, source code, tools, story documents, and the `asset-review/` archive as project material.

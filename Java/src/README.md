# Java source roots

| Root | Contents | Included by |
| --- | --- | --- |
| `main/java` | Game/runtime classes only | Every build |
| `test/java` | Assertion-based diagnostics | `build.ps1 -IncludeTests` |
| `review/java` | Visual exporters, audits, benchmarks | `build.ps1 -IncludeReviews` |
| `testSupport/java` | Shared animation roster/audit support | Either development switch |

Package declarations were preserved. A folder under a source root does not
necessarily represent a distinct package yet. Main code must never depend on
test, review, or test-support classes. `DebugMetrics` stays in main because the
live render metrics use it.

From `Java/`, run `scripts/test.ps1` for the smoke test, or pass `-Test` with a
fully qualified test class. Each invocation uses fresh ignored output under
`temp/tests`. The smoke test uses isolated saves and a link to runtime assets.

For a combined development build:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -IncludeTests -IncludeReviews -OutputDirectory temp/checks/classes
java -cp temp/checks/classes com.alderfall.game.CharacterAnimationTest
```

`-IncludeReviews` also compiles the experimental study sources kept beside their
sites in `tools/reviews`. Normal `run.ps1` builds only main sources. Never add
test classes to main merely to make a classpath command work; use the appropriate
build switches. Existing `smoke-test`, `benchmark-render`, `dialogue-qa`, and
`skill-tree-audit` scripts select their required source roots.

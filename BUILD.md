# Build and Setup

## Prerequisites

- JDK 21 (`java -version`). The project's Gradle toolchain is pinned to 21 because that's what's installed; there's nothing 21-specific in the code.
- Nothing else - `./gradlew` (checked in) downloads Gradle 8.7 itself on first run.

## Build

```bash
./gradlew :plugin:build          # compiles the plugin and runs its tests
./gradlew :sample-app:compileKotlin  # compiles the sample app with the plugin attached
```

Build outputs:
- Plugin jar: `plugin/build/libs/plugin-0.1.0.jar`
- Sample app classes: `sample-app/build/classes/kotlin/main/`

## Run tests

```bash
./gradlew :plugin:test
```

Test report: `plugin/build/reports/tests/test/index.html`. Raw JUnit XML: `plugin/build/test-results/test/`.

## Kotlin/Gradle version pinning

Both are pinned deliberately, not arbitrarily:

- **Kotlin 2.1.10** everywhere (root `build.gradle.kts`, `plugin/build.gradle.kts`, `sample-app/build.gradle.kts`) - chosen to match the Kotlin version that `dev.zacsweers.kctfork:core:0.7.1` (the test harness) bundles. The FIR extension API used by the plugin is internal/unstable across Kotlin versions, so plugin code, the compiler it's tested against, and the compiler a consumer builds with must all agree on the exact version.
- **Gradle 8.7** via the wrapper - arbitrary recent stable version, no specific requirement.

If you bump the Kotlin version, bump it in all three `build.gradle.kts` files and re-check `dev.zacsweers.kctfork:core`'s pinned Kotlin version (its POM declares the version it was built against - mismatches can cause `NoSuchMethodError`/`LinkageError` at test time even when everything compiles).

## Clean build

```bash
./gradlew clean build
```

# Quick Start

## Prerequisites

- JDK 21 (the project's Gradle toolchain is pinned to 21; see [BUILD.md](BUILD.md))
- No local Kotlin/Gradle install needed - `./gradlew` is checked in and downloads what it needs

## Run the tests

```bash
./gradlew :plugin:test
```

This compiles small Kotlin snippets through the real plugin (via `kotlin-compile-testing`) and asserts on the diagnostics it emits. See `plugin/src/test/kotlin/com/compile/sast/SastRulesTest.kt`.

## See it catch something

```bash
./gradlew :sample-app:compileKotlin
```

`sample-app/src/main/kotlin/com/compile/sast/sample/VulnerableSamples.kt` contains both vulnerable and safe examples for each rule. This build is expected to **fail** - SAST001 and SAST002 are ERROR severity:

```
e: .../VulnerableSamples.kt:14:5 SAST001 Hardcoded secret: property 'apiKey' is initialized with a hardcoded secret-shaped literal
e: .../VulnerableSamples.kt:25:22 SAST002 Weak cryptographic algorithm: 'MD5' uses weak algorithm/mode 'MD5'
```

Remove the vulnerable classes from that file (or comment them out) and the build succeeds.

## Using this in another project

There's no published Gradle plugin yet (see Roadmap in [README.md](README.md)). For now, wire the plugin jar in directly, the same way `sample-app/build.gradle.kts` does:

```kotlin
val pluginJar = project(":plugin").tasks.named<Jar>("jar")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(pluginJar)
    compilerOptions.freeCompilerArgs.addAll(
        "-Xplugin=${pluginJar.get().archiveFile.get().asFile.absolutePath}"
    )
}
```

Or via raw `kotlinc`:

```bash
kotlinc -Xplugin=path/to/plugin.jar MyFile.kt
```

## What each rule actually checks

See the table in [README.md](README.md) - each rule resolves the call/declaration through the compiler's FIR tree rather than matching source text, so e.g. `MessageDigest.getInstance("SHA-256")` and `getInstance("MD5")` are told apart by evaluating the constant argument, not by a generic "any string near getInstance" heuristic.

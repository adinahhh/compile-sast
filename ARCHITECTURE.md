# Architecture

## Overview

compile-sast registers Kotlin K2 FIR checkers, the same extension points the compiler itself uses for diagnostics like unresolved references or type mismatches. There is no separate analysis pass over the PSI tree and no custom scanner; the checks run inside the compiler's own resolution/checking phase, so they see fully-resolved types and constant-evaluated literals.

## Registration chain

1. **`SastCompilerPluginRegistrar`** (`org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar`)
   Declared via `META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar`. Sets `supportsK2 = true` and calls `FirExtensionRegistrarAdapter.registerExtension(SastFirExtensionRegistrar(config))`.

2. **`SastFirExtensionRegistrar`** (`org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar`)
   In `configurePlugin()`, registers a `(FirSession) -> SastAdditionalCheckersExtension` factory via the `+` operator.

3. **`SastAdditionalCheckersExtension`** (`org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension`)
   Exposes an `ExpressionCheckers` instance with all `FirFunctionCallChecker` instances, and a `DeclarationCheckers` instance with the `FirPropertyChecker`. These sets tell the FIR checker pipeline which checkers to invoke for which AST node kind.

4. **Checkers** (`com.compile.sast.rules.*`)
   See [Checker patterns](#checker-patterns) below.

5. **`SastDiagnostics`**
   Declares diagnostic factories (`error1`/`warning1` from `org.jetbrains.kotlin.diagnostics`) and registers renderers with `RootDiagnosticRendererFactory`. Each diagnostic message includes the SAST rule ID and CWE number (e.g. `SAST003 [CWE-89]`). Checkers report via `DiagnosticReporter.reportOn(source, factory, message, context)`, the same mechanism built-in compiler checkers use, so findings appear as ordinary `error:`/`warning:` lines in kotlinc/Gradle output.

## Checker patterns

All 9 rules fall into one of three patterns:

### Pattern A: dangerous sink + non-constant argument
Resolve the callee by FQN; if the first argument (or all arguments) is not a compile-time constant string, flag it. Used by: SQL injection, command injection, SSRF, path traversal.

```
toResolvedCallableSymbol() -> FirNamedFunctionSymbol / FirConstructorSymbol
containingClassLookupTag()?.classId in KNOWN_DANGEROUS_CLASSES
argument is not FirLiteralExpression(kind=String)  ->  report
```

`ProcessBuilder` and `Paths.get()` take vararg parameters; FIR wraps the individual elements in a `FirVarargArgumentsExpression`, which must be flattened before checking each element's constant-ness.

### Pattern B: known-weak token match
Resolve the callee by FQN; if the constant-evaluated string argument names a known-weak algorithm/protocol, flag it. Used by: weak crypto, weak TLS.

```
toResolvedCallableSymbol() -> FirNamedFunctionSymbol (getInstance)
containingClassLookupTag()?.classId == TARGET_CLASS
argument as FirLiteralExpression -> value in WEAK_TOKENS  ->  report
```

Weak TLS uses exact equality (not `contains`) because `"TLSv1"` is a substring of the safe `"TLSv1.2"`.

### Pattern C: dangerous call, no argument inspection
Resolve the callee by FQN; the presence of the call itself is the signal, regardless of arguments. Used by: insecure deserialization.

```
toResolvedCallableSymbol() -> FirNamedFunctionSymbol
containingClassLookupTag()?.classId in DANGEROUS_CLASSES
name in DANGEROUS_METHOD_NAMES  ->  report unconditionally
```

### Declaration checker
`HardcodedSecretChecker` is a `FirPropertyChecker` (not a `FirFunctionCallChecker`). It inspects `FirProperty.initializer` for a `FirLiteralExpression` of kind `ConstantValueKind.String` whose value matches a secret-shaped regex. `LoggingSensitiveDataChecker` extends Pattern A by additionally inspecting arguments as `FirQualifiedAccessExpression` instances and checking whether the resolved symbol's name matches the secret-name pattern.

## Why FIR instead of PSI text-matching

The original version of this project walked the PSI tree with `KtTreeVisitorVoid` and matched on `.text`. That approach cannot tell `"SELECT * FROM t WHERE id = '" + trustedConstant` from `... + userInput`, cannot resolve which `executeQuery` overload is being called, and has no notion of constant-folding. FIR checkers run after name/type resolution, so:

- `toResolvedCallableSymbol()` identifies the exact declaration being called, by FQN, not just "a method named `executeQuery`".
- `FirLiteralExpression`/`ConstantValueKind` give you the compiler's own constant evaluation, not a regex over source text.

The tradeoff: FIR's extension API is explicitly internal/unstable (no compatibility guarantee across Kotlin versions), which is why the project pins to one exact Kotlin version (2.1.10).

## Analysis approach and known limitations

The checkers implement **conservative constant analysis**: an argument is considered safe only if it is a compile-time string literal. This is a sound conservative approximation: zero false negatives for non-constant sink arguments, at the cost of more false positives than a full taint tracker.

Known limitations:

- **String templates and method chains are not detected.** `println("token=$token")` and `exec(cmd.toString())` are not flagged: the argument is not a direct symbol reference or literal. This is consistent with the no-taint-tracking scope.
- **No inter-procedural analysis.** If a tainted value is passed through a helper function before reaching a sink, it will not be flagged.
- **FQN allowlists are not exhaustive.** Only standard Java/JDK types and a small set of common libraries (SnakeYAML) are covered; framework-specific sinks (e.g. Spring `JdbcTemplate`) are not.
- **No per-rule configuration.** Severities are fixed in `SastDiagnostics`; there is no `-P` option to override them yet.

## Gradle plugin

`gradle-plugin/` contains a `KotlinCompilerPluginSupportPlugin` implementation (`SastGradlePlugin`). When applied, it automatically wires the `:plugin` compiler plugin jar into every Kotlin compilation in the consuming project. No `-Xplugin` hand-wiring needed. Consumers apply it via:

```kotlin
plugins {
    id("io.github.adinahhh.compile-sast") version "0.1.0"
}
```

`getPluginArtifact()` returns the Maven coordinates of `:plugin`; Gradle resolves this through the consuming project's normal `repositories {}`. The plugin marker artifact (for `plugins {}` block resolution) is auto-generated by the `java-gradle-plugin` + `maven-publish` combination.

## Testing

Tests live in `plugin/src/test/kotlin/com/compile/sast/rules/`, with one test class per checker (e.g. `SqlInjectionCheckerTest`). A shared `TestHelpers.kt` provides the `compile()` helper, which uses [`dev.zacsweers.kctfork:core`](https://github.com/ZacSweers/kotlin-compile-testing) (a K2-compatible fork of `kotlin-compile-testing`) to compile small Kotlin snippets in-process with `SastCompilerPluginRegistrar` attached, then asserts on `result.exitCode` and `result.messages`. This exercises the full registration chain end-to-end.

## References

- [Kotlin Compiler Plugin API](https://kotlinlang.org/docs/compiler-plugins.html)
- [Kotlin FIR checker extension points (source)](https://github.com/JetBrains/kotlin/tree/master/compiler/fir/checkers)
- [kotlin-compile-testing K2 fork (kctfork)](https://github.com/ZacSweers/kotlin-compile-testing)
- [CWE/SANS Top 25](https://cwe.mitre.org/top25/archive/2023/2023_top25_list.html)

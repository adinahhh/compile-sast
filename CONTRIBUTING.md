# Contributing a new rule

Rules are FIR checkers, not PSI visitors - they run inside the compiler's own resolved-tree checking phase. Pick the right base class for what you're inspecting:

| You want to check... | Base class | Package |
|---|---|---|
| a function call (e.g. `Runtime.exec(...)`) | `FirFunctionCallChecker` | `org.jetbrains.kotlin.fir.analysis.checkers.expression` |
| a property/val declaration | `FirPropertyChecker` | `org.jetbrains.kotlin.fir.analysis.checkers.declaration` |
| a class declaration | `FirRegularClassChecker` | `org.jetbrains.kotlin.fir.analysis.checkers.declaration` |

(These are all typealiases over `FirExpressionChecker<E>` / `FirDeclarationChecker<D>` - see `FirExpressionCheckerAliases.kt`/`FirDeclarationCheckerAliases.kt` in the Kotlin compiler sources if you need a node type not listed here.)

## 1. Add a diagnostic factory

In `SastDiagnostics.kt`, add a factory and its renderer:

```kotlin
val MY_NEW_CHECK by warning1<KtElement, String>()   // or error1 for ERROR severity
```

```kotlin
map.put(SastDiagnostics.MY_NEW_CHECK, "SAST00N <your message>: {0}", CommonRenderers.STRING)
```

## 2. Write the checker

```kotlin
package com.compile.sast.rules

object MyNewChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        // resolve by FQN (containingClassLookupTag()?.classId), not by method name text matching
        // inspect arguments via the resolved FIR node (FirLiteralExpression + ConstantValueKind for constants)
        reporter.reportOn(expression.source, SastDiagnostics.MY_NEW_CHECK, "details", context)
    }
}
```

Resolve by FQN/symbol, not source text. `expression.calleeReference.toResolvedCallableSymbol()` and `symbol.containingClassLookupTag()?.classId` give you the actual resolved declaration; matching `expression.text` against a method name string will also match unrelated methods that happen to share a name.

## 3. Register it

Add it to the relevant `Set<...>` in `SastAdditionalCheckersExtension.kt` (`functionCallCheckers`, `propertyCheckers`, etc.).

## 4. Test it

Add cases to `plugin/src/test/kotlin/com/compile/sast/SastRulesTest.kt` using the existing `compile(source: String)` helper - one snippet that should trigger the diagnostic, one similar-looking snippet that shouldn't. `kotlin-compile-testing` actually runs your checker; don't write a test that just asserts on a string literal.

## A note on the FIR API

This API is internal and not guaranteed stable across Kotlin versions - class/package locations have moved between minor versions (e.g. `FirConstExpression` became `FirLiteralExpression` in 2.1). If something doesn't resolve, the fastest way to find the current location is grepping the compiler's own sources jar:

```bash
curl -sL -o /tmp/kotlin-sources.jar \
  "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-compiler-embeddable/2.1.10/kotlin-compiler-embeddable-2.1.10-sources.jar"
unzip -l /tmp/kotlin-sources.jar | grep -i <ClassYouAreLookingFor>
```

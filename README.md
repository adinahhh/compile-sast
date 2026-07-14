# compile-sast: a security-aware Kotlin compiler plugin

A Kotlin **K2/FIR compiler plugin** that turns security checks into real compiler diagnostics. Findings appear as `kotlinc`/Gradle `warning:`/`error:` lines during local compilation — not in a separate scanner pass, and not only after a PR reaches CI.

Each rule uses the compiler's own resolved FIR tree (real call/type resolution, constant evaluation) rather than matching on raw source text.

## Rules

| ID | Title | Severity | Description | CWE |
|----|-------|----------|-------------|-----|
| SAST001 | Hardcoded secret | ERROR | A property whose name looks secret-shaped (`apiKey`, `password`, `token`, ...) is initialized with a constant string literal matching a known secret pattern (`sk-...`, `ghp_...`, `AKIA...`). | [CWE-798](https://cwe.mitre.org/data/definitions/798.html) |
| SAST002 | Weak cryptographic algorithm | ERROR | `MessageDigest.getInstance()` or `Cipher.getInstance()`, resolved by FQN, called with a constant string naming a weak algorithm or mode (MD5, SHA1, DES, RC4, ECB). | [CWE-327](https://cwe.mitre.org/data/definitions/327.html) |
| SAST003 | SQL injection | WARNING | A call to `executeQuery`/`execute`/`executeUpdate`/etc. on a resolved `java.sql` or JPA type whose query argument is not a compile-time constant string. | [CWE-89](https://cwe.mitre.org/data/definitions/89.html) |
| SAST004 | Command injection | WARNING | `Runtime.exec()` or `ProcessBuilder()`, resolved by FQN, called with command arguments that are not all compile-time constant strings. | [CWE-78](https://cwe.mitre.org/data/definitions/78.html) |
| SAST005 | Weak TLS/SSL protocol | ERROR | `SSLContext.getInstance()`, resolved by FQN, called with a deprecated protocol string (`SSLv3`, `TLSv1`, `TLSv1.1`). Uses exact matching — `TLSv1.2`/`TLSv1.3` are not flagged. | [CWE-326](https://cwe.mitre.org/data/definitions/326.html) |
| SAST006 | Insecure deserialization | ERROR | `ObjectInputStream.readObject()`/`readUnshared()` or `Yaml.load()`/`loadAll()` resolved by FQN. The call itself is the signal — there is no safe way to call these on untrusted input. | [CWE-502](https://cwe.mitre.org/data/definitions/502.html) |
| SAST007 | SSRF | WARNING | `java.net.URL` constructor called with any non-constant argument — a dynamic URL may redirect requests to an attacker-controlled host. | [CWE-918](https://cwe.mitre.org/data/definitions/918.html) |
| SAST008 | Path traversal | WARNING | `File()` constructor or `Paths.get()`, resolved by FQN, called with path arguments that are not all compile-time constants. | [CWE-22](https://cwe.mitre.org/data/definitions/22.html) |
| SAST009 | Logging sensitive data | WARNING | A logging call (`println`, SLF4J/Log4j/`java.util.logging` methods) whose arguments directly reference a secret-named variable or property. | [CWE-532](https://cwe.mitre.org/data/definitions/532.html) |
| SAST010 | XXE injection | WARNING | `DocumentBuilderFactory`, `SAXParserFactory`, `TransformerFactory`, or `XMLInputFactory` `.newInstance()` calls, resolved by FQN. These factories allow external entity expansion by default; the diagnostic message includes the specific hardening step required. | [CWE-611](https://cwe.mitre.org/data/definitions/611.html) |

Rules use **conservative constant analysis**: if an argument is not a compile-time string literal, it is flagged. This is a sound conservative approximation — zero false negatives for non-constant sink arguments, at the cost of more false positives than a full taint tracker. String templates (`"$password"`) and method chains (`apiKey.toString()`) are known limitations.

## How it works

```
Kotlin source
  -> Kotlin K2 compiler (FIR tree, already resolved)
  -> SastCompilerPluginRegistrar       registers a FirExtensionRegistrar
  -> SastAdditionalCheckersExtension   registers FirFunctionCallCheckers + FirPropertyCheckers
  -> per-rule checker (e.g. SqlInjectionChecker)
  -> DiagnosticReporter.reportOn(...)  real compiler diagnostic
  -> kotlinc / Gradle build output     ERROR severity fails the build
```

Checks run inside the compiler's own FIR checker pipeline — the same mechanism used for unresolved-reference errors. There is no separate scanner or side-channel.

## Project layout

```
compile-sast/
├── plugin/                             # the compiler plugin (published as com.compile.sast:plugin)
│   └── src/main/kotlin/com/compile/sast/
│       ├── SastCompilerPluginRegistrar.kt
│       ├── SastFirExtensionRegistrar.kt
│       ├── SastAdditionalCheckersExtension.kt
│       ├── SastDiagnostics.kt
│       └── rules/
│           ├── HardcodedSecretChecker.kt        # FirPropertyChecker
│           ├── WeakCryptoChecker.kt             # FirFunctionCallChecker
│           ├── SqlInjectionChecker.kt           # FirFunctionCallChecker
│           ├── CommandInjectionChecker.kt       # FirFunctionCallChecker
│           ├── WeakTlsChecker.kt               # FirFunctionCallChecker
│           ├── InsecureDeserializationChecker.kt # FirFunctionCallChecker
│           ├── SsrfChecker.kt                  # FirFunctionCallChecker
│           ├── PathTraversalChecker.kt          # FirFunctionCallChecker
│           └── LoggingSensitiveDataChecker.kt   # FirFunctionCallChecker
│   └── src/test/kotlin/com/compile/sast/
│       ├── TestHelpers.kt
│       └── rules/                      # one test class per checker
├── gradle-plugin/                      # Gradle plugin (com.compile.sast:gradle-plugin)
│   └── src/main/kotlin/com/compile/sast/gradle/
│       └── SastGradlePlugin.kt         # KotlinCompilerPluginSupportPlugin
├── sample-app/                         # demonstration of consumer-facing plugin apply
│   └── src/main/kotlin/com/compile/sast/sample/VulnerableSamples.kt
├── build.gradle.kts
└── settings.gradle.kts
```

## Building and running

```bash
# run the test suite (compiles real snippets through the plugin)
./gradlew :plugin:test

# publish both artifacts to local Maven and verify sample-app triggers diagnostics
./gradlew :plugin:publishToMavenLocal :gradle-plugin:publishToMavenLocal
./gradlew :sample-app:compileKotlin
```

`sample-app` applies the plugin via `plugins { id("com.compile.sast") version "0.1.0" }` — no `-Xplugin` hand-wiring. The build fails with SAST001/SAST002 errors from `VulnerableSamples.kt`.

## Requirements

- Kotlin **2.1.10** — the K2/FIR plugin API is internal/unstable and not guaranteed compatible across Kotlin versions; this version is pinned deliberately
- JDK 21

## Roadmap

- Per-rule configuration (severity overrides, enable/disable) via `CommandLineProcessor` for `-P` options
- Publish to Maven Central and Gradle Plugin Portal
- `SECURITY.md` and signed releases

## License

MIT

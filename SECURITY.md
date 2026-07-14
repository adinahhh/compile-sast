# Security Policy

## Supported versions

compile-sast is pre-1.0 (currently `0.1.0`) and not yet published to Maven
Central or the Gradle Plugin Portal. Only the latest commit on `main` is
supported - there are no maintained release branches yet.

## Reporting a vulnerability

Please report suspected vulnerabilities via a
[private GitHub security advisory](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability)
on this repository, rather than a public issue. This is a solo-maintained
project, so there's no formal SLA, but reports will be acknowledged as soon
as possible.

## Scope

compile-sast is a build-time static analysis tool, not a runtime security
boundary. Two categories of report are handled differently:

- **Vulnerabilities in the plugin itself** - e.g. a crafted source file that
  causes the compiler plugin to execute arbitrary code, escape the compiler
  process, or otherwise behave unsafely during compilation. These are
  in scope and should be reported privately as above.
- **Detection gaps in the rules** (false negatives) - e.g. a taint-tracking
  case the conservative constant analysis doesn't catch, or a sink not yet
  covered by an FQN allowlist. These are known, documented limitations (see
  [ARCHITECTURE.md](ARCHITECTURE.md#analysis-approach-and-known-limitations)),
  not vulnerabilities in compile-sast itself - please file them as a regular
  GitHub issue instead.

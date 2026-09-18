# compile-sast: Vulnerabilities as build failures, not CI failures

**BSides Houston** · September 17, 2026 · Whitney Zilton

[Slides (PDF)](compile-sast-bsides-houston-2026.pdf) · [source deck (PPTX)](compile-sast-bsides-houston-zilton.pptx)

## Abstract

A SAST finding that arrives in CI arrives after you have already decided you were
done. The thesis of this talk: if it can be a compile error, it should be a compile
error. The compiler is the earliest, cheapest, and most trusted feedback surface a
developer touches, and Kotlin's K2/FIR checker API finally makes it usable for
security rules the way Roslyn analyzers have been in .NET for a decade.

The talk walks through compile-sast, a Kotlin K2/FIR compiler plugin that reports
CWE-mapped findings as real compiler diagnostics, so a vulnerable pattern fails the
build on the developer's machine rather than in a pipeline. It covers why this
matters more as AI assistants increase code volume without increasing review
capacity, what the plugin does and deliberately does not do, the three-step
architecture, and a live demo of introducing vulnerabilities and watching the build
break and recover.

## Outline

1. **Motivation** — the SAST scan that fails after you thought the ticket was done
2. **Relevance** — AI-suggested code raises volume and lowers skepticism; a compile
   rule does not care who typed it
3. **Thesis** — cost of a fix rises with how late the signal arrives
4. **Scope** — what compile-sast is, and what it is not (no taint tracking, no
   dependency scanning, Kotlin only, not exhaustive)
5. **Why Kotlin** — K2/FIR versus Roslyn analyzers versus Go's out-of-compiler model
6. **Architecture** — Kotlin source → K2/FIR checker → compiler diagnostic
7. **Coverage** — ten CWE-mapped rule classes, with walkthroughs of CWE-798,
   CWE-89, and CWE-327
8. **Demo** — clean build, introduce vulnerabilities, build fails, fix, build passes
9. **Roadmap** — more rule classes, taint tracking, other languages, SCA plugin

## Discussion questions from the talk

- Which rules would you want to be blocking vs. warning?
- What is the boundary between "compiler's job" and "linter's job"?
- How would you sell build-time enforcement to a skeptical team?
- What are your thoughts on shift-left philosophy?

## Notes

The deck was written against the state of the project as of the talk. For the
current rule set, severities, and plugin coordinates, see the
[repository README](../../README.md).

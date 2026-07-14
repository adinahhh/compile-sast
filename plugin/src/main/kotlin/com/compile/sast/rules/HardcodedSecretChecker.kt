package com.compile.sast.rules

import com.compile.sast.SastDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * SAST001: flags property declarations whose name looks secret-shaped
 * (apiKey, password, token, ...) and whose initializer is a constant string
 * literal matching a known secret pattern (sk-..., ghp_..., AKIA...).
 *
 * Unlike the other rules, this is a FirPropertyChecker, not a
 * FirFunctionCallChecker - it inspects declarations, not call sites.
 */
object HardcodedSecretChecker : FirPropertyChecker(MppCheckerKind.Common) {

    private val SECRET_NAME_PATTERN = Regex(
        "(?i)(api[_-]?key|secret|password|passwd|token|private[_-]?key|access[_-]?key)"
    )

    private val SECRET_VALUE_PATTERN = Regex(
        "^(sk-[a-zA-Z0-9_-]{10,}|ghp_[a-zA-Z0-9]{20,}|AKIA[0-9A-Z]{12,}|[a-zA-Z0-9_\\-/+]{16,})$"
    )

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val name = declaration.name.asString()
        if (!SECRET_NAME_PATTERN.containsMatchIn(name)) return

        val initializer = declaration.initializer as? FirLiteralExpression ?: return
        if (initializer.kind != ConstantValueKind.String) return

        val value = initializer.value as? String ?: return
        if (value.isBlank() || !SECRET_VALUE_PATTERN.matches(value)) return

        reporter.reportOn(
            declaration.source,
            SastDiagnostics.HARDCODED_SECRET,
            "property '$name' is initialized with a hardcoded secret-shaped literal",
            context,
        )
    }
}

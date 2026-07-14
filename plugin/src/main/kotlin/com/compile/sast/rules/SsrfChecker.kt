package com.compile.sast.rules

import com.compile.sast.SastDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * SAST007: flags java.net.URL constructor calls where any argument is not a
 * compile-time constant - a dynamic URL may redirect requests to an
 * attacker-controlled host (SSRF).
 */
object SsrfChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private val URL_CLASS = ClassId.topLevel(FqName("java.net.URL"))

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirConstructorSymbol ?: return
        if (symbol.containingClassLookupTag()?.classId != URL_CLASS) return

        val arguments = expression.argumentList.arguments
        if (arguments.isEmpty()) return
        if (arguments.all { it is FirLiteralExpression }) return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.SSRF,
            "URL constructed from non-constant argument(s) - validate the host against an allowlist",
            context,
        )
    }
}

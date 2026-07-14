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
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * SAST005: flags SSLContext.getInstance calls whose constant-evaluated
 * protocol argument names a deprecated/weak TLS or SSL version.
 *
 * Uses exact matching (not contains) because "TLSv1" is a substring of
 * the safe "TLSv1.2"/"TLSv1.3" - the JCA protocol strings are a known,
 * fixed set so exact equality is both correct and unambiguous.
 */
object WeakTlsChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private val SSL_CONTEXT = ClassId.topLevel(FqName("javax.net.ssl.SSLContext"))

    // TLSv1.2 and TLSv1.3 are intentionally absent - they're not weak.
    private val WEAK_PROTOCOLS = setOf("SSL", "SSLv2", "SSLv3", "TLSv1", "TLSv1.1")

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        if (symbol.name.asString() != "getInstance") return

        val containingClassId = symbol.containingClassLookupTag()?.classId ?: return
        if (containingClassId != SSL_CONTEXT) return

        val argument = expression.argumentList.arguments.firstOrNull() ?: return
        val constArg = argument as? FirLiteralExpression ?: return
        if (constArg.kind != ConstantValueKind.String) return
        val value = constArg.value as? String ?: return

        val matched = WEAK_PROTOCOLS.firstOrNull { it.equals(value, ignoreCase = true) } ?: return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.WEAK_TLS,
            "'$value' is a deprecated protocol - use TLSv1.2 or TLSv1.3",
            context,
        )
    }
}

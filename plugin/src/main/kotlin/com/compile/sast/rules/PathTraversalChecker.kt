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
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * SAST008: flags File constructor and Paths.get() calls where any path
 * argument is not a compile-time constant - a dynamic path may allow an
 * attacker to escape the intended directory (path traversal).
 */
object PathTraversalChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private val FILE_CLASS = ClassId.topLevel(FqName("java.io.File"))
    private val PATHS_CLASS = ClassId.topLevel(FqName("java.nio.file.Paths"))

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!expression.isFileSystemSink()) return
        if (expression.hasOnlyConstantArguments()) return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.PATH_TRAVERSAL,
            "file path constructed from non-constant argument(s) - validate and canonicalize before use",
            context,
        )
    }

    private fun FirFunctionCall.isFileSystemSink(): Boolean {
        val symbol = calleeReference.toResolvedCallableSymbol() ?: return false
        return when {
            symbol is FirConstructorSymbol ->
                symbol.containingClassLookupTag()?.classId == FILE_CLASS

            symbol is FirNamedFunctionSymbol &&
                symbol.name.asString() == "get" &&
                symbol.containingClassLookupTag()?.classId == PATHS_CLASS -> true

            else -> false
        }
    }

    /** Paths.get()'s vararg parameter must be flattened before checking constant-ness. */
    private fun FirFunctionCall.hasOnlyConstantArguments(): Boolean {
        val arguments = argumentList.arguments.flatMap {
            if (it is FirVarargArgumentsExpression) it.arguments else listOf(it)
        }
        if (arguments.isEmpty()) return true
        return arguments.all { it is FirLiteralExpression }
    }
}

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
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * SAST004: flags Runtime.exec()/ProcessBuilder() calls whose command
 * arguments are not all compile-time constant strings - a dynamic command
 * may allow an attacker to inject arbitrary shell commands.
 */
object CommandInjectionChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private val RUNTIME = ClassId.topLevel(FqName("java.lang.Runtime"))
    private val PROCESS_BUILDER = ClassId.topLevel(FqName("java.lang.ProcessBuilder"))

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeLabel = commandSpawningCalleeLabel(expression) ?: return
        if (expression.hasOnlyConstantStringArguments()) return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.COMMAND_INJECTION,
            "command argument(s) to '$calleeLabel' are not all compile-time constant strings",
            context,
        )
    }

    private fun commandSpawningCalleeLabel(expression: FirFunctionCall): String? {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return null
        return when {
            symbol is FirNamedFunctionSymbol &&
                symbol.name.asString() == "exec" &&
                symbol.containingClassLookupTag()?.classId == RUNTIME -> "Runtime.exec"

            symbol is FirConstructorSymbol &&
                symbol.containingClassLookupTag()?.classId == PROCESS_BUILDER -> "ProcessBuilder"

            else -> null
        }
    }

    /**
     * Vararg call sites wrap their elements in a single FirVarargArgumentsExpression -
     * flatten before checking each individual argument's constant-ness.
     */
    private fun FirFunctionCall.hasOnlyConstantStringArguments(): Boolean {
        val arguments = argumentList.arguments.flatMap {
            if (it is FirVarargArgumentsExpression) it.arguments else listOf(it)
        }
        if (arguments.isEmpty()) return true
        return arguments.all { it is FirLiteralExpression && it.kind == ConstantValueKind.String }
    }
}

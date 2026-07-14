package com.compile.sast.rules

import com.compile.sast.SastDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * SAST009: flags logging calls whose arguments directly reference a
 * secret-named variable or property (same name pattern as HardcodedSecretChecker).
 *
 * Known limitation: string templates ("$password") and method chains
 * (apiKey.toString()) are not detected - this checker only sees direct
 * variable/property references passed as arguments. This is consistent
 * with the project's documented "no taint tracking" scope.
 */
object LoggingSensitiveDataChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private val SECRET_NAME_PATTERN =
        Regex("(?i)(api[_-]?key|secret|password|passwd|token|private[_-]?key|access[_-]?key)")

    private val LOGGING_SINKS = mapOf(
        ClassId.topLevel(FqName("java.io.PrintStream")) to
            setOf("println", "print"),
        ClassId.topLevel(FqName("org.slf4j.Logger")) to
            setOf("info", "warn", "error", "debug", "trace"),
        ClassId.topLevel(FqName("org.apache.logging.log4j.Logger")) to
            setOf("info", "warn", "error", "debug", "fatal", "trace"),
        ClassId.topLevel(FqName("java.util.logging.Logger")) to
            setOf("info", "warning", "severe", "fine", "log"),
    )

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        if (!symbol.isLoggingCall()) return

        val secretArg = expression.argumentList.arguments
            .firstOrNull { it.referencesSecretNamedSymbol() } ?: return
        val argName = (secretArg as FirQualifiedAccessExpression)
            .calleeReference.toResolvedCallableSymbol()?.name?.asString() ?: return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.LOGGING_SENSITIVE_DATA,
            "potential logging of secret-named value '$argName'",
            context,
        )
    }

    private fun FirNamedFunctionSymbol.isLoggingCall(): Boolean {
        val methodName = name.asString()
        // Top-level Kotlin println/print have no containing class at the language level.
        if (containingClassLookupTag() == null && methodName in setOf("println", "print")) return true
        val classId = containingClassLookupTag()?.classId ?: return false
        return LOGGING_SINKS[classId]?.contains(methodName) == true
    }

    private fun FirExpression.referencesSecretNamedSymbol(): Boolean {
        if (this !is FirQualifiedAccessExpression) return false
        val name = calleeReference.toResolvedCallableSymbol()?.name?.asString() ?: return false
        return SECRET_NAME_PATTERN.containsMatchIn(name)
    }
}

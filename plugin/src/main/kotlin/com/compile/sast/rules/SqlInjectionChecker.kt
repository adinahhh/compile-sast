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
 * SAST003: flags calls to executeQuery/execute/executeUpdate/etc. on a
 * resolved java.sql or JPA type whose query argument is not a compile-time
 * constant string - a dynamic query string may allow SQL injection.
 */
object SqlInjectionChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private val SQL_EXECUTOR_CLASSES = setOf(
        ClassId.topLevel(FqName("java.sql.Statement")),
        ClassId.topLevel(FqName("java.sql.PreparedStatement")),
        ClassId.topLevel(FqName("java.sql.Connection")),
        ClassId.topLevel(FqName("javax.persistence.EntityManager")),
    )

    private val SQL_METHOD_NAMES = setOf(
        "executeQuery", "execute", "executeUpdate", "createQuery", "createNativeQuery", "prepareStatement",
    )

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        if (symbol.name.asString() !in SQL_METHOD_NAMES) return

        val containingClassId = symbol.containingClassLookupTag()?.classId ?: return
        if (containingClassId !in SQL_EXECUTOR_CLASSES) return

        val argument = expression.argumentList.arguments.firstOrNull() ?: return
        val isConstantString = argument is FirLiteralExpression && argument.kind == ConstantValueKind.String
        if (isConstantString) return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.SQL_INJECTION,
            "query argument to '${symbol.name}' is not a compile-time constant string",
            context,
        )
    }
}

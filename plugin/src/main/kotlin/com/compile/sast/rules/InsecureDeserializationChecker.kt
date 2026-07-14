package com.compile.sast.rules

import com.compile.sast.SastDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * SAST006: flags calls to known-dangerous deserialization APIs regardless
 * of arguments - there is no safe way to call these methods on untrusted input.
 */
object InsecureDeserializationChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private data class DangerousSink(val classId: ClassId, val methodNames: Set<String>, val advice: String)

    private val SINKS = listOf(
        DangerousSink(
            classId = ClassId.topLevel(FqName("java.io.ObjectInputStream")),
            methodNames = setOf("readObject", "readUnshared"),
            advice = "ObjectInputStream deserialization executes arbitrary code in gadget-chain attacks",
        ),
        DangerousSink(
            classId = ClassId.topLevel(FqName("org.yaml.snakeyaml.Yaml")),
            methodNames = setOf("load", "loadAll"),
            advice = "Yaml.load/loadAll with default constructor allows arbitrary type instantiation - use SafeConstructor",
        ),
    )

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        val methodName = symbol.name.asString()
        val containingClassId = symbol.containingClassLookupTag()?.classId ?: return

        val sink = SINKS.firstOrNull {
            it.classId == containingClassId && methodName in it.methodNames
        } ?: return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.INSECURE_DESERIALIZATION,
            sink.advice,
            context,
        )
    }
}

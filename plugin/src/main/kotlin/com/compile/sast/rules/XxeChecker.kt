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
 * SAST010: flags XML factory newInstance() calls unconditionally - these
 * factories allow external entity expansion by default, so there's no
 * argument to inspect. WARNING rather than ERROR because correctly-hardened
 * code also calls newInstance(), so false positives are expected.
 */
object XxeChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private val VULNERABLE_FACTORIES = mapOf(
        ClassId.topLevel(FqName("javax.xml.parsers.DocumentBuilderFactory")) to
            "disable external entities via setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)",
        ClassId.topLevel(FqName("javax.xml.parsers.SAXParserFactory")) to
            "disable external entities via setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)",
        ClassId.topLevel(FqName("javax.xml.transform.TransformerFactory")) to
            "disable external entities via setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, \"\")",
        ClassId.topLevel(FqName("javax.xml.stream.XMLInputFactory")) to
            "disable external entities via setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)",
    )

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        if (symbol.name.asString() != "newInstance") return

        val containingClassId = symbol.containingClassLookupTag()?.classId ?: return
        val advice = VULNERABLE_FACTORIES[containingClassId] ?: return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.XXE,
            "${containingClassId.shortClassName} is XXE-unsafe by default - $advice",
            context,
        )
    }
}

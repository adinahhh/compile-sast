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
 * SAST002: flags MessageDigest.getInstance/Cipher.getInstance calls (resolved
 * by FQN) whose constant-evaluated algorithm/transformation argument names a
 * known-weak algorithm or mode.
 */
object WeakCryptoChecker : FirFunctionCallChecker(MppCheckerKind.Common) {

    private val MESSAGE_DIGEST = ClassId.topLevel(FqName("java.security.MessageDigest"))
    private val CIPHER = ClassId.topLevel(FqName("javax.crypto.Cipher"))

    private val WEAK_TOKENS = listOf("MD5", "MD2", "SHA1", "DES", "RC4", "ECB")

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
        if (symbol.name.asString() != "getInstance") return

        val containingClassId = symbol.containingClassLookupTag()?.classId ?: return
        if (containingClassId != MESSAGE_DIGEST && containingClassId != CIPHER) return

        val argument = expression.argumentList.arguments.firstOrNull() ?: return
        val constArg = argument as? FirLiteralExpression ?: return
        if (constArg.kind != ConstantValueKind.String) return
        val value = constArg.value as? String ?: return

        val matchedToken = WEAK_TOKENS.firstOrNull { value.contains(it, ignoreCase = true) } ?: return

        reporter.reportOn(
            expression.source,
            SastDiagnostics.WEAK_CRYPTO,
            "'$value' uses weak algorithm/mode '$matchedToken'",
            context,
        )
    }
}

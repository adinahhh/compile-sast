package com.compile.sast

import com.compile.sast.rules.CommandInjectionChecker
import com.compile.sast.rules.HardcodedSecretChecker
import com.compile.sast.rules.InsecureDeserializationChecker
import com.compile.sast.rules.LoggingSensitiveDataChecker
import com.compile.sast.rules.PathTraversalChecker
import com.compile.sast.rules.XxeChecker
import com.compile.sast.rules.SqlInjectionChecker
import com.compile.sast.rules.SsrfChecker
import com.compile.sast.rules.WeakCryptoChecker
import com.compile.sast.rules.WeakTlsChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class SastAdditionalCheckersExtension(
    session: FirSession,
    private val config: SastPluginConfig,
) : FirAdditionalCheckersExtension(session) {

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(
            SqlInjectionChecker,
            WeakCryptoChecker,
            CommandInjectionChecker,
            WeakTlsChecker,
            InsecureDeserializationChecker,
            SsrfChecker,
            PathTraversalChecker,
            LoggingSensitiveDataChecker,
            XxeChecker,
        )
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(
            HardcodedSecretChecker,
        )
    }
}

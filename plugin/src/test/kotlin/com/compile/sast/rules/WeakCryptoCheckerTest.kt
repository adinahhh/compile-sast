package com.compile.sast.rules

import com.compile.sast.compile
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class WeakCryptoCheckerTest {

    @Test
    fun `flags MessageDigest getInstance with MD5`() {
        val result = compile(
            """
            import java.security.MessageDigest

            fun hash(input: ByteArray): ByteArray {
                val digest = MessageDigest.getInstance("MD5")
                return digest.digest(input)
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("SAST002"))
    }

    @Test
    fun `does not flag MessageDigest getInstance with SHA-256`() {
        val result = compile(
            """
            import java.security.MessageDigest

            fun hash(input: ByteArray): ByteArray {
                val digest = MessageDigest.getInstance("SHA-256")
                return digest.digest(input)
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(!result.messages.contains("SAST002"))
    }
}

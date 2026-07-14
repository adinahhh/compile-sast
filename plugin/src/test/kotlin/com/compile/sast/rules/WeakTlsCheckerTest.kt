package com.compile.sast.rules

import com.compile.sast.compile
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class WeakTlsCheckerTest {

    @Test
    fun `flags SSLContext getInstance with SSLv3`() {
        val result = compile(
            """
            import javax.net.ssl.SSLContext

            fun connect() {
                val ctx = SSLContext.getInstance("SSLv3")
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("SAST005"))
    }

    @Test
    fun `flags SSLContext getInstance with TLSv1`() {
        val result = compile(
            """
            import javax.net.ssl.SSLContext

            fun connect() {
                val ctx = SSLContext.getInstance("TLSv1")
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("SAST005"))
    }

    @Test
    fun `does not flag SSLContext getInstance with TLSv1_2`() {
        val result = compile(
            """
            import javax.net.ssl.SSLContext

            fun connect() {
                val ctx = SSLContext.getInstance("TLSv1.2")
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(!result.messages.contains("SAST005"))
    }

    @Test
    fun `does not flag SSLContext getInstance with TLSv1_3`() {
        val result = compile(
            """
            import javax.net.ssl.SSLContext

            fun connect() {
                val ctx = SSLContext.getInstance("TLSv1.3")
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(!result.messages.contains("SAST005"))
    }
}

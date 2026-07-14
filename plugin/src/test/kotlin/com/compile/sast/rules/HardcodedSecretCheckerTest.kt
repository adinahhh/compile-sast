package com.compile.sast.rules

import com.compile.sast.compile
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class HardcodedSecretCheckerTest {

    @Test
    fun `flags hardcoded secret literal`() {
        val result = compile(
            """
            class Example {
                val apiKey = "sk-prod-1234567890abcdef"
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("SAST001"))
    }

    @Test
    fun `does not flag a non-secret-shaped value assigned to a secret-named property`() {
        val result = compile(
            """
            class Example {
                val apiKey = "unset"
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(!result.messages.contains("SAST001"))
    }
}

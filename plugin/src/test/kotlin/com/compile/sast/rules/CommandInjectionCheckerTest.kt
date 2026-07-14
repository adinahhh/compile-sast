package com.compile.sast.rules

import com.compile.sast.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class CommandInjectionCheckerTest {

    @Test
    fun `flags Runtime exec with non-constant command`() {
        val result = compile(
            """
            fun run(userInput: String) {
                Runtime.getRuntime().exec("sh -c " + userInput)
            }
            """
        )
        assertTrue(result.messages.contains("SAST004"))
    }

    @Test
    fun `does not flag Runtime exec with constant command`() {
        val result = compile(
            """
            fun run() {
                Runtime.getRuntime().exec("ls -la")
            }
            """
        )
        assertTrue(!result.messages.contains("SAST004"))
    }

    @Test
    fun `flags ProcessBuilder with non-constant argument`() {
        val result = compile(
            """
            fun run(userInput: String) {
                ProcessBuilder("sh", "-c", userInput)
            }
            """
        )
        assertTrue(result.messages.contains("SAST004"))
    }

    @Test
    fun `does not flag ProcessBuilder with all-constant arguments`() {
        val result = compile(
            """
            fun run() {
                ProcessBuilder("ls", "-la")
            }
            """
        )
        assertTrue(!result.messages.contains("SAST004"))
    }
}

package com.compile.sast.rules

import com.compile.sast.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class PathTraversalCheckerTest {

    @Test
    fun `flags File constructor with non-constant path`() {
        val result = compile(
            """
            import java.io.File

            fun read(userInput: String): String {
                return File(userInput).readText()
            }
            """
        )
        assertTrue(result.messages.contains("SAST008"))
    }

    @Test
    fun `does not flag File constructor with constant path`() {
        val result = compile(
            """
            import java.io.File

            fun read(): String {
                return File("/etc/config.json").readText()
            }
            """
        )
        assertTrue(!result.messages.contains("SAST008"))
    }

    @Test
    fun `flags Paths get with non-constant path`() {
        val result = compile(
            """
            import java.nio.file.Paths

            fun read(userInput: String) {
                val path = Paths.get(userInput)
            }
            """
        )
        assertTrue(result.messages.contains("SAST008"))
    }

    @Test
    fun `does not flag Paths get with constant path`() {
        val result = compile(
            """
            import java.nio.file.Paths

            fun read() {
                val path = Paths.get("/etc/config.json")
            }
            """
        )
        assertTrue(!result.messages.contains("SAST008"))
    }
}

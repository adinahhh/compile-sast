package com.compile.sast.rules

import com.compile.sast.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class SsrfCheckerTest {

    @Test
    fun `flags URL constructed from a non-constant string`() {
        val result = compile(
            """
            import java.net.URL

            fun fetch(endpoint: String) {
                val url = URL(endpoint)
            }
            """
        )
        assertTrue(result.messages.contains("SAST007"))
    }

    @Test
    fun `flags URL constructed from string concatenation`() {
        val result = compile(
            """
            import java.net.URL

            fun fetch(host: String) {
                val url = URL("https://" + host + "/api")
            }
            """
        )
        assertTrue(result.messages.contains("SAST007"))
    }

    @Test
    fun `does not flag URL constructed from a constant string`() {
        val result = compile(
            """
            import java.net.URL

            fun fetch() {
                val url = URL("https://api.example.com/data")
            }
            """
        )
        assertTrue(!result.messages.contains("SAST007"))
    }
}

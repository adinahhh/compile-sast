package com.compile.sast.rules

import com.compile.sast.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class LoggingSensitiveDataCheckerTest {

    @Test
    fun `flags println with secret-named parameter`() {
        val result = compile(
            """
            fun log(password: String) {
                println(password)
            }
            """
        )
        assertTrue(result.messages.contains("SAST009"))
    }

    @Test
    fun `flags println with secret-named local variable`() {
        val result = compile(
            """
            fun log(raw: String) {
                val apiKey = raw
                println(apiKey)
            }
            """
        )
        assertTrue(result.messages.contains("SAST009"))
    }

    @Test
    fun `does not flag println with non-secret-named variable`() {
        val result = compile(
            """
            fun log(username: String) {
                println(username)
            }
            """
        )
        assertTrue(!result.messages.contains("SAST009"))
    }

    @Test
    fun `does not flag println with a constant string`() {
        val result = compile(
            """
            fun log() {
                println("Application started")
            }
            """
        )
        assertTrue(!result.messages.contains("SAST009"))
    }

    @Test
    fun `flags java util logging Logger info with secret-named variable`() {
        val result = compile(
            """
            import java.util.logging.Logger

            fun log(token: String) {
                val logger = Logger.getLogger("App")
                logger.info(token)
            }
            """
        )
        assertTrue(result.messages.contains("SAST009"))
    }
}

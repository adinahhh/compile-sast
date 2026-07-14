package com.compile.sast.rules

import com.compile.sast.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class SqlInjectionCheckerTest {

    @Test
    fun `flags non-constant query string passed to executeQuery`() {
        val result = compile(
            """
            import java.sql.Connection

            fun run(connection: Connection, userId: String) {
                val statement = connection.createStatement()
                statement.executeQuery("SELECT * FROM users WHERE id = '" + userId + "'")
            }
            """
        )
        assertTrue(result.messages.contains("SAST003"))
    }

    @Test
    fun `does not flag a constant query string`() {
        val result = compile(
            """
            import java.sql.Connection

            fun run(connection: Connection) {
                val statement = connection.createStatement()
                statement.executeQuery("SELECT * FROM users")
            }
            """
        )
        assertTrue(!result.messages.contains("SAST003"))
    }
}

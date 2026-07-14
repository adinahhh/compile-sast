package com.compile.sast.rules

import com.compile.sast.compile
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class InsecureDeserializationCheckerTest {

    @Test
    fun `flags ObjectInputStream readObject`() {
        val result = compile(
            """
            import java.io.ObjectInputStream
            import java.io.InputStream

            fun deserialize(stream: InputStream): Any {
                return ObjectInputStream(stream).readObject()
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("SAST006"))
    }

    @Test
    fun `flags ObjectInputStream readUnshared`() {
        val result = compile(
            """
            import java.io.ObjectInputStream
            import java.io.InputStream

            fun deserialize(stream: InputStream): Any {
                return ObjectInputStream(stream).readUnshared()
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("SAST006"))
    }

    @Test
    fun `does not flag non-deserialization read on a stream`() {
        val result = compile(
            """
            import java.io.FileInputStream

            fun read(path: String): Int {
                return FileInputStream(path).read()
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(!result.messages.contains("SAST006"))
    }

    @Test
    fun `flags SnakeYAML Yaml load`() {
        val result = compile(
            """
            import org.yaml.snakeyaml.Yaml

            fun parse(input: String): Any {
                return Yaml().load(input)
            }
            """
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("SAST006"))
    }
}

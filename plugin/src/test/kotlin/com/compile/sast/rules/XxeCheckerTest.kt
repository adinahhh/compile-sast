package com.compile.sast.rules

import com.compile.sast.compile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class XxeCheckerTest {

    @Test
    fun `flags DocumentBuilderFactory newInstance`() {
        val result = compile(
            """
            import javax.xml.parsers.DocumentBuilderFactory

            fun parse(xml: String) {
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
            }
            """
        )
        assertTrue(result.messages.contains("SAST010"))
    }

    @Test
    fun `flags SAXParserFactory newInstance`() {
        val result = compile(
            """
            import javax.xml.parsers.SAXParserFactory

            fun parse() {
                val factory = SAXParserFactory.newInstance()
            }
            """
        )
        assertTrue(result.messages.contains("SAST010"))
    }

    @Test
    fun `flags TransformerFactory newInstance`() {
        val result = compile(
            """
            import javax.xml.transform.TransformerFactory

            fun transform() {
                val factory = TransformerFactory.newInstance()
            }
            """
        )
        assertTrue(result.messages.contains("SAST010"))
    }

    @Test
    fun `does not flag unrelated code`() {
        val result = compile(
            """
            fun greet(name: String): String {
                return "Hello, ${'$'}name"
            }
            """
        )
        assertTrue(!result.messages.contains("SAST010"))
    }
}

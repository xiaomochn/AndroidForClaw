package com.xiaomo.androidforclaw.agent.tools

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ExecFacadeTool unit tests.
 *
 * Full execution requires a device with EmbeddedTermuxRuntime installed;
 * here we only verify tool definition and parameter schema.
 */
class ExecFacadeToolTest {

    private val tool = ExecFacadeTool(context = mockk(relaxed = true))

    @Test
    fun `tool name is exec`() {
        assertEquals("exec", tool.name)
    }

    @Test
    fun `tool definition has command parameter`() {
        val def = tool.getToolDefinition()
        assertTrue(def.function.parameters.properties.containsKey("command"))
        assertTrue(def.function.parameters.required.contains("command"))
    }

    @Test
    fun `tool definition has timeout parameter`() {
        val def = tool.getToolDefinition()
        assertTrue(def.function.parameters.properties.containsKey("timeout"))
    }

    @Test
    fun `missing command returns error`() {
        val result = kotlinx.coroutines.runBlocking {
            tool.execute(emptyMap())
        }
        assertTrue(!result.success)
        assertTrue(result.content.contains("Missing"))
    }
}

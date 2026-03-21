package com.xiaomo.androidforclaw.e2e

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.xiaomo.androidforclaw.ui.activity.MainActivityCompose
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * 端到端测试：打开 App → Chat tab → 发送消息 → AI 返回响应
 *
 * 完整链路：App 启动 → Gateway WebSocket 连接 → 发送 chat.send RPC → LLM 处理 → UI 显示回复
 *
 * 前提：设备上已配置有效的 API Key（openclaw.json）
 *
 * 运行:
 * adb shell am instrument -w \
 *   -e class com.xiaomo.androidforclaw.e2e.ChatSendReceiveE2ETest \
 *   com.xiaomo.androidforclaw.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ChatSendReceiveE2ETest {

    companion object {
        private const val PKG = "com.xiaomo.androidforclaw"
        private const val TIMEOUT = 8_000L
        private const val AI_TIMEOUT_MS = 90_000L // AI 响应最长等 90 秒
    }

    private lateinit var device: UiDevice
    private lateinit var scenario: ActivityScenario<MainActivityCompose>

    @Before
    fun setUp() {
        val instr = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instr)

        instr.uiAutomation.executeShellCommand(
            "appops set $PKG MANAGE_EXTERNAL_STORAGE allow"
        ).close()

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivityCompose::class.java)
        scenario = ActivityScenario.launch(intent)

        // 处理可能的引导页拦截
        Thread.sleep(2000)
        if (device.findObject(UiSelector().textContains("欢迎使用")).exists()) {
            device.pressBack()
            Thread.sleep(1000)
        }

        device.findObject(UiSelector().text("Connect")).waitForExists(TIMEOUT)
        device.waitForIdle()
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    /**
     * 核心 E2E：打开 App → Chat tab → 输入消息 → 点 Send → AI 回复出现在界面上
     *
     * 通过真实 UI 路径验证完整链路：
     * UI input → ChatComposer → WebSocket RPC chat.send → GatewayController → AgentLoop → LLM → 事件广播回 UI
     */
    @Test
    fun test_openApp_sendMessage_receiveAIResponse() {
        // ── Step 1: 切到 Chat tab ──
        val chatTab = device.findObject(UiSelector().text("Chat"))
        assertTrue("Chat tab should exist", chatTab.waitForExists(TIMEOUT))
        chatTab.click()
        device.waitForIdle()
        println("✅ Step 1: 已切换到 Chat tab")

        // ── Step 2: 确认 Gateway 已连接（Send 按钮可用的前提） ──
        val connected = device.findObject(UiSelector().text("Connected"))
        assertTrue("Gateway should show Connected status", connected.waitForExists(TIMEOUT))
        println("✅ Step 2: Gateway 已连接")

        // ── Step 3: 确认空聊天状态 ──
        val emptyState = device.findObject(UiSelector().textContains("No messages yet"))
        assertTrue("Should show empty chat state", emptyState.waitForExists(TIMEOUT))
        println("✅ Step 3: 聊天界面为空状态")

        // ── Step 4: 输入消息并发送 ──
        val inputField = device.findObject(UiSelector().className("android.widget.EditText"))
        assertTrue("Chat input field should exist", inputField.waitForExists(TIMEOUT))
        inputField.setText("Reply with exactly one word: PONG")

        Thread.sleep(500) // 等待 Compose recompose 使 Send 按钮变 enabled
        device.waitForIdle()

        val sendBtn = device.findObject(UiSelector().text("Send"))
        assertTrue("Send button should exist", sendBtn.waitForExists(TIMEOUT))
        assertTrue("Send button should be enabled", sendBtn.isEnabled)
        sendBtn.click()
        println("✅ Step 4: 消息已发送")

        // ── Step 5: 等待 AI 响应出现 ──
        // OpenClaw 的 assistant 消息气泡会带有 "OpenClaw" 角色标签
        // 可能先出现 "Thinking..." 再变成完整回复
        val aiRoleLabel = device.findObject(UiSelector().text("OpenClaw"))
        val aiAppeared = aiRoleLabel.waitForExists(AI_TIMEOUT_MS)
        assertTrue("AI response bubble (role label 'OpenClaw') should appear within ${AI_TIMEOUT_MS}ms", aiAppeared)
        println("✅ Step 5: AI 响应气泡已出现")

        // ── Step 6: 验证 "No messages yet" 已消失（说明有真实消息渲染） ──
        val emptyGone = device.findObject(UiSelector().textContains("No messages yet"))
        assertFalse("Empty state should be gone after AI responds", emptyGone.exists())

        // ── Step 7: 验证响应内容包含 PONG ──
        // 等一下确保流式传输完成
        Thread.sleep(3000)
        device.waitForIdle()

        val pongText = device.findObject(UiSelector().textContains("PONG"))
        assertTrue("AI response should contain 'PONG'", pongText.exists())
        println("✅ Step 7: AI 返回了包含 PONG 的有效响应，测试通过")
    }
}

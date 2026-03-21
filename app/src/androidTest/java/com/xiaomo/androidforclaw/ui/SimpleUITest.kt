package com.xiaomo.androidforclaw.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.xiaomo.androidforclaw.core.MyApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * 简化的 UI 测试
 * 使用 UI Automator 进行基本的应用测试
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SimpleUITest {

    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext<MyApplication>()

        device.pressHome()
        device.waitForIdle()
    }

    @Test
    fun testAppLaunches() {
        val intent = context.packageManager.getLaunchIntentForPackage(
            context.packageName
        )?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        context.startActivity(intent)

        val launched = device.wait(
            Until.hasObject(By.pkg(context.packageName)),
            5000
        )

        assertTrue("应用应该启动", launched)
    }

    private fun launchApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(
            context.packageName
        )?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(context.packageName)), 5000)
        device.waitForIdle()
    }
}

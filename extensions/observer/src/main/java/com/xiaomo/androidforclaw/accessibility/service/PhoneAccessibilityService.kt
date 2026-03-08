package com.xiaomo.androidforclaw.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.IBinder
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

class PhoneAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PhoneAccessibilityService"
    }

    @JvmField
    var currentPackageName = ""
    var activityClassName = ""
    private var globalIndex = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate - Accessibility service created")
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBinderService.serviceInstance = this
        Log.d(TAG, "onServiceConnected - Accessibility service ready")

        // 启动前台服务，显示通知
        try {
            val intent = Intent(this, com.xiaomo.androidforclaw.accessibility.ForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.i(TAG, "✅ 前台服务已启动")
        } catch (e: Exception) {
            Log.e(TAG, "启动前台服务失败", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.packageName != packageName) {
                currentPackageName = event.packageName?.toString() ?: ""
                activityClassName = event.className?.toString() ?: ""
                Log.d(TAG, "Current app: $currentPackageName, Activity: $activityClassName")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind - Accessibility service disconnected")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityBinderService.serviceInstance = null

        // 停止前台服务
        try {
            val intent = Intent(this, com.xiaomo.androidforclaw.accessibility.ForegroundService::class.java)
            stopService(intent)
            Log.i(TAG, "✅ 前台服务已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止前台服务失败", e)
        }

        Log.d(TAG, "onDestroy - Accessibility service destroyed")
    }

    fun dumpView(): List<ViewNode> {
        val windows = this.windows
        if (windows.isEmpty()) {
            Log.w(TAG, "No windows available, trying rootInActiveWindow as fallback")
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                globalIndex = 0
                val nodesList = mutableListOf<ViewNode>()
                traverseNode(rootNode, nodesList)
                return nodesList
            }
            return emptyList()
        }

        globalIndex = 0
        val nodesList = mutableListOf<ViewNode>()
        val sortedWindows = windows.sortedByDescending { it.layer }
        Log.d(TAG, "Found ${sortedWindows.size} windows")

        for ((index, window) in sortedWindows.withIndex()) {
            val rootNode = window.root
            if (rootNode == null) {
                Log.w(TAG, "Window $index has no root node")
                continue
            }

            val windowTitle = window.title?.toString() ?: ""
            if (windowTitle.contains("FloatingRootContainer") ||
                windowTitle.contains("layout_floating")) {
                Log.d(TAG, "Skip system window: $windowTitle")
                continue
            }

            Log.d(TAG, "Processing window $index: ${window.title}, type: ${window.type}, layer: ${window.layer}")
            try {
                traverseNode(rootNode, nodesList)
            } catch (e: Exception) {
                Log.e(TAG, "Error traversing window $index", e)
            }
        }

        Log.d(TAG, "Total nodes collected: ${nodesList.size}")
        return nodesList
    }

    private fun traverseNode(node: AccessibilityNodeInfo, nodesList: MutableList<ViewNode>) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val isValidRect = rect.left >= 0 && rect.top >= 0 && rect.right > rect.left && rect.bottom > rect.top

        if (!isValidRect) {
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { childNode ->
                    traverseNode(childNode, nodesList)
                }
            }
            return
        }

        val centerX = rect.centerX()
        val centerY = rect.centerY()

        if (centerX < 0 || centerY < 0) {
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { childNode ->
                    traverseNode(childNode, nodesList)
                }
            }
            return
        }

        val nodeInfo = ViewNode(
            index = globalIndex++,
            text = node.text?.toString(),
            resourceId = node.viewIdResourceName,
            className = node.className?.toString(),
            packageName = node.packageName?.toString(),
            contentDesc = node.contentDescription?.toString(),
            clickable = node.isClickable,
            enabled = node.isEnabled,
            focusable = node.isFocusable,
            focused = node.isFocused,
            scrollable = node.isScrollable,
            point = Point(centerX, centerY),
            left = rect.left,
            right = rect.right,
            top = rect.top,
            bottom = rect.bottom
        )
        nodesList.add(nodeInfo)

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { childNode ->
                traverseNode(childNode, nodesList)
            }
        }
    }

    suspend fun performClickAt(
        x: Float,
        y: Float,
        isLongClick: Boolean = false
    ): Boolean {
        Log.d(TAG, "performClickAt: x=$x, y=$y, isLongClick=$isLongClick")

        val duration = if (isLongClick) 600L else 200L
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        val result = CompletableDeferred<Boolean>()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "Click completed: ($x, $y)")
                result.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.d(TAG, "Click cancelled: ($x, $y)")
                result.complete(false)
            }
        }, null)

        return withTimeoutOrNull(500) {
            result.await()
        } ?: false
    }

    fun pressHomeButton() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun pressBackButton() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        rootInActiveWindow?.let {
            val swipe = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        Path().apply {
                            moveTo(startX, startY)
                            lineTo(endX, endY)
                        }, 0L, 200L
                    )
                ).build()

            dispatchGesture(swipe, null, null)
        }
    }

    // Java-compatible synchronous wrapper
    fun performClickAtSync(x: Int, y: Int, isLongClick: Boolean): Boolean {
        return kotlinx.coroutines.runBlocking {
            performClickAt(x.toFloat(), y.toFloat(), isLongClick)
        }
    }
}

data class ViewNode(
    val index: Int,
    var text: String?,
    val resourceId: String?,
    val className: String?,
    val packageName: String?,
    val contentDesc: String?,
    val clickable: Boolean,
    val enabled: Boolean,
    val focusable: Boolean,
    val focused: Boolean,
    val scrollable: Boolean,
    val point: Point,
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
)

data class Point(val x: Int, val y: Int)

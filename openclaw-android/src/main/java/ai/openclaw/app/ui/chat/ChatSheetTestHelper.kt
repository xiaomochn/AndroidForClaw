package ai.openclaw.app.ui.chat

import androidx.compose.runtime.Composable
import ai.openclaw.app.chat.ChatSessionEntry

/**
 * Test helper to expose internal ChatThreadSelector for UI testing.
 */
object ChatSheetTestHelper {
    @Composable
    fun ChatThreadSelectorTest(
        sessionKey: String,
        sessions: List<ChatSessionEntry>,
        mainSessionKey: String,
        onSelectSession: (String) -> Unit = {},
        onDeleteSession: ((String) -> Unit)? = null,
    ) {
        ChatThreadSelector(
            sessionKey = sessionKey,
            sessions = sessions,
            mainSessionKey = mainSessionKey,
            onSelectSession = onSelectSession,
            onDeleteSession = onDeleteSession,
        )
    }
}

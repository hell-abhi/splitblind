package com.akeshari.splitblind

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.sync.SyncEngine
import com.akeshari.splitblind.ui.navigation.NavGraph
import com.akeshari.splitblind.ui.theme.SplitBlindTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var identity: Identity

    @Inject
    lateinit var syncEngine: SyncEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.akeshari.splitblind.ui.theme.ThemeManager.init(this)

        val deepLink = parseDeepLink(intent)

        if (deepLink is DeepLinkResult.ShortCode) {
            // Resolve the short code asynchronously, then render
            syncEngine.resolveShortCode(deepLink.code) { result ->
                runOnUiThread {
                    val resolved = if (result != null) {
                        DeepLinkData(
                            groupId = result.first,
                            groupKey = deepLink.groupKey.ifEmpty { result.third ?: "" },
                            groupName = result.second
                        )
                    } else {
                        Log.e("MainActivity", "Failed to resolve short code: ${deepLink.code}")
                        null
                    }
                    renderContent(resolved)
                }
            }
        } else {
            renderContent((deepLink as? DeepLinkResult.Full)?.data)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val deepLink = parseDeepLink(intent) ?: return

        if (deepLink is DeepLinkResult.ShortCode) {
            syncEngine.resolveShortCode(deepLink.code) { result ->
                runOnUiThread {
                    val resolved = if (result != null) {
                        DeepLinkData(
                            groupId = result.first,
                            groupKey = deepLink.groupKey.ifEmpty { result.third ?: "" },
                            groupName = result.second
                        )
                    } else null
                    renderContent(resolved)
                }
            }
        } else {
            renderContent((deepLink as? DeepLinkResult.Full)?.data)
        }
    }

    private fun renderContent(deepLink: DeepLinkData?) {
        setContent {
            SplitBlindTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    isOnboarded = identity.isOnboarded,
                    joinGroupId = deepLink?.groupId,
                    joinGroupKey = deepLink?.groupKey,
                    joinGroupName = deepLink?.groupName
                )
            }
        }
    }

    private data class DeepLinkData(
        val groupId: String,
        val groupKey: String,
        val groupName: String
    )

    private sealed class DeepLinkResult {
        data class Full(val data: DeepLinkData) : DeepLinkResult()
        data class ShortCode(val code: String, val groupKey: String) : DeepLinkResult()
    }

    private fun parseDeepLink(intent: Intent?): DeepLinkResult? {
        val uri = intent?.data ?: return null
        return parseInviteUri(uri)
    }

    private fun parseInviteUri(uri: Uri): DeepLinkResult? {
        val fragment = uri.fragment ?: return null

        // Format 1 (short): ?c={shortcode}#{base64key}
        val shortCode = uri.getQueryParameter("c")
        if (shortCode != null) {
            val groupKey = fragment
            return DeepLinkResult.ShortCode(code = shortCode, groupKey = groupKey)
        }

        // Format 2 (legacy): ?g={groupId}#{base64key}|{encodedName}
        val groupId = uri.getQueryParameter("g") ?: return null
        val parts = fragment.split("|", limit = 2)
        if (parts.isEmpty()) return null

        val groupKey = parts[0]
        val groupName = if (parts.size > 1) {
            java.net.URLDecoder.decode(parts[1], "UTF-8")
        } else {
            "Shared Group"
        }

        return DeepLinkResult.Full(
            DeepLinkData(
                groupId = groupId,
                groupKey = groupKey,
                groupName = groupName
            )
        )
    }
}

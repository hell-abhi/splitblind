package com.akeshari.splitblind

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.ui.navigation.NavGraph
import com.akeshari.splitblind.ui.theme.SplitBlindTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var identity: Identity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLink = parseDeepLink(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep links when activity is already running
        val deepLink = parseDeepLink(intent) ?: return
        // Re-create the content with the new deep link
        setContent {
            SplitBlindTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    isOnboarded = identity.isOnboarded,
                    joinGroupId = deepLink.groupId,
                    joinGroupKey = deepLink.groupKey,
                    joinGroupName = deepLink.groupName
                )
            }
        }
    }

    private data class DeepLinkData(
        val groupId: String,
        val groupKey: String,
        val groupName: String
    )

    private fun parseDeepLink(intent: Intent?): DeepLinkData? {
        val uri = intent?.data ?: return null
        return parseInviteUri(uri)
    }

    private fun parseInviteUri(uri: Uri): DeepLinkData? {
        // Format: https://hell-abhi.github.io/splitblind/?g={groupId}#{base64key}|{encodedName}
        val groupId = uri.getQueryParameter("g") ?: return null
        val fragment = uri.fragment ?: return null

        val parts = fragment.split("|", limit = 2)
        if (parts.isEmpty()) return null

        val groupKey = parts[0]
        val groupName = if (parts.size > 1) {
            java.net.URLDecoder.decode(parts[1], "UTF-8")
        } else {
            "Shared Group"
        }

        return DeepLinkData(
            groupId = groupId,
            groupKey = groupKey,
            groupName = groupName
        )
    }
}

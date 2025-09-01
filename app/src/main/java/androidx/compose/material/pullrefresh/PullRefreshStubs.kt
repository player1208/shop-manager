package androidx.compose.material.pullrefresh

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box

/**
 * Minimal stub implementations to allow compiling without the official pull-refresh library.
 * These provide the same API surface used in the app. Gesture behavior is intentionally no-op.
 */

class PullRefreshState()

@Composable
fun rememberPullRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit
): PullRefreshState = remember(refreshing) { PullRefreshState() }

fun Modifier.pullRefresh(state: PullRefreshState): Modifier = this

@Composable
fun PullRefreshIndicator(
    refreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier
) {
    if (refreshing) {
        Box(modifier = modifier) {
            CircularProgressIndicator()
        }
    }
}



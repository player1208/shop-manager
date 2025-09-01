package com.example.storemanagerassitent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 全局成功提示消息管理器
 */
object GlobalSuccessMessage {
    // 用于触发成功提示的状态
    private var _shouldShowSuccess by mutableStateOf(false)
    private var _successMessage by mutableStateOf("")
    
    val shouldShowSuccess: Boolean
        get() = _shouldShowSuccess
    
    val successMessage: String
        get() = _successMessage
    
    /**
     * 显示成功提示
     * @param message 提示文本（不包含图标，图标会自动添加）
     */
    fun showSuccess(message: String) {
        _successMessage = message
        _shouldShowSuccess = true
    }
    
    /**
     * 清除成功提示状态
     */
    fun clearSuccess() {
        _shouldShowSuccess = false
        _successMessage = ""
    }
}

/**
 * 自定义成功提示 Snackbar
 */
@Composable
fun SuccessSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    Snackbar(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        containerColor = Color.Black.copy(alpha = 0.9f),
        contentColor = Color.White,
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // 成功图标
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "成功",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                
                // 提示文本
                Text(
                    text = snackbarData.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    )
}

/**
 * 全局成功提示 SnackbarHost
 */
@Composable
fun GlobalSuccessSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // 改为优雅的中间提示：黑底白字圆角卡片，显示 1 秒
    if (GlobalSuccessMessage.shouldShowSuccess) {
        androidx.compose.runtime.LaunchedEffect(GlobalSuccessMessage.successMessage) {
            delay(1000)
            GlobalSuccessMessage.clearSuccess()
        }
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = GlobalSuccessMessage.successMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
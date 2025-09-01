package com.example.storemanagerassitent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 消息类型枚举
 */
enum class MessageType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * 消息数据类
 */
data class GlobalMessage(
    val message: String,
    val type: MessageType = MessageType.SUCCESS,
    val duration: Long = 3000L // 持续时间（毫秒）
)

/**
 * 增强的全局消息管理器
 * 支持多种消息类型：成功、错误、警告、信息
 */
object GlobalMessageManager {
    private var _currentMessage by mutableStateOf<GlobalMessage?>(null)
    private var _shouldShow by mutableStateOf(false)
    
    val currentMessage: GlobalMessage?
        get() = _currentMessage
    
    val shouldShow: Boolean
        get() = _shouldShow
    
    /**
     * 显示成功消息
     */
    fun showSuccess(message: String, duration: Long = 3000L) {
        showMessage(GlobalMessage(message, MessageType.SUCCESS, duration))
    }
    
    /**
     * 显示错误消息
     */
    fun showError(message: String, duration: Long = 4000L) {
        showMessage(GlobalMessage(message, MessageType.ERROR, duration))
    }
    
    /**
     * 显示警告消息
     */
    fun showWarning(message: String, duration: Long = 3500L) {
        showMessage(GlobalMessage(message, MessageType.WARNING, duration))
    }
    
    /**
     * 显示信息消息
     */
    fun showInfo(message: String, duration: Long = 3000L) {
        showMessage(GlobalMessage(message, MessageType.INFO, duration))
    }
    
    /**
     * 显示消息
     */
    private fun showMessage(message: GlobalMessage) {
        _currentMessage = message
        _shouldShow = true
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _shouldShow = false
        _currentMessage = null
    }
}

/**
 * 获取消息类型对应的颜色和图标
 */
@Composable
private fun getMessageStyle(type: MessageType): Pair<Color, ImageVector> {
    return when (type) {
        MessageType.SUCCESS -> Color(0xFF4CAF50) to Icons.Filled.Check
        MessageType.ERROR -> Color(0xFFF44336) to Icons.Filled.Close
        MessageType.WARNING -> Color(0xFFFF9800) to Icons.Filled.Warning
        MessageType.INFO -> Color(0xFF2196F3) to Icons.Filled.Info
    }
}

/**
 * 增强的自定义消息 Snackbar
 */
@Composable
fun EnhancedSnackbar(
    snackbarData: SnackbarData,
    messageType: MessageType = MessageType.SUCCESS,
    modifier: Modifier = Modifier
) {
    val (color, icon) = getMessageStyle(messageType)
    
    Snackbar(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        containerColor = color,
        contentColor = Color.White,
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                // 类型图标
                Icon(
                    imageVector = icon,
                    contentDescription = messageType.name,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                // 消息文本
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
 * 全局增强消息 SnackbarHost
 */
@Composable
fun GlobalEnhancedSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var currentMessageType by mutableStateOf(MessageType.SUCCESS)
    
    // 监听全局消息状态
    LaunchedEffect(GlobalMessageManager.shouldShow) {
        if (GlobalMessageManager.shouldShow) {
            val message = GlobalMessageManager.currentMessage
            if (message != null) {
                currentMessageType = message.type
                
                // 显示消息
                snackbarHostState.showSnackbar(
                    message = message.message,
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                
                // 等待指定时间后清除状态
                delay(message.duration)
                GlobalMessageManager.clearMessage()
            }
        }
    }
    
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            EnhancedSnackbar(
                snackbarData = snackbarData,
                messageType = currentMessageType
            )
        }
    )
}

/**
 * 加载状态管理器
 */
object LoadingStateManager {
    private var _isLoading by mutableStateOf(false)
    private var _loadingMessage by mutableStateOf("")
    
    val isLoading: Boolean
        get() = _isLoading
    
    val loadingMessage: String
        get() = _loadingMessage
    
    /**
     * 显示加载状态
     */
    fun showLoading(message: String = "加载中...") {
        _loadingMessage = message
        _isLoading = true
    }
    
    /**
     * 隐藏加载状态
     */
    fun hideLoading() {
        _isLoading = false
        _loadingMessage = ""
    }
}

/**
 * 确认对话框状态管理器
 */
data class ConfirmationDialogState(
    val title: String = "",
    val message: String = "",
    val confirmText: String = "确认",
    val cancelText: String = "取消",
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val isVisible: Boolean = false
)

object ConfirmationDialogManager {
    private var _dialogState by mutableStateOf(ConfirmationDialogState())
    
    val dialogState: ConfirmationDialogState
        get() = _dialogState
    
    /**
     * 显示确认对话框
     */
    fun showDialog(
        title: String,
        message: String,
        confirmText: String = "确认",
        cancelText: String = "取消",
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        _dialogState = ConfirmationDialogState(
            title = title,
            message = message,
            confirmText = confirmText,
            cancelText = cancelText,
            onConfirm = {
                onConfirm()
                hideDialog()
            },
            onCancel = {
                onCancel()
                hideDialog()
            },
            isVisible = true
        )
    }
    
    /**
     * 隐藏对话框
     */
    fun hideDialog() {
        _dialogState = _dialogState.copy(isVisible = false)
    }
}
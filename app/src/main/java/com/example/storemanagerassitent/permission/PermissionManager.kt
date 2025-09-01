package com.example.storemanagerassitent.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 权限管理器 - 简化版
 * 处理OCR功能所需的各种权限检查
 */
object PermissionManager {
    
    private const val TAG = "PermissionManager"
    
    // 存储权限
    private val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
    // 前台服务权限 (Android 14+)
    private val FOREGROUND_SERVICE_PERMISSIONS = if (Build.VERSION.SDK_INT >= 34) { // Android 14+
        arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
        )
    } else {
        arrayOf(Manifest.permission.FOREGROUND_SERVICE)
    }
    
    // Android 13+ 媒体权限
    private val MEDIA_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        emptyArray()
    }
    
    // 相机权限
    private val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    
    // OCR功能所需的所有权限
    private val OCR_PERMISSIONS = STORAGE_PERMISSIONS + MEDIA_PERMISSIONS + CAMERA_PERMISSIONS + FOREGROUND_SERVICE_PERMISSIONS
    
    /**
     * 检查是否拥有所有必需权限（屏幕录制功能）
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasStoragePermissions(context) && hasOverlayPermission(context) && hasForegroundServicePermissions(context)
    }
    
    /**
     * 检查OCR功能所需的所有权限
     */
    fun hasOcrPermissions(context: Context): Boolean {
        return hasCameraPermission(context) && hasMediaPermissions(context) && hasStoragePermissions(context)
    }
    
    /**
     * 检查存储权限
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 检查相机权限
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查媒体权限（Android 13+）
     */
    fun hasMediaPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MEDIA_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            true // Android 13以下不需要这些权限
        }
    }
    
    /**
     * 检查悬浮窗权限
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * 检查前台服务权限
     */
    fun hasForegroundServicePermissions(context: Context): Boolean {
        return FOREGROUND_SERVICE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 获取屏幕录制权限Intent
     */
    fun getScreenCaptureIntent(context: Context): Intent {
        val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mediaProjectionManager.createScreenCaptureIntent()
    }
    
    /**
     * 获取悬浮窗权限设置Intent
     */
    fun getOverlayPermissionIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }
    }
    
    /**
     * 获取OCR权限数组
     */
    fun getOcrPermissions(): Array<String> {
        return OCR_PERMISSIONS
    }
    
    /**
     * 获取缺失的OCR权限
     */
    fun getMissingOcrPermissions(context: Context): List<String> {
        return OCR_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 检查权限状态并返回详细信息（屏幕录制功能）
     */
    fun checkPermissionStatus(context: Context): PermissionStatus {
        val storageGranted = hasStoragePermissions(context)
        val overlayGranted = hasOverlayPermission(context)
        val foregroundServiceGranted = hasForegroundServicePermissions(context)
        
        return PermissionStatus(
            storagePermission = storageGranted,
            overlayPermission = overlayGranted,
            cameraPermission = false, // 屏幕录制功能不需要相机权限
            mediaPermission = false,  // 屏幕录制功能不需要媒体权限
            foregroundServicePermission = foregroundServiceGranted,
            allGranted = storageGranted && overlayGranted && foregroundServiceGranted
        )
    }
    
    /**
     * 检查OCR权限状态并返回详细信息
     */
    fun checkOcrPermissionStatus(context: Context): PermissionStatus {
        val storageGranted = hasStoragePermissions(context)
        val cameraGranted = hasCameraPermission(context)
        val mediaGranted = hasMediaPermissions(context)
        val overlayGranted = hasOverlayPermission(context)
        val foregroundServiceGranted = hasForegroundServicePermissions(context)
        
        return PermissionStatus(
            storagePermission = storageGranted,
            overlayPermission = overlayGranted,
            cameraPermission = cameraGranted,
            mediaPermission = mediaGranted,
            foregroundServicePermission = foregroundServiceGranted,
            allGranted = storageGranted && cameraGranted && mediaGranted && foregroundServiceGranted
        )
    }
    
    /**
     * 权限状态数据类
     */
    data class PermissionStatus(
        val storagePermission: Boolean,
        val overlayPermission: Boolean,
        val cameraPermission: Boolean = false,
        val mediaPermission: Boolean = false,
        val foregroundServicePermission: Boolean = false,
        val allGranted: Boolean
    ) {
        /**
         * 获取权限状态描述
         */
        fun getStatusDescription(): String {
            val missing = mutableListOf<String>()
            if (!storagePermission) missing.add("存储权限")
            if (!cameraPermission) missing.add("相机权限")
            if (!mediaPermission) missing.add("媒体权限")
            if (!overlayPermission) missing.add("悬浮窗权限")
            if (!foregroundServicePermission) missing.add("前台服务权限")
            
            return if (missing.isEmpty()) {
                "所有权限已授权"
            } else {
                "缺少权限：${missing.joinToString("、")}"
            }
        }
    }
}
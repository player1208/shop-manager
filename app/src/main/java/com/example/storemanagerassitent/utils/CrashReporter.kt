package com.example.storemanagerassitent.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 崩溃报告工具
 * 用于记录和分析应用崩溃信息
 */
object CrashReporter {
    
    private const val TAG = "CrashReporter"
    private const val CRASH_LOG_FILE = "crash_logs.txt"
    private const val ERROR_LOG_FILE = "error_logs.txt"
    
    private var context: Context? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * 初始化崩溃报告器
     */
    fun init(context: Context) {
        this.context = context
        setupUncaughtExceptionHandler()
        logAppStartup()
    }
    
    /**
     * 设置未捕获异常处理器
     */
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                logCrash(exception, thread.name)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash", e)
            }
            
            // 调用默认处理器
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    /**
     * 记录应用启动信息
     */
    private fun logAppStartup() {
        try {
            val ctx = context ?: return
            val deviceInfo = getDeviceInfo()
            val appInfo = getAppInfo(ctx)
            
            val startupInfo = """
                ==================== APP STARTUP ====================
                Time: ${dateFormat.format(Date())}
                $deviceInfo
                $appInfo
                ====================================================
                
            """.trimIndent()
            
            writeToFile(ERROR_LOG_FILE, startupInfo)
            Log.i(TAG, "App startup logged")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log app startup", e)
        }
    }
    
    /**
     * 记录崩溃信息
     */
    private fun logCrash(exception: Throwable, threadName: String) {
        try {
            val ctx = context ?: return
            val deviceInfo = getDeviceInfo()
            val appInfo = getAppInfo(ctx)
            
            val crashInfo = """
                ==================== CRASH REPORT ====================
                Time: ${dateFormat.format(Date())}
                Thread: $threadName
                $deviceInfo
                $appInfo
                
                Exception: ${exception.javaClass.simpleName}
                Message: ${exception.message}
                
                Stack Trace:
                ${getStackTrace(exception)}
                ====================================================
                
            """.trimIndent()
            
            writeToFile(CRASH_LOG_FILE, crashInfo)
            Log.e(TAG, "Crash logged: ${exception.message}", exception)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log crash", e)
        }
    }
    
    /**
     * 记录错误信息
     */
    fun logError(tag: String, message: String, exception: Throwable? = null) {
        try {
            val errorInfo = """
                ==================== ERROR LOG ====================
                Time: ${dateFormat.format(Date())}
                Tag: $tag
                Message: $message
                ${exception?.let { "Exception: ${it.javaClass.simpleName}\nMessage: ${it.message}\n${getStackTrace(it)}" } ?: ""}
                ================================================
                
            """.trimIndent()
            
            writeToFile(ERROR_LOG_FILE, errorInfo)
            Log.e(tag, message, exception)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log error", e)
        }
    }
    
    /**
     * 记录权限检查信息
     */
    fun logPermissionCheck(permissionName: String, granted: Boolean, reason: String = "") {
        try {
            val permissionInfo = """
                ==================== PERMISSION CHECK ====================
                Time: ${dateFormat.format(Date())}
                Permission: $permissionName
                Granted: $granted
                Reason: $reason
                ========================================================
                
            """.trimIndent()
            
            writeToFile(ERROR_LOG_FILE, permissionInfo)
            Log.i(TAG, "Permission check: $permissionName = $granted ($reason)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log permission check", e)
        }
    }
    
    /**
     * 记录OCR处理信息
     */
    fun logOcrProcess(stage: String, success: Boolean, message: String = "", exception: Throwable? = null) {
        try {
            val ocrInfo = """
                ==================== OCR PROCESS ====================
                Time: ${dateFormat.format(Date())}
                Stage: $stage
                Success: $success
                Message: $message
                ${exception?.let { "Exception: ${it.javaClass.simpleName}\nMessage: ${it.message}\n${getStackTrace(it)}" } ?: ""}
                ===================================================
                
            """.trimIndent()
            
            writeToFile(ERROR_LOG_FILE, ocrInfo)
            Log.i(TAG, "OCR Process - $stage: $success ($message)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log OCR process", e)
        }
    }
    
    /**
     * 获取设备信息
     */
    private fun getDeviceInfo(): String {
        return """
            Device Info:
            - Model: ${Build.MODEL}
            - Manufacturer: ${Build.MANUFACTURER}
            - Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            - Build: ${Build.DISPLAY}
        """.trimIndent()
    }
    
    /**
     * 获取应用信息
     */
    private fun getAppInfo(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            """
                App Info:
                - Package: ${packageInfo.packageName}
                - Version: ${packageInfo.versionName} (${packageInfo.versionCode})
                - Target SDK: ${packageInfo.applicationInfo?.targetSdkVersion ?: "Unknown"}
                - Min SDK: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) packageInfo.applicationInfo?.minSdkVersion ?: "Unknown" else "Unknown"}
            """.trimIndent()
        } catch (e: Exception) {
            "App Info: Failed to retrieve (${e.message})"
        }
    }
    
    /**
     * 获取堆栈跟踪
     */
    private fun getStackTrace(exception: Throwable): String {
        return try {
            val sw = java.io.StringWriter()
            val pw = PrintWriter(sw)
            exception.printStackTrace(pw)
            sw.toString()
        } catch (e: Exception) {
            "Failed to get stack trace: ${e.message}"
        }
    }
    
    /**
     * 写入文件
     */
    private fun writeToFile(fileName: String, content: String) {
        try {
            val ctx = context ?: return
            val file = File(ctx.filesDir, fileName)
            
            FileWriter(file, true).use { writer ->
                writer.write(content)
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to file: $fileName", e)
        }
    }
    
    /**
     * 读取崩溃日志
     */
    fun getCrashLogs(): String {
        return try {
            val ctx = context ?: return "Context not initialized"
            val file = File(ctx.filesDir, CRASH_LOG_FILE)
            if (file.exists()) {
                file.readText()
            } else {
                "No crash logs found"
            }
        } catch (e: Exception) {
            "Failed to read crash logs: ${e.message}"
        }
    }
    
    /**
     * 读取错误日志
     */
    fun getErrorLogs(): String {
        return try {
            val ctx = context ?: return "Context not initialized"
            val file = File(ctx.filesDir, ERROR_LOG_FILE)
            if (file.exists()) {
                file.readText()
            } else {
                "No error logs found"
            }
        } catch (e: Exception) {
            "Failed to read error logs: ${e.message}"
        }
    }
    
    /**
     * 清除日志文件
     */
    fun clearLogs() {
        try {
            val ctx = context ?: return
            File(ctx.filesDir, CRASH_LOG_FILE).delete()
            File(ctx.filesDir, ERROR_LOG_FILE).delete()
            Log.i(TAG, "Logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
    
    /**
     * 获取日志文件大小信息
     */
    fun getLogFileInfo(): String {
        return try {
            val ctx = context ?: return "Context not initialized"
            val crashFile = File(ctx.filesDir, CRASH_LOG_FILE)
            val errorFile = File(ctx.filesDir, ERROR_LOG_FILE)
            
            """
                Log File Info:
                - Crash logs: ${if (crashFile.exists()) "${crashFile.length()} bytes" else "Not found"}
                - Error logs: ${if (errorFile.exists()) "${errorFile.length()} bytes" else "Not found"}
            """.trimIndent()
        } catch (e: Exception) {
            "Failed to get log file info: ${e.message}"
        }
    }
}
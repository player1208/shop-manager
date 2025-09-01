package com.example.storemanagerassitent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.example.storemanagerassitent.overlay.OverlayPreviewController
import com.example.storemanagerassitent.MainActivity
import com.example.storemanagerassitent.R
import com.example.storemanagerassitent.utils.CrashReporter
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * 屏幕截图服务
 * 提供悬浮窗和屏幕截图功能
 */
class ScreenCaptureService : Service() {
    
    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_capture_channel"
        
        const val ACTION_START_CAPTURE = "START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "STOP_CAPTURE"
        const val ACTION_TAKE_SCREENSHOT = "TAKE_SCREENSHOT"
        const val ACTION_SHOW_FLOATING = "SHOW_FLOATING"
        
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        
        // 用于通知OCR结果的广播
        const val ACTION_SCREENSHOT_TAKEN = "com.example.storemanagerassitent.SCREENSHOT_TAKEN"
        const val EXTRA_SCREENSHOT_PATH = "screenshot_path"
    }
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    private var lastCapturedBitmap: Bitmap? = null
    private var previewToolbarView: View? = null
    private var previewToolbarParams: WindowManager.LayoutParams? = null
    private var overlayController: OverlayPreviewController? = null
    
    override fun onCreate() {
        super.onCreate()
        try {
            Log.i(TAG, "ScreenCaptureService onCreate")
            createNotificationChannel()
            overlayController = OverlayPreviewController(this)
            // 不在 onCreate 中初始化屏幕参数，延迟到真正需要时
            Log.i(TAG, "ScreenCaptureService onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in ScreenCaptureService onCreate", e)
            CrashReporter.logError(TAG, "Error in ScreenCaptureService onCreate", e)
            throw e
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                startCapture(resultCode, resultData)
            }
            ACTION_STOP_CAPTURE -> {
                stopCapture()
            }
            ACTION_TAKE_SCREENSHOT -> {
                takeScreenshot()
            }
            ACTION_SHOW_FLOATING -> {
                showFloatingButton()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "屏幕截图服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于智能批量导入的屏幕截图功能"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 初始化屏幕尺寸参数
     */
    private fun initializeScreenMetrics() {
        try {
            val displayMetrics = DisplayMetrics()
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // Service Context 无法直接访问 Display，使用 WindowManager 的默认显示器
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            display.getRealMetrics(displayMetrics)
            
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
            
            Log.i(TAG, "Screen metrics initialized: ${screenWidth}x${screenHeight}, density: $screenDensity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize screen metrics, using default values", e)
            CrashReporter.logError(TAG, "Failed to initialize screen metrics", e)
            
            // 使用默认值作为回退
            screenWidth = 1080
            screenHeight = 2340
            screenDensity = 480
            
            Log.i(TAG, "Using fallback screen metrics: ${screenWidth}x${screenHeight}, density: $screenDensity")
        }
    }
    
    /**
     * 开始截图服务
     */
    private fun startCapture(resultCode: Int, resultData: Intent?) {
        try {
            Log.i(TAG, "Starting screen capture with resultCode: $resultCode")
            
            if (resultCode != -1) {
                Log.e(TAG, "Invalid result code: $resultCode (expected -1)")
                CrashReporter.logError(TAG, "Invalid media projection result code: $resultCode", null)
                return
            }
            
            if (resultData == null) {
                Log.e(TAG, "Result data is null")
                CrashReporter.logError(TAG, "Media projection result data is null", null)
                return
            }
            
            val notification = createForegroundNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.i(TAG, "Started foreground service")
            
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
            Log.i(TAG, "Created media projection")
            
            // 注册MediaProjection回调 - Android 较新版本的要求
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped")
                    stopCapture()
                }
            }, null)
            Log.i(TAG, "MediaProjection callback registered")
            
            // 在这里初始化屏幕参数，因为现在我们有了媒体投影权限
            initializeScreenMetrics()
            
            setupImageReader()
            setupVirtualDisplay()
            showFloatingButton()
            
            Log.i(TAG, "Screen capture setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen capture", e)
            CrashReporter.logError(TAG, "Error starting screen capture", e)
            stopCapture()
        }
    }
    
    /**
     * 设置图像读取器
     */
    private fun setupImageReader() {
        // 仅创建 ImageReader，不再自动监听新帧，避免服务一启动就立即截图
        // 截图改为在用户点击悬浮按钮时主动抓取最新一帧
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
    }
    
    /**
     * 设置虚拟显示
     */
    private fun setupVirtualDisplay() {
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }
    
        /**
     * 显示悬浮按钮
     */
private fun showFloatingButton() {
        try {
            Log.i(TAG, "开始显示悬浮按钮...")
            
            // 检查悬浮窗权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.e(TAG, "No overlay permission, cannot show floating button")
                CrashReporter.logError(TAG, "No overlay permission for floating button", null)
                // 发送广播通知权限问题
                val intent = Intent("com.example.storemanagerassitent.FLOATING_BUTTON_ERROR")
                intent.putExtra("error", "没有悬浮窗权限")
                sendBroadcast(intent)
                return
            }
            
            Log.i(TAG, "悬浮窗权限检查通过")
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            Log.i(TAG, "正在加载悬浮按钮布局...")
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_capture_button, null)
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                // 修复关键问题：使用正确的flags组合以支持触摸事件
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END  // 改为右上角，更容易看到
                x = 50  // 距离右边缘50px
                y = 150  // 距离顶部150px，避开状态栏
            }
            
                    Log.i(TAG, "Setting up floating button with improved touch handling")
        Log.d(TAG, "Window params: type=${params.type}, flags=${params.flags}, format=${params.format}")
        Log.d(TAG, "Position: x=${params.x}, y=${params.y}, gravity=${params.gravity}")
        
            // 设置改进的拖拽和点击功能
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false
            
            floatingView?.setOnTouchListener { view, event ->
                Log.d(TAG, "Touch event: ${event.action}")
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        Log.d(TAG, "Touch DOWN at (${event.rawX}, ${event.rawY})")
                        // 给用户触觉反馈
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        // 只有移动距离超过阈值才开始拖拽
                        if (Math.abs(deltaX) > 20 || Math.abs(deltaY) > 20) {
                            isDragging = true
                            params.x = initialX + deltaX.toInt()
                            params.y = initialY + deltaY.toInt()
                            
                            try {
                                windowManager?.updateViewLayout(floatingView, params)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update view layout", e)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = Math.abs(event.rawX - initialTouchX)
                        val deltaY = Math.abs(event.rawY - initialTouchY)
                        Log.d(TAG, "Touch UP, isDragging: $isDragging, deltaX: $deltaX, deltaY: $deltaY")
                        
                        if (!isDragging && deltaX < 20 && deltaY < 20) {
                            // 这是一个点击事件，执行截图
                            Log.i(TAG, "Floating button clicked, taking screenshot")
                            // 给用户强烈的触觉反馈表示点击成功
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            
                            // 立即执行截图
                            takeScreenshot()
                        }
                        isDragging = false
                        true
                    }
                    else -> false
                }
            }
            
            // 添加备用的点击监听器，防止触摸监听器失效
            floatingView?.setOnClickListener {
                Log.i(TAG, "Floating button clicked via OnClickListener, taking screenshot")
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                takeScreenshot()
            }
            
            // 添加悬浮窗到WindowManager
            try {
                Log.i(TAG, "准备添加悬浮按钮到WindowManager...")
                windowManager?.addView(floatingView, params)
                Log.i(TAG, "悬浮按钮添加到WindowManager成功")
                
                // 验证悬浮按钮是否真的显示
                floatingView?.let { view ->
                    Log.i(TAG, "悬浮按钮视图状态: visibility=${view.visibility}, width=${view.width}, height=${view.height}")
                    Log.i(TAG, "悬浮按钮父视图: ${view.parent}")
                }
                
                // 发送成功广播
                val successIntent = Intent("com.example.storemanagerassitent.FLOATING_BUTTON_SUCCESS")
                sendBroadcast(successIntent)
                
                Log.i(TAG, "悬浮按钮显示完成！")
            } catch (e: Exception) {
                Log.e(TAG, "添加悬浮按钮到WindowManager失败", e)
                CrashReporter.logError(TAG, "Failed to add floating button to WindowManager", e)
                
                // 发送失败广播
                val errorIntent = Intent("com.example.storemanagerassitent.FLOATING_BUTTON_ERROR")
                errorIntent.putExtra("error", "添加悬浮按钮失败: ${e.message}")
                sendBroadcast(errorIntent)
                return
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "显示悬浮按钮失败", e)
            CrashReporter.logError(TAG, "Failed to show floating button", e)
            
            // 发送失败广播
            val errorIntent = Intent("com.example.storemanagerassitent.FLOATING_BUTTON_ERROR")
            errorIntent.putExtra("error", "显示悬浮按钮失败: ${e.message}")
            sendBroadcast(errorIntent)
        }
    }
    
    /**
     * 执行截图
     */
    private fun takeScreenshot() {
        try {
            Log.i(TAG, "Taking screenshot...")
            
            if (imageReader == null) {
                Log.e(TAG, "ImageReader is null, cannot take screenshot")
                CrashReporter.logError(TAG, "ImageReader is null when taking screenshot", null)
                return
            }

            // 主动请求一帧最新图像
            var image: Image? = null
            var retry = 0
            while (retry < 5 && image == null) {
                image = imageReader?.acquireLatestImage()
                if (image == null) {
                    try { Thread.sleep(50) } catch (_: InterruptedException) {}
                }
                retry++
            }
            
            if (image == null) {
                Log.e(TAG, "Failed to acquire image from ImageReader after retries")
                CrashReporter.logError(TAG, "Failed to acquire image from ImageReader after retries", null)
                return
            }

            Log.i(TAG, "Image acquired successfully, processing...")
            processScreenshot(image)
        } catch (e: Exception) {
            Log.e(TAG, "Error taking screenshot", e)
            CrashReporter.logError(TAG, "Error taking screenshot", e)
        }
    }
    
    /**
     * 处理截图
     */
    private fun processScreenshot(image: Image) {
        try {
            Log.i(TAG, "Processing screenshot...")
            
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            Log.d(TAG, "Image info: width=$screenWidth, height=$screenHeight, pixelStride=$pixelStride, rowStride=$rowStride")
            
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            // 裁剪掉padding
            val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            Log.i(TAG, "Bitmap created and cropped: ${croppedBitmap.width}x${croppedBitmap.height}")
            
            // 保留内存中的位图以用于预览和后续操作
            lastCapturedBitmap = croppedBitmap

            // 固定屏幕截图：直接显示 Overlay + 下滑弹窗
            overlayController?.showPreview(croppedBitmap, slideFromTop = true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process screenshot", e)
            CrashReporter.logError(TAG, "Failed to process screenshot", e)
        } finally {
            image.close()
        }
    }
    
    /**
     * 创建前台通知
     */
    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("智能批量导入")
            .setContentText("点击悬浮按钮进行截图")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    /**
     * 隐藏悬浮按钮
     */
    private fun hideFloatingButton() {
        floatingView?.let { view ->
            try {
                windowManager?.removeView(view)
                floatingView = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove floating view", e)
            }
        }
    }

    // 移除长截图工具栏相关逻辑（改为仅 Overlay 预览）

    // 移除：临时保存/滚动/拼接逻辑（固定屏幕截图不需要）
    
    /**
     * 停止截图服务
     */
    private fun stopCapture() {
        hideFloatingButton()
        overlayController?.hide()
        
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        stopForeground(true)
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
    }
}
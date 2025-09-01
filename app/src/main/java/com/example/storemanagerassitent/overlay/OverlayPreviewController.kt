package com.example.storemanagerassitent.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.storemanagerassitent.R

/**
 * 负责全局Overlay的展示与下滑弹窗动画
 */
class OverlayPreviewController(private val context: Context) {
    private var windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: View? = null

    fun showPreview(bitmap: Bitmap, slideFromTop: Boolean = true) {
        hide()
        val view = LayoutInflater.from(context).inflate(R.layout.overlay_screenshot_preview, null)
        rootView = view
        val image = view.findViewById<ImageView>(R.id.ivOverlayPreview)
        val sheet = view.findViewById<View>(R.id.topSheet)
        val close = view.findViewById<View>(R.id.btnCloseOverlay)

        image.setImageBitmap(bitmap)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // 初始把sheet移出屏幕，然后做滑入动画
        sheet.post {
            sheet.translationY = if (slideFromTop) -sheet.height.toFloat() else sheet.height.toFloat()
            sheet.alpha = 0f
            sheet.animate().translationY(0f).alpha(1f).setDuration(220).start()
        }

        // 触摸sheet可收起
        var downY = 0f
        var dragging = false
        sheet.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = e.rawY
                    dragging = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!dragging) return@setOnTouchListener false
                    val dy = e.rawY - downY
                    v.translationY = dy.coerceAtLeast(0f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    if (v.translationY > v.height / 3f) {
                        v.animate().translationY(v.height.toFloat()).alpha(0f).setDuration(180).withEndAction {
                            // 保留预览，仅收起sheet
                        }.start()
                    } else {
                        v.animate().translationY(0f).alpha(1f).setDuration(180).start()
                    }
                    true
                }
                else -> false
            }
        }

        close.setOnClickListener { hide() }

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {
            hide()
        }
    }

    fun updateBitmap(bitmap: Bitmap) {
        val image = rootView?.findViewById<ImageView>(R.id.ivOverlayPreview) ?: return
        image.setImageBitmap(bitmap)
    }

    fun hide() {
        rootView?.let { v ->
            try { windowManager.removeView(v) } catch (_: Exception) {}
        }
        rootView = null
    }
}




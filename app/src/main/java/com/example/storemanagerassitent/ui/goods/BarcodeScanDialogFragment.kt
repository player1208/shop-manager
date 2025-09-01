package com.example.storemanagerassitent.ui.goods

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.storemanagerassitent.R
import com.example.storemanagerassitent.data.api.BarcodeScannerProvider
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * 单条码扫描弹窗（不调用任何商品查询接口）
 */
class BarcodeScanDialogFragment : DialogFragment() {

    companion object {
        const val RESULT_KEY = "barcode_scan_result"
        const val RESULT_BARCODE = "barcode"
    }

    private lateinit var previewView: PreviewView
    private var viewFinder: View? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private val analyzerExecutor = Executors.newSingleThreadExecutor()

    private var roiPercentWidth: Float = 0.7f
    private var roiPercentHeight: Float = 0.4f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_scan_barcode_only, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewView = view.findViewById(R.id.previewView)
        viewFinder = view.findViewById(R.id.viewFinder)

        bindCamera()
        setupViewFinderOverlay()
    }

    override fun onDestroyView() {
        imageAnalysis?.clearAnalyzer()
        analyzerExecutor.shutdown()
        super.onDestroyView()
    }

    private fun bindCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis?.setAnalyzer(analyzerExecutor) { imageProxy ->
                processImage(BarcodeScannerProvider.scanner, imageProxy)
            }
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, selector, preview, imageAnalysis)
            } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupViewFinderOverlay() {
        val vf = viewFinder ?: return
        previewView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (previewView.width == 0 || previewView.height == 0) return
                previewView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val vfW = (previewView.width * roiPercentWidth).toInt()
                val vfH = (previewView.height * roiPercentHeight).toInt()
                val lp = FrameLayout.LayoutParams(vfW, vfH)
                lp.gravity = Gravity.CENTER
                vf.layoutParams = lp

                val border = GradientDrawable()
                border.shape = GradientDrawable.RECTANGLE
                border.setColor(0x00000000)
                border.cornerRadius = resources.displayMetrics.density * 8f
                border.setStroke((resources.displayMetrics.density * 2f).toInt(), 0x80FFFFFF.toInt())
                vf.background = border
                vf.visibility = View.VISIBLE
            }
        })
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(scanner: com.google.mlkit.vision.barcode.BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close(); return
        }

        // ROI 裁剪与过滤
        val frameW = imageProxy.width
        val frameH = imageProxy.height
        val roiW = (frameW * roiPercentWidth).toInt()
        val roiH = (frameH * roiPercentHeight).toInt()
        val left = (frameW - roiW) / 2
        val top = (frameH - roiH) / 2
        val roi = Rect(left, top, left + roiW, top + roiH)
        try { imageProxy.setCropRect(roi) } catch (_: Throwable) {}

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull { b ->
                    val box = b.boundingBox
                    val v = b.rawValue
                    v != null && v.isNotBlank() && (box == null || Rect.intersects(box, roi))
                }?.rawValue
                if (!value.isNullOrBlank()) {
                    imageAnalysis?.clearAnalyzer()
                    parentFragmentManager.setFragmentResult(
                        RESULT_KEY,
                        Bundle().apply { putString(RESULT_BARCODE, value) }
                    )
                    dismissAllowingStateLoss()
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}




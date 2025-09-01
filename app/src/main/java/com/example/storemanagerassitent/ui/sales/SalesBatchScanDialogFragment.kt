package com.example.storemanagerassitent.ui.sales

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.storemanagerassitent.R
import com.example.storemanagerassitent.data.api.BarcodeApiClient
import com.example.storemanagerassitent.data.db.ServiceLocator
import com.example.storemanagerassitent.data.api.BarcodeCache
import com.example.storemanagerassitent.data.api.BarcodeScannerProvider
import com.google.mlkit.vision.common.InputImage
import android.util.Size
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class SalesBatchScanDialogFragment : DialogFragment() {

    data class ScannedItem(
        val goodsId: String,
        val name: String,
        val spec: String,
        val quantity: Int
    )

    companion object {
        const val RESULT_KEY = "sales_batch_scan_result"
        const val RESULT_ITEMS = "items"
        const val ITEM_ID = "goods_id"
        const val ITEM_NAME = "name"
        const val ITEM_SPEC = "spec"
        const val ITEM_QUANTITY = "quantity"
    }

    private lateinit var previewView: PreviewView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConfirm: Button
    private var overlayMessage: TextView? = null
    private var overlaySuccess: TextView? = null

    private val uiScope = CoroutineScope(Dispatchers.Main + Job())
    private val analyzerExecutor = Executors.newSingleThreadExecutor()

    private val items = mutableListOf<ScannedItem>()
    private lateinit var adapter: SalesScannedProductsAdapter

    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_batch_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewView = view.findViewById(R.id.previewView)
        recyclerView = view.findViewById(R.id.recyclerView)
        overlayMessage = view.findViewById(R.id.overlayMessage)
        overlaySuccess = view.findViewById(R.id.overlaySuccess)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        btnConfirm.text = "添加到订单"

        adapter = SalesScannedProductsAdapter(items) { refreshConfirmEnabled() }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        refreshConfirmEnabled()

        btnConfirm.setOnClickListener {
            val list = ArrayList<Bundle>()
            items.forEach { item ->
                val b = Bundle().apply {
                    putString(ITEM_ID, item.goodsId)
                    putString(ITEM_NAME, item.name)
                    putString(ITEM_SPEC, item.spec)
                    putInt(ITEM_QUANTITY, item.quantity)
                }
                list.add(b)
            }
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply { putParcelableArrayList(RESULT_ITEMS, list) }
            )
            dismissAllowingStateLoss()
        }

        bindCamera()

        // 屏蔽外部点击直接关闭，避免绕过列表确认
        dialog?.setCanceledOnTouchOutside(false)

        // 拦截系统返回键：有已扫描商品时弹确认
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                if (items.isNotEmpty()) {
                    confirmExitIfNeeded()
                    true
                } else false
            } else false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
    }

    private fun refreshConfirmEnabled() {
        btnConfirm.isEnabled = items.isNotEmpty()
    }

    private fun bindCamera() {
        val context = requireContext()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScannerProvider.scanner
                imageAnalysis?.setAnalyzer(analyzerExecutor) { imageProxy ->
                    processImage(scanner, imageProxy)
                }

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
                applyInitialZoomAndCenterFocus()
            } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(scanner: com.google.mlkit.vision.barcode.BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close(); return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                if (!value.isNullOrBlank()) {
                    imageAnalysis?.clearAnalyzer()
                    handleBarcode(value)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun handleBarcode(barcode: String) {
        uiScope.launch {
            val goodsDao = ServiceLocator.database.goodsDao()

            val matched = withContext(Dispatchers.IO) {
                // 1) 本地条码
                val byBarcode = goodsDao.findByBarcode(barcode)
                if (byBarcode != null) return@withContext byBarcode

                // 2) 本地缓存 + 云端 + 本地模糊匹配（销售不允许新增）
                var apiName = ""; var apiSpec = ""
                BarcodeCache.get(barcode)?.let { cached ->
                    apiName = cached.name
                    apiSpec = cached.spec
                }
                try {
                    if (apiName.isBlank() && apiSpec.isBlank()) {
                        val resp = BarcodeApiClient.service.getGoodsDetails(barcode)
                        if (resp.isSuccessful) {
                            val body = resp.body()
                            if (body?.code == 1 && body.data != null) {
                                apiName = body.data.goodsName?.trim().orEmpty()
                                apiSpec = body.data.standard?.trim().orEmpty()
                                if (apiName.isNotBlank() || apiSpec.isNotBlank()) {
                                    BarcodeCache.put(barcode, apiName, apiSpec)
                                }
                            }
                        }
                    }
                } catch (_: Exception) { }

                if (apiName.isBlank() && apiSpec.isBlank()) return@withContext null

                val locals = goodsDao.getAll()
                findBestMatchInternal(apiName, apiSpec, locals)
            }

            if (matched == null) {
                showOverlayMessage("该商品不在库存中，请先进货")
            } else {
                val display = matched.name + if (matched.specifications.isNotBlank()) " " + matched.specifications else ""
                val index = items.indexOfFirst { it.goodsId == matched.id }
                if (index >= 0) {
                    items[index] = items[index].copy(quantity = items[index].quantity + 1)
                    adapter.notifyItemChanged(index)
                } else {
                    items.add(ScannedItem(matched.id, matched.name, matched.specifications, 1))
                    adapter.notifyItemInserted(items.size - 1)
                }
                showSuccessOverlay()
                refreshConfirmEnabled()
            }

            // 防抖 0.7s 恢复
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                imageAnalysis?.setAnalyzer(analyzerExecutor) { imageProxy ->
                    processImage(BarcodeScannerProvider.scanner, imageProxy)
                }
            }, 700)
        }
    }

    private fun applyInitialZoomAndCenterFocus() {
        val cam = camera ?: return
        try { cam.cameraControl.setZoomRatio(1.6f) } catch (_: Throwable) { }
        previewView.post {
            try {
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(previewView.width / 2f, previewView.height / 2f)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                cam.cameraControl.startFocusAndMetering(action)
            } catch (_: Throwable) { }
        }
    }

    private fun findBestMatchInternal(apiName: String, apiSpec: String, localProducts: List<com.example.storemanagerassitent.data.db.GoodsEntity>): com.example.storemanagerassitent.data.db.GoodsEntity? {
        return com.example.storemanagerassitent.utils.FuzzyMatchCache.findBestMatchWithCache(
            apiName, apiSpec, localProducts, 0.6
        )?.goods
    }

    private fun showOverlayMessage(text: String) {
        val tv = overlayMessage ?: return
        tv.text = text
        tv.visibility = View.VISIBLE
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tv.visibility = View.GONE }, 700)
    }

    private fun showSuccessOverlay() {
        val tv = overlaySuccess ?: return
        tv.visibility = View.VISIBLE
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tv.visibility = View.GONE }, 700)
    }

    private fun confirmExitIfNeeded() {
        if (items.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }
        android.app.AlertDialog.Builder(requireContext())
            .setMessage("你确认退出？")
            .setPositiveButton("退出") { _, _ ->
                items.clear()
                dismissAllowingStateLoss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}



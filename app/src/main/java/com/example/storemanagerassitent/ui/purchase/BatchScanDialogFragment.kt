package com.example.storemanagerassitent.ui.purchase

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.widget.Button
import com.example.storemanagerassitent.data.api.BarcodeScannerProvider
import com.google.mlkit.vision.common.InputImage
import android.util.Size
import android.view.Gravity
import android.widget.FrameLayout
import android.graphics.Rect
import android.view.ViewTreeObserver
import android.graphics.drawable.GradientDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.util.Log
import com.example.storemanagerassitent.data.api.BarcodeCache
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 批量扫码弹窗
 */
class BatchScanDialogFragment : DialogFragment() {

    data class ScannedItem(
        val name: String,
        val quantity: Int,
        val categoryId: String? = null,
        val barcode: String? = null
    )

    private data class ScanMatchDecision(
        val displayName: String,
        val matchedCategoryId: String?
    )

    companion object {
        const val RESULT_KEY = "batch_scan_result"
        const val RESULT_ITEMS = "items"
        const val ITEM_NAME = "name"
        const val ITEM_QUANTITY = "quantity"
        const val ITEM_CATEGORY_ID = "categoryId"
        const val ITEM_BARCODE = "barcode"
    }

    private lateinit var previewView: PreviewView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConfirm: Button
    private var viewFinder: View? = null
    private var overlayMessage: android.widget.TextView? = null
    private var overlaySuccess: android.widget.TextView? = null

    private val uiScope = CoroutineScope(Dispatchers.Main + Job())
    private val analyzerExecutor = Executors.newSingleThreadExecutor()

    private val items = mutableListOf<ScannedItem>()
    private lateinit var adapter: ScannedProductsAdapter

    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var roiPercentWidth: Float = 0.7f
    private var roiPercentHeight: Float = 0.4f

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
        viewFinder = view.findViewById(R.id.viewFinder)
        overlayMessage = view.findViewById(R.id.overlayMessage)
        overlaySuccess = view.findViewById(R.id.overlaySuccess)
        btnConfirm = view.findViewById(R.id.btnConfirm)

        // 加载分类列表
        val categories = runBlocking {
            ServiceLocator.categoryRepository.observeGoodsCategories().first()
        }
        // 过滤掉 “全部” 分类，不出现在选择列表
        val selectableCategories = categories.filter { it.id != "all" }
        adapter = ScannedProductsAdapter(items, onDataChanged = {
            refreshConfirmEnabled()
        }, categories = selectableCategories)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        refreshConfirmEnabled()

        btnConfirm.setOnClickListener {
            val list = ArrayList<android.os.Bundle>()
            items.forEach { item ->
                val b = android.os.Bundle().apply {
                    putString(ITEM_NAME, item.name)
                    putInt(ITEM_QUANTITY, item.quantity)
                    putString(ITEM_CATEGORY_ID, item.categoryId ?: "")
                    putString(ITEM_BARCODE, item.barcode ?: "")
                }
                list.add(b)
            }
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                android.os.Bundle().apply { putParcelableArrayList(RESULT_ITEMS, list) }
            )
            dismissAllowingStateLoss()
        }

        // 关闭内部取景框，仅使用卡片白色描边
        bindCamera()

        // 拦截系统返回：若列表非空则先确认
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                if (items.isNotEmpty()) {
                    confirmExitIfNeeded()
                    true
                } else {
                    false
                }
            } else false
        }

        // 点击外部区域返回时：若列表非空则先确认
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
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
            } catch (e: Exception) {
                // ignore: dialog can fail silently
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(
        scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        // 基于帧尺寸计算中心 ROI，并应用到 ImageProxy（用于裁剪和结果过滤）
        val frameW = imageProxy.width
        val frameH = imageProxy.height
        val roiW = (frameW * roiPercentWidth).toInt()
        val roiH = (frameH * roiPercentHeight).toInt()
        val left = (frameW - roiW) / 2
        val top = (frameH - roiH) / 2
        val roi = Rect(left, top, left + roiW, top + roiH)
        try {
            imageProxy.setCropRect(roi)
        } catch (_: Throwable) {
            // 某些设备可能不支持在此处设置 cropRect，忽略错误
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                // 仅接受位于 ROI 内的条码（若返回包含 boundingBox）
                val firstValue = barcodes.firstOrNull { b ->
                    val box = b.boundingBox
                    val value = b.rawValue
                    value != null && value.isNotBlank() && (box == null || Rect.intersects(box, roi))
                }?.rawValue
                if (!firstValue.isNullOrBlank()) {
                    // 暂停分析，避免重复
                    imageAnalysis?.clearAnalyzer()
                    handleBarcode(firstValue)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun setupViewFinderOverlay() {
        // 在预览布局完成后，按百分比设置取景框可视化尺寸
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

    private fun handleBarcode(barcode: String) {
        uiScope.launch {
            val repo = ServiceLocator.goodsRepository
            val goodsDao = ServiceLocator.database.goodsDao()

            // 1) 优先本地：根据条码精确匹配
            val decision: ScanMatchDecision? = withContext(Dispatchers.IO) {
                Log.d("BatchScan", "scan_start barcode=${barcode}")
                val localByBarcode = goodsDao.findByBarcode(barcode)
                if (localByBarcode != null) {
                    Log.d("BatchScan", "local_barcode_hit id=${localByBarcode.id} name=${localByBarcode.name} spec=${localByBarcode.specifications}")
                    return@withContext ScanMatchDecision(
                        displayName = localByBarcode.name + " " + localByBarcode.specifications,
                        matchedCategoryId = localByBarcode.categoryId
                    )
                } else {
                    Log.d("BatchScan", "local_barcode_miss")
                }

                // 2) 未命中则先查本地缓存，再查云端
                var apiName = ""
                var apiSpec = ""
                BarcodeCache.get(barcode)?.let { cached ->
                    apiName = cached.name
                    apiSpec = cached.spec
                    Log.d("BatchScan", "cache_hit name='${apiName}' spec='${apiSpec}'")
                }
                try {
                    if (apiName.isBlank() && apiSpec.isBlank()) {
                        val resp = BarcodeApiClient.service.getGoodsDetails(barcode = barcode)
                        if (resp.isSuccessful) {
                            val body = resp.body()
                            Log.d("BatchScan", "api_response http=${resp.code()} code=${body?.code} msg=${body?.msg}")
                            if (body?.code == 1 && body.data != null) {
                                apiName = body.data.goodsName?.trim().orEmpty()
                                apiSpec = body.data.standard?.trim().orEmpty()
                                Log.d("BatchScan", "api_parsed name='${apiName}' spec='${apiSpec}'")
                                if (apiName.isNotBlank() || apiSpec.isNotBlank()) {
                                    BarcodeCache.put(barcode, apiName, apiSpec)
                                    Log.d("BatchScan", "cache_put name='${apiName}' spec='${apiSpec}'")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("BatchScan", "api_error ${e.message}")
                }

                if (apiName.isBlank() && apiSpec.isBlank()) {
                    Log.d("BatchScan", "api_no_data -> overall_miss")
                    return@withContext null
                }

                // 3) 高级模糊匹配（名称/规格与本地列表）
                val locals = goodsDao.getAll()
                Log.d("BatchScan", "locals_size=${locals.size}")
                val best = findBestMatchInternal(apiName, apiSpec, locals)
                if (best != null) {
                    Log.d("BatchScan", "fuzzy_match_hit id=${best.id} name=${best.name} spec=${best.specifications}")
                    ScanMatchDecision(
                        displayName = best.name + " " + best.specifications,
                        matchedCategoryId = best.categoryId
                    )
                } else {
                    val fallback = apiName + (if (apiSpec.isNotBlank()) " " + apiSpec else "")
                    Log.d("BatchScan", "fuzzy_match_miss -> fallback='${fallback}'")
                    ScanMatchDecision(displayName = fallback, matchedCategoryId = null)
                }
            }

            if (decision == null || decision.displayName.isBlank()) {
                showOverlayMessage("未查到商品码")
                Log.d("BatchScan", "final_result miss -> show '未查到商品码'")
            } else {
                val resultName = decision.displayName
                val index = items.indexOfFirst { it.name == resultName }
                if (index >= 0) {
                    val existing = items[index]
                    val newCategory = existing.categoryId ?: decision.matchedCategoryId
                    items[index] = existing.copy(quantity = existing.quantity + 1, categoryId = newCategory, barcode = existing.barcode ?: barcode)
                    adapter.notifyItemChanged(index)
                    Log.d("BatchScan", "final_result increment name='${resultName}' newQty=${items[index].quantity}")
                } else {
                    items.add(ScannedItem(resultName, 1, categoryId = decision.matchedCategoryId, barcode = barcode))
                    adapter.notifyItemInserted(items.size - 1)
                    Log.d("BatchScan", "final_result add name='${resultName}' qty=1 (uncategorized)")
                }
                // 将条码写入本地商品（若命中现有商品且其条码为空）
                withContext(Dispatchers.IO) {
                    val existing = goodsDao.findByFullDisplayName(resultName)
                    if (existing != null && (existing.barcode == null || existing.barcode.isBlank())) {
                        val updated = existing.copy(barcode = barcode, lastUpdated = System.currentTimeMillis())
                        ServiceLocator.database.goodsDao().update(updated)
                        Log.d("BatchScan", "barcode_saved goodsId=${existing.id} barcode=${barcode}")
                    }
                }
                showSuccessOverlay()
                refreshConfirmEnabled()
            }

            // 防抖：0.7秒后再恢复分析
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                imageAnalysis?.setAnalyzer(analyzerExecutor) { imageProxy ->
                    processImage(BarcodeScannerProvider.scanner, imageProxy)
                }
            }, 700)
        }
    }

    private fun applyInitialZoomAndCenterFocus() {
        val cam = camera ?: return
        try {
            cam.cameraControl.setZoomRatio(1.6f)
        } catch (_: Throwable) { }

        // 等待 previewView 有尺寸后再进行对焦/测光
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

    private class ScannedItemsAdapter : RecyclerView.Adapter<ScannedItemsViewHolder>() {
        private var data: MutableList<ScannedItem> = mutableListOf()

        fun submitList(newData: List<ScannedItem>) {
            data = newData.toMutableList()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScannedItemsViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_scanned_product, parent, false)
            return ScannedItemsViewHolder(view)
        }

        override fun onBindViewHolder(holder: ScannedItemsViewHolder, position: Int) {
            holder.bind(data, position, this)
        }

        override fun getItemCount(): Int = data.size
    }

    private class ScannedItemsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<android.widget.TextView>(R.id.tvName)
        private val tvQuantity = itemView.findViewById<android.widget.TextView>(R.id.tvQuantity)
        private val btnIncrease = itemView.findViewById<android.widget.ImageButton>(R.id.btnIncrease)
        private val btnDecrease = itemView.findViewById<android.widget.ImageButton>(R.id.btnDecrease)
        fun bind(list: MutableList<ScannedItem>, position: Int, adapter: RecyclerView.Adapter<*>) {
            val item = list[position]
            tvName.text = item.name
            tvQuantity.text = item.quantity.toString()
            btnIncrease.setOnClickListener {
                list[position] = item.copy(quantity = item.quantity + 1)
                adapter.notifyItemChanged(position)
            }
            btnDecrease.setOnClickListener {
                val current = list[position]
                if (current.quantity > 1) {
                    list[position] = current.copy(quantity = current.quantity - 1)
                    adapter.notifyItemChanged(position)
                } else {
                    android.app.AlertDialog.Builder(itemView.context)
                        .setMessage("是否要从列表中移除该商品？")
                        .setPositiveButton("移除") { _, _ ->
                            list.removeAt(position)
                            adapter.notifyItemRemoved(position)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
        }
    }

    private fun showOverlayMessage(text: String) {
        val tv = overlayMessage ?: return
        tv.text = text
        tv.visibility = View.VISIBLE
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            tv.visibility = View.GONE
        }, 700)
    }

    private fun showSuccessOverlay() {
        val tv = overlaySuccess ?: return
        tv.visibility = View.VISIBLE
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            tv.visibility = View.GONE
        }, 700)
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

    private fun refreshConfirmEnabled() {
        val hasUncategorized = items.any { it.categoryId == null }
        btnConfirm.isEnabled = !hasUncategorized && items.isNotEmpty()
    }
}



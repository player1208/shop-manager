package com.example.storemanagerassitent.ui.purchase

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.example.storemanagerassitent.data.Goods
import com.example.storemanagerassitent.data.GoodsCategory
import com.example.storemanagerassitent.data.PurchaseOrderFormatter
import com.example.storemanagerassitent.data.ReviewableItem
import com.example.storemanagerassitent.ocr.TextParser
import com.example.storemanagerassitent.ocr.OcrProcessor
import com.example.storemanagerassitent.data.api.RemoteOcrClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 智能库吏 - 核心审核页
 * 支持 从相册选择/拍照 -> OCR识别 -> 列表审核 -> 继续添加
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartClerkScreen(
    onNavigateBack: () -> Unit,
    navigateToManualAdd: () -> Unit,
    autoOpenSource: Boolean = true,
    viewModel: PurchaseOrderViewModel = viewModel(),
    clerkVm: SmartClerkViewModel = viewModel()
) {
    val context = LocalContext.current

    // 分类数据
    var categories by remember { mutableStateOf<List<GoodsCategory>>(emptyList()) }
    LaunchedEffect(Unit) {
        categories = com.example.storemanagerassitent.data.db.ServiceLocator.categoryRepository
            .observeGoodsCategories()
            .first()
    }

    // 本地商品列表（用于模糊匹配）
    val localGoods by viewModel.goods.collectAsStateWithLifecycle(emptyList())

    // 审核条目状态
    val reviewRowsState = clerkVm.rows.collectAsStateWithLifecycle(emptyList())
    val reviewRows = remember(reviewRowsState.value) {
        reviewRowsState.value.map { ReviewRowState(it.item, it.matchedGoodsId) }.toMutableList()
    }
    // 进入时先尝试合并桥接条目
    LaunchedEffect(Unit) {
        val bridged = SmartClerkBridge.drain()
        if (bridged.isNotEmpty()) {
            clerkVm.merge(bridged, localGoods)
        }
    }
    // 页面恢复到前台时再次合并一次，保证稳定
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val bridged2 = SmartClerkBridge.drain()
                if (bridged2.isNotEmpty()) {
                    clerkVm.merge(bridged2, localGoods)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showExitConfirm by remember { mutableStateOf(false) }

    // 源选择与继续添加的底部弹窗
    var showSourceSheet by remember { mutableStateOf(false) }
    var showContinueSheet by remember { mutableStateOf(false) }
    var hasShownForCurrentEmptyState by remember { mutableStateOf(false) }

    // 只有在没有任何商品时才显示来源选择；从非空变为空时可再次显示
    LaunchedEffect(reviewRowsState.value) {
        val isEmpty = reviewRows.isEmpty()
        if (isEmpty && autoOpenSource) {
            if (!hasShownForCurrentEmptyState) {
                showSourceSheet = true
                hasShownForCurrentEmptyState = true
            }
        } else {
            showSourceSheet = false
            hasShownForCurrentEmptyState = false
        }
    }

    // OCR处理状态
    var isProcessing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // 全局返回拦截：非识别中时弹出确认对话框
    BackHandler(enabled = true) {
        if (!isProcessing) {
            showExitConfirm = true
        }
    }

    // 图库选择
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            launchOcrFlow(
                context = context,
                imageUri = uri,
                onStart = { isProcessing = true; errorText = null },
                onComplete = { items ->
                    isProcessing = false
                    clerkVm.merge(items, localGoods)
                },
                onError = { err -> isProcessing = false; errorText = err }
            )
        }
        showSourceSheet = false
    }

    // 拍照（全分辨率，FileProvider 输出到临时文件）
    val tempPhotoUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        val uri = tempPhotoUri.value
        if (success && uri != null) {
            launchOcrFlow(
                context = context,
                imageUri = uri,
                onStart = { isProcessing = true; errorText = null },
                onComplete = { items ->
                    isProcessing = false
                    clerkVm.merge(items, localGoods)
                },
                onError = { err -> isProcessing = false; errorText = err }
            )
        }
        showSourceSheet = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("智能库吏", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isProcessing) {
                            showExitConfirm = true
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(
                reviewRows = reviewRows,
                onContinueAdd = { showContinueSheet = true },
                onConfirm = {
                    commitAndConfirmInboundInternal(
                        reviewRowsProvider = { reviewRows },
                        onNavigateBack = onNavigateBack,
                        viewModel = viewModel,
                        clerkVm = clerkVm
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isProcessing) {
                // 内容区域空白，真实的拦截弹窗放在最外层
            } else if (reviewRows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorText ?: "请选择图片或拍照以开始识别",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(reviewRows, key = { _, it -> it.item.id }) { index, row ->
                        ReviewRowCard(
                            row = row,
                            categories = categories,
                            onChanged = { clerkVm.updateRow(index, row.item) },
                            onRemove = { clerkVm.removeAt(index) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showSourceSheet && !isProcessing) {
            ModalBottomSheet(onDismissRequest = { showSourceSheet = false }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("选择图片来源", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("从相册中选择图片")
                    }
                    Button(onClick = {
                        val uri = createTempImageUri(context)
                        tempPhotoUri.value = uri
                        cameraLauncher.launch(uri)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("拍照")
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (showContinueSheet && !isProcessing) {
            ModalBottomSheet(onDismissRequest = { showContinueSheet = false }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("继续添加", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = {
                        showContinueSheet = false
                        navigateToManualAdd()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("手动添加")
                    }
                    Button(onClick = {
                        showContinueSheet = false
                        showSourceSheet = true
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("智能库吏")
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // 识别中：全屏不可取消弹窗，拦截所有点击与返回
        if (isProcessing) {
            BackHandler(enabled = true) { /* do nothing to block back */ }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                        Text("正在识别商品订单，请稍候…")
                    }
                }
            }
        }

        if (showExitConfirm) {
            BackHandler(enabled = true) { /* block back while dialog visible */ }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showExitConfirm = false },
                title = { Text("确认退出？") },
                text = {
                    val count = reviewRows.size
                    Text(if (count > 0) "已识别 ${count} 条商品，是否保存为草稿？" else "确定要退出智能库吏吗？")
                },
                confirmButton = {
                    TextButton(onClick = {
                        // 保存为草稿（只保存名称/数量/分类）
                        saveClerkDraft(context.applicationContext, reviewRows)
                        showExitConfirm = false
                        onNavigateBack()
                    }) { Text("保存并退出") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { showExitConfirm = false }) { Text("取消") }
                        TextButton(onClick = {
                            showExitConfirm = false
                            // 直接退出：清空智能库吏商品列表
                            clerkVm.clear()
                            onNavigateBack()
                        }) { Text("直接退出") }
                    }
                }
            )
        }
    }
}

private fun saveClerkDraft(appContext: android.content.Context, rows: List<ReviewRowState>) {
    try {
        val dm = com.example.storemanagerassitent.data.DataStoreManager(appContext)
        val draft = com.example.storemanagerassitent.data.DataStoreManager.PurchaseDraft(
            id = java.util.UUID.randomUUID().toString(),
            items = rows.map { r ->
                com.example.storemanagerassitent.data.PurchaseOrderItem(
                    goodsId = r.matchedGoodsId,
                    goodsName = r.item.editedName,
                    specifications = r.item.editedSpecifications,
                    purchasePrice = 0.0,
                    quantity = r.item.editedQuantity,
                    category = r.item.selectedCategory
                )
            },
            totalAmount = rows.sumOf { it.item.editedQuantity * 0.0 },
            totalQuantity = rows.sumOf { it.item.editedQuantity }
        )
        // 异步保存
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dm.savePurchaseDraft(draft)
        }
    } catch (_: Exception) { }
}

private suspend fun decodeBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap {
    return withContext(Dispatchers.IO) {
        // 先解码位图
        val raw = if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        // 读取 EXIF 方向并旋转
        val rotated = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val degree = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
                if (degree != 0) raw.rotateBy(degree) else raw
            } ?: raw
        } catch (_: Exception) { raw }
        // 限制长边至 3000px，避免超大图对网络与内存的影响
        rotated.scaleDownToLongSide(3000)
    }
}

private fun launchOcrFlow(
    context: android.content.Context,
    imageUri: Uri? = null,
    bitmap: Bitmap? = null,
    onStart: () -> Unit,
    onComplete: (List<ReviewableItem>) -> Unit,
    onError: (String) -> Unit
) {
    onStart()
    lifecycleScopeFrom(context)?.launchWhenResumedSafe {
        try {
            val bmp = bitmap ?: decodeBitmapFromUri(context, imageUri!!)
            val processor = OcrProcessor()
            val parser = TextParser()
            val result = processor.processImage(bmp)
            if (!result.success) {
                onError(result.error ?: "识别失败")
                return@launchWhenResumedSafe
            }
            // 同步调用远端结构化（可选覆盖，先拿本地结果兜底）
            try {
                val scf = RemoteOcrClient()
                val base64 = RemoteOcrClient.bitmapToBase64(bmp)
                val raw = scf.extractDocMulti(imageBase64 = base64)
                val structItems = parseNameQtyItems(raw)
                if (structItems.isNotEmpty()) {
                    onComplete(structItems)
                    return@launchWhenResumedSafe
                }
            } catch (_: Exception) { /* 忽略远端结构化失败，沿用本地结果 */ }
            val items = parser.parseToGoodsItems(result)
            onComplete(items)
        } catch (e: Exception) {
            onError(e.message ?: "识别失败")
        }
    }
}

// 文件输出位置：cache/images/
private fun createTempImageUri(context: android.content.Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
    val file = File(imagesDir, "photo_${System.currentTimeMillis()}.jpg")
    val authority = context.packageName + ".fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
}

private fun Bitmap.rotateBy(degrees: Int): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.scaleDownToLongSide(maxLongSide: Int): Bitmap {
    val longSide = maxOf(width, height)
    if (longSide <= maxLongSide) return this
    val scale = maxLongSide.toFloat() / longSide.toFloat()
    val w = (width * scale).toInt().coerceAtLeast(1)
    val h = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, w, h, true)
}

private fun parseNameQtyItems(rawJson: String): List<ReviewableItem> {
    return try {
        val out = mutableListOf<ReviewableItem>()
        val root = org.json.JSONObject(rawJson)
        val resp = root.optJSONObject("Response") ?: return emptyList()
        val structural = resp.optJSONArray("StructuralList") ?: return emptyList()
        for (i in 0 until structural.length()) {
            val groups = structural.optJSONObject(i)?.optJSONArray("Groups") ?: continue
            for (g in 0 until groups.length()) {
                val lines = groups.optJSONObject(g)?.optJSONArray("Lines") ?: continue
                val kv = mutableMapOf<String, String>()
                for (l in 0 until lines.length()) {
                    val line = lines.optJSONObject(l) ?: continue
                    val key = line.optJSONObject("Key")?.optString("AutoName").orEmpty()
                    val value = line.optJSONObject("Value")?.optString("AutoContent").orEmpty()
                    if (key.isNotBlank() && value.isNotBlank()) kv[key] = value
                }
                val name = kv["商品名称"]?.trim()
                val qty = kv["商品数量"]?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                if (!name.isNullOrBlank()) {
                    out.add(
                        ReviewableItem(
                            recognizedName = name,
                            recognizedSpecifications = "",
                            recognizedQuantity = qty,
                            recognizedPrice = 0.0,
                            confidence = 0.9f,
                            editedName = name,
                            editedSpecifications = "",
                            editedQuantity = qty,
                            editedPrice = 0.0,
                            selectedCategory = "",
                            isExistingProduct = false
                        )
                    )
                }
            }
        }
        out
    } catch (_: Exception) { emptyList() }
}

private fun applyFuzzyMatch(
    items: List<ReviewableItem>,
    localGoods: List<Goods>,
    categories: List<GoodsCategory>,
    reviewRows: MutableList<ReviewRowState>
) {
    items.forEach { item ->
        val match = findBestMatch(item, localGoods)
        val resolved: ReviewRowState = if (match != null) {
            val standardizedName = listOfNotNull(match.name, match.specifications)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val updated = item.copy(
                editedName = standardizedName.ifBlank { item.editedName },
                editedSpecifications = match.specifications ?: item.editedSpecifications,
                isExistingProduct = true,
                selectedCategory = match.category ?: item.selectedCategory
            )
            ReviewRowState(updated, matchedGoodsId = match.id)
        } else {
            ReviewRowState(item.copy(isExistingProduct = false), matchedGoodsId = null)
        }

        // 合并规则：名称 + 规格 + 分类 (+ 价格) 相同则累加数量
        val keyName = resolved.item.editedName.trim()
        val keySpec = resolved.item.editedSpecifications.trim()
        val keyCat = resolved.item.selectedCategory
        val keyPrice = resolved.item.editedPrice
        val existedIndex = reviewRows.indexOfFirst { rr ->
            rr.item.editedName.trim() == keyName &&
            rr.item.editedSpecifications.trim() == keySpec &&
            rr.item.selectedCategory == keyCat &&
            rr.item.editedPrice == keyPrice
        }
        if (existedIndex >= 0) {
            val existed = reviewRows[existedIndex]
            val newQty = (existed.item.editedQuantity + resolved.item.editedQuantity).coerceAtLeast(1)
            val keepGoodsId = existed.matchedGoodsId ?: resolved.matchedGoodsId
            reviewRows[existedIndex] = existed.copy(
                item = existed.item.copy(editedQuantity = newQty),
                matchedGoodsId = keepGoodsId
            )
        } else {
            reviewRows.add(resolved)
        }
    }
}

private fun findBestMatch(target: ReviewableItem, localGoods: List<Goods>): Goods? {
    return com.example.storemanagerassitent.utils.FuzzyMatcher.findBestMatch(
        target.editedName, target.editedSpecifications, localGoods, 0.6
    )
}

@Composable
private fun BottomBar(
    reviewRows: List<ReviewRowState>,
    onContinueAdd: () -> Unit,
    onConfirm: () -> Unit
) {
    val totalCount = reviewRows.sumOf { it.item.editedQuantity }
    val totalAmount = reviewRows.sumOf { it.item.editedPrice * it.item.editedQuantity }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("已添加 ${totalCount} 件商品")
            Text(PurchaseOrderFormatter.formatCurrency(totalAmount), fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onContinueAdd) { Text("继续添加") }
            Button(onClick = onConfirm) { Text("入库") }
        }
    }
}

@Composable
private fun ReviewRowCard(
    row: ReviewRowState,
    categories: List<GoodsCategory>,
    onChanged: () -> Unit,
    onRemove: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (row.item.isExistingProduct) "库房匹配" else "新品",
                    color = if (row.item.isExistingProduct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (row.item.confidence < 0.7f) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Text("低置信度，建议核对")
                        }
                    }
                    TextButton(onClick = onRemove, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )) { Text("移除") }
                }
            }
            OutlinedTextField(
                value = row.item.editedName,
                onValueChange = { row.item = row.item.copy(editedName = it); onChanged() },
                label = { Text("商品名称") },
                modifier = Modifier.fillMaxWidth()
            )
            QuantityEditor(
                quantity = row.item.editedQuantity,
                onChange = { q ->
                    row.item = row.item.copy(editedQuantity = q)
                    onChanged()
                },
                modifier = Modifier.fillMaxWidth()
            )
            CategoryDropdown(
                categories = categories,
                selectedId = row.item.selectedCategory,
                enabled = !(row.item.isExistingProduct || row.matchedGoodsId != null),
                onSelected = { id -> row.item = row.item.copy(selectedCategory = id); onChanged() }
            )
        }
    }
}

@Composable
private fun QuantityEditor(quantity: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("数量", modifier = Modifier.weight(1f))
        TextButton(onClick = { if (quantity > 1) onChange((quantity - 1).coerceAtLeast(1)) }) { Text("−") }
        var text by remember(quantity) { mutableStateOf(quantity.toString()) }
        OutlinedTextField(
            value = text,
            onValueChange = { newValue ->
                val digits = newValue.filter { it.isDigit() }
                text = digits
                val v = digits.toIntOrNull()
                if (v != null && v > 0) onChange(v.coerceAtMost(999999))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            modifier = Modifier.width(80.dp),
            label = null,
            placeholder = { Text("1") }
        )
        TextButton(onClick = { onChange((quantity + 1).coerceAtMost(999999)) }) { Text("+") }
    }
}

@Composable
private fun PriceEditor(price: Double, onChange: (Double) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(price) { mutableStateOf(if (price > 0) price.toString() else "") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let(onChange)
        },
        label = { Text("进货价") },
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun CategoryDropdown(
    categories: List<GoodsCategory>,
    selectedId: String,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name ?: "请选择分类"
    Box {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("商品分类") },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(0.dp)
                .let { m -> m },
            enabled = enabled,
            trailingIcon = if (enabled) {
                {
                    TextButton(onClick = { if (enabled) expanded = !expanded }) { Text(if (expanded) "收起" else "选择") }
                }
            } else null
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { cat ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = {
                        onSelected(cat.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun commitToViewModelOrWarnInternal(
    reviewRowsProvider: () -> List<ReviewRowState>,
    onNavigateBack: () -> Unit,
    viewModel: PurchaseOrderViewModel
): Boolean {
    // 校验分类
    val current = reviewRowsProvider()
    if (current.isEmpty()) {
        onNavigateBack()
        return true
    }
    // 仅对未命中的新品要求必须选择分类
    val hasUncategorized = current.any { it.matchedGoodsId == null && it.item.selectedCategory.isBlank() }
    if (hasUncategorized) {
        com.example.storemanagerassitent.ui.components.GlobalSuccessMessage.showSuccess("请为所有新品选择分类")
        return false
    }
    current.forEach { row ->
        val price = row.item.editedPrice.takeIf { it > 0 } ?: 0.0
        val purchase = PurchaseItem(
            goodsId = row.matchedGoodsId,
            displayName = row.item.displayName,
            quantity = row.item.editedQuantity,
            purchasePrice = price,
            categoryId = row.item.selectedCategory
        )
        viewModel.addPurchaseItem(purchase)
    }
    onNavigateBack()
    return true
}

private fun commitAndConfirmInboundInternal(
    reviewRowsProvider: () -> List<ReviewRowState>,
    onNavigateBack: () -> Unit,
    viewModel: PurchaseOrderViewModel,
    clerkVm: SmartClerkViewModel
) {
    val ok = commitToViewModelOrWarnInternal(
        reviewRowsProvider = reviewRowsProvider,
        onNavigateBack = { /* 暂不返回，待入库成功后再返回 */ },
        viewModel = viewModel
    )
    if (!ok) return
    // 直接执行确认入库，成功后再返回
    viewModel.confirmInbound(onSuccessNavigateHome = {
        // 清空智能库吏界面并返回首页（两次返回：智能库吏 -> 新建进货 -> 首页）
        clerkVm.clear()
        onNavigateBack()
        onNavigateBack()
    })
}

// 屏内状态与便捷扩展
private data class ReviewRowState(var item: ReviewableItem, val matchedGoodsId: String?)

// helpers

private fun lifecycleScopeFrom(context: android.content.Context): LifecycleCoroutineScope? {
    val fa = findFragmentActivity(context)
    return fa?.lifecycleScope
}

private fun LifecycleCoroutineScope.launchWhenResumedSafe(block: suspend () -> Unit) {
    this.launchWhenResumed { block() }
}

private tailrec fun findFragmentActivity(context: android.content.Context?): FragmentActivity? {
    return when (context) {
        is FragmentActivity -> context
        is android.content.ContextWrapper -> findFragmentActivity(context.baseContext)
        else -> null
    }
}



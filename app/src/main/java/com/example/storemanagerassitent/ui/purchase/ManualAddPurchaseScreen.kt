package com.example.storemanagerassitent.ui.purchase

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.storemanagerassitent.data.Goods
import com.example.storemanagerassitent.data.PurchaseOrderFormatter
import com.example.storemanagerassitent.data.GoodsCategory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.storemanagerassitent.permission.PermissionManager
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentResultListener

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManualAddPurchaseScreen(
    onNavigateBack: () -> Unit,
    fromSmartClerk: Boolean = false,
    viewModel: PurchaseOrderViewModel = viewModel()
) {
    val allGoods by viewModel.goods.collectAsState()
    // 分类改为 Room：用于筛选 UI
    val categoriesFlow = remember { com.example.storemanagerassitent.data.db.ServiceLocator.categoryRepository.observeGoodsCategories() }
    val categories by categoriesFlow.collectAsState(initial = emptyList())

    var selectedCategory by remember { mutableStateOf("all") }
    var searchText by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showQuickCreate by remember { mutableStateOf(false) }
    var quickName by remember { mutableStateOf("") }
    var quickQty by remember { mutableStateOf(1) }
    var quickCategoryId by remember { mutableStateOf("") }
    var quickPrice by remember { mutableStateOf("") }
    
    // 从ViewModel获取状态
    val cart by viewModel.purchaseItems.collectAsState()
    val canConfirm = viewModel.hasItems()

    // 新增：商品数量选择弹窗状态
    var showQuantityDialog by remember { mutableStateOf(false) }
    var selectedGoods by remember { mutableStateOf<Goods?>(null) }
    var quantityInput by remember { mutableStateOf("1") }
    var isExistingItem by remember { mutableStateOf(false) }
    var existingItemIndex by remember { mutableStateOf(-1) }
    


    val filteredGoods = remember(allGoods, selectedCategory, searchText) {
        val byCat = if (selectedCategory == "all") allGoods else allGoods.filter { it.category == selectedCategory }
        if (searchText.isBlank()) byCat else byCat.filter { it.name.contains(searchText, true) || it.specifications.contains(searchText, true) }
    }

    var showCartSheet by remember { mutableStateOf(false) }
    
    // 智能返回确认对话框状态
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    
    // 智能返回逻辑
    fun handleBackPress() {
        val hasOrderItems = viewModel.hasItems()
        
        if (hasOrderItems) {
            // 有商品时显示确认对话框
            showExitConfirmDialog = true
        } else {
            // 无商品时直接返回
            onNavigateBack()
        }
    }

    // 弹窗优先级：当任何弹窗显示时，返回手势应先收起弹窗
    val hasBlockingPopup = showQuantityDialog || showQuickCreate || showExitConfirmDialog || showCartSheet
    // 统一拦截系统返回/手势返回，与左上角返回按钮行为保持一致（仅当无弹窗时启用）
    BackHandler(enabled = !hasBlockingPopup) { handleBackPress() }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val rootView = LocalView.current.rootView
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSearchFocused by remember { mutableStateOf(false) }

    // 扫码弹窗相关状态
    // 批量扫码结果监听将直接把数据加入待添加列表

    fun addExistingOrNewByName(name: String, quantity: Int, categoryId: String, barcode: String? = null) {
        coroutineScope.launch {
            val matched = com.example.storemanagerassitent.data.db.ServiceLocator.goodsRepository.findByFullDisplayName(name)
            if (matched != null) {
                val suggestedPrice = if (matched.purchasePrice > 0) matched.purchasePrice else matched.retailPrice * 0.8
            viewModel.addPurchaseItem(
                PurchaseItem(
                        goodsId = matched.id,
                        displayName = matched.displayName,
                    quantity = quantity,
                    purchasePrice = suggestedPrice,
                        categoryId = if (categoryId.isNotBlank()) categoryId else matched.category,
                        barcode = matched.barcode ?: barcode
                )
            )
        } else {
            viewModel.addPurchaseItem(
                PurchaseItem(
                    goodsId = null,
                        displayName = name,
                    quantity = quantity,
                    purchasePrice = 0.0,
                    categoryId = categoryId,
                    barcode = barcode
                )
            )
        }
        }
    }

    // 通过 DialogFragment 批量扫码
    fun showBatchScanDialog() {
        val activity = findFragmentActivity(context)
        activity?.let {
            val fm = it.supportFragmentManager
            val dialog = BatchScanDialogFragment()
            dialog.show(fm, "BatchScanDialog")
        } ?: run {
            toastMessage = "无法打开相机"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            toastMessage = "相机权限被拒绝"
            return@rememberLauncherForActivityResult
        }
        showBatchScanDialog()
    }

    // 规则二：当软键盘收起后，搜索框失去焦点
    DisposableEffect(isSearchFocused) {
        if (!isSearchFocused) return@DisposableEffect onDispose { }
        var hasShownKeyboard = false
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (imeVisible) {
                hasShownKeyboard = true
            } else if (hasShownKeyboard) {
                focusManager.clearFocus()
                isSearchFocused = false
            }
            insets
        }
        rootView.requestApplyInsets()
        onDispose { ViewCompat.setOnApplyWindowInsetsListener(rootView, null) }
    }

    // 返回键先收起搜索
    BackHandler(enabled = isSearchFocused) {
        focusManager.clearFocus()
        keyboardController?.hide()
        isSearchFocused = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "手动添加商品",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val lowStockCount = remember(allGoods) { allGoods.count { it.stockQuantity <= 1 } }
                        if (lowStockCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            BadgedBox(badge = { Badge { Text(lowStockCount.toString()) } }) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "缺货预警",
                                    tint = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBackPress() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        val isRefreshing by remember { mutableStateOf(false) }
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = { /* 数据来自 Flow，保持空实现触发重组即可 */ }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            // 搜索 + 快速创建
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val showAdd = filteredGoods.isEmpty() && searchText.isNotBlank()
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("搜索商品名称/规格") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showAdd) {
                                IconButton(onClick = {
                                    quickName = searchText.trim()
                                    quickQty = 1
                                    quickCategoryId = ""
                                    quickPrice = ""
                                    showQuickCreate = true
                                }) {
                                    Icon(Icons.Filled.Add, contentDescription = "快速创建")
                                }
                            }
                            IconButton(onClick = {
                                if (PermissionManager.hasCameraPermission(context)) {
                                    showBatchScanDialog()
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = "扫码")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state -> isSearchFocused = state.isFocused },
                    shape = RoundedCornerShape(16.dp)
                )
                
            }

            // 分类筛选
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategory == category.id,
                        onClick = { selectedCategory = category.id },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // 商品列表
            if (filteredGoods.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "未找到匹配的商品", 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCategory == "all") {
                        val grouped = categories.filter { it.id != "all" }.mapNotNull { cat ->
                            val list = filteredGoods.filter { it.category == cat.id }
                            if (list.isNotEmpty()) cat to list else null
                        }
                        grouped.forEach { (cat, list) ->
                            val headerColor = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (_: Exception) { Color(0xFF9C27B0) }
                            
                            stickyHeader(key = "header_${cat.id}") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 分类色条
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(20.dp)
                                                .background(color = headerColor, shape = RoundedCornerShape(2.dp))
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "${cat.name} (${list.size})", 
                                            style = MaterialTheme.typography.titleMedium, 
                                            fontWeight = FontWeight.Bold, 
                                            color = headerColor
                                        )
                                    }
                                }
                            }
                            
                            items(list, key = { it.id }) { goods ->
                                GoodsRow(
                                    goods = goods,
                                    categories = categories,
                                    onAdd = { goods ->
                                        // 检查是否已存在相同商品
                                        val suggestedPrice = if (goods.purchasePrice > 0) goods.purchasePrice else goods.retailPrice * 0.8
                                        val existingIndex = cart.indexOfFirst { it.goodsId == goods.id && it.purchasePrice == suggestedPrice }
                                        
                                        if (existingIndex >= 0) {
                                            // 商品已存在，显示提醒弹窗
                                            isExistingItem = true
                                            existingItemIndex = existingIndex
                                            selectedGoods = goods
                                            quantityInput = cart[existingIndex].quantity.toString()
                                            showQuantityDialog = true
                                        } else {
                                            // 新商品，显示数量选择弹窗
                                            isExistingItem = false
                                            existingItemIndex = -1
                                            selectedGoods = goods
                                            quantityInput = "1"
                                            showQuantityDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        items(filteredGoods, key = { g -> g.id }) { goods: Goods ->
                            GoodsRow(
                                goods = goods,
                                categories = categories,
                                onAdd = { goods ->
                                    // 检查是否已存在相同商品
                                    val suggestedPrice = if (goods.purchasePrice > 0) goods.purchasePrice else goods.retailPrice * 0.8
                                    val existingIndex = cart.indexOfFirst { it.goodsId == goods.id && it.purchasePrice == suggestedPrice }
                                    
                                    if (existingIndex >= 0) {
                                        // 商品已存在，显示提醒弹窗
                                        isExistingItem = true
                                        existingItemIndex = existingIndex
                                        selectedGoods = goods
                                        quantityInput = cart[existingIndex].quantity.toString()
                                        showQuantityDialog = true
                                    } else {
                                        // 新商品，显示数量选择弹窗
                                        isExistingItem = false
                                        existingItemIndex = -1
                                        selectedGoods = goods
                                        quantityInput = "1"
                                        showQuantityDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 底部栏，左侧查看待添加列表，右侧确认入库
            val bottomInset = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp, modifier = Modifier.padding(bottom = bottomInset)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { if (cart.isNotEmpty()) showCartSheet = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "查看待添加列表",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        val uniqueItemsCount = viewModel.getItemTypesCount()
                        Text(
                            text = if (uniqueItemsCount > 0) "待添加 $uniqueItemsCount 种商品（点此查看）" else "待添加 0 种商品",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (fromSmartClerk) {
                        Button(
                            onClick = {
                                // 将当前购物车条目转为 ReviewableItem 并投递给智能库吏
                                val items = cart.map { ci ->
                                    com.example.storemanagerassitent.data.ReviewableItem(
                                        recognizedName = ci.displayName,
                                        recognizedSpecifications = "",
                                        recognizedQuantity = ci.quantity,
                                        recognizedPrice = ci.purchasePrice,
                                        confidence = 0.85f,
                                        editedName = ci.displayName,
                                        editedSpecifications = "",
                                        editedQuantity = ci.quantity,
                                        editedPrice = ci.purchasePrice,
                                        selectedCategory = ci.categoryId,
                                        isExistingProduct = ci.goodsId != null
                                    )
                                }
                                SmartClerkBridge.add(items)
                                // 清空临时购物车，避免与普通手动添加共享缓存
                                viewModel.clearItems()
                                com.example.storemanagerassitent.ui.components.GlobalSuccessMessage.showSuccess("已添加到智能库吏")
                                // 直接返回智能库吏（外部导航根据 manualFromSmartClerk 控制去向）
                                onNavigateBack()
                            },
                            enabled = canConfirm,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("添加到智能库吏") }
                    } else {
                        Button(
                            onClick = {
                                viewModel.confirmInbound(onSuccessNavigateHome = {
                                    onNavigateBack()
                                })
                            },
                            enabled = canConfirm,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("确认入库") }
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 规则一：点击外部区域后自动收起
        if (isSearchFocused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        isSearchFocused = false
                    }
            )
        }
        }

        // 顶部优雅提示，不遮挡底部栏
        ElegantTopToast(toastMessage = toastMessage, onDismiss = { toastMessage = null })
    }

    // 快速创建弹窗
    if (showQuickCreate) {
        AlertDialog(
            onDismissRequest = { showQuickCreate = false },
            title = { Text("快速创建商品", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quickName,
                        onValueChange = { quickName = it },
                        label = { Text("商品名称及编号") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 自定义数量调节器（默认最小 1）
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("* 商品数量", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(onClick = { if (quickQty > 1) quickQty -= 1 }, enabled = quickQty > 1) {
                                Text("−")
                            }
                            Text(quickQty.toString(), modifier = Modifier.padding(horizontal = 12.dp))
                            FilledTonalIconButton(onClick = { quickQty += 1 }) { Text("+") }
                        }
                    }

                    // 类别选择
                    var expand by remember { mutableStateOf(false) }
                    val selectedCategoryName = categories.firstOrNull { it.id == quickCategoryId }?.name ?: "请选择分类"
                    ExposedDropdownMenuBox(expanded = expand, onExpandedChange = { expand = it }) {
                        OutlinedTextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("* 种类") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                            categories.filter { it.id != "all" }.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat.name) }, onClick = {
                                    quickCategoryId = cat.id
                                    expand = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quickPrice,
                        onValueChange = { input -> quickPrice = input.filter { it.isDigit() || it == '.' } },
                        label = { Text("进货价（可选）") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (quickQty <= 0 || quickCategoryId.isBlank() || quickName.isBlank()) {
                        toastMessage = "请完善必填项"
                        return@TextButton
                    }
                    val price = quickPrice.toDoubleOrNull() ?: 0.0
                    viewModel.addPurchaseItem(
                        PurchaseItem(goodsId = null, displayName = quickName.trim(), quantity = quickQty, purchasePrice = price, categoryId = quickCategoryId)
                    )
                    toastMessage = "已加入待添加"
                    showQuickCreate = false
                    searchText = ""
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { showQuickCreate = false }) { Text("取消") } }
        )
    }

    // 商品数量选择弹窗
    if (showQuantityDialog && selectedGoods != null) {
        val goods = selectedGoods!!
        val suggestedPrice = if (goods.purchasePrice > 0) goods.purchasePrice else goods.retailPrice * 0.8
        
        AlertDialog(
            onDismissRequest = { showQuantityDialog = false },
            title = { 
                Text(
                    if (isExistingItem) "商品已存在，是否修改数量？" else "选择商品数量",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 商品信息显示
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                goods.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "进货价: ${PurchaseOrderFormatter.formatCurrency(suggestedPrice)}",
                                color = Color(0xFF9C27B0),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isExistingItem) {
                                Text(
                                    "当前数量: ${cart[existingItemIndex].quantity}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    // 数量输入
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { input -> 
                            quantityInput = input.filter { it.isDigit() }
                        },
                        label = { Text("商品数量") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // 数量调节按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val currentQty = quantityInput.toIntOrNull() ?: 1
                                if (currentQty > 1) {
                                    quantityInput = (currentQty - 1).toString()
                                }
                            }
                        ) {
                            Text("−", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            quantityInput,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val currentQty = quantityInput.toIntOrNull() ?: 1
                                quantityInput = (currentQty + 1).toString()
                            }
                        ) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val qty = quantityInput.toIntOrNull() ?: 1
                        if (qty <= 0) {
                            toastMessage = "数量必须大于0"
                            return@TextButton
                        }
                        
                        if (isExistingItem) {
                            // 更新现有商品数量
                            viewModel.updateItemQuantity(goods.id, suggestedPrice, qty)
                            toastMessage = "已更新商品数量"
                        } else {
                            // 添加新商品
                            viewModel.addPurchaseItem(
                                PurchaseItem(
                                    goodsId = goods.id,
                                    displayName = goods.displayName,
                                    quantity = qty,
                                    purchasePrice = suggestedPrice,
                                    categoryId = goods.category
                                )
                            )
                            toastMessage = "已添加商品"
                        }
                        showQuantityDialog = false
                    }
                ) { 
                    Text(if (isExistingItem) "更新" else "添加") 
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuantityDialog = false }) { 
                    Text("取消") 
                }
            }
        )
    }

    // 批量扫码结果接收：将弹窗返回的商品名称和数量加入底部待确认列表
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val activity = findFragmentActivity(context)
        val manager = activity?.supportFragmentManager
        val listener = FragmentResultListener { _, bundle ->
            val list = bundle.getParcelableArrayList<android.os.Bundle>(BatchScanDialogFragment.RESULT_ITEMS)
            list?.forEach { b ->
                val name = b.getString(BatchScanDialogFragment.ITEM_NAME, "").trim()
                val qty = b.getInt(BatchScanDialogFragment.ITEM_QUANTITY, 1)
                val catId = b.getString(BatchScanDialogFragment.ITEM_CATEGORY_ID, "")
                val barcode = b.getString(BatchScanDialogFragment.ITEM_BARCODE, "").trim()
                if (name.isNotBlank() && qty > 0) {
                    if (barcode.isNotBlank()) {
                        // 若扫描返回了条码，并且匹配到现有商品且其条码为空，则补写条码（IO协程）
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val dao = com.example.storemanagerassitent.data.db.ServiceLocator.database.goodsDao()
                            val existing = dao.findByFullDisplayName(name)
                            if (existing != null && (existing.barcode == null || existing.barcode.isBlank())) {
                                dao.update(existing.copy(barcode = barcode, lastUpdated = System.currentTimeMillis()))
                            }
                        }
                    }
                    addExistingOrNewByName(name, qty, catId)
                }
            }
            if (!list.isNullOrEmpty()) {
                toastMessage = "已添加到待添加订单"
            }
        }
        manager?.setFragmentResultListener(BatchScanDialogFragment.RESULT_KEY, lifecycleOwner, listener)
        onDispose { }
    }

    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("待添加列表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (cart.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("暂无待添加商品", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    cart.forEachIndexed { index, item ->
                        val catName = categories.firstOrNull { it.id == item.categoryId }?.name
                            ?: if (item.categoryId.isBlank()) "未分类" else "未知分类"
                        CartRow(
                            item = item,
                            categoryName = catName,
                            onIncrease = {
                                viewModel.updateItemQuantityAt(index, item.quantity + 1)
                            },
                            onDecrease = {
                                val newQ = (item.quantity - 1).coerceAtLeast(1)
                                viewModel.updateItemQuantityAt(index, newQ)
                            },
                            onRemove = {
                                viewModel.removeItemAt(index)
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { viewModel.clearItems() }) { Text("清空") }
                                TextButton(onClick = { showCartSheet = false }) { Text("关闭") }
                            }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    
    // 智能退出确认对话框
    if (showExitConfirmDialog) {
        val saveAction: () -> Unit = if (fromSmartClerk) {
            {
                showExitConfirmDialog = false
                // 将当前购物车条目转为 ReviewableItem 并投递给智能库吏
                val items = viewModel.purchaseItems.value.map { ci ->
                    com.example.storemanagerassitent.data.ReviewableItem(
                        recognizedName = ci.displayName,
                        recognizedSpecifications = "",
                        recognizedQuantity = ci.quantity,
                        recognizedPrice = ci.purchasePrice,
                        confidence = 0.85f,
                        editedName = ci.displayName,
                        editedSpecifications = "",
                        editedQuantity = ci.quantity,
                        editedPrice = ci.purchasePrice,
                        selectedCategory = ci.categoryId,
                        isExistingProduct = ci.goodsId != null
                    )
                }
                SmartClerkBridge.add(items)
                viewModel.clearItems()
                onNavigateBack()
            }
        } else {
            {
                showExitConfirmDialog = false
                viewModel.saveOrderAsDraft()
                onNavigateBack()
            }
        }
        ExitConfirmationDialog(
            orderItemsCount = viewModel.getItemTypesCount(),
            onDismiss = { showExitConfirmDialog = false },
            onContinueEdit = { showExitConfirmDialog = false },
            onDiscardAndExit = {
                showExitConfirmDialog = false
                viewModel.clearOrderData()
                onNavigateBack()
            },
            onSaveAndExit = saveAction,
            saveButtonText = if (fromSmartClerk) "添加到智能库吏并返回" else "保存并退出"
        )
    }
}

private tailrec fun findFragmentActivity(context: Context?): FragmentActivity? {
    return when (context) {
        is FragmentActivity -> context
        is ContextWrapper -> findFragmentActivity(context.baseContext)
        else -> null
    }
}

data class PurchaseItem(
    val goodsId: String?,
    val displayName: String,
    val quantity: Int,
    val purchasePrice: Double,
    val categoryId: String,
    val barcode: String? = null
)

@Composable
private fun ElegantTopToast(toastMessage: String?, onDismiss: () -> Unit) {
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(1000)
            onDismiss()
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                    Text(toastMessage ?: "", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun CartRow(
    item: PurchaseItem,
    categoryName: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("进货价: ${PurchaseOrderFormatter.formatCurrency(item.purchasePrice)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("分类: ${categoryName}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onDecrease() }, enabled = item.quantity > 1) { Text("−") }
                Text(item.quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                TextButton(onClick = { onIncrease() }) { Text("+") }
            }
            TextButton(onClick = onRemove, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("移除") }
        }
    }
}

@Composable
private fun GoodsRow(
    goods: Goods,
    categories: List<GoodsCategory>,
    onAdd: (Goods) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowStock = goods.stockQuantity <= 1 // 预警阈值设为1
    
    // 获取分类颜色
    val categoryColor = remember(goods.category, categories) {
        val category = categories.find { it.id == goods.category }
        try {
            Color(android.graphics.Color.parseColor(category?.colorHex ?: "#9C27B0"))
        } catch (_: Exception) {
            Color(0xFF9C27B0)
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = when {
            isLowStock -> androidx.compose.foundation.BorderStroke(2.dp, Color.Red)
            else -> androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f))
        }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    goods.displayName, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                // 库存数量显示
                Text(
                    "库存: ${goods.stockQuantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLowStock) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(Modifier.height(4.dp))
            val suggestedPrice = if (goods.purchasePrice > 0) goods.purchasePrice else goods.retailPrice * 0.8
            Text("进货价: ${PurchaseOrderFormatter.formatCurrency(suggestedPrice)}", color = Color(0xFF9C27B0))
            if (isLowStock) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "⚠️ 库存不足",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onAdd(goods)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("添加")
            }
        }
    }
}

/**
 * 退出确认对话框
 */
@Composable
fun ExitConfirmationDialog(
    orderItemsCount: Int,
    onDismiss: () -> Unit,
    onContinueEdit: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onSaveAndExit: () -> Unit,
    saveButtonText: String = "保存并退出"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "确认退出？",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (orderItemsCount > 0) {
                        "您当前进货单中有 $orderItemsCount 种商品，请选择退出方式："
                    } else {
                        "确定要退出新建进货单吗？"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 三个按钮的布局
                if (orderItemsCount > 0) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 保存并退出按钮
                        Button(
                            onClick = onSaveAndExit,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = saveButtonText,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // 直接退出按钮
                        Button(
                            onClick = onDiscardAndExit,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "直接退出（清空数据）",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // 继续编辑按钮
                        TextButton(
                            onClick = onContinueEdit,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "继续编辑",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (orderItemsCount == 0) {
                Button(
                    onClick = onDiscardAndExit,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "确认退出",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // 有商品时，确认按钮为空，因为按钮在text区域
                Spacer(modifier = Modifier.size(0.dp))
            }
        },
        dismissButton = {
            if (orderItemsCount == 0) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            } else {
                // 有商品时，取消按钮为空，因为按钮在text区域
                Spacer(modifier = Modifier.size(0.dp))
            }
        }
    )
}



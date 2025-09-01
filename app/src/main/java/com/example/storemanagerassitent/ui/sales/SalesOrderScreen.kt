package com.example.storemanagerassitent.ui.sales

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storemanagerassitent.data.Goods
import com.example.storemanagerassitent.data.PaymentMethod
import com.example.storemanagerassitent.data.PaymentType
import com.example.storemanagerassitent.data.SalesOrderFormatter
import com.example.storemanagerassitent.data.SalesOrderItem

/**
 * 销售订单主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesOrderScreen(
    onNavigateBack: () -> Unit = {},
    prefillGoodsData: ((SalesOrderViewModel) -> Unit)? = null,
    viewModel: SalesOrderViewModel = viewModel()
) {
    val salesOrderState by viewModel.salesOrderState.collectAsState()
    val showProductSelection by viewModel.showProductSelection.collectAsState()
    val showQuantityDialog by viewModel.showQuantityDialog.collectAsState()
    val selectedGoods by viewModel.selectedGoods.collectAsState()
    val quantityDialogAmount by viewModel.quantityDialogAmount.collectAsState()
    val showPriceEditDialog by viewModel.showPriceEditDialog.collectAsState()
    val editingOrderItem by viewModel.editingOrderItem.collectAsState()
    val priceEditAmount by viewModel.priceEditAmount.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    
    // 智能返回确认对话框状态
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    
    // 页面进入时重置UI状态和处理预填充数据
    LaunchedEffect(Unit) {
        viewModel.resetUIStates()
        // 处理预填充商品数据
        prefillGoodsData?.invoke(viewModel)
    }
    
    // 智能返回逻辑
    fun handleBackPress() {
        val hasOrderItems = salesOrderState.items.isNotEmpty()
        
        if (hasOrderItems) {
            // 有商品时显示确认对话框
            showExitConfirmDialog = true
        } else {
            // 无商品时直接返回
            onNavigateBack()
        }
    }

    // 弹窗优先级：当任何弹窗显示时，返回手势应先收起弹窗
    val hasBlockingPopup = showProductSelection || showQuantityDialog || showPriceEditDialog || showExitConfirmDialog
    // 统一拦截系统返回/手势返回，与左上角返回按钮行为保持一致（仅当无弹窗时启用）
    BackHandler(enabled = !hasBlockingPopup) { handleBackPress() }
    
    // 商品选择界面作为全屏覆盖
    if (showProductSelection) {
        InventoryStyleProductSelectionScreen(
            onDismiss = viewModel::hideProductSelection,
            onSelectProduct = viewModel::selectProduct,
            onAddConfirmedItems = viewModel::addConfirmedItems,
            goodsFlow = viewModel.goodsFlow,
            cartItems = cartItems,
            onAddToCart = viewModel::addToCart,
            onRemoveFromCart = viewModel::removeFromCart,
            onClearCart = viewModel::clearCart
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "新建销售单",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { handleBackPress() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 可滚动的内容区域
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶部操作栏
                item {
                    TopActionBar(
                        onAddProduct = viewModel::showProductSelection,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                    )
                }
                
                // 订单列表项
                if (salesOrderState.items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "请点击上方按钮添加商品",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(salesOrderState.items) { item ->
                        OrderItemCard(
                            item = item,
                            onRemove = { viewModel.removeOrderItem(item.id) },
                            onUpdateQuantity = { quantity -> viewModel.updateOrderItemQuantity(item.id, quantity) },
                            onEditPrice = { viewModel.showPriceEditDialog(item) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                
                // 结算信息区
                item {
                    PaymentInfoArea(
                        salesOrderState = salesOrderState,
                        onSetPaymentMethod = viewModel::setPaymentMethod,
                        onSetPaymentType = viewModel::setPaymentType,
                        onSetDepositAmount = viewModel::setDepositAmount,
                        onSetCustomerName = viewModel::setCustomerName,
                        onSetCustomerPhone = viewModel::setCustomerPhone,
                        onSetCustomerAddress = viewModel::setCustomerAddress,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            // 底部结算栏 - 固定在底部
            BottomSettlementBar(
                totalAmount = salesOrderState.totalAmount,
                canComplete = salesOrderState.canCompleteOrder,
                onComplete = {
                    viewModel.completeOrder {
                        onNavigateBack() // 完成订单后返回主页
                    }
                },
                modifier = Modifier.padding(16.dp)
            )
        }
        
        // 数量确认对话框
        if (showQuantityDialog && selectedGoods != null) {
            QuantityConfirmDialog(
                goods = selectedGoods!!,
                quantity = quantityDialogAmount,
                onQuantityChange = viewModel::updateQuantity,
                onIncrease = viewModel::increaseQuantity,
                onDecrease = viewModel::decreaseQuantity,
                onConfirm = viewModel::confirmProductQuantity,
                onDismiss = viewModel::cancelProductQuantity
            )
        }
        
        // 价格编辑对话框
        if (showPriceEditDialog && editingOrderItem != null) {
            PriceEditDialog(
                orderItem = editingOrderItem!!,
                price = priceEditAmount,
                onPriceChange = viewModel::updatePriceEditAmount,
                onConfirm = viewModel::confirmPriceEdit,
                onDismiss = viewModel::hidePriceEditDialog
            )
        }
        
        // 智能退出确认对话框
        if (showExitConfirmDialog) {
            ExitConfirmationDialog(
                orderItemsCount = salesOrderState.items.size,
                onDismiss = { showExitConfirmDialog = false },
                onContinueEdit = { showExitConfirmDialog = false },
                onDiscardAndExit = {
                    showExitConfirmDialog = false
                    viewModel.clearOrderData() // 清空订单数据
                    onNavigateBack()
                },
                onSaveAndExit = {
                    showExitConfirmDialog = false
                    viewModel.saveOrderAsDraft() // 保存为草稿
                    onNavigateBack()
                }
            )
        }
        }
    }
}

/**
 * 顶部操作栏
 */
@Composable
fun TopActionBar(
    onAddProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onAddProduct,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "添加商品",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "添加商品",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}



/**
 * 订单项卡片
 */
@Composable
fun OrderItemCard(
    item: SalesOrderItem,
    onRemove: () -> Unit,
    onUpdateQuantity: (Int) -> Unit,
    onEditPrice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 商品名称、数量和删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "* ${item.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 在商品名称下方显示数量信息
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "数量: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                            // 简洁的数量调节器（每次严格 ±1）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            IconButton(
                                onClick = {
                                    val next = (item.quantity - 1).coerceAtLeast(1)
                                    if (next != item.quantity) onUpdateQuantity(next)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(
                                    text = "−",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                text = item.quantity.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            IconButton(
                                onClick = {
                                    val next = item.quantity + 1
                                    onUpdateQuantity(next)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 价格信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 单价（可点击编辑）
                    Text(
                        text = "单价: ${SalesOrderFormatter.formatCurrency(item.unitPrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { onEditPrice() }
                    )
                
                // 小计
                    Text(
                        text = "小计: ${SalesOrderFormatter.formatCurrency(item.subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
            }
        }
    }
}

/**
 * 结算信息区
 */
@Composable
fun PaymentInfoArea(
    salesOrderState: com.example.storemanagerassitent.data.SalesOrderState,
    onSetPaymentMethod: (PaymentMethod) -> Unit,
    onSetPaymentType: (PaymentType) -> Unit,
    onSetDepositAmount: (Double) -> Unit,
    onSetCustomerName: (String) -> Unit,
    onSetCustomerPhone: (String) -> Unit,
    onSetCustomerAddress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 支付方式
            Column {
                Text(
                    text = "* 支付方式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        FilterChip(
                            selected = salesOrderState.paymentMethod == method,
                            onClick = { onSetPaymentMethod(method) },
                            label = { Text(method.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
            
            HorizontalDivider()
            
            // 付款类型 (Radio Buttons)
            Column {
                Text(
                    text = "* 付款类型",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row {
                    PaymentType.values().forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = salesOrderState.paymentType == type,
                                onClick = { onSetPaymentType(type) }
                            )
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                if (salesOrderState.paymentType == PaymentType.DEPOSIT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = salesOrderState.depositAmount.toString(),
                        onValueChange = { value ->
                            val amount = value.toDoubleOrNull() ?: 0.0
                            onSetDepositAmount(amount)
                        },
                        label = { Text("定金金额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            HorizontalDivider()
            
            // 客户信息
            Column {
                Text(
                    text = "客户信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = salesOrderState.customerName,
                    onValueChange = onSetCustomerName,
                    label = { Text("客户姓名") },
                    placeholder = { Text("选填") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = salesOrderState.customerPhone,
                    onValueChange = onSetCustomerPhone,
                    label = { Text("联系电话") },
                    placeholder = { Text("选填") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = salesOrderState.customerAddress,
                    onValueChange = onSetCustomerAddress,
                    label = { Text("收货地址") },
                    placeholder = { Text("选填") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 底部结算栏
 */
@Composable
fun BottomSettlementBar(
    totalAmount: Double,
    canComplete: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomInset),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：合计金额
            Column {
                Text(
                    text = "合计:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = SalesOrderFormatter.formatCurrency(totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 右侧：收款按钮
            Button(
                onClick = onComplete,
                enabled = canComplete,
                modifier = Modifier
                    .height(48.dp)
                    .width(120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (canComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                Text(
                    text = "收款",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (canComplete) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
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
    onSaveAndExit: () -> Unit
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
                        "您当前订单中有 $orderItemsCount 件商品，请选择退出方式："
                } else {
                    "确定要退出新建销售单吗？"
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
                                text = "保存并退出",
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
            TextButton(onClick = onContinueEdit) {
                Text(
                        text = "取消",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                }
            } else {
                // 有商品时，取消按钮为空，因为按钮在text区域
                Spacer(modifier = Modifier.size(0.dp))
            }
        }
    )
} 
package com.example.storemanagerassitent.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storemanagerassitent.data.Goods
import com.example.storemanagerassitent.data.PaymentMethod
import com.example.storemanagerassitent.data.PaymentType
import com.example.storemanagerassitent.data.SalesOrder
import com.example.storemanagerassitent.data.SalesOrderItem
import com.example.storemanagerassitent.data.SalesOrderState
import com.example.storemanagerassitent.data.SampleData
// 移除模拟数据依赖，全部由 Room 仓库与真实数据驱动
import com.example.storemanagerassitent.data.DataStoreManager
import com.example.storemanagerassitent.ui.components.GlobalSuccessMessage
import com.example.storemanagerassitent.data.db.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 销售订单ViewModel
 */
class SalesOrderViewModel : ViewModel() {
    
    // 销售订单状态
    private val _salesOrderState = MutableStateFlow(SalesOrderState())
    val salesOrderState: StateFlow<SalesOrderState> = _salesOrderState.asStateFlow()
    
    // 商品选择状态
    private val _productSelectionState = MutableStateFlow(com.example.storemanagerassitent.data.ProductSelectionState())
    val productSelectionState: StateFlow<com.example.storemanagerassitent.data.ProductSelectionState> = _productSelectionState.asStateFlow()
    
    // 是否显示商品选择界面
    private val _showProductSelection = MutableStateFlow(false)
    val showProductSelection: StateFlow<Boolean> = _showProductSelection.asStateFlow()
    
    // 是否显示数量确认对话框
    private val _showQuantityDialog = MutableStateFlow(false)
    val showQuantityDialog: StateFlow<Boolean> = _showQuantityDialog.asStateFlow()
    
    // 当前选择的商品
    private val _selectedGoods = MutableStateFlow<Goods?>(null)
    val selectedGoods: StateFlow<Goods?> = _selectedGoods.asStateFlow()
    
    // 数量确认对话框的数量
    private val _quantityDialogAmount = MutableStateFlow(1)
    val quantityDialogAmount: StateFlow<Int> = _quantityDialogAmount.asStateFlow()
    
    // 是否显示价格编辑对话框
    private val _showPriceEditDialog = MutableStateFlow(false)
    val showPriceEditDialog: StateFlow<Boolean> = _showPriceEditDialog.asStateFlow()
    
    // 当前编辑价格的订单项
    private val _editingOrderItem = MutableStateFlow<SalesOrderItem?>(null)
    val editingOrderItem: StateFlow<SalesOrderItem?> = _editingOrderItem.asStateFlow()
    
    // 价格编辑对话框的价格
    private val _priceEditAmount = MutableStateFlow(0.0)
    val priceEditAmount: StateFlow<Double> = _priceEditAmount.asStateFlow()
    
    // 购物车状态 - 保存用户选择但未确认添加到订单的商品
    private val _cartItems = MutableStateFlow<List<SalesOrderItem>>(emptyList())
    val cartItems: StateFlow<List<SalesOrderItem>> = _cartItems.asStateFlow()
    
    // 是否显示购物车对话框
    private val _showCartDialog = MutableStateFlow(false)
    val showCartDialog: StateFlow<Boolean> = _showCartDialog.asStateFlow()
    
    // 草稿ID
    private var draftId: String = UUID.randomUUID().toString()
    
    // 数据存储管理器
    private var dataStoreManager: DataStoreManager? = null

    // 实时商品快照（来自数据库）
    private val _allGoods = MutableStateFlow<List<Goods>>(emptyList())
    val goodsFlow: StateFlow<List<Goods>> = _allGoods.asStateFlow()
    
    /**
     * 初始化数据存储管理器
     */
    fun initializeDataStore(dataStore: DataStoreManager) {
        dataStoreManager = dataStore
    }
    
    /**
     * 显示商品选择界面
     */
    fun showProductSelection() {
        _showProductSelection.value = true
        _productSelectionState.value = com.example.storemanagerassitent.data.ProductSelectionState(isSelectionMode = true)
    }
    
    /**
     * 隐藏商品选择界面
     */
    fun hideProductSelection() {
        _showProductSelection.value = false
        _productSelectionState.value = com.example.storemanagerassitent.data.ProductSelectionState()
    }
    
    /**
     * 重置UI状态（不包括订单数据）
     */
    fun resetUIStates() {
        _productSelectionState.value = com.example.storemanagerassitent.data.ProductSelectionState()
        _showProductSelection.value = false
        _showQuantityDialog.value = false
        _selectedGoods.value = null
        _quantityDialogAmount.value = 1
        _showPriceEditDialog.value = false
        _editingOrderItem.value = null
        _priceEditAmount.value = 0.0
        _showCartDialog.value = false
    }

    init {
        // 订阅数据库商品变化，用于选择器和库存校验
        viewModelScope.launch {
            ServiceLocator.goodsRepository.observeGoods().collect { goods ->
                _allGoods.value = goods
            }
        }
    }
    
    /**
     * 保存为草稿
     */
    private fun saveDraft() {
        viewModelScope.launch {
            dataStoreManager?.let { dataStore ->
                val currentState = _salesOrderState.value
                val draft = DataStoreManager.SalesDraft(
                    id = draftId,
                    items = currentState.items,
                    paymentMethod = currentState.paymentMethod?.name,
                    paymentType = currentState.paymentType?.name,
                    depositAmount = currentState.depositAmount,
                    customerName = currentState.customerName,
                    customerPhone = currentState.customerPhone,
                    customerAddress = currentState.customerAddress,
                    totalAmount = currentState.totalAmount,
                    totalQuantity = currentState.items.sumOf { it.quantity },
                    updatedAt = System.currentTimeMillis()
                )
                dataStore.saveSalesDraft(draft)
            }
        }
    }
    
    /**
     * 保存草稿并显示提示
     */
    fun saveOrderAsDraft() {
        if (_salesOrderState.value.items.isNotEmpty()) {
            saveDraft()
            GlobalSuccessMessage.showSuccess("销售单已保存为草稿")
        }
    }
    
    /**
     * 清空订单数据
     */
    fun clearOrderData() {
        _salesOrderState.value = SalesOrderState()
        draftId = UUID.randomUUID().toString()
        GlobalSuccessMessage.showSuccess("销售单数据已清空")
    }
    
    /**
     * 加载草稿
     */
    fun loadDraft(draft: DataStoreManager.SalesDraft) {
        draftId = draft.id
        _salesOrderState.value = SalesOrderState(
            items = draft.items,
            paymentMethod = draft.paymentMethod?.let { PaymentMethod.valueOf(it) },
            paymentType = draft.paymentType?.let { PaymentType.valueOf(it) },
            depositAmount = draft.depositAmount,
            customerName = draft.customerName,
            customerPhone = draft.customerPhone,
            customerAddress = draft.customerAddress
        )
        GlobalSuccessMessage.showSuccess("草稿已加载")
    }
    
    /**
     * 检查是否有订单商品
     */
    fun hasItems(): Boolean {
        return _salesOrderState.value.items.isNotEmpty()
    }
    
    /**
     * 重置整个订单（用于新建订单时）
     */
    fun resetOrder() {
        _salesOrderState.value = SalesOrderState()
        _cartItems.value = emptyList() // 完成订单后清空购物车
        resetUIStates()
    }
    
    /**
     * 选择商品
     * 如果商品已在订单中，显示当前数量
     */
    fun selectProduct(goods: Goods) {
        _selectedGoods.value = goods
        
        // 检查商品是否已在订单中
        val existingItem = _salesOrderState.value.items.find { 
            it.goodsId == goods.id && it.unitPrice == goods.retailPrice 
        }
        
        if (existingItem != null) {
            // 如果商品已存在，显示当前数量
            _quantityDialogAmount.value = existingItem.quantity
        } else {
            // 如果商品不存在，默认数量为1
            _quantityDialogAmount.value = 1
        }
        
        _showQuantityDialog.value = true
    }
    
    /**
     * 确认商品数量
     * 如果商品已存在，则更新数量而不是添加重复项
     */
    fun confirmProductQuantity() {
        val goods = _selectedGoods.value ?: return
        val quantity = _quantityDialogAmount.value
        
        // 检查库存
        if (quantity > goods.stockQuantity) {
            GlobalSuccessMessage.showSuccess("库存不足，当前库存：${goods.stockQuantity}")
            return
        }
        
        // 创建订单项
        val orderItem = SalesOrderItem(
            goodsId = goods.id,
            goodsName = goods.name,
            specifications = goods.specifications,
            unitPrice = goods.retailPrice, // 使用零售价
            quantity = quantity,
            category = goods.category
        )
        
        // 添加到订单，处理重复商品
        val currentItems = _salesOrderState.value.items.toMutableList()
        
        // 查找是否已存在相同商品（通过goodsId和unitPrice判断）
        val existingIndex = currentItems.indexOfFirst { 
            it.goodsId == orderItem.goodsId && it.unitPrice == orderItem.unitPrice 
        }
        
        if (existingIndex >= 0) {
            // 如果商品已存在，累加数量而不是替换
            val existingItem = currentItems[existingIndex]
            val newQuantity = existingItem.quantity + orderItem.quantity
            
            // 检查累加后的数量是否超过库存
            if (newQuantity > goods.stockQuantity) {
                val remainingStock = goods.stockQuantity - existingItem.quantity
                GlobalSuccessMessage.showSuccess("库存不足，最多还可添加 $remainingStock 件")
                return
            }
            
            currentItems[existingIndex] = existingItem.copy(quantity = newQuantity)
            GlobalSuccessMessage.showSuccess("数量已累加至 $newQuantity")
        } else {
            // 如果商品不存在，添加新记录
            currentItems.add(orderItem)
            GlobalSuccessMessage.showSuccess("添加成功")
        }
        
        _salesOrderState.value = _salesOrderState.value.copy(items = currentItems)
        
        // 自动保存草稿
        saveDraft()
        
        // 关闭对话框
        _showQuantityDialog.value = false
        _selectedGoods.value = null
    }
    
    /**
     * 批量添加商品到订单
     * 如果商品已存在，则更新数量而不是添加重复项
     */
    fun addMultipleItems(items: List<SalesOrderItem>) {
        if (items.isEmpty()) return
        
        val currentItems = _salesOrderState.value.items.toMutableList()
        
        // 处理每个要添加的商品
        items.forEach { newItem ->
            // 查找是否已存在相同商品（通过goodsId和unitPrice判断）
            val existingIndex = currentItems.indexOfFirst { 
                it.goodsId == newItem.goodsId && it.unitPrice == newItem.unitPrice 
            }
            
            if (existingIndex >= 0) {
                // 如果商品已存在，累加数量而不是替换
                val existingItem = currentItems[existingIndex]
                val newQuantity = existingItem.quantity + newItem.quantity
                currentItems[existingIndex] = existingItem.copy(quantity = newQuantity)
            } else {
                // 如果商品不存在，添加新记录
                currentItems.add(newItem)
            }
        }
        
        _salesOrderState.value = _salesOrderState.value.copy(items = currentItems)
        
        // 自动保存草稿
        saveDraft()
        
        // 关闭商品选择界面
        hideProductSelection()
        
        // 显示成功提示
        val itemCount = items.size
        GlobalSuccessMessage.showSuccess("成功添加 $itemCount 件商品")
    }
    
    /**
     * 直接批量添加已确认的商品项（无需再次确认）
     * 如果商品已存在，则更新数量而不是添加重复项
     */
    fun addConfirmedItems(items: List<SalesOrderItem>) {
        if (items.isEmpty()) return
        
        val currentItems = _salesOrderState.value.items.toMutableList()
        
        // 处理每个要添加的商品
        items.forEach { newItem ->
            // 查找是否已存在相同商品（通过goodsId和unitPrice判断）
            val existingIndex = currentItems.indexOfFirst { 
                it.goodsId == newItem.goodsId && it.unitPrice == newItem.unitPrice 
            }
            
            if (existingIndex >= 0) {
                // 如果商品已存在，累加数量而不是替换
                val existingItem = currentItems[existingIndex]
                val newQuantity = existingItem.quantity + newItem.quantity
                currentItems[existingIndex] = existingItem.copy(quantity = newQuantity)
            } else {
                // 如果商品不存在，添加新记录
                currentItems.add(newItem)
            }
        }
        
        _salesOrderState.value = _salesOrderState.value.copy(items = currentItems)
        
        // 自动保存草稿
        saveDraft()
        
        // 关闭商品选择界面
        hideProductSelection()
        
        // 显示成功提示
        val itemCount = items.size
        GlobalSuccessMessage.showSuccess("成功添加 $itemCount 件商品")
    }
    
    /**
     * 取消商品数量确认
     */
    fun cancelProductQuantity() {
        _showQuantityDialog.value = false
        _selectedGoods.value = null
    }
    
    /**
     * 增加数量
     */
    fun increaseQuantity() {
        val current = _quantityDialogAmount.value
        val goods = _selectedGoods.value
        if (goods != null && current < goods.stockQuantity) {
            _quantityDialogAmount.value = current + 1
        }
    }
    
    /**
     * 减少数量
     */
    fun decreaseQuantity() {
        val current = _quantityDialogAmount.value
        if (current > 1) {
            _quantityDialogAmount.value = current - 1
        }
    }
    
    /**
     * 更新数量
     */
    fun updateQuantity(quantity: Int) {
        _quantityDialogAmount.value = quantity
    }
    
    /**
     * 删除订单项
     */
    fun removeOrderItem(itemId: String) {
        val currentItems = _salesOrderState.value.items.toMutableList()
        currentItems.removeAll { it.id == itemId }
        _salesOrderState.value = _salesOrderState.value.copy(items = currentItems)
        
        // 自动保存草稿
        saveDraft()
    }
    
    /**
     * 更新订单项数量
     */
    fun updateOrderItemQuantity(itemId: String, quantity: Int) {
        val currentItems = _salesOrderState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = currentItems[index]
            currentItems[index] = item.copy(quantity = quantity)
            _salesOrderState.value = _salesOrderState.value.copy(items = currentItems)
            
            // 自动保存草稿
            saveDraft()
        }
    }
    
    /**
     * 显示价格编辑对话框
     */
    fun showPriceEditDialog(orderItem: SalesOrderItem) {
        _editingOrderItem.value = orderItem
        _priceEditAmount.value = orderItem.unitPrice
        _showPriceEditDialog.value = true
    }
    
    /**
     * 隐藏价格编辑对话框
     */
    fun hidePriceEditDialog() {
        _showPriceEditDialog.value = false
        _editingOrderItem.value = null
    }
    
    /**
     * 确认价格编辑
     */
    fun confirmPriceEdit() {
        val orderItem = _editingOrderItem.value ?: return
        val newPrice = _priceEditAmount.value
        
        val currentItems = _salesOrderState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.id == orderItem.id }
        if (index != -1) {
            val item = currentItems[index]
            currentItems[index] = item.copy(unitPrice = newPrice)
            _salesOrderState.value = _salesOrderState.value.copy(items = currentItems)
            
            // 自动保存草稿
            saveDraft()
        }
        
        hidePriceEditDialog()
    }
    
    /**
     * 更新价格编辑金额
     */
    fun updatePriceEditAmount(amount: Double) {
        _priceEditAmount.value = amount
    }
    
    /**
     * 设置支付方式
     */
    fun setPaymentMethod(method: PaymentMethod) {
        _salesOrderState.value = _salesOrderState.value.copy(paymentMethod = method)
        
        // 自动保存草稿
        saveDraft()
    }
    
    /**
     * 设置付款类型
     */
    fun setPaymentType(type: PaymentType) {
        _salesOrderState.value = _salesOrderState.value.copy(
            paymentType = type,
            depositAmount = if (type == PaymentType.FULL_PAYMENT) 0.0 else _salesOrderState.value.depositAmount
        )
        
        // 自动保存草稿
        saveDraft()
    }
    
    /**
     * 设置定金金额
     */
    fun setDepositAmount(amount: Double) {
        _salesOrderState.value = _salesOrderState.value.copy(depositAmount = amount)
        
        // 自动保存草稿
        saveDraft()
    }
    
    /**
     * 设置客户姓名
     */
    fun setCustomerName(name: String) {
        _salesOrderState.value = _salesOrderState.value.copy(customerName = name)
        
        // 自动保存草稿
        saveDraft()
    }
    
    /**
     * 设置客户电话
     */
    fun setCustomerPhone(phone: String) {
        _salesOrderState.value = _salesOrderState.value.copy(customerPhone = phone)
        
        // 自动保存草稿
        saveDraft()
    }
    
    /**
     * 设置客户地址
     */
    fun setCustomerAddress(address: String) {
        _salesOrderState.value = _salesOrderState.value.copy(customerAddress = address)
        
        // 自动保存草稿
        saveDraft()
    }
    
    /**
     * 完成订单
     */
    fun completeOrder(onComplete: (() -> Unit)? = null) {
        val state = _salesOrderState.value
        if (!state.canCompleteOrder) return
        
        viewModelScope.launch {
            try {
                // 持久化订单并扣减库存
                val order = SalesOrder(
                    items = state.items,
                    paymentMethod = state.paymentMethod!!,
                    paymentType = state.paymentType!!,
                    depositAmount = state.depositAmount,
                    customerName = state.customerName,
                    customerPhone = state.customerPhone,
                    customerAddress = state.customerAddress,
                    totalAmount = state.totalAmount
                )
                val updated = ServiceLocator.salesRepository.processSaleAndSave(order)
                val message = if (updated > 0) {
                    "收款成功，订单已生成，已更新 ${updated} 件商品库存"
                } else {
                    "收款成功，订单已生成"
                }
                GlobalSuccessMessage.showSuccess(message)
                resetOrder()
                onComplete?.invoke()
            } catch (e: IllegalArgumentException) {
                GlobalSuccessMessage.showSuccess("库存检查失败：${e.message}")
            } catch (e: Exception) {
                GlobalSuccessMessage.showSuccess("订单处理失败：${e.message}")
            }
        }
    }
    
    /**
     * 添加商品到购物车
     * 如果商品已存在，则更新数量而不是添加重复项
     */
    fun addToCart(item: SalesOrderItem) {
        val currentCartItems = _cartItems.value.toMutableList()
        
        // 查找是否已存在相同商品（通过goodsId和unitPrice判断）
        val existingIndex = currentCartItems.indexOfFirst { 
            it.goodsId == item.goodsId && it.unitPrice == item.unitPrice 
        }
        
        if (existingIndex >= 0) {
            // 如果商品已存在，累加数量而不是替换
            val existingItem = currentCartItems[existingIndex]
            val newQuantity = existingItem.quantity + item.quantity
            currentCartItems[existingIndex] = existingItem.copy(quantity = newQuantity)
        } else {
            // 如果商品不存在，添加新记录
            currentCartItems.add(item)
        }
        
        _cartItems.value = currentCartItems
    }
    
    /**
     * 批量添加商品到购物车
     * 如果商品已存在，则更新数量而不是添加重复项
     */
    fun addToCart(items: List<SalesOrderItem>) {
        if (items.isEmpty()) return
        val currentCartItems = _cartItems.value.toMutableList()
        
        // 处理每个要添加的商品
        items.forEach { newItem ->
            // 查找是否已存在相同商品（通过goodsId和unitPrice判断）
            val existingIndex = currentCartItems.indexOfFirst { 
                it.goodsId == newItem.goodsId && it.unitPrice == newItem.unitPrice 
            }
            
            if (existingIndex >= 0) {
                // 如果商品已存在，累加数量而不是替换
                val existingItem = currentCartItems[existingIndex]
                val newQuantity = existingItem.quantity + newItem.quantity
                currentCartItems[existingIndex] = existingItem.copy(quantity = newQuantity)
            } else {
                // 如果商品不存在，添加新记录
                currentCartItems.add(newItem)
            }
        }
        
        _cartItems.value = currentCartItems
    }
    
    /**
     * 从购物车移除商品
     */
    fun removeFromCart(itemId: String) {
        val currentCartItems = _cartItems.value.toMutableList()
        currentCartItems.removeAll { it.id == itemId }
        _cartItems.value = currentCartItems
    }
    
    /**
     * 清空购物车
     */
    fun clearCart() {
        _cartItems.value = emptyList()
    }
    
    /**
     * 将购物车中的商品确认添加到订单
     */
    fun confirmCartItems() {
        val cartItems = _cartItems.value
        if (cartItems.isNotEmpty()) {
            addConfirmedItems(cartItems)
            clearCart() // 添加到订单后清空购物车
            hideCartDialog() // 关闭购物车对话框
        }
    }
    
    /**
     * 显示购物车对话框
     */
    fun showCartDialog() {
        _showCartDialog.value = true
    }
    
    /**
     * 隐藏购物车对话框
     */
    fun hideCartDialog() {
        _showCartDialog.value = false
    }
    
    /**
     * 获取所有商品（用于商品选择）
     */
    fun getAllGoods(): List<Goods> {
        return goodsFlow.value.filter { !it.isDelisted }
    }
    

    
    /**
     * 预填充商品数据（从库存页面快捷销售）
     */
    fun prefillGoodsData(goods: Goods, quantity: Int) {
        // 创建销售订单项
        val orderItem = SalesOrderItem(
            goodsId = goods.id,
            goodsName = goods.name,
            specifications = goods.specifications,
            unitPrice = goods.retailPrice,
            quantity = quantity,
            category = goods.category
        )
        
        // 直接添加到订单中
        val currentItems = _salesOrderState.value.items.toMutableList()
        currentItems.add(orderItem)
        _salesOrderState.value = _salesOrderState.value.copy(items = currentItems)
        
        // 显示成功提示
        GlobalSuccessMessage.showSuccess("商品已添加到销售单")
    }
} 
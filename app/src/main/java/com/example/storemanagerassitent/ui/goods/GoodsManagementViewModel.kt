package com.example.storemanagerassitent.ui.goods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storemanagerassitent.data.Goods
import com.example.storemanagerassitent.data.GoodsCategory
import com.example.storemanagerassitent.data.OutboundReason
import com.example.storemanagerassitent.data.SampleData
import com.example.storemanagerassitent.data.db.ServiceLocator
import com.example.storemanagerassitent.data.SortOption
import com.example.storemanagerassitent.ui.components.GlobalSuccessMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 商品管理页面的ViewModel
 * 管理搜索、筛选、排序等状态和逻辑
 */
class GoodsManagementViewModel : ViewModel() {
    
    // 原始商品数据（来自 Room 仓库）
    private val _allGoods = MutableStateFlow<List<Goods>>(emptyList())
    val allGoods: StateFlow<List<Goods>> = _allGoods.asStateFlow()
    
    // 分类数据（订阅数据库）
    private val _categories = MutableStateFlow<List<GoodsCategory>>(emptyList())
    val categories: StateFlow<List<GoodsCategory>> = _categories.asStateFlow()
    
    // 搜索文本
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    
    // 选中的分类
    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    
    // 排序选项
    private val _selectedSortOption = MutableStateFlow(SortOption.STOCK_ASC)
    val selectedSortOption: StateFlow<SortOption> = _selectedSortOption.asStateFlow()
    
    // 搜索栏是否展开
    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()
    
    // 排序菜单是否显示
    private val _showSortMenu = MutableStateFlow(false)
    val showSortMenu: StateFlow<Boolean> = _showSortMenu.asStateFlow()
    
    // 选中的商品 (详情面板)
    private val _selectedGoods = MutableStateFlow<Goods?>(null)
    val selectedGoods: StateFlow<Goods?> = _selectedGoods.asStateFlow()
    
    // 详情面板是否显示
    private val _showGoodsDetail = MutableStateFlow(false)
    val showGoodsDetail: StateFlow<Boolean> = _showGoodsDetail.asStateFlow()
    
    // 出库原因选择对话框是否显示
    private val _showOutboundReasonDialog = MutableStateFlow(false)
    val showOutboundReasonDialog: StateFlow<Boolean> = _showOutboundReasonDialog.asStateFlow()
    
    // 库存调整对话框是否显示
    private val _showStockAdjustmentDialog = MutableStateFlow(false)
    val showStockAdjustmentDialog: StateFlow<Boolean> = _showStockAdjustmentDialog.asStateFlow()
    
    // 最终确认对话框是否显示
    private val _showFinalConfirmDialog = MutableStateFlow(false)
    val showFinalConfirmDialog: StateFlow<Boolean> = _showFinalConfirmDialog.asStateFlow()
    
    // 出库数量
    private val _outboundQuantity = MutableStateFlow(1)
    val outboundQuantity: StateFlow<Int> = _outboundQuantity.asStateFlow()
    
    // 销售数量输入对话框是否显示
    private val _showSaleQuantityDialog = MutableStateFlow(false)
    val showSaleQuantityDialog: StateFlow<Boolean> = _showSaleQuantityDialog.asStateFlow()
    
    // 销售数量
    private val _saleQuantity = MutableStateFlow(1)
    val saleQuantity: StateFlow<Int> = _saleQuantity.asStateFlow()
    
    // 入库数量输入对话框是否显示
    private val _showInboundQuantityDialog = MutableStateFlow(false)
    val showInboundQuantityDialog: StateFlow<Boolean> = _showInboundQuantityDialog.asStateFlow()
    
    // 入库数量
    private val _inboundQuantity = MutableStateFlow(1)
    val inboundQuantity: StateFlow<Int> = _inboundQuantity.asStateFlow()
    
    // 库存编辑确认对话框是否显示
    private val _showStockEditConfirmDialog = MutableStateFlow(false)
    val showStockEditConfirmDialog: StateFlow<Boolean> = _showStockEditConfirmDialog.asStateFlow()
    
    // 库存编辑对话框是否显示
    private val _showStockEditDialog = MutableStateFlow(false)
    val showStockEditDialog: StateFlow<Boolean> = _showStockEditDialog.asStateFlow()
    
    // 库存编辑数量
    private val _stockEditQuantity = MutableStateFlow(1)
    val stockEditQuantity: StateFlow<Int> = _stockEditQuantity.asStateFlow()
    
    // 批量下架模式相关状态
    private val _isBatchDelistMode = MutableStateFlow(false)
    val isBatchDelistMode: StateFlow<Boolean> = _isBatchDelistMode.asStateFlow()
    
    // 选中的商品ID列表
    private val _selectedGoodsIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedGoodsIds: StateFlow<Set<String>> = _selectedGoodsIds.asStateFlow()
    
    // 问题商品ID列表（库存不为0无法下架的商品）
    private val _problemGoodsIds = MutableStateFlow<Set<String>>(emptySet())
    val problemGoodsIds: StateFlow<Set<String>> = _problemGoodsIds.asStateFlow()
    
    // 批量下架确认对话框
    private val _showBatchDelistConfirmDialog = MutableStateFlow(false)
    val showBatchDelistConfirmDialog: StateFlow<Boolean> = _showBatchDelistConfirmDialog.asStateFlow()
    
    // 批量下架取消确认对话框
    private val _showBatchCancelConfirmDialog = MutableStateFlow(false)
    val showBatchCancelConfirmDialog: StateFlow<Boolean> = _showBatchCancelConfirmDialog.asStateFlow()
    
    // 更多操作菜单是否显示
    private val _showMoreOptionsMenu = MutableStateFlow(false)
    val showMoreOptionsMenu: StateFlow<Boolean> = _showMoreOptionsMenu.asStateFlow()
    
    // 分类选择对话框是否显示
    private val _showCategorySelector = MutableStateFlow(false)
    val showCategorySelector: StateFlow<Boolean> = _showCategorySelector.asStateFlow()
    
    // 仓库总价值对话框
    private val _showWarehouseValueDialog = MutableStateFlow(false)
    val showWarehouseValueDialog: StateFlow<Boolean> = _showWarehouseValueDialog.asStateFlow()
    
    // 商品名编辑相关状态
    private val _showGoodsNameEditDialog = MutableStateFlow(false)
    val showGoodsNameEditDialog: StateFlow<Boolean> = _showGoodsNameEditDialog.asStateFlow()
    
    private val _editingGoodsName = MutableStateFlow("")
    val editingGoodsName: StateFlow<String> = _editingGoodsName.asStateFlow()
    
    // 进价编辑相关状态
    private val _showPurchasePriceEditDialog = MutableStateFlow(false)
    val showPurchasePriceEditDialog: StateFlow<Boolean> = _showPurchasePriceEditDialog.asStateFlow()
    
    private val _editingPurchasePrice = MutableStateFlow("")
    val editingPurchasePrice: StateFlow<String> = _editingPurchasePrice.asStateFlow()

    // 商品码编辑相关状态
    private val _showBarcodeEditDialog = MutableStateFlow(false)
    val showBarcodeEditDialog: StateFlow<Boolean> = _showBarcodeEditDialog.asStateFlow()

    private val _editingBarcode = MutableStateFlow("")
    val editingBarcode: StateFlow<String> = _editingBarcode.asStateFlow()
    
    // 过滤后的商品列表（排除已下架商品）
    val filteredGoods: StateFlow<List<Goods>> = combine(
        _allGoods,
        _searchText,
        _selectedCategory,
        _selectedSortOption
    ) { goods, search, category, sort ->
        val availableGoods = goods.filter { !it.isDelisted }
        filterAndSortGoods(availableGoods, search, category, sort)
    }.let { flow ->
        val stateFlow = MutableStateFlow<List<Goods>>(emptyList())
        viewModelScope.launch {
            flow.collect { stateFlow.value = it }
        }
        stateFlow.asStateFlow()
    }
    
    init {
        // 订阅数据库的商品列表
        viewModelScope.launch {
            ServiceLocator.goodsRepository.observeGoods().collect { goods ->
                _allGoods.value = goods
            }
        }
        // 订阅数据库的分类列表
        viewModelScope.launch {
            ServiceLocator.categoryRepository.observeGoodsCategories().collect { list ->
                _categories.value = list
            }
        }
    }

    /**
     * 更新搜索文本
     */
    fun updateSearchText(text: String) {
        _searchText.value = text
    }
    
    /**
     * 切换搜索栏展开状态
     */
    fun toggleSearchExpanded() {
        _isSearchExpanded.value = !_isSearchExpanded.value
        // 搜索框收起时保持搜索文本，不清空搜索状态
    }
    
    /**
     * 选择分类
     */
    fun selectCategory(categoryId: String) {
        _selectedCategory.value = categoryId
    }
    
    /**
     * 选择排序选项
     */
    fun selectSortOption(option: SortOption) {
        _selectedSortOption.value = option
        _showSortMenu.value = false
    }
    
    /**
     * 切换排序菜单显示状态
     */
    fun toggleSortMenu() {
        _showSortMenu.value = !_showSortMenu.value
    }
    
    /**
     * 过滤和排序商品
     */
    private fun filterAndSortGoods(
        goods: List<Goods>,
        searchText: String,
        selectedCategory: String,
        sortOption: SortOption
    ): List<Goods> {
        // 分类筛选
        val filteredByCategory = if (selectedCategory == "all") {
            goods
        } else {
            goods.filter { it.category == selectedCategory }
        }
        
        // 搜索筛选
        val filteredBySearch = if (searchText.isEmpty()) {
            filteredByCategory
        } else {
            filteredByCategory.filter { 
                it.name.contains(searchText, ignoreCase = true) || 
                it.specifications.contains(searchText, ignoreCase = true)
            }
        }
        
        // 排序
        return when (sortOption) {
            SortOption.STOCK_ASC -> filteredBySearch.sortedBy { it.stockQuantity }
            SortOption.NAME_A_Z -> filteredBySearch.sortedBy { it.name }
            SortOption.LAST_UPDATED -> filteredBySearch.sortedByDescending { it.lastUpdated }
        }
    }
    
    /**
     * 按分类分组商品
     */
    fun getGroupedGoods(goods: List<Goods>): Map<String, List<Goods>> {
        val isAll = _selectedCategory.value == "all"
        val allCategories = _categories.value.filter { it.id != "all" }
        val filteredCategories = if (isAll) {
            allCategories
        } else {
            allCategories.filter { it.id == _selectedCategory.value }
        }

        val goodsByCategoryId = goods.groupBy { it.category }
        val ordered = linkedMapOf<String, List<Goods>>()

        filteredCategories.forEach { category ->
            val list = goodsByCategoryId[category.id] ?: emptyList()
            ordered[category.name] = list
        }

        if (isAll) {
            // 将未识别分类归入“未知分类”（仅在全部视图中显示）
            val unknown = goods.filter { g -> allCategories.none { it.id == g.category } }
            if (unknown.isNotEmpty()) {
                ordered["未知分类"] = (ordered["未知分类"] ?: emptyList()) + unknown
            }
        }
        return ordered
    }
    
    /**
     * 获取低库存商品数量
     */
    fun getLowStockCount(goods: List<Goods>): Int {
        return goods.count { it.isLowStock }
    }
    
    /**
     * 选择商品 (显示详情面板)
     */
    fun selectGoods(goods: Goods) {
        _selectedGoods.value = goods
        _showGoodsDetail.value = true
    }
    
    /**
     * 关闭详情面板
     */
    fun closeGoodsDetail() {
        _showGoodsDetail.value = false
        _selectedGoods.value = null
    }
    
    /**
     * 显示出库原因选择对话框（已废弃，直接进入销售界面）
     */
    fun showOutboundReasonDialog() {
        // 直接显示销售数量输入对话框，跳过出库原因选择
        _saleQuantity.value = 1 // 重置数量为1
        _showSaleQuantityDialog.value = true
    }
    
    /**
     * 隐藏出库原因选择对话框
     */
    fun hideOutboundReasonDialog() {
        _showOutboundReasonDialog.value = false
    }
    
    /**
     * 选择出库原因
     */
    fun selectOutboundReason(reason: OutboundReason) {
        _showOutboundReasonDialog.value = false
        when (reason) {
            OutboundReason.SOLD -> {
                // 显示销售数量输入对话框
                _saleQuantity.value = 1 // 重置数量为1
                _showSaleQuantityDialog.value = true
            }
        }
    }
    
    /**
     * 显示库存调整对话框
     */
    fun showStockAdjustmentDialog() {
        _showStockAdjustmentDialog.value = true
        _outboundQuantity.value = 1
    }
    
    /**
     * 隐藏库存调整对话框
     */
    fun hideStockAdjustmentDialog() {
        _showStockAdjustmentDialog.value = false
    }
    
    /**
     * 更新出库数量
     */
    fun updateOutboundQuantity(quantity: Int) {
        if (quantity > 0) {
            _outboundQuantity.value = quantity
        }
    }
    
    /**
     * 增加出库数量
     */
    fun increaseOutboundQuantity() {
        _selectedGoods.value?.let { goods ->
            if (_outboundQuantity.value < goods.stockQuantity) {
                _outboundQuantity.value += 1
            }
        }
    }
    
    /**
     * 减少出库数量
     */
    fun decreaseOutboundQuantity() {
        if (_outboundQuantity.value > 1) {
            _outboundQuantity.value -= 1
        }
    }
    
    /**
     * 确认出库 (显示最终确认对话框)
     */
    fun confirmOutbound() {
        _showStockAdjustmentDialog.value = false
        _showFinalConfirmDialog.value = true
    }
    
    /**
     * 隐藏最终确认对话框
     */
    fun hideFinalConfirmDialog() {
        _showFinalConfirmDialog.value = false
    }
    
    /**
     * 执行出库操作
     */
    fun executeOutbound() {
        _selectedGoods.value?.let { goods ->
            viewModelScope.launch {
                ServiceLocator.goodsRepository.adjustStock(goods.id, -_outboundQuantity.value)
                _showFinalConfirmDialog.value = false
                _outboundQuantity.value = 1
                GlobalSuccessMessage.showSuccess("库存已更正")
            }
        }
    }
    
    // === 批量下架相关方法 ===
    
    /**
     * 切换更多操作菜单显示状态
     */
    fun toggleMoreOptionsMenu() {
        _showMoreOptionsMenu.value = !_showMoreOptionsMenu.value
    }
    
    /**
     * 进入批量下架模式
     */
    fun enterBatchDelistMode() {
        _isBatchDelistMode.value = true
        _selectedGoodsIds.value = emptySet()
        _problemGoodsIds.value = emptySet()
        _showMoreOptionsMenu.value = false
    }
    
    /**
     * 退出批量下架模式
     */
    fun exitBatchDelistMode() {
        _isBatchDelistMode.value = false
        _selectedGoodsIds.value = emptySet()
        _problemGoodsIds.value = emptySet()
    }
    
    /**
     * 切换商品选中状态
     */
    fun toggleGoodsSelection(goodsId: String) {
        val currentSelected = _selectedGoodsIds.value.toMutableSet()
        if (currentSelected.contains(goodsId)) {
            currentSelected.remove(goodsId)
        } else {
            currentSelected.add(goodsId)
        }
        _selectedGoodsIds.value = currentSelected
        
        // 清除该商品的问题状态
        if (_problemGoodsIds.value.contains(goodsId)) {
            _problemGoodsIds.value = _problemGoodsIds.value - goodsId
        }
    }
    
    /**
     * 显示批量下架取消确认对话框
     */
    fun showBatchCancelConfirmDialog() {
        _showBatchCancelConfirmDialog.value = true
    }
    
    /**
     * 隐藏批量下架取消确认对话框
     */
    fun hideBatchCancelConfirmDialog() {
        _showBatchCancelConfirmDialog.value = false
    }
    
    /**
     * 确认取消批量下架
     */
    fun confirmCancelBatchDelist() {
        _showBatchCancelConfirmDialog.value = false
        exitBatchDelistMode()
    }
    
    /**
     * 显示批量下架确认对话框
     */
    fun showBatchDelistConfirmDialog() {
        if (_selectedGoodsIds.value.isNotEmpty()) {
            _showBatchDelistConfirmDialog.value = true
        }
    }
    
    /**
     * 隐藏批量下架确认对话框
     */
    fun hideBatchDelistConfirmDialog() {
        _showBatchDelistConfirmDialog.value = false
    }
    
    /**
     * 执行批量下架
     */
    fun executeBatchDelist() {
        val selectedIds = _selectedGoodsIds.value
        val allGoods = _allGoods.value
        val selectedGoods = allGoods.filter { it.id in selectedIds }

        // 区分可下架与不可下架（库存不为 0）
        val problemGoods = selectedGoods.filter { !it.canBeDelisted }
        val okGoods = selectedGoods.filter { it.canBeDelisted }

        viewModelScope.launch {
            // 先删除可下架的那部分（若有）
            if (okGoods.isNotEmpty()) {
                ServiceLocator.goodsRepository.deleteGoodsByIds(okGoods.map { it.id })
            }

            _showBatchDelistConfirmDialog.value = false

            if (problemGoods.isNotEmpty()) {
                // 标记问题商品并保留在批量模式中，方便用户处理
                val problemIds = problemGoods.map { it.id }.toSet()
                _problemGoodsIds.value = problemIds
                _selectedGoodsIds.value = problemIds

                val okCount = okGoods.size
                val problemCount = problemGoods.size
                val message = if (okCount > 0) {
                    "已下架 ${okCount} 件；${problemCount} 件库存不为0，未下架，已标记"
                } else {
                    "${problemCount} 件库存不为0，无法下架，已标记"
                }
                GlobalSuccessMessage.showSuccess(message)
            } else {
                // 全部成功，退出批量模式
                exitBatchDelistMode()
                GlobalSuccessMessage.showSuccess("成功下架 ${okGoods.size} 件商品")
            }
        }
    }
    
    /**
     * 获取选中商品数量
     */
    fun getSelectedGoodsCount(): Int {
        return _selectedGoodsIds.value.size
    }
    
    /**
     * 检查商品是否被选中
     */
    fun isGoodsSelected(goodsId: String): Boolean {
        return _selectedGoodsIds.value.contains(goodsId)
    }
    
    /**
     * 检查商品是否是问题商品
     */
    fun isGoodsProblem(goodsId: String): Boolean {
        return _problemGoodsIds.value.contains(goodsId)
    }
    
    // === 分类修改相关方法 ===
    
    /**
     * 显示分类选择对话框
     */
    fun showCategorySelector() {
        _showCategorySelector.value = true
    }
    
    /**
     * 隐藏分类选择对话框
     */
    fun hideCategorySelector() {
        _showCategorySelector.value = false
    }
    
    /**
     * 更新商品分类
     */
    fun updateGoodsCategory(goodsId: String, newCategoryId: String) {
        // 更新选中商品的分类
        _selectedGoods.value?.let { goods ->
            if (goods.id == goodsId) {
                val updatedGoods = goods.copy(category = newCategoryId)
                _selectedGoods.value = updatedGoods
            }
        }
        
        // 更新全量商品列表中的数据
        val updatedGoodsList = _allGoods.value.map { goods ->
            if (goods.id == goodsId) {
                goods.copy(category = newCategoryId)
            } else {
                goods
            }
        }
        _allGoods.value = updatedGoodsList
        
        // 关闭分类选择对话框
        _showCategorySelector.value = false
        
        // 显示成功提示
        GlobalSuccessMessage.showSuccess("分类已更新")
    }

    // === 总价值对话框方法 ===
    fun showWarehouseValueDialog() {
        _showWarehouseValueDialog.value = true
        _showMoreOptionsMenu.value = false
    }

    fun hideWarehouseValueDialog() {
        _showWarehouseValueDialog.value = false
    }
    
    /**
     * 显示销售数量输入对话框
     */
    fun showSaleQuantityDialog() {
        _saleQuantity.value = 1
        _showSaleQuantityDialog.value = true
    }
    
    /**
     * 隐藏销售数量输入对话框
     */
    fun hideSaleQuantityDialog() {
        _showSaleQuantityDialog.value = false
    }
    
    /**
     * 更新销售数量
     */
    fun updateSaleQuantity(quantity: Int) {
        if (quantity > 0) {
            _saleQuantity.value = quantity
        }
    }
    
    /**
     * 增加销售数量
     */
    fun increaseSaleQuantity() {
        val goods = _selectedGoods.value
        if (goods != null && _saleQuantity.value < goods.stockQuantity) {
            _saleQuantity.value++
        }
    }
    
    /**
     * 减少销售数量
     */
    fun decreaseSaleQuantity() {
        if (_saleQuantity.value > 1) {
            _saleQuantity.value--
        }
    }
    
    /**
     * 确认销售并跳转到销售订单页面
     */
    fun confirmSaleAndNavigate(onNavigateToSales: (Goods, Int) -> Unit) {
        val goods = _selectedGoods.value
        val quantity = _saleQuantity.value
        
        if (goods != null && quantity > 0 && quantity <= goods.stockQuantity) {
            // 隐藏所有对话框
            _showSaleQuantityDialog.value = false
            _showGoodsDetail.value = false
            
            // 跳转到销售订单页面并传递商品数据
            onNavigateToSales(goods, quantity)
        }
    }
    
    /**
     * 显示入库数量输入对话框
     */
    fun showInboundQuantityDialog() {
        _inboundQuantity.value = 1
        _showInboundQuantityDialog.value = true
    }
    
    /**
     * 隐藏入库数量输入对话框
     */
    fun hideInboundQuantityDialog() {
        _showInboundQuantityDialog.value = false
    }
    
    /**
     * 更新入库数量
     */
    fun updateInboundQuantity(quantity: Int) {
        if (quantity > 0) {
            _inboundQuantity.value = quantity
        }
    }
    
    /**
     * 增加入库数量
     */
    fun increaseInboundQuantity() {
        _inboundQuantity.value++
    }
    
    /**
     * 减少入库数量
     */
    fun decreaseInboundQuantity() {
        if (_inboundQuantity.value > 1) {
            _inboundQuantity.value--
        }
    }
    
    /**
     * 确认入库
     */
    fun confirmInbound() {
        val goods = _selectedGoods.value
        val quantity = _inboundQuantity.value
        if (goods != null && quantity > 0) {
            viewModelScope.launch {
                ServiceLocator.goodsRepository.adjustStock(goods.id, quantity)
                _showInboundQuantityDialog.value = false
                GlobalSuccessMessage.showSuccess("入库成功！")
            }
        }
    }
    
    /**
     * 显示库存编辑确认对话框
     */
    fun showStockEditConfirmDialog() {
        val goods = _selectedGoods.value
        if (goods != null) {
            _stockEditQuantity.value = goods.stockQuantity
            _showStockEditConfirmDialog.value = true
        }
    }
    
    /**
     * 隐藏库存编辑确认对话框
     */
    fun hideStockEditConfirmDialog() {
        _showStockEditConfirmDialog.value = false
    }
    
    /**
     * 显示库存编辑对话框
     */
    fun showStockEditDialog() {
        val goods = _selectedGoods.value
        if (goods != null) {
            _stockEditQuantity.value = goods.stockQuantity
            _showStockEditDialog.value = true
        }
    }
    
    /**
     * 隐藏库存编辑对话框
     */
    fun hideStockEditDialog() {
        _showStockEditDialog.value = false
    }
    
    /**
     * 更新库存编辑数量
     */
    fun updateStockEditQuantity(quantity: Int) {
        if (quantity >= 0) {
            _stockEditQuantity.value = quantity
        }
    }
    
    /**
     * 增加库存编辑数量
     */
    fun increaseStockEditQuantity() {
        _stockEditQuantity.value++
    }
    
    /**
     * 减少库存编辑数量
     */
    fun decreaseStockEditQuantity() {
        if (_stockEditQuantity.value > 0) {
            _stockEditQuantity.value--
        }
    }
    
    /**
     * 确认库存编辑
     */
    fun confirmStockEdit() {
        val goods = _selectedGoods.value
        val quantity = _stockEditQuantity.value
        if (goods != null && quantity >= 0) {
            val delta = quantity - goods.stockQuantity
            // 立即更新本地状态，确保界面及时刷新
            _selectedGoods.value = goods.copy(stockQuantity = quantity)
            _allGoods.value = _allGoods.value.map { g ->
                if (g.id == goods.id) g.copy(stockQuantity = quantity) else g
            }
            viewModelScope.launch {
                ServiceLocator.goodsRepository.adjustStock(goods.id, delta)
                _showStockEditDialog.value = false
                _showStockEditConfirmDialog.value = false
                GlobalSuccessMessage.showSuccess("库存已更新！")
            }
        }
    }
    
    // === 商品名编辑相关方法 ===
    
    /**
     * 显示商品名编辑对话框
     */
    fun showGoodsNameEditDialog() {
        val goods = _selectedGoods.value
        if (goods != null) {
            _editingGoodsName.value = goods.name
            _showGoodsNameEditDialog.value = true
        }
    }
    
    /**
     * 隐藏商品名编辑对话框
     */
    fun hideGoodsNameEditDialog() {
        _showGoodsNameEditDialog.value = false
        _editingGoodsName.value = ""
    }
    
    /**
     * 更新编辑中的商品名
     */
    fun updateEditingGoodsName(name: String) {
        _editingGoodsName.value = name
    }
    
    /**
     * 确认商品名编辑
     */
    fun confirmGoodsNameEdit() {
        val goods = _selectedGoods.value
        val newName = _editingGoodsName.value.trim()
        if (goods != null && newName.isNotBlank() && newName != goods.name) {
            // 立即更新本地状态
            _selectedGoods.value = goods.copy(name = newName)
            _allGoods.value = _allGoods.value.map { g -> if (g.id == goods.id) g.copy(name = newName) else g }
            viewModelScope.launch {
                ServiceLocator.goodsRepository.updateGoodsName(goods.id, newName)
                _showGoodsNameEditDialog.value = false
                _editingGoodsName.value = ""
                GlobalSuccessMessage.showSuccess("商品名已更新！")
            }
        } else {
            _showGoodsNameEditDialog.value = false
            _editingGoodsName.value = ""
        }
    }
    
    // === 进价编辑相关方法 ===
    
    /**
     * 显示进价编辑对话框
     */
    fun showPurchasePriceEditDialog() {
        val goods = _selectedGoods.value
        if (goods != null) {
            _editingPurchasePrice.value = goods.purchasePrice.toString()
            _showPurchasePriceEditDialog.value = true
        }
    }
    
    /**
     * 隐藏进价编辑对话框
     */
    fun hidePurchasePriceEditDialog() {
        _showPurchasePriceEditDialog.value = false
        _editingPurchasePrice.value = ""
    }
    
    /**
     * 更新编辑中的进价
     */
    fun updateEditingPurchasePrice(price: String) {
        _editingPurchasePrice.value = price
    }
    
    /**
     * 确认进价编辑
     */
    fun confirmPurchasePriceEdit() {
        val goods = _selectedGoods.value
        val newPriceStr = _editingPurchasePrice.value.trim()
        val newPrice = newPriceStr.toDoubleOrNull()
        
        if (goods != null && newPrice != null && newPrice > 0 && newPrice != goods.purchasePrice) {
            // 立即更新本地状态
            _selectedGoods.value = goods.copy(purchasePrice = newPrice)
            _allGoods.value = _allGoods.value.map { g -> if (g.id == goods.id) g.copy(purchasePrice = newPrice) else g }
            viewModelScope.launch {
                ServiceLocator.goodsRepository.updateGoodsPurchasePrice(goods.id, newPrice)
                _showPurchasePriceEditDialog.value = false
                _editingPurchasePrice.value = ""
                GlobalSuccessMessage.showSuccess("进价已更新！")
            }
        } else {
            _showPurchasePriceEditDialog.value = false
            _editingPurchasePrice.value = ""
        }
    }

    // === 商品码编辑相关方法 ===
    fun showBarcodeEditDialog() {
        val goods = _selectedGoods.value
        if (goods != null) {
            _editingBarcode.value = goods.barcode ?: ""
            _showBarcodeEditDialog.value = true
        }
    }

    fun hideBarcodeEditDialog() {
        _showBarcodeEditDialog.value = false
        _editingBarcode.value = ""
    }

    fun updateEditingBarcode(barcode: String) {
        _editingBarcode.value = barcode
    }

    fun confirmBarcodeEdit() {
        val goods = _selectedGoods.value
        val newCode = _editingBarcode.value.trim().ifBlank { null }
        if (goods != null) {
            // 立即更新本地状态
            _selectedGoods.value = goods.copy(barcode = newCode)
            _allGoods.value = _allGoods.value.map { g -> if (g.id == goods.id) g.copy(barcode = newCode) else g }
            viewModelScope.launch {
                ServiceLocator.goodsRepository.updateGoodsBarcode(goods.id, newCode)
                _showBarcodeEditDialog.value = false
                _editingBarcode.value = ""
                GlobalSuccessMessage.showSuccess(if (newCode != null) "商品码已更新！" else "已移除商品码")
            }
        } else {
            _showBarcodeEditDialog.value = false
            _editingBarcode.value = ""
        }
    }
}
package com.example.storemanagerassitent.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storemanagerassitent.data.SalesInsightData
import com.example.storemanagerassitent.data.TimePeriod
import com.example.storemanagerassitent.data.CategoryOption
import com.example.storemanagerassitent.data.ProductSalesData
import com.example.storemanagerassitent.data.db.ServiceLocator
import com.example.storemanagerassitent.data.db.toDomainOrder
import com.example.storemanagerassitent.data.Goods
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页ViewModel
 */
class HomeViewModel : ViewModel() {
    
    // 当前选中的时间维度
    private val _selectedTimePeriod = MutableStateFlow(TimePeriod.WEEK)
    val selectedTimePeriod: StateFlow<TimePeriod> = _selectedTimePeriod.asStateFlow()
    
    // 当前选中的分类
    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    
    // 销售洞察数据
    private val _salesInsightData = MutableStateFlow(
        SalesInsightData(
            period = TimePeriod.WEEK,
            selectedCategory = CategoryOption("all", "全部商品", true),
            productSales = emptyList()
        )
    )
    val salesInsightData: StateFlow<SalesInsightData> = _salesInsightData.asStateFlow()
    
    // 是否正在加载数据
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 分类下拉菜单是否展开
    private val _showCategoryDropdown = MutableStateFlow(false)
    val showCategoryDropdown: StateFlow<Boolean> = _showCategoryDropdown.asStateFlow()
    
    /**
     * 快速操作数据（静态）
     */
    val quickActions = com.example.storemanagerassitent.data.HomeData.quickActions
    
    /**
     * 分类选项列表
     */
    private val _categoryOptions = MutableStateFlow<List<CategoryOption>>(emptyList())
    val categoryOptions: StateFlow<List<CategoryOption>> = _categoryOptions.asStateFlow()

    // 低库存（售罄）提醒
    private val _showLowStockDialog = MutableStateFlow(false)
    val showLowStockDialog: StateFlow<Boolean> = _showLowStockDialog.asStateFlow()
    private val _lowStockGoods = MutableStateFlow<List<Goods>>(emptyList())
    val lowStockGoods: StateFlow<List<Goods>> = _lowStockGoods.asStateFlow()
    private var hasShownLowStockAlert: Boolean = false
    
    /**
     * 切换时间维度
     */
    fun selectTimePeriod(period: TimePeriod) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedTimePeriod.value = period
            _salesInsightData.value = computeSalesInsight(period, _selectedCategory.value)
            _isLoading.value = false
        }
    }
    
    /**
     * 切换分类筛选
     */
    fun selectCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedCategory.value = categoryId
            _showCategoryDropdown.value = false
            _salesInsightData.value = computeSalesInsight(_selectedTimePeriod.value, categoryId)
            _isLoading.value = false
        }
    }
    
    /**
     * 切换分类下拉菜单显示状态
     */
    fun toggleCategoryDropdown() {
        _showCategoryDropdown.value = !_showCategoryDropdown.value
    }
    
    /**
     * 隐藏分类下拉菜单
     */
    fun hideCategoryDropdown() {
        _showCategoryDropdown.value = false
    }
    
    /**
     * 处理快速操作点击
     */
    fun onQuickActionClick(actionId: String) {
        // TODO: 根据actionId导航到对应页面
        when (actionId) {
            "sales" -> {
                // 导航到销售开单页面
            }
            "purchase" -> {
                // 导航到进货开单页面
            }
        }
    }
    
    /**
     * 处理次要操作点击（查看记录）
     */
    fun onSecondaryActionClick(actionId: String) {
        // TODO: 根据actionId导航到对应记录页面
        when (actionId) {
            "sales" -> {
                // 导航到销售记录页面
            }
            "purchase" -> {
                // 导航到进货记录页面
            }
        }
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            _salesInsightData.value = computeSalesInsight(_selectedTimePeriod.value, _selectedCategory.value)
            _isLoading.value = false
        }
    }

    fun dismissLowStockDialog() { _showLowStockDialog.value = false }

    fun triggerLowStockDialog() {
        if (_lowStockGoods.value.isNotEmpty()) {
            _showLowStockDialog.value = true
        }
    }

    init {
        // 订阅分类列表，构造成首页下拉可选项
        viewModelScope.launch {
            ServiceLocator.categoryRepository.observeGoodsCategories().collectLatest { list ->
                val options = list.map { 
                    if (it.id == "all") CategoryOption("all", "全部商品", true)
                    else CategoryOption(it.id, it.name)
                }
                _categoryOptions.value = options
                // 当首次进入或分类变化时，刷新洞察
                _salesInsightData.value = computeSalesInsight(_selectedTimePeriod.value, _selectedCategory.value)
            }
        }
        // 订阅销售订单数量变化，触发洞察刷新
        viewModelScope.launch {
            ServiceLocator.database.salesOrderDao().observeCount().collectLatest {
                _salesInsightData.value = computeSalesInsight(_selectedTimePeriod.value, _selectedCategory.value)
            }
        }
        // 订阅商品，计算售罄清单并在首次进入首页时提醒
        viewModelScope.launch {
            ServiceLocator.goodsRepository.observeGoods().collectLatest { goods ->
                val zeroStock = goods.filter { it.stockQuantity == 0 }
                _lowStockGoods.value = zeroStock
                if (!hasShownLowStockAlert && zeroStock.isNotEmpty()) {
                    _showLowStockDialog.value = true
                    hasShownLowStockAlert = true
                }
            }
        }
    }

    private suspend fun loadOrdersByPeriod(period: TimePeriod): List<com.example.storemanagerassitent.data.SalesOrder> {
        val (start, end) = when (period) {
            TimePeriod.WEEK -> getWeekRange()
            TimePeriod.MONTH -> getMonthRange()
            TimePeriod.YEAR -> getYearRange()
        }
        val pairs = ServiceLocator.salesRepository.getOrdersByRange(start, end)
        return pairs.mapNotNull { (entity, items) -> toDomainOrder(entity, items) }
    }

    private fun getWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        val end = cal.timeInMillis
        return start to end
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }

    private fun getYearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        val end = cal.timeInMillis
        return start to end
    }

    private suspend fun computeSalesInsight(period: TimePeriod, categoryId: String): SalesInsightData {
        val orders = loadOrdersByPeriod(period)
        val allCategories = _categoryOptions.value

        val items = orders.flatMap { it.items }
            .filter { categoryId == "all" || it.category == categoryId }

        val grouped = items.groupBy { it.goodsId }
        val totals = grouped.map { (goodsId, list) ->
            val totalQty = list.sumOf { it.quantity }
            val first = list.first()
            val productName = "${first.goodsName} ${first.specifications}".trim()
            val categoryName = allCategories.find { it.id == first.category }?.name ?: ""
            ProductSalesData(goodsId, productName, categoryName, totalQty)
        }.sortedByDescending { it.salesCount }
            .take(10)
            .mapIndexed { index, data -> data.copy(rank = index + 1) }

        val selected = allCategories.find { it.id == categoryId } ?: CategoryOption("all", "全部商品", true)
        return SalesInsightData(period = period, selectedCategory = selected, productSales = totals)
    }
}
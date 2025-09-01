package com.example.storemanagerassitent.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.storemanagerassitent.data.DateFilterState
import com.example.storemanagerassitent.data.DateFilterType
import com.example.storemanagerassitent.data.PurchaseOrder
import com.example.storemanagerassitent.data.PurchaseRecordData
import com.example.storemanagerassitent.data.PurchaseRecordSummary
import java.util.Date
import com.example.storemanagerassitent.data.db.ServiceLocator
import com.example.storemanagerassitent.data.db.toDomainOrder

/**
 * 进货记录ViewModel
 */
class PurchaseRecordViewModel : ViewModel() {
    
    // 日期筛选状态
    private val _dateFilterState = MutableStateFlow(DateFilterState())
    val dateFilterState: StateFlow<DateFilterState> = _dateFilterState.asStateFlow()
    
    // 进货记录列表
    private val _purchaseRecords = MutableStateFlow<List<PurchaseRecordSummary>>(emptyList())
    val purchaseRecords: StateFlow<List<PurchaseRecordSummary>> = _purchaseRecords.asStateFlow()
    
    // 是否显示日历选择器
    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker: StateFlow<Boolean> = _showDatePicker.asStateFlow()
    
    // 是否显示日历悬浮窗
    private val _showCalendar = MutableStateFlow(false)
    val showCalendar: StateFlow<Boolean> = _showCalendar.asStateFlow()
    
    // 是否显示日期范围选择器
    private val _showDateRangeCalendar = MutableStateFlow(false)
    val showDateRangeCalendar: StateFlow<Boolean> = _showDateRangeCalendar.asStateFlow()
    
    // 是否显示订单详情
    private val _showOrderDetails = MutableStateFlow(false)
    val showOrderDetails: StateFlow<Boolean> = _showOrderDetails.asStateFlow()
    
    // 当前查看的订单详情
    private val _selectedOrder = MutableStateFlow<PurchaseOrder?>(null)
    val selectedOrder: StateFlow<PurchaseOrder?> = _selectedOrder.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 搜索状态
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    
    // 过滤后的进货记录
    private val _filteredPurchaseRecords = MutableStateFlow<List<PurchaseRecordSummary>>(emptyList())
    val filteredPurchaseRecords: StateFlow<List<PurchaseRecordSummary>> = _filteredPurchaseRecords.asStateFlow()
    
    init {
        // 初始化时加载今天的进货记录
        loadPurchaseRecords()
        // 订阅进货订单数量变化，自动刷新
        viewModelScope.launch {
            ServiceLocator.database.purchaseOrderDao().observeCount().collect { 
                loadPurchaseRecords() 
            }
        }
    }
    
    /**
     * 加载进货记录
     */
    private fun loadPurchaseRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 模拟网络延迟
            kotlinx.coroutines.delay(300)
            
            val (startTime, endTime) = _dateFilterState.value.getTimeRange()
            val orders = ServiceLocator.purchaseRepository.getOrdersByRange(startTime, endTime)
            val summaries = orders.map { (orderEntity, itemEntities) ->
                val order = toDomainOrder(orderEntity, itemEntities)
                PurchaseRecordSummary(
                    orderId = order.id,
                    firstItemName = order.items.firstOrNull()?.goodsName ?: "",
                    totalItemCount = order.items.sumOf { it.quantity },
                    totalAmount = order.totalAmount,
                    createdAt = order.createdAt,
                    supplierName = order.supplierName
                )
            }.sortedByDescending { it.createdAt }
            _purchaseRecords.value = summaries
            applySearchFilter()
            
            _isLoading.value = false
        }
    }
    
    /**
     * 应用搜索过滤
     */
    private fun applySearchFilter() {
        val searchQuery = _searchText.value.trim()
        if (searchQuery.isEmpty()) {
            _filteredPurchaseRecords.value = _purchaseRecords.value
        } else {
            _filteredPurchaseRecords.value = _purchaseRecords.value.filter { record ->
                record.orderId.contains(searchQuery, ignoreCase = true) ||
                record.supplierName.contains(searchQuery, ignoreCase = true) ||
                record.firstItemName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    /**
     * 更新搜索文本
     */
    fun updateSearchText(text: String) {
        _searchText.value = text
        applySearchFilter()
    }
    
    /**
     * 设置日期筛选
     */
    fun setDateFilter(filterState: DateFilterState) {
        _dateFilterState.value = filterState
        loadPurchaseRecords()
    }
    
    /**
     * 显示日历选择器
     */
    fun showDatePicker() {
        _showDatePicker.value = true
    }
    
    /**
     * 隐藏日历选择器
     */
    fun hideDatePicker() {
        _showDatePicker.value = false
    }
    
    /**
     * 显示日历悬浮窗
     */
    fun showCalendar() {
        _showCalendar.value = true
    }
    
    /**
     * 隐藏日历悬浮窗
     */
    fun hideCalendar() {
        _showCalendar.value = false
    }
    
    /**
     * 显示日期范围选择器
     */
    fun showDateRangeCalendar() {
        _showDateRangeCalendar.value = true
    }
    
    /**
     * 隐藏日期范围选择器
     */
    fun hideDateRangeCalendar() {
        _showDateRangeCalendar.value = false
    }
    
    /**
     * 选择预设的日期筛选
     */
    fun selectPresetDateFilter(filterType: DateFilterType) {
        setDateFilter(DateFilterState(filterType = filterType))
        hideDatePicker()
    }
    
    /**
     * 选择自定义日期
     */
    fun selectCustomDate(date: Date) {
        setDateFilter(
            DateFilterState(
                filterType = DateFilterType.CUSTOM_DATE,
                customDate = date
            )
        )
        hideDatePicker()
    }
    
    /**
     * 选择自定义月份
     */
    fun selectCustomMonth(year: Int, month: Int) {
        setDateFilter(
            DateFilterState(
                filterType = DateFilterType.CUSTOM_MONTH,
                customYear = year,
                customMonth = month
            )
        )
        hideDatePicker()
    }
    
    /**
     * 选择自定义年份
     */
    fun selectCustomYear(year: Int) {
        setDateFilter(
            DateFilterState(
                filterType = DateFilterType.CUSTOM_YEAR,
                customYear = year
            )
        )
        hideDatePicker()
    }
    
    /**
     * 选择自定义日期范围
     */
    fun selectCustomDateRange(startDate: Date, endDate: Date) {
        setDateFilter(
            DateFilterState(
                filterType = DateFilterType.CUSTOM_DATE_RANGE,
                customStartDate = startDate,
                customEndDate = endDate
            )
        )
        hideDateRangeCalendar()
    }
    
    /**
     * 显示订单详情
     */
    fun showOrderDetails(orderId: String) {
        viewModelScope.launch {
            val pair = ServiceLocator.purchaseRepository.getOrderById(orderId)
            if (pair != null) {
                val (entity, items) = pair
                _selectedOrder.value = toDomainOrder(entity, items)
                _showOrderDetails.value = true
            }
        }
    }
    
    /**
     * 隐藏订单详情
     */
    fun hideOrderDetails() {
        _showOrderDetails.value = false
        _selectedOrder.value = null
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        loadPurchaseRecords()
    }
}


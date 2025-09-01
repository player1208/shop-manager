package com.example.storemanagerassitent.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期筛选类型
 */
enum class DateFilterType(val displayName: String) {
    ALL("全部订单"),
    TODAY("今天"),
    THIS_WEEK("本周"),
    THIS_MONTH("本月"),
    THIS_YEAR("今年"),
    CUSTOM_DATE("自定义日期"),
    CUSTOM_MONTH("自定义月份"),
    CUSTOM_YEAR("自定义年份"),
    CUSTOM_DATE_RANGE("自定义范围")
}

/**
 * 日期筛选状态
 */
data class DateFilterState(
    val filterType: DateFilterType = DateFilterType.TODAY,
    val customDate: Date? = null,
    val customMonth: Int? = null, // 1-12
    val customYear: Int? = null,
    val customStartDate: Date? = null,
    val customEndDate: Date? = null
) {
    /**
     * 获取显示文本
     */
    fun getDisplayText(): String {
        return when (filterType) {
            DateFilterType.ALL -> "全部订单"
            DateFilterType.TODAY -> "今天"
            DateFilterType.THIS_WEEK -> "本周"
            DateFilterType.THIS_MONTH -> "本月"
            DateFilterType.THIS_YEAR -> "今年"
            DateFilterType.CUSTOM_DATE -> {
                customDate?.let { 
                    SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(it)
                } ?: "自定义日期"
            }
            DateFilterType.CUSTOM_MONTH -> {
                if (customYear != null && customMonth != null) {
                    "${customYear}年${customMonth}月"
                } else "自定义月份"
            }
            DateFilterType.CUSTOM_YEAR -> {
                customYear?.let { "${it}年" } ?: "自定义年份"
            }
            DateFilterType.CUSTOM_DATE_RANGE -> {
                if (customStartDate != null && customEndDate != null) {
                    val startStr = SimpleDateFormat("MM月dd日", Locale.CHINA).format(customStartDate)
                    val endStr = SimpleDateFormat("MM月dd日", Locale.CHINA).format(customEndDate)
                    "$startStr - $endStr"
                } else "自定义范围"
            }
        }
    }
    
    /**
     * 获取筛选的时间范围
     */
    fun getTimeRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        return when (filterType) {
            DateFilterType.ALL -> (0L to Long.MAX_VALUE)
            DateFilterType.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endTime = calendar.timeInMillis
                
                startTime to endTime
            }
            DateFilterType.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endTime = calendar.timeInMillis
                
                startTime to endTime
            }
            DateFilterType.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endTime = calendar.timeInMillis
                
                startTime to endTime
            }
            DateFilterType.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endTime = calendar.timeInMillis
                
                startTime to endTime
            }
            DateFilterType.CUSTOM_DATE -> {
                customDate?.let { date ->
                    calendar.time = date
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis
                    
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endTime = calendar.timeInMillis
                    
                    startTime to endTime
                } ?: (0L to Long.MAX_VALUE)
            }
            DateFilterType.CUSTOM_MONTH -> {
                if (customYear != null && customMonth != null) {
                    calendar.set(customYear, customMonth - 1, 1, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis
                    
                    calendar.add(Calendar.MONTH, 1)
                    calendar.add(Calendar.MILLISECOND, -1)
                    val endTime = calendar.timeInMillis
                    
                    startTime to endTime
                } else (0L to Long.MAX_VALUE)
            }
            DateFilterType.CUSTOM_YEAR -> {
                customYear?.let { year ->
                    calendar.set(year, 0, 1, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis
                    
                    calendar.add(Calendar.YEAR, 1)
                    calendar.add(Calendar.MILLISECOND, -1)
                    val endTime = calendar.timeInMillis
                    
                    startTime to endTime
                } ?: (0L to Long.MAX_VALUE)
            }
            DateFilterType.CUSTOM_DATE_RANGE -> {
                if (customStartDate != null && customEndDate != null) {
                    calendar.time = customStartDate
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis
                    
                    calendar.time = customEndDate
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endTime = calendar.timeInMillis
                    
                    startTime to endTime
                } else (0L to Long.MAX_VALUE)
            }
        }
    }
}

/**
 * 销售记录摘要（用于列表显示）
 */
data class SalesRecordSummary(
    val orderId: String,
    val firstItemName: String,
    val totalItemCount: Int,
    val totalAmount: Double,
    val createdAt: Long,
    val customerName: String = ""
) {
    val itemsSummary: String
        get() = if (totalItemCount == 1) {
            firstItemName
        } else {
            "$firstItemName 等${totalItemCount}件商品"
        }
    
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(createdAt))
    
    val formattedAmount: String
        get() = SalesOrderFormatter.formatCurrency(totalAmount)
}

/**
 * 销售记录数据源
 */
object SalesRecordData {
    
    /**
     * 模拟的销售记录数据
     */
    private val sampleSalesRecords = mutableListOf(
        // 今天的记录
        SalesOrder(
            id = "ORDER_001",
            items = listOf(
                SalesOrderItem(
                    goodsId = "1",
                    goodsName = "九牧王单孔冷热龙头",
                    specifications = "J-1022",
                    unitPrice = 128.0,
                    quantity = 1,
                    category = "bathroom"
                ),
                SalesOrderItem(
                    goodsId = "2", 
                    goodsName = "科勒面盆龙头",
                    specifications = "K-2350",
                    unitPrice = 85.5,
                    quantity = 2,
                    category = "bathroom"
                )
            ),
            paymentMethod = PaymentMethod.ALIPAY,
            paymentType = PaymentType.FULL_PAYMENT,
            totalAmount = 299.0,
            customerName = "张三",
            customerPhone = "13800138001",
            customerAddress = "北京市朝阳区xxx街道",
            createdAt = System.currentTimeMillis() - 3600000 // 1小时前
        ),
        
        SalesOrder(
            id = "ORDER_002", 
            items = listOf(
                SalesOrderItem(
                    goodsId = "5",
                    goodsName = "东鹏陶瓷地砖",
                    specifications = "800x800mm",
                    unitPrice = 45.0,
                    quantity = 20,
                    category = "tiles"
                )
            ),
            paymentMethod = PaymentMethod.CASH,
            paymentType = PaymentType.DEPOSIT,
            depositAmount = 500.0,
            totalAmount = 900.0,
            customerName = "李四",
            customerPhone = "13900139002", 
            createdAt = System.currentTimeMillis() - 7200000 // 2小时前
        ),
        
        // 昨天的记录
        SalesOrder(
            id = "ORDER_003",
            items = listOf(
                SalesOrderItem(
                    goodsId = "3",
                    goodsName = "汉斯格雅花洒套装",
                    specifications = "HS-2688",
                    unitPrice = 680.0,
                    quantity = 1,
                    category = "bathroom"
                ),
                SalesOrderItem(
                    goodsId = "4",
                    goodsName = "TOTO智能马桶",
                    specifications = "CW996B",
                    unitPrice = 3200.0,
                    quantity = 1,
                    category = "bathroom"
                )
            ),
            paymentMethod = PaymentMethod.BANK_CARD,
            paymentType = PaymentType.FULL_PAYMENT,
            totalAmount = 3880.0,
            customerName = "王五",
            customerPhone = "13700137003",
            createdAt = System.currentTimeMillis() - 86400000 - 3600000 // 昨天
        ),
        
        // 本周早些时候的记录
        SalesOrder(
            id = "ORDER_004",
            items = listOf(
                SalesOrderItem(
                    goodsId = "6",
                    goodsName = "美标浴缸",
                    specifications = "AS-1680",
                    unitPrice = 2400.0,
                    quantity = 1,
                    category = "bathroom"
                )
            ),
            paymentMethod = PaymentMethod.WECHAT,
            paymentType = PaymentType.DEPOSIT,
            depositAmount = 1000.0,
            totalAmount = 2400.0,
            customerName = "赵六",
            customerPhone = "13600136004",
            createdAt = System.currentTimeMillis() - 259200000 // 3天前
        ),
        
        // 上个月的记录
        SalesOrder(
            id = "ORDER_005",
            items = listOf(
                SalesOrderItem(
                    goodsId = "7",
                    goodsName = "箭牌卫浴套装",
                    specifications = "ARROW-S01",
                    unitPrice = 1500.0,
                    quantity = 1,
                    category = "bathroom"
                ),
                SalesOrderItem(
                    goodsId = "8",
                    goodsName = "德国汉莎水槽",
                    specifications = "HS-304",
                    unitPrice = 450.0,
                    quantity = 2,
                    category = "kitchen"
                )
            ),
            paymentMethod = PaymentMethod.ALIPAY,
            paymentType = PaymentType.FULL_PAYMENT,
            totalAmount = 2400.0,
            customerName = "钱七",
            customerPhone = "13500135005",
            createdAt = System.currentTimeMillis() - 2592000000 // 30天前
        )
    )
    
    /**
     * 根据日期筛选获取销售记录摘要
     */
    fun getSalesRecordSummaries(dateFilter: DateFilterState): List<SalesRecordSummary> {
        val (startTime, endTime) = dateFilter.getTimeRange()
        
        return sampleSalesRecords
            .filter { it.createdAt in startTime..endTime }
            .sortedByDescending { it.createdAt }
            .map { order ->
                SalesRecordSummary(
                    orderId = order.id,
                    firstItemName = order.items.firstOrNull()?.goodsName ?: "",
                    totalItemCount = order.items.sumOf { it.quantity },
                    totalAmount = order.totalAmount,
                    createdAt = order.createdAt,
                    customerName = order.customerName
                )
            }
    }
    
    /**
     * 根据订单ID获取完整的销售订单详情
     */
    fun getSalesOrderById(orderId: String): SalesOrder? {
        return sampleSalesRecords.find { it.id == orderId }
    }
    
    /**
     * 添加新的销售订单（用于真实订单完成后添加到记录中）
     */
    fun addSalesRecord(order: SalesOrder) {
        // 添加到模拟数据列表的开头（最新的在前面）
        sampleSalesRecords.add(0, order)
    }
}
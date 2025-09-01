package com.example.storemanagerassitent.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 进货记录摘要（用于列表显示）
 */
data class PurchaseRecordSummary(
    val orderId: String,
    val firstItemName: String,
    val totalItemCount: Int,
    val totalAmount: Double,
    val createdAt: Long,
    val supplierName: String = ""
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
 * 进货记录数据源
 */
object PurchaseRecordData {
    
    /**
     * 模拟的进货记录数据
     */
    private val samplePurchaseRecords = mutableListOf(
        // 今天的记录
        PurchaseOrder(
            id = "PURCHASE_001",
            items = listOf(
                PurchaseOrderItem(
                    goodsId = "1",
                    goodsName = "九牧王单孔冷热龙头",
                    specifications = "J-1022",
                    purchasePrice = 95.0,
                    quantity = 5,
                    category = "bathroom"
                ),
                PurchaseOrderItem(
                    goodsId = "2", 
                    goodsName = "科勒面盆龙头",
                    specifications = "K-2350",
                    purchasePrice = 65.0,
                    quantity = 8,
                    category = "bathroom"
                )
            ),
            totalAmount = 875.0,
            totalQuantity = 13,
            supplierName = "九牧王卫浴",
            supplierPhone = "400-123-4567",
            supplierAddress = "广东省佛山市xxx工业园",
            createdAt = System.currentTimeMillis() - 3600000 // 1小时前
        ),
        
        PurchaseOrder(
            id = "PURCHASE_002", 
            items = listOf(
                PurchaseOrderItem(
                    goodsId = "5",
                    goodsName = "东鹏陶瓷地砖",
                    specifications = "800x800mm",
                    purchasePrice = 32.0,
                    quantity = 50,
                    category = "tiles"
                )
            ),
            totalAmount = 1600.0,
            totalQuantity = 50,
            supplierName = "东鹏瓷砖",
            supplierPhone = "400-789-0123", 
            createdAt = System.currentTimeMillis() - 7200000 // 2小时前
        ),
        
        // 昨天的记录
        PurchaseOrder(
            id = "PURCHASE_003",
            items = listOf(
                PurchaseOrderItem(
                    goodsId = "3",
                    goodsName = "汉斯格雅花洒套装",
                    specifications = "HS-2688",
                    purchasePrice = 520.0,
                    quantity = 3,
                    category = "bathroom"
                ),
                PurchaseOrderItem(
                    goodsId = "4",
                    goodsName = "TOTO智能马桶",
                    specifications = "CW996B",
                    purchasePrice = 2400.0,
                    quantity = 2,
                    category = "bathroom"
                )
            ),
            totalAmount = 5760.0,
            totalQuantity = 5,
            supplierName = "汉斯格雅卫浴",
            supplierPhone = "400-456-7890",
            createdAt = System.currentTimeMillis() - 86400000 - 3600000 // 昨天
        ),
        
        // 本周早些时候的记录
        PurchaseOrder(
            id = "PURCHASE_004",
            items = listOf(
                PurchaseOrderItem(
                    goodsId = "6",
                    goodsName = "美标浴缸",
                    specifications = "AS-1680",
                    purchasePrice = 1800.0,
                    quantity = 2,
                    category = "bathroom"
                )
            ),
            totalAmount = 3600.0,
            totalQuantity = 2,
            supplierName = "美标卫浴",
            supplierPhone = "400-321-6540",
            createdAt = System.currentTimeMillis() - 259200000 // 3天前
        ),
        
        // 上个月的记录
        PurchaseOrder(
            id = "PURCHASE_005",
            items = listOf(
                PurchaseOrderItem(
                    goodsId = "7",
                    goodsName = "箭牌卫浴套装",
                    specifications = "ARROW-S01",
                    purchasePrice = 1100.0,
                    quantity = 2,
                    category = "bathroom"
                ),
                PurchaseOrderItem(
                    goodsId = "8",
                    goodsName = "德国汉莎水槽",
                    specifications = "HS-304",
                    purchasePrice = 320.0,
                    quantity = 4,
                    category = "kitchen"
                )
            ),
            totalAmount = 2480.0,
            totalQuantity = 6,
            supplierName = "箭牌卫浴",
            supplierPhone = "400-987-6543",
            createdAt = System.currentTimeMillis() - 2592000000 // 30天前
        )
    )
    
    /**
     * 根据日期筛选获取进货记录摘要
     */
    fun getPurchaseRecordSummaries(dateFilter: DateFilterState): List<PurchaseRecordSummary> {
        val (startTime, endTime) = dateFilter.getTimeRange()
        
        return samplePurchaseRecords
            .filter { it.createdAt in startTime..endTime }
            .sortedByDescending { it.createdAt }
            .map { order ->
                PurchaseRecordSummary(
                    orderId = order.id,
                    firstItemName = order.items.firstOrNull()?.goodsName ?: "",
                    totalItemCount = order.items.sumOf { it.quantity },
                    totalAmount = order.totalAmount,
                    createdAt = order.createdAt,
                    supplierName = order.supplierName
                )
            }
    }
    
    /**
     * 根据订单ID获取完整的进货订单详情
     */
    fun getPurchaseOrderById(orderId: String): PurchaseOrder? {
        return samplePurchaseRecords.find { it.id == orderId }
    }
    
    /**
     * 添加新的进货订单（用于真实订单完成后添加到记录中）
     */
    fun addPurchaseRecord(order: PurchaseOrder) {
        // 添加到模拟数据列表的开头（最新的在前面）
        samplePurchaseRecords.add(0, order)
    }
}

package com.example.storemanagerassitent.data

import kotlinx.serialization.Serializable
import java.text.DecimalFormat
import java.util.UUID

/**
 * 进货单项
 */
@Serializable
data class PurchaseOrderItem(
    val id: String = UUID.randomUUID().toString(),
    val goodsId: String? = null, // 如果是现有商品，这里有ID；新商品为null
    val goodsName: String,
    val specifications: String,
    val purchasePrice: Double, // 进货价
    val quantity: Int,
    val category: String,
    val isNewGoods: Boolean = false, // 标识是否为新商品
    val barcode: String? = null
) {
    val subtotal: Double
        get() = purchasePrice * quantity
    
    val displayName: String
        get() = "$goodsName $specifications"
}

/**
 * 进货单
 */
data class PurchaseOrder(
    val id: String = UUID.randomUUID().toString(),
    val items: List<PurchaseOrderItem>,
    val supplierName: String = "", // 供应商名称
    val supplierPhone: String = "", // 供应商电话
    val supplierAddress: String = "", // 供应商地址
    val totalAmount: Double,
    val totalQuantity: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 进货单状态
 */
data class PurchaseOrderState(
    val items: List<PurchaseOrderItem> = emptyList(),
    val supplierInfo: String = ""
) {
    val totalAmount: Double
        get() = items.sumOf { it.subtotal }
    
    val totalQuantity: Int
        get() = items.sumOf { it.quantity }
    
    val canComplete: Boolean
        get() = items.isNotEmpty()
}

/**
 * OCR识别的商品原始数据
 */
data class OcrRawItem(
    val rawText: String,
    val extractedName: String = "",
    val extractedPrice: Double = 0.0,
    val extractedQuantity: Int = 1,
    val confidence: Float = 0.0f // 识别置信度
)

/**
 * 待审核的进货商品
 */
data class ReviewableItem(
    val id: String = UUID.randomUUID().toString(),
    val recognizedName: String,
    val recognizedSpecifications: String = "",
    val recognizedQuantity: Int = 1,
    val recognizedPrice: Double = 0.0,
    val confidence: Float = 1.0f, // 识别置信度 0.0-1.0
    var editedName: String = recognizedName,
    var editedSpecifications: String = recognizedSpecifications,
    var editedQuantity: Int = recognizedQuantity,
    var editedPrice: Double = recognizedPrice,
    var selectedCategory: String = "", // 选中的分类ID
    var isExistingProduct: Boolean = false // 是否是已存在的商品
) {
    val displayName: String
        get() = "$editedName $editedSpecifications".trim()
    
    val subtotal: Double
        get() = editedPrice * editedQuantity
    
    val hasLowConfidence: Boolean
        get() = confidence < 0.7f
}

/**
 * 进货记录
 */
data class PurchaseRecord(
    val id: String = UUID.randomUUID().toString(),
    val purchaseOrder: PurchaseOrder,
    val newGoodsCreated: Int = 0, // 新创建的商品数量
    val existingGoodsUpdated: Int = 0, // 更新的现有商品数量
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 格式化工具
 */
object PurchaseOrderFormatter {
    private val currencyFormatter = DecimalFormat("¥#,##0.00")
    
    fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }
    
    fun formatQuantity(quantity: Int): String {
        return "${quantity}件"
    }
}
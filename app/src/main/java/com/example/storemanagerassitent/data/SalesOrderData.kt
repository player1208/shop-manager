package com.example.storemanagerassitent.data

import java.text.DecimalFormat
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * 销售订单项
 */
@Serializable
data class SalesOrderItem(
    val id: String = UUID.randomUUID().toString(),
    val goodsId: String,
    val goodsName: String,
    
    val specifications: String,
    val unitPrice: Double,
    val quantity: Int,
    val category: String
) {
    val subtotal: Double
        get() = unitPrice * quantity
    
    val displayName: String
        get() = "$goodsName $specifications"
}

/**
 * 支付方式
 */
enum class PaymentMethod(val displayName: String) {
    CASH("现金"),
    ALIPAY("支付宝"),
    WECHAT("微信"),
    BANK_CARD("银行卡")
}

/**
 * 付款类型
 */
enum class PaymentType(val displayName: String) {
    FULL_PAYMENT("全款"),
    DEPOSIT("定金")
}

/**
 * 销售订单
 */
data class SalesOrder(
    val id: String = UUID.randomUUID().toString(),
    val items: List<SalesOrderItem>,
    val paymentMethod: PaymentMethod,
    val paymentType: PaymentType?,
    val depositAmount: Double = 0.0,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val totalAmount: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingAmount: Double
        get() = if (paymentType == PaymentType.FULL_PAYMENT) 0.0 else totalAmount - depositAmount
}

/**
 * 销售订单状态
 */
data class SalesOrderState(
    val items: List<SalesOrderItem> = emptyList(),
    val paymentMethod: PaymentMethod? = null,
    val paymentType: PaymentType? = null,
    val depositAmount: Double = 0.0,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = ""
) {
    val totalAmount: Double
        get() = items.sumOf { it.subtotal }
    
    val remainingAmount: Double
        get() = if (paymentType == PaymentType.FULL_PAYMENT) 0.0 else totalAmount - depositAmount
    
    val canCompleteOrder: Boolean
        get() = items.isNotEmpty() && paymentMethod != null && paymentType != null &&
                (paymentType == PaymentType.FULL_PAYMENT || depositAmount > 0)
}

/**
 * 商品选择状态
 */
data class ProductSelectionState(
    val selectedItems: List<SalesOrderItem> = emptyList(),
    val isSelectionMode: Boolean = false
) {
    val selectedCount: Int
        get() = selectedItems.size
}

/**
 * 格式化工具
 */
object SalesOrderFormatter {
    private val currencyFormatter = DecimalFormat("¥#,##0.00")
    
    fun formatCurrency(amount: Double): String {
        return currencyFormatter.format(amount)
    }
    
    fun formatQuantity(quantity: Int): String {
        return quantity.toString()
    }
} 
package com.example.storemanagerassitent.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories"
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String
)

@Entity(
    tableName = "goods",
    indices = [
        Index(value = ["name", "specifications"], unique = false),
        Index(value = ["categoryId"], unique = false),
        Index(value = ["barcode"], unique = false)
    ]
)
data class GoodsEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val specifications: String,
    val barcode: String?,
    val stockQuantity: Int,
    val lowStockThreshold: Int,
    val imageUrl: String?,
    val purchasePrice: Double,
    val retailPrice: Double,
    val isDelisted: Boolean,
    val lastUpdated: Long
)

@Entity(
    tableName = "purchase_orders"
)
data class PurchaseOrderEntity(
    @PrimaryKey val id: String,
    val supplierName: String,
    val supplierPhone: String,
    val supplierAddress: String,
    val totalAmount: Double,
    val totalQuantity: Int,
    val createdAt: Long
)

@Entity(
    tableName = "purchase_order_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"]), Index(value = ["goodsId"])]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val goodsId: String?,
    val goodsName: String,
    val specifications: String,
    val purchasePrice: Double,
    val quantity: Int,
    val categoryId: String,
    val isNewGoods: Boolean
)

@Entity(
    tableName = "sales_orders"
)
data class SalesOrderEntity(
    @PrimaryKey val id: String,
    val paymentMethod: String,
    val paymentType: String?,
    val depositAmount: Double,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val totalAmount: Double,
    val createdAt: Long
)

@Entity(
    tableName = "sales_order_items",
    foreignKeys = [
        ForeignKey(
            entity = SalesOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"]), Index(value = ["goodsId"])]
)
data class SalesOrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val goodsId: String,
    val goodsName: String,
    val specifications: String,
    val unitPrice: Double,
    val quantity: Int,
    val categoryId: String
)



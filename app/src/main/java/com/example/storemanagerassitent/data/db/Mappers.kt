package com.example.storemanagerassitent.data.db

import com.example.storemanagerassitent.data.Goods
import com.example.storemanagerassitent.data.GoodsCategory
import com.example.storemanagerassitent.data.PurchaseOrder
import com.example.storemanagerassitent.data.PurchaseOrderItem
import com.example.storemanagerassitent.data.SalesOrder
import com.example.storemanagerassitent.data.SalesOrderItem

fun Goods.toEntity(): GoodsEntity = GoodsEntity(
    id = id,
    name = name,
    categoryId = category,
    specifications = specifications,
    barcode = barcode,
    stockQuantity = stockQuantity,
    lowStockThreshold = lowStockThreshold,
    imageUrl = imageUrl,
    purchasePrice = purchasePrice,
    retailPrice = retailPrice,
    isDelisted = isDelisted,
    lastUpdated = lastUpdated
)

fun GoodsEntity.toModel(): Goods = Goods(
    id = id,
    name = name,
    category = categoryId,
    specifications = specifications,
    barcode = barcode,
    stockQuantity = stockQuantity,
    lowStockThreshold = lowStockThreshold,
    imageUrl = imageUrl,
    purchasePrice = purchasePrice,
    retailPrice = retailPrice,
    isDelisted = isDelisted,
    lastUpdated = lastUpdated
)

fun GoodsCategory.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex
)

fun CategoryEntity.toModel(): GoodsCategory = GoodsCategory(
    id = id,
    name = name,
    colorHex = colorHex
)

fun PurchaseOrder.toEntity(): PurchaseOrderEntity = PurchaseOrderEntity(
    id = id,
    supplierName = supplierName,
    supplierPhone = supplierPhone,
    supplierAddress = supplierAddress,
    totalAmount = totalAmount,
    totalQuantity = totalQuantity,
    createdAt = createdAt
)

fun PurchaseOrderItem.toEntity(orderId: String): PurchaseOrderItemEntity = PurchaseOrderItemEntity(
    id = id,
    orderId = orderId,
    goodsId = goodsId,
    goodsName = goodsName,
    specifications = specifications,
    purchasePrice = purchasePrice,
    quantity = quantity,
    categoryId = category,
    isNewGoods = isNewGoods
)

fun SalesOrder.toEntity(): SalesOrderEntity = SalesOrderEntity(
    id = id,
    paymentMethod = paymentMethod.name,
    paymentType = paymentType?.name,
    depositAmount = depositAmount,
    customerName = customerName,
    customerPhone = customerPhone,
    customerAddress = customerAddress,
    totalAmount = totalAmount,
    createdAt = createdAt
)

fun SalesOrderItem.toEntity(orderId: String): SalesOrderItemEntity = SalesOrderItemEntity(
    id = id,
    orderId = orderId,
    goodsId = goodsId,
    goodsName = goodsName,
    specifications = specifications,
    unitPrice = unitPrice,
    quantity = quantity,
    categoryId = category
)

fun toDomainOrder(entity: PurchaseOrderEntity, items: List<PurchaseOrderItemEntity>): PurchaseOrder {
    val domainItems = items.map { item ->
        PurchaseOrderItem(
            id = item.id,
            goodsId = item.goodsId,
            goodsName = item.goodsName,
            specifications = item.specifications,
            purchasePrice = item.purchasePrice,
            quantity = item.quantity,
            category = item.categoryId,
            isNewGoods = item.isNewGoods
        )
    }
    return PurchaseOrder(
        id = entity.id,
        items = domainItems,
        supplierName = entity.supplierName,
        supplierPhone = entity.supplierPhone,
        supplierAddress = entity.supplierAddress,
        totalAmount = entity.totalAmount,
        totalQuantity = entity.totalQuantity,
        createdAt = entity.createdAt
    )
}

fun toDomainOrder(entity: SalesOrderEntity, items: List<SalesOrderItemEntity>): SalesOrder {
    val domainItems = items.map { item ->
        SalesOrderItem(
            id = item.id,
            goodsId = item.goodsId,
            goodsName = item.goodsName,
            specifications = item.specifications,
            unitPrice = item.unitPrice,
            quantity = item.quantity,
            category = item.categoryId
        )
    }
    return SalesOrder(
        id = entity.id,
        items = domainItems,
        paymentMethod = com.example.storemanagerassitent.data.PaymentMethod.valueOf(entity.paymentMethod),
        paymentType = entity.paymentType?.let { com.example.storemanagerassitent.data.PaymentType.valueOf(it) },
        depositAmount = entity.depositAmount,
        customerName = entity.customerName,
        customerPhone = entity.customerPhone,
        customerAddress = entity.customerAddress,
        totalAmount = entity.totalAmount,
        createdAt = entity.createdAt
    )
}



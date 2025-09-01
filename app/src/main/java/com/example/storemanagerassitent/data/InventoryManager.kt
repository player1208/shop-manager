package com.example.storemanagerassitent.data

import java.util.UUID

/**
 * 库存管理器
 * 负责处理商品库存的更新、创建等操作
 */
object InventoryManager {
    
    /**
     * 库存更新结果
     */
    data class InventoryUpdateResult(
        val newGoodsCreated: Int = 0,
        val existingGoodsUpdated: Int = 0,
        val totalItemsProcessed: Int = 0,
        val errors: List<String> = emptyList()
    ) {
        val isSuccess: Boolean
            get() = errors.isEmpty()
        
        val hasNewGoods: Boolean
            get() = newGoodsCreated > 0
        
        val hasUpdatedGoods: Boolean
            get() = existingGoodsUpdated > 0
    }
    
    /**
     * 处理进货单入库
     * 根据进货单项目更新或创建商品库存
     */
    fun processInboundOrder(purchaseItems: List<PurchaseOrderItem>): InventoryUpdateResult {
        var newGoodsCreated = 0
        var existingGoodsUpdated = 0
        val errors = mutableListOf<String>()
        
        try {
            purchaseItems.forEach { item ->
                try {
                    val existingGoods = findExistingGoods(item.goodsName, item.specifications)
                    
                    if (existingGoods != null) {
                        // 更新现有商品库存
                        updateExistingGoodsStock(existingGoods, item)
                        existingGoodsUpdated++
                    } else {
                        // 创建新商品
                        createNewGoods(item)
                        newGoodsCreated++
                    }
                } catch (e: Exception) {
                    errors.add("处理商品 ${item.displayName} 时发生错误: ${e.message}")
                }
            }
        } catch (e: Exception) {
            errors.add("入库处理失败: ${e.message}")
        }
        
        return InventoryUpdateResult(
            newGoodsCreated = newGoodsCreated,
            existingGoodsUpdated = existingGoodsUpdated,
            totalItemsProcessed = purchaseItems.size,
            errors = errors
        )
    }
    
    /**
     * 查找现有商品
     * 根据商品名称和规格匹配现有商品
     */
    private fun findExistingGoods(goodsName: String, specifications: String): Goods? {
        return SampleData.goods.find { goods ->
            // 完全匹配：名称和规格都相同
            goods.name.equals(goodsName, ignoreCase = true) && 
            goods.specifications.equals(specifications, ignoreCase = true)
        } ?: SampleData.goods.find { goods ->
            // 模糊匹配：仅名称相同，规格为空或相似
            goods.name.equals(goodsName, ignoreCase = true) && 
            (goods.specifications.isBlank() || specifications.isBlank())
        }
    }
    
    /**
     * 更新现有商品库存
     */
    private fun updateExistingGoodsStock(existingGoods: Goods, purchaseItem: PurchaseOrderItem) {
        // 在实际应用中，这里应该更新数据库
        // 目前我们只是模拟更新过程
        
        // 累加库存数量
        val newStock = existingGoods.stockQuantity + purchaseItem.quantity
        
        // 可选：更新进货价格（如果新进货价格不同）
        val shouldUpdatePrice = purchaseItem.purchasePrice > 0 && 
                               purchaseItem.purchasePrice != existingGoods.purchasePrice
        
        // 模拟数据库更新操作
        // 在真实应用中，这里会调用数据库更新方法
        val updatedGoods = existingGoods.copy(
            stockQuantity = newStock,
            purchasePrice = if (shouldUpdatePrice) purchaseItem.purchasePrice else existingGoods.purchasePrice,
            lastUpdated = System.currentTimeMillis()
        )
        
        // 更新示例数据（仅用于演示）
        updateSampleDataGoods(updatedGoods)
    }
    
    /**
     * 创建新商品
     */
    private fun createNewGoods(purchaseItem: PurchaseOrderItem) {
        // 在实际应用中，这里应该插入到数据库
        // 目前我们只是模拟创建过程
        
        val newGoods = Goods(
            id = UUID.randomUUID().toString(),
            name = purchaseItem.goodsName,
            specifications = purchaseItem.specifications,
            category = purchaseItem.category,
            purchasePrice = purchaseItem.purchasePrice,
            retailPrice = calculateSuggestedRetailPrice(purchaseItem.purchasePrice),
            stockQuantity = purchaseItem.quantity,
            lowStockThreshold = calculateMinStockAlert(purchaseItem.quantity),
            isDelisted = false,
            lastUpdated = System.currentTimeMillis()
        )
        
        // 添加到示例数据（仅用于演示）
        addToSampleDataGoods(newGoods)
    }
    
    /**
     * 计算建议零售价
     * 基于进货价格计算建议的零售价格（通常是进货价的1.2-1.5倍）
     */
    private fun calculateSuggestedRetailPrice(purchasePrice: Double): Double {
        return when {
            purchasePrice <= 0 -> 0.0
            purchasePrice < 10 -> purchasePrice * 1.5  // 低价商品使用更高倍率
            purchasePrice < 50 -> purchasePrice * 1.3  // 中价商品
            else -> purchasePrice * 1.2               // 高价商品使用较低倍率
        }
    }
    
    /**
     * 计算最小库存警戒值
     * 基于进货数量估算合理的库存警戒线
     */
    private fun calculateMinStockAlert(initialStock: Int): Int {
        return when {
            initialStock <= 5 -> 1
            initialStock <= 20 -> 3
            initialStock <= 50 -> 5
            else -> 10
        }
    }
    
    /**
     * 更新商品数据
     * 实际更新数据并通知相关组件
     */
    private fun updateSampleDataGoods(updatedGoods: Goods) {
        try {
            // 使用反射或直接修改SampleData中的数据
            val goodsList = SampleData.goods as? MutableList<Goods> ?: SampleData.goods.toMutableList()
            val index = goodsList.indexOfFirst { it.id == updatedGoods.id }
            if (index >= 0) {
                goodsList[index] = updatedGoods
                // 如果SampleData.goods不是MutableList，需要替换整个列表
                if (SampleData.goods !is MutableList<Goods>) {
                    // 通过反射更新SampleData
                    updateSampleDataList(goodsList)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("更新商品数据失败: ${e.message}", e)
        }
    }
    
    /**
     * 添加新商品到数据中
     * 实际添加数据并通知相关组件
     */
    private fun addToSampleDataGoods(newGoods: Goods) {
        try {
            val goodsList = SampleData.goods as? MutableList<Goods> ?: SampleData.goods.toMutableList()
            goodsList.add(newGoods)
            // 如果SampleData.goods不是MutableList，需要替换整个列表
            if (SampleData.goods !is MutableList<Goods>) {
                // 通过反射更新SampleData
                updateSampleDataList(goodsList)
            }
        } catch (e: Exception) {
            throw RuntimeException("添加新商品失败: ${e.message}", e)
        }
    }
    
    /**
     * 更新SampleData中的商品列表
     * 使用反射机制更新不可变列表
     */
    private fun updateSampleDataList(newList: List<Goods>) {
        try {
            val sampleDataClass = SampleData::class.java
            val goodsField = sampleDataClass.getDeclaredField("goods")
            goodsField.isAccessible = true
            goodsField.set(null, newList)
        } catch (e: Exception) {
            // 如果反射失败，至少记录错误
            println("警告：无法通过反射更新SampleData，数据可能不会持久化: ${e.message}")
        }
    }
    
    /**
     * 验证商品数据的有效性
     */
    fun validateGoodsData(purchaseItem: PurchaseOrderItem): List<String> {
        val errors = mutableListOf<String>()
        
        if (purchaseItem.goodsName.isBlank()) {
            errors.add("商品名称不能为空")
        }
        
        if (purchaseItem.quantity <= 0) {
            errors.add("商品数量必须大于0")
        }
        
        if (purchaseItem.purchasePrice < 0) {
            errors.add("进货价格不能为负数")
        }
        
        if (purchaseItem.category.isBlank()) {
            errors.add("商品分类不能为空")
        }
        
        return errors
    }
    
    /**
     * 批量验证商品数据
     */
    fun validateBatchGoodsData(purchaseItems: List<PurchaseOrderItem>): List<String> {
        val allErrors = mutableListOf<String>()
        
        purchaseItems.forEachIndexed { index, item ->
            val itemErrors = validateGoodsData(item)
            itemErrors.forEach { error ->
                allErrors.add("第${index + 1}件商品：$error")
            }
        }
        
        return allErrors
    }
    
    /**
     * 生成进货记录
     * 记录本次进货的详细信息，用于后续查询和统计
     */
    fun createPurchaseRecord(
        purchaseOrder: PurchaseOrder,
        updateResult: InventoryUpdateResult
    ): PurchaseRecord {
        return PurchaseRecord(
            id = UUID.randomUUID().toString(),
            purchaseOrder = purchaseOrder,
            newGoodsCreated = updateResult.newGoodsCreated,
            existingGoodsUpdated = updateResult.existingGoodsUpdated,
            createdAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 处理销售出库
     * 根据销售订单项减少商品库存
     */
    fun processSalesOutbound(salesItems: List<SalesOrderItem>): InventoryUpdateResult {
        var updatedGoodsCount = 0
        val errors = mutableListOf<String>()
        
        try {
            salesItems.forEach { item ->
                try {
                    val existingGoods = findGoodsById(item.goodsId)
                    
                    if (existingGoods != null) {
                        // 检查库存是否充足
                        if (existingGoods.stockQuantity >= item.quantity) {
                            // 减少库存
                            val newStock = existingGoods.stockQuantity - item.quantity
                            val updatedGoods = existingGoods.copy(
                                stockQuantity = newStock,
                                lastUpdated = System.currentTimeMillis()
                            )
                            updateSampleDataGoods(updatedGoods)
                            updatedGoodsCount++
                        } else {
                            errors.add("商品 ${item.displayName} 库存不足，现有库存：${existingGoods.stockQuantity}，需要：${item.quantity}")
                        }
                    } else {
                        errors.add("未找到商品：${item.displayName}")
                    }
                } catch (e: Exception) {
                    errors.add("处理商品 ${item.displayName} 时发生错误: ${e.message}")
                }
            }
        } catch (e: Exception) {
            errors.add("销售出库处理失败: ${e.message}")
        }
        
        return InventoryUpdateResult(
            newGoodsCreated = 0,
            existingGoodsUpdated = updatedGoodsCount,
            totalItemsProcessed = salesItems.size,
            errors = errors
        )
    }
    
    /**
     * 根据商品ID查找商品
     */
    private fun findGoodsById(goodsId: String): Goods? {
        return SampleData.goods.find { it.id == goodsId }
    }
    
    /**
     * 检查销售订单的库存可用性
     * 返回库存不足的商品列表
     */
    fun checkSalesStockAvailability(salesItems: List<SalesOrderItem>): List<String> {
        val insufficientStock = mutableListOf<String>()
        
        salesItems.forEach { item ->
            val existingGoods = findGoodsById(item.goodsId)
            if (existingGoods == null) {
                insufficientStock.add("商品 ${item.displayName} 不存在")
            } else if (existingGoods.stockQuantity < item.quantity) {
                insufficientStock.add("商品 ${item.displayName} 库存不足，现有：${existingGoods.stockQuantity}，需要：${item.quantity}")
            }
        }
        
        return insufficientStock
    }
}
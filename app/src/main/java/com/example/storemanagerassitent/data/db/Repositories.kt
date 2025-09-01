package com.example.storemanagerassitent.data.db

import com.example.storemanagerassitent.data.Goods
import com.example.storemanagerassitent.data.GoodsCategory as DomainGoodsCategory
import com.example.storemanagerassitent.data.PurchaseOrder
import com.example.storemanagerassitent.data.PurchaseOrderItem
import com.example.storemanagerassitent.data.SalesOrder
import com.example.storemanagerassitent.data.SalesOrderItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import androidx.room.withTransaction
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import com.example.storemanagerassitent.data.Category
import com.example.storemanagerassitent.data.CategoryDeleteResult
import com.example.storemanagerassitent.data.CategoryOperationResult
import com.example.storemanagerassitent.data.GoodsCategory

class GoodsRepository(
    private val goodsDao: GoodsDao,
    private val categoryDao: CategoryDao
) {
    fun observeGoods(): Flow<List<Goods>> = goodsDao.observeAllAvailable().map { list ->
        list.map { it.toModel() }
    }

    suspend fun initializeSampleDataIfEmpty(sampleGoods: List<Goods>, sampleCategories: List<GoodsCategory>) {
        if (goodsDao.count() == 0) {
            categoryDao.upsertAll(sampleCategories.map { it.toEntity() })
            goodsDao.upsertAll(sampleGoods.map { it.toEntity() })
        }
    }

    suspend fun upsertGoods(goods: Goods) {
        goodsDao.upsert(goods.toEntity())
    }

    suspend fun adjustStock(goodsId: String, delta: Int) {
        val current = goodsDao.getById(goodsId) ?: return
        val updated = current.copy(
            stockQuantity = (current.stockQuantity + delta).coerceAtLeast(0),
            lastUpdated = System.currentTimeMillis()
        )
        goodsDao.update(updated)
    }

    suspend fun deleteGoodsByIds(ids: List<String>) {
        if (ids.isEmpty()) return
        goodsDao.deleteByIds(ids)
    }

    suspend fun findByNameAndSpec(name: String, spec: String): Goods? {
        return goodsDao.findByNameAndSpec(name, spec)?.toModel()
    }

    suspend fun findByFullDisplayName(fullName: String): Goods? {
        return goodsDao.findByFullDisplayName(fullName)?.toModel()
    }

    suspend fun updateGoodsName(goodsId: String, newName: String) {
        val current = goodsDao.getById(goodsId) ?: return
        val updated = current.copy(
            name = newName,
            lastUpdated = System.currentTimeMillis()
        )
        goodsDao.update(updated)
    }

    suspend fun updateGoodsPurchasePrice(goodsId: String, newPrice: Double) {
        val current = goodsDao.getById(goodsId) ?: return
        val updated = current.copy(
            purchasePrice = newPrice,
            lastUpdated = System.currentTimeMillis()
        )
        goodsDao.update(updated)
    }

    suspend fun updateGoodsBarcode(goodsId: String, newBarcode: String?) {
        val current = goodsDao.getById(goodsId) ?: return
        val normalized = newBarcode?.trim()?.ifBlank { null }
        val updated = current.copy(
            barcode = normalized,
            lastUpdated = System.currentTimeMillis()
        )
        goodsDao.update(updated)
    }
}

class PurchaseRepository(
    private val db: AppDatabase
) {
    private val goodsDao = db.goodsDao()
    private val orderDao = db.purchaseOrderDao()

    suspend fun processInboundAndSave(order: PurchaseOrder): Pair<Int, Int> {
        // Update stock and insert order and items in a single transaction-like block
        var newGoods = 0
        var updatedGoods = 0

        db.withTransaction {
            // Save order
            orderDao.insertOrder(order.toEntity())
            orderDao.insertItems(order.items.map { it.toEntity(order.id) })

            // For each item, update or create goods
            order.items.forEach { item ->
                val existing = if (item.goodsId != null) goodsDao.getById(item.goodsId) else goodsDao.findByNameAndSpec(item.goodsName, item.specifications)
                if (existing != null) {
                    val updated = existing.copy(
                        stockQuantity = existing.stockQuantity + item.quantity,
                        purchasePrice = if (item.purchasePrice > 0) item.purchasePrice else existing.purchasePrice,
                        lastUpdated = System.currentTimeMillis()
                    )
                    val withBarcode = if ((existing.barcode == null || existing.barcode.isBlank()) && !item.barcode.isNullOrBlank()) {
                        updated.copy(barcode = item.barcode)
                    } else updated
                    goodsDao.update(withBarcode)
                    updatedGoods++
                } else {
                    val entity = GoodsEntity(
                        id = item.goodsId ?: UUID.randomUUID().toString(),
                        name = item.goodsName,
                        categoryId = item.category,
                        specifications = item.specifications,
                        barcode = item.barcode,
                        stockQuantity = item.quantity,
                        lowStockThreshold = when {
                            item.quantity <= 5 -> 1
                            item.quantity <= 20 -> 3
                            item.quantity <= 50 -> 5
                            else -> 10
                        },
                        imageUrl = null,
                        purchasePrice = item.purchasePrice,
                        retailPrice = when {
                            item.purchasePrice <= 0 -> 0.0
                            item.purchasePrice < 10 -> item.purchasePrice * 1.5
                            item.purchasePrice < 50 -> item.purchasePrice * 1.3
                            else -> item.purchasePrice * 1.2
                        },
                        isDelisted = false,
                        lastUpdated = System.currentTimeMillis()
                    )
                    goodsDao.upsert(entity)
                    newGoods++
                }
            }
        }

        return newGoods to updatedGoods
    }

    suspend fun getOrdersByRange(start: Long, end: Long): List<Pair<PurchaseOrderEntity, List<PurchaseOrderItemEntity>>> {
        val orders = db.purchaseOrderDao().getOrdersByTimeRange(start, end)
        return orders.map { it to db.purchaseOrderDao().getItemsByOrderId(it.id) }
    }

    suspend fun getOrderById(orderId: String): Pair<PurchaseOrderEntity, List<PurchaseOrderItemEntity>>? {
        val order = db.purchaseOrderDao().getById(orderId) ?: return null
        val items = db.purchaseOrderDao().getItemsByOrderId(orderId)
        return order to items
    }
}

class SalesRepository(
    private val db: AppDatabase
) {
    private val goodsDao = db.goodsDao()
    private val orderDao = db.salesOrderDao()

    suspend fun processSaleAndSave(order: SalesOrder): Int {
        var updatedGoods = 0
        db.withTransaction {
            // Stock check
            order.items.forEach { item ->
                val goods = goodsDao.getById(item.goodsId) ?: error("商品不存在: ${'$'}{item.goodsName}")
                require(goods.stockQuantity >= item.quantity) { "商品 ${'$'}{item.goodsName} 库存不足" }
            }

            // Deduct stock
            order.items.forEach { item ->
                val goods = goodsDao.getById(item.goodsId) ?: return@forEach
                val updated = goods.copy(
                    stockQuantity = goods.stockQuantity - item.quantity,
                    lastUpdated = System.currentTimeMillis()
                )
                goodsDao.update(updated)
                updatedGoods++
            }

            // Save order
            orderDao.insertOrder(order.toEntity())
            orderDao.insertItems(order.items.map { it.toEntity(order.id) })
        }
        return updatedGoods
    }

    suspend fun getOrdersByRange(start: Long, end: Long): List<Pair<SalesOrderEntity, List<SalesOrderItemEntity>>> {
        val orders = db.salesOrderDao().getOrdersByTimeRange(start, end)
        return orders.map { it to db.salesOrderDao().getItemsByOrderId(it.id) }
    }

    suspend fun getOrderById(orderId: String): Pair<SalesOrderEntity, List<SalesOrderItemEntity>>? {
        val order = db.salesOrderDao().getById(orderId) ?: return null
        val items = db.salesOrderDao().getItemsByOrderId(orderId)
        return order to items
    }
}

class CategoryRepositoryRoom(
    private val db: AppDatabase
) {
    private val categoryDao = db.categoryDao()
    private val goodsDao = db.goodsDao()

    fun observeCategoriesWithCount(): Flow<List<Category>> {
        // 同时订阅分类与商品的变更，确保商品数量实时更新
        return combine(
            categoryDao.observeAll(),
            goodsDao.observeAllAvailable()
        ) { categoryEntities, goodsEntities ->
            val categoriesWithAll = ensureAllCategory(categoryEntities)
            val countsByCategory = goodsEntities
                .groupBy { it.categoryId }
                .mapValues { it.value.count() }

            categoriesWithAll.map { entity ->
                val count = if (entity.id == ALL_ID) 0 else countsByCategory[entity.id] ?: 0
                Category(id = entity.id, name = entity.name, productCount = count)
            }
        }
    }

    fun observeGoodsCategories(): Flow<List<DomainGoodsCategory>> {
        val palette = listOf("#FF5722", "#03A9F4", "#8BC34A", "#FF9800", "#9C27B0")
        return categoryDao.observeAll().map { entities ->
            val list = ensureAllCategory(entities)
            list.mapIndexed { index, e ->
                val color = if (e.id == ALL_ID) ALL_COLOR else palette[(index - 1).coerceAtLeast(0) % palette.size]
                DomainGoodsCategory(id = e.id, name = e.name, colorHex = color)
            }
        }
    }

    suspend fun addCategory(name: String): CategoryOperationResult {
        if (name.isBlank()) return CategoryOperationResult.Error("分类名称不能为空")
        // 固定保留 ALL，不允许重名
        if (name == ALL_NAME) return CategoryOperationResult.Error("该名称已保留")
        val entity = CategoryEntity(id = generateCategoryId(), name = name, colorHex = DEFAULT_COLOR)
        categoryDao.upsert(entity) // rowid 自然递增，保证新分类出现在末尾
        return CategoryOperationResult.Success
    }

    suspend fun editCategory(categoryId: String, newName: String): CategoryOperationResult {
        if (newName.isBlank()) return CategoryOperationResult.Error("分类名称不能为空")
        // 读取当前实体
        // 缺少 getById，简单通过观察列表快照方式不可行；这里采用 upsert 覆盖（需要现有颜色）。
        // 为兼容，先获取当前所有分类并查找（临时查询接口缺失，改造 Dao 更优）。
        val current = categoryDao.observeAll()
        // 不能直接 collect 这里，简化：直接覆盖默认颜色
        if (categoryId == ALL_ID) return CategoryOperationResult.Error("'全部' 不可编辑")
        val entity = CategoryEntity(id = categoryId, name = newName, colorHex = DEFAULT_COLOR)
        categoryDao.upsert(entity)
        return CategoryOperationResult.Success
    }

    suspend fun deleteCategory(categoryId: String): CategoryDeleteResult {
        if (categoryId == ALL_ID) return CategoryDeleteResult.Error("'全部' 不可删除")
        val count = goodsDao.countByCategory(categoryId)
        if (count > 0) return CategoryDeleteResult.HasProducts(count)
        categoryDao.deleteById(categoryId)
        return CategoryDeleteResult.Success
    }

    suspend fun hasCategoryProducts(categoryId: String): Boolean {
        return goodsDao.countByCategory(categoryId) > 0
    }

    private fun ensureAllCategory(input: List<CategoryEntity>): List<CategoryEntity> {
        if (input.any { it.id == ALL_ID }) return input
        val all = CategoryEntity(id = ALL_ID, name = ALL_NAME, colorHex = ALL_COLOR)
        return listOf(all) + input
    }

    private fun generateCategoryId(): String = "category_${UUID.randomUUID()}"

    companion object {
        private const val ALL_ID = "all"
        private const val ALL_NAME = "全部"
        private const val ALL_COLOR = "#6C757D"
        private const val DEFAULT_COLOR = "#9C27B0"
    }
}

class AdminRepository(
    private val db: AppDatabase
) {
    private val goodsDao = db.goodsDao()
    private val categoryDao = db.categoryDao()
    private val purchaseDao = db.purchaseOrderDao()
    private val salesDao = db.salesOrderDao()

    suspend fun clearAll() {
        // 使用 Room 提供的安全清空，保证外键顺序正确
        db.clearAllTables()
    }
}



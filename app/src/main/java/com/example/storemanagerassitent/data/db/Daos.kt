package com.example.storemanagerassitent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY rowid")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface GoodsDao {
    @Query("SELECT * FROM goods WHERE isDelisted = 0 ORDER BY lastUpdated DESC")
    fun observeAllAvailable(): Flow<List<GoodsEntity>>

    @Query("SELECT * FROM goods WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoodsEntity?

    @Query("SELECT COUNT(*) FROM goods")
    suspend fun count(): Int

    @Query("SELECT * FROM goods WHERE name = :name AND specifications = :spec LIMIT 1")
    suspend fun findByNameAndSpec(name: String, spec: String): GoodsEntity?

    @Query("SELECT * FROM goods WHERE (name || ' ' || specifications) = :fullName OR name = :fullName LIMIT 1")
    suspend fun findByFullDisplayName(fullName: String): GoodsEntity?

    @Query("SELECT * FROM goods WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): GoodsEntity?

    @Query("SELECT * FROM goods")
    suspend fun getAll(): List<GoodsEntity>

    @Query("SELECT COUNT(*) FROM goods WHERE categoryId = :categoryId AND isDelisted = 0")
    suspend fun countByCategory(categoryId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(goods: List<GoodsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goods: GoodsEntity)

    @Update
    suspend fun update(goods: GoodsEntity)

    @Query("DELETE FROM goods WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

@Dao
interface PurchaseOrderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(order: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<PurchaseOrderItemEntity>)

    @Transaction
    @Query("SELECT * FROM purchase_orders WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    suspend fun getOrdersByTimeRange(start: Long, end: Long): List<PurchaseOrderEntity>

    @Query("SELECT * FROM purchase_orders WHERE id = :orderId LIMIT 1")
    suspend fun getById(orderId: String): PurchaseOrderEntity?

    @Query("SELECT * FROM purchase_order_items WHERE orderId = :orderId")
    suspend fun getItemsByOrderId(orderId: String): List<PurchaseOrderItemEntity>

    @Query("SELECT COUNT(*) FROM purchase_orders")
    fun observeCount(): Flow<Int>
}

@Dao
interface SalesOrderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(order: SalesOrderEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<SalesOrderItemEntity>)

    @Transaction
    @Query("SELECT * FROM sales_orders WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    suspend fun getOrdersByTimeRange(start: Long, end: Long): List<SalesOrderEntity>

    @Query("SELECT * FROM sales_orders WHERE id = :orderId LIMIT 1")
    suspend fun getById(orderId: String): SalesOrderEntity?

    @Query("SELECT * FROM sales_order_items WHERE orderId = :orderId")
    suspend fun getItemsByOrderId(orderId: String): List<SalesOrderItemEntity>

    @Query("SELECT COUNT(*) FROM sales_orders")
    fun observeCount(): Flow<Int>
}



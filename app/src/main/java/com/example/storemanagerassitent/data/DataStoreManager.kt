package com.example.storemanagerassitent.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * 数据持久化管理器
 * 使用DataStore保存进货单草稿和记录
 */
class DataStoreManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("store_manager_prefs")
        private val PURCHASE_DRAFTS_KEY = stringPreferencesKey("purchase_drafts")
        private val PURCHASE_RECORDS_KEY = stringPreferencesKey("purchase_records")
        private val SALES_DRAFTS_KEY = stringPreferencesKey("sales_drafts")
        private val APP_SETTINGS_KEY = stringPreferencesKey("app_settings")
        
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
    
    /**
     * 进货单草稿数据
     */
    @Serializable
    data class PurchaseDraft(
        val id: String,
        val supplierInfo: String = "",
        val items: List<PurchaseOrderItem> = emptyList(),
        val totalAmount: Double = 0.0,
        val totalQuantity: Int = 0,
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * 销售单草稿数据
     */
    @Serializable
    data class SalesDraft(
        val id: String,
        val items: List<SalesOrderItem> = emptyList(),
        val paymentMethod: String? = null,
        val paymentType: String? = null,
        val depositAmount: Double = 0.0,
        val customerName: String = "",
        val customerPhone: String = "",
        val customerAddress: String = "",
        val totalAmount: Double = 0.0,
        val totalQuantity: Int = 0,
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * 应用设置
     */
    @Serializable
    data class AppSettings(
        val ocrEnabled: Boolean = true,
        val autoSaveDrafts: Boolean = true,
        val reminderEnabled: Boolean = true,
        val defaultSupplier: String = "",
        val lastBackupTime: Long = 0L,
        val seedDisabled: Boolean = true,
        val hasClearedTestData: Boolean = false
    )
    
    /**
     * 保存进货单草稿
     */
    suspend fun savePurchaseDraft(draft: PurchaseDraft) {
        try {
            val currentDrafts = getPurchaseDrafts()
            val updatedDrafts = currentDrafts.toMutableList()
            
            // 查找是否已存在相同ID的草稿
            val existingIndex = updatedDrafts.indexOfFirst { it.id == draft.id }
            if (existingIndex >= 0) {
                updatedDrafts[existingIndex] = draft.copy(updatedAt = System.currentTimeMillis())
            } else {
                updatedDrafts.add(draft)
            }
            
            // 最多保存10个草稿
            if (updatedDrafts.size > 10) {
                updatedDrafts.sortBy { it.updatedAt }
                updatedDrafts.removeFirst()
            }
            
            val draftsJson = json.encodeToString(updatedDrafts)
            context.dataStore.edit { preferences ->
                preferences[PURCHASE_DRAFTS_KEY] = draftsJson
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取所有进货单草稿
     */
    suspend fun getPurchaseDrafts(): List<PurchaseDraft> {
        return try {
            val draftsFlow: Flow<String> = context.dataStore.data.map { preferences ->
                preferences[PURCHASE_DRAFTS_KEY] ?: "[]"
            }
            var draftsJson = ""
            draftsFlow.collect { draftsJson = it }
            json.decodeFromString<List<PurchaseDraft>>(draftsJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 删除进货单草稿
     */
    suspend fun deletePurchaseDraft(draftId: String) {
        try {
            val currentDrafts = getPurchaseDrafts()
            val updatedDrafts = currentDrafts.filter { it.id != draftId }
            val draftsJson = json.encodeToString(updatedDrafts)
            
            context.dataStore.edit { preferences ->
                preferences[PURCHASE_DRAFTS_KEY] = draftsJson
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 保存进货记录
     */
    suspend fun savePurchaseRecord(record: PurchaseRecord) {
        try {
            val currentRecords = getPurchaseRecords()
            val updatedRecords = currentRecords.toMutableList()
            updatedRecords.add(0, record) // 最新的记录放在前面
            
            // 最多保存100条记录
            if (updatedRecords.size > 100) {
                updatedRecords.removeAt(updatedRecords.size - 1)
            }
            
            val recordsJson = json.encodeToString(updatedRecords)
            context.dataStore.edit { preferences ->
                preferences[PURCHASE_RECORDS_KEY] = recordsJson
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取所有进货记录
     */
    suspend fun getPurchaseRecords(): List<PurchaseRecord> {
        return try {
            val recordsFlow: Flow<String> = context.dataStore.data.map { preferences ->
                preferences[PURCHASE_RECORDS_KEY] ?: "[]"
            }
            var recordsJson = ""
            recordsFlow.collect { recordsJson = it }
            json.decodeFromString<List<PurchaseRecord>>(recordsJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 保存销售单草稿
     */
    suspend fun saveSalesDraft(draft: SalesDraft) {
        try {
            val currentDrafts = getSalesDrafts()
            val updatedDrafts = currentDrafts.toMutableList()
            
            // 查找是否已存在相同ID的草稿
            val existingIndex = updatedDrafts.indexOfFirst { it.id == draft.id }
            if (existingIndex >= 0) {
                updatedDrafts[existingIndex] = draft.copy(updatedAt = System.currentTimeMillis())
            } else {
                updatedDrafts.add(draft)
            }
            
            // 最多保存10个草稿
            if (updatedDrafts.size > 10) {
                updatedDrafts.sortBy { it.updatedAt }
                updatedDrafts.removeFirst()
            }
            
            val draftsJson = json.encodeToString(updatedDrafts)
            context.dataStore.edit { preferences ->
                preferences[SALES_DRAFTS_KEY] = draftsJson
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取所有销售单草稿
     */
    suspend fun getSalesDrafts(): List<SalesDraft> {
        return try {
            val draftsFlow: Flow<String> = context.dataStore.data.map { preferences ->
                preferences[SALES_DRAFTS_KEY] ?: "[]"
            }
            var draftsJson = ""
            draftsFlow.collect { draftsJson = it }
            json.decodeFromString<List<SalesDraft>>(draftsJson)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 保存应用设置
     */
    suspend fun saveAppSettings(settings: AppSettings) {
        try {
            val settingsJson = json.encodeToString(settings)
            context.dataStore.edit { preferences ->
                preferences[APP_SETTINGS_KEY] = settingsJson
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取应用设置
     */
    suspend fun getAppSettings(): AppSettings {
        return try {
            val settingsFlow: Flow<String> = context.dataStore.data.map { preferences ->
                preferences[APP_SETTINGS_KEY] ?: "{}"
            }
            var settingsJson = ""
            settingsFlow.collect { settingsJson = it }
            if (settingsJson == "{}") {
                AppSettings() // 返回默认设置
            } else {
                json.decodeFromString<AppSettings>(settingsJson)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppSettings() // 返回默认设置
        }
    }
    
    /**
     * 清空所有数据
     */
    suspend fun clearAllData() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取数据统计信息
     */
    suspend fun getDataStatistics(): DataStatistics {
        return try {
            val drafts = getPurchaseDrafts()
            val records = getPurchaseRecords()
            
            DataStatistics(
                draftCount = drafts.size,
                recordCount = records.size,
                totalPurchaseAmount = records.sumOf { it.purchaseOrder.totalAmount },
                lastPurchaseTime = records.maxByOrNull { it.createdAt }?.createdAt ?: 0L
            )
        } catch (e: Exception) {
            e.printStackTrace()
            DataStatistics()
        }
    }
}

/**
 * 数据统计信息
 */
@Serializable
data class DataStatistics(
    val draftCount: Int = 0,
    val recordCount: Int = 0,
    val totalPurchaseAmount: Double = 0.0,
    val lastPurchaseTime: Long = 0L
)
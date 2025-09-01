package com.example.storemanagerassitent.data.db

import android.content.Context
import com.example.storemanagerassitent.data.SampleData
import com.example.storemanagerassitent.data.DataStoreManager

object ServiceLocator {
    @Volatile private var initialized: Boolean = false
    lateinit var database: AppDatabase
        private set

    val goodsRepository: GoodsRepository by lazy {
        GoodsRepository(database.goodsDao(), database.categoryDao())
    }

    val purchaseRepository: PurchaseRepository by lazy {
        PurchaseRepository(database)
    }

    val salesRepository: SalesRepository by lazy {
        SalesRepository(database)
    }

    val categoryRepository: CategoryRepositoryRoom by lazy {
        CategoryRepositoryRoom(database)
    }

    val adminRepository: AdminRepository by lazy {
        AdminRepository(database)
    }

    fun init(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    database = AppDatabase.getInstance(context)
                    initialized = true
                }
            }
        }
    }

    suspend fun seedSampleDataIfAllowed(dataStoreManager: DataStoreManager) {
        val settings = dataStoreManager.getAppSettings()
        if (settings.seedDisabled) return
        goodsRepository.initializeSampleDataIfEmpty(
            SampleData.goods,
            SampleData.categories
        )
    }
}



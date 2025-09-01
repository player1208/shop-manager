package com.example.storemanagerassitent.utils

import android.util.Log
import com.example.storemanagerassitent.data.db.GoodsEntity

/**
 * 型号匹配功能测试工具
 * 用于验证型号提取和匹配的准确性
 */
object ModelMatchingTest {
    
    private const val TAG = "ModelMatchingTest"
    
    /**
     * 运行完整的型号匹配测试
     */
    fun runTests() {
        Log.d(TAG, "=== 开始型号匹配测试 ===")
        
        testModelExtraction()
        testModelMatching()  
        testRealWorldScenarios()
        
        Log.d(TAG, "=== 型号匹配测试完成 ===")
    }
    
    /**
     * 测试型号提取功能
     */
    private fun testModelExtraction() {
        Log.d(TAG, "\n--- 测试型号提取功能 ---")
        
        val testCases = mapOf(
            "CHM2-2ZE控制器" to listOf("CHM2-2ZE"),
            "ABC1-5X 智能开关" to listOf("ABC1-5X"),
            "MH4-12A型号产品" to listOf("MH4-12A"),
            "产品型号CHM2-2" to listOf("CHM2-2"),
            "CH-2ZE系列" to listOf("CH-2ZE"),
            "新品CHM22ZE上市" to listOf("CHM22ZE"),
            "iPhone 14 Pro Max" to listOf("IPHONE 14"),
            "小米13 Ultra" to listOf("小米13"),
            "华为P50 Pro" to listOf("华为P50"),
            "产品(CHM2-2ZE)说明" to listOf("CHM2-2ZE"),
            "[A123X]型号" to listOf("A123X"),
            "\"B456Z\"产品" to listOf("B456Z"),
            "无型号产品" to emptyList()
        )
        
        testCases.forEach { (input, expected) ->
            val extracted = ModelExtractor.extractModels(input)
            val success = extracted.containsAll(expected) && expected.containsAll(extracted)
            
            Log.d(TAG, "输入: '$input'")
            Log.d(TAG, "期望: $expected")
            Log.d(TAG, "实际: $extracted")
            Log.d(TAG, "结果: ${if (success) "✓ 通过" else "✗ 失败"}\n")
        }
    }
    
    /**
     * 测试型号匹配功能
     */
    private fun testModelMatching() {
        Log.d(TAG, "\n--- 测试型号匹配功能 ---")
        
        val matchTestCases = listOf(
            // API商品名 to 仓库商品名 to 期望匹配的型号
            Triple("CHM2-2ZE控制器", "智能控制器CHM2-2ZE", "CHM2-2ZE"),
            Triple("ABC1-5X开关", "开关型号ABC1-5X", "ABC1-5X"),
            Triple("新品CHM2-2", "CHM2-2ZE控制器", null), // 型号不完全匹配
            Triple("MH412A产品", "MH-412A型号", "MH412A"), // 模糊匹配（去连字符）
            Triple("iPhone 14", "iPhone 14 Pro", "IPHONE 14"), // 包含关系
            Triple("普通产品", "另一个产品", null), // 无型号匹配
            Triple("A123产品", "B456产品", null) // 不同型号
        )
        
        matchTestCases.forEach { (apiName, localName, expectedModel) ->
            val matchedModel = ModelExtractor.findMatchingModel(apiName, localName)
            val success = (expectedModel == null && matchedModel == null) || 
                         (expectedModel != null && matchedModel?.equals(expectedModel, ignoreCase = true) == true)
            
            Log.d(TAG, "API: '$apiName'")
            Log.d(TAG, "仓库: '$localName'")
            Log.d(TAG, "期望匹配: $expectedModel")
            Log.d(TAG, "实际匹配: $matchedModel")
            Log.d(TAG, "结果: ${if (success) "✓ 通过" else "✗ 失败"}\n")
        }
    }
    
    /**
     * 测试真实场景
     */
    private fun testRealWorldScenarios() {
        Log.d(TAG, "\n--- 测试真实场景 ---")
        
        // 创建测试商品库存
        val testGoods = listOf(
            GoodsEntity("1", "CHM2-2ZE智能控制器", "1", "工业级", "123456789", 50, 5, null, 150.0, 200.0, false, System.currentTimeMillis()),
            GoodsEntity("2", "ABC1-5X开关模块", "1", "家用级", "123456790", 30, 5, null, 80.0, 120.0, false, System.currentTimeMillis()),
            GoodsEntity("3", "MH4-12A传感器", "2", "精密型", "123456791", 20, 3, null, 300.0, 450.0, false, System.currentTimeMillis()),
            GoodsEntity("4", "iPhone 14", "3", "128GB", "123456792", 10, 2, null, 5999.0, 6999.0, false, System.currentTimeMillis()),
            GoodsEntity("5", "小米13手机", "3", "256GB", "123456793", 15, 3, null, 3999.0, 4499.0, false, System.currentTimeMillis())
        )
        
        val testScenarios = listOf(
            "CHM2-2ZE控制器" to "1", // 应该匹配CHM2-2ZE智能控制器
            "ABC1-5X开关" to "2", // 应该匹配ABC1-5X开关模块
            "MH4-12A型传感器" to "3", // 应该匹配MH4-12A传感器
            "iPhone 14 Pro" to "4", // 应该匹配iPhone 14
            "小米13 Ultra" to "5", // 应该匹配小米13手机
            "CHM3-3ZE产品" to null, // 不应该匹配任何商品
            "未知产品ABC" to null // 不应该匹配任何商品
        )
        
        testScenarios.forEach { (apiName, expectedId) ->
            val result = FuzzyMatchCache.findBestMatchWithCache(apiName, "", testGoods, 0.5)
            val actualId = result?.goods?.id
            val success = expectedId == actualId
            
            Log.d(TAG, "API商品: '$apiName'")
            Log.d(TAG, "期望匹配ID: $expectedId")
            Log.d(TAG, "实际匹配ID: $actualId")
            if (result != null) {
                Log.d(TAG, "匹配商品: ${result.goods.name}")
                Log.d(TAG, "匹配得分: ${String.format("%.3f", result.score)}")
                Log.d(TAG, "匹配类型: ${result.matchType}")
            }
            Log.d(TAG, "结果: ${if (success) "✓ 通过" else "✗ 失败"}\n")
        }
    }
    
    /**
     * 测试型号提取的边界情况
     */
    fun testEdgeCases() {
        Log.d(TAG, "\n--- 测试边界情况 ---")
        
        val edgeCases = mapOf(
            "" to emptyList(), // 空字符串
            "   " to emptyList(), // 只有空格
            "abc" to emptyList(), // 太短无型号
            "123" to emptyList(), // 纯数字太短
            "A1B2C3D4E5F6G7H8" to emptyList(), // 太长不规范
            "CHM2-2ZE-Extra" to listOf("CHM2-2ZE"), // 包含额外内容
            "CHM2-2ZE和ABC1-5X" to listOf("CHM2-2ZE", "ABC1-5X"), // 多个型号
            "型号：CHM2-2ZE，规格说明" to listOf("CHM2-2ZE"), // 中文混合
        )
        
        edgeCases.forEach { (input, expected) ->
            val extracted = ModelExtractor.extractModels(input)
            val success = extracted.size == expected.size && extracted.containsAll(expected)
            
            Log.d(TAG, "输入: '$input' -> 提取: $extracted (期望: $expected) ${if (success) "✓" else "✗"}")
        }
    }
    
    /**
     * 性能测试
     */
    fun performanceTest() {
        Log.d(TAG, "\n--- 性能测试 ---")
        
        val testData = (1..1000).map { "测试商品CHM$it-${it}ZE" }
        
        val startTime = System.currentTimeMillis()
        testData.forEach { ModelExtractor.extractModels(it) }
        val endTime = System.currentTimeMillis()
        
        val avgTime = (endTime - startTime).toDouble() / testData.size
        Log.d(TAG, "处理${testData.size}个商品名耗时: ${endTime - startTime}ms")
        Log.d(TAG, "平均每个商品耗时: ${String.format("%.2f", avgTime)}ms")
    }
}



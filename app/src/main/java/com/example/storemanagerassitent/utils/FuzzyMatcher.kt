package com.example.storemanagerassitent.utils

import android.util.Log
import com.example.storemanagerassitent.data.db.GoodsEntity
import com.example.storemanagerassitent.data.Goods
import kotlin.math.max
import kotlin.math.min

/**
 * 统一的模糊匹配工具类
 * 提供多种匹配策略和评分机制
 */
object FuzzyMatcher {

    private const val TAG = "FuzzyMatcher"
    
    // 匹配结果数据类
    data class MatchResult(
        val goods: GoodsEntity,
        val score: Double,
        val matchType: MatchType
    )
    
    enum class MatchType {
        EXACT_BARCODE,    // 精确条码匹配
        MODEL_MATCH,      // 型号匹配（最高优先级）
        SPEC_CONTAINS,    // 规格包含匹配
        NAME_CONTAINS,    // 名称包含匹配
        FUZZY_SIMILAR,    // 模糊相似匹配
        EDIT_DISTANCE     // 编辑距离匹配
    }
    
    /**
     * 查找最佳匹配商品
     * @param apiName API返回的商品名称
     * @param apiSpec API返回的规格
     * @param localGoods 本地商品列表
     * @param threshold 匹配阈值 (0.0 - 1.0)
     * @return 匹配结果，null表示无匹配
     */
    fun findBestMatch(
        apiName: String, 
        apiSpec: String, 
        localGoods: List<GoodsEntity>,
        threshold: Double = 0.6
    ): MatchResult? {
        if (localGoods.isEmpty()) return null
        
        val cleanApiName = apiName.trim().lowercase()
        val cleanApiSpec = apiSpec.trim().lowercase()
        val apiCombined = listOfNotNull(
            cleanApiName.takeIf { it.isNotBlank() },
            cleanApiSpec.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        
        if (apiCombined.isBlank()) return null
        
        val candidates = mutableListOf<MatchResult>()
        
        localGoods.forEach { product ->
            val localName = product.name.trim().lowercase()
            val localSpec = (product.specifications ?: "").trim().lowercase()
            val localCombined = listOfNotNull(
                localName.takeIf { it.isNotBlank() },
                localSpec.takeIf { it.isNotBlank() }
            ).joinToString(" ")
            
            if (localCombined.isBlank()) return@forEach
            
            // 尝试多种匹配策略（按优先级排序）
            val matchResults = listOfNotNull(
                tryModelMatch(apiCombined, localCombined, product),
                trySpecContainsMatch(cleanApiSpec, localCombined, product),
                tryNameContainsMatch(apiCombined, localCombined, product),
                tryFuzzySimilarMatch(apiCombined, localCombined, product),
                tryEditDistanceMatch(apiCombined, localCombined, product)
            )
            
            // 选择得分最高的匹配
            matchResults.maxByOrNull { it.score }?.let { bestMatch ->
                if (bestMatch.score >= threshold) {
                    candidates.add(bestMatch)
                }
            }
        }
        
        // 返回得分最高的候选
        val best = candidates.maxByOrNull { it.score }
        if (best != null) {
            Log.d(TAG, "Best match found: id=${best.goods.id} name='${best.goods.name}' " +
                "score=${best.score} type=${best.matchType}")
        } else {
            Log.d(TAG, "No match found with threshold $threshold")
        }
        
        return best
    }
    
    /**
     * 型号匹配 - 最高优先级匹配策略
     */
    private fun tryModelMatch(
        apiCombined: String,
        localCombined: String,
        product: GoodsEntity
    ): MatchResult? {
        // 使用 ModelExtractor 查找匹配的型号
        val matchingModel = ModelExtractor.findMatchingModel(apiCombined, localCombined)
        
        return if (matchingModel != null) {
            // 型号匹配给予最高分数：0.95-1.0
            val score = when {
                // 完全相同的型号
                apiCombined.contains(matchingModel, ignoreCase = true) && 
                localCombined.contains(matchingModel, ignoreCase = true) -> 1.0
                
                // 模糊匹配的型号
                else -> 0.95
            }
            
            Log.d(TAG, "Model match found: '$matchingModel' for product ${product.name} with score $score")
            MatchResult(product, score, MatchType.MODEL_MATCH)
        } else {
            null
        }
    }
    
    /**
     * 规格包含匹配
     */
    private fun trySpecContainsMatch(
        apiSpec: String, 
        localCombined: String, 
        product: GoodsEntity
    ): MatchResult? {
        if (apiSpec.isBlank()) return null
        
        return if (localCombined.contains(apiSpec)) {
            // 完全包含给高分，但要低于型号匹配
            val coverage = apiSpec.length.toDouble() / localCombined.length
            val score = 0.85 + (coverage * 0.05) // 0.85-0.9分
            MatchResult(product, score, MatchType.SPEC_CONTAINS)
        } else {
            null
        }
    }
    
    /**
     * 名称包含匹配 (改进的长度判断)
     */
    private fun tryNameContainsMatch(
        apiCombined: String, 
        localCombined: String, 
        product: GoodsEntity
    ): MatchResult? {
        val minLength = min(apiCombined.length, localCombined.length)
        val maxLength = max(apiCombined.length, localCombined.length)
        
        // 动态阈值：短文本要求更高的匹配度
        val lengthRatio = minLength.toDouble() / maxLength
        val requiredRatio = when {
            minLength < 5 -> 0.8  // 短文本要求80%匹配
            minLength < 10 -> 0.7 // 中等文本70%
            else -> 0.6          // 长文本60%
        }
        
        return when {
            lengthRatio >= requiredRatio && apiCombined.contains(localCombined) -> {
                MatchResult(product, 0.65 + lengthRatio * 0.15, MatchType.NAME_CONTAINS) // 0.65-0.8分
            }
            lengthRatio >= requiredRatio && localCombined.contains(apiCombined) -> {
                MatchResult(product, 0.55 + lengthRatio * 0.15, MatchType.NAME_CONTAINS) // 0.55-0.7分
            }
            else -> null
        }
    }
    
    /**
     * 基于Jaccard相似度的模糊匹配
     */
    private fun tryFuzzySimilarMatch(
        apiCombined: String, 
        localCombined: String, 
        product: GoodsEntity
    ): MatchResult? {
        val similarity = calculateJaccardSimilarity(apiCombined, localCombined)
        return if (similarity > 0.3) {
            MatchResult(product, similarity * 0.5, MatchType.FUZZY_SIMILAR) // 最高0.5分
        } else {
            null
        }
    }
    
    /**
     * 编辑距离匹配
     */
    private fun tryEditDistanceMatch(
        apiCombined: String, 
        localCombined: String, 
        product: GoodsEntity
    ): MatchResult? {
        val maxLength = max(apiCombined.length, localCombined.length)
        if (maxLength == 0) return null
        
        val editDistance = calculateEditDistance(apiCombined, localCombined)
        val similarity = 1.0 - (editDistance.toDouble() / maxLength)
        
        return if (similarity > 0.5) {
            MatchResult(product, similarity * 0.45, MatchType.EDIT_DISTANCE) // 最高0.45分
        } else {
            null
        }
    }
    
    /**
     * 计算Jaccard相似度 (基于字符n-gram)
     */
    private fun calculateJaccardSimilarity(str1: String, str2: String, n: Int = 2): Double {
        if (str1 == str2) return 1.0
        if (str1.isEmpty() && str2.isEmpty()) return 1.0
        if (str1.isEmpty() || str2.isEmpty()) return 0.0
        
        val grams1 = generateNGrams(str1, n).toSet()
        val grams2 = generateNGrams(str2, n).toSet()
        
        val intersection = grams1.intersect(grams2).size
        val union = grams1.union(grams2).size
        
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }
    
    /**
     * 生成n-gram
     */
    private fun generateNGrams(text: String, n: Int): List<String> {
        if (text.length < n) return listOf(text)
        return (0..text.length - n).map { text.substring(it, it + n) }
    }
    
    /**
     * 计算编辑距离 (Levenshtein Distance)
     */
    private fun calculateEditDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length
        
        // 创建距离矩阵
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        // 初始化边界条件
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        // 填充距离矩阵
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // 删除
                    dp[i][j - 1] + 1,      // 插入
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * 兼容性方法：适配现有的Goods类型
     */
    fun findBestMatch(
        apiName: String, 
        apiSpec: String, 
        localGoods: List<Goods>,
        threshold: Double = 0.6
    ): Goods? {
        // 转换为GoodsEntity进行匹配
        val entities = localGoods.map { goods ->
            GoodsEntity(
                id = goods.id,
                name = goods.name,
                categoryId = goods.category, // Goods使用category，GoodsEntity使用categoryId
                specifications = goods.specifications,
                barcode = null, // Goods没有barcode字段
                stockQuantity = goods.stockQuantity,
                lowStockThreshold = goods.lowStockThreshold,
                imageUrl = goods.imageUrl,
                purchasePrice = goods.purchasePrice,
                retailPrice = goods.retailPrice,
                isDelisted = goods.isDelisted,
                lastUpdated = goods.lastUpdated
            )
        }
        
        return findBestMatch(apiName, apiSpec, entities, threshold)?.let { result ->
            // 从原始列表中找到对应的Goods
            localGoods.find { it.id == result.goods.id }
        }
    }
}

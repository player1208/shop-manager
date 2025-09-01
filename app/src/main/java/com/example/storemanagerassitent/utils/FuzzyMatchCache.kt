package com.example.storemanagerassitent.utils

import android.util.Log
import com.example.storemanagerassitent.data.db.GoodsEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * 模糊匹配缓存和索引管理器
 * 提供高性能的商品查找和匹配
 */
object FuzzyMatchCache {
    
    private const val TAG = "FuzzyMatchCache"
    private const val MAX_CACHE_SIZE = 1000
    private const val MAX_INDEX_TOKENS = 5000
    
    // LRU缓存 - 存储最近的匹配结果
    private val matchCache = object : LinkedHashMap<String, FuzzyMatcher.MatchResult?>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FuzzyMatcher.MatchResult?>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    
            // 倒排索引 - token -> 包含该token的商品列表
        private val invertedIndex = ConcurrentHashMap<String, MutableSet<String>>()

        // 商品ID到商品的映射
        private val goodsMap = ConcurrentHashMap<String, GoodsEntity>()
    
    // 索引版本号，用于判断是否需要重建索引
    private var indexVersion = 0L
    private var lastGoodsHash = 0
    
    /**
     * 构建或更新索引
     */
    fun buildIndex(goods: List<GoodsEntity>) {
        val currentHash = goods.hashCode()
        if (currentHash == lastGoodsHash && invertedIndex.isNotEmpty()) {
            Log.d(TAG, "Index up to date, skipping rebuild")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        // 清空现有索引
        invertedIndex.clear()
        goodsMap.clear()
        matchCache.clear()
        
        // 构建新索引
        goods.forEach { product ->
            goodsMap[product.id] = product
            
            // 提取所有可搜索的token
            val tokens = extractTokens(product)
            tokens.forEach { token ->
                invertedIndex.computeIfAbsent(token) { mutableSetOf() }.add(product.id)
            }
        }
        
        // 限制索引大小，避免内存过度使用
        if (invertedIndex.size > MAX_INDEX_TOKENS) {
            val sortedTokens = invertedIndex.toList().sortedByDescending { it.second.size }
            invertedIndex.clear()
            sortedTokens.take(MAX_INDEX_TOKENS).forEach { (token, productIds) ->
                invertedIndex[token] = productIds
            }
            Log.w(TAG, "Index size limited to $MAX_INDEX_TOKENS tokens")
        }
        
        lastGoodsHash = currentHash
        indexVersion = System.currentTimeMillis()
        
        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "Index built in ${duration}ms: ${goods.size} products, ${invertedIndex.size} tokens")
    }
    
    /**
     * 使用缓存和索引进行快速匹配
     */
    fun findBestMatchWithCache(
        apiName: String,
        apiSpec: String,
        allGoods: List<GoodsEntity>,
        threshold: Double = 0.6
    ): FuzzyMatcher.MatchResult? {
        // 构建缓存键
        val cacheKey = "${apiName.trim().lowercase()}|${apiSpec.trim().lowercase()}|$threshold"
        
        // 检查缓存
        if (matchCache.containsKey(cacheKey)) {
            Log.d(TAG, "Cache hit for: $cacheKey")
            return matchCache[cacheKey]
        }
        
        // 确保索引是最新的
        buildIndex(allGoods)
        
        // 获取候选商品 (使用索引优化)
        val candidates = getCandidateProducts(apiName, apiSpec, allGoods.size)
        
        Log.d(TAG, "Found ${candidates.size} candidates from ${allGoods.size} total products")
        
        // 在候选商品中进行匹配
        val result = FuzzyMatcher.findBestMatch(
            apiName, 
            apiSpec, 
            candidates, 
            threshold
        )
        
        // 缓存结果
        matchCache[cacheKey] = result
        
        return result
    }
    
    /**
     * 基于倒排索引获取候选商品
     */
    private fun getCandidateProducts(apiName: String, apiSpec: String, totalSize: Int): List<GoodsEntity> {
        if (invertedIndex.isEmpty()) {
            // 如果没有索引，返回所有商品
            return goodsMap.values.toList()
        }
        
        val searchTokens = extractSearchTokens(apiName, apiSpec)
        if (searchTokens.isEmpty()) {
            return goodsMap.values.toList()
        }
        
        // 为每个token收集相关商品ID
        val candidateScores = mutableMapOf<String, Int>()
        
        searchTokens.forEach { token ->
            invertedIndex[token]?.forEach { productId ->
                candidateScores[productId] = (candidateScores[productId] ?: 0) + 1
            }
        }
        
        // 如果候选商品太少，进行模糊token匹配
        if (candidateScores.size < minOf(20, totalSize / 10)) {
            val fuzzyMatches = findFuzzyTokenMatches(searchTokens)
            fuzzyMatches.forEach { (productId, score) ->
                candidateScores[productId] = maxOf(candidateScores[productId] ?: 0, score)
            }
        }
        
        // 按得分排序，取前50%或至少20个候选
        val minCandidates = minOf(20, totalSize)
        val maxCandidates = maxOf(minCandidates, totalSize / 2)
        
        val sortedCandidates = candidateScores.toList()
            .sortedByDescending { it.second }
            .take(maxCandidates)
            .mapNotNull { (productId, _) -> goodsMap[productId] }
        
        return sortedCandidates.ifEmpty { goodsMap.values.toList() }
    }
    
    /**
     * 模糊token匹配 - 当精确匹配结果太少时使用
     */
    private fun findFuzzyTokenMatches(searchTokens: List<String>): Map<String, Int> {
        val matches = mutableMapOf<String, Int>()
        
        searchTokens.forEach { searchToken ->
            invertedIndex.keys.forEach { indexToken ->
                if (indexToken != searchToken && isTokenSimilar(searchToken, indexToken)) {
                    invertedIndex[indexToken]?.forEach { productId ->
                        matches[productId] = (matches[productId] ?: 0) + 1
                    }
                }
            }
        }
        
        return matches
    }
    
    /**
     * 判断两个token是否相似
     */
    private fun isTokenSimilar(token1: String, token2: String): Boolean {
        if (token1.length < 2 || token2.length < 2) return false
        
        // 简单的相似度判断：
        // 1. 包含关系
        if (token1.contains(token2) || token2.contains(token1)) return true
        
        // 2. 编辑距离
        val maxLen = maxOf(token1.length, token2.length)
        val distance = calculateEditDistance(token1, token2)
        
        return distance.toDouble() / maxLen < 0.3
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
     * 从商品中提取所有可搜索的token
     */
    private fun extractTokens(product: GoodsEntity): Set<String> {
        val tokens = mutableSetOf<String>()
        
        // 商品名称
        product.name.split("\\s+".toRegex()).forEach { word ->
            val cleaned = word.trim().lowercase()
            if (cleaned.length >= 2) {
                tokens.add(cleaned)
                // 添加前缀匹配
                if (cleaned.length >= 3) {
                    for (i in 2..minOf(cleaned.length, 5)) {
                        tokens.add(cleaned.substring(0, i))
                    }
                }
            }
        }
        
        // 规格信息
        product.specifications?.split("\\s+".toRegex())?.forEach { word ->
            val cleaned = word.trim().lowercase()
            if (cleaned.length >= 2) {
                tokens.add(cleaned)
            }
        }
        
        // 条码 (完整)
        product.barcode?.takeIf { it.isNotBlank() }?.let { tokens.add(it) }
        
        return tokens
    }
    
    /**
     * 从搜索文本中提取token
     */
    private fun extractSearchTokens(apiName: String, apiSpec: String): List<String> {
        val tokens = mutableListOf<String>()
        
        // 处理名称
        apiName.trim().split("\\s+".toRegex()).forEach { word ->
            val cleaned = word.lowercase()
            if (cleaned.length >= 2) {
                tokens.add(cleaned)
            }
        }
        
        // 处理规格
        apiSpec.trim().split("\\s+".toRegex()).forEach { word ->
            val cleaned = word.lowercase()
            if (cleaned.length >= 2) {
                tokens.add(cleaned)
            }
        }
        
        return tokens.distinct()
    }
    
    /**
     * 清空缓存
     */
    fun clearCache() {
        matchCache.clear()
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): String {
        return "Cache: ${matchCache.size}/$MAX_CACHE_SIZE, " +
               "Index: ${invertedIndex.size} tokens, " +
               "Products: ${goodsMap.size}, " +
               "Version: $indexVersion"
    }
}

package com.example.storemanagerassitent.utils

import android.util.Log
import java.util.regex.Pattern

/**
 * 商品型号提取器
 * 专门用于从商品名称中提取型号信息，支持多种格式
 */
object ModelExtractor {
    
    private const val TAG = "ModelExtractor"
    
    // 常见型号格式的正则表达式（按优先级排序）
    private val modelPatterns = listOf(
        // 1. 字母+数字+连字符+数字+字母 格式：CHM2-2ZE, ABC1-5X, MH4-12A
        Pattern.compile("[A-Z]{2,5}\\d{1,3}-\\d{1,3}[A-Z]{1,3}", Pattern.CASE_INSENSITIVE),
        
        // 2. 字母+数字+连字符+数字 格式：CHM2-2, ABC1-5, MH4-12
        Pattern.compile("[A-Z]{2,5}\\d{1,3}-\\d{1,3}", Pattern.CASE_INSENSITIVE),
        
        // 3. 字母+连字符+数字+字母 格式：CH-2ZE, AB-5X, MH-12A
        Pattern.compile("[A-Z]{2,5}-\\d{1,3}[A-Z]{1,3}", Pattern.CASE_INSENSITIVE),
        
        // 4. 字母+连字符+数字 格式：CH-2, AB-5, MH-12
        Pattern.compile("[A-Z]{2,5}-\\d{1,3}", Pattern.CASE_INSENSITIVE),
        
        // 5. 字母+数字+字母 格式：CHM22ZE, ABC15X, MH412A
        Pattern.compile("[A-Z]{2,5}\\d{2,4}[A-Z]{1,3}", Pattern.CASE_INSENSITIVE),
        
        // 6. 字母+数字 格式：CHM22, ABC15, MH412
        Pattern.compile("[A-Z]{2,5}\\d{2,4}", Pattern.CASE_INSENSITIVE),
        
        // 7. 纯数字型号（带连字符）格式：123-456, 1234-5678
        Pattern.compile("\\d{3,4}-\\d{3,4}"),
        
        // 8. iPhone等特殊格式：iPhone 14, iPad Air, MacBook Pro
        Pattern.compile("(iPhone|iPad|MacBook|iMac|AirPods)\\s*\\w*\\s*\\d{1,2}", Pattern.CASE_INSENSITIVE),
        
        // 9. 小米等中文品牌格式：小米13, 华为P50, OPPO Find
        Pattern.compile("(小米|华为|OPPO|vivo|魅族|一加)\\s*[A-Z]*\\d{1,3}", Pattern.CASE_INSENSITIVE),
        
        // 10. 通用数字+字母组合：A123, B456X, C789ZE
        Pattern.compile("[A-Z]\\d{3,4}[A-Z]*", Pattern.CASE_INSENSITIVE)
    )
    
    /**
     * 从商品名称中提取所有可能的型号
     * @param productName 商品名称
     * @return 提取到的型号列表，按匹配优先级排序
     */
    fun extractModels(productName: String): List<String> {
        if (productName.isBlank()) return emptyList()
        
        val extractedModels = mutableListOf<String>()
        val cleanedName = productName.trim()
        
        modelPatterns.forEachIndexed { index, pattern ->
            val matcher = pattern.matcher(cleanedName)
            while (matcher.find()) {
                val model = matcher.group().trim().uppercase()
                if (model.isNotBlank() && !extractedModels.contains(model)) {
                    extractedModels.add(model)
                    Log.d(TAG, "Pattern ${index + 1} matched: '$model' from '$cleanedName'")
                }
            }
        }
        
        // 额外尝试提取括号、引号内的内容
        extractFromDelimiters(cleanedName)?.let { models ->
            models.forEach { model ->
                if (!extractedModels.contains(model)) {
                    extractedModels.add(model)
                    Log.d(TAG, "Delimiter extracted: '$model' from '$cleanedName'")
                }
            }
        }
        
        Log.d(TAG, "Extracted models from '$cleanedName': $extractedModels")
        return extractedModels
    }
    
    /**
     * 从括号、引号等分隔符中提取型号
     */
    private fun extractFromDelimiters(text: String): List<String>? {
        val results = mutableListOf<String>()
        
        // 提取括号内容：产品名称(CHM2-2ZE)
        val bracketPattern = Pattern.compile("\\(([^)]+)\\)")
        val bracketMatcher = bracketPattern.matcher(text)
        while (bracketMatcher.find()) {
            val content = bracketMatcher.group(1)?.trim()?.uppercase()
            if (content != null && content.matches("[A-Z0-9-]{3,15}".toRegex())) {
                results.add(content)
            }
        }
        
        // 提取方括号内容：产品名称[CHM2-2ZE]
        val squareBracketPattern = Pattern.compile("\\[([^]]+)]")
        val squareBracketMatcher = squareBracketPattern.matcher(text)
        while (squareBracketMatcher.find()) {
            val content = squareBracketMatcher.group(1)?.trim()?.uppercase()
            if (content != null && content.matches("[A-Z0-9-]{3,15}".toRegex())) {
                results.add(content)
            }
        }
        
        // 提取引号内容："CHM2-2ZE"
        val quotePattern = Pattern.compile("[\"']([^\"']+)[\"']")
        val quoteMatcher = quotePattern.matcher(text)
        while (quoteMatcher.find()) {
            val content = quoteMatcher.group(1)?.trim()?.uppercase()
            if (content != null && content.matches("[A-Z0-9-]{3,15}".toRegex())) {
                results.add(content)
            }
        }
        
        return results.takeIf { it.isNotEmpty() }
    }
    
    /**
     * 检查两个商品名称是否包含相同的型号
     * @param apiProductName API返回的商品名称
     * @param localProductName 本地仓库的商品名称
     * @return 匹配的型号，如果没有匹配返回null
     */
    fun findMatchingModel(apiProductName: String, localProductName: String): String? {
        val apiModels = extractModels(apiProductName)
        val localModels = extractModels(localProductName)
        
        if (apiModels.isEmpty() || localModels.isEmpty()) {
            Log.d(TAG, "No models found - API: $apiModels, Local: $localModels")
            return null
        }
        
        // 精确匹配
        apiModels.forEach { apiModel ->
            localModels.forEach { localModel ->
                if (apiModel.equals(localModel, ignoreCase = true)) {
                    Log.d(TAG, "Exact model match found: '$apiModel' == '$localModel'")
                    return apiModel
                }
            }
        }
        
        // 模糊匹配（去掉连字符后比较）
        apiModels.forEach { apiModel ->
            val apiModelClean = apiModel.replace("-", "").replace(" ", "")
            localModels.forEach { localModel ->
                val localModelClean = localModel.replace("-", "").replace(" ", "")
                if (apiModelClean.equals(localModelClean, ignoreCase = true) && apiModelClean.length >= 3) {
                    Log.d(TAG, "Fuzzy model match found: '$apiModel' ~= '$localModel'")
                    return apiModel
                }
            }
        }
        
        // 包含关系匹配（较短的型号包含在较长的型号中）
        apiModels.forEach { apiModel ->
            localModels.forEach { localModel ->
                if (apiModel.length >= 4 && localModel.length >= 4) {
                    if (apiModel.contains(localModel, ignoreCase = true) || 
                        localModel.contains(apiModel, ignoreCase = true)) {
                        Log.d(TAG, "Containment model match found: '$apiModel' <-> '$localModel'")
                        return apiModel
                    }
                }
            }
        }
        
        Log.d(TAG, "No model match found - API: $apiModels, Local: $localModels")
        return null
    }
    
    /**
     * 验证提取的型号是否有效
     */
    fun isValidModel(model: String): Boolean {
        if (model.length < 3) return false
        
        // 至少包含一个数字或字母
        if (!model.matches(".*[A-Za-z0-9].*".toRegex())) return false
        
        // 不能全是数字（除非是特定格式）
        if (model.matches("\\d+".toRegex()) && model.length < 4) return false
        
        return true
    }
    
    /**
     * 获取所有支持的型号格式示例（用于调试）
     */
    fun getSupportedFormats(): List<String> {
        return listOf(
            "CHM2-2ZE (字母数字-数字字母)",
            "CHM2-2 (字母数字-数字)",
            "CH-2ZE (字母-数字字母)",
            "CH-2 (字母-数字)",
            "CHM22ZE (字母数字字母)",
            "CHM22 (字母数字)",
            "123-456 (数字-数字)",
            "iPhone 14 (特殊品牌格式)",
            "小米13 (中文品牌格式)",
            "A123X (通用格式)"
        )
    }
}

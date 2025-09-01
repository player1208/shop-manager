package com.example.storemanagerassitent.ocr

import android.util.Log
import com.example.storemanagerassitent.data.ReviewableItem
import java.util.regex.Pattern

/**
 * 文字解析器
 * 将OCR识别的文字解析成商品信息
 */
class TextParser {
    
    companion object {
        private const val TAG = "TextParser"
        
        // 价格匹配正则表达式
        private val PRICE_PATTERN = Pattern.compile(
            "(?:[¥￥$]|价格?[：:]?)\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        
        // 数量匹配正则表达式  
        private val QUANTITY_PATTERN = Pattern.compile(
            "(?:数量|qty|x|×|数目)[：:]?\\s*([0-9]+)(?:[个件盒箱包袋])?",
            Pattern.CASE_INSENSITIVE
        )
        
        // 价格×数量格式匹配 - 按照文档要求的精确格式
        private val PRICE_QUANTITY_PATTERN = Pattern.compile(
            "([¥￥])([0-9]+(?:\\.[0-9]{1,2})?)x([0-9]+)([个件盒箱包袋])",
            Pattern.CASE_INSENSITIVE
        )
        
        // 备用的价格×数量格式匹配（更宽松的格式）
        private val PRICE_QUANTITY_PATTERN_LOOSE = Pattern.compile(
            "(?:[¥￥$]?)([0-9]+(?:\\.[0-9]{1,2})?)\\s*[x×*]\\s*([0-9]+)(?:[个件盒箱包袋])?",
            Pattern.CASE_INSENSITIVE
        )
        
        // 总价匹配
        private val TOTAL_PATTERN = Pattern.compile(
            "(?:总价?[：:]?|合计[：:]?|小计[：:]?)\\s*(?:[¥￥$]?)([0-9]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        
        // 商品编码匹配
        private val CODE_PATTERN = Pattern.compile(
            "(?:编码|代码|货号|SKU)[：:]?\\s*([A-Za-z0-9\\-_]+)",
            Pattern.CASE_INSENSITIVE
        )
        
        // 常见的无关文字（需要过滤）
        private val IRRELEVANT_KEYWORDS = setOf(
            "收银台", "找零", "支付", "微信", "支付宝", "现金", "银行卡",
            "总计", "应收", "实收", "优惠", "折扣", "会员", "积分",
            "欢迎光临", "谢谢惠顾", "小票", "发票", "收据"
        )
    }
    
    /**
     * 解析OCR结果为商品列表
     */
    fun parseToGoodsItems(ocrResult: OcrProcessor.OcrResult): List<ReviewableItem> {
        if (!ocrResult.success || ocrResult.recognizedText.isBlank()) {
            Log.w(TAG, "OCR result is empty or failed")
            return emptyList()
        }
        
        Log.d(TAG, "Starting to parse OCR text: ${ocrResult.recognizedText}")
        
        // 按行分割文本
        val lines = ocrResult.recognizedText.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        Log.d(TAG, "Split into ${lines.size} lines")
        
        // 检测商品区块
        val productBlocks = detectProductBlocks(lines)
        Log.d(TAG, "Detected ${productBlocks.size} product blocks")
        
        // 解析每个商品区块
        val items = productBlocks.mapNotNull { block ->
            parseProductBlock(block)
        }
        
        Log.d(TAG, "Successfully parsed ${items.size} items")
        return items
    }
    
    /**
     * 检测商品区块
     * 根据空行和视觉布局将文本分割成独立的商品区块
     */
    private fun detectProductBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var currentBlock = mutableListOf<String>()
        
        for (line in lines) {
            // 跳过无关的行
            if (isIrrelevantLine(line)) {
                continue
            }
            
            // 如果当前行包含商品特征，开始新的区块
            if (looksLikeProductStart(line) && currentBlock.isNotEmpty()) {
                blocks.add(currentBlock.toList())
                currentBlock = mutableListOf()
            }
            
            currentBlock.add(line)
        }
        
        // 添加最后一个区块
        if (currentBlock.isNotEmpty()) {
            blocks.add(currentBlock)
        }
        
        return blocks
    }
    
    /**
     * 判断是否为无关行
     */
    private fun isIrrelevantLine(line: String): Boolean {
        return IRRELEVANT_KEYWORDS.any { keyword ->
            line.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * 判断是否像商品开始行
     */
    private fun looksLikeProductStart(line: String): Boolean {
        // 包含精确价格×数量格式的行通常是商品行
        if (PRICE_QUANTITY_PATTERN.matcher(line).find()) return true
        if (PRICE_QUANTITY_PATTERN_LOOSE.matcher(line).find()) return true
        
        // 包含价格信息的行通常是商品行
        if (PRICE_PATTERN.matcher(line).find()) return true
        
        // 包含中文商品名称的行
        if (line.matches(Regex(".*[\\u4e00-\\u9fa5]{2,}.*"))) return true
        
        return false
    }
    
    /**
     * 解析单个商品区块
     */
    private fun parseProductBlock(block: List<String>): ReviewableItem? {
        if (block.isEmpty()) return null
        
        val combinedText = block.joinToString(" ")
        Log.d(TAG, "Parsing product block: $combinedText")
        
        // 提取商品名称
        val productName = extractProductName(block)
        if (productName.isBlank()) {
            Log.w(TAG, "Could not extract product name from block: $combinedText")
            return null
        }
        
        // 提取价格和数量
        val (price, quantity) = extractPriceAndQuantity(combinedText)
        
        // 提取规格
        val specifications = extractSpecifications(block, productName)
        
        // 提取商品编码
        val productCode = extractProductCode(combinedText)
        
        // 交叉验证与容错：使用区块右侧独立显示的总价进行验证
        val crossValidationResult = performCrossValidation(combinedText, price, quantity)
        
        // 计算置信度
        val confidence = calculateParsingConfidence(productName, price, quantity, combinedText, crossValidationResult)
        
        val finalName = if (productCode.isNotBlank()) {
            "$productName (编码: $productCode)"
        } else {
            productName
        }
        
        Log.d(TAG, "Parsed item: name=$finalName, price=$price, quantity=$quantity, confidence=$confidence")
        
        return ReviewableItem(
            recognizedName = finalName,
            recognizedSpecifications = specifications,
            recognizedPrice = price,
            recognizedQuantity = quantity,
            confidence = confidence
        )
    }
    
    /**
     * 提取商品名称
     */
    private fun extractProductName(block: List<String>): String {
        // 通常第一行包含商品名称
        for (line in block) {
            // 去除价格、数量等信息，提取纯商品名称
            var cleanLine = line
            
            // 移除价格信息
            cleanLine = PRICE_PATTERN.matcher(cleanLine).replaceAll("")
            cleanLine = PRICE_QUANTITY_PATTERN.matcher(cleanLine).replaceAll("")
            cleanLine = PRICE_QUANTITY_PATTERN_LOOSE.matcher(cleanLine).replaceAll("")
            cleanLine = TOTAL_PATTERN.matcher(cleanLine).replaceAll("")
            
            // 移除数量信息
            cleanLine = QUANTITY_PATTERN.matcher(cleanLine).replaceAll("")
            
            // 移除商品编码
            cleanLine = CODE_PATTERN.matcher(cleanLine).replaceAll("")
            
            cleanLine = cleanLine.trim()
            
            // 如果清理后的行包含中文且长度合适，认为是商品名称
            if (cleanLine.isNotBlank() && 
                cleanLine.matches(Regex(".*[\\u4e00-\\u9fa5]+.*")) && 
                cleanLine.length >= 2) {
                return cleanLine
            }
        }
        
        // 如果没有找到合适的商品名称，返回第一行的前半部分
        return block.firstOrNull()?.take(10) ?: ""
    }
    
    /**
     * 提取价格和数量
     * 按照文档要求：优先查找 (¥|￥)[\d\.]+x\d+(个|件|盒|箱) 格式
     */
    private fun extractPriceAndQuantity(text: String): Pair<Double, Int> {
        var price = 0.0
        var quantity = 1
        
        // 第一步：尝试匹配文档要求的精确格式 ¥216.00x1个
        val exactMatcher = PRICE_QUANTITY_PATTERN.matcher(text)
        if (exactMatcher.find()) {
            try {
                price = exactMatcher.group(2)?.toDouble() ?: 0.0  // 价格在第2组
                quantity = exactMatcher.group(3)?.toInt() ?: 1     // 数量在第3组
                Log.d(TAG, "Extracted from exact pattern: price=$price, quantity=$quantity, unit=${exactMatcher.group(4)}")
                return Pair(price, quantity)
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Failed to parse exact price/quantity pattern: ${exactMatcher.group()}")
            }
        }
        
        // 第二步：尝试更宽松的价格×数量格式
        val looseMatcher = PRICE_QUANTITY_PATTERN_LOOSE.matcher(text)
        if (looseMatcher.find()) {
            try {
                price = looseMatcher.group(1)?.toDouble() ?: 0.0
                quantity = looseMatcher.group(2)?.toInt() ?: 1
                Log.d(TAG, "Extracted from loose pattern: price=$price, quantity=$quantity")
                return Pair(price, quantity)
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Failed to parse loose price/quantity pattern: ${looseMatcher.group()}")
            }
        }
        
        // 第三步：单独提取价格
        val priceMatcher = PRICE_PATTERN.matcher(text)
        if (priceMatcher.find()) {
            try {
                price = priceMatcher.group(1)?.toDouble() ?: 0.0
                Log.d(TAG, "Extracted price separately: $price")
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Failed to parse price: ${priceMatcher.group(1)}")
            }
        }
        
        // 第四步：单独提取数量
        val qtyMatcher = QUANTITY_PATTERN.matcher(text)
        if (qtyMatcher.find()) {
            try {
                quantity = qtyMatcher.group(1)?.toInt() ?: 1
                Log.d(TAG, "Extracted quantity separately: $quantity")
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Failed to parse quantity: ${qtyMatcher.group(1)}")
            }
        }
        
        return Pair(price, quantity)
    }
    
    /**
     * 提取规格信息
     */
    private fun extractSpecifications(block: List<String>, productName: String): String {
        for (line in block) {
            if (line != productName && line.length < 50) {
                // 查找可能的规格信息（通常是较短的描述性文字）
                val cleanLine = line.replace(Regex("[¥￥$0-9×x*\\.]"), "").trim()
                if (cleanLine.isNotBlank() && 
                    cleanLine.length > 1 && 
                    cleanLine.length < 20 &&
                    !cleanLine.contains("价格") &&
                    !cleanLine.contains("数量")) {
                    return cleanLine
                }
            }
        }
        return ""
    }
    
    /**
     * 提取商品编码
     */
    private fun extractProductCode(text: String): String {
        val matcher = CODE_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(1) ?: ""
        } else {
            ""
        }
    }
    
    /**
     * 交叉验证与容错
     * 使用区块右侧独立显示的总价与单价 × 数量的结果进行比对
     */
    private fun performCrossValidation(text: String, price: Double, quantity: Int): CrossValidationResult {
        val totalMatcher = TOTAL_PATTERN.matcher(text)
        if (totalMatcher.find()) {
            try {
                val totalPrice = totalMatcher.group(1)?.toDouble() ?: 0.0
                val calculatedTotal = price * quantity
                val difference = Math.abs(totalPrice - calculatedTotal)
                val tolerance = Math.max(0.01, calculatedTotal * 0.05) // 5%容错或最少0.01
                
                val isValid = difference <= tolerance
                Log.d(TAG, "Cross validation: total=$totalPrice, calculated=$calculatedTotal, difference=$difference, valid=$isValid")
                
                return CrossValidationResult(
                    foundTotalPrice = true,
                    totalPrice = totalPrice,
                    calculatedTotal = calculatedTotal,
                    isValid = isValid,
                    difference = difference
                )
            } catch (e: NumberFormatException) {
                Log.w(TAG, "Failed to parse total price: ${totalMatcher.group(1)}")
            }
        }
        
        return CrossValidationResult(foundTotalPrice = false)
    }
    
    /**
     * 交叉验证结果
     */
    private data class CrossValidationResult(
        val foundTotalPrice: Boolean,
        val totalPrice: Double = 0.0,
        val calculatedTotal: Double = 0.0,
        val isValid: Boolean = false,
        val difference: Double = 0.0
    )
    
    /**
     * 计算解析置信度
     */
    private fun calculateParsingConfidence(
        productName: String, 
        price: Double, 
        quantity: Int, 
        originalText: String,
        crossValidation: CrossValidationResult
    ): Float {
        var confidence = 0.6f // 基础置信度
        
        // 商品名称质量评估
        if (productName.isNotBlank()) {
            confidence += 0.2f
            if (productName.matches(Regex(".*[\\u4e00-\\u9fa5]{3,}.*"))) {
                confidence += 0.1f // 包含较多中文字符
            }
        }
        
        // 价格有效性评估
        if (price > 0) {
            confidence += 0.1f
            if (price > 0.1 && price < 10000) {
                confidence += 0.05f // 合理的价格范围
            }
        } else {
            confidence -= 0.2f // 没有价格信息
        }
        
        // 数量有效性评估
        if (quantity > 0 && quantity <= 1000) {
            confidence += 0.05f
        }
        
        // 文本清晰度评估
        val textLength = originalText.length
        if (textLength > 5 && textLength < 200) {
            confidence += 0.05f
        }
        
        // 交叉验证评估
        if (crossValidation.foundTotalPrice) {
            if (crossValidation.isValid) {
                confidence += 0.15f // 交叉验证通过，大幅提升置信度
                Log.d(TAG, "Cross validation passed, confidence boosted")
            } else {
                confidence -= 0.1f // 交叉验证失败，降低置信度
                Log.d(TAG, "Cross validation failed, confidence reduced")
            }
        }
        
        return confidence.coerceIn(0.1f, 1.0f)
    }
}
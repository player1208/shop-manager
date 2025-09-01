package com.example.storemanagerassitent.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.example.storemanagerassitent.utils.CrashReporter

/**
 * OCR文字识别处理器
 * 使用Google ML Kit进行中文文字识别
 */
class OcrProcessor {
    
    companion object {
        private const val TAG = "OcrProcessor"
    }
    
    private val textRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    
    /**
     * OCR识别结果
     */
    data class OcrResult(
        val success: Boolean,
        val recognizedText: String = "",
        val textBlocks: List<TextBlock> = emptyList(),
        val error: String? = null
    )
    
    /**
     * 文字块数据
     */
    data class TextBlock(
        val text: String,
        val confidence: Float,
        val boundingBox: android.graphics.Rect?,
        val lines: List<TextLine> = emptyList()
    )
    
    /**
     * 文字行数据
     */
    data class TextLine(
        val text: String,
        val confidence: Float,
        val boundingBox: android.graphics.Rect?
    )
    
    /**
     * 处理图片进行OCR识别
     */
    suspend fun processImage(bitmap: Bitmap): OcrResult {
        return try {
            CrashReporter.logOcrProcess("START", true, "Starting OCR processing with bitmap ${bitmap.width}x${bitmap.height}")
            
            // 检查bitmap是否有效
            if (bitmap.isRecycled) {
                val error = "Bitmap is recycled"
                CrashReporter.logOcrProcess("VALIDATION", false, error)
                return OcrResult(success = false, error = error)
            }
            
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                val error = "Invalid bitmap dimensions: ${bitmap.width}x${bitmap.height}"
                CrashReporter.logOcrProcess("VALIDATION", false, error)
                return OcrResult(success = false, error = error)
            }
            
            CrashReporter.logOcrProcess("VALIDATION", true, "Bitmap validation passed")
            
            // 创建InputImage
            val inputImage = try {
                InputImage.fromBitmap(bitmap, 0)
            } catch (e: Exception) {
                CrashReporter.logOcrProcess("INPUT_IMAGE", false, "Failed to create InputImage", e)
                throw e
            }
            
            CrashReporter.logOcrProcess("INPUT_IMAGE", true, "InputImage created successfully")
            
            // 执行OCR识别
            val visionText = try {
                textRecognizer.process(inputImage).await()
            } catch (e: Exception) {
                CrashReporter.logOcrProcess("RECOGNITION", false, "ML Kit recognition failed", e)
                throw e
            }
            
            CrashReporter.logOcrProcess("RECOGNITION", true, "ML Kit recognition completed")
            
            // 转换结果
            val textBlocks = try {
                convertToTextBlocks(visionText)
            } catch (e: Exception) {
                CrashReporter.logOcrProcess("CONVERSION", false, "Failed to convert text blocks", e)
                throw e
            }
            
            val recognizedText = visionText.text
            
            Log.d(TAG, "OCR recognition completed. Text length: ${recognizedText.length}")
            Log.d(TAG, "Recognized text: $recognizedText")
            
            CrashReporter.logOcrProcess("COMPLETE", true, "OCR processing completed successfully, text length: ${recognizedText.length}")
            
            OcrResult(
                success = true,
                recognizedText = recognizedText,
                textBlocks = textBlocks
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "OCR processing failed", e)
            CrashReporter.logOcrProcess("ERROR", false, "OCR processing failed: ${e.message}", e)
            OcrResult(
                success = false,
                error = e.message ?: "OCR识别失败"
            )
        }
    }
    
    /**
     * 转换ML Kit的Text结果为自定义数据结构
     */
    private fun convertToTextBlocks(visionText: Text): List<TextBlock> {
        return visionText.textBlocks.map { textBlock ->
            val lines = textBlock.lines.map { line ->
                TextLine(
                    text = line.text,
                    confidence = calculateLineConfidence(line),
                    boundingBox = line.boundingBox
                )
            }
            
            TextBlock(
                text = textBlock.text,
                confidence = calculateBlockConfidence(textBlock),
                boundingBox = textBlock.boundingBox,
                lines = lines
            )
        }
    }
    
    /**
     * 计算文字块的置信度
     */
    private fun calculateBlockConfidence(textBlock: Text.TextBlock): Float {
        // ML Kit没有直接提供置信度，我们基于文字特征进行估算
        val text = textBlock.text
        
        var confidence = 0.8f // 基础置信度
        
        // 根据文字特征调整置信度
        if (text.length < 2) {
            confidence -= 0.2f // 短文字置信度较低
        }
        
        if (text.matches(Regex(".*[0-9]+.*"))) {
            confidence += 0.1f // 包含数字的文字通常识别较准确
        }
        
        if (text.matches(Regex(".*[¥￥$].*"))) {
            confidence += 0.1f // 包含货币符号的文字
        }
        
        // 检查是否包含常见的OCR错误字符
        val errorChars = listOf("丨", "丿", "乀", "乁", "乂")
        if (errorChars.any { text.contains(it) }) {
            confidence -= 0.3f
        }
        
        return confidence.coerceIn(0.1f, 1.0f)
    }
    
    /**
     * 计算文字行的置信度
     */
    private fun calculateLineConfidence(line: Text.Line): Float {
        // 类似于块置信度的计算方法
        val text = line.text
        var confidence = 0.8f
        
        if (text.length < 2) confidence -= 0.2f
        if (text.matches(Regex(".*[0-9]+.*"))) confidence += 0.1f
        if (text.matches(Regex(".*[¥￥$].*"))) confidence += 0.1f
        
        return confidence.coerceIn(0.1f, 1.0f)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            textRecognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close text recognizer", e)
        }
    }
}
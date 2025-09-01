package com.example.storemanagerassitent.data.api

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Minimal client to call SCF OCR endpoint with fixed flags.
 * Required params set as per requirement:
 * - ItemNamesShowMode=false (输出默认字段+自定义字段)
 * - ReturnFullText=true
 * - EnableCoord=true
 * - OutputParentKey=true
 * - OutputLanguage="cn"
 */
class RemoteOcrClient(
	private val endpoint: String = "https://1364601093-kcx0kpgf4q.ap-beijing.tencentscf.com/ocr/extract"
) {

	private val client: OkHttpClient by lazy {
		val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
		OkHttpClient.Builder()
			.addInterceptor(logging)
			.connectTimeout(java.time.Duration.ofSeconds(10))
			.readTimeout(java.time.Duration.ofSeconds(20))
			.writeTimeout(java.time.Duration.ofSeconds(20))
			.build()
	}

	companion object {
		fun bitmapToBase64(bitmap: Bitmap): String {
			// 自适应压缩：目标 Base64 长度 <= 6MB（约 6_000_000 字符）
			val maxBase64Len = 6_000_000
			val qualitySteps = intArrayOf(90, 85, 80, 75, 70)
			val scaleSteps = floatArrayOf(1.0f, 0.85f, 0.7f, 0.55f)
			var best: String? = null
			outer@ for (scale in scaleSteps) {
				val src = if (scale < 0.999f) {
					val w = (bitmap.width * scale).toInt().coerceAtLeast(64)
					val h = (bitmap.height * scale).toInt().coerceAtLeast(64)
					android.graphics.Bitmap.createScaledBitmap(bitmap, w, h, true)
				} else bitmap
				for (q in qualitySteps) {
					val baos = ByteArrayOutputStream()
					src.compress(Bitmap.CompressFormat.JPEG, q, baos)
					val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
					if (best == null || b64.length < (best?.length ?: Int.MAX_VALUE)) best = b64
					if (b64.length <= maxBase64Len) {
						if (src !== bitmap) try { src.recycle() } catch (_: Exception) {}
						return b64
					}
				}
				if (src !== bitmap) try { src.recycle() } catch (_: Exception) {}
			}
			// 兜底返回最小的那个
			return best ?: run {
				val baos = ByteArrayOutputStream()
				bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
				Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
			}
		}
	}

	suspend fun extractDocMulti(
		imageUrl: String? = null,
		imageBase64: String? = null,
		itemNames: List<String> = listOf(
			"商品名称", "商品数量"
		),
		configId: String = "Table"
	): String = withContext(Dispatchers.IO) {
		val json = JSONObject().apply {
			put("ConfigId", configId)
			if (!imageUrl.isNullOrBlank()) put("ImageUrl", imageUrl)
			if (!imageBase64.isNullOrBlank()) put("ImageBase64", imageBase64)
			put("ItemNames", JSONArray(itemNames))
			put("ItemNamesShowMode", true)
			put("ReturnFullText", false)
			put("EnableCoord", false)
			put("OutputParentKey", true)
			put("OutputLanguage", "cn")
		}

		val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
		val req = Request.Builder()
			.url(endpoint)
			.post(body)
			.build()

		client.newCall(req).execute().use { resp ->
			val text = resp.body?.string() ?: ""
			if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}: $text")
			text
		}
	}
}



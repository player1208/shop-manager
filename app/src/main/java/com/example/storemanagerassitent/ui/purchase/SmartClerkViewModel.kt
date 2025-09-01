package com.example.storemanagerassitent.ui.purchase

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.storemanagerassitent.data.ReviewableItem
import com.example.storemanagerassitent.data.Goods

data class ClerkRow(val item: ReviewableItem, val matchedGoodsId: String?)

class SmartClerkViewModel : ViewModel() {
    private val _rows = MutableStateFlow<List<ClerkRow>>(emptyList())
    val rows: StateFlow<List<ClerkRow>> = _rows.asStateFlow()

    fun clear() { _rows.value = emptyList() }

    fun updateRow(index: Int, item: ReviewableItem) {
        val list = _rows.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(item = item)
            _rows.value = list
        }
    }

    fun removeAt(index: Int) {
        val list = _rows.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _rows.value = list
        }
    }

    fun merge(items: List<ReviewableItem>, localGoods: List<Goods>) {
        val current = _rows.value.toMutableList()
        items.forEach { item ->
            val match = findBestMatch(item, localGoods)
            val resolved = if (match != null) {
                val standardizedName = listOfNotNull(match.name, match.specifications)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val updated = item.copy(
                    editedName = standardizedName.ifBlank { item.editedName },
                    editedSpecifications = match.specifications ?: item.editedSpecifications,
                    isExistingProduct = true,
                    selectedCategory = match.category ?: item.selectedCategory
                )
                ClerkRow(updated, matchedGoodsId = match.id)
            } else {
                // 保留来自“继续手动添加”的 isExistingProduct 标记
                ClerkRow(item, matchedGoodsId = null)
            }

            val keyName = resolved.item.editedName.trim()
            val keySpec = resolved.item.editedSpecifications.trim()
            val keyCat = resolved.item.selectedCategory
            val keyPrice = resolved.item.editedPrice
            val existedIndex = current.indexOfFirst { rr ->
                rr.item.editedName.trim() == keyName &&
                rr.item.editedSpecifications.trim() == keySpec &&
                rr.item.selectedCategory == keyCat &&
                rr.item.editedPrice == keyPrice
            }
            if (existedIndex >= 0) {
                val existed = current[existedIndex]
                val newQty = (existed.item.editedQuantity + resolved.item.editedQuantity).coerceAtLeast(1)
                val keepGoodsId = existed.matchedGoodsId ?: resolved.matchedGoodsId
                val mergedItem = existed.item.copy(
                    editedQuantity = newQty,
                    isExistingProduct = existed.item.isExistingProduct || resolved.item.isExistingProduct
                )
                current[existedIndex] = existed.copy(item = mergedItem, matchedGoodsId = keepGoodsId)
            } else {
                current.add(resolved)
            }
        }
        _rows.value = current
    }

    private fun findBestMatch(target: ReviewableItem, localGoods: List<Goods>): Goods? {
        return com.example.storemanagerassitent.utils.FuzzyMatcher.findBestMatch(
            target.editedName, target.editedSpecifications, localGoods, 0.6
        )
    }
}



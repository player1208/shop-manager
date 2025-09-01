package com.example.storemanagerassitent.ui.purchase

import com.example.storemanagerassitent.data.ReviewableItem
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 简单的跨界面桥接：用于从“手动添加”向“智能库吏”临时投递待审核的条目。
 * 使用内存队列，在智能库吏界面创建时会一次性拉取并合并。
 */
object SmartClerkBridge {
    private val buffer = CopyOnWriteArrayList<ReviewableItem>()

    fun add(items: List<ReviewableItem>) {
        if (items.isEmpty()) return
        buffer.addAll(items)
    }

    fun drain(): List<ReviewableItem> {
        if (buffer.isEmpty()) return emptyList()
        val snapshot = buffer.toList()
        buffer.clear()
        return snapshot
    }
}




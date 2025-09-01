package com.example.storemanagerassitent.data

/**
 * 商品数据模型
 */
data class Goods(
    val id: String,
    val name: String,
    val category: String,
    val specifications: String, // 规格/型号
    val stockQuantity: Int,
    val lowStockThreshold: Int = 5, // 低库存阈值，默认5件
    val barcode: String? = null,
    val imageUrl: String? = null,
    val purchasePrice: Double = 0.0, // 进价
    val retailPrice: Double = 0.0, // 零售价
    val isDelisted: Boolean = false, // 是否已下架
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * 是否低库存
     */
    val isLowStock: Boolean
        get() = stockQuantity <= lowStockThreshold
    
    /**
     * 商品名称和型号合并显示
     */
    val displayName: String
        get() = "$name $specifications"
    
    /**
     * 是否可以下架（库存为0）
     */
    val canBeDelisted: Boolean
        get() = stockQuantity == 0
}

/**
 * 商品分类
 */
data class GoodsCategory(
    val id: String,
    val name: String,
    val colorHex: String // 分类颜色
)

/**
 * 排序选项
 */
enum class SortOption(val displayName: String) {
    STOCK_ASC("按库存(从少到多)"),
    NAME_A_Z("按名称(A-Z)"),
    LAST_UPDATED("按更新时间")
}

/**
 * 出库原因
 */
enum class OutboundReason(val displayName: String) {
    SOLD("商品售出")
}

/**
 * 样本数据
 */
object SampleData {
    val categories = listOf(
        GoodsCategory("all", "全部", "#6C757D"),
        GoodsCategory("bathroom", "卫浴洁具", "#007BFF"),  // 蓝色
        GoodsCategory("manual_tools", "手动工具", "#FD7E14"), // 橙色
        GoodsCategory("power_tools", "电动工具", "#DC3545"),  // 红色
        GoodsCategory("pipes", "管材管件", "#28A745"),        // 绿色
        GoodsCategory("screws", "螺丝螺母", "#6F42C1")       // 紫色
    )
    
    val goods = listOf(
        // 卫浴洁具
        Goods(
            id = "1",
            name = "九牧王单孔冷热龙头",
            category = "bathroom",
            specifications = "J-1022",
            stockQuantity = 12,
            lowStockThreshold = 5,
            purchasePrice = 85.0,
            retailPrice = 128.0
        ),
        Goods(
            id = "2",
            name = "科勒浴缸水龙头",
            category = "bathroom",
            specifications = "K-45102T",
            stockQuantity = 3,
            lowStockThreshold = 5,
            purchasePrice = 260.0,
            retailPrice = 390.0
        ),
        Goods(
            id = "3",
            name = "汉斯格雅花洒套装",
            category = "bathroom",
            specifications = "HG-2680",
            stockQuantity = 8,
            lowStockThreshold = 5,
            purchasePrice = 450.0,
            retailPrice = 675.0
        ),
        
        // 手动工具
        Goods(
            id = "4",
            name = "世达螺丝刀套装",
            category = "manual_tools",
            specifications = "10件套",
            stockQuantity = 15,
            lowStockThreshold = 8,
            purchasePrice = 45.0,
            retailPrice = 67.5
        ),
        Goods(
            id = "5",
            name = "德国进口扳手",
            category = "manual_tools",
            specifications = "8-24mm",
            stockQuantity = 4,
            lowStockThreshold = 6,
            purchasePrice = 120.0,
            retailPrice = 180.0
        ),
        Goods(
            id = "6",
            name = "钢丝钳",
            category = "manual_tools",
            specifications = "8寸",
            stockQuantity = 20,
            lowStockThreshold = 10,
            purchasePrice = 25.0,
            retailPrice = 37.5
        ),
        
        // 电动工具
        Goods(
            id = "7",
            name = "博世电钻",
            category = "power_tools",
            specifications = "GSB120-LI",
            stockQuantity = 6,
            lowStockThreshold = 3,
            purchasePrice = 580.0,
            retailPrice = 870.0
        ),
        Goods(
            id = "8",
            name = "牧田角磨机",
            category = "power_tools",
            specifications = "GA5030",
            stockQuantity = 2,
            lowStockThreshold = 4,
            purchasePrice = 320.0,
            retailPrice = 480.0
        ),
        
        // 管材管件
        Goods(
            id = "9",
            name = "PVC管",
            category = "pipes",
            specifications = "DN110×3m",
            stockQuantity = 25,
            lowStockThreshold = 15,
            purchasePrice = 35.0,
            retailPrice = 52.5
        ),
        Goods(
            id = "10",
            name = "铜管接头",
            category = "pipes",
            specifications = "20mm",
            stockQuantity = 50,
            lowStockThreshold = 30,
            purchasePrice = 8.5,
            retailPrice = 12.75
        ),
        
        // 螺丝螺母
        Goods(
            id = "11",
            name = "304不锈钢螺丝",
            category = "screws",
            specifications = "M6×25",
            stockQuantity = 200,
            lowStockThreshold = 100,
            purchasePrice = 0.5,
            retailPrice = 0.75
        ),
        Goods(
            id = "12",
            name = "镀锌螺母",
            category = "screws",
            specifications = "M8",
            stockQuantity = 80,
            lowStockThreshold = 50,
            purchasePrice = 0.3,
            retailPrice = 0.45
        ),
        
        // 一些零库存商品（用于测试批量下架功能）
        Goods(
            id = "13",
            name = "旧款水龙头",
            category = "bathroom",
            specifications = "停产型号",
            stockQuantity = 0,
            lowStockThreshold = 5,
            purchasePrice = 65.0,
            retailPrice = 97.5
        ),
        Goods(
            id = "14",
            name = "过期胶水",
            category = "manual_tools",
            specifications = "已过期",
            stockQuantity = 0,
            lowStockThreshold = 10,
            purchasePrice = 12.0,
            retailPrice = 18.0
        )
    )
}
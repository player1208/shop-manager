package com.example.storemanagerassitent.data

/**
 * 时间维度枚举
 */
enum class TimePeriod(val displayName: String, val key: String) {
    WEEK("本周", "week"),
    MONTH("本月", "month"),
    YEAR("本年", "year")
}

/**
 * 商品销售数据
 */
data class ProductSalesData(
    val productId: String,
    val productName: String,
    val categoryName: String,
    val salesCount: Int,
    val rank: Int = 0
)

/**
 * 分类选项
 */
data class CategoryOption(
    val id: String,
    val name: String,
    val isSelected: Boolean = false
)

/**
 * 销售洞察数据
 */
data class SalesInsightData(
    val period: TimePeriod,
    val selectedCategory: CategoryOption,
    val productSales: List<ProductSalesData>
)

/**
 * 快速操作项
 */
data class QuickAction(
    val id: String,
    val title: String,
    val description: String,
    val secondaryActionText: String,
    val iconName: String = "default"
)

/**
 * 示例数据
 */
object HomeData {
    /**
     * 快速操作数据
     */
    val quickActions = listOf(
        QuickAction(
            id = "sales",
            title = "销售开单",
            description = "快速为客户创建销售单",
            secondaryActionText = "查看销售记录 →"
        ),
        QuickAction(
            id = "purchase",
            title = "进货开单", 
            description = "登记新到货的商品",
            secondaryActionText = "查看进货记录 →"
        )
    )
    
    /**
     * 分类选项列表
     */
    val categoryOptions = listOf(
        CategoryOption("all", "全部商品", true),
        CategoryOption("bathroom", "卫浴洁具"),
        CategoryOption("hand_tools", "手动工具"),
        CategoryOption("power_tools", "电动工具"),
        CategoryOption("hardware", "五金配件"),
        CategoryOption("decoration", "装修材料"),
        CategoryOption("safety", "安全设备"),
        CategoryOption("garden", "园艺工具")
    )
    
    /**
     * 示例商品销售数据
     */
    private fun getProductSalesData(period: TimePeriod, categoryId: String): List<ProductSalesData> {
        val allProducts = when (period) {
            TimePeriod.WEEK -> listOf(
                ProductSalesData("p1", "九牧王单孔冷热龙头 J-1022", "卫浴洁具", 52),
                ProductSalesData("p2", "科勒马桶盖板 K-8234", "卫浴洁具", 38),
                ProductSalesData("p3", "美标淋浴花洒套装", "卫浴洁具", 35),
                ProductSalesData("p4", "史丹利螺丝刀套装 12件", "手动工具", 29),
                ProductSalesData("p5", "博世电钻 GSB120", "电动工具", 27),
                ProductSalesData("p6", "得力扳手组合套装", "手动工具", 25),
                ProductSalesData("p7", "汉斯希尔前置过滤器", "卫浴洁具", 23),
                ProductSalesData("p8", "牧田角磨机 GA5030", "电动工具", 22),
                ProductSalesData("p9", "3M防护眼镜", "安全设备", 20),
                ProductSalesData("p10", "立邦内墙乳胶漆", "装修材料", 18)
            )
            TimePeriod.MONTH -> listOf(
                ProductSalesData("p1", "九牧王单孔冷热龙头 J-1022", "卫浴洁具", 198),
                ProductSalesData("p2", "史丹利螺丝刀套装 12件", "手动工具", 156),
                ProductSalesData("p3", "博世电钻 GSB120", "电动工具", 134),
                ProductSalesData("p4", "科勒马桶盖板 K-8234", "卫浴洁具", 125),
                ProductSalesData("p5", "美标淋浴花洒套装", "卫浴洁具", 112),
                ProductSalesData("p6", "牧田角磨机 GA5030", "电动工具", 89),
                ProductSalesData("p7", "得力扳手组合套装", "手动工具", 87),
                ProductSalesData("p8", "汉斯希尔前置过滤器", "卫浴洁具", 78),
                ProductSalesData("p9", "立邦内墙乳胶漆", "装修材料", 67),
                ProductSalesData("p10", "3M防护眼镜", "安全设备", 56),
                ProductSalesData("p11", "西门子开关插座", "五金配件", 45),
                ProductSalesData("p12", "施耐德断路器", "五金配件", 34)
            )
            TimePeriod.YEAR -> listOf(
                ProductSalesData("p1", "九牧王单孔冷热龙头 J-1022", "卫浴洁具", 2340),
                ProductSalesData("p2", "史丹利螺丝刀套装 12件", "手动工具", 1890),
                ProductSalesData("p3", "博世电钻 GSB120", "电动工具", 1567),
                ProductSalesData("p4", "科勒马桶盖板 K-8234", "卫浴洁具", 1423),
                ProductSalesData("p5", "美标淋浴花洒套装", "卫浴洁具", 1234),
                ProductSalesData("p6", "牧田角磨机 GA5030", "电动工具", 1098),
                ProductSalesData("p7", "得力扳手组合套装", "手动工具", 987),
                ProductSalesData("p8", "汉斯希尔前置过滤器", "卫浴洁具", 876),
                ProductSalesData("p9", "立邦内墙乳胶漆", "装修材料", 789),
                ProductSalesData("p10", "3M防护眼镜", "安全设备", 678),
                ProductSalesData("p11", "西门子开关插座", "五金配件", 567),
                ProductSalesData("p12", "施耐德断路器", "五金配件", 456),
                ProductSalesData("p13", "德力西电线电缆", "五金配件", 398),
                ProductSalesData("p14", "花园浇水喷头", "园艺工具", 234),
                ProductSalesData("p15", "园艺修剪剪刀", "园艺工具", 189)
            )
        }
        
        // 根据分类筛选
        val filteredProducts = if (categoryId == "all") {
            allProducts
        } else {
            val categoryName = categoryOptions.find { it.id == categoryId }?.name ?: ""
            allProducts.filter { it.categoryName == categoryName }
        }
        
        // 只返回销售数量大于0的商品，并添加排名
        return filteredProducts
            .filter { it.salesCount > 0 }
            .sortedByDescending { it.salesCount }
            .mapIndexed { index, product -> 
                product.copy(rank = index + 1)
            }
    }
    
    /**
     * 获取销售洞察数据
     */
    fun getSalesInsightData(period: TimePeriod, categoryId: String = "all"): SalesInsightData {
        val selectedCategory = categoryOptions.find { it.id == categoryId } 
            ?: categoryOptions.first()
        
        return SalesInsightData(
            period = period,
            selectedCategory = selectedCategory.copy(isSelected = true),
            productSales = getProductSalesData(period, categoryId)
        )
    }
}
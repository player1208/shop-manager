package com.example.storemanagerassitent.data

/**
 * 分类管理数据类
 */
data class Category(
    val id: String,
    val name: String,
    val productCount: Int = 0  // 该分类下的商品数量
)

/**
 * 分类操作结果
 */
sealed class CategoryOperationResult {
    object Success : CategoryOperationResult()
    data class Error(val message: String) : CategoryOperationResult()
}

/**
 * 分类删除结果
 */
sealed class CategoryDeleteResult {
    object Success : CategoryDeleteResult()
    data class HasProducts(val productCount: Int) : CategoryDeleteResult()
    data class Error(val message: String) : CategoryDeleteResult()
}

/**
 * 分类管理数据仓库
 */
@Deprecated("Snapshot repository is deprecated; use Room-based repository via ServiceLocator.categoryRepository")
object CategoryRepository {
    
    // 模拟分类数据存储
    private var categories = mutableListOf(
        Category("bathroom", "卫浴洁具", 8),
        Category("hand_tools", "手动工具", 12),
        Category("power_tools", "电动工具", 6),
        Category("hardware", "五金配件", 15),
        Category("decoration", "装修材料", 9),
        Category("safety", "安全设备", 4),
        Category("garden", "园艺工具", 3)
    )
    
    /**
     * 获取所有分类
     */
    fun getAllCategories(): List<Category> {
        return categories.toList()
    }
    
    /**
     * 添加新分类
     */
    fun addCategory(name: String): CategoryOperationResult {
        // 检查分类名称是否已存在
        if (categories.any { it.name == name }) {
            return CategoryOperationResult.Error("分类名称已存在")
        }
        
        // 检查名称是否为空
        if (name.isBlank()) {
            return CategoryOperationResult.Error("分类名称不能为空")
        }
        
        // 生成新ID
        val newId = "category_${System.currentTimeMillis()}"
        val newCategory = Category(newId, name, 0)
        
        categories.add(newCategory)
        return CategoryOperationResult.Success
    }
    
    /**
     * 编辑分类名称
     */
    fun editCategory(categoryId: String, newName: String): CategoryOperationResult {
        // 检查分类是否存在
        val categoryIndex = categories.indexOfFirst { it.id == categoryId }
        if (categoryIndex == -1) {
            return CategoryOperationResult.Error("分类不存在")
        }
        
        // 检查新名称是否为空
        if (newName.isBlank()) {
            return CategoryOperationResult.Error("分类名称不能为空")
        }
        
        // 检查新名称是否与其他分类重复（排除当前分类）
        if (categories.any { it.name == newName && it.id != categoryId }) {
            return CategoryOperationResult.Error("分类名称已存在")
        }
        
        // 更新分类名称
        val oldCategory = categories[categoryIndex]
        categories[categoryIndex] = oldCategory.copy(name = newName)
        
        return CategoryOperationResult.Success
    }
    
    /**
     * 删除分类
     */
    fun deleteCategory(categoryId: String): CategoryDeleteResult {
        val category = categories.find { it.id == categoryId }
            ?: return CategoryDeleteResult.Error("分类不存在")
        
        // 检查分类下是否有商品
        if (category.productCount > 0) {
            return CategoryDeleteResult.HasProducts(category.productCount)
        }
        
        // 删除分类
        categories.removeIf { it.id == categoryId }
        return CategoryDeleteResult.Success
    }
    
    /**
     * 获取分类商品数量（模拟）
     */
    fun getCategoryProductCount(categoryId: String): Int {
        return categories.find { it.id == categoryId }?.productCount ?: 0
    }
    
    /**
     * 更新分类商品数量（模拟）
     */
    fun updateCategoryProductCount(categoryId: String, count: Int) {
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index != -1) {
            categories[index] = categories[index].copy(productCount = count)
        }
    }
    
    /**
     * 模拟检查分类下是否有商品
     */
    fun hasCategoryProducts(categoryId: String): Boolean {
        // 在真实应用中，这里应该查询商品数据库
        // 这里使用模拟数据进行演示
        return getCategoryProductCount(categoryId) > 0
    }
}
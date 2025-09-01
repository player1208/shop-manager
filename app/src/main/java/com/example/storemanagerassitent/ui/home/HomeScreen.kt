package com.example.storemanagerassitent.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storemanagerassitent.data.ProductSalesData
import com.example.storemanagerassitent.data.QuickAction
import com.example.storemanagerassitent.data.TimePeriod
import com.example.storemanagerassitent.data.CategoryOption

/**
 * 首页主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onPurchaseOrderClick: () -> Unit = {},
    onSalesOrderClick: () -> Unit = {},
    onSalesRecordClick: () -> Unit = {},
    onPurchaseRecordClick: () -> Unit = {}
) {
    val selectedTimePeriod by viewModel.selectedTimePeriod.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categoryOptions by viewModel.categoryOptions.collectAsState()
    val salesInsightData by viewModel.salesInsightData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showCategoryDropdown by viewModel.showCategoryDropdown.collectAsState()
    val showLowStockDialog by viewModel.showLowStockDialog.collectAsState()
    val lowStockGoods by viewModel.lowStockGoods.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "店铺助手",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    val zeroCount by viewModel.lowStockGoods.collectAsState()
                    val count = zeroCount.count { it.stockQuantity == 0 }
                    androidx.compose.material3.BadgedBox(
                        modifier = Modifier.padding(end = 12.dp),
                        badge = {
                            if (count > 0) {
                                androidx.compose.material3.Badge {
                                    Text("$count")
                                }
                            }
                        }
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "库存告急",
                            tint = if (count > 0) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { viewModel.triggerLowStockDialog() }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val isRefreshing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() }
        )
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .pullRefresh(pullRefreshState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 核心操作区标题
            item {
                Text(
                    text = "快速操作",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 核心操作区卡片
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    viewModel.quickActions.forEach { action ->
                        QuickActionCard(
                            action = action,
                            modifier = Modifier.weight(1f),
                            onMainActionClick = { 
                                when (action.id) {
                                    "purchase" -> onPurchaseOrderClick()
                                    "sales" -> onSalesOrderClick()
                                    else -> viewModel.onQuickActionClick(action.id)
                                }
                            },
                            onSecondaryActionClick = { 
                                when (action.id) {
                                    "purchase" -> onPurchaseRecordClick()
                                    "sales" -> onSalesRecordClick()
                                    else -> viewModel.onSecondaryActionClick(action.id)
                                }
                            }
                        )
                    }
                }
            }
            
            // 销售洞察区标题
            item {
                Text(
                    text = "热销商品排行",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 时间维度切换器
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = TimePeriod.values().indexOf(selectedTimePeriod),
                            containerColor = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TimePeriod.values().forEach { period ->
                                Tab(
                                    selected = selectedTimePeriod == period,
                                    onClick = { viewModel.selectTimePeriod(period) },
                                    text = {
                                        Text(
                                            text = period.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selectedTimePeriod == period) 
                                                FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 分类选择器
                        CategorySelector(
                            selectedCategory = salesInsightData.selectedCategory,
                            categoryOptions = categoryOptions,
                            showDropdown = showCategoryDropdown,
                            onToggleDropdown = { viewModel.toggleCategoryDropdown() },
                            onCategorySelected = { viewModel.selectCategory(it.id) },
                            onDismiss = { viewModel.hideCategoryDropdown() }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 商品排名列表
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            ProductRankingList(
                                products = salesInsightData.productSales,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        // 仅显示指示器（无手势实现时保持可编译）
    }

    // 库存告急提醒对话框
    if (showLowStockDialog && lowStockGoods.isNotEmpty()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* 外部不消失，确保阅读 */ },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 顶部黄色提示图标
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "库存告急提醒",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "财务官提醒您:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(lowStockGoods.size) { index ->
                            val g = lowStockGoods[index]
                            Text("• ${g.displayName} 已告罄，请快补货吧！", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { viewModel.dismissLowStockDialog() }) {
                    Text("我知道了")
                }
            }
        )
    }
}

/**
 * 快速操作卡片
 */
@Composable
fun QuickActionCard(
    action: QuickAction,
    modifier: Modifier = Modifier,
    onMainActionClick: () -> Unit = {},
    onSecondaryActionClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .clickable { onMainActionClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 图标
            Icon(
                imageVector = when (action.id) {
                    "sales" -> Icons.Filled.Add
                    "purchase" -> Icons.Filled.ShoppingCart
                    else -> Icons.AutoMirrored.Filled.ArrowForward
                },
                contentDescription = action.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // 标题
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // 描述
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 次要操作链接
            Text(
                text = action.secondaryActionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSecondaryActionClick() }
            )
        }
    }
}

/**
 * 分类选择器
 */
@Composable
fun CategorySelector(
    selectedCategory: CategoryOption,
    categoryOptions: List<CategoryOption>,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    onCategorySelected: (CategoryOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 选择器按钮
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleDropdown() },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCategory.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "展开分类选择器",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 下拉菜单
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            categoryOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.name,
                            fontWeight = if (option.id == selectedCategory.id) 
                                FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onCategorySelected(option) }
                )
            }
        }
    }
}

/**
 * 商品排名列表
 */
@Composable
fun ProductRankingList(
    products: List<ProductSalesData>,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "该时间段内暂无销售记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        products.forEach { product ->
            ProductRankingItem(
                product = product,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 单个商品排名项
 */
@Composable
fun ProductRankingItem(
    product: ProductSalesData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = when (product.rank) {
                            1 -> Color(0xFFFFD700) // 金色
                            2 -> Color(0xFFC0C0C0) // 银色
                            3 -> Color(0xFFCD7F32) // 铜色
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${product.rank}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (product.rank) {
                        1, 2, 3 -> Color.White
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }
            
            // 商品信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = product.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // 销售数量
            Text(
                text = "售出 ${product.salesCount} 件",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
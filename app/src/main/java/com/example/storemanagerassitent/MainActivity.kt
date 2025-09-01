package com.example.storemanagerassitent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.storemanagerassitent.ui.goods.GoodsManagementScreen
import com.example.storemanagerassitent.ui.home.HomeScreen
import com.example.storemanagerassitent.ui.profile.ProfileScreen
import com.example.storemanagerassitent.ui.category.CategoryManagementScreen
import com.example.storemanagerassitent.ui.sales.SalesOrderScreen
import com.example.storemanagerassitent.ui.sales.SalesRecordScreen
import com.example.storemanagerassitent.ui.purchase.PurchaseRecordScreen
// PurchaseOrderScreen 已移除，不再使用
import com.example.storemanagerassitent.ui.components.GlobalSuccessSnackbarHost
import com.example.storemanagerassitent.ui.theme.StoreManagerAssitentTheme
import com.example.storemanagerassitent.ui.debug.DebugScreen
import com.example.storemanagerassitent.data.Goods
import com.example.storemanagerassitent.data.DataStoreManager
import com.example.storemanagerassitent.permission.PermissionManager
import com.example.storemanagerassitent.utils.CrashReporter
import android.util.Log
import kotlinx.coroutines.delay
import com.example.storemanagerassitent.data.db.ServiceLocator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * 底部导航项
 */
data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

/**
 * 应用页面定义
 */
 sealed class AppScreen {
    object Home : AppScreen()
    object Inventory : AppScreen()
    object Profile : AppScreen()
    object CategoryManagement : AppScreen()
    object SalesOrder : AppScreen()
     object SalesRecord : AppScreen()
     object PurchaseRecord : AppScreen()
     object NewPurchaseEntry : AppScreen()
     object ManualAddPurchase : AppScreen()
    object SmartClerk : AppScreen()
    object ClerkManualAdd : AppScreen()
    object Debug : AppScreen()
}

class MainActivity : FragmentActivity() {
    // 数据存储管理器
    private lateinit var dataStoreManager: DataStoreManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化数据存储管理器
        dataStoreManager = DataStoreManager(this)
        // 初始化数据库与仓库
        ServiceLocator.init(this)
        // 禁止预置示例数据：默认 settings.seedDisabled = true（除非用户改设置）
        lifecycleScope.launch {
            try {
                ServiceLocator.seedSampleDataIfAllowed(dataStoreManager)
                Log.i("MainActivity", "Sample data seeded to Room")
            } catch (e: Exception) {
                Log.e("MainActivity", "Seeding failed", e)
            }
        }
        
        try {
            // 初始化崩溃报告器
            CrashReporter.init(this)
            Log.i("MainActivity", "CrashReporter initialized successfully")
            
            // 记录权限状态
            logPermissionStatus()
            
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
            setContent {
                StoreManagerAssitentTheme {
                    var showAppSplash by remember { mutableStateOf(true) }
                    // 强制最短显示 700ms，保证过渡体验
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(700)
                        showAppSplash = false
                    }

                    if (showAppSplash) {
                        AppSplash()
                    } else {
                        MainScreen(dataStoreManager = dataStoreManager)
                    }
                }
            }
            
            Log.i("MainActivity", "MainActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            CrashReporter.logError("MainActivity", "Error in onCreate", e)
            throw e
        }
    }
    
    private fun logPermissionStatus() {
        try {
            val permissionStatus = PermissionManager.checkPermissionStatus(this)
            val ocrPermissionStatus = PermissionManager.checkOcrPermissionStatus(this)
            
            CrashReporter.logPermissionCheck("STORAGE", permissionStatus.storagePermission, "Screen capture functionality")
            CrashReporter.logPermissionCheck("OVERLAY", permissionStatus.overlayPermission, "Floating button functionality")
            CrashReporter.logPermissionCheck("FOREGROUND_SERVICE", permissionStatus.foregroundServicePermission, "Media projection service")
            CrashReporter.logPermissionCheck("CAMERA", ocrPermissionStatus.cameraPermission, "OCR image capture")
            CrashReporter.logPermissionCheck("MEDIA", ocrPermissionStatus.mediaPermission, "OCR image access")
            
            Log.i("MainActivity", "Permission status logged: ${permissionStatus.getStatusDescription()}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to log permission status", e)
            CrashReporter.logError("MainActivity", "Failed to log permission status", e)
        }
    }
}

@Composable
fun AppSplash() {
    // 以底部对齐裁剪，避免图片底部白边出现在屏幕上
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.startup_screen),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter
        )
    }
}

@Composable
fun MainScreen(dataStoreManager: DataStoreManager) {
    val navigationItems = listOf(
        NavigationItem("首页", Icons.Filled.Home, "home"),
        NavigationItem("库存", Icons.AutoMirrored.Filled.List, "inventory"),
        NavigationItem("我的", Icons.Filled.Person, "profile")
    )

    var selectedIndex by remember { mutableIntStateOf(0) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    
    // 全局成功提示的SnackbarHost状态
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 从库存页面传递到销售订单页面的商品数据
    var pendingGoodsData by remember { mutableStateOf<Pair<Goods, Int>?>(null) }

    // 处理导航逻辑
    fun navigateToScreen(screen: AppScreen) {
        currentScreen = screen
    }

    var autoOpenPurchaseManualSelection by remember { mutableStateOf(false) }

    fun navigateBack() {
        when (currentScreen) {
            AppScreen.CategoryManagement -> {
                currentScreen = AppScreen.Inventory
            }
            AppScreen.SalesOrder -> {
                currentScreen = AppScreen.Home
                selectedIndex = 0
            }
            AppScreen.SalesRecord -> {
                currentScreen = AppScreen.Home
                selectedIndex = 0
            }
            AppScreen.PurchaseRecord -> {
                currentScreen = AppScreen.Home
                selectedIndex = 0
            }
            AppScreen.NewPurchaseEntry -> {
                currentScreen = AppScreen.Home
                selectedIndex = 0
            }
            AppScreen.ManualAddPurchase -> {
                currentScreen = AppScreen.Home
                selectedIndex = 0
            }
            AppScreen.SmartClerk -> {
                currentScreen = AppScreen.NewPurchaseEntry
            }
            AppScreen.ClerkManualAdd -> {
                currentScreen = AppScreen.SmartClerk
            }
            
            AppScreen.Debug -> {
                currentScreen = AppScreen.Profile
                selectedIndex = 2
            }
            else -> {
                // 其他页面不处理返回，由底部导航控制
            }
        }
    }

    // 根据底部导航更新当前页面（仅在处于主标签页时生效，避免覆盖子页面导航）
    if (currentScreen == AppScreen.Home || currentScreen == AppScreen.Inventory || currentScreen == AppScreen.Profile) {
        when (selectedIndex) {
            0 -> currentScreen = AppScreen.Home
            1 -> currentScreen = AppScreen.Inventory
            2 -> currentScreen = AppScreen.Profile
        }
    }

    val isOnMainTabs = currentScreen == AppScreen.Home || currentScreen == AppScreen.Inventory || currentScreen == AppScreen.Profile

    var showExitHint by remember { mutableStateOf(false) }
    var lastBackPressedTime by remember { mutableStateOf(0L) }

    val context = LocalContext.current
    BackHandler(enabled = isOnMainTabs) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressedTime <= 2000) {
            (context as? ComponentActivity)?.finish()
        } else {
            showExitHint = true
            lastBackPressedTime = now
        }
    }

    if (showExitHint) {
        LaunchedEffect(showExitHint) {
            delay(1500)
            showExitHint = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
            // 只在主要页面显示底部导航栏
            if (currentScreen != AppScreen.CategoryManagement && 
                currentScreen != AppScreen.SalesOrder && 
                currentScreen != AppScreen.SalesRecord &&
                currentScreen != AppScreen.PurchaseRecord &&
                currentScreen != AppScreen.NewPurchaseEntry &&
                currentScreen != AppScreen.ManualAddPurchase &&
                currentScreen != AppScreen.SmartClerk &&
                currentScreen != AppScreen.ClerkManualAdd &&
                currentScreen != AppScreen.Debug) {
                NavigationBar {
                    navigationItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(text = item.label)
                            },
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index }
                        )
                    }
                }
            }
            },
            snackbarHost = {
                GlobalSuccessSnackbarHost(snackbarHostState = snackbarHostState)
            }
        ) { paddingValues ->
            when (currentScreen) {
            AppScreen.Home -> HomeScreen(
                modifier = Modifier.padding(paddingValues),
                onPurchaseOrderClick = {
                    navigateToScreen(AppScreen.NewPurchaseEntry)
                },
                onSalesOrderClick = {
                    navigateToScreen(AppScreen.SalesOrder)
                },
                onSalesRecordClick = {
                    navigateToScreen(AppScreen.SalesRecord)
                },
                onPurchaseRecordClick = {
                    navigateToScreen(AppScreen.PurchaseRecord)
                }
            )

                AppScreen.Inventory -> GoodsManagementScreen(
                modifier = Modifier.padding(paddingValues),
                onNavigateToCategoryManagement = {
                    navigateToScreen(AppScreen.CategoryManagement)
                },
                onNavigateToSalesOrder = { goods, quantity ->
                    // 存储待传递的商品数据
                    pendingGoodsData = Pair(goods, quantity)
                    navigateToScreen(AppScreen.SalesOrder)
                }
                )
                AppScreen.Profile -> ProfileScreen()
                AppScreen.CategoryManagement -> CategoryManagementScreen(
                    onNavigateBack = { navigateBack() }
                )
                AppScreen.SalesOrder -> SalesOrderScreen(
                    onNavigateBack = {
                        pendingGoodsData = null
                        navigateBack()
                    },
                    prefillGoodsData = pendingGoodsData?.let { (goods, quantity) ->
                        { viewModel ->
                            viewModel.initializeDataStore(dataStoreManager)
                            viewModel.prefillGoodsData(goods, quantity)
                            pendingGoodsData = null
                        }
                    }
                )
                AppScreen.NewPurchaseEntry -> com.example.storemanagerassitent.ui.purchase.NewPurchaseEntryScreen(
                    onNavigateBack = { navigateBack() },
                    onManualAddClick = {
                        navigateToScreen(AppScreen.ManualAddPurchase)
                    },
                    onSmartImportClick = {
                        navigateToScreen(AppScreen.SmartClerk)
                    }
                )
                AppScreen.ManualAddPurchase -> com.example.storemanagerassitent.ui.purchase.ManualAddPurchaseScreen(
                    onNavigateBack = { navigateBack() }
                )
                AppScreen.SmartClerk -> com.example.storemanagerassitent.ui.purchase.SmartClerkScreen(
                    onNavigateBack = { navigateBack() },
                    navigateToManualAdd = { navigateToScreen(AppScreen.ClerkManualAdd) },
                    autoOpenSource = true
                )
                AppScreen.ClerkManualAdd -> com.example.storemanagerassitent.ui.purchase.ClerkManualAddScreen(
                    onNavigateBack = { navigateBack() }
                )
                AppScreen.SalesRecord -> SalesRecordScreen(
                    onNavigateBack = { navigateBack() }
                )
                AppScreen.PurchaseRecord -> PurchaseRecordScreen(
                    onNavigateBack = { navigateBack() }
                )
                AppScreen.Debug -> DebugScreen(
                    onNavigateBack = { navigateBack() },
                    dataStoreManager = dataStoreManager
                )
            }
        }

        if (showExitHint && isOnMainTabs) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = "再滑一下退出小店财务官",
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    StoreManagerAssitentTheme {
        // 预览时跳过MainScreen
        Text("MainActivity Preview")
    }
}
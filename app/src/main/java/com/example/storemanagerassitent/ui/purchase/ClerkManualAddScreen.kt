package com.example.storemanagerassitent.ui.purchase

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ClerkManualAddScreen(
    onNavigateBack: () -> Unit
) {
    // 使用独立 key 创建一个单独的 PurchaseOrderViewModel 实例，避免与普通手动添加共享购物车
    val clerkVm: PurchaseOrderViewModel = viewModel(key = "clerk_manual_vm")
    // 复用现有的手动添加界面与完整逻辑，但通过 fromSmartClerk=true 切换底部按钮为“添加到智能库吏”并回流
    ManualAddPurchaseScreen(
        onNavigateBack = onNavigateBack,
        fromSmartClerk = true,
        viewModel = clerkVm
    )
}



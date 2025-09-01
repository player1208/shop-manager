package com.example.storemanagerassitent.ui.goods

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.storemanagerassitent.data.SampleData
import com.example.storemanagerassitent.ui.theme.StoreManagerAssitentTheme

/**
 * 添加商品页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoodsScreen(
    onNavigateBack: () -> Unit = {},
    onSaveGoods: (name: String, category: String, specifications: String, stockQuantity: Int, lowStockThreshold: Int) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var goodsName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var specifications by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var lowStockThreshold by remember { mutableStateOf("5") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    val categories = SampleData.categories.filter { it.id != "all" }
    
    // 表单验证
    val isFormValid = goodsName.isNotBlank() && 
                     selectedCategory.isNotBlank() && 
                     specifications.isNotBlank() &&
                     stockQuantity.toIntOrNull() != null &&
                     lowStockThreshold.toIntOrNull() != null
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加商品") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            if (isFormValid) {
                                onSaveGoods(
                                    goodsName,
                                    selectedCategory,
                                    specifications,
                                    stockQuantity.toInt(),
                                    lowStockThreshold.toInt()
                                )
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 商品名称
            OutlinedTextField(
                value = goodsName,
                onValueChange = { goodsName = it },
                label = { Text("商品名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 商品分类
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
            ) {
                OutlinedTextField(
                    value = categories.find { it.id == selectedCategory }?.name ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("商品分类 *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category.id
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }
            
            // 规格型号
            OutlinedTextField(
                value = specifications,
                onValueChange = { specifications = it },
                label = { Text("规格/型号 *") },
                placeholder = { Text("例如: 型号: J-1022") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 库存数量
            OutlinedTextField(
                value = stockQuantity,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() }) {
                        stockQuantity = it
                    }
                },
                label = { Text("库存数量 *") },
                placeholder = { Text("请输入数字") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            // 低库存阈值
            OutlinedTextField(
                value = lowStockThreshold,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() }) {
                        lowStockThreshold = it
                    }
                },
                label = { Text("低库存阈值 *") },
                placeholder = { Text("低于此数量时显示警告") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            // 说明文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "提示",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 带 * 的字段为必填项\n• 低库存阈值用于库存预警，当库存数量低于此值时会显示警告\n• 商品添加后可在商品列表中查看和管理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddGoodsScreenPreview() {
    StoreManagerAssitentTheme {
        AddGoodsScreen()
    }
}
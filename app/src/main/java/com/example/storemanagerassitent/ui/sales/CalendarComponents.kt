package com.example.storemanagerassitent.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Date

/**
 * 日历悬浮窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarBottomSheet(
    onDismiss: () -> Unit,
    onDateSelected: (Date) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "选择日期",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 日历组件
            CalendarView(
                onDateSelected = { date ->
                    onDateSelected(date)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 日历视图组件
 */
@Composable
fun CalendarView(
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = remember { Calendar.getInstance() }
    val today = remember { Calendar.getInstance() }
    
    // 当前显示的年月
    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    
    Column(modifier = modifier) {
        // 月份导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentMonth == 0) {
                        currentMonth = 11
                        currentYear--
                    } else {
                        currentMonth--
                    }
                }
            ) {
                Text("◀", style = MaterialTheme.typography.titleLarge)
            }
            
            Text(
                text = "${currentYear}年${currentMonth + 1}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = {
                    if (currentMonth == 11) {
                        currentMonth = 0
                        currentYear++
                    } else {
                        currentMonth++
                    }
                }
            ) {
                Text("▶", style = MaterialTheme.typography.titleLarge)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 星期标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 日期网格
        CalendarGrid(
            year = currentYear,
            month = currentMonth,
            today = today,
            onDateSelected = onDateSelected
        )
    }
}

/**
 * 日历网格
 */
@Composable
fun CalendarGrid(
    year: Int,
    month: Int,
    today: Calendar,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    
    // 获取本月第一天是星期几（0=周日）
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    // 获取本月有多少天
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // 计算需要显示的总格子数（6周 × 7天）
    val totalCells = 42
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(280.dp)
    ) {
        items(totalCells) { index ->
            val dayOfMonth = index - firstDayOfWeek + 1
            
            if (index < firstDayOfWeek || dayOfMonth > daysInMonth) {
                // 空白格子
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                )
            } else {
                // 日期格子
                CalendarDayCell(
                    day = dayOfMonth,
                    isToday = isToday(year, month, dayOfMonth, today),
                    onClick = {
                        calendar.set(year, month, dayOfMonth)
                        onDateSelected(calendar.time)
                    }
                )
            }
        }
    }
}

/**
 * 日历日期单元格
 */
@Composable
fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    
    val textColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val borderColor = if (isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        
        // 今天的特殊标记 - 在背景色之外添加圆点
        if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(50)
                        )
                        .padding(4.dp)
                ) {
                    // 小圆点作为今天的标记
                }
            }
        }
    }
}

/**
 * 检查是否是今天
 */
private fun isToday(year: Int, month: Int, day: Int, today: Calendar): Boolean {
    return year == today.get(Calendar.YEAR) &&
            month == today.get(Calendar.MONTH) &&
            day == today.get(Calendar.DAY_OF_MONTH)
}

/**
 * 日期范围选择器底部面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeCalendarBottomSheet(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Date, Date) -> Unit
) {
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "选择日期范围",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 已选择的日期范围显示
            if (startDate != null || endDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "开始日期: ${startDate?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).format(it) } ?: "未选择"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            startDate = null
                            endDate = null
                        }
                    )
                    Text(
                        text = "结束日期: ${endDate?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).format(it) } ?: "未选择"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            endDate = null
                        }
                    )
                }
            }
            
            // 日历组件
            CalendarView(
                onDateSelected = { date ->
                    when {
                        startDate == null -> {
                            startDate = date
                        }
                        endDate == null -> {
                            if (date.after(startDate)) {
                                endDate = date
                            } else {
                                startDate = date
                                endDate = null
                            }
                        }
                        else -> {
                            startDate = date
                            endDate = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 确认按钮
            if (startDate != null && endDate != null) {
                Button(
                    onClick = {
                        onDateRangeSelected(startDate!!, endDate!!)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("确认选择")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
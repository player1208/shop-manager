# 商品型号匹配功能使用指南

## 🎯 功能概述

新的模糊匹配算法现在以**型号匹配**作为最高优先级的匹配策略。当 API 返回的商品名中包含的型号与仓库中的型号匹配时，就会被识别为同一个商品。

## 📋 支持的型号格式

### 1. 标准工业型号格式

- `CHM2-2ZE` - 字母+数字+连字符+数字+字母
- `CHM2-2` - 字母+数字+连字符+数字
- `CH-2ZE` - 字母+连字符+数字+字母
- `CH-2` - 字母+连字符+数字

### 2. 紧凑型号格式

- `CHM22ZE` - 字母+数字+字母（无连字符）
- `CHM22` - 字母+数字（无连字符）
- `A123X` - 单字母+数字+字母

### 3. 数字型号格式

- `123-456` - 纯数字+连字符+数字
- `1234-5678` - 长数字组合

### 4. 品牌特殊格式

- `iPhone 14` - 苹果产品系列
- `iPad Air` - 苹果平板系列
- `MacBook Pro` - 苹果电脑系列

### 5. 中文品牌格式

- `小米13` - 小米手机系列
- `华为P50` - 华为手机系列
- `OPPO Find` - OPPO 手机系列

## 🔍 型号提取示例

```kotlin
// 示例商品名 -> 提取的型号
"CHM2-2ZE智能控制器" -> ["CHM2-2ZE"]
"产品型号ABC1-5X开关" -> ["ABC1-5X"]
"新品MH4-12A传感器上市" -> ["MH4-12A"]
"iPhone 14 Pro Max" -> ["IPHONE 14"]
"小米13 Ultra版本" -> ["小米13"]
"产品(CHM2-2ZE)说明书" -> ["CHM2-2ZE"]
"[A123X]型号产品" -> ["A123X"]
```

## ⚙️ 匹配策略优先级

新的匹配策略按优先级排序：

1. **型号匹配** (0.95-1.0 分) - 🥇 **最高优先级**
2. 规格包含匹配 (0.85-0.9 分)
3. 名称包含匹配 (0.55-0.8 分)
4. Jaccard 相似度 (0.0-0.5 分)
5. 编辑距离匹配 (0.0-0.45 分)

## 📊 匹配逻辑详解

### 精确型号匹配

```
API商品: "CHM2-2ZE控制器"
仓库商品: "智能控制器CHM2-2ZE"
匹配结果: ✅ 完全匹配 (得分: 1.0)
```

### 模糊型号匹配

```
API商品: "MH412A产品"
仓库商品: "MH-412A型号"
匹配结果: ✅ 模糊匹配 (得分: 0.95，去连字符后相同)
```

### 包含关系匹配

```
API商品: "iPhone 14"
仓库商品: "iPhone 14 Pro"
匹配结果: ✅ 包含匹配 (得分: 0.95)
```

### 无匹配情况

```
API商品: "CHM2-2ZE产品"
仓库商品: "ABC1-5X产品"
匹配结果: ❌ 型号不匹配，继续其他策略
```

## 🚀 使用方法

### 自动集成

型号匹配已自动集成到所有现有的商品匹配流程中：

- ✅ 进货批量扫描 (`BatchScanDialogFragment`)
- ✅ 销售批量扫描 (`SalesBatchScanDialogFragment`)
- ✅ 智能店员功能 (`SmartClerkViewModel`, `SmartClerkScreen`)

### 直接调用

```kotlin
// 使用缓存的高性能匹配
val result = FuzzyMatchCache.findBestMatchWithCache(
    apiName = "CHM2-2ZE控制器",
    apiSpec = "工业级",
    allGoods = goodsList,
    threshold = 0.6
)

if (result?.matchType == FuzzyMatcher.MatchType.MODEL_MATCH) {
    Log.d("Match", "通过型号匹配找到: ${result.goods.name}")
}
```

### 单独使用型号提取器

```kotlin
// 提取商品名中的型号
val models = ModelExtractor.extractModels("CHM2-2ZE智能控制器")
// 结果: ["CHM2-2ZE"]

// 检查两个商品是否有相同型号
val matchingModel = ModelExtractor.findMatchingModel(
    "CHM2-2ZE控制器",
    "智能控制器CHM2-2ZE"
)
// 结果: "CHM2-2ZE"
```

## 🔧 测试和调试

### 运行测试

```kotlin
// 运行完整测试套件
ModelMatchingTest.runTests()

// 测试边界情况
ModelMatchingTest.testEdgeCases()

// 性能测试
ModelMatchingTest.performanceTest()
```

### 调试日志

在 Logcat 中查看详细的匹配过程：

```
D/ModelExtractor: Pattern 1 matched: 'CHM2-2ZE' from 'CHM2-2ZE控制器'
D/ModelExtractor: Exact model match found: 'CHM2-2ZE' == 'CHM2-2ZE'
D/FuzzyMatcher: Model match found: 'CHM2-2ZE' for product 智能控制器 with score 1.0
D/FuzzyMatcher: Best match found: id=1 name='智能控制器CHM2-2ZE' score=1.000 type=MODEL_MATCH
```

## 📈 实际应用场景

### 场景 1：进货扫描

```
扫描条码 -> API返回: "CHM2-2ZE控制器模块"
仓库中存在: "智能控制器CHM2-2ZE (库存:50)"
匹配结果: ✅ 通过型号CHM2-2ZE精确匹配
```

### 场景 2：销售匹配

```
扫描条码 -> API返回: "iPhone 14手机"
仓库中存在: "iPhone 14 128GB (库存:10)"
匹配结果: ✅ 通过型号iPhone 14匹配
```

### 场景 3：智能识别

```
OCR识别: "新品MH4-12A传感器促销"
仓库中存在: "MH4-12A型精密传感器"
匹配结果: ✅ 通过型号MH4-12A精确匹配
```

## 🎯 优化建议

### 商品命名规范

为了最大化匹配准确性，建议：

1. **在商品名中明确包含型号**

   - ✅ 好: "CHM2-2ZE 智能控制器"
   - ❌ 差: "智能控制器(型号见包装)"

2. **使用标准型号格式**

   - ✅ 好: "CHM2-2ZE"、"ABC1-5X"
   - ❌ 差: "CHM2ZE"、"ABC1.5X"

3. **避免型号歧义**
   - ✅ 好: "iPhone 14"
   - ❌ 差: "iPhone 新款"

### 匹配阈值调整

根据业务需求调整匹配阈值：

```kotlin
// 严格匹配 - 只接受高分匹配
val result = FuzzyMatchCache.findBestMatchWithCache(
    apiName, apiSpec, goods, threshold = 0.9
)

// 宽松匹配 - 接受更多可能匹配
val result = FuzzyMatchCache.findBestMatchWithCache(
    apiName, apiSpec, goods, threshold = 0.5
)
```

## ⚠️ 注意事项

1. **型号格式要求**: 型号至少 3 个字符，包含字母或数字
2. **匹配优先级**: 型号匹配优先于所有其他策略
3. **性能考虑**: 大批量商品时建议使用带缓存的匹配方法
4. **日志监控**: 关注匹配日志，及时发现匹配异常

## 🆕 版本更新

- ✅ 添加了 10 种型号格式支持
- ✅ 实现了精确、模糊、包含三种匹配模式
- ✅ 集成到所有现有扫描流程
- ✅ 提供完整的测试工具
- ✅ 向后兼容，无需修改现有代码

---

**总结**: 新的型号匹配功能大幅提升了商品识别的准确性，特别是对于工业产品和电子产品。通过智能型号提取和多层次匹配策略，确保只要型号匹配就能准确识别为同一商品。🎯



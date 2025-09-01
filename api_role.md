▼ 接口信息
查询商品条形码对应的商品规格信息

接口地址：https://apis.tianapi.com/barcode/index
请求示例：https://apis.tianapi.com/barcode/index?key=你的APIKEY&barcode=6976586902578
支持协议：http/https
请求方式：get/post
返回格式：utf-8 json
▼ 请求参数
post 方式请求时，enctype 应为 application/x-www-form-urlencoded

上传文件二进制数据流方式，enctype 必须为 multipart/form-data

参数 url、base64 中有特殊字符时，建议对值 urlencode 编码后传递

名称 类型 必须 示例值/默认值 说明
key string 是 您自己的 APIKEY（注册账号后获得） API 密钥
barcode string 是 6976586902578 条形码
▼ 返回示例
接口数据样例仅作为预览参考，请以实际测试结果为准

旧域名返回的 json 结构和现在略有不同，请点击此处查看说明

成功调用，返回内容并产生计费：

    {

"code": 200,
"msg": "success",
"result": {
"barcode": "6976586902578",
"name": "多种维生素 B 族片",
"spec": "50 克",
"brand": "贤健",
"firm_name": "安徽谊康堂健康科技有限公司",
"firm_address": "",
"firm_status": "",
"gross_weight": "",
"width": "",
"height": "",
"depth": "",
"goods_type": "营养补充剂",
"goods_pic": "https://api.tianapi.com/goodspic/?img=qhUnfmO4bzEH"
}
}

失败调用，查看接口错误码释义：

    {

"code": 150,
"msg": "API 可用次数不足"
}

▼ 返回参数
公共参数指所有接口都会返回的参数，应用参数每个接口都不同

名称 类型 示例值 说明
公共参数
code int 200 状态码
msg string success 错误信息
result object {} 返回结果集
应用参数
barcode string 6976586902578 条形码
name string 多种维生素 B 族片 商品名称
spec string 50 克 规格
brand string 品牌 贤健
firm_name string 安徽谊康堂健康科技有限公司 生产商
firm_address string 生产商地址
firm_status string 生产状态
gross_weight string 毛重
width string 宽
height string 高
depth string 深
goods_type string 营养补充剂 商品分类
goods_pic string https://api.tianapi.com/goodspic/?img=qhUnfmO4bzEH8B 商品图片(3 小时内有效)
▼ 接口价格
本接口为按次计费类接口，如接口可用次数不足可点此充值天豆

充值金额 可用次数 参考单价
免费 100 次 ≈0 元/次
10 元(10W 天豆) 555 次 ≈0.018 元/次
100 元(110W 天豆)惠 6111 次 ≈0.0164 元/次
500 元(600W 天豆)惠 33333 次 ≈0.015 元/次
2000 元(2600W 天豆)惠 144444 次 ≈0.0138 元/次
2000 元以上专享优惠 套餐定制 联系商务客服
▼ 返回状态码
错误信息可能会有所调整，请根据错误状态码(code)进行流程判断

错误状态码 错误信息 解释帮助
100 内部服务器错误 报此错误码请及时反馈或等待官方修复
110 当前 API 已下线 接口已下线无法使用，可关注相关通知
120 API 暂时维护中 接口暂时关闭维护中，请注意相关公告
130 API 调用频率超限 超过每秒请求数上限，可在控制台-接口管理中查询
140 API 没有调用权限 请检查是否自行在接口管理中停用或被禁用了该接口
150 API 可用次数不足 免费类接口套餐超限或计次类接口余额不足，点此查看说明
160 账号未申请该 API 请先在接口文档页面申请该接口，点此查看说明
170 Referer 请求来源受限 设置了 Referer 白名单，但来源 Referer 不在白名单内
180 IP 请求来源受限 设置了 IP 白名单，但来源 IP 不在白名单内
190 当前 key 不可用 通常为账号无效，此状态无法恢复
230 key 错误或为空 请检查 apikey 是否填写错误，点此查看帮助
240 缺少 key 参数 请检查是否传递了 key 参数或者编码格式是否符合要求
250 数据返回为空 数据查询或转换失败，请检查输入值或注意中文编码问题
260 参数值不得为空 请检查关键参数是否传递了空值
270 参数值不符合要求 参数值不符合基本格式要求，点此查看说明
280 缺少必要的参数 缺少必填的参数，请根据接口文档检查
290 超过最大输入限制 参数值超过输入范围，请查看接口文档的说明
错误码 1 开头的是系统级错误，2 开头的是用户级错误，其中 200 表示请求成功处理并计费。
▼ 参考代码
此处代码仅演示关键请求片段，实际使用请根据具体环境修改

如需深入学习参考，可点击此处查看部分第三方完整开源项目

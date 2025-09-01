'use strict'

// Load local environment variables when running outside cloud (optional)
try { require('dotenv').config() } catch (_) {}

// Local Express wrapper for quick verification in a web environment.
const express = require('express')
const morgan = require('morgan')
const cors = require('cors')
const { main_handler } = require('./index')

const app = express()
app.use(morgan('tiny'))
app.use(cors({ origin: '*', methods: ['GET'] }))

app.get('/api/barcode/goods/details', async (req, res) => {
  const event = { queryStringParameters: req.query }
  const resp = await main_handler(event, {})
  res.status(resp.statusCode || 200)
  Object.entries(resp.headers || {}).forEach(([k, v]) => res.setHeader(k, v))
  res.send(resp.body)
})

// SCF Web函数要求监听 9000 端口；平台会注入 PORT=9000。
// 这里默认也设为 9000，避免未注入环境变量时端口不一致。
const port = process.env.PORT || 9000
app.listen(port, () => console.log(`Local SCF server on :${port}`))



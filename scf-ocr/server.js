'use strict'

try { require('dotenv').config() } catch (_) {}

const express = require('express')
const morgan = require('morgan')
const cors = require('cors')
const { main_handler } = require('./index')

const app = express()
app.use(morgan('tiny'))
app.use(cors({ origin: '*', methods: ['GET', 'POST'] }))
app.use(express.json({ limit: '10mb' }))

app.post('/ocr/extract', async (req, res) => {
  const event = { body: JSON.stringify(req.body) }
  const resp = await main_handler(event, {})
  res.status(resp.statusCode || 200)
  Object.entries(resp.headers || {}).forEach(([k, v]) => res.setHeader(k, v))
  res.send(resp.body)
})

app.get('/ocr/extract', async (req, res) => {
  const event = { queryStringParameters: req.query }
  const resp = await main_handler(event, {})
  res.status(resp.statusCode || 200)
  Object.entries(resp.headers || {}).forEach(([k, v]) => res.setHeader(k, v))
  res.send(resp.body)
})

const port = process.env.PORT || 9000
app.listen(port, () => console.log(`Local OCR SCF server on :${port}`))








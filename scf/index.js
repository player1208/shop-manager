'use strict'

// Serverless Cloud Function (e.g., Tencent SCF / AWS Lambda compatible style)
// Adapts TianAPI barcode API to a unified response shape: { code, msg, data }

// Load local environment variables when running outside cloud (optional)
try { require('dotenv').config() } catch (_) {}

const axios = require('axios')

const TIAN_BASE = 'https://apis.tianapi.com/'
const TIAN_PATH = 'barcode/index'
// API key must come from environment variables in production/local; do not hardcode secrets
const TIAN_API_KEY = process.env.TIAN_API_KEY

async function queryTianApi(barcode) {
  const url = `${TIAN_BASE}${TIAN_PATH}`
  const resp = await axios.post(url, new URLSearchParams({
    key: TIAN_API_KEY,
    barcode: String(barcode || '').trim()
  }), {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' },
    timeout: 8000
  })
  return resp
}

function toUnifiedResponse(tian) {
  // TianAPI: { code: 200, msg: 'success', result: { barcode, name, spec, ... } }
  if (tian && tian.code === 200 && tian.result) {
    return {
      code: 1,
      msg: 'OK',
      data: {
        goodsName: tian.result.name || '',
        standard: tian.result.spec || '',
        barcode: tian.result.barcode || ''
      }
    }
  }
  return {
    code: tian?.code || 500,
    msg: tian?.msg || 'upstream_error',
    data: null
  }
}

// SCF handler
exports.main_handler = async (event, context) => {
  try {
    const query = event?.queryString || event?.queryStringParameters || event || {}
    const barcode = String(query.barcode || '').trim()
    if (!TIAN_API_KEY) {
      return {
        statusCode: 500,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: 500, msg: 'missing_env_TIAN_API_KEY', data: null })
      }
    }
    if (!barcode) {
      return {
        statusCode: 400,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: 400, msg: 'missing barcode', data: null })
      }
    }

    const upstream = await queryTianApi(barcode)
    const unified = toUnifiedResponse(upstream.data)

    return {
      statusCode: 200,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(unified)
    }
  } catch (e) {
    return {
      statusCode: 500,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: 500, msg: e.message || 'server_error', data: null })
    }
  }
}




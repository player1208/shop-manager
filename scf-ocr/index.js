'use strict'

// Load env locally for development
try { require('dotenv').config() } catch (_) {}

const crypto = require('crypto')
const axios = require('axios')

// Environment variables: prefer safe custom names (TC_*),
// but also auto-detect Tencent's built-in temporary credentials.
// Note: SCF forbids user-defined keys starting with SCF_/QCLOUD_/TENCENTCLOUD_.
const SECRET_ID = process.env.TC_SECRET_ID || process.env.TENCENTCLOUD_SECRETID || process.env.TENCENTCLOUD_SECRET_ID
const SECRET_KEY = process.env.TC_SECRET_KEY || process.env.TENCENTCLOUD_SECRETKEY || process.env.TENCENTCLOUD_SECRET_KEY
const SESSION_TOKEN = process.env.TC_SESSION_TOKEN || process.env.TENCENTCLOUD_SESSIONTOKEN || process.env.TENCENTCLOUD_SESSION_TOKEN
const REGION = process.env.TC_REGION || process.env.TENCENTCLOUD_REGION || ''

const SERVICE = 'ocr'
const HOST = 'ocr.tencentcloudapi.com'
const ENDPOINT = `https://${HOST}`
const ACTION = 'ExtractDocMulti'
const VERSION = '2018-11-19'

function sha256Hex(message) {
  return crypto.createHash('sha256').update(message).digest('hex')
}

function hmacSha256(buffer, key, encoding) {
  return crypto.createHmac('sha256', key).update(buffer).digest(encoding)
}

function getDate(timestamp) {
  const date = new Date(timestamp * 1000).toISOString().slice(0, 10)
  return date
}

function buildTc3Signature(payload, timestamp) {
  const httpRequestMethod = 'POST'
  const canonicalUri = '/'
  const canonicalQueryString = ''
  const canonicalHeaders = `content-type:application/json; charset=utf-8\nhost:${HOST}\n`
  const signedHeaders = 'content-type;host'
  const hashedRequestPayload = sha256Hex(payload)
  const canonicalRequest = [
    httpRequestMethod,
    canonicalUri,
    canonicalQueryString,
    canonicalHeaders,
    signedHeaders,
    hashedRequestPayload
  ].join('\n')

  const date = getDate(timestamp)
  const credentialScope = `${date}/${SERVICE}/tc3_request`
  const hashedCanonicalRequest = sha256Hex(canonicalRequest)
  const stringToSign = `TC3-HMAC-SHA256\n${timestamp}\n${credentialScope}\n${hashedCanonicalRequest}`

  const secretDate = hmacSha256(date, 'TC3' + SECRET_KEY)
  const secretService = hmacSha256(SERVICE, secretDate)
  const secretSigning = hmacSha256('tc3_request', secretService)
  const signature = hmacSha256(stringToSign, secretSigning, 'hex')

  return { signature, credentialScope, signedHeaders }
}

async function callOcrApi(bodyObj) {
  if (!SECRET_ID || !SECRET_KEY) {
    const err = new Error('missing_env_TENCENTCLOUD_SECRET_ID_or_SECRET_KEY')
    err.statusCode = 500
    throw err
  }

  const payload = JSON.stringify(bodyObj)
  const timestamp = Math.floor(Date.now() / 1000)
  const { signature, credentialScope, signedHeaders } = buildTc3Signature(payload, timestamp)

  const headers = {
    'Content-Type': 'application/json; charset=utf-8',
    'Host': HOST,
    'X-TC-Action': ACTION,
    'X-TC-Version': VERSION,
    ...(REGION ? { 'X-TC-Region': REGION } : {}),
    'X-TC-Timestamp': String(timestamp),
    'Authorization': `TC3-HMAC-SHA256 Credential=${SECRET_ID}/${credentialScope}, SignedHeaders=${signedHeaders}, Signature=${signature}`
  }
  if (SESSION_TOKEN) headers['X-TC-Token'] = SESSION_TOKEN

  const resp = await axios.post(ENDPOINT, payload, { headers, timeout: 10000 })
  return resp.data
}

function buildRequestBodyFromEvent(event) {
  const query = event?.queryString || event?.queryStringParameters || {}
  const body = event?.body ? (typeof event.body === 'string' ? event.body : JSON.stringify(event.body)) : ''
  let fromBody = {}
  try { if (body) fromBody = JSON.parse(body) } catch (_) {}

  const ImageUrl = fromBody.ImageUrl || query.ImageUrl || ''
  const ImageBase64 = fromBody.ImageBase64 || query.ImageBase64 || ''
  const PdfPageNumber = Number(fromBody.PdfPageNumber || query.PdfPageNumber || '') || undefined
  const ItemNames = fromBody.ItemNames || query.ItemNames || undefined
  const ItemNamesShowMode = typeof fromBody.ItemNamesShowMode === 'boolean' ? fromBody.ItemNamesShowMode : (query.ItemNamesShowMode === 'true' ? true : undefined)
  const ReturnFullText = typeof fromBody.ReturnFullText === 'boolean' ? fromBody.ReturnFullText : (query.ReturnFullText === 'true' ? true : undefined)
  const ConfigId = fromBody.ConfigId || query.ConfigId || 'General'
  const EnableCoord = typeof fromBody.EnableCoord === 'boolean' ? fromBody.EnableCoord : (query.EnableCoord === 'true' ? true : undefined)
  const OutputParentKey = typeof fromBody.OutputParentKey === 'boolean' ? fromBody.OutputParentKey : (query.OutputParentKey === 'true' ? true : undefined)
  const OutputLanguage = fromBody.OutputLanguage || query.OutputLanguage || undefined

  const req = {
    ConfigId,
  }
  if (ImageUrl) req.ImageUrl = ImageUrl
  if (ImageBase64) req.ImageBase64 = ImageBase64
  if (PdfPageNumber) req.PdfPageNumber = PdfPageNumber
  if (Array.isArray(ItemNames)) req.ItemNames = ItemNames
  if (typeof ItemNamesShowMode === 'boolean') req.ItemNamesShowMode = ItemNamesShowMode
  if (typeof ReturnFullText === 'boolean') req.ReturnFullText = ReturnFullText
  if (typeof EnableCoord === 'boolean') req.EnableCoord = EnableCoord
  if (typeof OutputParentKey === 'boolean') req.OutputParentKey = OutputParentKey
  if (OutputLanguage) req.OutputLanguage = OutputLanguage

  if (!req.ImageUrl && !req.ImageBase64) {
    const err = new Error('missing ImageUrl or ImageBase64')
    err.statusCode = 400
    throw err
  }

  return req
}

function ok(body) {
  return { statusCode: 200, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
}

function err(statusCode, message) {
  return { statusCode, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ code: statusCode, msg: message }) }
}

exports.main_handler = async (event, context) => {
  try {
    const requestBody = buildRequestBodyFromEvent(event)
    const data = await callOcrApi(requestBody)
    return ok(data)
  } catch (e) {
    const status = e.statusCode || e.response?.status || 500
    const message = e.response?.data || e.message || 'server_error'
    return err(status, typeof message === 'string' ? message : JSON.stringify(message))
  }
}



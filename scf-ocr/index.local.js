'use strict'

const { main_handler } = require('./index')

async function run() {
  const ImageUrl = process.argv[2] || 'https://ocr-demo-1254418846.cos.ap-guangzhou.myqcloud.com/document/SmartStructuralOCR/SmartStructuralPro1.png'
  const event = { body: JSON.stringify({ ImageUrl, ConfigId: 'General', ItemNames: ['号码'], ReturnFullText: false }) }
  const resp = await main_handler(event, {})
  console.log('HTTP', resp.statusCode)
  console.log(resp.body)
}

run().catch(err => {
  console.error(err)
  process.exit(1)
})








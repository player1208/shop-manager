'use strict'

const { main_handler } = require('./index')

async function run() {
  const barcode = process.argv[2] || '6921168560509'
  const event = { queryStringParameters: { barcode } }
  const resp = await main_handler(event, {})
  console.log('HTTP', resp.statusCode)
  console.log(resp.body)
}

run().catch(err => {
  console.error(err)
  process.exit(1)
})











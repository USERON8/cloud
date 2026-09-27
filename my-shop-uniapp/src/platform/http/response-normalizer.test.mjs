import assert from 'node:assert/strict'
import test from 'node:test'
import {
  normalizeResponseData,
  parseJsonTextWithLongIntegers
} from './response-normalizer.ts'

test('preserves Java Long identifiers without numeric precision loss', () => {
  const payload = parseJsonTextWithLongIntegers(
    '{"code":200,"data":{"id":9223372036854775807,"userId":9007199254740993}}'
  )

  assert.deepEqual(payload, {
    code: 200,
    data: {
      id: '9223372036854775807',
      userId: '9007199254740993'
    }
  })
})

test('normalizes small identifier fields and identifier arrays to strings', () => {
  const payload = normalizeResponseData(
    '{"data":{"productId":50001,"shopId":30001,"merchantIds":[1,2,"3"],"status":1}}'
  )

  assert.deepEqual(payload, {
    data: {
      productId: '50001',
      shopId: '30001',
      merchantIds: ['1', '2', '3'],
      status: 1
    }
  })
})

test('keeps non-JSON text responses unchanged', () => {
  const html = '<form action="https://pay.example.test"></form>'
  assert.equal(normalizeResponseData(html), html)
})

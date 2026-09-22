import assert from 'node:assert/strict'
import test from 'node:test'
import { createOpsInputTools } from './input-tools.ts'

test('ops input tools report invalid optional values without throwing', () => {
  const errors = []
  const tools = createOpsInputTools((message) => errors.push(message))

  assert.equal(tools.parseOptionalNumber('abc', 'Price'), null)
  assert.equal(tools.parseOptionalBoolean('sometimes', 'Enabled'), null)
  assert.deepEqual(errors, ['Price must be numeric', 'Enabled must be true or false'])
})

test('ops input tools normalize lists and compact request payloads', () => {
  const tools = createOpsInputTools(() => {})

  assert.deepEqual(tools.parseNumberList('1, nope, 2, -3'), [1, 2])
  assert.deepEqual(tools.parseStringList('new, , hot'), ['new', 'hot'])
  assert.deepEqual(
    tools.compactPayload({ keyword: 'phone', status: '', tags: [], page: 0, enabled: false }),
    { keyword: 'phone', page: 0, enabled: false }
  )
})

test('ops input tools validate JSON and ISO-like dates', () => {
  const errors = []
  const tools = createOpsInputTools((message) => errors.push(message))

  assert.deepEqual(tools.parseJson('{"page":1}', 'Search'), { page: 1 })
  assert.equal(tools.normalizeDateInput('2026-09-21', 'Start date'), '2026-09-21')
  assert.equal(tools.normalizeDateInput('21/09/2026', 'Start date'), null)
  assert.equal(errors.at(-1), 'Start date must use the YYYY-MM-DD format')
})
